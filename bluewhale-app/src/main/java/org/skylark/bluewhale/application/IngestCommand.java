package org.skylark.bluewhale.application;

import org.skylark.bluewhale.domain.context.Scope;

public record IngestCommand(
        String traceId,
        String author,
        Scope scope,
        String ownerId,
        String ownerOrg,
        String content,
        String sourceRef
) {

    public IngestCommand {
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("trace id is required");
        }
        if (author == null || author.isBlank()) {
            throw new IllegalArgumentException("author is required");
        }
        if (scope == null) {
            throw new IllegalArgumentException("scope is required");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content is required");
        }
        if (sourceRef == null || sourceRef.isBlank()) {
            throw new IllegalArgumentException("source ref is required");
        }
    }
}
