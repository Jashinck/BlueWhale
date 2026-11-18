package org.skylark.bluewhale.domain.feedback;

public record AttributionFacts(
        boolean resultCorrect,
        boolean expectedUnitExists,
        boolean expectedUnitActive,
        boolean expectedUnitRecalled,
        boolean contentMismatch,
        boolean samePropertyConflict
) {
}