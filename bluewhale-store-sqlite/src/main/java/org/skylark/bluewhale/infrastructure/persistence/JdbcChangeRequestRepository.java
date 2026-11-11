package org.skylark.bluewhale.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.governance.ChangeRequest;
import org.skylark.bluewhale.domain.governance.ChangeRequestStatus;
import org.skylark.bluewhale.domain.governance.ChangeRequestType;
import org.skylark.bluewhale.domain.governance.KnowledgePatch;
import org.skylark.bluewhale.domain.port.ChangeRequestRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "bluewhale.store.mode", havingValue = "sqlite")
public class JdbcChangeRequestRepository implements ChangeRequestRepository {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public JdbcChangeRequestRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(ChangeRequest request) {
        int updated = jdbc.update("""
                UPDATE bw_change_request SET
                    type = ?, status = ?, author = ?, patch_ref = ?, attribution_ids = ?,
                    rollback_of = ?, regression_run_id = ?, updated_at = ?
                WHERE cr_id = ?
                """,
                request.type().name(), request.status().name(), request.author(),
                json(request.patch()), "[]",
                request.rollbackOf() == null ? null : request.rollbackOf().value().toString(),
                request.regressionRunId() == null ? null : request.regressionRunId().value().toString(),
                Timestamp.from(request.updatedAt()),
                request.id().value().toString());
        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO bw_change_request
                        (cr_id, type, status, author, patch_ref, attribution_ids, rollback_of,
                         regression_run_id, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    request.id().value().toString(), request.type().name(), request.status().name(),
                    request.author(), json(request.patch()), "[]",
                    request.rollbackOf() == null ? null : request.rollbackOf().value().toString(),
                    request.regressionRunId() == null ? null : request.regressionRunId().value().toString(),
                    Timestamp.from(request.createdAt()), Timestamp.from(request.updatedAt()));
        }
    }

    @Override
    public Optional<ChangeRequest> findById(UnitId id) {
        return jdbc.query("""
                        SELECT cr_id, type, status, author, patch_ref, rollback_of,
                               regression_run_id, created_at, updated_at
                        FROM bw_change_request
                        WHERE cr_id = ?
                        """,
                (rs, rowNum) -> map(rs.getString("cr_id"), rs.getString("type"),
                        rs.getString("status"), rs.getString("author"), rs.getString("patch_ref"),
                        rs.getString("rollback_of"), rs.getString("regression_run_id"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant()),
                id.value().toString()).stream().findFirst();
    }

    @Override
    public List<ChangeRequest> findByStatus(ChangeRequestStatus status) {
        return jdbc.query("""
                        SELECT cr_id, type, status, author, patch_ref, rollback_of,
                               regression_run_id, created_at, updated_at
                        FROM bw_change_request
                        WHERE status = ?
                        """,
                (rs, rowNum) -> map(rs.getString("cr_id"), rs.getString("type"),
                        rs.getString("status"), rs.getString("author"), rs.getString("patch_ref"),
                        rs.getString("rollback_of"), rs.getString("regression_run_id"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant()),
                status.name());
    }

    private ChangeRequest map(String crId, String type, String status, String author,
                              String patchRef, String rollbackOf, String regressionRunId,
                              java.time.Instant createdAt, java.time.Instant updatedAt) {
        try {
            KnowledgePatch patch = objectMapper.readValue(patchRef, KnowledgePatch.class);
            return new ChangeRequest(new UnitId(UUID.fromString(crId)),
                    ChangeRequestType.valueOf(type), ChangeRequestStatus.valueOf(status),
                    author, patch,
                    regressionRunId == null ? null : new UnitId(UUID.fromString(regressionRunId)),
                    rollbackOf == null ? null : new UnitId(UUID.fromString(rollbackOf)),
                    createdAt, updatedAt);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("cannot deserialize change request patch", e);
        }
    }

    private String json(KnowledgePatch patch) {
        try {
            return objectMapper.writeValueAsString(patch);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("cannot serialize change request patch", e);
        }
    }
}
