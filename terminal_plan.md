# Terminal Apps: Predictions Without “Invisible Typing” (Plan)

## Problem statement

In terminal/SSH apps (e.g. ConnectBot), typed characters sometimes do not appear until a “commit”
action (space/enter). This happens when the IME uses **composing text** (`setComposingText(...)`)
but the target app/editor does not render composing updates in its terminal view.

Disabling predictions fixes visibility, but loses the whole point: terminal commands benefit from
suggestions and typo-corrections.

## Goals

- Keep **suggestions/predictions enabled** for terminal apps.
- Ensure **every keystroke is visible immediately** in terminal apps.
- Keep terminal-safe behavior:
  - **No auto-pick / auto-correct commit** by default (manual pick only).
  - Optional, controlled auto-space after manual pick (configurable).
- Avoid breaking non-terminal apps that incorrectly report `TYPE_NULL`.

## Non-goals

- Perfectly “universal” detection of all terminal-like editors without any allowlist/override.
- Aggressive auto-correction in terminals (dangerous for commands).

## Root cause (why this happens)

Our prediction path prefers composing text so the IME can rewrite the in-progress word.
Terminal-style editors often:

- Accept `setComposingText(...)` (return `true`)
- And may even return the composing text from readback APIs
- But **do not paint** that composing region in the terminal UI until a later commit

So the IME thinks typing worked; the user sees “nothing until space”.

## Best-of-both-worlds approach

### Key idea: decouple _prediction_ from _output method_

Keep building the current word and generating suggestions, but **do not use composing output** in
terminal apps. Instead:

- Output each character as **committed** (key events / commitText), so the terminal shows it live.
- Keep the suggestion strip updating based on the internal `WordComposer`.
- When the user taps a suggestion, perform a **terminal-style replacement**:
  - send backspace (DEL) N times for the typed word
  - then output the chosen suggestion characters
  - optionally output a trailing space (opt-in)

This preserves “correct terminal commands” while eliminating invisible typing.

## Implementation plan

### 1) Add an explicit editor output mode (session-scoped)

Introduce a small, session-scoped flag or enum (e.g. `EditorOutputMode`):

- `STANDARD_COMPOSING` (default): composing text allowed
- `NO_COMPOSING_PREDICTION`: predictions ON, but output uses committed characters

Where it lives:

- Prefer `ImeSessionState` (because it’s editor-session policy), or `PredictionState` if it’s
  already the central place for per-editor behavior.

### 2) Decide output mode at `onStartInputView`

Set `NO_COMPOSING_PREDICTION` when terminal emulation is detected:

- `TerminalKeySender.isTerminalEmulation(editorInfo)` (package allowlist + inputClass == 0)

Add a user override (later):

- Per-app toggle: “Terminal mode output (no composing)” to support more terminal apps without
  growing an allowlist forever.

### 3) Character input: keep predicting, change output path

Update `CharacterInputHandler` so that when in `NO_COMPOSING_PREDICTION`:

- Never calls `setComposingText(...)`
- Always outputs characters via `sendKeyChar(...)` / `commitText(...)`
- Still calls `postUpdateSuggestions()` when appropriate

This ensures per-keystroke visibility in terminals.

### 4) Backspace/delete: don’t rely on deleteSurroundingText in terminal mode

Update `DeleteActionHelper` so that in `NO_COMPOSING_PREDICTION`:

- Update `WordComposer` state (so suggestions track deletions)
- Prefer emitting `KEYCODE_DEL` key events for deletion (more terminal-compatible)
- Only fall back to `deleteSurroundingText(...)` if key events fail (if we can detect failure)

### 5) Manual suggestion pick: terminal-style replacement

Update `SuggestionCommitter` so that in `NO_COMPOSING_PREDICTION`:

- Replace typed word by sending `KEYCODE_DEL` the correct number of times
  - Use codepoint count (not `String.length`) to handle emoji/surrogates safely.
- Output the picked suggestion as committed characters
- Optional: insert a trailing space after a manual pick (configurable; default OFF for safety)

This gives “terminal command correction” without composing.

### 6) Separator handling and state reset

Ensure separator commits (space/enter/punctuation) correctly:

- Reset `WordComposer` / prediction state as today
- Keep suggestion strip behavior consistent (possibly allow next-word suggestions as a separate,
  opt-in terminal setting)

### 7) Tests

Add focused unit tests (Robolectric where needed):

- `CharacterInputHandler`:
  - terminal mode → does not call `setComposingText(...)`
  - terminal mode → calls `sendKeyChar(...)` for each input
- `DeleteActionHelper`:
  - terminal mode → uses `KEYCODE_DEL` path and updates `WordComposer`
- `SuggestionCommitter`:
  - terminal mode → sends N deletes then emits committed suggestion characters

These tests should not require new dependencies.

### 8) Rollout and verification

Manual checks (device):

- ConnectBot: characters visible immediately; suggestion strip updates; manual pick replaces word.
- JuiceSSH (if applicable): same behavior.
- Normal apps (SMS/browser/search): composing behavior unchanged.

Release:

- bump version
- publish to F-Droid
- validate on at least one terminal app + one normal text app

## Open questions / decisions

- Should we default to **no auto-space** in terminals even after manual picks? (Recommended: yes.)
- Should next-word suggestions be enabled in terminals? (Maybe optional; commands often don’t want
  next-word “English” predictions.)
- How should we expose the per-app override UI (settings page vs a strip action)?
