package org.skylark.bluewhale.infrastructure;

import org.junit.jupiter.api.Test;
import org.skylark.bluewhale.domain.ontology.OntologySchema;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlOntologyAdapterTest {

    private static final String YAML = """
            ontologyVersion: "1.0"
            properties:
              - id: deposit_refund_condition
                type: enum
                values: [normal, breach, mutual]
                bindsRequired: true
            entityTypes: [Contract, Clause]
            relationTypes: [GOVERNS]
            """;

    @Test
    void parsesYamlIntoSchema() {
        OntologySchema schema = YamlOntologyAdapter.parse(
                new ByteArrayInputStream(YAML.getBytes(StandardCharsets.UTF_8)));
        assertEquals("1.0", schema.version());
        assertTrue(schema.hasProperty("deposit_refund_condition"));
        assertTrue(schema.isAllowedValue("deposit_refund_condition", "breach"));
        assertEquals(1, schema.requiredBindProperties().size());
    }

    @Test
    void loadsDefaultResource() {
        OntologySchema schema = new YamlOntologyAdapter("ontology/default.yaml").load();
        assertTrue(schema.hasProperty("deposit_refund_condition"));
        assertTrue(schema.hasProperty("contract_amount_threshold"));
    }

    @Test
    void missingResourceFailsFast() {
        assertThrows(IllegalStateException.class, () -> new YamlOntologyAdapter("ontology/absent.yaml"));
    }

    @Test
    void duplicatePropertyFailsFast() {
        String bad = """
                ontologyVersion: "1.0"
                properties:
                  - id: deposit_refund_condition
                    type: enum
                    values: [normal]
                    bindsRequired: true
                  - id: deposit_refund_condition
                    type: string
                """;
        assertThrows(IllegalArgumentException.class, () -> YamlOntologyAdapter.parse(
                new ByteArrayInputStream(bad.getBytes(StandardCharsets.UTF_8))));
    }
}