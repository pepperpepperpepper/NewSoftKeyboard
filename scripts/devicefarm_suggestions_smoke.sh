#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

if ! command -v devicefarm-smoke >/dev/null 2>&1; then
  echo "Missing devicefarm-smoke in PATH. See devicefarm_instructions.md for setup."
  exit 1
fi

OUT_DIR="${1:-outputs/devicefarm/suggestions-smoke/$(date +%Y%m%d-%H%M%S)}"
mkdir -p "${OUT_DIR}"

echo "Artifacts will be written to: ${OUT_DIR}"

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

FILTER="${DEVICEFARM_TEST_FILTER:-wtf.uhoh.newsoftkeyboard.app.ime.SuggestionsStripNoSuggestionsFlagInstrumentedTest#smoke}"

# Cost control: runs are billed only while executing, but a stuck run can be expensive.
# Ensure uploads are cleaned up and stop any in-flight run if this script is interrupted.
: "${DEVICEFARM_CLEANUP_UPLOADS:=1}"
: "${DEVICEFARM_MAX_DEVICES:=1}"
: "${DEVICEFARM_MIN_OS_VERSION:=16}"
: "${DEVICEFARM_JOB_TIMEOUT_MINUTES:=10}"
export DEVICEFARM_CLEANUP_UPLOADS
export DEVICEFARM_MAX_DEVICES
export DEVICEFARM_MIN_OS_VERSION
export DEVICEFARM_JOB_TIMEOUT_MINUTES

cleanup() {
  set +e
  devicefarm-smoke stop >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

echo "Running AWS Device Farm instrumentation smoke (filter: ${FILTER})..."
devicefarm-smoke run \
  --type INSTRUMENTATION \
  --app "${APP_APK}" \
  --test "${TEST_APK}" \
  --filter "${FILTER}" \
  --out "${OUT_DIR}"

echo "Smoke completed. Artifacts at: ${OUT_DIR}"
