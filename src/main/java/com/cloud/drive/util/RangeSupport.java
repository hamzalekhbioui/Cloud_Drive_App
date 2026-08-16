package com.cloud.drive.util;

import com.cloud.drive.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * RFC 7233 byte-range request parser.
 * Supports three forms:
 *   bytes=0-499       (first 500 bytes)
 *   bytes=500-        (from byte 500 to end)
 *   bytes=-500        (last 500 bytes)
 *
 * Returns {@code null} when the header is absent or unparseable (non-range request).
 * Throws 416 when the range is syntactically valid but semantically unsatisfiable.
 */
public final class RangeSupport {

    private RangeSupport() {}

    /**
     * Parse an HTTP {@code Range} header value.
     *
     * @param header the raw header value (may be {@code null})
     * @param size   the total size of the resource in bytes
     * @return a two-element array {@code [firstByte, lastByte]} (inclusive),
     *         or {@code null} if the header is absent / not a byte-range
     */
    public static long[] parse(String header, long size) {
        if (header == null || !header.startsWith("bytes=")) return null;
        String spec = header.substring(6).trim();
        try {
            long first, last;
            if (spec.startsWith("-")) {
                // suffix range "bytes=-500" → last 500 bytes
                long len = Long.parseLong(spec.substring(1));
                last = size - 1;
                first = Math.max(0, size - len);
            } else {
                String[] p = spec.split("-", 2);
                first = Long.parseLong(p[0]);
                last = p.length < 2 || p[1].isBlank() ? size - 1 : Long.parseLong(p[1]);
            }
            if (first < 0 || last >= size || first > last) {
                throw new ApiException(
                        "Range Not Satisfiable",
                        HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
            }
            return new long[]{ first, last };
        } catch (NumberFormatException e) {
            // malformed range — treat as a normal (non-range) request
            return null;
        }
    }
}
