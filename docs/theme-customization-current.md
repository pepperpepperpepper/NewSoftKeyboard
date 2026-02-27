# Current Theme Customization (New Soft Keyboard)

This document describes **how theme customization works today** (current UX + settings locations), including **custom background photos**. It’s intended as a baseline for a future overhaul.

As of: 2026-01-04 (repo state around app version `13.8.63`).

## Terminology

- **Theme**: a _keyboard theme add-on_ (selected in “Select theme”).
- **Current theme**: the currently enabled theme.
- **Background photo**: a user-picked image stored per-theme and applied as a keyboard “wallpaper” and/or key overlay.

## Entry points (how to reach theme settings)

### From the launcher app icon

- Open the **“New Soft Keyboard”** app icon.
  - If the keyboard is already enabled, this opens **Settings** (the in-app settings home).
  - If not enabled, this opens the **Setup wizard** first.

### From the keyboard (while typing)

- Long-press the **Enter** key (many layouts bind long-press Enter to the **Settings** key-code), which opens the IME **Options** dialog.
- In the IME **Options** dialog, tap:
  - **“Keyboard settings”** → opens in-app **Settings**.

IME Options dialog items (current order):

- “Keyboard settings”
- “Override default dictionary”
- “Change input method”
- “Incognito Mode …”

### From the setup wizard

- Setup wizard → “Done & more” page → **Theme** → opens **Select theme**.

### From Android system settings (device UI varies)

- Android Settings → Keyboard / On-screen keyboard → **New Soft Keyboard** → (settings entry / gear icon)

## Theme-related settings paths (in-app)

The main theme-related area is:

- **Settings → Look & feel → Theme**

That “Theme” section contains:

- **Select theme**
- **Background photo (current theme)**
- **Night-Mode settings**
- **Change theme colors based on used app** (checkbox)
- **Keyboard letter-case override** (list)

There are also theme-adjacent options _outside_ that section (documented below).

## Screen: “Select theme”

Primary paths:

- Settings → Look & feel → Theme → **Select theme**
- Setup wizard → Done & more → **Theme**

What you see / can do:

### Top action bar menu

- **Search for add-ons** (opens store search for add-ons using keyword “theme”)
- **Tweaks** (opens the “Tweaks” screen; see next section)

### Theme preview area

- A large **demo keyboard preview** updates when you select a theme.
- A checkbox: **“Change theme colors based on used app”**
  - When enabled, a row of demo “apps” appears (WhatsApp/Gmail/Phone/Twitter) to preview different overlay colors on the demo keyboard.

### “Customize current theme” row

- A dedicated row labeled:
  - **Customize current theme**
  - Summary: “Wallpaper, key previews, and size.”
- Tapping it opens the **Tweaks** screen.

### Theme list + store search row

- A grid/list of available themes; tapping one **enables** it.
- At the end of the list there’s an embedded **store search row** for installing additional themes/add-ons.

## Screen: “Tweaks”

Path:

- Settings → Look & feel → Theme → Select theme → **Customize current theme** (row)  
  or
- Settings → Look & feel → Theme → Select theme → **Tweaks** (top menu)

Important scope note:

- Despite living under the theme selector, **most settings here are global UI settings** (not per-theme).
- The **Background photo** entry is **per-theme** (it edits the current theme’s stored photo + per-theme photo settings).

Settings in this screen (current order):

1. **Background photo (current theme)**
   - Opens the background photo editor (next section).

2. **Key preview popup** (checkbox)

3. **Key preview position** (list)
   - Enabled only when “Key preview popup” is enabled.

4. **Show hint text** (checkbox)

5. **Override hint position** (checkbox)
   - Depends on “Show hint text”.

6. **Custom horizontal hint alignment** (list)
   - Depends on “Override hint position”.

7. **Custom vertical hint alignment** (list)
   - Depends on “Override hint position”.

8. **Keyboard letter-case override** (list)

9. **Show keyboard name** (checkbox)

10. **Hint label size** (list)

11. **Keys height factor in portrait mode** (slider)

12. **Keys height factor in landscape mode** (slider)

