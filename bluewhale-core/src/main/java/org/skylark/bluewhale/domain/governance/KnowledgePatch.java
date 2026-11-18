package org.skylark.bluewhale.domain.governance;

import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;

public record KnowledgePatch(
        Scope scope,
        String ownerId,
        String ownerOrg,
        String content,
        String sourceRef,
        UnitId targetUnitId
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

    public static KnowledgePatch add(Scope scope, String ownerId, String ownerOrg, String content, String sourceRef) {
        return new KnowledgePatch(scope, ownerId, ownerOrg, content, sourceRef, null);
    }

    public static KnowledgePatch revise(Scope scope, String ownerId, String ownerOrg, String content,
                                        String sourceRef, UnitId targetUnitId) {
        if (targetUnitId == null) {
            throw new IllegalArgumentException("revise patch requires a target unit");
        }
        return new KnowledgePatch(scope, ownerId, ownerOrg, content, sourceRef, targetUnitId);
    }
}