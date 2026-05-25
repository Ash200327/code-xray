FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the pre-compiled executable JAR from the local target directory
COPY target/*.jar app.jar

# Render injects PORT env var; Spring Boot reads SERVER_PORT
ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
