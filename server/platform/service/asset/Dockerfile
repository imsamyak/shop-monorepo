# syntax=docker/dockerfile:1
# The asset service. Build with server/platform as the context (the build needs the shared modules):
#   cd server/platform && docker build -f service/asset/Dockerfile -t shop-asset .
# The image runs the "prod" profile, so every setting arrives as an environment variable (see application-prod.yml).

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests -pl service/asset -am package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /build/service/asset/target/*-SNAPSHOT.jar app.jar
USER app

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8088
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3   CMD wget -q -O /dev/null http://localhost:8088/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
