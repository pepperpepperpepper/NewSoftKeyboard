# Retraining and Casing Normalization Plan

This workspace gathers the current language-model artifacts via symlink (see `kenlm/`, `distilgpt2/`) and outlines the plan for rebuilding them with proper casing. The goal is to produce mixed-case outputs natively so the keyboard can surface natural-looking suggestions without runtime heuristics.

## Goals
- Produce mixed/lowercase-friendly KenLM and VariKN n-gram models.
- Fine-tune or regenerate neural bundles (DistilGPT-2, TinyLlama) with casing preserved.
- Establish repeatable tooling so future bundles inherit the correct casing automatically.
- Validate that rebuilt models integrate with the existing Presage/ONNX pipelines and manifest format.

## Proposed Workstream

1. **Inventory & Baseline**
   - Capture checksums + metadata for every bundled/downloadable model (KenLM LibriSpeech, VariKN Sherlock, DistilGPT-2, TinyLlama).
   - Confirm current tokenizer configs (especially `lower_case` flags) so we understand why casing is lost.
   - Add integration logs proving predictions arrive in uppercase prior to UI rendering.

2. **Corpus Preparation**
   - Retrieve the original corpora or ARPA files for each n-gram bundle.
   - Normalize casing: either retain source mixed case or apply lowercase + sentence-case annotations.
   - For neural models, collect a mixed-case corpus representative of target usage (news/chat datasets, domain-specific text) and ensure tokenizers keep casing (disable `do_lower_case`).

3. **Retraining/Rebuilding**
   - **KenLM/VariKN**: regenerate ARPA models from normalized text; rebuild binaries (`build_binary`) and update vocab files. Record command lines in `retraining/scripts/kenlm_rebuild.sh`.
   - **Neural**: fine-tune DistilGPT-2 and TinyLlama on the prepared corpus (minimal epochs to restore casing). Export ONNX/quantized variants and tokenizer files; document hyperparameters and resulting perplexity.
   - Store outputs under `_artifacts/<model-id>/` before promoting to `~/suggestions/models`.

4. **Manifest & Packaging Updates**
   - Update each `manifest.json` to point at the regenerated files with new SHA-256 hashes.
   - Provide migration notes for downloader/catalog updates, including version bumps (e.g., `catalog.json?v=3`).

5. **Validation**
   - Add automated tests that assert casing behavior (e.g., `Hello` after `Good` context).
   - Measure latency/size deltas to ensure rebuilds stay within mobile budgets.
   - Manual Genymotion smoke tests for Presage + neural cascades.

6. **Rollout**
   - Publish new bundles to the model CDN/S3 bucket.
  - Document rollback strategy and user-facing changelog entries.

## Open Questions
- Do we want a hybrid runtime normalizer for edge cases (acronyms, ALL CAPS contexts) even after retraining?
- Should we maintain both mixed-case and lowercase variants for regression comparisons?
- Can we automate corpus licensing checks before publishing rebuilt models?

