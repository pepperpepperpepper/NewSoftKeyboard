#!/usr/bin/env bash
#
# Scaffold script for rebuilding KenLM/VariKN n-gram models with casing preserved.
# This is a template; adjust variables before running in a reproducible environment.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RETRAINING_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Inputs:
#   CORPUS        - path to mixed-case training text (one sentence per line recommended)
#   OUTPUT_DIR    - directory where rebuilt artifacts will be written
# Optional:
#   KENLM_BIN_DIR - directory containing kenlm binaries (build_binary, lmplz, etc.)
#   PRUNE_VALUE   - single pruning threshold for lmplz (defaults to 1e-7)
#   PRUNE_SCHEDULE- space-separated list of per-order prune thresholds (overrides PRUNE_VALUE)

CORPUS="${1:-}"
OUTPUT_DIR="${2:-}"

if [[ -z "${CORPUS}" || -z "${OUTPUT_DIR}" ]]; then
  echo "Usage: $0 /path/to/corpus.txt /path/to/output_dir" >&2
  exit 1
fi

if [[ ! -f "${CORPUS}" ]]; then
  echo "Corpus file not found: ${CORPUS}" >&2
  exit 2
fi

mkdir -p "${OUTPUT_DIR}"

KENLM_BIN_DIR="${KENLM_BIN_DIR:-${HOME}/suggestions/tools/kenlm/bin}"
LMPLZ="${KENLM_BIN_DIR}/lmplz"
BUILD_BINARY="${KENLM_BIN_DIR}/build_binary"
PRUNE_VALUE="${PRUNE_VALUE:-1e-7}"
PRUNE_SCHEDULE="${PRUNE_SCHEDULE:-}"

if [[ ! -x "${LMPLZ}" ]] || [[ ! -x "${BUILD_BINARY}" ]]; then
  cat >&2 <<EOF
KenLM binaries not found or not executable.
Expected to find lmplz and build_binary under ${KENLM_BIN_DIR}.
Set KENLM_BIN_DIR to point at your KenLM build before invoking this script.
EOF
  exit 3
fi

ORDER="${ORDER:-3}"
VOCAB_PATH="${OUTPUT_DIR}/model.vocab"
ARPA_PATH="${OUTPUT_DIR}/model.arpa"
BIN_PATH="${OUTPUT_DIR}/model.binary"

PRUNE_FLAG=(--prune)
if [[ -n "${PRUNE_SCHEDULE}" ]]; then
  echo "Using per-order prune schedule: ${PRUNE_SCHEDULE}"
  # shellcheck disable=SC2206 # intentional word splitting for schedule list
  PRUNE_VALUES=(${PRUNE_SCHEDULE})
  PRUNE_FLAG+=("${PRUNE_VALUES[@]}")
else
  echo "Using single prune value: ${PRUNE_VALUE}"
  PRUNE_FLAG+=("${PRUNE_VALUE}")
fi

echo "Generating ${ORDER}-gram ARPA model at ${ARPA_PATH}"
"${LMPLZ}" \
  --order "${ORDER}" \
  --text "${CORPUS}" \
  --arpa "${ARPA_PATH}" \
  "${PRUNE_FLAG[@]}"

echo "Extracting vocabulary to ${VOCAB_PATH}"
grep -v '^\\' "${ARPA_PATH}" | awk '{print $1}' | sort -u > "${VOCAB_PATH}"

echo "Building KenLM binary at ${BIN_PATH}"
"${BUILD_BINARY}" "${ARPA_PATH}" "${BIN_PATH}"

echo "Done. Files generated:"
ls -lh "${OUTPUT_DIR}"
