package org.skylark.bluewhale.application;

import org.skylark.bluewhale.domain.governance.ChangeRequest;
import org.skylark.bluewhale.domain.governance.ChangeRequestType;
import org.skylark.bluewhale.domain.governance.KnowledgePatch;
import org.skylark.bluewhale.domain.port.ChangeRequestRepository;
import org.skylark.bluewhale.domain.port.ClockPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngestContextUseCase {

    private final ChangeRequestRepository changeRequestRepository;
    private final ClockPort clock;

    public IngestContextUseCase(ChangeRequestRepository changeRequestRepository, ClockPort clock) {
        this.changeRequestRepository = changeRequestRepository;
        this.clock = clock;
    }

    @Transactional
    public IngestResult submit(IngestCommand command) {
        KnowledgePatch patch = new KnowledgePatch(command.scope(), command.ownerId(),
                command.ownerOrg(), command.content(), command.sourceRef());
        ChangeRequest request = ChangeRequest.draft(ChangeRequestType.ADD,
                command.author(), patch, clock.now());
        changeRequestRepository.save(request);
        return new IngestResult(request.id().toString(), request.status());
    }
}
