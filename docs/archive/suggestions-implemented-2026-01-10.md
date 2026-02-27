# Archived sections from `suggestions.md` (implemented)

Moved on: 2026-01-10

This file contains sections removed from `suggestions.md` because they describe work that is implemented.

---

## Status (this branch)

Implemented now (P0 + P1 + selected P2/P3):

- A single, preview-first appearance editor in `KeyboardThemeCustomizationFragment` (no nested sub-screens for features) with a sticky live preview.
- A pinned “section switcher” bar (Presets/Background/Colors/Typography/Shadows/Overlays/Reset) to keep navigation flat and previews obvious.
- Theme selector “Customize” routes directly to the owner (no mixed-scope “Tweaks” editor surface).
- Presets (per base theme): create/rename/delete, switch active preset, “Reset preset”, export/import (including export preview image + richer metadata; import preview includes preview image + rename + optional “set active”), and a preset picker dialog that shows cached per-preset preview thumbnails (saved on create/import/selection/export).
- Per-app presets (per base theme): bind last used app → current preset, bind any installed app → current preset (searchable picker), and manage bindings at scale (search/sort, per-binding actions, bulk clear). Applied only while IME is active.
- Wallpaper performance: shared async decode cache + cheap list previews + “Quality” guardrails.
- Key-face wallpaper overlays now apply the same photo effects as the background (gradient/vignette/grain), while cheap previews keep this off and avoid union-mask compositing.
- Key photo blend mode for key-face wallpaper overlays (Normal/Multiply/Screen/Overlay/Soft light), disabled in cheap previews.
- Layer stack v2: background + key overlay layer stacks support add/remove/reorder/duplicate, per-layer enabled + opacity + blend mode, per-layer direction (vertical/horizontal + reversed) for directional layers, and per-layer colors for solid/color-wash/gradient/highlight/vignette; “Soft light” is supported (Android 10+; falls back safely). Added per-layer blur, grain size (scale) and pattern layers (Dots, Grid, Stripes, Diagonal stripes, Triangles, Hexagons, Checkerboard with optional alternate color). Layer stack editor rows include mini previews and a per-layer Basic/Advanced toggle (direction/scale/stops/alternate color live in Advanced).
- Typography + shadows: key + hint font family/style + custom font import (TTF/OTF), key-label text shadow + key background shadow (including per-key-type overrides and key shadow spread).
- Shadow presets: text shadow style presets (Theme default/Off/Subtle/Strong) and key shadow style presets (Flat/Soft/Elevated).
- Custom font robustness: import validates font signature; warns when a selected custom font is missing or unloadable.
- Per-surface typography: separate fonts for keys, hints, suggestions strip, and keyboard name (spacebar).
- Live preview now includes the suggestions strip so typography changes are immediately visible.
- Text size presets (Smaller/Small/Large/Larger) per-surface: key labels, hints, suggestions strip, keyboard name (with “Same as key” and “Theme default (100%)” options for non-key surfaces).
- Live preview quick toggles: Suggestions strip on/off + Shift on/off + Typing demo on/off + “Show original (no edits)” before/after toggle.
- Live preview quick action: “Open keyboard” (launches `TestInputActivity` for full IME testing).
- Readability: a live “Readability” status (warnings) + an “Ensure readable text” toggle that auto-adjusts key/hint text colors (debounced on dim changes).
- Auto-contrast quick actions: “Auto-pick readable text colors” and “Apply high contrast (dark)”.
- Wallpaper-based scheme generator: “Auto-match photo colors” with intensity + special-keys intensity + target toggles (keyboard background tint, key background tint, special keys, text colors, text shadow).
- Genymotion gate: `scripts/genymotion_theme_smoke.sh` runs debug-only instrumentation smokes and captures artifacts.
- Preview clarity: tap any key shows a pressed-state preview; long-press opens Inspect even if Inspect toggle is off; Inspect highlights the key and can “jump to section”. Added Focus chips (Auto/Background/Keys/Text/Suggestions/Overlays) that highlight preview regions.
- Preview layouts: a simple layout chooser (Full/Compact/One-handed left/right/Landscape scaled) for quick sanity-checks while editing.
- Overrides summary: “What changed?” now lists per-setting overrides with per-setting Reset + “jump to owner” (no nested editor surfaces).
- Owner-duplication enforcement: added a single-owner registry + navigation helper for appearance shortcuts, plus tests that ensure `settings_key_*` controls are not editable outside their single owner screen (Preference-XML + code-driven status-only surfaces; deep links only).

---

### 5.2 v2+ IA (implemented here)

Status: implemented (this branch). This includes:

- A **Preset** model (fork/save/export/import)
- A preview-first, non-nested editor (`KeyboardThemeCustomizationFragment`) that combines preset selection + editing

We can then optionally rename “Look & feel” → “Appearance” (pure UX/labeling), but this should not block the functional work.

---

## 6) Feature Set (Roadmap)

