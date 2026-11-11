package org.skylark.bluewhale.interfaces.dto;

import org.skylark.bluewhale.domain.context.Outcome;

import java.util.List;

public record RecordFeedbackRequest(String traceId, List<Item> items) {

    public record Item(String unitId, Outcome outcome, Integer tokens) {
    }
}
