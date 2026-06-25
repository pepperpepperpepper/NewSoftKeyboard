# Keyboard API — `runMacro` (Tier-1 automation) plan

Status: **implemented** (contract + provider re-dispatch + `KeyboardApiMacroHandler` + eligibility
whitelist + unit tests). Design doc for extending the existing programmable
Keyboard API with a batching/sequencing primitive, without crossing into "full scripting"
(input reading / arbitrary text injection).

## Goal

Let a paired, authorized controller app run a **sequence of existing safe actions** in one
`ContentProvider.call()` — e.g. "jump to end of field → switch to symbols layer → insert my
signature snippet" — instead of N separate IPC calls.

## Governing principle (what keeps this Tier-1)

**`runMacro` adds ZERO new capability.** It is a batching primitive only. Every step is re-run
through the *same* `KeyboardApiProvider.dispatch()` path, so each step independently re-checks:

- its own per-method scope (`requireScope`),
- the password-field hard block,
- the high-risk-actions toggle,
- incognito / disallowed-context guards,
- the existing handlers' validation (allowlisted pref keys, snippet-id bounds, etc.).

A macro can therefore do **nothing** a sequence of individual `call()`s could not. It saves
round-trips and adds all-or-stop sequencing — nothing more.

This preserves the contract's core invariant (see `KeyboardApiContract` header comment): never
expose typed/clipboard/secret content, never accept arbitrary caller-supplied text. Macro steps
can only *name existing verbs*; `runSnippet` stays bounded to enabled snippet ids.

## Contract additions (`api/.../KeyboardApiContract.java`)

```
METHOD_RUN_MACRO            = "runMacro"
SCOPE_AUTOMATION_MACRO      = "automation.macro"   // outer user-granted gate
EXTRA_MACRO_STEPS           = "macro_steps"         // request: JSON array (String)
EXTRA_MACRO_STOP_ON_ERROR   = "macro_stop_on_error" // request: bool, default true
EXTRA_MACRO_RESULTS         = "macro_results"       // response: JSON array (String)
MAX_MACRO_STEPS             = 16
ERR_MACRO_TOO_LONG          = 20                    // next free error code
ERR_MACRO_STEP_NOT_ALLOWED  = 21
```

Bump `API_VERSION` 2 → 3.

### Wire format

Request `macro_steps` — JSON array of `{method, args}`; `args` keys are the existing `EXTRA_*`
names for that method:

```json
[
  {"method":"sendNavigationKey","args":{"navigation_key":"end"}},
  {"method":"switchKeyboardMode","args":{"keyboard_mode":"symbols"}},
  {"method":"runSnippet","args":{"snippet_id":"my_signature"}}
]
```

Response `macro_results` — per-step outcome:

```json
{"ok":true,"macro_results":[{"i":0,"ok":true},{"i":1,"ok":true},{"i":2,"ok":true}]}
```

On a failing step: that entry carries `error_code`; if `stop_on_error` (default), remaining steps
are marked `{"i":k,"skipped":true}` and the overall result is an error.

## Provider wiring (`KeyboardApiProvider.java`)

1. `requiredScopeForMethod()`: `METHOD_RUN_MACRO → SCOPE_AUTOMATION_MACRO` (enforced in the same
   auth gauntlet — toggle, user-unlock, allowlist, signature pin, scope, token — as every method).
2. `dispatch()` case:
   ```java
   case METHOD_RUN_MACRO:
     requireScope(authorization, SCOPE_AUTOMATION_MACRO);
     return runMacro(context, authorization, extras);
   ```
3. `runMacro(...)`:
   - parse + validate `macro_steps` (JSON, ≤ `MAX_MACRO_STEPS`, else `ERR_MACRO_TOO_LONG` /
     `ERR_BAD_ARGUMENTS`);
   - **charge the rate limiter for the whole batch up front**: `mRateLimiter.tryAcquire(pkg,
     steps.size())` → `ERR_RATE_LIMITED` if insufficient (no bypass);
   - for each step: reject if `method` not in the eligibility whitelist
     (`ERR_MACRO_STEP_NOT_ALLOWED`); else build a per-step `extras` Bundle from `args` and
     **re-enter `dispatch(ctx, auth, method, null, stepExtras)`** — catching `SecurityException`
     as `ERR_SCOPE_DENIED`;
   - append a **per-step audit entry** `runMacro:<method>` with the step's result code;
   - honor `stop_on_error`; assemble `macro_results`.

The re-dispatch must stay in the provider so it reuses the real guard stack. A
`KeyboardApiMacroHandler` may own JSON parsing / `args→extras` mapping / the whitelist to keep the
provider lean, but not the guard logic.

## Step eligibility whitelist

