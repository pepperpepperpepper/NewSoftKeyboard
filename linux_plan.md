# NewSoftKeyboard on Linux (Desktop‑Agnostic Touch Keyboard) — Plan (Minimize Double Work)

This plan is intentionally written to **avoid “two keyboards”** (Android vs Linux) by making:

- a **single source of truth** for layouts/themes/state-machine logic,
- **thin platform adapters** for UI + text commit,
- and **contract tests** that prevent platform drift.

## 0) Reality check (the big trap): Wayland focus vs a standalone OSK

### Repo-specific starting point (what we already have)

This repo already contains several pieces we should **reuse** rather than rewrite:

- **Layout/theme ecosystem:** ASK-style XML keyboards/themes (today: APK add-ons on Android).
- **Engine direction:** `engine-core`, `engine-presage`, `engine-neural` already push us toward “engine adapters behind interfaces”.
- **Compatibility layer:** `compat-ask` exists specifically to keep AnySoftKeyboard add-ons working while we evolve NewSoftKeyboard.

What we **do not** yet have (and must introduce carefully to avoid “two keyboards”):

- A **platform-agnostic keyboard core** (layout model + state machine + semantic output actions) shared by Android + Linux.
- A **pack format + pack loader** that works without Android resources and is usable by both platforms.

On Wayland, a normal “keyboard window” typically **takes focus when you tap it**, which means:

- the focused app is no longer focused, so
- key events you emit (including via `/dev/uinput`) may go to the keyboard itself or the wrong surface.

So “standalone app window + uinput works everywhere on Wayland” is not a safe assumption. If we want to stay
desktop-agnostic _and_ avoid double work, we need to explicitly choose a **presentation/activation strategy** _and_
a **commit strategy** up front.

## 1) Goal (what “Linux support” means here)

Build a **Linux touch on-screen keyboard** which:

- reuses **the same layout/theme formats** as NewSoftKeyboard (ASK-style XML),
- installs content as **packs** (files on disk, not APK add-ons),
- and can type into arbitrary apps (Wayland + X11) using a platform backend.

**Clarification for this repo:** Linux support must not fork layouts/themes. Linux must consume the _same_ keyboard/theme content
we ship for Android (either directly via packs, or via deterministic export tools during a transition).

## 2) Non-goals (for first Linux milestone)

- Perfect feature parity with Android (gesture typing, full popups, previews, etc.).
- Supporting every compositor/DE-specific protocol on day one.
- Copying Android’s `InputConnection` semantics exactly (Linux often cannot provide that without an IM framework).

## 3) Decision gates (choose early to prevent double work)

### Proposed default choices for NewSoftKeyboard (so we don’t drift)

Unless we intentionally decide otherwise, treat these as the “default path”:

- **Gate A0:** A0.1 (compositor-managed) as the primary; A0.3 as a developer/debug mode.
- **Gate A:** A1 (IBus/Fcitx-style IM integration) as the primary; A2/A3 optional fallbacks.
- **Gate B:** JVM core as the canonical runtime. Keep Linux host JVM-based _unless_ Gate B0 fails.
- **Gate C:** C1 (packs-in-repo) as the long-term target; C2 only as a time-boxed transition.

### Gate A0 — Presentation + activation strategy (Wayland makes this explicit)

This gate decides **how the OSK is shown/managed** without breaking focus or fighting the compositor’s policies.

**A0.1 Compositor-managed virtual keyboard (recommended for “desktop-agnostic Wayland”)**

- Register as a “virtual keyboard” in environments that support it and let the compositor launch/manage the OSK.
- Pros: compositor controls focus/activation; fewer hacks.
- Cons: varies by desktop; requires integration hooks (e.g., desktop entry metadata / portal-like patterns).

**A0.2 Layer/overlay surface (wlroots-ish / layer-shell style)**

- Present as an overlay surface intended for shell components.
- Pros: OSK-like placement/behavior can be more natural than a normal window.
- Cons: still compositor/protocol variability; UI toolkit support may be limited.

**A0.3 Normal application window (X11-first or “best effort Wayland”)**

- Treat the keyboard as a standard window.
- Pros: easiest to prototype UI.
- Cons: on Wayland, likely to hit the focus trap; may only work reliably with IM-managed commit.

