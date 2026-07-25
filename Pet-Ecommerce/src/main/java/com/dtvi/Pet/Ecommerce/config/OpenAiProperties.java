package com.dtvi.Pet.Ecommerce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai")
@Getter
@Setter
public class OpenAiProperties {
    private String baseUrl = "http://localhost:11434/v1";
    private String apiKey = "ollama";
    private String chatModel = "qwen2.5:3b-instruct";
    private String embeddingModel = "nomic-embed-text";
    private int topK = 5;
    private double minimumScore = 0.2;
    private int chunkSize = 900;
    private int chunkOverlap = 120;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && baseUrl != null && !baseUrl.isBlank()
                && chatModel != null && !chatModel.isBlank()
                && embeddingModel != null && !embeddingModel.isBlank();
    }
}
