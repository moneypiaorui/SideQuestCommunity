# syntax=docker/dockerfile:1.6
# Stage 1: Build
FROM maven:3.8.5-openjdk-17 AS build
ARG MAVEN_OPTS="-Xmx512m -XX:MaxMetaspaceSize=256m"
ENV MAVEN_OPTS=${MAVEN_OPTS}
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn clean package -pl sidequest-media -am -DskipTests

# Stage 2: Run
FROM openjdk:17.0.2-jdk-slim-bullseye
RUN apt-get update && apt-get install -y ffmpeg && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/sidequest-media/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]




