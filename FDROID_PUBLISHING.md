# F-Droid Publishing & Workflow (Canonical)

# Single source of truth — do not follow other scattered notes; keep this file updated.

## Goals

- Prevent accidental loss/pruning of APKs and indexes.
- Keep all desired historical builds visible under “Other versions”.
- Make publishing deterministic, scripted, and recoverable.

## Bucket & Retention

- Enable S3 **Versioning** and **MFA delete** on `fdroid-uh-oh-wtf`.
- Add lifecycle rule: expire non-current object versions after 180–365 days; never expire current.
- Before each deploy, refuse to run if versioning is disabled (the publish script checks this; bypass only with `SKIP_VERSIONING_CHECK=1`).

## Metadata Strategy

- Generate metadata from the actual APK inventory via a script (no manual edits). Script: `scripts/fdroid/generate_metadata.py`.
- Script logic: scan repo+archive for `wtf.uhoh.newsoftkeyboard_*.apk`, extract versionCode/versionName with `aapt`, set `CurrentVersion/Code` to the highest code (or `$CURRENT_VERSION_CODE` if provided).
- Metadata is intentionally minimal (this repo is distributed as APKs, not rebuilt by F-Droid):
  - `AutoUpdateMode: None`
  - `UpdateCheckMode: None`
  - `Builds: []`

## Safer Deploy Flow

1. Sync **from S3 to staging**: `aws s3 sync s3://fdroid-uh-oh-wtf/repo/ repo/` and same for `archive/`.
2. Run metadata generator on staging files.
3. Run `fdroid update --create-metadata` in the local staging dir (`$FDROID_DATA`), never directly against S3.
4. Validate counts:
   - Total APKs found >= expected_min (configurable, e.g., 7 or 40).
   - CurrentVersion matches build.gradle override.
5. If valid, sync staging **back to S3** (repo + archive + indexes) and invalidate CloudFront.
6. Before writing to S3, create a tarball backup of repo+archive+metadata with a timestamp under `backups/`.

## CI/Checks

- Add a CI job that:
  - Runs metadata generator.
  - Runs `fdroid update` in temp dir.
  - Asserts “Other versions” count >= threshold and no APK hash changes.
  - Fails if S3 versioning is off or APK count drops.

## Keep List

- Maintain `scripts/fdroid/keep_apks.txt` with versionCodes that must remain indexed. Deploy script aborts if any are missing locally or in S3.

## Operational Guardrails

- Never run `fdroid update` directly on S3; always via the staging script.
- Prefer running `DRY_RUN=1 fdroid/scripts/publish.sh` before a real publish to validate build + indexing without touching S3.

## Optional

- Split channels: keep latest N in `repo/`, rest in `archive/`, but index all via `KeepAll` so clients can downgrade without physical moves.
- Add proper repo/archive icons to silence warnings.

## Immediate Actions (if approved)

- Turn on S3 versioning + MFA delete.
- Add generator script + backup/staging deploy script.
- Regenerate metadata from current APK inventory; set CurrentVersion/Code to desired release.
- Redeploy via staging with CloudFront invalidation.

## One-Command Publish (canonical)

- Script: `fdroid/scripts/publish.sh`
- Prereqs: env file + `aapt` + `aws` + `/home/arch/fdroid-env/bin/fdroid`.
- Data dir default: `FDROID_DATA=/home/arch/fdroid` (overridable).
- Env file resolution (first match wins):
  - `$ENV_FILE` or `$FDROID_ENV_FILE`
  - `<repo>/fdroid/.env` (git-ignored; recommended)
  - `$FDROID_DATA/.env`
  - `$HOME/fdroid/.env`
- Flow (automated):
  1. source `.env`, guard envs
  2. sync from S3 (repo/archive)
  3. bump versionCode/versionName (unless `SKIP_BUMP=1`)
  4. build signed release
  5. stage APK into repo/, regenerate metadata from inventory
  6. run `fdroid update --create-metadata`
  7. validate APK count threshold
  8. backup repo+archive+metadata
  9. sync back to S3, invalidate CloudFront
  10. optional git commit (skip with `SKIP_COMMIT=1`)
- Safety switches:
  - Refuses to overwrite an existing `wtf.uhoh.newsoftkeyboard_<versionCode>.apk` unless `ALLOW_OVERWRITE=1`.
  - `DRY_RUN=1` runs everything up through `fdroid update` + validation + backup, but skips S3 sync, CloudFront invalidation, and git commit.

## Keystore / Signing Reference

- Keystore file used for **APK signing** (Gradle):
  - `KEY_STORE_FILE` (or `FDROID_KEYSTORE_FILE`)
  - `/tmp/newsoftkeyboard.keystore`
  - `$FDROID_DATA/keystore.jks`
  - `$HOME/fdroid/keystore.jks`
  - Legacy fallback: `/tmp/anysoftkeyboard.keystore`
- Alias: `fdroidrepo`
- Environment variables used by Gradle signing:
  - Store password: `FDROID_KEYSTORE_PASS` / `FDROID_KEY_STORE_PASS` or `KEY_STORE_FILE_PASSWORD`
  - Alias/key password: `FDROID_KEY_ALIAS_PASS` (preferred) or `FDROID_KEY_PASS` (legacy) or `KEY_STORE_FILE_DEFAULT_ALIAS_PASSWORD`
  - Optional alias override: `FDROID_KEY_ALIAS` (defaults to `fdroidrepo`)
- AWS / bucket: `FDROID_AWS_BUCKET`, `FDROID_AWS_ACCESS_KEY_ID`, `FDROID_AWS_SECRET_KEY`
- Guard step before running `fdroid update`:
  - `scripts/fdroid/check_keystore_env.sh` verifies these envs are set and non-empty; the publish script runs it automatically.
- Local secret storage:
  - Real values live in `fdroid/.env` (git-ignored); source it with `set -a && source fdroid/.env && set +a`.
  - `fdroid/.env.example` lists required keys for new machines.
