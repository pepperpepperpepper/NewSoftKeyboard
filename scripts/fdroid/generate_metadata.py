#!/usr/bin/env python3
"""
Generate metadata/wtf.uhoh.newsoftkeyboard.yml from the APKs present in repo/ and archive/.

The script is intentionally self contained and avoids extra dependencies. It:
 - Scans repo/ and archive/ (following symlinks) for wtf.uhoh.newsoftkeyboard_*.apk
 - Extracts versionCode + versionName via `aapt dump badging`
 - Emits a minimal static metadata file with KeepAll/None modes
 - Sets CurrentVersion/Code to $CURRENT_VERSION_CODE if provided, otherwise the highest code found

Usage:
  CURRENT_VERSION_CODE=15086 ./scripts/fdroid/generate_metadata.py

Prereqs: aapt in PATH (or set AAPT env to an explicit binary).
Run from the fdroid repo root (/home/arch/fdroid).
"""

import os
import subprocess
import sys
from pathlib import Path

PKG_NAME = "wtf.uhoh.newsoftkeyboard"
METADATA_PATH = Path("metadata") / f"{PKG_NAME}.yml"


def find_aapt() -> str:
    if os.environ.get("AAPT"):
        return os.environ["AAPT"]
    return subprocess.check_output(["which", "aapt"], text=True).strip()


def dump_badging(aapt: str, apk: Path) -> tuple[str, int]:
    out = subprocess.check_output([aapt, "dump", "badging", str(apk)], text=True)
    for line in out.splitlines():
        if line.startswith("package:"):
            tokens = line.split()
            attrs = {}
            for token in tokens[1:]:
                if "=" in token:
                    k, v = token.split("=", 1)
                    attrs[k] = v.strip("'")
            vc = int(attrs["versionCode"])
            vn = attrs["versionName"]
            return vn, vc
    raise RuntimeError(f"version info not found in {apk}")


def collect_apks(root: Path) -> list[Path]:
    apks: list[Path] = []
    for base in ("repo", "archive"):
        for apk in root.joinpath(base).rglob(f"{PKG_NAME}_*.apk"):
            apks.append(apk.resolve())
    return apks


def write_metadata(entries: list[tuple[str, int]], current_code: int, current_name: str) -> None:
    header = """Categories:
- System
License: Apache-2.0
WebSite: https://fdroid.uh-oh.wtf
SourceCode: https://github.com/pepperpepperpepper/NewSoftKeyboard
IssueTracker: https://github.com/pepperpepperpepper/NewSoftKeyboard/issues
Name: New Soft Keyboard
Summary: Hard fork of AnySoftKeyboard focused on custom layouts and hidden-switch behavior.
Description: |
  New Soft Keyboard is a lean fork of AnySoftKeyboard that keeps plugin/add-on compatibility
  while providing a slimmer APK and in-app downloadable language models.
AuthorName: Uh-Oh WTF
AutoUpdateMode: None
UpdateCheckMode: None
"""
    body = [
        f"CurrentVersion: {current_name}",
        f"CurrentVersionCode: {current_code}",
        "Builds: []",
    ]
    content = header + "\n".join(body) + "\n"
    METADATA_PATH.write_text(content)


def main() -> int:
    root = Path.cwd()
    if not (root / "repo").exists() or not (root / "archive").exists():
        print("Run from fdroid repo root (needs repo/ and archive/).", file=sys.stderr)
        return 1

    apks = collect_apks(root)
    if not apks:
        print("No APKs found in repo/ or archive/ for package.", file=sys.stderr)
        return 1

    aapt = find_aapt()
    versions: list[tuple[str, int]] = []
    for apk in sorted(apks):
        vn, vc = dump_badging(aapt, apk)
        versions.append((vn, vc))

    # Deduplicate by versionCode (keep first occurrence).
    seen = {}
    for vn, vc in versions:
        if vc not in seen:
            seen[vc] = vn
    pairs = sorted(seen.items(), key=lambda x: x[0])

    env_code = os.environ.get("CURRENT_VERSION_CODE")
    if env_code:
        current_code = int(env_code)
        current_name = seen.get(current_code)
        if current_name is None:
            print(f"CURRENT_VERSION_CODE {current_code} not found in APK set.", file=sys.stderr)
            return 1
    else:
        current_code = pairs[-1][0]
        current_name = pairs[-1][1]

    backup = METADATA_PATH.with_suffix(".yml.bak")
    if METADATA_PATH.exists():
        METADATA_PATH.replace(backup)
        print(f"Backed up existing metadata to {backup}")

    write_metadata(pairs, current_code, current_name)
    print(f"Wrote {METADATA_PATH} with {len(pairs)} versions; CurrentVersionCode={current_code} ({current_name})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
