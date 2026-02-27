# Haptics “mystery” notes

- In the code, there’s no “mic-only” haptics path: letter keys and the mic key should both hit `performKeyVibration(...)` from `onPress(...)` (`AnySoftKeyboard/ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImePressEffects.java:417`, `AnySoftKeyboard/ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImePressEffects.java:342`). If the mic “works” but letters don’t, the mic vibration is very likely coming from _outside_ the keyboard-key haptics (e.g., voice-recognition UI feedback), or you’re only noticing _long-press_ vibration on the mic.
- Key-press vibration defaults to **20ms** (`AnySoftKeyboard/ime/app/src/main/res/values/settings_defaults_dont_translate.xml:7`). Long-press vibration defaults to enabled (7ms via code) (`AnySoftKeyboard/ime/app/src/main/res/values/settings_defaults_dont_translate.xml:12`, `AnySoftKeyboard/ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImePressEffects.java:161`).
- **Android 14 QPR / Android 15+ note:** Android now has a _separate_ system-level toggle for keyboard vibration (internally `Settings.System.KEYBOARD_VIBRATION_ENABLED`, key name `keyboard_vibration_enabled`). The platform also introduced a dedicated vibration usage for keyboards: `VibrationAttributes.USAGE_IME_FEEDBACK` (internally `0x52` / `82`). If a device has keyboard vibration enabled but touch feedback disabled, keyboards that vibrate using `USAGE_TOUCH` can appear “dead” while other keyboard apps (using `USAGE_IME_FEEDBACK`) still vibrate.
- On Android 13+ (API 33), vibrations tagged as `VibrationAttributes.USAGE_TOUCH` can be suppressed when the OS-wide touch-haptics setting is effectively off (for example: `Settings.System.HAPTIC_FEEDBACK_ENABLED == 0` or `haptic_feedback_intensity == 0`). To avoid “SwiftKey vibrates but NSK doesn’t” in **system mode**, `PressVibratorV33` prefers the IME vibration usage (`USAGE_IME_FEEDBACK`) when available. In **non-system mode**, `PressVibratorV33` uses the legacy audio-attributes vibration path so the app’s “Vibrate on key-press” duration slider can still work even if the OS keyboard-vibration channel is disabled/suppressed.
- Power-saving/night-mode vibration controls can force the effective vibration durations to `0` (disabling key haptics) (`AnySoftKeyboard/ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImePressEffects.java:135`).
- Fast ways to pinpoint which case you’re in:
  1. Confirm whether the “mic haptic” happens immediately on press-down or only when voice UI starts.
  2. Temporarily disable “Use system vibration” and set “Vibrate on key press” to something >0ms—if letters then vibrate, the system-haptics path is being blocked by OS settings/gates.
  3. Check logcat for `Failed to read system vibration pref` / `Failed to interact with vibrator...` from `ImePressEffects`.

## What “system haptics” actually does here (and why it can feel like “no haptics”)

In `ImePressEffects.performKeyVibration(...)`, when system mode is on, the behavior depends on API level:

- **API 29+**: When system-wide touch haptics are enabled, NSK prefers view-based `performHapticFeedback(...)` (KEYBOARD_TAP → VIRTUAL_KEY). If that fails (or system-wide touch haptics are disabled), NSK uses vibrator-based system effects (`PressVibratorV29+` predefined click/heavy-click, and API 33 `VibrationAttributes`) as the primary implementation.
- **API < 29** (and when the vibrator is in an error state): NSK tries **View-based haptics first** and may return early if the framework reports it was handled.

Historically this created a nasty real-world failure mode for “system haptics”:

- On some devices/OS builds, `performHapticFeedback(KEYBOARD_TAP)` can return `true` even when the user has “Touch feedback” intensity set to none/0 (or an OEM bug suppresses that specific effect).
- In that situation, the code **never reaches** the vibrator-based fallback, so you get **no haptic** even though the framework call “succeeded”.

This pattern would also explain “mic haptics but not letters” if the mic interaction you’re noticing is actually:

- a **long-press** haptic (`LONG_PRESS`), which is a different effect (`ImePressEffects.java:381-388`), or
- haptic feedback from the **voice UI** rather than the key-press pipeline.

## Another key difference vs other keyboards: vibration usage + system setting interaction

One “this feels broken” difference (especially on Android 13+) is how vibrations are _classified_:

- On API 33+, NSK uses `VibrationAttributes.USAGE_TOUCH` when the OS-wide touch-haptics setting is enabled.
- When that OS setting is disabled, NSK falls back to the older `AudioAttributes` path (used on older Android versions) so the keyboard can still provide vibration when it’s enabled in NSK settings.

## Why it can look like “mic/toolbar haptics work, letter keys don’t”

