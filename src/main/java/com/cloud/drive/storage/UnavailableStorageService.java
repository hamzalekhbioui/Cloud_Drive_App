package com.cloud.drive.storage;

import com.cloud.drive.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.io.OutputStream;
import java.time.Duration;

public class UnavailableStorageService implements StorageService {
    private ApiException unavailable() {
        return new ApiException("Azure Storage is not configured.", HttpStatus.SERVICE_UNAVAILABLE);
    }
    public String createUploadTarget(String blobKey, String contentType, long maxBytes, Duration ttl) { throw unavailable(); }
    public String createReadUrl(String blobKey, boolean inline, Duration ttl) { throw unavailable(); }
    public void streamTo(String blobKey, long[] byteRange, OutputStream out) { throw unavailable(); }
    public void delete(String blobKey) { throw unavailable(); }
    public long assertLength(String blobKey, long expected) { throw unavailable(); }
}
