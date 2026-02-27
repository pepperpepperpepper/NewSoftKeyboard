# UI Settings User Stories (WIP)

This document captures user stories for **every user-facing setting** in the NewSoftKeyboard UI, with an emphasis on **privacy/security impact** and **clear ownership**.

## Goals

- Provide a single place to understand what each setting does, why a user would enable it, and what risk/tradeoffs it carries.
- Make it easier to review changes: adding/removing a setting should come with updating this doc.

## Non-goals

- Pixel-perfect UI specs.
- Product/roadmap prioritization (tracked in `plan.md`).

## User story template

- **Story:** As a `<persona>`, I want `<capability>` so that `<benefit>`.
- **Setting:** `<Screen path → Section → Toggle/List>`
- **Pref key / storage:** `<SharedPreferences key, DB table, file, etc.>`
- **Default:** `<value>`
- **Behavior:** `<what changes in the keyboard>`
- **Dependencies:** `<Android version, permissions, feature flags, other settings>`
- **Privacy / security notes:**
  - Data processed: `<typed text, clipboard, network, local files, etc.>`
  - Data stored: `<where, retention>`
  - Data shared: `<when/where>`
  - Risk: `<low/med/high> + why`
  - Mitigations: `<opt-in gates, confirmations, scoping, redaction, logging rules>`
- **Acceptance criteria:**
  - `<bulleted checks a reviewer can verify>`

## Inventory

Full inventory lives in `docs/user-stories-settings-inventory.md`.
Tap-path click map lives in `docs/settings-clickpaths.md`.

Notes:

- Keys starting with `nav:` are **actions/navigation** (not persistent settings).
- Other keys are typically stored in `SharedPreferences` (unless explicitly called out in a story).

## Stories by settings area

These stories focus on the _user intent_, _behavior_, and _privacy/security posture_ for each area.

For the exhaustive key-by-key list, see `docs/user-stories-settings-inventory.md`.

### Setup & enablement

#### Guided setup (first run)

- **Story:** As a new user, I want a guided setup so that I can enable the keyboard and set it as default without guessing where Android hides the controls.
- **Setting:** `Settings → Setup & permissions → Run setup wizard`
- **Pref key / storage:** `nav:setup_wizard` (action row; not stored)
- **Default:** N/A
- **Behavior:** Launches the in-app setup flow, which drives the user into the system IME enablement/default-picker flows.
- **Dependencies:** Android system settings UI; device OEM behavior.
- **Privacy / security notes:** Risk: **low** (no typed data; navigation only).
- **Acceptance criteria:**
  - Tapping the row opens the setup wizard.
  - The wizard can open Android IME settings and the input-method picker.

#### Permission fix-ups (mic + notifications)

- **Story:** As a user who enabled dictation or crash notifications, I want the settings home screen to tell me what permissions are missing so that I can fix it quickly.
- **Setting:** `Settings → Setup & permissions → Microphone permission needed` and `Settings → Setup & permissions → Notification permission needed`
- **Pref key / storage:** `nav:fix_microphone_permission`, `nav:fix_notification_permission` (actions)
- **Default:** N/A
- **Behavior:** Conditionally shows the relevant row and triggers the permission request / system notification settings.
- **Dependencies:** Microphone permission; notification permission (Android 13+) or notification settings state.
- **Privacy / security notes:** Risk: **medium** for mic prompting (enables voice features); mitigation is explicit user action + Android runtime permission dialogs.
- **Acceptance criteria:**
  - Rows only appear when the feature is enabled and permission is missing.
  - Tapping requests permission or opens the correct system screen without crashing.

### Languages & layouts

#### Manage keyboards and language packs

