package org.skylark.bluewhale.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.skylark.bluewhale.domain.graph.EdgeType;
import org.skylark.bluewhale.domain.port.ExtractionPort;
import org.skylark.bluewhale.infrastructure.index.InMemoryIndexAdapter;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryContextUnitRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryLifecycleEventRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryUnitEdgeRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryUsageEventRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunMaintenanceUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-11T08:00:00Z");
    private static final Scope SCOPE = Scope.agent("acme", "agent-1");

    private InMemoryContextUnitRepository unitRepository;
    private InMemoryUnitEdgeRepository edgeRepository;
    private InMemoryIndexAdapter index;

    @BeforeEach
    void setUp() {
        unitRepository = new InMemoryContextUnitRepository();
        edgeRepository = new InMemoryUnitEdgeRepository();
        index = new InMemoryIndexAdapter();
    }

    private RunMaintenanceUseCase maintenance(Optional<ExtractionPort> extractor) {
        InMemoryLifecycleEventRepository events = new InMemoryLifecycleEventRepository();
        LifecycleUseCase lifecycle = new LifecycleUseCase(unitRepository, events, () -> NOW);
        return new RunMaintenanceUseCase(unitRepository, edgeRepository,
                new InMemoryUsageEventRepository(), lifecycle, index, () -> NOW, extractor);
    }

    @Test
    void forgettingSoftRetiresDecayedUnitButExemptsProcedural() {
        Instant longAgo = NOW.minusSeconds(40L * 86400);
        ContextUnit episodic = activeUnit(UnitKind.EPISODIC, "stale fact", Economy.initial(0.5), longAgo);
        ContextUnit procedural = activeUnit(UnitKind.PROCEDURAL, "how to deploy", Economy.initial(0.5), longAgo);
        unitRepository.save(episodic);
        unitRepository.save(procedural);

        MaintenanceReport report = maintenance(Optional.empty()).run(SCOPE);

        assertEquals(1, report.retired());
        assertEquals(UnitState.ARCHIVED, unitRepository.findById(episodic.id()).orElseThrow()
                .lifecycle().state());
        assertEquals(UnitState.ACTIVE, unitRepository.findById(procedural.id()).orElseThrow()
                .lifecycle().state());
    }

    @Test
    void strengtheningRecomputesImportanceFromAdoptionSignals() {
        ContextUnit adopted = activeUnit(UnitKind.SEMANTIC, "useful fact",
                new Economy(0, 0, 0, 2, 0, 0.5), NOW);
        unitRepository.save(adopted);

        MaintenanceReport report = maintenance(Optional.empty()).run(SCOPE);

        assertEquals(1, report.strengthened());
        assertEquals(0.7, unitRepository.findById(adopted.id()).orElseThrow()
                .economy().importance(), 1e-9);
    }

    @Test
    void abstractionDistillsHighAdoptionEpisodicsIntoPendingReviewKnowledge() {
        for (int i = 0; i < 3; i++) {
            unitRepository.save(activeUnit(UnitKind.EPISODIC, "episode " + i,
                    new Economy(0, 0, 0, 3, 0, 0.5), NOW));
        }
        ExtractionPort extractor = sources -> "Distilled abstraction";

        MaintenanceReport report = maintenance(Optional.of(extractor)).run(SCOPE);

        assertEquals(1, report.distilled());
        List<ContextUnit> pending = unitRepository.findByScope(SCOPE, Set.of(UnitState.PENDING_REVIEW));
        assertEquals(1, pending.size());
        ContextUnit distilled = pending.get(0);
        assertEquals(Origin.DISTILLED, distilled.origin());
        assertEquals(UnitKind.SEMANTIC, distilled.kind());
        assertEquals("Distilled abstraction", distilled.content().abstractText());
        assertEquals(3, distilled.lineage().derivedFrom().size());
        assertEquals(3, edgeRepository.findByFrom(distilled.id(), EdgeType.DERIVED_FROM).size());
    }

    @Test
    void abstractionIsDisabledWithoutAnExtractor() {
        for (int i = 0; i < 3; i++) {
            unitRepository.save(activeUnit(UnitKind.EPISODIC, "episode " + i,
                    new Economy(0, 0, 0, 3, 0, 0.5), NOW));
        }

        MaintenanceReport report = maintenance(Optional.empty()).run(SCOPE);

        assertEquals(0, report.distilled());
        assertTrue(unitRepository.findByScope(SCOPE, Set.of(UnitState.PENDING_REVIEW)).isEmpty());
    }

    private static ContextUnit activeUnit(UnitKind kind, String text, Economy economy, Instant capturedAt) {
        return ContextUnit.create(UnitId.generate(), SCOPE, kind, Origin.OBSERVED, UnitContent.of(text),
                new Lifecycle(UnitState.ACTIVE, capturedAt, null, capturedAt, null, null,
                        capturedAt, null, null, null),
                Ownership.unowned(), Trust.observed(0.9), economy,
                Provenance.ofSource("maintenance-test"), Lineage.empty(), List.of());
    }
}