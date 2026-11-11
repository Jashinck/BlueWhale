package org.skylark.bluewhale.infrastructure;

import org.skylark.bluewhale.application.UsageEventRecorder;
import org.skylark.bluewhale.domain.feedback.UsageEvent;
import org.skylark.bluewhale.domain.port.UsageEventRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AsyncUsageEventRecorder implements UsageEventRecorder {

    private final UsageEventRepository usageEventRepository;

    public AsyncUsageEventRecorder(UsageEventRepository usageEventRepository) {
        this.usageEventRepository = usageEventRepository;
    }

    @Override
    @Async("usageExecutor")
    public void record(List<UsageEvent> events) {
        events.forEach(usageEventRepository::append);
    }
}