Duplication note:

- Many of the same preferences also appear in **Settings → Look & feel** under other categories (Key text & hints, Layout & size, Key feedback & previews).

## Screen: “Background photo (current theme)”

Paths:

- Settings → Look & feel → Theme → **Background photo (current theme)**  
  or
- Settings → Look & feel → Theme → Select theme → Tweaks → **Background photo (current theme)**

Scope:

- This screen edits the **currently selected theme** (the enabled theme at the time you use each control).

### Options on this screen

Always visible:

- **Pick background photo**
  - Opens a system picker (documents/gallery) for `image/*`.
  - Shows a small thumbnail preview icon once a photo is set.
  - Summary changes depending on state (not set / set / invalid / saving).
- **High quality (larger file)** (checkbox)
  - Global toggle; applied the next time you pick a photo.
- **Dim background** (slider 0–100)
  - Disabled unless a valid photo exists.
- **Try now**
  - Opens a preview/test input field to try the keyboard.
- **Reset background photo**
  - Removes the photo for the current theme and clears photo-related settings.
- **Apply to all themes**
  - Copies the current theme’s photo + per-photo settings to every theme (confirmation dialog).

Visible only when a valid photo exists:

- **Rotate photo**
  - Rotates the selected photo **90° clockwise** (repeatable).
- **Photo scale** (list)
  - Crop (fill)
  - Fit (contain)
  - Stretch
  - Tile
  - Mirror
- **Photo anchor** (list)
  - Top-left / Top / Top-right / Left / Center / Right / Bottom-left / Bottom / Bottom-right
  - Only shown for **Crop** and **Fit** scale modes.
- **Wallpaper mode** (list)
  - Background only
  - Background + key tint
  - Background + key texture
- **Key opacity** (slider 0–100)
  - Only shown when wallpaper mode is **Background + key tint** or **Background + key texture**.
- **Match key shape (slower)** (checkbox)
  - Only shown when wallpaper mode is **Background + key texture**.

### What “Wallpaper mode” means visually

- **Background only**
  - Photo is only behind the keyboard background.
- **Background + key tint**
  - Photo is drawn over each key as a rectangular overlay (continuous across keys because it’s anchored to the whole keyboard bounds), controlled by “Key opacity”.
- **Background + key texture**
  - Photo is drawn over keys as a texture overlay, clipped to a rounded-rect by default.
  - If “Match key shape” is enabled, the overlay is clipped to the theme’s actual key background shape (may be slower on some devices).

### Storage / persistence (implementation behavior)

- Photos are stored **per theme** as an app-private processed copy (currently `.webp`) under the app’s internal files directory (`files/wallpapers/…`).
- Per-photo settings (dim, mode, key opacity, rotation, scale mode, anchor, match-key-shape) are stored per theme in shared preferences.
- If a stored photo becomes unreadable/corrupt, it is marked invalid and the UI prompts you to pick a new one.

## Other theme-affecting settings outside the theme flow

### Night mode can darken the keyboard theme

Path:

- Settings → Look & feel → Theme → **Night-Mode settings**

Relevant option:

- **Darken keyboard theme** (checkbox)

Behavior:

- When Night-Mode is active and this toggle is enabled, the IME applies a dark overlay to the theme colors (this is not a theme selection change).

### Power-saving can apply a dark overlay (and is not located under “Theme”)

Path:

- Settings → Troubleshooting & backup → Performance & battery → **Power-Saving settings**

Relevant option:

- **Switch to dark, simple theme** (checkbox)

Behavior:

- When Power-Saving is active and this toggle is enabled, the IME applies a dark overlay to the theme colors (this is not the same as selecting a different theme).

## Why this feels “tangled” (observations)

- **Theme selection** (pick a theme) and **theme-affecting overlays** (remote app colors, night mode, power saving) are configured in multiple places.
- The **“Tweaks”** screen is entered from the theme picker but contains mostly **global** UI settings, which makes it easy to assume the settings are “per theme” when they are not.
- **Background photo** is per-theme and lives both as a direct entry in Look & feel and also nested under Tweaks.