- **Story:** As a multilingual user, I want to install language packs and enable multiple keyboard layouts so that I can type comfortably in different languages.
- **Setting:** `Keyboards & language packs → Language packs → Install language packs` and `Keyboards & language packs → Keyboard add-ons → Manage keyboards`
- **Pref key / storage:** `nav:language_packs_manager`, `nav:keyboards_manager` (actions)
- **Default:** N/A
- **Behavior:** Opens add-on manager flows to download/install language packs and enable/disable layouts.
- **Dependencies:** Installed add-ons; any store/search integration the managers use.
- **Privacy / security notes:** Risk: **low** (management UI only). If add-ons are sourced externally, ensure they do not gain access to typed text via the IME process.
- **Acceptance criteria:**
  - Managers open from settings and show currently installed/enabled items.
  - Enabling/disabling a keyboard affects what the IME can switch to.

#### Custom keyboard designer

- **Story:** As a user, I want to create and edit custom keyboard layouts so that I can tailor keys and layers for my workflow.
- **Setting:** `Keyboards & language packs → Keyboard add-ons → Keyboard Designer`
- **Pref key / storage:** `nav:custom_keyboards` (action) plus designer/editor storage (custom keyboard pack files)
- **Default:** N/A
- **Behavior:** Opens the custom keyboard editor UI and persists the resulting layouts as local files.
- **Dependencies:** Local filesystem storage; keyboard reload behavior.
- **Privacy / security notes:** Risk: **low/medium** (local files). Mitigation: local-only, no implicit sharing, validate layout files before loading.
- **Acceptance criteria:**
  - New layouts can be created and selected without crashing the IME.
  - Invalid layouts are rejected with a clear error instead of breaking typing.

#### Per-app layout memory (context awareness)

- **Story:** As a power user, I want the keyboard to remember my preferred alphabet layout per app so that each app “feels right” without manual switching.
- **Setting:** `Keyboards & language packs → Switching & language key → Remember alphabet layout per App`
- **Pref key / storage:** `SharedPreferences: settings_key_persistent_layout_per_package_id`
- **Default:** `true`
- **Behavior:** Remembers the last-used alphabet layout per package and restores it on focus changes.
- **Dependencies:** App package ID; layout switching logic.
- **Privacy / security notes:** Risk: **low/medium** (stores app package IDs + layout ID locally). Mitigation: store locally only; no logging; no network.
- **Acceptance criteria:**
  - Switching apps restores the last alphabet layout used in that app.
  - Disabling the setting stops per-app restoration and uses global behavior.

#### Hardware keyboard behavior

- **Story:** As a user with a physical keyboard, I want predictable language switching and key repeat behavior so that my workflow is consistent.
- **Setting:** `Keyboards & language packs → Hardware keyboard → …`
- **Pref key / storage:** `use_keyrepeat`, `settings_key_hide_soft_when_physical`, `settings_key_enable_alt_space_language_shortcut`, `settings_key_enable_shift_space_language_shortcut`
- **Default:** As in `docs/user-stories-settings-inventory.md`
- **Behavior:** Enables key repeat and hardware shortcuts; optionally hides the soft keyboard when physical keyboard is active.
- **Dependencies:** Hardware keyboard present; Android input method behavior.
- **Privacy / security notes:** Risk: **low**.
- **Acceptance criteria:**
  - Hardware-only settings are disabled/hidden when no hardware keyboard is present.
  - Shortcuts and hiding behavior match toggles without crashing.

### Text correction & suggestions

#### Suggestions strip and auto-correction

- **Story:** As a user, I want suggestions and auto-correction so that I type faster with fewer mistakes.
- **Setting:** `Typing → Suggestions & correction → Show suggestions` (+ related toggles)
- **Pref key / storage:** `SharedPreferences: candidates_on`, `auto_caps`, `quick_fix`, `settings_key_auto_dictionary_threshold`, `settings_key_auto_pick_suggestion_aggressiveness`
- **Default:** As in `docs/user-stories-settings-inventory.md`
- **Behavior:** Enables suggestions UI, capitalization, quick-fix corrections/abbreviations, and learning thresholds.
- **Dependencies:** Suggestion engine enabled; dictionaries available.
- **Privacy / security notes:**
  - Data processed: typed text (in-memory for suggestion computations).
  - Data stored: learned dictionary/user dictionary entries (local).
  - Risk: **medium** (local retention of learned words); mitigations: local-only storage; never log typed text.
