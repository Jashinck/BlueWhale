package org.skylark.bluewhale.domain.knowledge;

import org.skylark.bluewhale.domain.ontology.OntologySchema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class KnowledgeValidationGate {

    private final OntologySchema ontology;

    public KnowledgeValidationGate(OntologySchema ontology) {
        this.ontology = Objects.requireNonNull(ontology, "ontology is required");
    }

    public GateReport validatePage(WikiPage page) {
        List<String> reasons = new ArrayList<>();
        String binds = page.frontmatter().bindsProperty();
        if (binds == null || binds.isBlank()) {
            reasons.add("binds_property is required for rule pages");
        } else if (!ontology.hasProperty(binds)) {
            reasons.add("binds_property not defined in ontology: " + binds);
        }
        return reasons.isEmpty() ? GateReport.accept() : GateReport.of(ValidationOutcome.REJECT, reasons);
    }

    public GateReport validateClaim(Claim claim, String sourceText) {
        List<String> reasons = new ArrayList<>();
        ValidationOutcome outcome = ValidationOutcome.ACCEPT;
        if (claim.bindsProperty() != null && !ontology.hasProperty(claim.bindsProperty())) {
            reasons.add("property not defined in ontology: " + claim.bindsProperty());
            outcome = ValidationOutcome.REJECT;
        }
        if (claim.grade() == EvidenceGrade.SOURCE && sourceText != null) {
            EvidenceSpan span = claim.evidenceSpan();
            if (span.start() > sourceText.length() || span.end() > sourceText.length()) {
                reasons.add("evidence span not locatable in source; downgrade required");
                if (outcome != ValidationOutcome.REJECT) {
                    outcome = ValidationOutcome.DOWNGRADE;
                }
            }
        }
        return outcome == ValidationOutcome.ACCEPT ? GateReport.accept() : GateReport.of(outcome, reasons);
    }

    public Optional<String> detectPropertyConflict(List<Claim> claims) {
        Map<String, Set<String>> byProperty = new HashMap<>();
        for (Claim claim : claims) {
            if (claim.bindsProperty() == null) {
                continue;
            }
            byProperty.computeIfAbsent(claim.bindsProperty(), key -> new HashSet<>()).add(claim.text());
        }
        return byProperty.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .findFirst();
    }
}