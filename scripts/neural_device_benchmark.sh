#!/usr/bin/env bash
#
# On-device warm per-keystroke prefix-completion latency benchmark (neural_completion_plan.md Path B).
#
# Builds + installs the engine-neural instrumentation APK, stages the neural model into the test
# app's external files dir, and runs the benchmark via `am instrument` (NOT gradle
# connectedAndroidTest, which uninstalls the APK afterward and would wipe the staged model).
#
# Usage:
#   scripts/neural_device_benchmark.sh                 # report numbers only
#   ENFORCE_LATENCY_BUDGET=1 scripts/neural_device_benchmark.sh   # also fail if warm p50 > 25ms
#
# Pick a device with ANDROID_SERIAL=... when more than one is attached. Run against a REAL arm64
# device for shippable numbers; an emulator's translated timings are not representative and should
# not be gated with ENFORCE_LATENCY_BUDGET.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

DEVICE_ID="${ANDROID_SERIAL:-}"
if [[ -z "${DEVICE_ID}" ]]; then
  DEVICE_ID="$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')"
fi
if [[ -z "${DEVICE_ID}" ]]; then
  echo "No adb device found. Attach an arm64 device (or set ANDROID_SERIAL)." >&2
  exit 1
fi
ADB=(adb -s "${DEVICE_ID}")

ABI="$("${ADB[@]}" shell getprop ro.product.cpu.abi | tr -d '\r')"
echo "Device: ${DEVICE_ID} (${ABI})"
if [[ "${ABI}" != arm64* ]]; then
  echo "WARNING: device ABI is '${ABI}', not arm64 — latencies will not be representative." >&2
fi

TEST_PKG="wtf.uhoh.newsoftkeyboard.engine.neural.test"
RUNNER="androidx.test.runner.AndroidJUnitRunner"
TEST_CLASS="wtf.uhoh.newsoftkeyboard.engine.neural.NeuralPrefixCompletionLatencyInstrumentationTest"
MODEL_SRC="${ROOT_DIR}/engine-neural/build/neural_test_model"
MODEL_DEST="/sdcard/Android/data/${TEST_PKG}/files/neural-model"

echo "==> Building + installing instrumentation APK (also fetches the test model)..."
./gradlew :engine-neural:installDebugAndroidTest

if [[ ! -f "${MODEL_SRC}/model_int8.onnx" ]]; then
  echo "Model not found at ${MODEL_SRC}; fetching..."
  ./gradlew :engine-neural:fetchNeuralTestModel
fi

echo "==> Staging model on device at ${MODEL_DEST}..."
# Start from a clean, freshly-created dir: pushing into a pre-existing or already-relaxed-perms dir
# on emulated storage can trip a post-copy fchown that drops the file. Fresh dir -> push -> chmod.
"${ADB[@]}" shell rm -rf "${MODEL_DEST}"
"${ADB[@]}" shell mkdir -p "${MODEL_DEST}"
for f in model_int8.onnx vocab.json merges.txt; do
  # Data transfers fine even when adb returns non-zero on the cosmetic post-copy fchown into the
  # app-owned external dir, so don't let that abort the script — verify by existence below instead.
  "${ADB[@]}" push "${MODEL_SRC}/${f}" "${MODEL_DEST}/${f}" >/dev/null 2>&1 || true
done
# The dir is created by the adb 'shell' user; relax perms so the app's uid can traverse it.
"${ADB[@]}" shell chmod 777 "${MODEL_DEST}" || true
for f in model_int8.onnx vocab.json merges.txt; do
  if ! "${ADB[@]}" shell ls "${MODEL_DEST}/${f}" >/dev/null 2>&1; then
    echo "Failed to stage ${f} on device." >&2
    exit 1
  fi
done

ENFORCE_ARG=()
if [[ "${ENFORCE_LATENCY_BUDGET:-0}" == "1" ]]; then
  echo "==> Latency budget enforcement ON (warm p50 must be <= 25ms)."
  ENFORCE_ARG=(-e enforceLatencyBudget 1)
fi

echo "==> Running benchmark via am instrument..."
"${ADB[@]}" logcat -c || true
set +e
"${ADB[@]}" shell am instrument -w -r \
  "${ENFORCE_ARG[@]}" \
  -e class "${TEST_CLASS}" \
  "${TEST_PKG}/${RUNNER}"
INSTR_STATUS=$?
set -e

echo
echo "==> Latency report:"
"${ADB[@]}" logcat -d | sed -n '/On-device prefix-completion latency/,/warm_keystroke_ms/p' | tail -6

exit "${INSTR_STATUS}"
