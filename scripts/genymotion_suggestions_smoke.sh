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

OUT_DIR="outputs/genymotion/suggestions-smoke/$(date +%Y%m%d-%H%M%S)"
mkdir -p "${OUT_DIR}"

echo "Using device serial: ${DEVICE_ID}"
echo "Artifacts will be written to: ${OUT_DIR}"

echo "Clearing logcat..."
"${ADB[@]}" logcat -c || true

disable_animations() {
  echo "Disabling device animations (UI stability)..."
  local ok="true"
  for key in window_animation_scale transition_animation_scale animator_duration_scale; do
    if ! "${ADB[@]}" shell settings put global "${key}" 0 >/dev/null 2>&1; then
      ok="false"
    fi
  done

  local window_scale
  local transition_scale
  local animator_scale
  window_scale="$("${ADB[@]}" shell settings get global window_animation_scale 2>/dev/null | tr -d '\r' || true)"
  transition_scale="$("${ADB[@]}" shell settings get global transition_animation_scale 2>/dev/null | tr -d '\r' || true)"
  animator_scale="$("${ADB[@]}" shell settings get global animator_duration_scale 2>/dev/null | tr -d '\r' || true)"

  echo "Animation scales: window=${window_scale:-?} transition=${transition_scale:-?} animator=${animator_scale:-?}"

  if [[ "${ok}" != "true" ]] || [[ "${window_scale}" != "0" && "${window_scale}" != "0.0" ]]; then
    echo "WARNING: Could not fully disable animations via adb. If tests fail with 'Animations or transitions are enabled', disable them in Developer options."
  fi
}

disable_animations

echo "Building APKs (no Gradle build task)..."
TEST_BUILD_TYPE_VALUE="${TEST_BUILD_TYPE:-debug}"
if [[ "${TEST_BUILD_TYPE_VALUE}" != "debug" ]]; then
  echo "Unsupported TEST_BUILD_TYPE='${TEST_BUILD_TYPE_VALUE}'. Expected 'debug' (this smoke test installs debug-only harness activities)."
  exit 1
fi
if [[ -z "${TEST_BUILD_TYPE:-}" ]]; then
  echo "TEST_BUILD_TYPE not set; defaulting to '${TEST_BUILD_TYPE_VALUE}' for Genymotion suggestions smoke."
fi

TEST_BUILD_TYPE="${TEST_BUILD_TYPE_VALUE}" ./gradlew :ime:app:assembleNskDebug :ime:app:assembleAndroidTest

APP_APK="ime/app/build/outputs/apk/nsk/debug/app-nsk-debug.apk"
TEST_APK="ime/app/build/outputs/apk/androidTest/nsk/debug/app-nsk-debug-androidTest.apk"
if [[ ! -f "${APP_APK}" ]]; then
  echo "Missing app APK at ${APP_APK}"
  exit 1
fi
if [[ ! -f "${TEST_APK}" ]]; then
  echo "Missing androidTest APK at ${TEST_APK}"
  exit 1
fi

echo "Installing APKs..."
"${ADB[@]}" uninstall wtf.uhoh.newsoftkeyboard > "${OUT_DIR}/uninstall_app.txt" 2>&1 || true
"${ADB[@]}" uninstall wtf.uhoh.newsoftkeyboard.test > "${OUT_DIR}/uninstall_test.txt" 2>&1 || true
"${ADB[@]}" install -r -d "${APP_APK}" > "${OUT_DIR}/install_app.txt"
"${ADB[@]}" install -r -d "${TEST_APK}" > "${OUT_DIR}/install_test.txt"

TEST_RUNNER="wtf.uhoh.newsoftkeyboard.test/androidx.test.runner.AndroidJUnitRunner"
TEST_CLASS="wtf.uhoh.newsoftkeyboard.app.ime.SuggestionsStripNoSuggestionsFlagInstrumentedTest"

echo "Running suggestions-strip smoke..."
set +e
"${ADB[@]}" shell am instrument -w -r -e class "${TEST_CLASS}" "${TEST_RUNNER}" \
  | tee "${OUT_DIR}/instrumentation.txt"
TEST_EXIT="${PIPESTATUS[0]}"
set -e

echo "Capturing logcat..."
"${ADB[@]}" logcat -d > "${OUT_DIR}/logcat.txt" || true

if [[ "${TEST_EXIT}" -ne 0 ]]; then
  echo "Smoke FAILED (adb exit ${TEST_EXIT}). See ${OUT_DIR}/instrumentation.txt and ${OUT_DIR}/logcat.txt"
  exit "${TEST_EXIT}"
fi

if grep -q "FAILURES!!!" "${OUT_DIR}/instrumentation.txt"; then
  echo "Smoke FAILED (instrumentation reported failures). See ${OUT_DIR}/instrumentation.txt and ${OUT_DIR}/logcat.txt"
  exit 1
fi

if ! grep -q "OK (" "${OUT_DIR}/instrumentation.txt"; then
  echo "Smoke FAILED (missing success code). See ${OUT_DIR}/instrumentation.txt and ${OUT_DIR}/logcat.txt"
  exit 1
fi

echo "Smoke PASSED."
