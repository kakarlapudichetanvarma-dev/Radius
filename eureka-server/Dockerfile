# ══════════════════════════════════════════════════════════════════════
#  Dockerfile – discovery-service (Eureka Server)
#
#  Multi-stage build:
#    Stage 1 (builder) – compiles the Maven project and creates the fat-jar
#    Stage 2 (runtime) – minimal JRE image that only runs the jar
#
#  This keeps the final image as small as possible by discarding Maven
#  and all build-time tooling.
# ══════════════════════════════════════════════════════════════════════

# ── Stage 1: Build ─────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

LABEL maintainer="chatapp-team"
LABEL description="Eureka Discovery Server – build stage"

WORKDIR /build

# Copy dependency descriptors first so Docker can cache the Maven
# dependency layer independently from source changes.
COPY pom.xml .
COPY .mvn/ .mvn/

# Download all dependencies (cached unless pom.xml changes)
# Use -B (batch mode) and -q (quiet) for clean CI output
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B -q

# Copy source and compile
COPY src ./src
RUN mvn clean package -DskipTests -B -q

# ── Stage 2: Runtime ────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="chatapp-team"
LABEL description="Eureka Discovery Server – runtime image"
LABEL version="1.0.0"

# Create a non-root user for security best practices
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy only the compiled jar from the builder stage
COPY --from=builder /build/target/discovery-service-*.jar app.jar

# Create logs directory and set permissions
RUN mkdir -p logs && chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose the standard Eureka port
EXPOSE 8761

# JVM tuning flags for containerised environments:
#   -XX:+UseContainerSupport        – read cgroup memory/CPU limits
#   -XX:MaxRAMPercentage=75.0       – use 75% of container RAM for heap
#   -Djava.security.egd=...         – faster startup (avoid /dev/random block)
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -Djava.security.egd=file:/dev/./urandom"

# Healthcheck – Docker will mark the container unhealthy if this fails
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8761/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
