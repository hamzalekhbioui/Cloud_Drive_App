package com.cloud.drive.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cloud.drive.storage.StorageService;
import com.cloud.drive.storage.UnavailableStorageService;

import java.util.Optional;

@Configuration
@EnableConfigurationProperties(AzureOpenAiProperties.class)
public class AzureBlobConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    @ConditionalOnProperty(name = "azure.storage.enabled", havingValue = "false", matchIfMissing = true)
    public StorageService unavailableStorageService() {
        return new UnavailableStorageService();
    }

    /**
     * Primary BlobServiceClient bean used by the new {@code AzureBlobStorageService}.
     * Returns null when the connection string is blank so the service can fail-fast.
     */
    @Bean
    @ConditionalOnProperty(name = "azure.storage.enabled", havingValue = "true")
    public BlobServiceClient blobServiceClient(
            @Value("${azure.storage.connection-string:}") String connectionString) {
        if (connectionString == null || connectionString.isBlank()) {
            return null;
        }
        return new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }

    /**
     * Legacy Optional wrapper retained for backward compatibility with the old
     * {@code BlobStorageService} (multipart proxy uploads).
     * TODO: remove once the legacy /upload endpoint is decommissioned.
     */
    @Bean
    @ConditionalOnProperty(name = "azure.storage.enabled", havingValue = "true")
    public Optional<BlobServiceClient> optionalBlobServiceClient(
            @Value("${azure.storage.connection-string:}") String connectionString) {
        if (connectionString == null || connectionString.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient());
    }
}