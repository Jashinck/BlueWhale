package org.skylark.bluewhale.domain.orchestration;

import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;

import java.time.Instant;
import java.util.Objects;

public record WorkflowInstance(
        UnitId id,
        String sourceUri,
        Scope scope,
        String ownerId,
        WorkflowState state,
        String changeRequestId,
        String unitId,
        Instant createdAt,
        Instant updatedAt
) {

    public WorkflowInstance {
        Objects.requireNonNull(id, "workflow id is required");
        Objects.requireNonNull(state, "workflow state is required");
        Objects.requireNonNull(scope, "workflow scope is required");
        if (sourceUri == null || sourceUri.isBlank()) {
            throw new IllegalArgumentException("workflow source uri is required");
        }
    }

    public static WorkflowInstance start(String sourceUri, Scope scope, String ownerId, Instant now) {
        return new WorkflowInstance(UnitId.generate(), sourceUri, scope, ownerId,
                WorkflowState.RUNNING, null, null, now, now);
    }

    public WorkflowInstance awaitingReview(String crId, Instant now) {
        return new WorkflowInstance(id, sourceUri, scope, ownerId,
                WorkflowState.AWAITING_REVIEW, crId, null, createdAt, now);
    }

    public WorkflowInstance completed(String appliedUnitId, Instant now) {
        return new WorkflowInstance(id, sourceUri, scope, ownerId,
                WorkflowState.COMPLETED, changeRequestId, appliedUnitId, createdAt, now);
    }

    public WorkflowInstance failed(Instant now) {
        return new WorkflowInstance(id, sourceUri, scope, ownerId,
                WorkflowState.FAILED, changeRequestId, unitId, createdAt, now);
    }
}