**Recommendation if “desktop-agnostic Wayland” is a hard requirement:** start with **A0.1** and keep **A0.3** as a dev/debug mode.

---

### Gate A — Text commit strategy (pick one “primary”)

This gate decides **how the OSK delivers text** to the focused target app.

**A1. IM framework integration (recommended for “desktop-agnostic” Wayland)**

- Implement the Linux host as an **IBus/Fcitx** input method module (or a UI that talks to one).
- Pros: focus + commit handled by the ecosystem; can support preedit/surrounding text; closer to Android semantics.
- Cons: depends on IM framework availability/config; packaging complexity; may require compositor-managed activation (see Gate A0).

**A2. Wayland virtual keyboard protocol (fallback, less semantic)**

- Emit key events via “virtual keyboard” mechanisms on Wayland.
- Pros: can work without full IM semantics.
- Cons: limited surrounding text/preedit features; compositor support variability; still must solve A0 well.

**A3. `/dev/uinput` / X11 injection (X11-first)**

- Treat Wayland support as “best effort” until Gate A changes.
- Pros: simplest to get typing working on X11 quickly.
- Cons: Wayland focus remains the blocker; “desktop-agnostic” becomes weaker.

**Recommendation:** if “desktop-agnostic Wayland” is a hard requirement: start with **A1**, and keep **A2/A3** as optional fallback modes.

---

### Gate B — Runtime alignment (avoid rewriting the core twice)

To avoid duplicating logic:

- Keep the **portable core** in a **JVM module** (pure Java/Kotlin, no `android.*`) so both Android and Linux can call it.
- Linux UI should preferably be **JVM-based** as well (so it can call the core without JNI/FFI).

If we choose a C++/Rust Linux UI/toolkit, we must budget for an FFI boundary and treat it as an explicit long-term cost.

#### Gate B0 — UI toolkit reality check (Wayland OSK surfaces are not “normal windows”)

Before committing to a JVM UI toolkit for Linux, confirm it can support the chosen **Gate A0** strategy (compositor-managed OSK, layer/overlay surfaces, etc.).

If the toolkit can’t do OSK-grade Wayland surfaces, keep the **core JVM**, but consider:

- Native UI host + **thin IPC boundary** to the JVM core (JSON-RPC over stdin/stdout or a local socket),
- still one source of truth for logic, still contract tests on the JVM, no per-platform forks in the core.

---

### Gate C — “Single source of truth” for layouts/themes

To prevent maintaining two versions of keyboards/themes:

**C1. Packs in-repo are canonical (recommended long-term)**

- Keyboard/theme XML + icons live under `packs/` in this repo.
- Android build consumes these packs (embed as assets or transform at build time).
- Linux reads them directly.

**C2. Android resources are canonical (acceptable for a short transition)**

- Export Android resources → packs for Linux.
- Avoid adding Linux-only pack features (or you’ll fork the format).

If we want to minimize double work beyond the first prototype, aim for **C1**.

## 4) Architecture (ownership boundaries)

### A. Platform‑agnostic core (single source of truth)

Create/extend a core that owns **all keyboard logic**:

- Layout parsing + keyboard model (rows/keys/popup keys/codes/labels/hints).
- Theme model (colors/paddings/icon references).
- Per-session state machine (shift/alt/fn/caps, repeat, long-press selection).
- Candidate strip model + prediction interfaces (aligned with existing `engine-core` direction).

Hard rule: core must not depend on `android.*`, `androidx.*`, or Android resources.

**Important refinement (prevents Linux/Android drift):**

- Core must output **semantic editor actions**, not “raw platform events”.
- The platform decides how to realize actions (InputConnection vs IM commit vs virtual keyboard vs uinput).

Example core output actions:

- `CommitText("a")`
- `CommitPreedit(text, selection)` (optional, if backend supports)
- `DeleteBackward(count=1)`
- `DeleteForward(count=1)` (optional)
- `PerformEditorAction(Enter|Tab|Next|Done)`
- `SendKeyEvent(key=..., modifiers=...)` (rare escape hatch)

### B. Platform adapters (thin)

Adapters provide “the world” to the core via explicit interfaces:

