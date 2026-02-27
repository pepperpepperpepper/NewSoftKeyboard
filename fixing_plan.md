# Fixing Plan (Living) — Wallpaper Picker + Spacebar + Voice STT Feedback + Text Suggestions

_Last updated: 2026-02-19_

This document is the **single living plan** for the current user-visible regressions around:

- Keyboard wallpaper / “Background photo (current theme)” (settings + import + apply)
- Spacebar behavior (keyboard name + status overlays)
- Third‑party Voice STT feedback (spacebar badge + errors)
- Text suggestion strip quality (typed-word suggestions + next-word predictions)
- Compatibility with **AnySoftKeyboard add-ons** (Mike Rozoff keyboard/theme is the primary reference)

This file **absorbs and replaces** `feedback-plan.md` to avoid the “two TODOs” drift problem.

NOTE: Section 5 (“Text suggestions”) is kept at the top because the rest of this plan may be stale/already fixed; verify
status before acting on Sections 0–4.

## 5) Text suggestions (typed-word + next-word) — pipeline + fixing plan

### 5.0) Status (2026-02-19)

- Implemented P0: next-word personalization now ranks by **descending** `usedCount`.
- Implemented P1: next-word storage v2 persists `(nextWord, usedCount)` and auto-migrates v1 → v2 on load.
- Implemented: next-word candidate pooling is now large enough for reranking + prefix-conditioned UX (aggressiveness mapping: minimal=8, medium=24, maximum=48), and `SuggestImpl` now injects prefix matches from the full pooled next-word list (not only the displayed row).
- Implemented: while composing (2+ letter prefixes), prefix-matching typed suggestions are re-ranked by context, preferring neural first-token log-prob when available, and otherwise using the pipeline’s full next-word candidate pool order (so n-gram + legacy personalization/contacts still shape completions).
- Implemented: next-word suggestions no longer require the previous word to be “valid” when engines are inactive (legacy personalization/contacts can still suggest after names/out-of-dictionary tokens).
- Implemented P3: next-word settings summary shows engine/model state and the last neural activation failure (if any), to reduce “it’s weak” reports that are actually “neural is off / failed to activate”.
- Implemented P4: UI-chain quality eval harness covers personalization convergence + persistence (opt-in), so we can measure “learns X→Y and survives restart” at the editor level.
- Added next-word tap-chain report viewer (embedded automated results; shows example sentences by engine): `docs/nextword-test-suite/index.html` (render + upload via `scripts/genymotion_nextword_tapchain_report.sh`).
- Added automated tap-chain report harness (Genymotion): `ime/app/src/androidTest/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/nextword/NextWordTapChainReportUiAutomatorTest.java` + `scripts/genymotion_nextword_tapchain_report.sh` (renders + uploads the HTML report).
- Added regression tests: ordering, min-usage gate persistence, and v2 parser roundtrip.
- Verified: `GRADLE_USER_HOME=/mnt/finished/.gradle ./gradlew :ime:nextword:testDebugUnitTest -x lint`
- Verified (focused): `GRADLE_USER_HOME=/mnt/finished/.gradle ./gradlew :ime:app:testNskDebugUnitTest -x lint --tests "wtf.uhoh.newsoftkeyboard.app.dictionaries.SuggestionsProviderTest" --tests "wtf.uhoh.newsoftkeyboard.app.dictionaries.SuggestImplTest"`

### 5.1) Mental model: “suggestions” are two different systems

1. **Typed/current-word suggestions** (completions/corrections while composing a word)
   - Owner: `SuggestImpl.getSuggestions(...)` in
     `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/SuggestImpl.java`
2. **Next-word suggestions** (predictions shown after a word is committed)
   - Owner: `SuggestImpl.getNextSuggestions(...)` in
     `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/SuggestImpl.java`
   - Pipeline: `SuggestionsProvider` → `NextWordSuggestionsPipeline` → `NextWordPredictionEngines` and legacy sources

This distinction matters because “weak suggestions” can be:

- weak **completions/corrections** (dictionary/scoring issues), or
- weak **next-word predictions** (engine/personalization/persistence issues), or
- confusing **mixing** (next-word candidates injected into typed-word completions by prefix match).

