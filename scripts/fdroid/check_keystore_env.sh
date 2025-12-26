#!/usr/bin/env bash
# Fast guard to ensure required env vars are present before running fdroid update/deploy.

set -euo pipefail

missing=0

require() {
  local name="$1"
  if [[ -z "${!name-}" ]]; then
    echo "MISSING: $name"
    missing=1
  else
    echo "OK: $name is set"
  fi
}

echo "== Checking required F-Droid env vars =="
require FDROID_KEYSTORE_PASS
require FDROID_KEY_PASS
require FDROID_AWS_BUCKET
require FDROID_AWS_ACCESS_KEY_ID
require FDROID_AWS_SECRET_KEY

if [[ $missing -ne 0 ]]; then
  echo "One or more required env vars are missing. Export them and rerun."
  exit 1
fi

echo "All required env vars are set."
