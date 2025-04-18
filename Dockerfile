# Use a small Java runtime image
FROM openjdk:17-jdk-slim

# Create app directory
WORKDIR /app

# Copy the compiled jar
COPY target/*.jar app.jar

# Expose default Spring Boot port
EXPOSE 8080

# Start the app
ENTRYPOINT ["java", "-jar", "app.jar"]
