package org.skylark.bluewhale.domain.knowledge;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record WikiFrontmatter(
        String bindsProperty,
        EvidenceGrade grade,
        Instant effectiveFrom,
        String appliesTo,
        String sourceVersion,
        List<String> needs,
        List<String> triggers
) {

    public WikiFrontmatter {
        Objects.requireNonNull(grade, "wiki frontmatter grade is required");
        needs = needs == null ? List.of() : List.copyOf(needs);
        triggers = triggers == null ? List.of() : List.copyOf(triggers);
    }
}