package com.cloud.drive.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.cloud.drive.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.OffsetDateTime;

@Service
@ConditionalOnProperty(name = "azure.storage.enabled", havingValue = "true")
public class AzureBlobStorageService implements StorageService {

    private final BlobServiceClient client;
    private final String container;

    public AzureBlobStorageService(java.util.Optional<BlobServiceClient> client,
            @Value("${azure.storage.container-name}") String container) {
        this.client = client.orElse(null);
        this.container = container;
    }

    @Override
    public String createUploadTarget(String blobKey, String contentType, long maxBytes, Duration ttl) {
        requireAzure();
        BlobClient blob = containerClient().getBlobClient(blobKey);
        BlobSasPermission perm = new BlobSasPermission()
                .setCreatePermission(true)
                .setWritePermission(true);
        BlobServiceSasSignatureValues sas = new BlobServiceSasSignatureValues(
                OffsetDateTime.now().plus(ttl), perm);
        return blob.getBlobUrl() + "?" + blob.generateSas(sas);
    }

    @Override
    public String createReadUrl(String blobKey, boolean inline, Duration ttl) {
        requireAzure();
        BlobClient blob = containerClient().getBlobClient(blobKey);
        BlobSasPermission perm = new BlobSasPermission().setReadPermission(true);
        BlobServiceSasSignatureValues sas = new BlobServiceSasSignatureValues(
                OffsetDateTime.now().plus(ttl), perm)
                .setContentDisposition(inline ? "inline" : "attachment");
        return blob.getBlobUrl() + "?" + blob.generateSas(sas);
    }

    @Override
    public void streamTo(String blobKey, long[] byteRange, OutputStream out) {
        requireAzure();
        BlobClient blob = containerClient().getBlobClient(blobKey);
        try (InputStream in = blobRange(blob, byteRange)) {
            in.transferTo(out);
        } catch (Exception e) {
            throw new ApiException("Failed to read file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public long assertLength(String blobKey, long expected) {
        requireAzure();
        BlobClient blob = containerClient().getBlobClient(blobKey);
        long actual = blob.getProperties().getBlobSize();
        if (actual != expected) {
            blob.deleteIfExists();
            throw new ApiException("Upload size mismatch — the uploaded file does not match the declared size",
                    HttpStatus.BAD_REQUEST);
        }
        return actual;
    }

    @Override
    public void delete(String blobKey) {
        requireAzure();
        containerClient().getBlobClient(blobKey).deleteIfExists();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void requireAzure() {
        if (client == null) {
            throw new ApiException(
                    "Azure Storage is not configured. Set the AZURE_STORAGE_CONNECTION_STRING environment variable.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private InputStream blobRange(BlobClient blob, long[] range) {
        if (range == null)
            return blob.openInputStream();
        return blob.openInputStream(new BlobRange(range[0], range[1] - range[0] + 1), null);
    }

    private BlobContainerClient containerClient() {
        BlobContainerClient c = client.getBlobContainerClient(container);
        c.createIfNotExists();
        return c;
    }
}
