package org.skylark.bluewhale.domain.port;

import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.orchestration.WorkflowInstance;

import java.util.Optional;

public interface WorkflowInstanceRepository {

    void save(WorkflowInstance instance);

    Optional<WorkflowInstance> findById(UnitId id);
}