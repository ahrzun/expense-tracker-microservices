# syntax=docker/dockerfile:1
#
# Build context is the REPOSITORY ROOT, not this directory, because this module's
# parent POM lives at the root. See docker-compose.yml:
#     build: { context: ., dockerfile: analytics-service/Dockerfile }

# ---------- stage 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy only the POMs first so dependency resolution is a cached layer that is
# not invalidated every time application source changes.
# All four module POMs are needed: the root POM lists them as <modules>, so
# Maven must be able to read them to construct the reactor, even with -pl.
COPY pom.xml .
COPY expense-service/pom.xml expense-service/
COPY auth-service/pom.xml auth-service/
COPY api-gateway/pom.xml api-gateway/
COPY analytics-service/pom.xml analytics-service/
RUN mvn -B -pl analytics-service -am dependency:go-offline

COPY analytics-service/src analytics-service/src
RUN mvn -B -pl analytics-service -am -DskipTests package

# ---------- stage 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine
# The Docker healthcheck shells out to curl. A slim JRE image does NOT ship it,
# and a healthcheck whose binary is missing exits 127 forever, leaving the
# container permanently "unhealthy" and deadlocking every service_healthy
# dependency. So install it explicitly.
RUN apk add --no-cache curl

# Don't run the application as root.
RUN addgroup -S app && adduser -S app -G app
USER app

WORKDIR /app
COPY --from=build /build/analytics-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
