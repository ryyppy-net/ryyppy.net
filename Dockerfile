# syntax=docker/dockerfile:1
# https://docs.spring.io/spring-boot/reference/packaging/container-images/dockerfiles.html
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
# This stage never has .git in its build context (see .dockerignore), so
# git-commit-id-maven-plugin (pom.xml) has nothing to read here - write the
# git.properties it would otherwise generate ourselves, from Railway's own
# build-time commit variable, and skip the plugin so it doesn't overwrite it.
ARG RAILWAY_GIT_COMMIT_SHA
RUN echo "git.commit.id=$RAILWAY_GIT_COMMIT_SHA" > src/main/resources/git.properties
# Railway scopes cache mounts by service: https://docs.railway.com/builds/dockerfiles#cache-mounts
RUN --mount=type=cache,id=s/051f3916-5603-418f-a470-2c39ca314729-/root/.m2,target=/root/.m2 mvn -B -DskipTests -Dmaven.gitcommitid.skip=true package

# Zulu is the only JDK shipping CRaC's warp engine, and the JDK rather than the
# JRE because checkpoint.sh triggers the checkpoint through jcmd.
FROM azul/zulu-openjdk:25-jdk-crac AS builder
WORKDIR /builder
COPY --from=build /build/target/ryyppynet.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM azul/zulu-openjdk:25-jdk-crac
WORKDIR /application
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
COPY --chmod=0755 checkpoint.sh ./

# Railway injects service variables for any ARG declared in the stage using
# them. These land in the image history: `docker history` reveals the password.
# Only secrets need declaring - the rest of the production configuration lives
# in application-production.yml.
ARG SPRING_DATASOURCE_URL
ARG SPRING_DATASOURCE_USERNAME
ARG SPRING_DATASOURCE_PASSWORD
ARG GOOGLE_CLIENT_SECRET
ARG AUTH_RELAY_SECRET

# AOT cache training run against the real database. Safe with Spring AOT
# enabled since the jar has no flywayInitializer bean (see pom.xml).
# Best-effort: a failed run leaves no cache and the image boots without it.
RUN java -XX:AOTCacheOutput=app.aot \
    -Dspring.aot.enabled=true \
    -Dspring.context.exit=onRefresh \
    -jar application.jar \
    || echo "WARNING: AOT cache training failed; starting without a cache"

RUN ./checkpoint.sh

# Railway starts a fresh container on every wake from serverless sleep, so this
# restores on each wake. A build whose checkpoint step found no database wrote
# none, and boots instead; a checkpoint that fails to restore is a crash.
ENTRYPOINT ["/bin/sh", "-c", "if [ -s crac/core.img ]; then exec java -XX:CRaCRestoreFrom=crac; fi; exec java -XX:AOTCache=app.aot -Dspring.aot.enabled=true -jar application.jar"]
