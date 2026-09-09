#!/usr/bin/env bash
# Docker build stage command (see Dockerfile): package the WAR, then run
# the app twice against a real, ephemeral PostgreSQL instance that this
# script starts itself (see the "training Postgres" section below) - no
# Docker daemon is available inside this build step (nested
# containerization isn't supported here), so `spring.docker.compose.enabled`
# can't be used and the real Postgres server has to be started as a plain
# process.
#
# Pass 1 produces a JDK AOT cache (JEP 483/514): the training run boots
# with -Dspring.aot.enabled=true, so the cache is trained against the same
# AOT-processed bean definitions (generated at build time by the
# spring-boot-maven-plugin's process-aot goal - see pom.xml) that
# production actually runs with. Combining both cuts a *plain* boot's time
# roughly 70% versus neither - this is what the runtime start command
# falls back to if pass 2's CRaC checkpoint is ever missing or unusable.
#
# Pass 2 boots again - this time with the AOT cache from pass 1 already
# available, so it's warmed up faster too - and takes a CRaC checkpoint
# with Azul's Warp engine after the same warmup requests, so the shipped
# checkpoint captures JIT-compiled, already-warmed code paths rather than
# a checkpoint taken the instant the app becomes ready. This is what
# actually gets restored on every container start (see
# scripts/railway-start.sh); the AOT cache from pass 1 is only the
# fallback path. Restore-time secrets (real database credentials, unlike
# the throwaway training database here) are supplied separately at
# container start - see application.yml's spring.config.import and
# CheckpointListener - specifically so nothing baked into this image needs
# to be genuine.
#
# Both are best-effort in the sense that if a training run doesn't reach
# "Started" within its deadline, this script logs a warning and skips that
# artifact rather than failing the build; the runtime start command
# tolerates either being missing. Getting the training Postgres itself up
# is NOT best-effort, though: if it fails to start, this script aborts the
# build (via `set -e` below) rather than silently falling back to a fake
# datasource - a training run against the wrong kind of database would
# happily produce artifacts that never exercised the real JDBC driver,
# dialect, or connection pool code paths, defeating the point.
set -euo pipefail

cd "$(dirname "$0")/.."

echo "==> mvn package"
mvn -B -DskipTests package

WAR=target/ryyppynet.war
EXTRACTED_DIR=target/extracted
EXTRACTED_WAR="$EXTRACTED_DIR/ryyppynet.war"
AOT_CACHE=target/app.aot
TRAIN_LOG=target/aot-train.log
TRAIN_PORT=8080
CRAC_CHECKPOINT_DIR=target/crac-checkpoint

# Both are optional at runtime (scripts/railway-start.sh falls back to a
# plain boot if either is missing/unusable) but the Dockerfile always
# COPYs these exact paths out of this build stage, and `COPY` fails the
# whole build if the source doesn't exist - so guarantee both exist,
# empty if nothing else, regardless of what happens below.
touch "$AOT_CACHE"
mkdir -p "$CRAC_CHECKPOINT_DIR"

if [ ! -f "$WAR" ]; then
  echo "!! $WAR not found after mvn package, aborting" >&2
  exit 1
fi

# Extract the WAR into a thin executable WAR plus a flat lib/ directory
# (see issue #15). The JDK AOT cache is layout-specific, so it has to be
# trained against this extracted layout rather than the nested-jar WAR -
# that's also what the production start command now launches.
echo "==> extracting WAR"
rm -rf "$EXTRACTED_DIR"
java -Djarmode=tools -jar "$WAR" extract --destination "$EXTRACTED_DIR"

if [ ! -f "$EXTRACTED_WAR" ]; then
  echo "!! $EXTRACTED_WAR not found after extraction, aborting" >&2
  exit 1
fi

rm -f "$AOT_CACHE" "$TRAIN_LOG"

