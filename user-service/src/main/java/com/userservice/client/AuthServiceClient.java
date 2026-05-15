package com.userservice.client;

import com.userservice.dto.AuthApiResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class AuthServiceClient {

    private final WebClient webClient;

    public AuthServiceClient(WebClient webClient) {
        this.webClient = webClient;
    }

    private String normalizeToken(String token) {
        return "Bearer " + token.replace("Bearer ", "");
    }

    public AuthApiResponse getUserByPhone(String phoneNumber, String token) {
        return webClient
                .get()
                .uri("/users/phone/" + phoneNumber)
                .header("Authorization", normalizeToken(token))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new RuntimeException("Auth service error: " + body)))
                )
                .bodyToMono(AuthApiResponse.class)
                .onErrorResume(ex -> {
                    java.util.logging.Logger.getLogger(AuthServiceClient.class.getName())
                            .warning("getUserByPhone failed for " + phoneNumber + ": " + ex.getMessage());
                    return Mono.empty();
                })
                .block();
    }

    public AuthApiResponse getUserByUsername(String username, String token) {
        return webClient
                .get()
                .uri("/users/username/" + username)
                .header("Authorization", normalizeToken(token))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new RuntimeException("Auth service error: " + body)))
                )
                .bodyToMono(AuthApiResponse.class)
                .onErrorResume(ex -> {
                    java.util.logging.Logger.getLogger(AuthServiceClient.class.getName())
                            .warning("getUserByUsername failed for " + username + ": " + ex.getMessage());
                    return Mono.empty();
                })
                .block();
    }

    public AuthApiResponse getUserById(UUID userId, String token) {
        return webClient
                .get()
                .uri("/users/" + userId.toString())
                .header("Authorization", normalizeToken(token))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new RuntimeException("Auth service error [" + userId + "]: " + body)))
                )
                .bodyToMono(AuthApiResponse.class)
                .onErrorResume(ex -> {
                    java.util.logging.Logger.getLogger(AuthServiceClient.class.getName())
                            .warning("getUserById failed for " + userId + ": " + ex.getMessage());
                    return Mono.empty();
                })
                .block();
    }
}