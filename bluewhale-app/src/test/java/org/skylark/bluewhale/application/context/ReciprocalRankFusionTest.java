package org.skylark.bluewhale.application.context;

import org.junit.jupiter.api.Test;
import org.skylark.bluewhale.application.context.ReciprocalRankFusion.FusedCandidate;
import org.skylark.bluewhale.domain.context.RepresentationLine;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.port.IndexPort;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReciprocalRankFusionTest {

    private final ReciprocalRankFusion fusion = new ReciprocalRankFusion(60);
    private final UnitId first = UnitId.generate();
    private final UnitId second = UnitId.generate();
    private final UnitId third = UnitId.generate();

    @Test
    void shouldFuseByReciprocalRankAndDeduplicateWithinOneLine() {
        IndexPort.Candidate firstDense = candidate(first, RepresentationLine.CURATED_DOC, 0.9, 1, 0);
        List<List<IndexPort.Candidate>> lists = List.of(
                List.of(firstDense, candidate(first, RepresentationLine.CURATED_DOC, 0.8, 1, 0),
                        candidate(second, RepresentationLine.CURATED_DOC, 0.7, 0, 1)),
                List.of(candidate(first, RepresentationLine.ENTITY_GRAPH, 0.6, 1, 0),
                        candidate(third, RepresentationLine.ENTITY_GRAPH, 0.5, -1, 0))
        );

        List<FusedCandidate> fused = fusion.fuse(lists, 10);

        assertEquals(first, fused.get(0).unitId());
        assertEquals(2, fused.get(0).lines().size());
        assertTrue(fused.get(0).embedding().length > 0);
        assertEquals(3, fused.size());
        assertTrue(fused.stream().map(FusedCandidate::unitId).toList().containsAll(List.of(second, third)));
    }

    @Test
    void shouldRespectLimitAndRankOrder() {
        List<List<IndexPort.Candidate>> lists = List.of(
                List.of(candidate(first, RepresentationLine.CURATED_DOC, 1.0, 1, 0),
                        candidate(second, RepresentationLine.CURATED_DOC, 0.9, 0, 1)),
                List.of(candidate(second, RepresentationLine.EPISODE_TIMELINE, 1.0, 0, 1),
                        candidate(third, RepresentationLine.EPISODE_TIMELINE, 0.8, -1, 0))
        );

        List<FusedCandidate> fused = fusion.fuse(lists, 2);

        assertEquals(2, fused.size());
        assertEquals(second, fused.get(0).unitId());
    }

    private static IndexPort.Candidate candidate(UnitId id, RepresentationLine line,
                                                  double score, float x, float y) {
        return new IndexPort.Candidate(id, line, score, Map.of("k", line.name()),
                new float[]{x, y});
    }
}
