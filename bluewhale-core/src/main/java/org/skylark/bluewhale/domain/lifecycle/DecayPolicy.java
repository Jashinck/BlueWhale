package org.skylark.bluewhale.domain.lifecycle;

import java.time.Instant;

public record DecayPolicy(double decayFactor) {

    public DecayPolicy {
        if (decayFactor < 0.0) {
            throw new IllegalArgumentException("decay factor must not be negative");
        }
    }

    public static DecayPolicy defaultPolicy() {
        return new DecayPolicy(0.1);
    }

    public double effectiveImportance(double baseImportance, Instant lastSeen, Instant now) {
        if (lastSeen == null) {
            return Math.max(0.0, baseImportance);
        }
        double days = (now.getEpochSecond() - lastSeen.getEpochSecond()) / 86400.0;
        return Math.max(0.0, baseImportance * Math.exp(-decayFactor * days));
    }
}