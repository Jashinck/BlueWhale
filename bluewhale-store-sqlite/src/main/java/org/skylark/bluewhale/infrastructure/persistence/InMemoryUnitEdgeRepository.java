package org.skylark.bluewhale.infrastructure.persistence;

import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.graph.EdgeType;
import org.skylark.bluewhale.domain.graph.UnitEdge;
import org.skylark.bluewhale.domain.port.UnitEdgeRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "bluewhale.store.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryUnitEdgeRepository implements UnitEdgeRepository {

    private final ConcurrentHashMap<String, UnitEdge> store = new ConcurrentHashMap<>();

    @Override
    public void save(UnitEdge edge) {
        store.put(key(edge), edge);
    }

    @Override
    public List<UnitEdge> findByFrom(UnitId from) {
        return store.values().stream()
                .filter(edge -> edge.from().equals(from))
                .toList();
    }

    @Override
    public List<UnitEdge> findByTo(UnitId to) {
        return store.values().stream()
                .filter(edge -> edge.to().equals(to))
                .toList();
    }

    @Override
    public List<UnitEdge> findByFrom(UnitId from, EdgeType type) {
        return store.values().stream()
                .filter(edge -> edge.from().equals(from) && edge.type() == type)
                .toList();
    }

    private static String key(UnitEdge edge) {
        return edge.from() + "|" + edge.to() + "|" + edge.type() + "|" + edge.validFrom();
    }
}