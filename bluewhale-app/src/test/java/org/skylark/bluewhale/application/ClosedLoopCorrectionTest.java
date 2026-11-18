package org.skylark.bluewhale.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skylark.bluewhale.domain.context.Actor;
import org.skylark.bluewhale.domain.context.ActorType;
import org.skylark.bluewhale.domain.context.ContextUnit;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.context.UnitState;
import org.skylark.bluewhale.domain.feedback.Attribution;
import org.skylark.bluewhale.domain.feedback.AttributionCode;
import org.skylark.bluewhale.domain.feedback.AttributionFacts;
import org.skylark.bluewhale.domain.feedback.Signal;
import org.skylark.bluewhale.domain.feedback.SignalKind;
import org.skylark.bluewhale.domain.governance.ChangeRequest;
import org.skylark.bluewhale.domain.governance.ChangeRequestType;
import org.skylark.bluewhale.domain.port.EmbeddingPort;
import org.skylark.bluewhale.domain.context.AccessPolicy;
import org.skylark.bluewhale.infrastructure.index.InMemoryIndexAdapter;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryAttributionRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryChangeRequestRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryContextUnitRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryLifecycleEventRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryRegressionCaseRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemorySignalRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryUsageEventRepository;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClosedLoopCorrectionTest {

    private static final Instant NOW = Instant.parse("2026-11-12T08:00:00Z");
    private static final Scope SCOPE = Scope.of("acme", "ws-1", null, null, null, null);
    private static final Actor HUMAN = new Actor("reviewer-1", ActorType.HUMAN);

    private InMemoryContextUnitRepository unitRepository;
    private InMemoryChangeRequestRepository changeRequestRepository;
    private GovernReviewUseCase govern;
    private RecordSignalUseCase recordSignal;
    private AttributeSignalUseCase attributeSignal;
    private DistillUseCase distill;
    private RegressionUseCase regression;

    @BeforeEach
    void setUp() {
        unitRepository = new InMemoryContextUnitRepository();
        InMemoryLifecycleEventRepository events = new InMemoryLifecycleEventRepository();
        LifecycleUseCase lifecycle = new LifecycleUseCase(unitRepository, events, () -> NOW);
        InMemoryIndexAdapter index = new InMemoryIndexAdapter();
        EmbeddingPort embedding = new FixedEmbedding();
        ChangeApplier applier = new ChangeApplier(unitRepository, lifecycle, index, embedding, () -> NOW);
        changeRequestRepository = new InMemoryChangeRequestRepository();
        IngestContextUseCase ingest = new IngestContextUseCase(changeRequestRepository, () -> NOW);
        govern = new GovernReviewUseCase(changeRequestRepository, applier, () -> NOW);
        InMemorySignalRepository signalRepository = new InMemorySignalRepository();
        InMemoryAttributionRepository attributionRepository = new InMemoryAttributionRepository();
        InMemoryRegressionCaseRepository regressionCaseRepository = new InMemoryRegressionCaseRepository();
        recordSignal = new RecordSignalUseCase(signalRepository);
        attributeSignal = new AttributeSignalUseCase(attributionRepository, () -> NOW);
        distill = new DistillUseCase(signalRepository, attributionRepository, changeRequestRepository,
                regressionCaseRepository, unitRepository, () -> NOW);
        ResolveContextUseCase resolve = new ResolveContextUseCase(embedding, index, unitRepository,
                AccessPolicy.open(), () -> "rel-test", () -> NOW,
                new SynchronousUsageEventRecorder(new InMemoryUsageEventRepository()));
        regression = new RegressionUseCase(regressionCaseRepository, resolve, unitRepository);
        // 初始知识入库（ADD 走完整治理链）
        IngestResult submitted = ingest.submit(new IngestCommand("trace-1", "author-1", SCOPE,
                "owner-1", "legal", "保证金应退还", "policy.md"));
        UnitId crId = UnitId.of(submitted.crId());
        decide(crId, ChangeRequestDecision.Action.SUBMIT, null);
        decide(crId, ChangeRequestDecision.Action.APPROVE, null);
        decide(crId, ChangeRequestDecision.Action.REGRESS, null);
        decide(crId, ChangeRequestDecision.Action.READY, null);
        GovernResult released = govern.decide(new ChangeRequestDecision(crId,
                ChangeRequestDecision.Action.RELEASE, HUMAN, null));
        initialUnitId = UnitId.of(released.appliedUnitId());
    }

    private UnitId initialUnitId;

    private void decide(UnitId crId, ChangeRequestDecision.Action action, UnitId runId) {
        govern.decide(new ChangeRequestDecision(crId, action, HUMAN, runId));
    }

    @Test
    void humanCorrectionFlowsThroughTheFullClosedLoop() {
        Signal signal = Signal.report("trace-2", SignalKind.EDIT, initialUnitId, null,
                "HIGH", "保证金应在7个工作日内退还", "reviewer-1", NOW);
        recordSignal.record(signal);

        Attribution attribution = attributeSignal.attribute(signal,
                new AttributionFacts(false, true, true, true, true, false)).orElseThrow();
        assertEquals(AttributionCode.BAD_CLAIM, attribution.code());

        List<UnitId> drafted = distill.distill(SCOPE);
        assertEquals(1, drafted.size());
        UnitId reviseCrId = drafted.get(0);
        ChangeRequest reviseCr = changeRequestRepository.findById(reviseCrId).orElseThrow();
        assertEquals(ChangeRequestType.REVISE, reviseCr.type());
        assertEquals("SYSTEM", reviseCr.author());

        decide(reviseCrId, ChangeRequestDecision.Action.SUBMIT, null);
        decide(reviseCrId, ChangeRequestDecision.Action.APPROVE, null);
        decide(reviseCrId, ChangeRequestDecision.Action.REGRESS, null);
        RegressionReport report = regression.runDefectSet(SCOPE);
        assertTrue(report.allPassed());
        decide(reviseCrId, ChangeRequestDecision.Action.READY, report.runId());
        GovernResult released = govern.decide(new ChangeRequestDecision(reviseCrId,
                ChangeRequestDecision.Action.RELEASE, HUMAN, null));

        UnitId newUnitId = UnitId.of(released.appliedUnitId());
        assertEquals(UnitState.SUPERSEDED, unitRepository.findById(initialUnitId).orElseThrow()
                .lifecycle().state());
        ContextUnit revised = unitRepository.findById(newUnitId).orElseThrow();
        assertEquals(UnitState.ACTIVE, revised.lifecycle().state());
        assertEquals("保证金应在7个工作日内退还", revised.content().abstractText());
        assertTrue(revised.lineage().derivedFrom().contains(initialUnitId));
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