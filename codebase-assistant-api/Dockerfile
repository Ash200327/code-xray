# ── Stage 1: Build with Maven ──────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy POM first for layer-cache of dependency downloads
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build the fat JAR (skip tests for faster CI builds)
COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime image ──────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the executable JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Render injects PORT env var; Spring Boot reads SERVER_PORT
ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
