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

if [[ ! -f "${ROOT_DIR}/scripts/gmsaas_start_and_connect.sh" ]]; then
  echo "Missing helper script: ${ROOT_DIR}/scripts/gmsaas_start_and_connect.sh" >&2
  exit 2
fi

eval "$("${ROOT_DIR}/scripts/gmsaas_start_and_connect.sh")"
UUID="${GMSAAS_INSTANCE_UUID:-}"
SERIAL="${GMSAAS_DEVICE_SERIAL:-}"
if [[ -z "$UUID" || -z "$SERIAL" ]]; then
  echo "Failed to start/connect a Genymotion SaaS instance." >&2
  exit 3
fi
echo "Instance UUID: $UUID"
echo "Device serial: $SERIAL"

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
