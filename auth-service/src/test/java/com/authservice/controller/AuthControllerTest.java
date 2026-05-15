package com.authservice.controller;

import com.authservice.dto.AuthDtos.*;
import com.authservice.exception.AuthExceptions.*;
import com.authservice.security.JwtAuthenticationFilter;
import com.authservice.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@ActiveProfiles("test")
@DisplayName("AuthController Integration Tests (MockMvc)")
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private AuthService authService;

        private UserResponse sampleUser;
        private AuthTokenResponse sampleToken;

        @BeforeEach
        void setUp() {

            sampleUser =
                    new UserResponse(
                            UUID.randomUUID().toString(),
                            "testuser",
                            "test@example.com",
                            "+911234567890",
                            null,
                            true,
                            "2024-01-01T10:00:00"
                    );

            sampleToken =
                    new AuthTokenResponse(
                            "mock.jwt.token.here",
                            "Bearer",
                            86400L,
                            sampleUser
                    );
        }

        // =============================================
        // POST /register
        // =============================================

        @Test
        @DisplayName("POST /register: 201 Created on valid request")
        void register_validRequest_returns201() throws Exception {
                RegisterRequest request = new RegisterRequest(
                                "testuser", "test@example.com", "+911234567890", "SecureP@ss1");

                when(authService.register(any(RegisterRequest.class))).thenReturn(sampleUser);

                mockMvc.perform(post("/api/v1/auth/register")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.email").value("test@example.com"));
        }

        @Test
        @DisplayName("POST /register: 400 Bad Request on invalid email")
        void register_invalidEmail_returns400() throws Exception {
                RegisterRequest request = new RegisterRequest(
                                "testuser", "not-an-email", "+911234567890", "SecureP@ss1");

                mockMvc.perform(post("/api/v1/auth/register")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /register: 409 Conflict when email already exists")
        void register_emailConflict_returns409() throws Exception {
                RegisterRequest request = new RegisterRequest(
                                "testuser", "test@example.com", "+911234567890", "SecureP@ss1");

                when(authService.register(any())).thenThrow(
                                new UserAlreadyExistsException("An account with this email already exists."));

                mockMvc.perform(post("/api/v1/auth/register")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("POST /register: 400 on short password")
        void register_shortPassword_returns400() throws Exception {
                RegisterRequest request = new RegisterRequest(
                                "testuser", "test@example.com", "+911234567890", "short");

                mockMvc.perform(post("/api/v1/auth/register")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        // =============================================
        // POST /login
        // =============================================

        @Test
        @DisplayName("POST /login: 200 OK on valid credentials")
        void login_validCredentials_returns200() throws Exception {

            LoginRequest request =
                    new LoginRequest(
                            "test@example.com",
                            "SecureP@ss1"
                    );

            ApiResponse apiResp =
                    new ApiResponse(
                            true,
                            "OTP sent to your email.",
                            null
                    );

            when(
                    authService.login(
                            any(LoginRequest.class)
                    )
            ).thenReturn(apiResp);

            mockMvc.perform(
                            post("/api/v1/auth/login")
                                    .with(csrf())
                                    .contentType(
                                            MediaType.APPLICATION_JSON
                                    )
                                    .content(
                                            objectMapper.writeValueAsString(
                                                    request
                                            )
                                    )
                    )
                    .andExpect(
                            status().isOk()
                    )
                    .andExpect(
                            jsonPath("$.success")
                                    .value(true)
                    );
        }
        @Test
        @DisplayName("POST /login: 401 Unauthorized on invalid credentials")
        void login_invalidCredentials_returns401() throws Exception {
                LoginRequest request = new LoginRequest("test@example.com", "wrongpass");

                when(authService.login(any())).thenThrow(
                                new InvalidCredentialsException("Invalid email or password."));

                mockMvc.perform(post("/api/v1/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.status").value(401));
        }

        // =============================================
        // POST /verify-otp
        // =============================================

        @Test
        @DisplayName("POST /verify-otp: 200 OK with JWT on valid OTP")
        void verifyOtp_validOtp_returns200WithToken() throws Exception {
                OtpVerifyRequest request = new OtpVerifyRequest("test@example.com", "123456");

                when(authService.verifyOtp(any(OtpVerifyRequest.class))).thenReturn(sampleToken);

                mockMvc.perform(post("/api/v1/auth/verify-otp")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.token").value("mock.jwt.token.here"))
                                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("POST /verify-otp: 400 Bad Request on wrong OTP")
        void verifyOtp_wrongOtp_returns400() throws Exception {
                OtpVerifyRequest request = new OtpVerifyRequest("test@example.com", "000000");

                when(authService.verifyOtp(any())).thenThrow(
                                new InvalidOtpException("The OTP you entered is incorrect."));

                mockMvc.perform(post("/api/v1/auth/verify-otp")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /verify-otp: 410 Gone on expired OTP")
        void verifyOtp_expiredOtp_returns410() throws Exception {
                OtpVerifyRequest request = new OtpVerifyRequest("test@example.com", "123456");

                when(authService.verifyOtp(any())).thenThrow(
                                new OtpExpiredException("OTP has expired. Please request a new one."));

                mockMvc.perform(post("/api/v1/auth/verify-otp")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isGone());
        }

        @Test
        @DisplayName("POST /verify-otp: 400 Bad Request when OTP is not 6 digits")
        void verifyOtp_invalidOtpFormat_returns400() throws Exception {
                OtpVerifyRequest request = new OtpVerifyRequest("test@example.com", "12345"); // 5 digits

                mockMvc.perform(post("/api/v1/auth/verify-otp")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        // =============================================
        // GET /profile
        // =============================================

        @Test
        @DisplayName("GET /profile: 200 OK for authenticated user")
        @WithMockUser(username = "00000000-0000-0000-0000-000000000001")
        void getProfile_authenticated_returns200() throws Exception {
                when(authService.getProfile(any(UUID.class))).thenReturn(sampleUser);

                mockMvc.perform(get("/api/v1/auth/profile"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.email").value("test@example.com"));
        }

        @Test
        @DisplayName("GET /profile: 401 Unauthorized without authentication")
        void getProfile_unauthenticated_returns401() throws Exception {
                mockMvc.perform(get("/api/v1/auth/profile"))
                                .andExpect(status().isUnauthorized());
        }
}
