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
#
# spring.security.oauth2.client.registration.google.* and app.* below are
# the same fix applied to the other config that was silently stuck at
# whatever it looked like during training (google client-id/secret frozen
# at REPLACE_THIS, AUTH_RELAY_SECRET/GOOGLE_AUTH_HUB_URL frozen unset) -
# see application.yml's spring.cloud.refresh.extra-refreshable (for the
# OAuth2 client registration beans) and app.auth-relay-secret/
# app.google-auth-hub-url (read fresh per-call via Environment instead of
# System.getenv() - see AuthRelayTokenService, AuthRelayController,
# GlobalControllerAdvice).
#
# spring.datasource.hikari.* below are the same fix again, for a bug that
# was actually blocking Railway's sleep feature entirely: unset during
# training, these silently stayed unset after every restore too, so
# HikariCP fell back to its own default of minimum-idle == maximum-pool-size
# (10) - a combination where HikariCP documents that idle-timeout eviction
# never kicks in. The restored pool held up to 10 connections open to the
# real database permanently, which Railway's inactivity detector counts as
# outbound traffic, so the service could never be considered idle long
# enough to sleep. HikariDataSource is already in extra-refreshable (see
# application.yml), so wiring these through here is enough - no bean
# changes needed, unlike the OAuth2 registration repository.
HIKARI_YAML=""
if [ -n "${SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE:-}" ]; then
  HIKARI_YAML="${HIKARI_YAML}
      minimum-idle: ${SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE}"
fi
if [ -n "${SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE:-}" ]; then
  HIKARI_YAML="${HIKARI_YAML}
      maximum-pool-size: ${SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE}"
fi
if [ -n "${SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT:-}" ]; then
  HIKARI_YAML="${HIKARI_YAML}
      idle-timeout: ${SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT}"
fi
if [ -n "$HIKARI_YAML" ]; then
  HIKARI_YAML="
    hikari:${HIKARI_YAML}"
fi

cat > crac-runtime-config.yml <<EOF
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}${HIKARI_YAML}
  security:
    oauth2:
      client:
        registration:
          google:
            # :-REPLACE_THIS, not :- : an empty client-id fails
            # ClientRegistration.build()'s validation and would crash
            # bean construction on refresh, whereas the same placeholder
            # application.yml already falls back to is a harmless no-op.
            client-id: ${GOOGLE_CLIENT_ID:-REPLACE_THIS}
            client-secret: ${GOOGLE_CLIENT_SECRET:-REPLACE_THIS}
app:
  # Both optional (see AuthRelayTokenService/AuthRelayController) - guarded with :- so an
  # unset variable doesn't trip set -u, unlike the required SPRING_DATASOURCE_* above.
  auth-relay-secret: ${AUTH_RELAY_SECRET:-}
  google-auth-hub-url: ${GOOGLE_AUTH_HUB_URL:-}
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