This is organized by priority. “P0” is the minimum viable redesign that already unlocks big improvements.

### P0 — v1 Must-have (foundation that fits today)

Status: implemented (this branch).

- **Remove duplicated controls + make scope obvious**
  - Eliminate `KeyboardThemeTweaksFragment` as a mixed-scope editor.
  - Ensure each appearance feature has a single owner (global vs per-theme vs system overlay).
- **Background photo editor becomes preview-first**
  - Add a live keyboard preview to `KeyboardThemeCustomizationFragment`.
  - Keep existing controls (dim/rotate/scale/anchor/mode/opacity).
  - Clearly label expensive options (match-key-shape) and disable them in list previews.
- **Make settings lists fast**
  - Add a cheap preview mode for `AbstractAddOnsBrowserFragment` grid items to prevent sluggish “Manage Keyboards” / theme browsing.

This P0 set is the minimum to stop the UI/UX from re-tangling and to remove the worst performance pain.

### P0.1 — Genymotion Gate (required before commit/push)

Status: implemented (this branch), with a manual checklist + debug-only scripted smokes.

We cannot “trust” local unit tests for this work. **Before committing/pushing** we must verify the UX + performance on a real emulator runtime (Genymotion), because this is where we see:

- RecyclerView grids (“Manage keyboards”, theme selector) behavior under real frame scheduling.
- GPU/Canvas behavior for masking + `saveLayer(...)`.
- Any accidental main-thread bitmap decode or UI-thread stalls.

This is intentionally split into (A) a manual checklist and (B) a scripted smoke suite we should add so this stays repeatable.

#### A) Manual Genymotion checklist (fast, human visual verification)

**Device prerequisites**

- Genymotion device is booted and visible in `adb devices` (optionally set `GENYMOTION_DEV` to the device serial).
- Start from a clean-ish state if needed: clear app data, or at least clear wallpapers for the active theme.

**Port binding (important)**

We use Genymotion SaaS via the `gmsaas` ADB tunnel, which forwards an instance to a local `localhost:<port>` “device”.

- **Decision:** use `localhost:35329` by default.
  - Rationale: `localhost:4400` is commonly reserved/claimed by other Genymotion/Windows tooling; keeping the tunnel on
    a dedicated port avoids collisions and “wrong device” confusion.

If the tunnel is on the wrong port, rebind it explicitly:

1. List the running instance and UUID:
   - `gmsaas --format text instances list`
   - (If `gmsaas` isn’t in PATH in this environment: `/home/arch/.venvs/gmsaas/bin/gmsaas --format text instances list`)
2. (Optional) disconnect any existing ADB mapping:
   - `gmsaas instances adbdisconnect <INSTANCE_UUID>`
3. Connect the instance on the chosen port:
   - `gmsaas instances adbconnect --adb-serial-port 35329 <INSTANCE_UUID>`
4. Verify:
   - `adb devices -l` shows `localhost:35329 device`
   - `sudo ss -ltnp 'sport = :35329'` shows `gmadbtunneld` listening

**Navigation / “single-owner” rules**

1. Open: Settings → Look & feel → Theme → **Select theme**
   - The top menu action is labeled **Customize** (not “Tweaks”).
   - Tapping the menu action opens **Customize appearance** directly (no intermediate screens).
   - The **Customize current theme** row opens **Customize appearance** directly.
2. Open: Settings → Look & feel → Theme → **Change theme colors based on used app**
   - This is **status-only** (no checkbox here).
   - Tapping it deep-links to **Select theme**, and the toggle is adjusted only there.

**Live preview correctness**

3. Open: Settings → Look & feel → Theme → **Customize appearance**
   - A **Live preview** keyboard is visible at the top without scrolling.
   - After setting a photo (Background section), the preview shows the photo effect anchored to the full keyboard (continuous across keys).
4. While watching the live preview, adjust:
   - **Dim background** (confirm the preview darkens _while dragging_).
   - **Wallpaper mode** (Background only / Background + key tint / Background + key texture).
   - **Key opacity** (confirm key overlay strength changes _while dragging_ in tint/texture modes).
   - **Match key shape (slower)** (confirm it toggles clipping behavior; expect some cost, but no “hangs”).
   - **Rotate photo**, **Photo scale**, **Photo anchor** (confirm effect matches wallpaper transform expectations).
   - **Reset background photo** (confirm preview returns to theme wallpaper).

**Performance sanity**

5. Open: Settings → Keyboards & language packs → **Manage keyboards**
   - Initial load should not “stutter” for several seconds.
   - Scrolling should feel stable (no repeated heavy stalls when wallpaper + key overlays are enabled).
6. Log sanity:
   - There should be **no** debug log warnings about wallpaper decode happening on the main thread.
   - Use `scripts/adb.sh` with `GENYMOTION_DEV` if helpful for filtered logcat.

#### B) Scripted Genymotion smoke (implemented)

