package org.skylark.bluewhale.infrastructure.index;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.skylark.bluewhale.domain.context.RepresentationLine;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.port.IndexPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "bluewhale.store.mode", havingValue = "sqlite")
public class SqliteIndexAdapter implements IndexPort {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public SqliteIndexAdapter(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public void index(IndexEntry entry) {
        Scope scope = entry.scope();
        jdbc.update("""
                INSERT INTO bw_index_entry (
                    unit_id, representation_line, tenant, workspace, project, agent_id,
                    user_id, session_id, text, metadata, embedding
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (unit_id, representation_line) DO UPDATE SET
                    tenant = excluded.tenant,
                    workspace = excluded.workspace,
                    project = excluded.project,
                    agent_id = excluded.agent_id,
                    user_id = excluded.user_id,
                    session_id = excluded.session_id,
                    text = excluded.text,
                    metadata = excluded.metadata,
                    embedding = excluded.embedding,
                    updated_at = CURRENT_TIMESTAMP
                """,
                entry.unitId().value().toString(), entry.line().name(), scope.tenant(),
                scope.workspace(), scope.project(), scope.agentId(), scope.userId(),
                scope.sessionId(), entry.text(), json(entry.metadata()),
                bytes(entry.embedding()));
        upsertFts(entry);
    }

    @Override
    public void invalidate(UnitId id) {
        String unitId = id.value().toString();
        jdbc.update("DELETE FROM bw_index_entry WHERE unit_id = ?", unitId);
        jdbc.update("DELETE FROM bw_index_fts WHERE unit_id = ?", unitId);
    }

    @Override
    public List<Candidate> dense(Scope queryScope, float[] queryVector, int topK) {
        if (queryVector == null || queryVector.length == 0 || topK <= 0) {
            return List.of();
        }
        return entries(queryScope).stream()
                .map(entry -> new Candidate(entry.unitId(), entry.line(),
                        cosine(queryVector, entry.embedding()), entry.metadata(),
                        entry.embedding()))
                .sorted(Comparator.comparingDouble(Candidate::score).reversed())
                .limit(topK)
                .toList();
    }

    @Override
    public List<Candidate> lexical(Scope queryScope, String query, int topK) {
        if (query == null || query.isBlank() || topK <= 0) {
            return List.of();
        }
        List<Candidate> ftsHits = lexicalViaFts(queryScope, query, topK);
        return ftsHits.isEmpty() ? lexicalViaLike(queryScope, query, topK) : ftsHits;
    }

    private List<Candidate> lexicalViaFts(Scope queryScope, String query, int topK) {
        if (query.trim().length() < 3) {
            return List.of();
        }
        try {
            List<String> matched = jdbc.query(
                    "SELECT unit_id FROM bw_index_fts WHERE bw_index_fts MATCH ?",
                    (rs, rowNum) -> rs.getString("unit_id"),
                    "\"" + query.replace("\"", "\"\"") + "\"");
            if (matched.isEmpty()) {
                return List.of();
            }
            java.util.Set<String> matchedIds = new java.util.HashSet<>(matched);
            String needle = query.toLowerCase(Locale.ROOT);
            return entries(queryScope).stream()
                    .filter(entry -> matchedIds.contains(entry.unitId().toString()))
                    .map(entry -> new Candidate(entry.unitId(), entry.line(),
                            lexicalScore(entry.text(), needle), entry.metadata(),
                            entry.embedding()))
                    .filter(candidate -> candidate.score() > 0)
                    .sorted(Comparator.comparingDouble(Candidate::score).reversed())
                    .limit(topK)
                    .toList();
        } catch (RuntimeException ftsUnavailable) {
            return List.of();
        }
    }

    private List<Candidate> lexicalViaLike(Scope queryScope, String query, int topK) {
        String needle = query.toLowerCase(Locale.ROOT);
        return entries(queryScope).stream()
                .map(entry -> new Candidate(entry.unitId(), entry.line(),
                        lexicalScore(entry.text(), needle), entry.metadata(),
                        entry.embedding()))
                .filter(candidate -> candidate.score() > 0)
                .sorted(Comparator.comparingDouble(Candidate::score).reversed())
                .limit(topK)
                .toList();
    }

    private void upsertFts(IndexEntry entry) {
        String unitId = entry.unitId().value().toString();
        String line = entry.line().name();
        jdbc.update("DELETE FROM bw_index_fts WHERE unit_id = ? AND representation_line = ?",
                unitId, line);
        jdbc.update("INSERT INTO bw_index_fts (text, unit_id, representation_line) VALUES (?, ?, ?)",
                entry.text(), unitId, line);
    }

    private List<Entry> entries(Scope queryScope) {
        return jdbc.query("""
                        SELECT unit_id, representation_line, tenant, workspace, project, agent_id,
                               user_id, session_id, text, metadata, embedding
                        FROM bw_index_entry
                        WHERE tenant = ?
                        """,
                (rs, rowNum) -> new Entry(
                        new UnitId(java.util.UUID.fromString(rs.getString("unit_id"))),
                        RepresentationLine.valueOf(rs.getString("representation_line")),
                        new Scope(rs.getString("tenant"), rs.getString("workspace"),
                                rs.getString("project"), rs.getString("agent_id"),
                                rs.getString("user_id"), rs.getString("session_id")),
                        rs.getString("text"),
                        metadata(rs.getString("metadata")),
                        floats(rs.getBytes("embedding"))),
                queryScope.tenant()).stream()
                .filter(entry -> queryScope.canSee(entry.scope()))
                .toList();
    }

    private String json(Map<String, String> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("cannot serialize index metadata", e);
        }
    }

    private Map<String, String> metadata(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("cannot deserialize index metadata", e);
        }
    }

    private static byte[] bytes(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(embedding.length * Float.BYTES);
        for (float value : embedding) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }

    private static float[] floats(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new float[0];
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        float[] embedding = new float[bytes.length / Float.BYTES];
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] = buffer.getFloat();
        }
        return embedding;
    }

    private static double lexicalScore(String text, String needle) {
        String normalized = text.toLowerCase(Locale.ROOT);
        if (normalized.contains(needle)) {
            return 1.0;
        }
        long matched = List.of(needle.split("\\s+")).stream()
                .filter(token -> !token.isBlank() && normalized.contains(token))
                .count();
        return matched == 0 ? 0.0 : Math.min(0.9, matched * 0.2);
    }

    private static double cosine(float[] left, float[] right) {
        if (left == null || right == null || left.length != right.length || left.length == 0) {
            return 0.0;
        }
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < left.length; i++) {
            dot += (double) left[i] * right[i];
            leftNorm += (double) left[i] * left[i];
            rightNorm += (double) right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private record Entry(UnitId unitId, RepresentationLine line, Scope scope, String text,
                         Map<String, String> metadata, float[] embedding) {
    }
}
