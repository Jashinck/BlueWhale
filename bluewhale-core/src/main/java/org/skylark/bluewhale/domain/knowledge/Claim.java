package org.skylark.bluewhale.domain.knowledge;

import org.skylark.bluewhale.domain.context.UnitId;

import java.util.Objects;

public record Claim(
        UnitId id,
        String text,
        EvidenceGrade grade,
        EvidenceSpan evidenceSpan,
        String bindsProperty,
        String assertedBy,
        UnitId derivedFrom
) {

    public Claim {
        Objects.requireNonNull(id, "claim id is required");
        Objects.requireNonNull(grade, "claim grade is required");
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("claim text is required");
        }
        if (grade.isEvidenceSpanRequired() && evidenceSpan == null) {
            throw new IllegalArgumentException("grade " + grade + " requires an evidence span");
        }
        if ((grade == EvidenceGrade.EXPERT || grade == EvidenceGrade.RETRO)
                && (assertedBy == null || assertedBy.isBlank())) {
            throw new IllegalArgumentException("grade " + grade + " requires an assertedBy");
        }
        if (grade == EvidenceGrade.DERIVED && derivedFrom == null) {
            throw new IllegalArgumentException("derived claim must reference its source claim");
        }
    }

    public static Claim source(UnitId id, String text, EvidenceSpan span, String bindsProperty) {
        return new Claim(id, text, EvidenceGrade.SOURCE, span, bindsProperty, null, null);
    }

    public static Claim expert(UnitId id, String text, String assertedBy, String bindsProperty) {
        return new Claim(id, text, EvidenceGrade.EXPERT, null, bindsProperty, assertedBy, null);
    }
}