package org.skylark.bluewhale.domain.graph;

import org.skylark.bluewhale.domain.context.UnitId;

import java.time.Instant;
import java.util.Objects;

public record UnitEdge(
        UnitId from,
        UnitId to,
        EdgeType type,
        Instant validFrom,
        Instant validTo,
        double weight
) {

    public UnitEdge {
        Objects.requireNonNull(from, "edge source is required");
        Objects.requireNonNull(to, "edge target is required");
        Objects.requireNonNull(type, "edge type is required");
        Objects.requireNonNull(validFrom, "edge validFrom is required");
        if (weight < 0.0 || weight > 1.0) {
            throw new IllegalArgumentException("edge weight must be between 0 and 1");
        }
        if (validTo != null && validTo.isBefore(validFrom)) {
            throw new IllegalArgumentException("edge validTo must not be before validFrom");
        }
    }

    public static UnitEdge similar(UnitId from, UnitId to, double weight, Instant now) {
        return new UnitEdge(from, to, EdgeType.SIMILAR_TO, now, null, weight);
    }

    public static UnitEdge derivedFrom(UnitId child, UnitId parent, Instant now) {
        return new UnitEdge(child, parent, EdgeType.DERIVED_FROM, now, null, 1.0);
    }

    public boolean isEffectiveAt(Instant now) {
        return !validFrom.isAfter(now) && (validTo == null || validTo.isAfter(now));
    }

    public UnitEdge expiredAt(Instant realityEnd) {
        return new UnitEdge(from, to, type, validFrom, realityEnd, weight);
    }
}