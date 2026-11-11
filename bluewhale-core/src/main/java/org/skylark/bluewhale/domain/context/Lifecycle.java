package org.skylark.bluewhale.domain.context;

import java.time.Instant;

public record Lifecycle(
        UnitState state,
        Instant validFrom,
        Instant validTo,
        Instant learnedAt,
        Instant supersededAt,
        Instant sourcePublishedAt,
        Instant capturedAt,
        Instant reviewDueAt,
        Instant retentionUntil,
        Instant erasedAt
) {

    public Lifecycle {
        if (state == null) {
            throw new IllegalArgumentException("state is required");
        }
        if (validFrom != null && validTo != null && validTo.isBefore(validFrom)) {
            throw new IllegalArgumentException("validTo must not be before validFrom");
        }
    }

    public static Lifecycle captured(Instant now) {
        return new Lifecycle(UnitState.CAPTURED, now, null, now, null,
                null, now, null, null, null);
    }

    public Lifecycle withState(UnitState next, Instant now) {
        Instant erasedAt = next == UnitState.ERASED ? now : this.erasedAt;
        return new Lifecycle(next, validFrom, validTo, learnedAt, supersededAt,
                sourcePublishedAt, capturedAt, reviewDueAt, retentionUntil, erasedAt);
    }

    public Lifecycle superseded(Instant realityEnd, Instant systemTime) {
        Instant nextValidTo = validTo == null ? realityEnd : validTo;
        return new Lifecycle(UnitState.SUPERSEDED, validFrom, nextValidTo, learnedAt,
                systemTime, sourcePublishedAt, capturedAt, reviewDueAt, retentionUntil, erasedAt);
    }

    public boolean isEffectiveAt(Instant now) {
        return (validFrom == null || !validFrom.isAfter(now))
                && (validTo == null || validTo.isAfter(now));
    }
}
