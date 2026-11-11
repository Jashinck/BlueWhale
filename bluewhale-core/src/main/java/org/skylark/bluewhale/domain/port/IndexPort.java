package org.skylark.bluewhale.domain.port;

import org.skylark.bluewhale.domain.context.RepresentationLine;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;

import java.util.List;
import java.util.Map;

public interface IndexPort {

    void index(IndexEntry entry);

    void invalidate(UnitId id);

    List<Candidate> dense(Scope queryScope, float[] queryVector, int topK);

    default List<Candidate> lexical(Scope queryScope, String query, int topK) {
        return List.of();
    }

    default boolean supportsNativeHybrid() {
        return false;
    }

    record IndexEntry(
            UnitId unitId,
            Scope scope,
            RepresentationLine line,
            String text,
            float[] embedding,
            Map<String, String> metadata
    ) {
        public IndexEntry {
            if (unitId == null || scope == null || line == null || text == null || text.isBlank()) {
                throw new IllegalArgumentException("index entry requires unit, scope, line and text");
            }
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }

    record Candidate(
            UnitId unitId,
            RepresentationLine line,
            double score,
            Map<String, String> metadata,
            float[] embedding
    ) {
        public Candidate {
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
            embedding = embedding == null ? new float[0] : embedding.clone();
        }
    }
}
