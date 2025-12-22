#!/usr/bin/env bash
set -euo pipefail

# Starts a Genymotion SaaS device, connects ADB, installs New Soft Keyboard and the
# Mike Rozoff add-on, and enables the IME.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

RECIPE_UUID="${RECIPE_UUID:-9074ccc1-7aba-4c9b-b615-e69ef389738c}" # Android 14.0 - Genymotion Phone
INSTANCE_NAME="${INSTANCE_NAME:-nsk-android14}"
INSTANCE_UUID="${INSTANCE_UUID:-}"
ASK_APK="${ASK_APK:-$ROOT_DIR/ime/app/build/outputs/apk/nsk/debug/app-nsk-debug.apk}"
ROZOFF_APK="${ROZOFF_APK:-$HOME/mike-rozoff-anysoftkeyboard-addon/build/outputs/apk/debug/app-debug.apk}"
INSTALL_ROZOFF="${INSTALL_ROZOFF:-auto}"

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
  LINE=$("$GMSAAS_BIN" instances list | awk -v name="$INSTANCE_NAME" 'NR>2 && $2==name {print $0}')
  if [[ -n "$LINE" ]]; then
    UUID=$(echo "$LINE" | awk '{print $1}')
  else
    echo "Starting Genymotion SaaS instance: $RECIPE_UUID as $INSTANCE_NAME ..."
    set +e
    START_OUT=$("$GMSAAS_BIN" instances start "$RECIPE_UUID" "$INSTANCE_NAME" 2>&1)
    RET=$?
    set -e
    echo "$START_OUT"
    if [[ $RET -ne 0 || "$START_OUT" == *"LICENSE_EXPIRED"* ]]; then
      echo "Unable to start instance (return=$RET). If this contains LICENSE_EXPIRED, renew your Genymotion SaaS license or use another account/token." >&2
      exit 3
    fi

    echo "Waiting for instance to appear in the list..."
    for _ in {1..60}; do
      LINE=$("$GMSAAS_BIN" instances list | awk -v name="$INSTANCE_NAME" 'NR>2 && $2==name {print $0}')
      if [[ -n "$LINE" ]]; then
        UUID=$(echo "$LINE" | awk '{print $1}')
        break
      fi
      sleep 2
    done
  fi
fi
if [[ -z "$UUID" ]]; then
  echo "Failed to locate the instance UUID by name ($INSTANCE_NAME)." >&2
  exit 4
fi
echo "Instance UUID: $UUID"

echo "Connecting ADB to $UUID ..."
ADB_OUT=$("$GMSAAS_BIN" instances adbconnect "$UUID")
echo "$ADB_OUT"
SERIAL=$(echo "$ADB_OUT" | tr -d '\r' | grep -Eo '([A-Za-z0-9_.-]+:[0-9]+)' | tail -n 1 || true)
if [[ -z "$SERIAL" ]]; then
  echo "Failed to extract ADB serial. Output: $ADB_OUT" >&2
  exit 5
fi

echo "Waiting for device $SERIAL ..."
adb -s "$SERIAL" wait-for-device

if [[ ! -f "$ASK_APK" ]]; then
  echo "Host APK not found: $ASK_APK. Build it first: ./gradlew :ime:app:assembleNskDebug" >&2
  exit 6
fi
echo "Installing New Soft Keyboard ($ASK_APK) …"
adb -s "$SERIAL" install -r "$ASK_APK"

installRozoff="$(echo "$INSTALL_ROZOFF" | tr '[:upper:]' '[:lower:]')"
if [[ "$installRozoff" != "0" && "$installRozoff" != "false" && "$installRozoff" != "no" ]]; then
  if [[ -f "$ROZOFF_APK" ]]; then
    echo "Installing Mike Rozoff add-on ($ROZOFF_APK) …"
    adb -s "$SERIAL" install -r "$ROZOFF_APK"
  elif [[ "$installRozoff" == "1" || "$installRozoff" == "true" || "$installRozoff" == "yes" ]]; then
    echo "Rozoff add-on APK not found: $ROZOFF_APK. Build it first in ~/mike-rozoff-anysoftkeyboard-addon: ./gradlew assembleDebug" >&2
    exit 7
  else
    echo "Skipping Mike Rozoff add-on (not found at $ROZOFF_APK)."
  fi
fi

echo "Enabling and setting default IME …"
if [[ -z "${IME_COMPONENT:-}" ]]; then
  IME_PACKAGE="${IME_PACKAGE:-wtf.uhoh.newsoftkeyboard}"
  IME_SERVICE_CLASS="${IME_SERVICE_CLASS:-.NewSoftKeyboardService}"
  IME_COMPONENT="${IME_PACKAGE}/${IME_SERVICE_CLASS}"
fi
adb -s "$SERIAL" shell ime enable "$IME_COMPONENT" || true
adb -s "$SERIAL" shell ime set "$IME_COMPONENT" || true

echo "Done. Device serial: $SERIAL"
echo "Tip: export GENYMOTION_DEV=$SERIAL"
