package org.skylark.bluewhale.domain.ontology;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record OntologySchema(
        String version,
        List<PropertyDef> properties,
        List<String> entityTypes,
        List<String> relationTypes
) {

    public OntologySchema {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("ontology version is required");
        }
        properties = properties == null ? List.of() : List.copyOf(properties);
        entityTypes = entityTypes == null ? List.of() : List.copyOf(entityTypes);
        relationTypes = relationTypes == null ? List.of() : List.copyOf(relationTypes);
        Set<String> seen = new HashSet<>();
        for (PropertyDef property : properties) {
            if (!seen.add(property.id())) {
                throw new IllegalArgumentException("duplicate ontology property: " + property.id());
            }
        }
    }

    public boolean hasProperty(String id) {
        return property(id).isPresent();
    }

    public Optional<PropertyDef> property(String id) {
        return properties.stream().filter(property -> property.id().equals(id)).findFirst();
    }

    public boolean isAllowedValue(String propertyId, String value) {
        return property(propertyId).map(def -> def.allows(value)).orElse(false);
    }

    public List<PropertyDef> requiredBindProperties() {
        return properties.stream().filter(PropertyDef::bindsRequired).toList();
    }
}