- `PackSource` (list packs, read files, resolve assets)
- `PrefsStore`
- `TextContext` (optional: surrounding text / cursor if available)
- `TextOutputBackend` (**consumes semantic actions** and implements them on the platform)
- `Clock`, `Logger`

Android adapter:

- Implements these using `InputMethodService` + `InputConnection` + APK add-on discovery.

Linux adapter:

- Implements these using filesystem packs + chosen Gate A backend:
  - A1: IM framework integration backend
  - A2: Wayland virtual-keyboard backend
  - A3: X11/uinput backend

### C. UI front-ends (hosts)

Linux host owns:

- rendering + touch input + window/surface model (**must follow Gate A0**),
- show/hide behavior,
- routing touch → core events,
- calling the chosen `TextOutputBackend`.

Android host stays Android (no regression allowed for add-on compatibility).

## 5) Pack format (layouts/themes/icons as files)

We keep ASK/NSK’s **XML grammar** as the portable language, but we replace resource IDs with pack paths.

```
pack/
  manifest.json
  keyboards/
    <keyboard-id>.xml
  themes/
    <theme-id>.xml
  icons/
    *.png (and/or *.svg if supported)
```

`manifest.json` should include:

- `id`, `name`, `version`
- `minCoreVersion` (or schema version) to evolve safely
- `keyboards`: list of keyboard xml entries
- `themes`: list of theme entries
- optional `assets` hashes for reproducibility

### Avoiding double work in packs

- Prefer **one supported subset** of XML that both Android and Linux render identically.
- Add **pack contract tests** (below) so we don’t create “Linux-only” behavior.

## 6) Tooling (avoid duplicating content)

### Tool 1 — Export built-ins → pack (transition helper)

If Gate C2 is chosen temporarily:

- Export Android’s built-in keyboards/themes → a deterministic pack.
- Resolve `@drawable/...` to actual icon files.

### Tool 2 — Convert ASK add-on APK → pack (optional but high leverage)

This allows installing Mike Rozoff and other add-ons on Linux without changing their sources:

- unzip APK → extract keyboard/theme XML + images → normalize into `pack/`.
- deterministic resource mapping (debuggable output).

This should be a CLI/script outside the Android runtime (no new app deps).

## 7) Contract tests (the “anti-double-work” safety net)

Add core-level tests that run on the host JVM and are shared by both platforms:

- **Parse golden tests**: XML → model matches expected (keys, widths, popups).
- **Theme resolution tests**: same theme yields same colors/icons paths.
- **State machine tests**: shift/caps/alt/fn/repeat/long-press transitions.
- **Hit-test tests**: touch coordinate → key selection is stable for a given layout.

**Add one end-to-end golden test (high leverage):**

- **Key sequence → semantic output log**: simulate touches/keys, record emitted semantic actions (`CommitText`, `DeleteBackward`, etc.), and compare to a golden file.
- This catches drift across platforms even when the underlying transport differs (Android `InputConnection` vs Linux IM vs virtual key events).

These tests are the main tool to prevent Linux and Android from drifting into two different keyboards.

### 7.1 Test matrix (Linux-first)

The goal is “fast feedback on Linux” without requiring a full Android emulator run for every change.

**Tier 0 — Core contract tests (must run on Linux without Android SDK)**

- Command (once `keyboard-core` exists): `./gradlew :keyboard-core:test`
- Scope: XML parsing, state machine, hit-testing, semantic action golden tests.
- Hard rule: module uses `java-library` (or equivalent) and has **zero** `android.*` dependencies.

**Tier 1 — Host JVM tests that currently use Android Gradle Plugin (Linux, but requires Android SDK)**

- Example: `./gradlew :engine-neural:test`
- Scope: host-only logic that still lives in `com.android.library` modules (transition state).
- Direction: over time, migrate portable pieces into `keyboard-core` so Tier 0 grows and Tier 1 shrinks.

**Tier 2 — Android unit tests (Robolectric)**

- Command: `./gradlew :ime:app:testNskDebugUnitTest -x lint`
- Scope: Android-only adapters and wiring that can be tested without a device.

**Tier 3 — Android instrumentation smoke (Genymotion / device)**

- Command: `./gradlew :ime:app:connectedNskDebugAndroidTest -x lint`
- Scope: end-to-end IME behavior, add-on compatibility, regressions that only show up on device/emulator.

