package com.chatapp.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * DiscoveryServiceApplication – Entry point for the Eureka Service Registry.
 *
 * <p>@EnableEurekaServer activates the embedded Netflix Eureka server.
 * All other microservices in the chat-app ecosystem will register here
 * and use this server to discover one another at runtime.
 *
 * <p>Dashboard is available at: http://localhost:8761
 */
@SpringBootApplication
@EnableEurekaServer  // Turns this Spring Boot app into a Eureka discovery server
public class EurekaServerApplication {

    private static final Logger log =
            LoggerFactory.getLogger(EurekaServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
        log.info("=======================================================");
        log.info("  Eureka Discovery Server started successfully");
        log.info("  Dashboard → http://localhost:8761");
        log.info("  Health   → http://localhost:8761/actuator/health");
        log.info("=======================================================");
    }
}
