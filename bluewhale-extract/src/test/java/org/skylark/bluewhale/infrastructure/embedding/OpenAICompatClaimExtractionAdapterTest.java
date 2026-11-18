package org.skylark.bluewhale.infrastructure.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skylark.bluewhale.domain.knowledge.Claim;
import org.skylark.bluewhale.domain.knowledge.EvidenceGrade;
import org.skylark.bluewhale.domain.ontology.OntologySchema;
import org.skylark.bluewhale.domain.ontology.PropertyDef;
import org.skylark.bluewhale.domain.port.OntologyPort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAICompatClaimExtractionAdapterTest {

    private static final String SOURCE = "正常退租应退还保证金。违约时不予退还。";
    private static final OntologySchema ONTOLOGY = new OntologySchema("1.0",
            List.of(new PropertyDef("deposit_refund_condition", "enum", List.of("normal"), true)),
            List.of(), List.of());

    private static OpenAICompatClaimExtractionAdapter adapter(String apiKey) {
        OntologyPort ontologyPort = () -> ONTOLOGY;
        return new OpenAICompatClaimExtractionAdapter("http://localhost", apiKey, "test-model",
                new ObjectMapper(), ontologyPort);
    }

    @Test
    void locatableQuoteBecomesSourceClaimWithSpan() {
        String json = "[{\"text\":\"正常退租退还保证金\",\"quote\":\"正常退租应退还保证金\",\"bindsProperty\":\"deposit_refund_condition\"}]";
        List<Claim> claims = adapter("").parseClaims(json, SOURCE);
        assertEquals(1, claims.size());
        Claim claim = claims.get(0);
        assertEquals(EvidenceGrade.SOURCE, claim.grade());
        assertEquals("deposit_refund_condition", claim.bindsProperty());
        assertEquals(0, claim.evidenceSpan().start());
        assertEquals("正常退租应退还保证金".length(), claim.evidenceSpan().end());
    }

    @Test
    void nonLocatableQuoteIsSkipped() {
        String json = "[{\"text\":\"某断言\",\"quote\":\"这段不在原文里\",\"bindsProperty\":null}]";
        assertTrue(adapter("").parseClaims(json, SOURCE).isEmpty());
    }

    @Test
    void markdownFencesAreStripped() {
        String json = "```json\n[{\"text\":\"违约不退\",\"quote\":\"违约时不予退还\",\"bindsProperty\":\"null\"}]\n```";
        List<Claim> claims = adapter("").parseClaims(json, SOURCE);
        assertEquals(1, claims.size());
        assertNull(claims.get(0).bindsProperty());
    }

    @Test
    void noApiKeyReturnsNoClaims() {
        assertTrue(adapter("").extractClaims(SOURCE).isEmpty());
    }
}