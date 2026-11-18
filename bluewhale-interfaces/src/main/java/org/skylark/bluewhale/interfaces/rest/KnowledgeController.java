package org.skylark.bluewhale.interfaces.rest;

import org.skylark.bluewhale.application.orchestration.KnowledgeWorkflowEngine;
import org.skylark.bluewhale.domain.context.Actor;
import org.skylark.bluewhale.domain.context.ActorType;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.orchestration.WorkflowInstance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeWorkflowEngine engine;

    public KnowledgeController(KnowledgeWorkflowEngine engine) {
        this.engine = engine;
    }

    @PostMapping("/construct")
    public ResponseEntity<WorkflowResponse> construct(@RequestBody ConstructRequest request) {
        Scope scope = Scope.of(request.tenant(), request.workspace(), request.project(),
                request.agentId(), request.userId(), request.sessionId());
        WorkflowInstance workflow = engine.start(request.sourceUri(), scope, request.ownerId());
        return ResponseEntity.ok(toResponse(workflow));
    }

    @PostMapping("/workflows/{id}/resume")
    public ResponseEntity<WorkflowResponse> resume(@PathVariable String id, @RequestBody ResumeRequest request) {
        Actor actor = new Actor(request.actorId(), ActorType.valueOf(request.actorType()));
        WorkflowInstance workflow = engine.resume(UnitId.of(id), actor, request.approved());
        return ResponseEntity.ok(toResponse(workflow));
    }

    private static WorkflowResponse toResponse(WorkflowInstance workflow) {
        return new WorkflowResponse(workflow.id().toString(), workflow.state().name(),
                workflow.changeRequestId(), workflow.unitId());
    }

    public record ConstructRequest(
            String sourceUri, String tenant, String workspace, String project,
            String agentId, String userId, String sessionId, String ownerId) {
    }

    public record ResumeRequest(String actorId, String actorType, boolean approved) {
    }

    public record WorkflowResponse(String workflowId, String state, String changeRequestId, String unitId) {
    }
}