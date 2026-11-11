package org.skylark.bluewhale.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.skylark.bluewhale.domain.context.Actor;
import org.skylark.bluewhale.domain.context.ActorType;
import org.skylark.bluewhale.domain.context.ContextUnit;
import org.skylark.bluewhale.domain.context.Economy;
import org.skylark.bluewhale.domain.context.Lifecycle;
import org.skylark.bluewhale.domain.context.LifecycleEvent;
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
import org.skylark.bluewhale.domain.governance.ChangeRequest;
import org.skylark.bluewhale.domain.governance.ChangeRequestStatus;
import org.skylark.bluewhale.domain.governance.ChangeRequestType;
import org.skylark.bluewhale.domain.governance.KnowledgePatch;
import org.skylark.bluewhale.domain.memory.Episode;
import org.skylark.bluewhale.domain.port.IndexPort;
import org.skylark.bluewhale.infrastructure.index.SqliteIndexAdapter;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcRepositoryPersistenceTest {

    private static final Instant NOW = Instant.parse("2026-11-09T08:00:00Z");

    @Test
    void shouldPersistAndRestoreAcrossRepositoryInstances() throws Exception {
        String url = databaseUrl();
        DataSource dataSource = dataSource(url);
        initialize(dataSource);
        JdbcContextUnitRepository first = repository(dataSource);
        ContextUnit active = unit(Scope.of("acme", "ws-1", "project-a", "agent-1",
                "user-1", "session-1"), "dark theme", UnitState.ACTIVE);

        first.save(active);

        JdbcContextUnitRepository reopened = repository(dataSource(url));
        ContextUnit restored = reopened.findById(active.id()).orElseThrow();

        assertEquals(active.id(), restored.id());
        assertEquals(active.scope(), restored.scope());
        assertEquals("dark theme", restored.content().abstractText());
        assertEquals(UnitState.ACTIVE, restored.lifecycle().state());
        assertEquals(active.tags(), restored.tags());
        assertEquals(active.contentHash(), restored.contentHash());
        assertEquals(active.lineage().derivedFrom(), restored.lineage().derivedFrom());
    }

    @Test
    void shouldFilterByScopeAndStateAndAppendLifecycleEvidence() throws Exception {
        DataSource dataSource = dataSource(databaseUrl());
        initialize(dataSource);
        JdbcContextUnitRepository repository = repository(dataSource);
        JdbcLifecycleEventRepository events = new JdbcLifecycleEventRepository(new JdbcTemplate(dataSource));
        JdbcEpisodeRepository episodes = new JdbcEpisodeRepository(new JdbcTemplate(dataSource));
        Scope scope = Scope.of("acme", "ws-1", "project-a", "agent-1", "user-1", null);
        ContextUnit first = unit(scope, "first", UnitState.ACTIVE);
        ContextUnit second = unit(scope, "second", UnitState.STALE_SUSPECT);
        ContextUnit otherAgent = unit(Scope.of("acme", "ws-2", "project-b", "agent-2",
                "user-2", null), "other", UnitState.ACTIVE);
        repository.save(first);
        repository.save(second);
        repository.save(otherAgent);

        List<ContextUnit> found = repository.findByScope(
                Scope.of("acme", "ws-1", "project-a", "agent-1", "user-1", null),
                Set.of(UnitState.ACTIVE, UnitState.STALE_SUSPECT));

        assertEquals(2, found.size());

        LifecycleEvent event = new LifecycleEvent(UUID.randomUUID(), first.id(),
                UnitState.ACTIVE, UnitState.STALE_SUSPECT, "ACTIVE_TO_STALE_SUSPECT",
                "reviewer-1", ActorType.HUMAN, NOW, "source changed", null);
        events.append(event);
        Episode episode = Episode.capture(scope, "conversation", "captured evidence", NOW);
        episodes.append(episode);

        assertEquals(1, events.findByUnitId(first.id()).size());
        assertEquals("captured evidence", episodes.findById(episode.id()).orElseThrow().payload());
    }

    @Test
    void shouldKeepRecallIndexAcrossRepositoryRestart() throws Exception {
        String url = databaseUrl();
        DataSource dataSource = dataSource(url);
        initialize(dataSource);
        Scope scope = Scope.agent("acme", "agent-1");
        UnitId unitId = UnitId.generate();
        SqliteIndexAdapter first = new SqliteIndexAdapter(
                new JdbcTemplate(dataSource), new ObjectMapper());
        first.index(new IndexPort.IndexEntry(unitId, scope,
                org.skylark.bluewhale.domain.context.RepresentationLine.CURATED_DOC,
                "sqlite persisted index", new float[]{1, 0, 0}, java.util.Map.of("kind", "SEMANTIC")));

        SqliteIndexAdapter reopened = new SqliteIndexAdapter(
                new JdbcTemplate(dataSource(url)), new ObjectMapper());

        assertEquals(unitId, reopened.dense(scope, new float[]{1, 0, 0}, 5).get(0).unitId());
        assertEquals(unitId, reopened.lexical(scope, "persisted index", 5).get(0).unitId());
    }

    @Test
    void shouldUpdateTheSameAggregateInsteadOfDuplicatingRows() throws Exception {
        DataSource dataSource = dataSource(databaseUrl());
        initialize(dataSource);
        JdbcContextUnitRepository repository = repository(dataSource);
        ContextUnit active = unit(Scope.agent("acme", "agent-1"), "original", UnitState.ACTIVE);
        repository.save(active);
        ContextUnit stale = active.transition(
                org.skylark.bluewhale.domain.context.Transition.ACTIVE_TO_STALE_SUSPECT,
                new Actor("reviewer-1", ActorType.HUMAN), "stale", NOW, null).unit();

        repository.save(stale);

        assertEquals(UnitState.STALE_SUSPECT,
                repository.findById(active.id()).orElseThrow().lifecycle().state());
        Integer count = new JdbcTemplate(dataSource).queryForObject(
                "SELECT COUNT(*) FROM bw_context_unit", Integer.class);
        assertEquals(1, count);
    }

    @Test
    void shouldRejectStaleOptimisticStateUpdate() throws Exception {
        DataSource dataSource = dataSource(databaseUrl());
        initialize(dataSource);
        JdbcContextUnitRepository repository = repository(dataSource);
        ContextUnit active = unit(Scope.agent("acme", "agent-1"), "original", UnitState.ACTIVE);
        repository.save(active);
        ContextUnit stale = active.transition(
                org.skylark.bluewhale.domain.context.Transition.ACTIVE_TO_STALE_SUSPECT,
                new Actor("reviewer-1", ActorType.HUMAN), "stale", NOW, null).unit();

        assertTrue(repository.updateIfState(active.id(), UnitState.ACTIVE, stale));
        assertFalse(repository.updateIfState(active.id(), UnitState.ACTIVE, stale));
    }

    @Test
    void shouldPersistAndRestoreChangeRequest() throws Exception {
        DataSource dataSource = dataSource(databaseUrl());
        initialize(dataSource);
        JdbcChangeRequestRepository repository = new JdbcChangeRequestRepository(
                new JdbcTemplate(dataSource), new ObjectMapper());
        ChangeRequest draft = ChangeRequest.draft(ChangeRequestType.ADD, "author-1",
                new KnowledgePatch(Scope.of("acme", "ws-1", null, null, null, null),
                        "owner-1", "legal", "policy rule", "policy.md"), NOW);

        repository.save(draft);

        ChangeRequest restored = repository.findById(draft.id()).orElseThrow();
        assertEquals(draft.id(), restored.id());
        assertEquals("policy rule", restored.patch().content());
        assertEquals(ChangeRequestStatus.DRAFT, restored.status());
    }

    private static JdbcContextUnitRepository repository(DataSource dataSource) {
        return new JdbcContextUnitRepository(new JdbcTemplate(dataSource), new ObjectMapper());
    }

    private static DataSource dataSource(String url) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl(url);
        return dataSource;
    }

    private static void initialize(DataSource dataSource) {
        new ResourceDatabasePopulator(new ClassPathResource("db/sqlite/schema.sql"))
                .execute(dataSource);
    }

    private static String databaseUrl() throws Exception {
        Path directory = Files.createTempDirectory("bluewhale-sqlite-");
        return "jdbc:sqlite:" + directory.resolve("context.db");
    }

    private static ContextUnit unit(Scope scope, String text, UnitState state) {
        return ContextUnit.create(UnitId.generate(), scope, UnitKind.SEMANTIC, Origin.OBSERVED,
                new UnitContent(text, text + " overview", text, null),
                new Lifecycle(state, NOW.minusSeconds(60), null, NOW.minusSeconds(60),
                        null, NOW.minusSeconds(3600), NOW.minusSeconds(30),
                        NOW.plusSeconds(86400), NOW.plusSeconds(86400 * 365), null),
                Ownership.unowned(), new Trust(0.9, 3),
                new Economy(12, 2, 1, 1, 0, 0.8),
                new Provenance(null, "wiki/page.md", 0, 10, "ontology-1", "extractor-1"),
                new Lineage(List.of(UnitId.generate()), false),
                List.of("tag-1", "tag-2"));
    }
}
