# Next-Word Suggestions: What Looks Normal vs What Looks Wrong (Tap-Chain Report Deep Dive)

Source artifacts (latest):

- Genymotion report JSON: `outputs/genymotion/nextword-tapchain-report/20260226-003418/report.json`
- Genymotion logcat: `outputs/genymotion/nextword-tapchain-report/20260226-003418/logcat.txt`
- Genymotion HTML viewer (uploaded): `outputs/genymotion/nextword-tapchain-report/20260226-003418/url.txt` (key: `nsk-nextword-suite/index.html`)
- Genymotion (filtered `context-visibility`) report JSON: `outputs/genymotion/nextword-tapchain-report/20260225-091612/report.json`
- Genymotion (filtered `context-visibility`) HTML viewer (uploaded): `outputs/genymotion/nextword-tapchain-report/20260225-091612/url.txt` (key: `nsk-nextword-suite-only-context-visibility/index.html`)
- Genymotion (filtered `compare-free-run-2`) report JSON: `outputs/genymotion/nextword-tapchain-report/20260226-002002/report.json`
- Genymotion (filtered `compare-free-run-2`) HTML viewer (uploaded): `outputs/genymotion/nextword-tapchain-report/20260226-002002/url.txt` (key: `nsk-nextword-suite-only-compare-free-run-2/index.html`)
- Device Farm matrix directory: `outputs/devicefarm/nextword-tapchain-report/20260226-011712/`
- Device Farm HTML index (uploaded): `outputs/devicefarm/nextword-tapchain-report/20260226-011712/url.txt` (key: `nsk-nextword-suite-devicefarm/index.html`)

Prior artifact set referenced in this write-up:

- Report JSON: `outputs/genymotion/nextword-tapchain-report/20260220-031718/report.json`
- Logcat: `outputs/genymotion/nextword-tapchain-report/20260220-031718/logcat.txt`
- HTML viewer (uploaded): `outputs/genymotion/nextword-tapchain-report/20260220-031718/url.txt`

This note summarizes what looks **normal** about our current next-word pipeline, and what looks
**unusual / potentially wrong**, based on the Genymotion “tap the top next-word repeatedly”
report run on **2026-02-20** (NSK `13.8.104 (15155)`), with fixes validated by later runs on
**2026-02-21**, **2026-02-22**, **2026-02-23**, **2026-02-25**, and **2026-02-26**.

## What Looks Normal (and Good)

### 1) Neural is actually using multi-word context

The `neural-context-sensitivity` case uses two prompts that both end with `"... the "`:

- Case A: `I deposited money in the `
- Case B: `I sat on the bank of the `

Neural’s top suggestions differ across the two cases:

- Case A: `deposit, wallet, form`
- Case B: `city, day, town`

This indicates the neural engine sees more than the last token and is conditioned on broader
context (at least within the engine’s context window).

### 2) Legacy Markov/learned (“mode=none”) behaves as expected

After training on `alpha beta gamma delta`, the learned-dictionary (Markov-ish) chain test predicts:
`beta → gamma → delta`, producing `alpha beta gamma delta`.

### 3) Domain-like token filtering appears to work (in this run)

We did not see domain-token artifacts (`com/http/www`) in the neural start suggestions, and the report
did not record domain warnings for the `keep me ` “golden phrase” case.

## What’s Unusual / Potentially Wrong

### 1) HYBRID doesn’t behave “hybrid” in the report; it behaves “mostly n-gram”

Every HYBRID run’s _start_ suggestions look like Presage / fixture suggestions:

- `pizza, banana, and` (fixture vocabulary)
- `moon, cat, and` (fixture vocabulary)

Most importantly: HYBRID **fails the context sensitivity check**.
In `neural-context-sensitivity`, HYBRID returns the same suggestions for both Case A and Case B:
`pizza, banana, and`.

That’s a very strong signal that HYBRID is not consistently incorporating neural in the user-visible
top row.

### 2) Root cause: neural is consistently far above the HYBRID latency budget, so HYBRID skips neural

The HYBRID pipeline includes a latency budget and “cooldown passes” that skip neural when the last
neural inference exceeds the budget:

- Budget: `NEURAL_LATENCY_BUDGET_MS = 25ms`
- Cooldown: `HYBRID_NEURAL_COOLDOWN_PASSES = 2`
- Skip condition + logging: `NextWordPredictionEngines.collectNeuralCandidates` logs
  “Skipping neural cascade…” and decrements `mHybridNeuralCooldownRemaining`.

Code references:

- Budget constants: `ime/nextword/src/main/java/wtf/uhoh/newsoftkeyboard/nextword/prediction/NextWordPredictionEngines.java:37`
- Skip logic: `ime/nextword/src/main/java/wtf/uhoh/newsoftkeyboard/nextword/prediction/NextWordPredictionEngines.java:369`

The logcat shows:

- Neural inference latencies roughly **132–313ms** (median ~176ms), which is >7× the budget.
- Many “Skipping neural cascade…” lines after the first slow call, which means HYBRID will often
  fall back to Presage-only results.

Example symptom (“flicker” for the same context `the`):

- First, neural runs and `the` shows neural-like suggestions (`and`, `Image`, `I`, …)
- Then cooldown triggers and the next `the` update shows n-gram suggestions (`pizza`, `banana`, `and`, …)

This is why HYBRID appears non-context-sensitive: neural is frequently absent from the visible top
strip due to skip/cooldown.

### 3) Neural inference appears to run on the UI thread (causing jank) in the tested flow

In the logcat, `SuggestionsProvider` and `NSKSuggest` lines often show PID==TID (e.g. `5702  5702`),
which is typically the app’s main thread in Android logs. Around neural inference we also see:

- “Skipped 73 frames! The application may be doing too much work on its main thread.”

Even if not every call is on the main thread, the observed “frame skip” indicates that at least in
this test harness path, neural inference is impacting UI responsiveness. That’s structurally wrong
for an IME: next-word should never stall rendering / input handling.

### 4) HYBRID’s generated sentences look like “n-gram loops with occasional neural contamination”

In the fixture chain case, the HYBRID “tap-chain” output stays mostly in the fixture vocabulary and
only injects a small number of out-of-fixture tokens (e.g. `next`, `after`, `was`). This is what you
get when:

- Presage is always present and fast, and
- Neural only runs intermittently (or is skipped) and therefore only occasionally influences the chain.

## What to Fix Next (Concrete)

### A) Don’t run neural inference synchronously on the “next-word after separator/pick” path

Right now next-word suggestions are requested immediately after separator handling:
`ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/SeparatorHandler.java:85`

If that path is synchronous, it will magnify neural latency into user-visible stutter/jank. We should:

- Show fast sources immediately (Presage + legacy) and update suggestions asynchronously when neural arrives, or
- Perform neural scoring on a background thread and only apply results if still relevant to the current context.

### B) Rework the HYBRID latency/cooldown strategy

Today, HYBRID’s “skip neural for N passes after a slow call” can produce:

- Context sensitivity loss (HYBRID looks like n-gram)
- Strip instability/flicker across consecutive updates

If neural is routinely ~150–300ms on typical devices, a 25ms budget guarantees chronic skipping.
We either need:

- A realistic budget and an async pipeline, or
- A speed improvement so typical inference meets budget.

### C) Reduce neural latency: implement KV-cache / incremental decoding (completed)

Implemented KV-cache reuse in `NeuralPredictionManager`: we keep the `past_key_values` tensors from
the last inference (via `present.*` outputs) and, when the next context is an extension of the cached
context, run inference on only the newly-appended tokens with the cached past.

Code: `engine-neural/src/main/java/wtf/uhoh/newsoftkeyboard/engine/neural/NeuralPredictionManager.java`

For GPT-2 style models, reusing `past_key_values` across sequential next-word predictions is the
standard way to avoid re-encoding the entire context every time. Without it, latency grows and
becomes unsuitable for a live IME.

