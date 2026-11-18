package org.skylark.bluewhale.application;

import org.skylark.bluewhale.domain.context.UnitId;

public record RegressionReport(UnitId runId, int passed, int failed) {

    public boolean allPassed() {
        return failed == 0;
    }
}