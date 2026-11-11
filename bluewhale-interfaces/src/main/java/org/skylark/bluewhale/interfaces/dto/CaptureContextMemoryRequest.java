package org.skylark.bluewhale.interfaces.dto;

import org.skylark.bluewhale.domain.context.UnitKind;

import java.util.List;

public record CaptureContextMemoryRequest(
        String traceId,
        String tenant,
        String workspace,
        String project,
        String agentId,
        String userId,
        String sessionId,
        String source,
        String text,
        UnitKind kind,
        Double confidence,
        List<String> tags
) {
}
