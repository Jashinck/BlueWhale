package org.skylark.bluewhale.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skylark.bluewhale.domain.context.ContextUnit;
import org.skylark.bluewhale.domain.context.Economy;
import org.skylark.bluewhale.domain.context.Lifecycle;
import org.skylark.bluewhale.domain.context.Lineage;
import org.skylark.bluewhale.domain.context.Origin;
import org.skylark.bluewhale.domain.context.Outcome;
import org.skylark.bluewhale.domain.context.Ownership;
import org.skylark.bluewhale.domain.context.Provenance;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.Trust;
import org.skylark.bluewhale.domain.context.UnitContent;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.context.UnitKind;
import org.skylark.bluewhale.domain.context.UnitState;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryContextUnitRepository;
import org.skylark.bluewhale.infrastructure.persistence.InMemoryUsageEventRepository;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecordFeedbackUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-10T08:00:00Z");

    private InMemoryContextUnitRepository unitRepository;
    private InMemoryUsageEventRepository usageEventRepository;
    private RecordFeedbackUseCase useCase;

    @BeforeEach
    void setUp() {
        unitRepository = new InMemoryContextUnitRepository();
        usageEventRepository = new InMemoryUsageEventRepository();
        useCase = new RecordFeedbackUseCase(usageEventRepository, unitRepository, () -> NOW);
    }

    @Test
    void shouldAppendUsageEventAndUpdateEconomy() {
        ContextUnit unit = activeUnit();
        unitRepository.save(unit);

        useCase.record(new FeedbackBatch("trace-1", List.of(
                new FeedbackBatch.FeedbackItem(unit.id(), Outcome.ADOPTED, 12))));

        ContextUnit updated = unitRepository.findById(unit.id()).orElseThrow();
        assertEquals(1, updated.economy().nAdopted());
        assertEquals(1, usageEventRepository.findByUnitId(unit.id()).size());
        assertEquals(Outcome.ADOPTED, usageEventRepository.findByUnitId(unit.id()).get(0).outcome());
    }

    @Test
    void correctionShouldIncrementCorrectedCounterSeparately() {
        ContextUnit unit = activeUnit();
        unitRepository.save(unit);

        useCase.record(new FeedbackBatch("trace-2", List.of(
                new FeedbackBatch.FeedbackItem(unit.id(), Outcome.CORRECTED, 5),
                new FeedbackBatch.FeedbackItem(unit.id(), Outcome.CITED, 3))));

        ContextUnit updated = unitRepository.findById(unit.id()).orElseThrow();
        assertEquals(1, updated.economy().nCorrected());
        assertEquals(1, updated.economy().nCited());
        assertEquals(2, usageEventRepository.findByUnitId(unit.id()).size());
    }

    private static ContextUnit activeUnit() {
        return ContextUnit.create(UnitId.generate(), Scope.agent("acme", "agent-1"),
                UnitKind.SEMANTIC, Origin.OBSERVED, UnitContent.of("a retrievable fact"),
                new Lifecycle(UnitState.ACTIVE, NOW.minusSeconds(60), null, NOW.minusSeconds(60),
                        null, null, NOW.minusSeconds(60), null, null, null),
                Ownership.unowned(), Trust.observed(0.9), Economy.initial(0.8),
                Provenance.ofSource("wiki.md"), Lineage.empty(), List.of());
    }
}
