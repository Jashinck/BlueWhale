package org.skylark.bluewhale.domain.context;

public record Trust(double confidence, int authority) {

    public Trust {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0 and 1");
        }
        if (authority < 1 || authority > 5) {
            throw new IllegalArgumentException("authority must be between 1 and 5");
        }
    }

    public static Trust observed(double confidence) {
        return new Trust(confidence, 3);
    }
}