- **Acceptance criteria:**
  - Disabling “Show suggestions” removes the suggestion UI and suppresses dependent options.
  - Learning threshold changes how quickly unknown words are learned.

#### Punctuation and spacing assists

- **Story:** As a user, I want punctuation/spacing conveniences so that common formatting is fast and consistent.
- **Setting:** `Typing → Punctuation & spacing → …`
- **Pref key / storage:** `double_space_to_period`, `settings_key_bool_should_swap_punctuation_and_space`, `default_domain_text`
- **Default:** As in `docs/user-stories-settings-inventory.md`
- **Behavior:** Double-space inserts period; optionally swaps punctuation/space keys; default domain inserts a common suffix.
- **Dependencies:** Keyboard layouts that support these behaviors.
- **Privacy / security notes:** Risk: **low** (local transformation only).
- **Acceptance criteria:**
  - Double-space inserts a period only when enabled.
  - Default domain is applied only in relevant input contexts.

### Dictionaries (local / user / app-specific)

#### User and abbreviation dictionaries

- **Story:** As a user, I want to add my own words and abbreviations so that the keyboard learns my vocabulary and expansions.
- **Setting:** `Typing → Suggestions & correction → …` + dictionary editor actions
- **Pref key / storage:** `SharedPreferences: settings_key_use_user_dictionary`, plus editor actions `nav:user_dictionary_editor` and `nav:abbreviation_editor`
- **Default:** As in `docs/user-stories-settings-inventory.md`
- **Behavior:** Enables user dictionary usage and opens editors to add/remove entries.
- **Dependencies:** Dictionary storage layer; editor screens.
- **Privacy / security notes:** Risk: **medium** (stores user-entered words locally). Mitigation: local-only; include in backup/restore only when user explicitly triggers it.
- **Acceptance criteria:**
  - When enabled, user dictionary entries contribute to suggestions.
  - Editors allow adding/removing entries and changes take effect.

#### Contacts dictionary (permission-gated)

- **Story:** As a user, I want contact names to be suggested so that typing names is faster, but only if I explicitly opt in.
- **Setting:** `Typing → Suggestions & correction → Use contacts dictionary`
- **Pref key / storage:** `SharedPreferences: settings_key_use_contacts_dictionary`
- **Default:** As in `docs/user-stories-settings-inventory.md`
- **Behavior:** When enabled, the keyboard may read contacts (subject to Android permission) and suggest contact names.
- **Dependencies:** Android `READ_CONTACTS` permission.
- **Privacy / security notes:** Risk: **high** (contacts are sensitive). Mitigations: explicit opt-in, permission prompt, clear UI labeling, no network transmission, no logging of contacts-derived suggestions.
- **Acceptance criteria:**
  - Enabling prompts for permission if missing.
  - Disabling stops contacts usage without leaving stale permissions prompts.

#### Context profiles (per-app presets)

- **Story:** As a power user, I want per-app presets (dictionary + transformation rules) so that typing in each app can follow different conventions.
- **Setting:** `Typing → Suggestions & correction → Context profiles`
- **Pref key / storage:** Non-`SharedPreferences` controller state + profile store (see `ContextProfiles…` classes); UI keys include `context_profiles_enabled`, `context_profiles_manage_presets`, `context_profiles_manage_bindings`, `context_profiles_reset`
- **Default:** Disabled
- **Behavior:** Enables app-scoped profile rules and allows managing presets and app bindings.
- **Dependencies:** App package detection; profile store.
- **Privacy / security notes:** Risk: **medium** (stores app bindings + user-defined rules). Mitigation: local-only; avoid collecting app content beyond package ID; never log typed text.
- **Acceptance criteria:**
  - When disabled, no app-specific overrides are applied.
  - Reset clears profiles/bindings and returns to global behavior.

