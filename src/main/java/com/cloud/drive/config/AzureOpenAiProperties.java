package com.cloud.drive.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "azure.openai")
public class AzureOpenAiProperties {
    private String endpoint;
    private String apiKey;
    private String apiVersion = "2024-02-15-preview";
    private String chatDeployment;
    private String embeddingDeployment;

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getApiVersion() { return apiVersion; }
    public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
    public String getChatDeployment() { return chatDeployment; }
    public void setChatDeployment(String chatDeployment) { this.chatDeployment = chatDeployment; }
    public String getEmbeddingDeployment() { return embeddingDeployment; }
    public void setEmbeddingDeployment(String embeddingDeployment) { this.embeddingDeployment = embeddingDeployment; }

    public boolean isConfigured() {
        return endpoint != null && !endpoint.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiVersion != null && !apiVersion.isBlank()
                && chatDeployment != null && !chatDeployment.isBlank()
                && embeddingDeployment != null && !embeddingDeployment.isBlank();
    }
}