### 5.2) Signal chain: what runs when

**A) User types a letter (composing continues) → typed/current-word strip refresh**

- `ImeSuggestionsController.handleCharacter(...)` →
  `CharacterInputHandler.handleCharacter(...)` →
  `SuggestionsUpdater.postUpdateSuggestions()` (debounced) →
  `ImeSuggestionsController.performUpdateSuggestions()` →
  `SuggestionRefresher.performUpdateSuggestions(...)` →
  `SuggestImpl.getSuggestions(wordComposer)` →
  `CandidateView.setSuggestions(...)`.

Notes:

- The refresh is debounced via `GET_SUGGESTIONS_DELAY` in
  `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeSuggestionsController.java`.
- A delayed refresh can otherwise clobber next-word strips after manual pick; we currently guard against that in
  `ImeSuggestionsController.performUpdateSuggestions()`.

**B) User commits a word (SPACE / punctuation / ENTER) → next-word strip refresh**

- `ImeSuggestionsController.handleSeparator(...)` →
  `SeparatorHandler.handleSeparator(...)` commits the word and records it for suggestions →
  `Suggest.notifyWordCommitted(...)` updates engine context window →
  `Suggest.getNextSuggestions(previousWord, ...)` populates strip with next words.

Notes:

- When the word composer is empty (common after manual pick with auto-space disabled),
  `SeparatorHandler` falls back to `lastCommittedWordForNextSuggestions()` to avoid empty next-word requests.

**C) User starts the next word (types `t` after a space)**

- `SuggestImpl.getSuggestions(...)` may inject up to
  `MAX_PREFIX_MATCHING_NEXT_WORDS_IN_TYPED_SUGGESTIONS` from the last computed next-word list into the typed-word
  suggestions strip (prefix match), then de-dupes/trims.

### 5.3) How we currently rank candidates

**Typed/current-word suggestions**

- Source order (high-level): abbreviations → auto-text → optional split-words → contacts/user/context-profile/main dictionaries.
  - See `SuggestionsDictionariesManager.getSuggestions(...)` in
    `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/SuggestionsDictionariesManager.java`.
- Scoring is frequency-driven with a coarse “commonality” gate:
  - If edit distance is within `{commonalityMaxDistance}` and length diff within `{commonalityMaxLengthDiff}`,
    we treat it as a possible fix and boost frequency by `POSSIBLE_FIX_THRESHOLD_FREQUENCY`.
  - See `SuggestionWordMatcher.haveSufficientCommonality(...)` in
    `ime/dictionaries/src/main/java/wtf/uhoh/newsoftkeyboard/dictionaries/SuggestionWordMatcher.java`.
- Context re-ranking exists only for prefix-typed candidates (2+ letters) and only if neural has a scoring context:
  - `SuggestImpl.maybeRerankPrefixMatchingTypedSuggestionsByContext(...)` prefers neural scoring when available, and falls back to the pipeline’s full next-word candidate pool order when it is not.

**Next-word suggestions**

- Orchestration: `NextWordSuggestionsPipeline.appendNextWords(...)` in
  `ime/nextword/src/main/java/wtf/uhoh/newsoftkeyboard/nextword/pipeline/NextWordSuggestionsPipeline.java`.
- Sources (in order of collection, then re-ranked):
  - **Engines**: Presage (n-gram) + Neural (ONNX), depending on mode.
  - **Legacy personalization**: user next-word dictionary (`NextWordDictionary` per locale).
  - **Contacts next-word**: derived from contact name parts.
- Normalization + reranking:
  - `CandidateNormalizer` trims/dedupes and drops punctuation-only.
  - `CandidateReranker` applies heuristic diversity and repeat avoidance.
  - Neural scoring context can re-score candidates via “first token log-prob” when available.

### 5.4) Most likely reasons suggestions feel weak (root causes)

**P0 — Next-word personalization ordering was backwards (fixed 2026-02-19)**

- Previously, `NextWord.NextWordComparator` sorted ascending by `usedCount`, so the _least used_ next words came first.
  This is now fixed to sort descending:
  - `ime/nextword/src/main/java/wtf/uhoh/newsoftkeyboard/nextword/NextWord.java`
