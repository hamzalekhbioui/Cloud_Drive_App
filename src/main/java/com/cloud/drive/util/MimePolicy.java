package com.cloud.drive.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Centralised Content-Disposition and inline/attachment policy.
 * Types that browsers can render natively are served {@code inline};
 * everything else triggers a download prompt via {@code attachment}.
 */
public final class MimePolicy {

    private MimePolicy() {}

    /** MIME types the browser can safely render in-tab. */
    private static final Set<String> INLINE_TYPES = Set.of(
            "application/pdf",
            "image/png", "image/jpeg", "image/gif", "image/webp", "image/svg+xml",
            "video/mp4", "video/webm",
            "audio/mpeg", "audio/ogg", "audio/wav",
            "text/plain", "text/html", "text/csv"
    );

    /**
     * @return {@code true} if this content type should be served {@code inline}
     */
    public static boolean shouldInline(String contentType) {
        return contentType != null && INLINE_TYPES.contains(contentType.toLowerCase());
    }

    /**
     * RFC 5987 / RFC 6266 compliant filename encoding for Content-Disposition.
     * Produces a value safe for use in {@code filename*=UTF-8''...} when the
     * name contains non-ASCII characters, and a plain ASCII fallback otherwise.
     */
    public static String encodeFilename(String rawName) {
        if (rawName == null || rawName.isBlank()) return "download";
        return URLEncoder.encode(rawName, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
