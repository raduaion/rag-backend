# -----------------------
# 1) BUILD STAGE
# -----------------------
FROM maven:3.9.11-eclipse-temurin-17 AS builder

# Create and use /app directory for building
WORKDIR /app

# Copy the Maven config and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the source code
COPY src ./src
# Copy the license
COPY config ./config

RUN mvn -q -e spotless:check

# Build the Spring Boot JAR (skip tests for faster builds if desired)
RUN mvn package -DskipTests


# -----------------------
# 2) RUN STAGE
# -----------------------
FROM eclipse-temurin:17-jre

# Create directories for Hermes configs, data and logs
RUN mkdir -p /opt/hermes/config \
    && mkdir -p /opt/hermes/data \
    && mkdir -p /opt/hermes/logs

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar /app/my-spring-app.jar

# (Optional) Set environment variables to configure logs and config location
#   If your Spring Boot app uses them:
# ENV SPRING_CONFIG_LOCATION="/opt/hermes/config/"
# ENV LOGGING_FILE_PATH="/opt/hermes/logs"
#   Then add them to the java command if needed:
# ENV JAVA_OPTS="-Dspring.config.location=file:${SPRING_CONFIG_LOCATION} \
#                -Dlogging.file.path=${LOGGING_FILE_PATH}"
# Expose the Spring Boot port
EXPOSE 8080

# Optional: run with extra Java opts for config/log dirs
# ENTRYPOINT ["java", "-jar", "/app/my-spring-app.jar"]

# Or if passing environment variables for config/logging:
ENTRYPOINT ["java", \
    "-Dspring.config.location=file:/opt/hermes/config/", \
    "-Dlogging.file.path=/opt/hermes/logs/", \
    "-jar", "/app/my-spring-app.jar"]

