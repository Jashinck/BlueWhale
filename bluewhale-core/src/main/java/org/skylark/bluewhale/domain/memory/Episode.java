package org.skylark.bluewhale.domain.memory;

import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;

import java.time.Instant;

public record Episode(
        UnitId id,
        Scope scope,
        Instant occurredAt,
        String source,
        String payload
) {

    public Episode {
        if (id == null || scope == null || occurredAt == null) {
            throw new IllegalArgumentException("episode requires id, scope and time");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("episode source is required");
        }
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("episode payload is required");
        }
    }

    public static Episode capture(Scope scope, String source, String payload, Instant now) {
        return new Episode(UnitId.generate(), scope, now, source, payload);
    }
}