### D) Make this failure mode visible in the report itself

The report should capture per-run signals that currently only show up in logcat, e.g.:

- `neuralLatencyMs`
- `neuralWasSkipped` (HYBRID cooldown)
- Whether displayed suggestions came from neural vs presage vs legacy sources

This would let us catch “HYBRID is actually n-gram” regressions by looking at `report.json` alone.

## Progress (Implemented)

### 2026-02-20: Make HYBRID neural async + refresh on completion

Changes:

- HYBRID neural inference is no longer executed on the UI thread. We now:
  - return fast Presage/legacy suggestions immediately, and
  - compute neural candidates on a background executor and refresh the strip when ready.
- HYBRID no longer uses the 25ms latency budget + cooldown skip for candidate collection (the code
  still exists, but the HYBRID path no longer calls it). This removes the “HYBRID behaves mostly
  n-gram” failure mode caused by chronic “Skipping neural cascade…”.

Code:

- Async HYBRID neural + caching: `ime/nextword/src/main/java/wtf/uhoh/newsoftkeyboard/nextword/prediction/NextWordPredictionEngines.java`
- Refresh hook wiring:
  - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/SuggestionsProvider.java`
  - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/SuggestImpl.java`
  - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeSuggestionsController.java`
- Test reliability with async strip updates:
  - `ime/app/src/debug/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/CandidateViewTestRegistry.java`
  - `ime/app/src/release/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/CandidateViewTestRegistry.java`
  - `ime/app/src/androidTest/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/nextword/NextWordTapChainReportUiAutomatorTest.java`
- Genymotion runner: added ADB reconnect + logcat timeout to avoid hangs when the ADB port changes:
  - `scripts/genymotion_nextword_tapchain_report.sh`

Validation:

- New report artifacts: `outputs/genymotion/nextword-tapchain-report/20260221-002755/`
- Uploaded HTML: `outputs/genymotion/nextword-tapchain-report/20260221-002755/url.txt`
- Logcat check: no “Skipping neural cascade…” lines in `outputs/genymotion/nextword-tapchain-report/20260221-002755/logcat.txt`
- Context sensitivity: HYBRID now differs across the two prompts in `neural-context-sensitivity`
  (it’s not consistently stuck on fixture tokens anymore), but we still sometimes capture Presage
  “start suggestions” before the async neural refresh lands. See next steps below.

### Next step (recommended) — completed

This is now implemented as part of the `schemaVersion=3` report updates below (“Make the async
HYBRID upgrade visible in the report”).

### 2026-02-21: Make `report.json` extraction robust (logcat line reordering)

Problem:

- With the larger schema, the stdout JSON lines can share the same millisecond timestamp in logcat,
  and `adb logcat -d` can output those lines out-of-order, corrupting `report.json`.

Fix:

- The test now prints a gzip+base64 encoded report split into line-numbered chunks:
  `NWJSONGZ:<lineNo>:<base64Chunk>` (older runs used `NWJSON:<lineNo>:<jsonLine>`).
- The Genymotion runner extracts those lines, sorts by `lineNo`, and reconstructs `report.json`.

Code:

- Line-numbered stdout: `ime/app/src/androidTest/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/nextword/NextWordTapChainReportUiAutomatorTest.java`
- Reassembly in runner: `scripts/genymotion_nextword_tapchain_report.sh`

### 2026-02-21: Make the async HYBRID “upgrade” visible in the report (schema v3)

Problem:

- HYBRID now upgrades suggestions asynchronously (fast fallback → neural), but `report.json` could still
  look like “HYBRID is n‑gram/fixture” because we were often capturing the pre-refresh strip, and the
  report had no per-run signal that async neural actually completed and triggered a refresh.
- Additionally, the test harness was not maintaining `ImeSuggestionsController`’s
  `lastCommittedWordForNextSuggestions`, which meant the async refresh callback could fire but not
  actually re-render the next-word strip in the harness.

Fix:

- Bumped report schema to `schemaVersion=3` and now record, per run:
  - `startSuggestionsInitial` (first strip after requesting next-word)
  - `startSuggestionsSettled` (after waiting for HYBRID async neural completion/refresh)
  - `hybridNeuralAsync` telemetry snapshots (`initial` + `settled`) including cache/in-flight info,
    compute latency, and whether the refresh callback ran.
- Added `ImeTestApi` helpers to make the “fast fallback vs. settled” sampling deterministic:
  - `clearSuggestionsForTest()`
  - `commitTextNoSuggestionsForTest()` (seed context without scheduling suggestions early)
  - HYBRID telemetry accessors for the report (`dumpHybridNeuralAsyncDebugStateForTest`, count getter)
- Fixed the harness refresh wiring by recording the last committed token for next-word:
  - New `ImeSuggestionsController.recordLastCommittedWordForNextSuggestionsForTest(...)`
  - Called from `ImeTestApi.commitText*` when whitespace-terminated tokens are observed.
- Updated the HTML viewer to show both initial vs settled suggestions and expose HYBRID telemetry.

Code:

- HYBRID async telemetry: `ime/nextword/src/main/java/wtf/uhoh/newsoftkeyboard/nextword/prediction/NextWordPredictionEngines.java`
- Test API + committed-word tracking:
  - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeSuggestionsController.java`
  - `ime/app/src/debug/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeTestApi.java`
  - `ime/app/src/release/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeTestApi.java`
- Report schema + waiting logic:
  - `ime/app/src/androidTest/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/nextword/NextWordTapChainReportUiAutomatorTest.java`
- Viewer: `docs/nextword-test-suite/index.html`

Validation:

- Fresh report with visible HYBRID initial→settled upgrade: `outputs/genymotion/nextword-tapchain-report/20260221-231735/`
- Uploaded viewer key (same URL each run): `outputs/genymotion/nextword-tapchain-report/20260221-231735/url.txt`

### 2026-02-21: KV-cache / incremental decoding for neural inference

Problem:

- Neural inference was paying full-context cost on every request even when the context was just
  growing by one committed token, which keeps steady-state latency too high for a live IME.

Fix:

- Cache the model’s `present.*` tensors (past-key-values) from the previous call and, when the next
  request’s encoded context starts with the cached encoded context, run the model on only the delta
  tokens using the cached past (then update the cache to the new `present.*` outputs).

Code:

- `engine-neural/src/main/java/wtf/uhoh/newsoftkeyboard/engine/neural/NeuralPredictionManager.java`

Validation (Genymotion tap-chain, 2026-02-21):

- Report artifacts: `outputs/genymotion/nextword-tapchain-report/20260221-231735/`
- Uploaded viewer key (same URL each run): `outputs/genymotion/nextword-tapchain-report/20260221-231735/url.txt`
- Logcat latency distribution (“Neural inference latency …ms” lines):
  - Before KV-cache (20260221-015323): n=149, median=233ms, mean=236.5ms, p95=280ms, max=300ms
  - After KV-cache (20260221-231735): n=134, median=174.5ms, mean=178.2ms, p95=218ms, max=288ms

### 2026-02-22: Make KV-cache benefits visible in `report.json` + HTML (per-inference samples)

Problem:

- KV-cache improvements were measurable in logcat (“Neural inference latency …ms”), but were not
  visible in `report.json` (and therefore not reviewable in the HTML viewer) because the report only
  captured a small number of “snapshot” telemetry points.

Fix:

- Record per-inference samples for neural predictions during each run, including:
  - ONNX runtime latency (`onnxLatencyMs`)
  - Pipeline latency around the neural call (`pipelineLatencyMs`)
  - Whether the neural engine used KV-cache (`usedKvCache`) and related token counts
- Export those samples into `report.json` as `neuralInferences` on each `mode=neural` / `mode=hybrid`
  run, and render a summary in the HTML viewer.

Code:

