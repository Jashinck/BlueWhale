package org.skylark.bluewhale.domain.lifecycle;

import org.skylark.bluewhale.domain.context.UnitKind;

public record RetentionPolicy(double forgettingThreshold) {

    public RetentionPolicy {
        if (forgettingThreshold < 0.0 || forgettingThreshold > 1.0) {
            throw new IllegalArgumentException("forgetting threshold must be between 0 and 1");
        }
    }

    public static RetentionPolicy defaultPolicy() {
        return new RetentionPolicy(0.05);
    }

    public boolean isExempt(UnitKind kind) {
        return kind == UnitKind.PROCEDURAL;
    }

    public boolean shouldRetire(UnitKind kind, double effectiveImportance) {
        return !isExempt(kind) && effectiveImportance < forgettingThreshold;
    }
}