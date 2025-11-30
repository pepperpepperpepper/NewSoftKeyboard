#!/usr/bin/env python3
"""
Convert the Simple English Wikipedia parquet dump to a newline-delimited text file.

Usage:
    python export_simple_enwiki.py \
        --input /mnt/.../simple_wikipedia/data/train-00000-of-00001-....parquet \
        --output /mnt/.../simple_wikipedia/simple_wikipedia.txt
"""

from __future__ import annotations

import argparse
import pathlib
import sys
from typing import Optional

import pyarrow.parquet as pq


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Export Simple English Wikipedia parquet to text.")
    parser.add_argument("--input", required=True, help="Path to the parquet file.")
    parser.add_argument("--output", required=True, help="Destination text file.")
    parser.add_argument(
        "--limit",
        type=int,
        default=None,
        help="Optional max number of rows to export (for smoke tests).",
    )
    return parser.parse_args()


def export_parquet(input_path: str, output_path: str, limit: Optional[int] = None) -> None:
    pq_file = pq.ParquetFile(input_path)
    rows_written = 0
    output_dir = pathlib.Path(output_path).parent
    output_dir.mkdir(parents=True, exist_ok=True)

    with open(output_path, "w", encoding="utf-8") as text_out:
        for batch in pq_file.iter_batches(batch_size=2048, columns=["text"]):
            column = batch.column(0)
            for value in column.to_pylist():
                if value is None:
                    continue
                text_out.write(value.strip() + "\n")
                rows_written += 1
                if limit is not None and rows_written >= limit:
                    return


def main() -> int:
    args = parse_args()
    export_parquet(args.input, args.output, args.limit)
    return 0


if __name__ == "__main__":
    sys.exit(main())
