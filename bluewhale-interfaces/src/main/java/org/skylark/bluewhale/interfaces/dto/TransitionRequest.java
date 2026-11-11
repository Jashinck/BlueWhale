package org.skylark.bluewhale.interfaces.dto;

import org.skylark.bluewhale.domain.context.ActorType;
import org.skylark.bluewhale.domain.context.Transition;

import java.util.UUID;

public record TransitionRequest(
        Transition transition,
        String actorId,
        ActorType actorType,
        String reason,
        UUID batchId
) {
}
