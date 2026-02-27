# Dictionary Plan: LatinIME‑class gesture typing with legacy support

## Summary

We want “best in class” gesture/swipe typing (LatinIME‑style) **without breaking existing AnySoftKeyboard (ASK) language packs**. The highest quality path is:

1. **Keep existing ASK dictionaries** (current `.dict` format + `newsoftkeyboard_jnidictionaryv2`) for tap‑typing suggestions and for backwards compatibility.
2. Add a **new LatinIME‑compatible gesture decoding engine** (vendored AOSP source) that uses **LatinIME‑format dictionaries**, and ship those dictionaries in updated language packs.
3. At runtime, **prefer the new engine** when a LatinIME dictionary exists; otherwise **fall back** to the legacy gesture typing engine (or disable gesture typing for that language with a clear message).

This document focuses on dictionaries and packaging because that’s what determines whether existing packs keep working.

## Current state (what we have today)

### ASK dictionary toolchain

- Language packs build dictionaries via Gradle plugin `make-dictionary` (`buildSrc/src/main/java/MakeDictionaryPlugin.java`).
- It merges word sources into `words_merged.xml`, then generates an ASK binary dictionary using `MakeBinaryDictionary` (`buildSrc/src/main/java/MakeBinaryDictionary.java`).
- Output is stored in pack resources under `src/main/res/raw/*.dict` and referenced via a generated `values/*_words_dict_array.xml`.

### ASK runtime dictionary loader

- The main app loads pack dictionaries via `ResourceBinaryDictionary` (`ime/dictionaries/jnidictionaryv2/...`), backed by native `newsoftkeyboard_jnidictionaryv2`.

### Gesture typing today (why it’s BETA)

- Gesture typing currently loads _all words_ into memory for each keyboard via `Dictionary.getLoadedWords`, then scores candidates by distance in Java (`ime/gesturetyping/.../GestureTypingDetector.java`).
- That approach is correct but not competitive in:
  - latency (O(N) scan per gesture end),
  - memory (word lists in Java heap),
  - multilingual scaling.

## Goals

### Product goals

- Gesture typing quality comparable to AOSP LatinIME (baseline users already recognize).
- Keep existing language packs installable and functional (no “everything broke” upgrade).
- Allow staged rollout: new engine can be opt‑in until stable, then become default.

### Engineering goals

- **No full word‑list materialization** in Java just to do gesture typing.
- Keep builds reproducible (important for F‑Droid): no downloading during build.
- Avoid adding new third‑party Maven dependencies; vendor sources instead.

### Security goals

Language packs are APKs that can be installed by users. Treat pack‑provided binary dictionary inputs as **untrusted**:

- Never write extracted dictionaries outside app private storage.
- Validate dictionary headers/versions/sizes before passing to native parsers.
- Add fuzz/property tests where feasible to reduce native parser risk.

## Non‑goals (for first iteration)

- Replacing ASK’s existing tap‑typing suggestion engine immediately.
- Cross‑device personalization sync for gesture typing.
- On‑device learning model (beyond existing frequency weights).

## Proposed architecture

### 1) Two engines, selected per language/keyboard

- `LegacyGestureTypingEngine`
  - Uses existing `GestureTypingDetector` (Java distance scan), fed by `Dictionary.getLoadedWords`.
  - Works with existing packs (current ASK `.dict`).
- `LatinImeGestureTypingEngine` (new)
  - Uses vendored AOSP LatinIME gesture decoder and its dictionary format.
  - Requires LatinIME‑format dictionary artifacts in the language pack.

Selection logic (high level):

- If `LatinImeDictionary` is available for current alphabet keyboard locale → use `LatinImeGestureTypingEngine`.
- Else → use legacy engine (or show “Gesture typing not available for this language until pack update”).

### 2) Dual dictionary formats in language packs

For each language pack we will ship:

- **ASK dict** (existing):
  - For legacy suggestions and backwards compatibility.
- **LatinIME dict** (new):
  - Dedicated to gesture typing (initially).

This allows:

- Older main‑app versions still work with updated packs (they ignore the new dict files).
- New main‑app versions still work with old packs (they fall back).

## Dictionary artifacts & packaging

### A) Where to store LatinIME dictionaries

We should prefer **assets** for the new dicts, because:

- LatinIME dictionaries are large and typically read via file descriptors / memory map.
- `AssetManager.openFd()` only works for **uncompressed** assets.

Plan:

- Store new dicts under pack `src/main/assets/latinime_dict/…`
- Ensure they are **not compressed** in the APK (Gradle/AGP “noCompress” for the relevant extensions).

