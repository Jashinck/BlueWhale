package org.skylark.bluewhale.application;

import org.skylark.bluewhale.domain.context.UnitState;

public record CaptureResult(
        String traceId,
        String episodeId,
        String unitId,
        UnitState state,
        boolean indexed
) {
}
