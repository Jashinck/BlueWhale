package org.skylark.bluewhale.application;

import org.skylark.bluewhale.domain.feedback.UsageEvent;

import java.util.List;

public interface UsageEventRecorder {
    void record(List<UsageEvent> events);
}
