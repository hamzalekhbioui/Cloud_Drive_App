package com.cloud.drive.security.admin;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminJwtUtilTest {

    private static final String SECRET = "test-admin-secret-that-is-long-enough-for-jwt-signing-123456";
    private AdminJwtUtil adminJwtUtil;

    @BeforeEach
    void setUp() {
        adminJwtUtil = new AdminJwtUtil(SECRET, 3_600_000L);
    }

    @Test
    void generatedTokenContainsAdminTypeAndRoundTrips() {
        String token = adminJwtUtil.generateToken("admin@example.com");

        assertThat(adminJwtUtil.isValid(token)).isTrue();
        assertThat(adminJwtUtil.extractEmail(token)).isEqualTo("admin@example.com");
    }

    @Test
    void tenantStyleTokenIsNotValidAsAdminToken() {
        String token = Jwts.builder()
                .subject("tenant@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .compact();

        assertThat(adminJwtUtil.isValid(token)).isFalse();
    }

    @Test
    void rejectsShortSecrets() {
        assertThatThrownBy(() -> new AdminJwtUtil("too-short", 3_600_000L))
                .isInstanceOf(IllegalStateException.class);
    }
}
