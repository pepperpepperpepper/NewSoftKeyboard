#!/usr/bin/env python3

import argparse
import json
import shutil
import sys
from dataclasses import dataclass
from pathlib import Path
from xml.etree import ElementTree


def _strip_namespace(tag: str) -> str:
    if "}" in tag:
        return tag.split("}", 1)[1]
    return tag


def _is_keyboard_xml(xml_path: Path) -> bool:
    try:
        tree = ElementTree.parse(xml_path)
    except ElementTree.ParseError:
        return False
    return _strip_namespace(tree.getroot().tag) == "Keyboard"


@dataclass(frozen=True)
class PackEntry:
    entry_id: str
    relative_path: str


def _write_default_theme(theme_dir: Path) -> PackEntry:
    theme_dir.mkdir(parents=True, exist_ok=True)
    theme_path = theme_dir / "default.xml"
    theme_path.write_text(
        """<?xml version="1.0" encoding="utf-8"?>
<KeyboardTheme>
    <Color name="keyboardBackground" value="#202020"/>
</KeyboardTheme>
""",
        encoding="utf-8",
    )
    return PackEntry(entry_id="default", relative_path="themes/default.xml")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Exports ASK/NSK keyboard XML (source tree) into a portable NewSoftKeyboard pack directory."
    )
    parser.add_argument("--source", required=True, help="Directory containing *.xml keyboard files to export.")
    parser.add_argument("--output", required=True, help="Output pack directory (will be created).")
    parser.add_argument("--pack-id", default=None, help="Pack id (default: output directory name).")
    parser.add_argument("--name", default=None, help="Pack display name (default: pack-id).")
    parser.add_argument("--version", type=int, default=1, help="Pack version integer (default: 1).")
    parser.add_argument(
        "--with-default-theme",
        action="store_true",
        help="Write a minimal default theme under themes/default.xml and add it to the manifest.",
    )
    parser.add_argument("--force", action="store_true", help="Delete the output directory if it already exists.")
    args = parser.parse_args()

    source_dir = Path(args.source).expanduser().resolve()
    output_dir = Path(args.output).expanduser().resolve()

    if not source_dir.is_dir():
        print(f"Source directory does not exist: {source_dir}", file=sys.stderr)
        return 2

    if output_dir.exists():
        if not args.force:
            print(
                f"Output directory already exists; pass --force to overwrite: {output_dir}",
                file=sys.stderr,
            )
            return 2
        shutil.rmtree(output_dir)

    pack_id = args.pack_id or output_dir.name
    pack_name = args.name or pack_id

    keyboards_dir = output_dir / "keyboards"
    themes_dir = output_dir / "themes"

    keyboard_entries: list[PackEntry] = []
    for xml_path in sorted(source_dir.rglob("*.xml")):
        if not _is_keyboard_xml(xml_path):
            continue
        dest_name = xml_path.name
        dest_path = keyboards_dir / dest_name
        dest_path.parent.mkdir(parents=True, exist_ok=True)
        dest_path.write_bytes(xml_path.read_bytes())
        keyboard_entries.append(
            PackEntry(entry_id=xml_path.stem, relative_path=f"keyboards/{dest_name}")
        )

    if not keyboard_entries:
        print(f"No <Keyboard/> XML files found under: {source_dir}", file=sys.stderr)
        return 2

    theme_entries: list[PackEntry] = []
    if args.with_default_theme:
        theme_entries.append(_write_default_theme(themes_dir))

    manifest = {
        "schemaVersion": 1,
        "id": pack_id,
        "name": pack_name,
        "version": args.version,
        "keyboards": [{"id": e.entry_id, "path": e.relative_path} for e in keyboard_entries],
        "themes": [{"id": e.entry_id, "path": e.relative_path} for e in theme_entries],
    }

    output_dir.mkdir(parents=True, exist_ok=True)
    (output_dir / "manifest.json").write_text(
        json.dumps(manifest, indent=2, sort_keys=False) + "\n", encoding="utf-8"
    )

    print(f"Wrote pack: {output_dir}")
    print(f"- Keyboards: {len(keyboard_entries)}")
    print(f"- Themes: {len(theme_entries)}")
    print(f"- Manifest: {output_dir / 'manifest.json'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
