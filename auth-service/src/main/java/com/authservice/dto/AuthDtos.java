package com.authservice.dto;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonInclude;

public class AuthDtos {

    // =============================================
    // RegisterRequest
    // =============================================

    public static class RegisterRequest {

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[a-zA-Z0-9_]+$")
        private String username;

        @NotBlank
        @Email
        private String email;

        // UPDATED
        @NotBlank
        private String phoneNumber;

        @NotBlank
        @Size(min = 8)
        private String password;

        public RegisterRequest() {
        }

        public RegisterRequest(
                String username,
                String email,
                String phoneNumber,
                String password) {

            this.username = username;
            this.email = email;
            this.phoneNumber = phoneNumber;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }


    // =============================================
    // LoginRequest
    // =============================================
// =============================================
// Forgot Password Request
// =============================================
public static class ForgotPasswordRequest {

    @NotBlank
    @Email
    private String email;

    public ForgotPasswordRequest() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

// =============================================
// Reset Password Request
// =============================================
public static class ResetPasswordRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String otp;

    @NotBlank
    @Size(min = 8)
    private String newPassword;

    public ResetPasswordRequest() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
    public static class LoginRequest {

        @NotBlank
        @Email
        private String email;

        @NotBlank
        private String password;

        public LoginRequest() {
        }

        public LoginRequest(
                String email,
                String password) {

            this.email = email;
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }


    // =============================================
    // OTP
    // =============================================

    public static class OtpVerifyRequest {

        @NotBlank
        @Email
        private String email;

        @NotBlank
        private String otp;

        public OtpVerifyRequest() {
        }

        public OtpVerifyRequest(
                String email,
                String otp) {

            this.email = email;
            this.otp = otp;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getOtp() {
            return otp;
        }

        public void setOtp(String otp) {
            this.otp = otp;
        }
    }


    // =============================================
    // Profile Update
    // =============================================

    public static class ProfileUpdateRequest {

        private String username;
        private String newPassword;

        public ProfileUpdateRequest() {
        }

        public ProfileUpdateRequest(
                String username,
                String newPassword) {

            this.username = username;
            this.newPassword = newPassword;
        }

        public String getUsername() {
            return username;
        }

        public String getNewPassword() {
            return newPassword;
        }
    }


    // =============================================
    // API Response
    // =============================================

    public static class ApiResponse {

        private boolean success;
        private String message;
        private Object data;

        public ApiResponse() {
        }

        public ApiResponse(
                boolean success,
                String message,
                Object data) {

            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Object getData() {
            return data;
        }
    }


    // =============================================
    // User Response
    // =============================================

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserResponse {

        private String id;
        private String username;
        private String email;
        private String phoneNumber;
        private String profilePicture;
        private boolean active;
        private String createdAt;

        public UserResponse() {
        }

        public UserResponse(
                String id,
                String username,
                String email,
                String phoneNumber,
                String profilePicture,
                boolean active,
                String createdAt) {

            this.id = id;
            this.username = username;
            this.email = email;
            this.phoneNumber = phoneNumber;
            this.profilePicture = profilePicture;
            this.active = active;
            this.createdAt = createdAt;
        }

        public String getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public String getProfilePicture() {
            return profilePicture;
        }

        public boolean isActive() {
            return active;
        }

        public String getCreatedAt() {
            return createdAt;
        }
    }


    // =============================================
    // Token Response
    // =============================================

    public static class AuthTokenResponse {

        private String token;
        private String tokenType;
        private long expiresIn;
        private UserResponse user;

        public AuthTokenResponse(
                String token,
                String tokenType,
                long expiresIn,
                UserResponse user) {

            this.token = token;
            this.tokenType = tokenType;
            this.expiresIn = expiresIn;
            this.user = user;
        }

        public String getToken() {
            return token;
        }

        public String getTokenType() {
            return tokenType;
        }

        public long getExpiresIn() {
            return expiresIn;
        }

        public UserResponse getUser() {
            return user;
        }
    }


    // =============================================
    // Error Response
    // =============================================

    public static class ErrorResponse {

        private int status;
        private String error;
        private String message;
        private String path;
        private String timestamp;

        public ErrorResponse() {
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public void setError(String error) {
            this.error = error;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }
}