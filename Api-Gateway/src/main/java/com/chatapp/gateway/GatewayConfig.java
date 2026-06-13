package com.chatapp.gateway;

import com.chatapp.gateway.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Value("${user.service.url}")
    private String userServiceUrl;

    @Value("${chat.service.url}")
    private String chatServiceUrl;

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    // Must match gateway.secret in every downstream service's application.properties
    @Value("${gateway.secret:chatapp-gateway-secret}")
    private String gatewaySecret;

    public GatewayConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()

                // ── AUTH SERVICE (public — no JWT filter) ─────────────────────
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**")
                        .filters(f -> f
                                // Add gateway secret header so auth-service
                                // knows the request came through the gateway
                                .addRequestHeader("X-Gateway-Source", gatewaySecret)
                        )
                        .uri(authServiceUrl))

                // ── USER SERVICE (protected — JWT required) ────────────────────
                // /api/v1/users/friends/request → /friends/request
                .route("user-service", r -> r
                        .path("/api/v1/users/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(
                                        new JwtAuthenticationFilter.Config()
                                ))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                                .rewritePath(
                                        "/api/v1/users/(?<segment>.*)",
                                        "/${segment}"
                                )
                                // Add gateway secret header — user-service
                                // rejects requests without this header
                                .addRequestHeader("X-Gateway-Source", gatewaySecret)
                        )
                        .uri(userServiceUrl))

                // ── CHAT SERVICE (protected — JWT required) ────────────────────
                .route("chat-service", r -> r
                        .path("/api/v1/chat/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(
                                        new JwtAuthenticationFilter.Config()
                                ))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                                .addRequestHeader("X-Gateway-Source", gatewaySecret)
                        )
                        .uri(chatServiceUrl))
                        // ── CHAT SERVICE (protected — JWT required) ────────────────────
.route("chat-service", r -> r
        .path("/api/v1/chat/**", "/api/v1/community/**")
        .filters(f -> f
                .filter(jwtAuthFilter.apply(
                        new JwtAuthenticationFilter.Config()
                ))
                .requestRateLimiter(config -> config
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(userKeyResolver()))
                .addRequestHeader("X-Gateway-Source", gatewaySecret)
        )
        .uri(chatServiceUrl))

// ── CHAT SERVICE WebSocket ─────────────────────────────────────  ✅ add this
.route("chat-service-ws", r -> r
        .path("/ws/**")
        .filters(f -> f
                .addRequestHeader("X-Gateway-Source", gatewaySecret)
        )
        .uri(chatServiceUrl))

                // ── NOTIFICATION SERVICE (protected — JWT required) ────────────
                .route("notification-service", r -> r
                        .path("/api/v1/notifications/**")
                        .filters(f -> f
                                .filter(jwtAuthFilter.apply(
                                        new JwtAuthenticationFilter.Config()
                                ))
                                .requestRateLimiter(config -> config
                                        .setRateLimiter(redisRateLimiter())
                                        .setKeyResolver(userKeyResolver()))
                                .rewritePath(
                                        "/api/v1/notifications/(?<segment>.*)",
                                        "/${segment}"
                                )
                                .addRequestHeader("X-Gateway-Source", gatewaySecret)
                        )
                        .uri(notificationServiceUrl))

                .build();
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20);
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.just(
                exchange.getRequest().getHeaders().getFirst("X-User-Id") != null
                        ? exchange.getRequest().getHeaders().getFirst("X-User-Id")
                        : "anonymous"
        );
    }
}