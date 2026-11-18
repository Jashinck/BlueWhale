package org.skylark.bluewhale.domain.knowledge;

public record EvidenceSpan(String sourceRef, int start, int end) {

    public EvidenceSpan {
        if (sourceRef == null || sourceRef.isBlank()) {
            throw new IllegalArgumentException("evidence span requires a source reference");
        }
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("invalid evidence span range");
        }
    }
}