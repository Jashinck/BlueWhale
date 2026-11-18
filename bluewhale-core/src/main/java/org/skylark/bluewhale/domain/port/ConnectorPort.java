package org.skylark.bluewhale.domain.port;

public interface ConnectorPort {

    FetchedContent fetch(String uri);

    record FetchedContent(String uri, String content, String contentHash) {
    }
}