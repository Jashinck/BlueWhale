package org.skylark.bluewhale.application;

import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.governance.ChangeRequest;
import org.skylark.bluewhale.domain.port.ChangeRequestRepository;
import org.skylark.bluewhale.domain.port.ClockPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class GovernReviewUseCase {

    private final ChangeRequestRepository changeRequestRepository;
    private final ChangeApplier changeApplier;
    private final ClockPort clock;

    public GovernReviewUseCase(ChangeRequestRepository changeRequestRepository,
                               ChangeApplier changeApplier, ClockPort clock) {
        this.changeRequestRepository = changeRequestRepository;
        this.changeApplier = changeApplier;
        this.clock = clock;
    }

    @Transactional
    public GovernResult decide(ChangeRequestDecision decision) {
        ChangeRequest request = changeRequestRepository.findById(decision.crId())
                .orElseThrow(() -> new NoSuchElementException(
                        "change request not found: " + decision.crId()));
        var now = clock.now();
        ChangeRequest next = switch (decision.action()) {
            case SUBMIT -> request.submitForReview(decision.actor(), now);
            case APPROVE -> request.approve(decision.actor(), now);
            case REGRESS -> request.startRegression(decision.actor(), now);
            case READY -> request.markReady(decision.actor(), now, decision.regressionRunId());
            case RELEASE -> request.release(decision.actor(), now);
            case REJECT -> request.reject(decision.actor(), now);
            case ROLLBACK -> request.rollback(decision.actor(), now);
        };
        UnitId appliedUnit = next.isReleased() ? changeApplier.apply(next, decision.actor()) : null;
        changeRequestRepository.save(next);
        return new GovernResult(next.id().toString(), next.status(),
                appliedUnit == null ? null : appliedUnit.toString());
    }
}
