package com.authservice.service;

import com.authservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtUtil Unit Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String TEST_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRY_MS = 86400000L;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", EXPIRY_MS);
    }

    @Test
    @DisplayName("generateToken: produces a non-blank token")
    void generateToken_producesToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateToken(userId, "user@example.com", "user");
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("validateToken: returns true for a fresh token")
    void validateToken_freshToken_returnsTrue() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateToken(userId, "user@example.com", "user");
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken: returns false for a tampered token")
    void validateToken_tamperedToken_returnsFalse() {
        assertThat(jwtUtil.validateToken("tampered.jwt.token")).isFalse();
    }

    @Test
    @DisplayName("extractUserId: returns correct userId from token")
    void extractUserId_returnsCorrectId() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateToken(userId, "user@example.com", "user");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(userId.toString());
    }

    @Test
    @DisplayName("extractEmail: returns correct email from token")
    void extractEmail_returnsCorrectEmail() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateToken(userId, "user@example.com", "user");
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("getExpirationMs: returns configured value")
    void getExpirationMs_returnsConfiguredValue() {
        assertThat(jwtUtil.getExpirationMs()).isEqualTo(EXPIRY_MS);
    }
}
