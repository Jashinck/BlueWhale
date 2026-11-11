package org.skylark.bluewhale.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skylark.bluewhale.domain.context.Actor;
import org.skylark.bluewhale.domain.context.ActorType;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.governance.ChangeRequestStatus;
import org.skylark.bluewhale.domain.port.EmbeddingPort;
import org.skylark.bluewhale.infrastructure.index.InMemoryIndexAdapter;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryChangeRequestRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryContextUnitRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryLifecycleEventRepository;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KnowledgeClosedLoopUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-10T08:00:00Z");
    private static final Actor REVIEWER = new Actor("reviewer-1", ActorType.HUMAN);

    private InMemoryContextUnitRepository unitRepository;
    private InMemoryChangeRequestRepository changeRequestRepository;
    private IngestContextUseCase ingest;
    private GovernReviewUseCase govern;

    @BeforeEach
    void setUp() {
        unitRepository = new InMemoryContextUnitRepository();
        changeRequestRepository = new InMemoryChangeRequestRepository();
        InMemoryLifecycleEventRepository events = new InMemoryLifecycleEventRepository();
        LifecycleUseCase lifecycle = new LifecycleUseCase(unitRepository, events, () -> NOW);
        EmbeddingPort embedding = new FixedEmbedding();
        InMemoryIndexAdapter index = new InMemoryIndexAdapter();
        ChangeApplier applier = new ChangeApplier(unitRepository, lifecycle, index, embedding, () -> NOW);
        ingest = new IngestContextUseCase(changeRequestRepository, () -> NOW);
        govern = new GovernReviewUseCase(changeRequestRepository, applier, () -> NOW);
    }

    @Test
    void shouldReleaseKnowledgeAndMakeItRetrievable() {
        Scope scope = Scope.of("acme", "ws-1", null, null, null, null);
        IngestResult submitted = ingest.submit(new IngestCommand("trace-1", "author-1", scope,
                "owner-1", "legal", "合同金额超过 10 万需特批", "policy.md"));
        assertEquals(ChangeRequestStatus.DRAFT, submitted.status());

        UnitId crId = UnitId.of(submitted.crId());
        govern.decide(new ChangeRequestDecision(crId,
                ChangeRequestDecision.Action.SUBMIT, REVIEWER, null));
        govern.decide(new ChangeRequestDecision(crId,
                ChangeRequestDecision.Action.APPROVE, REVIEWER, null));
        govern.decide(new ChangeRequestDecision(crId,
                ChangeRequestDecision.Action.REGRESS, REVIEWER, null));
        govern.decide(new ChangeRequestDecision(crId,
                ChangeRequestDecision.Action.READY, REVIEWER, null));
        GovernResult released = govern.decide(new ChangeRequestDecision(crId,
                ChangeRequestDecision.Action.RELEASE, REVIEWER, null));

        assertEquals(ChangeRequestStatus.RELEASED, released.status());
        assertNotNull(released.appliedUnitId());
        assertEquals("合同金额超过 10 万需特批",
                unitRepository.findById(UnitId.of(released.appliedUnitId()))
                        .orElseThrow().content().abstractText());
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
