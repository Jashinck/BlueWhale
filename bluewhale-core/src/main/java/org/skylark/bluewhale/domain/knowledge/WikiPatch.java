package org.skylark.bluewhale.domain.knowledge;

import java.util.List;
import java.util.Objects;

public record WikiPatch(
        WikiPatchType type,
        String pagePath,
        String diff,
        String sourceHash,
        List<Claim> claims
) {

    public WikiPatch {
        Objects.requireNonNull(type, "wiki patch type is required");
        if (pagePath == null || pagePath.isBlank()) {
            throw new IllegalArgumentException("wiki patch page path is required");
        }
        if (diff == null || diff.isBlank()) {
            throw new IllegalArgumentException("wiki patch diff is required");
        }
        if (sourceHash == null || sourceHash.isBlank()) {
            throw new IllegalArgumentException("wiki patch source hash is required");
        }
        claims = claims == null ? List.of() : List.copyOf(claims);
    }
}