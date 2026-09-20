package com.cloud.drive.security.admin;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Token utility for the admin panel. Deliberately isolated from {@code JwtUtil}:
 * its own secret, a much shorter expiry and a {@code typ=admin} claim so a tenant
 * token can never authenticate on the admin chain (and vice versa).
 */
@Component
public class AdminJwtUtil {

    private static final Logger log = LoggerFactory.getLogger(AdminJwtUtil.class);

    /** Minimum secret length to satisfy HMAC-SHA256 key requirements. */
    private static final int MIN_SECRET_LENGTH = 32;

    /** Known insecure default — reject on startup. */
    private static final String KNOWN_DEFAULT_PREFIX = "CloudDriveLocalSecret";

    /** Claim marking the token as an admin token. */
    public static final String TYPE_CLAIM = "typ";
    public static final String TYPE_ADMIN = "admin";

    private final String secret;
    private final long expiration;

    public AdminJwtUtil(@Value("${admin.jwt.secret}") String secret,
                        @Value("${admin.jwt.expiration}") long expiration) {
        validateSecret(secret);
        this.secret = secret;
        this.expiration = expiration;
    }

    /**
     * Fail-fast: abort startup if the admin JWT secret is missing, too short, or the
     * publicly-known default. This prevents silent auth bypass in production.
     */
    private static void validateSecret(String s) {
        if (s == null || s.isBlank()) {
            throw new IllegalStateException(
                    "FATAL: admin.jwt.secret is not set. "
                  + "Set the ADMIN_JWT_SECRET environment variable (>= " + MIN_SECRET_LENGTH + " chars). Aborting startup.");
        }
        if (s.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "FATAL: admin.jwt.secret is too short (" + s.length() + " chars). "
                  + "Must be at least " + MIN_SECRET_LENGTH + " characters. Aborting startup.");
        }
        if (s.startsWith(KNOWN_DEFAULT_PREFIX)) {
            throw new IllegalStateException(
                    "FATAL: admin.jwt.secret is still the hardcoded default. "
                  + "Set a unique ADMIN_JWT_SECRET environment variable. Aborting startup.");
        }
        log.info("Admin JWT secret validated (length={})", s.length());
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .claim(TYPE_CLAIM, TYPE_ADMIN)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(signingKey())
                .compact();
    }

    public String extractEmail(String token) {
        return parse(token).getSubject();
    }

    /** Valid only when the signature verifies, the token is unexpired and it carries {@code typ=admin}. */
    public boolean isValid(String token) {
        try {
            return TYPE_ADMIN.equals(parse(token).get(TYPE_CLAIM, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpiration() {
        return expiration;
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
