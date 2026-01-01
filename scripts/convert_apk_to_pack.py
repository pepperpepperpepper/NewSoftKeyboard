#!/usr/bin/env python3

import argparse
import json
import re
import shutil
import subprocess
import sys
import zipfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional


@dataclass
class XmlNode:
    name: str
    attrs: dict[str, str] = field(default_factory=dict)
    children: list["XmlNode"] = field(default_factory=list)
    text: Optional[str] = None


@dataclass
class StyleDef:
    name: str
    parent: Optional[str]
    items: dict[str, str] = field(default_factory=dict)


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


def _parse_aapt2_resources_dump(
    output: str,
) -> tuple[dict[str, str], dict[str, str], dict[str, StyleDef]]:
    id_to_name: dict[str, str] = {}
    colors: dict[str, str] = {}
    styles: dict[str, StyleDef] = {}

    current_color: Optional[str] = None
    current_style: Optional[StyleDef] = None

    resource_pattern = re.compile(r"^resource\s+(0x[0-9a-fA-F]+)\s+(\w+)/([^\s]+)$")
    color_value_pattern = re.compile(r"^\(\)\s+(#[0-9a-fA-F]{6,8})$")
    style_header_parent_pattern = re.compile(r"parent=(style/[^\s]+)")
    style_item_key_pattern = re.compile(r"^(0x[0-9a-fA-F]+)\s*(?:\(([^)]+)\))?$")

    def normalize_style_item_key(raw: str) -> str:
        # aapt2 dump resources style item keys typically look like:
        #   0x010100d4(android:background)
        #   0x7f040123(wtf.uhoh.newsoftkeyboard:attr/keyTextColor)
        # Sometimes the key is just the numeric id. We try to turn these into stable attribute names.
        raw_key = raw.strip()
        if not raw_key:
            return ""

        match = style_item_key_pattern.match(raw_key)
        res_id = match.group(1).lower() if match else None
        display = match.group(2) if match else None

        is_android_attr = False
        if display:
            display_lower = display.lower()
            is_android_attr = display_lower.startswith("android:") or display_lower.startswith(
                "android."
            )

        candidate = (display or "").strip()
        if not candidate:
            if res_id:
                candidate = id_to_name.get(res_id) or id_to_name.get(res_id.lower()) or res_id
            else:
                candidate = raw_key

        simplified = candidate
        if "R.attr." in simplified:
            simplified = simplified.rsplit("R.attr.", 1)[1]
        if ":attr/" in simplified:
            simplified = simplified.split(":attr/", 1)[1]
        if simplified.startswith("attr/"):
            simplified = simplified.split("/", 1)[1]
        if "/attr/" in simplified:
            simplified = simplified.split("/attr/", 1)[1]
        if "/" in simplified:
            simplified = simplified.rsplit("/", 1)[1]
        if ":" in simplified:
            simplified = simplified.split(":", 1)[1]

        if res_id == "0x010100d4" or (is_android_attr and simplified == "background"):
            return "keyboardBackground"

        return simplified.strip()

    for raw_line in output.splitlines():
        line = raw_line.strip()
        match = resource_pattern.match(line)
        if match:
            current_color = None
            current_style = None

            res_id, res_type, res_name = match.group(1).lower(), match.group(2), match.group(3)
            full_name = f"{res_type}/{res_name}"
            id_to_name[res_id] = full_name

            if res_type == "color":
                current_color = full_name
            elif res_type == "style":
                current_style = StyleDef(name=full_name, parent=None)
                styles[full_name] = current_style
            continue

        if current_color:
            match = color_value_pattern.match(line)
            if match:
                colors[current_color] = match.group(1).lower()
                current_color = None
            continue

            if current_style:
                if "(style)" in line:
                    match = style_header_parent_pattern.search(line)
                    if match:
                        current_style.parent = match.group(1)
                    continue

                if "=" in line:
                    key, value = line.split("=", 1)
                    key = normalize_style_item_key(key)
                    value = value.strip()
                    if key:
                        current_style.items[key] = value
            continue

    return id_to_name, colors, styles


