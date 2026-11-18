package org.skylark.bluewhale.application;

import org.skylark.bluewhale.domain.context.Actor;
import org.skylark.bluewhale.domain.context.ContextUnit;
import org.skylark.bluewhale.domain.context.Economy;
import org.skylark.bluewhale.domain.context.Lifecycle;
import org.skylark.bluewhale.domain.context.Lineage;
import org.skylark.bluewhale.domain.context.Origin;
import org.skylark.bluewhale.domain.context.Ownership;
import org.skylark.bluewhale.domain.context.Provenance;
import org.skylark.bluewhale.domain.context.RepresentationLine;
import org.skylark.bluewhale.domain.context.Transition;
import org.skylark.bluewhale.domain.context.Trust;
import org.skylark.bluewhale.domain.context.UnitContent;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.context.UnitKind;
import org.skylark.bluewhale.domain.context.UnitState;
import org.skylark.bluewhale.domain.governance.ChangeRequest;
import org.skylark.bluewhale.domain.governance.KnowledgePatch;
import org.skylark.bluewhale.domain.port.ClockPort;
import org.skylark.bluewhale.domain.port.ContextUnitRepository;
import org.skylark.bluewhale.domain.port.EmbeddingPort;
import org.skylark.bluewhale.domain.port.IndexPort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class ChangeApplier {

    private final ContextUnitRepository unitRepository;
    private final LifecycleUseCase lifecycle;
    private final IndexPort indexPort;
    private final EmbeddingPort embeddingPort;
    private final ClockPort clock;

    public ChangeApplier(ContextUnitRepository unitRepository, LifecycleUseCase lifecycle,
                         IndexPort indexPort, EmbeddingPort embeddingPort, ClockPort clock) {
        this.unitRepository = unitRepository;
        this.lifecycle = lifecycle;
        this.indexPort = indexPort;
        this.embeddingPort = embeddingPort;
        this.clock = clock;
    }

    public UnitId apply(ChangeRequest request, Actor actor) {
        return switch (request.type()) {
            case ADD, REBIND -> applyAdd(request, actor);
            case REVISE -> applyRevise(request, actor);
            case DEPRECATE -> applyDeprecate(request, actor);
            default -> throw new UnsupportedOperationException("change type not supported: " + request.type());
        };
    }

    private UnitId applyAdd(ChangeRequest request, Actor actor) {
        ContextUnit unit = newUnit(request.patch(), clock.now(), Lineage.empty());
        unitRepository.save(unit);
        activate(unit, request, actor);
        return unit.id();
    }

    private UnitId applyRevise(ChangeRequest request, Actor actor) {
        KnowledgePatch patch = request.patch();
        UnitId target = requireTarget(patch);
        ContextUnit old = unitRepository.findById(target)
                .orElseThrow(() -> new NoSuchElementException("revise target not found: " + target));
        Instant now = clock.now();
        ContextUnit revised = newUnit(patch, now, Lineage.derivedFrom(List.of(target)));
        unitRepository.save(revised);
        activate(revised, request, actor);
        lifecycle.supersede(old.id(), revised.id(), now, actor, "revised via change request " + request.id());
        indexPort.invalidate(old.id());
        return revised.id();
    }

    private UnitId applyDeprecate(ChangeRequest request, Actor actor) {
        UnitId target = requireTarget(request.patch());
        lifecycle.transition(target, Transition.ACTIVE_TO_ARCHIVED, actor,
                "deprecated via change request " + request.id(), null);
        indexPort.invalidate(target);
        return target;
    }

    private ContextUnit newUnit(KnowledgePatch patch, Instant now, Lineage lineage) {
        return ContextUnit.create(UnitId.generate(), patch.scope(), UnitKind.SEMANTIC, Origin.DECLARED,
                UnitContent.of(patch.content()),
                new Lifecycle(UnitState.PENDING_REVIEW, now, null, now, null, null, now,
                        now.plusSeconds(86400L * 365), null, null),
                Ownership.owned(patch.ownerId(), patch.ownerOrg(), 365),
                new Trust(1.0, 4), Economy.initial(0.8),
                Provenance.ofSource(patch.sourceRef()), lineage, List.of());
    }

    private void activate(ContextUnit unit, ChangeRequest request, Actor actor) {
        lifecycle.transition(unit.id(), Transition.PENDING_REVIEW_TO_ACTIVE, actor,
                "released via change request " + request.id(), null);
        indexPort.index(new IndexPort.IndexEntry(unit.id(), unit.scope(), RepresentationLine.CURATED_DOC,
                unit.content().abstractText(), embeddingPort.embed(unit.content().abstractText()),
                Map.of("kind", unit.kind().name(), "origin", unit.origin().name(),
                        "state", UnitState.ACTIVE.name())));
    }

    private static UnitId requireTarget(KnowledgePatch patch) {
        if (patch.targetUnitId() == null) {
            throw new IllegalArgumentException("change requires a target unit");
        }
        return patch.targetUnitId();
    }
}