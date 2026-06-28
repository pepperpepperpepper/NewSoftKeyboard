# Neural completion — optimization design

Status: **planned, not implemented.** Design doc for improving the on-device transformer
suggestion/completion path. Covers five optimizations; #1 (prefix-constrained decoding) and #2
(per-word context-KV reuse) are co-designed because they share the same data flow and only make
sense together.

## Current state (what we're optimizing)

The stack already exists and is mature:

- **`engine-neural/NeuralPredictionManager`** — ONNX Runtime GPT-2 inference. GPT-2 BPE
  `Gpt2Tokenizer`, KV-cache reuse (prefix-extension only), greedy multi-token word expansion
  (`greedyExpandWordScored`), top-k logit selection via heap, log-prob scoring. `MAX_CONTEXT_TOKENS
  = 64`.
- **`NextWordPredictionEngines`** — orchestrates HYBRID mode (Presage n-gram + neural) by default;
  `NEURAL_LATENCY_BUDGET_MS = 25`, `HYBRID_NEURAL_COOLDOWN_PASSES = 2`; neural runs async on a
  dedicated single-thread executor.
- **`engine-models/ModelDownloader` / `ModelStore`** — models are downloaded at runtime, not bundled.
- **`SuggestImpl.getSuggestions`** (`SuggestImpl.java:285,361`) — neural touches suggestions two ways:
  next-word injection, and reranking dictionary candidates by first-token log-prob
  (`sortCandidatesByNeuralFirstTokenLogProbIfAvailable`, which reuses a cached
  `NextWordScoringContext` — cheap CPU, not fresh inference).

### The core limitation

`PredictionEngine.predict(String[] contextTokens, int maxResults)` (`NeuralEngineAdapter.java:48`)
only sees **committed** context. On the mid-word completion path the model never sees the in-progress
typed prefix; it can only **reorder dictionary-derived candidates**. It cannot complete a word the
dictionaries don't contain (rare words, proper nouns, domain terms, neologisms).

---

## #1 + #2 (co-designed): prefix-constrained decoding over a cached context KV

These are designed together: prefix-constrained decoding is the *capability*; per-word context-KV
reuse is what makes it *affordable per keystroke*. Implementing one without the other is wrong —
prefix decoding without KV reuse re-encodes 64 tokens on every keystroke (too slow inside the 25 ms
budget); KV reuse without prefix decoding has nothing new to accelerate.

### The shared data flow

Split the per-keystroke neural work into two layers with very different cost and cadence:

```
            committed context (stable within a word)        in-progress prefix (changes per keystroke)
            ─────────────────────────────────────────       ───────────────────────────────────────────
  layer A:  encode + forward pass → context KV  (once per word, cached)
  layer B:                                                    constrain logits to prefix → expand → score  (every keystroke, cheap)
```

- **Layer A — context KV (the #2 part).** When a word boundary is crossed (word committed, sentence
  reset, cursor moved, field changed), recompute the forward pass over the committed context tokens
  and **retain the KV cache** keyed by *(model id, context-token sequence)* — explicitly **excluding
  the in-progress word**. This is the one expensive forward pass, amortized across every keystroke of
  the next word.
