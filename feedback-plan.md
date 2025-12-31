# UX Feedback TODO (Living) — Voice STT + Photo Theme

_Last updated: 2025-12-31_

This is a **living TODO/spec** for user-facing feedback issues (voice STT UI, theme/photo wallpaper, etc).
When we fix or implement something, update:

- Status checkbox
- The concrete files touched
- The verification steps (and tests, if added)

## 0) Current Priority TODOs

### A) Keyboard wallpaper / photo theme (user-selected “background photo”)

**Symptom:** User selects a photo wallpaper in settings, but the keyboard background remains unchanged (reported as “photo invisible” on Mike Rozoff add-on theme).

- [ ] Repro (manual)
  - Pick Mike Rozoff theme
  - Settings → Look & Feel → Theme tweaks → Keyboard wallpaper customization
  - Select a photo + apply
  - Expected: photo becomes the IME background behind the keyboard (and/or key face overlay, depending on mode)
  - Actual: background doesn’t change (photo appears “invisible”)
- [x] Root cause (code)
  - `ImeThemeOverlay.applyKeyboardWallpaper` was applying only theme wallpaper/fallback and **ignoring user photo overrides**
    by calling `KeyboardWallpaperResolver.resolveThemeWallpaperOrFallback(...)`.
- [x] Fix (UX default)
  - Default the wallpaper mode to `Key texture` on first import (and when a wallpaper exists without an explicit mode) so opaque themes don’t make the feature appear “broken”.
  - Files:
    - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/theme/KeyboardWallpaperOverrideStore.java`
    - `ime/app/src/test/java/wtf/uhoh/newsoftkeyboard/app/theme/KeyboardWallpaperOverrideStoreTest.java` (`testImportFromUriSetsVisibleDefaultMode`, `testWallpaperModeDefaultsToVisibleOptionWhenWallpaperExists`)
- [x] Fix (code)
  - Use `KeyboardWallpaperResolver.resolveImeWallpaper(...)` so photo overrides apply at the IME container level.
  - Files:
    - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeThemeOverlay.java`
- [ ] Verify (add-on compatibility)
  - Confirm Mike Rozoff theme shows the photo background.
  - Confirm “mode” semantics:
    - `Background only`: photo visible behind keys (may be subtle for fully-opaque key backgrounds).
    - `Key tint` / `Key texture`: photo visible on key faces even for opaque key backgrounds.
- [x] Add regression test (Robolectric)
  - Add a small test that fails if `ImeThemeOverlay` applies `resolveThemeWallpaperOrFallback` instead of `resolveImeWallpaper`.
  - Files:
    - `ime/app/src/test/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeServiceThemeOverlayTest.java` (`testAppliesImeWallpaperResolverSoPhotoOverridesCanWork`)

### B) Voice STT visual feedback (Third‑Party STT)

**Goal:** predictable, theme-safe feedback while recording/transcribing, with actionable errors (Retry/Save/Discard) and no “flicker” state transitions.

- [x] Keep spacebar voice status labels plain text (avoid emoji font-size inconsistencies).
  - Files:
    - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/VoiceStatusRenderer.java`
    - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/KeyDrawHelper.java`
- [x] Replace “spacebar label mutation” with a badge overlay (Option 2) to preserve add-on spacebar icons/labels.
  - Files:
    - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/InputViewBinder.java`
    - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/KeyboardViewBase.java`
    - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/KeyboardDrawCoordinator.java`
    - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/DrawInputsBuilder.java`
    - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/DrawInputs.java`
    - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/KeyDrawHelper.java`
    - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/VoiceStatusBadgeState.java`
    - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/VoiceStatusRenderer.java`
    - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/VoiceUiHelper.java`
    - `ime/app/src/main/res/values/strings.xml`
    - `ime/app/src/test/java/wtf/uhoh/newsoftkeyboard/app/ime/VoiceStatusRendererTest.java`
- [x] Make voice state monotonic: `RECORDING → WAITING → (IDLE|ERROR)` (no intermediate `IDLE` flicker).
  - Implemented in `VoiceImeController` (single source of truth); `VoiceUiHelper` no longer invents state.
  - Files:
    - `ime/voiceime/src/main/java/com/google/android/voiceime/VoiceImeController.java`
    - `ime/voiceime/src/main/java/com/google/android/voiceime/VoiceRecognitionTrigger.java`
    - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/VoiceUiHelper.java`
    - `ime/voiceime/src/test/java/com/google/android/voiceime/VoiceImeControllerTest.java`