- Neural inference debug state: `engine-neural/src/main/java/wtf/uhoh/newsoftkeyboard/engine/neural/NeuralPredictionManager.java`
- Sample recording + export: `ime/nextword/src/main/java/wtf/uhoh/newsoftkeyboard/nextword/prediction/NextWordPredictionEngines.java`
- Test API plumbing:
  - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/SuggestionsProvider.java`
  - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/SuggestImpl.java`
  - `ime/app/src/debug/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeTestApi.java`
  - `ime/app/src/release/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeTestApi.java`
- Report export: `ime/app/src/androidTest/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/nextword/NextWordTapChainReportUiAutomatorTest.java`
- Viewer: `docs/nextword-test-suite/index.html`

Validation (Genymotion tap-chain, 2026-02-22):

- Report artifacts: `outputs/genymotion/nextword-tapchain-report/20260222-043450/`
- Uploaded viewer key (same URL each run): `outputs/genymotion/nextword-tapchain-report/20260222-043450/url.txt`
- Example (fixture-the-chain, mode=hybrid, NORMAL): `neuralInferences` shows `usedKvCache=true` for
  19/20 samples, ONNX median latency ~32ms, and pipeline median latency ~178ms (Genymotion).

### 2026-02-22: Prevent cross-editor next-word context leakage when seeding is unavailable

Problem:

- We seed next-word context from editor text via `InputConnection.getTextBeforeCursor(...)` on
  `onStartInputView`, but this is best-effort: it can return empty (blank field), or throw/return
  null for some editors.
- Before this change, if seeding did not happen, the next-word engines could retain the previous
  editor’s context window and produce “random / wrong” next-word suggestions (and could also be a
  privacy smell if the new editor disables context access).

Fix:

- Always reset next-word sentence/context when a new input view starts, before attempting to seed
  from editor text.
- When seeding succeeds, it repopulates the in-memory context window immediately after the reset.

Code:

- Reset at input start: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeServiceBase.java`

### 2026-02-22: Reduce neural pipeline overhead (token decode caching)

Problem:

- The neural candidate extractor decodes many vocab token IDs per inference. Our GPT-2 tokenizer’s
  `decodeId(...)` path was allocation-heavy (per-call byte deque) and had no per-id decoded cache,
  which can inflate `pipelineLatencyMs` even when KV-cache keeps ONNX latency low.

Fix:

- Cache decoded strings per token-id (`decodedIdCache`) and add an array-backed id→token lookup for
  fast decode hot paths.
- Remove per-decode `ArrayDeque` allocations in `decodeToken(...)`.

Code:

- Tokenizer optimizations: `engine-neural/src/main/java/wtf/uhoh/newsoftkeyboard/engine/neural/Gpt2Tokenizer.java`

Validation (Genymotion tap-chain, 2026-02-22):

- Earlier run: `outputs/genymotion/nextword-tapchain-report/20260222-022657/report.json`
- Latest run: `outputs/genymotion/nextword-tapchain-report/20260222-043450/report.json`
- Across all exported `neuralInferences` samples (n=162): pipeline median improved ~180.5ms → ~170.0ms,
  p95 improved ~238.9ms → ~228.7ms, ONNX median improved ~35.0ms → ~33.0ms (Genymotion).

### 2026-02-22: Make NEURAL mode async (avoid UI-thread stalls) + update report harness

Problem:

- We previously fixed UI-thread stalls for HYBRID by moving neural inference off the main thread.
- But NEURAL mode still ran neural inference synchronously inside `appendNextWords(...)`, meaning
  selecting “neural” as the prediction engine could still jank/stall the IME during next-word
  updates (especially on separator/pick-driven refreshes).

Fix:

- NEURAL mode now uses the same async neural request/cache path as HYBRID:
  - return fast sources immediately (often legacy next-word dictionaries),
  - schedule neural on a background executor,
  - refresh the strip when neural candidates are ready.
- Updated the tap-chain report harness to also wait for the NEURAL async settle so the report can
  capture “initial vs settled” start suggestions for NEURAL runs, similar to HYBRID.

Code:

- Async NEURAL collection: `ime/nextword/src/main/java/wtf/uhoh/newsoftkeyboard/nextword/prediction/NextWordPredictionEngines.java`
- Report settle wait for NEURAL: `ime/app/src/androidTest/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/nextword/NextWordTapChainReportUiAutomatorTest.java`
- Viewer label (telemetry applies to NEURAL too now): `docs/nextword-test-suite/index.html`

Validation (Genymotion tap-chain, 2026-02-22):

- New report artifacts (first async-NEURAL validation): `outputs/genymotion/nextword-tapchain-report/20260222-051315/`
- Latest report artifacts (also includes later perf tuning): `outputs/genymotion/nextword-tapchain-report/20260222-052650/`
- Uploaded viewer key (same URL each run): `outputs/genymotion/nextword-tapchain-report/20260222-052650/url.txt`

### 2026-02-22: Cut neural pipeline latency by limiting word-expansion work

Problem:

- Even with KV-cache, `pipelineLatencyMs` was still much higher than `onnxLatencyMs`, suggesting
  significant post-ONNX overhead and/or extra ONNX calls (notably: word-expansion steps).

Fix:

- Limit “expand short candidate into a longer word” work to only the top candidate:
  - `WORD_EXPANSION_MAX_CANDIDATES: 3 → 1`

Code:

- Word-expansion limit: `engine-neural/src/main/java/wtf/uhoh/newsoftkeyboard/engine/neural/NeuralPredictionManager.java`

Validation (Genymotion tap-chain, 2026-02-22; all exported `neuralInferences`, n=162):

- Before: `outputs/genymotion/nextword-tapchain-report/20260222-051315/report.json`
  - pipeline median ~174ms, p95 ~238ms, ONNX median ~37ms, KV hit ~91.4%
- After: `outputs/genymotion/nextword-tapchain-report/20260222-052650/report.json`
  - pipeline median ~108ms, p95 ~166ms, ONNX median ~38ms, KV hit ~91.4%

### 2026-02-22: Make suggestion sources visible in `report.json` + HTML (schema v4)

Problem:

- Even with “initial vs settled” strips, it was still hard to tell (from `report.json` alone) _why_
  a displayed suggestion appears, because the strip is a merge across:
  - on-device engines (Presage + Neural),
  - legacy next-word dictionaries,
  - contacts next-word suggestions, and
  - optional punctuation suggestions.

Fix:

- Added a best-effort “source breakdown” snapshot to the tap-chain report:
  - `nextWordPipeline.initial` + `nextWordPipeline.settled`
  - Includes the full candidate pool (`finalSuggestions`) and a parallel `finalSuggestionSources`
    array tagging each candidate as `neural`, `ngram`, `legacy`, `contacts`, `punctuation`, or
    `unknown`.
- Rendered this in the HTML viewer as a simple table under “Suggestion source breakdown”.

Code:

- Engine candidate source snapshots: `ime/nextword/src/main/java/wtf/uhoh/newsoftkeyboard/nextword/prediction/NextWordPredictionEngines.java`
- Pipeline snapshot + tagging: `ime/nextword/src/main/java/wtf/uhoh/newsoftkeyboard/nextword/pipeline/NextWordSuggestionsPipeline.java`
- Test API plumbing:
  - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/SuggestionsProvider.java`
  - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/SuggestImpl.java`
  - `ime/app/src/debug/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeTestApi.java`
  - `ime/app/src/release/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeTestApi.java`
- Report export: `ime/app/src/androidTest/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/nextword/NextWordTapChainReportUiAutomatorTest.java`
- Viewer: `docs/nextword-test-suite/index.html`

Validation (Genymotion tap-chain, 2026-02-22):

- New report artifacts (schema v4): `outputs/genymotion/nextword-tapchain-report/20260222-155522/`
- Uploaded viewer key (same URL each run): `outputs/genymotion/nextword-tapchain-report/20260222-155522/url.txt`

### 2026-02-22: Reset + reseed next-word context on idle cursor moves

Problem:

- If the user moves the cursor while **not composing**, and lands in whitespace (not “touching” a word),
  the “restart word suggestion” path intentionally does nothing (“no-man land”). In that case we could
  retain stale next-word state from the prior cursor location:
  - `lastCommittedWordForNextSuggestions` fallback used by separator handling
  - next-word sentence state used for learning + context-driven engines

Fix:

- On unexpected cursor/selection moves while idle (not composing), we now:
  - reset next-word sentence state + clear `lastCommittedWordForNextSuggestions` + clear the strip, and
  - best-effort reseed the engine context window from `getTextBeforeCursor(...)` (when safe), and
  - if the cursor is in whitespace and we can extract a prior token, immediately show next-word suggestions for it.

Code:

- Selection hook: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/SelectionUpdateProcessor.java`
- Reset/reseed/display: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeSuggestionsController.java`
- Unit coverage: `ime/app/src/test/java/wtf/uhoh/newsoftkeyboard/app/ime/SelectionUpdateProcessorTest.java`

### 2026-02-22: Add a context-visibility report case (reset → reseed from editor text)

Problem:

- We need concrete evidence for when we can read existing editor text via `InputConnection.getTextBeforeCursor(...)`,
  and how much next-word quality depends on seeding that context when the IME’s in-memory next-word sentence is reset
  (for example, after input-view restarts).

Fix:

- Added a new tap-chain report case `context-visibility` (NEURAL + HYBRID) that:
  - commits a seed phrase,
  - resets in-memory next-word sentence state (“restart”),
  - captures top suggestions before seeding (context missing),
  - attempts to reseed the engines’ context from editor text (and records the decision + readback results), and
  - captures top suggestions after seeding (context restored when possible).
- The case runs 3 scenarios:
  - `NORMAL` (readback expected to work)
  - `READBACK_NULL` (simulated editor where `getTextBeforeCursor` returns null)
  - `KEEP_FLAGS` (seeding blocked by `NO_SUGGESTIONS` + `IME_FLAG_NO_PERSONALIZED_LEARNING`)
- Exported the seed attempt info into `report.json` as `editorSeedAttempt` and render it in the HTML viewer.

Code:

- Tap-chain case + export: `ime/app/src/androidTest/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/nextword/NextWordTapChainReportUiAutomatorTest.java`
- Test API helpers: `ime/app/src/debug/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeTestApi.java`
- Test API helpers (release): `ime/app/src/release/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeTestApi.java`
- Viewer: `docs/nextword-test-suite/index.html`

### 2026-02-22: Add a Device Farm tap-chain report runner (device matrix)

Problem:

- We have a good Genymotion runner (`scripts/genymotion_nextword_tapchain_report.sh`), but we lacked a Device Farm
  equivalent to run the same report on a real-device matrix and publish comparable HTML artifacts.

Fix:

- Added `scripts/devicefarm_nextword_tapchain_report.sh`, which:
  - runs `NextWordTapChainReportUiAutomatorTest#generateTapChainReport` via `devicefarm-smoke`
  - extracts `NWJSONGZ:` chunks (gzip+base64; or legacy `NWJSON:` lines) from Device Farm artifacts
    (`LOG/ListArtifactType.log_*` → `System.out`), with a raw `FILE/Logcat_` fallback
  - uses gzip+base64 chunking (`NWJSONGZ:`) to stay under Device Farm `System.out` truncation limits for large reports
  - reconstructs per-device `report.json` (including decoding `NWJSONGZ:`), renders per-device HTML pages, and writes a
    simple `upload/index.html` that links them
  - uploads to stable keys under `nsk-nextword-suite-devicefarm/` (override with `DEVICEFARM_UPLOAD_KEY_PREFIX`) and
    writes `outputs/devicefarm/nextword-tapchain-report/<ts>/url.txt`