- This affects:
  - user next-word dictionary (`NextWordsContainer.getNextWordSuggestions()`),
  - contacts next-name-part ordering (`ContactsDictionary.loadAllResources()`).

Impact (before the fix): personalization would “learn” but appear not to, because the most-used transitions were pushed
to the bottom and often truncated out by `maxResults`.

**P1 — Next-word personalization persistence was lossy (fixed 2026-02-19)**

Previously:

- On disk we stored only an ordered list, not `(candidate, usedCount)`:
  - `NextWordsFileParserV1.storeNextWords(...)` in
    `ime/nextword/src/main/java/wtf/uhoh/newsoftkeyboard/nextword/NextWordsFileParserV1.java`
- On load, every next-word got a synthetic `usedCount` (derived from list size), losing real ranking information:
  - `NextWordsContainer(String word, List<String> nextWords)` in
    `ime/nextword/src/main/java/wtf/uhoh/newsoftkeyboard/nextword/NextWordsContainer.java`

Now:

- v2 persists `(candidate, usedCount)` and v1 auto-migrates to v2 on load:
  - `ime/nextword/src/main/java/wtf/uhoh/newsoftkeyboard/nextword/NextWordsFileParserV2.java`
  - `ime/nextword/src/main/java/wtf/uhoh/newsoftkeyboard/nextword/NextWordsStorage.java`

Impact (before the fix): even after fixing ordering, personalization would reset quality after restart and interact
badly with `minWordUsage` (aggressiveness).

**P2 — Engine availability is often not what “hybrid” implies**

- Default engine mode is `hybrid`, but neural models are not bundled by default; activation failures can silently
  downgrade to `ngram` or `none`:
  - `NextWordPredictionEngines.handleNeuralActivationFailure(...)` in
    `ime/nextword/src/main/java/wtf/uhoh/newsoftkeyboard/nextword/prediction/NextWordPredictionEngines.java`

Impact: users expect “smart” next-word predictions but are effectively on legacy-only (or none) without realizing it.

**P3 — Gating disables predictions/personalization in many real-world fields**

- `InputFieldConfigurator` disables prediction/auto-pick/auto-space for email/url/password/number fields and can respect
  the app’s `NO_SUGGESTIONS` flag:
  - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/InputFieldConfigurator.java`
- `IME_FLAG_NO_PERSONALIZED_LEARNING` forces incognito (no learning/personal sources):
  - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeIncognito.java`

Impact: “weak suggestions” reports can be app-specific: the pipeline is intentionally constrained by editor flags.

### 5.5) Fix plan (prioritized)

**P0 — Fix next-word ordering (implemented)**

- `NextWord.NextWordComparator` sorts **descending** by `usedCount`.
- Validate both:
  - `NextWordDictionary.getNextWords(...)` returns most-used first.
  - `ContactsDictionary` next-name-part ordering becomes most-used first.

**P1 — Add NextWords storage v2 (persist counts) (implemented)**

- File format v2 persists `(nextWord, usedCount)` pairs.
- v1 → v2 auto-migration (best-effort) runs on load:
  - v1 ordering is the only ranking signal, so we synthesize small counts from ordering and persist v2 for future runs.
  - This also corrects for the historic v1 “least-used-first” persistence bug once v2 is written.
- Backward compatibility: still reads v1 on upgrade; writes v2 going forward.

**P2 — Reduce “domain token” artifacts across all sources (implemented)**

- Apply domain-like filtering at the pipeline normalization level (`CandidateNormalizer.normalizeForNextWordUx`), so
  Presage/legacy sources do not surface `com/http/www` unless the context is clearly URL-like.

**P2b — Make context-aware completions more reliable (implemented)**

- Increase next-word candidate pooling beyond the visible row (aggressiveness mapping: minimal=8, medium=24,
  maximum=48) so reranking can pick a better top row and prefix matches can be found beyond the UI row.
- Cache the full pooled next-word list in `SuggestImpl` and use it for prefix-matching injection while composing (not
  only the displayed next-word row).

**P3 — Make engine/model state visible**

✅ Implemented.

