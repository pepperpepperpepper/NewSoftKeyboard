# Photo Background Per Theme — TODO

## Decision (UX)

- Background image should be **per-theme**, because it’s part of the theme’s visual identity (like key shapes/colors/icons).
- Must be safe + reversible:
  - Default state is **Theme default background** (no photo override).
  - Add **Background image (for this theme)** → pick from gallery.
  - Add **Opacity/Dim controls** so keys remain readable.
  - Add **Reset this theme’s customizations** (removes photo + related sliders for that theme).
  - Optional: **Apply this background to all themes** (copies current theme’s override to the rest).

## Current State (what’s blocking this today)

- Theme API already defines `keyboardWallpaper` (behind the keyboard) in `api/src/main/res/values/keyboard_theme_api.xml`.
- Base theme sets `keyboardWallpaper` in `ime/app/src/main/res/values/styles_base_keyboard_theme.xml`.
- Runtime currently hardcodes the IME container background to `R.drawable.nsk_wallpaper` in `ImeBase.onCreateInputView()`,
  which prevents themes (and any “gallery wallpaper”) from owning this layer reliably.

## Architectural Boundaries (ownership)

- **Keyboard wallpaper** (image behind the entire IME view) should be owned by the **IME container** layer:
  - `ImeBase` + `KeyboardViewContainerView` is where this background should be applied.
- **Keyboard/key backgrounds** (inside the drawn keyboard view) remain owned by:
  - `KeyboardViewBase` + `ThemeAttributeLoader` + `ThemeOverlayCombiner`.
- The per-theme photo feature is an **override** on top of `keyboardWallpaper`:
  - If photo is set for the active theme, use it.
  - Else fall back to theme’s `keyboardWallpaper`.

## Data Model (prefs)

- Store per-theme settings keyed by the theme id (from `KeyboardTheme.getId()`):
  - `photo_wallpaper_uri::<themeId>` = string (persisted content URI)
  - `photo_wallpaper_opacity::<themeId>` = int/float (e.g., 0–100)
  - Optional: `photo_wallpaper_scale::<themeId>` = enum (fit/centerCrop/etc.)
- Provide a single “reset” that deletes all keys for that theme id.

## Storage + Permissions (Android)

- Use `Intent.ACTION_OPEN_DOCUMENT` so we can keep a long-term read grant.
- Call `takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION)`.
- Do not copy the original image to shared storage. Keep it app-private by URI access.
- On failure (URI revoked, missing provider, permission lost):
  - Fall back to theme `keyboardWallpaper` and surface a “Re-pick image” action.

## Implementation Steps

### 1) Stop hardcoding the wallpaper

- Remove/replace `mInputViewContainer.setBackgroundResource(R.drawable.nsk_wallpaper);` in `ImeBase.onCreateInputView()`.
- Instead, set background via a single owner path (theme/override aware), e.g. through `ImeThemeOverlay` or a new
  `KeyboardWallpaperController` that:
  - Reads current theme id + overlay settings.
  - Chooses one of:
    - per-theme photo background (if valid),
    - else theme `keyboardWallpaper`,
    - else fallback drawable.

### 2) Add settings UI under Theme Tweaks

- Add preferences under keyboard theme tweaks (or theme selector screen) for:
  - Pick background image (per theme)
  - Reset theme customizations
  - Opacity/Dim slider
  - Optional: Apply to all themes
- Ensure the UI clearly indicates “for this theme”.

### 3) Decode + apply the image safely

- Decode off the UI thread.
- Size appropriately (IME view size / screen width) to avoid OOM.
- Use `ImageDecoder` on newer APIs; use `BitmapFactory` fallback where needed.
- Apply alpha/overlay:
  - Prefer a controlled alpha on the photo drawable (or a dim overlay layer) rather than mutating key styles.

### 4) Apply on theme changes + rotation

- When `KeyboardThemeFactory.observeCurrentTheme` emits, re-apply wallpaper based on the new theme id.
- On orientation changes (portrait/landscape), re-decode/scale if needed.

## Compatibility Notes

- This must **not** break existing APK add-on themes:
  - They already expect `keyboardWallpaper` to work.
  - The per-theme photo override should be additive and optional.
- Keep existing resource keys and `keyboardWallpaper` attr semantics intact.

## Test Plan

- Unit tests:
  - per-theme pref key mapping (theme id → stored values)
  - reset logic clears only the targeted theme’s keys
  - URI validation fallback behavior (missing/invalid URI falls back to theme wallpaper)
- Instrumentation:
  - Pick an image (fake/stub via test content provider if available) → verify background changes.
  - Switch theme → verify per-theme wallpaper switches.
  - Reset customizations → verify wallpaper returns to theme default.

## Open Questions (decide before implementing polish)

- Opacity control:
  - Should “opacity” mean alpha on the image, or a dim overlay on top of it?
- Scaling:
  - Fit vs center-crop; do we expose it or keep a single sane default?
- Export:
  - Do we want “export theme customizations” to include the photo reference (URI) or copy the bitmap into app storage?
