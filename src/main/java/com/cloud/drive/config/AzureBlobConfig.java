package com.cloud.drive.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureBlobConfig {

    @Bean
    @ConditionalOnProperty(name = "azure.storage.enabled", havingValue = "true")
    public BlobServiceClient blobServiceClient(
            @Value("${azure.storage.connection-string}") String connectionString) {
        return new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
    }
}