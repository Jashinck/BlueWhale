package org.skylark.bluewhale.domain.feedback;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributionServiceTest {

    private final AttributionService service = new AttributionService();

    private static AttributionFacts facts(boolean correct, boolean exists, boolean active,
                                          boolean recalled, boolean mismatch, boolean conflict) {
        return new AttributionFacts(correct, exists, active, recalled, mismatch, conflict);
    }

    @Test
    void correctResultIsNotAttributed() {
        assertTrue(service.attribute(facts(true, true, true, true, false, false)).isEmpty());
    }

    @Test
    void missingExpectedBasisIsMissing() {
        assertEquals(AttributionCode.MISSING,
                service.attribute(facts(false, false, false, false, false, false)).orElseThrow());
    }

    @Test
    void activeButNotRecalledIsNoHit() {
        assertEquals(AttributionCode.NO_HIT,
                service.attribute(facts(false, true, true, false, false, false)).orElseThrow());
    }

    @Test
    void inactiveAndNotRecalledIsStale() {
        assertEquals(AttributionCode.STALE,
                service.attribute(facts(false, true, false, false, false, false)).orElseThrow());
    }

    @Test
    void recalledButContentMismatchIsBadClaim() {
        assertEquals(AttributionCode.BAD_CLAIM,
                service.attribute(facts(false, true, true, true, true, false)).orElseThrow());
    }

    @Test
    void samePropertyConflictIsConflict() {
        assertEquals(AttributionCode.CONFLICT,
                service.attribute(facts(false, true, true, true, false, true)).orElseThrow());
    }

    @Test
    void otherwiseConsumerError() {
        assertEquals(AttributionCode.CONSUMER_ERR,
                service.attribute(facts(false, true, true, true, false, false)).orElseThrow());
    }

    @Test
    void consumerErrorAndWrongPickCannotDraft() {
        assertFalse(AttributionCode.CONSUMER_ERR.canDraft());
        assertFalse(AttributionCode.WRONG_PICK.canDraft());
        assertTrue(AttributionCode.BAD_CLAIM.canDraft());
        assertTrue(AttributionCode.MISSING.canDraft());
    }
}