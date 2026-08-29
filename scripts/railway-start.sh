#!/usr/bin/env bash
# Railway start command. Uses the AOT cache produced during the build
# (scripts/railway-build-aot-cache.sh) when present; falls back to a plain
# start otherwise so a training hiccup during build never blocks a deploy.
set -euo pipefail

cd "$(dirname "$0")/.."

WAR=target/ryyppynet.war
AOT_CACHE=target/app.aot

if [ -f "$AOT_CACHE" ]; then
  echo "==> starting with AOT cache ($AOT_CACHE)"
  exec java -XX:AOTCache="$AOT_CACHE" -jar "$WAR"
else
  echo "==> AOT cache not found, starting without it"
  exec java -jar "$WAR"
fi