# --- AOT training Postgres --------------------------------------------
# Start a real, throwaway PostgreSQL 14 server (matching
# docker/docker-compose.yml's dev version) using actual EnterpriseDB
# server binaries - no Docker involved, just the postgres/initdb
# executables run directly. We use the "-alpine" (musl-linked) flavor of
# io.zonky.test.postgres's binaries rather than the default glibc one:
# the glibc build needs a specific old libicuuc.so.60 that current base
# images don't ship, while the alpine build only needs the `musl` package,
# which is a normal apt install.
PG_VERSION=14.24.0
PG_ROOT="$(pwd)/target/aot-train-postgres"
PG_BIN="$PG_ROOT/bin"
PG_DATA="$PG_ROOT/pgdata"
PG_SOCK="$PG_ROOT/sock"
PG_PORT=5432
PG_DB=ryyppynet
MUSL_LOADER=/usr/lib/ld-musl-x86_64.so.1

echo "==> starting AOT training Postgres $PG_VERSION"
rm -rf "$PG_ROOT"
mkdir -p "$PG_ROOT" "$PG_SOCK"

if [ ! -e "$MUSL_LOADER" ]; then
  if [ "$(id -u)" -ne 0 ]; then
    echo "!! musl runtime missing and not running as root, cannot install it, aborting" >&2
    exit 1
  fi
  echo "==> installing musl (needed to run the alpine-linked Postgres binaries)"
  apt-get update -qq
  apt-get install -y -qq musl
fi
if [ ! -e "$MUSL_LOADER" ]; then
  echo "!! musl runtime still missing after install, aborting" >&2
  exit 1
fi

mvn -q -B dependency:copy \
  -Dartifact=io.zonky.test.postgres:embedded-postgres-binaries-linux-amd64-alpine:"$PG_VERSION" \
  -DoutputDirectory="$PG_ROOT"
PG_BINARIES_JAR="$PG_ROOT/embedded-postgres-binaries-linux-amd64-alpine-$PG_VERSION.jar"
PG_TXZ_MEMBER=$(unzip -l "$PG_BINARIES_JAR" | grep -o '[^ ]*\.txz' | head -1)
unzip -o -q "$PG_BINARIES_JAR" "$PG_TXZ_MEMBER" -d "$PG_ROOT"
tar xJf "$PG_ROOT/$PG_TXZ_MEMBER" -C "$PG_ROOT"

# `postgres`/`initdb` refuse to run as root, and this build step commonly
# does run as root, so run the server as a dedicated unprivileged user.
if [ "$(id -u)" -eq 0 ]; then
  PG_OS_USER=aottrainpg
  id -u "$PG_OS_USER" >/dev/null 2>&1 || useradd --system --no-create-home --shell /usr/sbin/nologin "$PG_OS_USER"
  chown -R "$PG_OS_USER" "$PG_ROOT"
  RUN_AS_ROOT=1
else
  PG_OS_USER="$(id -un)"
  RUN_AS_ROOT=0
fi

run_as() {
  if [ "$RUN_AS_ROOT" = "1" ]; then
    su "$PG_OS_USER" -s /bin/bash -c "$*"
  else
    bash -c "$*"
  fi
}

cleanup_training_postgres() {
  if [ -n "${PG_PID:-}" ] && kill -0 "$PG_PID" 2>/dev/null; then
    run_as "$MUSL_LOADER $PG_BIN/pg_ctl -D $PG_DATA stop -m fast" >/dev/null 2>&1 || kill "$PG_PID" 2>/dev/null || true
    wait "$PG_PID" 2>/dev/null || true
  fi
  rm -rf "$PG_ROOT"
}
trap cleanup_training_postgres EXIT

echo ryyppynet > "$PG_ROOT/pwfile"
run_as "$MUSL_LOADER $PG_BIN/initdb -D $PG_DATA -U $PG_DB --pwfile=$PG_ROOT/pwfile -A md5"

# The app's database ("ryyppynet") doesn't exist yet - initdb only creates
# the bootstrap superuser above plus the template databases. Create it via
# single-user mode since this stripped-down binary bundle has no psql/
# createdb client tools.
echo "CREATE DATABASE $PG_DB;" > "$PG_ROOT/createdb.sql"
run_as "$MUSL_LOADER $PG_BIN/postgres --single -D $PG_DATA template1 < $PG_ROOT/createdb.sql" > "$PG_ROOT/createdb.log" 2>&1

