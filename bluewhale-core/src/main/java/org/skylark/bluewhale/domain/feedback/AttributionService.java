package org.skylark.bluewhale.domain.feedback;

import java.util.Optional;

public final class AttributionService {

    public Optional<AttributionCode> attribute(AttributionFacts facts) {
        if (facts.resultCorrect()) {
            return Optional.empty();
        }
        if (!facts.expectedUnitExists()) {
            return Optional.of(AttributionCode.MISSING);
        }
        if (!facts.expectedUnitRecalled()) {
            return Optional.of(facts.expectedUnitActive() ? AttributionCode.NO_HIT : AttributionCode.STALE);
        }
        if (facts.contentMismatch()) {
            return Optional.of(AttributionCode.BAD_CLAIM);
        }
        if (facts.samePropertyConflict()) {
            return Optional.of(AttributionCode.CONFLICT);
        }
        return Optional.of(AttributionCode.CONSUMER_ERR);
    }
}