Code:

- `scripts/devicefarm_nextword_tapchain_report.sh`

Validation:

- Device Farm matrix run (2026-02-23):
  - Artifacts: `outputs/devicefarm/nextword-tapchain-report/20260223-103625/`
  - Uploaded HTML index: `outputs/devicefarm/nextword-tapchain-report/20260223-103625/url.txt`
  - Devices: Google Pixel 8 Pro (Android 14), Google Pixel 9a (Android 15), Samsung Galaxy S25+ (Android 15)

### 2026-02-22: Expand the tap-chain report prompts (edge-case tokenization)

Fix:

- Added a new tap-chain report case `edge-cases-tokenization` (ngram + neural + hybrid) that records top suggestions
  for prompts containing:
  - URL text (`https://example.com`)
  - email text (`test@example.com`)
  - path text (`/var/log`)
  - numbers/currency (`$5.99`)
  - multiline/newline
- Tightened the “domain-like token” scanner in the report test to avoid false positives like `come` being flagged as
  `com` (now checks exact tokens / URL prefixes).
- Updated the HTML viewer to include the new case.

Code:

- Tap-chain case: `ime/app/src/androidTest/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/nextword/NextWordTapChainReportUiAutomatorTest.java`
- Viewer: `docs/nextword-test-suite/index.html`

### 2026-02-22: Fix tap-chain capture for async-only NEURAL/HYBRID start suggestions

Problem:

- In `mode=neural` (and sometimes `mode=hybrid`) when `noPersonalizedLearning=1`, we can have **no fast fallback**
  suggestions (legacy next-word sources are intentionally disabled). In that situation our async neural path returns an
  empty strip first, then upgrades later.
- The tap-chain harness was skipping the “wait for async settle” step when the initial strip was empty, which meant
  some report runs recorded empty `startSuggestions*` and then failed at pick 0.

Fix:

- In `runCaseSingleScenario`, always sample `hybridNeuralAsync` telemetry and wait for async settle for `mode=neural` /
  `mode=hybrid` (even when the initial strip is empty), then do an extra short wait for UI suggestions to appear before
  sampling `startSuggestionsSettled`.

Code:

- `ime/app/src/androidTest/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/nextword/NextWordTapChainReportUiAutomatorTest.java`

### 2026-02-22: Add summary metrics to the HTML viewer

Fix:

- Added a “Summary metrics” card to the report viewer that computes per-engine rollups from the embedded `schemaVersion=4`
  data:
  - % of top-3 suggestions by source (from the `nextWordPipeline` settled snapshot)
  - async upgrade rate + an estimated “settle time” (from `hybridNeuralAsync.*.last*UptimeMs`)
  - ONNX/pipeline latency median/p95 + KV-cache hit rate (from `neuralInferences`)

Code:

- Viewer: `docs/nextword-test-suite/index.html`

Validation (Genymotion tap-chain, 2026-02-22):

- New report artifacts: `outputs/genymotion/nextword-tapchain-report/20260222-221720/`
- Uploaded viewer key (same URL each run): `outputs/genymotion/nextword-tapchain-report/20260222-221720/url.txt`

### 2026-02-23: Preserve next-word context across input-view restarts (fix incognito empties)

Problem:

- In the tap-chain report runs, we observed frequent `onStartInputView(..., restarting=true)` calls (likely due to repeated
  `showSoftInput`/toggle requests from the test harness).
- We previously reset next-word sentence/context on **every** `onStartInputView`, which means every restart cleared:
  - engine context window (`NextWordPredictionEngines.mPresageContext`)
  - next-word dictionaries sentence state
- In `IME_FLAG_NO_PERSONALIZED_LEARNING` / incognito / `NO_SUGGESTIONS` fields, editor seeding is intentionally blocked, so a
  restart would leave the IME with **empty context** and therefore empty NEURAL/HYBRID start suggestions.

Fix:

- Only reset next-word sentence/context on `onStartInputView` when `restarting=false` (new editor). On view restarts for the
  same editor, preserve the in-memory context window.
