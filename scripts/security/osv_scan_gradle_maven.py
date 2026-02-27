#!/usr/bin/env python3

import argparse
import json
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request
from dataclasses import dataclass
from pathlib import Path


DEPENDENCY_LINE_RE = re.compile(
    r"^[\s|+\\-]*"
    r"(?P<group>[A-Za-z0-9_.-]+):"
    r"(?P<artifact>[A-Za-z0-9_.-]+):"
    r"(?P<version>[^\s()]+)"
    r"(?:\s*->\s*(?P<resolved>[^\s()]+))?"
)


OSV_QUERYBATCH_URL = "https://api.osv.dev/v1/querybatch"


@dataclass(frozen=True)
class MavenCoordinate:
    group: str
    artifact: str
    version: str

    @property
    def package_name(self) -> str:
        return f"{self.group}:{self.artifact}"

    @property
    def display(self) -> str:
        return f"{self.group}:{self.artifact}:{self.version}"


def _run_gradle_dependencies(gradle: str, project: str, configuration: str) -> str | None:
    cmd = [
        gradle,
        "--no-daemon",
        f"{project}:dependencies",
        "--configuration",
        configuration,
        "--console=plain",
    ]
    proc = subprocess.run(cmd, capture_output=True, text=True)
    combined = (proc.stdout or "") + (proc.stderr or "")
    if proc.returncode == 0:
        return combined

    missing_config_fragments = [
        f"Configuration with name '{configuration}' not found",
        f"configuration '{configuration}' not found",
        f"Configuration '{configuration}' not found",
    ]
    if any(fragment in combined for fragment in missing_config_fragments):
        return None

    raise RuntimeError(f"Gradle failed for {project}::{configuration}:\n{combined}")


def _parse_maven_coordinates(deps_text: str) -> set[MavenCoordinate]:
    coords: set[MavenCoordinate] = set()
    for line in deps_text.splitlines():
        match = DEPENDENCY_LINE_RE.match(line)
        if not match:
            continue
        group = match.group("group")
        artifact = match.group("artifact")
        version = match.group("resolved") or match.group("version")

        if version in ("unspecified", "", None):
            continue
        if version.startswith("{"):
            # Dependency constraints sometimes render as "{strictly ...}".
            continue
        if group == "project":
            continue

        coords.add(MavenCoordinate(group=group, artifact=artifact, version=version))
    return coords


def _http_post_json(url: str, payload: dict, timeout_s: int = 30) -> dict:
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url, data=data, headers={"Content-Type": "application/json"}, method="POST"
    )
    with urllib.request.urlopen(req, timeout=timeout_s) as resp:
        raw = resp.read().decode("utf-8", errors="replace")
    return json.loads(raw)


def _query_osv(coords: list[MavenCoordinate]) -> dict[MavenCoordinate, list[dict]]:
    findings: dict[MavenCoordinate, list[dict]] = {}

    batch_size = 128
    for start in range(0, len(coords), batch_size):
        batch = coords[start : start + batch_size]
        payload = {
            "queries": [
                {
                    "package": {"ecosystem": "Maven", "name": c.package_name},
                    "version": c.version,
                }
                for c in batch
            ]
        }

        last_error: Exception | None = None
        for attempt in range(3):
            try:
                response = _http_post_json(OSV_QUERYBATCH_URL, payload, timeout_s=30)
                results = response.get("results", [])
                if len(results) != len(batch):
                    raise RuntimeError(
                        f"Unexpected OSV response length: got {len(results)}, expected {len(batch)}"
                    )
                for c, r in zip(batch, results):
                    vulns = r.get("vulns") or []
                    if vulns:
                        findings[c] = vulns
                last_error = None
                break
            except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError) as e:
                last_error = e
                time.sleep(2**attempt)
            except Exception as e:
                last_error = e
                break

        if last_error is not None:
            raise RuntimeError(f"OSV query failed: {last_error}") from last_error

    return findings


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Scan Gradle-resolved Maven dependencies (including transitive) via OSV."
    )
    parser.add_argument("--gradle", default="./gradlew", help="Path to Gradle wrapper.")
    parser.add_argument("--project", default=":ime:app", help="Gradle project to scan.")
    parser.add_argument(
        "--configuration",
        action="append",
        default=[],
        help="Gradle configuration to scan (repeatable).",
    )
    args = parser.parse_args()

    repo_root = Path(__file__).resolve().parents[2]
    gradle_path = str((repo_root / args.gradle).resolve())

    configurations = args.configuration or [
        "nskReleaseRuntimeClasspath",
        "askCompatReleaseRuntimeClasspath",
    ]

    all_coords: set[MavenCoordinate] = set()
    scanned_any = False
    for config in configurations:
        deps_text = _run_gradle_dependencies(gradle_path, args.project, config)
        if deps_text is None:
            print(f"SKIP: {args.project}::{config} (configuration not found)")
            continue
        scanned_any = True
        coords = _parse_maven_coordinates(deps_text)
        all_coords.update(coords)
        print(f"OK: Collected {len(coords)} Maven coordinates from {args.project}::{config}")

    if not scanned_any:
        print("ERROR: No configurations were scanned (none were found).")
        return 2

    sorted_coords = sorted(all_coords, key=lambda c: c.display)
    print(f"Querying OSV for {len(sorted_coords)} unique Maven coordinates...")

    findings = _query_osv(sorted_coords)
    if not findings:
        print("OK: No known vulnerabilities reported by OSV for scanned coordinates.")
        return 0

    print("\nERROR: Vulnerable dependencies found:\n")
    for coord in sorted(findings.keys(), key=lambda c: c.display):
        vulns = findings[coord]
        vuln_ids = [v.get("id", "UNKNOWN") for v in vulns]
        print(f"- {coord.display}: {', '.join(vuln_ids)}")
    print("\nUpdate dependencies to fixed versions and re-run this scan.")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())

