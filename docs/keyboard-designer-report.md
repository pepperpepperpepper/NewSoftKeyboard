# Keyboard Designer Report (NewSoftKeyboard)

Date: 2025-12-29

## Goal

Add an in-app **Keyboard Designer** that lets users create and maintain their own keyboard layouts (and optionally themes) **without** needing to build/install a separate “language pack” APK, while keeping **full backwards compatibility** with existing AnySoftKeyboard (ASK) add-ons (keyboards/themes/plugins).

This report focuses on:

1. What the current keyboard system looks like in this repo.
2. What “design your own keyboard” actually means in terms of data/behavior.
3. The practical constraints (Android packaging, resources, add-on model).
4. A recommended architecture that fits the existing codebase boundaries.
5. A phased implementation plan that keeps the app shippable.

## Status (as of 2025-12-29)

Already implemented in this repo (usable today for tooling/tests):

- A file-based keyboard pack format (`manifest.json` + `keyboards/*.xml` + optional themes/icons).
- A JVM-only module with pack parsing + validation: `:keyboard-core`.
- A deterministic exporter script that turns source-tree keyboard XML into a pack directory:
  - `scripts/export_keyboard_pack.py`
- A deterministic converter that turns a _compiled_ keyboard/theme APK into a portable pack directory
  (reconstructs XML and extracts a portable subset of colors/icons into `themes/*.xml` + `icons/*`):
  - `scripts/convert_apk_to_pack.py` (requires `aapt2` on the host machine)
- A Linux dev host that can load a pack keyboard and press keys (useful for pack smoke + iteration):
  - `linux-host/src/main/java/wtf/uhoh/newsoftkeyboard/linuxhost/LinuxHostMain.java`

Not implemented yet (this document proposes the architecture/plan):

- Android runtime loading of a pack keyboard as an IME keyboard (not just parsing).
- Pack install/uninstall UI (zip import/export) and an in-app “Keyboard Designer” UI.
- Pack theme application on Android (colors/icons applied to the runtime keyboard theme system).

## Non-goals (to keep scope sane)

- Replacing the existing ASK add-on system.
- Allowing packs to execute code (packs are data only).
- Perfect, lossless support for every Android resource reference inside existing keyboard XML on day 1.
  - The designer must preserve ASK add-ons as-is; importing an add-on into a pack may require resolving
    some references or warning the user.

## Current Keyboard System (Android) — How It Works Today

### Add-on discovery + selection

Keyboards are discovered via the add-on system:

- **Factory**: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/KeyboardFactory.java`
  - Loads keyboards via `MultipleAddOnsFactory`.
  - Supports NSK + ASK metadata/actions via:
    - `PluginActions.ACTION_KEYBOARD_NEW` / `PluginActions.METADATA_KEYBOARDS_NEW`
    - plus compatibility receivers for ASK actions/metadata.
- **Per-keyboard entry**: a `<Keyboard .../>` node (in an add-on XML) provides:
  - `id`, `nameResId`, `layoutResId`, optional `landscapeResId`
  - `defaultDictionaryLocale`
  - optional physical keyboard mapping, icon, etc.
  - Example: `addons/languages/english/pack/src/main/res/xml/english_keyboards.xml`

In settings, the keyboard list UI is:

- `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ui/settings/KeyboardAddOnBrowserFragment.java`
  - Uses `DemoKeyboardView` to render a preview:
    - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/DemoKeyboardView.java`

### Runtime keyboard construction

When a keyboard is selected, the app creates a `KeyboardDefinition` instance:

- `KeyboardAddOnAndBuilder#createKeyboard(...)`
  - returns an `ExternalKeyboard` (`KeyboardDefinition` subclass)
  - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/ExternalKeyboard.java`

Then the keyboard loads keys from the keyboard layout XML resource:

- `Keyboard#loadKeyboard(...)`
  - `Resources.getXml(mLayoutResId)` → `XmlResourceParser`
  - `KeyboardXmlLoader.loadKeyboard(...)`
    - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/KeyboardXmlLoader.java`

The XML loader creates:

- Rows: `Keyboard.Row` / `KeyboardRowBase`
  - `KeyboardRowAttributesParser` parses row-level sizing/mode/edge flags.
- Keys: `KeyboardKey` (`Keyboard.Key` subclass)
  - `KeyboardKeyAttributesApplier` parses core attributes.
  - `KeyboardKey` parses ASK/NSK extended attributes (hint labels, long press, etc.).

### Generic rows (top/bottom extensions) are applied after base layout parsing

One critical “gotcha” for a designer: the keyboard a user sees is not always “just the XML”.

`KeyboardDefinition#loadKeyboard(...)` does:

1. `super.loadKeyboard(...)` (loads the base keyboard XML)
2. `addGenericRows(...)` (injects top-row and bottom-row extensions)

Those generic rows come from add-ons too:

- `NskApplicationBase.getTopRowFactory(...)`
- `NskApplicationBase.getBottomRowFactory(...)`

Implications for the designer:

- The live preview should be explicit about whether it is showing:
  - “Base layout only” (extensions disabled), or
  - “As installed” (extensions applied using current user settings).
- Editing a custom keyboard should _not_ accidentally bake extension keys into the base layout unless
  the user explicitly chooses to “merge extensions into layout”.

### What is configurable via keyboard XML today

At minimum, the XML supports (non-exhaustive but the major knobs):

Keyboard-level:

- `android:keyWidth`, `android:keyHeight`, `android:horizontalGap`, `android:verticalGap`
- `ask:autoCap`, `ask:showPreview`

Row-level:

- `android:keyWidth`, `android:keyHeight`, `android:horizontalGap`, `android:verticalGap`
- `android:rowEdgeFlags`
- `android:keyboardMode` (row-gating by “mode”)

Key-level (core):

- `android:codes` (CSV or int; can represent output chars or special actions)
- `android:keyLabel`
- `android:keyIcon`, `android:iconPreview`
- `android:keyOutputText` + shifted variants (`shiftedKeyOutputText`)
- `android:keyEdgeFlags`
- `android:keyWidth`, `android:keyHeight`, `android:horizontalGap`
- `android:popupCharacters`, `android:popupKeyboard`
- `android:isRepeatable`, `android:isModifier`, `android:isSticky`

Key-level (ASK/NSK extended; parsed in `KeyboardKey`):

- `ask:hintLabel`, `ask:hintIcon`
- `ask:longPressCode`
- `ask:shiftedCodes`, `ask:shiftedKeyLabel`, `ask:isShiftAlways`
- `ask:isFunctional`, `ask:showInLayout`
- `ask:extra_key_data` (used for keyboard switching / custom behaviors)
- emoji tags/variants (`ask:tags`, `ask:genders`, `ask:skinTones`)

So: the **existing XML schema is already expressive enough** to describe a “user-designed” keyboard layout.

## What “Keyboard Designer” Needs To Produce (Scope Clarification)

“Design a keyboard” could mean different levels of ambition. Here’s the scope spectrum, and what each implies technically:

### A) “Custom key map” (minimal)

Users can take an existing keyboard and:

- change labels
- change key codes (what gets committed)
- change long-press output
- adjust widths/gaps per key

This is mostly editing a single layout + serializing it.

### B) “Custom layout” (typical expectation)

Users can:

- add/remove/reorder rows
- add/remove/reorder keys within rows
- set per-key sizing rules
- create and link popup mini-keyboards
- create multiple “layers” (alphabet/symbols/etc.) and switch between them

This becomes a keyboard “pack” (multiple XML files + manifest).

### C) “Full custom keyboard + theme” (power-user)

Users can also design a theme:

- key/background colors, text colors
- icon set overrides for special keys

