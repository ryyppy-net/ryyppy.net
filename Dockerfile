# syntax=docker/dockerfile:1
# https://docs.spring.io/spring-boot/reference/packaging/container-images/dockerfiles.html
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
# Railway scopes cache mounts by service: https://docs.railway.com/builds/dockerfiles#cache-mounts
RUN --mount=type=cache,id=s/051f3916-5603-418f-a470-2c39ca314729-/root/.m2,target=/root/.m2 mvn -B -DskipTests package

FROM bellsoft/liberica-openjre-debian:25-cds AS builder
WORKDIR /builder
COPY --from=build /build/target/ryyppynet.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM bellsoft/liberica-openjre-debian:25-cds
WORKDIR /application
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./

# Railway injects service variables for any ARG declared in the stage using
# them. These land in the image history: `docker history` reveals the password.
ARG SPRING_DATASOURCE_URL
ARG SPRING_DATASOURCE_USERNAME
ARG SPRING_DATASOURCE_PASSWORD

# AOT cache training run against the real database. Safe with Spring AOT
# enabled since the jar has no flywayInitializer bean (see pom.xml).
# Best-effort: a failed run leaves no cache and the image boots without it.
RUN java -XX:AOTCacheOutput=app.aot \
    -Dspring.aot.enabled=true \
    -Dspring.context.exit=onRefresh \
    -jar application.jar \
    || echo "WARNING: AOT cache training failed; starting without a cache"

ENTRYPOINT ["java", "-XX:AOTCache=app.aot", "-Dspring.aot.enabled=true", "-jar", "application.jar"]
