package com.authservice.service;

public interface EmailService {

    /**
     * Send account creation welcome email.
     */
    void sendWelcomeEmail(String toEmail, String username);

    /**
     * Send a 6-digit OTP to the user for login verification.
     */
    void sendOtpEmail(String toEmail, String username, String otp);
}