This requires a theme format and asset storage (png/svg) with preview.

This repo already has a **pack-level** concept in `keyboard-core` (see below), which matches B/C well.

## Hard Constraints (Why This Isn’t “Just Generate XML”)

### 1) You can’t generate Android resources at runtime

Existing keyboards are **compiled resources** (`@xml/...`, `@integer/...`, `@drawable/...`) packaged inside an APK.

The app cannot “write” new `R.xml.*` resources on-device and then reference them as resource IDs.

So any user-created keyboard must be stored as **files** (app-private storage) and loaded via an alternate loading path.

### 2) Existing loader relies on compiled `XmlResourceParser` typed values

The current keyboard parsing pipeline is heavily based on:

- `Resources.getXml(@XmlRes id)` returning a compiled `XmlResourceParser`
- `TypedArray` accessors (`getFraction`, `getDimensionPixelOffset`, `getResourceId`, …)

If we parse a raw XML file from disk, the “typed value” behavior is not guaranteed to match compiled resource XML (especially for resource references like `@integer/key_code_enter`).

Therefore:

- a runtime-loaded keyboard must avoid resource references (or we must implement a resolver), and
- we likely need a **file-based parser** that interprets strings into the needed units (fractions/dp/px/etc).

### 3) Backwards compatibility with ASK add-ons must remain intact

Whatever we do, we must not break:

- installed ASK keyboard packs (separate packages)
- ASK themes and other plugins

This strongly suggests:

- custom keyboards should be an _additional_ source of keyboards (like “local packs”),
  not a replacement for the existing add-on discovery.

### 4) Local packs must be treated as untrusted inputs

Even though packs are typically user-authored, we should assume packs can be malformed:

- Zip extraction must be hardened against path traversal (“zip slip”).
- Manifest + paths must be validated before using any pack contents.
- Runtime parsing must have reasonable limits (file sizes, key counts) to avoid OOM/ANR.

## Existing Infrastructure That Makes A Designer Feasible

### `keyboard-core`: file-based keyboard packs + validation

This repo already contains a file-based “pack” concept (currently used by linux-host tooling):

- Pack loader:
  - `keyboard-core/src/main/java/wtf/uhoh/newsoftkeyboard/keyboard/core/packs/FileSystemKeyboardPackLoader.java`
- Manifest model + JSON parser:
  - `keyboard-core/src/main/java/wtf/uhoh/newsoftkeyboard/keyboard/core/packs/PackManifest.java`
  - `keyboard-core/src/main/java/wtf/uhoh/newsoftkeyboard/keyboard/core/packs/PackManifestJson.java`
- ASK-XML keyboard parser (DOM, file-based):
  - `keyboard-core/src/main/java/wtf/uhoh/newsoftkeyboard/keyboard/core/parser/AskXmlKeyboardParser.java`
- Theme XML parser (file-based):
  - `keyboard-core/src/main/java/wtf/uhoh/newsoftkeyboard/keyboard/core/theme/ThemeXmlParser.java`
- Pack validator:
  - `keyboard-core/src/main/java/wtf/uhoh/newsoftkeyboard/keyboard/core/packs/KeyboardPackValidator.java`

Important detail: `AskXmlKeyboardParser` preserves **raw attributes** on each key, which is valuable for round-tripping.

Current gaps (important for a real designer/editor):

- `AskXmlKeyboardParser` currently does **not** preserve:
  - root `<Keyboard ...>` attributes (keyWidth/keyHeight/gaps, etc.)
  - `<Row ...>` attributes (rowEdgeFlags, row sizing, keyboardMode gating, etc.)
- `KeyboardPackValidator` currently validates “parses without throwing”, but does not enforce designer-level
  invariants like “must include Space/Delete/Enter” or “row widths don’t exceed screen”.

This is fine for the current Linux host smoke UI, but Phase 0/1 on Android will need richer
round-trippable models (keyboard/row attributes + richer validation) so edits don’t silently drop metadata.

