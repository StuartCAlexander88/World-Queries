# Stage 1: Build the shaded JAR
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package

# Stage 2: Run the JAR
FROM eclipse-temurin:17
WORKDIR /app

# Copy the shaded JAR from the builder stage
COPY --from=build /app/target/World-Queries-1.0-SNAPSHOT.jar ./app.jar

# Run the application
CMD ["java", "-jar", "app.jar"]