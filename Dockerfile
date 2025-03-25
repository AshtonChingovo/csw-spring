# Use official Maven image to build the application
FROM maven:3.8.8-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy project files
COPY . .

# Build the application (creates the JAR file in /app/target/)
RUN mvn clean package -DskipTests

# Use a lightweight JDK runtime image to run the app
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/csw-0.0.1-SNAPSHOT.jar /app/app.jar

# Set environment variables for database connection
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/cosw
ENV SPRING_DATASOURCE_USERNAME=root
ENV SPRING_DATASOURCE_PASSWORD=root

EXPOSE 8080

# Run the application
CMD ["java", "-jar", "/app/app.jar"]