#!/usr/bin/env bash
# Sube un APK a un servidor MobSF y descarga el reporte estático y dinámico.
#
# Uso:  MOBSF_API_KEY=... bash security/mobsf-scan.sh app/build/outputs/apk/debug/app-debug.apk
set -euo pipefail

APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
SERVER="${MOBSF_SERVER:-http://localhost:8000}"
OUT_DIR="$(dirname "$0")/reports"

if [ -z "${MOBSF_API_KEY:-}" ]; then
  echo "Falta MOBSF_API_KEY. Ver security/DAST.md." >&2
  exit 1
fi

if [ ! -f "$APK" ]; then
  echo "No se encontró el APK: $APK  (correr: gradlew :app:assembleDebug)" >&2
  exit 1
fi

mkdir -p "$OUT_DIR"

echo "==> Subiendo $APK a $SERVER"
UPLOAD=$(curl -sS -F "file=@${APK}" "${SERVER}/api/v1/upload" -H "Authorization: ${MOBSF_API_KEY}")
HASH=$(printf '%s' "$UPLOAD" | sed -n 's/.*"hash"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')

if [ -z "$HASH" ]; then
  echo "Falló la subida: $UPLOAD" >&2
  exit 1
fi

echo "==> Escaneando (hash $HASH)"
curl -sS -X POST "${SERVER}/api/v1/scan" \
  -H "Authorization: ${MOBSF_API_KEY}" \
  --data "hash=${HASH}" > "${OUT_DIR}/mobsf-report.json"

echo "==> Generando PDF"
curl -sS -X POST "${SERVER}/api/v1/download_pdf" \
  -H "Authorization: ${MOBSF_API_KEY}" \
  --data "hash=${HASH}" --output "${OUT_DIR}/mobsf-report.pdf"

echo "==> Reportes guardados en ${OUT_DIR}"
