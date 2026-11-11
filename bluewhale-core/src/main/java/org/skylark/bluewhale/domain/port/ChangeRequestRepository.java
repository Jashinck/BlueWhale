package org.skylark.bluewhale.domain.port;

import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.governance.ChangeRequest;
import org.skylark.bluewhale.domain.governance.ChangeRequestStatus;

import java.util.List;
import java.util.Optional;

public interface ChangeRequestRepository {
    void save(ChangeRequest changeRequest);

    Optional<ChangeRequest> findById(UnitId id);

    List<ChangeRequest> findByStatus(ChangeRequestStatus status);
}
