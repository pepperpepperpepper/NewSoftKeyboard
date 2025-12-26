# Building NewSoftKeyboard

This repository builds **NewSoftKeyboard**.

You will still see legacy `AnySoftKeyboard`/`com.menny.android.anysoftkeyboard` names in a few places (package/class names,
component entrypoints, and add-on action strings). Those are kept on purpose for **upgrade stability** and **runtime
compatibility** with existing AnySoftKeyboard add-ons (keyboards/dictionaries/themes/quick-text). This does **not** mean we
build upstream AnySoftKeyboard as a dependency.

## Why this file exists

- A single, known-good set of build/test commands (humans + automation).
- A place to pin “gotchas” (Gradle task to avoid, emulator adb timeouts, signing env names).
- The canonical pointer to release + F-Droid publishing docs (so instructions don’t drift across random notes/scripts).

## Prerequisites

- JDK 17
- Android SDK 34 (platforms + build-tools 34.x)
- NDK r27
- Gradle cache: set `GRADLE_USER_HOME=/mnt/finished/.gradle` to avoid disk pressure
- Presage sources: run `scripts/setup_presage.sh` once (or wire into CI prebuild)

## Common builds

- Debug app (default `nsk` flavor): `GRADLE_USER_HOME=/mnt/finished/.gradle ./gradlew :ime:app:assembleNskDebug`
- AndroidTest APK:
  - debug (recommended for Genymotion): `GRADLE_USER_HOME=/mnt/finished/.gradle TEST_BUILD_TYPE=debug ./gradlew :ime:app:assembleNskDebugAndroidTest -x lint`
  - release (only if you intend to run release instrumentation): `GRADLE_USER_HOME=/mnt/finished/.gradle TEST_BUILD_TYPE=release ./gradlew :ime:app:assembleAndroidTest -x lint`
- askCompat flavor (legacy add-on compatibility): `./gradlew :ime:app:assembleAskCompatDebug`
  - Use when validating existing AnySoftKeyboard plug-ins without repackaging. Launcher icon background is green to distinguish it from the default build.
- Unit tests (app JVM): `GRADLE_USER_HOME=/mnt/finished/.gradle ./gradlew :ime:app:testNskDebugUnitTest -x lint`
- Unit tests (tokenizer/neural): `GRADLE_USER_HOME=/mnt/finished/.gradle ./gradlew :engine-neural:test`
- Release (unsigned if keystore envs missing): `GRADLE_USER_HOME=/mnt/finished/.gradle ./gradlew :ime:app:assembleNskRelease -x lint`

Signing (when available):

- Keystore path resolution (in order):
  - `KEY_STORE_FILE` (or `FDROID_KEYSTORE_FILE`)
  - `/tmp/newsoftkeyboard.keystore`
  - `$FDROID_DATA/keystore.jks`
  - `$HOME/fdroid/keystore.jks`
  - Legacy fallback: `/tmp/anysoftkeyboard.keystore`
- Password env vars:
  - `KEY_STORE_FILE_PASSWORD` / `FDROID_KEYSTORE_PASS` / `FDROID_KEY_STORE_PASS`
  - `KEY_STORE_FILE_DEFAULT_ALIAS_PASSWORD` / `FDROID_KEY_ALIAS_PASS` / `FDROID_KEY_PASS` (legacy)
  - `override_release_key_alias` / `FDROID_KEY_ALIAS` (default `fdroidrepo`)
- If env vars are not exported (common in non-interactive shells), Gradle will also try reading a `.env` file from:
  - `$FDROID_ENV_FILE` (explicit override) or `$ENV_FILE` (legacy)
  - `<repo>/fdroid/.env`
  - `$FDROID_DATA/.env`
  - `$HOME/fdroid/.env`

## Devices & tests

- Emulator target: Genymotion; set `GENYMOTION_DEV` to the device ADB serial (usually `localhost:<port>` on Genymotion SaaS) and wrap `adb` in `timeout` for reliability.
  - Genymotion SaaS example: `export GENYMOTION_DEV="$(gmsaas instances adbconnect <INSTANCE_UUID>)"`
- Build + install debug app & tests (nsk flavor):
  ```bash
  TEST_BUILD_TYPE=debug ./gradlew :ime:app:assembleNskDebug :ime:app:assembleAndroidTest -x lint
  timeout 90s adb -s "$GENYMOTION_DEV" install -r -t ime/app/build/outputs/apk/nsk/debug/app-nsk-debug.apk
  timeout 120s adb -s "$GENYMOTION_DEV" install -r -t ime/app/build/outputs/apk/androidTest/nsk/debug/app-nsk-debug-androidTest.apk
  ```