This is the strongest foundation for a keyboard designer because:

- it already assumes file-based inputs (not compiled resources),
- it already has manifest/versioning/validation hooks,
- it can be shared across Android + Linux (one pack format).

#### Pack manifest schema (v1)

The current manifest schema is intentionally small:

- `schemaVersion` (defaults to `1`; other values rejected)
- `id` (string)
- `name` (string)
- `version` (int)
- `minCoreVersion` (optional string)
- `keyboards`: array of `{ "id": "...", "path": "keyboards/..." }`
- `themes`: array of `{ "id": "...", "path": "themes/..." }`

Paths are validated as pack-relative, forward-slash, and must not contain `..` or Android resource references.

Known gaps for Android selection UI (need a decision before Phase 0 UI work):

- Add-on keyboards have per-keyboard metadata outside the layout XML (display name, default dictionary locale,
  optional landscape layout, etc.). The current pack manifest only has `id` + `path`.
- For a good Android UX, we likely need at least:
  - per-keyboard display name (string)
  - optional `defaultDictionaryLocale`
  - optional “portrait vs landscape” representation (see later section)

Recommendation:

- Extend manifest entries with optional fields (keeping `schemaVersion: 1` if we only add optional fields).
  - `PackManifestJson` already skips unknown keys, so the format is forward-compatible.
  - Note: to _use_ those fields on Android, we’ll still need to extend the `PackManifest` model and parser
    in `keyboard-core` (the current Java object has only `{id,path}` per entry).

#### Pack theme format (portable subset)

`keyboard-core` currently supports a portable theme subset intended to work cross-platform:

- `<Color name="..." value="#RRGGBB|#AARRGGBB"/>`
- `<Icon name="..." path="icons/..."/>`

Android resource references (like `@color/...`) are rejected in pack themes.

Theme key naming (must be stable/portable):

- We should standardize `name="..."` onto stable identifiers, not resource IDs.
  - Recommended: use attribute-like names (e.g., `keyboardBackground`, `keyBackground`, `keyTextColor`,
    `hintTextColor`, `keyboardNameTextColor`) and a small, stable icon name set (e.g., `enterIcon`, `shiftIcon`,
    `deleteIcon`, `micIcon`).
- `scripts/convert_apk_to_pack.py` currently emits some theme keys as raw hex attribute IDs (except
  `keyboardBackground` which is already normalized). That is **not portable** across APKs.
  - Follow-up task: update the converter to map style item keys through `aapt2 dump resources` so it emits
    stable `attr/<name>` (or plain `<name>`) keys in the pack theme output.

## Recommended Architecture: “Custom Keyboard Packs” (File-Based)

### High-level idea

Add a new keyboard source: **user-owned keyboard packs** stored in app-private storage.

- A pack is a directory containing:
  - `manifest.json`
  - one or more `keyboards/*.xml`
  - optionally `themes/*.xml`
  - optionally `icons/*.png`

Example directory:

```
<app files>/keyboard_packs/
  my_pack_001/
    manifest.json
    keyboards/
      main.xml
      symbols.xml
    themes/
      blue.xml
    icons/
      enter.png
      mic.png
```

Then:

- a **pack repository** lists installed packs
- a **custom keyboard registry** exposes each pack keyboard as a selectable keyboard in the app
- the designer edits pack contents and writes them back to storage

### Where this fits in the current codebase

Keep ownership clear and avoid “helper sprawl” by having one owner per concept:

- `keyboard-core` owns:
  - pack format (manifest/schema)
  - file parsing/validation (AskXmlKeyboardParser / ThemeXmlParser)
- `ime/app` owns:
  - Android storage location for packs + pack install/uninstall (zip import/export)
  - Android UI (designer screens)
  - Android runtime loading and rendering + key handling

Recommended internal owners (Android-side, proposed classes/packages):

