#!/bin/bash

set -euo pipefail

# =============================================================================
# NewSoftKeyboard English-Only Setup Script
# =============================================================================
# Builds and installs the NewSoftKeyboard debug APK (English only), avoiding
# unnecessary language packs.
#
# Usage: ./setup_newsoftkeyboard_english.sh
# =============================================================================

echo "Setting up NewSoftKeyboard (English only)..."

if ! timeout 10s adb devices | grep -q "device$"; then
  echo "No Android device connected. Please connect a device and enable USB debugging."
  exit 1
fi

echo "Device connected"

echo "Building NewSoftKeyboard main app..."
GRADLE_USER_HOME="${GRADLE_USER_HOME:-/mnt/finished/.gradle}" ./gradlew :ime:app:assembleNskDebug -x lint

echo "Installing NewSoftKeyboard..."
timeout 90s adb install -r ./ime/app/build/outputs/apk/nsk/debug/app-nsk-debug.apk

echo "Verifying installation..."
INSTALLED_PACKAGES="$(timeout 30s adb shell pm list packages | grep -i "wtf\\.uhoh\\.newsoftkeyboard" || true)"

if [[ -n "${INSTALLED_PACKAGES}" ]]; then
  echo "NewSoftKeyboard installed successfully:"
  echo "${INSTALLED_PACKAGES}"
  echo ""
  echo "Next steps:"
  echo "1. Go to Settings → System → Keyboard"
  echo "2. Enable NewSoftKeyboard"
  echo "3. Switch to NewSoftKeyboard in any text input app"
else
  echo "Installation verification failed. Please check manually."
  exit 1
fi
