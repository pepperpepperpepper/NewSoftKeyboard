#!/usr/bin/env python3

import argparse
import json
import re
import shutil
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional


@dataclass
class XmlNode:
    name: str
    attrs: dict[str, str] = field(default_factory=dict)
    children: list["XmlNode"] = field(default_factory=list)
    text: Optional[str] = None


def _run(cmd: list[str]) -> str:
    result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    if result.returncode != 0:
        raise RuntimeError(
            f"Command failed ({result.returncode}): {' '.join(cmd)}\n{result.stderr.strip()}"
        )
    return result.stdout


def _escape_attr(value: str) -> str:
    return (
        value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
    )


def _parse_aapt2_xmltree(output: str) -> tuple[XmlNode, dict[str, str]]:
    # Returns (root_node, xmlns_map prefix->uri)
    xmlns: dict[str, str] = {}
    uri_to_prefix: dict[str, str] = {}
    unknown_uri_prefixes: dict[str, str] = {}

    root: Optional[XmlNode] = None
    stack: list[tuple[int, XmlNode]] = []

    def ensure_prefix_for_uri(uri: str) -> str:
        if uri in uri_to_prefix:
            return uri_to_prefix[uri]
        if uri in unknown_uri_prefixes:
            return unknown_uri_prefixes[uri]
        prefix = f"ns{len(unknown_uri_prefixes)}"
        unknown_uri_prefixes[uri] = prefix
        xmlns[prefix] = uri
        uri_to_prefix[uri] = prefix
        return prefix

    for raw_line in output.splitlines():
        line = raw_line.rstrip()
        stripped = line.lstrip()
        indent = len(line) - len(stripped)

        if stripped.startswith("N: "):
            # N: android=http://schemas.android.com/apk/res/android (line=6)
            decl = stripped[3:]
            if "=" in decl:
                prefix, rest = decl.split("=", 1)
                uri = rest.split(" ", 1)[0]
                prefix = prefix.strip()
                if prefix == "":
                    # default namespace
                    xmlns[""] = uri
                else:
                    xmlns[prefix] = uri
                uri_to_prefix[uri] = prefix
            continue

        if stripped.startswith("E: "):
            # E: Keyboard (line=6)
            name = stripped[3:].split(" ", 1)[0].strip()
            node = XmlNode(name=name)

            while stack and indent <= stack[-1][0]:
                stack.pop()

            if stack:
                stack[-1][1].children.append(node)
            else:
                root = node

            stack.append((indent, node))
            continue

        if stripped.startswith("A: "):
            # A: http://schemas...:keyLabel(0x0101024b)="q" (Raw: "q")
            if not stack:
                continue
            current = stack[-1][1]

            rest = stripped[3:]
            if "=" not in rest:
                continue
            key_part, value_part = rest.split("=", 1)
            key_part = key_part.split("(", 1)[0].strip()
            value_part = value_part.strip()

            # Prefer Raw string when present (it preserves original string content).
            if "(Raw:" in value_part:
                raw_section = value_part.split("(Raw:", 1)[1]
                first_quote = raw_section.find('"')
                last_quote = raw_section.rfind('"')
                if first_quote != -1 and last_quote != -1 and last_quote > first_quote:
                    value_part = raw_section[first_quote + 1 : last_quote]
                else:
                    value_part = value_part.split(" ", 1)[0].strip()
            else:
                if value_part.startswith('"'):
                    # Extract the quoted string: "foo"
                    last_quote = value_part.find('"', 1)
                    if last_quote != -1:
                        value_part = value_part[1:last_quote]

            # Namespace handling: key_part can be either "name" or "<uri>:<local>"
            if ":" in key_part:
                ns_uri, local = key_part.rsplit(":", 1)
                if "://" in ns_uri:
                    prefix = uri_to_prefix.get(ns_uri)
                    if prefix is None:
                        prefix = ensure_prefix_for_uri(ns_uri)
                    qualified = f"{prefix}:{local}" if prefix else local
                else:
                    # Already a prefix form like android:keyLabel
                    qualified = key_part
            else:
                qualified = key_part

            current.attrs[qualified] = value_part
            continue

    if root is None:
        raise RuntimeError("aapt2 xmltree parse failed: no root element found")
    return root, xmlns


