package org.skylark.bluewhale.domain.context;

public record Actor(String id, ActorType type) {

    public Actor {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("actor id is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("actor type is required");
        }
    }

    public static Actor system(String id) {
        return new Actor(id, ActorType.SYSTEM);
    }
}
