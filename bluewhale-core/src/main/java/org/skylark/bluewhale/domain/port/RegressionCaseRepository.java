package org.skylark.bluewhale.domain.port;

import org.skylark.bluewhale.domain.governance.RegressionCase;

import java.util.List;

public interface RegressionCaseRepository {

    void save(RegressionCase regressionCase);

    List<RegressionCase> findAll();
}