def _write_xml(node: XmlNode, xmlns: dict[str, str]) -> str:
    lines: list[str] = ['<?xml version="1.0" encoding="utf-8"?>']

    def write_node(n: XmlNode, depth: int, is_root: bool) -> None:
        indent = "  " * depth
        attrs = dict(n.attrs)
        if is_root:
            # Ensure we always include android ns if present in attrs
            if any(k.startswith("android:") for k in attrs.keys()) and "android" not in xmlns:
                xmlns["android"] = "http://schemas.android.com/apk/res/android"
            for prefix, uri in sorted(xmlns.items(), key=lambda kv: kv[0]):
                if prefix == "":
                    attrs["xmlns"] = uri
                else:
                    attrs[f"xmlns:{prefix}"] = uri

        attrs_str = "".join(
            f' {name}="{_escape_attr(value)}"' for name, value in sorted(attrs.items())
        )

        if not n.children and (n.text is None or n.text == ""):
            lines.append(f"{indent}<{n.name}{attrs_str} />")
            return

        lines.append(f"{indent}<{n.name}{attrs_str}>")
        if n.text:
            lines.append(f"{indent}  {_escape_attr(n.text)}")
        for child in n.children:
            write_node(child, depth + 1, False)
        lines.append(f"{indent}</{n.name}>")

    write_node(node, 0, True)
    return "\n".join(lines) + "\n"


def _list_xml_resource_names(aapt2_path: str, apk_path: Path) -> list[str]:
    resources = _run([aapt2_path, "dump", "resources", str(apk_path)])
    names: set[str] = set()
    pattern = re.compile(r"^resource\s+0x[0-9a-fA-F]+\s+xml/([A-Za-z0-9_]+)$")
    for line in resources.splitlines():
        line = line.strip()
        match = pattern.match(line)
        if match:
            names.add(match.group(1))
    return sorted(names)


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Converts a compiled Android APK's res/xml Keyboard layouts into a portable NewSoftKeyboard pack."
    )
    parser.add_argument("--apk", required=True, help="Path to the APK.")
    parser.add_argument("--output", required=True, help="Output pack directory (will be created).")
    parser.add_argument("--aapt2", default="aapt2", help="Path to aapt2 (default: aapt2 in PATH).")
    parser.add_argument("--pack-id", default=None, help="Pack id (default: apk file name).")
    parser.add_argument("--name", default=None, help="Pack display name (default: pack-id).")
    parser.add_argument("--version", type=int, default=1, help="Pack version integer (default: 1).")
    parser.add_argument(
        "--with-default-theme",
        action="store_true",
        help="Write a minimal default theme under themes/default.xml and add it to the manifest.",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Delete the output directory if it already exists.",
    )
    args = parser.parse_args()

    apk_path = Path(args.apk).expanduser().resolve()
    output_dir = Path(args.output).expanduser().resolve()
    aapt2_path = args.aapt2

    if not apk_path.is_file():
        print(f"APK not found: {apk_path}", file=sys.stderr)
        return 2

    if output_dir.exists():
        if not args.force:
            print(
                f"Output directory already exists; pass --force to overwrite: {output_dir}",
                file=sys.stderr,
            )
            return 2
        shutil.rmtree(output_dir)

    pack_id = args.pack_id or apk_path.stem
    pack_name = args.name or pack_id

    xml_names = _list_xml_resource_names(aapt2_path, apk_path)
    if not xml_names:
        print("No XML resources found in the APK.", file=sys.stderr)
        return 2

    keyboards_dir = output_dir / "keyboards"
    themes_dir = output_dir / "themes"
    keyboards_dir.mkdir(parents=True, exist_ok=True)
    themes_dir.mkdir(parents=True, exist_ok=True)

    keyboard_entries: list[dict[str, str]] = []
    for name in xml_names:
        apk_file = f"res/xml/{name}.xml"
        try:
            xmltree = _run([aapt2_path, "dump", "xmltree", str(apk_path), "--file", apk_file])
        except RuntimeError:
            continue
        root, xmlns = _parse_aapt2_xmltree(xmltree)
        if root.name != "Keyboard":
            continue

        out_path = keyboards_dir / f"{name}.xml"
        out_path.write_text(_write_xml(root, xmlns), encoding="utf-8")
        keyboard_entries.append({"id": name, "path": f"keyboards/{name}.xml"})

    if not keyboard_entries:
        print("No <Keyboard/> layouts found under res/xml in the APK.", file=sys.stderr)
        return 2

    theme_entries: list[dict[str, str]] = []
    if args.with_default_theme:
        default_theme = themes_dir / "default.xml"
        default_theme.write_text(
            """<?xml version="1.0" encoding="utf-8"?>
<KeyboardTheme>
  <Color name="keyboardBackground" value="#202020"/>
</KeyboardTheme>
""",
            encoding="utf-8",
        )
        theme_entries.append({"id": "default", "path": "themes/default.xml"})

    manifest = {
        "schemaVersion": 1,
        "id": pack_id,
        "name": pack_name,
        "version": args.version,
        "keyboards": keyboard_entries,
        "themes": theme_entries,
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
