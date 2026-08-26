package com.cloud.drive.ai;

import com.cloud.drive.config.AzureOpenAiProperties;
import com.cloud.drive.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
public class AzureOpenAiProvider implements EmbeddingProvider, ChatProvider {
    private final AzureOpenAiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public AzureOpenAiProvider(AzureOpenAiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isConfigured() { return properties.isConfigured(); }

    @Override
    public float[] embed(String text) {
        ensureConfigured();
        try {
            String body = objectMapper.writeValueAsString(java.util.Map.of("input", text));
            JsonNode root = post("/openai/deployments/" + properties.getEmbeddingDeployment() + "/embeddings", body);
            JsonNode values = root.path("data").path(0).path("embedding");
            if (!values.isArray() || values.isEmpty()) throw providerError("Embedding response was empty");
            float[] result = new float[values.size()];
            for (int i = 0; i < result.length; i++) result[i] = (float) values.get(i).asDouble();
            return result;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw providerError("Embedding provider request failed");
        }
    }

    @Override
    public String complete(List<ChatMessage> messages) {
        ensureConfigured();
        try {
            String body = objectMapper.writeValueAsString(java.util.Map.of(
                    "messages", messages.stream().map(m -> java.util.Map.of("role", m.role(), "content", m.content())).toList(),
                    "temperature", 0.1,
                    "max_tokens", 800));
            JsonNode root = post("/openai/deployments/" + properties.getChatDeployment() + "/chat/completions", body);
            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) throw providerError("Chat response was empty");
            return content;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw providerError("Chat provider request failed");
        }
    }

    private JsonNode post(String path, String body) throws Exception {
        String endpoint = properties.getEndpoint().replaceAll("/+$", "");
        URI uri = URI.create(endpoint + path + "?api-version=" + properties.getApiVersion());
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("api-key", properties.getApiKey())
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw providerError("Azure OpenAI request failed (" + response.statusCode() + ")");
        }
        return objectMapper.readTree(response.body());
    }

    private void ensureConfigured() {
        if (!properties.isConfigured()) {
            throw providerError("AI processing is not configured. Set Azure OpenAI endpoint, API key, API version, chat deployment, and embedding deployment.");
        }
    }

    private ApiException providerError(String message) {
        return new ApiException(message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
