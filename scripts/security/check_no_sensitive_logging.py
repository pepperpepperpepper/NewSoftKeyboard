#!/usr/bin/env python3

import argparse
import re
import sys
from pathlib import Path


LOG_CALL_RE = re.compile(r"(?:Logger|Log)\.(?:v|d|i|w|e)\s*\((?:.|\n)*?\);\s*", re.MULTILINE)

# Heuristic allow-list: these identifiers are commonly used to hold sensitive user content
# (typed text, suggestions, prompts, transcription results, secrets).
SENSITIVE_IDENTIFIERS = [
    "text",
    "typedText",
    "word",
    "candidate",
    "suggestion",
    "subWord",
    "result",
    "transcription",
    "prompt",
    "apiKey",
    "api_key",
    "token",
    "secret",
    "password",
    "mLastRecognitionResult",
]

EXCLUDED_RELATIVE_PATHS = {
    "ime/base/src/main/java/wtf/uhoh/newsoftkeyboard/base/utils/Logger.java",
    "ime/app/src/debug/java/wtf/uhoh/newsoftkeyboard/app/debug/LogCatLogProvider.java",
}


def _strip_string_literals(java_like: str) -> str:
    """Best-effort removal of string/char literals (keeps newlines)."""
    out = []
    i = 0
    n = len(java_like)
    in_double = False
    in_single = False
    in_triple = False

    while i < n:
        ch = java_like[i]

        if in_triple:
            if java_like.startswith('"""', i):
                out.append('"""')
                i += 3
                in_triple = False
                continue
            # Preserve newlines for line mapping
            out.append("\n" if ch == "\n" else " ")
            i += 1
            continue

        if in_double:
            if ch == "\\" and i + 1 < n:
                # escape sequence inside string
                out.append(" ")
                out.append(" ")
                i += 2
                continue
            if ch == '"':
                out.append('"')
                in_double = False
                i += 1
                continue
            out.append("\n" if ch == "\n" else " ")
            i += 1
            continue

        if in_single:
            if ch == "\\" and i + 1 < n:
                out.append(" ")
                out.append(" ")
                i += 2
                continue
            if ch == "'":
                out.append("'")
                in_single = False
                i += 1
                continue
            out.append("\n" if ch == "\n" else " ")
            i += 1
            continue

        if java_like.startswith('"""', i):
            out.append('"""')
            i += 3
            in_triple = True
            continue

        if ch == '"':
            out.append('"')
            i += 1
            in_double = True
            continue

        if ch == "'":
            out.append("'")
            i += 1
            in_single = True
            continue

        out.append(ch)
        i += 1

    return "".join(out)


def _remove_safe_usages(call: str, identifier: str) -> str:
    patterns = [
        rf"\b{re.escape(identifier)}\s*\.\s*length\s*(?:\(\s*\))?",
        rf"\b{re.escape(identifier)}\s*\.\s*size\s*(?:\(\s*\))?",
        rf"TextUtils\s*\.\s*isEmpty\s*\(\s*{re.escape(identifier)}\s*\)",
        rf"\b{re.escape(identifier)}\s*\.\s*isEmpty\s*\(\s*\)",
        rf"\b{re.escape(identifier)}\s*(?:==|!=)\s*null\b",
    ]
    sanitized = call
    for pattern in patterns:
        sanitized = re.sub(pattern, "SAFE", sanitized)
    return sanitized


def _is_relevant_source(path: Path) -> bool:
    if path.suffix not in (".java", ".kt"):
        return False
    path_str = str(path).replace("\\", "/")
    if "/src/test/" in path_str or "/src/androidTest/" in path_str:
        return False
    return True


def _find_issues(path: Path, content: str) -> list[tuple[int, str]]:
    issues: list[tuple[int, str]] = []
    for match in LOG_CALL_RE.finditer(content):
        call = match.group(0)
        # Compute start line number of the logging call (1-based).
        line_no = content.count("\n", 0, match.start()) + 1

        call_no_strings = _strip_string_literals(call)

        if re.search(r"\.getAbsolutePath\s*\(", call_no_strings) or re.search(
            r"\.getPath\s*\(", call_no_strings
        ):
            issues.append((line_no, "Logs a file path via getPath/getAbsolutePath"))
            continue

        for identifier in SENSITIVE_IDENTIFIERS:
            sanitized = _remove_safe_usages(call_no_strings, identifier)
            # Avoid false-positives for method calls like host.word() by excluding direct invocations.
            if re.search(rf"\b{re.escape(identifier)}\b(?!\s*\()", sanitized):
                issues.append((line_no, f"Logs potentially sensitive identifier '{identifier}'"))
                break

    return issues


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Fails if Java/Kotlin sources appear to log sensitive user content."
    )
    parser.add_argument(
        "--root",
        default=None,
        help="Repo root directory (defaults to auto-detect from script location).",
    )
    args = parser.parse_args()

    repo_root = (
        Path(args.root).resolve()
        if args.root is not None
        else Path(__file__).resolve().parents[2]
    )

    scan_roots = [repo_root / "ime", repo_root / "api"]

    all_issues: list[tuple[Path, int, str]] = []
    for root in scan_roots:
        if not root.exists():
            continue
        for path in root.rglob("*"):
            if not path.is_file() or not _is_relevant_source(path):
                continue
            rel_path_str = str(path.relative_to(repo_root)).replace("\\", "/")
            if rel_path_str in EXCLUDED_RELATIVE_PATHS:
                continue
            try:
                content = path.read_text(encoding="utf-8", errors="replace")
            except Exception:
                continue
            for line_no, reason in _find_issues(path, content):
                all_issues.append((path, line_no, reason))

    if not all_issues:
        print("OK: No obvious sensitive logging patterns found.")
        return 0

    print("ERROR: Potential sensitive logging detected:\n")
    for path, line_no, reason in all_issues:
        rel = path.relative_to(repo_root) if path.is_relative_to(repo_root) else path
        print(f"{rel}:{line_no}: {reason}")
    print("\nPlease redact content (log only length/booleans/ids) or remove the log line.")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
