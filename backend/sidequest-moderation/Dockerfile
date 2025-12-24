# Stage 1: Build
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -pl sidequest-moderation -am -DskipTests

# Stage 2: Run
FROM openjdk:17.0.2-jdk-slim-bullseye
WORKDIR /app
COPY --from=build /app/sidequest-moderation/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]

