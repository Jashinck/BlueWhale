package org.skylark.bluewhale.domain.port;

import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.feedback.UsageEvent;

import java.util.List;

public interface UsageEventRepository {
    void append(UsageEvent event);

    List<UsageEvent> findByUnitId(UnitId unitId);
}
