# Clipboard Plan: long‑press clipboard icon + complete Settings UI

## Summary

Goal: make clipboard features easy to find and reliably accessible from the keyboard (via **long‑press on a clipboard icon**) and provide a single, complete **Settings → Clipboard** section for enablement, privacy, and behavior.

This plan focuses on:

1. A consistent keyboard affordance: **tap = paste**, **long‑press = clipboard history/manager** (matches current behavior).
2. A Settings hub that explains access and controls privacy‑sensitive behavior.

## Audit: existing UI + docs to stay consistent with

The app already has a well-defined Settings IA and documentation for it. This plan is written to match those sources:

- Settings category exists at Settings Home: `Settings → Clipboard` (`nav:category_clipboard`). See `docs/settings-clickpaths.md`.
- Current user-facing toggle is **“Sync clipboard with OS”** (`settings_key_os_clipboard_sync`, default `true`). See:
  - `ime/app/src/main/res/xml/prefs_clipboard_settings.xml`
  - `docs/user-stories-settings.md` (Clipboard → OS clipboard sync)
  - `docs/user-stories-settings-inventory.md` (privacy impact **High**)

Rule for this plan: keep **screen names + tap paths stable** (don’t reshuffle categories), and add clipboard-specific controls **under Settings → Clipboard** using the same patterns as other high-risk areas (e.g., the “Privacy notice” rows in voice settings).

## Current state (what exists today)

### Keyboard access paths

- **Clipboard action in the strip above the keyboard** (actions strip):
  - Shown when OS clipboard sync is enabled and a clipboard change was observed recently.
  - Tap pastes (`ClipboardStripActionProvider#outputClipboardText()` → `ImeClipboard#performPaste()`).
  - Long‑press opens the clipboard history dialog (`ClipboardStripActionProvider#showAllClipboardOptions()` → `ImeClipboard#showAllClipboardEntries()`).
- **Paste key long‑press** in specific extension keyboards:
  - `ime/app/src/main/res/xml/ext_kbd_top_row_text_editing.xml`: paste key long‑press → `CLIPBOARD_PASTE_POPUP`.
  - `ime/app/src/main/res/xml/ext_kbd_utility_utility.xml`: paste key long‑press → `CLIPBOARD_PASTE_POPUP`.

### Settings

- Clipboard settings screen exists (`ClipboardSettingsFragment`) but currently only exposes one switch:
  - `settings_key_os_clipboard_sync` (“Sync clipboard with OS”).

### Clipboard history mechanics

- History is in‑memory (per `ClipboardV11`/`ClipboardV16`/`ClipboardV28` instance) and limited to 15 entries.
- Entries are only collected when a listener is registered (tied to `settings_key_os_clipboard_sync`).
- The “clipboard strip” UI currently hides itself after a timeout even if OS clipboard content and/or history still exists.

## Pain points to address

1. **Discoverability**: users may not realize “clipboard history” exists or that it’s behind a long‑press.
2. **Consistency**: clipboard history is reachable from some extension keyboards and from the strip action, but not guaranteed to be available at all times.
3. **Settings completeness**: clipboard behavior, privacy expectations, and access instructions are not centralized in the Clipboard settings category.
4. **Privacy/security**: clipboard previews are masked in secure inputs, but the full history dialog can still reveal entries unless explicitly handled.

## UX proposal

### A) Keyboard: one obvious clipboard entry point

Primary affordance: a **clipboard icon in the actions strip above the keyboard**, consistent with existing UI.

- **Tap**: paste current OS clipboard (existing behavior).
- **Long‑press**: open clipboard history/manager (existing dialog, improved behavior below).
- When clipboard/history is empty:
  - Show “Clipboard is empty” toast (already implemented),
  - plus a short hint like “Copy text, then long‑press to see history”.

Visibility policy (to fit existing action-strip user flow):

- Default: keep the prior timeout-based affordance (avoid clutter):
  - show the clipboard icon after a clipboard change is observed,
  - hide the preview text after a short delay,
  - hide the clipboard icon after a longer timeout.

If we want a power-user affordance, gate an “Always show clipboard icon” option behind Settings (default off) so the clipboard history/manager is reachable even when the last copy was a while ago.

Secondary access paths (existing / optional), ordered by invasiveness:

1. Keep using the existing **actions strip clipboard icon**, but change visibility rules so it can be accessed reliably.
2. Add a configurable **toolbar/row button** that is always shown (similar to other quick keys).
3. Add an optional **dedicated key** on the main keyboard layout (theme/row dependent; higher risk of layout disruption).

### B) Clipboard history dialog behavior

Keep the current dialog UI (`ImeClipboard#showAllClipboardEntries`) but improve:

- **Visibility policy**: allow reaching it even when the last clipboard event was “too long ago” (see icon policy above).
- **Secure input behavior** (default safe):
  - In password/secure fields, either:
    - show masked entry previews and require an extra confirmation before pasting, or
    - block opening the history and show a toast explaining why.
  - Make this controlled by a settings toggle.

### C) Settings: “Clipboard” becomes the single hub

Add a richer settings screen under **Settings → Clipboard** (as documented in `docs/settings-clickpaths.md`) that provides:

0. **Privacy notice (non-interactive)**
   - Pattern-match `prefs_speech_to_text.xml`’s “Privacy notice” row.
   - Explicitly state: clipboard contents can be sensitive; feature is opt-in via the toggle; do not log/share.
   - Clarify retention: today history is in-memory and resets with IME lifecycle (unless we implement P2 persistence).
1. **Enablement (existing)**
   - Keep the existing label **“Sync clipboard with OS”** (avoid breaking documented clickpaths/inventory).
   - Update summary text to mention: “Enables paste shortcut + long-press clipboard history.”
