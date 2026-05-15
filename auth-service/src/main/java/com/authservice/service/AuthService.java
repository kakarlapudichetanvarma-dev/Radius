package com.authservice.service;

import com.authservice.dto.AuthDtos.*;

import java.util.UUID;

public interface AuthService {

    /**
     * Register a new user. Hashes the password, persists the user,
     * and sends a welcome email asynchronously.
     */
    UserResponse register(RegisterRequest request);

    /**
     * Validate email+password, generate an OTP, store in Redis,
     * and send via email. Returns a message (does NOT return a JWT yet).
     */
    ApiResponse login(LoginRequest request);

    /**
     * Verify the OTP for the given email, then generate and return a JWT token.
     */
    AuthTokenResponse verifyOtp(OtpVerifyRequest request);

    /**
     * Update the authenticated user's username and/or profile picture.
     * Optionally update the password.
     */
    UserResponse updateProfile(
            UUID userId,
            ProfileUpdateRequest request,
            byte[] profilePicture,
            String pictureFilename
    );
    UserResponse getUserById(
            UUID userId
    );
    /**
     * Retrieve user profile by ID.
     */
    UserResponse getProfile(UUID userId);

    // ADDED
    UserResponse getUserByPhone(String phoneNumber);

    // ADDED
    UserResponse getUserByUsername(String username);
}