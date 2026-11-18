package org.skylark.bluewhale.infrastructure;

import org.skylark.bluewhale.domain.port.ConnectorPort;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

public class FilesystemConnectorAdapter implements ConnectorPort {

    @Override
    public FetchedContent fetch(String uri) {
        try {
            String content = Files.readString(Path.of(uri), StandardCharsets.UTF_8);
            return new FetchedContent(uri, content, sha256(content));
        } catch (IOException e) {
            throw new UncheckedIOException("cannot fetch source: " + uri, e);
        }
    }

    private static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}