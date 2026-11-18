package org.skylark.bluewhale.domain.ontology;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OntologySchemaTest {

    private static OntologySchema schema() {
        return new OntologySchema("1.0", List.of(
                new PropertyDef("deposit_refund_condition", "enum", List.of("normal", "breach"), true),
                new PropertyDef("review_note", "string", List.of(), false)),
                List.of("Contract"), List.of("GOVERNS"));
    }

    @Test
    void rejectsDuplicatePropertyIds() {
        assertThrows(IllegalArgumentException.class, () -> new OntologySchema("1.0", List.of(
                new PropertyDef("p", "string", List.of(), false),
                new PropertyDef("p", "string", List.of(), false)),
                List.of(), List.of()));
    }

    @Test
    void rejectsMissingVersion() {
        assertThrows(IllegalArgumentException.class, () ->
                new OntologySchema(" ", List.of(), List.of(), List.of()));
    }

    @Test
    void closedSetPropertyRejectsUnlistedValue() {
        OntologySchema schema = schema();
        assertTrue(schema.isAllowedValue("deposit_refund_condition", "normal"));
        assertFalse(schema.isAllowedValue("deposit_refund_condition", "unknown"));
        assertFalse(schema.isAllowedValue("missing_property", "normal"));
    }

    @Test
    void openSetPropertyAllowsAnyValue() {
        assertTrue(schema().isAllowedValue("review_note", "anything"));
    }

    @Test
    void requiredBindPropertiesFiltersByFlag() {
        List<PropertyDef> required = schema().requiredBindProperties();
        assertEquals(1, required.size());
        assertEquals("deposit_refund_condition", required.get(0).id());
    }
}