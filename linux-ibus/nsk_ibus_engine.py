#!/usr/bin/env python3

import argparse
import json
import os
import socket
import threading
from typing import Any, Optional

import gi

gi.require_version("IBus", "1.0")
from gi.repository import GLib, IBus  # noqa: E402

ENGINE_NAME = "newsoftkeyboard"
BUS_NAME = "org.freedesktop.IBus.NewSoftKeyboard"
SOCKET_ENV = "NSK_IBUS_SOCKET"
CONTROL_SOCKET_ENV = "NSK_IBUS_CONTROL_SOCKET"

_control_socket_path: Optional[str] = None

_active_engine_lock = threading.Lock()
_active_engine: Optional["NewSoftKeyboardEngine"] = None
_active_state_lock = threading.Lock()
_active_state: Optional[bool] = None


def _default_socket_path() -> str:
  xdg_runtime = os.environ.get("XDG_RUNTIME_DIR")
  if xdg_runtime and xdg_runtime.strip():
    return os.path.join(xdg_runtime, "newsoftkeyboard", "ibus.sock")
  return os.path.join("/tmp", "newsoftkeyboard", "ibus.sock")


def _default_control_socket_path(commit_socket_path: str) -> str:
  if commit_socket_path.endswith(".sock"):
    return commit_socket_path[:-4] + ".control.sock"
  return commit_socket_path + ".control"


def _send_control_message_async(message: dict[str, Any]) -> None:
  socket_path = _control_socket_path
  if socket_path is None or not socket_path.strip():
    return

  def send() -> None:
    try:
      with socket.socket(socket.AF_UNIX, socket.SOCK_STREAM) as client:
        client.connect(socket_path)
        payload = json.dumps(message, separators=(",", ":"), ensure_ascii=False) + "\n"
        client.sendall(payload.encode("utf-8"))
    except OSError:
      return

  threading.Thread(target=send, daemon=True).start()


def _set_active_state(is_active: bool) -> None:
  global _active_state
  with _active_state_lock:
    if _active_state == is_active:
      return
    _active_state = is_active
  _send_control_message_async({"type": "activate" if is_active else "deactivate"})


def _get_active_engine() -> Optional["NewSoftKeyboardEngine"]:
  with _active_engine_lock:
    return _active_engine


def _set_active_engine(engine: Optional["NewSoftKeyboardEngine"]) -> None:
  global _active_engine
  with _active_engine_lock:
    _active_engine = engine


def _apply_action(engine: "NewSoftKeyboardEngine", message: dict[str, Any]) -> None:
  action_type = message.get("type")
  if action_type == "status":
    is_active = _active_state is True
    _send_control_message_async({"type": "activate" if is_active else "deactivate"})
    return
  if action_type == "commit":
    text = message.get("text")
    if not isinstance(text, str):
      return
    engine.commit_text(IBus.Text.new_from_string(text))
  elif action_type == "delete_backward":
    count = message.get("count", 1)
    if not isinstance(count, int):
      return
    if count <= 0:
      return
    engine.delete_surrounding_text(-count, count)
  elif action_type == "delete_forward":
    count = message.get("count", 1)
    if not isinstance(count, int):
      return
    if count <= 0:
      return
    engine.delete_surrounding_text(0, count)
  elif action_type == "editor_action":
    name = message.get("action")
    if not isinstance(name, str):
      return
    normalized = name.strip().upper()
    if normalized in ("ENTER", "NEXT", "DONE"):
      engine.commit_text(IBus.Text.new_from_string("\n"))
    elif normalized == "TAB":
      engine.commit_text(IBus.Text.new_from_string("\t"))


def _dispatch_message(message: dict[str, Any]) -> bool:
  engine = _get_active_engine()
  if engine is None:
    return False
  _apply_action(engine, message)
  return False


def _socket_server(socket_path: str) -> None:
  os.makedirs(os.path.dirname(socket_path), exist_ok=True)
  try:
    if os.path.exists(socket_path):
      os.remove(socket_path)
  except OSError:
    # Best effort cleanup; binding will fail loudly if the path is unusable.
    pass

  server = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
  server.bind(socket_path)
  server.listen(1)

  while True:
    conn, _ = server.accept()
    with conn:
      file = conn.makefile("r", encoding="utf-8", newline="\n")
      for line in file:
        raw = line.strip()
        if not raw:
          continue
        try:
          message = json.loads(raw)
        except json.JSONDecodeError:
          continue
        if not isinstance(message, dict):
          continue
        GLib.idle_add(_dispatch_message, message)


class NewSoftKeyboardEngine(IBus.Engine):
  def do_enable(self) -> None:
    _set_active_engine(self)
    _set_active_state(True)

  def do_disable(self) -> None:
    if _get_active_engine() is self:
      _set_active_engine(None)
    _set_active_state(False)

  def do_focus_in(self) -> None:
    _set_active_engine(self)
    _set_active_state(True)

  def do_focus_out(self) -> None:
    if _get_active_engine() is self:
      _set_active_engine(None)
    _set_active_state(False)


def main() -> None:
  parser = argparse.ArgumentParser(
    description="NewSoftKeyboard IBus engine (dev bridge via unix socket JSONL)"
  )
  parser.add_argument(
    "--socket",
    default=os.environ.get(SOCKET_ENV) or _default_socket_path(),
    help=f"Unix socket path (default: ${SOCKET_ENV} or XDG_RUNTIME_DIR/newsoftkeyboard/ibus.sock)",
  )
  parser.add_argument(
    "--control-socket",
    default=os.environ.get(CONTROL_SOCKET_ENV),
    help=f"Unix socket path for OSK visibility control (default: ${CONTROL_SOCKET_ENV} or derived from --socket)",
  )
  args, _ = parser.parse_known_args()
  global _control_socket_path
  _control_socket_path = args.control_socket or _default_control_socket_path(args.socket)

  IBus.init()

  thread = threading.Thread(target=_socket_server, args=(args.socket,), daemon=True)
  thread.start()

  bus = IBus.Bus()
  factory = IBus.Factory.new(bus.get_connection())
  factory.add_engine(ENGINE_NAME, NewSoftKeyboardEngine)
  bus.request_name(BUS_NAME, 0)

  mainloop = GLib.MainLoop()
  try:
    mainloop.run()
  except KeyboardInterrupt:
    pass


if __name__ == "__main__":
  main()
