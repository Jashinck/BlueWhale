package org.skylark.bluewhale.domain.feedback;

import org.skylark.bluewhale.domain.context.UnitId;

import java.time.Instant;
import java.util.Objects;

public record Signal(
        UnitId id,
        String traceId,
        SignalKind kind,
        UnitId unitId,
        UnitId expectedUnitId,
        String severity,
        String reason,
        String reporter,
        Instant at
) {

    public Signal {
        Objects.requireNonNull(id, "signal id is required");
        Objects.requireNonNull(kind, "signal kind is required");
        Objects.requireNonNull(at, "signal time is required");
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("signal trace id is required");
        }
    }

    public static Signal report(String traceId, SignalKind kind, UnitId unitId, UnitId expectedUnitId,
                                String severity, String reason, String reporter, Instant now) {
        return new Signal(UnitId.generate(), traceId, kind, unitId, expectedUnitId, severity, reason, reporter, now);
    }
}