- [x] Replace modal dialog errors with an IME-attached banner/strip action (Retry/Save/Discard), dialog as fallback only.
  - Files:
    - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/hosts/ImeVoiceInputCallbacks.java`
    - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/hosts/VoiceErrorStripActionProvider.java`
    - `ime/app/src/main/res/layout/voice_error_strip_action.xml`
    - `ime/app/src/main/res/values/strings.xml`
    - `ime/app/src/test/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeVoiceInputCallbacksTest.java`
- [x] Remove direct toasts from `ThirdPartySpeechTrigger`; route errors to host for consistent UI ownership.
  - Files:
    - `ime/voiceime/src/main/java/com/google/android/voiceime/ThirdPartySpeechTrigger.java`
    - `api/src/main/res/values/strings.xml` (generic voice error strings used by voiceime)
- [x] Localize user-visible voice error strings.
  - Status: show a localized short message by default; in debug builds we append a compact raw error detail.
  - Files:
    - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/hosts/ImeVoiceInputCallbacks.java`

---

## Voice STT Visual Feedback (Third‑Party STT) — UX + Engineering Plan (Reference)

_Note: most of this plan has already been implemented. Keep this section as design rationale; remaining work is tracked in §0._

## 0) Problem Statement

In third‑party speech‑to‑text (STT) mode, the IME previously gave **inconsistent visual feedback**:

- Spacebar text (“Recording / Waiting / Error”) could look wrong (size changes per state) and could break themed/icon spacebar designs.
- Error feedback was modal (AlertDialog) and sometimes redundant with backend toasts.
- State updates could flicker (`IDLE → WAITING`, `IDLE → ERROR`) due to callback ordering.

We need a feedback system that is:

- **Predictable** (stable state machine; no flicker).
- **Theme/add‑on compatible** (we adapt to add‑ons like Mike Rozoff’s package; we do not require add‑on changes).
- **Actionable** (errors provide Retry/Save/Discard without losing audio).
- **Extensible** (can add progress, time remaining, backend name later without rewiring everything).

This document is a **design + engineering spec**: it should be implementable, testable, and not create “helper sprawl”.

## 1) Constraints / Non‑Goals

### Constraints (non‑negotiable)

- **AnySoftKeyboard add‑on compatibility**: add‑ons control their own key labels/icons/layouts; NSK must not accidentally override them.
- Must work across keyboard variants: base keyboard, mode variants, popups/mini‑keyboards, and sub‑keyboards.
- No new runtime 3rd‑party dependencies.

### Non‑goals (for this project slice)

- Replacing the third‑party STT backend system.
- Adding “live transcription” / streaming partial text (we can extend later).
- Adding a new settings surface (unless we discover a strong need).

## 2) Glossary (because “spacebar label” is overloaded)

- **Key**: a rendered keyboard button (a `Keyboard.Key`/`KeyboardKey`) with geometry, codes, and optional label/icon.
- **Key model mutation**: changing fields on a `Keyboard.Key` instance (e.g., `key.label = "Recording"`). This persists across frames until the keyboard is rebuilt or the mutation is reverted.
- **Space key label**: `Keyboard.Key.label` for the space key (may be empty/null).
- **Keyboard name on spacebar**: a fallback label substituted at draw time when the space key label is empty _and_ “show keyboard name” is enabled.
- **Themed key‑code icon**: a drawable picked by key code (only used when the label is empty).
- **Per‑key icon**: an icon set directly on the key from the XML layout; it always wins.
- **Actions strip**: the IME’s built‑in “overlay area” above the main keyboard where lightweight actions (chips/buttons) can be shown without blocking typing (`KeyboardViewContainerView`).
- **Voice UI states**: user‑perceived states: `IDLE`, `RECORDING`, `WAITING`, `ERROR`.

## 3) Source Map (ground truth in code)

### Voice pipeline (state + callbacks)

- Trigger selection: `ime/voiceime/src/main/java/com/google/android/voiceime/VoiceRecognitionTrigger.java`
  - If third‑party backend configured → `ThirdPartySpeechTrigger`.
- Third‑party trigger: `ime/voiceime/src/main/java/com/google/android/voiceime/ThirdPartySpeechTrigger.java`
  - Owns audio file lifecycle, retry/save/discard.
  - Emits callbacks in a specific order (see §4.2).
- UI‑agnostic controller: `ime/voiceime/src/main/java/com/google/android/voiceime/VoiceImeController.java`
  - Routes trigger callbacks to host callbacks on main thread.
  - Currently emits intermediate `IDLE` states on “stop recording” and on “transcription finished/error”.

### IME host (where UI is updated)

- Wiring: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeServiceBase.java`
  - `voiceImeController.attachCallbacks()` in `onCreate()`.
  - Host callbacks implemented by `ImeVoiceInputCallbacks`.
