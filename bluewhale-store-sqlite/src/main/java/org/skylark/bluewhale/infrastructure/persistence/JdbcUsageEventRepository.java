package org.skylark.bluewhale.infrastructure.persistence;

import org.skylark.bluewhale.domain.context.Outcome;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.feedback.UsageEvent;
import org.skylark.bluewhale.domain.port.UsageEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "bluewhale.store.mode", havingValue = "sqlite")
public class JdbcUsageEventRepository implements UsageEventRepository {

    private final JdbcTemplate jdbc;

    public JdbcUsageEventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void append(UsageEvent event) {
        jdbc.update("""
                INSERT INTO bw_usage_event (unit_id, trace_id, event, at, tokens, outcome)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                event.unitId().value().toString(), event.traceId(), "USAGE",
                Timestamp.from(event.at()), event.tokens(), event.outcome().name());
    }

    @Override
    public List<UsageEvent> findByUnitId(UnitId unitId) {
        return jdbc.query("""
                        SELECT unit_id, trace_id, outcome, tokens, at
                        FROM bw_usage_event
                        WHERE unit_id = ?
                        ORDER BY at
                        """,
                (rs, rowNum) -> new UsageEvent(
                        new UnitId(UUID.fromString(rs.getString("unit_id"))),
                        rs.getString("trace_id"),
                        Outcome.valueOf(rs.getString("outcome")),
                        rs.getInt("tokens"),
                        rs.getTimestamp("at").toInstant()),
                unitId.value().toString());
    }
}
