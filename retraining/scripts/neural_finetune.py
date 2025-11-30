#!/usr/bin/env python3
"""
Minimal Hugging Face fine-tuning scaffold for DistilGPT-2 / TinyLlama on the
normalized Simple English Wikipedia corpus.
"""

from __future__ import annotations

import argparse
import itertools
import sys
from pathlib import Path
from typing import Optional

from datasets import Dataset, load_dataset
from transformers import (
    AutoModelForCausalLM,
    AutoTokenizer,
    DataCollatorForLanguageModeling,
    Trainer,
    TrainingArguments,
    set_seed,
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Fine-tune GPT-style models on normalized text.")
    parser.add_argument("--model-id", required=True, help="Base model ID (e.g., distilgpt2, TinyLlama/TinyLlama-1.1B).")
    parser.add_argument("--dataset", required=True, help="Path to normalized text file.")
    parser.add_argument("--output-dir", required=True, help="Directory for checkpoints/export.")
    parser.add_argument("--epochs", type=int, default=1)
    parser.add_argument("--batch-size", type=int, default=8)
    parser.add_argument("--block-size", type=int, default=256)
    parser.add_argument("--learning-rate", type=float, default=5e-5)
    parser.add_argument("--seed", type=int, default=1337)
    parser.add_argument("--max-steps", type=int, default=-1)
    parser.add_argument("--push-to-hub", action="store_true")
    parser.add_argument(
        "--onnx-output",
        help="Optional output path for exporting the fine-tuned model to ONNX (requires transformers[onnx] extras).",
    )
    parser.add_argument(
        "--quantized-output",
        help="Optional output path for dynamic-quantized ONNX export (requires onnxruntime).",
    )
    parser.add_argument(
        "--onnx-opset",
        type=int,
        default=13,
        help="Opset version to use when exporting ONNX.",
    )
    return parser.parse_args()


def load_text_dataset(path: str) -> Dataset:
    ds = load_dataset("text", data_files={"train": path})
    return ds["train"]


def tokenize_and_chunk(dataset: Dataset, tokenizer: AutoTokenizer, block_size: int) -> Dataset:
    tokenizer.model_max_length = block_size
    tokenizer.pad_token = tokenizer.eos_token

    def tokenize(batch: dict) -> dict:
        return tokenizer(batch["text"])

    tokenized = dataset.map(tokenize, batched=True, remove_columns=["text"])

    def group(batch: dict) -> dict:
        concatenated = sum(batch["input_ids"], [])
        total_length = (len(concatenated) // block_size) * block_size
        concatenated = concatenated[:total_length]
        result = {
            "input_ids": [concatenated[i : i + block_size] for i in range(0, total_length, block_size)]
        }
        result["attention_mask"] = [
            [1] * block_size for _ in range(0, total_length, block_size)
        ]
        return result

    return tokenized.map(group, batched=True)


def main() -> int:
    args = parse_args()
    set_seed(args.seed)

    dataset = load_text_dataset(args.dataset)
    tokenizer = AutoTokenizer.from_pretrained(args.model_id, use_fast=True)
    tokenized = tokenize_and_chunk(dataset, tokenizer, args.block_size)

    model = AutoModelForCausalLM.from_pretrained(args.model_id)
    data_collator = DataCollatorForLanguageModeling(tokenizer=tokenizer, mlm=False)

    training_args = TrainingArguments(
        output_dir=args.output_dir,
        overwrite_output_dir=True,
        num_train_epochs=args.epochs,
        per_device_train_batch_size=args.batch_size,
        learning_rate=args.learning_rate,
        weight_decay=0.01,
        logging_steps=100,
        save_strategy="epoch",
        seed=args.seed,
        max_steps=args.max_steps,
        push_to_hub=args.push_to_hub,
        fp16=False,
    )

    trainer = Trainer(
        model=model,
        args=training_args,
        train_dataset=tokenized,
        data_collator=data_collator,
    )
    trainer.train()
    trainer.save_model(args.output_dir)
    tokenizer.save_pretrained(args.output_dir)

    onnx_exported = False
    if args.onnx_output:
        onnx_exported = export_to_onnx(
            model,
            tokenizer,
            Path(args.onnx_output),
            args.block_size,
            args.onnx_opset,
        )
    quantized_ok = False
    if args.quantized_output:
        quantized_ok = quantize_onnx(
            Path(args.quantized_output), Path(args.onnx_output) if args.onnx_output else None
        )

    print(f"Fine-tuning complete; artifacts stored in {args.output_dir}")
    if args.onnx_output:
        status = "created" if onnx_exported else "skipped"
        print(f"ONNX export {status}: {args.onnx_output}")
    if args.quantized_output and quantized_ok:
        print(f"Quantized ONNX written to {args.quantized_output}")
    return 0


def export_to_onnx(model, tokenizer, output_path: Path, block_size: int, opset: int) -> bool:
    try:
        from transformers.onnx import FeaturesManager
    except ImportError as exc:
        print(
            f"Skipping ONNX export because transformers.onnx dependencies are missing: {exc}",
            file=sys.stderr,
        )
        return False

    output_path.parent.mkdir(parents=True, exist_ok=True)
    feature = "causal-lm"
    try:
        model_kind, onnx_config_cls = FeaturesManager.check_supported_model_or_raise(model, feature=feature)
    except Exception as exc:  # pylint: disable=broad-except
        print(f"Model {model.__class__.__name__} not supported for ONNX export: {exc}", file=sys.stderr)
        return False

    onnx_config = onnx_config_cls(model.config)
    print(
        f"Exporting {model_kind} to ONNX at {output_path} (opset {opset}) "
        "using the legacy torch.onnx exporter."
    )
    try:
        _export_with_torchscript(tokenizer, model, onnx_config, opset, output_path)
        return True
    except Exception as exc:  # pylint: disable=broad-except
        print(f"Legacy ONNX export failed: {exc}", file=sys.stderr)
        return False


def quantize_onnx(output_path: Path, source_path: Optional[Path]) -> bool:
    try:
        from onnxruntime.quantization import QuantType, quantize_dynamic
    except ImportError as exc:
        print(f"Skipping ONNX quantization because onnxruntime is missing: {exc}", file=sys.stderr)
        return False

    if source_path is None or not source_path.exists():
        print("Quantization requested but no ONNX export path is available; skipping.", file=sys.stderr)
        return False

    output_path.parent.mkdir(parents=True, exist_ok=True)
    print(f"Quantizing ONNX model to {output_path}")
    quantize_dynamic(str(source_path), str(output_path), weight_type=QuantType.QInt8)
    return True


def _export_with_torchscript(tokenizer, model, onnx_config, opset: int, output_path: Path) -> None:
    """Mirror transformers.onnx.convert.export_pytorch but force torchscript exporter."""
    from transformers.utils import TensorType

    import torch

    model.config.return_dict = True
    model.eval()
    dummy_inputs = onnx_config.generate_dummy_inputs(tokenizer, framework=TensorType.PYTORCH)

    input_names = list(onnx_config.inputs.keys())
    output_names = list(onnx_config.outputs.keys())
    dynamic_axes = dict(itertools.chain(onnx_config.inputs.items(), onnx_config.outputs.items()))

    onnx_config.patch_ops()
    try:
        with torch.no_grad():
            torch.onnx.export(  # type: ignore[call-arg]
                model,
                (dummy_inputs,),
                output_path.as_posix(),
                input_names=input_names,
                output_names=output_names,
                dynamic_axes=dynamic_axes,
                do_constant_folding=True,
                opset_version=opset,
                dynamo=True,
            )
    finally:
        onnx_config.restore_ops()


if __name__ == "__main__":
    sys.exit(main())
