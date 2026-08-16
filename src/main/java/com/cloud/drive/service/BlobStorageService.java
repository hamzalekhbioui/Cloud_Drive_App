package com.cloud.drive.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.cloud.drive.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class BlobStorageService {

    private final BlobServiceClient blobServiceClient;
    private final String containerName;

    public BlobStorageService(Optional<BlobServiceClient> blobServiceClient,
                              @Value("${azure.storage.container-name}") String containerName) {
        this.blobServiceClient = blobServiceClient.orElse(null);
        this.containerName = containerName;
    }

    private void requireAzure() {
        if (blobServiceClient == null) {
            throw new ApiException(
                "Azure Storage is not configured. Set the AZURE_STORAGE_CONNECTION_STRING environment variable to enable file uploads.",
                HttpStatus.SERVICE_UNAVAILABLE
            );
        }
    }

    /**
     * Uploads a file by proxying it through the backend.
     *
     * @deprecated Use the two-phase direct-to-storage flow via {@code StorageService} instead.
     *             This method buffers the entire file in the backend heap, wasting memory and bandwidth.
     *             Retained only for backward compatibility with the legacy POST /upload endpoint.
     *
     * @param file                the multipart file
     * @param blobFileName        the blob key
     * @param validatedContentType the server-detected MIME type (never trust client header)
     */
    @Deprecated
    public String uploadFile(MultipartFile file, String blobFileName, String validatedContentType) throws IOException {
        requireAzure();
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        containerClient.createIfNotExists();
        BlobClient blobClient = containerClient.getBlobClient(blobFileName);

        blobClient.upload(file.getInputStream(), file.getSize(), true);

        // P2.1 — use server-detected MIME type; default to attachment to prevent XSS
        BlobHttpHeaders headers = new BlobHttpHeaders()
                .setContentType(validatedContentType)
                .setContentDisposition("attachment");
        blobClient.setHttpHeaders(headers);

        return generateSasUrl(blobClient);
    }

    public void streamToOutput(String blobFileName, java.io.OutputStream outputStream) throws IOException {
        requireAzure();
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobFileName);
        try (java.io.InputStream is = blobClient.openInputStream()) {
            is.transferTo(outputStream);
        }
    }

    public void deleteFile(String blobFileName) {
        requireAzure();
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobFileName);
        blobClient.deleteIfExists();
    }

    public String generateSasUrlForBlob(String blobFileName) {
        requireAzure();
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobFileName);
        return generateSasUrl(blobClient);
    }

    private String generateSasUrl(BlobClient blobClient) {
        BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
        OffsetDateTime expiryTime = OffsetDateTime.now().plusHours(1);

        // P2.1 — default to attachment to prevent stored XSS;
        // inline rendering is handled by the streaming endpoint with MimePolicy checks
        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiryTime, permission)
                .setContentDisposition("attachment");

        String sasToken = blobClient.generateSas(values);
        return blobClient.getBlobUrl() + "?" + sasToken;
    }
}
