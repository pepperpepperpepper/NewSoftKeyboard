package wtf.uhoh.newsoftkeyboard.app.dictionaries.nextword;

import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
import wtf.uhoh.newsoftkeyboard.nextword.NextWordDictionary;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

/**
 * UI-level next-word chaining quality metrics.
 *
 * <p>This harness validates editor-committed text (not just candidate strings) so we can detect
 * whitespace/commit issues like missing spaces that fuse adjacent words.
 *
 * <p>Opt-in: set {@code RUN_NEXTWORD_QUALITY_EVAL=1} to run.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class NextWordEnginesUiChainQualityEvalUiAutomatorTest {

  private static final String TAG = "NextWordUiChainEval";

  private static final String EVAL_ENABLED_ARG = "RUN_NEXTWORD_QUALITY_EVAL";

  private static final long READY_TIMEOUT_MS = 10_000L;
  private static final long SUGGESTIONS_TIMEOUT_MS = 10_000L;
  private static final long EDITOR_UPDATE_TIMEOUT_MS = 6_000L;
  private static final long SHORT_WAIT_MS = 400L;

  private static final int MAX_RESULTS = 8;
  private static final int CHAIN_TARGET_WORDS = 40;

  private static final String DEFAULT_TEST_NEURAL_MODEL_ID = "distilgpt2_mixedcase_sanity";
  private static final String DEFAULT_TEST_NEURAL_MODEL_BUNDLE_URL =
      "https://fdroid.uh-oh.wtf/models/distilgpt2_mixedcase_sanity_v1.zip";

  private static final String FIXTURE_NGRAM_MODEL_ID = "fixture_kenlm_the_nonsense_3gram";

  private static volatile UiDevice sDevice;
  private UiDevice mDevice;
  private ActivityScenario<TestInputActivity> mScenario;
  private String mImeComponent;

  private static String getAppPackage() {
    return InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName();
  }

  private static String resId(String idName) {
    return getAppPackage() + ":id/" + idName;
  }

  @Before
  public void setUp() throws Exception {
    assumeTrue(
        "Set RUN_NEXTWORD_QUALITY_EVAL=1 to run the UI-chain quality evaluation.",
        isQualityEvalEnabled());

    if (sDevice == null) {
      IllegalStateException last = null;
      for (int attempt = 0; attempt < 3; attempt++) {
        try {
          sDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
          last = null;
          break;
        } catch (IllegalStateException e) {
          last = e;
          SystemClock.sleep(500);
        }
      }
      if (sDevice == null && last != null) {
        throw last;
      }
    }
    mDevice = sDevice;

    wakeAndUnlockDevice();
    ensureImeEnabledAndSelected();
    assertImeSelected();

    final Context context = ApplicationProvider.getApplicationContext();
    ensureNgramModelActive(context);
    // Best-effort; we can still evaluate NGRAM if neural is unavailable.
    ensureMixedcaseNeuralModelActive(context);

    // Common prefs for all engine modes: allow suggestions and auto-space so chaining works.
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(context);
    prefs
        .edit()
        .putBoolean(context.getString(R.string.settings_key_show_suggestions), true)
        // KEEP_FLAGS runs depend on ignoring the app's NO_SUGGESTIONS flag so we can verify that
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
  public void evaluateUiChain_allEngineModes_bestEffort() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final ModelStore.ActiveModel ngramModel =
        new ModelStore(context).ensureActiveModel(EngineType.NGRAM);
    final ModelStore.ActiveModel neuralModel =
        new ModelStore(context).ensureActiveModel(EngineType.NEURAL);

    final String ngramModelId = ngramModel == null ? "none" : ngramModel.getDefinition().getId();
    final String neuralModelId = neuralModel == null ? "none" : neuralModel.getDefinition().getId();

    for (Scenario scenario : new Scenario[] {Scenario.NORMAL, Scenario.KEEP_FLAGS}) {
      for (String mode : new String[] {"ngram", "neural", "hybrid"}) {
        if ("neural".equals(mode) || "hybrid".equals(mode)) {
          if (neuralModel == null) {
            Log.w(
                TAG,
                "Skipping mode="
                    + mode
                    + " scenario="
                    + scenario.id
                    + " because no neural model is installed.");
            continue;
          }
        }

        configureEngineMode(mode);
        final UiChainMetrics metrics =
            runHarnessAndMeasureUiChain(
                mode, scenario.noPersonalizedLearning, scenario.noSuggestions);

        final String summary =
            metrics.renderSummary(mode, scenario.id, ngramModelId, neuralModelId);
        final String json = metrics.toJson(mode, scenario.id, ngramModelId, neuralModelId);
        Log.i(TAG, summary);
        System.out.println(summary);
        writeMetricsReport(context, mode + "-" + scenario.id, summary, json);
      }
    }
  }

  @Test
  public void evaluateLegacyPersonalizationConvergenceAndPersistence_bestEffort() throws Exception {
    // Focused check for the legacy next-word dictionary behavior:
    // - Convergence: after repeating "hello world" N times, "world" should be suggested after
    // "hello"
    //   at default (medium) aggressiveness (minWordUsage=3).
    // - Persistence: after closing dictionaries, the stored data should reload with the same
    // ranking.
    configureEngineMode("none");

    final Context context = ApplicationProvider.getApplicationContext();
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(context);
    prefs
        .edit()
        .putBoolean(context.getString(R.string.settings_key_show_suggestions), true)
        .putBoolean(context.getString(R.string.settings_key_auto_space), true)
        .putString(context.getString(R.string.settings_key_next_word_dictionary_type), "words")
        .putString(
            context.getString(R.string.settings_key_next_word_suggestion_aggressiveness),
            "medium_aggressiveness")
        .apply();
    SystemClock.sleep(SHORT_WAIT_MS);

    launchTestHarnessAndSeed(/* noPersonalizedLearning= */ false, /* noSuggestionsFlag= */ false);
    mScenario.onActivity(
        activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.clearLearningDataForTest());
    SystemClock.sleep(SHORT_WAIT_MS);

    final int repetitions = 3;
    for (int i = 0; i < repetitions; i++) {
      mScenario.onActivity(
          activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.commitText("hello world "));
      SystemClock.sleep(SHORT_WAIT_MS);
    }

    mScenario.onActivity(
        activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.commitText("hello "));
    SystemClock.sleep(SHORT_WAIT_MS);
    waitForNonEmptySuggestions();

    final String[] head = {"", "", ""};
    mScenario.onActivity(
        activity -> {
          final int count = CandidateViewTestRegistry.getCount();
          final int scanLimit = Math.min(count, head.length);
          for (int i = 0; i < scanLimit; i++) {
            final String candidate = CandidateViewTestRegistry.getSuggestionAt(i);
            head[i] = candidate == null ? "" : candidate.trim();
          }
        });

    boolean found = false;
    for (String s : head) {
      if ("world".equalsIgnoreCase(s)) {
        found = true;
        break;
      }
    }
    if (!found) {
      throw new AssertionError(
          "Expected 'world' in the top suggestions after training; got " + Arrays.toString(head));
    }

    // Flush user dictionaries to disk and verify the next-word dictionary reloads with the same
    // ranking. This approximates the "restart" persistence check without killing the IME process.
    mScenario.onActivity(
        activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.closeDictionariesForTest());
    SystemClock.sleep(SHORT_WAIT_MS);

    final NextWordDictionary dictionary = new NextWordDictionary(context, "en");
    dictionary.load();
    final java.util.Iterator<String> nextWords =
        dictionary.getNextWords("hello", /* maxResults= */ 8, /* minWordUsage= */ 3).iterator();
    if (!nextWords.hasNext()) {
      dictionary.close();
      throw new AssertionError(
          "Expected persisted next-word suggestions for 'hello', but got none");
    }
    final String next = nextWords.next();
    dictionary.close();
    if (!"world".equalsIgnoreCase(next)) {
      throw new AssertionError(
          "Expected persisted next word 'world' for 'hello', got '" + next + "'");
    }
  }

  private UiChainMetrics runHarnessAndMeasureUiChain(
      @NonNull String engineMode, boolean noPersonalizedLearning, boolean noSuggestionsFlag)
      throws Exception {
    launchTestHarnessAndSeed(noPersonalizedLearning, noSuggestionsFlag);
    waitForNonEmptySuggestions();

    final UiChainMetrics metrics = new UiChainMetrics();

    final ArrayList<String> expectedTokens = new ArrayList<>();
    expectedTokens.add("the");
    metrics.seedToken = "the";

    String previousText = getEditorText();

    while (expectedTokens.size() < CHAIN_TARGET_WORDS) {
      waitForNonEmptySuggestions();

      final String[] head = {""};
      mScenario.onActivity(
          activity -> {
            final String suggestion = CandidateViewTestRegistry.getSuggestionAt(0);
            head[0] = suggestion == null ? "" : suggestion;
          });

      final List<String> pickedTokens = tokenizeSentence(head[0]);
      metrics.picksAttempted++;
      if (pickedTokens.isEmpty()) {
        metrics.emptyOrNonWordPicks++;
        break;
      }
      expectedTokens.addAll(pickedTokens);
      metrics.picksWithMultipleTokens += pickedTokens.size() > 1 ? 1 : 0;

      mScenario.onActivity(activity -> CandidateViewTestRegistry.pickIfAvailable(0));

      final String updatedText = waitForEditorTextToChange(previousText);
      if (updatedText == null) {
        metrics.editorUpdateTimeouts++;
        break;
      }
      previousText = updatedText;

      if (!waitForEditorTextToEndWithSpaceBestEffort()) {
        metrics.trailingSpaceTimeouts++;
      }

      final List<String> actualTokens = tokenizeSentence(previousText);
      metrics.finalEditorText = previousText;
      metrics.finalTokens = actualTokens;
      metrics.recordAlignment(expectedTokens, actualTokens);

      if (expectedTokens.size() >= CHAIN_TARGET_WORDS) break;
      SystemClock.sleep(100);
    }

    metrics.expectedWords = expectedTokens.size();
    return metrics;
  }

  private void configureEngineMode(@NonNull String mode) {
    final Context context = ApplicationProvider.getApplicationContext();
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(context);
    prefs
        .edit()
        .putString(context.getString(R.string.settings_key_prediction_engine_mode), mode)
        .apply();
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

  private void waitForEditorVisible() {
    boolean visible =
        mDevice.wait(Until.hasObject(By.res(resId("test_edit_text"))), READY_TIMEOUT_MS);
    if (!visible) {
      throw new AssertionError("Test editor not visible");
    }
  }

  private void waitForKeyboardVisible() {
    boolean visible =
        mDevice.wait(Until.hasObject(By.res(resId("AnyKeyboardMainView"))), READY_TIMEOUT_MS);
    if (!visible) {
      visible = mDevice.wait(Until.hasObject(By.res(resId("candidate_view"))), READY_TIMEOUT_MS);
    }
    if (!visible) {
      throw new AssertionError("Keyboard window not visible");
    }
  }

  private void focusEditor() {
    UiObject2 editor = mDevice.wait(Until.findObject(By.res(resId("test_edit_text"))), 3000);
    if (editor != null) {
      editor.click();
      SystemClock.sleep(300);
    }
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
    throw new AssertionError("Suggestions did not appear");
  }

  @Nullable
  private String waitForEditorTextToChange(@Nullable String previousText) {
    final String before = previousText == null ? "" : previousText;
    final long start = SystemClock.uptimeMillis();
    do {
      final String now = getEditorText();
      if (!now.equals(before)) {
        return now;
      }
      SystemClock.sleep(50);
    } while (SystemClock.uptimeMillis() - start < EDITOR_UPDATE_TIMEOUT_MS);
    return null;
  }

  private boolean waitForEditorTextToEndWithSpaceBestEffort() {
    final long start = SystemClock.uptimeMillis();
    do {
      final String text = getEditorText();
      if (text.endsWith(" ")) return true;
      SystemClock.sleep(50);
    } while (SystemClock.uptimeMillis() - start < 3000L);
    return getEditorText().endsWith(" ");
  }

  @NonNull
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

  private boolean isQualityEvalEnabled() {
    final String arg = InstrumentationRegistry.getArguments().getString(EVAL_ENABLED_ARG);
    if ("1".equals(arg) || "true".equalsIgnoreCase(arg)) return true;
    final String property = System.getProperty(EVAL_ENABLED_ARG);
    return "1".equals(property) || "true".equalsIgnoreCase(property);
  }

  @NonNull
  private List<String> tokenizeSentence(@Nullable String sentence) {
    if (sentence == null) return java.util.Collections.emptyList();
    final String cleaned = sentence.toLowerCase(Locale.US).replaceAll("[^a-z']+", " ").trim();
    if (cleaned.isEmpty()) return java.util.Collections.emptyList();
    return Arrays.asList(cleaned.split("\\s+"));
  }

  private void writeMetricsReport(
      @NonNull Context context,
      @NonNull String runName,
      @NonNull String summary,
      @NonNull String json) {
    final File baseDir = context.getExternalFilesDir(null);
    final File reportsDir =
        baseDir == null
            ? new File(context.getFilesDir(), "reports/nextword-quality")
            : new File(baseDir, "reports/nextword-quality");
    if (!reportsDir.exists() && !reportsDir.mkdirs()) {
      return;
    }
    final long timestampMs = System.currentTimeMillis();
    final File outTextFile =
        new File(reportsDir, "ui-chain-" + runName + "-" + timestampMs + ".txt");
    final File outJsonFile =
        new File(reportsDir, "ui-chain-" + runName + "-" + timestampMs + ".json");
    try (FileOutputStream output = new FileOutputStream(outTextFile)) {
      output.write(summary.getBytes(StandardCharsets.UTF_8));
      output.flush();
      Log.i(TAG, "Wrote UI-chain quality report to " + outTextFile.getAbsolutePath());
    } catch (Exception ignored) {
      // best-effort
    }
    try (FileOutputStream output = new FileOutputStream(outJsonFile)) {
      output.write(json.getBytes(StandardCharsets.UTF_8));
      output.flush();
      Log.i(TAG, "Wrote UI-chain quality JSON report to " + outJsonFile.getAbsolutePath());
    } catch (Exception ignored) {
      // best-effort
    }
  }

  @NonNull
  private static String jsonString(@Nullable String value) {
    if (value == null) return "null";
    final StringBuilder out = new StringBuilder();
    out.append('"');
    for (int i = 0; i < value.length(); i++) {
      final char c = value.charAt(i);
      switch (c) {
        case '\\':
          out.append("\\\\");
          break;
        case '"':
          out.append("\\\"");
          break;
        case '\n':
          out.append("\\n");
          break;
        case '\r':
          out.append("\\r");
          break;
        case '\t':
          out.append("\\t");
          break;
        default:
          out.append(c);
      }
    }
    out.append('"');
    return out.toString();
  }

  private enum Scenario {
    NORMAL("normal", false, false),
    KEEP_FLAGS("keep_flags", true, true);

    final String id;
    final boolean noPersonalizedLearning;
    final boolean noSuggestions;

    Scenario(String id, boolean noPersonalizedLearning, boolean noSuggestions) {
      this.id = id;
      this.noPersonalizedLearning = noPersonalizedLearning;
      this.noSuggestions = noSuggestions;
    }
  }

  private static final class UiChainMetrics {
    private static final List<String> JOINED_STOPWORD_PAIRS =
        Arrays.asList(
            "ofthe", "inthe", "tothe", "onthe", "andthe", "forthe", "fromthe", "withthe", "atthe");

    @Nullable String finalEditorText;
    @Nullable List<String> finalTokens;
    @Nullable String seedToken;

    int expectedWords;
    int picksAttempted;
    int picksWithMultipleTokens;
    int emptyOrNonWordPicks;
    int editorUpdateTimeouts;
    int trailingSpaceTimeouts;

    int tokenAlignmentErrors;
    int boundaryFusionEvents;
    int boundaryFusionJoinedStopwordEvents;

    void recordAlignment(@NonNull List<String> expectedTokens, @NonNull List<String> actualTokens) {
      final AlignmentResult result = computeAlignment(expectedTokens, actualTokens);
      if (!result.aligned) {
        tokenAlignmentErrors++;
      }
      boundaryFusionEvents = result.boundaryFusions;
      boundaryFusionJoinedStopwordEvents = result.joinedStopwordFusions;
    }

    @NonNull
    String renderSummary(
        @NonNull String engineMode,
        @NonNull String scenarioId,
        @NonNull String ngramModelId,
        @NonNull String neuralModelId) {
      final int actualWords = finalTokens == null ? 0 : finalTokens.size();
      final double mismatchRate = rate(tokenAlignmentErrors, Math.max(1, picksAttempted));
      final int boundaryCount = Math.max(1, expectedWords - 1);
      final double fusionRate = rate(boundaryFusionEvents, boundaryCount);
      final double joinedStopwordFusionRate =
          rate(boundaryFusionJoinedStopwordEvents, boundaryCount);
      final double trailingSpaceTimeoutRate =
          rate(trailingSpaceTimeouts, Math.max(1, picksAttempted));
      final double multiTokenPickRate = rate(picksWithMultipleTokens, Math.max(1, picksAttempted));
      final double emptyOrNonWordPickRate = rate(emptyOrNonWordPicks, Math.max(1, picksAttempted));

      final String sample = finalTokens == null ? "" : sampleTokens(finalTokens, /* limit= */ 20);

      return "NextWord UI-Chain Quality\n"
          + "engine_mode: "
          + engineMode
          + "\n"
          + "scenario: "
          + scenarioId
          + "\n"
          + "ngram_model_id: "
          + ngramModelId
          + "\n"
          + "neural_model_id: "
          + neuralModelId
          + "\n"
          + String.format(Locale.US, "picks_attempted: %d%n", picksAttempted)
          + String.format(Locale.US, "expected_words: %d%n", expectedWords)
          + String.format(Locale.US, "final_words: %d%n", actualWords)
          + String.format(Locale.US, "token_alignment_error_rate: %.4f%n", mismatchRate)
          + String.format(Locale.US, "boundary_fusion_rate: %.4f%n", fusionRate)
          + String.format(
              Locale.US, "joined_stopword_fusion_rate: %.4f%n", joinedStopwordFusionRate)
          + String.format(
              Locale.US, "trailing_space_timeout_rate: %.4f%n", trailingSpaceTimeoutRate)
          + String.format(Locale.US, "multi_token_pick_rate: %.4f%n", multiTokenPickRate)
          + String.format(Locale.US, "empty_or_non_word_pick_rate: %.4f%n", emptyOrNonWordPickRate)
          + String.format(Locale.US, "editor_update_timeouts: %d%n", editorUpdateTimeouts)
          + String.format(Locale.US, "empty_or_non_word_pick_count: %d%n", emptyOrNonWordPicks)
          + String.format(Locale.US, "chain_sample: %s%n", sample);
    }

    @NonNull
    String toJson(
        @NonNull String engineMode,
        @NonNull String scenarioId,
        @NonNull String ngramModelId,
        @NonNull String neuralModelId) {
      final int actualWords = finalTokens == null ? 0 : finalTokens.size();
      final String sample = finalTokens == null ? "" : sampleTokens(finalTokens, /* limit= */ 40);
      final String editorText = finalEditorText == null ? "" : finalEditorText;

      final StringBuilder out = new StringBuilder();
      out.append("{\n");
      out.append("  \"schema\": \"nextword-ui-chain-quality\",\n");
      out.append("  \"harness\": \"uiautomator\",\n");
      out.append("  \"engine_mode\": ").append(jsonString(engineMode)).append(",\n");
      out.append("  \"scenario\": ").append(jsonString(scenarioId)).append(",\n");
      out.append("  \"ngram_model_id\": ").append(jsonString(ngramModelId)).append(",\n");
      out.append("  \"neural_model_id\": ").append(jsonString(neuralModelId)).append(",\n");
      out.append("  \"timestamp_ms\": ").append(System.currentTimeMillis()).append(",\n");
      out.append("  \"picks_attempted\": ").append(picksAttempted).append(",\n");
      out.append("  \"expected_words\": ").append(expectedWords).append(",\n");
      out.append("  \"final_words\": ").append(actualWords).append(",\n");
      out.append("  \"metrics\": {\n");
      final int boundaryCount = Math.max(1, expectedWords - 1);
      out.append(
          String.format(
              Locale.US,
              "    \"token_alignment_error_rate\": %.6f,%n",
              rate(tokenAlignmentErrors, Math.max(1, picksAttempted))));
      out.append(
          String.format(
              Locale.US,
              "    \"boundary_fusion_rate\": %.6f,%n",
              rate(boundaryFusionEvents, boundaryCount)));
      out.append(
          String.format(
              Locale.US,
              "    \"joined_stopword_fusion_rate\": %.6f,%n",
              rate(boundaryFusionJoinedStopwordEvents, boundaryCount)));
      out.append(
          String.format(
              Locale.US,
              "    \"trailing_space_timeout_rate\": %.6f,%n",
              rate(trailingSpaceTimeouts, Math.max(1, picksAttempted))));
      out.append(
          String.format(
              Locale.US,
              "    \"multi_token_pick_rate\": %.6f,%n",
              rate(picksWithMultipleTokens, Math.max(1, picksAttempted))));
      out.append(
          String.format(
              Locale.US,
              "    \"empty_or_non_word_pick_rate\": %.6f,%n",
              rate(emptyOrNonWordPicks, Math.max(1, picksAttempted))));
      out.append("    \"editor_update_timeouts\": ").append(editorUpdateTimeouts).append(",\n");
      out.append("    \"multi_token_pick_count\": ").append(picksWithMultipleTokens).append(",\n");
      out.append("    \"empty_or_non_word_pick_count\": ").append(emptyOrNonWordPicks).append("\n");
      out.append("  },\n");
      out.append("  \"samples\": {\n");
      out.append("    \"chain\": ").append(jsonString(sample)).append(",\n");
      out.append("    \"editor_text\": ").append(jsonString(editorText)).append("\n");
      out.append("  }\n");
      out.append("}\n");
      return out.toString();
    }

    private double rate(int numerator, int denominator) {
      if (denominator <= 0) return 0.0;
      return (double) numerator / denominator;
    }

    @NonNull
    private String sampleTokens(@NonNull List<String> tokens, int limit) {
      final int n = Math.min(limit, tokens.size());
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < n; i++) {
        if (i > 0) sb.append(' ');
        sb.append(tokens.get(i));
      }
      if (tokens.size() > n) sb.append(" …");
      return sb.toString();
    }

    private static final class AlignmentResult {
      final boolean aligned;
      final int boundaryFusions;
      final int joinedStopwordFusions;

      private AlignmentResult(boolean aligned, int boundaryFusions, int joinedStopwordFusions) {
        this.aligned = aligned;
        this.boundaryFusions = boundaryFusions;
        this.joinedStopwordFusions = joinedStopwordFusions;
      }
    }

    @NonNull
    private AlignmentResult computeAlignment(
        @NonNull List<String> expectedTokens, @NonNull List<String> actualTokens) {
      int i = 0;
      int j = 0;
      int fusions = 0;
      int joinedStopwordFusions = 0;

      while (i < expectedTokens.size() && j < actualTokens.size()) {
        final String expected = expectedTokens.get(i);
        final String actual = actualTokens.get(j);
        if (expected.equals(actual)) {
          i++;
          j++;
          continue;
        }

        if (i + 1 < expectedTokens.size()) {
          final String fused = expected + expectedTokens.get(i + 1);
          if (fused.equals(actual)) {
            fusions++;
            if (containsJoinedStopwordPair(fused)) {
              joinedStopwordFusions++;
            }
            i += 2;
            j++;
            continue;
          }
        }

        // Unrecognized mismatch (replacement, extra token, deletion, etc.)
        return new AlignmentResult(false, fusions, joinedStopwordFusions);
      }

      final boolean aligned = i == expectedTokens.size() && j == actualTokens.size();
      return new AlignmentResult(aligned, fusions, joinedStopwordFusions);
    }

    private boolean containsJoinedStopwordPair(@NonNull String token) {
      for (String fused : JOINED_STOPWORD_PAIRS) {
        if (token.contains(fused)) return true;
      }
      return false;
    }
  }

  private void ensureKenlmModelActive(@NonNull Context context) {
    final ModelStore store = new ModelStore(context);
    ModelStore.ActiveModel active = store.ensureActiveModel(EngineType.NGRAM);
    if (active == null) {
      final ModelDefinition definition = stageFixtureNgramModel(context);
      store.persistSelectedModelId(EngineType.NGRAM, definition.getId());
      active = store.ensureActiveModel(EngineType.NGRAM);
    }
    if (active == null) {
      throw new AssertionError("Expected an NGRAM model to be available for Presage");
    }

    final PresagePredictionManager manager = new PresagePredictionManager(context);
    if (!manager.activate()) {
      throw new AssertionError(
          "Presage predictor failed to activate with bundled model: "
              + manager.getLastActivationError());
    }
    manager.deactivate();
  }

  private void ensureNgramModelActive(@NonNull Context context) {
    ensureKenlmModelActive(context);
  }

  private boolean ensureMixedcaseNeuralModelActive(@NonNull Context context) throws Exception {
    final ModelStore store = new ModelStore(context);
    final ModelStore.ActiveModel activeModel = store.ensureActiveModel(EngineType.NEURAL);
    if (activeModel != null
        && DEFAULT_TEST_NEURAL_MODEL_ID.equals(activeModel.getDefinition().getId())) {
      return true;
    }

    final ModelDefinition defForEntry =
        ModelDefinition.builder(DEFAULT_TEST_NEURAL_MODEL_ID)
            .setLabel("DistilGPT-2 mixedcase (sanity)")
            .setEngineType(EngineType.NEURAL)
            .setOnnxFile("model_int8.onnx", null, null)
            .setTokenizerVocabFile("vocab.json", null, null)
            .setTokenizerMergesFile("merges.txt", null, null)
            .build();
    final PresageModelCatalog.CatalogEntry target =
        new PresageModelCatalog.CatalogEntry(
            defForEntry,
            DEFAULT_TEST_NEURAL_MODEL_BUNDLE_URL,
            "06dbfa67aed36b24c931dabdb10060b0e93b4af5cbf123c1ce7358b26fec13d4",
            53587027L,
            1,
            false);

    final ModelDownloader downloader = new ModelDownloader(context, store);
    try {
      DownloaderCompat.run(downloader, target);
    } catch (Exception exception) {
      final ModelStore.ActiveModel fallback = store.ensureActiveModel(EngineType.NEURAL);
      if (fallback == null) {
        Log.w(
            TAG,
            "Neural model download failed; neural/hybrid UI-chain eval will be skipped.",
            exception);
        return false;
      }
      Log.w(TAG, "Neural model download failed; continuing with installed model.", exception);
    }
    store.persistSelectedModelId(EngineType.NEURAL, DEFAULT_TEST_NEURAL_MODEL_ID);

    final NeuralPredictionManager manager = new NeuralPredictionManager(context);
    if (!manager.activate()) {
      Log.w(TAG, "Neural predictor failed to activate with mixedcase model");
      return false;
    }
    manager.deactivate();
    return true;
  }

  private static ModelDefinition stageFixtureNgramModel(@NonNull Context targetContext) {
    final File modelDir =
        new File(
            targetContext.getNoBackupFilesDir(),
            "presage" + File.separator + "models" + File.separator + FIXTURE_NGRAM_MODEL_ID);
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

    writeFile(new File(modelDir, "fixture.arpa"), FIXTURE_ARPA);
    writeFile(new File(modelDir, "fixture.vocab"), FIXTURE_VOCAB);

    final File manifest = new File(modelDir, "manifest.json");
    try (FileOutputStream outputStream = new FileOutputStream(manifest)) {
      outputStream.write(definition.toJson().toString(2).getBytes(StandardCharsets.UTF_8));
    } catch (Exception exception) {
      throw new AssertionError("Failed writing fixture model manifest", exception);
    }

    return definition;
  }

  private static void writeFile(@NonNull File file, @NonNull String contents) {
    try (FileOutputStream outputStream = new FileOutputStream(file)) {
      outputStream.write(contents.getBytes(StandardCharsets.UTF_8));
    } catch (Exception exception) {
      throw new AssertionError(
          "Failed writing fixture model file " + file.getAbsolutePath(), exception);
    }
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

  private void ensureImeEnabledAndSelected() throws IOException {
    mImeComponent = resolveImeComponentId();
    final String enableOutput = executeShellCommand("ime enable --user 0 " + mImeComponent).trim();
    if (enableOutput.contains("Unknown") || enableOutput.contains("Error")) {
      throw new IOException("Failed to enable IME. Output: " + enableOutput);
    }
    executeShellCommand("ime set --user 0 " + mImeComponent);

    final String enabled = executeShellCommand("settings get secure enabled_input_methods").trim();
    final String expanded = expandComponent(mImeComponent);
    if (!enabled.contains(mImeComponent) && !enabled.contains(expanded)) {
      final String prefix = enabled.isEmpty() ? "" : enabled + ":";
      executeShellCommand(
          "settings put secure enabled_input_methods \"" + prefix + mImeComponent + "\"");
    }
    executeShellCommand("settings put secure show_ime_with_hard_keyboard 1");
    SystemClock.sleep(400);
  }

  private void assertImeSelected() throws IOException {
    final String current = executeShellCommand("settings get secure default_input_method").trim();
    final String expanded = expandComponent(mImeComponent);
    if (!(current.equals(mImeComponent) || current.equals(expanded))) {
      throw new AssertionError(
          "NewSoftKeyboard IME not selected. Expected: " + mImeComponent + " Current: " + current);
    }
  }

  private String resolveImeComponentId() throws IOException {
    final String list = executeShellCommand("ime list -a -s").trim();
    final String[] lines = list.split("\\n");
    final String prefix = getAppPackage() + "/";
    String fallback = null;
    for (String line : lines) {
      final String trimmed = line.trim();
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
    executeShellCommand("input keyevent 82");
    SystemClock.sleep(300);
  }

  private static String expandComponent(String component) {
    String[] parts = component.split("/", 2);
    if (parts.length != 2) return component;
    String pkg = parts[0];
    String svc = parts[1];
    if (!svc.startsWith(".")) return component;
    return pkg + "/" + pkg + svc;
  }
}
