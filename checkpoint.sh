#!/bin/sh
# Writes the CRaC checkpoint the entry point restores.
#
# Railway grants no Linux capabilities to builds or containers, so the
# checkpoint is taken by warp rather than CRaC's default CRIU engine.
set -eu

CHECKPOINT=/application/crac
LOG=/tmp/checkpoint-boot.log

rm -rf "$CHECKPOINT"

# Railway builds and runs on separate hosts, so the JVM is held to the x86-64
# baseline every host has. A checkpoint freezes resolved property values, and
# production configuration lives in application-production.yml. The
# OpenTelemetry agent attaches here and its exporter settings are frozen too.
java -XX:CRaCEngine=warp \
    -javaagent:opentelemetry-javaagent.jar \
    -XX:CPUFeatures=generic \
    -XX:CRaCCheckpointTo="$CHECKPOINT" \
    -Dspring.aot.enabled=true \
    -Dspring.profiles.active=production \
    -jar application.jar > "$LOG" 2>&1 &
APP=$!

# A half-started context checkpoints a JVM with no Tomcat and a connection pool
# mid-open.
i=0
while [ "$i" -lt 180 ]; do
    if grep -q "Started RyyppyApplication" "$LOG" 2>/dev/null; then
        jcmd application.jar JDK.checkpoint || true
        break
    fi
    kill -0 "$APP" 2>/dev/null || break
    i=$((i + 1))
    sleep 1
done

wait "$APP" 2>/dev/null || true

# warp SIGKILLs the JVM once the image is written, so a successful checkpoint
# and a failed boot both exit non-zero; the image on disk is the only result.
if [ -s "$CHECKPOINT/core.img" ]; then
    echo "CRaC checkpoint written ($(du -sh "$CHECKPOINT" | cut -f1))"
    # The agent's configuration is frozen into the checkpoint, so show what it read.
    grep -E 'otel\.javaagent.*(INFO|WARN|ERROR)|OpenTelemetry set to|xport|Exception|ndpoint' "$LOG" | head -200 || true
else
    rm -rf "$CHECKPOINT"
    echo "WARNING: no CRaC checkpoint written; the image will boot normally" >&2
    tail -20 "$LOG" >&2 || true
fi
