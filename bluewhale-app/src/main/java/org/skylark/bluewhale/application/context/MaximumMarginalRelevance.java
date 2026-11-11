package org.skylark.bluewhale.application.context;

import org.skylark.bluewhale.application.context.ReciprocalRankFusion.FusedCandidate;

import java.util.ArrayList;
import java.util.List;

public final class MaximumMarginalRelevance {

    private final double lambda;

    public MaximumMarginalRelevance(double lambda) {
        if (lambda < 0.0 || lambda > 1.0) {
            throw new IllegalArgumentException("MMR lambda must be between 0 and 1");
        }
        this.lambda = lambda;
    }

    public List<FusedCandidate> select(List<FusedCandidate> candidates, int limit) {
        if (candidates.isEmpty() || limit <= 0) {
            return List.of();
        }
        double maxRelevance = candidates.stream()
                .mapToDouble(FusedCandidate::score)
                .max()
                .orElse(0.0);
        if (maxRelevance <= 0.0) {
            maxRelevance = 1.0;
        }

        List<FusedCandidate> remaining = new ArrayList<>(candidates);
        List<FusedCandidate> selected = new ArrayList<>();
        while (!remaining.isEmpty() && selected.size() < limit) {
            FusedCandidate best = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (FusedCandidate candidate : remaining) {
                double relevance = candidate.score() / maxRelevance;
                double redundancy = selected.stream()
                        .mapToDouble(chosen -> cosine(candidate.embedding(), chosen.embedding()))
                        .max()
                        .orElse(0.0);
                double mmr = lambda * relevance - (1.0 - lambda) * redundancy;
                if (mmr > bestScore) {
                    bestScore = mmr;
                    best = candidate;
                }
            }
            selected.add(best);
            remaining.remove(best);
        }
        return selected;
    }

    static double cosine(float[] left, float[] right) {
        if (left == null || right == null || left.length == 0
                || left.length != right.length) {
            return 0.0;
        }
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < left.length; i++) {
            dot += (double) left[i] * right[i];
            leftNorm += (double) left[i] * left[i];
            rightNorm += (double) right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
