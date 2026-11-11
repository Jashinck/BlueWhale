package org.skylark.bluewhale.application.context;

import org.junit.jupiter.api.Test;
import org.skylark.bluewhale.application.context.ReciprocalRankFusion.FusedCandidate;
import org.skylark.bluewhale.domain.context.RepresentationLine;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.context.UnitState;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StateAwareRerankerTest {

    @Test
    void shouldRankActiveContentAboveStaleContentOfEqualRecallScore() {
        UnitId stale = UnitId.generate();
        UnitId active = UnitId.generate();
        FusedCandidate staleCandidate = candidate(stale);
        FusedCandidate activeCandidate = candidate(active);

        List<FusedCandidate> reranked = new StateAwareReranker().rerank(
                List.of(staleCandidate, activeCandidate),
                id -> id.equals(active) ? UnitState.ACTIVE : UnitState.STALE_SUSPECT);

        assertEquals(active, reranked.get(0).unitId());
        assertEquals(stale, reranked.get(1).unitId());
    }

    private static FusedCandidate candidate(UnitId id) {
        return new FusedCandidate(id, 1.0, new float[]{1, 0}, Map.of(),
                Set.of(RepresentationLine.CURATED_DOC));
    }
}
