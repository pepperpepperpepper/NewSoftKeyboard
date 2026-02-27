#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

if ! command -v devicefarm-smoke >/dev/null 2>&1; then
  echo "Missing devicefarm-smoke in PATH. See devicefarm_instructions.md for setup."
  exit 1
fi

ensure_device_pool_exists() {
  if [[ "${SKIP_ENSURE_DEVICE_POOL:-0}" == "1" ]]; then
    echo "SKIP_ENSURE_DEVICE_POOL=1 set; skipping Device Farm device-pool ensure."
    return 0
  fi

  if ! command -v aws >/dev/null 2>&1; then
    echo "Missing aws CLI in PATH; cannot ensure Device Farm device pool."
    exit 1
  fi
  if ! command -v jq >/dev/null 2>&1; then
    echo "Missing jq in PATH; cannot ensure Device Farm device pool."
    exit 1
  fi

  local region="us-west-2"
  local project_name="${DEVICEFARM_PROJECT_NAME:-codex-android-smoke}"
  local pool_name="${DEVICEFARM_DEVICE_POOL_NAME:-codex-android-smoke-phones}"

  local min_os_version="${DEVICEFARM_MIN_OS_VERSION:-16}"
  local model_contains="Pixel 10 Pro"
  if [[ -v DEVICEFARM_MODEL_CONTAINS ]]; then
    model_contains="${DEVICEFARM_MODEL_CONTAINS}"
  fi
  local manufacturer="Google"
  if [[ -v DEVICEFARM_MANUFACTURER ]]; then
    manufacturer="${DEVICEFARM_MANUFACTURER}"
  fi
  local max_devices="${DEVICEFARM_MAX_DEVICES:-1}"

  local project_arn
  project_arn="$(
    aws --region "${region}" devicefarm list-projects --output json \
      | jq -r --arg name "${project_name}" '.projects[] | select(.name == $name) | .arn' \
      | head -n 1 \
      || true
  )"

  if [[ -z "${project_arn}" || "${project_arn}" == "null" ]]; then
    echo "Creating Device Farm project: ${project_name}"
    project_arn="$(
      aws --region "${region}" devicefarm create-project --name "${project_name}" --output json \
        | jq -r '.project.arn'
    )"
  fi

  local pool_arn
  pool_arn="$(
    aws --region "${region}" devicefarm list-device-pools --arn "${project_arn}" --output json \
      | jq -r --arg name "${pool_name}" '.devicePools[] | select(.name == $name) | .arn' \
      | head -n 1 \
      || true
  )"

  if [[ -n "${pool_arn}" && "${pool_arn}" != "null" ]]; then
    echo "Using existing Device Farm device pool: ${pool_name}"
    return 0
  fi

  # NOTE: Device Farm expects quoted operands INSIDE the string value field.
  # Example: "\"ANDROID\"" and "\"14\"" (not just "ANDROID" or "14").
  local rules
  rules="$(
    jq -cn \
      --arg min_os "${min_os_version}" \
      --arg model "${model_contains}" \
      --arg mfg "${manufacturer}" \
      '
        [
          {attribute:"PLATFORM", operator:"EQUALS", value:"\"ANDROID\""},
          {attribute:"FORM_FACTOR", operator:"EQUALS", value:"\"PHONE\""},
          {attribute:"FLEET_TYPE", operator:"EQUALS", value:"\"PUBLIC\""},
          {attribute:"OS_VERSION", operator:"GREATER_THAN_OR_EQUALS", value:("\"" + $min_os + "\"")}
        ]
        + (if ($model|length) > 0 then [{attribute:"MODEL", operator:"CONTAINS", value:("\"" + $model + "\"")}] else [] end)
        + (if ($mfg|length) > 0 then [{attribute:"MANUFACTURER", operator:"EQUALS", value:("\"" + $mfg + "\"")}] else [] end)
      '
  )"

  echo "Creating Device Farm device pool: ${pool_name} (maxDevices=${max_devices}, minOS=${min_os_version})"
  aws --region "${region}" devicefarm create-device-pool \
    --project-arn "${project_arn}" \
    --name "${pool_name}" \
    --rules "${rules}" \
    --max-devices "${max_devices}" \
    --output json \
    >/dev/null
}

OUT_DIR="${1:-outputs/devicefarm/nextword-tapchain-report/$(date +%Y%m%d-%H%M%S)}"
mkdir -p "${OUT_DIR}"

echo "Artifacts will be written to: ${OUT_DIR}"

