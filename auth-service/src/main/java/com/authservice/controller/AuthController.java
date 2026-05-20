package com.authservice.controller;

import com.authservice.dto.AuthDtos.*;
import com.authservice.exception.AuthExceptions.UserNotFoundException;
import com.authservice.service.AuthService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.authservice.entity.User;
import com.authservice.repository.UserRepository;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
private final UserRepository userRepository;
    private static final Logger log =
            LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(
        AuthService authService,
        UserRepository userRepository
) {
    this.authService = authService;
    this.userRepository = userRepository;
}


    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info(
                "Received registration request for email: {}",
                request.getEmail()
        );

        UserResponse user =
                authService.register(request);

        ApiResponse response =
                new ApiResponse(
                        true,
                        "Registration successful. Welcome aboard!",
                        user
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(
            @Valid @RequestBody LoginRequest request) {

        log.info(
                "Received login request for email: {}",
                request.getEmail()
        );

        ApiResponse response =
                authService.login(request);

        return ResponseEntity.ok(response);
    }

@PostMapping("/forgot-password")
public ResponseEntity<ApiResponse> forgotPassword(
        @Valid @RequestBody ForgotPasswordRequest request) {
    log.info("Forgot password request for email: {}", request.getEmail());
    ApiResponse response = authService.forgotPassword(request);
    return ResponseEntity.ok(response);
}

@PostMapping("/reset-password")
public ResponseEntity<ApiResponse> resetPassword(
        @Valid @RequestBody ResetPasswordRequest request) {
    log.info("Reset password request for email: {}", request.getEmail());
    ApiResponse response = authService.resetPassword(request);
    return ResponseEntity.ok(response);
}
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request) {

        log.info(
                "Received OTP verification request for email: {}",
                request.getEmail()
        );

        AuthTokenResponse tokenResponse =
                authService.verifyOtp(request);

        ApiResponse response =
                new ApiResponse(
                        true,
                        "Login successful.",
                        tokenResponse
                );

        return ResponseEntity.ok(response);
    }


    @GetMapping("/profile")
    public ResponseEntity<ApiResponse> getProfile(
            Authentication authentication) {

        UUID userId =
                UUID.fromString(
                        (String) authentication.getPrincipal()
                );

        log.info(
                "Profile fetch request for userId: {}",
                userId
        );

        UserResponse user =
                authService.getProfile(userId);

        ApiResponse response =
                new ApiResponse(
                        true,
                        "Profile fetched successfully.",
                        user
                );

        return ResponseEntity.ok(response);
}
    
@GetMapping("/users/{userId}/profile-picture")
public ResponseEntity<byte[]> getProfilePicture(
        @PathVariable UUID userId) {

    User user = userRepository.findById(userId)
            .orElseThrow(() ->
                    new UserNotFoundException("User not found."));

    if (user.getProfilePicture() == null) {
        return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok()
            .header("Content-Type", "image/jpeg")
            .header("Cache-Control", "max-age=86400")
            .body(user.getProfilePicture());
}
    
    @GetMapping("/users/phone/{phoneNumber}")
    public ResponseEntity<ApiResponse> getByPhone(
            @PathVariable String phoneNumber) {

        UserResponse user =
                authService.getUserByPhone(
                        phoneNumber
                );

        ApiResponse response =
                new ApiResponse(
                        true,
                        "User fetched successfully.",
                        user
                );

        return ResponseEntity.ok(
                response
        );
    }


    @GetMapping("/users/username/{username}")
    public ResponseEntity<ApiResponse> getByUsername(
            @PathVariable String username) {

        UserResponse user =
                authService.getUserByUsername(
                        username
                );

        ApiResponse response =
                new ApiResponse(
                        true,
                        "User fetched successfully.",
                        user
                );

        return ResponseEntity.ok(
                response
        );
    }
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse> getById(
            @PathVariable UUID userId) {

        UserResponse user =
                authService.getUserById(
                        userId
                );

        ApiResponse response =
                new ApiResponse(
                        true,
                        "User fetched successfully.",
                        user
                );

        return ResponseEntity.ok(
                response
        );
    }

    @PutMapping(
            value = "/profile",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse> updateProfile(

            @RequestPart(
                    value = "username",
                    required = false
            )
            String username,

            @RequestPart(
                    value = "newPassword",
                    required = false
            )
            String newPassword,

            @RequestPart(
                    value = "profilePicture",
                    required = false
            )
            MultipartFile profilePicture,

            Authentication authentication
    ) throws IOException {

        UUID userId =
                UUID.fromString(
                        (String) authentication.getPrincipal()
                );

        log.info(
                "Profile update request for userId: {}",
                userId
        );

        ProfileUpdateRequest updateRequest =
                new ProfileUpdateRequest(
                        username,
                        newPassword
                );

        byte[] pictureBytes =
                profilePicture != null
                        ? profilePicture.getBytes()
                        : null;

        String pictureFilename =
                profilePicture != null
                        ? profilePicture.getOriginalFilename()
                        : null;

        UserResponse updated =
                authService.updateProfile(
                        userId,
                        updateRequest,
                        pictureBytes,
                        pictureFilename
                );

        ApiResponse response =
                new ApiResponse(
                        true,
                        "Profile updated successfully.",
                        updated
                );

        return ResponseEntity.ok(response);
    }
}