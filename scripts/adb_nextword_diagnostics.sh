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

OUT_DIR="${1:-outputs/adb/nextword/$(date +%Y%m%d-%H%M%S)}"
mkdir -p "${OUT_DIR}"

FILTER_REGEX="${ADB_NEXTWORD_FILTER_REGEX:-Next-word request \\(|getNextSuggestions sources for|Invoking neural next-word with context|extractTopWords produced|filterCandidatesForKeyboardUx}"

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

Repro steps (example):
1) Enable next-word suggestions and set engine mode to Neural.
   Optional: enable extra neural diagnostics logs:
     adb shell setprop NSK_TEST_LOGS true
     # restart the IME/app after changing the property
2) In the target app, type: "keep me informed" (or your failing phrase).
3) If suggestions go empty after a pick/space, reproduce that flow too.
4) Stop this script with Ctrl-C when done.

EOF

( "${ADB[@]}" logcat -v time | grep --line-buffered -E "${FILTER_REGEX}" | tee "${OUT_DIR}/logcat_filtered.txt" ) &
CAPTURE_PID="$!"
wait "${CAPTURE_PID}" || true
CAPTURE_PID=""
capture_snapshot
