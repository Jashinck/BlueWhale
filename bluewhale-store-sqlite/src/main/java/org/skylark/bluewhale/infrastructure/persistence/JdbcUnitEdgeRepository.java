package org.skylark.bluewhale.infrastructure.persistence;

import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.graph.EdgeType;
import org.skylark.bluewhale.domain.graph.UnitEdge;
import org.skylark.bluewhale.domain.port.UnitEdgeRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
@ConditionalOnProperty(name = "bluewhale.store.mode", havingValue = "sqlite")
public class JdbcUnitEdgeRepository implements UnitEdgeRepository {

    private static final String COLUMNS = "from_id, to_id, edge_type, valid_from, valid_to, weight";

    private final JdbcTemplate jdbc;
    private final RowMapper<UnitEdge> rowMapper = this::map;

    public JdbcUnitEdgeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(UnitEdge edge) {
        jdbc.update("""
                INSERT INTO bw_unit_edge (from_id, to_id, edge_type, valid_from, valid_to, weight)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(from_id, to_id, edge_type, valid_from)
                DO UPDATE SET valid_to = excluded.valid_to, weight = excluded.weight
                """,
                edge.from().value().toString(),
                edge.to().value().toString(),
                edge.type().name(),
                Timestamp.from(edge.validFrom()),
                edge.validTo() == null ? null : Timestamp.from(edge.validTo()),
                edge.weight());
    }

    @Override
    public List<UnitEdge> findByFrom(UnitId from) {
        return jdbc.query("SELECT " + COLUMNS + " FROM bw_unit_edge WHERE from_id = ?",
                rowMapper, from.value().toString());
    }

    @Override
    public List<UnitEdge> findByTo(UnitId to) {
        return jdbc.query("SELECT " + COLUMNS + " FROM bw_unit_edge WHERE to_id = ?",
                rowMapper, to.value().toString());
    }

    @Override
    public List<UnitEdge> findByFrom(UnitId from, EdgeType type) {
        return jdbc.query("SELECT " + COLUMNS + " FROM bw_unit_edge WHERE from_id = ? AND edge_type = ?",
                rowMapper, from.value().toString(), type.name());
    }

    private UnitEdge map(ResultSet rs, int rowNum) throws SQLException {
        return new UnitEdge(
                UnitId.of(rs.getString("from_id")),
                UnitId.of(rs.getString("to_id")),
                EdgeType.valueOf(rs.getString("edge_type")),
                instant(rs, "valid_from"),
                instant(rs, "valid_to"),
                rs.getDouble("weight"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}