#!/bin/bash
# Gets a Claude Code cloud session ready for the e2e suite: the app's Postgres
# needs a Docker daemon, and the container's Chromium is not the build our
# pinned Playwright expects.
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

cd "${CLAUDE_PROJECT_DIR:-.}"

# The cloud container has Docker installed but does not start the daemon.
if ! docker info >/dev/null 2>&1 && command -v dockerd >/dev/null; then
  if ! pgrep -x dockerd >/dev/null; then
    # /var/run survives a container restore, so its pid files can name a process
    # that is now something else, and dockerd then waits on it and exits.
    if ! pgrep -x containerd >/dev/null; then
      rm -f /var/run/docker.pid /var/run/docker/containerd/containerd.pid
    fi
    nohup dockerd >/tmp/dockerd.log 2>&1 &
  fi
  for _ in $(seq 1 30); do
    docker info >/dev/null 2>&1 && break
    sleep 1
  done
fi

hashes=e2e/node_modules/.session-start.sha256
if ! sha256sum --check --status "$hashes" 2>/dev/null; then
  rm -f "$hashes"
  (cd e2e && npm install --no-audit --no-fund)
  sha256sum .claude/hooks/session-start.sh e2e/package.json e2e/package-lock.json > "$hashes"
fi

chromium=$(ls /opt/pw-browsers/chromium-*/chrome-linux64/chrome 2>/dev/null | head -1 || true)
if [ -n "$chromium" ] && [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  echo "export PLAYWRIGHT_CHROMIUM_EXECUTABLE=$chromium" >> "$CLAUDE_ENV_FILE"
fi

if docker info >/dev/null 2>&1; then
  echo "Docker daemon is running; the e2e suite runs with: cd e2e && npm test"
else
  echo "Docker daemon did not start (see /tmp/dockerd.log); mvn spring-boot:run and the e2e suite need it."
fi
