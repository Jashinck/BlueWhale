package org.skylark.bluewhale.infrastructure.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.skylark.bluewhale.domain.context.UnitId;
import org.skylark.bluewhale.domain.knowledge.Claim;
import org.skylark.bluewhale.domain.knowledge.EvidenceSpan;
import org.skylark.bluewhale.domain.ontology.OntologySchema;
import org.skylark.bluewhale.domain.ontology.PropertyDef;
import org.skylark.bluewhale.domain.port.ClaimExtractionPort;
import org.skylark.bluewhale.domain.port.OntologyPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class OpenAICompatClaimExtractionAdapter implements ClaimExtractionPort {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final OntologyPort ontologyPort;
    private final String apiKey;
    private final String model;

    public OpenAICompatClaimExtractionAdapter(
            @Value("${bluewhale.extract.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${bluewhale.extract.api-key:}") String apiKey,
            @Value("${bluewhale.extract.model:gpt-4o-mini}") String model,
            ObjectMapper objectMapper, OntologyPort ontologyPort) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.ontologyPort = ontologyPort;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public List<Claim> extractClaims(String sourceText) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("No extract API key configured; returning no claims");
            return List.of();
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            ArrayNode messages = body.putArray("messages");
            ObjectNode system = messages.addObject();
            system.put("role", "system");
            system.put("content", "You extract factual knowledge claims from source text. "
                    + "Return ONLY a JSON array, no prose or markdown. Each element: "
                    + "{\"text\":\"<claim>\",\"quote\":\"<exact verbatim substring of the source>\","
                    + "\"bindsProperty\":\"<property id or null>\"}. "
                    + "The quote must appear verbatim in the source.");
            ObjectNode user = messages.addObject();
            user.put("role", "user");
            user.put("content", buildUserPrompt(sourceText));

            String response = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            String content = objectMapper.readTree(response).at("/choices/0/message/content").asText();
            return parseClaims(content, sourceText);
        } catch (Exception e) {
            log.warn("Claim extraction API call failed: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildUserPrompt(String sourceText) {
        OntologySchema schema = ontologyPort.load();
        String properties = String.join(", ", schema.properties().stream().map(PropertyDef::id).toList());
        return "Allowed bindsProperty values: [" + properties + "]\n\nSource text:\n" + sourceText;
    }

    List<Claim> parseClaims(String content, String sourceText) {
        try {
            String json = stripFences(content);
            JsonNode array = objectMapper.readTree(json);
            List<Claim> claims = new ArrayList<>();
            for (JsonNode node : array) {
                String text = node.path("text").asText(null);
                String quote = node.path("quote").asText(null);
                String bindsProperty = node.path("bindsProperty").asText(null);
                if (text == null || text.isBlank() || quote == null) {
                    continue;
                }
                int start = sourceText.indexOf(quote);
                if (start < 0) {
                    continue;
                }
                if (bindsProperty != null && (bindsProperty.isBlank() || "null".equals(bindsProperty))) {
                    bindsProperty = null;
                }
                claims.add(Claim.source(UnitId.generate(), text,
                        new EvidenceSpan("source", start, start + quote.length()), bindsProperty));
            }
            return claims;
        } catch (Exception e) {
            log.warn("Cannot parse extraction result: {}", e.getMessage());
            return List.of();
        }
    }

    private static String stripFences(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}