### Gestures & key behaviors

#### Gesture typing

- **Story:** As a user, I want optional gesture typing so that I can swipe to enter words, and I want it to clearly disable conflicting swipe actions.
- **Setting:** `Gestures & quick keys → Gesture typing`
- **Pref key / storage:** `SharedPreferences: settings_key_gesture_typing`
- **Default:** As in `docs/user-stories-settings-inventory.md`
- **Behavior:** Enables gesture typing; may disable/lock conflicting swipe-direction actions.
- **Dependencies:** Gesture typing engine; touch input.
- **Privacy / security notes:** Risk: **low/medium** (processes typed paths; no additional storage beyond normal suggestions).
- **Acceptance criteria:**
  - Enabling gesture typing disables conflicting swipe-direction settings.
  - Disabling gesture typing re-enables swipe-direction settings.

#### Swipe/pinch/stretch actions

- **Story:** As a user, I want to map swipe and pinch/stretch gestures to actions so that I can optimize keyboard navigation.
- **Setting:** `Gestures & quick keys → Swipe & pinch gestures → …`
- **Pref key / storage:** `SharedPreferences: settings_key_swipe_*`, `settings_key_pinch_gesture_action`, `settings_key_separate_gesture_action`, plus thresholds.
- **Default:** As in `docs/user-stories-settings-inventory.md`
- **Behavior:** Maps gestures to actions like shift/hide/next keyboard; adjusts recognition thresholds.
- **Dependencies:** Touch gesture recognizer.
- **Privacy / security notes:** Risk: **low**.
- **Acceptance criteria:**
  - Changing a gesture action changes runtime behavior immediately (or after keyboard restart if required).
  - Threshold changes make gestures more/less sensitive without breaking typing.

#### Quick keys, emoji defaults, and quick-text behavior

- **Story:** As a user, I want quick-text/emoji defaults and quick-keys behavior controls so that common inserts are fast and consistent.
- **Setting:** `Gestures & quick keys → Quick keys & emoji → …` and `Quick keys` settings screen
- **Pref key / storage:** `SharedPreferences: settings_key_one_shot_quick_text_popup`, `settings_key_search_quick_text_tags`, `settings_key_initial_quick_text_tab`, `settings_key_default_emoji_skin_tone`, `settings_key_emoticon_default_text`, etc.
- **Default:** As in `docs/user-stories-settings-inventory.md`
- **Behavior:** Controls quick-text popup behavior, tag searching, default emoji modifiers, and default emoticon insertions.
- **Dependencies:** Android version gates for emoji features; quick-text add-ons.
- **Privacy / security notes:** Risk: **low/medium** (user-defined defaults stored locally). Mitigation: local-only; avoid logging inserts.
- **Acceptance criteria:**
  - Default emoji modifiers apply consistently to supported emoji.
  - Quick-text popup behavior matches the toggle(s) and persists across restarts.

### Appearance & themes

#### Select a theme

- **Story:** As a user, I want to choose a keyboard theme so that the keyboard matches my preferences and accessibility needs.
- **Setting:** `Look & feel → Theme → Keyboard theme`
- **Pref key / storage:** Theme selection is managed by the theme add-on system (not a single `SharedPreferences` key); entry action: `nav:theme_selector`
- **Default:** App-defined theme default
- **Behavior:** Opens theme selector and persists the chosen theme.
- **Dependencies:** Installed theme add-ons.
- **Privacy / security notes:** Risk: **low**.
- **Acceptance criteria:**
  - Selecting a theme updates keyboard appearance.
  - Theme selection persists across restarts.

#### Customize appearance (wallpaper, colors, fonts, shadows, presets)

