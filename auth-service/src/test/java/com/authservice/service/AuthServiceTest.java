package com.authservice.service;

import com.authservice.dto.AuthDtos.*;
import com.authservice.entity.User;
import com.authservice.exception.AuthExceptions.*;
import com.authservice.repository.UserRepository;
import com.authservice.service.impl.AuthServiceImpl;
import com.authservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository  userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService    emailService;
    @Mock private OtpService      otpService;
    @Mock private JwtUtil         jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;
    private UUID userId;

    @BeforeEach
    void setUp() {

        userId = UUID.randomUUID();

        sampleUser = new User();

        sampleUser.setId(userId);
        sampleUser.setUsername("testuser");
        sampleUser.setEmail("test@example.com");
        sampleUser.setPhoneNumber("+911234567890");
        sampleUser.setPasswordHash("$2a$12$hashedpassword");
        sampleUser.setActive(true);
        sampleUser.setCreatedAt(LocalDateTime.now());
        sampleUser.setUpdatedAt(LocalDateTime.now());
    }

    // =============================================
    // Register Tests
    // =============================================

    @Test
    @DisplayName("register: success — persists user and sends welcome email")
    void register_success() {
        RegisterRequest request = new RegisterRequest(
                "testuser", "test@example.com", "+911234567890", "SecureP@ss1");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashed");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUsername()).isEqualTo("testuser");

        verify(userRepository).save(any(User.class));
        verify(emailService).sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("register: throws UserAlreadyExistsException when email is taken")
    void register_emailAlreadyExists_throws() {
        RegisterRequest request = new RegisterRequest(
                "newuser", "test@example.com", "+911234567890", "SecureP@ss1");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("email");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: throws UserAlreadyExistsException when username is taken")
    void register_usernameAlreadyExists_throws() {
        RegisterRequest request = new RegisterRequest(
                "testuser", "other@example.com", "+911234567890", "SecureP@ss1");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Username");
    }

    @Test
    @DisplayName("register: throws UserAlreadyExistsException when phone is taken")
    void register_phoneAlreadyExists_throws() {
        RegisterRequest request = new RegisterRequest(
                "testuser", "new@example.com", "+911234567890", "SecureP@ss1");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber("+911234567890")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("phone");
    }

    // =============================================
    // Login Tests
    // =============================================

    @Test
    @DisplayName("login: success — generates and sends OTP")
    void login_success() {
        LoginRequest request = new LoginRequest("test@example.com", "password");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password", sampleUser.getPasswordHash())).thenReturn(true);
        when(otpService.generateAndStoreOtp(anyString())).thenReturn("123456");

        ApiResponse response = authService.login(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("OTP");

        verify(otpService).generateAndStoreOtp("test@example.com");
        verify(emailService).sendOtpEmail(anyString(), anyString(), eq("123456"));
    }

    @Test
    @DisplayName("login: throws InvalidCredentialsException for unknown email")
    void login_userNotFound_throws() {
        LoginRequest request = new LoginRequest("unknown@example.com", "password");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("login: throws InvalidCredentialsException for wrong password")
    void login_wrongPassword_throws() {
        LoginRequest request = new LoginRequest("test@example.com", "wrongpass");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongpass", sampleUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(otpService, never()).generateAndStoreOtp(anyString());
    }

    @Test
    @DisplayName("login: throws AccountInactiveException for disabled account")
    void login_inactiveAccount_throws() {
        sampleUser.setActive(false);
        LoginRequest request = new LoginRequest("test@example.com", "password");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountInactiveException.class);
    }

    // =============================================
    // OTP Verification Tests
    // =============================================

    @Test
    @DisplayName("verifyOtp: success — returns JWT token")
    void verifyOtp_success() {
        OtpVerifyRequest request = new OtpVerifyRequest("test@example.com", "123456");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        doNothing().when(otpService).validateOtp("test@example.com", "123456");
        when(jwtUtil.generateToken(any(), anyString(), anyString())).thenReturn("mock.jwt.token");
        when(jwtUtil.getExpirationMs()).thenReturn(86400000L);

        AuthTokenResponse response = authService.verifyOtp(request);

        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("verifyOtp: throws InvalidOtpException for wrong OTP")
    void verifyOtp_invalidOtp_throws() {
        OtpVerifyRequest request = new OtpVerifyRequest("test@example.com", "000000");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        doThrow(new InvalidOtpException("OTP is incorrect"))
                .when(otpService).validateOtp("test@example.com", "000000");

        assertThatThrownBy(() -> authService.verifyOtp(request))
                .isInstanceOf(InvalidOtpException.class);
    }

    @Test
    @DisplayName("verifyOtp: throws OtpExpiredException when OTP has expired")
    void verifyOtp_expiredOtp_throws() {
        OtpVerifyRequest request = new OtpVerifyRequest("test@example.com", "123456");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        doThrow(new OtpExpiredException("OTP has expired"))
                .when(otpService).validateOtp("test@example.com", "123456");

        assertThatThrownBy(() -> authService.verifyOtp(request))
                .isInstanceOf(OtpExpiredException.class);
    }

    // =============================================
    // Profile Update Tests
    // =============================================

    @Test
    @DisplayName("updateProfile: success — updates username")
    void updateProfile_updateUsername_success() {
    	ProfileUpdateRequest request =
    	        new ProfileUpdateRequest(
    	                "newusername",
    	                null
    	        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByUsername("newusername")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse response = authService.updateProfile(userId, request, null, null);

        assertThat(response.getUsername()).isEqualTo("newusername");
    }

    @Test
    @DisplayName("updateProfile: throws UserAlreadyExistsException if new username is taken")
    void updateProfile_usernameTaken_throws() {
    	ProfileUpdateRequest request =
    	        new ProfileUpdateRequest(
    	                "takenuser",
    	                null
    	        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByUsername("takenuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.updateProfile(userId, request, null, null))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    @DisplayName("updateProfile: updates password when newPassword is provided")
    void updateProfile_updatePassword_success() {
    	ProfileUpdateRequest request =
    	        new ProfileUpdateRequest(
    	                null,
    	                "NewP@ss123"
    	        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.encode("NewP@ss123")).thenReturn("$2a$12$newHash");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        authService.updateProfile(userId, request, null, null);

        verify(passwordEncoder).encode("NewP@ss123");
    }

    @Test
    @DisplayName("getProfile: returns user data for valid userId")
    void getProfile_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        UserResponse response = authService.getProfile(userId);

        assertThat(response.getId()).isEqualTo(userId.toString());
        assertThat(response.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("getProfile: throws UserNotFoundException for unknown userId")
    void getProfile_notFound_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getProfile(userId))
                .isInstanceOf(UserNotFoundException.class);
    }
}
