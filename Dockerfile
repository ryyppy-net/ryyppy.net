# Standard Spring Boot container image, following
# https://docs.spring.io/spring-boot/reference/packaging/container-images/dockerfiles.html
#
# Stage 1 unpacks the repackaged (uber) jar into Spring Boot's layered
# layout; stage 2 copies those layers in one COPY each, so a rebuild that
# only changes application code re-pushes the small application layer
# instead of the ~70MB dependency layer.
#
# Java 25 lets us add the JDK AOT cache (JEP 483/514) on top: a training
# run inside the image records the classes the app loads while its Spring
# context refreshes, and the runtime start command replays that cache
# instead of loading and linking those classes from scratch.

# Perform the extraction in a separate builder container
FROM bellsoft/liberica-openjre-debian:25-cds AS builder
WORKDIR /builder

# This points to the built jar file in the target folder
ARG JAR_FILE=target/*.jar

# Copy the jar file to the working directory and rename it to application.jar
COPY ${JAR_FILE} application.jar

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

# Execute the AOT cache training run.
#
# The `aot-train` profile points this boot at an in-memory HSQLDB rather
# than Postgres: a `docker build` has no service containers, so no real
# Postgres is reachable here. See application-aot-train.yml.
#
# Note the absence of -Dspring.aot.enabled=true, which the runtime start
# command below does set. Spring AOT resolves @Conditional evaluation at
# build time, so an AOT-processed context instantiates flywayInitializer
# regardless of the profile's spring.flyway.enabled=false - and Flyway 13
# rejects HSQLDB outright ("Unsupported Database"), failing the run. Doing
# the training boot without Spring AOT keeps the JDK cache trainable with
# no database server in the build; the cache still covers the bulk of the
# classes the AOT-processed boot loads. Measured cost vs. training the two
# together: roughly +0.4s of startup (see README).
RUN java -XX:AOTCacheOutput=app.aot \
    -Dspring.profiles.active=aot-train \
    -Dspring.context.exit=onRefresh \
    -jar application.jar

# Start the application jar with AOT cache enabled - this is not the uber jar
# used by the builder. This jar only contains application code and references
# to the extracted jar files.
ENTRYPOINT ["java", "-XX:AOTCache=app.aot", "-Dspring.aot.enabled=true", "-jar", "application.jar"]
