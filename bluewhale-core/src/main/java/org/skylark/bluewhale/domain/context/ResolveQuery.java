package org.skylark.bluewhale.domain.context;

import java.util.Set;

public record ResolveQuery(
        String traceId,
        Scope scope,
        String intent,
        String query,
        BudgetSpec budget,
        Set<RepresentationLine> lines,
        RepresentationDepth maxDepth
) {

    public ResolveQuery {
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("trace id is required");
        }
        if (scope == null) {
            throw new IllegalArgumentException("scope is required");
        }
        budget = budget == null ? BudgetSpec.defaults() : budget;
        lines = lines == null || lines.isEmpty() ? Set.of(RepresentationLine.values()) : Set.copyOf(lines);
        maxDepth = maxDepth == null ? RepresentationDepth.OVERVIEW : maxDepth;
    }
}
