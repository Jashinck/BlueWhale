package org.skylark.bluewhale.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skylark.bluewhale.domain.context.Actor;
import org.skylark.bluewhale.domain.context.ActorType;
import org.skylark.bluewhale.domain.context.ContextUnit;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.context.UnitState;
import org.skylark.bluewhale.domain.governance.ChangeRequest;
import org.skylark.bluewhale.domain.governance.ChangeRequestType;
import org.skylark.bluewhale.domain.governance.KnowledgePatch;
import org.skylark.bluewhale.domain.port.EmbeddingPort;
import org.skylark.bluewhale.infrastructure.index.InMemoryIndexAdapter;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryContextUnitRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryLifecycleEventRepository;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangeApplierTest {

    private static final Instant NOW = Instant.parse("2026-11-12T08:00:00Z");
    private static final Scope SCOPE = Scope.of("acme", "ws-1", null, null, null, null);
    private static final Actor REVIEWER = new Actor("reviewer-1", ActorType.HUMAN);

    private InMemoryContextUnitRepository unitRepository;
    private ChangeApplier applier;

    @BeforeEach
    void setUp() {
        unitRepository = new InMemoryContextUnitRepository();
        InMemoryLifecycleEventRepository events = new InMemoryLifecycleEventRepository();
        LifecycleUseCase lifecycle = new LifecycleUseCase(unitRepository, events, () -> NOW);
        applier = new ChangeApplier(unitRepository, lifecycle, new InMemoryIndexAdapter(),
                new FixedEmbedding(), () -> NOW);
    }

    private UnitId addUnit(String content) {
        KnowledgePatch patch = KnowledgePatch.add(SCOPE, "owner-1", "legal", content, "policy.md");
        return applier.apply(ChangeRequest.draft(ChangeRequestType.ADD, "author-1", patch, NOW), REVIEWER);
    }

    @Test
    void reviseSupersedesOldAndActivatesRevisedWithLineage() {
        UnitId oldId = addUnit("原始规则");
        KnowledgePatch revisePatch = KnowledgePatch.revise(SCOPE, "owner-1", "legal",
                "修订后的规则", "policy.md", oldId);

        UnitId newId = applier.apply(
                ChangeRequest.draft(ChangeRequestType.REVISE, "author-1", revisePatch, NOW), REVIEWER);

        ContextUnit old = unitRepository.findById(oldId).orElseThrow();
        assertEquals(UnitState.SUPERSEDED, old.lifecycle().state());
        assertEquals(newId, old.supersededBy());
        ContextUnit revised = unitRepository.findById(newId).orElseThrow();
        assertEquals(UnitState.ACTIVE, revised.lifecycle().state());
        assertEquals("修订后的规则", revised.content().abstractText());
        assertTrue(revised.lineage().derivedFrom().contains(oldId));
    }

    @Test
    void deprecateArchivesTarget() {
        UnitId oldId = addUnit("待废止规则");
        KnowledgePatch deprecatePatch = new KnowledgePatch(SCOPE, "owner-1", "legal",
                "废止原因", "policy.md", oldId);

        UnitId returned = applier.apply(
                ChangeRequest.draft(ChangeRequestType.DEPRECATE, "author-1", deprecatePatch, NOW), REVIEWER);

        assertEquals(oldId, returned);
        assertEquals(UnitState.ARCHIVED, unitRepository.findById(oldId).orElseThrow().lifecycle().state());
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