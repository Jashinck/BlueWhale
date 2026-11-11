package org.skylark.bluewhale.application;

import org.skylark.bluewhale.domain.context.Outcome;
import org.skylark.bluewhale.domain.context.UnitId;

import java.util.List;

public record FeedbackBatch(String traceId, List<FeedbackItem> items) {

    public FeedbackBatch {
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("trace id is required");
        }
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record FeedbackItem(UnitId unitId, Outcome outcome, int tokens) {
        public FeedbackItem {
            if (unitId == null || outcome == null) {
                throw new IllegalArgumentException("feedback item requires unit and outcome");
            }
            if (tokens < 0) {
                throw new IllegalArgumentException("feedback tokens must not be negative");
            }
        }
    }
}
