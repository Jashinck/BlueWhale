package org.skylark.bluewhale.infrastructure;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.skylark.bluewhale.domain.knowledge.EvidenceGrade;
import org.skylark.bluewhale.domain.knowledge.WikiFrontmatter;
import org.skylark.bluewhale.domain.knowledge.WikiPage;
import org.skylark.bluewhale.domain.port.WikiStorePort;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class GitWikiStoreAdapter implements WikiStorePort {

    private static final PersonIdent AUTHOR = new PersonIdent("BlueWhale", "bluewhale@local");

    private final Path repoDir;
    private final Git git;

    public GitWikiStoreAdapter(Path repoDir) {
        this.repoDir = repoDir;
        try {
            Files.createDirectories(repoDir);
            this.git = Files.exists(repoDir.resolve(".git"))
                    ? Git.open(repoDir.toFile())
                    : Git.init().setDirectory(repoDir.toFile()).call();
        } catch (Exception e) {
            throw new IllegalStateException("cannot open wiki git repo: " + repoDir, e);
        }
    }

    @Override
    public WikiPage read(String path) {
        String raw = readFile(path);
        int end = raw.indexOf("\n---\n", 4);
        if (!raw.startsWith("---\n") || end < 0) {
            throw new IllegalArgumentException("wiki page missing frontmatter: " + path);
        }
        String yaml = raw.substring(4, end);
        String body = raw.substring(end + 5);
        Map<String, Object> map = new Yaml().load(yaml);
        WikiFrontmatter frontmatter = new WikiFrontmatter(
                str(map.get("bindsProperty")),
                EvidenceGrade.valueOf(str(map.get("grade"))),
                map.get("effectiveFrom") == null ? null : Instant.parse(str(map.get("effectiveFrom"))),
                str(map.get("appliesTo")),
                str(map.get("sourceVersion")),
                strList(map.get("needs")),
                strList(map.get("triggers")));
        return new WikiPage(path, frontmatter, body, sha256(body));
    }

    @Override
    public void write(WikiPage page) {
        String content = serialize(page);
        Path target = repoDir.resolve(page.path());
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, content, StandardCharsets.UTF_8);
            git.add().addFilepattern(page.path()).call();
            git.commit().setMessage("wiki: " + page.path())
                    .setAuthor(AUTHOR).setCommitter(AUTHOR).call();
        } catch (Exception e) {
            throw new IllegalStateException("cannot write wiki page: " + page.path(), e);
        }
    }

    @Override
    public List<String> listPages() {
        try (Stream<Path> paths = Files.walk(repoDir)) {
            return paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .map(p -> repoDir.relativize(p).toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean exists(String path) {
        return Files.exists(repoDir.resolve(path));
    }

    private String readFile(String path) {
        try {
            return Files.readString(repoDir.resolve(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read wiki page: " + path, e);
        }
    }

    private static String serialize(WikiPage page) {
        WikiFrontmatter fm = page.frontmatter();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("bindsProperty", fm.bindsProperty());
        map.put("grade", fm.grade().name());
        map.put("effectiveFrom", fm.effectiveFrom() == null ? null : fm.effectiveFrom().toString());
        map.put("appliesTo", fm.appliesTo());
        map.put("sourceVersion", fm.sourceVersion());
        map.put("needs", fm.needs());
        map.put("triggers", fm.triggers());
        return "---\n" + new Yaml().dump(map) + "---\n" + page.markdown();
    }

    private static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static List<String> strList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            result.add(String.valueOf(item));
        }
        return result;
    }
}