- UI glue: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/VoiceUiHelper.java`
- Spacebar/voice‑key updates: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/VoiceStatusRenderer.java`
  - Current: updates voice-key state and a draw-time spacebar badge (no key-model mutation).
- Error surface: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/hosts/ImeVoiceInputCallbacks.java`
  - Current: shows an IME-attached strip action with Retry/Save/Discard (dialog fallback only).

### IME UI surfaces (where “non‑modal” can actually live)

The IME already has a safe, compositor‑agnostic place for lightweight, actionable UI:

- Container: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/KeyboardViewContainerView.java`
  - Supports “strip actions” via `addStripAction(...)` (views laid over the candidates row).
  - This is a good fit for voice error actions (Retry/Save/Discard) without a modal dialog.

### Keyboard rendering (why the “spacebar label” is high-risk)

Rendering order (critical):

1. Base label is derived from `key.label` and adjusted (`KeyLabelAdjuster`).
2. Icon precedence: `KeyIconDrawer.drawIconIfNeeded(...)`:
   - Per‑key icon overrides labels.
   - Key‑code icons only draw when label is empty.
   - If an icon draws, the label becomes `null` (so keyboard name substitution won’t run).
3. Keyboard name substitution: `KeyboardNameRenderer.applyKeyboardNameIfNeeded(...)`:
   - Only substitutes keyboard name if the space key label is empty.
4. Label draw: `KeyLabelRenderer.drawLabel(...)`:
   - Uses “keyboard name” paint rules on **every space key label** today (not only the keyboard-name fallback).
   - Emoji heuristic: `EmojiUtils.isLabelOfEmoji(label)` → multiplies text size by `1.35`.

References:

- `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/KeyDrawHelper.java`
- `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/KeyIconDrawer.java`
- `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/KeyboardNameRenderer.java`
- `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/KeyLabelRenderer.java`
- `ime/base/src/main/java/wtf/uhoh/newsoftkeyboard/utils/EmojiUtils.java`

## 4) Previously Broken / Root Causes (status)

### 4.1) Spacebar label mutation broke add‑on intent (fixed)

Previously, `VoiceStatusRenderer` replaced `key.label` on the space key with transient status strings (`"Recording" / "Waiting" / "Error"`).

This caused:

- **Theme regression risk**: if a theme uses a spacebar key‑code icon (only shown when label empty), then replacing the label disables that icon for the duration of voice input.
- **Keyboard-name regression risk**: keyboard-name substitution only triggers when the label is empty; replacing it suppresses the fallback.
- **Typography inconsistency**: the spacebar label must fit in the key; longer words (e.g., “Recording”) can shrink more than shorter labels (“Waiting”), so font size changes across states.
  - This is amplified by current draw rules: `KeyLabelRenderer` uses the **keyboard-name paint style for every space key label**, not only the “keyboard name” fallback, so we’re effectively asking the “spacebar name renderer” to also render transient status strings.

