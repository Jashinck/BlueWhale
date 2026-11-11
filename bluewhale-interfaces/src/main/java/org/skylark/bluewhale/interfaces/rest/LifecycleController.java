package org.skylark.bluewhale.interfaces.rest;

import org.skylark.bluewhale.application.LifecycleUseCase;
import org.skylark.bluewhale.domain.context.Actor;
import org.skylark.bluewhale.domain.context.ActorType;
import org.skylark.bluewhale.domain.context.LifecycleEvent;
import org.skylark.bluewhale.domain.context.TransitionResult;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.interfaces.dto.LifecycleEventResponse;
import org.skylark.bluewhale.interfaces.dto.TransitionRequest;
import org.skylark.bluewhale.interfaces.dto.TransitionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/context/units")
public class LifecycleController {

    private final LifecycleUseCase lifecycle;

    public LifecycleController(LifecycleUseCase lifecycle) {
        this.lifecycle = lifecycle;
    }

    @PostMapping("/{id}/transitions")
    public ResponseEntity<TransitionResponse> transition(@PathVariable String id,
                                                         @RequestBody TransitionRequest request) {
        ActorType actorType = request.actorType() == null ? ActorType.HUMAN : request.actorType();
        Actor actor = new Actor(request.actorId(), actorType);
        TransitionResult result = lifecycle.transition(UnitId.of(id), request.transition(),
                actor, request.reason(), request.batchId());
        return ResponseEntity.ok(TransitionResponse.from(result));
    }

    @GetMapping("/{id}/lifecycle-events")
    public ResponseEntity<List<LifecycleEventResponse>> history(@PathVariable String id) {
        return ResponseEntity.ok(lifecycle.history(UnitId.of(id)).stream()
                .map(LifecycleEventResponse::from)
                .toList());
    }
}
