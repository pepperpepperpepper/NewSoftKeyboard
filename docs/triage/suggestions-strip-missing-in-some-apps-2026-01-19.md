# Suggestions strip missing in some apps (2026-01-19)

## Field report

- Users report the **suggestions strip is missing in some apps/fields**, even though it is enabled in NSK settings.
- Example: **Google Keep** — “many Keep note editors” show **no suggestions strip**.

This is a UX problem because users expect consistent behavior across text fields, especially for ordinary note-taking apps.

## How the strip is currently gated (code pointers)

At input start, we decide whether prediction/suggestions should be enabled based on `EditorInfo`:

- `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/InputFieldConfigurator.java`
  - Turns `predictionOn` off for non-text fields (number/phone/datetime) and password variations.
  - If the editor provides `TYPE_NULL` (or an otherwise “classless” `inputType` where
    `inputType & TYPE_MASK_CLASS == 0`), we treat it as “text” for strip visibility, but disable
    auto-space and auto-pick for safety/compat.
- If the editor sets `EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS`, behavior depends on the user setting:
  - **Respect enabled**: turns `predictionOn` off (no word suggestions), but keeps the strip visible
    with a **“No suggestions (app)”** action that opens Typing settings and scrolls to the toggle.
  - **Respect disabled**: keeps `predictionOn` on, but disables auto-pick/auto-correct for safety.
- `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/PredictionState.java`
  - `isPredictionOn()` is `predictionOn && showSuggestions`.
- `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeSuggestionsController.java`
  - Applies input-field config in `onStartInputView(...)` and then sets strip visibility via
    `getInputViewContainer().setActionsStripVisibility(isPredictionOn())`.
  - Reads `settings_key_respect_app_no_suggestions_flag` to decide whether to respect `NO_SUGGESTIONS`.

So if Keep sets `TYPE_TEXT_FLAG_NO_SUGGESTIONS`, the strip will be hidden only when the “respect” setting is enabled (password-ish input types still disable suggestions unconditionally).

Note: As of late Jan 2026, when “respect” is enabled and we disable predictions due to `NO_SUGGESTIONS`, we still keep the strip visible (empty) to avoid the “it randomly disappears” UX, and provide a quick link to the setting.

## Hypotheses

1. **Keep sets `TYPE_TEXT_FLAG_NO_SUGGESTIONS`** on some/all note editors (common in some Google apps).
1. **Keep sets `TYPE_NULL` (or a “classless” `inputType` where `inputType & TYPE_MASK_CLASS == 0`)**
   on some/all note editors (less common, but would explain a fully missing strip in keyboards that
   treat that as “no prediction”).
1. Keep uses a text variation we treat as “no prediction” (less likely for a normal note editor).
1. A per-app **Context Profile** may be disabling next-word suggestions or other suggestion sources, making the UI appear “dead” even when the strip itself is visible.
1. Power saving suggestion gating could be disabling `showSuggestions` if the power-saving override is enabled (but the report sounds per-app/field, not global).

## Next actions (investigation plan)

1. Add logging that prints `EditorInfo.packageName`, `fieldId`, `inputType`, `imeOptions` and whether we’re respecting/ignoring `NO_SUGGESTIONS` (done in `InputFieldConfigurator`).
2. Add a targeted **instrumentation test** (Genymotion-friendly) that launches a simple activity and asserts:
   - fields without `NO_SUGGESTIONS` show the strip,
   - fields with `NO_SUGGESTIONS` show/hide based on the new setting,
   - we log/explain the reason.
   - Implemented as `SuggestionsStripNoSuggestionsFlagInstrumentedTest` + `TestInputTypesActivity` (see below).
3. Keep iterating on override UX:
   - current behavior: **global toggle** `settings_key_respect_app_no_suggestions_flag` (default: off),
   - when off, we keep suggestions visible but disable auto-pick/auto-correct in `NO_SUGGESTIONS` fields.

## Open questions

- In Google Keep, does the editor set `NO_SUGGESTIONS` consistently, or only for certain note types/widgets?
- Is a global override sufficient, or do we need per-app overrides (and/or “don’t learn in NO_SUGGESTIONS fields” as a separate switch)?

## Genymotion / emulator validation (no Google login required)

Google Keep may not run on stock Genymotion images (no Google Play Services / login), so we need an offline repro.

### Built-in harness (preferred)

Use the app’s internal test harness activity:

- `wtf.uhoh.newsoftkeyboard.app.debug.TestInputTypesActivity`
  - Plain multi-line editor: should show the strip.
  - `textNoSuggestions` editor (Keep-like): should show/hide based on the “Respect apps that disable suggestions” toggle.
  - `classlessNoSuggestions` editor: reports `TYPE_TEXT_FLAG_NO_SUGGESTIONS` with no class bits set (some editors set flags but omit `TYPE_CLASS_TEXT`).
  - `typeNull` editor: should show the strip, but auto-space/auto-pick are disabled. Note: some Android versions coerce `android:inputType="none"` on `EditText`, so the harness uses `TypeNullEditText` to force `EditorInfo.inputType=0`.

You can launch it from `adb`:

```bash
adb shell am start -n wtf.uhoh.newsoftkeyboard/wtf.uhoh.newsoftkeyboard.app.debug.TestInputTypesActivity
```

### Instrumentation test (automated)

The automated regression is:

- `ime/app/src/androidTest/java/wtf/uhoh/newsoftkeyboard/app/ime/SuggestionsStripNoSuggestionsFlagInstrumentedTest.java`

Run it on a connected emulator/device via Gradle, filtering to the single class:

```bash
./gradlew :ime:app:tasks --all | rg "connectedNsk.*AndroidTest"
./gradlew :ime:app:connectedNskReleaseAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=wtf.uhoh.newsoftkeyboard.app.ime.SuggestionsStripNoSuggestionsFlagInstrumentedTest
```

If you need to force debug androidTest (Genymotion-stable), set `TEST_BUILD_TYPE=debug` and re-check available tasks.

### Real apps (optional)

If you still want a real note-taking app on Genymotion without Google login, prefer an offline/open-source notes app installed via F-Droid, then check whether its editor sets `NO_SUGGESTIONS` (look for the `Input requested NO_SUGGESTIONS (inputType=0x...)` log line).

### ADB log capture helper (real device)

If you have a physical device where the strip is missing (e.g., Google Keep), you can capture the relevant `InputFieldConfigurator` decision logs via:

```bash
scripts/adb_suggestions_strip_diagnostics.sh
```
