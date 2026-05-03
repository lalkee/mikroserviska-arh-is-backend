# Build stage
# Using Amazon Corretto 25 as it is the most stable Maven/Java 25 combo currently on Docker Hub
FROM maven:3.9.15-amazoncorretto-25 AS build
WORKDIR /app

# Optimization: Only copy pom.xml to cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
# Using the Corretto 25 JDK-slim image since the alpine-jre version for 25 is not yet standardized
FROM amazoncorretto:25-al2023-jdk
WORKDIR /app

# Use a wildcard or a more generic name if your version changes frequently
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]