Runtime load strategy:

- Try `openFd()` for zero‑copy mapping.
- If not possible (compressed), fall back to extracting to `Context.getNoBackupFilesDir()` and mapping the extracted file.

### B) Versioning & cache invalidation

We need a stable way to know when an extracted dict needs refresh:

- Include a small metadata file per dict (e.g., JSON or protobuf) adjacent to the asset:
  - locale, format version, build timestamp, checksum (sha256) of the dict bytes.
- Store extracted dicts as:
  - `noBackupFilesDir/latinime_dict/{packPackageName}/{sha256}.dict`
- On load:
  - if cached sha matches metadata sha → reuse
  - else → extract new file and delete old ones for that package

## Build pipeline changes (language packs)

### A) Generate both formats from the same merged word list

We already merge to `words_merged.xml`. Extend the pipeline to generate:

1. ASK format (existing behavior, unchanged).
2. LatinIME format (new task).

Implementation approach:

- Add a second Gradle task in `make-dictionary` plugin:
  - `makeLatinImeDictionary`
  - Depends on `mergeAllWordLists`
  - Outputs into `src/main/assets/latinime_dict/…` (or build dir + copy task)

### B) Vendor the LatinIME dictionary build tool

We should vendor whichever AOSP LatinIME dictionary builder is appropriate **as source** into the repo (buildSrc or a dedicated `tools/` module) so builds remain self‑contained.

Guidelines:

- Pin a specific upstream commit.
- Keep vendored code in an isolated folder and document patch diffs.
- Preserve upstream license headers and add/extend NOTICE files as required.

### C) Language pack rollout strategy

1. Start with English pack to validate end‑to‑end.
2. Add automation to update all packs:
   - a script/task that runs dictionary generation for all `addons/languages/*/pack`
3. Publish updated packs alongside main app update.

## Runtime changes (main app)

### A) Introduce a “LatinIME dictionary provider”

New component responsibilities:

- Discover available LatinIME dictionaries from:
  - current language pack (by locale),
  - optionally the main app (for a built‑in fallback language).
- Open/map dictionary bytes safely.
- Provide a narrow API to the gesture decoder.

### B) Safety checks before native usage

Before passing bytes/FD to native code:

- Validate header magic/version.
- Validate size bounds (reasonable maximum; reject absurd sizes).
- Refuse non‑seekable streams for mmap paths.
- Log only non‑sensitive info (package name, locale, version), never typed text.

## Testing plan

### P0 tests (must have)

- Unit tests for dictionary discovery:
  - pack provides dict for locale → selected
  - pack missing dict → fallback engine used
- Unit tests for cache invalidation (sha‑based refresh).
- Gesture typing “golden” tests for English:
  - given a trace → top candidate matches expected word
  - performance: candidate generation executes within a budget in unit tests (use generous thresholds).

### P1 tests (important)

- Corrupt dict handling:
  - invalid header → engine disabled/fallback, no crash
  - truncated file → safe failure
- Fuzz/property tests (where practical):
  - feed random bytes into dict header parser (not full native decode)

## Milestones & priorities

### P0 — Foundation (end‑to‑end working)

- Vendor LatinIME gesture decoder sources into a new module (builds on CI).
- Implement LatinIME dictionary packaging for **English pack** (assets + noCompress).
- Implement runtime loader + engine selection + fallback behavior.
- Add P0 tests.

### P1 — Scale out (all language packs)

- Extend dictionary generator to all language packs.
- Add build/CI guardrails:
  - ensure generated dict assets exist for packs that opt‑in
  - prevent accidental compression of dict assets
- Improve UX copy:
  - “Gesture typing requires updated language pack” when missing

### P2 — Performance & quality hardening

- Optimize dictionary loading (file descriptor mapping, reuse across keyboards where safe).
- Profile and reduce memory spikes on first use.
- Expand language coverage and add more golden tests.

## Open questions (need decisions)

1. **Graceful fallback UX (decision)**: if the language pack is old and does not include a LatinIME
   dictionary for the active locale, we will:
   - fall back to the legacy engine for this session, and
   - show a **one‑time prompt** suggesting the user update the language pack for better gesture
     typing quality/performance.
     Implementation detail: persist “prompt shown” as a boolean keyed by `{packPackageName, locale}`
     in app preferences, and only evaluate/show when gesture typing is enabled and the user enters a
     gesture typing session for that locale (avoid prompting on mere keyboard switch).
2. **Dict storage location**: assets only, or allow `res/raw` too?
3. **Feature flag**: keep gesture typing “BETA” until LatinIME engine is default for the top N languages?
