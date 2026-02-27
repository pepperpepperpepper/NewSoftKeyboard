#!/usr/bin/env bash
set -euo pipefail

# Starts a Genymotion SaaS device and connects ADB.
#
# Output (stdout) is shell-safe assignments so callers can eval/source:
#   eval "$(scripts/gmsaas_start_and_connect.sh)"
# Then read:
#   $GMSAAS_INSTANCE_UUID
#   $GMSAAS_DEVICE_SERIAL
#
# Credentials are loaded from environment variables or from ~/.api-keys.
# This script does not print secrets.

RECIPE_UUID="${RECIPE_UUID:-9074ccc1-7aba-4c9b-b615-e69ef389738c}" # Android 14.0 - Genymotion Phone
INSTANCE_NAME="${INSTANCE_NAME:-nsk-android14}"
INSTANCE_UUID="${INSTANCE_UUID:-}"
MAX_RUN_DURATION_MINUTES="${MAX_RUN_DURATION_MINUTES:-${GMSAAS_MAX_RUN_DURATION_MINUTES:-}}"

if ! command -v adb >/dev/null 2>&1; then
  echo "adb is not in PATH." >&2
  exit 2
fi

GMSAAS_BIN="${GMSAAS_BIN:-gmsaas}"
if ! command -v "$GMSAAS_BIN" >/dev/null 2>&1; then
  if [[ -x "$HOME/.venvs/gmsaas/bin/gmsaas" ]]; then
    GMSAAS_BIN="$HOME/.venvs/gmsaas/bin/gmsaas"
  elif [[ -x "$HOME/.local/bin/gmsaas" ]]; then
    GMSAAS_BIN="$HOME/.local/bin/gmsaas"
  fi
fi
if ! command -v "$GMSAAS_BIN" >/dev/null 2>&1; then
  echo "gmsaas is not in PATH (and not found at ~/.venvs/gmsaas/bin/gmsaas)." >&2
  echo "Install it with: python -m pip install --user gmsaas (or use a venv/pipx)." >&2
  exit 2
fi

API_KEYS_FILE="${API_KEYS_FILE:-$HOME/.api-keys}"
if [[ -z "${GENYMOTION_API_TOKEN:-}" && -z "${GENYMOTION_API_KEY:-}" && -f "$API_KEYS_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$API_KEYS_FILE"
fi

# Backwards-compat: some environments still export GENYMOTION_API_KEY.
if [[ -z "${GENYMOTION_API_TOKEN:-}" && -n "${GENYMOTION_API_KEY:-}" ]]; then
  export GENYMOTION_API_TOKEN="$GENYMOTION_API_KEY"
fi

UUID=""
if [[ -n "$INSTANCE_UUID" ]]; then
  UUID="$INSTANCE_UUID"
else
  UUID=$(
    "$GMSAAS_BIN" instances list \
      | awk -v name="$INSTANCE_NAME" 'NR>2 && $2==name && ($4=="ONLINE" || $4=="RUNNING") {print $1; exit}'
  )
  if [[ -z "$UUID" ]]; then
    UUID=$("$GMSAAS_BIN" instances list | awk 'NR>2 && ($4=="ONLINE" || $4=="RUNNING") {print $1; exit}')
  fi
  if [[ -z "$UUID" ]]; then
    echo "No running Genymotion SaaS instances found; starting one: $RECIPE_UUID as $INSTANCE_NAME ..." >&2

    START_ARGS=()
    if [[ -n "${MAX_RUN_DURATION_MINUTES}" ]]; then
      START_ARGS+=(--max-run-duration "${MAX_RUN_DURATION_MINUTES}")
    fi

    set +e
    START_OUT=$("$GMSAAS_BIN" instances start "${START_ARGS[@]}" "$RECIPE_UUID" "$INSTANCE_NAME" 2>&1)
    RET=$?
    set -e
    echo "$START_OUT" >&2
    if [[ $RET -ne 0 || "$START_OUT" == *"LICENSE_EXPIRED"* ]]; then
      echo "Unable to start instance (return=$RET). If this contains LICENSE_EXPIRED, renew your Genymotion SaaS license or use another account/token." >&2
      exit 3
    fi

    echo "Waiting for instance to appear in the running list..." >&2
    for _ in {1..90}; do
      UUID=$(
        "$GMSAAS_BIN" instances list \
          | awk -v name="$INSTANCE_NAME" 'NR>2 && $2==name && ($4=="ONLINE" || $4=="RUNNING") {print $1; exit}'
      )
      if [[ -n "$UUID" ]]; then
        break
      fi
      sleep 2
    done
  fi
fi
if [[ -z "$UUID" ]]; then
  echo "Failed to locate a running instance UUID." >&2
  exit 4
fi
echo "Instance UUID: $UUID" >&2

echo "Connecting ADB to $UUID ..." >&2
ADB_OUT=$("$GMSAAS_BIN" instances adbconnect "$UUID")
echo "$ADB_OUT" >&2
SERIAL=$(echo "$ADB_OUT" | tr -d '\r' | grep -Eo '([A-Za-z0-9_.-]+:[0-9]+)' | tail -n 1 || true)
if [[ -z "$SERIAL" ]]; then
  echo "Failed to extract ADB serial. Output: $ADB_OUT" >&2
  exit 5
fi

echo "Waiting for device $SERIAL ..." >&2
adb -s "$SERIAL" wait-for-device

printf 'GMSAAS_INSTANCE_UUID=%q\n' "$UUID"
printf 'GMSAAS_DEVICE_SERIAL=%q\n' "$SERIAL"
