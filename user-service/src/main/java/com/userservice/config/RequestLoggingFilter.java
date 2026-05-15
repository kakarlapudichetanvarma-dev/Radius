package com.userservice.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.logging.Logger;

@Component
@Order(1)
public class RequestLoggingFilter implements Filter {

    private static final Logger log =
            Logger.getLogger(RequestLoggingFilter.class.getName());

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest hReq = (HttpServletRequest) req;
        HttpServletResponse hRes = (HttpServletResponse) res;

        long start = Instant.now().toEpochMilli();
        String method = hReq.getMethod();
        String uri = hReq.getRequestURI();
        String ip = resolveIp(hReq);

        log.info(String.format("→ %s %s [IP: %s]", method, uri, ip));

        try {
            chain.doFilter(req, res);
        } finally {
            log.info(String.format(
                    "← %s %s | status=%d | %dms",
                    method,
                    uri,
                    hRes.getStatus(),
                    Instant.now().toEpochMilli() - start
            ));
        }
    }

    private String resolveIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank())
                ? xff.split(",")[0].trim()
                : req.getRemoteAddr();
    }
}