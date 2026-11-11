package org.skylark.bluewhale.application;

import org.skylark.bluewhale.application.context.MaximumMarginalRelevance;
import org.skylark.bluewhale.application.context.ReciprocalRankFusion;
import org.skylark.bluewhale.application.context.ReciprocalRankFusion.FusedCandidate;
import org.skylark.bluewhale.application.context.StateAwareReranker;
import org.skylark.bluewhale.domain.context.ContextBundle;
import org.skylark.bluewhale.domain.context.ContextBundle.Cost;
import org.skylark.bluewhale.domain.context.ContextBundle.Gate;
import org.skylark.bluewhale.domain.context.ContextBundle.Note;
import org.skylark.bluewhale.domain.context.ContextBundle.Omitted;
import org.skylark.bluewhale.domain.context.ContextBundle.ProvenanceView;
import org.skylark.bluewhale.domain.context.ContextBundle.Section;
import org.skylark.bluewhale.domain.context.ContextBundle.Slot;
import org.skylark.bluewhale.domain.context.ContextBundle.TraceStep;
import org.skylark.bluewhale.domain.context.ContextBundle.TrustView;
import org.skylark.bluewhale.domain.context.ContextBundle.ValidityView;
import org.skylark.bluewhale.domain.context.ContextUnit;
import org.skylark.bluewhale.domain.context.MemoryContextMapper;
import org.skylark.bluewhale.domain.context.RepresentationDepth;
import org.skylark.bluewhale.domain.context.RepresentationLine;
import org.skylark.bluewhale.domain.context.ResolveQuery;
import org.skylark.bluewhale.domain.context.Outcome;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.context.UnitKind;
import org.skylark.bluewhale.domain.context.UnitState;
import org.skylark.bluewhale.domain.feedback.UsageEvent;
import org.skylark.bluewhale.domain.governance.AccessPolicy;
import org.skylark.bluewhale.domain.model.memory.MemoryId;
import org.skylark.bluewhale.domain.port.ClockPort;
import org.skylark.bluewhale.domain.port.ContextUnitRepository;
import org.skylark.bluewhale.domain.port.EmbeddingPort;
import org.skylark.bluewhale.domain.port.IndexPort;
import org.skylark.bluewhale.domain.port.ReleasePort;
import org.skylark.bluewhale.domain.repository.MemoryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ResolveContextUseCase {

    private static final int RECALL_LIMIT = 20;
    private static final Set<UnitState> RETRIEVABLE_STATES =
            Set.of(UnitState.ACTIVE, UnitState.STALE_SUSPECT, UnitState.CONFLICTED);
    private static final Set<String> NON_RETRIEVAL_INTENTS =
            Set.of("CHITCHAT", "SMALL_TALK", "CREATIVE", "IMAGE_GENERATION");

    private final EmbeddingPort embeddingPort;
    private final IndexPort indexPort;
    private final ContextUnitRepository unitRepository;
    private final MemoryRepository legacyMemoryRepository;
    private final AccessPolicy accessPolicy;
    private final ReleasePort releasePort;
    private final ClockPort clock;
    private final UsageEventRecorder usageEventRecorder;
    private final ReciprocalRankFusion fusion = new ReciprocalRankFusion();
    private final StateAwareReranker reranker = new StateAwareReranker();
    private final MaximumMarginalRelevance mmr = new MaximumMarginalRelevance(0.7);

    public ResolveContextUseCase(EmbeddingPort embeddingPort, IndexPort indexPort,
                                 ContextUnitRepository unitRepository,
                                 MemoryRepository legacyMemoryRepository,
                                 AccessPolicy accessPolicy, ReleasePort releasePort,
                                 ClockPort clock, UsageEventRecorder usageEventRecorder) {
        this.embeddingPort = embeddingPort;
        this.indexPort = indexPort;
        this.unitRepository = unitRepository;
        this.legacyMemoryRepository = legacyMemoryRepository;
        this.accessPolicy = accessPolicy;
        this.releasePort = releasePort;
        this.clock = clock;
        this.usageEventRecorder = usageEventRecorder;
    }

    public ContextBundle resolve(ResolveQuery query) {
        long started = System.nanoTime();
        String releaseId = releasePort.currentReleaseId();
        List<TraceStep> trace = new ArrayList<>();

        if (!accessPolicy.allows(query.scope())) {
            trace.add(new TraceStep("permission", "denied"));
            return empty(query, releaseId, Gate.EMPTY, "ACCESS_DENIED", started, trace);
        }
        trace.add(new TraceStep("permission", "allowed"));

        if (query.query() == null || query.query().isBlank()) {
            trace.add(new TraceStep("intent-gate", "empty query"));
            return empty(query, releaseId, Gate.EMPTY, "EMPTY_QUERY", started, trace);
        }
        String intent = query.intent() == null ? "" : query.intent().toUpperCase(Locale.ROOT);
        if (NON_RETRIEVAL_INTENTS.contains(intent)) {
            trace.add(new TraceStep("intent-gate", "non-retrieval intent: " + intent));
            return empty(query, releaseId, Gate.NON_RETRIEVAL, "NON_RETRIEVAL_INTENT", started, trace);
        }
        trace.add(new TraceStep("intent-gate", "retrieval-bearing"));

        List<List<IndexPort.Candidate>> recallLists = new ArrayList<>();
        recallLists.add(denseRecall(query, trace));
        recallLists.add(lexicalRecall(query, trace));
        List<IndexPort.Candidate> timeline = timelineRecall(query, intent, trace);
        if (!timeline.isEmpty()) {
            recallLists.add(timeline);
        }
        List<IndexPort.Candidate> allCandidates = recallLists.stream()
                .flatMap(List::stream)
                .toList();
        if (allCandidates.isEmpty()) {
            trace.add(new TraceStep("recall", "no candidates"));
            return empty(query, releaseId, Gate.EMPTY, "NO_HITS", started, trace);
        }

        Instant now = clock.now();
        Map<UnitId, ContextUnit> units = loadVisibleUnits(query, allCandidates, trace);
        List<List<IndexPort.Candidate>> filteredLists = new ArrayList<>();
        int[] rejected = {0};
        for (List<IndexPort.Candidate> ranked : recallLists) {
            filteredLists.add(filterCandidates(ranked, query, units, now, rejected));
        }
        if (filteredLists.stream().allMatch(List::isEmpty)) {
            trace.add(new TraceStep("state-filter", "no retrievable units"));
            return empty(query, releaseId, Gate.EMPTY, "NO_RETRIEVABLE_UNITS", started, trace);
        }
        trace.add(new TraceStep("state-filter", rejected[0] + " rejected"));

        List<FusedCandidate> fused = fusion.fuse(filteredLists, RECALL_LIMIT);
        trace.add(new TraceStep("rrf", fused.size() + " fused from "
                + recallLists.size() + " recall lines"));
        List<FusedCandidate> reranked = reranker.rerank(fused,
                id -> units.get(id).lifecycle().state());
        trace.add(new TraceStep("rerank", reranked.size() + " state-weighted"));
        List<FusedCandidate> deduplicated = deduplicateByContent(reranked, units);
        trace.add(new TraceStep("dedupe", (reranked.size() - deduplicated.size())
                + " duplicate hashes removed"));
        List<FusedCandidate> selected = mmr.select(deduplicated, RECALL_LIMIT);
        trace.add(new TraceStep("mmr", selected.size() + " diverse candidates"));

        List<Section> sections = new ArrayList<>();
        List<Omitted> omitted = new ArrayList<>();
        List<Note> notes = new ArrayList<>();
        int usedTokens = 0;
        List<UsageEvent> usageEvents = new ArrayList<>();
        for (FusedCandidate candidate : selected) {
            ContextUnit unit = units.get(candidate.unitId());
            RepresentationDepth depth = chooseDepth(unit, query.maxDepth(),
                    query.budget().maxTokens(), usedTokens);
            if (depth == null) {
                omitted.add(new Omitted(unit.id().toString(), "BUDGET_EXCEEDED", true));
                continue;
            }
            int tokens = unit.content().estimatedTokensAt(depth);
            usedTokens += tokens;
            sections.add(toSection(unit, depth));
            usageEvents.add(new UsageEvent(unit.id(), query.traceId(), Outcome.SEEN, tokens, now));
            if (unit.lifecycle().state() == UnitState.CONFLICTED) {
                notes.add(new Note(unit.id().toString(), "CONFLICT_UNRESOLVED", null));
            }
        }

        if (sections.isEmpty()) {
            trace.add(new TraceStep("budget", "all candidates exceeded budget"));
            return empty(query, releaseId, Gate.EMPTY, "BUDGET_EXCLUDED", started, trace);
        }
        usageEventRecorder.record(usageEvents);
        trace.add(new TraceStep("budget", sections.size() + " sections, " + usedTokens
                + " tokens, " + omitted.size() + " omitted"));
        trace.add(new TraceStep("usage", sections.size() + " seen events recorded"));

        return new ContextBundle(query.traceId(), releaseId, Gate.RETRIEVAL_BEARING, null,
                sections, omitted, notes, List.of(),
                new Cost(usedTokens, 0, elapsedMs(started)), trace);
    }

    private List<IndexPort.Candidate> denseRecall(ResolveQuery query, List<TraceStep> trace) {
        try {
            float[] vector = embeddingPort.embed(query.query());
            List<IndexPort.Candidate> candidates = indexPort.dense(query.scope(), vector, RECALL_LIMIT);
            trace.add(new TraceStep("dense-recall", candidates.size() + " hits"));
            return candidates;
        } catch (RuntimeException failure) {
            trace.add(new TraceStep("dense-recall", "failed: " + failure.getMessage()));
            return List.of();
        }
    }

    private List<IndexPort.Candidate> lexicalRecall(ResolveQuery query, List<TraceStep> trace) {
        try {
            List<IndexPort.Candidate> candidates = indexPort.lexical(
                    query.scope(), query.query(), RECALL_LIMIT);
            trace.add(new TraceStep("lexical-recall", candidates.size() + " hits"));
            return candidates;
        } catch (RuntimeException failure) {
            trace.add(new TraceStep("lexical-recall", "failed: " + failure.getMessage()));
            return List.of();
        }
    }

    private List<IndexPort.Candidate> timelineRecall(ResolveQuery query, String intent,
                                                     List<TraceStep> trace) {
        if (!query.lines().contains(RepresentationLine.EPISODE_TIMELINE)
                || (!intent.contains("TEMPORAL") && !intent.contains("HISTORY"))) {
            return List.of();
        }
        List<ContextUnit> recent = unitRepository.findByScope(query.scope(), RETRIEVABLE_STATES)
                .stream()
                .filter(unit -> unit.lifecycle().capturedAt() != null)
                .sorted(Comparator.comparing((ContextUnit unit) -> unit.lifecycle().capturedAt())
                        .reversed())
                .limit(RECALL_LIMIT)
                .toList();
        List<IndexPort.Candidate> candidates = new ArrayList<>(recent.size());
        for (int i = 0; i < recent.size(); i++) {
            ContextUnit unit = recent.get(i);
            candidates.add(new IndexPort.Candidate(unit.id(),
                    RepresentationLine.EPISODE_TIMELINE, 1.0 - i * 0.01,
                    Map.of("source", "timeline"), new float[0]));
        }
        trace.add(new TraceStep("timeline-recall", candidates.size() + " hits"));
        return candidates;
    }

    private Map<UnitId, ContextUnit> loadVisibleUnits(ResolveQuery query,
                                                      List<IndexPort.Candidate> candidates,
                                                      List<TraceStep> trace) {
        Map<UnitId, ContextUnit> visible = new LinkedHashMap<>();
        int rejected = 0;
        for (UnitId unitId : new LinkedHashSet<>(candidates.stream()
                .map(IndexPort.Candidate::unitId).toList())) {
            ContextUnit unit = unitRepository.findById(unitId)
                    .or(() -> legacyMemoryRepository.findById(MemoryId.of(unitId.toString()))
                            .map(MemoryContextMapper::toContextUnit))
                    .orElse(null);
            if (unit == null) {
                rejected++;
                trace.add(new TraceStep("load", unitId + " missing from store"));
                continue;
            }
            if (!unit.isVisibleTo(query.scope(), accessPolicy)) {
                rejected++;
                trace.add(new TraceStep("permission", unitId + " out of scope"));
                continue;
            }
            visible.put(unitId, unit);
        }
        trace.add(new TraceStep("permission", visible.size() + " visible, " + rejected + " rejected"));
        return visible;
    }

    private List<IndexPort.Candidate> filterCandidates(List<IndexPort.Candidate> ranked,
                                                       ResolveQuery query,
                                                       Map<UnitId, ContextUnit> units,
                                                       Instant now, int[] rejected) {
        List<IndexPort.Candidate> filtered = new ArrayList<>();
        for (IndexPort.Candidate candidate : ranked) {
            if (!query.lines().contains(candidate.line())) {
                rejected[0]++;
                continue;
            }
            ContextUnit unit = units.get(candidate.unitId());
            if (unit == null) {
                rejected[0]++;
                continue;
            }
            if (!unit.isRetrievable(now)) {
                rejected[0]++;
                continue;
            }
            filtered.add(candidate);
        }
        return filtered;
    }

    private List<FusedCandidate> deduplicateByContent(List<FusedCandidate> candidates,
                                                      Map<UnitId, ContextUnit> units) {
        Map<String, FusedCandidate> unique = new LinkedHashMap<>();
        for (FusedCandidate candidate : candidates) {
            ContextUnit unit = units.get(candidate.unitId());
            unique.putIfAbsent(unit.contentHash(), candidate);
        }
        return List.copyOf(unique.values());
    }

    private RepresentationDepth chooseDepth(ContextUnit unit, RepresentationDepth maxDepth,
                                            int budget, int usedTokens) {
        int remaining = budget - usedTokens;
        int cost = unit.content().estimatedTokensAt(maxDepth);
        if (cost <= remaining) {
            return maxDepth;
        }
        cost = unit.content().estimatedTokensAt(RepresentationDepth.ABSTRACT);
        return cost <= remaining ? RepresentationDepth.ABSTRACT : null;
    }

    private Section toSection(ContextUnit unit, RepresentationDepth depth) {
        String hash = unit.contentHash();
        String citation = unit.id() + "@" + hash + "#anchor-1";
        return new Section(
                unit.kind() == UnitKind.WORKING ? Slot.CORE : Slot.ON_DEMAND,
                unit.id().toString(),
                unit.kind(),
                depth,
                unit.content().textAt(depth),
                "PROSE",
                new TrustView(unit.trust().confidence(), unit.origin(), unit.lifecycle().state()),
                new ValidityView(instant(unit.lifecycle().validFrom()), instant(unit.lifecycle().validTo())),
                hash,
                citation,
                new ProvenanceView(
                        unit.provenance().episodeId() == null ? null : unit.provenance().episodeId().toString(),
                        unit.provenance().sourceRef(),
                        unit.provenance().spanStart(),
                        unit.provenance().spanEnd()
                )
        );
    }

    private ContextBundle empty(ResolveQuery query, String releaseId, Gate gate, String reason,
                                long started, List<TraceStep> trace) {
        trace.add(new TraceStep("result", gate + ":" + reason));
        return new ContextBundle(query.traceId(), releaseId, gate, reason, List.of(), List.of(),
                List.of(), List.of(), new Cost(0, 0, elapsedMs(started)), trace);
    }

    private static long elapsedMs(long started) {
        return Math.max(0, (System.nanoTime() - started) / 1_000_000);
    }

    private static String instant(Instant value) {
        return value == null ? null : value.toString();
    }
}
