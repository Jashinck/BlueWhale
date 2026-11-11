package org.skylark.bluewhale.infrastructure.persistence;

import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.feedback.UsageEvent;
import org.skylark.bluewhale.domain.port.UsageEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
@ConditionalOnProperty(name = "bluewhale.store.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryUsageEventRepository implements UsageEventRepository {

    private final CopyOnWriteArrayList<UsageEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void append(UsageEvent event) {
        events.add(event);
    }

    @Override
    public List<UsageEvent> findByUnitId(UnitId unitId) {
        return events.stream()
                .filter(event -> event.unitId().equals(unitId))
                .sorted(Comparator.comparing(UsageEvent::at))
                .toList();
    }
}
