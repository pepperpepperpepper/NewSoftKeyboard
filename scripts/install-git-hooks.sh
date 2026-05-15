#!/usr/bin/env bash
# Wire up the tracked git hooks at scripts/git-hooks by pointing
# core.hooksPath at them. Run once after cloning or when the hooks change.

set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
target="scripts/git-hooks"

if [[ ! -d "${repo_root}/${target}" ]]; then
  echo "ERROR: ${repo_root}/${target} not found" >&2
  exit 1
fi

chmod +x "${repo_root}/${target}"/*

git -C "${repo_root}" config core.hooksPath "$target"

echo "core.hooksPath set to '$target' for ${repo_root}"
echo "Installed hooks:"
ls -1 "${repo_root}/${target}"
