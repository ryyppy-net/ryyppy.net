#!/bin/sh
# Restores the image's CRaC checkpoint, or boots normally when it has none.
#
# Railway starts a fresh container on every wake from serverless sleep, so this
# runs on each wake and not only on deploy.
set -eu

CHECKPOINT=/application/crac

boot() {
    exec java -XX:AOTCache=app.aot -Dspring.aot.enabled=true -jar application.jar
}

[ -s "$CHECKPOINT/core.img" ] || boot

# The restore cannot be exec'd because its exit status decides whether to fall
# back, which leaves this shell as PID 1 - so Railway's SIGTERM at sleep needs
# forwarding by hand, or the JVM is killed without shutting down.
child=''
trap 'if [ -n "$child" ]; then kill -TERM "$child" 2>/dev/null || true; fi' TERM INT

# A restored JVM carries the options it was checkpointed with.
java -XX:CRaCRestoreFrom="$CHECKPOINT" &
child=$!

set +e
wait "$child"
status=$?
if [ "$status" -gt 128 ]; then
    wait "$child" 2>/dev/null
    status=$?
fi
set -e

# 0 is a clean shutdown and >=128 is death by signal; either way the restored
# app ran. Anything else is a checkpoint this host would not take, and a
# single-replica service must not crash-loop on that.
if [ "$status" -eq 0 ] || [ "$status" -ge 128 ]; then
    exit "$status"
fi

echo "WARNING: CRaC restore exited $status; falling back to a normal boot" >&2
boot