- In next-word settings screen, the Prediction engine + Language models rows surface:
  - current engine mode (including HYBRID),
  - active model label(s),
  - and “last neural activation error” (timestamp + message) when present.

**P4 — Measurement harness**

✅ Implemented (opt-in).

- UI-chain evaluation harness now also covers:
  - personalization convergence (repeat `hello world` N times and assert `world` is suggested after `hello` at medium aggressiveness),
  - persistence across a dictionary close/reload (approximates restart) by validating the on-disk next-word dictionary.
- App-flag scenarios (`NO_SUGGESTIONS`, `NO_PERSONALIZED_LEARNING`) are covered via the KEEP_FLAGS scenario in:
  `ime/app/src/androidTest/java/wtf/uhoh/newsoftkeyboard/app/dictionaries/nextword/NextWordEnginesUiChainQualityEvalUiAutomatorTest.java`.

### 5.6) Success criteria (measurable)

- **Personalization**: after a user repeatedly types `X Y`, the top next-word suggestion for `X` becomes `Y` (and stays
  that way) without requiring extreme aggressiveness.
- **Persistence**: next-word ranking does not collapse after app/IME restart.
- **UX stability**: no regression in:
  - incognito behavior (no learning),
  - fields with `NO_PERSONALIZED_LEARNING`,
  - fields where prediction is intentionally off.

## 0) Current shipped context

- Latest published build (F-Droid): `13.8.53` (`versionCode 15104`)
- Regressions reported on real device (Pixel Fold): settings crash + spacebar state issues
- Emulator note: Genymotion previously showed an ANR symptom in the same settings area (not a clean stack trace yet)

## 0.1) What’s already implemented (grounded in source)

This is important so we don’t “plan” work that already exists.

- Wallpaper overrides are stored per theme in `KeyboardWallpaperOverrideStore` and imported off-main-thread in
  `KeyboardThemeCustomizationFragment.onPhotoPicked(...)`.
- IME wallpaper application uses the override-aware resolver:
  - `ImeThemeOverlay.applyKeyboardWallpaper(...)` uses `KeyboardWallpaperResolver.resolveImeWallpaper(...)`.
- Voice STT visual feedback is implemented as a **draw-time badge overlay** (no key-model mutation):
  - `VoiceImeController` is the single state machine (`IDLE/RECORDING/WAITING/ERROR`).
  - `VoiceStatusRenderer` forwards state to `InputViewBinder`/`KeyboardViewBase`.
  - `KeyDrawHelper` draws the badge when `DrawInputs.spacebarVoiceBadgeText != null`.

## 0.2) Status update (2026-01-01) — what changed since this plan was written

### Code changes applied (shipped)

**Wallpaper picker crash hardening**

- `KeyboardThemeCustomizationFragment` now uses lifecycle-safe context access (`getContext()` + null guards) in:
  - `getCurrentTheme()`
  - `refreshPhotoPreview(...)`
  - `onPhotoPicked(...)`
  - `showPickFailedDialog(...)`

This should prevent “fragment not attached” crashes when returning from the document/photo picker.

**Wallpaper “invisible photo” migration**

- `KeyboardWallpaperOverrideStore.importFromUri(...)` now treats “first wallpaper import” as a migration:
  - If the theme had no wallpaper file yet and the mode was still `BACKGROUND_ONLY`, we default the mode to
    `KEY_TEXTURE` so the feature appears to work on opaque themes.
  - Also sets a more-visible default key-overlay opacity (60%) on first import (can be adjusted later).
  - Added a Robolectric test covering this migration.

**Spacebar keyboard-name restoration**

- `KeyboardNameRenderer.applyKeyboardNameIfNeeded(...)` now:
  - shows the keyboard name on the spacebar even when an icon is drawn (common UX expectation),
  - treats whitespace-only labels as empty so add-on layouts that use `" "` as a placeholder still allow keyboard-name.
  - Added a Robolectric test for this behavior.
- `DrawInputsBuilder` now falls back to using `keyTextSize` when a theme doesn’t provide `keyboardNameTextSize`, so
  add-on themes that omit the attribute still show the keyboard name when the setting is enabled.

**Wallpaper import crash hardening (OOM during encoding)**

