package org.skylark.bluewhale.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skylark.bluewhale.domain.context.BudgetSpec;
import org.skylark.bluewhale.domain.context.ContextBundle;
import org.skylark.bluewhale.domain.context.ContextUnit;
import org.skylark.bluewhale.domain.context.Lifecycle;
import org.skylark.bluewhale.domain.context.Lineage;
import org.skylark.bluewhale.domain.context.Origin;
import org.skylark.bluewhale.domain.context.Provenance;
import org.skylark.bluewhale.domain.context.RepresentationDepth;
import org.skylark.bluewhale.domain.context.RepresentationLine;
import org.skylark.bluewhale.domain.context.ResolveQuery;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.Trust;
import org.skylark.bluewhale.domain.context.UnitContent;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.context.UnitKind;
import org.skylark.bluewhale.domain.context.UnitState;
import org.skylark.bluewhale.domain.context.Economy;
import org.skylark.bluewhale.domain.context.Ownership;
import org.skylark.bluewhale.domain.context.AccessPolicy;
import org.skylark.bluewhale.domain.memory.CaptureSignal;
import org.skylark.bluewhale.domain.port.EmbeddingPort;
import org.skylark.bluewhale.domain.port.IndexPort;
import org.skylark.bluewhale.infrastructure.index.InMemoryIndexAdapter;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryContextUnitRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryEpisodeRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryLifecycleEventRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryUnitEdgeRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryUsageEventRepository;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolveContextUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-09T08:00:00Z");
    private static final Scope SCOPE = Scope.agent("acme", "agent-1");

    private InMemoryContextUnitRepository unitRepository;
    private InMemoryUsageEventRepository usageEventRepository;
    private InMemoryIndexAdapter index;
    private ResolveContextUseCase resolve;
    private CaptureMemoryUseCase capture;

    @BeforeEach
    void setUp() {
        unitRepository = new InMemoryContextUnitRepository();
        usageEventRepository = new InMemoryUsageEventRepository();
        index = new InMemoryIndexAdapter();
        InMemoryLifecycleEventRepository events = new InMemoryLifecycleEventRepository();
        InMemoryEpisodeRepository episodes = new InMemoryEpisodeRepository();
        EmbeddingPort embedding = new FixedEmbedding();
        resolve = new ResolveContextUseCase(embedding, index, unitRepository,
                AccessPolicy.open(), () -> "rel-test", () -> NOW,
                new SynchronousUsageEventRecorder(usageEventRepository));
        capture = new CaptureMemoryUseCase(episodes, unitRepository, events,
                embedding, index, new InMemoryUnitEdgeRepository(), () -> NOW);
    }

    @Test
    void capturedMemoryShouldBecomeActiveAndRetrievable() {
        CaptureResult captureResult = capture.capture(new CaptureSignal("trace-capture", SCOPE,
                "conversation", "User prefers dark theme", UnitKind.WORKING, 0.9,
                List.of("preference")));

        assertEquals(UnitState.ACTIVE, captureResult.state());
        assertTrue(captureResult.indexed());

        ContextBundle bundle = resolve.resolve(query("trace-resolve", "TASK", "dark theme", 100));

        assertEquals(ContextBundle.Gate.RETRIEVAL_BEARING, bundle.gate());
        assertEquals("rel-test", bundle.releaseId());
        assertEquals(1, bundle.sections().size());
        assertEquals(ContextBundle.Slot.CORE, bundle.sections().get(0).slot());
        assertTrue(bundle.sections().get(0).citation().contains("@sha256:"));
        assertEquals(0, bundle.cost().retrievalLlmCalls());
        assertTrue(bundle.trace().stream().anyMatch(step -> step.stage().equals("rrf")));
        assertTrue(bundle.trace().stream().anyMatch(step -> step.stage().equals("mmr")));
        assertEquals(1, usageEventRepository.findByUnitId(
                org.skylark.bluewhale.domain.context.UnitId.of(captureResult.unitId())).size());
    }

    @Test
    void exactDuplicateContentShouldBeInjectedOnlyOnce() {
        capture.capture(new CaptureSignal("dup-1", SCOPE, "conversation",
                "User prefers dark theme", UnitKind.WORKING, 0.9, List.of()));
        capture.capture(new CaptureSignal("dup-2", SCOPE, "conversation",
                "User prefers dark theme", UnitKind.WORKING, 0.9, List.of()));
        capture.capture(new CaptureSignal("dup-3", SCOPE, "conversation",
                "Keep answers concise", UnitKind.WORKING, 0.9, List.of()));

        ContextBundle bundle = resolve.resolve(query("trace-dedupe", "TASK", "dark theme", 100));

        assertEquals(2, bundle.sections().size());
        assertEquals(2, bundle.sections().stream()
                .map(ContextBundle.Section::contentHash).distinct().count());
        assertTrue(bundle.trace().stream().anyMatch(step -> step.stage().equals("dedupe")
                && step.detail().contains("1 duplicate")));
    }

    @Test
    void temporalIntentShouldUseTheEventTimelineLine() {
        unitRepository.save(activeUnit("older event", NOW.minusSeconds(120)));
        unitRepository.save(activeUnit("newer event", NOW.minusSeconds(30)));

        ContextBundle bundle = resolve.resolve(new ResolveQuery("trace-timeline", SCOPE,
                "TEMPORAL_ORDER", "summarize the timeline", new BudgetSpec(100),
                Set.of(RepresentationLine.EPISODE_TIMELINE), RepresentationDepth.OVERVIEW));

        assertEquals(ContextBundle.Gate.RETRIEVAL_BEARING, bundle.gate());
        assertEquals(2, bundle.sections().size());
        assertEquals("newer event", bundle.sections().get(0).text());
        assertTrue(bundle.trace().stream()
                .anyMatch(step -> step.stage().equals("timeline-recall")));
    }

    @Test
    void nonRetrievalIntentShouldReturnEmptyWithoutSearching() {
        ContextBundle bundle = resolve.resolve(query("trace-chat", "CHITCHAT", "hello", 100));

        assertEquals(ContextBundle.Gate.NON_RETRIEVAL, bundle.gate());
        assertEquals("NON_RETRIEVAL_INTENT", bundle.emptyReason());
        assertTrue(bundle.sections().isEmpty());
    }

    @Test
    void nonRetrievableStateMustBeFilteredBeforeAssembly() {
        ContextUnit quarantined = ContextUnit.create(UnitId.generate(), SCOPE,
                UnitKind.SEMANTIC, Origin.OBSERVED, UnitContent.of("untrusted content"),
                new Lifecycle(UnitState.QUARANTINED, NOW.minusSeconds(60), null,
                        NOW.minusSeconds(60), null, null, NOW.minusSeconds(60), null, null, null),
                Ownership.unowned(), Trust.observed(0.9), Economy.initial(0.9),
                Provenance.ofSource("untrusted.txt"), Lineage.empty(), List.of());
        unitRepository.save(quarantined);
        index.index(new IndexPort.IndexEntry(quarantined.id(), quarantined.scope(),
                RepresentationLine.EPISODE_TIMELINE, "untrusted content", new float[]{1, 0, 0},
                java.util.Map.of()));

        ContextBundle bundle = resolve.resolve(query("trace-quarantine", "TASK", "untrusted", 100));

        assertEquals(ContextBundle.Gate.EMPTY, bundle.gate());
        assertEquals("NO_RETRIEVABLE_UNITS", bundle.emptyReason());
        assertTrue(bundle.sections().isEmpty());
    }

    @Test
    void accessPolicyShouldRejectOutOfTenantScope() {
        ResolveContextUseCase restricted = new ResolveContextUseCase(new FixedEmbedding(), index,
                unitRepository,
                new AccessPolicy(Set.of("other"), Set.of(), Set.of()),
                () -> "rel-test", () -> NOW,
                new SynchronousUsageEventRecorder(usageEventRepository));

        ContextBundle bundle = restricted.resolve(query("trace-denied", "TASK", "anything", 100));

        assertEquals(ContextBundle.Gate.EMPTY, bundle.gate());
        assertEquals("ACCESS_DENIED", bundle.emptyReason());
        assertNotNull(bundle.trace());
    }

    private static ContextUnit activeUnit(String text, Instant capturedAt) {
        return ContextUnit.create(UnitId.generate(), SCOPE, UnitKind.EPISODIC, Origin.OBSERVED,
                UnitContent.of(text),
                new Lifecycle(UnitState.ACTIVE, capturedAt, null, capturedAt, null,
                        null, capturedAt, null, null, null),
                Ownership.unowned(), Trust.observed(0.9), Economy.initial(0.9),
                Provenance.ofSource("timeline"), Lineage.empty(), List.of());
    }

    private static ResolveQuery query(String traceId, String intent, String text, int budget) {
        return new ResolveQuery(traceId, SCOPE, intent, text, new BudgetSpec(budget),
                Set.of(RepresentationLine.values()), RepresentationDepth.OVERVIEW);
    }

    private static final class FixedEmbedding implements EmbeddingPort {
        @Override
        public float[] embed(String text) {
            return new float[]{1, 0, 0};
        }

        @Override
        public List<float[]> embedBatch(List<String> texts) {
            return texts.stream().map(this::embed).toList();
        }

        @Override
        public int getDimension() {
            return 3;
        }
    }
}
