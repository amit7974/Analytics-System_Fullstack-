package com.example.analytics.service.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Real semantic embedding provider backed by the OpenAI Embeddings API.
 * Activate with: embedding.provider=openai and openai.api-key=sk-...
 */
@Service
@ConditionalOnProperty(name = "embedding.provider", havingValue = "openai")
public class OpenAiEmbeddingService implements EmbeddingService {

    private static final String ENDPOINT = "https://api.openai.com/v1/embeddings";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;

    public OpenAiEmbeddingService(RestTemplate restTemplate,
                                   @Value("${openai.api-key:}") String apiKey,
                                   @Value("${openai.embedding-model:text-embedding-3-small}") String model) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public double[] embed(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "embedding.provider=openai requires openai.api-key to be set (env OPENAI_API_KEY)");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", model,
                "input", text
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        JsonNode response = restTemplate.postForObject(ENDPOINT, request, JsonNode.class);

        if (response == null || !response.has("data") || response.get("data").isEmpty()) {
            throw new IllegalStateException("OpenAI embeddings API returned an unexpected response");
        }

        JsonNode embeddingNode = response.get("data").get(0).get("embedding");
        double[] vector = new double[embeddingNode.size()];
        for (int i = 0; i < embeddingNode.size(); i++) {
            vector[i] = embeddingNode.get(i).asDouble();
        }
        return vector;
    }

    @Override
    public String providerName() {
        return "openai:" + model;
    }
}
