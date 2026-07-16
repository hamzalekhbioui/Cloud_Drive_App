package com.cloud.drive.security;

import com.cloud.drive.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Centralised filename validation, extension allow/deny-list, and sanitisation.
 *
 * <p><b>Security</b>: blocks dangerous extensions (html, svg, js, exe, etc.) that could
 * lead to stored XSS or code execution; strips path traversal and control characters;
 * only allows a curated set of known-safe extensions through.</p>
 */
public final class FilenamePolicy {

    private FilenamePolicy() {}

    /** Extensions explicitly denied — executable or script-capable formats. */
    private static final Set<String> DENY_EXT = Set.of(
            "html", "htm", "svg", "js", "mjs", "jar", "class",
            "exe", "bat", "cmd", "sh", "php", "jsp", "asp", "aspx");

    /** Allow-listed extensions mapped to their canonical MIME types. */
    private static final Map<String, String> EXT_TO_MIME = Map.ofEntries(
            Map.entry("png",  "image/png"),
            Map.entry("jpg",  "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif",  "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("pdf",  "application/pdf"),
            Map.entry("txt",  "text/plain"),
            Map.entry("csv",  "text/csv"),
            Map.entry("json", "application/json"),
            Map.entry("zip",  "application/zip"),
            Map.entry("doc",  "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls",  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt",  "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("mp4",  "video/mp4"),
            Map.entry("webm", "video/webm"),
            Map.entry("mp3",  "audio/mpeg"),
            Map.entry("ogg",  "audio/ogg"),
            Map.entry("wav",  "audio/wav"));

    /**
     * Extract and validate the file extension.
     *
     * @param name the original filename
     * @return the extension including the leading dot, e.g. {@code ".png"}
     * @throws ApiException 400 if the extension is missing, denied, or not in the allow-list
     */
    public static String extension(String name) {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            throw new ApiException("Missing file extension", HttpStatus.BAD_REQUEST);
        }
        String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (DENY_EXT.contains(ext) || !EXT_TO_MIME.containsKey(ext)) {
            throw new ApiException("File extension not allowed: ." + ext, HttpStatus.BAD_REQUEST);
        }
        return "." + ext;
    }

    /**
     * Look up the canonical MIME type for a validated extension.
     *
     * @param ext the extension including the leading dot, e.g. {@code ".png"}
     * @return the MIME type, e.g. {@code "image/png"}
     */
    public static String contentTypeFor(String ext) {
        return EXT_TO_MIME.get(ext.substring(1));
    }

    /**
     * Sanitise a user-supplied filename: strip path traversal, control characters,
     * NUL bytes, and collapse double-dots. Ensures the result is a single safe basename.
     *
     * @param name the raw filename from the client
     * @return the sanitised filename
     * @throws ApiException 400 if the filename is empty/blank or exceeds 255 chars after sanitisation
     */
    public static String sanitize(String name) {
        if (name == null || name.isBlank()) {
            throw new ApiException("Empty filename", HttpStatus.BAD_REQUEST);
        }
        // normalise path separators and strip any directory prefix
        String base = name.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        // strip control characters (U+0000–U+001F, U+007F)
        base = base.replaceAll("[\u0000-\u001f\u007f]", "")
                   // collapse consecutive dots ("..") to prevent traversal
                   .replaceAll("\\.{2,}", ".")
                   .trim();
        if (base.isBlank() || base.length() > 255) {
            throw new ApiException("Invalid filename", HttpStatus.BAD_REQUEST);
        }
        return base;
    }

    /**
     * RFC 5987 encoding for {@code Content-Disposition filename*} parameter.
     * Defeats header/CRLF injection by percent-encoding the filename.
     *
     * @param name the sanitised filename
     * @return the encoded value ready for use in {@code filename*=utf-8''...}
     */
    public static String encodeFilename(String name) {
        if (name == null || name.isBlank()) return "download";
        return URLEncoder.encode(name, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
