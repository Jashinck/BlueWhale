package org.skylark.bluewhale.domain.feedback;

import org.skylark.bluewhale.domain.context.UnitId;

import java.time.Instant;
import java.util.Objects;

public record Attribution(
        UnitId id,
        UnitId signalId,
        AttributionCode code,
        UnitId targetUnit,
        String derivedBy,
        Instant at,
        String reviewedBy,
        Instant reviewedAt
) {

    public Attribution {
        Objects.requireNonNull(id, "attribution id is required");
        Objects.requireNonNull(signalId, "attribution signal is required");
        Objects.requireNonNull(code, "attribution code is required");
        Objects.requireNonNull(at, "attribution time is required");
    }

    public static Attribution derive(UnitId signalId, AttributionCode code, UnitId targetUnit, Instant now) {
        return new Attribution(UnitId.generate(), signalId, code, targetUnit, "PROGRAM", now, null, null);
    }

    public Attribution reviewed(String reviewer, Instant now) {
        return new Attribution(id, signalId, code, targetUnit, derivedBy, at, reviewer, now);
    }
}