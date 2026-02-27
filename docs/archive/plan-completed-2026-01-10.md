# Archived sections from `plan.md` (completed)

Moved on: 2026-01-10

This file contains sections removed from `plan.md` because they are completed/historical. The canonical owners and
architecture rules still live in `plan.md`.

---

## Immediate Focus: Clear Ownership Conflicts (next refactor slices)

These are the highest-impact “ownership unclear” problems right now. This plan is biased toward making ownership
obvious and shrinking `:ime:app` into a thin shell.

1. **Next-word orchestration/pipeline is split** (done 2025-12-19)
   - Previous symptom: `:ime:nextword` existed (legacy next-word dictionary) but engine-agnostic candidate pipeline code lived under
     `:ime:app` (`wtf.uhoh.newsoftkeyboard.pipeline.*`).
   - Decision (this plan): `:ime:nextword` owns **all engine-agnostic next-word suggestion plumbing** (legacy nextword + candidate
     normalization/merging/orchestration helpers).
   - Done (2025-12-19): moved next-word prediction orchestration out of `:ime:app` into `:ime:nextword`:
     - Engine-agnostic pipeline helpers live in `wtf.uhoh.newsoftkeyboard.nextword.pipeline`.
     - Engine wiring/state lives in `wtf.uhoh.newsoftkeyboard.nextword.prediction.NextWordPredictionEngines`.
   - Definition of done:
     - No `wtf.uhoh.newsoftkeyboard.pipeline.*` classes remain in `:ime:app`.
     - `:ime:nextword` exposes the minimal “candidate pipeline” API used by `SuggestionsProvider`.
     - No new cross-cutting `*Utils/*Helper` packages were created to make this compile.

2. **Presage is split across two modules without a crisp contract** (done 2025-12-20)
   - Symptom: Java Presage engine/model code lives in `:engine-presage`, but native build/vendor details live in
     `:ime:suggestions:presage`, and it’s not always clear where fixes belong.
   - Decision (this plan):
     - `:ime:suggestions:presage` owns _only_ the native binding boundary (JNI/CMake/vendor staging mechanics).
     - `:engine-presage` owns the Presage engine behavior (Java API, model store/downloader/selection/policies).
     - `:ime:app` owns settings/UI only (no Presage policy).
   - Done (2025-12-20): moved Presage vendor staging (`scripts/setup_presage.sh` + `third_party/presage`) to
     `:ime:suggestions:presage` so `:engine-presage` contains no vendor/CMake knowledge.
   - Action: document the contract in `FDROID_PUBLISHING.md`/`BUILDING.md` if needed, but more importantly enforce it in code:
     no Java policy leaking into `:ime:suggestions:presage`, no CMake/vendor knowledge leaking into `:engine-presage`.
   - Definition of done:
     - `:engine-presage` can be reasoned about without opening CMake/vendor sources.
     - `:ime:suggestions:presage` contains no Java-side policy decisions (no URLs/settings/model selection).
     - The only integration between them is a narrow API boundary (JNI surface + outputs).

3. **`SuggestionsProvider` is still a “god integrator”** (done 2025-12-20)
   - Symptom: It mixes legacy nextword dictionaries, engine predictions, normalization, merging, and fallback decisions.
   - Decision (this plan):
     - `SuggestionsProvider` becomes a thin adapter: collect context tokens + call into a single owned orchestrator.
     - The orchestrator/pipeline lives outside `:ime:app` (owned by `:ime:nextword`) and talks to engines only via `:engine-core`.
   - Done (2025-12-20): moved `getNextWords` orchestration into `:ime:nextword`
     (`wtf.uhoh.newsoftkeyboard.nextword.pipeline.NextWordSuggestionsPipeline`).
   - Action: slice-by-slice extraction until `SuggestionsProvider` is basically “prepare inputs + apply outputs”.
   - Definition of done:
     - Normalization/merging/fallback decisions live in the pipeline owner (not inside `SuggestionsProvider`).
     - `SuggestionsProvider` reads like wiring code: context in → call orchestrator → apply candidates to holder.
   - Done (2025-12-20): `SuggestionsProvider#getNextWords(...)` is a thin adapter and delegates all pipeline decisions to
     `NextWordSuggestionsPipeline`.

