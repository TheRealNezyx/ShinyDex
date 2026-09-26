#!/usr/bin/env bash
# Corre todo el SAST (detekt + Android Lint).
#
# detekt corre con la tarea `detektSast`, que usa su propio proceso con JDK 21, así que
# funciona sin importar con qué JVM corra el daemon de Gradle.
set -euo pipefail

cd "$(dirname "$0")/.."

./gradlew :app:detektSast :app:lintDebug "$@"

echo
echo "==> detekt: app/build/reports/detekt/"
echo "==> lint:   app/build/reports/lint-results-debug.html"
