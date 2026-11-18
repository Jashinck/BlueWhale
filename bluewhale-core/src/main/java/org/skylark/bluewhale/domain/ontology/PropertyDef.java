package org.skylark.bluewhale.domain.ontology;

import java.util.List;

public record PropertyDef(String id, String type, List<String> values, boolean bindsRequired) {

    public PropertyDef {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("property id is required");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("property type is required");
        }
        values = values == null ? List.of() : List.copyOf(values);
    }

    public boolean isClosedSet() {
        return !values.isEmpty();
    }

    public boolean allows(String value) {
        return !isClosedSet() || values.contains(value);
    }
}