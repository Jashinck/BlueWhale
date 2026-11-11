package org.skylark.bluewhale.domain.governance;

import org.skylark.bluewhale.domain.context.Scope;

public record KnowledgePatch(
        Scope scope,
        String ownerId,
        String ownerOrg,
        String content,
        String sourceRef
) {

    public KnowledgePatch {
        if (scope == null) {
            throw new IllegalArgumentException("patch scope is required");
        }
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("patch owner is required");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("patch content is required");
        }
        if (sourceRef == null || sourceRef.isBlank()) {
            throw new IllegalArgumentException("patch source is required");
        }
    }
}
