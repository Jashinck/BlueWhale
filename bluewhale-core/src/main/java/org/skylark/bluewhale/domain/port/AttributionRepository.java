package org.skylark.bluewhale.domain.port;

import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.feedback.Attribution;

import java.util.List;
import java.util.Optional;

public interface AttributionRepository {

    void save(Attribution attribution);

    Optional<Attribution> findBySignalId(UnitId signalId);

    List<Attribution> findAll();
}