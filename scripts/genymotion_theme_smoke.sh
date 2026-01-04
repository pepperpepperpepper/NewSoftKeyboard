#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

DEVICE_ID="${GENYMOTION_DEV:-${ANDROID_SERIAL:-}}"
if [[ -z "${DEVICE_ID}" ]]; then
  DEVICE_ID="$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')"
fi
if [[ -z "${DEVICE_ID}" ]]; then
  echo "No adb device found. Set GENYMOTION_DEV or ANDROID_SERIAL, or connect a device."
  exit 1
fi

ADB=(adb -s "${DEVICE_ID}")

OUT_DIR="outputs/genymotion/theme-smoke/$(date +%Y%m%d-%H%M%S)"
mkdir -p "${OUT_DIR}"

echo "Using device serial: ${DEVICE_ID}"
echo "Artifacts will be written to: ${OUT_DIR}"

TEST_BUILD_TYPE_VALUE="${TEST_BUILD_TYPE:-debug}"
if [[ "${TEST_BUILD_TYPE_VALUE}" != "debug" && "${TEST_BUILD_TYPE_VALUE}" != "release" ]]; then
  echo "Unsupported TEST_BUILD_TYPE='${TEST_BUILD_TYPE_VALUE}'. Expected 'debug' or 'release'."
  exit 1
fi
if [[ -z "${TEST_BUILD_TYPE:-}" ]]; then
  echo "TEST_BUILD_TYPE not set; defaulting to '${TEST_BUILD_TYPE_VALUE}' for Genymotion smoke."
fi

echo "Clearing logcat..."
"${ADB[@]}" logcat -c || true

echo "Building APKs (no Gradle build task)..."
if [[ "${TEST_BUILD_TYPE_VALUE}" == "debug" ]]; then
  APP_TASK=":ime:app:assembleNskDebug"
  TEST_TASK=":ime:app:assembleNskDebugAndroidTest"
  APP_APK="ime/app/build/outputs/apk/nsk/debug/app-nsk-debug.apk"
  TEST_APK="ime/app/build/outputs/apk/androidTest/nsk/debug/app-nsk-debug-androidTest.apk"
else
  APP_TASK=":ime:app:assembleNskRelease"
  TEST_TASK=":ime:app:assembleNskReleaseAndroidTest"
  APP_APK="ime/app/build/outputs/apk/nsk/release/app-nsk-release.apk"
  TEST_APK="ime/app/build/outputs/apk/androidTest/nsk/release/app-nsk-release-androidTest.apk"
fi

TEST_BUILD_TYPE="${TEST_BUILD_TYPE_VALUE}" ./gradlew "${APP_TASK}" "${TEST_TASK}"

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
TEST_CLASSES=(
  "wtf.uhoh.newsoftkeyboard.app.ui.settings.ThemeCustomizationNavigationSmokeTest"
  "wtf.uhoh.newsoftkeyboard.app.ui.settings.BackgroundPhotoLivePreviewSmokeTest"
  "wtf.uhoh.newsoftkeyboard.app.ui.settings.CheapPreviewGuardSmokeTest"
  "wtf.uhoh.newsoftkeyboard.app.ui.settings.NoMainThreadDecodeSmokeTest"
)
TEST_CLASS_ARG="$(IFS=,; echo "${TEST_CLASSES[*]}")"

echo "Running instrumentation smoke..."
set +e
"${ADB[@]}" shell am instrument -w -r -e class "${TEST_CLASS_ARG}" "${TEST_RUNNER}" \
  | tee "${OUT_DIR}/instrumentation.txt"
TEST_EXIT="${PIPESTATUS[0]}"
set -e

echo "Capturing logcat..."
"${ADB[@]}" logcat -d > "${OUT_DIR}/logcat.txt" || true

echo "Capturing gfxinfo framestats..."
"${ADB[@]}" shell dumpsys gfxinfo wtf.uhoh.newsoftkeyboard framestats \
  > "${OUT_DIR}/gfxinfo_framestats.txt" || true

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