Fixed by: switching to a draw-time badge overlay (`KeyDrawHelper` + `VoiceStatusBadgeState`) driven by `InputViewBinder.setVoiceInputState` (no key-model mutation).

### 4.2) State flicker from callback ordering (fixed)

Actual callback order in `ThirdPartySpeechTrigger`:

- On recording stop (success): `notifyRecordingStateChanged(false)` → `notifyRecordingEnded()` → transcription starts.
- On transcription error: `notifyTranscriptionStateChanged(false)` → `notifyTranscriptionError(error)`.

Current `VoiceImeController` mapping:

- recording=false (when previous state was RECORDING) → sets `WAITING`
- transcribing=false → no-op (wait for `textWritten` or `error`)
- textWritten → sets `IDLE`
- error callback → sets `ERROR`

Net result:

- Stop recording success: `RECORDING → WAITING → IDLE` (no intermediate `IDLE` flicker).
- Transcription error: `WAITING → ERROR` (no intermediate `IDLE` flicker).

State ownership is now single-source in `VoiceImeController`; `VoiceUiHelper` is a pure consumer of `VoiceInputState` updates.

### 4.3) Restore bookkeeping (fixed)

The old label-mutation path stored original labels keyed by `keyboard.getKeyboardId()`, which is not guaranteed unique per keyboard instance/variant.
This is no longer relevant because the badge overlay path does not store/restore labels at all.

### 4.4) Error UI is split across layers (toast + dialog) (fixed)

Previously, `ThirdPartySpeechTrigger` showed toasts for some local errors (`showError(...)`), while host code (`ImeVoiceInputCallbacks`) showed dialogs for transcription errors.

Fixed by: routing all errors to the host and presenting them as an IME-attached strip action (Retry/Save/Discard), with dialog fallback only.

## 5) Target UX (channels and responsibilities)

We want three feedback channels, each with one clear job:

1. **Spacebar = passive status indicator**
   - Always visible; never blocks typing.
   - Must not replace base label/icon/name.
2. **Voice key = control + confirmation**
   - Shows “recording” immediately.
   - Shows “waiting/transcribing” (busy) distinctly from idle.
3. **Error surface = action**
   - Should be non‑modal by default (banner/sheet in the IME).
   - Dialog is fallback only when we can’t safely attach a non‑modal surface.

## 6) Proposed Design (decision + alternatives)

### Recommended design: Option 2 (spacebar status badge overlay)

Keep the spacebar’s existing visual semantics (add‑on label / keyboard name / themed icon), and render a small status badge overlay that we fully own.

Why this is the “safe default”:

- Preserves add‑on/theme intent (Mike Rozoff’s keyboards should look exactly as authored).
- Avoids label/icon/name precedence conflicts.
- Badge typography can be fixed, consistent across all states, and independent from label-fit.

Where it fits in the draw pipeline (no model mutation):

- The badge draws **after** background + base icon/label/name are resolved (inside `KeyDrawHelper`), using only:
  - the key bounds/padding,
  - theme colors/sizes,
  - and the current `VoiceInputState` snapshot.
- This keeps ownership clear: voice state belongs to the IME/controller, and drawing belongs to the rendering layer.

### Alternative: Option 1 (spacebar label replacement)

This is the current behavior (`VoiceStatusRenderer` mutates spacebar label).
It can be made “less bad” with safer restoration bookkeeping, but it still inherently fights icons/name fallback and label sizing.

Plan: treat Option 1 as a temporary stopgap; migrate to Option 2.

## 7) UX Spec (exact UI behavior per state)

### 7.0) State matrix (single-glance)

| State       | Spacebar         | Voice key             | Error surface                     | Notes                                                                  |
| ----------- | ---------------- | --------------------- | --------------------------------- | ---------------------------------------------------------------------- |
| `IDLE`      | no badge         | normal                | none                              | Base spacebar visuals (label/icon/keyboard-name) are untouched.        |
| `RECORDING` | `REC` badge      | active/checked        | none                              | Badge is short (icon/3 letters) so it never triggers label-fit shrink. |
| `WAITING`   | `WAIT`/`…` badge | busy state            | none                              | “Waiting” is transcribing; no text is committed yet.                   |
| `ERROR`     | `ERR`/`!` badge  | error mark (optional) | action strip (Retry/Save/Discard) | Keep pending audio until user discards; avoid repeated dialogs.        |

