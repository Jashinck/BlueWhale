package org.skylark.bluewhale.domain.context;

import java.util.Objects;
import java.util.UUID;

public record UnitId(UUID value) {

    public UnitId {
        Objects.requireNonNull(value, "unit id must not be null");
    }

    public static UnitId generate() {
        return new UnitId(UUID.randomUUID());
    }

    public static UnitId of(String value) {
        return new UnitId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
