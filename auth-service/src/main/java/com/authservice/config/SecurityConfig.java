package com.authservice.config;

import com.authservice.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final GatewayWhitelistFilter gatewayWhitelistFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            GatewayWhitelistFilter gatewayWhitelistFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.gatewayWhitelistFilter = gatewayWhitelistFilter;
    }

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/verify-otp",
            "/actuator/health"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated()
                )
                // GatewayWhitelistFilter runs first — blocks everything
                // not coming through the API Gateway, no exceptions
                .addFilterBefore(
                        gatewayWhitelistFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterAfter(
                        jwtAuthenticationFilter,
                        GatewayWhitelistFilter.class
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ─────────────────────────────────────────────────────────────────────
    // GatewayWhitelistFilter
    //
    // Blocks ALL requests that did not come through the API Gateway.
    // No exceptions — every request must have the X-Gateway-Source header.
    //
    //   localhost:8083/api/v1/auth/login         → 403 blocked
    //   localhost:8083/api/v1/auth/users/{id}    → 403 blocked
    //   localhost:8080/api/v1/auth/login         → 200 works via gateway
    //   localhost:8080/api/v1/auth/users/{id}    → 200 works via gateway
    // ─────────────────────────────────────────────────────────────────────
    @Component
    public static class GatewayWhitelistFilter extends OncePerRequestFilter {

        @Value("${gateway.secret:chatapp-gateway-secret}")
        private String gatewaySecret;

        private static final String GATEWAY_HEADER = "X-Gateway-Source";

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain chain)
                throws ServletException, IOException {

            // Only health check bypasses the gateway requirement
            if (request.getRequestURI().equals("/actuator/health")) {
                chain.doFilter(request, response);
                return;
            }

            String incomingHeader = request.getHeader(GATEWAY_HEADER);

            if (incomingHeader == null || !incomingHeader.equals(gatewaySecret)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"success\":false," +
                        "\"message\":\"Direct access not allowed. Use the API Gateway.\"}"
                );
                return;
            }

            chain.doFilter(request, response);
        }
    }
}