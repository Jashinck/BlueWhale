package org.skylark.bluewhale.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.skylark.bluewhale.domain.context.ContextUnit;
import org.skylark.bluewhale.domain.context.Economy;
import org.skylark.bluewhale.domain.context.Lifecycle;
import org.skylark.bluewhale.domain.context.Lineage;
import org.skylark.bluewhale.domain.context.Origin;
import org.skylark.bluewhale.domain.context.Ownership;
import org.skylark.bluewhale.domain.context.Provenance;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.Trust;
import org.skylark.bluewhale.domain.context.UnitContent;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.context.UnitKind;
import org.skylark.bluewhale.domain.context.UnitState;
import org.skylark.bluewhale.domain.port.ContextUnitRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(name = "bluewhale.store.mode", havingValue = "sqlite")
public class JdbcContextUnitRepository implements ContextUnitRepository {

    private static final String COLUMNS = """
            id, tenant, workspace, project, agent_id, user_id, session_id,
            kind, origin, state, l0_abstract, l1_overview, l2_inline, l2_ref,
            confidence, authority, importance, cost_tokens, n_seen, n_cited, n_adopted, n_corrected,
            valid_from, valid_to, learned_at, superseded_at, source_published_at, captured_at,
            review_due_at, retention_until, erased_at, superseded_by,
            owner_id, owner_org, review_cycle_days, legal_hold, hold_reason,
            episode_id, source_ref, span_start, span_end, ontology_version, extractor_version,
            lineage_parents, needs_reextract, tags, content_hash
            """;

    private static final String UPDATE_SET = """
            UPDATE bw_context_unit SET
                tenant = ?, scope_path = ?, workspace = ?, project = ?, agent_id = ?, user_id = ?, session_id = ?,
                kind = ?, origin = ?, state = ?, l0_abstract = ?, l1_overview = ?, l2_inline = ?, l2_ref = ?,
                confidence = ?, authority = ?, importance = ?, cost_tokens = ?, n_seen = ?, n_cited = ?,
                n_adopted = ?, n_corrected = ?,
                valid_from = ?, valid_to = ?, learned_at = ?, superseded_at = ?, source_published_at = ?,
                captured_at = ?, review_due_at = ?, retention_until = ?, erased_at = ?, superseded_by = ?,
                owner_id = ?, owner_org = ?, review_cycle_days = ?, legal_hold = ?, hold_reason = ?,
                episode_id = ?, source_ref = ?, span_start = ?, span_end = ?, ontology_version = ?,
                extractor_version = ?, lineage_parents = ?, needs_reextract = ?, tags = ?, content_hash = ?,
                updated_at = CURRENT_TIMESTAMP
            """;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final RowMapper<ContextUnit> rowMapper = this::map;

    public JdbcContextUnitRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(ContextUnit unit) {
        List<Object> values = values(unit);
        String id = unit.id().value().toString();
        int updated = jdbc.update(UPDATE_SET + " WHERE id = ?", withIdAtEnd(values, id));
        if (updated == 0) {
            Object[] args = withIdAtEnd(values, id);
            String placeholders = String.join(", ", Collections.nCopies(args.length, "?"));
            jdbc.update("""
                    INSERT INTO bw_context_unit (
                        tenant, scope_path, workspace, project, agent_id, user_id, session_id,
                        kind, origin, state, l0_abstract, l1_overview, l2_inline, l2_ref,
                        confidence, authority, importance, cost_tokens, n_seen, n_cited, n_adopted, n_corrected,
                        valid_from, valid_to, learned_at, superseded_at, source_published_at, captured_at,
                        review_due_at, retention_until, erased_at, superseded_by,
                        owner_id, owner_org, review_cycle_days, legal_hold, hold_reason,
                        episode_id, source_ref, span_start, span_end, ontology_version, extractor_version,
                        lineage_parents, needs_reextract, tags, content_hash, id
                    ) VALUES (%s)
                    """.formatted(placeholders), args);
        }
    }

    @Override
    public boolean updateIfState(UnitId id, UnitState expected, ContextUnit unit) {
        List<Object> args = new ArrayList<>(values(unit));
        args.add(id.value().toString());
        args.add(expected.name());
        return jdbc.update(UPDATE_SET + " WHERE id = ? AND state = ?", args.toArray()) == 1;
    }

    @Override
    public Optional<ContextUnit> findById(UnitId id) {        List<ContextUnit> found = jdbc.query("SELECT " + COLUMNS
                + " FROM bw_context_unit WHERE id = ?", rowMapper, id.value().toString());
        return found.stream().findFirst();
    }

    @Override
    public List<ContextUnit> findByIds(Collection<UnitId> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        String placeholders = ids.stream().map(ignored -> "?")
                .collect(Collectors.joining(", "));
        Object[] args = ids.stream().map(id -> id.value().toString()).toArray();
        return jdbc.query("SELECT " + COLUMNS + " FROM bw_context_unit WHERE id IN ("
                + placeholders + ")", rowMapper, args);
    }

