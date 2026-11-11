package org.skylark.bluewhale.infrastructure.persistence;

import org.skylark.bluewhale.domain.context.ContextUnit;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.context.UnitState;
import org.skylark.bluewhale.domain.port.ContextUnitRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "bluewhale.store.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryContextUnitRepository implements ContextUnitRepository {

    private final ConcurrentHashMap<UnitId, ContextUnit> store = new ConcurrentHashMap<>();

    @Override
    public void save(ContextUnit unit) {
        store.put(unit.id(), unit);
    }

    @Override
    public boolean updateIfState(UnitId id, UnitState expected, ContextUnit unit) {
        boolean[] updated = {false};
        store.compute(id, (key, existing) -> {
            if (existing == null || existing.lifecycle().state() != expected) {
                return existing;
            }
            updated[0] = true;
            return unit;
        });
        return updated[0];
    }

    @Override
    public Optional<ContextUnit> findById(UnitId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<ContextUnit> findByIds(Collection<UnitId> ids) {
        return ids.stream()
                .map(store::get)
                .filter(unit -> unit != null)
                .toList();
    }

    @Override
    public List<ContextUnit> findByScope(Scope scope, Set<UnitState> states) {
        return store.values().stream()
                .filter(unit -> scope.canSee(unit.scope()))
                .filter(unit -> states == null || states.contains(unit.lifecycle().state()))
                .sorted(Comparator.comparing(unit -> unit.id().toString()))
                .toList();
    }
}
