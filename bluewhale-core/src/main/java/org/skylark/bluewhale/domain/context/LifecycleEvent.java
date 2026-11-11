package org.skylark.bluewhale.domain.context;

import java.time.Instant;
import java.util.UUID;

public record LifecycleEvent(
        UUID eventId,
        UnitId unitId,
        UnitState fromState,
        UnitState toState,
        String trigger,
        String actorId,
        ActorType actorType,
        Instant occurredAt,
        String reason,
        UUID batchId
) {

    public LifecycleEvent {
        if (eventId == null || unitId == null || toState == null || occurredAt == null) {
            throw new IllegalArgumentException("lifecycle event requires id, unit, target state and time");
        }
    }
}
