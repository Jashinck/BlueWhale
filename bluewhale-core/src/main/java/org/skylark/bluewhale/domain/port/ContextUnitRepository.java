package org.skylark.bluewhale.domain.port;

import org.skylark.bluewhale.domain.context.ContextUnit;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.context.UnitState;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ContextUnitRepository {
    void save(ContextUnit unit);

    boolean updateIfState(UnitId id, UnitState expected, ContextUnit unit);

    Optional<ContextUnit> findById(UnitId id);

    List<ContextUnit> findByIds(Collection<UnitId> ids);

    List<ContextUnit> findByScope(Scope scope, Set<UnitState> states);
}
