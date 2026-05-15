package com.authservice.service;

import com.authservice.exception.AuthExceptions.*;
import com.authservice.service.impl.OtpServiceImpl;
import com.authservice.util.OtpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtpService Unit Tests")
class OtpServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private OtpUtil otpUtil;

    @InjectMocks
    private OtpServiceImpl otpService;

    private static final String EMAIL = "test@example.com";
    private static final String OTP_KEY = "otp:test@example.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "otpExpirationSeconds", 300L);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(otpUtil.buildOtpKey(EMAIL)).thenReturn(OTP_KEY);
    }

    @Test
    @DisplayName("generateAndStoreOtp: generates OTP and stores in Redis with TTL")
    void generateAndStoreOtp_storesInRedis() {
        when(otpUtil.generateOtp()).thenReturn("654321");

        String otp = otpService.generateAndStoreOtp(EMAIL);

        assertThat(otp).isEqualTo("654321");
        verify(valueOps).set(eq(OTP_KEY), eq("654321"), eq(300L), any());
    }

    @Test
    @DisplayName("validateOtp: success — deletes OTP after successful validation")
    void validateOtp_success_deletesKey() {
        when(valueOps.get(OTP_KEY)).thenReturn("654321");

        assertThatNoException().isThrownBy(() -> otpService.validateOtp(EMAIL, "654321"));

        verify(redisTemplate).delete(OTP_KEY);
    }

    @Test
    @DisplayName("validateOtp: throws OtpExpiredException when key not in Redis")
    void validateOtp_expired_throws() {
        when(valueOps.get(OTP_KEY)).thenReturn(null);

        assertThatThrownBy(() -> otpService.validateOtp(EMAIL, "654321"))
                .isInstanceOf(OtpExpiredException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("validateOtp: throws InvalidOtpException when OTP does not match")
    void validateOtp_mismatch_throws() {
        when(valueOps.get(OTP_KEY)).thenReturn("654321");

        assertThatThrownBy(() -> otpService.validateOtp(EMAIL, "000000"))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("incorrect");

        verify(redisTemplate, never()).delete(OTP_KEY);
    }
}
