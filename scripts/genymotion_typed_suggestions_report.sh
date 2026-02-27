#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

DEVICE_ID="${GENYMOTION_DEV:-${ANDROID_SERIAL:-}}"
GMSAAS_INSTANCE_UUID="${GMSAAS_INSTANCE_UUID:-}"
if [[ -z "${DEVICE_ID}" ]]; then
  DEVICE_ID="$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')"
fi
if [[ -z "${DEVICE_ID}" ]]; then
  echo "No adb device found; starting a Genymotion SaaS instance..." >&2
  eval "$("${ROOT_DIR}/scripts/gmsaas_start_and_connect.sh")"
  DEVICE_ID="${GMSAAS_DEVICE_SERIAL:-}"
  GMSAAS_INSTANCE_UUID="${GMSAAS_INSTANCE_UUID:-}"
fi
if [[ -z "${DEVICE_ID}" ]]; then
  echo "No adb device found. Set GENYMOTION_DEV or ANDROID_SERIAL, or connect a device."
  exit 1
fi

ADB=(adb -s "${DEVICE_ID}")

OUT_DIR="outputs/genymotion/typed-suggestions-report/$(date +%Y%m%d-%H%M%S)"
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

echo "Starting System.out logcat capture (report JSON)..."
SYSTEM_OUT_LOGCAT="${OUT_DIR}/system_out_logcat.txt"
set +e
timeout 3600 "${ADB[@]}" logcat -v time System.out:I '*:S' > "${SYSTEM_OUT_LOGCAT}" 2>&1 &
SYSTEM_OUT_LOGCAT_PID="$!"
set -e

reconnect_gmsaas_adb_if_possible() {
  if [[ -n "${GMSAAS_INSTANCE_UUID}" ]]; then
    echo "Reconnecting ADB via Genymotion SaaS instance ${GMSAAS_INSTANCE_UUID}..." >&2
    eval "$(
      INSTANCE_UUID="${GMSAAS_INSTANCE_UUID}" \
        "${ROOT_DIR}/scripts/gmsaas_start_and_connect.sh"
    )"
  else
    echo "Reconnecting ADB via Genymotion SaaS (discovering instance)..." >&2
    eval "$("${ROOT_DIR}/scripts/gmsaas_start_and_connect.sh")"
    GMSAAS_INSTANCE_UUID="${GMSAAS_INSTANCE_UUID:-}"
  fi
  if [[ -n "${GMSAAS_DEVICE_SERIAL:-}" ]]; then
    DEVICE_ID="${GMSAAS_DEVICE_SERIAL}"
    ADB=(adb -s "${DEVICE_ID}")
    echo "Reconnected device serial: ${DEVICE_ID}" >&2
  fi
}

echo "Building APKs (no Gradle build task)..."
TEST_BUILD_TYPE_VALUE="${TEST_BUILD_TYPE:-debug}"
if [[ "${TEST_BUILD_TYPE_VALUE}" != "debug" ]]; then
  echo "Unsupported TEST_BUILD_TYPE='${TEST_BUILD_TYPE_VALUE}'. Expected 'debug' (this report uses debug-only harness activities)."
  exit 1
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
TEST_CLASS="wtf.uhoh.newsoftkeyboard.app.dictionaries.typed.TypedSuggestionsReportUiAutomatorTest"

echo "Running typed-suggestions report..."
INSTRUMENT_ARGS=()
if [[ -n "${ONLY_CASE_ID:-}" ]]; then
  echo "Filtering report to onlyCaseId='${ONLY_CASE_ID}'"
  INSTRUMENT_ARGS+=(-e onlyCaseId "${ONLY_CASE_ID}")
fi
set +e
"${ADB[@]}" shell am instrument -w -r "${INSTRUMENT_ARGS[@]}" -e class "${TEST_CLASS}" "${TEST_RUNNER}" \
  | tee "${OUT_DIR}/instrumentation.txt"
TEST_EXIT="${PIPESTATUS[0]}"
set -e

echo "Stopping System.out logcat capture..."
set +e
kill "${SYSTEM_OUT_LOGCAT_PID}" >/dev/null 2>&1 || true
wait "${SYSTEM_OUT_LOGCAT_PID}" >/dev/null 2>&1 || true
set -e