run_as "exec $MUSL_LOADER $PG_BIN/postgres -D $PG_DATA -p $PG_PORT -k $PG_SOCK -c listen_addresses=127.0.0.1" \
  > "$PG_ROOT/pg.log" 2>&1 &
PG_PID=$!

READY=""
DEADLINE=$((SECONDS + 15))
while [ $SECONDS -lt $DEADLINE ]; do
  if timeout 1 bash -c ": < /dev/tcp/127.0.0.1/$PG_PORT" 2>/dev/null; then
    READY=1
    break
  fi
  if ! kill -0 "$PG_PID" 2>/dev/null; then
    break
  fi
  sleep 0.3
done
if [ -z "$READY" ]; then
  echo "!! AOT training Postgres did not become ready, aborting" >&2
  echo "!! server log follows:" >&2
  cat "$PG_ROOT/pg.log" >&2 || true
  exit 1
fi
echo "==> AOT training Postgres ready on 127.0.0.1:$PG_PORT"
# ------------------------------------------------------------------------

echo "==> AOT training run"
set +e
# Railway sets SPRING_DATASOURCE_URL/USERNAME/PASSWORD for the real Postgres
# on this service, and env vars outrank application-aot-train.yml's
# spring.datasource.*, so left alone they'd point this boot at the
# production JDBC URL instead of the training Postgres above. Unset them
# for this subprocess so the profile's own datasource config applies.
env -u SPRING_DATASOURCE_URL -u SPRING_DATASOURCE_USERNAME -u SPRING_DATASOURCE_PASSWORD \
  AOT_TRAIN_PG_PORT="$PG_PORT" \
  java -Dspring.aot.enabled=true -XX:AOTCacheOutput="$AOT_CACHE" \
  -jar "$EXTRACTED_WAR" \
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
fi

# Warm representative request paths so the produced artifacts cover more
# than just the bare boot path. The first three are public; the rest
# register a throwaway user (POST /ui/addUser auto-authenticates, saving
# the session into the cookie jar) and walk it through the app's actual
# hot paths - profile/party JSON DTOs and, most importantly, logging a
# drink so AlcoholCalculator's promille calculation actually runs. Takes a
# distinct email/cookie-jar suffix per call since both training passes
# share the same training Postgres - reusing one email would 409 on the
# second pass's registration and silently skip its authenticated paths.
warmup() {
  local base="$1" suffix="$2"
  local cookie_jar="$PG_ROOT.cookies-$suffix"
  curl -fsS "$base/actuator/health" -o /dev/null || true
  curl -fsS "$base/ui/login" -o /dev/null || true
  curl -fsS -L "$base/" -o /dev/null || true
  curl -fsS -c "$cookie_jar" -o /dev/null \
    --data-urlencode "name=Warmup User" \
    --data-urlencode "sex=MALE" \
    --data-urlencode "weight=80" \
    --data-urlencode "email=warmup-$suffix@example.com" \
    --data-urlencode "password=warmup-password" \
    "$base/ui/addUser" || true
  curl -fsS -b "$cookie_jar" -L "$base/" -o /dev/null || true
  curl -fsS -b "$cookie_jar" "$base/API/v2/profile" -o /dev/null || true
  curl -fsS -b "$cookie_jar" -X POST "$base/API/v2/profile/drinks" -o /dev/null || true
  curl -fsS -b "$cookie_jar" "$base/API/v2/profile" -o /dev/null || true
  curl -fsS -b "$cookie_jar" -X POST --data-urlencode "name=Warmup Party" "$base/API/v2/parties" -o /dev/null || true
  curl -fsS -b "$cookie_jar" "$base/API/v2/parties" -o /dev/null || true
  rm -f "$cookie_jar"
}

BASE="http://localhost:${TRAIN_PORT}"
if [ -n "$STARTED" ]; then
  sleep 1
  warmup "$BASE" "aot"
  sleep 1

  kill "$TRAIN_PID" 2>/dev/null || true
  wait "$TRAIN_PID" 2>/dev/null || true

  if [ -f "$AOT_CACHE" ]; then
    echo "==> AOT cache created: $AOT_CACHE ($(du -h "$AOT_CACHE" | cut -f1))"
  else
    echo "!! AOT cache was not produced, deploy will fall back to a plain start" >&2
  fi
