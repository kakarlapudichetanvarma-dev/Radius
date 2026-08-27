# ══════════════════════════════════════════════════════════════════════
#  Dockerfile – eureka-server
# ══════════════════════════════════════════════════════════════════════

FROM eclipse-temurin:21-jdk-alpine AS builder

LABEL maintainer="chatapp-team"
LABEL description="Eureka Discovery Server - build stage"

WORKDIR /build

COPY pom.xml .
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn clean package -DskipTests -B -q

FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="chatapp-team"
LABEL description="Eureka Discovery Server - runtime image"
LABEL version="1.0.0"

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /build/target/discovery-service-*.jar app.jar

RUN mkdir -p logs && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8761

ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8761/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]