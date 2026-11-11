package org.skylark.bluewhale.infrastructure.persistence;

import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.governance.ChangeRequest;
import org.skylark.bluewhale.domain.governance.ChangeRequestStatus;
import org.skylark.bluewhale.domain.port.ChangeRequestRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "bluewhale.store.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryChangeRequestRepository implements ChangeRequestRepository {

    private final ConcurrentHashMap<UnitId, ChangeRequest> store = new ConcurrentHashMap<>();

    @Override
    public void save(ChangeRequest changeRequest) {
        store.put(changeRequest.id(), changeRequest);
    }

    @Override
    public Optional<ChangeRequest> findById(UnitId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ChangeRequest> findByStatus(ChangeRequestStatus status) {
        return store.values().stream()
                .filter(request -> request.status() == status)
                .toList();
    }
}
