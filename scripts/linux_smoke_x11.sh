#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-/mnt/finished/.gradle}"

PACK_DIR="${1:-keyboard-core/src/test/resources/fixtures/packs/basic_pack}"
TEXT="${2:-abc}"

if ! command -v xvfb-run >/dev/null; then
  echo "Missing xvfb-run. On Arch: sudo pacman -S xorg-server-xvfb" >&2
  exit 2
fi
if ! command -v xterm >/dev/null; then
  echo "Missing xterm. On Arch: sudo pacman -S xterm" >&2
  exit 2
fi
if ! command -v xdotool >/dev/null; then
  echo "Missing xdotool. On Arch: sudo pacman -S xdotool" >&2
  exit 2
fi
if ! command -v openbox >/dev/null; then
  echo "Missing openbox. On Arch: sudo pacman -S openbox" >&2
  exit 2
fi

TARGET_TITLE="nsk-smoke-target-$$"
TMP_FILE="$(mktemp -t nsk_smoke_typed_XXXXXX.txt)"

cleanup() {
  rm -f "$TMP_FILE"
}
trap cleanup EXIT

echo "Running X11 smoke under Xvfb..."
echo "- Pack: $PACK_DIR"
echo "- Text: $TEXT"

echo "Building linux-host distribution..."
GRADLE_USER_HOME="$GRADLE_USER_HOME" "$REPO_ROOT/gradlew" :linux-host:installDist >/dev/null

xvfb_env=()
if [[ -f /usr/share/glvnd/egl_vendor.d/50_mesa.json ]]; then
  xvfb_env+=("__EGL_VENDOR_LIBRARY_FILENAMES=/usr/share/glvnd/egl_vendor.d/50_mesa.json")
fi
xvfb_env+=("LIBGL_ALWAYS_SOFTWARE=1")

export REPO_ROOT PACK_DIR TEXT TMP_FILE TARGET_TITLE GRADLE_USER_HOME

env "${xvfb_env[@]}" xvfb-run -a -s "-screen 0 1024x768x24 -nolisten tcp -ac -extension GLX" bash -lc '
  set -euo pipefail
  cd "$REPO_ROOT"

  # A window manager is required for reliable focus/activation under Xvfb.
  openbox &
  OB_PID=$!
  trap "kill $OB_PID 2>/dev/null || true" EXIT

  xterm -fa "Liberation Mono" -fs 12 -T "$TARGET_TITLE" -geometry 80x24 -e bash -lc "cat > \"$TMP_FILE\"" &
  XT_PID=$!

  WINDOW_ID=""
  for _ in $(seq 1 100); do
    WINDOW_ID="$(xdotool search --name "$TARGET_TITLE" 2>/dev/null | head -n1 || true)"
    if [[ -n "$WINDOW_ID" ]]; then
      break
    fi
    sleep 0.05
  done

  if [[ -z "$WINDOW_ID" ]]; then
    echo "Failed to locate xterm window." >&2
    kill "$XT_PID" 2>/dev/null || true
    exit 1
  fi

  ./linux-host/build/install/linux-host/bin/linux-host --smoke "$PACK_DIR" --output=xdotool --xdotool-window "$WINDOW_ID" --text="$TEXT" >/dev/null

  # Flush the TTY line buffer so `cat` writes to the output file.
  xdotool key --window "$WINDOW_ID" --clearmodifiers Return
  kill "$XT_PID" 2>/dev/null || true
  wait "$XT_PID" || true

  ACTUAL="$(head -n 1 "$TMP_FILE")"
  if [[ "$ACTUAL" != "$TEXT" ]]; then
    echo "Smoke failed: expected $TEXT, got $ACTUAL" >&2
    exit 1
  fi
'

echo "PASS: X11 smoke typed '$TEXT' successfully."
