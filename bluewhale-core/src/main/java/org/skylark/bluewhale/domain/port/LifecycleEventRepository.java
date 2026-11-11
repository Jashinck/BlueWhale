package org.skylark.bluewhale.domain.port;

import org.skylark.bluewhale.domain.context.LifecycleEvent;
import org.skylark.bluewhale.domain.context.UnitId;

import java.util.List;

public interface LifecycleEventRepository {
    void append(LifecycleEvent event);

    List<LifecycleEvent> findByUnitId(UnitId unitId);
}
