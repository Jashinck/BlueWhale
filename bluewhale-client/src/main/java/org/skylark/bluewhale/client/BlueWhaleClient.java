package org.skylark.bluewhale.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class BlueWhaleClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BlueWhaleClient(String baseUrl) {
        this(baseUrl, HttpClient.newHttpClient(), new ObjectMapper());
    }

    public BlueWhaleClient(String baseUrl, HttpClient httpClient, ObjectMapper objectMapper) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public ContextBundleResponse resolve(ResolveRequest request) {
        return post("/api/context/resolve", request, ContextBundleResponse.class);
    }

    public CaptureResponse capture(CaptureRequest request) {
        return post("/api/context/memories", request, CaptureResponse.class);
    }

    public void feedback(FeedbackRequest request) {
        send(post(request, "/api/context/feedback"), 204, Void.class);
    }

    public TransitionResponse transition(String unitId, TransitionRequest request) {
        return post("/api/context/units/" + unitId + "/transitions", request,
                TransitionResponse.class);
    }

    public List<LifecycleEventResponse> lifecycleEvents(String unitId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/context/units/" + unitId + "/lifecycle-events"))
                .GET()
                .build();
        return send(request, 200, new TypeReference<>() {
        });
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        return send(post(body, path), 200, responseType);
    }

    private HttpRequest post(Object body, String path) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(body);
            return HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                    .build();
        } catch (IOException e) {
            throw new BlueWhaleClientException("cannot serialize request body", e);
        }
    }

    private <T> T send(HttpRequest request, int expectedStatus, Class<T> responseType) {
        try {
            HttpResponse<byte[]> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofByteArray());
            ensureStatus(response, expectedStatus);
            if (responseType == Void.class) {
                return null;
            }
            return objectMapper.readValue(response.body(), responseType);
        } catch (IOException e) {
            throw new BlueWhaleClientException("cannot read response", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BlueWhaleClientException("request interrupted", e);
        }
    }

    private <T> T send(HttpRequest request, int expectedStatus, TypeReference<T> typeReference) {
        try {
            HttpResponse<byte[]> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofByteArray());
            ensureStatus(response, expectedStatus);
            return objectMapper.readValue(response.body(), typeReference);
        } catch (IOException e) {
            throw new BlueWhaleClientException("cannot read response", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BlueWhaleClientException("request interrupted", e);
        }
    }

    private static void ensureStatus(HttpResponse<byte[]> response, int expectedStatus) {
        if (response.statusCode() != expectedStatus) {
            throw new BlueWhaleClientException("unexpected status " + response.statusCode()
                    + ": " + new String(response.body()));
        }
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public record ResolveRequest(
            String traceId,
            String tenant,
            String workspace,
            String project,
            String agentId,
            String userId,
            String sessionId,
            String intent,
            String query,
            Integer maxTokens,
            List<String> lines,
            String maxDepth
    ) {
    }

    public record CaptureRequest(
            String traceId,
            String tenant,
            String workspace,
            String project,
            String agentId,
            String userId,
            String sessionId,
            String source,
            String text,
            String kind,
            Double confidence,
            List<String> tags
    ) {
    }

    public record FeedbackRequest(String traceId, List<Item> items) {

        public record Item(String unitId, String outcome, Integer tokens) {
        }
    }

    public record TransitionRequest(
            String transition,
            String actorId,
            String actorType,
            String reason,
            String batchId
    ) {
    }

    public record CaptureResponse(
            String traceId,
            String episodeId,
            String unitId,
            String state,
            boolean indexed
    ) {
    }

    public record TransitionResponse(String unitId, String state, LifecycleEventResponse event) {
    }

    public record LifecycleEventResponse(
            String eventId,
            String unitId,
            String fromState,
            String toState,
            String trigger,
            String actorId,
            String actorType,
            String occurredAt,
            String reason,
            String batchId
    ) {
    }

    public static class BlueWhaleClientException extends RuntimeException {
        public BlueWhaleClientException(String message) {
            super(message);
        }

        public BlueWhaleClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
