package org.skylark.bluewhale.application.context;

import org.skylark.bluewhale.application.context.ReciprocalRankFusion.FusedCandidate;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.context.UnitState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public final class StateAwareReranker {

    public List<FusedCandidate> rerank(List<FusedCandidate> candidates,
                                       Function<UnitId, UnitState> states) {
        List<FusedCandidate> reranked = new ArrayList<>(candidates.size());
        for (FusedCandidate candidate : candidates) {
            UnitState state = states.apply(candidate.unitId());
            reranked.add(candidate.withScore(candidate.score() * weight(state)));
        }
        reranked.sort(Comparator.comparingDouble(FusedCandidate::score).reversed()
                .thenComparing(candidate -> candidate.unitId().toString()));
        return reranked;
    }

    private static double weight(UnitState state) {
        return switch (state) {
            case ACTIVE -> 1.0;
            case CONFLICTED -> 0.9;
            case STALE_SUSPECT -> 0.75;
            default -> 0.0;
        };
    }
}
