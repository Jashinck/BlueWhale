package org.skylark.bluewhale.client;

import java.util.List;

public record ContextBundleResponse(
        String traceId,
        String releaseId,
        String gate,
        String emptyReason,
        List<Section> sections,
        List<Omitted> omitted,
        List<Note> notes,
        List<RequiredFact> requiredFacts,
        Cost cost,
        List<TraceStep> trace
) {

    public ContextBundleResponse {
        sections = sections == null ? List.of() : sections;
        omitted = omitted == null ? List.of() : omitted;
        notes = notes == null ? List.of() : notes;
        requiredFacts = requiredFacts == null ? List.of() : requiredFacts;
        trace = trace == null ? List.of() : trace;
    }

    public record Section(
            String slot,
            String unitId,
            String kind,
            String depth,
            String text,
            String render,
            TrustView trust,
            ValidityView validity,
            String contentHash,
            String citation,
            ProvenanceView provenance
    ) {
    }

    public record TrustView(double confidence, String origin, String state) {
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