**Tier 4 — Linux integration smoke (depends on Gate A/A0 choices)**

- Goal: “types into at least one real app” reliably.
- Implementation options:
  - If **A1 (IBus/Fcitx)**: smoke focuses on “IM module commits text correctly” + activation behavior.
  - If **A3 (X11/uinput)**: can be automated under X11 (optionally with `xvfb-run`) and documented as Wayland-limited.

## 8) Phased delivery plan (ordered to minimize rework)

### Phase 0 — Gates + interfaces + contract test harness (1–3 days)

- Choose Gate **A0** + **A** + **B** + **C** (even if temporarily).
- Define the adapter interfaces and add the contract test scaffolding.
- Define the **semantic action** output API from core → `TextOutputBackend`.

Acceptance:

- Core builds and contract tests run on the JVM without Android.

### Phase 1 — Packs + portable model + state machine (1–2 weeks)

- Implement pack loader + XML parser in core.
- Implement keyboard session state machine.
- Add/extend contract tests against a couple of representative keyboards/themes (including Mike Rozoff content via converted pack if possible).
- Add “key sequence → semantic output” golden tests for at least 1–2 layouts.

Acceptance:

- Core can load a pack and produce deterministic models and semantic output actions.

### Phase 2 — Linux host MVP (backend depends on Gate A + Gate A0) (1–3 weeks)

If Gate A1 (recommended):

- Build the minimal Linux host + IM framework integration for semantic commit.
- Ensure the OSK is launched/managed per Gate A0 so focus/activation works as intended.

If Gate A3 (X11-first):

- Build the Linux host + uinput backend for typing, explicitly documenting Wayland limitations.
- Still implement semantic output; the backend maps semantics → injected events.

Acceptance:

- Types reliably into at least one target stack (X11 and/or the chosen Wayland pathway),
- without platform-specific forks in core,
- and without changing pack formats per platform.

### Phase 3 — Themes/icons + candidate strip (1–2 weeks)

- Implement icon cache/resolution and candidate strip rendering in the Linux host.

Acceptance:

- “Mike Rozoff blue” looks correct (palette + icons) using the shared theme model.

### Phase 4 — Predictions (2–4 weeks)

- Keep prediction logic in portable modules.
- Provide platform-specific runtime adapters where unavoidable:
  - Neural: desktop ONNX runtime adapter on Linux (Android keeps `onnxruntime-android`).
  - Presage: build native for Linux and load it.

Acceptance:

- Predictions form a coherent chain without “duplicate filtering hacks”.

## 9) Definition of “desktop‑agnostic success”

“Success” must be defined per Gate A0 + Gate A:

- If A0.1 + A1: Works across common Wayland/X11 desktops **via the IM framework + compositor-managed activation** where supported.
- If A0.2 + A2: Works on compositors that support the required overlay + virtual keyboard pathways.
- If A0.3 + A3: Works on X11 reliably, and Wayland support is explicitly scoped (not promised as universal).

In all cases:

- One set of packs works across platforms.
- Contract tests protect cross-platform behavior.
- Core emits the same semantic actions across platforms; only adapters differ.

## 10) Concrete work items in this repo (to start Phase 0 cleanly)

The guide above is the “what/why”. This section is the “what we change in _this_ repo”.

### 10.1 Introduce a keyboard core module (portable, no Android deps)

Create a new JVM-only module (name TBD, e.g. `keyboard-core`):

- Owns: layout model, theme model (logical values), per-session state machine, hit-testing inputs, semantic output actions.
- Outputs: semantic editor actions (commit text, delete, editor action, optional preedit).
- Must not depend on: `android.*`, `androidx.*`, resource IDs, APK add-on classes.

**Android integration rule:** Android becomes a thin adapter that feeds touch events + prefs + pack sources into the core and translates
semantic actions into `InputConnection` calls. This is how we keep one source of truth for behavior.

### 10.2 Define “pack source” and “asset resolution” contracts

Add explicit interfaces (kept in the portable core) for:

- Listing installed packs.
- Reading a keyboard/theme XML file by pack-relative path.
- Resolving icons by pack-relative path (no `@drawable/...` in the core).

Android pack source can be backed by:

- Bundled packs in assets/resources (long-term once we reach Gate C1), and/or
- Legacy APK add-on extraction (transition tool, not core runtime logic).