fi

# --- Pass 2: CRaC checkpoint --------------------------------------------
# Same training Postgres, same extracted WAR, this time with the AOT cache
# from pass 1 (if it exists) and a CRaC checkpoint destination. jcmd
# JDK.checkpoint (rather than -Dspring.context.checkpoint=onRefresh) is
# what lets the warmup above happen *before* the checkpoint is taken -
# checkpointing on refresh happens too early in the bean lifecycle for
# HikariCheckpointRestoreLifecycle to run at all (it needs Spring's
# DefaultLifecycleProcessor to already be running), and even setting that
# aside, a checkpoint taken before any warmup traffic captures cold,
# un-JIT-compiled code paths - the whole point of training here is to
# avoid paying that cost on every container start.
CRAC_TRAIN_LOG=target/crac-train.log
rm -rf "$CRAC_CHECKPOINT_DIR" "$CRAC_TRAIN_LOG"
mkdir -p "$CRAC_CHECKPOINT_DIR"

echo "==> CRaC checkpoint training run"
AOT_CACHE_OPTS=()
if [ -f "$AOT_CACHE" ]; then
  AOT_CACHE_OPTS=(-Dspring.aot.enabled=true "-XX:AOTCache=$AOT_CACHE")
fi

set +e
env -u SPRING_DATASOURCE_URL -u SPRING_DATASOURCE_USERNAME -u SPRING_DATASOURCE_PASSWORD \
  AOT_TRAIN_PG_PORT="$PG_PORT" \
  java "${AOT_CACHE_OPTS[@]}" -XX:CRaCCheckpointTo="$CRAC_CHECKPOINT_DIR" \
  -jar "$EXTRACTED_WAR" \
  --spring.profiles.active=aot-train \
  --server.port="$TRAIN_PORT" \
  > "$CRAC_TRAIN_LOG" 2>&1 &
CRAC_TRAIN_PID=$!
set -e

STARTED=""
DEADLINE=$((SECONDS + 60))
while [ $SECONDS -lt $DEADLINE ]; do
  if grep -q "Started RyyppyApplication" "$CRAC_TRAIN_LOG" 2>/dev/null; then
    STARTED=1
    break
  fi
  if ! kill -0 "$CRAC_TRAIN_PID" 2>/dev/null; then
    break
  fi
  sleep 0.2
done

if [ -n "$STARTED" ]; then
  sleep 1
  warmup "$BASE" "crac"
  sleep 1

  echo "==> taking CRaC checkpoint"
  jcmd "$CRAC_TRAIN_PID" JDK.checkpoint > "$PG_ROOT.checkpoint.log" 2>&1 || true
  # jcmd JDK.checkpoint ends the target process on success (that's how
  # CRaC checkpointing works - it isn't a live snapshot-and-continue), so
  # this wait just reaps it; a checkpoint that failed instead leaves the
  # process running, in which case kill it so the build doesn't hang.
  wait "$CRAC_TRAIN_PID" 2>/dev/null || true
  kill "$CRAC_TRAIN_PID" 2>/dev/null || true

  if [ -f "$CRAC_CHECKPOINT_DIR/core.img" ]; then
    echo "==> CRaC checkpoint created: $CRAC_CHECKPOINT_DIR ($(du -sh "$CRAC_CHECKPOINT_DIR" | cut -f1))"
  else
    echo "!! CRaC checkpoint was not produced, deploy will fall back to a plain start" >&2
    echo "!! checkpoint log follows:" >&2
    cat "$PG_ROOT.checkpoint.log" >&2 || true
    cat "$CRAC_TRAIN_LOG" >&2 || true
    rm -rf "$CRAC_CHECKPOINT_DIR"
    mkdir -p "$CRAC_CHECKPOINT_DIR"
  fi
else
  echo "!! CRaC training run did not start within 60s, skipping checkpoint" >&2
  echo "!! full training log follows:" >&2
  cat "$CRAC_TRAIN_LOG" >&2 || true
  kill "$CRAC_TRAIN_PID" 2>/dev/null || true
  wait "$CRAC_TRAIN_PID" 2>/dev/null || true
fi

exit 0
