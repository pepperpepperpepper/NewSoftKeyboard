#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENGINE_SCRIPT="${REPO_ROOT}/linux-ibus/nsk_ibus_engine.py"

usage() {
  cat <<'EOF'
Usage: bash scripts/install_ibus_engine.sh [--force] [--dry-run]

Installs a dev IBus component pointing at the repo-local engine script.

Options:
  --force    Overwrite existing files even if they don't look like ours.
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

if [[ ! -f "${ENGINE_SCRIPT}" ]]; then
  echo "Missing engine script: ${ENGINE_SCRIPT}" >&2
  exit 1
fi

if ! command -v ibus >/dev/null; then
  echo "Missing required dependency: ibus" >&2
  echo "On Arch: sudo pacman -S ibus" >&2
  exit 2
fi
if ! command -v python3 >/dev/null; then
  echo "Missing required dependency: python3" >&2
  exit 2
fi
if ! python3 - <<'PY' >/dev/null 2>&1; then
import gi

gi.require_version("Gtk", "3.0")
gi.require_version("IBus", "1.0")

from gi.repository import Gtk, IBus  # noqa: F401
PY
  echo "Missing required Python/GI runtime for IBus/GTK." >&2
  echo "On Arch: sudo pacman -S python-gobject gtk3 ibus" >&2
  exit 2
fi

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

ensure_safe_to_overwrite() {
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
    echo "WARNING: overwriting existing file (forced): $path" >&2
    return 0
  fi
  echo "Refusing to overwrite existing file: $path" >&2
  echo "Pass --force to overwrite." >&2
  exit 1
}

ensure_safe_to_overwrite "${ENGINE_WRAPPER}" "nsk_ibus_engine.py"
ensure_safe_to_overwrite "${COMPONENT_FILE}" "org.freedesktop.IBus.NewSoftKeyboard"

sudo_run mkdir -p "$(dirname "${ENGINE_WRAPPER}")" "$(dirname "${COMPONENT_FILE}")"

ENGINE_WRAPPER_CONTENT=$(
  cat <<EOF
#!/usr/bin/env sh
exec python3 "${ENGINE_SCRIPT}" "\$@"
EOF
)
if ((DRY_RUN)); then
  echo "+ sudo tee ${ENGINE_WRAPPER} >/dev/null <<'EOF'"
  echo "${ENGINE_WRAPPER_CONTENT}"
  echo "EOF"
else
  echo "${ENGINE_WRAPPER_CONTENT}" | sudo tee "${ENGINE_WRAPPER}" >/dev/null
fi
sudo_run chmod 0755 "${ENGINE_WRAPPER}"

COMPONENT_CONTENT=$(
  cat <<EOF
<?xml version="1.0" encoding="utf-8"?>
<component>
    <name>org.freedesktop.IBus.NewSoftKeyboard</name>
    <description>NewSoftKeyboard (dev) — on-screen keyboard bridge</description>
    <exec>${ENGINE_WRAPPER}</exec>
    <version>dev</version>
    <author>NewSoftKeyboard</author>
    <license>GPL</license>
    <homepage>https://github.com/pepperpepperpepper/NewSoftKeyboard</homepage>
    <textdomain>newsoftkeyboard</textdomain>
    <engines>
        <engine>
            <name>newsoftkeyboard</name>
            <language>en</language>
            <license>GPL</license>
            <author>NewSoftKeyboard</author>
            <layout>us</layout>
            <longname>NewSoftKeyboard (dev)</longname>
            <description>NewSoftKeyboard IBus engine (dev)</description>
            <icon>input-keyboard</icon>
            <rank>99</rank>
        </engine>
    </engines>
</component>
EOF
)
if ((DRY_RUN)); then
  echo "+ sudo tee ${COMPONENT_FILE} >/dev/null <<'EOF'"
  echo "${COMPONENT_CONTENT}"
  echo "EOF"
else
  echo "${COMPONENT_CONTENT}" | sudo tee "${COMPONENT_FILE}" >/dev/null
fi

run ibus write-cache >/dev/null 2>&1 || true
run ibus restart >/dev/null 2>&1 || true

echo "Installed IBus engine wrapper: ${ENGINE_WRAPPER}"
echo "Installed IBus component: ${COMPONENT_FILE}"
echo "Next:"
echo "  - Restart IBus: ibus restart"
echo "  - Select engine: ibus engine newsoftkeyboard"
