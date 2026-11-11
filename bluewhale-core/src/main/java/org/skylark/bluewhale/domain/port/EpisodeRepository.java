package org.skylark.bluewhale.domain.port;

import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.memory.Episode;

import java.util.Optional;

public interface EpisodeRepository {
    void append(Episode episode);

    Optional<Episode> findById(UnitId id);
}
