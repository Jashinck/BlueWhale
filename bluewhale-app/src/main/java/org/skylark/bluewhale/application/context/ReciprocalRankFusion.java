package org.skylark.bluewhale.application.context;

import org.skylark.bluewhale.domain.context.RepresentationLine;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.port.IndexPort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ReciprocalRankFusion {

    public static final int DEFAULT_K = 60;

    private final int k;

    public ReciprocalRankFusion() {
        this(DEFAULT_K);
    }

    public ReciprocalRankFusion(int k) {
        if (k <= 0) {
            throw new IllegalArgumentException("RRF k must be positive");
        }
        this.k = k;
    }

    public List<FusedCandidate> fuse(List<List<IndexPort.Candidate>> rankedLists, int limit) {
        Map<UnitId, Accumulator> candidates = new LinkedHashMap<>();
        for (List<IndexPort.Candidate> ranked : rankedLists) {
            Set<UnitId> seenInList = new HashSet<>();
            int rank = 0;
            for (IndexPort.Candidate candidate : ranked) {
                if (!seenInList.add(candidate.unitId())) {
                    continue;
                }
                rank++;
                candidates.computeIfAbsent(candidate.unitId(), ignored -> new Accumulator(candidate))
                        .accept(candidate, 1.0 / (k + rank));
            }
        }
        return candidates.values().stream()
                .map(Accumulator::toCandidate)
                .sorted(Comparator.comparingDouble(FusedCandidate::score).reversed()
                        .thenComparing(candidate -> candidate.unitId().toString()))
                .limit(Math.max(0, limit))
                .toList();
    }

    public record FusedCandidate(
            UnitId unitId,
            double score,
            float[] embedding,
            Map<String, String> metadata,
            Set<RepresentationLine> lines
    ) {
        public FusedCandidate {
            embedding = embedding == null ? new float[0] : embedding.clone();
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
            lines = lines == null ? Set.of() : Set.copyOf(lines);
        }

        public FusedCandidate withScore(double nextScore) {
            return new FusedCandidate(unitId, nextScore, embedding, metadata, lines);
        }
    }

    private static final class Accumulator {
        private final UnitId unitId;
        private final Map<String, String> metadata = new HashMap<>();
        private final Set<RepresentationLine> lines = EnumSet.noneOf(RepresentationLine.class);
        private float[] embedding = new float[0];
        private double score;

        private Accumulator(IndexPort.Candidate first) {
            this.unitId = first.unitId();
        }

        private void accept(IndexPort.Candidate candidate, double contribution) {
            score += contribution;
            metadata.putAll(candidate.metadata());
            lines.add(candidate.line());
            if (embedding.length == 0 && candidate.embedding().length > 0) {
                embedding = candidate.embedding().clone();
            }
        }

        private FusedCandidate toCandidate() {
            return new FusedCandidate(unitId, score, embedding, metadata, lines);
        }
    }
}
