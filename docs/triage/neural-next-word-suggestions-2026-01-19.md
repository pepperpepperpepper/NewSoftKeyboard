# Neural next-word suggestions triage (2026-01-19)

## Symptom summary

In **Next-Word Suggestions → Engine: Neural (DistilGPT-2)**, next-word UX looks broken/unhelpful:

- Next-word suggestions can stay **empty** after committing a word and pressing **space**, and only “wake up” after the user types the **first letter** of the next word.
- Some neural candidates look like **token fragments** (example: `com` after typing `keep me `), not full words.
- Candidates can look **context-agnostic** (example: after `keep me i` the suggestions resemble generic “i/in/is/it/if…” completions instead of something like `informed`).
- Depending on settings, picking a suggestion may not insert a trailing **space**, making fluent “tap suggestion → continue typing” awkward.

This is user-visible and makes the neural engine feel “random” or “not working”.

## Repro (field report)

1. Enable next-word suggestions.
2. Set next-word engine to **Neural** and pick a DistilGPT-2 model.
3. Try to type: `keep me informed`
   - Type `ke` → pick suggestion `keep`.
   - Press space.
   - Type `m` → pick suggestion `me` (space may not be added).
   - After `keep me ` observe suggestion `com` (partial token).
   - After typing `keep me i` observe generic suggestions like `i/in/is/it/if…`.

## Likely contributing issues (code-level)

### 1) Next-suggestions get cleared on “manual space after manual pick”

When the user **manually picks** a suggestion (committing a word) and **auto-space is disabled**, the next key the user presses is often a **space**. At that moment the current `WordComposer` is usually empty, so `SeparatorActionHelper.Result.wordForNextSuggestions` becomes empty too.

`SeparatorHandler` then calls `Suggest.getNextSuggestions("")`, which returns an empty list and can override the next-word suggestions that were just computed after the pick.

Net effect: the suggestions strip can go empty after space, and only show something again once the user starts typing the next word.

#### Code pointers

- `ime/app/.../SuggestionPicker.java`
  - Manual pick commits the suggestion and (optionally) inserts a space.
  - When auto-space is disabled, no separator flow runs.
- `ime/app/.../SeparatorHandler.java`
  - On a user-pressed space, it asks for next suggestions using `result.wordForNextSuggestions`.
- `ime/app/.../SeparatorActionHelper.java`
  - When `typedWord` is empty, `wordForNextSuggestions` is empty.
- `ime/app/.../SuggestImpl.java`
  - `getNextSuggestions("")` returns an empty list and overwrites the currently displayed next-words.

#### Status

- Implemented mitigation: when `wordForNextSuggestions` is empty, `SeparatorHandler` now falls back to the IME session’s last committed word (if available) to request next suggestions. If that fallback is also empty (e.g., leading spaces), it skips calling `Suggest.getNextSuggestions(...)` so a “manual space” won’t clobber an already-populated next-word strip.

### 1b) Auto-space after pick can trigger a “restart word suggestions” flow (clearing the strip)

When auto-space is enabled, a manual pick commits the word and then inserts a space. If the space is
inserted via a key-event path (rather than `InputConnection.commitText`), some editors can deliver
selection updates in a way that looks “unexpected” to `SelectionUpdateProcessor`, which then
triggers `postRestartWordSuggestion()`.

That restart flow aborts prediction and rebuilds a composing word at the cursor. When the cursor is
at a word boundary, the rebuilt word can be empty, and the suggestions strip ends up cleared. The
user then sees “no suggestions until I type the next letter.”

Mitigation: insert the auto-space via the same `InputConnectionRouter` (commitText) inside the
picker’s batch-edit, so the editor treats it as part of the same edit transaction.

#### Status

- Implemented mitigation: `ImeSuggestionsController.pickSuggestionManually(...)` now calls `markExpectingSelectionUpdate()` before committing the pick (and optional auto-space), so editor selection updates from that transaction won’t trigger `SelectionUpdateProcessor` → `postRestartWordSuggestion()` and clear the next-word strip.

### 2) Neural engine currently returns “next token”, not “next word”

`NeuralPredictionManager` currently takes the top-k logits and decodes them **as single GPT-2 tokens**. GPT-2 BPE tokens are often **sub-word pieces**, so a token like `" com"` will decode to `com`, even if the intended word is `company` (or another longer continuation).

Mitigations in this codebase:

- Prefer GPT‑2 “word-start” tokens (raw decode begins with whitespace) and filter to word-like strings.
- Avoid feeding the model a trailing space in the context (helps avoid continuation fragments).
- For short candidates, attempt a small greedy expansion to reach a word-shaped output:
  - when `past_key_values` / `present` outputs are available, expand cheaply using cached state;
  - otherwise, fall back to a **strictly bounded** full-sequence rerun expansion (slower, but avoids showing `com`/`in` fragments when no-cache models are used).

### 3) Neural mode can be empty early in a sentence

`NextWordPredictionEngines` currently requires at least `NEURAL_MIN_CONTEXT_TOKENS` (1) before invoking the neural engine. That means neural can run after the first committed word (context size >= 1).

