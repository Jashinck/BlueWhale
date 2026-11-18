package org.skylark.bluewhale.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skylark.bluewhale.domain.knowledge.EvidenceGrade;
import org.skylark.bluewhale.domain.knowledge.WikiFrontmatter;
import org.skylark.bluewhale.domain.knowledge.WikiPage;
import org.skylark.bluewhale.domain.port.ConnectorPort;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitWikiStoreAdapterTest {

    private static final Instant NOW = Instant.parse("2026-11-12T08:00:00Z");

    @TempDir
    Path tempDir;

    @Test
    void writeThenReadRoundTrips() {
        GitWikiStoreAdapter store = new GitWikiStoreAdapter(tempDir.resolve("wiki"));
        WikiFrontmatter frontmatter = new WikiFrontmatter("deposit_refund_condition", EvidenceGrade.SOURCE,
                NOW, "lease", "v1", List.of("amount"), List.of("deposit"));
        WikiPage page = new WikiPage("rules/deposit.md", frontmatter, "# 保证金\n正常退租应退还。", "sha256:input");

        store.write(page);

        assertTrue(store.exists("rules/deposit.md"));
        WikiPage read = store.read("rules/deposit.md");
        assertEquals("deposit_refund_condition", read.frontmatter().bindsProperty());
        assertEquals(EvidenceGrade.SOURCE, read.frontmatter().grade());
        assertEquals(NOW, read.frontmatter().effectiveFrom());
        assertEquals(List.of("amount"), read.frontmatter().needs());
        assertEquals("# 保证金\n正常退租应退还。", read.markdown());
        assertTrue(read.sourceHash().startsWith("sha256:"));
        assertEquals(List.of("rules/deposit.md"), store.listPages());
    }

    @Test
    void connectorFetchesFileContent() throws Exception {
        Path source = tempDir.resolve("source.md");
        Files.writeString(source, "合同文本");

        ConnectorPort.FetchedContent fetched = new FilesystemConnectorAdapter().fetch(source.toString());

        assertEquals("合同文本", fetched.content());
        assertTrue(fetched.contentHash().startsWith("sha256:"));
    }
}