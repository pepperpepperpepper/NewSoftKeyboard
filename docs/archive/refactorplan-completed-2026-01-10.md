# Archived sections from `refactorplan.md` (completed)

Moved on: 2026-01-10

This file contains refactor plans removed from `refactorplan.md` because they are implemented.

---

# Refactor Plan: split `KeyboardWallpaperOverrideStore.java` into ~3 files

Target file:
`ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/theme/KeyboardWallpaperOverrideStore.java` (~1593 LOC)

## Goal

Reduce risk and make future wallpaper work safer by separating 3 distinct concerns:

1. **SharedPreferences key registry + scalar CRUD** (wallpaper mode, blend modes, opacities, etc).
2. **Bulk ops** (clear + copy) so new prefs can’t be accidentally missed.
3. **Wallpaper file I/O** (import, decode/scale, EXIF best-effort, file naming).

Non-goals:

- No UX/behavior changes.
- No preference key/value migration (keys must remain byte-for-byte identical).
- No new dependencies.
- No API changes at call sites (keep `KeyboardWallpaperOverrideStore` public facade).

## Current anatomy (why it’s monolithic)

`KeyboardWallpaperOverrideStore` currently mixes:

- **Key registry**: ~30 `PREF_*_PREFIX` constants + `*Key(themeId)` builders.
- **Per-setting CRUD**: dozens of `get*/set*/clear*` methods for ints/bools/strings.
- **Layer-stack serialization + legacy fallback**:
  - JSON `KeyboardWallpaperLayer[]` encode/decode.
  - Legacy “layer order” string parsing and “buildLegacy\*LayerStack”.
- **Bulk ops**: `clear(themeId)` and `copyToTheme(source, target)` (long hand-maintained lists).
- **File store**: `getWallpaperFile`, `hasWallpaper`, `importFromUri`, downscale decode, EXIF rotation,
  hashing, and file copy helpers.

This makes the class hard to reason about and very error-prone when adding a new preference:
it’s easy to forget to include it in `clear(...)` and/or `copyToTheme(...)`.

## Proposed split (3 files total, keep public API stable)

### 1) Keep: `KeyboardWallpaperOverrideStore.java` (public facade + orchestration)

Responsibilities after refactor:

- Own `Context appContext` and `SharedPreferences prefs` (still uses `DirectBootAwareSharedPreferences`).
- Keep all **public methods and constants** currently used by call sites/tests:
  - `hasWallpaper`, `getWallpaperFile`, `importFromUri`, `clear`, `copyToTheme`, all getters/setters.
  - Static normalizers used by UI (`normalizeBlendMode`, `normalizeRotationDegrees`) remain here to avoid
    wide call-site edits (can delegate internally).
- Construct and delegate to:
  - `KeyboardWallpaperOverridePrefs prefsDelegate`
  - `KeyboardWallpaperFileStore fileDelegate`
- Orchestrate cross-cutting operations:
  - `clear(themeId)` should delete the file (fileDelegate) + clear prefs (prefsDelegate) + bump change token.
  - `copyToTheme(source, target)` should copy the file (fileDelegate) + copy prefs (prefsDelegate) + bump token.
  - `importFromUri(...)` should write the file (fileDelegate) + apply “first import defaults” (prefsDelegate).

### 2) New: `KeyboardWallpaperOverridePrefs.java` (keys + scalar prefs + bulk ops)

Package: `wtf.uhoh.newsoftkeyboard.app.theme` (package-private `final class`)

Responsibilities:

- Own the **entire preference key registry**:
  - all `PREF_*_PREFIX` constants and `*Key(themeId)` builders.
- Own scalar operations:
  - dim/mode/alphas/blend modes/rotation/scale/anchor/match-key-shape/quality/vignette/gradient/grain/
    saturation/contrast/invalid flag/import-high-quality flag.
- Own change-token plumbing:
  - `markWallpaperChanged(themeId, editor)` and `getWallpaperChangeToken(themeId)` (same semantics).
- Own serialization helpers for pref-backed formats:
  - `parseLayerOrder/serializeLayerOrder/normalizeLayerOrder`
  - `parseLayerStack/serializeLayerStack`
  - `buildLegacyBackgroundLayerStack/buildLegacyKeyLayerStack` (legacy fallback when JSON isn’t present)
- Centralize bulk ops so they can’t drift:
  - `clearAllPrefs(themeId, editor)` (only pref removals; file deletion handled elsewhere)
  - `copyAllPrefs(sourceThemeId, targetThemeId, editor)` (copy/remove logic, including “has override” semantics)
  - Implement copy/remove via small helpers:
    - `copyIntOrRemove(...)`, `copyStringOrRemove(...)`, `copyBooleanOrRemove(...)`

