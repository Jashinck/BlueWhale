package org.skylark.bluewhale.application;

import org.skylark.bluewhale.domain.feedback.UsageEvent;
import org.skylark.bluewhale.domain.port.UsageEventRepository;

import java.util.List;

public class SynchronousUsageEventRecorder implements UsageEventRecorder {

    private final UsageEventRepository usageEventRepository;

    public SynchronousUsageEventRecorder(UsageEventRepository usageEventRepository) {
        this.usageEventRepository = usageEventRepository;
    }

    @Override
    public void record(List<UsageEvent> events) {
        events.forEach(usageEventRepository::append);
    }
}