2. **Keyboard access**
   - “Clipboard icon visibility” (ListPreference), default aligned with current contextual-strip behavior:
     - “Show when clipboard has content” (recommended)
     - “Always show” (power users / discoverability)
     - “Never show” (for minimalists; clipboard still usable via extension keyboards)
3. **Secure fields**
   - “Mask clipboard preview in password fields” (default on; maps to existing masking behavior in `ClipboardStripActionProvider#setClipboardText`).
   - “Allow clipboard history dialog in password fields” (default off; **dangerous toggle** requiring confirmation, consistent with `ProgrammableApiSettingsFragment` confirm flows).
4. **Maintenance**
   - “Clear OS clipboard” (explicit action; best-effort via `ClipboardManager.clearPrimaryClip()` on API 28+; fallback on older).
   - “Clear clipboard history” is only feasible if history is shared/owned outside a single `ClipboardVxx` instance. Options:
     - (Preferred) Refactor clipboard-history ownership into a shared in-process store (still in-memory by default), or
     - Provide a “Reset clipboard history” action that toggles `settings_key_os_clipboard_sync` off→on (forces `ClipboardVxx.mEntries.clear()` in the IME instance) plus clears OS clipboard.
5. **Help**
   - A static row: “Tip: long‑press the clipboard icon above the keyboard to see history.”

## Technical plan (implementation outline)

### 1) Make clipboard access reliable from the keyboard

**P0 (minimal):** keep the existing actions strip icon behavior but add a settings-driven override.

- In `ImeClipboard`:
  - Keep time‑based removal as the default (so the clipboard icon is not always visible).
  - Add a preference that, when enabled, keeps the clipboard icon reachable (tap = paste, long‑press = history) beyond the timeout.

**P1 (better):** wire icon visibility to an explicit preference (so “Always show” is opt-in).

- Add a new preference controlling strip visibility policy (see Settings spec above).
- Implement by adding/removing the strip action provider based on:
  - preference value,
  - `settings_key_os_clipboard_sync`,
  - current input type (secure fields).

### 2) Make history size configurable

Optional. Only do this if we’re confident it’s needed for user workflow; otherwise keep fixed-size to reduce UI surface.

- Add a new preference key and default (e.g., `settings_key_clipboard_history_size`, default 15).
- Update `ClipboardV11` (and derived classes) to respect the configured max size instead of the hardcoded constant:
  - store `maxEntries` as a field and update via a setter when the preference changes,
  - ensure trimming occurs when decreasing the max.

### 3) Secure input handling

- Add settings keys for secure behavior:
  - `settings_key_clipboard_mask_in_secure_fields` (default true),
  - `settings_key_clipboard_allow_history_dialog_in_secure_fields` (default false).
- In `ImeClipboard#showAllClipboardEntries`:
  - check `isTextPassword(currentInputEditorInfo())` and apply the configured behavior:
    - block / mask list / confirm before paste.
- Ensure no clipboard content is logged.

### 4) Expand Clipboard settings UI

- Update `ime/app/src/main/res/xml/prefs_clipboard_settings.xml` to include:
  - new preferences (list/switch/action),
  - a “Privacy notice” + “Help” static preferences (non‑selectable), matching patterns in `ime/app/src/main/res/xml/prefs_speech_to_text.xml`.
- Update/add strings:
  - keep “Sync clipboard with OS” title stable, but strengthen summary text.
- Add new preference keys to:
  - `ime/app/src/main/res/values/settings_keys_dont_translate.xml`
  - `ime/app/src/main/res/values/settings_defaults_dont_translate.xml`
- Implement click handlers in `ClipboardSettingsFragment` for:
  - confirmation-gated secure-field toggles,
  - clear actions (OS clipboard, and history-reset strategy as chosen).

### 5) Optional: expose a “clipboard manager” UI in Settings (higher scope)

Only if we decide history must be viewable outside the IME:

- Introduce a small, dependency‑free persistence layer (e.g., SharedPreferences‑backed JSON) to store clipboard history.
- Replace per-instance `ClipboardVxx.mEntries` as the source of truth with a shared repository accessible from both IME and Settings.
- Add a Settings screen that lists entries with delete/clear.

This is higher privacy risk and should be opt‑in.

## Documentation updates (keep UI docs “source of truth”)

When implementing any new preference keys or changing labels/behavior, update:

- `docs/user-stories-settings.md` (add stories for new clipboard settings, especially anything affecting secure fields)
- `docs/user-stories-settings-inventory.md` (new keys + defaults + privacy impact)
- `docs/settings-clickpaths.md` (new rows + tap paths)

## Testing plan

### P0 (must)

- Robolectric unit tests:
  - “Show clipboard icon always” preference toggling results in strip icon visible.
  - History size preference trims entries correctly.
  - Secure field behavior blocks/masks dialog according to settings.
- IME clipboard tests:
  - Extend `ImeServiceClipboardTest` to cover:
    - icon stays available beyond previous timeout window,
    - long‑press opens dialog under the new visibility rules.

### P1 (important)

- Regression tests for:
  - clearing clipboard history from settings,
  - switching history size while entries exist.

## Milestones

- **P0:** Keep clipboard icon **not always visible** by default (timeout-based), and add an “Always show clipboard icon” preference for power users.
- **P1:** Add secure-field toggles (with confirmations).
- **P2:** Optional history size tuning and/or persistent clipboard manager (opt‑in, privacy reviewed).

## Open questions

1. `settings_key_os_clipboard_sync` is currently default-on (`true`). Is that still the desired privacy posture, or should we flip to default-off and rely on onboarding/help?
2. Should clipboard history be captured while the IME is not visible (Android restrictions vary), or only while the keyboard is active?
3. Do we want pinned entries / quick snippets as part of the clipboard manager, or keep it strictly as “recently copied” history?