def _resolve_style_items(style_name: str, styles: dict[str, StyleDef]) -> dict[str, str]:
    cache: dict[str, dict[str, str]] = {}

    def resolve(name: str) -> dict[str, str]:
        if name in cache:
            return cache[name]
        style = styles.get(name)
        if style is None:
            cache[name] = {}
            return cache[name]

        items: dict[str, str] = {}
        if style.parent:
            items.update(resolve(style.parent))
        items.update(style.items)
        cache[name] = items
        return items

    return resolve(style_name)


def _build_drawable_index(zip_names: list[str]) -> dict[str, list[str]]:
    drawable_index: dict[str, list[str]] = {}
    pattern = re.compile(r"^res/drawable[^/]*/([A-Za-z0-9_]+)\.([A-Za-z0-9]+)$")
    for name in zip_names:
        match = pattern.match(name)
        if match:
            drawable_name, ext = match.group(1), match.group(2).lower()
            if ext not in {"png", "webp", "xml", "jpg", "jpeg"}:
                continue
            drawable_index.setdefault(drawable_name, []).append(name)
    return drawable_index


def _drawable_rank(zip_path: str) -> int:
    # Prefer nodpi or higher density assets.
    folder = zip_path.split("/", 2)[1] if zip_path.startswith("res/") else ""
    density_rank = 0
    if "nodpi" in folder:
        density_rank = 100
    elif "xxxhdpi" in folder:
        density_rank = 90
    elif "xxhdpi" in folder:
        density_rank = 80
    elif "xhdpi" in folder:
        density_rank = 70
    elif "hdpi" in folder:
        density_rank = 60
    elif "mdpi" in folder:
        density_rank = 50
    elif "ldpi" in folder:
        density_rank = 40

    ext = zip_path.rsplit(".", 1)[1].lower()
    ext_rank = {"png": 3, "webp": 2, "jpg": 2, "jpeg": 2, "xml": 1}.get(ext, 0)
    return density_rank * 10 + ext_rank


