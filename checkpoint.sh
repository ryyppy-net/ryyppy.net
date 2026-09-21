#!/bin/sh
# Writes the CRaC checkpoint that entrypoint.sh restores.
#
# The warp engine needs no Linux capabilities; CRaC's default CRIU engine needs
# CHECKPOINT_RESTORE and SYS_PTRACE, which Railway grants neither builds nor
# containers.
set -eu

CHECKPOINT=/application/crac
LOG=/tmp/checkpoint-boot.log

rm -rf "$CHECKPOINT"

# A checkpoint freezes resolved property values, and production configuration
# lives in application-production.yml, so the profile has to be active here
# rather than only where the container runs.
java -XX:AOTCache=app.aot \
    -XX:CRaCEngine=warp \
    -XX:CRaCCheckpointTo="$CHECKPOINT" \
    -Dspring.aot.enabled=true \
    -Dspring.profiles.active=production \
    -jar application.jar > "$LOG" 2>&1 &
APP=$!

# Checkpointing a half-started context captures a JVM with no Tomcat and a
# connection pool mid-open, so wait for Spring to report the context up.
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
# and a failed boot both exit non-zero. The image on disk is the only result.
if [ ! -s "$CHECKPOINT/core.img" ]; then
    rm -rf "$CHECKPOINT"
    echo "WARNING: no CRaC checkpoint written; the image will boot normally" >&2
    tail -20 "$LOG" >&2 || true
fi
