package com.chatservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import com.chatservice.exception.ChatExceptions.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import java.util.Map;
import java.util.UUID;

@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final WebClient authWebClient;
    private final HttpServletRequest request;

    public UserServiceClient(
            WebClient authWebClient,
            HttpServletRequest request) {

        this.authWebClient = authWebClient;
        this.request = request;
    }

    public UUID getUserIdByUsername(String username) {
        try {
            log.info("Calling auth service for username={}", username);

            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

            log.info("Forwarding Authorization header={}", authHeader);

            Map response = authWebClient.get()
                    .uri("/api/v1/auth/users/username/" + username)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("Auth service raw response={}", response);

            if (response == null) {
                log.error("Auth service returned null response for username={}", username);
                throw new UserNotFoundException("User not found: " + username);
            }

            Object dataObj = response.get("data");
            log.info("data field={}", dataObj);

            if (dataObj == null) {
                log.error("data field is null in response for username={}", username);
                throw new UserNotFoundException("User not found: " + username);
            }

            Map data = (Map) dataObj;
            log.info("data map keys={}", data.keySet());

            // try both "id" and "userId" since we don't know the field name
            String userId = data.get("id") != null
                    ? (String) data.get("id")
                    : data.get("userId") != null
                        ? String.valueOf(data.get("userId"))
                        : null;

            log.info("Resolved userId={} for username={}", userId, username);

            if (userId == null) {
                log.error("Neither 'id' nor 'userId' found in data for username={}", username);
                throw new UserNotFoundException("User not found: " + username);
            }

            return UUID.fromString(userId);

        } catch (UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Exception calling auth service for username={} error={}", username, e.getMessage(), e);
            throw new UserNotFoundException("User not found: " + username);
        }
    }
}