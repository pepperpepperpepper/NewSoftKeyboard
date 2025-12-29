# Linux (dev) — IBus + X11 smokes

This repo includes a minimal Linux “host” for exercising NewSoftKeyboard keyboard packs outside Android.

Two output modes are supported:

- **X11 (`xdotool`)**: legacy key injection for quick local testing.
- **IBus**: a dev IBus engine that commits text/actions into the focused app via an IME-style path.

## Key components

- `linux-host/`: Java/Swing host app + CLI (`:linux-host:installDist`)
- `linux-ibus/nsk_ibus_engine.py`: IBus engine that bridges JSONL actions over a unix socket
- Install/uninstall helpers:
  - `scripts/install_ibus_engine.sh`
  - `scripts/uninstall_ibus_engine.sh`
- Smokes:
  - `scripts/linux_smoke_x11.sh`
  - `scripts/linux_smoke_ibus.sh`

## IBus install (dev)

The install script registers a **dev** engine system-wide by writing:

- Wrapper: `/usr/lib/ibus/ibus-engine-newsoftkeyboard`
- Component: `/usr/share/ibus/component/newsoftkeyboard.xml`

Install:

- `bash scripts/install_ibus_engine.sh`

Uninstall:

- `bash scripts/uninstall_ibus_engine.sh`

Both scripts support:

- `--dry-run`
- `--force` (override safety checks)

### Dependencies

Arch (example):

- `sudo pacman -S --needed ibus python-gobject gtk3`
- For smokes: `sudo pacman -S --needed xorg-server-xvfb xterm xdotool openbox ripgrep`

## Socket env vars (IBus mode)

These env vars control where the engine and host communicate:

- `NSK_IBUS_SOCKET`
  - **Engine listens** here for JSONL actions (`commit`, `delete_backward`, `editor_action`, …).
  - Default: `$XDG_RUNTIME_DIR/newsoftkeyboard/ibus.sock` (fallback: `/tmp/newsoftkeyboard/ibus.sock`)
- `NSK_IBUS_CONTROL_SOCKET`
  - **Host binds** here (server) when `--ibus-activation` is enabled.
  - **Engine connects** here (client) to send `activate` / `deactivate` messages.
  - Default: derived from `NSK_IBUS_SOCKET` (`*.control.sock`)

## Running smokes

X11 smoke (headless Xvfb + xdotool):

- `bash scripts/linux_smoke_x11.sh`

IBus smoke (headless Xvfb + private dbus + ibus-daemon + gtk target):

- `bash scripts/install_ibus_engine.sh`
- `bash scripts/linux_smoke_ibus.sh`

## Running the host manually

Build the linux-host distribution:

- `./gradlew :linux-host:installDist`

Then run:

- `./linux-host/build/install/linux-host/bin/linux-host --run <pack-dir-or-id> --output=xdotool --xdotool-window <id>`
- `./linux-host/build/install/linux-host/bin/linux-host --run <pack-dir-or-id> --output=ibus --ibus-activation`