TEST_BUILD_TYPE_VALUE="${TEST_BUILD_TYPE:-debug}"
if [[ "${TEST_BUILD_TYPE_VALUE}" != "debug" ]]; then
  echo "This report only supports TEST_BUILD_TYPE=debug (got '${TEST_BUILD_TYPE_VALUE}')."
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

FILTER="${DEVICEFARM_TEST_FILTER:-wtf.uhoh.newsoftkeyboard.app.dictionaries.nextword.NextWordTapChainReportUiAutomatorTest#generateTapChainReport}"

# Cost control: runs are billed only while executing, but a stuck run can be expensive.
# Ensure uploads are cleaned up and stop any in-flight run if this script is interrupted.
: "${DEVICEFARM_CLEANUP_UPLOADS:=1}"
: "${DEVICEFARM_MAX_DEVICES:=1}"
: "${DEVICEFARM_MIN_OS_VERSION:=16}"
: "${DEVICEFARM_JOB_TIMEOUT_MINUTES:=45}"
export DEVICEFARM_CLEANUP_UPLOADS
export DEVICEFARM_MAX_DEVICES
export DEVICEFARM_MIN_OS_VERSION
export DEVICEFARM_JOB_TIMEOUT_MINUTES

cleanup() {
  set +e
  devicefarm-smoke stop >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

ensure_device_pool_exists

if [[ "${SKIP_DEVICEFARM_RUN:-0}" == "1" ]]; then
  echo "SKIP_DEVICEFARM_RUN=1 set; skipping AWS Device Farm run (assuming artifacts already exist in: ${OUT_DIR})"
else
  echo "Running AWS Device Farm instrumentation tap-chain report (filter: ${FILTER})..."
  devicefarm-smoke run \
    --type INSTRUMENTATION \
    --app "${APP_APK}" \
    --test "${TEST_APK}" \
    --filter "${FILTER}" \
    --out "${OUT_DIR}"
fi

echo "Extracting report JSON per-device..."

UPLOAD_DIR="${OUT_DIR}/upload"
mkdir -p "${UPLOAD_DIR}/devices"

declare -a DEVICE_NAMES=()
while IFS= read -r -d '' d; do
  DEVICE_NAMES+=("$(basename "${d}")")
done < <(find "${OUT_DIR}" -maxdepth 1 -mindepth 1 -type d -not -name upload -print0 | sort -z)

if [[ "${#DEVICE_NAMES[@]}" -eq 0 ]]; then
  echo "No per-device artifact directories found under ${OUT_DIR}."
  exit 1
fi

generated=0
for device_name in "${DEVICE_NAMES[@]}"; do
  device_dir="${OUT_DIR}/${device_name}"
  report_lines="${device_dir}/report_lines.txt"
  report_json="${device_dir}/report.json"

  echo "- ${device_name}"

  : > "${report_lines}"

  log_json="$(find "${device_dir}" -type f -path "*/LOG/ListArtifactType.log_*" -print -quit || true)"
  if [[ -n "${log_json}" && -f "${log_json}" ]] && command -v jq >/dev/null 2>&1; then
    sysout="${device_dir}/system_out.txt"
    jq -r '.[] | select(.tag=="System.out") | .data' "${log_json}" > "${sysout}" || true
    if [[ -s "${sysout}" ]]; then
      awk '
        $0 ~ /NEXTWORD_TAPCHAIN_REPORT_JSON_BEGIN/ {inside=1; next}
        $0 ~ /NEXTWORD_TAPCHAIN_REPORT_JSON_END/ {inside=0}
        inside==1 && $0 ~ /^NWJSON(GZ)?:/ {print}
      ' "${sysout}" > "${report_lines}" || true
    fi
  fi

  if [[ ! -s "${report_lines}" ]]; then
    logcat_file="$(find "${device_dir}" -type f -path "*/FILE/Logcat_*" -print -quit || true)"
    if [[ -n "${logcat_file}" && -f "${logcat_file}" ]]; then
      awk '
        $0 ~ /NEXTWORD_TAPCHAIN_REPORT_JSON_BEGIN/ {inside=1; next}
        $0 ~ /NEXTWORD_TAPCHAIN_REPORT_JSON_END/ {inside=0}
        inside==1 && $0 ~ /NWJSON/ {
          pos = index($0, "NWJSON");
          if (pos > 0) print substr($0, pos);
        }
      ' "${logcat_file}" > "${report_lines}" || true
    fi
  fi

  if [[ ! -s "${report_lines}" ]]; then
    echo "  ERROR: No report lines found (missing NWJSON/NWJSONGZ markers)."
    continue
  fi

  if grep -q "^NWJSONGZ:" "${report_lines}"; then
    if ! command -v base64 >/dev/null 2>&1; then
      echo "  ERROR: Missing base64 in PATH; cannot decode NWJSONGZ report."
      continue
    fi
    if ! command -v gzip >/dev/null 2>&1; then
      echo "  ERROR: Missing gzip in PATH; cannot decode NWJSONGZ report."
      continue
    fi

    sort -t: -k2,2n "${report_lines}" \
      | sed -E 's/^NWJSONGZ:[0-9]+://' \
      | tr -d '\n' \
      | base64 -d \
      | gzip -dc \
      > "${report_json}"
  else
    sort -t: -k2,2n "${report_lines}" \
      | sed -E 's/^NWJSON:[0-9]+://' \
      > "${report_json}"
  fi

  if [[ ! -s "${report_json}" ]]; then
    echo "  ERROR: Failed reconstructing report.json."
    continue
  fi

  echo "  Rendering HTML..."
  node scripts/render_nextword_tapchain_report.js \
    docs/nextword-test-suite/index.html \
    "${report_json}" \
    "${device_dir}/index.html" \
    > "${device_dir}/render_out.txt"

  mkdir -p "${UPLOAD_DIR}/devices/${device_name}"
  cp "${device_dir}/index.html" "${UPLOAD_DIR}/devices/${device_name}/index.html"
  cp "${report_json}" "${UPLOAD_DIR}/devices/${device_name}/report.json"

  generated=$((generated + 1))
done

if [[ "${generated}" -eq 0 ]]; then
  echo "No reports were generated."
  exit 1
fi

echo "Generating index page..."
INDEX_HTML="${UPLOAD_DIR}/index.html"
{
  cat <<'HTML'
<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>NSK Next‑Word — Device Farm Tap‑Chain Reports</title>
    <style>
      body {
        margin: 0;
        padding: 24px 16px 64px;
        font: 14px/1.5 system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial;
        color: #111827;
        background: #ffffff;
      }
      main {
        max-width: 980px;
        margin: 0 auto;
      }
      h1 {
        margin: 0;
        font-size: 22px;
      }
      p {
        margin: 10px 0;
        color: #4b5563;
      }
      ul {
        padding-left: 18px;
      }
      li {
        margin: 6px 0;
      }
      code {
        font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
      }
      a {
        color: #111827;
        text-decoration: underline;
      }
    </style>
  </head>
  <body>
    <main>
      <h1>NSK Next‑Word — Device Farm Tap‑Chain Reports</h1>
      <p>Each link is a full report page with embedded JSON.</p>
      <ul>
HTML

  for device_name in "${DEVICE_NAMES[@]}"; do
    if [[ -f "${UPLOAD_DIR}/devices/${device_name}/index.html" ]]; then
      printf '        <li><a href="devices/%s/index.html"><code>%s</code></a></li>\n' "${device_name}" "${device_name}"
    fi
  done

  cat <<'HTML'
      </ul>
    </main>
  </body>
</html>
HTML
} > "${INDEX_HTML}"

echo "Wrote index page: ${INDEX_HTML}"

: "${DEVICEFARM_UPLOAD_KEY_PREFIX:=nsk-nextword-suite-devicefarm}"
: "${SKIP_UPLOAD:=0}"

if [[ "${SKIP_UPLOAD}" == "1" ]]; then
  echo "SKIP_UPLOAD=1 set; skipping upload."
  exit 0
fi

if ! command -v wtf-upload >/dev/null 2>&1; then
  echo "Missing wtf-upload in PATH; skipping upload."
  exit 0
fi

echo "Uploading reports (key prefix: ${DEVICEFARM_UPLOAD_KEY_PREFIX})..."
for device_name in "${DEVICE_NAMES[@]}"; do
  device_html="${UPLOAD_DIR}/devices/${device_name}/index.html"
  if [[ -f "${device_html}" ]]; then
    wtf-upload --key "${DEVICEFARM_UPLOAD_KEY_PREFIX}/devices/${device_name}/index.html" \
      --content-type text/html \
      --cache-control "no-cache, max-age=0" \
      "${device_html}" \
      > "${OUT_DIR}/${device_name}/url.txt"
  fi
done

URL="$(
  wtf-upload --key "${DEVICEFARM_UPLOAD_KEY_PREFIX}/index.html" \
    --content-type text/html \
    --cache-control "no-cache, max-age=0" \
    "${INDEX_HTML}"
)"
echo "${URL}" | tee "${OUT_DIR}/url.txt"

echo "Done: ${URL}"
