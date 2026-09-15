# Stage 1: Build application JAR using Maven
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY pom.xml .
COPY .mvn ./.mvn
COPY src ./src

RUN ./mvnw package -DskipTests -Dmaven.resolver.transport=wagon || mvn package -DskipTests

# Stage 2: Runtime container
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
