# NewSoftKeyboard – release notes

## 13.8.35 (code 15086)
- Refactored IME internals: extracted EditorStateTracker, InputConnectionRouter, and TouchDispatcher to shrink monolith classes.
- Prediction engines modularized: engine-core interfaces plus engine-presage and engine-neural adapters; app depends on modules only.
- Presage vendor staging and TinyXML fixes restore native builds; release builds now cover all ABIs.
- Neural host test hook added (downloads mixed-case model by default unless skipped); skips cleanly when ONNX JNI is unavailable.
- Suggestions pipeline now uses shared normalizer/merger helpers.
- Build/test docs consolidated in BUILDING.md; refactor roadmap lives in docs/newsoftkeyboard-refactor-plan.md.

## Earlier snapshots
- Mixed-case neural model published via catalog downloader (distilgpt2_mixedcase_sanity_v1).
- Catalog-driven model management UI with SHA-256 verification and per-engine activation.
- F-Droid packaging YAML generated under outputs/fdroid/ (not auto-published).