- `KeyboardPacksRepository` (single source of truth for what packs are installed and where they live)
- `KeyboardPacksInstaller` (zip import/export + validation, responsible for safe extraction)
- `PackKeyboardRuntimeLoader` (turns a pack keyboard file into runtime `KeyboardDefinition` objects)
- `CustomKeyboardFactory` (adapts installed packs into “selectable keyboards” for `KeyboardSwitcher`)

The goal is to keep:

- parsing/validation logic in one place (`keyboard-core`),
- Android runtime bridging in one place (runtime loader),
- UI in one place (designer screens),
- and avoid scattering “special cases” through `KeyboardKey` / `KeyboardXmlLoader`.

### How custom keyboards become selectable (integration points)

The app currently expects keyboards to come from `KeyboardFactory` (add-ons).

Two feasible integration patterns:

**Option 1 (cleaner): separate “Custom Keyboards” factory + switching support**

- Add a new `CustomKeyboardFactory` (not `MultipleAddOnsFactory`) that exposes `KeyboardAddOnAndBuilder`-like objects for file packs.
- Teach `KeyboardSwitcher` to merge the enabled list from:
  - `KeyboardFactory` (add-ons)
  - `CustomKeyboardFactory` (local packs)

This keeps the add-on system untouched.

**Option 2 (UI-only at first): treat custom keyboards as a special internal add-on**

- Surface custom keyboards only inside the settings UI first (preview + enable).
- Once stable, integrate into the main `KeyboardSwitcher` list.

Option 1 is the target; Option 2 is a safe stepping stone.

## Turning A Pack Keyboard XML Into A Working Android Keyboard

This is the main technical challenge.

### Problem statement

`KeyboardDefinition` currently loads from `@XmlRes` and depends on compiled resource parsing.

But our pack keyboard is a **raw XML file**.

We need a loader that:

1. parses raw XML
2. produces a runtime keyboard layout (keys with x/y/width/height/codes/etc.)
3. feeds it into the existing rendering/touch pipeline

### MVP strategy (recommended)

Keep the existing rendering/touch engine, but add a **file-based keyboard loader** parallel to `KeyboardXmlLoader`.

Concretely:

- Introduce a `PackKeyboardDefinition extends KeyboardDefinition`
  - accepts a parsed “layout spec” instead of `@XmlRes`
  - constructs `KeyboardKey`/`Row` objects programmatically
- Implement a parser that reads the same subset of attributes we already support
  - keyWidth/keyHeight/gaps
  - codes/labels/popupCharacters/longPress/hints/etc
  - rowEdgeFlags/mode gating

This avoids rewriting the entire keyboard view.

### Avoiding double-parsers (editing vs runtime)

We should avoid writing two totally separate keyboard XML parsers (one for “editing” and one for “runtime”).
The cleanest split is:

- `keyboard-core` parses XML into a “round-trippable” model (`KeyboardModel` / `KeySpec`) and preserves raw
  attributes.
- Android runtime code interprets the raw attributes (fractions/dimensions/enums) into the concrete runtime
  `KeyboardDefinition` / `KeyboardKey` objects.

This keeps “what does the file say?” separate from “how does Android render it?” and reduces duplicated logic.

### Typed-value compatibility surface (what we must support)

The compiled-resource loader gets a lot “for free” from Android’s resource system (fractions/dimensions/enums).
For packs, we must explicitly decide what we support in v1 and validate it.

Minimum practical support for real-world ASK/NSK keyboards:

- Fractions used heavily in layouts (e.g., `10%p`-style widths/heights).
- Plain numbers for `android:codes` (and `,`-separated lists).
- Plain strings for labels and popup characters.
- Edge flags and mode-gating as strings/enums.

Recommendation:

- For **designer-authored packs**: write numeric codes and pack-relative icon paths (no `@integer/...`).
- For **imported packs** (from existing add-ons): either
  - resolve known resource references at import time (preferred), or
  - preserve them as symbolic strings and surface “unresolved reference” warnings.

