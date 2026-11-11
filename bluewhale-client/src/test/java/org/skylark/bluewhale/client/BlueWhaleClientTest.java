package org.skylark.bluewhale.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlueWhaleClientTest {

    private HttpServer server;
    private BlueWhaleClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/context/resolve", exchange -> {
            String json = "{\"traceId\":\"t-1\",\"releaseId\":\"rel-1\",\"gate\":\"RETRIEVAL_BEARING\","
                    + "\"emptyReason\":null,\"sections\":[{\"slot\":\"CORE\",\"unitId\":\"u-1\","
                    + "\"kind\":\"WORKING\",\"depth\":\"OVERVIEW\",\"text\":\"dark theme\","
                    + "\"render\":\"PROSE\",\"trust\":{\"confidence\":0.9,\"origin\":\"OBSERVED\","
                    + "\"state\":\"ACTIVE\"},\"validity\":{\"validFrom\":null,\"validTo\":null},"
                    + "\"contentHash\":\"sha256:abc\",\"citation\":\"u-1@abc#anchor-1\","
                    + "\"provenance\":{\"episode\":null,\"source\":null,\"spanStart\":null,\"spanEnd\":null}}],"
                    + "\"omitted\":[],\"notes\":[],\"requiredFacts\":[],"
                    + "\"cost\":{\"tokens\":2,\"retrievalLlmCalls\":0,\"elapsedMs\":3},\"trace\":[]}";
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/api/context/feedback", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        client = new BlueWhaleClient("http://localhost:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldDeserializeResolveResponse() {
        ContextBundleResponse bundle = client.resolve(new BlueWhaleClient.ResolveRequest(
                "t-1", "acme", null, null, "agent-1", null, null,
                "TASK", "dark theme", 500, List.of("CURATED_DOC"), "OVERVIEW"));

        assertEquals("rel-1", bundle.releaseId());
        assertEquals(1, bundle.sections().size());
        assertEquals("dark theme", bundle.sections().get(0).text());
        assertEquals(0, bundle.cost().retrievalLlmCalls());
    }

    @Test
    void shouldAcceptNoContentFeedbackResponse() {
        client.feedback(new BlueWhaleClient.FeedbackRequest("t-1",
                List.of(new BlueWhaleClient.FeedbackRequest.Item("u-1", "ADOPTED", 5))));
    }

    @Test
    void shouldThrowOnUnexpectedStatus() {
        BlueWhaleClient wrongPort = new BlueWhaleClient(
                "http://localhost:" + server.getAddress().getPort() + "/missing");
        assertThrows(BlueWhaleClient.BlueWhaleClientException.class,
                () -> wrongPort.resolve(new BlueWhaleClient.ResolveRequest(
                        "t-1", "acme", null, null, "agent-1", null, null,
                        "TASK", "q", 100, List.of("CURATED_DOC"), "OVERVIEW")));
    }
}
