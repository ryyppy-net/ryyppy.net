#!/usr/bin/env bash
# Container entrypoint. Restores from the CRaC checkpoint baked into the
# image by scripts/railway-build.sh if one is present and usable, falling
# back to a plain boot (still benefiting from the JDK AOT cache from the
# same build) otherwise - e.g. the very first deploy of a change to this
# script itself, or if the checkpoint ever fails to produce a working
# restore for some other reason.
#
# Runs on *every* container start: the first one after a deploy, and every
# subsequent Railway "sleep" wake-up, since Railway's sleep is a real stop
# (SIGTERM) followed by a fresh container start on the next request, not a
# pause - see docs.railway.com/reference/app-sleeping.
set -euo pipefail

cd "$(dirname "$0")/.."

# Real secrets for *this* container, written fresh on every start. Restore
# reuses the checkpoint's frozen JVM state - including a frozen
# System.getenv() snapshot from whenever the checkpoint was taken - so
# ${ENV_VAR} placeholders resolved from the environment would silently
# keep whatever credentials were live during the build's training run.
# Spring Cloud's context refresh (see application.yml's
# spring.config.import and CheckpointListener) re-imports this file fresh
# on every restore, which is what actually makes the swap work - verified
# via pg_stat_activity, not just log output.
cat > crac-runtime-config.yml <<EOF
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
EOF

CHECKPOINT_DIR=target/crac-checkpoint

# The listening port is fixed at whatever the build's training run bound
# to (8080 - see scripts/railway-build.sh's TRAIN_PORT) and baked into the
# checkpoint; restore can't rebind it to a different $PORT the way a plain
# boot could, since Tomcat's listener is part of the restored JVM state,
# not something re-read from the environment. The service's networking
# config expects 8080 for exactly this reason, so the fallback boot below
# is pinned the same way rather than trusting Railway's injected $PORT to
# match - keeping both paths consistent instead of only the fallback one
# honoring it.
if [ -f "$CHECKPOINT_DIR/core.img" ]; then
  echo "==> restoring from CRaC checkpoint"
  exec java -XX:CRaCRestoreFrom="$CHECKPOINT_DIR"
fi

echo "!! no usable CRaC checkpoint at $CHECKPOINT_DIR, falling back to a plain boot" >&2
exec java -Dspring.aot.enabled=true -XX:AOTCache=target/app.aot \
  -Dserver.port=8080 \
  -jar target/extracted/ryyppynet.war