Note: keep the “first import defaults” logic together:

- “default to visible mode on first import” (and the backwards-compat comment) should be a single helper:
  - `applyFirstImportDefaults(themeId, editor, hadExistingWallpaper, exifRotationDegrees)`

### 3) New: `KeyboardWallpaperFileStore.java` (file path + import + bitmap decode)

Package: `wtf.uhoh.newsoftkeyboard.app.theme` (package-private `final class`)

Responsibilities:

- Own wallpaper directory + file naming (hash-to-filename + `.webp` extension must remain identical).
- Own file ops:
  - `File getWallpaperFile(themeId)`
  - `boolean hasWallpaper(themeId)`
  - `void deleteWallpaper(themeId)` (best-effort)
  - `void copyWallpaperFile(sourceThemeId, targetThemeId)`
- Own import pipeline:
  - `ImportResult importFromUri(themeId, sourceUri, maxW, maxH, highQuality)`:
    - Read EXIF rotation best-effort (reflection-based API 24+).
    - Decode bounds + decode downscaled bitmap + optional additional scaling.
    - Encode as WEBP with the existing quality behavior (90 vs 100).
    - Return `hadExistingWallpaper` + `exifRotationDegrees`.
  - Keep all OOM handling semantics (wrap as `IOException`, recycle bitmaps, never crash).
- Own pure helpers:
  - `calculateInSampleSize`, `hashToFileName`, `copyFile`.

## Step-by-step implementation plan (mechanical, low risk)

1. Create `KeyboardWallpaperFileStore` and move:
   - `getWallpaperFile`, `hasWallpaper`, `importFromUri` decode+write pieces, EXIF reader, hash+copy helpers.
2. Create `KeyboardWallpaperOverridePrefs` and move:
   - all pref key constants/builders
   - scalar getters/setters
   - layer stack codecs + legacy stack builders
   - clear/copy pref logic
3. Reduce `KeyboardWallpaperOverrideStore` to a facade:
   - delegate all scalar accessors to prefsDelegate
   - orchestrate file+pref operations for `clear`, `copyToTheme`, `importFromUri`
4. Run targeted tests and assemble:
   - `./gradlew :ime:app:testNskDebugUnitTest`
   - `./gradlew :ime:app:assembleNskDebug`

## Implementation status

Implemented (2026-01-10):

- Added `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/theme/KeyboardWallpaperFileStore.java`.
- Added `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/theme/KeyboardWallpaperOverridePrefs.java`.
- Updated `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/theme/KeyboardWallpaperOverrideStore.java` to be a facade delegating to the above.
- Verified with `./gradlew :ime:app:testNskDebugUnitTest` and `./gradlew :ime:app:assembleNskDebug`.

## Gotchas to preserve

- Preference keys must remain identical (no migration).
- “First import defaults” behavior:
  - default mode should stay `BACKGROUND_KEY_TINT` on first import, and the “legacy background-only persisted”
    edge case must keep working.
  - default key alpha should still be set to 60 on first import (only when no existing keyAlpha pref).
- Change-token must still bump exactly once per edit batch (tests depend on it).
- Keep best-effort behavior: user storage ops must never crash the app (delete/copy/import are resilient).

---

# Refactor Plan: split `KeyboardThemeCustomizationWallpaperLayerStackEditorDialog.java` into ~3 files

Target file:
`ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ui/settings/KeyboardThemeCustomizationWallpaperLayerStackEditorDialog.java` (~1570 LOC)

## Goal

Cut the file roughly in half (or better) by separating:

1. **Dialog orchestration** (open/close, revert-on-cancel, apply-to-store).
2. **Layer-card rendering + per-layer editor widgets** (opacity/blend/direction/scale/color).
3. **Gradient-stops editor dialog** (a nested dialog with its own mini-UI + interpolation math).

Non-goals:

- No UX/behavior changes (same controls, same defaults, same live-preview updates).
- No new dependencies.
- Keep call sites stable (`show(...)` and `describeLayerStack(...)` signatures remain).

## Current anatomy (why it’s monolithic)

This file is effectively 3 UIs + a utility library in one:

- Outer dialog state + revert semantics (initial stack snapshot, `accepted/changed`, restore on cancel).
- Large inline renderer (`render[0]`) that builds per-layer cards and wires:
  - enable switch
  - move up/down, duplicate, remove
  - opacity slider
  - blend mode chooser dialog
  - direction chooser (for gradient/highlight/stripes)
  - scale slider (for grain/dots/grid/stripes/blur/checker)
  - type-specific color pickers (plus checker alternate color)
  - gradient-stops “advanced editor” entry point
- Nested gradient-stops editor dialog (another list+cards UI).
- Static helpers: labels, color formatting, lerp, dp conversion, gradient-stop bookkeeping.

