package org.skylark.bluewhale.domain.knowledge;

import org.junit.jupiter.api.Test;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.ontology.OntologySchema;
import org.skylark.bluewhale.domain.ontology.PropertyDef;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeValidationGateTest {

    private static final Instant NOW = Instant.parse("2026-11-12T08:00:00Z");
    private static final OntologySchema ONTOLOGY = new OntologySchema("1.0",
            List.of(new PropertyDef("deposit_refund_condition", "enum", List.of("normal", "breach"), true)),
            List.of(), List.of());
    private static final KnowledgeValidationGate GATE = new KnowledgeValidationGate(ONTOLOGY);

    private static WikiPage page(String bindsProperty) {
        return new WikiPage("rules/deposit.md",
                new WikiFrontmatter(bindsProperty, EvidenceGrade.SOURCE, NOW, null, "v1", null, null),
                "# 保证金", "hash");
    }

    @Test
    void pageWithoutBindsPropertyIsRejected() {
        assertEquals(ValidationOutcome.REJECT, GATE.validatePage(page(null)).outcome());
    }

    @Test
    void pageWithUndefinedPropertyIsRejected() {
        assertEquals(ValidationOutcome.REJECT, GATE.validatePage(page("unknown_property")).outcome());
    }

    @Test
    void pageWithDefinedPropertyIsAccepted() {
        assertTrue(GATE.validatePage(page("deposit_refund_condition")).isAccepted());
    }

    @Test
    void claimWithUndefinedPropertyIsRejected() {
        Claim claim = Claim.source(UnitId.generate(), "阈值", new EvidenceSpan("policy.md", 0, 2), "unknown");
        assertEquals(ValidationOutcome.REJECT, GATE.validateClaim(claim, "阈值文本").outcome());
    }

    @Test
    void sourceClaimWithUnlocatableSpanIsDowngraded() {
        Claim claim = Claim.source(UnitId.generate(), "阈值", new EvidenceSpan("policy.md", 0, 999), "deposit_refund_condition");
        assertEquals(ValidationOutcome.DOWNGRADE, GATE.validateClaim(claim, "短文本").outcome());
    }

    @Test
    void validSourceClaimIsAccepted() {
        Claim claim = Claim.source(UnitId.generate(), "阈值", new EvidenceSpan("policy.md", 0, 2), "deposit_refund_condition");
        assertTrue(GATE.validateClaim(claim, "阈值文本").isAccepted());
    }

    @Test
    void samePropertyDifferentTextIsConflict() {
        Claim a = Claim.source(UnitId.generate(), "口径A", new EvidenceSpan("s", 0, 1), "deposit_refund_condition");
        Claim b = Claim.source(UnitId.generate(), "口径B", new EvidenceSpan("s", 0, 1), "deposit_refund_condition");
        Optional<String> conflict = GATE.detectPropertyConflict(List.of(a, b));
        assertTrue(conflict.isPresent());
        assertEquals("deposit_refund_condition", conflict.get());
    }

    @Test
    void samePropertySameTextIsNotConflict() {
        Claim a = Claim.source(UnitId.generate(), "口径A", new EvidenceSpan("s", 0, 1), "deposit_refund_condition");
        Claim b = Claim.source(UnitId.generate(), "口径A", new EvidenceSpan("s", 0, 1), "deposit_refund_condition");
        assertTrue(GATE.detectPropertyConflict(List.of(a, b)).isEmpty());
    }
}