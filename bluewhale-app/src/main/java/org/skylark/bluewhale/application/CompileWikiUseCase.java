package org.skylark.bluewhale.application;

import org.skylark.bluewhale.domain.context.ContextUnit;
import org.skylark.bluewhale.domain.context.Economy;
import org.skylark.bluewhale.domain.context.Lifecycle;
import org.skylark.bluewhale.domain.context.Lineage;
import org.skylark.bluewhale.domain.context.Origin;
import org.skylark.bluewhale.domain.context.Ownership;
import org.skylark.bluewhale.domain.context.Provenance;
import org.skylark.bluewhale.domain.context.RepresentationLine;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.Trust;
import org.skylark.bluewhale.domain.context.UnitContent;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.context.UnitKind;
import org.skylark.bluewhale.domain.context.UnitState;
import org.skylark.bluewhale.domain.knowledge.GateReport;
import org.skylark.bluewhale.domain.knowledge.KnowledgeValidationGate;
import org.skylark.bluewhale.domain.knowledge.WikiPage;
import org.skylark.bluewhale.domain.port.ClockPort;
import org.skylark.bluewhale.domain.port.ContextUnitRepository;
import org.skylark.bluewhale.domain.port.EmbeddingPort;
import org.skylark.bluewhale.domain.port.IndexPort;
import org.skylark.bluewhale.domain.port.OntologyPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class CompileWikiUseCase {

    private final ContextUnitRepository unitRepository;
    private final IndexPort indexPort;
    private final EmbeddingPort embeddingPort;
    private final OntologyPort ontologyPort;
    private final ClockPort clock;

    public CompileWikiUseCase(ContextUnitRepository unitRepository, IndexPort indexPort,
                              EmbeddingPort embeddingPort, OntologyPort ontologyPort, ClockPort clock) {
        this.unitRepository = unitRepository;
        this.indexPort = indexPort;
        this.embeddingPort = embeddingPort;
        this.ontologyPort = ontologyPort;
        this.clock = clock;
    }

    @Transactional
    public CompileResult compile(WikiPage page, Scope scope, String ownerId) {
        KnowledgeValidationGate gate = new KnowledgeValidationGate(ontologyPort.load());
        GateReport report = gate.validatePage(page);
        if (!report.isAccepted()) {
            throw new IllegalStateException("wiki page rejected by gate: " + report.reasons());
        }
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("compiled knowledge requires an owner");
        }
        Instant now = clock.now();
        // sourceRef 携带 path@sourceHash，作为编译产物与 Wiki 真源的漂移检测绑定（P1 约定）。
        ContextUnit unit = ContextUnit.create(UnitId.generate(), scope, UnitKind.SEMANTIC, Origin.DECLARED,
                UnitContent.of(page.markdown()),
                new Lifecycle(UnitState.ACTIVE, now, null, now, null, null, now, null, null, null),
                Ownership.owned(ownerId, null, 365),
                new Trust(1.0, 4), Economy.initial(0.8),
                Provenance.ofSource(page.path() + "@" + page.sourceHash()),
                Lineage.empty(), List.of());
        unitRepository.save(unit);
        float[] embedding = embeddingPort.embed(unit.content().abstractText());
        indexPort.index(new IndexPort.IndexEntry(unit.id(), scope, RepresentationLine.CURATED_DOC,
                unit.content().abstractText(), embedding,
                Map.of("kind", unit.kind().name(), "origin", unit.origin().name(),
                        "state", unit.lifecycle().state().name())));
        return new CompileResult(unit.id().toString(), page.path(), page.sourceHash());
    }
}