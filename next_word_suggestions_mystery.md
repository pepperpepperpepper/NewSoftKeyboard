# Next-word suggestions “mystery” (Neural / DistilGPT2)

## Reported behavior (user repro)

Target phrase: **“keep me informed”**

1. Type `ke` → suggestion `keep` (good).
2. After committing `keep`, **no next-word suggestion appears** until pressing `space` and then typing `m`.
3. With `m` typed, pick suggestion `me`, but **no trailing space is inserted**.
4. After that, **no suggestion** until typing `space`.
5. After `keep me `, suggestion shown is **`com`** (unexpected).
6. After `keep me i`, suggestions become `i, in, is, it, if, its, ice, I'd…` (does not surface “informed”).

## What the code is doing (high-level)

This codebase has **two distinct “suggestion” systems** that can be easy to confuse:

- **Current-word suggestions**: completions/corrections based on the _currently composed_ word (e.g., typing `i` → `in`, `is`, `it`, …). This is `SuggestImpl.getSuggestions(...)`.
- **Next-word suggestions**: predictions based on the _previous committed_ word(s). This is `SuggestImpl.getNextSuggestions(previousWord, ...)` and the `ime/nextword` pipeline.

The neural engine integration (DistilGPT2 / TinyLlama etc.) currently predicts **next-token**, not “next complete word”.

## Where this happens in code

### Why “picking `me` doesn’t add a space”

The suggestions-strip pick path only inserts a trailing space when `autoSpaceEnabled` is true:

- `AnySoftKeyboard/ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/SuggestionPicker.java:77`

`autoSpaceEnabled` comes from `PredictionState.autoSpace` (defaults to true, but can be user-disabled or forced off by field type).

Auto-space is _forced off_ in email fields (and some “internet” modes) here:

- `AnySoftKeyboard/ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/InputFieldConfigurator.java:67-74`

So “no space added” usually means either:

- user turned off auto-space, **or**
- the input field is `TYPE_TEXT_VARIATION_EMAIL_ADDRESS` / `WEB_EMAIL_ADDRESS` (auto-space forced off).

### Why “no suggestions after space” can happen

Separator (space/punctuation) handling generates next-word suggestions using the **current word composer**:

- `AnySoftKeyboard/ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/SeparatorHandler.java:29-65`
- It calls `Suggest.getNextSuggestions(result.wordForNextSuggestions, ...)` (`SeparatorHandler.java:61-64`)
- `wordForNextSuggestions` is `wordToOutput`, derived from the **typed word composer** (`SeparatorActionHelper.java:46-87`)

If the current `WordComposer` is empty at the moment you press space, `wordForNextSuggestions` becomes empty:

- `AnySoftKeyboard/ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/WordComposerTracker.java:31-39`
- and then `SuggestImpl.getNextSuggestions` returns empty for `previousWord.length()==0`:
  - `AnySoftKeyboard/ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/SuggestImpl.java:149-153`

This matches the UX symptom “no next-word suggestions after space” in scenarios where the word was already committed earlier (e.g., a manual pick just happened, auto-space was off, and then the user presses space).

Mitigation in this codebase: when `wordForNextSuggestions` is empty (a user-pressed separator while not composing a word), `SeparatorHandler` now avoids clobbering the existing next-word strip with an empty `getNextSuggestions("")` request.

### Why “no next-word suggestions after a manual pick” can happen (app-dependent)

When typing letters, we post a **delayed** suggestions refresh:

- `AnySoftKeyboard/ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/CharacterInputHandler.java`
  - `host.postUpdateSuggestions()` schedules `MSG_UPDATE_SUGGESTIONS` after `GET_SUGGESTIONS_DELAY`.
  - (See the `host.postUpdateSuggestions()` call in `updateComposingForPrediction(...)`.)

If the user taps a suggestion **before** that delayed message runs, the manual-pick path will:

1. commit the chosen word and reset the composer (`prepareWordComposerForNextWord()`), then
2. show next-word suggestions for the committed word.

But then the delayed `MSG_UPDATE_SUGGESTIONS` can still fire **after** the pick. Since the current `WordComposer` is now empty, `performUpdateSuggestions()` produces an empty list and can **clobber the next-word strip**.

This is more likely in “heavier” apps where UI thread timing differs (e.g., Google Keep) — the delayed message has a better chance of firing after the pick.

Fix: cancel pending suggestion messages when the user manually picks a suggestion:

- `AnySoftKeyboard/ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeSuggestionsController.java:599`

### Why neural predictions are “non-sequiturs”

Neural next-word predictions start as “top-K next tokens” from the model, but `NeuralPredictionManager` now tries to make them **word-shaped**:

- Context text fed to GPT2: `final String contextText = " " + String.join(" ", contextTokens);`
  - (no trailing space; see “word-start tokens” note below)
- Candidate shaping (current behavior):
  - Filter decoded candidates to “word-like” strings (letters + `'`, bounded length).
  - Prefer GPT‑2 “word-start” tokens (raw decoded token starts with whitespace), with a fallback pass if nothing matches.
  - For very short candidates, attempt a short greedy expansion:
    - use `past_key_values`/`present` when available (fast),
    - otherwise fall back to a strictly bounded “rerun the full sequence” expansion (slower, but avoids showing raw fragments when a model doesn’t export cache outputs).

