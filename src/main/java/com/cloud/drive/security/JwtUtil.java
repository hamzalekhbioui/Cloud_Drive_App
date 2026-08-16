package com.cloud.drive.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    /** Minimum secret length to satisfy HMAC-SHA256 key requirements. */
    private static final int MIN_SECRET_LENGTH = 32;

    /** Known insecure default — reject on startup. */
    private static final String KNOWN_DEFAULT_PREFIX = "CloudDriveLocalSecret";

    private final String secret;
    private final String secret;

    private final long expiration;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        validateSecret(secret);
        this.secret = secret;
        this.expiration = expiration;
    }

    /**
     * Fail-fast: abort startup if the JWT secret is missing, too short, or the
     * publicly-known default. This prevents silent auth bypass in production.
     */
    private static void validateSecret(String s) {
        if (s == null || s.isBlank()) {
            throw new IllegalStateException(
                    "FATAL: jwt.secret is not set. "
                  + "Set the JWT_SECRET environment variable (>= " + MIN_SECRET_LENGTH + " chars). Aborting startup.");
        }
        if (s.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "FATAL: jwt.secret is too short (" + s.length() + " chars). "
                  + "Must be at least " + MIN_SECRET_LENGTH + " characters. Aborting startup.");
        }
        if (s.startsWith(KNOWN_DEFAULT_PREFIX)) {
            throw new IllegalStateException(
                    "FATAL: jwt.secret is still the hardcoded default. "
                  + "Set a unique JWT_SECRET environment variable. Aborting startup.");
        }
        log.info("JWT secret validated (length={})", s.length());
    }
        this.secret = secret;
        this.expiration = expiration;
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(signingKey())
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}