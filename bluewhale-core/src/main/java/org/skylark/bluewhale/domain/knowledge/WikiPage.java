package org.skylark.bluewhale.domain.knowledge;

import java.util.Objects;

public record WikiPage(
        String path,
        WikiFrontmatter frontmatter,
        String markdown,
        String sourceHash
) {

    public WikiPage {
        Objects.requireNonNull(frontmatter, "wiki frontmatter is required");
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("wiki page path is required");
        }
        if (markdown == null || markdown.isBlank()) {
            throw new IllegalArgumentException("wiki page markdown is required");
        }
        if (sourceHash == null || sourceHash.isBlank()) {
            throw new IllegalArgumentException("wiki page source hash is required");
        }
    }
}