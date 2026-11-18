package org.skylark.bluewhale.infrastructure.persistence;

import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.orchestration.WorkflowInstance;
import org.skylark.bluewhale.domain.port.WorkflowInstanceRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "bluewhale.store.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryWorkflowInstanceRepository implements WorkflowInstanceRepository {

    private final ConcurrentHashMap<UnitId, WorkflowInstance> store = new ConcurrentHashMap<>();

    @Override
    public void save(WorkflowInstance instance) {
        store.put(instance.id(), instance);
    }

    @Override
    public Optional<WorkflowInstance> findById(UnitId id) {
        return Optional.ofNullable(store.get(id));
    }
}