Linux pack source is filesystem-based from day one.

### 10.3 Contract test harness (host JVM)

Add host JVM tests that are shared by both platforms:

- Parse golden tests (XML → model).
- State-machine tests (modifier transitions, long-press behaviors).
- Hit-test tests (touch → key selection).
- End-to-end “key sequence → semantic action log” golden.

### 10.4 Time-boxed transition tool(s) (if we aren’t ready for packs-in-repo immediately)

If we choose Gate C2 temporarily, add tooling under `tools/` or `scripts/` to:

- Export built-in Android keyboards/themes into a deterministic pack.
- Convert an ASK add-on APK into a pack (to reuse Mike Rozoff and other add-ons on Linux without source changes).

**Hard rule:** these are build/dev tools, not Android runtime dependencies.

## 11) Task list (updated)

### 11.1 Decision gates (lock these before building UI)

- [x] Gate A0 decision: primary (A0.1) + dev mode (A0.3) documented (2025-12-28)
  - Primary: compositor-managed OSK (A0.1). Dev/debug: normal app window (A0.3).
- [x] Gate A decision: primary commit strategy (A1) documented + whether we support A2/A3 fallback (2025-12-28)
  - Primary: IM framework integration (A1). Optional future fallback: virtual keyboard / uinput (A2/A3) where viable.
- [ ] Gate B0 check: confirm Linux UI/toolkit can satisfy A0; decide JVM UI vs native host + IPC
  - Pending: pick UI host strategy after a quick feasibility spike for Wayland OSK surfaces.
- [x] Gate C decision: commit to C1 (packs-in-repo) or time-box C2 (export tooling) with a deadline (2025-12-28)
  - Target: packs-in-repo (C1). Export tooling (C2) only as a short migration bridge, not a long-term fork.

### 11.2 Core + contract tests (Linux-first)

- [x] Add `keyboard-core` (JVM-only, `java-library`, no `android.*`) (2025-12-28)
- [x] Define semantic output actions and the core input event API (2025-12-28)
- [x] Add adapter interfaces (`PackSource`, `PrefsStore`, `TextContext?`, `TextOutputBackend`, `Clock`, `Logger`) (2025-12-28)
- [x] Add Tier 0 contract test harness (`:keyboard-core:test`) (2025-12-28)
- [x] Add at least one end-to-end golden (“key sequence → semantic action log”) (2025-12-28)

Implemented in:

- Module: `keyboard-core/`
- Parser: `keyboard-core/src/main/java/wtf/uhoh/newsoftkeyboard/keyboard/core/parser/AskXmlKeyboardParser.java`
- Session + semantic log golden: `keyboard-core/src/test/java/wtf/uhoh/newsoftkeyboard/keyboard/core/session/KeyboardSessionGoldenTest.java`
- Run: `GRADLE_USER_HOME=/mnt/finished/.gradle ./gradlew :keyboard-core:test`

### 11.3 Packs + parsers (single content source)

- [x] Define `manifest.json` schema + schema versioning rules (2025-12-28)
- [x] Implement filesystem pack loader (Linux) and use it in Tier 0 tests (2025-12-28)
- [x] Implement XML parsing subset → portable keyboard model (2025-12-28)
- [x] Implement theme parsing/model + icon path resolution (pack-relative, no Android resource IDs) (2025-12-28)

Implemented in:

- Pack manifest schema + strict schemaVersion gating:
  - `keyboard-core/src/main/java/wtf/uhoh/newsoftkeyboard/keyboard/core/packs/PackManifest.java`
  - `keyboard-core/src/main/java/wtf/uhoh/newsoftkeyboard/keyboard/core/packs/PackManifestJson.java`
- Filesystem pack loader:
  - `keyboard-core/src/main/java/wtf/uhoh/newsoftkeyboard/keyboard/core/packs/FileSystemKeyboardPackLoader.java`
  - `keyboard-core/src/main/java/wtf/uhoh/newsoftkeyboard/keyboard/core/io/FileSystemPackSource.java`
- Keyboard XML parsing subset:
  - `keyboard-core/src/main/java/wtf/uhoh/newsoftkeyboard/keyboard/core/parser/AskXmlKeyboardParser.java`
