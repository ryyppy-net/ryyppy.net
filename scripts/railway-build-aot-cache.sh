#!/usr/bin/env bash
# Railway build command: package the WAR, then do a short training run to
# produce a JDK AOT cache (JEP 483/514) so the production start command
# can boot ~30% faster. The training run boots against an in-memory HSQLDB
# (see application-aot-train.yml) so it needs no external services during
# the build.
#
# The AOT cache is best-effort: if training fails for any reason, this
# script logs a warning and still exits 0 so the deploy proceeds without
# it. The deploy command (railpack.json's JAVA_OPTS) always passes
# -XX:AOTCache=target/app.aot; if the file is missing or invalid the JVM
# just logs a warning and boots normally, so no fallback logic is needed
# at start time.
set -euo pipefail

cd "$(dirname "$0")/.."

echo "==> mvn package"
mvn -B -DskipTests package

WAR=target/ryyppynet.war
AOT_CACHE=target/app.aot
TRAIN_LOG=target/aot-train.log
TRAIN_PORT=8080

if [ ! -f "$WAR" ]; then
  echo "!! $WAR not found after mvn package, aborting" >&2
  exit 1
fi

rm -f "$AOT_CACHE" "$TRAIN_LOG"

echo "==> AOT training run"
set +e
java -XX:AOTCacheOutput="$AOT_CACHE" \
  -jar "$WAR" \
  --spring.profiles.active=aot-train \
  --server.port="$TRAIN_PORT" \
  > "$TRAIN_LOG" 2>&1 &
TRAIN_PID=$!
set -e

STARTED=""
DEADLINE=$((SECONDS + 60))
while [ $SECONDS -lt $DEADLINE ]; do
  if grep -q "Started RyyppyApplication" "$TRAIN_LOG" 2>/dev/null; then
    STARTED=1
    break
  fi
  if ! kill -0 "$TRAIN_PID" 2>/dev/null; then
    break
  fi
  sleep 0.2
done

if [ -z "$STARTED" ]; then
  echo "!! AOT training run did not start within 60s, skipping AOT cache" >&2
  echo "!! full training log follows:" >&2
  cat "$TRAIN_LOG" >&2 || true
  kill "$TRAIN_PID" 2>/dev/null || true
  wait "$TRAIN_PID" 2>/dev/null || true
  exit 0
fi

# Warm a couple of representative request paths (public, no auth needed)
# so the cache covers MVC/JSP rendering and the DB health check, not just
# the bare boot path.
sleep 1
curl -fsS "http://localhost:${TRAIN_PORT}/actuator/health" -o /dev/null || true
curl -fsS "http://localhost:${TRAIN_PORT}/ui/login" -o /dev/null || true
curl -fsS "http://localhost:${TRAIN_PORT}/" -o /dev/null || true
sleep 1

kill "$TRAIN_PID" 2>/dev/null || true
wait "$TRAIN_PID" 2>/dev/null || true

if [ -f "$AOT_CACHE" ]; then
  echo "==> AOT cache created: $AOT_CACHE ($(du -h "$AOT_CACHE" | cut -f1))"
else
  echo "!! AOT cache was not produced, deploy will fall back to a plain start" >&2
fi

exit 0