- **Story:** As a user, I want to customize the current theme (including wallpapers and presets) so that I can personalize the keyboard without installing new themes.
- **Setting:** `Look & feel → Theme → Customize appearance`
- **Pref key / storage:** Theme override stores (e.g., `KeyboardThemeUserOverridesStore`, `KeyboardWallpaperOverrideStore`, preset store); action: `nav:keyboard_theme_wallpaper_customization`
- **Default:** No overrides (theme defaults)
- **Behavior:** Lets the user import photos, adjust opacity/tints, change fonts, and export/import presets.
- **Dependencies:** Android document picker; local storage; theme system.
- **Privacy / security notes:** Risk: **medium** (local files/photos). Mitigations: use scoped storage/document picker, avoid storing URIs without user consent, never upload theme photos.
- **Acceptance criteria:**
  - User can import a photo and see it applied in preview.
  - Reset clears overrides and returns to theme defaults.
  - Export/import uses explicit user-selected URIs.

#### Night mode behavior

- **Story:** As a user, I want night mode controls so that the keyboard can be comfortable in low light without breaking sound/vibration preferences.
- **Setting:** `Look & feel → Theme → Night mode`
- **Pref key / storage:** `SharedPreferences: settings_key_night_mode`, `settings_key_night_mode_*_control`
- **Default:** As in `docs/user-stories-settings-inventory.md`
- **Behavior:** Controls night mode policy and whether app theme, keyboard theme, sound, and vibration follow night mode.
- **Dependencies:** Android/AppCompat night mode support.
- **Privacy / security notes:** Risk: **low**.
- **Acceptance criteria:**
  - Changing app night mode control triggers `applyDayNight()` without crashing.
  - Sound/vibration toggles affect night mode behavior only (not global enablement).

#### Key hints, previews, and toolbar rows

- **Story:** As a user, I want control over hint labels, previews, and toolbar rows so that I can balance information density, accessibility, and screen space.
- **Setting:** `Look & feel → Key text & hints / Layout size / Toolbar / Feedback & previews`
- **Pref key / storage:** `SharedPreferences: settings_key_show_hint_text_key`, `settings_key_hint_size`, `settings_key_extension_keyboard_enabled`, `settings_key_key_press_shows_preview_popup`, and related options.
- **Default:** As in `docs/user-stories-settings-inventory.md`
- **Behavior:** Toggles hint labels, keyboard name display, preview popups, fullscreen modes, and extension toolbar row behavior.
- **Dependencies:** Screen orientation; device UI constraints; installed row add-ons.
- **Privacy / security notes:** Risk: **low**.
- **Acceptance criteria:**
  - Enabling/disabling hints and previews updates the keyboard UI reliably.
  - Toolbar rows can be enabled/disabled and selected without breaking typing.

### Haptics & sound

#### Keyboard vibration (haptics)

- **Story:** As a user, I want haptic feedback on keypress and long-press so that typing feels responsive, with fine-grained control over vibration duration.
- **Setting:** `Look & feel → Feedback & previews → …` (and any legacy “Effects” surface)
- **Pref key / storage:** `SharedPreferences: settings_key_vibrate_on_key_press_duration_int` (tri-state: `-1` = system default, `0` = off, `>0` = custom ms), `settings_key_vibrate_on_long_press` (and advanced fallback: `settings_key_system_vibration_fallback_duration_int`).
- **Default:** As in `docs/user-stories-settings-inventory.md`
- **Behavior:** Enables/disables vibration and configures duration; supports deferring to Android system vibration settings when the duration is set to `-1` (“system default”).
- **Dependencies:** Android vibrator APIs; OS version-specific support for predefined effects.
- **Privacy / security notes:** Risk: **low**.
- **Acceptance criteria:**
  - When enabled, keypress triggers vibration respecting duration or system control.
  - When disabled, no vibration occurs on keypress/long-press.

