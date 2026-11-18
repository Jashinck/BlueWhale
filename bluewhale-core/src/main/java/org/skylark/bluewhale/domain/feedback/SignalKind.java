package org.skylark.bluewhale.domain.feedback;

public enum SignalKind {
    EXPLICIT_DOWN,
    EDIT,
    REJECT,
    ADOPT,
    OVERRIDE,
    IGNORE,
    EVAL_FAIL,
    SOURCE_CHANGED,
    REVIEW_DUE,
    CONFLICT;

    public boolean isImplicit() {
        return this == ADOPT || this == OVERRIDE || this == IGNORE;
    }
}