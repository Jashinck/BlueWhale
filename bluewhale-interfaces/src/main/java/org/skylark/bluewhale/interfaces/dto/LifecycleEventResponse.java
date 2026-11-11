package org.skylark.bluewhale.interfaces.dto;

import org.skylark.bluewhale.domain.context.ActorType;
import org.skylark.bluewhale.domain.context.LifecycleEvent;
import org.skylark.bluewhale.domain.context.UnitState;

import java.time.Instant;
import java.util.UUID;

public record LifecycleEventResponse(
        UUID eventId,
        String unitId,
        UnitState fromState,
        UnitState toState,
        String trigger,
        String actorId,
        ActorType actorType,
        Instant occurredAt,
        String reason,
        UUID batchId
) {

    public static LifecycleEventResponse from(LifecycleEvent event) {
        return new LifecycleEventResponse(event.eventId(), event.unitId().toString(),
                event.fromState(), event.toState(), event.trigger(), event.actorId(),
                event.actorType(), event.occurredAt(), event.reason(), event.batchId());
    }
}
