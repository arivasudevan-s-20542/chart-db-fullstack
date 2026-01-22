# Build stage with Java 21 for Docker/GitHub Actions deployment
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

# Use github profile for Java 21 compilation
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests -Pgithub

# Runtime stage with Java 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