4. **Model installation/selection state is misowned** (done 2025-12-20)
   - Symptom (pre-fix): shared model state was implemented as `PresageModel*` types used by both Presage and Neural, implying Presage
     “owned” shared model state.
   - Decision (this plan): introduce a shared “models” owner (candidate module: `:engine-models`) which owns:
     - selection state per engine type
     - staged file layout + manifest parsing
     - download/verify policy (SHA-256)
   - Done (2025-12-20): introduced `:engine-models` and moved the shared model store/downloader/selection code there, removing the
     `:engine-neural` → `:engine-presage` dependency edge.
   - Done (2025-12-20): renamed `PresageModel*` → engine-agnostic `Model*` in `:engine-models` (`ModelStore`, `ModelDefinition`,
     `ModelDownloader`, `ModelFiles`) while keeping the on-device layout under `no_backup/presage/models` for compatibility.
   - Definition of done:
     - `:engine-neural` no longer depends on `:engine-presage` to reach shared model state.
     - The shared model owner has no Presage- or Neural-specific policy.

5. **Voice input has no single controller** (completed 2025-12-20)
   - Symptom: state is spread across triggers/backends under `:ime:voiceime`.
   - Decision (this plan): create a single `VoiceImeController` that owns “active recording session + chosen backend + callback routing”.
   - Action: route all triggers through the controller; keep IME service interactions behind a narrow interface.
   - Definition of done:
     - A reader can find “who owns voice recording state” by opening one class.
   - Done (2025-12-20): introduced `com.google.android.voiceime.VoiceImeController` in `:ime:voiceime` and moved callback
     routing out of `:ime:app` (`VoiceInputController` deleted).
   - Done (2025-12-20): routed trigger usage through the controller (start recognition + lifecycle hooks) and moved UI reads
     off `VoiceRecognitionTrigger` (UI uses controller state).

6. **View code depends on IME runtime (reverse dependency)**
   - Status: **completed (2025-12-19)**
   - Previous symptom:
     - `wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateView` imported `ImeSuggestionsController` and stored a direct service reference (`mService`).
     - `KeyboardViewBase/KeyboardView/KeyboardViewContainerView` imported/implemented `InputViewBinder/InputViewActionsProvider` which lived under `wtf.uhoh.newsoftkeyboard.app.ime`.
   - Why this matters: it creates “reverse” edges where view/touch/render code depends on IME runtime, making both sides harder to split and inviting “helpers” that bridge layers.
   - Decision (this plan): views depend only on **view-owned contracts**.
     - A view may talk “up” only via a narrow `Host` interface owned by the view package (or a dedicated view-contract package), not by importing IME service types.
   - Done (2025-12-19):
     - Added `CandidateViewHost` and updated `CandidateView` to depend only on that contract.
     - Moved `InputViewBinder` and `InputViewActionsProvider` into `wtf.uhoh.newsoftkeyboard.app.keyboards.views`.
   - Definition of done:
     - No `wtf.uhoh.newsoftkeyboard.app.keyboards.views.*` class imports `wtf.uhoh.newsoftkeyboard.app.ime.*`.
     - `CandidateView` is testable with a fake host (no service dependency).

