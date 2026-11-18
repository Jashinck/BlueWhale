package org.skylark.bluewhale.application;

import org.skylark.bluewhale.domain.context.ContextUnit;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.feedback.Attribution;
import org.skylark.bluewhale.domain.feedback.AttributionCode;
import org.skylark.bluewhale.domain.feedback.Signal;
import org.skylark.bluewhale.domain.governance.ChangeRequest;
import org.skylark.bluewhale.domain.governance.ChangeRequestType;
import org.skylark.bluewhale.domain.governance.KnowledgePatch;
import org.skylark.bluewhale.domain.governance.RegressionCase;
import org.skylark.bluewhale.domain.port.AttributionRepository;
import org.skylark.bluewhale.domain.port.ChangeRequestRepository;
import org.skylark.bluewhale.domain.port.ClockPort;
import org.skylark.bluewhale.domain.port.ContextUnitRepository;
import org.skylark.bluewhale.domain.port.RegressionCaseRepository;
import org.skylark.bluewhale.domain.port.SignalRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DistillUseCase {

    private final SignalRepository signalRepository;
    private final AttributionRepository attributionRepository;
    private final ChangeRequestRepository changeRequestRepository;
    private final RegressionCaseRepository regressionCaseRepository;
    private final ContextUnitRepository unitRepository;
    private final ClockPort clock;

    public DistillUseCase(SignalRepository signalRepository, AttributionRepository attributionRepository,
                          ChangeRequestRepository changeRequestRepository,
                          RegressionCaseRepository regressionCaseRepository,
                          ContextUnitRepository unitRepository, ClockPort clock) {
        this.signalRepository = signalRepository;
        this.attributionRepository = attributionRepository;
        this.changeRequestRepository = changeRequestRepository;
        this.regressionCaseRepository = regressionCaseRepository;
        this.unitRepository = unitRepository;
        this.clock = clock;
    }

    public List<UnitId> distill(Scope scope) {
        List<UnitId> drafted = new ArrayList<>();
        for (Signal signal : signalRepository.findAll()) {
            if (signal.kind().isImplicit() || signal.unitId() == null) {
                continue;
            }
            Attribution attribution = attributionRepository.findBySignalId(signal.id()).orElse(null);
            if (attribution == null || !attribution.code().canDraft()) {
                continue;
            }
            ChangeRequestType type = draftType(attribution.code());
            if (type == null) {
                continue;
            }
            ContextUnit target = unitRepository.findById(signal.unitId()).orElse(null);
            if (target == null) {
                continue;
            }
            String owner = target.ownership().ownerId() == null ? "system" : target.ownership().ownerId();
            String content = signal.reason() == null || signal.reason().isBlank()
                    ? target.content().abstractText() : signal.reason();
            KnowledgePatch patch = new KnowledgePatch(scope, owner, target.ownership().ownerOrg(),
                    content, "distill:" + signal.id(), signal.unitId());
            ChangeRequest request = ChangeRequest.draft(type, "SYSTEM", patch, clock.now());
            changeRequestRepository.save(request);
            regressionCaseRepository.save(new RegressionCase(UnitId.generate(), content,
                    signal.unitId(), false, request.id()));
            drafted.add(request.id());
        }
        return drafted;
    }

    private static ChangeRequestType draftType(AttributionCode code) {
        return switch (code) {
            case BAD_CLAIM -> ChangeRequestType.REVISE;
            case STALE -> ChangeRequestType.DEPRECATE;
            case NO_HIT -> ChangeRequestType.REBIND;
            default -> null;
        };
    }
}