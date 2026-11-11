package org.skylark.bluewhale.domain.context;

import java.util.List;

public record Lineage(List<UnitId> derivedFrom, boolean needsReextract) {

    public Lineage {
        derivedFrom = derivedFrom == null ? List.of() : List.copyOf(derivedFrom);
        if (derivedFrom.stream().anyMatch(id -> id == null)) {
            throw new IllegalArgumentException("lineage parents must not contain null");
        }
    }

    public static Lineage empty() {
        return new Lineage(List.of(), false);
    }

    public static Lineage derivedFrom(List<UnitId> parents) {
        return new Lineage(parents, false);
    }

    public boolean hasEvidenceOfDerivation() {
        return !derivedFrom.isEmpty();
    }
}