- `KeyboardWallpaperOverrideStore.importFromUri(...)` now wraps `OutOfMemoryError` during bitmap WEBP encoding
  into an `IOException` so it can be handled via the UI error flow instead of crashing the app.

**Instrumentation stability**

- `GenericRowsInstrumentedTest` now launches `MainSettingsActivity` via an explicit `Intent` built from the
  **target** context (avoids resolving the activity against the test package).

### Test status (local host)

- ✅ `GRADLE_USER_HOME=/mnt/finished/.gradle ./gradlew :ime:app:assembleNskDebug -x lint`
- ✅ `GRADLE_USER_HOME=/mnt/finished/.gradle ./gradlew :ime:app:testNskDebugUnitTest -x lint`

### Genymotion / instrumentation status (green)

On the current emulator device (serial `localhost:35329`), running:

- `TEST_BUILD_TYPE=debug GRADLE_USER_HOME=/mnt/finished/.gradle ./gradlew :ime:app:connectedNskDebugAndroidTest -x lint`

…now passes:

- Result: `tests=35 failures=0 errors=0 skipped=4`
- Report: `ime/app/build/reports/androidTests/connected/debug/flavors/nsk/index.html`
- XML: `ime/app/build/outputs/androidTest-results/connected/debug/flavors/nsk/TEST-penandpdf-local - 16-_ime_app-nsk.xml`

Also note:

- `:ime:app:connectedAndroidTest` currently tries to run **multiple flavors** (including `askCompatRelease`), and can fail on
  emulators where both packages are installed because both declare the internal test permission
  (`INSTALL_FAILED_DUPLICATE_PERMISSION`). Prefer targeted tasks like `connectedNskDebugAndroidTest` for smoke.
- If you have signing env vars set, Gradle may default `testBuildType` to `release` and you won’t get `*DebugAndroidTest`
  tasks. Always pass `TEST_BUILD_TYPE=debug` for emulator smoke.

## 1) Blockers (must fix before next release)

### 1.1) Settings crash: “Background photo (current theme)”

**User-facing symptom**

- Opening/selecting a background photo in:
  - Settings → Look & Feel → Theme tweaks → **Background photo (current theme)**
  - causes the app to crash (real device report).

**Expected**

- User selects an image → preview updates → Apply persists selection → keyboard shows the photo according to the selected mode.

**Actual**

- Crash while selecting (and in earlier emulator runs: UI became unresponsive / input dispatch timeout).

**Immediate goals**

- (A) Reproduce with a real Java stack trace (`AndroidRuntime FATAL EXCEPTION`).
- (B) Fix the crash/ANR **without changing add-on behavior** and without requiring network.

### 1.2) Crash when changing “Wallpaper mode”

**User-facing symptom**

- After importing a background photo and changing **Wallpaper mode**, the keyboard may crash even though the
  wallpaper effect appears to apply.

**Hypothesis**

- Some devices/renderers can crash or OOM when using `Canvas.saveLayer(...)` + PorterDuff `DST_IN` masking in the
  hot draw loop (particularly with per-key masked overlays).

**Immediate mitigation (implemented)**

- `KeyDrawHelper.drawKeyTextureOverlayWithMask(...)` now wraps the masked-overlay path in a defensive
  `try/catch(RuntimeException | OutOfMemoryError)` and falls back to the simpler rounded-rect overlay instead of
  crashing the IME. (Shipped in `13.8.53` / `15104`.)

**Data we need (highest priority)**

- Crash report stack trace from the crash dialog (“SEND”) or `adb logcat` from the device during the crash.
- If reproducing via adb:
  - confirm build type (release/debug) and versionName/versionCode.

**Likely root-cause candidates (to confirm, not assumptions)**

- **Fragment lifecycle / “not attached” crash while the picker is open**
  - `KeyboardThemeCustomizationFragment.onPhotoPicked(...)` calls `requireContext()` and `getCurrentTheme()`.
  - If the fragment is detached (rotation, app backgrounded, user navigates away while picker is open),
    this can throw `IllegalStateException: Fragment not attached to a context` on return.