- Preserve `lastCommittedWordForNextSuggestions` across `onStartInputView(restarting=true)` so async NEURAL/HYBRID refresh has
  a stable “previous token” to re-render against.
- Stabilize instrumentation seed commits by marking selection updates as expected (avoids the idle-cursor-move reset path
  clearing context in no-personalized-learning fields).
- Report infra hardening:
  - Tap-chain test no longer fails the entire run on transient UI visibility issues (launch retries + throws `RuntimeException`
    so per-run errors are recorded instead of aborting the report).
  - Genymotion runner streams `System.out` logcat during the run to avoid logcat-buffer truncation dropping early `NWJSON*` lines.

Code:

- Reset policy: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeServiceBase.java`
- Preserve last committed token on restarts: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeSuggestionsController.java`
- Test-seed commit stability: `ime/app/src/debug/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeTestApi.java`
- Test-seed commit stability (release tests): `ime/app/src/release/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeTestApi.java`
- Report best-effort + retries: `ime/app/src/androidTest/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/nextword/NextWordTapChainReportUiAutomatorTest.java`
- System.out streaming extraction: `scripts/genymotion_nextword_tapchain_report.sh`

Validation (Genymotion tap-chain, 2026-02-23):

- New report artifacts: `outputs/genymotion/nextword-tapchain-report/20260223-010442/`
- Uploaded viewer key (same URL each run): `outputs/genymotion/nextword-tapchain-report/20260223-010442/url.txt`
- Key regression fixed: `fixture-the-chain` KEEP_FLAGS (noPersonalizedLearning=1 + noSuggestionsFlag=1) now produces non-empty NEURAL/HYBRID start suggestions again.

### 2026-02-23: Seed “previous word” from editor text on new editor start (improves resume-in-field)

Problem:

- On a **new editor** start (`onStartInputView(restarting=false)`), we already best-effort seeded the engine context window
  from `InputConnection.getTextBeforeCursor(...)`, but we did **not** seed the IME’s `lastCommittedWordForNextSuggestions`.
- This meant separator-driven next-word fallback requests (e.g., pressing Space while not composing a word) could be empty
  even when the field already contained text (common when resuming a long note/chat draft).

Fix:

- When editor seeding is allowed and the cursor is **not** at the start/inside of a word (i.e., the next character is a
  separator or the cursor is at end-of-text), we now also seed `lastCommittedWordForNextSuggestions` from the tokenized
  editor text. This enables next-word fallback flows without learning/persisting from editor content.
- Expanded the tap-chain report’s `context-visibility` case with prepopulated-field scenarios to validate this behavior.
  - Stabilized those scenarios by avoiding UI click-based cursor placement and warming up dictionaries/models before the
    forced “next-word from last committed word” request.
  - Report now records the seeded in-memory previous token via `lastCommittedWordForNextSuggestionsAtStart` for review.

Code:

- Seed previous-word token: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeServiceBase.java`
- Setter plumbing: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeSuggestionsController.java`
- Harness extras for prefill/cursor: `ime/app/src/debug/java/wtf/uhoh/newsoftkeyboard/app/debug/TestInputActivity.java`
- Harness extras for prefill/cursor (release): `ime/app/src/release/java/wtf/uhoh/newsoftkeyboard/app/debug/TestInputActivity.java`
- Harness introspection (last-committed token): `ime/app/src/debug/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeTestApi.java`
- Harness introspection (last-committed token, release): `ime/app/src/release/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeTestApi.java`
- Report scenarios: `ime/app/src/androidTest/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/nextword/NextWordTapChainReportUiAutomatorTest.java`

Validation (Genymotion tap-chain, 2026-02-23):

- New report artifacts: `outputs/genymotion/nextword-tapchain-report/20260223-052409/`
- `context-visibility` now includes:
  - `PREPOPULATED (onStartInputView seed)`: `lastCommittedWordForNextSuggestionsAtStart="the"`, forcedCount>0, non-empty NEURAL/HYBRID start suggestions.
  - `PREPOPULATED (cursor before word)`: `lastCommittedWordForNextSuggestionsAtStart=""`, forcedCount=0, start suggestions empty (by design; cursor is before a word).

### 2026-02-23: Seed editor context via ExtractedText fallback (improves apps with null readback)

Problem:

- Some editors return `null`/empty from `InputConnection.getTextBeforeCursor(...)` (or throw), which makes
  editor seeding fail even in non-privacy-restricted fields. This weakens next-word suggestions when resuming
  typing in an existing field.

Fix:

- When seeding next-word context from editor text, we now fall back to `InputConnection.getExtractedText(...)`
  (bounded via `ExtractedTextRequest.hintMaxChars/hintMaxLines`) when `getTextBeforeCursor(...)` fails.
- Updated the test-only seed probe (`ImeTestApi.seedNextWordEngineContextFromEditorTextForTest`) to report which
  readback method actually succeeded (`readbackMethod`: `getTextBeforeCursor` vs `getExtractedText`).

Code:

- ExtractedText access: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/InputConnectionRouter.java`
- New-editor seeding: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeServiceBase.java`
- Cursor-move reseed: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeSuggestionsController.java`
- Tap-chain seed probe: `ime/app/src/debug/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeTestApi.java`
- Tap-chain seed probe (release): `ime/app/src/release/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeTestApi.java`

Validation (Genymotion tap-chain, 2026-02-23):

- New report artifacts: `outputs/genymotion/nextword-tapchain-report/20260223-195909/`
- `context-visibility` now shows `READBACK_NULL` succeeding with `readbackMethod="getExtractedText"` (while
  `getTextBeforeCursor="null"`), so we can seed context in editors that don't support readback.
- `context-visibility` includes a new `PREPOPULATED (password field)` scenario where `allowed=false` (seeding blocked).

### 2026-02-24: Expand `context-visibility` to cover resume + cursor-move reseed (implemented; validated in Genymotion)

Changes:

- Added `RESUME_SAME_FIELD (pause/resume activity)` scenario to the tap-chain report to capture whether the IME
  receives `onStartInputView(..., restarting=...)` on resume and whether next-word context remains usable.
- Added `CURSOR_MOVE (between newlines)` scenario to exercise the “unexpected cursor move while idle” reseed path
  (`ImeSuggestionsController.onUnexpectedCursorMoveWhileNotPredicting → maybeSeedNextWordContextFromEditorAfterCursorMove`).
- Exposed test-only `onStartInputView` telemetry via `ImeTestApi.dumpStartInputViewStateForTest()` and export it into
  `report.json` as `imeLifecycle` on those runs.
- Included engine context snapshot (`contextTokens`) in next-word pipeline debug snapshots (`nextWordPipeline.*`) so
  we can review what multi-word context was actually used when generating the strip.

Code:

- `contextTokens` in pipeline debug snapshots: `ime/nextword/src/main/java/wtf/uhoh/newsoftkeyboard/nextword/pipeline/NextWordSuggestionsPipeline.java`
- `onStartInputView` counters/state: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeSuggestionsController.java`
- Test API export: `ime/app/src/debug/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeTestApi.java`
- Test API export (release): `ime/app/src/release/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeTestApi.java`
- New report scenarios: `ime/app/src/androidTest/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/nextword/NextWordTapChainReportUiAutomatorTest.java`
- Viewer: show `imeLifecycle`: `docs/nextword-test-suite/index.html`

Validation (Genymotion tap-chain, 2026-02-24):

- New report artifacts: `outputs/genymotion/nextword-tapchain-report/20260224-222002/`
- `context-visibility` now includes `RESUME_SAME_FIELD` and `CURSOR_MOVE` runs, each exporting `imeLifecycle`.
- `RESUME_SAME_FIELD`: `imeLifecycle.afterResume.lastRestarting=false` and `onStartInputViewCount` increments, indicating the
  IME gets a fresh `onStartInputView(..., restarting=false)` on this pause/resume flow (so editor seeding needs to work).