### Why not reuse `KeyboardXmlLoader` directly?

`KeyboardXmlLoader` takes `XmlResourceParser` and expects typed values (`TypedArray`).

We _can_ keep the structure of the loader (iterate Keyboard/Row/Key tags), but we should:

- switch to `XmlPullParser` parsing for file-based XML, and
- implement string → value parsing (fractions/dp/ints/enums) ourselves.

This keeps the parsing logic in one owner instead of scattering “special cases” throughout `KeyboardKey`.

### Avoiding resource references

User packs should avoid compiled-resource references like:

- `@integer/key_code_enter`
- `@drawable/...`
- `@xml/popup_one_row`

Instead:

- for key codes: write numeric values (use `com.anysoftkeyboard.api.KeyCodes` constants)
- for icons: use pack-relative icon paths (`icons/enter.png`)
- for popup keyboards: reference another pack keyboard by id (logical link), not `@xml/...`

We can still offer import-from-existing-keyboard and “resolve” those references during import:

- `@integer/key_code_enter` → actual int code at import time
- `@drawable/mike_rozoff_hint_mic` → copy the referenced drawable into the pack and rewrite as `icons/...`

This provides compatibility while keeping pack format self-contained.

### Popup keyboards in packs (explicit representation)

ASK add-ons often use `android:popupKeyboard="@xml/popup_..."`. For packs we should support a file-based form.
Two viable options:

1. **Path-based**: allow `android:popupKeyboard="keyboards/popup_one_row.xml"` when loading from a pack.
2. **Id-based**: introduce an NSK-only attribute (e.g., `nsk:popupKeyboardId="popup_one_row"`) and keep the
   original attribute only for add-ons.

Recommendation:

- Start with path-based values for packs (simple, keeps XML close to ASK shape).
- When importing an add-on, rewrite `@xml/...` popups into pack-relative paths.

## UI/UX Proposal: In-App Keyboard Designer

### Entry points

Recommended entry points in existing UI:

1. **Keyboards list** (`KeyboardAddOnBrowserFragment`)
   - add a “Create custom keyboard” button (top action or FAB)
2. A “Custom Keyboards” section under Language settings
   - `LanguageSettingsFragment` → keyboards tile → tab/switcher for “Installed” vs “Custom”

### Core screens

**1) Custom keyboard library**

- list of user keyboards (the user-facing unit is a “Keyboard”; packs are an implementation detail)
  - group by “Pack” only when the user expands “Advanced details”
  - search + filter (alphabet keyboards vs symbols/utility, recently edited)
  - sort by: pinned, recently edited, name
- each row/card includes:
  - keyboard name + optional pack name
  - small preview thumbnail (use `DemoKeyboardView#setOnViewBitmapReadyListener(...)`)
  - “Enabled” toggle (so the user doesn’t have to hunt through another screen)
- primary actions:
  - New (template / clone existing installed keyboard / import from file)
  - Edit
  - Duplicate
  - Export/share (zip)
  - Delete
- secondary actions (overflow):
  - Rename, change icon/preview, show validation report, “open pack folder” (debug/dev only)

New keyboard flow (wizard-style, minimal cognitive load):

1. Pick a base:
   - “Blank QWERTY”, “Blank 10-key”, “Blank symbols”, “Clone from installed keyboard”
   - “Import pack zip” (if the user already has a pack on disk)
2. Name + language metadata:
   - display name
   - optional default dictionary locale
3. Preview + confirm:
   - show “Base layout only” vs “As installed” (extensions) preview
   - run validator and show any warnings before creation

**2) Keyboard editor**

- large live keyboard canvas using an interactive view (`KeyboardView` with a “designer” action listener),
  not `DemoKeyboardView` (which intentionally does not handle touch events)
