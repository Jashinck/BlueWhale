package org.skylark.bluewhale.infrastructure.persistence;

import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.memory.Episode;
import org.skylark.bluewhale.domain.port.EpisodeRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "bluewhale.store.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryEpisodeRepository implements EpisodeRepository {

    private final ConcurrentHashMap<UnitId, Episode> episodes = new ConcurrentHashMap<>();

    @Override
    public void append(Episode episode) {
        episodes.put(episode.id(), episode);
    }

    @Override
    public Optional<Episode> findById(UnitId id) {
        return Optional.ofNullable(episodes.get(id));
    }
}
