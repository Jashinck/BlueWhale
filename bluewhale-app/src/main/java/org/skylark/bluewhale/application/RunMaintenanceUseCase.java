package org.skylark.bluewhale.application;

import org.skylark.bluewhale.domain.context.Actor;
import org.skylark.bluewhale.domain.context.ContextUnit;
import org.skylark.bluewhale.domain.context.Economy;
import org.skylark.bluewhale.domain.context.Lifecycle;
import org.skylark.bluewhale.domain.context.Lineage;
import org.skylark.bluewhale.domain.context.Origin;
import org.skylark.bluewhale.domain.context.Ownership;
import org.skylark.bluewhale.domain.context.Provenance;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.Transition;
import org.skylark.bluewhale.domain.context.Trust;
import org.skylark.bluewhale.domain.context.UnitContent;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.context.UnitKind;
import org.skylark.bluewhale.domain.context.UnitState;
import org.skylark.bluewhale.domain.feedback.UsageEvent;
import org.skylark.bluewhale.domain.graph.UnitEdge;
import org.skylark.bluewhale.domain.lifecycle.DecayPolicy;
import org.skylark.bluewhale.domain.lifecycle.RetentionPolicy;
import org.skylark.bluewhale.domain.lifecycle.StrengtheningPolicy;
import org.skylark.bluewhale.domain.port.ClockPort;
import org.skylark.bluewhale.domain.port.ContextUnitRepository;
import org.skylark.bluewhale.domain.port.ExtractionPort;
import org.skylark.bluewhale.domain.port.IndexPort;
import org.skylark.bluewhale.domain.port.UnitEdgeRepository;
import org.skylark.bluewhale.domain.port.UsageEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class RunMaintenanceUseCase {

    private static final Actor MAINTAINER = Actor.system("memory-maintenance");
    private static final Set<UnitState> MAINTAINABLE =
            Set.of(UnitState.ACTIVE, UnitState.STALE_SUSPECT, UnitState.CONFLICTED);
    private static final double ABSTRACTION_IMPORTANCE = 0.7;
    private static final int ABSTRACTION_MIN = 3;

    private final ContextUnitRepository unitRepository;
    private final UnitEdgeRepository edgeRepository;
    private final UsageEventRepository usageEventRepository;
    private final LifecycleUseCase lifecycle;
    private final IndexPort indexPort;
    private final ClockPort clock;
    private final Optional<ExtractionPort> extractionPort;
    private final DecayPolicy decayPolicy = DecayPolicy.defaultPolicy();
    private final RetentionPolicy retentionPolicy = RetentionPolicy.defaultPolicy();
    private final StrengtheningPolicy strengtheningPolicy = StrengtheningPolicy.defaultPolicy();

    public RunMaintenanceUseCase(ContextUnitRepository unitRepository, UnitEdgeRepository edgeRepository,
                                 UsageEventRepository usageEventRepository, LifecycleUseCase lifecycle,
                                 IndexPort indexPort, ClockPort clock, Optional<ExtractionPort> extractionPort) {
        this.unitRepository = unitRepository;
        this.edgeRepository = edgeRepository;
        this.usageEventRepository = usageEventRepository;
        this.lifecycle = lifecycle;
        this.indexPort = indexPort;
        this.clock = clock;
        this.extractionPort = extractionPort;
    }

    @Transactional
    public MaintenanceReport run(Scope scope) {
        Instant now = clock.now();
        List<ContextUnit> units = unitRepository.findByScope(scope, MAINTAINABLE);
        int retired = 0;
        int strengthened = 0;
        List<ContextUnit> abstractionCandidates = new ArrayList<>();

        for (ContextUnit unit : units) {
            double recomputed = strengtheningPolicy.recomputeImportance(unit.economy());
            if (Math.abs(recomputed - unit.economy().importance()) > 1e-9) {
                unit = unit.withImportance(recomputed);
                unitRepository.save(unit);
                strengthened++;
            }

            double effective = decayPolicy.effectiveImportance(
                    unit.economy().importance(), lastSeen(unit), now);
            Transition retire = retireTransition(unit.lifecycle().state());
            if (retire != null && retentionPolicy.shouldRetire(unit.kind(), effective)) {
                lifecycle.transition(unit.id(), retire, MAINTAINER,
                        "retired: effective importance below threshold", null);
                indexPort.invalidate(unit.id());
                retired++;
                continue;
            }

            if (unit.kind() == UnitKind.EPISODIC && unit.economy().importance() > ABSTRACTION_IMPORTANCE) {
                abstractionCandidates.add(unit);
            }
        }

        int distilled = distill(scope, abstractionCandidates, now);
        return new MaintenanceReport(retired, strengthened, distilled);
    }

    private int distill(Scope scope, List<ContextUnit> candidates, Instant now) {
        if (extractionPort.isEmpty() || candidates.size() < ABSTRACTION_MIN) {
            return 0;
        }
        List<String> sources = candidates.stream().map(unit -> unit.content().abstractText()).toList();
        String abstraction = extractionPort.get().distill(sources);
        if (abstraction == null || abstraction.isBlank()) {
            return 0;
        }
        List<UnitId> parents = candidates.stream().map(ContextUnit::id).toList();
        ContextUnit distilled = ContextUnit.create(UnitId.generate(), scope, UnitKind.SEMANTIC,
                Origin.DISTILLED, UnitContent.of(abstraction),
                new Lifecycle(UnitState.EXTRACTED, now, null, now, null, null, now, null, null, null),
                Ownership.unowned(), Trust.observed(0.8), Economy.initial(0.8),
                Provenance.ofSource("distillation"), Lineage.derivedFrom(parents), List.of());
        unitRepository.save(distilled);
        lifecycle.transition(distilled.id(), Transition.EXTRACTED_TO_PENDING_REVIEW, MAINTAINER,
                "distilled from " + parents.size() + " episodic units", null);
        for (UnitId parent : parents) {
            edgeRepository.save(UnitEdge.derivedFrom(distilled.id(), parent, now));
        }
        return 1;
    }

    private Instant lastSeen(ContextUnit unit) {
        return usageEventRepository.findByUnitId(unit.id()).stream()
                .map(UsageEvent::at)
                .max(Comparator.naturalOrder())
                .orElseGet(() -> unit.lifecycle().capturedAt() != null
                        ? unit.lifecycle().capturedAt() : unit.lifecycle().learnedAt());
    }

    private static Transition retireTransition(UnitState state) {
        return switch (state) {
            case ACTIVE -> Transition.ACTIVE_TO_ARCHIVED;
            case STALE_SUSPECT -> Transition.STALE_SUSPECT_TO_EXPIRED;
            default -> null;
        };
    }
}