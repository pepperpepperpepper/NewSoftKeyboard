# Theme Customization Redesign — Decisions + Roadmap

This is a decision log + design spec for a **better, more powerful, and less confusing** theme customization system for New Soft Keyboard. It is based on the current behavior described in `docs/theme-customization-current.md`, and **verified against the current Settings implementation in this repo**.

What this document does:

- Audit the current setup (UX + implementation touchpoints).
- Turn the redesign into **explicit decisions** (what we will build, and why).
- Define a practical roadmap that stays **fast** and avoids “expensive wallpaper effects”.

What this document does not do:

- Replace a code review: it only references the implementation at a high level.
- Specify final visuals/pixel-perfect UI.

As-of: 2026-01-12

## Status (this branch)

Implemented items are archived at `docs/archive/suggestions-implemented-2026-01-10.md`.

Priority update (2026-01-12): P3 is complete (per-app presets vs “Change theme colors based on used app” overlay: precedence + UI messaging). P4 is complete (owner shortcut migration + design tokens for typography and shadows).

Still open (optional):

- Optional: richer overlay badges in the preview (beyond the current overlay status line + preview-only simulation modes).
- (Deferred) Extra layer types beyond the built-in set (e.g., subtle paper/noise variants) and richer per-layer UIs.
- Long-term evaluation: exporting a preset as a full add-on APK/theme package (not planned for v1/v2; zip export remains primary).

---

## 0) Fit Check: Current Settings Tree (Source-of-truth)

This is the reality we must fit into (or intentionally replace). References are to the current codebase:

- **Navigation graph**: `ime/app/src/main/res/navigation/settings_main.xml`
  - Start destination is `SettingsHomeFragment`.
- **Settings Home**: `SettingsHomeFragment` + `ime/app/src/main/res/xml/prefs_settings_home.xml`
  - “Look & feel” is the existing home category entry point for theme/UI appearance.
- **Look & feel screen**: `LookAndFeelSettingsFragment` + `ime/app/src/main/res/xml/prefs_look_and_feel_settings.xml`
  - Theme-related rows live under the “Theme” section:
    - `nav:theme_selector` → `KeyboardThemeSelectorFragment`
    - `nav:keyboard_theme_wallpaper_customization` → `KeyboardThemeCustomizationFragment`
    - `nav:night_mode_settings` → `NightModeSettingsFragment`
    - plus global theme-affecting settings like `settings_key_apply_remote_app_colors` and `settings_key_theme_case_type_override`.
- **Theme selection screen**: `KeyboardThemeSelectorFragment` (an `AbstractAddOnsBrowserFragment`)
  - Has an always-visible **live keyboard preview** (`DemoKeyboardView`).
  - Contains a second copy of **“apply remote app colors”** (checkbox + demo apps).
  - Inserts a **“Customize current theme”** row + has a menu **“Customize”** action, both routing to `KeyboardThemeCustomizationFragment`.
  - Has a deep link entry (`@string/deeplink_url_themes`) in `settings_main.xml`.
- **Theme “Tweaks” screen**: `KeyboardThemeTweaksFragment` + `ime/app/src/main/res/xml/prefs_keyboard_theme_tweaks.xml`
  - Deprecated compatibility surface (shortcuts/deep links only; no editable controls).
- **Customize appearance**: `KeyboardThemeCustomizationFragment`
  - Stores per-preset overrides (wallpaper, colors, typography) scoped by preset id.
  - Preview-first UX: includes a live keyboard preview and in-place controls.
- **Settings search**: `SettingsSearchFragment`
  - Indexes **Preference XML** screens. Custom non-preference UIs won’t show up in search unless we add manual indexing.
- **Legacy settings entry points still exist**
  - `UserInterfaceSettingsFragment` (tile-based) and related legacy UI fragments remain in `settings_main.xml` and can navigate into the same theme/overlay screens.
  - These screens must remain **deep-link only** (no duplicate appearance controls) and need explicit search/indexing work if we ever want them searchable (they are not Preference XML).
- **Performance context** (why some settings screens feel sluggish):
  - `AbstractAddOnsBrowserFragment` renders a `DemoKeyboardView` **inside every grid item** in lists such as “Manage Keyboards”.
  - If wallpaper key overlays (especially match-key-shape) are enabled, these previews can become expensive.

### 0.1 Does the prior “Appearance → Presets” design fit this tree?

Partially:

- The **single-owner rule**, **scope clarity**, and **preview-first editing** fit perfectly and fix real problems we have today.
- A “Preset system” now exists (`KeyboardThemePresetStore` + per-preset override stores), enabling “save as / fork / export” flows.
- The “no nesting” constraint is now satisfied by routing **Theme selector → Customize → Customize appearance editor** directly (no intermediate editor screens).

So: the direction is correct, but we need to make the plan **fit the existing fragments/keys**, and define **decisions for v1** that untangle the UI _before_ adding more power.

---

## 0.2 Decisions (v1: untangle + preview + speed)

These decisions are made to align the redesign with the current Settings tree while meeting the UX constraints (preview-first, non-nested, single-owner).

1. **Single-owner is non-negotiable**
   - Every appearance-related control is edited in exactly one place (**Global**, **This theme/preset**, or **System overlay**).
   - Any secondary location is **status-only + deep link** (no duplicate toggles/sliders).

2. **Delete the “Tweaks scope trap”**
   - `KeyboardThemeTweaksFragment` stops being a dumping ground for global settings.
   - Decision (v1): remove “Tweaks” as a user-facing editor surface:
     - Theme selector menu “Tweaks” and “Customize current theme” must navigate **directly** to the owning editor (`KeyboardThemeCustomizationFragment` in v1; Appearance Studio in v2+).
     - `KeyboardThemeTweaksFragment` may remain temporarily for compatibility, but must contain **no editable controls** (shortcuts/deep links only, if kept at all).

3. **One owner for Background photo**
   - Background photo + its controls are owned by **one screen** (v1 owner: `KeyboardThemeCustomizationFragment`).
   - Entry points may exist in multiple places, but they all deep-link to the same owner.
   - The owner must be **preview-first** (see next decision).

4. **Live preview is required for appearance editing**
   - Any screen that edits theme appearance (starting with Background photo) must show an obvious, immediate keyboard preview (use `DemoKeyboardView` as the baseline).
   - “Try now” remains as an optional validation step, but should not be the primary feedback loop.

5. **Settings lists must use a cheap preview path by default**
   - In list/grid previews (themes list tiles, keyboard layout tiles, etc.), we default to a fast rendering mode:
     - no match-key-shape masking
     - no heavy layer compositing
     - fixed preview resolution buckets + caching
   - Full fidelity is reserved for the _selected_ preview / editor.

6. **Presets are a core requirement (implemented)**
   - Presets are implemented now (`KeyboardThemePresetStore` + per-preset override stores) to enable fork/save/export/import without shipping APKs.
   - Compatibility rule: the “Default” preset id equals the base theme id, so existing per-theme wallpaper data continues to apply.
   - The editor surface stays non-nested: preset selection + editing live in `KeyboardThemeCustomizationFragment`.

---

## 0.3 Owner Table (v1: concrete “single-owner” commitments)

This is the minimum set of “owners” we must enforce immediately to stop re-tangling.

| Feature / setting                                                         | Scope                         | v1 owner UI                                               | Secondary locations                                             |
| ------------------------------------------------------------------------- | ----------------------------- | --------------------------------------------------------- | --------------------------------------------------------------- |
| Theme selection (current theme)                                           | Global                        | `KeyboardThemeSelectorFragment`                           | Setup wizard / legacy entry points may deep-link here           |
| Background photo + transforms + key overlay mode                          | Per-preset (active preset id) | `KeyboardThemeCustomizationFragment`                      | Any other entry point is a deep-link only                       |
| “Adapt theme colors to used app” (`settings_key_apply_remote_app_colors`) | Global overlay                | `KeyboardThemeSelectorFragment` (has preview + demo apps) | Look & feel shows status + deep-link only (keep row for search) |
| Night mode “Darken keyboard theme”                                        | System overlay                | `NightModeSettingsFragment`                               | Look & feel may deep-link only                                  |
| Power saving “Switch to dark, simple theme”                               | System overlay                | `PowerSavingSettingsFragment`                             | Any other location is deep-link only                            |
| Theme case override (`settings_key_theme_case_type_override`)             | Global                        | `LookAndFeelSettingsFragment`                             | Must not appear in theme selector/tweaks                        |

## 0.4 Owner Registry (v1 inventory + where it lives)

