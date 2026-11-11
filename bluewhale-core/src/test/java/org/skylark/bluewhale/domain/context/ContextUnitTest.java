package org.skylark.bluewhale.domain.context;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextUnitTest {

    private static final Instant NOW = Instant.parse("2026-11-09T08:00:00Z");
    private static final Scope SCOPE = Scope.agent("acme", "agent-1");

    @Test
    void shouldRejectDeclaredKnowledgeWithoutOwner() {
        assertThrows(IllegalArgumentException.class, () -> ContextUnit.create(
                UnitId.generate(), SCOPE, UnitKind.SEMANTIC, Origin.DECLARED,
                UnitContent.of("policy"), lifecycle(UnitState.EXTRACTED),
                Ownership.unowned(), Trust.observed(0.9), Economy.initial(0.9),
                Provenance.ofSource("policy.md"), Lineage.empty(), List.of()));
    }

    @Test
    void shouldRejectDistilledUnitWithoutLineage() {
        assertThrows(IllegalArgumentException.class, () -> ContextUnit.create(
                UnitId.generate(), SCOPE, UnitKind.SEMANTIC, Origin.DISTILLED,
                UnitContent.of("summary"), lifecycle(UnitState.EXTRACTED),
                Ownership.unowned(), Trust.observed(0.9), Economy.initial(0.9),
                Provenance.ofSource("memory"), Lineage.empty(), List.of()));
    }

    @Test
    void shouldRequireProvenance() {
        assertThrows(IllegalArgumentException.class,
                () -> new Provenance(null, null, null, null, null, null));
    }

    @Test
    void transitionShouldReturnNewStateAndAppendOnlyEvent() {
        ContextUnit captured = unit(UnitState.CAPTURED, Origin.OBSERVED, 0.9);

        TransitionResult extracted = captured.transition(Transition.CAPTURE_TO_EXTRACTED,
                Actor.system("extractor"), "parsed", NOW, null);
        TransitionResult active = extracted.unit().transition(Transition.EXTRACTED_TO_ACTIVE,
                Actor.system("policy"), "observed and confident", NOW, null);

        assertEquals(UnitState.CAPTURED, captured.lifecycle().state());
        assertEquals(UnitState.EXTRACTED, extracted.unit().lifecycle().state());
        assertEquals(UnitState.ACTIVE, active.unit().lifecycle().state());
        assertEquals(UnitState.EXTRACTED, active.event().fromState());
        assertTrue(active.event().trigger().contains("EXTRACTED_TO_ACTIVE"));
    }

    @Test
    void shouldRejectAutomaticActivationBelowConfidenceThreshold() {
        ContextUnit extracted = unit(UnitState.EXTRACTED, Origin.OBSERVED, 0.5);

        assertThrows(IllegalStateException.class, () -> extracted.transition(
                Transition.EXTRACTED_TO_ACTIVE, Actor.system("policy"), "too weak", NOW, null));
    }

    @Test
    void shouldKeepSupersededHistoryAndBlockRetrieval() {
        ContextUnit active = unit(UnitState.ACTIVE, Origin.OBSERVED, 0.9);

        TransitionResult result = active.markSuperseded(UnitId.generate(),
                NOW, NOW, Actor.system("reconciler"), "new version");

        assertEquals(UnitState.SUPERSEDED, result.unit().lifecycle().state());
        assertEquals(NOW, result.unit().lifecycle().validTo());
        assertFalse(result.unit().isRetrievable(NOW));
        assertEquals("SUPERSEDE", result.event().trigger());
    }

    @Test
    void legalHoldShouldBlockErasure() {
        ContextUnit expired = ContextUnit.create(
                UnitId.generate(), SCOPE, UnitKind.SEMANTIC, Origin.OBSERVED,
                UnitContent.of("contract"), lifecycle(UnitState.EXPIRED),
                new Ownership("legal", "compliance", 365, true, "litigation hold"),
                Trust.observed(0.9), Economy.initial(0.9), Provenance.ofSource("contract.pdf"),
                Lineage.empty(), List.of());

        assertThrows(IllegalStateException.class, () -> expired.transition(
                Transition.EXPIRED_TO_ERASED, Actor.system("erasure"), "retention", NOW, null));
    }

    @Test
    void recordAppliedShouldTrackEconomyCounters() {
        ContextUnit active = unit(UnitState.ACTIVE, Origin.OBSERVED, 0.9);

        ContextUnit updated = active.recordApplied(Outcome.CITED)
                .recordApplied(Outcome.ADOPTED)
                .recordApplied(Outcome.CORRECTED);

        assertEquals(1, updated.economy().nCited());
        assertEquals(1, updated.economy().nAdopted());
        assertEquals(1, updated.economy().nCorrected());
    }

    private static ContextUnit unit(UnitState state, Origin origin, double confidence) {
        return ContextUnit.create(UnitId.generate(), SCOPE, UnitKind.EPISODIC, origin,
                UnitContent.of("user prefers dark mode"), lifecycle(state), Ownership.unowned(),
                Trust.observed(confidence), Economy.initial(confidence),
                Provenance.ofEpisode(UnitId.generate()), Lineage.empty(), List.of("preference"));
    }

    private static Lifecycle lifecycle(UnitState state) {
        return new Lifecycle(state, NOW.minusSeconds(60), null, NOW.minusSeconds(60),
                null, null, NOW.minusSeconds(60), null, null, null);
    }
}
