package org.skylark.bluewhale.domain.port;

import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.feedback.Signal;

import java.util.List;

public interface SignalRepository {

    void append(Signal signal);

    List<Signal> findByUnitId(UnitId unitId);

    List<Signal> findAll();
}