The key-press haptics pipeline for _keyboard keys_ is `PointerTracker → ImeServiceBase.onPress(...) → ImePressEffects.performKeyVibration(...)`.

If you feel haptics on the mic (or arrow keys / toolbar controls) but not on letter keys, there are a few common explanations:

1. **You’re feeling a non-key haptic**

- Some UI elements outside the keyboard grid (suggestions strip taps, toolbar buttons, voice-recognition UI) can trigger their own haptics.
- Those haptics are _not_ evidence that the keyboard-key pipeline is working; they can come from the system or from a different view click path.

2. **You’re only seeing long-press vibration**

- Key-press vibration defaults to `0ms` (off), while long-press defaults to a short “yes” (7ms).
- In Settings, “Use system vibration” disables the “Vibrate on key-press” duration slider (so you can’t “turn it up” while staying in system mode).
- If you mostly notice haptics on things you long-press (mic long-press, popup keys, etc.), that fits “mic works, letters don’t”.

3. **System haptics are enabled, but the effect we’re using is effectively suppressed**

- On API < 29 (and when vibrator interaction fails), we fall back to view-based haptic-feedback (`KEYBOARD_TAP` / `VIRTUAL_KEY`).
- On some devices, that call can “succeed” (returns `true`) even when the user’s effective touch feedback intensity is 0 or the OEM suppresses that specific effect, so relying on it can make the keyboard feel dead.

4. **Per-app vibration settings**

- Some OEM Android builds have per-app vibration intensity/disable controls. If NSK’s app vibration is set to 0/disabled there, NSK can appear to have “no haptics” while other keyboard apps still vibrate.

## Why third-party keyboards can still vibrate when NSK doesn’t

Based on this codebase, the “SwiftKey works, NSK doesn’t” datapoint is consistent with at least one of:

- **Different vibration “usage” / attributes:** On API 33+, vibrations tagged as `USAGE_TOUCH` can be suppressed when the OS “Touch feedback” channel is disabled. Some keyboards may always use a different usage/legacy API and still vibrate; NSK falls back to the legacy audio-attributes vibration path when the OS-wide touch-haptics setting is off.
- **System “Keyboard vibration” toggle interaction (Android 14+/15+):** Vibrations using `USAGE_IME_FEEDBACK` may be gated by the system’s keyboard-vibration toggle (`keyboard_vibration_enabled`). Some keyboards still vibrate by using legacy audio-attributes vibrations (or a different usage). To keep NSK’s own “Vibrate on key-press” duration slider effective even when that system keyboard channel is disabled/suppressed, NSK uses the legacy audio-attributes vibration path in **non-system mode**.
- **Different fallback strategy:** If `performHapticFeedback(...)` lies about success, a keyboard that _does not trust that return value_ (or that does vibrator-first) will be more robust.
- **Different OS support level:** On API < 29, NSK can’t use predefined system click effects from the vibrator service (those are wired up in `PressVibratorV29+`), so “system mode” leans harder on view-based haptics and can fail more often when the OS suppresses that path.

## What we should verify next (practical triage)

1. **Confirm whether key-press vibration works at all in non-system mode**

- Turn **off** “Use system vibration”.
- Set “Vibrate on key-press” to **20–50ms**.
- If you _still_ get no haptics on letter keys, the issue is not specific to “system mode” (it’s OS/app-level suppression or vibrator service failure).

2. **Confirm system-wide touch feedback and app-level vibration**

- Check `Settings.System.HAPTIC_FEEDBACK_ENABLED` and (when present) `haptic_feedback_intensity` (what this code reads for system mode).
- On Android 14+/15+ builds with a dedicated keyboard vibration toggle, also check `keyboard_vibration_enabled`.
- On-device via adb:
  - `adb shell settings get system haptic_feedback_enabled`
  - `adb shell settings get system haptic_feedback_intensity`
  - `adb shell settings get system keyboard_vibration_enabled`
- Also check OEM per-app vibration controls for the NSK app (if your device has them).

3. **Run the on-device regression**

- `AnySoftKeyboard/ime/app/src/androidTest/java/wtf/uhoh/newsoftkeyboard/app/ime/KeyPressHapticsInstrumentedTest.java`
- This verifies the _logic_ that system mode prefers view-haptics when system-wide touch haptics are enabled (and prefers the vibrator otherwise), and that view haptics still tries `KEYBOARD_TAP → VIRTUAL_KEY`. It cannot verify “physical vibration happened”, but it catches obvious regressions.

## Fix plan ideas (ranked)

1. **Add a user-controlled “system mode fallback vibration duration” (low risk)**

