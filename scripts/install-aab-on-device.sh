#!/bin/bash

AAB_PATH="$1"
DEVICE_ID="${3:-${GENYMOTION_DEV:-${ANDROID_SERIAL:-}}}"

if [[ -z "${AAB_PATH}" ]]; then
    echo "Usage: $0 <path-to.aab> [bundletool-version] [adb-serial]"
    exit 1
fi

TOOL_VERSION="${2:-1.15.5}"
TOOL_JAR="build/bundletool.jar"
if [[ ! -f "${TOOL_JAR}" ]]; then
    echo "Downloading bundle-tool version ${TOOL_VERSION}..."
    URL="https://github.com/google/bundletool/releases/download/${TOOL_VERSION}/bundletool-all-${TOOL_VERSION}.jar"
    wget --tries=5 --waitretry=5 --progress=dot:mega --output-document="${TOOL_JAR}" "${URL}"
fi

TEMP_DIR="$(mktemp -d)"
TEMP_APKS="${TEMP_DIR}/temp.apks"

echo "Building APK-set..."
java -jar "${TOOL_JAR}" build-apks --bundle="${AAB_PATH}" --output="${TEMP_APKS}"

ADB_BIN="$(which adb)"
echo "Installing to connected device (using ADB at '${ADB_BIN}')..."
INSTALL_ARGS=(install-apks --adb="${ADB_BIN}" --apks="${TEMP_APKS}")
if [[ -n "${DEVICE_ID}" ]]; then
    echo "Using device serial '${DEVICE_ID}'"
    INSTALL_ARGS+=(--device-id="${DEVICE_ID}")
fi
java -jar "${TOOL_JAR}" "${INSTALL_ARGS[@]}"
