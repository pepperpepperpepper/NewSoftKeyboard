# Suggestions Integration Progress

Last updated: November 15, 2025

## Presage Native Bring-Up
- CMake builds vendored Presage 0.9.1 into a static lib; JNI bridge (`presage_bridge.cpp`) exposes open/close/score/predict and compiles with modern NDK toolchains (`config.h`/`dirs.h`).
- Runtime loads Presage models from `no_backup/presage/models/<model-id>` using per-model manifests (SHA‑256 validation) and generates a Presage XML profile pointing to the selected model.

## ASK App Integration
- `PresagePredictionManager` activates/deactivates the native session and routes predictions into `SuggestionsProvider` when the engine mode is `ngram` or `hybrid`.
- Preference toggle exists in settings; app default is `none`.

## Verification
- Module assembles: `./gradlew :ime:suggestions:presage:assembleDebug` (NDK build).
- Unit: `SuggestionsProviderPresageTest` (Robolectric) validates staging and JNI contract (via shadow) and asserts checksums are recorded.

## Assets
- LM assets are not versioned. Install them via ADB/Download into `no_backup/presage/models/<model-id>` (manifests + binaries) or, for developer builds, use `scripts/sync_suggestion_models.sh` to bundle a bootstrap asset.
- Catalog now publishes the LibriSpeech KenLM 3‑gram, the optional VariKN “Sherlock Holmes 3‑gram,” and the TinyLlama 1.1B (INT4) neural bundle; downloading/activating any entry restages `presage_ngram.xml` automatically.

## Neural Path Status
- ONNX Runtime Mobile path is wired up. `NeuralPredictionManager` dynamically inspects the model’s inputs (`input_ids`, optional `position_ids`, `past_key_values`), materializes empty caches (handling `-1` dimensions), feeds context tokens through the ONNX session, and surfaces the top‑logit tokens back into `SuggestionsProvider`.
- DistilGPT‑2 (default) and TinyLlama 1.1B (optional) bundles are hosted via the catalog; instrumentation (`NeuralPredictionManagerTest`) downloads each model on Genymotion, activates them, and asserts predictions are returned.
- Tokenizer: `Gpt2Tokenizer` now exposes helper APIs (`getVocabSize`, `isSpecialToken`, raw token strings) and tolerates SentencePiece-style tokens (`▁`). Token decoding strips the byte fallback and replaces U+2581 with spaces.
- Current polish TODOs: normalise casing/punctuation before presenting suggestions, evaluate hybrid scheduling, and improve hit-rate metrics.

## Follow-Ups
- On-device instrumentation for latency/quality; add telemetry.
- Golden-trace replay for regression checks; optional cascade tuning.