- Keep “system haptics” as the primary path.
- If the user opts in, always do a short `vibrateFallback(oneShotDuration)` after the view haptic attempt (or when the system mode is enabled but haptics appear dead), so users can work around OEM bugs without disabling system mode.
- Implemented as the “Fallback vibration (system mode)” slider (`settings_key_system_vibration_fallback_duration_int`). When set to >0ms and “Use system vibration” is enabled, NSK will also trigger a vibrator one-shot even if `performHapticFeedback(...)` reports success.

2. **Stop trusting `performHapticFeedback(...)` return values in system mode (medium risk)**

- Always attempt both `KEYBOARD_TAP` and `VIRTUAL_KEY` (even if the first reports handled), and only treat haptics as “done” if we have strong evidence.
- Risk: double-haptics on devices where both effects actually vibrate.
- Implemented: `performSystemHapticFeedback(...)` now attempts both effects even if the first reports “handled”.

3. **Prefer vibrator-first in system mode (higher behavior change)**

- Use the vibrator predefined click/heavy-click (and API 33 `VibrationAttributes`) as the primary implementation, using view haptics as the “last ditch” fallback.
- This may match what many keyboards do in practice and avoids the “return true but no haptic” trap, but it changes semantics vs the current “view-first” design.
- Implemented (with a guard): on API 29+, system mode uses vibrator-first when system-wide touch haptics are disabled (or view haptics fail), and falls back to view haptics when vibrator interaction fails.

4. **Use a keyboard-like predefined effect (tick) and fall back when unsupported (low risk)**

- Some devices may not implement specific predefined effects well (or at all), making “system mode” feel like “no haptics”.
- Prefer `VibrationEffect.EFFECT_TICK` for key-press system mode (closer to `KEYBOARD_TAP`), fall back to `EFFECT_CLICK`, and if predefined effects are unsupported, use a short duration-based one-shot fallback.
- Implemented: `PressVibratorV29` selects the first supported predefined effect and falls back to one-shot durations (10ms press / 30ms long-press) when needed.

5. **Use the default vibrator on API 31+ and make fallback vibrations harder to suppress (low risk)**

- On API 31+ (`VibratorManager`), always use the system’s _default_ vibrator rather than relying on the legacy `VIBRATOR_SERVICE` lookup.
- On API 33+, when we’re in a fallback vibration path (system-mode fallback duration slider / duration-based fallback), always use the legacy audio-attributes vibration path instead of `VibrationAttributes.USAGE_TOUCH` to maximize the chances of producing a physical haptic on OEM builds.
- Implemented: `ImePressEffects` picks the vibrator via `VibratorManager` when available, and `PressVibratorV33` uses audio attributes for fallback vibrations.

6. **Prefer view-haptics first when system-wide touch haptics are enabled (low risk)**

- On some devices, direct `Vibrator.vibrate(...)` calls from IMEs appear suppressed while view-based `performHapticFeedback(...)` still produces haptics (this can look like “mic/toolbar haptics work but letters don’t”).
- When system-wide touch haptics are enabled, use view-haptics as the primary system-mode implementation and fall back to vibrator-based system effects when view haptics fail.
- Implemented: on API 29+, system mode prefers view-haptics when system-wide touch haptics are enabled.

7. **Don’t trust `KEYBOARD_TAP` alone in system mode (low risk)**

- Some devices appear to report `performHapticFeedback(KEYBOARD_TAP)` as “handled” while producing no physical haptics.
- In system mode, treat `VIRTUAL_KEY` as the more reliable success signal; if it isn’t handled on the input view, also try `VIRTUAL_KEY` on the container view before considering view-haptics “done”.
- Implemented: in system mode, `performSystemHapticFeedback(...)` only treats `VIRTUAL_KEY` as reliable and will also probe the container view’s `VIRTUAL_KEY` path.

8. **Read “touch feedback intensity” from both `Settings.System` and `Settings.Secure` (low risk)**

- Some Android builds appear to store the `"haptic_feedback_intensity"` setting under `Settings.Secure` instead of `Settings.System`.
- If we only read `Settings.System`, we can incorrectly think touch-haptics are enabled and pick haptic paths/attributes that get suppressed (resulting in “no haptics” even though other keyboards vibrate).
- Implemented: `ImePressEffects` now observes `"haptic_feedback_intensity"` from both `Settings.System` and `Settings.Secure` and treats intensity `0` in either location as “system-wide touch haptics disabled”.

9. **Don’t let previous vibrator errors “dead-end” system haptics on API 29+ (low risk)**

- On API 29+ in system mode, if view haptics fail and the vibrator path previously threw (setting `mVibratorError=true`), the old code would skip the vibrator path entirely and (because system-wide touch haptics were enabled) would not re-attempt view haptics either, resulting in “no haptics”.
- Implemented: when system mode is enabled and view haptics fail, NSK will still attempt a safer vibrator fallback (`vibrateFallback(...)` / the user-controlled system-fallback one-shot) even if a previous vibration attempt set `mVibratorError=true`.

