package com.userservice.config;

import com.userservice.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final GatewayWhitelistFilter gatewayWhitelistFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtFilter,
            GatewayWhitelistFilter gatewayWhitelistFilter) {
        this.jwtFilter = jwtFilter;
        this.gatewayWhitelistFilter = gatewayWhitelistFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                // Whitelist filter runs FIRST — blocks anything not from gateway
                // before JWT filter even runs
                .addFilterBefore(
                        gatewayWhitelistFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterAfter(
                        jwtFilter,
                        GatewayWhitelistFilter.class
                );

        return http.build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // GatewayWhitelistFilter
    //
    // Blocks any request that did NOT come through the API Gateway.
    // The gateway adds a secret header "X-Gateway-Source" to every request
    // it forwards. If that header is missing or has the wrong value,
    // this filter immediately returns 403 — the JWT filter never runs.
    //
    // This means:
    //   localhost:8082/friends/request        → 403 (no gateway header)
    //   localhost:8080/api/v1/users/friends/request → 200 (gateway adds header)
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

            // Always let health checks through
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