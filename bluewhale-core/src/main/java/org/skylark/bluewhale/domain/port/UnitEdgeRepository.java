package org.skylark.bluewhale.domain.port;

import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.graph.EdgeType;
import org.skylark.bluewhale.domain.graph.UnitEdge;

import java.util.List;

public interface UnitEdgeRepository {

    void save(UnitEdge edge);

    List<UnitEdge> findByFrom(UnitId from);

    List<UnitEdge> findByTo(UnitId to);

    List<UnitEdge> findByFrom(UnitId from, EdgeType type);
}