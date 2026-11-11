package org.skylark.bluewhale.interfaces.rest;

import org.skylark.bluewhale.application.CaptureMemoryUseCase;
import org.skylark.bluewhale.application.CaptureResult;
import org.skylark.bluewhale.application.CaptureSignal;
import org.skylark.bluewhale.application.FeedbackBatch;
import org.skylark.bluewhale.application.RecordFeedbackUseCase;
import org.skylark.bluewhale.application.ResolveContextUseCase;
import org.skylark.bluewhale.domain.context.BudgetSpec;
import org.skylark.bluewhale.domain.context.ContextBundle;
import org.skylark.bluewhale.domain.context.ResolveQuery;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.context.UnitKind;
import org.skylark.bluewhale.interfaces.dto.CaptureContextMemoryRequest;
import org.skylark.bluewhale.interfaces.dto.RecordFeedbackRequest;
import org.skylark.bluewhale.interfaces.dto.ResolveContextRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/context")
public class ContextController {

    private final ResolveContextUseCase resolveContext;
    private final CaptureMemoryUseCase captureMemory;
    private final RecordFeedbackUseCase recordFeedback;

    public ContextController(ResolveContextUseCase resolveContext, CaptureMemoryUseCase captureMemory,
                             RecordFeedbackUseCase recordFeedback) {
        this.resolveContext = resolveContext;
        this.captureMemory = captureMemory;
        this.recordFeedback = recordFeedback;
    }

    @PostMapping("/resolve")
    public ResponseEntity<ContextBundle> resolve(@RequestBody ResolveContextRequest request) {
        Scope scope = Scope.of(request.tenant(), request.workspace(), request.project(),
                request.agentId(), request.userId(), request.sessionId());
        int maxTokens = request.maxTokens() == null ? 2000 : request.maxTokens();
        ResolveQuery query = new ResolveQuery(request.traceId(), scope, request.intent(),
                request.query(), new BudgetSpec(maxTokens), request.lines(), request.maxDepth());
        return ResponseEntity.ok(resolveContext.resolve(query));
    }

    @PostMapping("/memories")
    public ResponseEntity<CaptureResult> capture(@RequestBody CaptureContextMemoryRequest request) {
        Scope scope = Scope.of(request.tenant(), request.workspace(), request.project(),
                request.agentId(), request.userId(), request.sessionId());
        double confidence = request.confidence() == null ? 0.8 : request.confidence();
        CaptureSignal signal = new CaptureSignal(request.traceId(), scope, request.source(),
                request.text(), request.kind() == null ? UnitKind.EPISODIC : request.kind(),
                confidence, request.tags());
        return ResponseEntity.ok(captureMemory.capture(signal));
    }

    @PostMapping("/feedback")
    public ResponseEntity<Void> feedback(@RequestBody RecordFeedbackRequest request) {
        recordFeedback.record(new FeedbackBatch(request.traceId(),
                request.items().stream()
                        .map(item -> new FeedbackBatch.FeedbackItem(
                                UnitId.of(item.unitId()), item.outcome(),
                                item.tokens() == null ? 0 : item.tokens()))
                        .toList()));
        return ResponseEntity.noContent().build();
    }
}
