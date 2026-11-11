package org.skylark.bluewhale.domain.context;

public enum UnitState {
    CAPTURED(false),
    EXTRACTED(false),
    QUARANTINED(false),
    SHADOW(false),
    PENDING_REVIEW(false),
    SCHEDULED(false),
    ACTIVE(true),
    STALE_SUSPECT(true),
    CONFLICTED(true),
    SUPERSEDED(false),
    ARCHIVED(false),
    EXPIRED(false),
    ERASED(false);

    private final boolean retrievable;

    UnitState(boolean retrievable) {
        this.retrievable = retrievable;
    }

    public boolean isRetrievable() {
        return retrievable;
    }
}
