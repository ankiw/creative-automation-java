# Multi-stage build for optimized image size

# Stage 1: Build
FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy dependency definitions first (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM --platform=linux/amd64 eclipse-temurin:17-jre
WORKDIR /app

# Create non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy JAR from build stage
COPY --from=build /app/target/creative-automation-*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/pipeline/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
