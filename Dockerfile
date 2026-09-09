# Railway builds this with its Dockerfile builder instead of Railpack,
# because Railpack's Java provider only lets you pick a JDK *version*
# (RAILPACK_JDK_VERSION), not a vendor - and getting CRaC checkpoint/restore
# working needs Azul Zulu's CRaC build specifically. See
# scripts/railway-build.sh and scripts/railway-start.sh for what actually
# happens at build and start time; this file just wires the two stages
# together.
ARG ZULU_CRAC_URL=https://cdn.azul.com/zulu/bin/zulu25.36.205-ca-crac-jdk25.0.4.1-linux_x64.tar.gz

FROM debian:bookworm-slim AS jdk
ARG ZULU_CRAC_URL
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl \
    && curl -fsSL "$ZULU_CRAC_URL" -o /tmp/jdk.tar.gz \
    && mkdir -p /opt/jdk \
    && tar xzf /tmp/jdk.tar.gz -C /opt/jdk --strip-components=1 \
    && rm /tmp/jdk.tar.gz \
    && rm -rf /var/lib/apt/lists/*
ENV JAVA_HOME=/opt/jdk
ENV PATH="$JAVA_HOME/bin:$PATH"

FROM jdk AS build
# maven: builds the app. musl: needed by railway-build.sh's ephemeral
# training Postgres (the io.zonky.test.postgres alpine/musl-linked
# binaries - see that script for why). unzip: that script also unpacks
# the Postgres binaries jar with it. sudo isn't needed; this build stage
# already runs as root.
RUN apt-get update && apt-get install -y --no-install-recommends maven musl unzip \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY . .
RUN bash scripts/railway-build.sh

FROM jdk AS runtime
WORKDIR /app
# Only what scripts/railway-start.sh actually needs at runtime - not the
# full build tree (source, .git, Maven's local repo, the throwaway
# training Postgres binaries).
COPY --from=build /app/target/extracted target/extracted
COPY --from=build /app/target/app.aot target/app.aot
COPY --from=build /app/target/crac-checkpoint target/crac-checkpoint
COPY scripts/railway-start.sh scripts/railway-start.sh
RUN chmod +x scripts/railway-start.sh
EXPOSE 8080
ENTRYPOINT ["scripts/railway-start.sh"]
