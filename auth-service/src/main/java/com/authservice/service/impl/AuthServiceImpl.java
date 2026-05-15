package com.authservice.service.impl;

import com.authservice.dto.AuthDtos.*;
import com.authservice.entity.User;
import com.authservice.exception.AuthExceptions.*;
import com.authservice.repository.UserRepository;
import com.authservice.service.AuthService;
import com.authservice.service.EmailService;
import com.authservice.service.OtpService;
import com.authservice.util.JwtUtil;
import com.authservice.util.PhoneValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log =
            LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OtpService otpService;
    private final JwtUtil jwtUtil;

    // ADDED
    private final PhoneValidator phoneValidator;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            OtpService otpService,
            JwtUtil jwtUtil,
            PhoneValidator phoneValidator) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.otpService = otpService;
        this.jwtUtil = jwtUtil;

        // ADDED
        this.phoneValidator = phoneValidator;
    }


    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {

        log.info(
                "Registration attempt for email: {}",
                request.getEmail()
        );

        validateUniqueFields(request);

        // ADDED
        if (!phoneValidator.isValid(
                request.getPhoneNumber()
        )) {

            throw new InvalidPhoneNumberException(
                    "Invalid phone number."
            );
        }

        String passwordHash =
                passwordEncoder.encode(
                        request.getPassword()
                );

        User user = new User();

        user.setUsername(
                request.getUsername()
        );

        user.setEmail(
                request.getEmail()
                        .toLowerCase()
        );

        // UPDATED
        user.setPhoneNumber(
                phoneValidator.toE164(
                        request.getPhoneNumber()
                )
        );

        user.setPasswordHash(
                passwordHash
        );

        user.setActive(
                true
        );

        user = userRepository.save(user);

        try {

            emailService.sendWelcomeEmail(
                    user.getEmail(),
                    user.getUsername()
            );

        } catch (Exception e) {

            log.warn(
                    "Email failed: {}",
                    e.getMessage()
            );
        }

        return toUserResponse(user);
    }
    
    @Override
    public UserResponse getUserById(
            UUID userId) {

        User user =
                userRepository
                        .findById(
                                userId
                        )
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "User not found."
                                ));

        return toUserResponse(
                user
        );
    }

    @Override
    public UserResponse getUserByPhone(
            String phoneNumber) {

        User user =
                userRepository.findByPhoneNumber(
                        phoneValidator.toE164(
                                phoneNumber
                        )
                )
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found."
                        ));

        return toUserResponse(user);
    }


    @Override
    public UserResponse getUserByUsername(
            String username) {

        User user =
                userRepository.findByUsername(
                        username
                )
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found."
                        ));

        return toUserResponse(user);
    }


    @Override
    public ApiResponse login(
            LoginRequest request) {

        User user =
                userRepository.findByEmail(
                        request.getEmail()
                                .toLowerCase()
                )
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Invalid email or password."
                        ));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()
        )) {

            throw new InvalidCredentialsException(
                    "Invalid email or password."
            );
        }

        String otp =
                otpService.generateAndStoreOtp(
                        user.getEmail()
                );

        emailService.sendOtpEmail(
                user.getEmail(),
                user.getUsername(),
                otp
        );

        return new ApiResponse(
                true,
                "OTP sent.",
                null
        );
    }


    @Override
    public AuthTokenResponse verifyOtp(
            OtpVerifyRequest request) {

        User user =
                userRepository.findByEmail(
                        request.getEmail()
                                .toLowerCase()
                )
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found."
                        ));

        otpService.validateOtp(
                user.getEmail(),
                request.getOtp()
        );

        String token =
                jwtUtil.generateToken(
                        user.getId(),
                        user.getEmail(),
                        user.getUsername()
                );

        return new AuthTokenResponse(
                token,
                "Bearer",
                jwtUtil.getExpirationMs() / 1000,
                toUserResponse(user)
        );
    }


    @Override
    public UserResponse getProfile(
            UUID userId) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "User not found."
                                ));

        return toUserResponse(user);
    }


    @Override
    @Transactional
    public UserResponse updateProfile(
            UUID userId,
            ProfileUpdateRequest request,
            byte[] profilePicture,
            String pictureFilename) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new UserNotFoundException(
                                        "User not found."
                                ));

        if (StringUtils.hasText(
                request.getUsername()
        )) {

            user.setUsername(
                    request.getUsername()
            );
        }

        if (StringUtils.hasText(
                request.getNewPassword()
        )) {

            user.setPasswordHash(
                    passwordEncoder.encode(
                            request.getNewPassword()
                    )
            );
        }

        if (profilePicture != null) {
            user.setProfilePicture(profilePicture);
        }

        user =
                userRepository.save(
                        user
                );

        return toUserResponse(user);
    }


    private void validateUniqueFields(
            RegisterRequest request) {

        if (userRepository.existsByEmail(
                request.getEmail()
                        .toLowerCase()
        )) {

            throw new UserAlreadyExistsException(
                    "Email already exists."
            );
        }

        if (userRepository.existsByPhoneNumber(
                phoneValidator.toE164(
                        request.getPhoneNumber()
                )
        )) {

            throw new UserAlreadyExistsException(
                    "Phone number already exists."
            );
        }
    }


    private UserResponse toUserResponse(User user) {

        String profilePicBase64 = null;

        if (user.getProfilePicture() != null) {
            profilePicBase64 =
                    Base64.getEncoder()
                            .encodeToString(
                                    user.getProfilePicture()
                            );
        }

        return new UserResponse(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                profilePicBase64,
                user.isActive(),
                user.getCreatedAt() != null
                        ? user.getCreatedAt().toString()
                        : null
        );
    }
}