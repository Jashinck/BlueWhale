package org.skylark.bluewhale.infrastructure.persistence;

import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.feedback.Signal;
import org.skylark.bluewhale.domain.port.SignalRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
@ConditionalOnProperty(name = "bluewhale.store.mode", havingValue = "memory", matchIfMissing = true)
public class InMemorySignalRepository implements SignalRepository {

    private final CopyOnWriteArrayList<Signal> store = new CopyOnWriteArrayList<>();

    @Override
    public void append(Signal signal) {
        store.add(signal);
    }

    @Override
    public List<Signal> findByUnitId(UnitId unitId) {
        return store.stream().filter(signal -> unitId.equals(signal.unitId())).toList();
    }

    @Override
    public List<Signal> findAll() {
        return List.copyOf(store);
    }
}