### 7.1) Spacebar badge spec

- **Placement:** top‑right or mid‑right corner of the space key, inside key padding, not affecting the base label centering.
- **Content (no emoji):**
  - `RECORDING`: `REC` (or a tiny mic glyph we own, not emoji).
  - `WAITING`: `…` or `WAIT`.
  - `ERROR`: `ERR` or `!` (plus optional subtle flash).
- **Shape:** rounded “pill” background behind badge text/icon.
- **Color:** derived from theme text color; background derived from key background (slightly tinted) to maintain contrast.
- **Sizing:** fixed relative to theme text size (e.g., `keyboardNameTextSize * 0.6`) and never changes by label-fit logic.
- **Overflow rules:** if space key is too narrow (split layouts / one‑hand mode), badge can:
  - shrink down to a minimum, then
  - hide the text and show only a dot, then
  - hide entirely as last resort (but voice key should still reflect status).

### 7.2) Voice key visual spec

Current: voice key only reflects “recording” (`VoiceKey.voiceActive`).
Target:

- `RECORDING`: active/checked state (current).
- `WAITING`: busy state (distinct visual; could be “checked + not pressed” or a dedicated busy drawable state).
- `ERROR`: optional small error mark on the voice key until dismissed.

### 7.3) Error surface spec (action first, not noise)

When transcription fails:

- Prefer a **non‑modal action strip** (inside the IME):
  - Use `KeyboardViewContainerView.addStripAction(...)` to show a lightweight banner/chip row.
  - Content: short title (“Voice failed”), optional detail on tap/expand, and actions: `Retry`, `Save recording`, `Discard`.
  - This keeps the UI inside the IME window and avoids disruptive dialogs while typing.
- Keep `AlertDialog` as a fallback only when the input view container is unavailable.

Error taxonomy (so the UX stays coherent as we add cases):

- **Permission/config errors** (mic permission missing, backend not configured):
  - Surface: action strip (not a toast)
  - Actions: `Grant permission` / `Open settings` (instead of `Retry`)
- **Recording errors** (recorder failed, storage issues):
  - Surface: action strip
  - Actions: `Try again` (starts a new recording), `Dismiss`
- **Transcription errors** (network/backend failure):
  - Surface: action strip
  - Actions: `Retry`, `Save recording`, `Discard`
- **Commit-to-app errors** (can’t insert into target app / no input connection):
  - Surface: action strip
  - Actions: `Copy text`, `Retry insert`, `Save recording`, `Discard` (future improvement)

Error text hygiene:

- Do not dump raw backend exceptions into UI.
- Strip obvious noise (stack traces / HTTP dumps), clamp to a sane length, and keep a “Details” affordance if needed.

Key behavioral guarantees:

- `Retry` must not delete pending audio.
- `Save recording` must not delete pending audio.
- `Discard` deletes pending audio and clears the error state.

### 7.4) Accessibility spec

- Announce state transitions (recording started, transcribing, error).
- Do not rely only on color; badge text/icon conveys meaning.

## 8) Engineering Plan (phased, ownership‑driven)

### Phase 1 — Fix correctness first (state machine + restoration safety)

