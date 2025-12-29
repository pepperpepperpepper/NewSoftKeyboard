#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: bash scripts/uninstall_ibus_engine.sh [--force] [--dry-run]

Removes the dev IBus wrapper/component installed by scripts/install_ibus_engine.sh.

Options:
  --force    Remove files even if they don't look like ours.
  --dry-run  Print what would be done without making changes.
EOF
}

FORCE=0
DRY_RUN=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --force)
      FORCE=1
      shift
      ;;
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

ENGINE_WRAPPER="/usr/lib/ibus/ibus-engine-newsoftkeyboard"
COMPONENT_FILE="/usr/share/ibus/component/newsoftkeyboard.xml"

run() {
  if ((DRY_RUN)); then
    printf '+ %q' "$@"
    printf '\n'
    return 0
  fi
  "$@"
}

sudo_run() {
  if ((DRY_RUN)); then
    printf '+ sudo %q' "$@"
    printf '\n'
    return 0
  fi
  sudo "$@"
}

read_file_or_empty() {
  local path="$1"
  if [[ -f "$path" ]]; then
    cat "$path"
  else
    true
  fi
}

ensure_safe_to_remove() {
  local path="$1"
  local marker="$2"
  if [[ ! -f "$path" ]]; then
    return 0
  fi
  local existing
  existing="$(read_file_or_empty "$path")"
  if [[ "$existing" == *"$marker"* ]]; then
    return 0
  fi
  if ((FORCE)); then
    echo "WARNING: removing file that does not look like ours (forced): $path" >&2
    return 0
  fi
  echo "Refusing to remove file that does not look like ours: $path" >&2
  echo "Pass --force to remove anyway." >&2
  exit 1
}

ensure_safe_to_remove "${ENGINE_WRAPPER}" "nsk_ibus_engine.py"
ensure_safe_to_remove "${COMPONENT_FILE}" "org.freedesktop.IBus.NewSoftKeyboard"

if [[ -f "${ENGINE_WRAPPER}" ]]; then
  sudo_run rm -f "${ENGINE_WRAPPER}"
  echo "Removed IBus engine wrapper: ${ENGINE_WRAPPER}"
fi
if [[ -f "${COMPONENT_FILE}" ]]; then
  sudo_run rm -f "${COMPONENT_FILE}"
  echo "Removed IBus component: ${COMPONENT_FILE}"
fi

if command -v ibus >/dev/null; then
  run ibus write-cache >/dev/null 2>&1 || true
  run ibus restart >/dev/null 2>&1 || true
fi

echo "Done."

