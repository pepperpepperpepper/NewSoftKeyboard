#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

DEVICE_ID="${GENYMOTION_DEV:-${ANDROID_SERIAL:-}}"
if [[ -z "${DEVICE_ID}" ]]; then
  DEVICE_ID="$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')"
fi
if [[ -z "${DEVICE_ID}" ]]; then
  echo "No adb device found; starting a Genymotion SaaS instance..." >&2
  eval "$("${ROOT_DIR}/scripts/gmsaas_start_and_connect.sh")"
  DEVICE_ID="${GMSAAS_DEVICE_SERIAL:-}"
fi
if [[ -z "${DEVICE_ID}" ]]; then
  echo "No adb device found. Set GENYMOTION_DEV or ANDROID_SERIAL, or connect a device."
  exit 1
fi

ADB=(adb -s "${DEVICE_ID}")

OUT_DIR="${1:-outputs/adb/suggestions-strip/$(date +%Y%m%d-%H%M%S)}"
mkdir -p "${OUT_DIR}"

FILTER_REGEX="${ADB_SUGGESTIONS_STRIP_FILTER_REGEX:-Configuring input field:|Input requested TYPE_NULL|Input requested NO_SUGGESTIONS|Input field config result|Suggestion strip:}"

CAPTURE_PID=""
capture_snapshot() {
  echo "Capturing full logcat snapshot..."
  "${ADB[@]}" logcat -d > "${OUT_DIR}/logcat_full.txt" 2>&1 || true
  echo "Done. Review:"
  echo "  ${OUT_DIR}/logcat_filtered.txt"
  echo "  ${OUT_DIR}/logcat_full.txt"
}

cleanup() {
  trap - INT TERM
  if [[ -n "${CAPTURE_PID}" ]]; then
    kill "${CAPTURE_PID}" 2>/dev/null || true
    wait "${CAPTURE_PID}" 2>/dev/null || true
  fi
  capture_snapshot
  exit 0
}

trap cleanup INT TERM

echo "Using device serial: ${DEVICE_ID}"
echo "Artifacts will be written to: ${OUT_DIR}"
echo "Filtering logcat with: ${FILTER_REGEX}"

echo "Clearing logcat..."
"${ADB[@]}" logcat -c || true

cat <<EOF

Repro steps (example: Google Keep):
1) Open the app and focus a note editor where the suggestions strip is missing.
2) Switch between editors/fields if needed.
3) Stop this script with Ctrl-C when done.

EOF

( "${ADB[@]}" logcat -v time | grep --line-buffered -E "${FILTER_REGEX}" | tee "${OUT_DIR}/logcat_filtered.txt" ) &
CAPTURE_PID="$!"
wait "${CAPTURE_PID}" || true
CAPTURE_PID=""
capture_snapshot
