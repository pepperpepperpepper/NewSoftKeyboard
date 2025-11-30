#!/usr/bin/env bash
#
# Helper entrypoint for running the neural fine-tuning + export workflow.
# Usage:
#   DATASET=/path/to/text.txt OUTPUT_ROOT=/path/to/out \\
#     retraining/scripts/neural_pipeline.sh distilgpt2
#

set -euo pipefail

MODEL_ID="${1:-distilgpt2}"
DATASET="${DATASET:-/mnt/subtitled/datasets/simple_enwiki/simple_wikipedia/simple_wikipedia.normalized.txt}"
OUTPUT_ROOT="${OUTPUT_ROOT:-/mnt/subtitled/datasets/simple_enwiki/artifacts/neural/${MODEL_ID//\//_}}"
CHECKPOINT_DIR="${OUTPUT_ROOT}/checkpoints"
ONNX_DIR="${OUTPUT_ROOT}/onnx"
ONNX_PATH="${ONNX_DIR}/${MODEL_ID//\//_}.onnx"
QUANT_PATH="${ONNX_DIR}/${MODEL_ID//\//_}_int8.onnx"

mkdir -p "${CHECKPOINT_DIR}"

EPOCHS="${EPOCHS:-1}"
BATCH_SIZE="${BATCH_SIZE:-4}"
BLOCK_SIZE="${BLOCK_SIZE:-256}"
LEARNING_RATE="${LEARNING_RATE:-5e-5}"
MAX_STEPS="${MAX_STEPS:--1}"

python3 "$(dirname "$0")/neural_finetune.py" \
  --model-id "${MODEL_ID}" \
  --dataset "${DATASET}" \
  --output-dir "${CHECKPOINT_DIR}" \
  --epochs "${EPOCHS}" \
  --batch-size "${BATCH_SIZE}" \
  --block-size "${BLOCK_SIZE}" \
  --learning-rate "${LEARNING_RATE}" \
  --max-steps "${MAX_STEPS}" \
  --onnx-output "${ONNX_PATH}" \
  --quantized-output "${QUANT_PATH}"

echo "Neural pipeline finished. Check ${OUTPUT_ROOT} for checkpoints and ONNX exports."
