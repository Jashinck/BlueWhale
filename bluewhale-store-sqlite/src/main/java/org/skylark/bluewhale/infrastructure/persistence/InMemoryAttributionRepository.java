package org.skylark.bluewhale.infrastructure.persistence;

import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.feedback.Attribution;
import org.skylark.bluewhale.domain.port.AttributionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "bluewhale.store.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryAttributionRepository implements AttributionRepository {

    private final ConcurrentHashMap<UnitId, Attribution> store = new ConcurrentHashMap<>();

    @Override
    public void save(Attribution attribution) {
        store.put(attribution.signalId(), attribution);
    }

    @Override
    public Optional<Attribution> findBySignalId(UnitId signalId) {
        return Optional.ofNullable(store.get(signalId));
    }

    @Override
    public List<Attribution> findAll() {
        return List.copyOf(store.values());
    }
}