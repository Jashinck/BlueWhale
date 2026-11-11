package org.skylark.bluewhale.interfaces.rest;

import org.skylark.bluewhale.application.ChangeRequestDecision;
import org.skylark.bluewhale.application.GovernResult;
import org.skylark.bluewhale.application.GovernReviewUseCase;
import org.skylark.bluewhale.application.IngestCommand;
import org.skylark.bluewhale.application.IngestContextUseCase;
import org.skylark.bluewhale.application.IngestResult;
import org.skylark.bluewhale.domain.context.Actor;
import org.skylark.bluewhale.domain.context.ActorType;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.interfaces.dto.ChangeRequestDecisionRequest;
import org.skylark.bluewhale.interfaces.dto.SubmitChangeRequestRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/change-requests")
public class ChangeRequestController {

    private final IngestContextUseCase ingest;
    private final GovernReviewUseCase govern;

    public ChangeRequestController(IngestContextUseCase ingest, GovernReviewUseCase govern) {
        this.ingest = ingest;
        this.govern = govern;
    }

    @PostMapping
    public ResponseEntity<IngestResult> submit(@RequestBody SubmitChangeRequestRequest request) {
        Scope scope = Scope.of(request.tenant(), request.workspace(), request.project(),
                request.agentId(), request.userId(), request.sessionId());
        IngestCommand command = new IngestCommand(request.traceId(), request.author(), scope,
                request.ownerId(), request.ownerOrg(), request.content(), request.sourceRef());
        return ResponseEntity.ok(ingest.submit(command));
    }

    @PostMapping("/{id}/decision")
    public ResponseEntity<GovernResult> decide(@PathVariable String id,
                                               @RequestBody ChangeRequestDecisionRequest request) {
        Actor actor = new Actor(request.actorId(),
                request.actorType() == null ? ActorType.HUMAN : ActorType.valueOf(request.actorType()));
        ChangeRequestDecision decision = new ChangeRequestDecision(UnitId.of(id),
                ChangeRequestDecision.Action.valueOf(request.action()), actor,
                request.regressionRunId() == null ? null : UnitId.of(request.regressionRunId()));
        return ResponseEntity.ok(govern.decide(decision));
    }
}
