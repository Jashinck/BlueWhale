package org.skylark.bluewhale.interfaces.rest;

import org.skylark.bluewhale.application.AttributeSignalUseCase;
import org.skylark.bluewhale.application.DistillUseCase;
import org.skylark.bluewhale.application.RecordSignalUseCase;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.feedback.Attribution;
import org.skylark.bluewhale.domain.feedback.AttributionFacts;
import org.skylark.bluewhale.domain.feedback.Signal;
import org.skylark.bluewhale.domain.feedback.SignalKind;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeLoopController {

    private final RecordSignalUseCase recordSignal;
    private final AttributeSignalUseCase attributeSignal;
    private final DistillUseCase distill;

    public KnowledgeLoopController(RecordSignalUseCase recordSignal,
                                   AttributeSignalUseCase attributeSignal, DistillUseCase distill) {
        this.recordSignal = recordSignal;
        this.attributeSignal = attributeSignal;
        this.distill = distill;
    }

    @PostMapping("/signals")
    public ResponseEntity<SignalResponse> reportSignal(@RequestBody SignalRequest request) {
        Signal signal = Signal.report(request.traceId(), SignalKind.valueOf(request.kind()),
                request.unitId() == null ? null : UnitId.of(request.unitId()),
                request.expectedUnitId() == null ? null : UnitId.of(request.expectedUnitId()),
                request.severity(), request.reason(), request.reporter(), Instant.now());
        recordSignal.record(signal);
        AttributionFacts facts = new AttributionFacts(false, true, true, true,
                signal.kind() == SignalKind.EDIT, signal.kind() == SignalKind.CONFLICT);
        Optional<Attribution> attribution = attributeSignal.attribute(signal, facts);
        return ResponseEntity.ok(new SignalResponse(signal.id().toString(),
                attribution.map(a -> a.code().name()).orElse(null)));
    }

    @PostMapping("/distill")
    public ResponseEntity<DistillResponse> distill(@RequestBody DistillRequest request) {
        Scope scope = Scope.of(request.tenant(), request.workspace(), request.project(),
                request.agentId(), request.userId(), request.sessionId());
        List<UnitId> drafted = distill.distill(scope);
        return ResponseEntity.ok(new DistillResponse(drafted.stream().map(UnitId::toString).toList()));
    }

    public record SignalRequest(String traceId, String kind, String unitId, String expectedUnitId,
                                String severity, String reason, String reporter) {
    }

    public record SignalResponse(String signalId, String attributionCode) {
    }

    public record DistillRequest(String tenant, String workspace, String project,
                                 String agentId, String userId, String sessionId) {
    }

    public record DistillResponse(List<String> draftedChangeRequestIds) {
    }
}