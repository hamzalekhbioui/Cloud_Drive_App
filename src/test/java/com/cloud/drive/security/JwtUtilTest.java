package com.cloud.drive.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-that-is-at-least-32-characters-long");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3_600_000L); // 1h
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
        JwtUtil other = new JwtUtil();
        ReflectionTestUtils.setField(other, "secret", "different-secret-key-also-32-characters-long");
        ReflectionTestUtils.setField(other, "expiration", 3_600_000L);

        String foreignToken = other.generateToken("alice@example.com");

        assertThat(jwtUtil.isValid(foreignToken)).isFalse();
    }
}