def _extract_drawable(
    apk_path: Path,
    aapt2_path: str,
    zip_file: zipfile.ZipFile,
    drawable_index: dict[str, list[str]],
    drawable_ref: str,
    icons_dir: Path,
    extracted: dict[str, str],
) -> Optional[str]:
    if not drawable_ref.startswith("@drawable/"):
        return None

    drawable_name = drawable_ref.split("/", 1)[1].strip()
    if not drawable_name:
        return None
    if drawable_name in extracted:
        return extracted[drawable_name]

    candidates = drawable_index.get(drawable_name, [])
    if not candidates:
        return None
    best = sorted(candidates, key=_drawable_rank, reverse=True)[0]
    ext = best.rsplit(".", 1)[1].lower()

    icons_dir.mkdir(parents=True, exist_ok=True)
    out_name = f"{drawable_name}.{ext}"
    out_path = icons_dir / out_name

    if ext == "xml":
        xmltree = _run([aapt2_path, "dump", "xmltree", str(apk_path), "--file", best])
        root, xmlns = _parse_aapt2_xmltree(xmltree)
        out_path.write_text(_write_xml(root, xmlns), encoding="utf-8")
    else:
        out_path.write_bytes(zip_file.read(best))

    rel_path = f"icons/{out_name}"
    extracted[drawable_name] = rel_path
    return rel_path


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Converts a compiled Android APK's keyboard layouts (and optionally themes) into a portable NewSoftKeyboard pack."
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
        "--include-themes",
        action="store_true",
        help="Export themes by converting <KeyboardThemes/> listings and their referenced style resources.",
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

    id_to_name: dict[str, str] = {}
    colors: dict[str, str] = {}
    styles: dict[str, StyleDef] = {}
    drawable_index: dict[str, list[str]] = {}

    if args.include_themes:
        resources_dump = _run([aapt2_path, "dump", "resources", str(apk_path)])
        id_to_name, colors, styles = _parse_aapt2_resources_dump(resources_dump)

        with zipfile.ZipFile(apk_path, "r") as apk_zip:
            drawable_index = _build_drawable_index(apk_zip.namelist())

    keyboard_entries: list[dict[str, str]] = []
    theme_listing_roots: list[XmlNode] = []
    for name in xml_names:
        apk_file = f"res/xml/{name}.xml"
        try:
            xmltree = _run([aapt2_path, "dump", "xmltree", str(apk_path), "--file", apk_file])
        except RuntimeError:
            continue

        root, xmlns = _parse_aapt2_xmltree(xmltree)
        if root.name == "Keyboard":
            out_path = keyboards_dir / f"{name}.xml"
            out_path.write_text(_write_xml(root, xmlns), encoding="utf-8")
            keyboard_entries.append({"id": name, "path": f"keyboards/{name}.xml"})
            continue

        if args.include_themes and root.name == "KeyboardThemes":
            theme_listing_roots.append(root)
            continue

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

    if args.include_themes and theme_listing_roots:
        extracted_drawables: dict[str, str] = {}
        style_cache: dict[str, dict[str, str]] = {}

        def resolve_style(style_name: str) -> dict[str, str]:
            if style_name in style_cache:
                return style_cache[style_name]
            style_cache[style_name] = {}
            merged = _resolve_style_items(style_name, styles)
            style_cache[style_name] = merged
            return merged

        def to_safe_filename(raw: str) -> str:
            cleaned = re.sub(r"[^A-Za-z0-9._-]+", "_", raw.strip())
            return cleaned if cleaned else "theme"

        with zipfile.ZipFile(apk_path, "r") as apk_zip:
            for listing_root in theme_listing_roots:
                for theme_node in listing_root.children:
                    if theme_node.name != "KeyboardTheme":
                        continue

                    theme_id = theme_node.attrs.get("id") or "theme"
                    theme_res = theme_node.attrs.get("themeRes")
                    icon_res = theme_node.attrs.get("iconsThemeRes")
                    if not theme_res:
                        continue

                    theme_style = id_to_name.get(theme_res.lstrip("@").lower())
                    icon_style = id_to_name.get(icon_res.lstrip("@").lower()) if icon_res else None
                    if not theme_style or not theme_style.startswith("style/"):
                        continue

                    colors_out: dict[str, str] = {}
                    icons_out: dict[str, str] = {}

                    for key, value in resolve_style(theme_style).items():
                        logical = "keyboardBackground" if key == "0x010100d4" else key
                        if value.startswith("@color/"):
                            resolved = colors.get("color/" + value.split("/", 1)[1])
                            if resolved:
                                colors_out[logical] = resolved
                        elif value.startswith("#"):
                            colors_out[logical] = value.lower()
                        elif value.startswith("@drawable/"):
                            extracted = _extract_drawable(
                                apk_path,
                                aapt2_path,
                                apk_zip,
                                drawable_index,
                                value,
                                output_dir / "icons",
                                extracted_drawables,
                            )
                            if extracted:
                                icons_out[logical] = extracted

                    if icon_style and icon_style.startswith("style/"):
                        for key, value in resolve_style(icon_style).items():
                            if not value.startswith("@drawable/"):
                                continue
                            extracted = _extract_drawable(
                                apk_path,
                                aapt2_path,
                                apk_zip,
                                drawable_index,
                                value,
                                output_dir / "icons",
                                extracted_drawables,
                            )
                            if extracted:
                                icons_out[key] = extracted

                    if not colors_out and not icons_out:
                        continue

                    file_name = to_safe_filename(theme_id) + ".xml"
                    theme_path = themes_dir / file_name
                    if theme_path.exists():
                        # avoid collisions if duplicate IDs appear
                        theme_path = themes_dir / (to_safe_filename(theme_id) + "_2.xml")
                        file_name = theme_path.name

                    theme_xml_lines = ['<?xml version="1.0" encoding="utf-8"?>', "<KeyboardTheme>"]
                    for name, value in sorted(colors_out.items()):
                        theme_xml_lines.append(f'  <Color name="{name}" value="{value}"/>')
                    for name, path in sorted(icons_out.items()):
                        theme_xml_lines.append(f'  <Icon name="{name}" path="{path}"/>')
                    theme_xml_lines.append("</KeyboardTheme>")
                    theme_path.write_text("\n".join(theme_xml_lines) + "\n", encoding="utf-8")

                    theme_entries.append({"id": theme_id, "path": f"themes/{file_name}"})

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
