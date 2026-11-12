package org.skylark.bluewhale.domain.memory;

import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitKind;

import java.util.List;

public record CaptureSignal(
        String traceId,
        Scope scope,
        String source,
        String text,
        UnitKind kind,
        double confidence,
        List<String> tags
) {

    public CaptureSignal {
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("trace id is required");
        }
        if (scope == null) {
            throw new IllegalArgumentException("scope is required");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source is required");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("captured text is required");
        }
        kind = kind == null ? UnitKind.EPISODIC : kind;
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0 and 1");
        }
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}