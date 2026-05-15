package com.authservice.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
@Order(1)
public class RequestLoggingFilter implements Filter {

    private static final Logger log =
            LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        long startMs = Instant.now().toEpochMilli();

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String clientIp = resolveClientIp(request);

        log.info("-> {} {} [IP: {}]", method, uri, clientIp);

        try {
            chain.doFilter(req, res);
        } finally {
            long duration = Instant.now().toEpochMilli() - startMs;
            log.info("<- {} {} | status={} | {}ms",
                    method, uri, response.getStatus(), duration);
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