#### Keypress sound and custom volume

- **Story:** As a user, I want optional keypress sounds and a custom volume so that audio feedback can be tuned independently of system defaults.
- **Setting:** `Look & feel → Feedback & previews → Sound on key-press` (+ UI tweaks for volume where present)
- **Pref key / storage:** `SharedPreferences: sound_on`, `settings_key_use_custom_sound_volume`, `custom_sound_volume`
- **Default:** As in `docs/user-stories-settings-inventory.md`
- **Behavior:** Plays keypress sounds and optionally uses a user-defined volume.
- **Dependencies:** Android audio APIs.
- **Privacy / security notes:** Risk: **low**.
- **Acceptance criteria:**
  - Sound toggle controls whether keypress sounds play.
  - Custom volume only applies when enabled.

### Voice input / dictation

#### Choose dictation provider

- **Story:** As a user, I want to choose a dictation backend so that I can trade off accuracy, privacy, and cost.
- **Setting:** `Speech to text → Speech-to-text backend`
- **Pref key / storage:** `SharedPreferences: settings_key_speech_to_text_backend`
- **Default:** `openai`
- **Behavior:** Selects which backend receives microphone audio for transcription.
- **Dependencies:** Microphone permission; backend implementation.
- **Privacy / security notes:** Risk: **high** (microphone audio + network). Mitigation: explicit opt-in, clear privacy notice, no background recording, no logging of transcripts.
- **Acceptance criteria:**
  - Selecting a backend updates the active provider.
  - Privacy notice is visible on the dictation settings screen.

#### Store API keys securely (encrypted at rest)

- **Story:** As a user, I want my dictation API keys stored securely so that enabling dictation doesn’t leak secrets if backups/logs are shared.
- **Setting:** `OpenAI → API key` and `ElevenLabs → API key`
- **Pref key / storage:** `settings_key_openai_api_key`, `settings_key_elevenlabs_api_key` stored via `SpeechToTextSecretsStore` (Android Keystore); preference is non-persistent.
- **Default:** Empty
- **Behavior:** Saves API key encrypted and removes any legacy plaintext value from `SharedPreferences`.
- **Dependencies:** Android Keystore availability.
- **Privacy / security notes:** Risk: **high** (secrets). Mitigation: Keystore encryption + avoid echoing key in UI.
- **Acceptance criteria:**
  - Setting an API key persists it across restarts.
  - Clearing removes the key and removes any legacy plaintext pref.

#### Configure network endpoints safely

- **Story:** As an advanced user, I want to override dictation endpoints/models so that I can self-host or experiment, while the UI makes it clear this may affect privacy/security.
- **Setting:** `OpenAI → Endpoint/Model/Language…` and `ElevenLabs → Endpoint/Model/Language…`
- **Pref key / storage:** `SharedPreferences: settings_key_openai_endpoint`, `settings_key_elevenlabs_endpoint`, and related model/language prefs.
- **Default:** As in `docs/user-stories-settings-inventory.md`
- **Behavior:** Changes which URL is called and how requests are configured.
- **Dependencies:** Network connectivity; backend compatibility.
- **Privacy / security notes:** Risk: **high** (network destination controls who receives audio/transcripts). Mitigations: strong in-UI warnings, no silent fallback, avoid sending data when disabled.
- **Acceptance criteria:**
  - Changing endpoint takes effect for new dictation requests.
  - Invalid endpoints fail gracefully without crashing the IME.

### Clipboard

#### OS clipboard sync

- **Story:** As a user, I want control over OS clipboard integration so that clipboard contents are only used when I explicitly allow it.
- **Setting:** `Clipboard → OS clipboard sync`
- **Pref key / storage:** `SharedPreferences: settings_key_os_clipboard_sync`
- **Default:** As in `docs/user-stories-settings-inventory.md`
- **Behavior:** Enables/disables syncing keyboard clipboard features with the OS clipboard.
- **Dependencies:** Android clipboard APIs.
- **Privacy / security notes:** Risk: **high** (clipboard may contain sensitive data). Mitigation: default-safe behavior, clear labeling, no logging, no sharing.
- **Acceptance criteria:**
  - When disabled, the keyboard does not read/write OS clipboard beyond what Android mandates.
  - When enabled, clipboard integration functions as documented.