- **Main-thread theme resolution** while building the customization UI
  - `KeyboardThemeCustomizationFragment.refreshState()` calls
    `NskApplicationBase.getKeyboardThemeFactory(requireContext()).getEnabledAddOn()` on the main thread.
  - If theme enabling / add-on resolution performs disk IO or lazy parsing, it can cause ANRs.
- **Wallpaper import edge cases** (already off-main-thread, but still can throw)
  - `KeyboardWallpaperOverrideStore.importFromUri(...)` can throw `SecurityException` / `IOException` / OOM-wrapped `IOException`.
- **URI/permission edge cases**:
  - `SecurityException` if we lose persisted URI permissions or request wrong flags.
  - `IllegalArgumentException` if the picker returns a URI we don’t handle.

**Fix plan (ordered)**

1. Capture the stack trace and classify failure type:
   - Crash (`FATAL EXCEPTION`) vs ANR (input dispatch timeout).
2. If crash:
   - fix the concrete exception (nulls/URI permissions/incorrect assumptions).
   - specifically verify whether this is the “fragment not attached” case and add a guard if so.
3. If ANR:
   - move any add-on/theme resolution and any wallpaper import work off the main thread.
   - show a small progress state (disable the row or show “Loading…”), but keep the UX lightweight.
4. Add regression coverage:
   - Robolectric test for the store/import path (already exists),
   - plus a focused instrumentation “open customization screen” test to ensure it doesn’t ANR/crash.

**Verification (must pass)**

- Manual on real device:
  - Select image → Apply → keyboard background visibly changes for Mike Rozoff theme.
  - Back out/in, rotate, reopen settings: state persists and no crash.
- Emulator (Genymotion):
  - Navigate to the same setting without freezing.

**Implementation status**

- ✅ Added lifecycle-safe context access in the settings fragment to prevent “not attached” crashes when the picker returns.
- ✅ Added an instrumentation regression test covering the pick flow:
  - `KeyboardThemeCustomizationFragmentTest.pickingWallpaperDoesNotCrashAndMarksPhotoAsSet`
- ✅ Added a migration so first import defaults to a visible mode even if an older build stored `BACKGROUND_ONLY`.
- ⬜ Still required: reproduce the original crash on a real device or via logcat and confirm it’s gone with this change.

---

### 1.2) Spacebar does not return to the intended “keyboard name” / base state

**User-facing symptom**

- After voice input states (Recording/Waiting/Error), the spacebar does **not** return to its normal behavior:
  - it should show the authored spacebar visuals (label/icon),
  - or (when enabled) the **keyboard name on the spacebar**.

User feedback: the original “keyboard name on spacebar” behavior felt **hard-coded** and should instead be treated as a
principled “spacebar status line” mechanism (single owned place where we can surface status without breaking themes).

**Expected**

- When voice state becomes `IDLE`, the spacebar rendering is indistinguishable from “no voice ever happened”.

**Actual**

- Fonts are now consistent (good), but the base spacebar display does not restore correctly (bad).

**What we must not do**

- No “ignore duplicates” / “hide labels” hacks.
- No theme-breaking label mutation that prevents spacebar icons or keyboard-name fallback from working.

**Likely root-cause candidates (to confirm)**

- The voice overlay state is not resetting to `IDLE` (or the view is not invalidated).
- The base spacebar keyboard-name fallback is no longer reachable because:
  - the label is treated as non-empty even when it should be empty, or
  - the theme-derived `keyboardNameTextSize` resolves to `0` for add-on themes, so name is never drawn even when enabled.
  - `DrawInputsBuilder` gates keyboard-name substitution on `keyboardNameTextSize > 1f`, so missing/zero theme values fully disable it.
- Rendering precedence regression:
  - key-code icons only draw when label is empty; if we accidentally set a label, icons disappear.
- **Mismatch between code and intended precedence**
  - `KeyIconDrawer` returns `null` to prevent label drawing when an icon is drawn, but
    `KeyDrawHelper` currently applies keyboard-name substitution _after_ icon drawing and will
    draw the keyboard name even when an icon was drawn.
  - Decided contract: when “Show keyboard name” is enabled, the keyboard name may render on the
    **spacebar** even when an icon is drawn (common IME UX); non-space keys keep the “icon suppresses
    label” behavior.

