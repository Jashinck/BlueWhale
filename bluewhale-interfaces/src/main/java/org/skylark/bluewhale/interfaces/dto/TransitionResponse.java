package org.skylark.bluewhale.interfaces.dto;

import org.skylark.bluewhale.domain.context.TransitionResult;
import org.skylark.bluewhale.domain.context.UnitState;

public record TransitionResponse(
        String unitId,
        UnitState state,
        LifecycleEventResponse event
) {

    public static TransitionResponse from(TransitionResult result) {
        return new TransitionResponse(result.unit().id().toString(),
                result.unit().lifecycle().state(), LifecycleEventResponse.from(result.event()));
    }
}
