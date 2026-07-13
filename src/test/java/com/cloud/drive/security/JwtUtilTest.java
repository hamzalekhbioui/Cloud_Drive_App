package com.cloud.drive.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
                "test-secret-key-that-is-at-least-32-characters-long",
                3_600_000L
        );
    }

    @Test
    void generateToken_thenExtractEmail_roundtrips() {
        String token = jwtUtil.generateToken("alice@example.com");

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("alice@example.com");
        assertThat(jwtUtil.isValid(token)).isTrue();
    }

    @Test
    void isValid_returnsFalse_forTamperedToken() {
        String token = jwtUtil.generateToken("alice@example.com");
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThat(jwtUtil.isValid(tampered)).isFalse();
    }

    @Test
    void isValid_returnsFalse_forGarbage() {
        assertThat(jwtUtil.isValid("not-a-jwt")).isFalse();
    }

    @Test
    void isValid_returnsFalse_whenSignedByDifferentSecret() {
        JwtUtil other = new JwtUtil(
                "different-secret-key-also-32-characters-long",
                3_600_000L
        );

        String foreignToken = other.generateToken("alice@example.com");

        assertThat(jwtUtil.isValid(foreignToken)).isFalse();
    }
}
