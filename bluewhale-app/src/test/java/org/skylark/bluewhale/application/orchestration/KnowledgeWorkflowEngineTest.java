package org.skylark.bluewhale.application.orchestration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skylark.bluewhale.application.ChangeApplier;
import org.skylark.bluewhale.application.GovernReviewUseCase;
import org.skylark.bluewhale.application.IngestContextUseCase;
import org.skylark.bluewhale.application.LifecycleUseCase;
import org.skylark.bluewhale.domain.context.Actor;
import org.skylark.bluewhale.domain.context.ActorType;
import org.skylark.bluewhale.domain.context.Origin;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.knowledge.Claim;
import org.skylark.bluewhale.domain.knowledge.EvidenceSpan;
import org.skylark.bluewhale.domain.knowledge.WikiPage;
import org.skylark.bluewhale.domain.ontology.OntologySchema;
import org.skylark.bluewhale.domain.ontology.PropertyDef;
import org.skylark.bluewhale.domain.orchestration.WorkflowInstance;
import org.skylark.bluewhale.domain.orchestration.WorkflowState;
import org.skylark.bluewhale.domain.port.ClaimExtractionPort;
import org.skylark.bluewhale.domain.port.ConnectorPort;
import org.skylark.bluewhale.domain.port.EmbeddingPort;
import org.skylark.bluewhale.domain.port.OntologyPort;
import org.skylark.bluewhale.domain.port.WikiStorePort;
import org.skylark.bluewhale.infrastructure.index.InMemoryIndexAdapter;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryChangeRequestRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryContextUnitRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryLifecycleEventRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryWorkflowInstanceRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeWorkflowEngineTest {

    private static final Instant NOW = Instant.parse("2026-11-12T08:00:00Z");
    private static final Scope SCOPE = Scope.of("acme", "ws-1", null, null, null, null);
    private static final Actor HUMAN = new Actor("reviewer-1", ActorType.HUMAN);
    private static final Actor SYSTEM = Actor.system("bot");
    private static final OntologySchema ONTOLOGY = new OntologySchema("1.0",
            List.of(new PropertyDef("deposit_refund_condition", "enum", List.of("normal"), true)),
            List.of(), List.of());

    private InMemoryContextUnitRepository unitRepository;
    private InMemoryWorkflowInstanceRepository workflows;
    private FakeWikiStore wikiStore;
    private KnowledgeWorkflowEngine engine;

    @BeforeEach
    void setUp() {
        unitRepository = new InMemoryContextUnitRepository();
        InMemoryChangeRequestRepository changeRequests = new InMemoryChangeRequestRepository();
        InMemoryLifecycleEventRepository events = new InMemoryLifecycleEventRepository();
        LifecycleUseCase lifecycle = new LifecycleUseCase(unitRepository, events, () -> NOW);
        InMemoryIndexAdapter index = new InMemoryIndexAdapter();
        EmbeddingPort embedding = new FixedEmbedding();
        ChangeApplier applier = new ChangeApplier(unitRepository, lifecycle, index, embedding, () -> NOW);
        IngestContextUseCase ingest = new IngestContextUseCase(changeRequests, () -> NOW);
        GovernReviewUseCase govern = new GovernReviewUseCase(changeRequests, applier, () -> NOW);
        workflows = new InMemoryWorkflowInstanceRepository();
        wikiStore = new FakeWikiStore();
        ConnectorPort connector = uri -> new ConnectorPort.FetchedContent(uri, "合同文本", "sha256:src");
        ClaimExtractionPort extractor = text -> List.of(Claim.source(UnitId.generate(),
                "正常退租应退还保证金", new EvidenceSpan("src", 0, 5), "deposit_refund_condition"));
        OntologyPort ontologyPort = () -> ONTOLOGY;
        engine = new KnowledgeWorkflowEngine(connector, wikiStore, Optional.of(extractor), ontologyPort,
                ingest, govern, workflows, () -> NOW);
    }

    @Test
    void startProducesAwaitingReviewAndWritesWiki() {
        WorkflowInstance workflow = engine.start("lease.md", SCOPE, "owner-1");

        assertEquals(WorkflowState.AWAITING_REVIEW, workflow.state());
        assertNotNull(workflow.changeRequestId());
        assertEquals(1, wikiStore.pages.size());
    }

    @Test
    void humanApprovalReleasesAndCompilesUnit() {
        WorkflowInstance workflow = engine.start("lease.md", SCOPE, "owner-1");

        WorkflowInstance completed = engine.resume(workflow.id(), HUMAN, true);

        assertEquals(WorkflowState.COMPLETED, completed.state());
        assertNotNull(completed.unitId());
        assertEquals(Origin.DECLARED, unitRepository.findById(UnitId.of(completed.unitId()))
                .orElseThrow().origin());
    }

    @Test
    void systemCannotSelfApprove() {
        WorkflowInstance workflow = engine.start("lease.md", SCOPE, "owner-1");
        assertThrows(IllegalStateException.class, () -> engine.resume(workflow.id(), SYSTEM, true));
    }

    @Test
    void startWithoutExtractorFails() {
        KnowledgeWorkflowEngine noExtractor = new KnowledgeWorkflowEngine(
                uri -> new ConnectorPort.FetchedContent(uri, "文本", "sha256:src"),
                wikiStore, Optional.empty(), () -> ONTOLOGY,
                new IngestContextUseCase(new InMemoryChangeRequestRepository(), () -> NOW),
                new GovernReviewUseCase(new InMemoryChangeRequestRepository(),
                        new ChangeApplier(unitRepository,
                                new LifecycleUseCase(unitRepository, new InMemoryLifecycleEventRepository(), () -> NOW),
                                new InMemoryIndexAdapter(), new FixedEmbedding(), () -> NOW), () -> NOW),
                workflows, () -> NOW);

        WorkflowInstance workflow = noExtractor.start("lease.md", SCOPE, "owner-1");

        assertEquals(WorkflowState.FAILED, workflow.state());
    }

    private static final class FakeWikiStore implements WikiStorePort {
        final Map<String, WikiPage> pages = new HashMap<>();

        @Override
        public WikiPage read(String path) {
            return pages.get(path);
        }

        @Override
        public void write(WikiPage page) {
            pages.put(page.path(), page);
        }

        @Override
        public List<String> listPages() {
            return new ArrayList<>(pages.keySet());
        }

        @Override
        public boolean exists(String path) {
            return pages.containsKey(path);
        }
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