package org.skylark.bluewhale.application;

import org.skylark.bluewhale.domain.feedback.UsageEvent;
import org.skylark.bluewhale.domain.port.ClockPort;
import org.skylark.bluewhale.domain.port.ContextUnitRepository;
import org.skylark.bluewhale.domain.port.UsageEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RecordFeedbackUseCase {

    private final UsageEventRepository usageEventRepository;
    private final ContextUnitRepository unitRepository;
    private final ClockPort clock;

    public RecordFeedbackUseCase(UsageEventRepository usageEventRepository,
                                 ContextUnitRepository unitRepository, ClockPort clock) {
        this.usageEventRepository = usageEventRepository;
        this.unitRepository = unitRepository;
        this.clock = clock;
    }

    @Transactional
    public void record(FeedbackBatch batch) {
        Instant now = clock.now();
        for (FeedbackBatch.FeedbackItem item : batch.items()) {
            usageEventRepository.append(new UsageEvent(
                    item.unitId(), batch.traceId(), item.outcome(), item.tokens(), now));
            unitRepository.findById(item.unitId())
                    .ifPresent(unit -> unitRepository.save(unit.recordApplied(item.outcome())));
        }
    }
}
