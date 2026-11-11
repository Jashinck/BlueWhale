package org.skylark.bluewhale.infrastructure.persistence;

import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.memory.Episode;
import org.skylark.bluewhale.domain.port.EpisodeRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "bluewhale.store.mode", havingValue = "sqlite")
public class JdbcEpisodeRepository implements EpisodeRepository {

    private final JdbcTemplate jdbc;

    public JdbcEpisodeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void append(Episode episode) {
        jdbc.update("""
                INSERT INTO bw_episode (id, tenant, scope_path, occurred_at, source, payload)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                episode.id().value().toString(), episode.scope().tenant(), episode.scope().path(),
                Timestamp.from(episode.occurredAt()), episode.source(), episode.payload());
    }

    @Override
    public Optional<Episode> findById(UnitId id) {
        List<Episode> found = jdbc.query("""
                        SELECT id, tenant, scope_path, occurred_at, source, payload
                        FROM bw_episode WHERE id = ?
                        """,
                (rs, rowNum) -> new Episode(
                        new UnitId(java.util.UUID.fromString(rs.getString("id"))),
                        new Scope(rs.getString("tenant"), null, null, null, null, null),
                        rs.getTimestamp("occurred_at").toInstant(),
                        rs.getString("source"),
                        rs.getString("payload")),
                id.value().toString());
        return found.stream().findFirst();
    }
}
