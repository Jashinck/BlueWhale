package org.skylark.bluewhale.infrastructure.persistence;

import org.skylark.bluewhale.domain.context.LifecycleEvent;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.port.LifecycleEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
@ConditionalOnProperty(name = "bluewhale.store.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryLifecycleEventRepository implements LifecycleEventRepository {

    private final CopyOnWriteArrayList<LifecycleEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void append(LifecycleEvent event) {
        events.add(event);
    }

    @Override
    public List<LifecycleEvent> findByUnitId(UnitId unitId) {
        return events.stream()
                .filter(event -> event.unitId().equals(unitId))
                .sorted(Comparator.comparing(LifecycleEvent::occurredAt))
                .toList();
    }
}
