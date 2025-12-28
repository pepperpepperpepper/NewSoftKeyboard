#!/usr/bin/env bash
# Fast guard to ensure required env vars are present before running fdroid update/deploy.

set -euo pipefail

missing=0

require_any() {
  local description="$1"
  shift

  local found=
  local name=
  for name in "$@"; do
    if [[ -n "${!name-}" ]]; then
      found="$name"
      break
    fi
  done

  if [[ -z "$found" ]]; then
    echo "MISSING: $description (set one of: $*)"
    missing=1
  else
    echo "OK: $description (${found} is set)"
  fi
}

resolve_keystore() {
  local fdroid_data="${FDROID_DATA:-/home/arch/fdroid}"
  local candidate=

  candidate="${KEY_STORE_FILE-}"
  if [[ -n "$candidate" ]]; then echo "$candidate"; return 0; fi
  candidate="${FDROID_KEYSTORE_FILE-}"
  if [[ -n "$candidate" ]]; then echo "$candidate"; return 0; fi

  if [[ -f "/tmp/newsoftkeyboard.keystore" ]]; then echo "/tmp/newsoftkeyboard.keystore"; return 0; fi
  if [[ -f "${fdroid_data}/keystore.jks" ]]; then echo "${fdroid_data}/keystore.jks"; return 0; fi
  if [[ -f "${HOME}/fdroid/keystore.jks" ]]; then echo "${HOME}/fdroid/keystore.jks"; return 0; fi
  if [[ -f "/tmp/anysoftkeyboard.keystore" ]]; then echo "/tmp/anysoftkeyboard.keystore"; return 0; fi

  return 1
}

echo "== Checking required F-Droid env vars =="
require_any "Keystore store password" KEY_STORE_FILE_PASSWORD FDROID_KEYSTORE_PASS FDROID_KEY_STORE_PASS
require_any "Keystore key password" KEY_STORE_FILE_DEFAULT_ALIAS_PASSWORD FDROID_KEY_ALIAS_PASS FDROID_KEY_PASS
require_any "AWS bucket" FDROID_AWS_BUCKET
require_any "AWS access key" FDROID_AWS_ACCESS_KEY_ID
require_any "AWS secret key" FDROID_AWS_SECRET_KEY

keystore_path="$(resolve_keystore || true)"
if [[ -z "$keystore_path" ]]; then
  echo "MISSING: Keystore file (set KEY_STORE_FILE or FDROID_KEYSTORE_FILE, or place at /tmp/newsoftkeyboard.keystore, \$FDROID_DATA/keystore.jks, or \$HOME/fdroid/keystore.jks)"
  missing=1
elif [[ ! -f "$keystore_path" ]]; then
  echo "MISSING: Keystore file does not exist at '$keystore_path'"
  missing=1
else
  echo "OK: Keystore file found at '$keystore_path'"
fi

if [[ $missing -ne 0 ]]; then
  echo "One or more required env vars are missing. Export them and rerun."
  exit 1
fi

echo "All required env vars are set."
