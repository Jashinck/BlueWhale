package org.skylark.bluewhale.domain.feedback;

public enum AttributionCode {
    MISSING,
    NO_HIT,
    WRONG_PICK,
    BAD_CLAIM,
    STALE,
    CONFLICT,
    CONSUMER_ERR;

    public boolean canDraft() {
        return this != CONSUMER_ERR && this != WRONG_PICK;
    }
}