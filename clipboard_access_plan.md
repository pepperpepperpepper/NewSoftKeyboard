# Clipboard picker — access redesign plan

Status: **planned, not implemented.** Design doc for how the user *reaches* the clipboard
history picker. The picker itself (`ImeClipboard.showAllClipboardEntries` → `AlertDialog` +
`ClipboardEntriesAdapter`) is unchanged; only the doorway changes.

## Problem

The single strip chip (`ClipboardStripActionProvider` + `clipboard_suggestion_action.xml`) is
overloaded as both a "just copied" preview and the entrance to history. Three concrete flaws:

1. **One target, two meanings, hidden primary.** Single tap = paste-latest (destructive — text
   lands in the field immediately); long-press = open picker. The discoverable gesture does the
   risky thing; the *invisible* `setOnLongClickListener` is the real "browse history" door, so
   nobody finds it.
2. **Doorway gated on recency, not intent.** `shouldShowClipboardActionIcon()` only shows the chip
   inside the `MAX_TIME_TO_SHOW_SYNCED_CLIPBOARD_HINT` window after a copy, and `onKey()` removes it
   as soon as the user starts typing (default mode). Exactly when someone wants older history, the
   access point has already auto-hidden.
3. **Reliability hidden behind a setting.** Becoming persistent requires the
   `settings_key_clipboard_action_always_visible` toggle, which most users never find. The chip also
   monopolizes the single strip slot with preview text.

## Goal

Make the access point **persistent (present whenever history exists), predictable (always the same
single action: open the picker), and non-destructive by default**, without adding any vertical
screen space — it stays in the existing `candidate_strip_height` action-strip band.

## Approach: split "just-copied hint" from "open clipboard"

Decouple the two jobs the one chip currently does.

### A. Persistent clipboard icon (the doorway)

- A small **icon** strip action (reuse `ic_clipboard_paste_in_app`, the picker's own icon), pinned
  at the fixed end of the action strip.
- Shown whenever there is anything to browse — i.e. `!mClipboard.isOsClipboardEmpty() ||
  mClipboard.getClipboardEntriesCount() > 0` — **independent of the recency timer**, and **not**
  cleared by `onKey()`.
- **Single tap → `showAllClipboardOptions()`** (open the picker). One meaning. The safe action is now
  the primary, discoverable one; the icon is tiny (no preview text eating strip width).

### B. Transient "Copied: …" hint (paste-latest)

- The existing preview-text behavior stays, but as a **separate, clearly time-boxed affordance**: it
  flashes after a copy, **tap pastes the latest entry**, and it auto-hides on the existing
  `MAX_TIME_TO_SHOW_SYNCED_CLIPBOARD_ENTRY` / `..._HINT` timers and on first keypress.
- It is **no longer the doorway to history**. Paste-latest and browse-history stop sharing a target
  and stop relying on a hidden long-press.
- The hint is **user-suppressible** via the repurposed
  `settings_key_clipboard_action_always_visible` toggle (see Settings below): off → no preview ever,
  while the icon doorway stays.

### C. Retire the overloaded gesture

- Remove (or demote to a redundant shortcut) the long-press-to-browse on the hint. "Open picker" is
  promoted to a normal tap on the dedicated icon (A).

### D. Optional typing-time route

- Surface the existing `KeyCodes.CLIPBOARD_PASTE_POPUP (-133)` as a real labeled key reachable from
  the symbols layout, so the picker is reachable mid-sentence without depending on the strip state.
  (Layout-only; the keycode already routes to `showAllClipboardEntries` in
  `handleClipboardOperation`.) Can ship in a follow-up.

## Resulting model

| Element | When shown | Gesture | Action |
|---|---|---|---|
| Clipboard icon (new) | whenever history/OS-clip non-empty | tap | open picker |
| "Copied: …" hint (existing text) | transient, post-copy | tap | paste latest |
| Symbols-row clipboard key (optional, D) | always in symbols layout | tap | open picker |

The icon is the stable door; the hint is a convenience that comes and goes.

## Code touch points (for the implementation pass — not done yet)

- **`ClipboardStripActionProvider`** — today renders one preview `TextView` and overloads tap/long
  -press. Split into (or add) a lightweight **icon action** whose `setOnClickListener` calls
  `owner.showAllClipboardOptions()`. Keep the preview `TextView` path as the transient hint with
  tap = `owner.outputClipboardText()`. The `ClipboardActionOwner` interface already exposes both
  `outputClipboardText()` and `showAllClipboardOptions()`, so no new owner methods are needed.
- **`ImeClipboard`**:
  - `shouldShowClipboardActionIcon()` — gate the **icon** on "history exists" rather than the
    recency window; keep the recency logic only for the transient hint.
  - `updateClipboardActionIconVisibility()` — add the persistent icon action; drive the hint
    text separately.
  - `onKey()` — stop removing the *icon* on first keypress; keep hiding only the hint text.
  - `onClipboardEntryChanged()` / auto-hide timers (`scheduleClipboardActionAutoHideIfNeeded`,
    `scheduleClipboardTextAutoHide`) — apply only to the hint, not the icon.
- **Settings** — the persistent icon is now always the doorway (no setting gates it; it shows
  whenever history exists). **`settings_key_clipboard_action_always_visible` is repurposed to
  control the transient "Copied: …" hint**: when on (default), the hint flashes after a copy as
  today; when off, the hint is suppressed entirely (paste-latest preview never shows) while the icon
  remains. This keeps an escape hatch for users who dislike the preview without sacrificing the
  doorway. Rename the user-facing string accordingly (e.g. "Show copied-text preview"); the pref
  *key* stays the same to avoid migrating stored values. `settings_key_os_clipboard_sync` remains
  the master on/off for the whole feature (icon + hint).
- **Layout/res** — the icon can reuse `clipboard_suggestion_action.xml`'s container with an
  `ImageView` variant, or a new minimal `clipboard_action_icon.xml`. Optional (D): add the
  clipboard key to the symbols layout XML + a label/popup string.

## Out of scope (separate efforts)

- The picker's own visuals/layout (`clipboard_dialog_entry.xml`, dialog → panel) — explicitly
  unchanged here.
- Pinned vs. history entries and clipboard persistence across restarts — orthogonal; can layer on
  later.

## Test plan

- Icon is present when `getClipboardEntriesCount() > 0` even after typing several keys (regression
  against the `onKey()` auto-remove).
- Icon is absent when history is empty and OS clip is empty.
- Tap on icon opens the picker (`showAllClipboardOptions`), never pastes.
- Transient hint still appears after a copy, tap pastes latest, and auto-hides on timer + first
  keypress.
- Password fields still suppress preview text (`isTextPassword` / `isNumberPassword`) — icon may
  still open the picker, but previews stay hidden.
- `settings_key_os_clipboard_sync = false` hides everything (icon + hint).
- Repurposed toggle: with `settings_key_clipboard_action_always_visible = false`, a copy shows **no**
  preview hint, but the icon doorway is still present and still opens the picker; with it `true`
  (default), the hint flashes after a copy as before.
