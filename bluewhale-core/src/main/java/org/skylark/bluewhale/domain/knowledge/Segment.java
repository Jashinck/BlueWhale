package org.skylark.bluewhale.domain.knowledge;

import org.skylark.bluewhale.domain.context.UnitId;

import java.util.Objects;

public record Segment(
        UnitId id,
        UnitId sourceId,
        UnitId parentId,
        int ordinal,
        String path,
        String abstractText,
        String overview,
        String contentRef
) {

    public Segment {
        Objects.requireNonNull(id, "segment id is required");
        Objects.requireNonNull(sourceId, "segment source is required");
        if (ordinal < 0) {
            throw new IllegalArgumentException("segment ordinal must not be negative");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("segment path is required");
        }
    }
}