package org.skylark.bluewhale.infrastructure.persistence;

import org.skylark.bluewhale.domain.context.ActorType;
import org.skylark.bluewhale.domain.context.LifecycleEvent;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.context.UnitState;
import org.skylark.bluewhale.domain.port.LifecycleEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "bluewhale.store.mode", havingValue = "sqlite")
public class JdbcLifecycleEventRepository implements LifecycleEventRepository {

    private final JdbcTemplate jdbc;

    public JdbcLifecycleEventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void append(LifecycleEvent event) {
        jdbc.update("""
                INSERT INTO bw_lifecycle_event
                    (id, unit_id, from_state, to_state, trigger, actor_id, actor_type, at, reason, batch_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                event.eventId().toString(), event.unitId().value().toString(),
                event.fromState() == null ? null : event.fromState().name(),
                event.toState().name(), event.trigger(), event.actorId(),
                event.actorType() == null ? null : event.actorType().name(),
                Timestamp.from(event.occurredAt()), event.reason(),
                event.batchId() == null ? null : event.batchId().toString());
    }

    @Override
    public List<LifecycleEvent> findByUnitId(UnitId unitId) {
        return jdbc.query("""
                        SELECT id, unit_id, from_state, to_state, trigger, actor_id, actor_type,
                               at, reason, batch_id
                        FROM bw_lifecycle_event
                        WHERE unit_id = ?
                        ORDER BY at
                        """,
                (rs, rowNum) -> new LifecycleEvent(
                        UUID.fromString(rs.getString("id")),
                        new UnitId(UUID.fromString(rs.getString("unit_id"))),
                        rs.getString("from_state") == null ? null
                                : UnitState.valueOf(rs.getString("from_state")),
                        UnitState.valueOf(rs.getString("to_state")),
                        rs.getString("trigger"),
                        rs.getString("actor_id"),
                        rs.getString("actor_type") == null ? null
                                : ActorType.valueOf(rs.getString("actor_type")),
                        rs.getTimestamp("at").toInstant(),
                        rs.getString("reason"),
                        rs.getString("batch_id") == null ? null
                                : UUID.fromString(rs.getString("batch_id"))),
                unitId.value().toString());
    }
}
