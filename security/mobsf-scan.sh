#!/usr/bin/env bash
# Uploads an APK to a running MobSF server and downloads the static + dynamic report.
#
# Usage:  MOBSF_API_KEY=... bash security/mobsf-scan.sh app/build/outputs/apk/debug/app-debug.apk
set -euo pipefail

APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"
SERVER="${MOBSF_SERVER:-http://localhost:8000}"
OUT_DIR="$(dirname "$0")/reports"

if [ -z "${MOBSF_API_KEY:-}" ]; then
  echo "MOBSF_API_KEY is not set. See security/DAST.md." >&2
  exit 1
fi

if [ ! -f "$APK" ]; then
  echo "APK not found: $APK  (run: gradlew :app:assembleDebug)" >&2
  exit 1
fi

mkdir -p "$OUT_DIR"

echo "==> Uploading $APK to $SERVER"
UPLOAD=$(curl -sS -F "file=@${APK}" "${SERVER}/api/v1/upload" -H "Authorization: ${MOBSF_API_KEY}")
HASH=$(printf '%s' "$UPLOAD" | sed -n 's/.*"hash"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')

if [ -z "$HASH" ]; then
  echo "Upload failed: $UPLOAD" >&2
  exit 1
fi

echo "==> Scanning (hash $HASH)"
curl -sS -X POST "${SERVER}/api/v1/scan" \
  -H "Authorization: ${MOBSF_API_KEY}" \
  --data "hash=${HASH}" > "${OUT_DIR}/mobsf-report.json"

echo "==> Generating PDF"
curl -sS -X POST "${SERVER}/api/v1/download_pdf" \
  -H "Authorization: ${MOBSF_API_KEY}" \
  --data "hash=${HASH}" --output "${OUT_DIR}/mobsf-report.pdf"

echo "==> Reports written to ${OUT_DIR}"
