package com.cloud.drive.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class BlobStorageService {

    private final BlobServiceClient blobServiceClient;
    private final String containerName;

    public BlobStorageService(BlobServiceClient blobServiceClient,
                              @Value("${azure.storage.container-name}") String containerName) {
        this.blobServiceClient = blobServiceClient;
        this.containerName = containerName;
    }

    public String uploadFile(MultipartFile file, String blobFileName) throws IOException {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobFileName);

        blobClient.upload(file.getInputStream(), file.getSize(), true);
        return generateSasUrl(blobClient);
    }

    public void deleteFile(String blobFileName) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobFileName);
        blobClient.deleteIfExists();
    }

    public String generateSasUrlForBlob(String blobFileName) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobFileName);
        return generateSasUrl(blobClient);
    }

    private String generateSasUrl(BlobClient blobClient) {
        BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
        OffsetDateTime expiryTime = OffsetDateTime.now().plusHours(1); // 1 hour valid token

        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiryTime, permission);
        String sasToken = blobClient.generateSas(values);

        return blobClient.getBlobUrl() + "?" + sasToken;
    }
}
