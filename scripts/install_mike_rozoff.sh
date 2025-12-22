#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK_PATH="$ROOT_DIR/ime/app/build/outputs/apk/nsk/debug/app-nsk-debug.apk"

echo "🛠  Building NewSoftKeyboard nskDebug APK..."
(
  cd "$ROOT_DIR"
  ./gradlew :ime:app:assembleNskDebug >/dev/null
)

if [[ ! -f "$APK_PATH" ]]; then
  echo "❌ APK artifact not found at $APK_PATH"
  exit 1
fi

SERIAL="${GENYMOTION_DEV:-}"
if [[ -z "$SERIAL" ]]; then
  # Best-effort autodetect; prefer Genymotion SaaS (localhost:*).
  mapfile -t serials < <(adb devices | awk 'NR>1 && $2=="device" {print $1}')
  for s in "${serials[@]}"; do [[ "$s" == localhost:* ]] && SERIAL="$s" && break; done
  if [[ -z "$SERIAL" ]]; then for s in "${serials[@]}"; do [[ "$s" == 127.0.0.1:* ]] && SERIAL="$s" && break; done; fi
  if [[ -z "$SERIAL" ]]; then for s in "${serials[@]}"; do [[ "$s" == emulator-* ]] && SERIAL="$s" && break; done; fi
  if [[ -z "$SERIAL" && "${#serials[@]}" -eq 1 ]]; then SERIAL="${serials[0]}"; fi
fi
ADB_ARGS=()
if [[ -n "$SERIAL" ]]; then
  ADB_ARGS=(-s "$SERIAL")
fi

echo "🔌 Waiting for an adb device${SERIAL:+ ($SERIAL)}..."
adb "${ADB_ARGS[@]}" wait-for-device

echo "📦 Installing $APK_PATH"
adb "${ADB_ARGS[@]}" install -r "$APK_PATH"

echo "✅ Build installed."
