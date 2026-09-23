#!/usr/bin/env bash
# Runs the full SAST suite (detekt + Android Lint).
#
# detekt is executed by the `detektSast` task, which forks its own JDK 21 process, so this
# works no matter which JVM the Gradle daemon is on.
set -euo pipefail

cd "$(dirname "$0")/.."

./gradlew :app:detektSast :app:lintDebug "$@"

echo
echo "==> detekt: app/build/reports/detekt/"
echo "==> lint:   app/build/reports/lint-results-debug.html"
