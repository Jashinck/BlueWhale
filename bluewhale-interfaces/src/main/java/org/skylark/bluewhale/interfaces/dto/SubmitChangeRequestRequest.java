package org.skylark.bluewhale.interfaces.dto;

public record SubmitChangeRequestRequest(
        String traceId,
        String author,
        String tenant,
        String workspace,
        String project,
        String agentId,
        String userId,
        String sessionId,
        String ownerId,
        String ownerOrg,
        String content,
        String sourceRef
) {
}
