#!/usr/bin/env bash
set -euo pipefail

# End-to-end IBus smoke:
# - Starts headless X11 (Xvfb + openbox)
# - Starts ibus-daemon (private bus)
# - Sets global engine to `newsoftkeyboard`
# - Uses a tiny GTK app (TextView) as the target
# - Sends JSONL semantic actions to the engine socket
# - Verifies the committed text

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

TMP_DIR="$(mktemp -d)"
cleanup() { rm -rf "${TMP_DIR}"; }
trap cleanup EXIT

export XDG_DATA_HOME="${TMP_DIR}/data"
export XDG_CACHE_HOME="${TMP_DIR}/cache"
export XDG_CONFIG_HOME="${TMP_DIR}/config"
export XDG_RUNTIME_DIR="${TMP_DIR}/runtime"
mkdir -p "${XDG_DATA_HOME}" "${XDG_CACHE_HOME}" "${XDG_CONFIG_HOME}" "${XDG_RUNTIME_DIR}"

export NSK_IBUS_SOCKET="${XDG_RUNTIME_DIR}/newsoftkeyboard/ibus.sock"
export NSK_IBUS_CONTROL_SOCKET="${XDG_RUNTIME_DIR}/newsoftkeyboard/ibus.control.sock"

dbus-run-session -- bash -lc '
set -euo pipefail

export XDG_DATA_HOME="'"${XDG_DATA_HOME}"'"
export XDG_CACHE_HOME="'"${XDG_CACHE_HOME}"'"
export XDG_CONFIG_HOME="'"${XDG_CONFIG_HOME}"'"
export XDG_RUNTIME_DIR="'"${XDG_RUNTIME_DIR}"'"
export NSK_IBUS_SOCKET="'"${NSK_IBUS_SOCKET}"'"
export NSK_IBUS_CONTROL_SOCKET="'"${NSK_IBUS_CONTROL_SOCKET}"'"

ibus write-cache >/dev/null 2>&1 || true

Xvfb :99 -screen 0 900x700x24 -nolisten tcp -extension GLX >/dev/null 2>&1 &
export DISPLAY=:99
openbox >/dev/null 2>&1 &
sleep 0.5

ibus-daemon -drx --panel disable >/dev/null 2>&1 &
sleep 1

if ! ibus list-engine | rg -q "^\\s*newsoftkeyboard\\b"; then
  echo "IBus engine newsoftkeyboard not registered. Run: bash scripts/install_ibus_engine.sh" >&2
  ibus list-engine >&2
  exit 1
fi

export IBUS_ADDRESS="$(ibus address)"
gdbus call --address "${IBUS_ADDRESS}" --dest org.freedesktop.IBus --object-path /org/freedesktop/IBus --method org.freedesktop.IBus.SetGlobalEngine newsoftkeyboard --timeout 20 >/dev/null

# Wait for the engine to create its socket.
for _ in $(seq 1 200); do
  [[ -S "${NSK_IBUS_SOCKET}" ]] && break
  sleep 0.05
done
[[ -S "${NSK_IBUS_SOCKET}" ]] || { echo "Engine socket not created: ${NSK_IBUS_SOCKET}" >&2; exit 1; }

export GTK_IM_MODULE=ibus
export XMODIFIERS=@im=ibus

python3 - <<'"'"'PY'"'"'
import json
import os
import socket
import sys
import threading

import gi

gi.require_version("Gtk", "3.0")
from gi.repository import GLib, Gtk

expected = "abd\n"
socket_path = os.environ["NSK_IBUS_SOCKET"]
control_socket_path = os.environ["NSK_IBUS_CONTROL_SOCKET"]
activation_messages = []

os.makedirs(os.path.dirname(control_socket_path), exist_ok=True)
try:
  if os.path.exists(control_socket_path):
    os.remove(control_socket_path)
except OSError:
  pass

control_server = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
control_server.bind(control_socket_path)
control_server.listen(5)


def accept_activation() -> None:
  while True:
    conn, _ = control_server.accept()
    with conn:
      file = conn.makefile("r", encoding="utf-8", newline="\n")
      for line in file:
        raw = line.strip()
        if raw:
          activation_messages.append(raw)


threading.Thread(target=accept_activation, daemon=True).start()

win = Gtk.Window()
view = Gtk.TextView()
buffer = view.get_buffer()
win.add(view)
win.set_default_size(500, 200)
win.show_all()
view.grab_focus()


def send_messages() -> bool:
  with socket.socket(socket.AF_UNIX, socket.SOCK_STREAM) as s:
    s.connect(socket_path)

    def send(message):
      s.sendall((json.dumps(message) + "\n").encode("utf-8"))

    send({"type": "commit", "text": "a"})
    send({"type": "commit", "text": "b"})
    send({"type": "commit", "text": "c"})
    send({"type": "delete_backward", "count": 1})
    send({"type": "commit", "text": "d"})
    send({"type": "editor_action", "action": "ENTER"})

  return False


def has_activate_message() -> bool:
  for raw in activation_messages:
    try:
      obj = json.loads(raw)
    except json.JSONDecodeError:
      continue
    if isinstance(obj, dict) and obj.get("type") == "activate":
      return True
  return False


def finish() -> bool:
  start = buffer.get_start_iter()
  end = buffer.get_end_iter()
  actual = buffer.get_text(start, end, True)
  if actual != expected:
    print(f"Unexpected buffer text: {actual!r} != {expected!r}", file=sys.stderr)
    raise SystemExit(1)
  if not has_activate_message():
    print(f"Missing activation messages; got: {activation_messages!r}", file=sys.stderr)
    raise SystemExit(1)
  print("OK:", actual.encode("unicode_escape").decode("ascii"))
  Gtk.main_quit()
  return False


GLib.timeout_add(200, send_messages)
GLib.timeout_add(1500, finish)
Gtk.main()
PY
'

echo "IBus smoke OK"