1. Make `VoiceImeController` emit a monotonic user‑perceived state:
   - Target: `RECORDING → WAITING → (IDLE|ERROR)`
   - Remove intermediate `IDLE` emissions caused by `recording=false` and `transcribing=false` callbacks when we already know the next state.
   - Make `VoiceImeController` the **single source of truth** for `VoiceInputState` (remove any additional state forcing/interpretation from `VoiceUiHelper`).
   - Status: Done.
   - Files:
     - `ime/voiceime/src/main/java/com/google/android/voiceime/VoiceImeController.java`
     - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/VoiceUiHelper.java`
     - `ime/voiceime/src/main/java/com/google/android/voiceime/VoiceRecognitionTrigger.java`
2. Eliminate “restore by keyboardId” risk:
   - If any label mutation remains, key restoration by keyboard instance identity (e.g., `WeakHashMap<KeyboardDefinition, …>`), not by `keyboardId`.
3. Localize strings:
   - Move hardcoded UI strings in `VoiceStatusRenderer` and `ImeVoiceInputCallbacks` to resources.
4. Consolidate error UI ownership:
   - `ime/voiceime` should not show toasts; it should report errors to the host and let host decide UI.
   - Concretely: turn `ThirdPartySpeechTrigger.showError(...)` into a callback that reaches `HostCallbacks.onVoiceError(...)` (with a “fatal/nonfatal” code), then remove direct toasts.

### Phase 2 — Implement Option 2 rendering (badge overlay)

1. Add a dedicated render path for the spacebar badge:
   - Owned by the keyboard rendering layer (so it does not mutate key models).
   - Drawn after the base label/icon/name has been resolved.
   - Suggested implementation shape:
     - New small renderer (e.g., `SpacebarVoiceBadgeRenderer`) that takes `(Canvas, KeyboardKey, Rect padding, VoiceInputState, Theme snapshot)` and draws the badge.
2. Extend voice key visuals to represent WAITING (and optionally ERROR).

### Phase 3 — Error UX (banner first, dialog fallback)

1. Implement IME‑attached banner (and keep dialog fallback).
2. Ensure Retry/Save/Discard correctly affect pending audio and state.
3. Ensure the error surface is mutually exclusive with other “strip” content:
   - If inline suggestions/autofill strip is visible, voice errors should either:
     - temporarily take priority (and restore previous strip afterward), or
     - coexist without overlapping (explicit layout choice).

## 9) Tests / Verification (prove it works)

### Host-side unit tests (fast)

- State machine tests for `VoiceImeController` (simulate callback order):
  - stop recording success should not emit `IDLE` before `WAITING`
  - transcription error should not emit `IDLE` before `ERROR`
  - Implemented in: `ime/voiceime/src/test/java/com/google/android/voiceime/VoiceImeControllerTest.java`
- Restoration bookkeeping tests (if label mutation remains anywhere):
  - keyboard switching/popups do not cross‑restore labels.

### Instrumentation tests (Genymotion + one real device)

- Record → stop → waiting → error flow asserts that UI state changes are stable and visible.
- Add-on regression check:
  - install Mike Rozoff add-on
  - confirm spacebar icon/label remains as authored while badge overlays state.

### Manual smoke checklist (must-run before release)

- Start/stop voice quickly (stress) and verify no flicker.
- Switch keyboards/sub‑keyboards while recording; verify no stale badge/label leaks.
- Force network failure; verify Retry/Save/Discard do what they say.
- Verify “spacebar name” behavior remains unchanged:
  - if an add‑on/theme previously showed a keyboard name or an icon on the spacebar, it still does.
  - voice badge overlays without shifting centering or changing base typography.

## 10) Acceptance Criteria (pass/fail)

- Spacebar feedback is visible and stable (no flicker/jitter).
- Add‑on/theme spacebar visuals are preserved (icons/name/labels remain intact).
- Badge typography is consistent across all voice states (no resizing artifacts).
- Errors provide `Retry`, `Save recording`, `Discard` and never lose audio unless user discards.
- All user-visible strings are localized (no hardcoded English).

## 11) Ownership Boundaries (to prevent helper sprawl)

- `ime/voiceime` owns: audio lifecycle, backend integration, retry/save/discard semantics.
- `VoiceImeController` owns: the host-facing **state machine** (stable, monotonic).
- `ImeServiceBase` + `VoiceUiHelper` own: “which keyboard/view is current” and when to request redraw.
- Keyboard rendering layer owns: how the badge is drawn (no model mutation).
- `ImeVoiceInputCallbacks` (or a dedicated banner controller) owns: error UI surface selection (banner vs dialog fallback).
  - `VoiceUiHelper` should be a pure consumer of state: it must not invent state transitions of its own.
