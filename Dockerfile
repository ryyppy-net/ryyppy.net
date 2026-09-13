# Standard Spring Boot container image, following
# https://docs.spring.io/spring-boot/reference/packaging/container-images/dockerfiles.html
#
# The reference Dockerfile starts from an already-built jar (its JAR_FILE
# arg). Railway builds straight from the git repo, where target/ does not
# exist, so stage 1 here runs the Maven build that used to be Railpack's
# build command; stages 2 and 3 are the reference's own two stages.
#
# Stage 2 unpacks the repackaged (uber) jar into Spring Boot's layered
# layout; stage 3 copies those layers in one COPY each, so a rebuild that
# only changes application code re-pushes the small application layer
# instead of the ~70MB dependency layer.
#
# Java 25 lets us add the JDK AOT cache (JEP 483/514) on top: a training
# run inside the image records the classes the app loads while its Spring
# context refreshes, and the runtime start command replays that cache
# instead of loading and linking those classes from scratch.

# Build the jar - the same `mvn package` that used to be Railpack's build
# command. Tests are skipped here; CI runs them on every push.
#
# Deliberately no `dependency:go-offline` warm-up layer: it resolves every
# plugin's dependencies across all lifecycle phases, and on this project it
# ran past 20 minutes on Railway's builder without finishing, which is worse
# than the dependency downloads it was meant to save.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -B -DskipTests package

# Perform the extraction in a separate builder container
FROM bellsoft/liberica-openjre-debian:25-cds AS builder
WORKDIR /builder

# Copy the jar built in the previous stage and rename it to application.jar
COPY --from=build /build/target/ryyppynet.jar application.jar

# Extract the jar file using an efficient layout
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# This is the runtime container
FROM bellsoft/liberica-openjre-debian:25-cds
WORKDIR /application

# Copy the extracted jar contents from the builder container into the working
# directory in the runtime container. Every copy step creates a new docker
# layer, which allows docker to only pull the changes it really needs.
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./

# Datasource settings for the AOT cache training run below. Railway injects
# its service variables into the build for any ARG declared in the stage that
# uses them, so on Railway these arrive with no --build-arg needed; locally
# you pass them yourself (see README). Leave them unset and the training run
# simply fails and is skipped.
#
# Caveat: build args consumed by a RUN step are recorded in the built image's
# history, so the database password is readable by anyone who can pull the
# image. Railway keeps images private to the project; treat that as the
# boundary, and rotate the credential rather than assuming the image hides it.
ARG SPRING_DATASOURCE_URL
ARG SPRING_DATASOURCE_USERNAME
ARG SPRING_DATASOURCE_PASSWORD

# Execute the AOT cache training run.
#
# This boots against the real database (the ARGs above) under the `aot-train`
# profile, which disables Flyway so the run cannot write to it - see
# application-aot-train.yml. Training against the real thing is what puts the
# pgjdbc driver, the actual SQL dialect and the pool's code paths into the
# cache.
#
# Note the absence of -Dspring.aot.enabled=true, which the runtime start
# command below does set. Spring AOT freezes @Conditional evaluation at build
# time, so an AOT-processed context creates flywayInitializer regardless of
# the profile's spring.flyway.enabled=false - and this run would then apply
# any pending migration to the real database during the build, before the
# deploy carrying it is accepted. Training without Spring AOT keeps the run
# read-only. Measured cost of that choice: ~0.2s of startup (see README).
#
# Best-effort: if the database is unreachable from the build (a network
# policy, an IP allowlist, an outage), the training run fails, no cache is
# written, and the image still starts - the JVM logs an AOT error for the
# missing cache and boots normally, just without the speed-up. A deploy
# should not be blocked by this.
RUN java -XX:AOTCacheOutput=app.aot \
    -Dspring.profiles.active=aot-train \
    -Dspring.context.exit=onRefresh \
    -jar application.jar \
    || echo "WARNING: AOT cache training failed; starting without a cache"

# Start the application jar with AOT cache enabled - this is not the uber jar
# used by the builder. This jar only contains application code and references
# to the extracted jar files.
ENTRYPOINT ["java", "-XX:AOTCache=app.aot", "-Dspring.aot.enabled=true", "-jar", "application.jar"]