Goal: make the above repeatable and objective so “Genymotion vetting” is not a memory-based manual ritual.

**Add androidTest coverage (UI + integration)**

We have a small instrumentation suite (Espresso + UIAutomator, already in our deps) that asserts:

1. **ThemeCustomizationNavigationSmokeTest**
   - Theme selector menu “Customize” and “Customize current theme” row both open `KeyboardThemeCustomizationFragment`.
2. **BackgroundPhotoLivePreviewSmokeTest**
   - Inject a known wallpaper for the current theme (resource URI) and verify the live preview background becomes a `LayerDrawable` (photo + dim overlay) within a bounded timeout.
   - Change dim/key opacity values and assert the preview invalidates/updates (at minimum, no crashes and state changes are reflected).
3. **CheapPreviewGuardSmokeTest**
   - Open “Manage keyboards” and assert list/grid item `DemoKeyboardView`s have expensive wallpaper effects disabled (reflection is acceptable in tests).
4. **NoMainThreadDecodeSmokeTest**
   - Clear logcat, exercise the screens that trigger wallpaper loads, dump logcat, and assert no “decode on main thread” warning is present.

**Single script to run it all on Genymotion**

Use `scripts/genymotion_theme_smoke.sh` which:

- Targets `GENYMOTION_DEV` if set (otherwise uses the first `adb` device).
- Builds **without** using Gradle `build`:
  - Default smoke (stable, non-minified): `TEST_BUILD_TYPE=debug ./gradlew :ime:app:assembleNskDebug :ime:app:assembleNskDebugAndroidTest`
  - Note: **release androidTest is minified/obfuscated**, and in practice it can break the instrumentation stack (Espresso/UIAutomator) and/or app-level test hooks with `NoSuchMethodError` due to R8 shrink/optimizations.
  - **Decision:** the automated Genymotion smoke suite is **debug-only**.
- Installs:
  - Debug: `ime/app/build/outputs/apk/nsk/debug/app-nsk-debug.apk` + `ime/app/build/outputs/apk/androidTest/nsk/debug/app-nsk-debug-androidTest.apk`
- Runs the smoke classes via:
  - `adb shell am instrument ... wtf.uhoh.newsoftkeyboard.test/androidx.test.runner.AndroidJUnitRunner`
- Captures artifacts into `outputs/genymotion/theme-smoke/<timestamp>/`:
  - `logcat` dump
  - optional `dumpsys gfxinfo wtf.uhoh.newsoftkeyboard framestats` before/after a scripted scroll

Quick usage:

- Debug instrumentation smoke: `GENYMOTION_DEV=localhost:35329 scripts/genymotion_theme_smoke.sh`
- Release install helper (no instrumentation; manual checklist still required): `TEST_BUILD_TYPE=release GENYMOTION_DEV=localhost:35329 scripts/genymotion_theme_smoke.sh`

Once this exists, **Genymotion becomes a real gate**: no commit/push until the script passes.

#### C) “Release build” gate (manual; required for shipping/F-Droid)

We still must validate the **real shipped build** (release/minified) — but we do it as a **manual Genymotion run**, not as instrumentation.

**Why (decision rationale)**

- Running Espresso against a minified release APK is fragile: R8 can remove/reshape methods and shrink Kotlin stdlib pieces that Espresso uses, leading to runtime `NoSuchMethodError` even when the app itself is fine.
- Even if we added keep rules, we would risk “testing a special release variant” (not what users get) or weakening shrink/obfuscation in production.

**What we do instead**

Shortcut (build + install only): `TEST_BUILD_TYPE=release GENYMOTION_DEV=<serial> scripts/genymotion_theme_smoke.sh`

1. Ensure release builds:
   - `./gradlew :ime:app:assembleNskRelease`
2. Install the release APK on Genymotion:
   - `adb -s "$GENYMOTION_DEV" install -r -d ime/app/build/outputs/apk/nsk/release/app-nsk-release.apk`
3. Run the “Manual Genymotion checklist” above again on the release build (especially “Manage keyboards” smoothness + live preview responsiveness).
4. Capture basic artifacts for regressions:
   - `adb -s "$GENYMOTION_DEV" logcat -c`
   - Reproduce the interactions
   - `adb -s "$GENYMOTION_DEV" logcat -d > outputs/genymotion/theme-smoke/<timestamp>/logcat_release_manual.txt`

This makes “release correctness” real, without blocking the workflow on brittle release instrumentation.

### P1 — v2 “Make it real” (presets + saving)

Status: implemented (this branch).

- **Preset system**
  - Create preset from current theme.
  - Duplicate/rename/delete presets.
  - Each preset stores: base theme id + overrides.
- **Forking/saving**
  - “Save as new preset” (duplicate current preset under a new name)
  - “Reset preset to base theme defaults”
- **Simple per-preset overrides**
  - Key text color
  - Hint text color
  - Keyboard background tint (or overlay)
  - Key background tint (applied as color filter over the base key background)
