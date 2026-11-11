package org.skylark.bluewhale.domain.feedback;

import org.skylark.bluewhale.domain.context.Outcome;
import org.skylark.bluewhale.domain.context.UnitId;

import java.time.Instant;

public record UsageEvent(
        UnitId unitId,
        String traceId,
        Outcome outcome,
        int tokens,
        Instant at
) {

    public UsageEvent {
        if (unitId == null || traceId == null || traceId.isBlank()
                || outcome == null || at == null) {
            throw new IllegalArgumentException("usage event requires unit, trace, outcome and time");
        }
        if (tokens < 0) {
            throw new IllegalArgumentException("usage tokens must not be negative");
        }
    }
}
