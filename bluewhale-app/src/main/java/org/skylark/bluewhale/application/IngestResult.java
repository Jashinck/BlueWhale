package org.skylark.bluewhale.application;

import org.skylark.bluewhale.domain.governance.ChangeRequestStatus;

public record IngestResult(String crId, ChangeRequestStatus status) {
}