echo "Capturing logcat..."
reconnect_gmsaas_adb_if_possible || true
if ! timeout 60 "${ADB[@]}" logcat -d > "${OUT_DIR}/logcat.txt"; then
  echo "Logcat capture failed; attempting one more ADB reconnect..." >&2
  reconnect_gmsaas_adb_if_possible || true
  if ! timeout 60 "${ADB[@]}" logcat -d > "${OUT_DIR}/logcat.txt"; then
    echo "Failed to capture logcat (device may have disconnected). Try re-running after reconnecting adb." >&2
    exit 1
  fi
fi

if [[ "${TEST_EXIT}" -ne 0 ]]; then
  echo "Report run FAILED (adb exit ${TEST_EXIT}). See ${OUT_DIR}/instrumentation.txt and ${OUT_DIR}/logcat.txt"
  exit "${TEST_EXIT}"
fi

if grep -q "FAILURES!!!" "${OUT_DIR}/instrumentation.txt"; then
  echo "Report run FAILED (instrumentation reported failures). See ${OUT_DIR}/instrumentation.txt and ${OUT_DIR}/logcat.txt"
  exit 1
fi

echo "Extracting JSON..."
REPORT_LINES="${OUT_DIR}/report_lines.txt"
if [[ -s "${SYSTEM_OUT_LOGCAT}" ]]; then
  awk '
    $0 ~ /TSJSON(GZ)?:/ {
      sub(/^.*TSJSON/, "TSJSON");
      print
    }
  ' "${SYSTEM_OUT_LOGCAT}" > "${REPORT_LINES}"
else
  echo "WARNING: Missing ${SYSTEM_OUT_LOGCAT}; falling back to logcat.txt for report extraction." >&2
  awk '
    $0 ~ /TYPED_SUGGESTIONS_REPORT_JSON_BEGIN/ {inside=1; next}
    $0 ~ /TYPED_SUGGESTIONS_REPORT_JSON_END/ {inside=0}
    inside==1 && $0 ~ /System\.out:/ {
      sub(/^.*System\.out: /, "");
      if ($0 ~ /^TSJSON(GZ)?:/) {
        print
      }
    }
  ' "${OUT_DIR}/logcat.txt" > "${REPORT_LINES}"
fi

if grep -q "^TSJSONGZ:" "${REPORT_LINES}"; then
  if ! command -v base64 >/dev/null 2>&1; then
    echo "Missing base64 in PATH; cannot decode TSJSONGZ report." >&2
    exit 1
  fi
  if ! command -v gzip >/dev/null 2>&1; then
    echo "Missing gzip in PATH; cannot decode TSJSONGZ report." >&2
    exit 1
  fi

  sort -t: -k2,2n "${REPORT_LINES}" \
    | sed -E 's/^TSJSONGZ:[0-9]+://' \
    | tr -d '\n' \
    | base64 -d \
    | gzip -dc \
    > "${OUT_DIR}/report.json"
else
  sort -t: -k2,2n "${REPORT_LINES}" \
    | sed -E 's/^TSJSON:[0-9]+://' \
    > "${OUT_DIR}/report.json"
fi

if [[ ! -s "${OUT_DIR}/report.json" ]]; then
  echo "Missing report JSON in ${OUT_DIR}/instrumentation.txt"
  exit 1
fi

echo "Rendering HTML report..."
node scripts/render_typed_suggestions_report.js \
  docs/typed-suggestions-test-suite/index.html \
  "${OUT_DIR}/report.json" \
  "${OUT_DIR}/index.html" \
  > "${OUT_DIR}/render_out.txt"

echo "Uploading report..."
UPLOAD_KEY="nsk-typed-suggestions-suite/index.html"
if [[ -n "${ONLY_CASE_ID:-}" ]]; then
  SAFE_ONLY_CASE_ID="${ONLY_CASE_ID//[^A-Za-z0-9._-]/_}"
  UPLOAD_KEY="nsk-typed-suggestions-suite-only-${SAFE_ONLY_CASE_ID}/index.html"
fi
URL="$(
  wtf-upload --key "${UPLOAD_KEY}" \
    --content-type text/html \
    --cache-control "no-cache, max-age=0" \
    "${OUT_DIR}/index.html"
)"
echo "${URL}" | tee "${OUT_DIR}/url.txt"

echo "Done: ${URL}"

