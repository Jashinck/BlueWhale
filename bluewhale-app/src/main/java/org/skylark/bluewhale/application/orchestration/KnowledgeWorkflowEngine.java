package org.skylark.bluewhale.application.orchestration;

import org.skylark.bluewhale.application.ChangeRequestDecision;
import org.skylark.bluewhale.application.GovernResult;
import org.skylark.bluewhale.application.GovernReviewUseCase;
import org.skylark.bluewhale.application.IngestCommand;
import org.skylark.bluewhale.application.IngestContextUseCase;
import org.skylark.bluewhale.application.IngestResult;
import org.skylark.bluewhale.domain.context.Actor;
import org.skylark.bluewhale.domain.context.Scope;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.knowledge.Claim;
import org.skylark.bluewhale.domain.knowledge.EvidenceGrade;
import org.skylark.bluewhale.domain.knowledge.GateReport;
import org.skylark.bluewhale.domain.knowledge.KnowledgeValidationGate;
import org.skylark.bluewhale.domain.knowledge.WikiFrontmatter;
import org.skylark.bluewhale.domain.knowledge.WikiPage;
import org.skylark.bluewhale.domain.orchestration.WorkflowInstance;
import org.skylark.bluewhale.domain.orchestration.WorkflowState;
import org.skylark.bluewhale.domain.port.ClaimExtractionPort;
import org.skylark.bluewhale.domain.port.ClockPort;
import org.skylark.bluewhale.domain.port.ConnectorPort;
import org.skylark.bluewhale.domain.port.OntologyPort;
import org.skylark.bluewhale.domain.port.WikiStorePort;
import org.skylark.bluewhale.domain.port.WorkflowInstanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class KnowledgeWorkflowEngine {

    private final ConnectorPort connector;
    private final WikiStorePort wikiStore;
    private final Optional<ClaimExtractionPort> extractor;
    private final OntologyPort ontology;
    private final IngestContextUseCase ingest;
    private final GovernReviewUseCase govern;
    private final WorkflowInstanceRepository workflows;
    private final ClockPort clock;

    public KnowledgeWorkflowEngine(ConnectorPort connector, WikiStorePort wikiStore,
                                   Optional<ClaimExtractionPort> extractor, OntologyPort ontology,
                                   IngestContextUseCase ingest, GovernReviewUseCase govern,
                                   WorkflowInstanceRepository workflows, ClockPort clock) {
        this.connector = connector;
        this.wikiStore = wikiStore;
        this.extractor = extractor;
        this.ontology = ontology;
        this.ingest = ingest;
        this.govern = govern;
        this.workflows = workflows;
        this.clock = clock;
    }

    @Transactional
    public WorkflowInstance start(String sourceUri, Scope scope, String ownerId) {
        WorkflowInstance workflow = WorkflowInstance.start(sourceUri, scope, ownerId, clock.now());
        try {
            ConnectorPort.FetchedContent source = connector.fetch(sourceUri);
            if (extractor.isEmpty()) {
                throw new IllegalStateException("no claim extractor configured");
            }
            List<Claim> claims = extractor.get().extractClaims(source.content());
            if (claims.isEmpty()) {
                throw new IllegalStateException("no claims extracted from source");
            }
            WikiPage page = draftPage(claims, source);
            GateReport report = new KnowledgeValidationGate(ontology.load()).validatePage(page);
            if (!report.isAccepted()) {
                throw new IllegalStateException("wiki page rejected by gate: " + report.reasons());
            }
            wikiStore.write(page);
            IngestResult changeRequest = ingest.submit(new IngestCommand(
                    "wf-" + workflow.id(), "SYSTEM", scope, ownerId, null, page.markdown(), page.path()));
            workflow = workflow.awaitingReview(changeRequest.crId(), clock.now());
        } catch (RuntimeException failure) {
            workflow = workflow.failed(clock.now());
        }
        workflows.save(workflow);
        return workflow;
    }

    @Transactional
    public WorkflowInstance resume(UnitId workflowId, Actor reviewer, boolean approved) {
        WorkflowInstance workflow = workflows.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("workflow not found: " + workflowId));
        if (workflow.state() != WorkflowState.AWAITING_REVIEW) {
            throw new IllegalStateException("workflow is not awaiting review: " + workflow.state());
        }
        Instant now = clock.now();
        if (!approved) {
            WorkflowInstance rejected = workflow.failed(now);
            workflows.save(rejected);
            return rejected;
        }
        UnitId crId = UnitId.of(workflow.changeRequestId());
        govern.decide(new ChangeRequestDecision(crId, ChangeRequestDecision.Action.SUBMIT, reviewer, null));
        govern.decide(new ChangeRequestDecision(crId, ChangeRequestDecision.Action.APPROVE, reviewer, null));
        govern.decide(new ChangeRequestDecision(crId, ChangeRequestDecision.Action.REGRESS, reviewer, null));
        govern.decide(new ChangeRequestDecision(crId, ChangeRequestDecision.Action.READY, reviewer, null));
        GovernResult released = govern.decide(new ChangeRequestDecision(crId, ChangeRequestDecision.Action.RELEASE, reviewer, null));
        WorkflowInstance completed = workflow.completed(released.appliedUnitId(), now);
        workflows.save(completed);
        return completed;
    }

    private WikiPage draftPage(List<Claim> claims, ConnectorPort.FetchedContent source) {
        String markdown = String.join("\n", claims.stream().map(Claim::text).toList());
        String bindsProperty = claims.stream().map(Claim::bindsProperty)
                .filter(Objects::nonNull).findFirst().orElse(null);
        WikiFrontmatter frontmatter = new WikiFrontmatter(bindsProperty, EvidenceGrade.SOURCE,
                clock.now(), null, "v1", List.of(), List.of());
        String path = "knowledge/" + Integer.toHexString(source.uri().hashCode()) + ".md";
        return new WikiPage(path, frontmatter, markdown, source.contentHash());
    }
}