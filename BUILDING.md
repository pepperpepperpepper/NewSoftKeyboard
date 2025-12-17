# Building NewSoftKeyboard

This is a lean fork of AnySoftKeyboard. Keep models out of the APK; build fast; ship through the downloader/catalog.

## Prerequisites
- JDK 17
- Android SDK 34 (platforms + build-tools 34.x)
- NDK r27
- Gradle cache: set `GRADLE_USER_HOME=/mnt/finished/.gradle` to avoid disk pressure
- Presage sources: run `scripts/setup_presage.sh` once (or wire into CI prebuild)

## Common builds
- Debug app (default `nsk` flavor): `./gradlew :ime:app:assembleDebug`
- AndroidTest APK:
  - debug (recommended for Genymotion): `TEST_BUILD_TYPE=debug ./gradlew :ime:app:assembleAndroidTest -x lint`
  - release (only if you intend to run release instrumentation): `./gradlew :ime:app:assembleAndroidTest -x lint`
- askCompat flavor (legacy add-on compatibility): `./gradlew :ime:app:assembleAskCompatDebug`
  - Use when validating existing AnySoftKeyboard plug-ins without repackaging. Launcher icon background is green to distinguish it from the default build.
- Unit tests (tokenizer/neural): `./gradlew :engine-neural:test`
- Release (unsigned if keystore envs missing): `./gradlew :ime:app:assembleRelease`

Signing (when available):
- Keystore path defaults to `/tmp/anysoftkeyboard.keystore` (symlink to your real store).
- Env vars (either prefix works):
  - `KEY_STORE_FILE_PASSWORD` / `FDROID_KEYSTORE_PASS`
  - `KEY_STORE_FILE_DEFAULT_ALIAS_PASSWORD` / `FDROID_KEY_ALIAS_PASS`
  - `override_release_key_alias` / `FDROID_KEY_ALIAS` (default `fdroidrepo`)

## Devices & tests
- Emulator target: Genymotion at `localhost:42865`; wrap `adb` in `timeout` for reliability.
- Build + install debug app & tests (nsk flavor):
  ```bash
  TEST_BUILD_TYPE=debug ./gradlew :ime:app:assembleNskDebug :ime:app:assembleAndroidTest -x lint
  timeout 90s adb -s localhost:42865 install -r -t ime/app/build/outputs/apk/nsk/debug/app-nsk-debug.apk
  timeout 120s adb -s localhost:42865 install -r -t ime/app/build/outputs/apk/androidTest/nsk/debug/app-nsk-debug-androidTest.apk
  ```
- Build + install askCompat flavor (legacy add-on checks):
  ```bash
  TEST_BUILD_TYPE=debug ./gradlew :ime:app:assembleAskCompatDebug :ime:app:assembleAndroidTest -x lint
  timeout 90s adb -s localhost:42865 install -r -t ime/app/build/outputs/apk/askCompat/debug/app-askcompat-debug.apk
  timeout 120s adb -s localhost:42865 install -r -t ime/app/build/outputs/apk/androidTest/askCompat/debug/app-askcompat-debug-androidTest.apk
  ```
- IME component names:
  - NSK flavor: `wtf.uhoh.newsoftkeyboard/wtf.uhoh.newsoftkeyboard.NewSoftKeyboardService`
  - askCompat flavor: `wtf.uhoh.newsoftkeyboard.askcompat/com.menny.android.anysoftkeyboard.SoftKeyboard`
  - Test helpers default to NSK; override with `IME_SERVICE_CLASS=com.menny.android.anysoftkeyboard.SoftKeyboard` if you need the legacy service for a given run.
- Run neural sentence sanity (neural manager):
  ```bash
  timeout 240s adb -s localhost:42865 shell am instrument -w -r \
    -e class com.anysoftkeyboard.dictionaries.presage.NeuralNonsenseSentenceInstrumentedTest#buildNonsenseSentenceFromNeuralPredictions \
    wtf.uhoh.newsoftkeyboard.test/androidx.test.runner.AndroidJUnitRunner
  ```
- Run UI tap sentence test (end-to-end suggestions):
  ```bash
  timeout 420s adb -s localhost:42865 shell am instrument -w -r \
    -e class com.anysoftkeyboard.dictionaries.presage.NextWordSuggestionsUiAutomatorTest#composeNonsenseSentenceUsingOnlySuggestions \
    wtf.uhoh.newsoftkeyboard.test/androidx.test.runner.AndroidJUnitRunner
  ```
- Inspect sentence:
  ```bash
  timeout 30s adb -s localhost:42865 logcat -d | grep NON_SENSE_SENTENCE=
  # expected example: first time I had a chance to meet him in the
  ```
- Optional neural debug logs: set `timeout 10s adb -s localhost:42865 shell setprop NSK_TEST_LOGS true` before running tests to dump top‑k logits/decoded tokens.
- Host tokenizer/neural sanity: `./gradlew :engine-neural:test`
- Smoke test external add-ons (real APKs from F‑Droid):
  ```bash
  # Install example language pack + theme pack (update the _6158 suffix as needed).
  curl -L -o /tmp/ask_german.apk https://f-droid.org/repo/com.anysoftkeyboard.languagepack.german_6158.apk
  curl -L -o /tmp/ask_3d_theme.apk https://f-droid.org/repo/com.anysoftkeyboard.theme.three_d_6158.apk
  timeout 90s adb -s localhost:42865 install -r -t /tmp/ask_german.apk
  timeout 90s adb -s localhost:42865 install -r -t /tmp/ask_3d_theme.apk

  # Verify discovery in the host app (skips if packages are missing).
  timeout 240s adb -s localhost:42865 shell am instrument -w -r \
    -e expected_language_pack com.anysoftkeyboard.languagepack.german \
    -e expected_theme_pack com.anysoftkeyboard.theme.three_d \
    -e class com.anysoftkeyboard.addons.cts.ExternalAddOnSmokeInstrumentedTest \
    wtf.uhoh.newsoftkeyboard.test/androidx.test.runner.AndroidJUnitRunner
  ```

## Models
- Catalog URL baked in: `https://fdroid.uh-oh.wtf/models/catalog.json?v=3`
- Models download to `no_backup/presage/models/<model-id>` with SHA‑256 validation. Keep APK free of large assets.

## Publishing to F-Droid
- Build release APK: `./gradlew :ime:app:assembleRelease`
- Metadata/YAML lives under `outputs/fdroid/`. When ready, run your deployment script (e.g., `scripts/update_and_deploy.sh`) with the F-Droid env vars exported (`source /home/arch/fdroid/.env`).

## Don’ts
- Don’t run `./gradlew build` (Android aggregates are flaky); use module tasks above.
- Don’t add new runtime third‑party dependencies without approval.
