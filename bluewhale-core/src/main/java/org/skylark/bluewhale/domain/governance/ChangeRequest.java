package org.skylark.bluewhale.domain.governance;

import org.skylark.bluewhale.domain.context.Actor;
import org.skylark.bluewhale.domain.context.ActorType;
import org.skylark.bluewhale.domain.context.UnitId;

import java.time.Instant;
import java.util.Objects;

public final class ChangeRequest {

    private final UnitId id;
    private final ChangeRequestType type;
    private final ChangeRequestStatus status;
    private final String author;
    private final KnowledgePatch patch;
    private final UnitId regressionRunId;
    private final UnitId rollbackOf;
    private final Instant createdAt;
    private final Instant updatedAt;

    public ChangeRequest(UnitId id, ChangeRequestType type, ChangeRequestStatus status,
                         String author, KnowledgePatch patch, UnitId regressionRunId,
                         UnitId rollbackOf, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "change request id is required");
        this.type = Objects.requireNonNull(type, "change request type is required");
        this.status = Objects.requireNonNull(status, "change request status is required");
        this.author = requireText(author, "change request author is required");
        this.patch = Objects.requireNonNull(patch, "change request patch is required");
        this.regressionRunId = regressionRunId;
        this.rollbackOf = rollbackOf;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    public static ChangeRequest draft(ChangeRequestType type, String author,
                                      KnowledgePatch patch, Instant now) {
        return new ChangeRequest(UnitId.generate(), type, ChangeRequestStatus.DRAFT,
                author, patch, null, null, now, now);
    }

    public ChangeRequest submitForReview(Actor actor, Instant now) {
        requireStatus(ChangeRequestStatus.DRAFT);
        return copy(ChangeRequestStatus.IN_REVIEW, actor, now);
    }

    public ChangeRequest approve(Actor actor, Instant now) {
        requireStatus(ChangeRequestStatus.IN_REVIEW);
        if ("SYSTEM".equals(author) && actor.type() == ActorType.SYSTEM) {
            throw new IllegalStateException("system-authored drafts cannot be self-approved");
        }
        return copy(ChangeRequestStatus.APPROVED, actor, now);
    }

    public ChangeRequest startRegression(Actor actor, Instant now) {
        requireStatus(ChangeRequestStatus.APPROVED);
        return copy(ChangeRequestStatus.REGRESSING, actor, now);
    }

    public ChangeRequest markReady(Actor actor, Instant now, UnitId regressionRunId) {
        requireStatus(ChangeRequestStatus.REGRESSING);
        if (requiresRegressionEvidence() && regressionRunId == null) {
            throw new IllegalStateException("this change requires a regression run before READY");
        }
        return new ChangeRequest(id, type, ChangeRequestStatus.READY, author, patch,
                regressionRunId, rollbackOf, createdAt, now);
    }

    public ChangeRequest release(Actor actor, Instant now) {
        requireStatus(ChangeRequestStatus.READY);
        return copy(ChangeRequestStatus.RELEASED, actor, now);
    }

    public ChangeRequest reject(Actor actor, Instant now) {
        requireStatus(ChangeRequestStatus.IN_REVIEW);
        return copy(ChangeRequestStatus.REJECTED, actor, now);
    }

    public ChangeRequest rollback(Actor actor, Instant now) {
        requireStatus(ChangeRequestStatus.RELEASED);
        return copy(ChangeRequestStatus.ROLLED_BACK, actor, now);
    }

    public boolean isReleased() {
        return status == ChangeRequestStatus.RELEASED;
    }

    private boolean requiresRegressionEvidence() {
        return type == ChangeRequestType.REVISE
                || type == ChangeRequestType.DEPRECATE
                || type == ChangeRequestType.ONTOLOGY
                || type == ChangeRequestType.RECLASS;
    }

    private ChangeRequest copy(ChangeRequestStatus next, Actor actor, Instant now) {
        return new ChangeRequest(id, type, next, author, patch, regressionRunId,
                rollbackOf, createdAt, now);
    }

    private void requireStatus(ChangeRequestStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("change request must be " + expected
                    + " but was " + status);
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public UnitId id() {
        return id;
    }

    public ChangeRequestType type() {
        return type;
    }

    public ChangeRequestStatus status() {
        return status;
    }

    public String author() {
        return author;
    }

    public KnowledgePatch patch() {
        return patch;
    }

    public UnitId regressionRunId() {
        return regressionRunId;
    }

    public UnitId rollbackOf() {
        return rollbackOf;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
