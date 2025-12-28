# TODO — Linux compatibility (IBus + smokes)

## IBus component install

- [x] Add install script for the IBus component (system-wide):
  - `scripts/install_ibus_engine.sh`
  - Installs:
    - Wrapper: `/usr/lib/ibus/ibus-engine-newsoftkeyboard`
    - Component XML: `/usr/share/ibus/component/newsoftkeyboard.xml`
  - Followed by: `ibus write-cache` (best-effort) and `ibus restart`.

- [ ] Add uninstall script (symmetry with install):
  - Remove wrapper + component XML.
  - Re-run `ibus write-cache` and restart IBus.

- [ ] Improve installer robustness:
  - Confirm required runtime deps (`ibus`, `python-gobject`, `gtk3`) with clear error messages.
  - Add `--dry-run` and `--force` flags (optional).

## Runtime hardening

- [ ] Make `--output=ibus` fail fast with a clear message when sockets can’t bind/connect.
- [ ] Document the activation/control socket env vars in one place:
  - `NSK_IBUS_SOCKET`
  - `NSK_IBUS_CONTROL_SOCKET`

## CI / smoke coverage

- [ ] Add CI job(s) to run:
  - `bash scripts/linux_smoke_x11.sh`
  - `bash scripts/linux_smoke_ibus.sh`
