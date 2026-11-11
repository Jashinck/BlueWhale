package org.skylark.bluewhale.infrastructure.index;

import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.port.IndexPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "bluewhale.store.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryIndexAdapter implements IndexPort {

    private record Entry(IndexEntry value) {
    }

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public void index(IndexEntry entry) {
        entries.put(key(entry.unitId(), entry.line().name()), new Entry(entry));
    }

    @Override
    public void invalidate(UnitId id) {
        entries.keySet().removeIf(key -> key.startsWith(id + ":"));
    }

    @Override
    public List<Candidate> dense(Scope queryScope, float[] queryVector, int topK) {
        if (queryVector == null || queryVector.length == 0 || topK <= 0) {
            return List.of();
        }
        return entries.values().stream()
                .map(Entry::value)
                .filter(entry -> queryScope.canSee(entry.scope()))
                .map(entry -> new Candidate(entry.unitId(), entry.line(),
                        cosine(queryVector, entry.embedding()), entry.metadata(), entry.embedding()))
                .sorted(Comparator.comparingDouble(Candidate::score).reversed())
                .limit(topK)
                .toList();
    }

    @Override
    public List<Candidate> lexical(Scope queryScope, String query, int topK) {
        if (query == null || query.isBlank() || topK <= 0) {
            return List.of();
        }
        String needle = query.toLowerCase(Locale.ROOT);
        return entries.values().stream()
                .map(Entry::value)
                .filter(entry -> queryScope.canSee(entry.scope()))
                .map(entry -> new Candidate(entry.unitId(), entry.line(),
                        lexicalScore(entry.text(), needle), entry.metadata(), entry.embedding()))
                .filter(candidate -> candidate.score() > 0)
                .sorted(Comparator.comparingDouble(Candidate::score).reversed())
                .limit(topK)
                .toList();
    }

    private static double lexicalScore(String text, String needle) {
        String normalized = text.toLowerCase(Locale.ROOT);
        if (normalized.contains(needle)) {
            return 1.0;
        }
        long matched = List.of(needle.split("\\s+")).stream()
                .filter(token -> !token.isBlank() && normalized.contains(token))
                .count();
        return matched == 0 ? 0.0 : Math.min(0.9, matched * 0.2);
    }

    private static double cosine(float[] left, float[] right) {
        if (left == null || right == null || left.length != right.length || left.length == 0) {
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

    private static String key(UnitId id, String line) {
        return id + ":" + line;
    }
}
