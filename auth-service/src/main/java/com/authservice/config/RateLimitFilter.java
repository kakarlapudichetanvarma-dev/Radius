package com.authservice.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

@Component
@Order(2) // runs after RequestLoggingFilter (Order 1), before Security filters
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(RateLimitFilter.class);

    @Value("${rate.limit.capacity:10}")
    private long capacity;

    @Value("${rate.limit.refill-tokens:10}")
    private long refillTokens;

    @Value("${rate.limit.refill-seconds:60}")
    private long refillSeconds;

    private final ProxyManager<String> proxyManager;

    public RateLimitFilter(ProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        // Skip health check
        if (request.getRequestURI().equals("/actuator/health")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        String bucketKey = "rate_limit:" + clientIp;

        // Each IP gets its own bucket in Redis
        Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(
                        capacity,
                        Refill.greedy(refillTokens, Duration.ofSeconds(refillSeconds))
                ))
                .build();

        Bucket bucket = proxyManager.builder().build(bucketKey, configSupplier);

        if (bucket.tryConsume(1)) {
            // Request allowed — add remaining count header so client can see it
            long remaining = bucket.getAvailableTokens();
            response.addHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            response.addHeader("X-RateLimit-Limit", String.valueOf(capacity));
            chain.doFilter(request, response);
        } else {
            // Blocked
            log.warn("Rate limit exceeded for IP: {} on {}", clientIp, request.getRequestURI());
            response.setStatus(429);
            response.setContentType("application/json");
            response.addHeader("X-RateLimit-Remaining", "0");
            response.addHeader("X-RateLimit-Limit", String.valueOf(capacity));
            response.getWriter().write(
                    "{\"success\":false," +
                    "\"message\":\"Too many requests. Please slow down and try again later.\"}"
            );
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}