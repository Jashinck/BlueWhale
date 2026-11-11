package org.skylark.bluewhale.domain.context;

public enum Transition {
    CAPTURE_TO_EXTRACTED(UnitState.CAPTURED, UnitState.EXTRACTED),
    EXTRACTED_TO_QUARANTINED(UnitState.EXTRACTED, UnitState.QUARANTINED),
    EXTRACTED_TO_SHADOW(UnitState.EXTRACTED, UnitState.SHADOW),
    EXTRACTED_TO_PENDING_REVIEW(UnitState.EXTRACTED, UnitState.PENDING_REVIEW),
    EXTRACTED_TO_ACTIVE(UnitState.EXTRACTED, UnitState.ACTIVE),
    QUARANTINED_TO_EXTRACTED(UnitState.QUARANTINED, UnitState.EXTRACTED),
    SHADOW_TO_PENDING_REVIEW(UnitState.SHADOW, UnitState.PENDING_REVIEW),
    PENDING_REVIEW_TO_SCHEDULED(UnitState.PENDING_REVIEW, UnitState.SCHEDULED),
    PENDING_REVIEW_TO_ACTIVE(UnitState.PENDING_REVIEW, UnitState.ACTIVE),
    PENDING_REVIEW_TO_QUARANTINED(UnitState.PENDING_REVIEW, UnitState.QUARANTINED),
    SCHEDULED_TO_ACTIVE(UnitState.SCHEDULED, UnitState.ACTIVE),
    ACTIVE_TO_STALE_SUSPECT(UnitState.ACTIVE, UnitState.STALE_SUSPECT),
    ACTIVE_TO_CONFLICTED(UnitState.ACTIVE, UnitState.CONFLICTED),
    ACTIVE_TO_ARCHIVED(UnitState.ACTIVE, UnitState.ARCHIVED),
    STALE_SUSPECT_TO_ACTIVE(UnitState.STALE_SUSPECT, UnitState.ACTIVE),
    STALE_SUSPECT_TO_EXPIRED(UnitState.STALE_SUSPECT, UnitState.EXPIRED),
    CONFLICTED_TO_ACTIVE(UnitState.CONFLICTED, UnitState.ACTIVE),
    ARCHIVED_TO_EXPIRED(UnitState.ARCHIVED, UnitState.EXPIRED),
    SUPERSEDED_TO_EXPIRED(UnitState.SUPERSEDED, UnitState.EXPIRED),
    EXPIRED_TO_ERASED(UnitState.EXPIRED, UnitState.ERASED),
    EXPIRED_TO_ARCHIVED(UnitState.EXPIRED, UnitState.ARCHIVED);

    private final UnitState from;
    private final UnitState to;

    Transition(UnitState from, UnitState to) {
        this.from = from;
        this.to = to;
    }

    public UnitState from() {
        return from;
    }

    public UnitState to() {
        return to;
    }
}
