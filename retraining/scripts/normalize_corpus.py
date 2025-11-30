#!/usr/bin/env python3
"""
Simple corpus normalizer to help rebuild mixed-case language models.

Reads UTF-8 text and optionally applies heuristic sentence casing while preserving
common acronyms supplied via command-line flags.
"""

from __future__ import annotations

import argparse
import io
import re
import sys
from typing import Iterable, Set


SENTENCE_END_REGEX = re.compile(r"([.!?])(\s+)")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Normalize corpus casing for LM retraining.")
    parser.add_argument(
        "input",
        help="Input text file. Use '-' to read from stdin.",
    )
    parser.add_argument(
        "--output",
        "-o",
        default="-",
        help="Output file path (default: stdout).",
    )
    parser.add_argument(
        "--sentence-case",
        action="store_true",
        help="After lowercasing, apply heuristic sentence casing to the first letter."
    )
    parser.add_argument(
        "--acronym",
        action="append",
        default=[],
        help="Acronym to keep uppercase (can be provided multiple times).",
    )
    parser.add_argument(
        "--titlecase",
        action="append",
        default=[],
        help="Word to force Title Case (first letter uppercase) wherever it appears.",
    )
    return parser.parse_args()


def load_stream(path: str, mode: str) -> io.TextIOBase:
    if path == "-":
        return sys.stdin if "r" in mode else sys.stdout
    return open(path, mode, encoding="utf-8")


def normalize_line(
    line: str,
    acronyms: Set[str],
    titlecase_words: Set[str],
    sentence_case: bool,
) -> str:
    stripped = line.rstrip("\n")
    lower = stripped.lower()
    if not sentence_case:
        normalized = lower
    else:
        normalized = apply_sentence_case(lower)
    normalized = restore_titlecase(normalized, titlecase_words)
    return restore_acronyms(normalized, acronyms)


def apply_sentence_case(text: str) -> str:
    # Capitalize the very first alphabetical character and characters after sentence terminators.
    chars = list(text)
    capitalize_next = True

    for index, char in enumerate(chars):
        if capitalize_next and char.isalpha():
            chars[index] = char.upper()
            capitalize_next = False
        elif char in ".!?":
            capitalize_next = True

    return "".join(chars)


def restore_acronyms(text: str, acronyms: Iterable[str]) -> str:
    result = text
    for acronym in acronyms:
        lower = acronym.lower()
        if not lower:
            continue
        result = replace_whole_word(result, lower, acronym.upper())
    return result


def restore_titlecase(text: str, titlecase_words: Iterable[str]) -> str:
    result = text
    for word in titlecase_words:
        lower = word.lower()
        if not lower:
            continue
        replacement = word[:1].upper() + word[1:].lower()
        result = replace_whole_word(result, lower, replacement)
    return result


def replace_whole_word(text: str, lower_word: str, replacement: str) -> str:
    pattern = r"\b{}\b".format(re.escape(lower_word))
    return re.sub(pattern, replacement, text)


def normalize_corpus(
    input_stream: io.TextIOBase,
    output_stream: io.TextIOBase,
    acronyms: Set[str],
    titlecase_words: Set[str],
    sentence_case: bool,
) -> None:
    for line in input_stream:
        normalized = normalize_line(line, acronyms, titlecase_words, sentence_case)
        output_stream.write(normalized + "\n")


def main() -> int:
    args = parse_args()
    acronyms = {item.strip() for item in args.acronym if item.strip()}
    titlecase_words = {item.strip() for item in args.titlecase if item.strip()}

    with load_stream(args.input, "r") as input_stream:
        with load_stream(args.output, "w") as output_stream:
            normalize_corpus(
                input_stream,
                output_stream,
                acronyms,
                titlecase_words,
                args.sentence_case,
            )
    return 0


if __name__ == "__main__":
    sys.exit(main())