Allow the side-effecting control verbs; exclude meta/auth/destructive/read methods. (Per-step
scope checks already enforce permission — this is about what's sensible to batch and closing
footguns.)

- **Eligible:** `toggleIncognito`, `switchLanguage`, `switchKeyboardMode`, `sendNavigationKey`,
  `sendTab`, `sendEscape`, `clipboardCopy`, `clipboardCut`, `clipboardPaste`,
  `clipboardSelectAll`, `undo`, `redo`, `runSnippet`, `setPreference`, `setPreferences`,
  `setSessionPreset`, `setSessionThemePreset`, `setSessionKeyboardId`, `clearSessionOverrides`,
  `reloadSettings`.
- **Excluded:** `runMacro` (no nesting), `requestPairing`, `getPairingStatus`, `getAuditLog`,
  `clearAuditLog`, `setSecret`, `getSecretStatus`, all reads (`getKeyboardStatus`,
  `getPreference`, `getCapabilities`, `getApiVersion`, `ping`), `clearLearningData`,
  `clearQuickTextHistory`, `clearClipboardHistory` (destructive — keep deliberate/single),
  `openSettings`, `openMediaInsertionUi` (UI trampolines — keep single so the 2s cooldown can't be
  batch-circumvented).

## Bounds & semantics

- Length ≤ 16; reject longer.
- Cap raw `macro_steps` JSON length (e.g. 8 KB) before parsing.
- No nested `runMacro`.
- `stop_on_error` default true.
- **Non-transactional**: IME actions can't be rolled back; the result array makes partial
  execution visible. Documented explicitly.
- Whole batch charged to the rate limiter (25/s/caller) up front.

## New scopes/actions — summary

| Add | Kind | Notes |
|---|---|---|
| `runMacro` | method | re-dispatches eligible verbs through full guards |
| `automation.macro` | **scope** (only new one) | user-granted gate: "may run macros"; per-step scopes still required |
| `macro_steps` / `macro_results` / `macro_stop_on_error` | extras | bounded JSON |
| `MAX_MACRO_STEPS`, `ERR_MACRO_TOO_LONG`, `ERR_MACRO_STEP_NOT_ALLOWED` | limits | length + eligibility |

Optional, deferred: a `"delay"` pseudo-step (≤100 ms, total ≤500 ms). **Skip initially** — it
blocks the binder thread; keep macros instantaneous.

## Capabilities/UI follow-through

- `KeyboardApiCapabilitiesHandler`: advertise `runMacro` in supported methods and
  `automation.macro` in supported scopes.
- `KeyboardApiPairingHandler.SUPPORTED_GRANTABLE_SCOPES`: add `automation.macro` so it can be
  requested + approved.
- Pairing-approval UI (`ProgrammableApiPairingRequestsUi`): list `automation.macro` as a grantable
  scope with a clear label ("Run action macros").

## Security properties (review checklist)

- No capability inflation — a macro == N ordinary calls; missing scope on step *k* →
  `ERR_SCOPE_DENIED` for that step, never a bypass.
- Invariant intact — steps name existing verbs only; no arbitrary text; no input read.
- Abuse controls preserved — batch rate-limited up front; password-field / high-risk / incognito
  fire per step; **every sub-step audited** (more granular trail, not less).
- Bounded, non-nesting, non-transactional, stop-on-error.

## Test plan (mirror `KeyboardApiProviderTest` patterns)

1. Happy path: 3 eligible steps all succeed; `macro_results` all ok; sub-steps audited.
2. Missing per-step scope: controller holds `automation.macro` but not `ime.inject.snippets`; the
   `runSnippet` step returns `ERR_SCOPE_DENIED`; earlier steps still ran (or stop_on_error halts).
3. Missing outer scope: no `automation.macro` → whole call `ERR_SCOPE_DENIED` before any step.
4. Over-length (>16) → `ERR_MACRO_TOO_LONG`; nothing executed.
5. Non-eligible step (`setSecret`, `runMacro`) → `ERR_MACRO_STEP_NOT_ALLOWED`.
6. Password-field: an injection step inside a macro is blocked exactly as the single call is.
7. Rate limit: a batch exceeding remaining budget → `ERR_RATE_LIMITED`, nothing executed.
8. `stop_on_error=false`: failing middle step doesn't halt; later steps still attempted.
9. Malformed JSON / oversized payload → `ERR_BAD_ARGUMENTS`.

## Explicitly out of scope (would make it Tier-2 / unsafe)

- Any step that reads the input buffer / selection / clipboard *content*.
- Any `commitText(arbitrary)` or caller-supplied text injection.
- An embedded/general script interpreter (Lua/JS) executing caller- or user-supplied code.
- Exposing the editor/target-app package name to callers.