- `CURSOR_MOVE`: `imeLifecycle.cursorMove.lastCommittedWordAfterMove="world"` and `editorSeedAttempt.previousWord="world"`,
  showing the reseed-after-cursor-move path can recover context in “no-man land” cursor positions.
- `RESUME_SAME_FIELD`: `editorSeedAttempt.previousWord="the"` (and `nextWordPipeline.*.contextTokens` includes
  `I deposited money in the`), confirming the resume scenario now keeps the cursor at end-of-text and yields correct seeding.

Note:

- The earlier `RESUME_SAME_FIELD` run (`outputs/genymotion/nextword-tapchain-report/20260224-020625/report.json`) used a UI click
  to focus the editor after resume, which can place the cursor mid-token and skew `editorSeedAttempt.tokens`/`previousWord`.
  The updated harness uses `requestFocus()` + `setSelection(editText.length())`, and the re-run above validates correct seeding.

### 2026-02-24: Export explicit request→UI refresh timing per run (implemented; validate in next report)

Problem:

- The viewer’s settle-time metric was derived from internal `hybridNeuralAsync.*.last*UptimeMs` timestamps, which is an
  indirect proxy (listener callbacks) and can be misleading in “multi-stage” runs like `context-visibility`.

Fix:

- Each run now exports a `timing` object in `report.json`:
  - `requestToInitialMs`: request→first strip capture
  - `requestToUiFirstChangeMs`: request→first observed strip change (best-effort; intended to approximate async refresh)
  - `requestToSettledMs`: request→post-wait strip capture
- The HTML “Summary metrics” rollup now prefers `timing.requestToUiFirstChangeMs` for settle-time, and excludes
  `context-visibility` from the async-upgrade/settle-time rollups.

Code:

- Tap-chain timing export: `ime/app/src/androidTest/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/nextword/NextWordTapChainReportUiAutomatorTest.java`
- Viewer rollup: `docs/nextword-test-suite/index.html`

Validation (Genymotion tap-chain, 2026-02-24):

- New report artifacts: `outputs/genymotion/nextword-tapchain-report/20260224-222002/`
- Runs now include `timing.requestToUiFirstChangeMs` (request→first observed strip change), and the viewer rollup uses it.

## Remaining / Next Steps (Open)

### 1) Context visibility (priority)

We now have an automated `context-visibility` case in the tap-chain report. Next: confirm (with tests + real apps)
how much prior text the IME can actually see:

- When the user pauses and later resumes typing in the **same** input field, do we reliably get prior words via
  `InputConnection.getTextBeforeCursor(...)`, or are we mostly relying on the in-memory committed-token window?
- Verify behavior across:
  - plain `EditText`-style fields
  - large multiline note editors
  - privacy-restricted fields (`NO_SUGGESTIONS`, passwords, incognito)
  - cursor moves + edits (backspace, mid-text insertion)

Current behavior (as of 2026-02-23, validated in `context-visibility` in `outputs/genymotion/nextword-tapchain-report/20260223-195909/report.json`):

- We maintain an in-memory committed-word context window for engines (default 20 words; configurable).
- On **new editor** start (`onStartInputView(restarting=false)`), we reset next-word state and _best-effort_ seed engine context
  from `getTextBeforeCursor(4096, 0)` when allowed (non-password, not incognito, no `IME_FLAG_NO_PERSONALIZED_LEARNING`, no
  `NO_SUGGESTIONS`), falling back to `getExtractedText(...)` when direct readback is null/empty/throws, tokenizing up to 64
  tokens.
- When editor seeding is allowed and the cursor is not positioned _before_ a word character, we also seed the IME’s
  `lastCommittedWordForNextSuggestions` from the editor text (best-effort, no learning) so separator-driven next-word
  fallback requests can work even before the IME has observed an in-session committed token.
- In privacy-restricted fields (`IME_FLAG_NO_PERSONALIZED_LEARNING` / incognito / `NO_SUGGESTIONS`), editor seeding is
  intentionally blocked; engines only “see” what the IME observed in the current session (committed tokens), and we do not
  query prior editor text for context.
- On **input-view restarts** for the same editor (`restarting=true`), we now preserve the in-memory context window (so pausing
  and resuming typing in the same field does not automatically wipe context in those privacy-restricted scenarios).

2026-02-25 (implemented + validated in `outputs/genymotion/nextword-tapchain-report/20260225-014330/report.json`):

- Fix: when the cursor is inside a token (mid-word), trim the trailing token fragment before seeding next-word engine context
  from editor text (prevents poisoning context with partial tokens like `ba` from `ba|nk`).
- Report: export `contextTokensAtStart` for prepopulated scenarios to validate what the engines actually see right after
  `onStartInputView` seeding (even when we intentionally do not seed `lastCommittedWordForNextSuggestions`).
- Harness: add `PREPOPULATED (cursor inside word)`.
- Validation: `PREPOPULATED (cursor inside word)` now shows `cursorInsideToken=true`, `contextTokensAtStart` ends with
  `... I deposited money in the`, and `lastCommittedWordForNextSuggestionsAtStart` stays empty (so we don’t force next-word
  suggestions while the cursor is mid-token).

2026-02-25 (implemented + validated in `outputs/genymotion/nextword-tapchain-report/20260225-092757/report.json`):

- Expand `context-visibility` scenarios with additional privacy/editor cases:
  - `RESUME_SAME_FIELD_KEEP_FLAGS` (blocked seeding, ensure context doesn’t wipe on restart)
  - `PREPOPULATED (NO_SUGGESTIONS field)`
  - `PREPOPULATED (IME_FLAG_NO_PERSONALIZED_LEARNING field)`
  - `PREPOPULATED (incognito mode)`
  - `PREPOPULATED (large multiline note)` (stress >4k editor readback truncation)
  - `CURSOR_MOVE_INSERT` (cursor move to “no-man land”, reseed from editor, then insert a token)
  - Code: `ime/app/src/androidTest/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/nextword/NextWordTapChainReportUiAutomatorTest.java`
- Add test-only toggles needed by the new scenarios:
  - `ImeTestApi.setIncognitoModeForTest(...)`
  - `ImeTestApi.clearLastCommittedWordForNextSuggestionsForTest()`
  - Code: `ime/app/src/debug/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeTestApi.java`
  - Code: `ime/app/src/release/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeTestApi.java`
- Viewer: truncate long `seed` strings in the meta line (keeps large-multiline runs readable).
  - Code: `docs/nextword-test-suite/index.html`

2026-02-25 (Genymotion tap-chain re-run, `outputs/genymotion/nextword-tapchain-report/20260225-092757/report.json`):

- The new `context-visibility` runs appear in the report for both `mode=neural` and `mode=hybrid`.
- `PREPOPULATED (large multiline note)`: `getTextBeforeCursor` returns 4096 chars, but our tokenizer stops at the last
  newline (treats `\n` as a sentence boundary), so the seeded `contextTokensAtStart` only includes the last “line” tail.
- `PREPOPULATED (incognito mode)`: seeding is blocked as intended (`editorSeedAttempt.allowed=false`) and the run shows
  empty context (no learning, no editor readback).
- `RESUME_SAME_FIELD_KEEP_FLAGS`: in the “keep flags” / `IME_FLAG_NO_PERSONALIZED_LEARNING` scenario, editor seeding is blocked
  (expected), so we rely on the in-memory committed-token context window. Root cause (before the fix): on pause/resume the
  IME window can be hidden and `onWindowHidden()` called `abortCorrectionAndResetPredictionState(true)`, which reset the
  next-word sentence (clearing engine context) but did _not_ clear `lastCommittedWordForNextSuggestions`. On resume we still
  had `"the"` as the last-committed token, but the engine context window was empty → no suggestions.
