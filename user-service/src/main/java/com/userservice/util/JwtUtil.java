package com.userservice.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.logging.Logger;

@Component
public class JwtUtil {

    private static final Logger log =
            Logger.getLogger(JwtUtil.class.getName());

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private Key signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    /** Returns the userId (subject) claim, or null if the token is invalid. */
    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractEmail(String token) {
        return (String) parseClaims(token).get("email");
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (MalformedJwtException e) {
            log.severe(String.format(
                    "Malformed JWT: %s",
                    e.getMessage()
            ));
        } catch (ExpiredJwtException e) {
            log.severe(String.format(
                    "Expired JWT: %s",
                    e.getMessage()
            ));
        } catch (UnsupportedJwtException e) {
            log.severe(String.format(
                    "Unsupported JWT: %s",
                    e.getMessage()
            ));
        } catch (IllegalArgumentException e) {
            log.severe(String.format(
                    "Empty JWT claims: %s",
                    e.getMessage()
            ));
        }

        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}