#### Clipboard icon visibility

- **Story:** As a user, I want to choose whether the clipboard icon is always shown so that I can balance quick access with a clutter-free keyboard.
- **Setting:** `Clipboard → Always show clipboard icon`
- **Pref key / storage:** `SharedPreferences: settings_key_clipboard_action_always_visible`
- **Default:** As in `docs/user-stories-settings-inventory.md`
- **Behavior:** When enabled, the clipboard icon remains visible above the keyboard. When disabled, it appears after a clipboard change and hides after a short timeout. Long-press opens clipboard history.
- **Dependencies:** Requires `Clipboard → OS clipboard sync` to be enabled.
- **Privacy / security notes:** Risk: **low**. This setting only affects UI affordance; clipboard collection is controlled by OS clipboard sync.
- **Acceptance criteria:**
  - Default behavior matches the prior timeout-based visibility (not always visible).
  - When enabled, the clipboard icon remains accessible for long-press history at any time.

### Advanced

#### Compatibility workarounds

- **Story:** As a user hitting device/app quirks, I want compatibility toggles so that I can recover functionality without waiting for upstream fixes.
- **Setting:** `Troubleshooting & backup → Compatibility → …`
- **Pref key / storage:** `SharedPreferences: settings_key_workaround_disable_rtl_fix`, `settings_key_allow_suggestions_restart`, `settings_key_always_use_fallback_user_dictionary`
- **Default:** As in `docs/user-stories-settings-inventory.md`
- **Behavior:** Enables targeted workarounds for RTL rendering, suggestion restart behavior, and dictionary fallback.
- **Dependencies:** Device-specific behavior; dictionary engine.
- **Privacy / security notes:** Risk: **low** (behavioral toggles).
- **Acceptance criteria:**
  - Each toggle changes behavior only in its intended scope (no global regressions).
  - Toggles are safe to flip without a crash.

#### Power saving mode

- **Story:** As a user, I want a power saving mode so that the keyboard can reduce CPU/battery usage when needed.
- **Setting:** `Troubleshooting & backup → Performance & battery → Power saving`
- **Pref key / storage:** `SharedPreferences: settings_key_power_save_mode` and `settings_key_power_save_mode_*_control`
- **Default:** As in `docs/user-stories-settings-inventory.md`
- **Behavior:** Adjusts feature availability (suggestions/gesture typing/animations/sound/vibration/theme) based on power save mode.
- **Dependencies:** Power saving policy evaluation; feature gating.
- **Privacy / security notes:** Risk: **low**.
- **Acceptance criteria:**
  - Switching modes updates gating behavior predictably.
  - Controls only affect the keyboard (no system settings changes).

#### Settings UI & launcher visibility

- **Story:** As a user, I want control over settings UI language and launcher visibility so that the app fits my device and workflow.
- **Setting:** `Settings UI & launcher → …`
- **Pref key / storage:** `SharedPreferences: settings_key_force_locale`, `settings_key_keyboard_icon_in_status_bar`, `settings_key_show_settings_app`
- **Default:** As in `docs/user-stories-settings-inventory.md`
- **Behavior:** Overrides in-app locale, toggles status-bar icon, and hides/shows the settings app launcher entry.
- **Dependencies:** Android launcher behavior; OS locale list.
- **Privacy / security notes:** Risk: **low**.
- **Acceptance criteria:**
  - Force-locale takes effect after restart (or as documented).
  - Launcher entry visibility matches the toggle without requiring unsafe permissions.

