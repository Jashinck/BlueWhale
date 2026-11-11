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
import org.skylark.bluewhale.domain.governance.ChangeRequest;
import org.skylark.bluewhale.domain.governance.KnowledgePatch;
import org.skylark.bluewhale.domain.port.ClockPort;
import org.skylark.bluewhale.domain.port.ContextUnitRepository;
import org.skylark.bluewhale.domain.port.EmbeddingPort;
import org.skylark.bluewhale.domain.port.IndexPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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
        KnowledgePatch patch = request.patch();
        var now = clock.now();
        ContextUnit unit = ContextUnit.create(UnitId.generate(), patch.scope(),
                UnitKind.SEMANTIC, Origin.DECLARED, UnitContent.of(patch.content()),
                new Lifecycle(org.skylark.bluewhale.domain.context.UnitState.PENDING_REVIEW,
                        now, null, now, null, null, now, now.plusSeconds(86400L * 365), null, null),
                Ownership.owned(patch.ownerId(), patch.ownerOrg(), 365),
                new Trust(1.0, 4), Economy.initial(0.8),
                Provenance.ofSource(patch.sourceRef()), Lineage.empty(), List.of());
        unitRepository.save(unit);
        lifecycle.transition(unit.id(), Transition.PENDING_REVIEW_TO_ACTIVE, actor,
                "released via change request " + request.id(), null);
        indexPort.index(new IndexPort.IndexEntry(unit.id(), unit.scope(),
                RepresentationLine.CURATED_DOC, unit.content().abstractText(),
                embeddingPort.embed(unit.content().abstractText()),
                Map.of("kind", unit.kind().name(), "origin", unit.origin().name(),
                        "state", org.skylark.bluewhale.domain.context.UnitState.ACTIVE.name())));
        return unit.id();
    }
}