- Pack theme parsing:
  - `keyboard-core/src/main/java/wtf/uhoh/newsoftkeyboard/keyboard/core/theme/ThemeXmlParser.java`
  - `keyboard-core/src/main/java/wtf/uhoh/newsoftkeyboard/keyboard/core/theme/ThemeModel.java`
- Tier 0 pack fixture + loader test:
  - `keyboard-core/src/test/java/wtf/uhoh/newsoftkeyboard/keyboard/core/packs/FileSystemKeyboardPackLoaderTest.java`
  - `keyboard-core/src/test/resources/fixtures/packs/basic_pack/`

### 11.4 Transition tooling (only if Gate C2 is active)

- [ ] Export built-in Android keyboards/themes → deterministic pack output
  - Note: keyboard layouts are exportable today via `scripts/convert_apk_to_pack.py` against the built APK; theme conversion remains TODO.
- [x] Convert ASK add-on APK → pack (keyboard layouts only; theme/icons still TODO) (2025-12-28)
- [x] Add pack validator (required files, paths resolve, reproducible mapping) (2025-12-28)

Implemented in:

- Validator (manifest IDs, XML parse, icon existence checks):
  - `keyboard-core/src/main/java/wtf/uhoh/newsoftkeyboard/keyboard/core/packs/KeyboardPackValidator.java`
  - `keyboard-core/src/test/java/wtf/uhoh/newsoftkeyboard/keyboard/core/packs/KeyboardPackValidatorTest.java`

Available tooling:

- Source-tree → pack exporter (no Android runtime deps):
  - `scripts/export_keyboard_pack.py`
  - Example (export one language pack’s layouts):
    - `python3 scripts/export_keyboard_pack.py --source addons/languages/hebrew/pack/src/main/res/xml --output /tmp/nsk_hebrew_pack --with-default-theme --force`

- APK → pack exporter (uses `aapt2`, exports `res/xml/*` layouts whose root tag is `<Keyboard/>`):
  - `scripts/convert_apk_to_pack.py`
  - Example:
    - `python3 scripts/convert_apk_to_pack.py --apk /path/to/addon.apk --output /tmp/nsk_addon_pack --with-default-theme --force`

### 11.5 Linux host MVP (thin adapter)

- [x] Add a Linux dev host (A0.3 normal window) which loads a pack and routes clicks → core (2025-12-28)
- [x] Implement Linux `PrefsStore` (read-only) + pack install locations (2025-12-28)
- [ ] Implement `TextOutputBackend` for Gate A (A1 first)
  - [x] Dev backends: stdout (JSONL) + X11 `xdotool` (A3-ish) (2025-12-28)
- [ ] Implement Gate A0.1 activation (compositor-managed) for production
- [ ] Add Tier 4 smoke and document how to run it locally

Implemented in:

- Dev host module (normal app window): `linux-host/`
- Run the demo against the fixture pack:
  - `GRADLE_USER_HOME=/mnt/finished/.gradle ./gradlew :linux-host:run --args="keyboard-core/src/test/resources/fixtures/packs/basic_pack"`
- Linux pack repository + install/list commands (XDG + env overrides):
  - `linux-host/src/main/java/wtf/uhoh/newsoftkeyboard/linuxhost/packs/LinuxPackRepository.java`
  - `linux-host/src/main/java/wtf/uhoh/newsoftkeyboard/linuxhost/fs/XdgPaths.java`
  - `NSK_PACKS_DIR`, `NSK_PREFS_FILE`
- Linux prefs (read-only file store):
  - `linux-host/src/main/java/wtf/uhoh/newsoftkeyboard/linuxhost/prefs/FilePrefsStore.java`
  - Keys: `output.mode`, `xdotool.window`, `xdotool.delay_ms`
- Dev output backends:
  - `linux-host/src/main/java/wtf/uhoh/newsoftkeyboard/linuxhost/output/StdoutJsonTextOutputBackend.java`
  - `linux-host/src/main/java/wtf/uhoh/newsoftkeyboard/linuxhost/output/XdotoolTextOutputBackend.java`
- Headless smoke (no UI) which runs the session and emits actions:
  - `./gradlew :linux-host:run --args="--smoke keyboard-core/src/test/resources/fixtures/packs/basic_pack --text=abc --output=stdout"`
