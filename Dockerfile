# Stage 1: Build application JAR using Maven image
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Pre-fetch Maven dependencies for layer caching
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B || true

# Copy source code and build package
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests -B

# Stage 2: Runtime container
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
