package com.cloud.drive.util;

import java.security.SecureRandom;
import java.util.Base64;

public class TokenGenerator {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    /**
     * Generates a cryptographically secure, URL-safe token with at least 256 bits of randomness.
     */
    public static String generateToken() {
        byte[] randomBytes = new byte[32]; // 32 bytes = 256 bits
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }
}
