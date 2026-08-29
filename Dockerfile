# Single-stage on purpose: the AOT cache (see scripts/railway-build-aot-cache.sh)
# is only valid when trained by the exact same JDK build that runs it later.
# Building and running in the same image guarantees that, at the cost of a
# larger final image (full Maven + JDK instead of a slim JRE layer).
FROM maven:3.9-eclipse-temurin-25

WORKDIR /app

COPY pom.xml .
COPY src ./src
COPY scripts ./scripts

RUN bash scripts/railway-build-aot-cache.sh

CMD ["bash", "scripts/railway-start.sh"]
