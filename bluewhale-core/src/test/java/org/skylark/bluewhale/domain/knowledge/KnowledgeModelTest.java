package org.skylark.bluewhale.domain.knowledge;

import org.junit.jupiter.api.Test;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeModelTest {

    private static final Instant NOW = Instant.parse("2026-11-12T08:00:00Z");
    private static final Scope SCOPE = Scope.agent("acme", "agent-1");

    @Test
    void sourceClaimRequiresEvidenceSpan() {
        assertThrows(IllegalArgumentException.class, () ->
                new Claim(UnitId.generate(), "阈值 10 万", EvidenceGrade.SOURCE, null, "p", null, null));
    }

    @Test
    void expertClaimRequiresAssertedBy() {
        assertThrows(IllegalArgumentException.class, () ->
                new Claim(UnitId.generate(), "专家口径", EvidenceGrade.EXPERT, null, "p", null, null));
    }

    @Test
    void derivedClaimRequiresSourceClaim() {
        EvidenceSpan span = new EvidenceSpan("policy.md", 0, 10);
        assertThrows(IllegalArgumentException.class, () ->
                new Claim(UnitId.generate(), "推导", EvidenceGrade.DERIVED, span, "p", null, null));
    }

    @Test
    void sourceClaimFactoryIsValid() {
        Claim claim = Claim.source(UnitId.generate(), "阈值 10 万", new EvidenceSpan("policy.md", 0, 10), "p");
        assertEquals(EvidenceGrade.SOURCE, claim.grade());
        assertTrue(claim.grade().isAutoRewritable());
    }

    @Test
    void expertGradeIsNotAutoRewritable() {
        Claim claim = Claim.expert(UnitId.generate(), "专家口径", "expert-1", "p");
        assertFalse(claim.grade().isAutoRewritable());
    }

    @Test
    void evidenceSpanRejectsInvalidRange() {
        assertThrows(IllegalArgumentException.class, () -> new EvidenceSpan("policy.md", 10, 5));
        assertThrows(IllegalArgumentException.class, () -> new EvidenceSpan(" ", 0, 5));
    }

    @Test
    void sourceRequiresContentHash() {
        assertThrows(IllegalArgumentException.class, () ->
                new Source(UnitId.generate(), SCOPE, "policy.md", SourceKind.DOCUMENT, " ", NOW));
    }

    @Test
    void wikiPatchRequiresDiff() {
        assertThrows(IllegalArgumentException.class, () ->
                new WikiPatch(WikiPatchType.MODIFY, "rules/deposit.md", " ", "hash", List.of()));
    }

    @Test
    void wikiPageRequiresSourceHash() {
        WikiFrontmatter frontmatter = new WikiFrontmatter("p", EvidenceGrade.SOURCE, NOW, null, "v1", null, null);
        assertThrows(IllegalArgumentException.class, () ->
                new WikiPage("rules/deposit.md", frontmatter, "# 保证金", " "));
    }
}