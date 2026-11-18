package org.skylark.bluewhale.infrastructure.persistence;

import org.skylark.bluewhale.domain.governance.RegressionCase;
import org.skylark.bluewhale.domain.port.RegressionCaseRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
@ConditionalOnProperty(name = "bluewhale.store.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryRegressionCaseRepository implements RegressionCaseRepository {

    private final CopyOnWriteArrayList<RegressionCase> store = new CopyOnWriteArrayList<>();

    @Override
    public void save(RegressionCase regressionCase) {
        store.add(regressionCase);
    }

    @Override
    public List<RegressionCase> findAll() {
        return List.copyOf(store);
    }
}