package org.skylark.bluewhale.domain.knowledge;

import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;

import java.time.Instant;
import java.util.Objects;

public record Source(
        UnitId id,
        Scope scope,
        String uri,
        SourceKind kind,
        String contentHash,
        Instant fetchedAt
) {

    public Source {
        Objects.requireNonNull(id, "source id is required");
        Objects.requireNonNull(scope, "source scope is required");
        Objects.requireNonNull(kind, "source kind is required");
        Objects.requireNonNull(fetchedAt, "source fetchedAt is required");
        if (uri == null || uri.isBlank()) {
            throw new IllegalArgumentException("source uri is required");
        }
        if (contentHash == null || contentHash.isBlank()) {
            throw new IllegalArgumentException("source content hash is required");
        }
    }

    public static Source register(Scope scope, String uri, SourceKind kind, String contentHash, Instant now) {
        return new Source(UnitId.generate(), scope, uri, kind, contentHash, now);
    }
}