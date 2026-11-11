package org.skylark.bluewhale.application;

import org.skylark.bluewhale.domain.context.Actor;
import org.skylark.bluewhale.domain.context.UnitId;

public record ChangeRequestDecision(
        UnitId crId,
        Action action,
        Actor actor,
        UnitId regressionRunId
) {

    public ChangeRequestDecision {
        if (crId == null || action == null || actor == null) {
            throw new IllegalArgumentException("decision requires change request, action and actor");
        }
    }

    public enum Action {
        SUBMIT,
        APPROVE,
        REGRESS,
        READY,
        RELEASE,
        REJECT,
        ROLLBACK
    }
}