7. **NSK package hygiene: `wtf.uhoh.newsoftkeyboard.*` stays entrypoints-only** (done 2025-12-19)
   - Previous symptom: `wtf.uhoh.newsoftkeyboard.ime.*Host` contained IME/runtime wrapper implementations (not entrypoints).
   - Decision (this plan): `wtf.uhoh.newsoftkeyboard.*` is for branded entrypoints only (service/application/activity).
   - Action:
     - Done (2025-12-19): moved `wtf.uhoh.newsoftkeyboard.ime.*Host` → `wtf.uhoh.newsoftkeyboard.app.ime.hosts`.
     - Done (2025-12-19): moved `wtf.uhoh.newsoftkeyboard.pipeline.*` → `:ime:nextword` (Immediate Focus #1).
   - Definition of done:
     - `wtf.uhoh.newsoftkeyboard.*` contains only entrypoints (no “new logic lives here”).

## Performance + UX Focus: Keyboard Background Photo (2026-01-04)

This work keeps the **same** background-photo visual effect across the actual IME and all preview surfaces, but makes the
implementation cheap enough to use everywhere (especially in RecyclerView grids like “Manage keyboards” and “Themes”).

### Current findings (code-level)

- Photo wallpapers are decoded synchronously via `BitmapFactory.decodeFile(...)` in `KeyboardWallpaperResolver`, with no
  downsampling. This can run on the main thread during theme apply. (`ime/app/.../KeyboardWallpaperResolver.java`)
- Theme application calls into photo resolution during `ThemeAttributeLoaderRunner.applyThemeAttributes(...)`, so any view
  that applies themes during bind/layout can trigger photo decoding. (`ime/app/.../ThemeAttributeLoaderRunner.java`)
- Preview surfaces (eg, `DemoKeyboardView` in add-on browsers) create many keyboard views quickly; each view has its own
  `KeyboardWallpaperResolver` instances (background + key overlay), multiplying work. (`ime/app/.../DemoKeyboardView.java`,
  `ime/app/.../KeyboardViewBaseInitializer.java`)
- Key-face photo is drawn in the draw loop per key; “match key shape” can add per-key `saveLayer` + alpha-mask blending
  (cached masks, but still expensive per key). (`ime/app/.../KeyDrawHelper.java`,
  `ime/app/.../KeyBackgroundAlphaMaskCache.java`)

### Goals (non-negotiables)

- Previews must match the real wallpaper effect (no “fast preview” that changes appearance).
- No disk bitmap decoding on the main thread.
- Bounded memory usage (no unbounded bitmap growth across theme/keyboards browsing).
- No new third-party dependencies.

### Plan (phased; intended PR slices)

1. **Instrumentation + guardrails**
   - Add `Trace` sections around wallpaper decode, wallpaper apply, key overlay draw, and alpha-mask creation.
   - Add debug-only logging when a wallpaper decode happens on the main thread (should go to zero).

2. **Shared, size-aware wallpaper bitmap repository**
   - Introduce a process-wide `WallpaperBitmapRepository`:
     - `LruCache` keyed by `{themeId, fileLastModified, bucketedMaxDim}`.
     - In-flight request de-dupe (many views request → one decode runs).
     - Decode with `inJustDecodeBounds + inSampleSize` to downsample to view-appropriate sizes.
   - Use size bucketing (eg, round max-dimension to nearest 256px) to avoid cache fragmentation.

3. **Non-blocking wallpaper background apply**
   - Remove blocking photo decode from theme-apply paths (eg, `ThemeAttributeLoaderRunner`).
   - Apply a cheap placeholder immediately (theme wallpaper or default), then async-load the photo once view bounds are
     known; update background and invalidate when ready.
   - Share decoded `Bitmap`s across views; keep per-view `Drawable` instances (Drawable state must remain view-local).

4. **Key overlay algorithm: preserve effect, remove per-key heavy work**
   - Keep the existing “continuous photo anchored to keyboard bounds” look.
   - Replace the per-key masking path with a keyboard-sized cached mask and a single overlay pass:
     - Build (and cache) one `ALPHA_8` mask bitmap per keyboard layout + size that represents the union of key faces.
     - Draw overlay once per frame: `saveLayer` once → draw wallpaper shader once → apply mask once → restore.
   - Keep a safe fallback to the current per-key method if a renderer/device cannot handle the optimized path.
   - Maintain fast single-key invalidation behavior by using a hybrid strategy:
     - Use the optimized full-overlay path for full invalidations.
     - Use the current per-key path only when truly drawing a single key.

5. **Expand wallpaper transform options (scale/anchor/tiling)**
   - Add per-theme preferences:
     - Scale mode: center-crop (current), fit/contain, stretch, tile, mirror.
     - Anchor for crop/fit: 9-point grid (center/edges/corners).
   - Implement a generalized shader-matrix calculator (replacing the current center-crop-only logic) and reuse it for:
     - IME background drawable
     - Key-face overlay shader
     - Settings preview thumbnail
   - Update UI in `KeyboardThemeCustomizationFragment` and cover behavior with Robolectric tests.

### Acceptance criteria

- Opening and scrolling “Manage keyboards” and “Themes” does not trigger main-thread wallpaper decoding.
- Scrolling remains smooth with background photo enabled and key texture enabled.
- Memory stays bounded (cache size is capped; bitmaps are reused; no runaway allocations).
- The visual effect (continuous photo across the keyboard + keys) matches between previews and the actual IME.

### Implementation status (2026-01-04)

- Done: steps 1–5 implemented in `:ime:app`.
  - Async, shared decode/cache: `WallpaperBitmapRepository`.
  - Non-blocking apply: `KeyboardWallpaperResolver.applyPhotoOverrideIfAnyAsync(...)` wired from `ImeThemeOverlay` and
    `ThemeAttributeLoaderRunner`.
  - Key texture optimization: `KeyDrawHelper` uses a cached keyboard-sized union mask for full redraws (single `saveLayer`
    - `DST_IN`), and falls back to per-key masking for single-key redraws.
  - Transform options + UI: `KeyboardWallpaperTransform`, `KeyboardWallpaperOverrideStore` + `KeyboardThemeCustomizationFragment`.

## Audit Snapshot (2025-12-20)

This is the current “facts on the ground” snapshot that informed the focus list above.

### Biggest production files (signal only)

Top offenders (excluding tests; `BaseCharactersTable.java` is data-only):

- `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/KeyboardViewBase.java` (~799 LOC)
- `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeServiceBase.java` (~645 LOC)
- `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/ImeSuggestionsController.java` (~704 LOC)
- `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/KeyboardSwitcher.java` (~620 LOC)
- `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/views/PointerTracker.java` (~423 LOC)
- `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/keyboards/KeyboardDefinition.java` (~472 LOC)

- Done (2025-12-20): extracted `KeyboardRowBase`/`KeyboardKeyBase` out of `Keyboard.java` (keeping `Keyboard.Row/Key` wrappers)
  and moved `KeyDrawableStateProvider` to `wtf.uhoh.newsoftkeyboard.app.keyboards` (keyboard model no longer depends on `*.views` for key state).
- Done (2025-12-20): extracted `PointerKeySender` from `PointerTracker` so `PointerTracker` primarily owns per-pointer state
  transitions (down/move/up) and delegates “how to emit key events” to a single owned component.
- Done (2025-12-24): introduced `KeyboardDefinition` as the keyboard model owner; migrated internal call-sites + tests off the
  temporary `AnyKeyboard` shim, then deleted it.
- Done (2025-12-24): promoted `KeyboardKey` to the public key type and removed the nested `KeyboardDefinition.AnyKey`;
  migrated production + tests to use `KeyboardKey` directly.
- Done (2025-12-24): introduced `ImeServiceBase` as the core IME service owner; migrated internal call-sites away from the legacy
  `AnySoftKeyboard` type name, then deleted the shim.
- Done (2025-12-24): introduced `ImeSuggestionsController` as the suggestions lifecycle owner; migrated internal call-sites away from
  the legacy `AnySoftKeyboardSuggestions` type name, then deleted the shim.
- Done (2025-12-24): migrated internal collaborators to depend on owned `ImeServiceBase`/`ImeSuggestionsController` types instead of
  legacy names (and removed temporary legacy shims once migration was complete).

### Helper sprawl (actual inventory)

- Generic utility packages currently exist in two places:
  - Done (2025-12-20): moved app-level `wtf.uhoh.newsoftkeyboard.utils` files into `:ime:base` to eliminate the `:ime:app` utils dumping ground.
  - `ime/base/src/main/java/com/anysoftkeyboard/utils` (8 files:
    EmojiUtils/IMEUtil/LocaleTools/ModifierKeyState/Workarounds/XmlUtils/Triple/XmlWriter)
- Done (2025-12-20): reduced root-package helper sprawl by moving former helpers (`DeleteActionHelper`,
  `ModifierKeyEventHelper`, `SelectionEditHelper`, `SpecialWrapHelper`, `TerminalKeySender`) into `wtf.uhoh.newsoftkeyboard.app.ime`,
  and moving wiring hosts into `wtf.uhoh.newsoftkeyboard.app.ime.hosts` (e.g., `Ime*Host` wrappers).
- Done (2025-12-20): shrank `ImeServiceBase` by removing large anonymous action/callback implementations; host wrappers now
  accept callback value objects and are wired via method references.
- Done (2025-12-21): extracted `ImeServiceBase`’s `onCreate()` wiring into `ImeServiceInitializer` so the service is primarily
  a host/orchestrator and wiring changes don’t bloat the entrypoint file.
- Done (2025-12-21): extracted crash-handler wiring (RxJava + default uncaught handler + Chewbacca setup) into
  `wtf.uhoh.newsoftkeyboard.app.crash.CrashHandlerInstaller` so `NskApplicationBase` stays an entrypoint host.
- Naming scan (signal only): there are currently ~46 production files named `*Utils/*Util/*Helper` across modules.
  - Not all of these are “bad” (many are properly owned inside `wtf.uhoh.newsoftkeyboard.app.ime.*` or `wtf.uhoh.newsoftkeyboard.app.keyboards.views.*`),
    but they are a strong attractor for “helper sprawl”.
  - Rule: keep these helpers **owned and local** (package-private or nested), and never use their existence to justify adding a new
    cross-layer/generic helper.
- Done (2025-12-20): removed the empty helper attractor directory `ime/app/src/main/java/wtf/uhoh/newsoftkeyboard/app/ime/helpers`.

### Cross-layer edges (resolved)

- View layer importing IME runtime (resolved 2025-12-19):
  - `CandidateView → CandidateViewHost`
  - `InputViewBinder/InputViewActionsProvider` moved to `wtf.uhoh.newsoftkeyboard.app.keyboards.views`
- Keyboard model importing view types (resolved 2025-12-21):
  - `KeyboardSwitcher` now depends on `wtf.uhoh.newsoftkeyboard.app.keyboards.ThemedKeyboardDimensProvider` instead of `InputViewBinder`
    (keeps keyboard-switching code free of view-layer types).

### Dependency direction (spot checks)

- Engine modules do not import IME runtime types (good boundary hygiene):
  - `engine-core/engine-presage/engine-neural` contain no `import wtf.uhoh.newsoftkeyboard.app.ime.*` and no `import wtf.uhoh.newsoftkeyboard.app.ime.ImeServiceBase`.

### “App shell owns algorithms” (resolved)

- Done (2025-12-19): moved the engine-agnostic next-word pipeline out of `:ime:app` into `:ime:nextword` (`wtf.uhoh.newsoftkeyboard.nextword.pipeline`).

### “NSK package = entrypoints” (resolved)

- Done (2025-12-19): moved `wtf.uhoh.newsoftkeyboard.ime.*Host` → `wtf.uhoh.newsoftkeyboard.app.ime.hosts` so `wtf.uhoh.newsoftkeyboard.*` is entrypoints-only.

### “Dictionary ownership split” (note for later)

- `:ime:dictionaries` currently owns the base dictionary interfaces (`Dictionary`, `WordComposer`, loader, etc.).
- Many concrete dictionary implementations + orchestrators still live under `:ime:app` in `wtf.uhoh.newsoftkeyboard.app.dictionaries.*`.
- This is not necessarily “wrong”, but it must be made explicit (either via module rename like `:ime:dictionaries-core` or via migration) to avoid “where does dictionary logic live?” ambiguity.
