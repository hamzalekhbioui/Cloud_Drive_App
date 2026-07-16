package com.cloud.drive.util;

import com.cloud.drive.exception.ApiException;
import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Centralised MIME validation, Content-Disposition, and inline/attachment policy.
 *
 * <p><b>Security</b>: uses Apache Tika magic-byte detection so we never trust the
 * client-supplied {@code Content-Type}. Only allow-listed MIME types may be stored;
 * only a safe subset of those may be served {@code inline} (never html/svg/xml/js).</p>
 */
public final class MimePolicy {

    private MimePolicy() {}

    private static final Tika TIKA = new Tika();

    /** MIME types we accept for storage. Everything else is rejected at upload time. */
    private static final Set<String> ALLOWED = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp",
            "application/pdf",
            "text/plain",
            "text/csv",
            "application/zip",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "video/mp4", "video/webm",
            "audio/mpeg", "audio/ogg", "audio/wav",
            "application/json",
            "application/octet-stream"
    );

    /**
     * Types safe to render inline in the browser.
     * Intentionally excludes text/html, image/svg+xml, text/xml, application/javascript —
     * any type whose inline rendering could trigger script execution (XSS).
     */
    private static final Set<String> INLINE_SAFE = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp",
            "video/mp4", "video/webm",
            "audio/mpeg", "audio/ogg", "audio/wav",
            "application/pdf"
    );

    /**
     * Detect the real MIME type from the file's magic bytes via Apache Tika,
     * and validate it against the allow-list.
     *
     * @param bytes        the file input stream (only the first few KB are read for detection)
     * @param declaredName the original filename (used as a hint for Tika when magic bytes are ambiguous)
     * @return the detected MIME type
     * @throws ApiException with 415 Unsupported Media Type if the type is not allowed
     * @throws IOException  if reading from the stream fails
     */
    public static String detect(InputStream bytes, String declaredName) throws IOException {
        String detected = TIKA.detect(bytes, declaredName);
        if (!ALLOWED.contains(detected)) {
            throw new ApiException(
                    "File type not permitted: " + detected,
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
        return detected;
    }

    /**
     * Validate an extension-derived MIME type against the allow-list.
     * Used for the two-phase upload where no file bytes are available at begin-time.
     *
     * @param mimeType the MIME type derived from the file extension
     * @throws ApiException with 415 if the type is not allowed
     */
    public static void validateType(String mimeType) {
        if (!ALLOWED.contains(mimeType)) {
            throw new ApiException(
                    "File type not permitted: " + mimeType,
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
    }

    /**
     * @return {@code true} if this content type should be served {@code inline}
     *         (safe — cannot trigger script execution)
     */
    public static boolean shouldInline(String contentType) {
        return contentType != null && INLINE_SAFE.contains(contentType.toLowerCase());
    }
}
