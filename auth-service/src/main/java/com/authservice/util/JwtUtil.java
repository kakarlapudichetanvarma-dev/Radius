package com.authservice.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private static final Logger log =
            LoggerFactory.getLogger(JwtUtil.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    private Key getSigningKey() {

        byte[] keyBytes =
                Decoders.BASE64.decode(jwtSecret);

        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(
            UUID userId,
            String email,
            String username) {

        Date now = new Date();

        Date expiry =
                new Date(now.getTime() + jwtExpirationMs);

        log.debug(
                "Generating JWT token for user: {}",
                email
        );

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("email", email)
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(
                        getSigningKey(),
                        SignatureAlgorithm.HS256
                )
                .compact();
    }

    public String extractUserId(
            String token) {

        return parseClaims(token).getSubject();
    }

    public String extractEmail(
            String token) {

        return (String)
                parseClaims(token).get("email");
    }

    public boolean validateToken(
            String token) {

        try {

            parseClaims(token);

            return true;

        } catch (MalformedJwtException e) {

            log.error(
                    "Invalid JWT token: {}",
                    e.getMessage()
            );

        } catch (ExpiredJwtException e) {

            log.error(
                    "JWT token is expired: {}",
                    e.getMessage()
            );

        } catch (UnsupportedJwtException e) {

            log.error(
                    "JWT token is unsupported: {}",
                    e.getMessage()
            );

        } catch (IllegalArgumentException e) {

            log.error(
                    "JWT claims string is empty: {}",
                    e.getMessage()
            );
        }

        return false;
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }

    private Claims parseClaims(
            String token) {

        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}