    @Override
    public List<ContextUnit> findByScope(Scope scope, Set<UnitState> states) {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS)
                .append(" FROM bw_context_unit WHERE tenant = ?");
        List<Object> args = new ArrayList<>();
        args.add(scope.tenant());
        if (states != null && !states.isEmpty()) {
            sql.append(" AND state IN (")
                    .append(states.stream().map(ignored -> "?").collect(Collectors.joining(", ")))
                    .append(")");
            states.forEach(state -> args.add(state.name()));
        }
        return jdbc.query(sql.toString(), rowMapper, args.toArray()).stream()
                .filter(unit -> scope.canSee(unit.scope()))
                .toList();
    }

    private List<Object> values(ContextUnit unit) {
        Scope scope = unit.scope();
        Lifecycle lifecycle = unit.lifecycle();
        Economy economy = unit.economy();
        Ownership ownership = unit.ownership();
        Provenance provenance = unit.provenance();
        List<Object> values = new ArrayList<>();
        values.add(scope.tenant());
        values.add(scope.path());
        values.add(scope.workspace());
        values.add(scope.project());
        values.add(scope.agentId());
        values.add(scope.userId());
        values.add(scope.sessionId());
        values.add(unit.kind().name());
        values.add(unit.origin().name());
        values.add(lifecycle.state().name());
        values.add(unit.content().abstractText());
        values.add(unit.content().overview());
        values.add(unit.content().inlineText());
        values.add(unit.content().ref());
        values.add(unit.trust().confidence());
        values.add(unit.trust().authority());
        values.add(economy.importance());
        values.add(economy.costTokens());
        values.add(economy.nSeen());
        values.add(economy.nCited());
        values.add(economy.nAdopted());
        values.add(economy.nCorrected());
        values.add(timestamp(lifecycle.validFrom()));
        values.add(timestamp(lifecycle.validTo()));
        values.add(timestamp(lifecycle.learnedAt()));
        values.add(timestamp(lifecycle.supersededAt()));
        values.add(timestamp(lifecycle.sourcePublishedAt()));
        values.add(timestamp(lifecycle.capturedAt()));
        values.add(timestamp(lifecycle.reviewDueAt()));
        values.add(timestamp(lifecycle.retentionUntil()));
        values.add(timestamp(lifecycle.erasedAt()));
        values.add(unit.supersededBy() == null ? null : unit.supersededBy().value().toString());
        values.add(ownership.ownerId());
        values.add(ownership.ownerOrg());
        values.add(ownership.reviewCycleDays());
        values.add(ownership.legalHold());
        values.add(ownership.holdReason());
        values.add(provenance.episodeId() == null ? null : provenance.episodeId().value().toString());
        values.add(provenance.sourceRef());
        values.add(provenance.spanStart());
        values.add(provenance.spanEnd());
        values.add(provenance.ontologyVersion());
        values.add(provenance.extractorVersion());
        values.add(json(unit.lineage().derivedFrom().stream().map(id -> id.value().toString()).toList()));
        values.add(unit.lineage().needsReextract());
        values.add(json(unit.tags()));
        values.add(unit.contentHash());
        return values;
    }

    private static Object[] withIdAtEnd(List<Object> values, String id) {
        List<Object> args = new ArrayList<>(values);
        args.add(id);
        return args.toArray();
    }

    private ContextUnit map(ResultSet rs, int rowNum) throws SQLException {
        Scope scope = new Scope(rs.getString("tenant"), rs.getString("workspace"),
                rs.getString("project"), rs.getString("agent_id"), rs.getString("user_id"),
                rs.getString("session_id"));
        Lifecycle lifecycle = new Lifecycle(
                UnitState.valueOf(rs.getString("state")),
                instant(rs, "valid_from"), instant(rs, "valid_to"),
                instant(rs, "learned_at"), instant(rs, "superseded_at"),
                instant(rs, "source_published_at"), instant(rs, "captured_at"),
                instant(rs, "review_due_at"), instant(rs, "retention_until"),
                instant(rs, "erased_at"));
        Ownership ownership = new Ownership(rs.getString("owner_id"),
                rs.getString("owner_org"), integer(rs, "review_cycle_days"),
                rs.getBoolean("legal_hold"), rs.getString("hold_reason"));
        UUID episodeId = uuid(rs, "episode_id");
        UUID supersededBy = uuid(rs, "superseded_by");
        Provenance provenance = new Provenance(
                episodeId == null ? null : new UnitId(episodeId), rs.getString("source_ref"),
                integer(rs, "span_start"), integer(rs, "span_end"),
                rs.getString("ontology_version"), rs.getString("extractor_version"));
        List<UnitId> parents = strings(rs.getString("lineage_parents")).stream()
                .map(UnitId::of)
                .toList();
        return ContextUnit.restore(
                new UnitId(uuidRequired(rs, "id")),
                scope,
                UnitKind.valueOf(rs.getString("kind")),
                Origin.valueOf(rs.getString("origin")),
                new UnitContent(rs.getString("l0_abstract"), rs.getString("l1_overview"),
                        rs.getString("l2_inline"), rs.getString("l2_ref")),
                lifecycle,
                ownership,
                new Trust(rs.getDouble("confidence"), rs.getInt("authority")),
                new Economy(rs.getInt("cost_tokens"), rs.getInt("n_seen"), rs.getInt("n_cited"),
                        rs.getInt("n_adopted"), rs.getInt("n_corrected"), rs.getDouble("importance")),
                provenance,
                new Lineage(parents, rs.getBoolean("needs_reextract")),
                supersededBy == null ? null : new UnitId(supersededBy),
                strings(rs.getString("tags")));
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("cannot serialize context unit facet", e);
        }
    }

    private List<String> strings(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("cannot deserialize context unit facet", e);
        }
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static Integer integer(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).intValue();
    }

    private static UUID uuid(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value == null ? null : UUID.fromString(value);
    }

    private static UUID uuidRequired(ResultSet rs, String column) throws SQLException {
        UUID value = uuid(rs, column);
        if (value == null) {
            throw new SQLException("required UUID column is null: " + column);
        }
        return value;
    }
}