Important implications:

- A GPT2 “token” is often **not a full word**; it can be a common fragment or a frequent web token (`com`, `'t`, etc.).
- Multi-token decoding exists, but is deliberately bounded and best-effort (fastest when the ONNX export includes `past_key_values` / `present`).
- Legacy fallback is keyed off “had usable normalized candidates”, not “had any raw candidates”, so that filtered/empty neural output can fall back instead of showing an empty strip.
- Neural engine is invoked once there is at least 1 context token:
  - `NEURAL_MIN_CONTEXT_TOKENS = 1` (`NextWordPredictionEngines.java`)

## Most likely causes for the exact repro

1. **Auto-space is disabled** (by setting or input field type), so:
   - picking `me` does not insert a space (`SuggestionPicker.java:77-80`), and
   - pressing space after a manual pick can clear next-word suggestions because the word composer is already empty (see “Why no suggestions after space”).

2. **Neural engine outputs token-level candidates**, which can yield things like `com` after `keep me` depending on the model distribution and the lack of word-boundary/word-completion decoding.

3. The “keep me i → i/in/is/it…” set is very likely **current-word completions** (not next-word predictions). If next-word predictions contained “informed”, it _would_ be eligible to surface when typing `i`, but it likely never exists in the next-word candidate list because neural is not producing full-word candidates.

4. **Some editors emit multiple `onUpdateSelection` callbacks** for a single manual pick (for example: word commit + space insert + internal selection stabilization). If the IME only treats the _first_ selection update as “expected”, later ones can be mistaken for an unexpected cursor move while idle, which triggers the restart-word-suggestion flow and clears the next-word strip.

   Fix (implemented): `SelectionExpectationTracker` now expects a small _budget_ of selection updates per edit (default: 3) instead of only one, so the next-word strip is not cleared after manual picks in editors that send multiple updates.

## Debug checklist (to confirm on-device)

1. Confirm whether auto-space is enabled:
   - In settings, verify `settings_key_auto_space` is on.
   - Also confirm the field type: email fields force auto-space off (`InputFieldConfigurator.java:67-74`).

2. Confirm next-word suggestions are enabled:
   - `settings_key_next_word_dictionary_type` should be `words` or `words_punctuations` (not `off`).

3. Confirm neural engine is actually active (not silently falling back to ngram/none):
   - Check the stored last error pref `settings_key_prediction_engine_last_neural_error` (wired in `NextWordPredictionEngines`).
   - Look for toast “Neural model failed to load … falling back …”.

4. Log what the neural engine is actually seeing/returning:
   - Capture `contextTokens` passed to `NeuralPredictionManager.predictNextWords` and the raw decoded tokens.
   - If `com` is coming directly from the model, you’ll see it in raw top-k output.

## Fixing plan (phased)

### Phase 1 — Fix the UX regression: “space clears next-word suggestions”

Goal: After a manual pick with auto-space disabled, pressing space should still yield next-word suggestions for the last committed word.

Plan:

1. In the separator flow, when `typedWord` is empty, use the last committed word (likely in `WordComposerTracker.previousWord()` or a dedicated “lastCommittedWordForSuggestions” field) instead of passing an empty string into `getNextSuggestions`.
2. Add a regression test covering:
   - type `ke`, pick `keep` with auto-space disabled, press space, verify suggestions are non-empty and based on “keep”.

### Phase 2 — Make neural output “word-like” instead of “token-like”

Goal: Neural next-word suggestions should resemble words (“informed”), not common fragments (“com”).

Plan options (pick one; they can be combined):

1. **Word-boundary filtering**:
   - Keep track of whether the decoded token originally began with a leading-space marker (GPT2 word boundary).
   - Only accept boundary tokens for next-word suggestions.
   - Don’t lose this info in `CandidateNormalizer` (it currently trims).

2. **Multi-token word decoding** (recommended):
   - For each of the top-N first tokens, run a short decode loop (greedy/beam) to extend until a word boundary/punctuation is reached.
   - Score candidates by cumulative log-prob, then surface top K completed words.
   - Add hard limits (max chars, max tokens, latency budget).

3. **Better cascade strategy**:
   - In `HYBRID` mode, consider always allowing legacy next-word dictionaries to contribute (deduped), rather than suppressing them whenever neural had any raw output.

### Phase 3 — Model quality and selection

Goal: Default neural model should be capable of producing useful next-word suggestions for common phrases.

Plan:

1. Treat `distilgpt2_mixedcase_sanity` as a test/sanity model, not the default “recommended” model for users.
2. Prefer a better model (e.g., TinyLlama) as the default if it performs better and fits latency constraints.
3. Add a small “golden phrase” evaluation harness (instrumented test) that checks for expected candidates in a few contexts (non-strict, “contains any of X”).

## Success criteria

- After committing `keep` and pressing space, suggestions appear immediately (no need to type `m`).
- Picking `me` inserts a space when auto-space is enabled, and does not degrade next-word suggestions when auto-space is disabled.
- After `keep me `, neural suggestions include plausible continuations such as `informed`, `updated`, `posted`, etc., and do not heavily bias to web fragments like `com`.
