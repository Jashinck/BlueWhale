package org.skylark.bluewhale.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skylark.bluewhale.domain.context.ContextUnit;
import org.skylark.bluewhale.domain.context.Origin;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.context.UnitKind;
import org.skylark.bluewhale.domain.context.UnitState;
import org.skylark.bluewhale.domain.knowledge.EvidenceGrade;
import org.skylark.bluewhale.domain.knowledge.WikiFrontmatter;
import org.skylark.bluewhale.domain.knowledge.WikiPage;
import org.skylark.bluewhale.domain.ontology.OntologySchema;
import org.skylark.bluewhale.domain.ontology.PropertyDef;
import org.skylark.bluewhale.domain.port.EmbeddingPort;
import org.skylark.bluewhale.domain.port.OntologyPort;
import org.skylark.bluewhale.infrastructure.index.InMemoryIndexAdapter;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryContextUnitRepository;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompileWikiUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-12T08:00:00Z");
    private static final Scope SCOPE = Scope.agent("acme", "agent-1");
    private static final OntologySchema ONTOLOGY = new OntologySchema("1.0",
            List.of(new PropertyDef("deposit_refund_condition", "enum", List.of("normal", "breach"), true)),
            List.of(), List.of());

    private InMemoryContextUnitRepository unitRepository;
    private InMemoryIndexAdapter index;
    private CompileWikiUseCase compile;

    @BeforeEach
    void setUp() {
        unitRepository = new InMemoryContextUnitRepository();
        index = new InMemoryIndexAdapter();
        OntologyPort ontologyPort = () -> ONTOLOGY;
        compile = new CompileWikiUseCase(unitRepository, index, new FixedEmbedding(), ontologyPort, () -> NOW);
    }

    private static WikiPage page(String bindsProperty) {
        return new WikiPage("rules/deposit.md",
                new WikiFrontmatter(bindsProperty, EvidenceGrade.SOURCE, NOW, null, "v1", null, null),
                "# 保证金退还规则\n正常退租应退还保证金。", "sha256:abc");
    }

    @Test
    void compilesWikiPageToDeclaredActiveUnit() {
        CompileResult result = compile.compile(page("deposit_refund_condition"), SCOPE, "owner-1");

        ContextUnit unit = unitRepository.findById(UnitId.of(result.unitId())).orElseThrow();
        assertEquals(UnitKind.SEMANTIC, unit.kind());
        assertEquals(Origin.DECLARED, unit.origin());
        assertEquals(UnitState.ACTIVE, unit.lifecycle().state());
        assertTrue(unit.provenance().sourceRef().contains("sha256:abc"));
        assertEquals("sha256:abc", result.sourceHash());
        assertEquals(1, index.dense(SCOPE, new float[]{1, 0, 0}, 5).size());
    }

    @Test
    void rejectsPageWithoutBindsProperty() {
        assertThrows(IllegalStateException.class, () -> compile.compile(page(null), SCOPE, "owner-1"));
    }

    @Test
    void rejectsCompileWithoutOwner() {
        assertThrows(IllegalArgumentException.class,
                () -> compile.compile(page("deposit_refund_condition"), SCOPE, " "));
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