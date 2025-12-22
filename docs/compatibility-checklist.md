# Add‑On Compatibility Checklist

Use this list when validating NewSoftKeyboard remains compatible with existing AnySoftKeyboard add‑ons:

- Discovery actions and meta‑data
  - Keyboards: actions `wtf.uhoh.newsoftkeyboard.KEYBOARD`, `com.anysoftkeyboard.plugin.KEYBOARD`, and `com.menny.android.anysoftkeyboard.KEYBOARD`
  - Dictionaries: actions `wtf.uhoh.newsoftkeyboard.DICTIONARY`, `com.anysoftkeyboard.plugin.DICTIONARY`, and `com.menny.android.anysoftkeyboard.DICTIONARY`
  - Themes: actions `wtf.uhoh.newsoftkeyboard.KEYBOARD_THEME` and `com.anysoftkeyboard.plugin.KEYBOARD_THEME`
  - Quick‑text: actions `wtf.uhoh.newsoftkeyboard.QUICK_TEXT_KEY` and `com.anysoftkeyboard.plugin.QUICK_TEXT_KEY`
  - Extension keyboards: actions `wtf.uhoh.newsoftkeyboard.EXTENSION_KEYBOARD` and `com.anysoftkeyboard.plugin.EXTENSION_KEYBOARD`
  - Meta‑data keys accepted:
    - Keyboards/Dictionaries: `wtf.uhoh.newsoftkeyboard.*`, `com.anysoftkeyboard.plugindata.*`, and `com.menny.android.anysoftkeyboard.(keyboards|dictionaries)`
    - Themes/Quick‑text/Extensions: `wtf.uhoh.newsoftkeyboard.plugindata.*` and `com.anysoftkeyboard.plugindata.*`
- XML schema
  - Root/add‑on node names unchanged (Keyboards/Dictionaries, Keyboard/Dictionary)
  - Attributes supported: ids, names, descriptions, sort index, defaultEnabled, icons, etc.
- Runtime behavior
  - Add‑on enablement and ordering are preserved across installs/updates.
  - Broadcast receiver `AddOnUICardReceiver` continues to respond to `com.anysoftkeyboard.UI_CARD_UPDATE`.
  - Packaging/branding changes in NewSoftKeyboard must not require add‑on APK changes; existing ASK packs should work unmodified.
- Manifests and queries
  - App manifest queries include both NewSoftKeyboard and legacy ASK actions (verified under `ime/app/src/main/AndroidManifest.xml`).
- Tests
  - Unit: addon factories resolve receivers for both namespaces.
  - Instrumentation: install a sample ASK keyboard pack APK and verify discovery + switch.
  - Instrumentation (real APKs): `ExternalAddOnSmokeInstrumentedTest` with an installed F-Droid language pack + theme pack.

Notes

- Keep constants centralized in `wtf/uhoh/newsoftkeyboard/api/PluginActions.java` to avoid drift.
- When adding new add‑on surfaces, provide both `wtf.uhoh.newsoftkeyboard.*` and legacy `com.anysoftkeyboard.*` variants until deprecation.