**Fix plan (ordered)**

1. Reproduce on device with a deterministic sequence:
   - enable “Show keyboard name”
   - start voice → stop voice → observe spacebar in idle state
   - repeat while switching sub-keyboards (Mike Rozoff) to catch stale state.
2. Confirm rendering pipeline invariants on idle:
   - spacebar base label/icon/name resolution runs exactly as before voice was engaged.
3. Ensure voice UI never mutates `KeyboardKey` labels/icons (draw-time overlay only).
4. If keyboard-name fallback is blocked by missing theme values:
   - define a safe fallback sizing rule so add-on themes that omit `keyboardNameTextSize` still show it when enabled
     (e.g., derive from `keyTextSize`).
5. Enforce the precedence contract:
   - spacebar may show keyboard name even with an icon when enabled,
   - non-space keys: icon suppresses text.
6. Add a small host-side regression test:
   - “voice state returns to IDLE → base spacebar label/icon/name is unchanged”.

**Verification (must pass)**

- Mike Rozoff theme:
  - Keyboard name appears when setting is enabled (and disappears when disabled).
  - Voice record/transcribe/error does not permanently change spacebar visuals.
- Other add-ons:
  - At least one ASK add-on theme that uses spacebar icons still renders correctly.

**Implementation status**

- ✅ Keyboard name substitution now treats whitespace-only labels as empty, so add-on layouts using `" "` don’t block it.
- ✅ Keyboard name substitution now shows the keyboard name on the spacebar even when an icon is drawn (when the setting is enabled).
- ✅ Dismissing the voice error strip now forces `ERROR → IDLE` (and is covered by:
  - `ImeVoiceInputCallbacksTest.testDismissingErrorStripClearsErrorStateToIdle`)
- ⬜ Still required: verify on device that voice `RECORDING/WAITING/ERROR → IDLE` restores the expected base spacebar visuals.

## 2) Non-blocking improvements (after blockers are fixed)

### 2.1) “Spacebar status line” (principled, extensible design)

We should treat the spacebar as an **owned status surface** with a stable contract:

- Base visuals (theme/add-on intent) always render first.
- NSK-owned overlays render last and never change the key model.
- Overlays are capability-driven (hide/compact when the key is too narrow).

This is already partially implemented via the voice badge overlay, but we should generalize the concept so we can add
future statuses (downloader progress, connectivity warnings, etc.) without ad-hoc hacks.

## 3) Collapsed notes from `feedback-plan.md` (kept for reference)

### 3.1) Voice STT feedback — correctness constraints

Non-negotiable constraints (must remain true after fixes):

- Voice feedback must not mutate the key model (`KeyboardKey.label/icon`) at runtime.
- `VoiceImeController` remains the single source of truth for voice UI states.
- Spacebar icon/label intent from add-on layouts/themes must keep working.
- Error actions must be non-modal first (strip/banner), dialog fallback only.

### 3.2) Wallpaper/photo theme — UX constraints

- The feature must appear to “work” even on fully-opaque themes (defaulting to a visible mode on import is good).
- Import must be resilient:
  - never crash on bad URIs,
  - handle large images (downscale + OOM handling),
  - keep UI responsive.

### 3.3) Source pointers (where to look first)

- Wallpaper settings UI:
  - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ui/settings/KeyboardThemeCustomizationFragment.java`
- Wallpaper storage + defaults:
  - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/theme/KeyboardWallpaperOverrideStore.java`
- Wallpaper application (IME container background):
  - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeThemeOverlay.java`
- Spacebar rendering pipeline:
  - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/KeyDrawHelper.java`
  - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/KeyIconDrawer.java`
  - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/KeyboardNameRenderer.java`
  - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/KeyLabelRenderer.java`
- Voice state machine + UI glue:
  - `ime/voiceime/src/main/java/com/google/android/voiceime/VoiceImeController.java`
  - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/VoiceStatusRenderer.java`
  - `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/VoiceStatusBadgeState.java`

## 4) Pointers

- Compatibility constraints live in: `docs/compatibility-checklist.md`
- Architecture refactor roadmap lives in: `plan.md`
