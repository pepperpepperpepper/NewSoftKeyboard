#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENGINE_SCRIPT="${REPO_ROOT}/linux-ibus/nsk_ibus_engine.py"

if [[ ! -f "${ENGINE_SCRIPT}" ]]; then
  echo "Missing engine script: ${ENGINE_SCRIPT}" >&2
  exit 1
fi

ENGINE_WRAPPER="/usr/lib/ibus/ibus-engine-newsoftkeyboard"
COMPONENT_FILE="/usr/share/ibus/component/newsoftkeyboard.xml"

sudo mkdir -p "$(dirname "${ENGINE_WRAPPER}")" "$(dirname "${COMPONENT_FILE}")"

sudo tee "${ENGINE_WRAPPER}" >/dev/null <<EOF
#!/usr/bin/env sh
exec python3 ${ENGINE_SCRIPT} "\$@"
EOF
sudo chmod 0755 "${ENGINE_WRAPPER}"

sudo tee "${COMPONENT_FILE}" >/dev/null <<EOF
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

ibus write-cache >/dev/null 2>&1 || true

echo "Installed IBus engine wrapper: ${ENGINE_WRAPPER}"
echo "Installed IBus component: ${COMPONENT_FILE}"
echo "Next:"
echo "  - Restart IBus: ibus restart"
echo "  - Select engine: ibus engine newsoftkeyboard"