- Build + install askCompat flavor (legacy add-on checks):
  ```bash
  TEST_BUILD_TYPE=debug ./gradlew :ime:app:assembleAskCompatDebug :ime:app:assembleAndroidTest -x lint
  timeout 90s adb -s "$GENYMOTION_DEV" install -r -t ime/app/build/outputs/apk/askCompat/debug/app-askcompat-debug.apk
  timeout 120s adb -s "$GENYMOTION_DEV" install -r -t ime/app/build/outputs/apk/androidTest/askCompat/debug/app-askcompat-debug-androidTest.apk
  ```
- IME component names:
  - NSK flavor: `wtf.uhoh.newsoftkeyboard/.NewSoftKeyboardService`
  - askCompat flavor: `wtf.uhoh.newsoftkeyboard.askcompat/com.menny.android.anysoftkeyboard.SoftKeyboard`
  - Test helpers default to NSK; override with `IME_COMPONENT` (recommended) if you need askCompat: `IME_COMPONENT=wtf.uhoh.newsoftkeyboard.askcompat/com.menny.android.anysoftkeyboard.SoftKeyboard`.
- Run neural sentence sanity (neural manager):
  ```bash
  timeout 240s adb -s "$GENYMOTION_DEV" shell am instrument -w -r \
    -e class wtf.uhoh.newsoftkeyboard.app.dictionaries.neural.NeuralNonsenseSentenceInstrumentedTest#buildNonsenseSentenceFromNeuralPredictions \
    wtf.uhoh.newsoftkeyboard.test/androidx.test.runner.AndroidJUnitRunner
  ```
- Run UI tap sentence test (end-to-end suggestions):
  ```bash
  timeout 420s adb -s "$GENYMOTION_DEV" shell am instrument -w -r \
    -e class wtf.uhoh.newsoftkeyboard.app.dictionaries.presage.NextWordSuggestionsUiAutomatorTest#composeNonsenseSentenceUsingOnlySuggestions \
    wtf.uhoh.newsoftkeyboard.test/androidx.test.runner.AndroidJUnitRunner
  ```
- Inspect sentence:
  ```bash
  timeout 30s adb -s "$GENYMOTION_DEV" logcat -d | grep NON_SENSE_SENTENCE=
  # expected example: first time I had a chance to meet him in the
  ```
- Optional neural debug logs: set `timeout 10s adb -s "$GENYMOTION_DEV" shell setprop NSK_TEST_LOGS true` before running tests to dump top‑k logits/decoded tokens.
- Host tokenizer/neural sanity: `./gradlew :engine-neural:test`
- Smoke test external add-ons (real APKs from F‑Droid):

  ```bash
  # Install example language pack + theme pack (update the _6158 suffix as needed).
  curl -L -o /tmp/ask_german.apk https://f-droid.org/repo/com.anysoftkeyboard.languagepack.german_6158.apk
  curl -L -o /tmp/ask_3d_theme.apk https://f-droid.org/repo/com.anysoftkeyboard.theme.three_d_6158.apk
  timeout 90s adb -s "$GENYMOTION_DEV" install -r -t /tmp/ask_german.apk
  timeout 90s adb -s "$GENYMOTION_DEV" install -r -t /tmp/ask_3d_theme.apk

  # Verify discovery in the host app (skips if packages are missing).
  timeout 240s adb -s "$GENYMOTION_DEV" shell am instrument -w -r \
    -e expected_language_pack com.anysoftkeyboard.languagepack.german \
    -e expected_theme_pack com.anysoftkeyboard.theme.three_d \
    -e class com.anysoftkeyboard.addons.cts.ExternalAddOnSmokeInstrumentedTest \
    wtf.uhoh.newsoftkeyboard.test/androidx.test.runner.AndroidJUnitRunner
  ```

## Models

- Catalog URL baked in: `https://fdroid.uh-oh.wtf/models/catalog.json?v=3`
- Models download to `no_backup/presage/models/<model-id>` with SHA‑256 validation. Keep APK free of large assets.

## Publishing to F-Droid

- Canonical workflow and one-command publish lives in `FDROID_PUBLISHING.md`.

## Don’ts

- Don’t run `./gradlew build` (Android aggregates are flaky); use module tasks above.
- Don’t add new runtime third‑party dependencies without approval.