The owner table above is the “philosophy”. This section is the **explicit inventory** we can enforce.

### 0.4.1 Where the owner registry should live (code)

We need this to be machine-usable (search, deep links, and duplication checks). The human-readable copy stays here, but the source-of-truth should live in code:

- New: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ui/settings/AppearanceOwnerRegistry.java`
  - A small static list of entries (no new dependencies).
  - Each entry maps a setting/control id to:
    - scope: **Global** / **This preset** / **System overlay**
    - owner destination: `R.id.…`
    - optional scroll target key (Preference key or custom scroll key)
    - allowed secondary surfaces (status-only + deep link)
- Consumers (should migrate to use the registry, instead of hard-coded lists):
  - `SettingsSearchFragment` (manual indexing for non-Preference-XML UIs, and better “Jump to owner” search results).
  - Any “shortcut” preferences in other screens (ex: Look & feel rows that should deep-link to the owner).
  - Debug/test duplication checks (fail fast when a second owner is introduced).

### 0.4.2 Global + system overlays (scope owners)

These settings have historically been duplicated or mis-scoped; the registry must explicitly lock them down.

| Control / feature                  | Canonical id (key)                                                              | Scope          | Owner screen (destination)                                                       | Allowed secondary surfaces (status-only + deep link)                                                                                     | Notes                                                                               |
| ---------------------------------- | ------------------------------------------------------------------------------- | -------------- | -------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| Select theme (base theme add-ons)  | `nav:theme_selector`                                                            | Global         | `KeyboardThemeSelectorFragment` (`R.id.keyboardThemeSelectorFragment`)           | Setup wizard; legacy UI fragments; Look & feel entry                                                                                     | Contains live preview + add-on browser UI.                                          |
| Customize appearance (per-preset)  | `nav:keyboard_theme_wallpaper_customization`                                    | This preset    | `KeyboardThemeCustomizationFragment` (`R.id.keyboardThemeCustomizationFragment`) | Theme selector “Customize” action + row; Tweaks legacy surface                                                                           | Single owner for wallpaper layers, colors, fonts, shadows, presets.                 |
| Adapt theme colors to used app     | `settings_key_apply_remote_app_colors`                                          | Global overlay | `KeyboardThemeSelectorFragment` (`R.id.keyboardThemeSelectorFragment`)           | `LookAndFeelSettingsFragment` (status-only; click navigates); `KeyboardThemeCustomizationOverlaysSection` (status-only; click navigates) | Must not be toggleable in Look & feel; keep there for search/discoverability.       |
| Night mode (darken keyboard theme) | `settings_key_night_mode_theme_control` (+ `settings_key_night_mode`)           | System overlay | `NightModeSettingsFragment` (`R.id.nightModeSettingsFragment`)                   | Look & feel entry; `KeyboardThemeCustomizationOverlaysSection`                                                                           | Editor surfaces must show overlay status to prevent “why did it change?” confusion. |
| Power saving theme overlay         | `settings_key_power_save_mode_theme_control` (+ `settings_key_power_save_mode`) | System overlay | `PowerSavingSettingsFragment` (`R.id.powerSavingSettingsFragment`)               | Troubleshooting/Effects shortcuts; `KeyboardThemeCustomizationOverlaysSection`                                                           | Same overlay-awareness rule as Night mode.                                          |
| Theme case override                | `settings_key_theme_case_type_override`                                         | Global         | `LookAndFeelSettingsFragment` (`R.id.lookAndFeelSettingsFragment`)               | None                                                                                                                                     | Keep out of theme selector/customizer.                                              |

### 0.4.3 Global “look & feel” UI settings (formerly mixed into Tweaks)

These are **global keyboard UI settings**, not per-theme/preset appearance, and must remain owned by Preference-XML screens (searchable and stable).

Owner: `LookAndFeelSettingsFragment` (`R.id.lookAndFeelSettingsFragment`), Preference keys:

- Key preview popup: `settings_key_key_press_shows_preview_popup`
- Key preview position: `settings_key_key_press_preview_popup_position`
- Show hint text: `settings_key_show_hint_text_key`
- Override hint position: `settings_key_use_custom_hint_align_key`
- Custom horizontal hint alignment: `settings_key_custom_hint_align_key`
- Custom vertical hint alignment: `settings_key_custom_hint_valign_key`
- Hint label size: `settings_key_hint_size`
- Show keyboard name: `settings_key_show_keyboard_name_text_key`
- Keys height factor (portrait): `settings_key_zoom_percent_in_portrait`
- Keys height factor (landscape): `settings_key_zoom_percent_in_landscape`

Rule: none of the above may appear as editable controls in theme selection or “Customize appearance” surfaces. If we want shortcuts, they must be deep links to the owner.

### 0.4.4 Per-preset appearance controls (Customize appearance → scroll targets)

Owner: `KeyboardThemeCustomizationFragment` (`R.id.keyboardThemeCustomizationFragment`).

These ids are the stable **scroll targets** used for in-page navigation and Settings search deep-links (see `AppearanceOwnerRegistryThemeCustomizationSearchEntries`).

Sections (top-level anchors):

- Presets: `section:presets`
- Background: `section:background`
- Colors: `section:colors`
- Typography: `section:typography`
- Shadows: `section:shadows`
- Overlays (status-only deep links): `section:overlays`
- Reset: `section:reset`

Per-section controls (stable ids):

- Presets (`section:presets`)
  - Preset selection: `keyboard_theme_preset_selection`
  - Save as: `keyboard_theme_presets_save_as`
  - Export: `keyboard_theme_presets_export`
  - Import: `keyboard_theme_presets_import`
- Background (`section:background`)
  - Pick photo: `keyboard_theme_wallpaper_customization_pick`
  - Import quality: `keyboard_theme_wallpaper_customization_high_quality_import`
  - Rotate: `keyboard_theme_wallpaper_customization_rotate`
  - Scale mode: `keyboard_theme_wallpaper_customization_scale_mode`
  - Anchor: `keyboard_theme_wallpaper_customization_anchor`
  - Saturation: `keyboard_theme_wallpaper_customization_saturation`
  - Contrast: `keyboard_theme_wallpaper_customization_contrast`
  - Background layer stack: `keyboard_theme_wallpaper_customization_background_layer_stack`
  - Apply photo to (mode): `keyboard_theme_wallpaper_customization_mode`
  - Key overlay opacity (all/special/modifier/enter/spacebar):
    - `keyboard_theme_wallpaper_customization_key_opacity`
    - `keyboard_theme_wallpaper_customization_special_key_opacity`
    - `keyboard_theme_wallpaper_customization_modifier_key_opacity`
    - `keyboard_theme_wallpaper_customization_enter_key_opacity`
    - `keyboard_theme_wallpaper_customization_spacebar_opacity`
  - Key blend mode: `keyboard_theme_wallpaper_customization_key_blend_mode`
  - Key layer stack: `keyboard_theme_wallpaper_customization_key_layer_stack`
  - Quality: `keyboard_theme_wallpaper_customization_quality`
  - Match key shape: `keyboard_theme_wallpaper_customization_match_key_shape`
  - Try now: `keyboard_theme_wallpaper_customization_try_now`
  - Apply to all themes: `keyboard_theme_wallpaper_customization_apply_to_all`
  - Reset background photo: `keyboard_theme_wallpaper_customization_reset`
- Colors (`section:colors`)
  - Readability status: `keyboard_theme_appearance_readability_status`
  - Ensure readable text: `keyboard_theme_appearance_ensure_readable_text`
  - Auto-pick readable colors: `keyboard_theme_appearance_auto_readable_colors`
  - Auto-match photo colors: `keyboard_theme_appearance_auto_photo_colors`
  - High contrast (dark): `keyboard_theme_appearance_high_contrast_dark`
  - Manual colors (per key type + background):
    - Key text: `keyboard_theme_override_key_text_color`
    - Special key text: `keyboard_theme_override_special_key_text_color`
    - Modifier key text: `keyboard_theme_override_modifier_key_text_color`
    - Enter key text: `keyboard_theme_override_enter_key_text_color`
    - Spacebar text: `keyboard_theme_override_spacebar_text_color`
    - Hint text: `keyboard_theme_override_hint_text_color`
    - Key background tint: `keyboard_theme_override_key_background_tint`
    - Special key background tint: `keyboard_theme_override_special_key_background_tint`
    - Modifier key background tint: `keyboard_theme_override_modifier_key_background_tint`
    - Enter key background tint: `keyboard_theme_override_enter_key_background_tint`
    - Spacebar background tint: `keyboard_theme_override_spacebar_background_tint`
    - Keyboard background tint: `keyboard_theme_override_keyboard_background_tint`
    - Key background opacity: `keyboard_theme_override_key_background_opacity`
    - Keyboard background opacity: `keyboard_theme_override_keyboard_background_opacity`
- Typography (`section:typography`)
  - Key font family/style:
    - `keyboard_theme_override_key_font_family`
    - `keyboard_theme_override_key_font_style`
  - Hint font family/style:
    - `keyboard_theme_override_hint_font_family`
    - `keyboard_theme_override_hint_font_style`
  - Suggestions font family/style:
    - `keyboard_theme_override_suggestion_font_family`
    - `keyboard_theme_override_suggestion_font_style`
  - Keyboard name font family/style:
    - `keyboard_theme_override_keyboard_name_font_family`
    - `keyboard_theme_override_keyboard_name_font_style`
  - Text size presets:
    - `keyboard_theme_override_key_label_text_size`
    - `keyboard_theme_override_hint_text_size`
    - `keyboard_theme_override_suggestion_text_size`
    - `keyboard_theme_override_keyboard_name_text_size`
  - Label fit:
    - Auto-fit: `keyboard_theme_override_key_label_auto_fit`
    - Auto-fit min size: `keyboard_theme_override_key_label_auto_fit_min_size_percent`
    - Ellipsize: `keyboard_theme_override_key_label_ellipsize`
  - Custom font:
    - Missing warning: `keyboard_theme_override_key_custom_font_missing`
    - Import: `keyboard_theme_override_key_custom_font_import`
    - Remove: `keyboard_theme_override_key_custom_font_remove`
- Shadows (`section:shadows`)
  - Shadow target (all/special/modifier/enter/spacebar): `keyboard_theme_override_shadow_target`
  - Text shadow:
    - Preset: `keyboard_theme_override_key_text_shadow_style`
    - Color: `keyboard_theme_override_key_text_shadow_color`
    - Radius: `keyboard_theme_override_key_text_shadow_radius_dp`
    - Offset X/Y: `keyboard_theme_override_key_text_shadow_offset_x_dp`, `keyboard_theme_override_key_text_shadow_offset_y_dp`
  - Key shadow:
    - Preset: `keyboard_theme_override_key_background_shadow_style`
    - Color: `keyboard_theme_override_key_background_shadow_color`
    - Offset X/Y: `keyboard_theme_override_key_background_shadow_offset_x_dp`, `keyboard_theme_override_key_background_shadow_offset_y_dp`
    - Spread: `keyboard_theme_override_key_background_shadow_spread_dp`
- Reset (`section:reset`)
  - Reset preset: `keyboard_theme_appearance_reset_preset`
  - Reset all appearance overrides: `keyboard_theme_appearance_reset_all`

Registry rule: every editable control must have a stable id (Preference key or scroll target). If a control is unkeyed, it can’t be reliably owned, searched, or deep-linked.

## Step-by-step walkthrough (current → proposed)

### Step A — Today: “theme customization” is actually 3 separate systems

1. **Theme selection (add-ons)**  
   Users pick a packaged theme at:
   - Settings → Look & feel → Theme → Select theme

2. **Theme-affecting overlays (mostly global)**  
   Users enable things like:
   - “Change theme colors based on used app”
   - Night-mode “Darken keyboard theme”
   - Power-saving “Switch to dark, simple theme”
     These are not clearly “part of the theme”, and they live in different parts of Settings.

3. **Background photo (per-theme override)**  
   Users can set a per-theme background photo and a small set of controls (dim/rotate/scale/mode).

Net effect: users experience a single “appearance” system, but we present it as multiple places with mixed scopes.

### Step B — Today: “Tweaks” is a scope trap

The theme selector offers “Customize current theme / Tweaks”, but most items there are **global keyboard UI settings**
(previews, hints, size). Only background photo is **per-theme**. This is the main UX tangle.

### Step C — Today: wallpaper effect is visually strong but feature-limited

What’s good:

- The “continuous wallpaper across keys” look is achieved by anchoring a shader to keyboard bounds (good model).

What’s missing:

- No layer stack (photo + gradient + tint + vignette + noise…).
- No blend modes.
- No per-key-type rules (spacebar vs action keys vs normal).
- No real “build your own theme” flow: no presets, no “save as”, no export/import.

### Step D — Decision (v2+): introduce a first-class “Preset” system

Define 3 concepts explicitly:

1. **Base Theme** (add-on): packaged theme resources.
2. **Preset** (user-created): base theme + overrides (wallpaper layers, colors, effects, fonts).
3. **Global UI settings**: key preview, hints, keyboard size, etc.

Key rule: **users select a preset**, not a raw add-on theme.

This enables:

- “Fork / Save as new theme” (duplicate preset).
- Multiple personalized variants of the same base theme.
- Export/import/sharing that does not require shipping an APK.

### Step E — Decision: reorganize settings to match the mental model (without adding nesting)

Replace the current “Theme selector → Tweaks → (sometimes wallpaper)” scope trap with a flat ownership model:

- Settings → Look & feel → **Select theme**
  - Selection stays here (with live preview).
  - “Customize” routes directly to `KeyboardThemeCustomizationFragment` (the single owner for per-preset appearance editing).
- Settings → Look & feel → **Customize appearance**
  - Routes directly to the same owner (no alternate editors).
- Global appearance settings stay owned by their proper global screens (and are never duplicated inside theme editors).

### Step F — Proposal: expand capabilities while staying fast

Add richer styling features (layers, blend modes, shadows, fonts) but enforce guardrails:

- A per-preset **Quality** level (Low/Balanced/High)
- Make “Advanced (may be slower)” explicit
- Ensure list previews use cheap rendering modes by default

## 1) Baseline (pre-redesign, for context)

This section summarizes the legacy behavior captured in `docs/theme-customization-current.md` (i.e., what existed before the work in this branch).

### 1.1 Legacy “Customization” split across systems

1. **Theme selection (add-ons)**
   - Themes are selected from **Settings → Look & feel → Theme → Select theme**.
   - Themes themselves are packaged as add-ons and mostly define resources in the `AnyKeyboardViewTheme` styleable (`api/src/main/res/values/keyboard_theme_api.xml`).

2. **Theme-affecting overlays (global-ish)**
   - “Change theme colors based on used app” (global preference) applies a color overlay via `ThemeOverlayCombiner` (`ime/overlay/.../ThemeOverlayCombiner.java`).
   - Night-mode “Darken keyboard theme” and power-saving “Switch to dark, simple theme” apply additional overlays elsewhere.

3. **Background photo (legacy per-theme override)**
   - Legacy builds stored the image + settings per theme id (`KeyboardWallpaperOverrideStore`).
   - In this branch, wallpaper + appearance overrides are scoped per preset id (default preset id equals base theme id).
   - Applied as:
     - View background photo (keyboard “wallpaper”), via `KeyboardWallpaperResolver`.
     - Optional key-face overlay (tint/texture), also via `KeyboardWallpaperResolver` + keyboard drawing code.

### 1.2 Legacy UX confusion: “Tweaks” mixed scopes (now resolved)

The **Tweaks** screen is entered from the theme selector but contains mostly **global** settings (key previews, hints, zoom, etc.) plus one per-theme editor (background photo). This creates a misleading mental model: “I’m editing the theme,” but most options are not per-theme.

### 1.3 Legacy background photo options (before layers/presets)

Legacy photo controls:

- Import (global “high quality” toggle)
- Dim, rotate
- Scale: crop/fit/stretch/tile/mirror + anchor (only for crop/fit)
- Wallpaper mode: background only / background+key tint / background+key texture
- Key opacity slider (tint/texture)
- Match key shape (slower) (texture mode only)

Legacy gaps (many are implemented in this branch; see “Status” above):

- No “layer stack” (photo + gradient + tint + texture + vignette + noise, etc.)
- No blend-mode choice (multiply/screen/overlay/soft-light…)
- No blur, saturation, contrast, hue controls
- No per-key-type rules (action keys, spacebar, function keys)
- No per-theme color editing (text/hints/backgrounds)
- No exporting/sharing of a “customized theme”

---

## 2) Implementation Audit (What The Code Actually Does)

This section is about **how the theme pipeline works**, and where it is currently hard to extend cleanly.

### 2.1 Theme resources flow

- `ThemeAttributeLoader` reads theme attributes from add-on resources and loads them into the keyboard view, including key background drawable and keyboard background drawable.
- `ThemeOverlayCombiner` then optionally applies a **global overlay** (currently via `LightingColorFilter`) to key background and keyboard background, and overrides text colors.
- Wallpaper is handled separately (not part of `ThemeOverlayCombiner`):
  - View background photo override is resolved/applied by `KeyboardWallpaperResolver`.
  - Key-face wallpaper overlay is also resolved by `KeyboardWallpaperResolver` and is drawn in `KeyDrawHelper`.

### 2.2 Typography and shadows (legacy limitations)

(These bullets describe how it was before this branch; see “Status” above for the current implementation.)

- **Key font**: themes could select `keyTextStyle` (normal/bold/italic), but there was no **font family**
  selection and no user-facing typography editor.
- **Hints/suggestions fonts**: some surfaces used hard-coded `Typeface.DEFAULT` / `DEFAULT_BOLD`,
  so typography was not consistently themeable across the UI.
- **Text shadows**: the theme API already included shadow parameters (`shadowColor`, `shadowRadius`,
  `shadowOffsetX/Y`) used by key-label rendering, but they were not user-editable.
- **Key shadows**: there was no distinct “key shadow” effect (only whatever the key background drawable
  implied, plus optional text shadow).

### 2.3 Wallpaper rendering strategy (important for performance)

**Good news:** the current key overlay is _not_ “decoding a photo per key”. It uses:

- A **single** `Bitmap` + `BitmapShader`, with a shader matrix anchored to the keyboard view bounds (`KeyboardWallpaperResolver.resolveKeyFaceOverlay` + `KeyboardWallpaperTransform.updateShaderMatrix`).

**Potential performance cost areas (especially in preview/list UIs):**

- `Match key shape` mode uses alpha masks and/or a union mask:
  - `KeyBackgroundAlphaMaskCache` extracts alpha masks from key background drawables (can be expensive on first use for each drawable+size).
  - `KeyDrawHelper.ensureKeyFaceUnionMask` builds a per-keyboard union mask bitmap (ALPHA_8) at keyboard view size.
  - `KeyDrawHelper.drawKeyboardTextureOverlayWithMask` uses `saveLayer(...)` + DST_IN compositing.
- Those costs are mostly amortized by caching, but in settings screens that create multiple previews with different sizes, caches may churn and the “first paint” can feel slow.

### 2.4 State/storage structure (themes vs presets)

- Wallpaper files stored under internal storage `files/wallpapers/<sha>.webp`
- Wallpaper settings stored in shared prefs using keys with an id prefix (base theme id for the Default preset; preset id for user presets)
- “High quality import” is a global pref

This is functional, but it does not scale well to:

- multiple named presets
- sharing/exporting
- a bigger parameter space (multiple layers, effects, colors, per-key-type rules)

---

## 3) What’s Tangled / Incomplete (Pain Points)

### UX / discoverability

- Theme selection and theme customization are split, and “Tweaks” is a misleading label/location.
- Users can’t easily answer: “Is this setting global or per-theme?”
- No consistent “edit current theme → save as preset” flow.
- Photo customization is per-theme, but some controls are duplicated/reachable from multiple paths.

### Feature gaps

- No per-theme color picking for:
  - key text / hint text
  - key background tint
  - keyboard background tint
  - accent colors for special keys
- No layering model: only one image + one overlay mode.
- No export/import or sharing of customizations.
- No “build a theme from wallpaper” workflow (palette extraction + auto-contrast + presets).

### Performance / quality controls

- There’s no user-facing concept of “quality vs performance” besides “High quality (larger file)”.
- Advanced effects (match key shape, blur, shadows) need explicit guardrails and fallbacks.

---

## 4) Proposed New Mental Model

Introduce a clear, explicit split:

1. **Base Theme** (from add-on): “Neon”, “Classic Dark”, etc.
2. **Theme Preset** (user-created): “My Neon (Photo + Tweaks)”, “Work High Contrast”, etc.
3. **Global Keyboard UI Settings**: key previews, hints location, keyboard size, etc.

Key rule: **A preset is what users select**, not the raw add-on theme.

Internally: a preset references a base theme + a set of overrides (colors, wallpaper layers, effects).

---

## 5) Proposed Settings IA (Where Features Live)

### 5.1 v1 IA (fits existing Settings tree)

We keep the existing Settings category structure and remove nesting/duplication:

Settings

- **Look & feel**
  - **Select theme** (`KeyboardThemeSelectorFragment`)
    - “Customize” deep-links to the owner editor (`KeyboardThemeCustomizationFragment`)
  - **Customize appearance** (`KeyboardThemeCustomizationFragment`) ← _single owner_
  - **Night mode** (`NightModeSettingsFragment`) ← system overlay owner
  - Other global UI settings remain in their existing owner screens (mostly under Look & feel)
- **Troubleshooting → Power saving** (`PowerSavingSettingsFragment`) ← system overlay owner

In v1 we do **not** introduce a new “Appearance” top-level category or add extra navigation depth. We fix scope + ownership first.

### 5.2 v2+ IA (implemented here)

Implemented; details archived at `docs/archive/suggestions-implemented-2026-01-10.md`.

### 5.3 v2+ Appearance Studio (presets + editor)

This screen is the long-term home for “appearance” and replaces nested flows.

At the top:

- Current preset card: name + live preview
- Actions: **Duplicate**, **Export**, **Reset** (no “Customize” button needed if editing is in-place)

Core layout:

- Sticky live preview
- A horizontal section switcher (non-nested): **Presets** | **Background** | **Keys** | **Text** | **Effects** | **Global**

Presets section:

- Preset list (user presets + built-in templates)
- “Get more base themes” (add-on store search)
- Rename / duplicate / delete

### 5.4 v2+ Appearance Studio sections (organize by what users see)

Sections (proposed order):

1. **Preview** (sticky or top)
2. **Background**
3. **Keys**
4. **Text & icons**
5. **Special keys** (optional advanced: action keys, spacebar, modifiers)
6. **Effects & performance**
7. **Reset / restore defaults**

Proposed contents (high level):

- **Background**: wallpaper layers (photo/gradient/tint/vignette/noise), transforms, dim/blur, quality.
- **Keys**: key background tint/opacity, key-face overlay layers, key corner/shape options (if supported),
  **key shadow** (optional).
- **Text & icons**: key text/hint text colors, **typography (fonts + styles)**, icon tinting.
- **Effects & performance**: text shadow overrides, blend modes, masking (“match key shape”), quality,
  “disable expensive effects on battery saver”.
- **Reset**: reset section / reset preset / reset to base theme.

### 5.5 Mapping: current screens/settings → decisions

This is a concrete “untangling map” so the redesign has a clear target.

- **Settings → Look & feel → Select theme**
  - Decision: stays the theme selection UI (`KeyboardThemeSelectorFragment`), because it already provides a live preview.
  - Decision: “Customize” must no longer route to a mixed-scope screen.
- **Theme selector → “Tweaks/Customize”**
  - Decision: routes directly to **Customize appearance** (`KeyboardThemeCustomizationFragment`, single non-nested editor surface).
- **Settings → Look & feel → Customize appearance**
  - Decision: same owner as above (no alternate editor).
- **Settings → Look & feel → “Change theme colors based on used app”**
  - Decision (v1): keep the setting global, but make **`KeyboardThemeSelectorFragment` the only editor** (it already has the live preview + demo UI).
  - Look & feel should become **status-only + deep link** to the owner (to avoid duplicated controls).
- **Global overlays**
  - Night mode stays owned by `NightModeSettingsFragment` (linked from Look & feel).
  - Power saving theme overlay stays owned by `PowerSavingSettingsFragment` (under Troubleshooting), but appearance editors must surface an “overlay active” badge to prevent confusion.

---

## 6) Feature Set (Roadmap)

This is organized by priority. “P0” is the minimum viable redesign that already unlocks big improvements.

Completed roadmap sections (P0/P0.1/P1) are archived at `docs/archive/suggestions-implemented-2026-01-10.md`.

Execution order update (2026-01-12): P3 is complete (precedence + UI messaging). Next focus can return to optional **P2** expansion.

### P2 — Strongly desired (big visual flexibility)

Status: mostly implemented (this branch). Implemented details are tracked in `docs/archive/suggestions-implemented-2026-01-10.md`.

Remaining (P2):

- More built-in layer types beyond the current set, focusing on high-value + low-jank:
  - Additional patterns (subtle paper/noise variants).
  - Photo adjustments (brightness/contrast/saturation/temperature) with strict quality guardrails (implemented: Saturation/Contrast/Brightness/Temperature sliders; future: consider converting to per-layer adjustments if needed).
- Richer per-layer UIs for complex layers without adding nested screens:
  - Implemented: in-row mini-previews (swatches/gradient thumbnail) + inline “basic vs advanced” controls per layer.
  - Better affordances for blend mode + opacity (avoid burying the most important knobs).

Deferred (not current focus):

- Mask/shape layers (rounded-rect, key-gap “gutter” mask, safe-area mask for labels) that can replace one-off “match key shape” style toggles.

- **Layer stack model**
  - Background layers: [photo] + [color overlay] + [gradient] + [vignette] + [noise/texture]
  - Key-face layers: [photo texture] + [tint] + [highlight] + [noise]
  - Each layer has: enabled, opacity, blend mode, and (if image) transform.

- **Blend modes**
  - At minimum: Normal, Multiply, Screen, Overlay, Soft Light.
  - (Implementation detail: choose blend modes we can support safely across Android versions and GPU drivers.)

- **Better per-key-type styling** (implemented)
  - Separate rules for: normal keys, special keys, modifier keys, Enter key, and spacebar.
  - Controls: background tint, text color, wallpaper-overlay opacity.
  - Fallback decision: if modifier/Enter overrides are unset, inherit the “special keys” values (preserves legacy behavior where these keys were treated as “special”).
  - Implementation note: “Enter key” currently means `KeyCodes.ENTER`; “modifier keys” are `KeyboardKey.modifier == true` or one of `KeyCodes.{SHIFT,SHIFT_LOCK,ALT,ALT_MODIFIER,CTRL,CTRL_LOCK,FUNCTION}`.

- **Drop shadow options (keys + text)**
  - Implemented: key-label text shadow overrides + key background drop shadow.
  - Implemented: per-key-type shadow rules (special/spacebar/modifier/enter) with “inherit from special keys” behavior for Enter/Modifier when unset.
  - Implemented: key background shadow spread control (All keys target).

- **Fonts / typography (user-editable)**
  - Implemented: per-preset font family choice (system families + custom font file) and per-surface typography (keys, hints, suggestions strip, keyboard name).
  - Implemented: per-surface style override (normal/bold/italic).
  - Implemented: “Auto-fit key labels” toggle (on by default) + improved label fit algorithm (binary search; padding-aware) to reduce clipping/overlap with custom fonts.
  - Implemented: min auto-fit size control (30/40/50/60%) and ellipsize option to prevent long labels from overlapping.
  - Implemented: per-surface text size presets with more steps (Smaller/Small/Large/Larger) for key labels, hints, suggestions strip, and keyboard name.
  - Implemented: clearer “Theme default (100%)” mental model for non-key surfaces (in addition to “Same as key”).

- **Auto-contrast + accessibility helpers**
  - Implemented: actions (“Auto-pick readable text colors”, “Apply high contrast (dark)”) plus an “Ensure readable text” toggle and a live “Readability” status/warnings panel.
  - Implemented: a simple contrast meter (approx ratios vs photo average) with per-key-type readouts when overrides differ.

### P3 — Nice-to-have / advanced

Status: mostly implemented (this branch). Implemented details are tracked in `docs/archive/suggestions-implemented-2026-01-10.md`.

Remaining (P3): (none)

- **Export / import presets**
  - Export a preset as a single file (e.g., zip with JSON + images) so users can share “themes”.
  - Implemented: export can exclude the background photo + wallpaper settings (privacy).
  - Implemented: import shows a preview + metadata, allows renaming, and can optionally set the imported preset as active.
  - Import always creates a new preset (never overwriting silently).

- **Custom font import**
  - Implemented: import a `.ttf/.otf` into app-private storage and use it for keys/hints/suggestions/keyboard name.
  - Implemented: validate font signature on import; warn when “Custom” is selected but the font file is missing or unloadable, with a one-tap re-import action.

- **Generate theme from wallpaper**
  - Implemented: “Auto-match photo colors” with an intensity slider, a special-keys intensity slider, and target toggles (keyboard background tint, key background tint, include special keys, apply text colors, apply text shadow).
  - Implemented: palette source choices (Dominant/Vibrant/Muted/Dark/Light variants).

- **Per-app presets**
  - Implemented: bind last used app → current preset (per base theme).
  - Implemented: bind any installed app → current preset (searchable picker, launchable apps).
  - Implemented: manage bindings at scale (search/sort, per-binding actions, bulk clear).
  - Resolved: interaction with the global “adapt to used app colors” overlay (precedence + UI messaging).

### P4 — UX clarity + anti‑tangle (make edits obvious)

Status: implemented (this branch).

Goal: keep the editor powerful without becoming confusing or nested.

- **Owner registry + duplication enforcement**
  - Implemented: `AppearanceOwnerRegistry` + Settings search integration (deep-linking results for registry keys) + uniqueness test + status-only enforcement tests (Preference XML + code-driven status-only surfaces) + migrated appearance shortcuts/deep links to use the registry navigation helper.
- **Preview clarity**
  - Implemented: pressed-state preview + Inspect (tap/long-press) with key highlight + “jump to section”.
  - Implemented: Focus chips (Auto/Background/Keys/Text/Suggestions/Overlays) that highlight preview regions.
  - Implemented: overlay status + preview-only overlay simulation, with explicit “preview-only” dialog copy and Inspect showing the current overlay preview mode.
  - Implemented: Inspect dialog includes a compact “resolved override details” summary (colors/typography/shadows/background).
- **Overrides summary**
  - Implemented: per-section “What changed?” with jump + reset, plus a per-setting diff list with per-setting reset and “jump to owner” actions.
- **Design tokens**
  - Implemented (v1): palette/roles for colors (Primary/Secondary/Accent/Key surface/Background), applied as defaults with per-setting override support.
  - Implemented: “linked/overridden” affordances for color overrides (summaries + dialog linked-source + reset-to-linked).
  - Implemented: secondary token support for typography and shadows (text shadow + key shadow), including import/export.

---

## 7) Proposed Data Model (for Presets)

Keep it simple and versioned. Example conceptual schema (not final):

- `preset_id` (stable)
- `preset_name`
- `base_theme_id` (add-on theme id)
- `overrides`:
  - `colors`: keyText, hintText, keyboardBgTint, keyBgTint, etc.
  - `background_layers`: ordered list (photo/gradient/color/noise…)
  - `key_layers`: ordered list (photo overlay/tint/texture…)
  - `effects`: shadows, blur, etc + quality knobs
  - `typography`: font family + per-surface overrides (optional)

Storage approach:

- Preset JSON stored in app-private storage (or SharedPreferences for small configs).
- Images stored as files under a preset folder, referenced by id (avoid duplicating big blobs in prefs).
- Include a `version` field for migration.

---

## 8) Applying Presets in the Rendering Pipeline (Architecture Suggestion)

Goal: add flexibility **without making draw() expensive**.

### 8.1 Where to apply overrides

Proposed ordering:

1. Load base theme resources (current `ThemeAttributeLoader` behavior)
2. Apply **preset overrides** (colors, drawables, effects) via a “ThemeOverrideApplier”
3. Apply global overlays (night mode / power saving) consistently in one place
4. Apply “adapt to app colors” last (or make it a preset layer with intensity)
5. Apply wallpaper background + key overlays

### 8.2 Keep expensive work off the draw loop

For image/layer effects:

- Do “heavy” transforms (blur, color adjustments, compositing) once:
  - on preset change
  - on wallpaper change
  - on keyboard size/orientation change
- Output a processed bitmap at an appropriate resolution bucket and then:
  - use a single `BitmapShader` anchored to keyboard bounds (current strategy)
  - or draw as a view background drawable (also current strategy)

For match-key-shape overlays:

- Keep the union-mask optimization path.
- Ensure preview/list screens use a cheaper mode by default (see next section).

---

## 9) UX + Performance Guardrails (Make It Feel Fast)

### 9.1 One explicit “Quality” setting per preset

Example:

- **Low**: no blur, no match-key-shape, lower max wallpaper size, simplified blending
- **Balanced**: default, supports key texture overlay with rounded-rect clipping
- **High**: enables match-key-shape, higher resolution, optional advanced effects

### 9.2 Previews should be cheap

Settings screens (theme list, keyboard list, etc.) should:

- Prefer a single cached preview render or simplified overlay mode.
- Avoid match-key-shape in list rows by default.
- Avoid rebuilding union masks for every small preview; reuse a fixed preview size.

### 9.3 Make “advanced effects” self-documenting

Anything that can noticeably impact battery/CPU/GPU should be:

- in an “Advanced” section
- labeled “may be slower”
- optionally disabled when battery saver is active

---

## 10) Migration Strategy (How to Get There Without Breaking Users)

Presets are introduced (this branch) with a compatibility-first approach:

1. The “Default” preset id equals the base theme id (so existing per-theme wallpaper/override keys continue to work).
2. User presets use stable ids (`user_preset::…`) and store their overrides under the preset id.
3. If we later want to deprecate legacy per-theme keys, add an explicit one-time migration that copies base-theme-id overrides into the preset bundle.

---

## 11) Suggested Incremental Refactor Plan (Practical Steps)

This is an implementation-friendly sequence that avoids a “big bang” rewrite.

0. **Inventory appearance settings + owners (to prevent re-tangling)**
   - Create a single “owner registry” mapping every appearance-related setting to exactly one owner:
     - **Global** / **This preset** / **System overlay**
   - Use this registry to drive:
     - Appearance search results
     - “Jump to …” deep links (shortcuts without duplicate controls)
     - A duplication check (no toggles/sliders outside the owner screen)

1. **Ship v1 P0: untangle + preview + speed**
   - Remove or repurpose `KeyboardThemeTweaksFragment` so it no longer duplicates global settings.
   - Route Theme selector “Customize/Tweaks” directly to `KeyboardThemeCustomizationFragment` (Customize appearance editor).
   - Add a live keyboard preview to `KeyboardThemeCustomizationFragment`.
   - Add a cheap preview path for list/grid tiles to fix sluggish settings UIs.

2. **Create a preset concept (data-only, v2 foundation)**
   - Add a minimal preset store: `preset_id`, `preset_name`, `base_theme_id`.
   - Keep base theme selection compatible: selecting a preset selects the underlying base theme.

3. **Build Appearance Studio (preset editor UI, v2)**
   - Start by migrating the existing Background photo options into the Appearance Studio **Background** section.
   - Ensure the editor is the long-term single owner for all per-preset appearance features.

4. **Add basic per-preset overrides (v2)**

- Colors: key text, hint text, keyboard background tint, key background tint.
- Typography: font family (system families first) + key text style override.
- Implement a simple in-app color picker (no new dependencies), with safe defaults + reset.

5. **Add preset management (v2)**

- Duplicate/rename/delete and a clear “Reset to base theme”.
- Establish the expectation that presets are real objects users manage.

6. **Export/import presets**

- JSON + images bundle; import always creates a new preset.
- Only after this step do “share my theme” requests become realistically supportable.

7. **Layer stack + advanced effects**

- Introduce layers one at a time (gradient first, then blend modes, then blur/noise, etc.).
- Add/iterate on the “Quality” guardrail in parallel.

Status note:

- Steps 1–7 are implemented in this branch (see `docs/archive/suggestions-implemented-2026-01-10.md`).
- Step 0 is still the long-term “anti re-tangle” safety net: we should finish it (registry + duplication checks) before adding more appearance features.

Next practical steps (status):

8. **Owner registry + duplication enforcement** (implemented)
   - Add a small, explicit owner registry for appearance controls (Global / This preset / System overlay).
   - Use it to drive settings search + “Jump to…” links and to flag duplicates during development (tests or debug asserts).

9. **Make preview understanding effortless** (implemented)
   - Add overlay badges + preview-only simulation toggles (no new owners).
   - Add Focus/Inspect so users can see what they’re editing and why.

10. **Stabilize the long-term model** (implemented)

- Add Tokens (palette/roles) so colors don’t become duplicated knobs across sections.
- Support apps → preset binding (and clarify interaction with “Change theme colors based on used app”).

11. **Expand layer types + UIs carefully** (deferred)

- Add a small number of high-value new layer types (mask/shape, extra patterns) and improve per-layer editing UIs without adding nested screens.

12. **Long-term: “export as full theme add-on”** (deferred)

- Evaluate whether we actually want APK/theme-pack export (maintenance + security), or if preset zip export is sufficient.

---

## 12) Decisions (Resolved)

- **“Adapt to used app colors” (`settings_key_apply_remote_app_colors`)**
  - Decision (v1): keep it **global** for compatibility, but enforce single-owner UI (no duplicates). Owner UI is `KeyboardThemeSelectorFragment`.
  - Decision (future): consider making it per-preset (or binding apps → presets) later, but keep it global for now.
- **Per-app presets vs “Change theme colors based on used app”**
  - Decision: per-app bindings choose the preset first (per base theme). If enabled, “Change theme colors based on used app” is applied on top and may override some colors; presets still apply to wallpaper, typography, shadows, etc.
- **Deep links**
  - Decision (v1): preserve the existing theme deep link (`deeplink_url_themes`) to keep external entry points stable.
  - Decision (v2+): if/when Appearance Studio replaces theme selection as the primary surface, keep the deep link stable and route it to the best equivalent destination.
- **Non-nested appearance editing**
  - Decision: per-preset appearance editing stays in a single owner surface (today: `KeyboardThemeCustomizationFragment`).
  - Decision: complex editors (color picker, gradient stops, file pickers) may use dialogs/bottom-sheets, but must return to the same section (no deep navigation stacks).
- **Blank base theme**
  - Decision: allow a built-in “Blank” base theme eventually, but treat it as a **template** (not a default) and ship it after presets exist.
- **Export format**
  - Decision: use a single shareable file (zip bundle) with a small JSON manifest + image assets.
- **Export as add-on APK**
  - Decision: not planned for v1/v2. Revisit only if preset zip export proves insufficient for a real workflow.
- **Multiple wallpapers per preset (day/night)**
  - Decision: not in v1/v2 initial preset release. Users can create two presets (Day/Night) and we can add scheduling/switching later.

---

## 13) UI/UX Improvements (Concrete Ideas)

This section is a brainstorm of UI/UX improvements on top of the redesign, plus a few “quick wins” that can improve the current system even before presets ship.

### 13.1 Naming, scope, and microcopy

- Prefer a name that users understand:
  - “Preset” is accurate but may read as technical; alternatives: **“Theme style”**, **“Theme profile”**, **“Theme setup”**.
  - Keep “Base theme” (add-on) as an advanced detail (shown in the editor, but not front-and-center).
- Everywhere we show a setting, label scope consistently:
  - Use a small tag: **Global** vs **This preset**.
  - In lists, include a one-line summary: “Applies to all themes” vs “Only affects this preset”.
- Avoid “Tweaks” entirely in new UI; if it remains during migration, rename to “Appearance shortcuts” and label global/per-theme rows.
- Replace ambiguous copy:
  - “High quality (larger file)” → “Import quality” (and show an estimated size/resolution impact).
  - “Wallpaper mode” → “Apply photo to” (Background / Keys / Both).
  - “Match key shape (slower)” → “Match key shape (uses more battery)” with an info dialog.

### 13.2 Navigation and wayfinding

- Give the appearance system a single home:
  - “Appearance → Themes & presets” should be the only entry point for theme selection/customization.
- Add “Edit current appearance” shortcuts where users already go:
  - Setup wizard “Done & more” should land on the same “Themes & presets” screen.
  - IME Options dialog can include “Edit theme/preset” (optional, but very discoverable).
- Provide a breadcrumb-like header in the preset editor:
  - `{Preset name}` (editable) + “Based on {Base theme}”.
- Add a “What does this do?” help affordance per major section (Background/Keys/Text).

### 13.3 Themes & presets screen (selection + management)

- Make the current selection obvious:
  - Large “Current preset” card with preview + name + last edited time.
  - One-tap actions: **Customize**, **Duplicate**, **Share** (Export), **Reset**.
- Make editing fast without nesting:
  - Add quick “Edit …” shortcuts on the current preset card (Background / Keys / Text / Effects) that deep-link into the owner section inside the preset editor.
- Preset list improvements:
  - Sort: “Most recently used” by default; also offer A–Z.
  - Show small badges: “Uses photo”, “High contrast”, “High quality”.
  - Add search (filter by name).
- Make “Get more themes” clearly separate from presets:
  - “Get more base themes (add-ons)” row at bottom with store/search.

### 13.4 Preset editor (general interaction model)

- Avoid “death by settings”:
  - Provide **Basic** vs **Advanced** toggle per section (Background/Keys/Text).
  - In Basic, show 3–6 core controls; Advanced reveals the rest.
- Provide a “preview-first” editing loop:
  - Sticky preview at top with a “Try typing” mini field (like current “Try now”, but embedded).
  - Optional toggle to show/hide suggestions strip in the preview.
- Make preview obvious and always accessible:
  - Keep the preview visible across the entire editor (not only on the first screen), even when scrolling long lists of controls.
  - Provide quick state toggles next to the preview (Portrait/Landscape, shift/caps, symbols, suggestions strip on/off).
  - Allow expanding the preview to full-screen (and back) without navigating away, so users can validate fine details (shadows, texture, hint size).
- Avoid nested settings pages:
  - Keep the entire preset editor at a single navigation depth (tabs/chips/anchors are OK; pushing new sub-screens for “Advanced” is not).
  - When a “picker” is needed (photo, color), use a modal/bottom-sheet and return the user to the same editor section.
- Avoid “dependency nesting” within controls:
  - Prefer a single control that captures the concept (e.g., “Hint placement” as a single list) instead of checkbox → checkbox → list chains.
  - If a control must depend on another, keep it visible but disabled with a clear explanation (avoid hiding rows that make the feature feel “lost”).
- Make changes safe and reversible:
  - Show “Reset section” and “Reset all” in every section footer.
  - Consider an “Undo last change” (even single-step is helpful).
  - If we keep immediate-save semantics, still provide “Revert changes since opening editor” (session-based).
- Make performance trade-offs visible:
  - Display preset “Quality: Low/Balanced/High” prominently.
  - Inline warnings next to expensive options (blur, match-key-shape, heavy shadows).

### 13.5 Background photo editor (better than today)

- Replace the current “rotate/scale/anchor” mental model with direct manipulation:
  - After picking a photo, open a crop/position screen: pinch to zoom, drag to move, rotate if needed.
  - Keep the existing scale modes (tile/mirror/stretch) as Advanced options.
- Offer two sources:
  - “Choose image” (gallery/files)
  - “Use system wallpaper” (if feasible) with a clear note about privacy/performance.
- Add missing but common controls (all optional/Advanced):
  - Blur (small range; keep it off by default)
  - Saturation/contrast (small range)
  - Color tint overlay (with opacity)
  - Vignette (opacity/size)
  - Noise/grain (subtle, cheap) to reduce banding and improve readability
- Make “Apply photo to” explicit:
  - Background only / Keys only / Background + keys
  - When applying to keys: “Key coverage” (opacity), and “Clip mode” (Rounded-rect default, Match key shape in Advanced).
- Replace “Apply to all themes” with safer targeting:
  - “Copy background settings to…” (multi-select presets) or “Apply to all presets” with a review step.
- Add “Preview on multiple keyboard sizes”:
  - Tiny toggle chips: Portrait / Landscape / One-handed (if supported) so users can catch cropping issues early.

### 13.6 Keys (visual hierarchy and readability)

- Make key readability a first-class outcome:
  - “Ensure readable text” (auto-contrast) should be discoverable from Keys/Text sections.
  - Offer a “High contrast” quick preset action.
- Improve key styling controls (progressive disclosure):
  - Basic: key background opacity, key border on/off, border opacity, corner rounding (if supported).
  - Advanced: per-key-type styling (normal/action/space/modifier) + separate background/text overrides.
- Add “Pressed/active state” preview:
  - Toggle to show pressed key appearance in preview (helps validate contrast).

### 13.7 Text, icons, typography (fonts + layout safety)

- Provide typography controls that are safe by default:
  - Basic: Font family (Default/Sans/Serif/Monospace), weight (Normal/Bold), text size (Small/Default/Large).
  - Advanced: per-surface overrides (key label vs hints vs suggestions strip vs keyboard name).
- Add layout guardrails:
  - Implemented: “Auto-fit key labels” toggle (on by default) to avoid clipping with custom fonts.
  - “Capitals look” preview row (to see `A` vs `a` vs `Ä`/accented glyphs).
- Expose existing text shadow controls with clear intent:
  - “Text outline/shadow” presets: Off / Subtle / Strong (with Advanced sliders for radius/offset).
- Icons:
  - “Icon tint” and “Icon emphasis” (opacity) so icons remain visible on photo backgrounds.

### 13.8 Shadows and depth (drop shadows without chaos)

- Split the concept into two user-facing knobs:
  - “Text shadow” (cheap-ish; already supported by paint shadow)
  - “Key shadow / elevation” (can be expensive; keep off by default and label clearly)
- Provide a “Depth style” picker rather than raw numbers first:
  - Flat / Soft / Elevated (and then Advanced sliders).

### 13.9 Managing, forking, exporting, and “making my own theme”

- Make “forking” the default behavior when customizing:
  - If the current selection is a built-in/default preset, “Customize” can prompt:
    - “Edit current preset” vs “Create a copy first” (default to copy if the preset is shared/default).
- Preset management should support real workflows:
  - Rename, duplicate, delete
  - “Change base theme” while keeping overrides (with preview of what will change)
  - Export/share (Android share sheet) and import
- Add “Theme gallery” concept later (optional):
  - Mark presets as favorites
  - Tagging (Photo / Dark / Light / High contrast)

### 13.10 Performance UX (especially in lists and previews)

- Ensure settings lists never feel sluggish:
  - Use a single fixed-size preview renderer (same bitmap size) for rows.
  - In list previews, default to the cheapest overlay path (no match-key-shape, no blur).
  - Provide a “Reduce preview quality in settings” global toggle if needed.
- Provide clear “why it’s slow” feedback:
  - If enabling a heavy feature, show a one-time dialog: “This may reduce battery life on older devices.”
  - If the app detects repeated slow renders, suggest switching preset quality to Low.

### 13.11 Quick wins within the legacy UI (pre-redesign)

(Status: largely superseded by the implemented “Customize appearance” editor + presets; kept for reference.)

- In the existing “Tweaks” screen, add scope labels in titles:
  - “Key preview popup (Global)”, “Show hint text (Global)”, “Background photo (Current theme)”.
- Reorder “Tweaks” to group by category and separate the single per-theme editor:
  - Background photo first, then a divider “Global settings shortcuts”.
- Consolidate “Customize appearance” to a single entry point; any secondary location should be a deep link only.
- Move “Import quality” under the Background section and make it per-preset/per-photo rather than global.

### 13.12 Guided workflows (make it easy to get a good result)

- Add a “Quick setup” flow in the preset editor:
  - Pick base theme → pick photo (optional) → choose readability level (normal/high contrast) → save.
- Add one-tap “recipes” (apply multiple settings at once):
  - “Make keys more readable”
  - “Make background subtler”
  - “Reduce battery usage”
  - “Increase contrast”
  - “Use app colors lightly” vs “Use app colors strongly”
- Provide “Before/after” comparison:
  - A simple toggle “Show original base theme” in the preview, so users understand what their edits changed.

### 13.13 Empty states, errors, and “what just happened?”

- Better empty states:
  - No presets yet → show “Create your first preset” CTA + 2–3 suggested starting points.
  - No photo set → show “Choose image” + small explanation of how it affects keys/background.
- Make error handling self-explanatory:
  - If photo decode fails or file missing, show a clear reason + “Pick a new image” + “Remove image”.
  - If a font fails to load (future), fall back and show a non-blocking warning.
- For any “global overlay” (night mode / power saving), show a small banner in preview:
  - “Night mode overlay active” with a shortcut to that setting, so users don’t misattribute colors to the preset.

### 13.14 Accessibility and international text considerations (UX)

- Add a “Contrast meter” in the editor (simple pass/warn) for:
  - key label vs key background
  - hint text vs key background
  - suggestion strip text vs background
- Preview with real-world glyphs:
  - Provide a preview row with accented letters, non‑Latin scripts, and emoji so font/contrast issues are visible.
- Make large-text mode obvious:
  - If the user increases key label size or chooses a large font, show a “layout safety” note and suggest Auto‑fit.

### 13.15 Non-nested UI + single-owner settings (hard constraints)

- Adopt a strict “single owner” rule:
  - Every appearance-related feature is owned by exactly one place: **Global**, **This preset**, or **System overlay**.
  - The owning screen contains the control; everywhere else may show only a status/summary + a deep link.
- Remove duplicated controls:
  - Eliminate cases where the same setting is adjustable from multiple screens (“Tweaks” vs Look & feel vs elsewhere).
  - If we keep quick access, use “Jump to …” rows that navigate to the owner (no inline toggles/sliders outside the owner).
- Keep navigation flat:
  - Target a maximum depth of: Settings → Look & feel → **Appearance Studio** (single screen).
  - Inside Appearance Studio, avoid pushing sub-pages for individual features; use in-page sections/chips and a single consistent layout.
- Replace “nesting” with search + deep links:
  - Add an Appearance search that returns settings with their owner (Global/This preset/System overlay) and navigates directly to the owning control.
- Make scope impossible to miss:
  - Show a small tag on every control: **Global** / **This preset** / **System overlay**.
  - In the preview header, show active overlays as badges (Night mode / Power saving / “Adaptive app colors” if global).
- Make “live preview” universal:
  - Every Appearance page (including global ones like Key previews) should include the same immediate keyboard preview so changes are visible without leaving the page.

### 13.16 Appearance Studio (chosen IA)

Decision (v2+): use a single screen with lateral navigation (see section 5.3):

- One **Appearance Studio** screen with a sticky preview and a horizontal section switcher:
  - **Presets** | **Background** | **Keys** | **Text** | **Effects** | **Global**
- Everything stays at one depth; switching sections does not push a new screen.
- Each section is still single-owner:
  - “Background” controls appear only in Background, “Text” only in Text, etc.
  - The “Presets” section manages select/rename/duplicate/export/import (no editing controls there except “Jump to …” and Reset).
- The preview never disappears (collapse/expand, but always reachable).
- A search icon in the header navigates to the owner section (search replaces nesting).

### 13.17 “What changed?” (preset diff + override management)

Users need confidence about what they’ve modified, and developers need a way to prevent duplicated ownership.

- Add an **Overrides** summary panel (always visible near the top of the editor):
  - Shows which areas have overrides: Background / Keys / Text / Effects
  - Shows a count: “12 overrides”
- Provide an **Overrides list** view (still within the editor, not a new screen):
  - Each row shows: setting name, current value, and **Reset** action (reset is allowed even outside the owner, because it removes customization rather than adding a second edit path).
  - Rows deep-link to the owning control for editing.
- Add “Reset all overrides in this section” and “Reset all overrides” consistently.

### 13.18 Owner-first deep links (shortcuts without duplicated controls)

Shortcuts are critical for speed, but they must not create second owners.

- Allow “status-only” rows outside the owner:
  - Example: on Themes & presets, show “Background: Photo (Dim 30%, Keys: Tint 20%)” + **Edit** button.
  - No toggles or sliders in the summary row.
- Standardize the shortcut UI:
  - “Jump to Background → Photo”
  - “Jump to Text → Font”
- Make deep links stable across refactors by routing through the owner registry (not hard-coded fragment class names).

### 13.19 Make previews feel immediate (interaction details)

Even with the right IA, the preview needs to “pull” the user into the editing loop.

- Label the preview clearly as live: “Preview (live)”.
- Make the preview itself interactive:
  - Tap any key to show its pressed state.
  - Long-press a key in preview to reveal a tiny “Inspect” popover (key type: normal/action/space) so per-key-type rules are discoverable.
- Always provide an embedded “Try typing” field with a clear caret so users know it’s interactive.
- Implemented: a one-tap “Open keyboard” action for full IME testing (kept secondary; preview-first).
- If something is taking time (first-time mask build, wallpaper reprocess), show a small non-blocking “Updating preview…” indicator instead of freezing UI.

### 13.20 Control design rules (avoid dependency nesting)

Make complex features usable without multi-level dependency chains.

- Prefer a single “mode” control over multiple toggles:
  - Example: “Hints” mode: Off / On / On + custom placement (instead of checkbox → checkbox → lists).
- Prefer presets first, sliders second:
  - Example: Text outline: Off / Subtle / Strong (preset) + Advanced sliders for radius/offset.
- Keep dependent controls visible but disabled with a one-line explanation.
- Use consistent footer patterns:
  - “Reset section” / “Reset all” / “Learn more” (help)

### 13.21 Manage keyboards / selection lists (UX to prevent sluggishness)

List screens are where “cool effects” most often backfire.

- In any list/grid (themes, presets, languages/keyboards), default to a cheap preview:
  - Lower resolution, no match-key-shape, no blur, no per-key compositing layers.
- Use progressive rendering:
  - Show a placeholder preview immediately, then upgrade quality when idle.
- Provide a global toggle:
  - “Fast previews in settings” (on by default on older devices, or auto-enabled after slow render detection).

### 13.22 Design tokens (single-owner for colors, typography, effects)

As features grow, the only way to stay “non-nested” and “single-owner” is to avoid repeating the same knobs in multiple places.

- Add a **Tokens** (or “Palette & roles”) section in the preset editor:
  - Define a small set of semantic roles: Background, Key surface, Key border, Primary text, Secondary text, Accent, Error/critical.
  - Surfaces (keys/text/suggestions) reference roles rather than owning their own unrelated colors by default.
- Make overrides explicit without creating second owners:
  - Next to any value that can be overridden, show: “Linked to token” / “Overridden”.
  - Provide one action: “Reset to token”.
- Keep tokens editable in one place only:
  - Colors edited only in Tokens.
  - Typography edited only in Typography.
  - Shadows/effects edited only in Effects.

### 13.23 “Focus” and “Inspect” for preview clarity (make edits obvious)

- Add a **Focus** control near the preview (chips):
  - Background | Keys | Text | Suggestions | System overlays
- When the user edits a setting, automatically switch focus and highlight the affected region:
  - Editing key border → outline keys in the preview.
  - Editing suggestion colors → show suggestion strip and highlight it.
- Add an **Inspect** mode:
  - Tap a key in the preview to show its “type” (normal/action/space/modifier) and the resolved style (text color, background tint, overlay mode).
  - The inspect popover includes “Edit owner…” deep links (no inline edits).

### 13.24 Overlay awareness + preview simulation (avoid “why did it change?”)

Global overlays (night mode, battery saver, app-color overlay) cause huge confusion during appearance editing.

- In the preview header, always show active overlay badges:
  - “Night overlay”, “Battery saver overlay”, “App-color overlay”.
- Allow _preview simulation_ toggles (preview-only, not owners):
  - “Simulate night overlay”
  - “Simulate battery saver overlay”
  - “Simulate app-color overlay”
- Editing the real setting always navigates to the owner screen.

### 13.25 “Live” edits without jank (interaction design rules)

To keep previews immediate, some controls need interaction guardrails.

- For expensive operations (blur, match-key-shape, complex blending):
  - While dragging a slider, show a cheap approximation.
  - Apply the final high-quality result on “release” (end of drag) with a small “Updating preview…” indicator.
- Debounce expensive recomputes:
  - Apply in ~150–300ms batches rather than per-pixel slider movement.
- Provide an Advanced toggle:
  - “Update while dragging (may be slower)”.

### 13.26 Preset templates (reduce cognitive load without nesting)

- Add built-in “starter presets” (templates) that users can duplicate:
  - Photo wallpaper (readable)
  - High contrast
  - Minimal/flat
  - “Material-like” (subtle depth + accent)
- If we add a “Blank base theme”, position it as a template:
  - “Start from scratch (Blank)” as a first-class template, not an “advanced hidden” option.

### 13.27 Bulk actions done safely (replace “Apply to all themes”)

We should keep the power of “Apply to all themes” without the risk.

- Replace with “Copy/Paste” and explicit targeting:
  - “Copy Background”
  - “Paste to…” (multi-select presets)
  - “Copy Text”
  - “Copy Effects”
- Always show a review step:
  - “You are applying Background changes to 5 presets” with a list of names.

### 13.28 Export/import UX (make sharing feel real)

- Make export a first-class action from:
  - Current preset card
  - Preset editor header
- Export package UX:
  - Include a preview image, preset name, base theme id/name, and a version marker.
  - Add an “Export without photo” option for privacy.
- Import UX:
  - Show a preview + a summary of what will be imported (Background/Keys/Text/Effects).
  - Always import as a new preset; optionally offer “Replace existing preset…” as an advanced action.

### 13.29 One-place editing across “global” pages (prevent drift)

Even with the new IA, global pages (Key previews/Text/Size) can still feel disconnected.

- Every Appearance sub-page should include:
  - The same live preview header
  - A compact “Current preset” summary row with a deep-link to “Edit preset…”
- Any “shortcut” shown on global pages must be status-only + deep-link (no duplicate toggles/sliders).

### 13.30 Keyboard-side entry points (reduce navigation friction)

- In IME Options, add a single entry:
  - “Edit appearance” → opens Appearance Studio or the preset editor (depending on final IA).
- Add a quick switcher for presets (optional):
  - Long-press “Edit appearance” shows a small list to switch presets without leaving the keyboard.

### 13.31 Preview state controls (realistic validation without nesting)

- Add preview toggles near the preview header:
  - Layout: Letters / Symbols
  - Case: lower/shift/caps
  - Orientation: portrait/landscape
  - Suggestions strip: on/off
  - “Show key popups” (preview-only)
- If per-language issues matter (fonts/glyphs), allow switching the preview layout to 2–3 sample locales (optional).
