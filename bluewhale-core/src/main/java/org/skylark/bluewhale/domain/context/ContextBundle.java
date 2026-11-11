package org.skylark.bluewhale.domain.context;

import java.util.List;

public record ContextBundle(
        String traceId,
        String releaseId,
        Gate gate,
        String emptyReason,
        List<Section> sections,
        List<Omitted> omitted,
        List<Note> notes,
        List<RequiredFact> requiredFacts,
        Cost cost,
        List<TraceStep> trace
) {

    public ContextBundle {
        sections = sections == null ? List.of() : List.copyOf(sections);
        omitted = omitted == null ? List.of() : List.copyOf(omitted);
        notes = notes == null ? List.of() : List.copyOf(notes);
        requiredFacts = requiredFacts == null ? List.of() : List.copyOf(requiredFacts);
        trace = trace == null ? List.of() : List.copyOf(trace);
    }

    public enum Gate {
        RETRIEVAL_BEARING,
        NON_RETRIEVAL,
        EMPTY
    }

    public enum Slot {
        CORE,
        ON_DEMAND
    }

    public record Section(
            Slot slot,
            String unitId,
            UnitKind kind,
            RepresentationDepth depth,
            String text,
            String render,
            TrustView trust,
            ValidityView validity,
            String contentHash,
            String citation,
            ProvenanceView provenance
    ) {
    }

    public record TrustView(double confidence, Origin origin, UnitState state) {
    }

    public record ValidityView(String validFrom, String validTo) {
    }

    public record ProvenanceView(String episode, String source, Integer spanStart, Integer spanEnd) {
    }

    public record Omitted(String unitId, String reason, boolean recoverable) {
    }

    public record Note(String unitId, String reason, String conflictWith) {
    }

    public record RequiredFact(String fact, String policy) {
    }

    public record Cost(int tokens, int retrievalLlmCalls, long elapsedMs) {
    }

    public record TraceStep(String stage, String detail) {
    }
}
