package com.userservice.security;

import com.userservice.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log =
            Logger.getLogger(JwtAuthenticationFilter.class.getName());

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        log.fine(String.format(
                "JWT filter processing: %s %s",
                request.getMethod(),
                uri
        ));

        try {

            String token =
                    extractToken(request);

            if (StringUtils.hasText(token)
                    && jwtUtil.validateToken(token)) {

                String userId =
                        jwtUtil.extractUserId(token);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                Collections.emptyList()
                        );

                auth.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(auth);

                log.fine(String.format(
                        "Authenticated userId=%s for %s",
                        userId,
                        uri
                ));
            }

        } catch (Exception e) {

            log.severe(String.format(
                    "JWT filter error for %s: %s",
                    uri,
                    e.getMessage()
            ));
        }

        chain.doFilter(request, response);
    }

    private String extractToken(
            HttpServletRequest req) {

        String bearer =
                req.getHeader("Authorization");

        if (StringUtils.hasText(bearer)
                && bearer.startsWith("Bearer ")) {

            return bearer.substring(7);
        }

        return null;
    }
}