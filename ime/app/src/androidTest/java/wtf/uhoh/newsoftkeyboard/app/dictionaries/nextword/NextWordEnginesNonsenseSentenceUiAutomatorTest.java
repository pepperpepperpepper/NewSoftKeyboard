package wtf.uhoh.newsoftkeyboard.app.dictionaries.nextword;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.debug.TestInputActivity;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.presage.DownloaderCompat;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.presage.PresageModelCatalog;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateViewTestRegistry;
import wtf.uhoh.newsoftkeyboard.engine.EngineType;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelDefinition;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelDownloader;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelStore;
import wtf.uhoh.newsoftkeyboard.engine.neural.NeuralPredictionManager;
import wtf.uhoh.newsoftkeyboard.engine.presage.PresagePredictionManager;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NextWordEnginesNonsenseSentenceUiAutomatorTest {

  private static final String TAG = "NextWordEngines";

  private static String getAppPackage() {
    return InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName();
  }

  private static String resId(String idName) {
    return getAppPackage() + ":id/" + idName;
  }

  private static final long READY_TIMEOUT_MS = 10000L;
  private static final long SHORT_WAIT_MS = 400L;
  private static final long SUGGESTIONS_TIMEOUT_MS = 10000L;

  private UiDevice mDevice;
  private ActivityScenario<TestInputActivity> mScenario;
  private String mImeComponent;

  @Before
  public void setUp() throws Exception {
    mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    // UiDevice holds onto a UiAutomation instance. Avoid calling Instrumentation#getUiAutomation
    // separately, since some Android versions throw "UiAutomationService ... already registered!".
    clearLogcat();
    wakeAndUnlockDevice();

    ensureImeEnabledAndSelected();
    assertImeSelected();

    final Context context = ApplicationProvider.getApplicationContext();
    ensureNgramModelActive(context);
    ensureMixedcaseModelActive(context);

    // Common prefs for all engine modes: allow suggestions and auto-space so chaining works.
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(context);
    prefs
        .edit()
        .putBoolean(context.getString(R.string.settings_key_show_suggestions), true)
        // KEEP_FLAGS test depends on ignoring the app's NO_SUGGESTIONS flag so we can verify that
        // next-word predictions still work under Keep-like editor flags.
        .putBoolean(context.getString(R.string.settings_key_respect_app_no_suggestions_flag), false)
        .putBoolean(context.getString(R.string.settings_key_auto_space), true)
        .putString(context.getString(R.string.settings_key_next_word_dictionary_type), "words")
        .apply();
  }

  @After
  public void tearDown() {
    if (mScenario != null) {
      mScenario.close();
      mScenario = null;
    }
  }

  @Test
  public void composeNonsenseSentences_allEngineModes() throws Exception {
    for (String mode : new String[] {"ngram", "neural", "hybrid"}) {
      final String sentence = runHarnessAndComposeSentence(mode, false, false);
      final String line = "ENGINE=" + mode + " NON_SENSE_SENTENCE=" + sentence;
      Log.i(TAG, line);
      System.out.println(line);
    }
  }

  @Test
  public void composeNonsenseSentences_allEngineModes_underKeepFlags() throws Exception {
    // Google Keep and other apps may set IME_FLAG_NO_PERSONALIZED_LEARNING and/or NO_SUGGESTIONS.
    for (String mode : new String[] {"ngram", "neural", "hybrid"}) {
      final String sentence = runHarnessAndComposeSentence(mode, true, true);
      final String line = "ENGINE=" + mode + " KEEP_FLAGS=1 NON_SENSE_SENTENCE=" + sentence;
      Log.i(TAG, line);
      System.out.println(line);
    }
  }

  private String runHarnessAndComposeSentence(
      String engineMode, boolean noPersonalizedLearning, boolean noSuggestionsFlag)
      throws Exception {
    configureEngineMode(engineMode);
    launchTestHarnessAndSeed(noPersonalizedLearning, noSuggestionsFlag);
    waitForNonEmptySuggestions();

    // Seed is "the " (1 word). Pick 19 more to build a 20-word sentence.
    final int picks = 19;
    for (int i = 0; i < picks; i++) {
      mScenario.onActivity(activity -> CandidateViewTestRegistry.pickIfAvailable(0));
      SystemClock.sleep(SHORT_WAIT_MS);
      waitForEditorTextToEndWithSpace();
      waitForNonEmptySuggestions();
    }
    final String text = getEditorText().trim();
    if (text.isEmpty()) {
      dumpWindowHierarchyForDebug();
      fail("Expected editor to contain committed suggestion text for engine=" + engineMode);
    }
    return text;
  }

  private void configureEngineMode(String mode) {
    final Context context = ApplicationProvider.getApplicationContext();
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(context);
    prefs
        .edit()
        .putString(context.getString(R.string.settings_key_prediction_engine_mode), mode)
        .apply();
    // Give listeners time to pick up the change before we show the IME.
    SystemClock.sleep(250);
  }

  private void launchTestHarnessAndSeed(boolean noPersonalizedLearning, boolean noSuggestionsFlag) {
    if (mScenario != null) {
      mScenario.close();
      mScenario = null;
    }
    final Context context = ApplicationProvider.getApplicationContext();
    final Intent intent = new Intent(context, TestInputActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.putExtra(
        TestInputActivity.EXTRA_IME_FLAG_NO_PERSONALIZED_LEARNING, noPersonalizedLearning);
    intent.putExtra(TestInputActivity.EXTRA_TYPE_TEXT_FLAG_NO_SUGGESTIONS, noSuggestionsFlag);
    mScenario = ActivityScenario.launch(intent);

    waitForEditorVisible();
    focusEditor();
    mScenario.onActivity(TestInputActivity::forceShowKeyboard);
    SystemClock.sleep(SHORT_WAIT_MS);
    waitForKeyboardVisible();

    mScenario.onActivity(
        activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.commitText("the "));
    SystemClock.sleep(SHORT_WAIT_MS);
    mScenario.onActivity(
        activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.forceNextWordFromCursor());
    SystemClock.sleep(SHORT_WAIT_MS);
  }

  private void waitForNonEmptySuggestions() {
    final long start = SystemClock.uptimeMillis();
    int count;
    do {
      final int[] c = {0};
      mScenario.onActivity(activity -> c[0] = CandidateViewTestRegistry.getCount());
      count = c[0];
      if (count > 0) return;
      SystemClock.sleep(200);
    } while (SystemClock.uptimeMillis() - start < SUGGESTIONS_TIMEOUT_MS);

    dumpWindowHierarchyForDebug();
    throw new AssertionError("Suggestions did not appear");
  }

  private String getEditorText() {
    for (int attempt = 0; attempt < 3; attempt++) {
      final UiObject2 editor = mDevice.findObject(By.res(resId("test_edit_text")));
      if (editor == null) return "";
      try {
        final String text = editor.getText();
        return text == null ? "" : text;
      } catch (androidx.test.uiautomator.StaleObjectException ignored) {
        SystemClock.sleep(50);
      }
    }
    return "";
  }

  private void waitForEditorTextToEndWithSpace() {
    final long start = SystemClock.uptimeMillis();
    do {
      final String text = getEditorText();
      if (text.endsWith(" ")) return;
      SystemClock.sleep(100);
    } while (SystemClock.uptimeMillis() - start < 3000);
    final String text = getEditorText();
    fail("Expected editor text to end with space, but was '" + text + "'");
  }

  private void ensureKenlmModelActive(Context context) {
    final ModelStore store = new ModelStore(context);
    ModelStore.ActiveModel active = store.ensureActiveModel(EngineType.NGRAM);
    if (active == null) {
      final ModelDefinition definition = stageFixtureNgramModel(context);
      store.persistSelectedModelId(EngineType.NGRAM, definition.getId());
      active = store.ensureActiveModel(EngineType.NGRAM);
    }
    assertNotNull("Expected an NGRAM model to be available for Presage", active);

    final PresagePredictionManager manager = new PresagePredictionManager(context);
    if (!manager.activate()) {
      fail(
          "Presage predictor failed to activate with bundled model: "
              + manager.getLastActivationError());
    }
    manager.deactivate();
  }

  private void ensureNgramModelActive(Context context) {
    ensureKenlmModelActive(context);
  }

  private static ModelDefinition stageFixtureNgramModel(Context targetContext) {
    final java.io.File modelDir =
        new java.io.File(
            targetContext.getNoBackupFilesDir(),
            "presage"
                + java.io.File.separator
                + "models"
                + java.io.File.separator
                + "fixture_kenlm_the_nonsense_3gram");
    if (!modelDir.exists() && !modelDir.mkdirs()) {
      throw new AssertionError("Failed creating model directory " + modelDir.getAbsolutePath());
    }

    final ModelDefinition definition =
        ModelDefinition.builder(modelDir.getName())
            .setLabel("Fixture KenLM (the-cat-banana)")
            .setEngineType(EngineType.NGRAM)
            .setArpaFile("fixture.arpa", null, null, false)
            .setVocabFile("fixture.vocab", null, null, false)
            .build();

    writeFile(new java.io.File(modelDir, "fixture.arpa"), FIXTURE_ARPA);
    writeFile(new java.io.File(modelDir, "fixture.vocab"), FIXTURE_VOCAB);

    final java.io.File manifest = new java.io.File(modelDir, "manifest.json");
    try (java.io.FileOutputStream outputStream = new java.io.FileOutputStream(manifest)) {
      outputStream.write(definition.toJson().toString(2).getBytes(StandardCharsets.UTF_8));
    } catch (Exception exception) {
      throw new AssertionError("Failed writing fixture model manifest", exception);
    }

    return definition;
  }

  private static void writeFile(java.io.File file, String contents) {
    try (java.io.FileOutputStream outputStream = new java.io.FileOutputStream(file)) {
      outputStream.write(contents.getBytes(StandardCharsets.UTF_8));
    } catch (Exception exception) {
      throw new AssertionError(
          "Failed writing fixture model file " + file.getAbsolutePath(), exception);
    }
  }

  private void ensureMixedcaseModelActive(Context context) throws Exception {
    final ModelStore store = new ModelStore(context);
    final ModelStore.ActiveModel activeModel = store.ensureActiveModel(EngineType.NEURAL);
    if (activeModel != null
        && "distilgpt2_mixedcase_sanity".equals(activeModel.getDefinition().getId())) {
      return;
    }

    final ModelDefinition defForEntry =
        ModelDefinition.builder("distilgpt2_mixedcase_sanity")
            .setLabel("DistilGPT-2 mixedcase (sanity)")
            .setEngineType(EngineType.NEURAL)
            .setOnnxFile("model_int8.onnx", null, null)
            .setTokenizerVocabFile("vocab.json", null, null)
            .setTokenizerMergesFile("merges.txt", null, null)
            .build();
    final PresageModelCatalog.CatalogEntry target =
        new PresageModelCatalog.CatalogEntry(
            defForEntry,
            "https://fdroid.uh-oh.wtf/models/distilgpt2_mixedcase_sanity_v1.zip",
            "06dbfa67aed36b24c931dabdb10060b0e93b4af5cbf123c1ce7358b26fec13d4",
            53587027L,
            1,
            false);

    final ModelDownloader downloader = new ModelDownloader(context, store);
    try {
      DownloaderCompat.run(downloader, target);
    } catch (IOException e) {
      // If network is unavailable, proceed only if a model is already installed.
      final ModelStore.ActiveModel fallback = store.ensureActiveModel(EngineType.NEURAL);
      if (fallback == null) {
        throw e;
      }
      Log.w(TAG, "Downloader error (continuing with already installed model): ", e);
    }
    store.persistSelectedModelId(EngineType.NEURAL, "distilgpt2_mixedcase_sanity");

    final NeuralPredictionManager manager = new NeuralPredictionManager(context);
    if (!manager.activate()) {
      fail("Neural predictor failed to activate with mixedcase model");
    }
    manager.deactivate();
  }

  private static final String FIXTURE_ARPA =
      "\\data\\\n"
          + "ngram 1=12\n"
          + "ngram 2=12\n"
          + "ngram 3=11\n"
          + "\n"
          + "\\1-grams:\n"
          + "-0.1761 <s> -0.1761\n"
          + "-0.2218 the -0.2218\n"
          + "-0.2218 cat -0.2218\n"
          + "-0.2218 saw -0.2218\n"
          + "-0.2218 a -0.2218\n"
          + "-0.2218 banana -0.2218\n"
          + "-0.2218 under -0.2218\n"
          + "-0.2218 moon -0.2218\n"
          + "-0.2218 and -0.2218\n"
          + "-0.2218 pizza -0.2218\n"
          + "-0.2218 again -0.2218\n"
          + "-0.2218 quickly -0.2218\n"
          + "\\2-grams:\n"
          + "-0.0970 <s> the -0.2218\n"
          + "-0.0970 the cat -0.2218\n"
          + "-0.0970 cat saw -0.2218\n"
          + "-0.0970 saw a -0.2218\n"
          + "-0.0970 a banana -0.2218\n"
          + "-0.0970 banana under -0.2218\n"
          + "-0.0970 under the -0.2218\n"
          + "-0.0970 the moon -0.2218\n"
          + "-0.0970 moon and -0.2218\n"
          + "-0.0970 and pizza -0.2218\n"
          + "-0.0970 pizza again -0.2218\n"
          + "-0.0970 again quickly -0.2218\n"
          + "\\3-grams:\n"
          + "-0.0458 <s> the cat\n"
          + "-0.0458 the cat saw\n"
          + "-0.0458 cat saw a\n"
          + "-0.0458 saw a banana\n"
          + "-0.0458 a banana under\n"
          + "-0.0458 banana under the\n"
          + "-0.0458 under the moon\n"
          + "-0.0458 the moon and\n"
          + "-0.0458 moon and pizza\n"
          + "-0.0458 and pizza again\n"
          + "-0.0458 pizza again quickly\n"
          + "\\end\\\n";

  private static final String FIXTURE_VOCAB =
      "the\ncat\nsaw\na\nbanana\nunder\nmoon\nand\npizza\nagain\nquickly\n";

  private void waitForEditorVisible() {
    boolean visible =
        mDevice.wait(Until.hasObject(By.res(resId("test_edit_text"))), READY_TIMEOUT_MS);
    if (!visible) {
      dumpWindowHierarchyForDebug();
      fail("Test editor not visible");
    }
  }

  private void waitForKeyboardVisible() {
    boolean visible =
        mDevice.wait(Until.hasObject(By.res(resId("AnyKeyboardMainView"))), READY_TIMEOUT_MS);
    if (!visible) {
      visible = mDevice.wait(Until.hasObject(By.res(resId("candidate_view"))), READY_TIMEOUT_MS);
    }
    if (!visible) {
      dumpWindowHierarchyForDebug();
      fail("Keyboard window not visible");
    }
  }

  private void focusEditor() {
    UiObject2 editor = mDevice.wait(Until.findObject(By.res(resId("test_edit_text"))), 3000);
    if (editor != null) {
      editor.click();
      SystemClock.sleep(300);
    }
  }

  private void dumpWindowHierarchyForDebug() {
    // Some Android versions/devices crash the `uiautomator` dump command when called from an
    // instrumentation test that already owns UiAutomation. Avoid making failures noisier or
    // poisoning later test runs.
    Log.w(TAG, "Skipping window hierarchy dump (known to be unstable on some devices).");
  }

  private void clearLogcat() throws IOException {
    executeShellCommand("logcat -c");
  }

  private void ensureImeEnabledAndSelected() throws IOException {
    mImeComponent = resolveImeComponentId();
    String enableOutput = executeShellCommand("ime enable --user 0 " + mImeComponent).trim();
    Log.d(TAG, "ime enable output: " + enableOutput);
    if (enableOutput.contains("Unknown") || enableOutput.contains("Error")) {
      throw new IOException("Failed to enable IME. Output: " + enableOutput);
    }

    String setOutput = executeShellCommand("ime set --user 0 " + mImeComponent).trim();
    Log.d(TAG, "ime set output: " + setOutput);

    String enabled = executeShellCommand("settings get secure enabled_input_methods").trim();
    String expanded = expandComponent(mImeComponent);
    if (!enabled.contains(mImeComponent) && !enabled.contains(expanded)) {
      String prefix = enabled.isEmpty() ? "" : enabled + ":";
      executeShellCommand(
          "settings put secure enabled_input_methods \"" + prefix + mImeComponent + "\"");
    }
    executeShellCommand("settings put secure show_ime_with_hard_keyboard 1");
    SystemClock.sleep(400);
  }

  private void assertImeSelected() throws IOException {
    String current = executeShellCommand("settings get secure default_input_method").trim();
    String expanded = expandComponent(mImeComponent);
    if (!(current.equals(mImeComponent) || current.equals(expanded))) {
      Log.e(TAG, "default_input_method=" + current);
      String enabled = executeShellCommand("settings get secure enabled_input_methods").trim();
      Log.e(TAG, "enabled_input_methods=" + enabled);
      String imeListAll = executeShellCommand("ime list -a -s").trim();
      Log.e(TAG, "ime list -a -s:\n" + imeListAll);
      throw new AssertionError(
          "NewSoftKeyboard IME not selected. Expected: " + mImeComponent + " Current: " + current);
    }
  }

  private String resolveImeComponentId() throws IOException {
    String list = executeShellCommand("ime list -a -s").trim();
    String[] lines = list.split("\\n");
    String prefix = getAppPackage() + "/";
    String fallback = null;
    for (String line : lines) {
      String trimmed = line.trim();
      if (!trimmed.startsWith(prefix)) continue;
      if (trimmed.endsWith(".NewSoftKeyboardService")
          || trimmed.endsWith("/.NewSoftKeyboardService")) {
        return trimmed;
      }
      if (trimmed.endsWith(".SoftKeyboard") || trimmed.endsWith("/.SoftKeyboard")) {
        fallback = trimmed;
      } else if (fallback == null) {
        fallback = trimmed;
      }
    }
    if (fallback != null) return fallback;
    throw new IOException("Unable to find NewSoftKeyboard IME in: " + list);
  }

  private static String expandComponent(String component) {
    String[] parts = component.split("/", 2);
    if (parts.length != 2) return component;
    String pkg = parts[0];
    String svc = parts[1];
    if (!svc.startsWith(".")) return component;
    return pkg + "/" + pkg + svc;
  }

  private String executeShellCommand(String command) throws IOException {
    return mDevice.executeShellCommand(command);
  }

  private void wakeAndUnlockDevice() throws IOException {
    try {
      if (!mDevice.isScreenOn()) {
        mDevice.wakeUp();
      }
    } catch (android.os.RemoteException e) {
      throw new RuntimeException(e);
    }
    executeShellCommand("wm dismiss-keyguard");
    executeShellCommand("input keyevent 82"); // menu/unlock
    SystemClock.sleep(300);
  }
}
