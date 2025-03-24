# Use the OpenJDK 17 image as the base image
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the Spring Boot JAR file from the host to the container
COPY ./target/spring-0.0.1-SNAPSHOT.jar /app/spring-0.0.1-SNAPSHOT.jar

# Expose the port that your Spring Boot app will run on
EXPOSE 8080

# Set environment variables (similar to docker-compose)
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/cosw
ENV SPRING_DATASOURCE_USERNAME=root
ENV SPRING_DATASOURCE_PASSWORD=root

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "spring-0.0.1-SNAPSHOT.jar"]