- **Layer B — prefix-constrained expansion (the #1 part).** On each keystroke, starting from the
  cached context KV, run prefix-constrained decoding for the in-progress word:
  1. Encode the typed prefix to its BPE token(s). The prefix usually splits into a known
     leading-space token + continuation tokens; feed those through the cached KV as a short delta
     step (1–3 tokens, cheap) to obtain logits for the next token.
  2. **Constrain the candidate set** to token ids whose decoded form is consistent with completing
     the prefix — i.e. the prefix's final partial BPE token's continuations, plus exact-boundary
     cases. Concretely: mask the logit vector to the token-id subset compatible with the prefix
     (precomputed prefix→token-id trie over the tokenizer vocab; see "Tokenizer prefix index").
  3. Expand the surviving seeds with the existing `greedyExpandWordScored` machinery, which already
     reuses KV across expansion steps, and score by summed log-prob.

### Why this is the right shape

- The stable/volatile split mirrors how typing actually works: context changes once per word, prefix
  changes once per keystroke. Caching at the word boundary is the natural seam.
- Layer B reuses the *existing* expansion + scoring code; the genuinely new pieces are (a) the
  context/prefix KV split and (b) the prefix→token-id constraint.

### Concrete changes

1. **`PredictionEngine` / `NeuralEngineAdapter`** — add a prefix-aware entry point, e.g.
   `complete(String[] contextTokens, String inProgressPrefix, int maxResults)` returning
   prefix-consistent completions with sequence-prob scores. Keep the existing `predict(...)` for the
   empty-prefix next-word case (it becomes the `inProgressPrefix == ""` path).
2. **`NeuralPredictionManager`** —
   - Split `mKvCache` into a **context KV** (layer A, keyed by context tokens excluding the current
     word, invalidated on word/sentence/field boundary) and the existing transient
     expansion KV (layer B, per-call).
   - Add `runContextForward(int[] contextTokens)` that produces and retains the context KV.
   - Add prefix-constrained seed selection: given the prefix's trailing partial token, intersect
     top-k logits with the prefix-compatible token-id set before expanding.
   - Reuse `greedyExpandWordScored` unchanged for expansion from each seed.
3. **Tokenizer prefix index (`Gpt2Tokenizer`)** — build, once at activation, a structure mapping a
   character prefix → the set of vocab token ids whose decoded surface form starts with that prefix
   (bounded by a max fan-out). This makes step 2's constraint O(prefix) instead of scanning the whole
   vocab per keystroke. Memory bound: cap entries / fan-out; lazily populated is acceptable.
4. **`SuggestImpl.getSuggestions`** — where it currently only *reranks* dictionary candidates
   (`maybeRerankPrefixMatchingTypedSuggestionsByContext`, line 361), also *inject* neural
   prefix-completions that the dictionaries missed, deduped against dictionary results, capped like
   the existing `MAX_PREFIX_MATCHING_NEXT_WORDS_IN_TYPED_SUGGESTIONS` so neural never floods the strip.
5. **Cache invalidation hooks** — invalidate the context KV from the same places that reset
   next-word context today (sentence reset, `onFinishInputView`, cursor jump, field/profile change).
   Getting this wrong shows stale completions, so it must share the existing reset plumbing rather
   than invent new triggers.

### Cost model / guardrails

- Layer A forward pass stays within the existing async HYBRID executor and the 25 ms budget +
  cooldown; it runs once per word, not per keystroke.
- Layer B per-keystroke work is a 1–3 token delta step + masked top-k + bounded greedy expansion —
  designed to fit comfortably under budget given a cached context KV.
- If the context KV is cold (first word, just-reset), Layer B falls back to the current behavior
  (dictionary completion + cached-context rerank) rather than blocking on a synchronous forward pass.

### Test plan (#1+#2)

- Prefix `"recom"` with empty/neutral context completes to `"recommend"`/`"recommendation"` even when
  removed from the dictionary (proves model-driven completion, not dictionary rerank).
- Context-sensitivity: `"I want to book a"` + prefix `"fl"` ranks `"flight"` above `"floor"`.
- KV reuse: typing successive keystrokes of one word triggers exactly **one** context forward pass
  (assert via `LastInferenceDebugState` / inference-sample telemetry), and per-keystroke latency after
  the first is well under the next-word path's.
- Invalidation: committing a word, sentence reset, and cursor move each force a fresh context KV
  (assert no stale completion leaks across the boundary).
- Budget: a cold context KV does not block the main thread; completion degrades to dictionary-only.

---

## #3: configure the ONNX session (cheap, low-risk latency win)

`mSessionOptions = new OrtSession.SessionOptions()` (`NeuralPredictionManager.java:225`) is bare —
**no execution provider, no thread count, no graph optimization level**.

- Set `setOptimizationLevel(ALL_OPT)`.
- Add an execution provider: **XNNPACK** as the safe CPU default; optionally **NNAPI** where the
  device/model support it, behind a capability check with graceful fallback to plain CPU.
- Set a sane intra-op thread count (e.g. 2) and explicit execution mode rather than the unspecified
  default — keyboards run alongside the host app, so unbounded threads are counterproductive.

Self-contained, no behavior change, directly eases the 25 ms budget that currently trips cooldowns.
**Recommended to land first**, before #1/#2, so their latency is measured against an optimized session.

### Test plan (#3)

- Activation still succeeds with XNNPACK enabled; falls back cleanly when an EP is unavailable
  (no crash, logs the fallback).
- Inference parity: top-k tokens for a fixed context/model are unchanged (or within float tolerance)
  vs. the unconfigured session — optimization must not change outputs.
- Latency: record `LastInferenceDebugState.onnxLatencyMs` before/after on the test model; expect a
  measurable drop.

---

## #4: beam expansion over greedy

Word expansion is greedy and expands only the single top short candidate
(`WORD_EXPANSION_MAX_CANDIDATES = 1`, `greedyExpandWordScored`). Replace the single-path greedy walk
with a small beam (width 2–3) over the existing `WORD_EXPANSION_MAX_EXTRA_TOKENS` horizon, keeping the
top-scoring completed word. Affordable specifically *because* #2 makes per-step inference from a
cached KV cheap.

- Keep the beam tiny and bounded; this is a quality refinement, not a search overhaul.
- Reuse the existing log-prob scoring; the beam just retains N partial hypotheses instead of 1.

### Test plan (#4)

- Multi-token words (e.g. `"appoint"` → `"appointment"`) win over a greedy local maximum that the
  width-1 path settles for.
- Latency stays within budget for beam width 2–3 on the test model.
- Determinism: ties broken stably (by score then token order) so suggestions don't jitter.

---

## #5: richer context

Context is word-token capped (`DEFAULT_CONTEXT_WINDOW_WORDS = 20`, `MAX_CONTEXT_TOKENS = 64`) and the
sentence-boundary reset is tuned for n-grams.

- **Longer / softer context.** Transformers benefit from more context than n-grams. Evaluate raising
  the neural context window independently of Presage's, and softening the hard sentence-boundary reset
  for the neural path (n-gram resets at sentence start; the transformer can carry prior-sentence
  context).
- **Field/app bias from `ContextProfiles`.** The active profile already knows the field/app
  (`SuggestionsDictionariesManager.setContextProfileSafeToggles`). Surface a lightweight signal
  (e.g. a domain hint, or simply allowing domain-like tokens as the URL heuristic
  `looksLikeDomainContext` already does) to bias decoding per field.
- This is the most exploratory item and should be **driven by the quality-eval harness**
  (`NeuralNextWordQualityEvalHostTest`) rather than shipped on intuition.

### Test plan (#5)

- Quality-eval harness shows non-regression (ideally improvement) in next-word/completion accuracy at
  the larger window.
- Latency at the larger window stays within budget (interacts with #2/#3 — measure together).
- Privacy: longer retained context respects the existing password/incognito suppression and
  `ContextProfiles` personal-content toggles — no context retained where the field forbids it.

---

## Sequencing

1. **#3** first — self-contained, low-risk, gives an optimized baseline to measure against.
2. **#1 + #2 together** — the high-value pair; prefix-constrained decoding on a cached context KV.
3. **#4** — beam expansion, once cached-KV per-step inference is cheap.
4. **#5** — context tuning, gated on the quality-eval harness.

## Out of scope

- Changing the model architecture or shipping a different/distilled model (orthogonal; the download
  infrastructure already supports swapping models).
- The Presage n-gram engine internals.
- Server-side / cloud inference — this stays fully on-device.
