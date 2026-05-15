package com.authservice.service;

public interface OtpService {

    /**
     * Generate a 6-digit OTP, store it in Redis (with 5-min TTL), and return it.
     */
    String generateAndStoreOtp(String email);

    /**
     * Validate submitted OTP against Redis-stored value.
     * Throws OtpExpiredException if key missing, InvalidOtpException if mismatch.
     * Deletes OTP from Redis on success (single-use).
     */
    void validateOtp(String email, String submittedOtp);
}