## Current Status
- Debug logging has been added to `SuggestionsProvider` so raw Presage/Neural predictions (and their contexts) are captured in debug builds.
- Retraining scaffolding now lives in `retraining/scripts/`, including `kenlm_rebuild.sh` (KenLM rebuild template), `normalize_corpus.py` (heuristic casing normalizer with acronym/titlecase controls), and `export_simple_enwiki.py` (parquet → text).
- The Simple English Wikipedia corpus was pulled to `/mnt/subtitled/datasets/simple_enwiki/simple_wikipedia` (83 MB parquet, SHA-256 `668d11a63e5c30f60e483b63b947edc2f2918f8d5c25857710abbdcf7e74b933`) and expanded to plain text (`simple_wikipedia.txt`, 136 MB, SHA-256 `e17bd651ea7f642a76893acdfd8b4b59d8f66a5b052cbbc3f32bfdf67e1e28ca`).
- `normalize_corpus.py` now restores acronyms using word boundaries (no more turning “daisy” into “dAIsy” when `AI` is listed) and accepts expanded titlecase/acronym sets. The latest command:
  ```
  python3 retraining/scripts/normalize_corpus.py \
    /mnt/subtitled/datasets/simple_enwiki/simple_wikipedia/simple_wikipedia.txt \
    --sentence-case \
    --titlecase January --titlecase February --titlecase March --titlecase April --titlecase May --titlecase June \
    --titlecase July --titlecase August --titlecase September --titlecase October --titlecase November --titlecase December \
    --titlecase Latin --titlecase Greek --titlecase Roman --titlecase Aphrodite --titlecase Zeus --titlecase Athena \
    --titlecase Apollo --titlecase Hermes --titlecase Hera --titlecase Poseidon --titlecase Demeter --titlecase Artemis \
    --titlecase Ares --titlecase Hephaestus --titlecase Dionysus --titlecase Cleopatra --titlecase Caesar --titlecase Pharos \
    --titlecase Athens --titlecase Sparta --titlecase Egypt --titlecase London --titlecase Paris --titlecase Rome \
    --titlecase Jupiter --titlecase Saturn --titlecase Neptune --titlecase Mercury --titlecase Venus --titlecase Earth --titlecase Mars \
    --acronym USA --acronym UK --acronym EU --acronym UN --acronym NATO --acronym USSR --acronym UNESCO \
    --acronym NASA --acronym WHO --acronym UNICEF --acronym LGBTQ --acronym AI \
    --output /mnt/subtitled/datasets/simple_enwiki/simple_wikipedia/simple_wikipedia.normalized.txt
  ```
  produced `simple_wikipedia.normalized.txt` (SHA-256 `6cb7988c0c9c248f1c11d254ab088b0f00b246b6228c16bfe5f03d2bcf3309e0`) and a refreshed 50 k-line sample (`simple_wikipedia_sample.normalized.txt`, SHA-256 `22a9d18bbfc4ad76ec1a7db43e0150533e1c50553dd72be40d5f7900b7a92c08`).
- KenLM was rebuilt via the updated `retraining/scripts/kenlm_rebuild.sh` (now supporting per-order prune schedules) using `PRUNE_SCHEDULE="0 1 1"`. The resulting artifacts live under `/mnt/subtitled/datasets/simple_enwiki/artifacts/kenlm_mixedcase/`:
  - ARPA (`model.arpa`, 124 MB, SHA-256 `03b6cc28926fa29f533f131ee465f8b948076833acf1a7138f93e0502d3324d5`)
  - Vocabulary (`model.vocab`, 28 MB, SHA-256 `0401f6ef10c5cb143ca36922746e90b89bfaf669bd98f29bdcb5190ee932805c`)
  - Binary (`model.binary`, 106 MB, SHA-256 `1578aca1844777a108fde62a4560cee763688891a408c9d0a210d5090fa446d0`)
- Added `retraining/scripts/neural_finetune.py`, a Hugging Face Trainer scaffold for mixed-case DistilGPT-2/TinyLlama fine-tunes, plus `retraining/scripts/neural_pipeline.sh`, which chains fine-tuning with optional ONNX + quantized exports (see usage example below).

## Next Actions
1. Gather emulator/device logs with the new instrumentation to baseline current uppercase outputs for both n-gram and neural engines.
2. Expand the normalization vocabulary (proper nouns beyond months, domain-specific acronyms) and re-run `normalize_corpus.py` as needed before regenerating regional models.
3. Use `retraining/scripts/kenlm_rebuild.sh` with `KENLM_BIN_DIR=/mnt/subtitled/tools/kenlm/build/bin` and a per-order prune schedule (example: `PRUNE_SCHEDULE="0 1 1"`) to produce final ARPA/vocab sets for packaging + manifest updates.
4. Wire `retraining/scripts/neural_finetune.py` into a repeatable fine-tune/export workflow (including ONNX + quantization for the downloader). A starting point:
   ```
   DATASET=/mnt/subtitled/datasets/simple_enwiki/simple_wikipedia/simple_wikipedia.normalized.txt \
   OUTPUT_ROOT=/mnt/subtitled/datasets/simple_enwiki/artifacts/neural/distilgpt2_mixedcase \
   EPOCHS=1 BATCH_SIZE=2 MAX_STEPS=200 \
   retraining/scripts/neural_pipeline.sh distilgpt2
   ```
   The helper script installs checkpoints under `$OUTPUT_ROOT/checkpoints` and, when `onnx`, `onnxruntime`, and `transformers[onnx]` are present, writes ONNX + quantized exports under `$OUTPUT_ROOT/onnx/`.
