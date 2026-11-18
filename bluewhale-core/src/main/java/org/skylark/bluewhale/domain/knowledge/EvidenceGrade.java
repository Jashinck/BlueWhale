package org.skylark.bluewhale.domain.knowledge;

public enum EvidenceGrade {
    SOURCE(true, true),
    DERIVED(true, false),
    EXPERT(false, false),
    RETRO(false, false);

    private final boolean evidenceSpanRequired;
    private final boolean autoRewritable;

    EvidenceGrade(boolean evidenceSpanRequired, boolean autoRewritable) {
        this.evidenceSpanRequired = evidenceSpanRequired;
        this.autoRewritable = autoRewritable;
    }

    public boolean isEvidenceSpanRequired() {
        return evidenceSpanRequired;
    }

    public boolean isAutoRewritable() {
        return autoRewritable;
    }
}