10. **Read “haptic feedback enabled” from both `Settings.System` and `Settings.Secure` (low risk)**

- Some Android builds appear to store the global `haptic_feedback_enabled` toggle under `Settings.Secure` instead of `Settings.System`.
- If we only read `Settings.System`, we can incorrectly think system-wide touch haptics are enabled (and prefer view-based haptics that may be suppressed/buggy), leading to “no haptics” even though other keyboards vibrate.
- Implemented: `ImePressEffects` now observes `Settings.System.HAPTIC_FEEDBACK_ENABLED` from both `Settings.System` and `Settings.Secure` and treats `0` in either location as “system-wide touch haptics disabled”.

11. **Treat `VIBRATION_EFFECT_SUPPORT_UNKNOWN` as unsupported for system predefined effects (low risk)**

- Some devices report predefined effect support as `UNKNOWN` and then silently produce no vibration for that effect (especially `EFFECT_TICK`), which makes “system haptics” feel like “no haptics”.
- Implemented: `PressVibratorV29` now only selects predefined effects that are explicitly `VIBRATION_EFFECT_SUPPORT_YES` and otherwise falls back to a duration-based vibration.

12. **Prefer `EFFECT_CLICK` over `EFFECT_TICK` for system key-press effects (low risk)**

- Some devices report `EFFECT_TICK` as supported but it’s extremely subtle (or effectively silent), which makes “system haptics” feel like “no haptics” on regular letter keys.
- Implemented: `PressVibratorV29` now prefers `EFFECT_CLICK` and falls back to `EFFECT_TICK` for key-press system mode.

13. **Ignore the global touch-haptics toggle for view haptics in system mode (low risk)**

- Some OEM builds can suppress `View.performHapticFeedback(...)` when the global touch-haptics toggle is off, even though the user has explicitly enabled keyboard haptics (and other keyboards still vibrate).
- Implemented: in system mode, `performSystemHapticFeedback(...)` now passes `FLAG_IGNORE_GLOBAL_SETTING` in addition to `FLAG_IGNORE_VIEW_SETTING`.

14. **Treat “vibrator service exists” but “no vibrator hardware” as “no vibrator” (low risk)**

- Some devices/emulators expose a `Vibrator` service but report `Vibrator.hasVibrator() == false`. In that state, `Vibrator.vibrate(...)` can be a no-op (no exception), and system mode can return early after doing “nothing”.
- Implemented: system mode now uses `PressVibrator.hasVibrator()` (backed by `Vibrator.hasVibrator()`) when deciding whether the vibrator-based system effects path is viable.

15. **Prefer the legacy vibrator service when the default vibrator reports no hardware (low risk)**

- On some devices/emulators, `VibratorManager#getDefaultVibrator()` can return a non-null `Vibrator` that reports `hasVibrator() == false`, while the legacy `Context.VIBRATOR_SERVICE` vibrator is functional.
- If we always prefer the default vibrator when present, system mode can end up using a “dead” vibrator instance and produce no vibration even though other keyboard apps vibrate.
- Implemented: `ImePressEffects` now prefers the first vibrator instance that reports having hardware (default vibrator first, legacy vibrator as fallback).

16. **Use the dedicated IME vibration usage on API 33+ (medium risk, high potential impact)**

- Newer Android builds split keyboard vibration from generic touch feedback and route keyboard haptics through `VibrationAttributes.USAGE_IME_FEEDBACK`.
- If NSK vibrates using `USAGE_TOUCH`, it can be suppressed by “Touch feedback” settings even when “Keyboard vibration” is enabled (and other keyboards still vibrate).
- Implemented: `PressVibratorV33` now prefers `USAGE_IME_FEEDBACK` (value `0x52` / `82`) and falls back safely when unavailable.

## How to confirm which bucket you’re in (no code changes needed)

1. First, prove whether _any_ non-long-press key vibration works:
   - Turn **off** “Use system vibration”.
   - Set “Vibrate on key press” to something obvious like **20–50ms**.
   - If you still get no haptics, the issue is not “system mode”; it’s either a device/OS suppression or an app-level failure talking to the vibrator service.

2. If non-system vibration works but system mode doesn’t:
   - Check the OS setting backing “touch haptics”: `Settings.System.HAPTIC_FEEDBACK_ENABLED` (what this code reads).
   - On a device with `adb`, you can inspect it:
     - `adb shell settings get system haptic_feedback_enabled`
     - If your device uses different keys, list likely ones: `adb shell settings list system | grep -i haptic`

3. If the mic button “haptics” are the only thing you feel:
   - Check whether you feel the same haptic by long-pressing any letter key (to trigger popups / long-press). If yes, you’re probably only seeing long-press feedback.