- tap a key → select + open key editor (bottom sheet / dialog)
- row editor:
  - add/remove row
  - adjust row defaults (keyWidth/keyHeight/gaps)
- layout controls:
  - “Add key”, “Delete key”, “Move key”
  - distribute/align (optional)
- mode/layers:
  - tabs for `alphabet`, `symbols`, etc (implemented as separate keyboards linked by custom-key switch key)

Editor UX details (recommended):

- Selection model:
  - Tap selects; long-press opens key editor; drag moves key within row (Phase 2).
  - A “Row mode” toggle makes dragging reorder rows instead of keys.
- Property surface:
  - Bottom sheet with three tabs: **Key**, **Row**, **Keyboard**.
  - Each tab shows only the common/high-value fields first; advanced fields are behind an “Advanced” expander.
- “As installed” preview toggles:
  - Toggle: **Show extensions** (top/bottom row add-ons on/off).
  - Toggle: **Preview theme** (current theme vs selected theme vs pack theme).
- Built-in validation panel:
  - Inline warnings (yellow) vs errors (red).
  - “Fix suggestions” for common issues (e.g., missing Symbols access).
- Undo/redo:
  - Essential for safe experimentation (especially when users drag keys/rows).

### Orientation variants (portrait vs landscape)

Today, a keyboard add-on can specify both portrait and landscape layouts (`layoutResId` and optional
`landscapeResId`). A designer needs the same capability.

For file-based packs, there are two workable representations:

1. **Two keyboards linked as variants (recommended for MVP)**
   - e.g., `main_portrait` and `main_landscape` are separate keyboard ids in the same pack.
   - the editor UI shows them as “Portrait / Landscape” tabs, but writes two XML files.
   - avoids a manifest schema bump.
2. **One keyboard entry with two paths**
   - a future manifest schema can add `portraitPath` + `landscapePath` on a single entry.
   - slightly cleaner data model but requires schemaVersion bump + migration.

MVP recommendation: implement (1) first, then migrate to (2) only if it becomes painful.

**3) Key editor dialog**

Minimum fields:

- Label (display)
- Output (commit text OR key code)
- Long-press output/code
- Width (percentage or “default”)
- Hint label / hint icon
- Popup characters / linked popup keyboard

Advanced fields (collapsed section):

- shifted codes/labels
- tags (emoji)
- showInLayout / isFunctional flags
- edge flags

### Preview and test typing

To avoid “design blind”:

- include a small “test field” in the editor screen
  - can be a fake editor (TextView) and feed events through the same pipeline the IME uses
  - or reuse existing demo typing simulator (but for realistic, real typing is better)

## Safety/Validation Rules (Non-hacky, Markov/UX-friendly)

A keyboard designer should prevent common foot-guns without forcing “hacks”:

Validation examples:

- Must contain: Delete, Space, Enter (or warn loudly).
- Must provide a way to reach symbols (or warn).
- Row widths should not overflow display width beyond tolerance.
- Disallow malformed UTF-16 or invalid code points.
- Validate long-press and popup links are resolvable.

Use the pack validator (`KeyboardPackValidator`) + Android-side validation (for platform constraints).

## Implementation Plan (Phased, Shippable)

### Phase 0 — Foundations (no UI editing yet)

Goal: “We can load a custom keyboard pack and use it as a keyboard.”

Deliverables:

- Android pack storage location + `KeyboardPacksRepository`.
- Zip import/export (`KeyboardPacksInstaller`) with safe extraction + pack validation.
  - Explicitly support packs produced by `scripts/export_keyboard_pack.py` and `scripts/convert_apk_to_pack.py`.
- `PackKeyboardDefinition` preview in `DemoKeyboardView`.
- A dev-only switch to set a pack keyboard as the active keyboard (to iterate safely).

Acceptance criteria:

- Importing a valid pack succeeds; invalid packs show actionable errors.
- A pack keyboard can be previewed and (in dev mode) set active without breaking ASK add-ons.
  - “Breaking” includes crashing, missing keyboard list entries, or overriding ASK layouts.

### Phase 1 — Minimal designer (editing keys)

Goal: “User can create a keyboard from a template and edit keys.”

- Add “Create custom keyboard” flow:
  - clone from a template pack shipped with the app (no APK/resource resolution required)
  - optional: clone an installed add-on keyboard by resolving references at import-time
    - if we can’t resolve a given reference (rare edge cases), surface warnings and keep the keyboard usable
- Implement key editor dialog:
  - edit label/codes/long-press/width/hints
- Save and re-render preview live.
- Enable the custom keyboard as a selectable keyboard.

### Phase 2 — Layout editing + popups + multiple layers

Goal: “Full layout designer.”

- Add row editor + add/move/remove keys.
- Implement popup keyboard editing and linking.
- Support “symbols” keyboard as separate pack keyboard linked by `CUSTOM_KEYBOARD_SWITCH`.

### Phase 3 — Theme designer (optional)

- Allow creating a pack theme (colors + icon overrides).
- Theme preview + apply to editor preview.

## Testing Strategy (to keep regressions low)

Unit tests (host/JVM where possible):

- pack manifest round-trip
- XML parse/serialize for custom keyboard spec
  - include keyboard-level + row-level attributes (so edits don’t drop important metadata)
- pack theme key normalization (stable names; no raw resource IDs in the exported pack format)
- installer safety (zip-slip/path traversal, overwrite rules, max sizes)
- validation rules (missing keys, bad sizes, broken popup links)

Android instrumentation:

- pack runtime:
  - import pack zip → enable custom keyboard → type a short sentence → verify committed output
  - export pack → re-import → behavior unchanged
- compatibility baseline:
  - ensure external ASK add-ons are still discoverable and unaffected:
    - `ime/app/src/androidTest/java/wtf/uhoh/newsoftkeyboard/compat/addons/cts/ExternalAddOnSmokeInstrumentedTest.java`

## Open Questions / Decisions Needed

1. **Pack format for special keycodes**: numeric only vs symbolic names.
   - Numeric is simplest and self-contained.
   - Symbolic is nicer for human editing but needs a stable mapping table.
2. **Where to store packs**:
   - `files/keyboard_packs/` makes sense for user-authored content (and allows Android backup if enabled).
   - `no_backup/` is consistent with how models are stored in this repo, but may be undesirable for user content.
   - Recommendation: start with `files/` and add export/import so users can back up explicitly.
3. **Import scope**:
   - Support importing a compiled add-on keyboard into a pack (template).
   - This is highly desirable: users start from a known-good layout.
4. **Designer UX**:
   - “Dialog” vs full-screen editor:
     - a full-screen editor is more realistic; the “key editor” can be a dialog.
5. **Pack themes on Android**:
   - The current theme system is resource/style driven (`KeyboardTheme` uses `@style` + `DrawableBuilder` loads `@drawable`).
   - For pack themes (file-based), we likely need a bridging layer:
     - apply pack colors/icons as overrides on top of a selected base theme (recommended), or
     - introduce a new “file theme” type and teach the view to load icons/colors without relying on resource IDs.
   - Either way, the portable theme format must use stable keys (see “Theme key naming” above).

## Summary (Recommendation)

The cleanest path is:

- implement **Custom Keyboard Packs** stored on-device,
- reuse `keyboard-core` pack parsing/validation as the format baseline,
- add an Android-side loader that builds `KeyboardDefinition` objects from pack keyboards,
- build the UI as:
  - preview thumbnails via `DemoKeyboardView`
  - an interactive editor surface via `KeyboardView` + designer-specific action handling

This fits existing boundaries, avoids runtime resource-generation traps, preserves ASK add-on compatibility, and sets up a shared pack format that can later be used across Android + Linux.