- Fix (implemented + validated in `outputs/genymotion/nextword-tapchain-report/20260225-091612/report.json` and
  `outputs/genymotion/nextword-tapchain-report/20260225-092757/report.json`):
  - Treat “same editor” + “readback blocked” as a preserve-context condition even when `restarting=false`:
    - Code: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeServiceBase.java`
    - Code: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeSuggestionsController.java`
  - Do not wipe next-word context on `onWindowHidden()`; window-hide is often a transient lifecycle event and editor-boundary
    resets are handled in `onStartInputView(...)`:
    - Code: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeServiceBase.java`
  - Make the incognito test toggle persist across `onStartInputView(...)` by using `ImeIncognito.setIncognito(..., byUser=true)`:
    - Code: `ime/app/src/debug/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeTestApi.java`
    - Code: `ime/app/src/release/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeTestApi.java`
  - Validation signal: `RESUME_SAME_FIELD_KEEP_FLAGS` now shows `nextWordPipeline.*.contextTokens=["I","deposited","money","in","the"]`
    and non-empty start suggestions in both `mode=neural` and `mode=hybrid`.

### 2) Run the tap-chain report on a device matrix (Genymotion + Device Farm)

We have a good Genymotion runner (`scripts/genymotion_nextword_tapchain_report.sh`) and a Device Farm runner
(`scripts/devicefarm_nextword_tapchain_report.sh`).

Completed (Device Farm matrix, 2026-02-23):

- Artifacts: `outputs/devicefarm/nextword-tapchain-report/20260223-103625/`
- Uploaded HTML index: `outputs/devicefarm/nextword-tapchain-report/20260223-103625/url.txt` (stable key:
  `nsk-nextword-suite-devicefarm/index.html`)
- Devices: Google Pixel 8 Pro (Android 14), Google Pixel 9a (Android 15), Samsung Galaxy S25+ (Android 15)

Per-device comparison notes:

- ONNX vs pipeline: Pixels show ~2–3× pipeline overhead above ONNX (likely tokenizer/candidate plumbing/merge + UI refresh),
  while the Samsung device’s pipeline is much closer to ONNX.
- HYBRID “upgrade” works on all devices in this matrix: top‑3 neural share rises to ~93% after settling.
- Upgrade delay appears device-dependent: the Samsung device shows much lower request→listener time than Pixels in this run.
- No UI-thread/jank signal in these logs (“Skipped … frames!” count is 0 for all devices in this matrix).

Scripted summary (Device Farm 2026-02-23; generated with
`node scripts/summarize_nextword_tapchain_matrix.js outputs/devicefarm/nextword-tapchain-report/20260223-103625/`):

| Device             |     Android | Inferences | ONNX p50/p95 (ms) | Pipeline p50/p95 (ms) | KV‑cache hit | Hybrid top‑3 neural (init→settled) | Hybrid req→listener p50/p95 (ms) | Skipped frames (count/max) |
| ------------------ | ----------: | ---------: | ----------------: | --------------------: | -----------: | ---------------------------------: | -------------------------------: | -------------------------: |
| Google Pixel 8 Pro | 14 (sdk 34) |        174 |           31 / 62 |              80 / 118 |          85% |                          46% → 93% |                        752 / 752 |                          0 |
| Google Pixel 9a    | 15 (sdk 35) |        174 |           35 / 63 |              83 / 115 |          85% |                          38% → 93% |                        791 / 791 |                          0 |
| samsung SM-S936U1  | 15 (sdk 35) |        174 |           19 / 23 |               40 / 44 |          85% |                          54% → 93% |                        342 / 342 |                          0 |

Note:

- The “Hybrid req→listener” number is based on cache-miss runs where we can match
  `hybridNeuralAsync.*.lastRequestGeneration` → `lastListenerGeneration`; sample size is low in this data set.

Completed (Device Farm matrix re-run, 2026-02-25; post warm-up + stale-work coalescing):

- Artifacts: `outputs/devicefarm/nextword-tapchain-report/20260225-231900/`
- Uploaded HTML index: `outputs/devicefarm/nextword-tapchain-report/20260225-231900/url.txt` (stable key:
  `nsk-nextword-suite-devicefarm/index.html`)
- Devices: Google Pixel 8 (Android 14), Google Pixel 8a (Android 15), Samsung Galaxy S24+ (Android 14)

Per-device comparison notes (2026-02-25):

- HYBRID “upgrade” still works on all devices in this matrix: top‑3 neural share rises to ~92% after settling.
- Upgrade delay is now low and consistent across this matrix: hybrid req→listener p50 is ~70–85ms.
- KV-cache hit rate is ~43% in this run (lower than the 2026-02-23 matrix’s ~85%). Follow-up (2026-02-26) found a real
  pipeline cause + fix (see “KV-cache follow-up” below); needs a Device Farm re-run to confirm on real devices.
- No UI-thread/jank signal in these logs (“Skipped … frames!” count is 0 for all devices in this matrix).

Scripted summary (Device Farm 2026-02-25; generated with
`node scripts/summarize_nextword_tapchain_matrix.js outputs/devicefarm/nextword-tapchain-report/20260225-231900/`):

| Device            |     Android | Inferences | ONNX p50/p95 (ms) | Pipeline p50/p95 (ms) | KV‑cache hit | Hybrid top‑3 neural (init→settled) | Hybrid req→listener p50/p95 (ms) | Skipped frames (count/max) |
| ----------------- | ----------: | ---------: | ----------------: | --------------------: | -----------: | ---------------------------------: | -------------------------------: | -------------------------: |
| Google Pixel 8    | 14 (sdk 34) |        212 |           53 / 93 |             102 / 147 |          43% |                          17% → 92% |                          85 / 85 |                          0 |
| Google Pixel 8a   | 15 (sdk 35) |        212 |           59 / 95 |             110 / 148 |          43% |                          17% → 92% |                          77 / 88 |                          0 |
| samsung SM-S926U1 | 14 (sdk 34) |        212 |           42 / 62 |              87 / 107 |          43% |                          29% → 92% |                          70 / 70 |                          0 |

Completed (Device Farm matrix re-run, 2026-02-26; includes “fresh private-field editor reset” fix):

- Artifacts: `outputs/devicefarm/nextword-tapchain-report/20260226-011712/`
- Uploaded HTML index: `outputs/devicefarm/nextword-tapchain-report/20260226-011712/url.txt` (stable key:
  `nsk-nextword-suite-devicefarm/index.html`)
- Devices: Google Pixel 9 Pro (Android 15), Samsung Galaxy A36 (Android 15), Samsung Galaxy S25+ (Android 15)

Per-device comparison notes (2026-02-26):

- KV-cache hit rate is back to ~70–71% across this matrix (vs ~43% in the 2026-02-25 re-run).
- Hybrid req→listener remains low across this matrix (still ~40–150ms p50 depending on device).
- No UI-thread/jank signal in these logs (“Skipped … frames!” count is 0 for all devices in this matrix).

Scripted summary (Device Farm 2026-02-26; generated with
`node scripts/summarize_nextword_tapchain_matrix.js outputs/devicefarm/nextword-tapchain-report/20260226-011712/`):

| Device             |     Android | Inferences | ONNX p50/p95 (ms) | Pipeline p50/p95 (ms) | KV‑cache hit | Hybrid top‑3 neural (init→settled) | Hybrid req→listener p50/p95 (ms) | Skipped frames (count/max) |
| ------------------ | ----------: | ---------: | ----------------: | --------------------: | -----------: | ---------------------------------: | -------------------------------: | -------------------------: |
| Google Pixel 9 Pro | 15 (sdk 35) |        212 |           37 / 70 |              87 / 119 |          71% |                          50% → 92% |                          87 / 89 |                          0 |
| samsung SM-A366U1  | 15 (sdk 35) |        214 |           50 / 91 |             109 / 154 |          70% |                          11% → 89% |                        107 / 157 |                          0 |
| samsung SM-S936U1  | 15 (sdk 35) |        212 |           20 / 30 |               41 / 53 |          71% |                          97% → 92% |                          42 / 42 |                          0 |

2026-02-25 (implemented + validated):

- Coalesce bursty async neural requests by skipping stale queued work before running inference. This reduces the
  “tap-chain backlog” failure mode where users only see the fast fallback strip because neural upgrades arrive too late.
  - Code: `ime/nextword/src/main/java/wtf/uhoh/newsoftkeyboard/nextword/prediction/NextWordPredictionEngines.java`
- Warm up neural on dictionary setup. We were re-activating Presage after `close()`/`hibernate()`, but not neural, so the
  first next-word request could pay a full neural activation cost.
  - Code: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/SuggestionsProvider.java`

