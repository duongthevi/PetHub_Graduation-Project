package com.dtvi.Pet.Ecommerce.ai;

import com.dtvi.Pet.Ecommerce.config.OpenAiProperties;
import com.dtvi.Pet.Ecommerce.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatibleClient {

    private final OpenAiProperties properties;
    private final RestClient.Builder restClientBuilder;

    public OpenAiCompatibleClient(OpenAiProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    public List<List<Double>> embed(List<String> inputs) {
        requireConfigured();
        try {
            JsonNode response = client().post()
                    .uri("/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", properties.getEmbeddingModel(),
                            "input", inputs,
                            "encoding_format", "float"
                    ))
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.path("data").isArray()) {
                throw unavailable();
            }

            List<IndexedEmbedding> indexed = new ArrayList<>();
            for (JsonNode item : response.path("data")) {
                List<Double> vector = new ArrayList<>();
                for (JsonNode value : item.path("embedding")) {
                    vector.add(value.asDouble());
                }
                indexed.add(new IndexedEmbedding(item.path("index").asInt(), vector));
            }
            indexed.sort(Comparator.comparingInt(IndexedEmbedding::index));
            if (indexed.size() != inputs.size()) {
                throw unavailable();
            }
            return indexed.stream().map(IndexedEmbedding::vector).toList();
        } catch (RestClientException ex) {
            throw unavailable();
        }
    }

    public String chat(List<ChatTurn> messages) {
        requireConfigured();
        try {
            List<Map<String, String>> payloadMessages = messages.stream()
                    .map(message -> Map.of("role", message.role(), "content", message.content()))
                    .toList();
            JsonNode response = client().post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", properties.getChatModel(),
                            "temperature", 0.2,
                            "messages", payloadMessages
                    ))
                    .retrieve()
                    .body(JsonNode.class);
            String content = response == null
                    ? null
                    : response.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                throw unavailable();
            }
            return content.trim();
        } catch (RestClientException ex) {
            throw unavailable();
        }
    }

    private RestClient client() {
        String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
        return restClientBuilder
                .clone()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .build();
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw BusinessException.badRequest(
                    "AI_NOT_CONFIGURED",
                    "OpenAI-compatible API has not been configured"
            );
        }
    }

    private BusinessException unavailable() {
        return new BusinessException(
                org.springframework.http.HttpStatus.BAD_GATEWAY,
                "AI_PROVIDER_UNAVAILABLE",
                "The AI provider is temporarily unavailable"
        );
    }

    public record ChatTurn(String role, String content) {
    }

    private record IndexedEmbedding(int index, List<Double> vector) {
    }
}
