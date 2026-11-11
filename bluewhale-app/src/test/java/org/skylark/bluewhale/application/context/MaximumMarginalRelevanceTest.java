package org.skylark.bluewhale.application.context;

import org.junit.jupiter.api.Test;
import org.skylark.bluewhale.application.context.ReciprocalRankFusion.FusedCandidate;
import org.skylark.bluewhale.domain.context.RepresentationLine;
import org.skylark.bluewhale.domain.context.UnitId;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaximumMarginalRelevanceTest {

    @Test
    void shouldPreferComplementaryContentOverADuplicate() {
        UnitId redundant = UnitId.generate();
        UnitId duplicate = UnitId.generate();
        UnitId diverse = UnitId.generate();
        FusedCandidate first = candidate(redundant, 1.0, 1, 0);
        FusedCandidate sameVector = candidate(duplicate, 0.95, 1, 0);
        FusedCandidate complementary = candidate(diverse, 0.9, 0, 1);

        List<FusedCandidate> selected = new MaximumMarginalRelevance(0.7)
                .select(List.of(first, sameVector, complementary), 2);

        assertEquals(redundant, selected.get(0).unitId());
        assertEquals(diverse, selected.get(1).unitId());
    }

    @Test
    void shouldPreserveRankedOrderWhenNoDiversityIsRequested() {
        UnitId a = UnitId.generate();
        UnitId b = UnitId.generate();

        List<FusedCandidate> selected = new MaximumMarginalRelevance(1.0)
                .select(List.of(candidate(a, 1.0, 1, 0), candidate(b, 0.5, -1, 0)), 2);

        assertEquals(a, selected.get(0).unitId());
        assertEquals(b, selected.get(1).unitId());
    }

    private static FusedCandidate candidate(UnitId id, double score, float x, float y) {
        return new FusedCandidate(id, score, new float[]{x, y}, Map.of(),
                Set.of(RepresentationLine.CURATED_DOC));
    }
}