Validation (Genymotion tap-chain, `compare-free-run-2`, 2026-02-25):

- Artifacts: `outputs/genymotion/nextword-tapchain-report/20260225-141649/`
- `mode=hybrid` now upgrades quickly: `timing.requestToUiFirstChangeMs=276` (was ~1553ms in the previous run before warm-up).

Validation (Genymotion tap-chain, full suite, 2026-02-26):

- Artifacts: `outputs/genymotion/nextword-tapchain-report/20260226-003418/`
- `compare-free-run-2` `mode=hybrid`: `timing.requestToUiFirstChangeMs=203` and `timing.requestToSettledMs=206`
  (was `261` / `279` in `outputs/genymotion/nextword-tapchain-report/20260225-220301/report.json`).

2026-02-26 (implemented + validated): KV-cache follow-up — avoid preserving next-word context across fresh private-field editors

Problem:

- After the 2026-02-25 “preserve context when readback blocked + same editor” change, many report runs in
  `noPersonalizedLearning=1` fields started with `contextTokensCount=20` (the cap) from the first neural inference.
  That pins neural requests to a sliding 20-word window, which makes KV-cache hits impossible (we only hit when the encoded
  context grows; sliding requires a full re-run).

Root cause:

- Our “same editor” key (`packageName:fieldId:inputType:imeOptions`) can match across **fresh** instances of the same editor.
- In readback-blocked editors we preserved context on `restarting=false` whenever the key matched, even when the cursor was at
  position 0 (fresh/empty editor).

Fix:

- Only preserve next-word context for `restarting=false` + readback-blocked + sameEditor when the cursor indicates there is
  existing content (`EditorInfo.initialSelStart/initialSelEnd > 0`). Otherwise reset.
  - Code: `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeServiceBase.java`

Validation (Genymotion full suite, 2026-02-26):

- Artifacts: `outputs/genymotion/nextword-tapchain-report/20260226-003418/`
- KV-cache hit rate improved to **71%** (was **43%** in `outputs/genymotion/nextword-tapchain-report/20260225-220301/report.json`).
- ONNX/pipeline p50 improved to **42/115ms** (was **104/177ms** in `outputs/genymotion/nextword-tapchain-report/20260225-220301/report.json`).

### 3) Expand the test-suite prompts (edge cases)

Add prompts that stress tokenization, filtering, and context updates:

- Partially done: new `edge-cases-tokenization` case added to the tap-chain report.
- Partially done: expanded `edge-cases-tokenization` with emojis, mixed-language text, quotes/parentheses, punctuation,
  and time/percent prompts.
- Done (Genymotion tap-chain, 2026-02-25): added a new `long-form-context` case (notes/chat/paragraphs) to stress longer
  “real app”-like contexts.
  - Code: `ime/app/src/androidTest/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/nextword/NextWordTapChainReportUiAutomatorTest.java`
  - Report: `outputs/genymotion/nextword-tapchain-report/20260225-033110/report.json` now includes `results.long-form-context`.
  - Key observation: even for multi‑kilobyte prompts, `nextWordPipeline.*.contextTokens` is capped at **20 words**
    (see `DEFAULT_CONTEXT_WINDOW_WORDS = 20` in
    `ime/nextword/src/main/java/wtf/uhoh/newsoftkeyboard/nextword/prediction/NextWordPredictionEngines.java:42`).
- Done: URL/email/domain/path-like prompts (ensure domain-token filtering remains correct across all sources)
- numbers/currency/percent, time/dates, quotes/parentheses, emojis, mixed-language text
- sentence boundaries + punctuation-driven transitions
- multiline paragraphs + newlines

### 4) Make the report easier to review (summary metrics)

Using the existing `schemaVersion=4` data, add top-level rollups in the HTML:

- Partially done: added the “Summary metrics” card in `docs/nextword-test-suite/index.html`.
- Done: export per-run `timing.requestToUiFirstChangeMs` (request→first observed strip change) from the harness, and use it
  for settle-time rollups in the viewer.
- Done (Genymotion tap-chain, 2026-02-25): show `nextWordPipeline.*.contextTokens` in the HTML so we can review
  context-window truncation directly.
  - Code: `docs/nextword-test-suite/index.html`
  - Viewer: `outputs/genymotion/nextword-tapchain-report/20260225-033110/index.html` (uploaded via `url.txt`)
- Done (Genymotion tap-chain, 2026-02-26): export neural activation timing separately from inference timing (to make
  cold-start regressions obvious in `report.json` and the HTML summary).
  - `neuralInferences[*].activationLatencyMs`: activation cost if activation happens on the request path (otherwise `0`).
  - `hybridNeuralAsync.*.neuralLastActivationLatencyMs`: last activation time even if warm-up happens before inference samples.
  - Code: `engine-neural/src/main/java/wtf/uhoh/newsoftkeyboard/engine/neural/NeuralPredictionManager.java`
  - Code: `ime/nextword/src/main/java/wtf/uhoh/newsoftkeyboard/nextword/prediction/NextWordPredictionEngines.java`
  - Code: `docs/nextword-test-suite/index.html`
  - Validation: `outputs/genymotion/nextword-tapchain-report/20260226-002002/report.json`

### 5) Follow-up perf + correctness audits using source attribution

Now that we can tag each merged candidate’s origin, use the report to find:

- sources that bypass normalization/filtering (`CandidateNormalizer.normalizeForNextWordUx`)
- duplicate/near-duplicate suggestions from multiple engines (merge stability)
- cases where the “settled” strip regresses relative to “initial” (bad rerank / bad merge)

Audit (Genymotion 2026-02-26, `outputs/genymotion/nextword-tapchain-report/20260226-003418/report.json`):

- 114 runs: 0 `unknown` sources, 0 duplicates (case-insensitive), 0 `com/http/https/www`, 0 “weird” tokens (empty/whitespace)
- `mode=hybrid` (44 runs): 0 runs where the settled top‑3 had _fewer_ neural items than the initial top‑3

### 2026-02-27: Fix “manual pick shows no next-word until SPACE” (selection-update budget)

User-facing symptom:

- After manually picking a completion (e.g., `than` → `thank`), the strip sometimes shows **no next-word suggestions**
  until pressing `SPACE`.

Root cause:

- Some editors emit **multiple** `onUpdateSelection(...)` callbacks for a single “pick + optional space insert”.
- The IME previously treated only the **first** selection update as “expected”; later callbacks were treated as
  unexpected cursor movement while idle, triggering the restart-word-suggestion flow and clearing the next‑word strip.

Fix:

- `SelectionExpectationTracker` now expects a small _budget_ of selection updates per edit (default: 3), so we don’t
  clear next-word suggestions in editors that send multiple selection updates for a single pick.

Related UX cleanup:

- Filter unrelated **ALL‑CAPS acronyms** (>=3 chars) from typed-word suggestions when the user is typing lowercase
  (e.g., `tha` no longer surfaces `TNA` as a correction/completion candidate).

## Remaining TODOs

- (none right now)
