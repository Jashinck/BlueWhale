package org.skylark.bluewhale.domain.governance;

import org.skylark.bluewhale.domain.context.UnitId;

import java.util.Objects;

public record RegressionCase(
        UnitId id,
        String query,
        UnitId expectedUnitId,
        boolean expectEmpty,
        UnitId originCrId
) {

    public RegressionCase {
        Objects.requireNonNull(id, "regression case id is required");
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("regression case query is required");
        }
        if (!expectEmpty && expectedUnitId == null) {
            throw new IllegalArgumentException("non-empty regression case requires an expected unit");
        }
    }
}