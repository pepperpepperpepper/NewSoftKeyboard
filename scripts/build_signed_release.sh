#!/usr/bin/env bash
# Build a signed release APK locally (no F-Droid publish, no version bump, no S3).
# Wraps `:ime:app:assembleNskRelease` and reads signing creds from the same env/.env
# files that gradle/apk_module.gradle resolves (so it works even in non-interactive shells).
#
# Usage:
#   scripts/build_signed_release.sh           # build only
#   scripts/build_signed_release.sh --install # build + adb install -r on default device
#   FLAVOR=askCompat scripts/build_signed_release.sh   # build askCompatRelease instead

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

FLAVOR="${FLAVOR:-Nsk}"
TASK=":ime:app:assemble${FLAVOR}Release"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-/mnt/finished/.gradle}"
export GRADLE_USER_HOME

# Resolve env file (mirrors gradle/apk_module.gradle resolution order).
ENV_FILE="${FDROID_ENV_FILE:-${ENV_FILE:-}}"
if [[ -z "${ENV_FILE}" ]]; then
  for c in "${ROOT}/fdroid/.env" "${FDROID_DATA:-}/.env" "${HOME}/fdroid/.env"; do
    [[ -f "$c" ]] && ENV_FILE="$c" && break
  done
fi
if [[ -n "${ENV_FILE}" && -f "${ENV_FILE}" ]]; then
  echo "== Sourcing signing env from: ${ENV_FILE}"
  set -a; source "${ENV_FILE}"; set +a
fi

# Verify at least one keystore-pass and one key-pass var is set; otherwise gradle silently
# falls through to building an unsigned release, which is almost never what you want.
have_store_pass=0
for v in KEY_STORE_FILE_PASSWORD FDROID_KEYSTORE_PASS FDROID_KEY_STORE_PASS; do
  [[ -n "${!v:-}" ]] && have_store_pass=1 && break
done
have_key_pass=0
for v in KEY_STORE_FILE_DEFAULT_ALIAS_PASSWORD FDROID_KEY_ALIAS_PASS FDROID_KEY_PASS; do
  [[ -n "${!v:-}" ]] && have_key_pass=1 && break
done
if [[ $have_store_pass -eq 0 || $have_key_pass -eq 0 ]]; then
  echo "ERROR: signing passwords not set." >&2
  echo "  Need one of: KEY_STORE_FILE_PASSWORD / FDROID_KEYSTORE_PASS / FDROID_KEY_STORE_PASS" >&2
  echo "  And one of: KEY_STORE_FILE_DEFAULT_ALIAS_PASSWORD / FDROID_KEY_ALIAS_PASS / FDROID_KEY_PASS" >&2
  echo "  See BUILDING.md (Signing section) for keystore path resolution." >&2
  exit 1
fi

echo "== Running ${TASK}"
./gradlew "${TASK}" -x lint

flavor_lc="$(echo "${FLAVOR}" | tr '[:upper:]' '[:lower:]')"
APK_DIR="${ROOT}/ime/app/build/outputs/apk/${flavor_lc}/release"
APK="$(ls -t "${APK_DIR}"/*.apk 2>/dev/null | head -1 || true)"
if [[ -z "${APK}" ]]; then
  echo "ERROR: build succeeded but no APK at ${APK_DIR}" >&2
  exit 1
fi
echo "== Built signed APK: ${APK}"

if [[ "${1:-}" == "--install" ]]; then
  echo "== adb install -r ${APK}"
  adb install -r "${APK}"
fi
