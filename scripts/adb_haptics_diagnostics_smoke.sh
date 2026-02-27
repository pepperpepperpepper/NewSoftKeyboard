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

OUT_DIR="${1:-outputs/adb/haptics-diagnostics/$(date +%Y%m%d-%H%M%S)}"
mkdir -p "${OUT_DIR}"

echo "Using device serial: ${DEVICE_ID}"
echo "Artifacts will be written to: ${OUT_DIR}"

echo "Clearing logcat..."
"${ADB[@]}" logcat -c || true

echo "Capturing device state (before)..."
"${ADB[@]}" shell getprop > "${OUT_DIR}/getprop.txt" 2>&1 || true
"${ADB[@]}" shell settings get system haptic_feedback_enabled > "${OUT_DIR}/settings_system_haptic_feedback_enabled.txt" 2>&1 || true
"${ADB[@]}" shell settings get secure haptic_feedback_enabled > "${OUT_DIR}/settings_secure_haptic_feedback_enabled.txt" 2>&1 || true
"${ADB[@]}" shell settings get system haptic_feedback_intensity > "${OUT_DIR}/settings_system_haptic_feedback_intensity.txt" 2>&1 || true
"${ADB[@]}" shell settings get secure haptic_feedback_intensity > "${OUT_DIR}/settings_secure_haptic_feedback_intensity.txt" 2>&1 || true
"${ADB[@]}" shell settings get system keyboard_vibration_enabled > "${OUT_DIR}/settings_system_keyboard_vibration_enabled.txt" 2>&1 || true
"${ADB[@]}" shell settings get secure keyboard_vibration_enabled > "${OUT_DIR}/settings_secure_keyboard_vibration_enabled.txt" 2>&1 || true
"${ADB[@]}" shell dumpsys vibrator > "${OUT_DIR}/dumpsys_vibrator_before.txt" 2>&1 || true
"${ADB[@]}" shell dumpsys vibrator_manager > "${OUT_DIR}/dumpsys_vibrator_manager_before.txt" 2>&1 || true

TEST_BUILD_TYPE_VALUE="${TEST_BUILD_TYPE:-debug}"
if [[ "${TEST_BUILD_TYPE_VALUE}" != "debug" ]]; then
  echo "This smoke test only supports TEST_BUILD_TYPE=debug (got '${TEST_BUILD_TYPE_VALUE}')."
  exit 1
fi

if [[ "${SKIP_BUILD:-0}" != "1" ]]; then
  echo "Building APKs (no Gradle build task)..."
  TEST_BUILD_TYPE="${TEST_BUILD_TYPE_VALUE}" \
    ./gradlew :ime:app:assembleNskDebug :ime:app:assembleAndroidTest
else
  echo "SKIP_BUILD=1 set; using existing APKs."
fi

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

if [[ "${SKIP_INSTALL:-0}" != "1" ]]; then
  echo "Installing APKs..."
  CLEAN_INSTALL="${CLEAN_INSTALL:-0}"
  if [[ "${CLEAN_INSTALL}" == "1" ]]; then
    echo "CLEAN_INSTALL=1 set; uninstalling app/test (this WILL wipe NSK app data)..." >&2
    "${ADB[@]}" uninstall wtf.uhoh.newsoftkeyboard > "${OUT_DIR}/uninstall_app.txt" 2>&1 || true
    "${ADB[@]}" uninstall wtf.uhoh.newsoftkeyboard.test > "${OUT_DIR}/uninstall_test.txt" 2>&1 || true
  else
    echo "Preserving NSK app data (no uninstall). Set CLEAN_INSTALL=1 to wipe app data." >&2
  fi

  set +e
  "${ADB[@]}" install -r -d "${APP_APK}" > "${OUT_DIR}/install_app.txt" 2>&1
  APP_INSTALL_EXIT="$?"
  "${ADB[@]}" install -r -d "${TEST_APK}" > "${OUT_DIR}/install_test.txt" 2>&1
  TEST_INSTALL_EXIT="$?"
  set -e

  if [[ "${APP_INSTALL_EXIT}" -ne 0 ]]; then
    echo "Failed installing app APK (exit ${APP_INSTALL_EXIT}). See ${OUT_DIR}/install_app.txt" >&2
    echo "If this is a signature mismatch, re-run with CLEAN_INSTALL=1 (will wipe app data)." >&2
    exit "${APP_INSTALL_EXIT}"
  fi
  if [[ "${TEST_INSTALL_EXIT}" -ne 0 ]]; then
    echo "Failed installing androidTest APK (exit ${TEST_INSTALL_EXIT}). See ${OUT_DIR}/install_test.txt" >&2
    echo "Retrying after uninstalling only the test package..." >&2
    "${ADB[@]}" uninstall wtf.uhoh.newsoftkeyboard.test > "${OUT_DIR}/uninstall_test_retry.txt" 2>&1 || true
    "${ADB[@]}" install -r -d "${TEST_APK}" > "${OUT_DIR}/install_test_retry.txt" 2>&1
  fi
else
  echo "SKIP_INSTALL=1 set; using existing installed APKs."
fi

TEST_RUNNER="wtf.uhoh.newsoftkeyboard.test/androidx.test.runner.AndroidJUnitRunner"
FILTER="${ADB_TEST_FILTER:-wtf.uhoh.newsoftkeyboard.app.ime.DeviceFarmHapticsDiagnosticsTest#smoke}"

echo "Running haptics diagnostics instrumentation (filter: ${FILTER})..."
set +e
"${ADB[@]}" shell am instrument -w -r -e class "${FILTER}" "${TEST_RUNNER}" \
  | tee "${OUT_DIR}/instrumentation.txt"
TEST_EXIT="${PIPESTATUS[0]}"
set -e

echo "Capturing device state (after)..."
"${ADB[@]}" shell dumpsys vibrator > "${OUT_DIR}/dumpsys_vibrator_after.txt" 2>&1 || true
"${ADB[@]}" shell dumpsys vibrator_manager > "${OUT_DIR}/dumpsys_vibrator_manager_after.txt" 2>&1 || true

echo "Capturing logcat..."
"${ADB[@]}" logcat -d > "${OUT_DIR}/logcat.txt" || true
"${ADB[@]}" logcat -d -s DeviceFarmHaptics:I > "${OUT_DIR}/logcat_devicefarm_haptics.txt" 2>&1 || true

if [[ "${TEST_EXIT}" -ne 0 ]]; then
  echo "Diagnostics FAILED (adb exit ${TEST_EXIT}). See ${OUT_DIR}/instrumentation.txt and ${OUT_DIR}/logcat_devicefarm_haptics.txt"
  exit "${TEST_EXIT}"
fi

if grep -q "FAILURES!!!" "${OUT_DIR}/instrumentation.txt"; then
  echo "Diagnostics FAILED (instrumentation reported failures). See ${OUT_DIR}/instrumentation.txt and ${OUT_DIR}/logcat_devicefarm_haptics.txt"
  exit 1
fi

if ! grep -q "OK (" "${OUT_DIR}/instrumentation.txt"; then
  echo "Diagnostics FAILED (missing success code). See ${OUT_DIR}/instrumentation.txt and ${OUT_DIR}/logcat_devicefarm_haptics.txt"
  exit 1
fi

echo "Diagnostics PASSED. Review ${OUT_DIR}/logcat_devicefarm_haptics.txt"
