package org.skylark.bluewhale.domain.knowledge;

import java.util.List;

public record GateReport(ValidationOutcome outcome, List<String> reasons) {

    public GateReport {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    public static GateReport accept() {
        return new GateReport(ValidationOutcome.ACCEPT, List.of());
    }

    public static GateReport of(ValidationOutcome outcome, List<String> reasons) {
        return new GateReport(outcome, reasons);
    }

    public boolean isAccepted() {
        return outcome == ValidationOutcome.ACCEPT;
    }
}