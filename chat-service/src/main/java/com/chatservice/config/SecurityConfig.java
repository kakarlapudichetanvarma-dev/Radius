package com.chatservice.config;

import com.chatservice.security.JwtAuthenticationFilter;
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

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final GatewayWhitelistFilter  gatewayWhitelistFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            GatewayWhitelistFilter  gatewayWhitelistFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.gatewayWhitelistFilter  = gatewayWhitelistFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/ws/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(gatewayWhitelistFilter,  UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter,  GatewayWhitelistFilter.class);

        return http.build();
    }

    // ── GatewayWhitelistFilter ────────────────────────────────────────────
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

            String uri = request.getRequestURI();

            // Health and WebSocket handshake bypass gateway check
            if (uri.equals("/actuator/health") || uri.startsWith("/ws")) {
                chain.doFilter(request, response);
                return;
            }

            String incoming = request.getHeader(GATEWAY_HEADER);
            if (incoming == null || !incoming.equals(gatewaySecret)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"success\":false," +
                        "\"message\":\"Direct access not allowed. Use the API Gateway.\"}");
                return;
            }

            chain.doFilter(request, response);
        }
    }
}
