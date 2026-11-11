package org.skylark.bluewhale.domain.port;

import java.time.Instant;

public interface ClockPort {
    Instant now();
}