### Privacy & security

#### Programmable API (controller apps)

- **Story:** As an advanced user, I want an opt-in programmable API so that trusted apps can control specific keyboard behaviors, without allowing arbitrary apps to exfiltrate or automate typed text.
- **Setting:** `Programmable API → …` (deep-linkable settings screen)
- **Pref key / storage:** `SharedPreferences: settings_key_keyboard_api_enabled`, `settings_key_keyboard_api_high_risk_actions_enabled`, `settings_key_keyboard_api_clipboard_copy_cut_enabled`, `settings_key_keyboard_api_automation_controllers_enabled` + controller allow-list stores.
- **Default:** Disabled
- **Behavior:** Enables the API provider component and allows pairing/allow-listing controllers with scopes; high-risk actions require explicit confirmation.
- **Dependencies:** Controller pairing store; scope enforcement; password-field detection.
- **Privacy / security notes:** Risk: **high** (external app control). Mitigations: default-off, allow-list, scope checks, high-risk confirmation dialogs, audit log without payloads, block in password fields.
- **Acceptance criteria:**
  - Enabling the API toggles the provider component enabled state.
  - High-risk toggles show confirmation and do not enable silently.
  - Audit log records calls without storing typed text payloads.

#### Crash reporting / diagnostics without typed-text leakage

- **Story:** As a user, I want optional crash notifications and diagnostics so that bugs can be reported, without risking typed-text leakage.
- **Setting:** `Troubleshooting & backup → Developer & diagnostics → Report on crash` and diagnostic actions.
- **Pref key / storage:** `SharedPreferences: settings_key_show_chewbacca` and related diagnostic actions (`nav:logcat_viewer`, developer tools).
- **Default:** As in `docs/user-stories-settings-inventory.md`
- **Behavior:** Shows crash notifications and surfaces developer tools/logcat viewer.
- **Dependencies:** Notification permissions; logcat access limitations.
- **Privacy / security notes:** Risk: **high** (logs can contain sensitive info). Mitigations: redact/avoid logging typed text; clear UI warnings before sharing logs.
- **Acceptance criteria:**
  - Crash notifications can be disabled and stop appearing.
  - Sharing diagnostics does not include typed text (enforced by code review + tests/CI guardrails).

### Backups / import / export

#### Backup and restore user data

- **Story:** As a user, I want an explicit backup/restore flow so that I can migrate settings and dictionaries between devices.
- **Setting:** `Troubleshooting & backup → Backup & restore → Backup` / `Restore`
- **Pref key / storage:** `nav:backup_prefs`, `nav:restore_prefs` (actions)
- **Default:** N/A
- **Behavior:** Exports/imports user data via user-chosen storage location.
- **Dependencies:** Android document picker; backup/restore implementation.
- **Privacy / security notes:** Risk: **high** (exports user dictionaries/settings). Mitigations: explicit user action, scoped storage, no background exports, clear “contains personal data” warning.
- **Acceptance criteria:**
  - Backup prompts for a destination and succeeds without silent failure.
  - Restore prompts for a source and clearly indicates what will be overwritten.

### About / diagnostics

#### About, licenses, changelog, and issue reporting

- **Story:** As a user, I want to find version info, licenses, and changelog, and report issues so that I can understand what I’m running and get support.
- **Setting:** `Help & about → …`
- **Pref key / storage:** `nav:about`, `nav:licenses`, `nav:changelog`, `nav:report_issue` (actions)
- **Default:** N/A
- **Behavior:** Navigates to about/licensing/changelog screens or issue reporting flow.
- **Dependencies:** UI navigation; external browser (for issue reporting) if used.
- **Privacy / security notes:** Risk: **low/medium** depending on what is shared in a report; mitigation: avoid auto-attaching logs without consent.
- **Acceptance criteria:**
  - Each action opens the expected screen without crash.
  - Issue reporting does not automatically include typed text.