### 4) Fallback to legacy is keyed off “had usable candidates”

The pipeline uses `MergeOutcome.hadNormalized` to decide whether it should fall back to legacy next-word sources. If the neural engine returns raw tokens that normalize to **zero usable candidates**, callers should prefer falling back instead of showing an empty/fragment-y strip.

This can make the neural engine feel “randomly broken” because it sometimes returns output that we immediately filter out.

Note: `NeuralPredictionManager` now allows returning an **empty** candidate list when everything is filtered out (for example, domain artifacts like `com` in normal prose), so HYBRID/legacy sources can fill the strip instead of surfacing the artifacts.

### 5) Next-word prefix injection can make 1-letter suggestions feel “random”

While composing a word, `SuggestImpl.getSuggestions(...)` also injects _prefix-matching_ next-word
candidates (`mNextSuggestions`) into the typed-word suggestion list. This is meant to make next-word
predictions act like context-aware completions (“keep me `i`” → “informed”).

However, when the user has only typed 1 letter, too many candidates can match and the injected list
can dominate the strip, making it feel “random” or overly context-heavy.

Mitigation implemented: keep the typed word at index 0 while composing, only inject up to 3
prefix-matching next-word candidates, match prefixes case-insensitively, and apply typed casing to
the injected candidates (`SuggestImpl`).

## Additional hypotheses to validate

- **Context drift / double-counting**: engine context is currently updated inside `appendNextWords(...)` whenever `getNextSuggestions(...)` is called, not when a word is actually committed. If UI/controller flows call `getNextSuggestions` multiple times for the same committed word (or call it once on pick and once on a follow-up separator), the context deque can drift and harm prediction quality.
- **Context drift / double-counting**: engine context is updated on `notifyWordCommitted(...)`. Validate we don’t call it twice for the same committed word (for example: a manual pick followed by a separator path that also reports a commit).
- **Space anchoring / word-start tokens**: GPT‑2 BPE uses “word start” tokens that include a leading-space marker. Feeding the model a context that ends with a trailing space can push the model toward emitting tokens that _don’t_ include the marker (more fragment-like). The current approach keeps the leading space but avoids a trailing space, and prefers decoded tokens whose raw form starts with whitespace (with a fallback pass if nothing matches).

## Fixing plan (incremental)

1. **Repro + logging**
   - Add debug logging (guarded by debug/test flags) to log:
     - “what triggered suggestions update” (separator vs manual pick),
     - the word used for `getNextSuggestions(...)`,
     - neural raw decoded tokens vs normalized candidates.
   - Ensure logs can be enabled in Genymotion instrumentation runs.

2. **Stop clearing next-word suggestions on manual space**
   - Ensure we don’t overwrite next-word UI with `getNextSuggestions("")` when the space press didn’t actually commit a word.
   - Options:
     - Skip calling `getNextSuggestions(...)` when `wordForNextSuggestions` is empty (keep the existing next-word strip).
     - Or track a “last committed word for next-word suggestions” in IME session state and use it when `wordForNextSuggestions` is empty.
   - Add a focused unit test for the controller flow (manual pick → space) to prevent regressions.

3. **Make neural candidates “word-shaped”**
   - Filter decoded tokens:
     - Prefer candidates that start at word boundary (decoded token starts with whitespace before trim).
     - Drop very short fragments and tokens containing non-word characters (except apostrophes).
   - If filtered results are empty, fall back to n-gram/legacy sources (even in neural mode) rather than showing fragments.

4. **Optional: generate full next-words (within latency budget)**
   - For the top 1–2 initial tokens, run a short greedy expansion loop to reach a boundary (whitespace/punctuation), producing full-word candidates.
   - Keep strict bounds (token count + wall time) so we don’t regress typing latency.

5. **Genymotion regression coverage**
   - Added: `NextWordSuggestionsUiAutomatorTest.manualSpaceAfterManualPickKeepsNextWordSuggestions` (manual pick with auto-space disabled → user presses space → next-word strip remains populated).
   - Still desirable: a heuristic assertion that candidates are not token fragments in normal prose (to catch regressions like `com`/`in`).

## Release notes / risk

- The “stop clearing suggestions on manual space” change is low-risk and should improve UX for all engines.
- The “word-shaped candidates” filtering may reduce neural suggestions count but should increase perceived quality.
- Full word generation is higher-risk (latency) and should be guarded behind a strict budget and tested on-device before publishing.

## ADB log capture helper (real device)

If you want to verify next-word behavior in a real app (for example: “suggestions go empty after pick/space”), you can capture the relevant logcat lines via:

```bash
scripts/adb_nextword_diagnostics.sh
```

Optional: to include additional neural engine internals (word expansion + filtering logs), set:

```bash
adb shell setprop NSK_TEST_LOGS true
```

Then restart the IME/app and re-run the repro + capture.

By default this filters for:

- next-word request sources (separator vs manual pick),
- `SuggestImpl.getNextSuggestions(...)` sources,
- neural invocation context and word-expansion latency logs.

If you want to change what is captured, set `ADB_NEXTWORD_FILTER_REGEX` before running the script.
