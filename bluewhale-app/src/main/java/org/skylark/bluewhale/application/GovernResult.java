package org.skylark.bluewhale.application;

import org.skylark.bluewhale.domain.governance.ChangeRequestStatus;

public record GovernResult(String crId, ChangeRequestStatus status, String appliedUnitId) {
}