## Proposed split (3 files total, keep public entry points)

### 1) Keep: `KeyboardThemeCustomizationWallpaperLayerStackEditorDialog.java` (facade)

Responsibilities after refactor:

- Keep the public entrypoints used by `KeyboardThemeCustomizationBackgroundSection`:
  - `static void show(...)`
  - `static String describeLayerStack(...)` (can delegate internally, but keep signature)
- Own only:
  - reading initial stack from `KeyboardWallpaperOverrideStore`
  - the accepted/cancel revert logic
  - constructing the editor controller and showing the dialog

### 2) New: `KeyboardThemeCustomizationWallpaperLayerStackEditorController.java` (UI builder + per-layer editors)

Package: `wtf.uhoh.newsoftkeyboard.app.ui.settings` (package-private `final class`)

Responsibilities:

- Own `ArrayList<KeyboardWallpaperLayer> stack` mutation and the `applyToStore` callback.
- Build the root view (button row + list container) and expose:
  - `View buildContentView()`
  - `void render()` (rebuilds list cards)
- Contain all per-layer “card” creation logic (currently inside the giant `for` loop):
  - `View buildLayerCard(index, layer)`
  - small helpers to reduce repetition:
    - `KeyboardWallpaperLayer copyWithEnabled(...)`
    - `copyWithOpacity(...)`
    - `copyWithBlendMode(...)`
    - `copyWithDirection(...)`
    - `copyWithScale(...)`
    - `copyWithArgb(...)` / `copyWithArgb2(...)` / `copyWithGradientStops(...)`
- Keep _live preview_ behavior identical:
  - call `applyToStore.run()` at the same times as today (seekbar movement vs stop-tracking).

### 3) New: `KeyboardThemeCustomizationWallpaperGradientStopsEditorDialog.java` (nested dialog)

Package: `wtf.uhoh.newsoftkeyboard.app.ui.settings` (package-private `final class`)

Responsibilities:

- Own `show(...)` for the gradient-stops editor:
  - inputs: context, mutable stack, layerIndex, applyToStore, renderOuter
  - fully encapsulates the inner dialog UI and stop list logic.
- Own the stop-edit math currently embedded:
  - “insert into largest gap” + `lerpArgb`
  - sorting/validation (>=2 stops) and updating the parent layer’s `argb/argb2/gradientStops`.

Shared static helpers (labels/colors/dp) should live in **one place** to avoid duplication.
Pick either:

- A small new util file: `KeyboardThemeCustomizationWallpaperLayerUiUtil.java`, OR
- Put them as package-private statics on the controller class and have both dialog classes use it.

(Either way, the goal is “one owner” for label/formatting logic.)

## Step-by-step extraction plan (mechanical, low risk)

1. Extract `KeyboardThemeCustomizationWallpaperGradientStopsEditorDialog` first (largest isolated chunk).
   - Keep the outer call site identical:
     - `editStops.setOnClickListener(... -> GradientStopsEditorDialog.show(...))`
2. Extract `KeyboardThemeCustomizationWallpaperLayerStackEditorController`:
   - Move the big `render[0]` lambda and the per-layer card building into the controller.
   - Keep the same list rebuild strategy (`removeAllViews()` and rebuild).
3. Reduce the facade file:
   - `show(...)` becomes orchestration + dialog lifecycle only.
   - Keep `describeLayerStack(...)` signature (delegate if desired).
4. Run unit tests and assemble:
   - `./gradlew :ime:app:testNskDebugUnitTest`
   - `./gradlew :ime:app:assembleNskDebug`

## Implementation status

Implemented (2026-01-10):

- Updated `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ui/settings/KeyboardThemeCustomizationWallpaperLayerStackEditorDialog.java` to a facade (kept `show(...)` and `describeLayerStack(...)` signatures).
- Added `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ui/settings/KeyboardThemeCustomizationWallpaperLayerStackEditorController.java` with the outer editor UI + shared helper methods.
- Added `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ui/settings/KeyboardThemeCustomizationWallpaperGradientStopsEditorDialog.java` for the nested gradient-stops editor.
- Verified with `./gradlew :ime:app:testNskDebugUnitTest` and `./gradlew :ime:app:assembleNskDebug`.

## Gotchas to preserve

- Revert-on-cancel semantics:
  - if the dialog is dismissed without “OK”, restore the initial stack (or clear override) exactly as today.
- Disabled-state behavior:
  - when a layer is disabled, its controls must stay disabled (opacity/blend/direction/scale/color buttons).
- Gradient defaults:
  - the “2-stop fallback” behavior must remain identical when the layer has no stored stops.
- No changes to the user-facing string resources or ordering of layer-type choices.
