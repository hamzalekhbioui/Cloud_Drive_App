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

    public String uploadFile(MultipartFile file, String blobFileName) throws IOException {
        requireAzure();
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        containerClient.createIfNotExists();
        BlobClient blobClient = containerClient.getBlobClient(blobFileName);

        blobClient.upload(file.getInputStream(), file.getSize(), true);

        // Set content type so browsers can render the file inline
        BlobHttpHeaders headers = new BlobHttpHeaders()
                .setContentType(file.getContentType())
                .setContentDisposition("inline");
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

        // setContentDisposition("inline") overrides the response header via the rscd SAS parameter,
        // forcing browsers to display the file rather than download it — works for old and new blobs
        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiryTime, permission)
                .setContentDisposition("inline");

        String sasToken = blobClient.generateSas(values);
        return blobClient.getBlobUrl() + "?" + sasToken;
    }
}
