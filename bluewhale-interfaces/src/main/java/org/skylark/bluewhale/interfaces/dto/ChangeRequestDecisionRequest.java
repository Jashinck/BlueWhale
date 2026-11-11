package org.skylark.bluewhale.interfaces.dto;

public record ChangeRequestDecisionRequest(
        String action,
        String actorId,
        String actorType,
        String regressionRunId
) {
}
