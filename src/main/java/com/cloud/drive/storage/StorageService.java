package com.cloud.drive.storage;

import java.io.OutputStream;
import java.time.Duration;

/**
 * Storage-agnostic port (SOLID: services depend on abstraction, not a specific provider).
 * Implementations are expected to handle SAS token generation, streaming, and deletion
 * against the underlying object store (Azure Blob Storage, S3, etc.).
 */
public interface StorageService {

    /** Mint a short-lived write SAS URL the client can PUT to directly. */
    String createUploadTarget(String blobKey, String contentType, long maxBytes, Duration ttl);

    /** Mint a short-lived read SAS URL. */
    String createReadUrl(String blobKey, boolean inline, Duration ttl);

    /** Stream a stored object to the given output. Supports byte-range via the impl. */
    void streamTo(String blobKey, long[] byteRange, OutputStream out);

    /** Delete a stored object. */
    void delete(String blobKey);

    /**
     * Verify the object exists and its real length matches the declared metadata.
     * Anti-forgery check for the commit handshake.
     *
     * @return the actual blob size
     * @throws com.cloud.drive.exception.ApiException if the size mismatch or blob is missing
     */
    long assertLength(String blobKey, long expected);
}
