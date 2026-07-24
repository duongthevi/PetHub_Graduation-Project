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
    private String baseUrl = "https://api.openai.com/v1";
    private String apiKey;
    private String chatModel = "gpt-4.1-mini";
    private String embeddingModel = "text-embedding-3-small";
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
