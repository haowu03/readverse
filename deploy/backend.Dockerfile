# ReadVerse backend image (multi-stage build)
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /build/rv-server/target/rv-server-1.0.0.jar /app/app.jar
RUN mkdir -p /app/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
