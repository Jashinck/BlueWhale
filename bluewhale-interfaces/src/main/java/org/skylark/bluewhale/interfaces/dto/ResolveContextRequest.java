package org.skylark.bluewhale.interfaces.dto;

import org.skylark.bluewhale.domain.context.RepresentationDepth;
import org.skylark.bluewhale.domain.context.RepresentationLine;

import java.util.Set;

public record ResolveContextRequest(
        String traceId,
        String tenant,
        String workspace,
        String project,
        String agentId,
        String userId,
        String sessionId,
        String intent,
        String query,
        Integer maxTokens,
        Set<RepresentationLine> lines,
        RepresentationDepth maxDepth
) {
}
