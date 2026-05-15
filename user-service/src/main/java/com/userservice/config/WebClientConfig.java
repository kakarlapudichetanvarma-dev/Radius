package com.userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    // Now points to the API Gateway instead of auth-service directly.
    // All requests go: user-service → gateway (8080) → auth-service (8083)
    // Set in application.properties:
    //   auth.service.url=http://localhost:8080
    @Value("${auth.service.url:http://localhost:8080}")
    private String authServiceUrl;

    // Must match gateway.secret in gateway and auth-service
    @Value("${gateway.secret:chatapp-gateway-secret}")
    private String gatewaySecret;

    @Bean
    public WebClient webClient() {

        ExchangeStrategies strategies =
                ExchangeStrategies.builder()
                        .codecs(
                                (ClientCodecConfigurer configurer) ->
                                        configurer
                                                .defaultCodecs()
                                                .maxInMemorySize(20 * 1024 * 1024)
                        )
                        .build();

        // Calls auth-service THROUGH the gateway.
        // Gateway route: /api/v1/auth/** → auth-service
        // So AuthServiceClient calls like:
        //   /users/{id}          → gateway → auth-service /api/v1/auth/users/{id}
        //   /users/phone/{phone} → gateway → auth-service /api/v1/auth/users/phone/{phone}
        return WebClient.builder()
                .baseUrl(authServiceUrl + "/api/v1/auth")
                .defaultHeader("X-Gateway-Source", gatewaySecret)
                .exchangeStrategies(strategies)
                .build();
    }
}