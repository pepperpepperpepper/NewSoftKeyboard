package wtf.uhoh.newsoftkeyboard.app.dictionaries.nextword;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;
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

/**
 * Produces a human-readable "tap the next-word strip in series" report, intended for demos and
 * review.
 *
 * <p>Outputs a gzip+base64 encoded JSON blob on stdout between markers:
 *
 * <pre>
 * NEXTWORD_TAPCHAIN_REPORT_JSON_BEGIN
 * NWJSONGZ:000000:&lt;base64 chunk&gt;
 * NWJSONGZ:000001:&lt;base64 chunk&gt;
 * NEXTWORD_TAPCHAIN_REPORT_JSON_END
 * </pre>
 *
 * <p>This test is intentionally best-effort: it records timeouts/errors into the report instead of
 * failing fast.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class NextWordTapChainReportUiAutomatorTest {

  private static final String TAG = "NextWordTapChainRpt";

  public static final String REPORT_BEGIN = "NEXTWORD_TAPCHAIN_REPORT_JSON_BEGIN";
  public static final String REPORT_END = "NEXTWORD_TAPCHAIN_REPORT_JSON_END";
  private static final int REPORT_CHUNK_CHARS = 1800;

  private static final long READY_TIMEOUT_MS = 10_000L;
  private static final long SUGGESTIONS_TIMEOUT_MS = 15_000L;
  private static final long EDITOR_UPDATE_TIMEOUT_MS = 6_000L;
  private static final long SHORT_WAIT_MS = 400L;
  private static final long HYBRID_ASYNC_REFRESH_TIMEOUT_MS = 1500L;

  private UiDevice mDevice;
  @Nullable private ActivityScenario<TestInputActivity> mScenario;
  private String mImeComponent;

  private static String getAppPackage() {
    return InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName();
  }

  private static String resId(String idName) {
    return getAppPackage() + ":id/" + idName;
  }

  @Before
  public void setUp() throws Exception {
    mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    clearLogcat();
    wakeAndUnlockDevice();

    ensureImeEnabledAndSelected();
    assertImeSelected();

    final Context context = ApplicationProvider.getApplicationContext();
    ensureNgramModelActive(context);
    ensureMixedcaseNeuralModelActive(context);

    // Common prefs for all cases.
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(context);
    prefs
        .edit()
        .putBoolean(context.getString(R.string.settings_key_show_suggestions), true)
        // For Keep-like editors, we still want to observe predictions for this report.
        .putBoolean(context.getString(R.string.settings_key_respect_app_no_suggestions_flag), false)
        .putBoolean(context.getString(R.string.settings_key_auto_space), true)
        .putString(context.getString(R.string.settings_key_next_word_dictionary_type), "words")
        .putString(
            context.getString(R.string.settings_key_next_word_suggestion_aggressiveness),
            "maximum_aggressiveness")
        .apply();
  }

  @After
  public void tearDown() {
    closeScenarioIfOpen();
  }

  @Test
  public void generateTapChainReport() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();

    final JSONObject root = new JSONObject();
    root.put("schemaVersion", 4);
    final java.text.SimpleDateFormat fmt =
        new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    fmt.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
    root.put("exportedAtUtc", fmt.format(new java.util.Date()));

    final JSONObject meta = new JSONObject();
    meta.put("device_model", Build.MODEL);
    meta.put("device_manufacturer", Build.MANUFACTURER);
    meta.put("android_sdk", Build.VERSION.SDK_INT);
    meta.put("android_release", Build.VERSION.RELEASE);
    meta.put("build_fingerprint", Build.FINGERPRINT);
    meta.put("device", (Build.MANUFACTURER + " " + Build.MODEL).trim());
    meta.put("android", Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")");
    meta.put("lang", Locale.getDefault().toString());

    try {
      final android.content.pm.PackageInfo info =
          context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
      meta.put("app_version_name", info.versionName);
      String versionCodeLabel = "";
      if (Build.VERSION.SDK_INT >= 28) {
        final long versionCode = info.getLongVersionCode();
        meta.put("app_version_code", versionCode);
        versionCodeLabel = String.valueOf(versionCode);
      } else {
        //noinspection deprecation
        meta.put("app_version_code", info.versionCode);
        versionCodeLabel = String.valueOf(info.versionCode);
      }
      final String versionName = info.versionName == null ? "" : info.versionName;
      meta.put(
          "nsk",
          versionName.isEmpty()
              ? ("versionCode " + versionCodeLabel)
              : (versionName + " (" + versionCodeLabel + ")"));
    } catch (Exception ignored) {
      // best-effort
    }

    final ModelStore store = new ModelStore(context);
    final ModelStore.ActiveModel ngramModel = store.ensureActiveModel(EngineType.NGRAM);
    final ModelStore.ActiveModel neuralModel = store.ensureActiveModel(EngineType.NEURAL);
    meta.put("ngram_model_id", ngramModel == null ? "none" : ngramModel.getDefinition().getId());
    meta.put("neural_model_id", neuralModel == null ? "none" : neuralModel.getDefinition().getId());
    root.put("meta", meta);

    final JSONObject results = new JSONObject();
    final String onlyCaseIdArg =
        InstrumentationRegistry.getArguments().getString("onlyCaseId", "").trim();
    final java.util.Set<String> onlyCaseIds = new java.util.LinkedHashSet<>();
    if (!onlyCaseIdArg.isEmpty()) {
      for (String raw : onlyCaseIdArg.split("[,\\s]+")) {
        if (raw == null) continue;
        final String token = raw.trim();
        if (!token.isEmpty()) onlyCaseIds.add(token);
      }
      Log.i(TAG, "Filtering tap-chain report to cases: " + onlyCaseIds);
    }
    final boolean runAllCases = onlyCaseIds.isEmpty();

    // 1) Markov/learned dictionary (engine mode "none") deterministic chain.
    if (runAllCases || onlyCaseIds.contains("markov-chain-1")) {
      results.put(
          "markov-chain-1",
          runMarkovDeterministicChain(
              /* id= */ "markov-chain-1",
              /* trainingPhrase= */ "alpha beta gamma delta ",
              /* repetitions= */ 3,
              /* seed= */ "alpha ",
              /* picks= */ 3));
    }

    // 2) Markov branching ranking.
    if (runAllCases || onlyCaseIds.contains("markov-branching")) {
      results.put(
          "markov-branching",
          runMarkovBranchingRanking(
              /* id= */ "markov-branching",
              /* trainDominant= */ "alpha beta gamma ",
              /* dominantReps= */ 3,
              /* trainMinor= */ "alpha bingo bango ",
              /* minorReps= */ 1,
              /* seed= */ "alpha "));
    }

    // 3) Engine-mode fixture chain (cross-engine comparability).
    if (runAllCases || onlyCaseIds.contains("fixture-the-chain")) {
      results.put(
          "fixture-the-chain",
          runFixtureTheChainAllEngineModes(/* id= */ "fixture-the-chain", /* picks= */ 19));
    }

    // 4) Neural/hybrid "golden phrase" + domain-token check.
    if (runAllCases || onlyCaseIds.contains("neural-golden-keep-me-informed")) {
      results.put(
          "neural-golden-keep-me-informed",
          runNeuralGoldenPhraseKeepMe(/* id= */ "neural-golden-keep-me-informed", /* picks= */ 6));
    }

    // 5) Neural/hybrid free-run story prompt.
    if (runAllCases || onlyCaseIds.contains("compare-free-run-2")) {
      results.put(
          "compare-free-run-2",
          runNeuralFreeRun(
              /* id= */ "compare-free-run-2", /* seed= */ "Once upon a ", /* picks= */ 12));
    }

    // 6) Context sensitivity: money vs. water.
    if (runAllCases || onlyCaseIds.contains("neural-context-sensitivity")) {
      results.put(
          "neural-context-sensitivity",
          runNeuralContextSensitivity(/* id= */ "neural-context-sensitivity"));
    }

    // 7) Degeneration/repetition check.
    if (runAllCases || onlyCaseIds.contains("neural-degeneration")) {
      results.put(
          "neural-degeneration",
          runNeuralFreeRun(
              /* id= */ "neural-degeneration", /* seed= */ "I think ", /* picks= */ 18));
    }

    // 8) Context visibility: how much prior editor text we can read/seed.
    if (runAllCases || onlyCaseIds.contains("context-visibility")) {
      results.put("context-visibility", runContextVisibility(/* id= */ "context-visibility"));
    }

    // 9) Edge-case prompts: tokenization + filtering (URLs, emails, paths, numbers, newlines).
    if (runAllCases || onlyCaseIds.contains("edge-cases-tokenization")) {
      results.put(
          "edge-cases-tokenization", runEdgeCasesTokenization(/* id= */ "edge-cases-tokenization"));
    }

    // 10) Longer "real app" contexts: paragraphs/notes/chat threads (top suggestions only).
    if (runAllCases || onlyCaseIds.contains("long-form-context")) {
      results.put("long-form-context", runLongFormContext(/* id= */ "long-form-context"));
    }

    root.put("results", results);

    final String pretty = root.toString(2);
    final byte[] gz = gzip(pretty);
    final String base64 = Base64.encodeToString(gz, Base64.NO_WRAP);
    Log.i(
        TAG,
        "Generated tap-chain report ("
            + pretty.length()
            + " chars, gzip="
            + gz.length
            + " bytes, base64="
            + base64.length()
            + " chars).");
    System.out.println(REPORT_BEGIN);
    int lineNo = 0;
    for (int start = 0; start < base64.length(); start += REPORT_CHUNK_CHARS) {
      final int end = Math.min(start + REPORT_CHUNK_CHARS, base64.length());
      System.out.println(
          String.format(Locale.US, "NWJSONGZ:%06d:%s", lineNo++, base64.substring(start, end)));
    }
    System.out.println(REPORT_END);
  }

  @NonNull
  private static byte[] gzip(@NonNull String content) throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
      gzip.write(content.getBytes(StandardCharsets.UTF_8));
    }
    return baos.toByteArray();
  }

  @NonNull
  private JSONObject runMarkovDeterministicChain(
      @NonNull String id,
      @NonNull String trainingPhrase,
      int repetitions,
      @NonNull String seed,
      int picks)
      throws Exception {
    final JSONObject byMode = new JSONObject();
    final JSONObject modeResult = new JSONObject();

    configureEngineMode("none");
    configureNextWordAggressiveness("medium_aggressiveness");

    final RunOutput out =
        runCaseSingleScenario(
            id,
            /* mode= */ "none",
            /* noPersonalizedLearning= */ false,
            /* noSuggestionsFlag= */ false,
            /* clearLearningData= */ true,
            /* trainingSteps= */ repeat(trainingPhrase, repetitions),
            /* seed= */ seed,
            /* picks= */ picks);
    modeResult.put("output", out.renderedOutput);
    modeResult.put("runs", new JSONArray().put(runOutputToJson(out)));
    if (out.error != null) modeResult.put("error", out.error);
    byMode.put("none", modeResult);
    return byMode;
  }

  @NonNull
  private JSONObject runMarkovBranchingRanking(
      @NonNull String id,
      @NonNull String trainDominant,
      int dominantReps,
      @NonNull String trainMinor,
      int minorReps,
      @NonNull String seed)
      throws Exception {
    final JSONObject byMode = new JSONObject();
    final JSONObject modeResult = new JSONObject();

    configureEngineMode("none");
    configureNextWordAggressiveness("medium_aggressiveness");

    final List<String> training = new ArrayList<>();
    training.addAll(repeat(trainDominant, dominantReps));
    training.addAll(repeat(trainMinor, minorReps));

    final RunOutput out =
        runCaseSingleScenario(
            id,
            /* mode= */ "none",
            /* noPersonalizedLearning= */ false,
            /* noSuggestionsFlag= */ false,
            /* clearLearningData= */ true,
            /* trainingSteps= */ training,
            /* seed= */ seed,
            /* picks= */ 0);
    modeResult.put("output", out.renderedOutput);
    modeResult.put("runs", new JSONArray().put(runOutputToJson(out)));
    if (out.error != null) modeResult.put("error", out.error);
    byMode.put("none", modeResult);
    return byMode;
  }

  @NonNull
  private JSONObject runFixtureTheChainAllEngineModes(@NonNull String id, int picks)
      throws Exception {
    final JSONObject byMode = new JSONObject();
    for (String mode : new String[] {"ngram", "neural", "hybrid"}) {
      configureEngineMode(mode);
      configureNextWordAggressiveness("maximum_aggressiveness");

      final JSONObject modeResult = new JSONObject();
      final StringBuilder combined = new StringBuilder();

      final RunOutput normal =
          runCaseSingleScenario(
              id,
              mode,
              /* noPersonalizedLearning= */ false,
              /* noSuggestionsFlag= */ false,
              /* clearLearningData= */ false,
              /* trainingSteps= */ java.util.Collections.emptyList(),
              /* seed= */ "the ",
              picks);
      combined.append("NORMAL:\n").append(normal.renderedOutput).append("\n");
      if (normal.error != null) {
        combined.append("\nNORMAL_ERROR: ").append(normal.error).append("\n");
      }

      final RunOutput keep =
          runCaseSingleScenario(
              id,
              mode,
              /* noPersonalizedLearning= */ true,
              /* noSuggestionsFlag= */ true,
              /* clearLearningData= */ false,
              /* trainingSteps= */ java.util.Collections.emptyList(),
              /* seed= */ "the ",
              picks);
      combined.append("\nKEEP_FLAGS:\n").append(keep.renderedOutput).append("\n");
      if (keep.error != null) {
        combined.append("\nKEEP_FLAGS_ERROR: ").append(keep.error).append("\n");
      }

      modeResult.put("output", combined.toString().trim());
      modeResult.put(
          "runs",
          new JSONArray()
              .put(runOutputToJson(normal, "NORMAL"))
              .put(runOutputToJson(keep, "KEEP_FLAGS")));
      byMode.put(mode, modeResult);
    }
    return byMode;
  }

  @NonNull
  private JSONObject runNeuralGoldenPhraseKeepMe(@NonNull String id, int picks) throws Exception {
    final JSONObject byMode = new JSONObject();
    for (String mode : new String[] {"neural", "hybrid"}) {
      configureEngineMode(mode);
      configureNextWordAggressiveness("maximum_aggressiveness");

      final JSONObject modeResult = new JSONObject();
      final StringBuilder combined = new StringBuilder();

      final RunOutput normal =
          runCaseSingleScenario(
              id,
              mode,
              /* noPersonalizedLearning= */ true,
              /* noSuggestionsFlag= */ false,
              /* clearLearningData= */ false,
              /* trainingSteps= */ java.util.Collections.emptyList(),
              /* seed= */ "keep me ",
              picks);
      combined.append("SEED: keep me \n").append(normal.renderedOutput);

      // Lightweight domain-token scan for visibility.
      final boolean hasDomain = containsDomainLikeTokens(normal.startSuggestions);
      if (hasDomain) {
        combined.append("\nWARNING: domain-like token detected in start suggestions.");
      }
      if (normal.error != null) {
        combined.append("\nERROR: ").append(normal.error);
      }

      modeResult.put("output", combined.toString().trim());
      modeResult.put("runs", new JSONArray().put(runOutputToJson(normal)));
      if (hasDomain)
        modeResult.put(
            "warnings", new JSONArray().put("Domain-like token detected in start suggestions."));
      byMode.put(mode, modeResult);
    }
    return byMode;
  }

  @NonNull
  private JSONObject runEdgeCasesTokenization(@NonNull String id) throws Exception {
    final JSONObject byMode = new JSONObject();

    final List<Prompt> prompts =
        Arrays.asList(
            new Prompt("URL (embedded)", "I bookmarked https://example.com because it "),
            new Prompt("Email (embedded)", "Contact me at test@example.com when you "),
            new Prompt("Path (embedded)", "The logs are in /var/log and they "),
            new Prompt("Money/number", "It costs $5.99 and it "),
            new Prompt("Multiline", "First line.\nSecond line and then "),
            new Prompt("Emoji (inline)", "I feel so 😊 when I "),
            new Prompt("Emoji (end)", "This is great 😂 "),
            new Prompt("Mixed language (ES/EN)", "Gracias for your "),
            new Prompt("Mixed language (CJK/EN)", "今天我去了北京 and then I "),
            new Prompt("Quotes", "He said, \"I will "),
            new Prompt("Parentheses", "The function (which "),
            new Prompt("Sentence boundary", "I finished. Then I "),
            new Prompt("Ellipsis", "Well... I "),
            new Prompt("Hyphenated", "State-of-the-art "),
            new Prompt("Hashtag", "Trending #ai and "),
            new Prompt("Time", "Meet me at 5:30 pm and "),
            new Prompt("Percent", "Battery is at 12% so "));

    for (String mode : new String[] {"ngram", "neural", "hybrid"}) {
      configureEngineMode(mode);
      configureNextWordAggressiveness("maximum_aggressiveness");

      final JSONObject modeResult = new JSONObject();
      final JSONArray runs = new JSONArray();
      final JSONArray warnings = new JSONArray();

      for (Prompt p : prompts) {
        final RunOutput out =
            runCaseSingleScenario(
                id,
                mode,
                /* noPersonalizedLearning= */ true,
                /* noSuggestionsFlag= */ false,
                /* clearLearningData= */ false,
                /* trainingSteps= */ java.util.Collections.emptyList(),
                /* seed= */ p.seed,
                /* picks= */ 0);
        runs.put(runOutputToJson(out, p.label));

        if (containsDomainLikeTokens(out.startSuggestions)) {
          warnings.put(p.label + ": domain-like token detected in start suggestions.");
        }
      }

      modeResult.put("runs", runs);
      if (warnings.length() > 0) modeResult.put("warnings", warnings);
      byMode.put(mode, modeResult);
    }

    return byMode;
  }

  @NonNull
  private JSONObject runLongFormContext(@NonNull String id) throws Exception {
    final JSONObject byMode = new JSONObject();

    final String filler =
        "This is filler text to push older context out of the window. "
            + "It should not matter once we exceed the tokenizer limit. ";
    final StringBuilder overflow = new StringBuilder();
    overflow.append("BEGIN_SENTINEL ");
    for (int i = 0; i < 30; i++) overflow.append(filler);
    overflow.append("END_SENTINEL ");
    overflow.append(
        "Now that there is a lot of preceding text, the keyboard should only use the most recent"
            + " context when it suggests the ");

    final List<Prompt> prompts =
        Arrays.asList(
            new Prompt(
                "Long note (paragraphs)",
                "Today I'm writing a longer note to test next-word suggestions. On a phone"
                    + " keyboard, predictions should stay stable and useful even when the text area"
                    + " already contains a lot of text. Sometimes I pause, move the cursor, and"
                    + " continue typing. Other times I paste content from an email or a document"
                    + " and then add one more sentence.\n\n"
                    + "In this experiment, I'm going to keep typing until the keyboard has enough"
                    + " context to suggest the "),
            new Prompt(
                "Long chat (multiline)",
                ""
                    + "Alex: Are we still on for dinner tonight?\n"
                    + "Me: Yes—let's meet at 7.\n"
                    + "Alex: Perfect. Also, can you bring the charger?\n"
                    + "Me: Sure. I'll bring the "),
            new Prompt(
                "Long todo list",
                ""
                    + "Project TODO:\n"
                    + "- review the PR\n"
                    + "- update the docs\n"
                    + "- run the tests\n"
                    + "- send the status update\n"
                    + "\n"
                    + "After that, we should schedule the "),
            new Prompt(
                "Long technical paragraph",
                "I'm debugging next-word prediction. The engine merges candidates from multiple"
                    + " sources, normalizes them, filters domain-like tokens, and then ranks the"
                    + " final strip. Performance matters because latency can cause jank in the UI."
                    + " Given all of that, the next change I want to make is to improve the "),
            new Prompt("Context overflow (sentinel)", overflow.toString()));

    for (String mode : new String[] {"ngram", "neural", "hybrid"}) {
      configureEngineMode(mode);
      configureNextWordAggressiveness("maximum_aggressiveness");

      final JSONObject modeResult = new JSONObject();
      final JSONArray runs = new JSONArray();

      for (Prompt p : prompts) {
        final RunOutput out =
            runCaseSingleScenario(
                id,
                mode,
                /* noPersonalizedLearning= */ true,
                /* noSuggestionsFlag= */ false,
                /* clearLearningData= */ false,
                /* trainingSteps= */ java.util.Collections.emptyList(),
                /* seed= */ p.seed,
                /* picks= */ 0);
        runs.put(runOutputToJson(out, p.label));
      }

      modeResult.put("runs", runs);
      byMode.put(mode, modeResult);
    }

    return byMode;
  }

  @NonNull
  private JSONObject runNeuralFreeRun(@NonNull String id, @NonNull String seed, int picks)
      throws Exception {
    final JSONObject byMode = new JSONObject();
    for (String mode : new String[] {"neural", "hybrid"}) {
      configureEngineMode(mode);
      configureNextWordAggressiveness("maximum_aggressiveness");

      final RunOutput out =
          runCaseSingleScenario(
              id,
              mode,
              /* noPersonalizedLearning= */ true,
              /* noSuggestionsFlag= */ false,
              /* clearLearningData= */ false,
              /* trainingSteps= */ java.util.Collections.emptyList(),
              /* seed= */ seed,
              picks);
      final JSONObject modeResult = new JSONObject();
      modeResult.put("output", out.renderedOutput);
      modeResult.put("runs", new JSONArray().put(runOutputToJson(out)));
      if (out.error != null) modeResult.put("error", out.error);
      byMode.put(mode, modeResult);
    }
    return byMode;
  }

  @NonNull
  private JSONObject runNeuralContextSensitivity(@NonNull String id) throws Exception {
    final JSONObject byMode = new JSONObject();
    for (String mode : new String[] {"neural", "hybrid"}) {
      configureEngineMode(mode);
      configureNextWordAggressiveness("maximum_aggressiveness");

      final RunOutput a =
          runCaseSingleScenario(
              id,
              mode,
              /* noPersonalizedLearning= */ true,
              /* noSuggestionsFlag= */ false,
              /* clearLearningData= */ false,
              /* trainingSteps= */ java.util.Collections.emptyList(),
              /* seed= */ "I deposited money in the ",
              /* picks= */ 0);
      final RunOutput b =
          runCaseSingleScenario(
              id,
              mode,
              /* noPersonalizedLearning= */ true,
              /* noSuggestionsFlag= */ false,
              /* clearLearningData= */ false,
              /* trainingSteps= */ java.util.Collections.emptyList(),
              /* seed= */ "I sat on the bank of the ",
              /* picks= */ 0);

      final String output =
          ""
              + "Case A: I deposited money in the\n"
              + "Top suggestions: "
              + a.startSuggestionsCsv
              + "\n\n"
              + "Case B: I sat on the bank of the\n"
              + "Top suggestions: "
              + b.startSuggestionsCsv
              + (a.error != null ? "\n\nCase A error: " + a.error : "")
              + (b.error != null ? "\n\nCase B error: " + b.error : "");

      final JSONObject modeResult = new JSONObject();
      modeResult.put("output", output.trim());
      modeResult.put(
          "runs",
          new JSONArray()
              .put(runOutputToJson(a, "CASE_A_MONEY"))
              .put(runOutputToJson(b, "CASE_B_WATER")));
      byMode.put(mode, modeResult);
    }
    return byMode;
  }

  private static boolean containsDomainLikeTokens(@Nullable List<String> suggestions) {
    if (suggestions == null || suggestions.isEmpty()) return false;
    for (String s : suggestions) {
      if (s == null) continue;
      final String t = s.trim().toLowerCase(Locale.ROOT);
      if (t.isEmpty()) continue;
      if ("com".equals(t)
          || "www".equals(t)
          || "http".equals(t)
          || "https".equals(t)
          || t.startsWith("http://")
          || t.startsWith("https://")
          || t.startsWith("www.")) {
        return true;
      }
    }
    return false;
  }

  private static final class Prompt {
    @NonNull final String label;
    @NonNull final String seed;

    Prompt(@NonNull String label, @NonNull String seed) {
      this.label = label;
      this.seed = seed;
    }
  }

  @NonNull
  private JSONObject runContextVisibility(@NonNull String id) throws Exception {
    final JSONObject byMode = new JSONObject();
    for (String mode : new String[] {"neural", "hybrid"}) {
      configureEngineMode(mode);
      configureNextWordAggressiveness("maximum_aggressiveness");

      final String seed = "I deposited money in the ";
      final String prefillText = seed.trim();
      final String largeMultilinePrefillText = buildLargeMultilinePrefillText(seed);

      final RunOutput normal =
          runContextVisibilitySingleScenario(
              id,
              mode,
              /* noPersonalizedLearning= */ false,
              /* noSuggestionsFlag= */ false,
              /* simulateInvisibleComposing= */ false,
              seed);
      final RunOutput readbackNull =
          runContextVisibilitySingleScenario(
              id,
              mode,
              /* noPersonalizedLearning= */ false,
              /* noSuggestionsFlag= */ false,
              /* simulateInvisibleComposing= */ true,
              seed);
      final RunOutput keepFlags =
          runContextVisibilitySingleScenario(
              id,
              mode,
              /* noPersonalizedLearning= */ true,
              /* noSuggestionsFlag= */ true,
              /* simulateInvisibleComposing= */ false,
              seed);
      final RunOutput resumeSameField =
          runContextVisibilityResumeSameFieldSingleScenario(
              id,
              mode,
              /* noPersonalizedLearning= */ false,
              /* noSuggestionsFlag= */ false,
              /* simulateInvisibleComposing= */ false,
              seed);
      final RunOutput resumeSameFieldKeepFlags =
          runContextVisibilityResumeSameFieldSingleScenario(
              id,
              mode,
              /* noPersonalizedLearning= */ true,
              /* noSuggestionsFlag= */ true,
              /* simulateInvisibleComposing= */ false,
              seed);
      final RunOutput prepopulated =
          runContextVisibilityPrepopulatedSingleScenario(
              id,
              mode,
              /* noPersonalizedLearning= */ false,
              /* noSuggestionsFlag= */ false,
              /* simulateInvisibleComposing= */ false,
              prefillText,
              /* prefillCursorPosition= */ -1,
              /* editorInputTypeOverride= */ -1);
      final String prefillCursorInside = prefillText + " bank";
      final RunOutput prepopulatedCursorBeforeWord =
          runContextVisibilityPrepopulatedSingleScenario(
              id,
              mode,
              /* noPersonalizedLearning= */ false,
              /* noSuggestionsFlag= */ false,
              /* simulateInvisibleComposing= */ false,
              prefillCursorInside,
              /* prefillCursorPosition= */ prefillCursorInside.indexOf("bank"),
              /* editorInputTypeOverride= */ -1);
      final RunOutput prepopulatedCursorInsideWord =
          runContextVisibilityPrepopulatedSingleScenario(
              id,
              mode,
              /* noPersonalizedLearning= */ false,
              /* noSuggestionsFlag= */ false,
              /* simulateInvisibleComposing= */ false,
              prefillCursorInside,
              /* prefillCursorPosition= */ prefillCursorInside.indexOf("bank") + 2,
              /* editorInputTypeOverride= */ -1);
      final RunOutput prepopulatedNoSuggestions =
          runContextVisibilityPrepopulatedSingleScenario(
              id,
              mode,
              /* noPersonalizedLearning= */ false,
              /* noSuggestionsFlag= */ true,
              /* simulateInvisibleComposing= */ false,
              prefillText,
              /* prefillCursorPosition= */ -1,
              /* editorInputTypeOverride= */ -1);
      final RunOutput prepopulatedNoPersonalizedLearning =
          runContextVisibilityPrepopulatedSingleScenario(
              id,
              mode,
              /* noPersonalizedLearning= */ true,
              /* noSuggestionsFlag= */ false,
              /* simulateInvisibleComposing= */ false,
              prefillText,
              /* prefillCursorPosition= */ -1,
              /* editorInputTypeOverride= */ -1);
      final RunOutput prepopulatedIncognito =
          runContextVisibilityPrepopulatedIncognitoSingleScenario(
              id,
              mode,
              /* noPersonalizedLearning= */ false,
              /* noSuggestionsFlag= */ false,
              /* simulateInvisibleComposing= */ false,
              prefillText,
              /* prefillCursorPosition= */ -1,
              /* editorInputTypeOverride= */ -1);
      final RunOutput prepopulatedLargeMultiline =
          runContextVisibilityPrepopulatedSingleScenario(
              id,
              mode,
              /* noPersonalizedLearning= */ false,
              /* noSuggestionsFlag= */ false,
              /* simulateInvisibleComposing= */ false,
              largeMultilinePrefillText,
              /* prefillCursorPosition= */ -1,
              /* editorInputTypeOverride= */ -1);
      final RunOutput prepopulatedPassword =
          runContextVisibilityPrepopulatedSingleScenario(
              id,
              mode,
              /* noPersonalizedLearning= */ false,
              /* noSuggestionsFlag= */ false,
              /* simulateInvisibleComposing= */ false,
              prefillText,
              /* prefillCursorPosition= */ -1,
              /* editorInputTypeOverride= */ InputType.TYPE_CLASS_TEXT
                  | InputType.TYPE_TEXT_VARIATION_PASSWORD);
      final RunOutput cursorMove =
          runContextVisibilityCursorMoveSingleScenario(
              id,
              mode,
              /* noPersonalizedLearning= */ false,
              /* noSuggestionsFlag= */ false,
              /* simulateInvisibleComposing= */ false);
      final RunOutput cursorMoveInsert =
          runContextVisibilityCursorMoveInsertSingleScenario(
              id,
              mode,
              /* noPersonalizedLearning= */ false,
              /* noSuggestionsFlag= */ false,
              /* simulateInvisibleComposing= */ false);

      final JSONObject modeResult = new JSONObject();
      modeResult.put(
          "runs",
          new JSONArray()
              .put(runOutputToJson(normal, "NORMAL (reset → reseed)"))
              .put(
                  runOutputToJson(
                      readbackNull, "READBACK_NULL (getTextBeforeCursor=null; extracted fallback)"))
              .put(runOutputToJson(keepFlags, "KEEP_FLAGS (seeding blocked)"))
              .put(runOutputToJson(resumeSameField, "RESUME_SAME_FIELD (pause/resume activity)"))
              .put(
                  runOutputToJson(
                      resumeSameFieldKeepFlags, "RESUME_SAME_FIELD_KEEP_FLAGS (blocked seeding)"))
              .put(runOutputToJson(prepopulated, "PREPOPULATED (onStartInputView seed)"))
              .put(
                  runOutputToJson(
                      prepopulatedCursorBeforeWord, "PREPOPULATED (cursor before word)"))
              .put(
                  runOutputToJson(
                      prepopulatedCursorInsideWord, "PREPOPULATED (cursor inside word)"))
              .put(
                  runOutputToJson(prepopulatedNoSuggestions, "PREPOPULATED (NO_SUGGESTIONS field)"))
              .put(
                  runOutputToJson(
                      prepopulatedNoPersonalizedLearning,
                      "PREPOPULATED (IME_FLAG_NO_PERSONALIZED_LEARNING field)"))
              .put(runOutputToJson(prepopulatedIncognito, "PREPOPULATED (incognito mode)"))
              .put(
                  runOutputToJson(
                      prepopulatedLargeMultiline, "PREPOPULATED (large multiline note)"))
              .put(runOutputToJson(prepopulatedPassword, "PREPOPULATED (password field)"))
              .put(runOutputToJson(cursorMove, "CURSOR_MOVE (between newlines)"))
              .put(
                  runOutputToJson(
                      cursorMoveInsert, "CURSOR_MOVE_INSERT (between newlines; insert token)")));
      byMode.put(mode, modeResult);
    }
    return byMode;
  }

  @NonNull
  private static String buildLargeMultilinePrefillText(@NonNull String tail) {
    final String line =
        "This is a long note line with several words to exercise editor readback and truncation.\n";
    final StringBuilder sb = new StringBuilder();
    while (sb.length() < 5200) sb.append(line);
    sb.append("\n").append(tail);
    return sb.toString();
  }

  @NonNull
  private RunOutput runContextVisibilitySingleScenario(
      @NonNull String caseId,
      @NonNull String mode,
      boolean noPersonalizedLearning,
      boolean noSuggestionsFlag,
      boolean simulateInvisibleComposing,
      @NonNull String seed)
      throws Exception {
    final RunOutput out = new RunOutput();
    out.caseId = caseId;
    out.mode = mode;
    out.noPersonalizedLearning = noPersonalizedLearning;
    out.noSuggestionsFlag = noSuggestionsFlag;
    out.seed = seed;
    out.picks = 0;

    final String[] seedAttemptJson = {""};
    try {
      launchTestHarness(
          noPersonalizedLearning,
          noSuggestionsFlag,
          simulateInvisibleComposing,
          /* simulateTypeNull= */ false);
      waitForNonEmptySuggestionsBestEffort();

      // Ensure the editor content is empty before we write the seed.
      mScenario.onActivity(
          activity -> {
            final EditText editText = activity.findViewById(R.id.test_edit_text);
            if (editText != null) editText.setText("");
          });
      SystemClock.sleep(120);
      mScenario.onActivity(
          activity ->
              wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.commitTextNoSuggestionsForTest(seed));
      waitForEditorTextToEndWithSpaceBestEffort();
      SystemClock.sleep(120);

      // Stage A: simulate a "restart" of next-word state by clearing in-memory context.
      mScenario.onActivity(
          activity -> {
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.clearSuggestionsForTest();
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.resetNeuralInferenceSamplesForTest();
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.clearNextWordPipelineDebugStateForTest();
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.resetNextWordSentence();
          });
      SystemClock.sleep(120);

      int baselineAsyncListenerCount = getHybridNeuralAsyncListenerInvocationCountForTest();
      mScenario.onActivity(
          activity ->
              wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                  .forceNextWordFromLastCommittedWordForTest());
      SystemClock.sleep(80);
      waitForNonEmptySuggestionsQuickBestEffort(/* timeoutMs= */ 2500L);
      waitForNonEmptySuggestionsBestEffort();

      if ("hybrid".equals(mode) || "neural".equals(mode)) {
        out.hybridTelemetryInitial = readHybridNeuralAsyncTelemetryBestEffort();
        waitForHybridNeuralAsyncSettleBestEffort(
            baselineAsyncListenerCount,
            out.hybridTelemetryInitial,
            HYBRID_ASYNC_REFRESH_TIMEOUT_MS);
      }
      out.initialSuggestions = readTopSuggestions(3);
      out.initialSuggestionsCsv = String.join(", ", out.initialSuggestions);
      out.nextWordPipelineInitial = readNextWordPipelineDebugStateBestEffort();

      // Stage B: attempt to reconstruct context from editor text (getTextBeforeCursor).
      mScenario.onActivity(
          activity ->
              seedAttemptJson[0] =
                  wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                      .seedNextWordEngineContextFromEditorTextForTest(
                          /* maxChars= */ 4096, /* maxTokens= */ 64, /* tailChars= */ 96));
      SystemClock.sleep(120);
      try {
        out.editorSeedAttempt = new JSONObject(seedAttemptJson[0]);
      } catch (Exception ignored) {
        // best-effort only
      }

      mScenario.onActivity(
          activity -> {
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.clearSuggestionsForTest();
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.resetNeuralInferenceSamplesForTest();
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.clearNextWordPipelineDebugStateForTest();
          });
      SystemClock.sleep(120);

      baselineAsyncListenerCount = getHybridNeuralAsyncListenerInvocationCountForTest();
      mScenario.onActivity(
          activity ->
              wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                  .forceNextWordFromLastCommittedWordForTest());
      SystemClock.sleep(80);
      waitForNonEmptySuggestionsQuickBestEffort(/* timeoutMs= */ 2500L);
      waitForNonEmptySuggestionsBestEffort();

      if ("hybrid".equals(mode) || "neural".equals(mode)) {
        waitForHybridNeuralAsyncSettleBestEffort(
            baselineAsyncListenerCount,
            readHybridNeuralAsyncTelemetryBestEffort(),
            HYBRID_ASYNC_REFRESH_TIMEOUT_MS);
        out.hybridTelemetrySettled = readHybridNeuralAsyncTelemetryBestEffort();
      }

      out.settledSuggestions = readTopSuggestions(3);
      out.settledSuggestionsCsv = String.join(", ", out.settledSuggestions);
      out.nextWordPipelineSettled = readNextWordPipelineDebugStateBestEffort();
      out.neuralInferenceSamples = readNeuralInferenceSamplesBestEffort();

      out.startSuggestions = out.settledSuggestions;
      out.startSuggestionsCsv = out.settledSuggestionsCsv;
      out.finalText = safeGetEditorText();
      out.renderedOutput = renderContextVisibilityOutput(out, seedAttemptJson[0]);
    } catch (Exception e) {
      out.error = e.toString();
      out.finalText = safeGetEditorText();
      out.renderedOutput = renderContextVisibilityOutput(out, seedAttemptJson[0]);
    }
    return out;
  }

  @NonNull
  private RunOutput runContextVisibilityPrepopulatedIncognitoSingleScenario(
      @NonNull String caseId,
      @NonNull String mode,
      boolean noPersonalizedLearning,
      boolean noSuggestionsFlag,
      boolean simulateInvisibleComposing,
      @NonNull String prefillText,
      int prefillCursorPosition,
      int editorInputTypeOverride)
      throws Exception {
    final boolean[] didSetIncognito = {false};
    try {
      if (mScenario != null) {
        mScenario.onActivity(
            activity ->
                didSetIncognito[0] =
                    wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.setIncognitoModeForTest(true));
      } else {
        didSetIncognito[0] =
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.setIncognitoModeForTest(true);
      }
      SystemClock.sleep(120);

      final RunOutput out =
          runContextVisibilityPrepopulatedSingleScenario(
              caseId,
              mode,
              noPersonalizedLearning,
              noSuggestionsFlag,
              simulateInvisibleComposing,
              prefillText,
              prefillCursorPosition,
              editorInputTypeOverride);
      if (out.editorSeedAttempt == null) out.editorSeedAttempt = new JSONObject();
      out.editorSeedAttempt.put("incognitoSetOk", didSetIncognito[0]);
      return out;
    } finally {
      if (mScenario != null) {
        mScenario.onActivity(
            activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.setIncognitoModeForTest(false));
      } else {
        wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.setIncognitoModeForTest(false);
      }
      SystemClock.sleep(80);
    }
  }

  @NonNull
  private RunOutput runContextVisibilityPrepopulatedSingleScenario(
      @NonNull String caseId,
      @NonNull String mode,
      boolean noPersonalizedLearning,
      boolean noSuggestionsFlag,
      boolean simulateInvisibleComposing,
      @NonNull String prefillText,
      int prefillCursorPosition,
      int editorInputTypeOverride)
      throws Exception {
    final RunOutput out = new RunOutput();
    out.caseId = caseId;
    out.mode = mode;
    out.noPersonalizedLearning = noPersonalizedLearning;
    out.noSuggestionsFlag = noSuggestionsFlag;
    out.seed = prefillText;
    out.picks = 0;

    final String[] seedAttemptJson = {""};
    final int[] forcedCount = {0};
    final String[] lastCommittedAtStart = {""};
    final String[] contextTokensAtStartJson = {""};
    try {
      launchTestHarness(
          noPersonalizedLearning,
          noSuggestionsFlag,
          simulateInvisibleComposing,
          /* simulateTypeNull= */ false,
          /* prefillText= */ prefillText,
          /* prefillCursorPosition= */ prefillCursorPosition,
          /* editorInputTypeOverride= */ editorInputTypeOverride);

      mScenario.onActivity(
          activity -> {
            lastCommittedAtStart[0] =
                wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                    .getLastCommittedWordForNextSuggestionsForTest();
            contextTokensAtStartJson[0] =
                wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                    .dumpNextWordEngineContextTokensForTest();
          });
      SystemClock.sleep(80);
      // Warm up dictionaries/models so the forced next-word request is meaningful (especially for
      // async NEURAL mode).
      waitForNonEmptySuggestionsBestEffort();

      mScenario.onActivity(
          activity -> {
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.clearSuggestionsForTest();
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.resetNeuralInferenceSamplesForTest();
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.clearNextWordPipelineDebugStateForTest();
          });
      SystemClock.sleep(120);

      final int baselineAsyncListenerCount = getHybridNeuralAsyncListenerInvocationCountForTest();
      mScenario.onActivity(
          activity ->
              forcedCount[0] =
                  wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                      .forceNextWordFromLastCommittedWordForTest());
      SystemClock.sleep(80);
      waitForNonEmptySuggestionsQuickBestEffort(/* timeoutMs= */ 2500L);

      out.initialSuggestions = readTopSuggestions(3);
      out.initialSuggestionsCsv = String.join(", ", out.initialSuggestions);
      out.nextWordPipelineInitial = readNextWordPipelineDebugStateBestEffort();

      if ("hybrid".equals(mode) || "neural".equals(mode)) {
        out.hybridTelemetryInitial = readHybridNeuralAsyncTelemetryBestEffort();
        waitForHybridNeuralAsyncSettleBestEffort(
            baselineAsyncListenerCount,
            out.hybridTelemetryInitial,
            HYBRID_ASYNC_REFRESH_TIMEOUT_MS);
        out.hybridTelemetrySettled = readHybridNeuralAsyncTelemetryBestEffort();
      }

      out.settledSuggestions = readTopSuggestions(3);
      out.settledSuggestionsCsv = String.join(", ", out.settledSuggestions);
      out.nextWordPipelineSettled = readNextWordPipelineDebugStateBestEffort();
      out.neuralInferenceSamples = readNeuralInferenceSamplesBestEffort();

      out.startSuggestions = out.settledSuggestions;
      out.startSuggestionsCsv = out.settledSuggestionsCsv;
      out.finalText = safeGetEditorText();
    } catch (Exception e) {
      out.error = e.toString();
      out.finalText = safeGetEditorText();
    }

    try {
      if (mScenario != null) {
        mScenario.onActivity(
            activity ->
                seedAttemptJson[0] =
                    wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                        .seedNextWordEngineContextFromEditorTextForTest(
                            /* maxChars= */ 4096, /* maxTokens= */ 64, /* tailChars= */ 96));
      }
      out.editorSeedAttempt = new JSONObject(seedAttemptJson[0]);
      out.editorSeedAttempt.put("forcedNextWordFromLastCommittedWordCount", forcedCount[0]);
      out.editorSeedAttempt.put(
          "lastCommittedWordForNextSuggestionsAtStart", lastCommittedAtStart[0]);
      try {
        out.editorSeedAttempt.put(
            "contextTokensAtStart", new JSONArray(contextTokensAtStartJson[0]));
      } catch (Exception ignored) {
        out.editorSeedAttempt.put("contextTokensAtStartRaw", contextTokensAtStartJson[0]);
      }
    } catch (Exception ignored) {
      // best-effort only
    }
    out.renderedOutput = renderContextVisibilityOutput(out, seedAttemptJson[0]);
    return out;
  }

  @NonNull
  private RunOutput runContextVisibilityResumeSameFieldSingleScenario(
      @NonNull String caseId,
      @NonNull String mode,
      boolean noPersonalizedLearning,
      boolean noSuggestionsFlag,
      boolean simulateInvisibleComposing,
      @NonNull String seed)
      throws Exception {
    final RunOutput out = new RunOutput();
    out.caseId = caseId;
    out.mode = mode;
    out.noPersonalizedLearning = noPersonalizedLearning;
    out.noSuggestionsFlag = noSuggestionsFlag;
    out.seed = seed;
    out.picks = 0;

    final String[] seedAttemptJson = {""};
    final String[] lifecycleBeforePause = {""};
    final String[] lifecycleAfterResume = {""};
    try {
      launchTestHarness(
          noPersonalizedLearning,
          noSuggestionsFlag,
          simulateInvisibleComposing,
          /* simulateTypeNull= */ false);
      waitForNonEmptySuggestionsBestEffort();

      // Ensure the editor content is empty before we write the seed.
      mScenario.onActivity(
          activity -> {
            final EditText editText = activity.findViewById(R.id.test_edit_text);
            if (editText != null) editText.setText("");
          });
      SystemClock.sleep(120);
      mScenario.onActivity(
          activity ->
              wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.commitTextNoSuggestionsForTest(seed));
      waitForEditorTextToEndWithSpaceBestEffort();
      SystemClock.sleep(120);

      mScenario.onActivity(
          activity ->
              lifecycleBeforePause[0] =
                  wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.dumpStartInputViewStateForTest());
      SystemClock.sleep(80);

      // Simulate a pause/resume of the same editor instance.
      if (mScenario != null) {
        mScenario.moveToState(Lifecycle.State.CREATED);
        SystemClock.sleep(500);
        mScenario.moveToState(Lifecycle.State.RESUMED);
      }
      SystemClock.sleep(300);
      mScenario.onActivity(
          activity -> {
            final EditText editText = activity.findViewById(R.id.test_edit_text);
            if (editText == null) return;
            editText.requestFocus();
            editText.setSelection(editText.length());
          });
      SystemClock.sleep(250);
      mScenario.onActivity(TestInputActivity::forceShowKeyboard);
      SystemClock.sleep(250);
      if (!waitForKeyboardVisible()) {
        throw new RuntimeException("Keyboard window not visible after resume");
      }
      waitForNonEmptySuggestionsBestEffort();

      mScenario.onActivity(
          activity ->
              lifecycleAfterResume[0] =
                  wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.dumpStartInputViewStateForTest());
      SystemClock.sleep(80);
      try {
        final JSONObject lifecycle = new JSONObject();
        lifecycle.put("beforePause", new JSONObject(lifecycleBeforePause[0]));
        lifecycle.put("afterResume", new JSONObject(lifecycleAfterResume[0]));
        out.imeLifecycle = lifecycle;
      } catch (Exception ignored) {
        // best-effort only
      }

      mScenario.onActivity(
          activity -> {
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.clearSuggestionsForTest();
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.resetNeuralInferenceSamplesForTest();
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.clearNextWordPipelineDebugStateForTest();
          });
      SystemClock.sleep(120);

      final int baselineAsyncListenerCount = getHybridNeuralAsyncListenerInvocationCountForTest();
      mScenario.onActivity(
          activity ->
              wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                  .forceNextWordFromLastCommittedWordForTest());
      SystemClock.sleep(80);
      waitForNonEmptySuggestionsQuickBestEffort(/* timeoutMs= */ 2500L);
      waitForNonEmptySuggestionsBestEffort();

      out.initialSuggestions = readTopSuggestions(3);
      out.initialSuggestionsCsv = String.join(", ", out.initialSuggestions);
      out.nextWordPipelineInitial = readNextWordPipelineDebugStateBestEffort();

      if ("hybrid".equals(mode) || "neural".equals(mode)) {
        out.hybridTelemetryInitial = readHybridNeuralAsyncTelemetryBestEffort();
        waitForHybridNeuralAsyncSettleBestEffort(
            baselineAsyncListenerCount,
            out.hybridTelemetryInitial,
            HYBRID_ASYNC_REFRESH_TIMEOUT_MS);
        out.hybridTelemetrySettled = readHybridNeuralAsyncTelemetryBestEffort();
      }

      out.settledSuggestions = readTopSuggestions(3);
      out.settledSuggestionsCsv = String.join(", ", out.settledSuggestions);
      out.nextWordPipelineSettled = readNextWordPipelineDebugStateBestEffort();
      out.neuralInferenceSamples = readNeuralInferenceSamplesBestEffort();

      out.startSuggestions = out.settledSuggestions;
      out.startSuggestionsCsv = out.settledSuggestionsCsv;
      out.finalText = safeGetEditorText();
    } catch (Exception e) {
      out.error = e.toString();
      out.finalText = safeGetEditorText();
    }

    try {
      if (mScenario != null) {
        mScenario.onActivity(
            activity ->
                seedAttemptJson[0] =
                    wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                        .seedNextWordEngineContextFromEditorTextForTest(
                            /* maxChars= */ 4096, /* maxTokens= */ 64, /* tailChars= */ 96));
      }
      out.editorSeedAttempt = new JSONObject(seedAttemptJson[0]);
    } catch (Exception ignored) {
      // best-effort only
    }
    out.renderedOutput = renderContextVisibilityOutput(out, seedAttemptJson[0]);
    return out;
  }

  @NonNull
  private RunOutput runContextVisibilityCursorMoveSingleScenario(
      @NonNull String caseId,
      @NonNull String mode,
      boolean noPersonalizedLearning,
      boolean noSuggestionsFlag,
      boolean simulateInvisibleComposing)
      throws Exception {
    final String prefillText = "Hello world\n\nAgain here\n";
    final int cursorBetweenNewlines = prefillText.indexOf("\n\n") + 1;

    final RunOutput out = new RunOutput();
    out.caseId = caseId;
    out.mode = mode;
    out.noPersonalizedLearning = noPersonalizedLearning;
    out.noSuggestionsFlag = noSuggestionsFlag;
    out.seed = prefillText;
    out.picks = 0;

    final String[] seedAttemptJson = {""};
    final String[] lastCommittedAtStart = {""};
    final String[] lastCommittedAfterMove = {""};
    try {
      launchTestHarness(
          noPersonalizedLearning,
          noSuggestionsFlag,
          simulateInvisibleComposing,
          /* simulateTypeNull= */ false,
          /* prefillText= */ prefillText,
          /* prefillCursorPosition= */ -1,
          /* editorInputTypeOverride= */ -1);

      // Warm up dictionaries/models so the forced next-word request is meaningful (especially for
      // async NEURAL mode).
      waitForNonEmptySuggestionsBestEffort();

      mScenario.onActivity(
          activity ->
              lastCommittedAtStart[0] =
                  wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                      .getLastCommittedWordForNextSuggestionsForTest());
      SystemClock.sleep(80);

      // Move the cursor to a "no-man land" position (between two separators) and let the IME
      // reseed context from editor text.
      mScenario.onActivity(
          activity -> {
            final EditText editText = activity.findViewById(R.id.test_edit_text);
            if (editText == null) return;
            editText.requestFocus();
            editText.setSelection(Math.max(0, Math.min(cursorBetweenNewlines, editText.length())));
          });
      SystemClock.sleep(250);

      mScenario.onActivity(
          activity ->
              lastCommittedAfterMove[0] =
                  wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                      .getLastCommittedWordForNextSuggestionsForTest());
      SystemClock.sleep(80);
      try {
        final JSONObject cursorMove = new JSONObject();
        cursorMove.put("targetCursor", cursorBetweenNewlines);
        cursorMove.put("lastCommittedWordAtStart", lastCommittedAtStart[0]);
        cursorMove.put("lastCommittedWordAfterMove", lastCommittedAfterMove[0]);
        out.imeLifecycle = new JSONObject().put("cursorMove", cursorMove);
      } catch (Exception ignored) {
        // best-effort only
      }

      mScenario.onActivity(
          activity -> {
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.clearSuggestionsForTest();
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.resetNeuralInferenceSamplesForTest();
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.clearNextWordPipelineDebugStateForTest();
          });
      SystemClock.sleep(120);

      final int baselineAsyncListenerCount = getHybridNeuralAsyncListenerInvocationCountForTest();
      mScenario.onActivity(
          activity ->
              wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                  .forceNextWordFromLastCommittedWordForTest());
      SystemClock.sleep(80);
      waitForNonEmptySuggestionsQuickBestEffort(/* timeoutMs= */ 2500L);
      waitForNonEmptySuggestionsBestEffort();

      out.initialSuggestions = readTopSuggestions(3);
      out.initialSuggestionsCsv = String.join(", ", out.initialSuggestions);
      out.nextWordPipelineInitial = readNextWordPipelineDebugStateBestEffort();

      if ("hybrid".equals(mode) || "neural".equals(mode)) {
        out.hybridTelemetryInitial = readHybridNeuralAsyncTelemetryBestEffort();
        waitForHybridNeuralAsyncSettleBestEffort(
            baselineAsyncListenerCount,
            out.hybridTelemetryInitial,
            HYBRID_ASYNC_REFRESH_TIMEOUT_MS);
        out.hybridTelemetrySettled = readHybridNeuralAsyncTelemetryBestEffort();
      }

      out.settledSuggestions = readTopSuggestions(3);
      out.settledSuggestionsCsv = String.join(", ", out.settledSuggestions);
      out.nextWordPipelineSettled = readNextWordPipelineDebugStateBestEffort();
      out.neuralInferenceSamples = readNeuralInferenceSamplesBestEffort();

      out.startSuggestions = out.settledSuggestions;
      out.startSuggestionsCsv = out.settledSuggestionsCsv;
      out.finalText = safeGetEditorText();
    } catch (Exception e) {
      out.error = e.toString();
      out.finalText = safeGetEditorText();
    }

    try {
      if (mScenario != null) {
        mScenario.onActivity(
            activity ->
                seedAttemptJson[0] =
                    wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                        .seedNextWordEngineContextFromEditorTextForTest(
                            /* maxChars= */ 4096, /* maxTokens= */ 64, /* tailChars= */ 96));
      }
      out.editorSeedAttempt = new JSONObject(seedAttemptJson[0]);
    } catch (Exception ignored) {
      // best-effort only
    }
    out.renderedOutput = renderContextVisibilityOutput(out, seedAttemptJson[0]);
    return out;
  }

  @NonNull
  private RunOutput runContextVisibilityCursorMoveInsertSingleScenario(
      @NonNull String caseId,
      @NonNull String mode,
      boolean noPersonalizedLearning,
      boolean noSuggestionsFlag,
      boolean simulateInvisibleComposing)
      throws Exception {
    final String prefillText = "Hello world\n\nAgain here\n";
    final int cursorBetweenNewlines = prefillText.indexOf("\n\n") + 1;
    final String insertedToken = "there";

    final RunOutput out = new RunOutput();
    out.caseId = caseId;
    out.mode = mode;
    out.noPersonalizedLearning = noPersonalizedLearning;
    out.noSuggestionsFlag = noSuggestionsFlag;
    out.seed = prefillText;
    out.picks = 0;

    final String[] seedAttemptJson = {""};
    final String[] lastCommittedAtStart = {""};
    final String[] lastCommittedAfterMove = {""};
    final String[] lastCommittedAfterInsert = {""};
    final String[] contextAfterMoveJson = {""};
    final String[] contextAfterInsertJson = {""};
    try {
      launchTestHarness(
          noPersonalizedLearning,
          noSuggestionsFlag,
          simulateInvisibleComposing,
          /* simulateTypeNull= */ false,
          /* prefillText= */ prefillText,
          /* prefillCursorPosition= */ -1,
          /* editorInputTypeOverride= */ -1);

      // Warm up dictionaries/models so the forced next-word request is meaningful (especially for
      // async NEURAL mode).
      waitForNonEmptySuggestionsBestEffort();

      mScenario.onActivity(
          activity ->
              lastCommittedAtStart[0] =
                  wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                      .getLastCommittedWordForNextSuggestionsForTest());
      SystemClock.sleep(80);

      // Move the cursor to a "no-man land" position (between two separators) and let the IME
      // reseed context from editor text.
      mScenario.onActivity(
          activity -> {
            final EditText editText = activity.findViewById(R.id.test_edit_text);
            if (editText == null) return;
            editText.requestFocus();
            editText.setSelection(Math.max(0, Math.min(cursorBetweenNewlines, editText.length())));
          });
      SystemClock.sleep(250);

      mScenario.onActivity(
          activity -> {
            lastCommittedAfterMove[0] =
                wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                    .getLastCommittedWordForNextSuggestionsForTest();
            contextAfterMoveJson[0] =
                wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                    .dumpNextWordEngineContextTokensForTest();
          });
      SystemClock.sleep(120);

      // Insert a whitespace-terminated token at the new cursor position.
      mScenario.onActivity(
          activity ->
              wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.commitTextNoSuggestionsForTest(
                  insertedToken + " "));
      SystemClock.sleep(200);

      mScenario.onActivity(
          activity -> {
            lastCommittedAfterInsert[0] =
                wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                    .getLastCommittedWordForNextSuggestionsForTest();
            contextAfterInsertJson[0] =
                wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                    .dumpNextWordEngineContextTokensForTest();
          });
      SystemClock.sleep(80);

      try {
        final JSONObject cursorMoveInsert = new JSONObject();
        cursorMoveInsert.put("targetCursor", cursorBetweenNewlines);
        cursorMoveInsert.put("insertedToken", insertedToken);
        cursorMoveInsert.put("lastCommittedWordAtStart", lastCommittedAtStart[0]);
        cursorMoveInsert.put("lastCommittedWordAfterMove", lastCommittedAfterMove[0]);
        cursorMoveInsert.put("lastCommittedWordAfterInsert", lastCommittedAfterInsert[0]);
        cursorMoveInsert.put("contextTokensAfterMoveRaw", contextAfterMoveJson[0]);
        cursorMoveInsert.put("contextTokensAfterInsertRaw", contextAfterInsertJson[0]);
        out.imeLifecycle = new JSONObject().put("cursorMoveInsert", cursorMoveInsert);
      } catch (Exception ignored) {
        // best-effort only
      }

      mScenario.onActivity(
          activity -> {
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.clearSuggestionsForTest();
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.resetNeuralInferenceSamplesForTest();
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.clearNextWordPipelineDebugStateForTest();
          });
      SystemClock.sleep(120);

      final int baselineAsyncListenerCount = getHybridNeuralAsyncListenerInvocationCountForTest();
      mScenario.onActivity(
          activity ->
              wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                  .forceNextWordFromLastCommittedWordForTest());
      SystemClock.sleep(80);
      waitForNonEmptySuggestionsQuickBestEffort(/* timeoutMs= */ 2500L);
      waitForNonEmptySuggestionsBestEffort();

      out.initialSuggestions = readTopSuggestions(3);
      out.initialSuggestionsCsv = String.join(", ", out.initialSuggestions);
      out.nextWordPipelineInitial = readNextWordPipelineDebugStateBestEffort();

      if ("hybrid".equals(mode) || "neural".equals(mode)) {
        out.hybridTelemetryInitial = readHybridNeuralAsyncTelemetryBestEffort();
        waitForHybridNeuralAsyncSettleBestEffort(
            baselineAsyncListenerCount,
            out.hybridTelemetryInitial,
            HYBRID_ASYNC_REFRESH_TIMEOUT_MS);
        out.hybridTelemetrySettled = readHybridNeuralAsyncTelemetryBestEffort();
      }

      out.settledSuggestions = readTopSuggestions(3);
      out.settledSuggestionsCsv = String.join(", ", out.settledSuggestions);
      out.nextWordPipelineSettled = readNextWordPipelineDebugStateBestEffort();
      out.neuralInferenceSamples = readNeuralInferenceSamplesBestEffort();

      out.startSuggestions = out.settledSuggestions;
      out.startSuggestionsCsv = out.settledSuggestionsCsv;
      out.finalText = safeGetEditorText();
    } catch (Exception e) {
      out.error = e.toString();
      out.finalText = safeGetEditorText();
    }

    try {
      if (mScenario != null) {
        mScenario.onActivity(
            activity ->
                seedAttemptJson[0] =
                    wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                        .seedNextWordEngineContextFromEditorTextForTest(
                            /* maxChars= */ 4096, /* maxTokens= */ 64, /* tailChars= */ 96));
      }
      out.editorSeedAttempt = new JSONObject(seedAttemptJson[0]);
    } catch (Exception ignored) {
      // best-effort only
    }
    out.renderedOutput = renderContextVisibilityOutput(out, seedAttemptJson[0]);
    return out;
  }

  @NonNull
  private static String renderContextVisibilityOutput(
      @NonNull RunOutput out, @Nullable String seedAttemptJson) {
    final StringBuilder sb = new StringBuilder();
    sb.append(renderRunOutput(out));
    if (seedAttemptJson != null && !seedAttemptJson.trim().isEmpty()) {
      sb.append("\n\nseedNextWordEngineContextFromEditorTextForTest: ")
          .append(seedAttemptJson.trim());
    }
    return sb.toString().trim();
  }

  @NonNull
  private static JSONObject runOutputToJson(@NonNull RunOutput out) throws Exception {
    return runOutputToJson(out, null);
  }

  @NonNull
  private static JSONObject runOutputToJson(@NonNull RunOutput out, @Nullable String label)
      throws Exception {
    final JSONObject json = new JSONObject();
    if (label != null) json.put("label", label);
    json.put("caseId", out.caseId);
    json.put("mode", out.mode);
    json.put("noPersonalizedLearning", out.noPersonalizedLearning);
    json.put("noSuggestionsFlag", out.noSuggestionsFlag);
    json.put("seed", out.seed);
    json.put("picks", out.picks);
    final JSONArray startSuggestions = new JSONArray();
    if (out.startSuggestions != null) {
      for (String s : out.startSuggestions) startSuggestions.put(s);
    }
    json.put("startSuggestions", startSuggestions);

    final JSONArray initialSuggestions = new JSONArray();
    if (out.initialSuggestions != null) {
      for (String s : out.initialSuggestions) initialSuggestions.put(s);
    }
    json.put("startSuggestionsInitial", initialSuggestions);

    final JSONArray settledSuggestions = new JSONArray();
    if (out.settledSuggestions != null) {
      for (String s : out.settledSuggestions) settledSuggestions.put(s);
    }
    json.put("startSuggestionsSettled", settledSuggestions);

    if (out.hybridTelemetryInitial != null || out.hybridTelemetrySettled != null) {
      final JSONObject hybrid = new JSONObject();
      if (out.hybridTelemetryInitial != null) hybrid.put("initial", out.hybridTelemetryInitial);
      if (out.hybridTelemetrySettled != null) hybrid.put("settled", out.hybridTelemetrySettled);
      json.put("hybridNeuralAsync", hybrid);
    }

    if (out.nextWordPipelineInitial != null || out.nextWordPipelineSettled != null) {
      final JSONObject pipeline = new JSONObject();
      if (out.nextWordPipelineInitial != null) pipeline.put("initial", out.nextWordPipelineInitial);
      if (out.nextWordPipelineSettled != null) pipeline.put("settled", out.nextWordPipelineSettled);
      json.put("nextWordPipeline", pipeline);
    }

    if (out.timing != null) {
      json.put("timing", out.timing);
    }

    if (out.editorSeedAttempt != null) {
      json.put("editorSeedAttempt", out.editorSeedAttempt);
    }
    if (out.imeLifecycle != null) {
      json.put("imeLifecycle", out.imeLifecycle);
    }

    if (out.neuralInferenceSamples != null) {
      json.put("neuralInferences", out.neuralInferenceSamples);
    }

    final JSONArray pickedTokens = new JSONArray();
    if (out.pickedTokens != null) {
      for (String s : out.pickedTokens) pickedTokens.put(s);
    }
    json.put("pickedTokens", pickedTokens);
    json.put("finalText", out.finalText == null ? "" : out.finalText);
    if (out.error != null) json.put("error", out.error);
    json.put("renderedOutput", out.renderedOutput);
    return json;
  }

  private void configureEngineMode(@NonNull String mode) {
    final Context context = ApplicationProvider.getApplicationContext();
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(context);
    prefs
        .edit()
        .putString(context.getString(R.string.settings_key_prediction_engine_mode), mode)
        .apply();
    // Give listeners time to pick up the change before we show the IME.
    SystemClock.sleep(250);
  }

  private void configureNextWordAggressiveness(@NonNull String value) {
    final Context context = ApplicationProvider.getApplicationContext();
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(context);
    prefs
        .edit()
        .putString(
            context.getString(R.string.settings_key_next_word_suggestion_aggressiveness), value)
        .apply();
    SystemClock.sleep(150);
  }

  @NonNull
  private RunOutput runCaseSingleScenario(
      @NonNull String caseId,
      @NonNull String mode,
      boolean noPersonalizedLearning,
      boolean noSuggestionsFlag,
      boolean clearLearningData,
      @NonNull List<String> trainingSteps,
      @NonNull String seed,
      int picks)
      throws Exception {
    final RunOutput out = new RunOutput();
    out.caseId = caseId;
    out.mode = mode;
    out.noPersonalizedLearning = noPersonalizedLearning;
    out.noSuggestionsFlag = noSuggestionsFlag;
    out.seed = seed;
    out.picks = picks;

    try {
      launchTestHarness(noPersonalizedLearning, noSuggestionsFlag);
      waitForNonEmptySuggestionsBestEffort();

      if (clearLearningData) {
        mScenario.onActivity(
            activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.clearLearningDataForTest());
        SystemClock.sleep(SHORT_WAIT_MS);
      }

      if (!trainingSteps.isEmpty()) {
        mScenario.onActivity(
            activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.resetNextWordSentence());
        SystemClock.sleep(120);
        for (String s : trainingSteps) {
          mScenario.onActivity(
              activity ->
                  wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.commitTextNoSuggestionsForTest(s));
          SystemClock.sleep(120);
        }

        // Training should not pollute the displayed editor content in the report.
        mScenario.onActivity(
            activity -> {
              final EditText editText = activity.findViewById(R.id.test_edit_text);
              if (editText != null) editText.setText("");
            });
        SystemClock.sleep(SHORT_WAIT_MS);
        mScenario.onActivity(
            activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.resetNextWordSentence());
        SystemClock.sleep(120);
      }

      mScenario.onActivity(
          activity -> {
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.clearSuggestionsForTest();
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.resetNeuralInferenceSamplesForTest();
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.clearNextWordPipelineDebugStateForTest();
          });
      SystemClock.sleep(120);
      final String beforeSeedText = getEditorText();
      mScenario.onActivity(
          activity ->
              wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.commitTextNoSuggestionsForTest(seed));
      final String afterSeedText = waitForEditorTextToChange(beforeSeedText);
      if (afterSeedText == null && out.error == null) {
        out.error = "Seed commit timeout";
      }
      waitForEditorTextToEndWithSpaceBestEffort();
      SystemClock.sleep(80);
      final int baselineAsyncListenerCount = getHybridNeuralAsyncListenerInvocationCountForTest();
      final long requestUptimeMs = SystemClock.uptimeMillis();
      mScenario.onActivity(
          activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.forceNextWordFromCursor());
      SystemClock.sleep(80);
      waitForNonEmptySuggestionsQuickBestEffort(/* timeoutMs= */ 2500L);
      waitForNonEmptySuggestionsBestEffort();
      out.initialSuggestions = readTopSuggestions(3);
      out.initialSuggestionsCsv = String.join(", ", out.initialSuggestions);
      final long initialCaptureUptimeMs = SystemClock.uptimeMillis();
      out.nextWordPipelineInitial = readNextWordPipelineDebugStateBestEffort();
      long uiFirstChangeUptimeMs = -1L;
      if ("hybrid".equals(mode) || "neural".equals(mode)) {
        out.hybridTelemetryInitial = readHybridNeuralAsyncTelemetryBestEffort();
        uiFirstChangeUptimeMs =
            waitForHybridNeuralAsyncSettleAndReturnUiFirstChangeUptimeMsBestEffort(
                baselineAsyncListenerCount,
                out.hybridTelemetryInitial,
                HYBRID_ASYNC_REFRESH_TIMEOUT_MS,
                out.initialSuggestions);
        waitForNonEmptySuggestionsQuickBestEffort(/* timeoutMs= */ 2500L);
      }
      out.settledSuggestions = readTopSuggestions(3);
      out.settledSuggestionsCsv = String.join(", ", out.settledSuggestions);
      final long settledCaptureUptimeMs = SystemClock.uptimeMillis();
      out.nextWordPipelineSettled = readNextWordPipelineDebugStateBestEffort();
      if ("hybrid".equals(mode) || "neural".equals(mode)) {
        out.hybridTelemetrySettled = readHybridNeuralAsyncTelemetryBestEffort();
      }
      out.startSuggestions = out.settledSuggestions;
      out.startSuggestionsCsv = out.settledSuggestionsCsv;
      try {
        final JSONObject timing = new JSONObject();
        timing.put("requestUptimeMs", requestUptimeMs);
        timing.put("initialCaptureUptimeMs", initialCaptureUptimeMs);
        timing.put("settledCaptureUptimeMs", settledCaptureUptimeMs);
        if (initialCaptureUptimeMs >= requestUptimeMs) {
          timing.put("requestToInitialMs", initialCaptureUptimeMs - requestUptimeMs);
        }
        if (settledCaptureUptimeMs >= requestUptimeMs) {
          timing.put("requestToSettledMs", settledCaptureUptimeMs - requestUptimeMs);
        }
        if (uiFirstChangeUptimeMs > 0) {
          timing.put("uiFirstChangeUptimeMs", uiFirstChangeUptimeMs);
          if (uiFirstChangeUptimeMs >= requestUptimeMs) {
            timing.put("requestToUiFirstChangeMs", uiFirstChangeUptimeMs - requestUptimeMs);
          }
          if (uiFirstChangeUptimeMs >= initialCaptureUptimeMs) {
            timing.put("initialToUiFirstChangeMs", uiFirstChangeUptimeMs - initialCaptureUptimeMs);
          }
        }
        out.timing = timing;
      } catch (Exception ignored) {
        // best-effort only
      }

      String previousText = getEditorText();
      final List<String> picked = new ArrayList<>();
      for (int i = 0; i < picks; i++) {
        final String[] head = {""};
        mScenario.onActivity(
            activity -> {
              final String candidate = CandidateViewTestRegistry.pickNowAndReturnSuggestionAt(0);
              head[0] = candidate == null ? "" : candidate.trim();
            });
        if (head[0].isEmpty()) {
          out.error = "Empty first suggestion at pick " + i;
          break;
        }
        picked.add(head[0]);

        final String updatedText = waitForEditorTextToChange(previousText);
        if (updatedText == null) {
          out.error = "Editor update timeout after pick " + i;
          break;
        }
        previousText = updatedText;
        waitForEditorTextToEndWithSpaceBestEffort();
        waitForNonEmptySuggestionsBestEffort();
        SystemClock.sleep(90);
      }

      out.pickedTokens = picked;
      out.finalText = getEditorText().trim();
      if ("neural".equals(mode) || "hybrid".equals(mode)) {
        out.neuralInferenceSamples = readNeuralInferenceSamplesBestEffort();
      }
      out.renderedOutput = renderRunOutput(out);
      return out;
    } catch (Exception e) {
      out.error = e.getClass().getSimpleName() + ": " + e.getMessage();
      out.finalText = safeGetEditorText();
      if ("neural".equals(mode) || "hybrid".equals(mode)) {
        out.neuralInferenceSamples = readNeuralInferenceSamplesBestEffort();
      }
      out.renderedOutput = renderRunOutput(out);
      return out;
    } finally {
      closeScenarioIfOpen();
    }
  }

  @NonNull
  private static String renderRunOutput(@NonNull RunOutput out) {
    final StringBuilder sb = new StringBuilder();
    sb.append("mode=").append(out.mode);
    if (out.noPersonalizedLearning) sb.append(" noPersonalizedLearning=1");
    if (out.noSuggestionsFlag) sb.append(" noSuggestionsFlag=1");
    sb.append("\nseed: ").append(out.seed.replace("\n", "\\n").trim());
    if (out.initialSuggestionsCsv != null && !out.initialSuggestionsCsv.isEmpty()) {
      sb.append("\nstart suggestions (initial): ").append(out.initialSuggestionsCsv);
    }
    if (out.settledSuggestionsCsv != null
        && !out.settledSuggestionsCsv.isEmpty()
        && !out.settledSuggestionsCsv.equals(out.initialSuggestionsCsv)) {
      sb.append("\nstart suggestions (settled): ").append(out.settledSuggestionsCsv);
    }
    if (out.timing != null) {
      final long reqToInitial = out.timing.optLong("requestToInitialMs", -1L);
      final long reqToUiChange = out.timing.optLong("requestToUiFirstChangeMs", -1L);
      final long reqToSettled = out.timing.optLong("requestToSettledMs", -1L);
      final StringBuilder timing = new StringBuilder();
      if (reqToInitial >= 0) timing.append("req→initial=").append(reqToInitial).append("ms");
      if (reqToUiChange >= 0) {
        if (timing.length() > 0) timing.append(", ");
        timing.append("req→ui-change=").append(reqToUiChange).append("ms");
      }
      if (reqToSettled >= 0) {
        if (timing.length() > 0) timing.append(", ");
        timing.append("req→settled=").append(reqToSettled).append("ms");
      }
      if (timing.length() > 0) {
        sb.append("\ntiming: ").append(timing);
      }
    }
    if (out.picks > 0) {
      sb.append("\npicks: ").append(out.picks);
    }
    if (out.pickedTokens != null && !out.pickedTokens.isEmpty()) {
      sb.append("\npicked: ").append(out.pickedTokens);
    }
    if (out.finalText != null && !out.finalText.isEmpty()) {
      sb.append("\nfinal: ").append(out.finalText);
    }
    if (out.error != null) {
      sb.append("\nerror: ").append(out.error);
    }
    return sb.toString().trim();
  }

  @NonNull
  private List<String> readTopSuggestions(int max) {
    final int limit = Math.max(0, max);
    final List<String> out = new ArrayList<>(limit);
    if (mScenario == null) return out;
    mScenario.onActivity(
        activity -> {
          final int count = CandidateViewTestRegistry.getCount();
          final int scan = Math.min(count, limit);
          for (int i = 0; i < scan; i++) {
            final String candidate = CandidateViewTestRegistry.getSuggestionAt(i);
            final String trimmed = candidate == null ? "" : candidate.trim();
            if (!trimmed.isEmpty()) out.add(trimmed);
          }
        });
    return out;
  }

  private void waitForNonEmptySuggestionsQuickBestEffort(long timeoutMs) {
    if (mScenario == null) return;
    final long start = SystemClock.uptimeMillis();
    do {
      final int[] c = {0};
      mScenario.onActivity(activity -> c[0] = CandidateViewTestRegistry.getCount());
      if (c[0] > 0) return;
      SystemClock.sleep(30);
    } while (SystemClock.uptimeMillis() - start < timeoutMs);
  }

  private int getHybridNeuralAsyncListenerInvocationCountForTest() {
    if (mScenario == null) return 0;
    final int[] out = {0};
    mScenario.onActivity(
        activity ->
            out[0] =
                wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                    .getHybridNeuralAsyncListenerInvocationCountForTest());
    return out[0];
  }

  @Nullable
  private JSONArray readNeuralInferenceSamplesBestEffort() {
    if (mScenario == null) return null;
    final String[] raw = {null};
    mScenario.onActivity(
        activity ->
            raw[0] =
                wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.dumpNeuralInferenceSamplesForTest());
    if (raw[0] == null) return null;
    try {
      return new JSONArray(raw[0]);
    } catch (Exception e) {
      return null;
    }
  }

  @Nullable
  private JSONObject readHybridNeuralAsyncTelemetryBestEffort() {
    if (mScenario == null) return null;
    final String[] raw = {null};
    mScenario.onActivity(
        activity ->
            raw[0] =
                wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                    .dumpHybridNeuralAsyncDebugStateForTest());
    if (raw[0] == null) return null;
    try {
      return new JSONObject(raw[0]);
    } catch (Exception e) {
      return null;
    }
  }

  @Nullable
  private JSONObject readNextWordPipelineDebugStateBestEffort() {
    if (mScenario == null) return null;
    final String[] raw = {null};
    mScenario.onActivity(
        activity ->
            raw[0] =
                wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                    .dumpNextWordPipelineDebugStateForTest());
    if (raw[0] == null) return null;
    final String trimmed = raw[0].trim();
    if (trimmed.isEmpty() || "{}".equals(trimmed)) return null;
    try {
      return new JSONObject(trimmed);
    } catch (Exception e) {
      return null;
    }
  }

  private void waitForHybridNeuralAsyncSettleBestEffort(
      int baselineListenerCount, @Nullable JSONObject initialTelemetry, long timeoutMs) {
    if (initialTelemetry == null) return;
    final String contextKey = initialTelemetry.optString("contextKey", "");
    if (contextKey.isEmpty()) return;

    final boolean cacheHit =
        initialTelemetry.optBoolean("lastRequestCacheHit", false)
            || "cache_hit".equals(initialTelemetry.optString("lastRequestNotScheduledReason", ""));
    if (cacheHit) return;

    long generation = initialTelemetry.optLong("lastRequestGeneration", 0L);
    if (generation == 0L) {
      final long inFlightGen = initialTelemetry.optLong("inFlightGeneration", 0L);
      final String inFlightKey = initialTelemetry.optString("inFlightContextKey", "");
      if (contextKey.equals(inFlightKey)) generation = inFlightGen;
    }
    if (generation == 0L) return;

    final long start = SystemClock.uptimeMillis();
    do {
      final JSONObject telemetry = readHybridNeuralAsyncTelemetryBestEffort();
      if (telemetry == null) return;

      final long computeGen = telemetry.optLong("lastComputeGeneration", 0L);
      final String computeKey = telemetry.optString("lastComputeContextKey", "");
      if (computeGen == generation && contextKey.equals(computeKey)) {
        if (!telemetry.optBoolean("lastComputeHadNormalized", false)) return;

        final int listenerCount = telemetry.optInt("listenerInvocationCount", 0);
        final long listenerGen = telemetry.optLong("lastListenerGeneration", 0L);
        final String listenerKey = telemetry.optString("lastListenerContextKey", "");
        if (listenerCount > baselineListenerCount
            && listenerGen == generation
            && contextKey.equals(listenerKey)) {
          return;
        }

        final String suppressed = telemetry.optString("lastListenerSuppressedReason", "");
        if (!suppressed.isEmpty()) return;
      }

      SystemClock.sleep(60);
    } while (SystemClock.uptimeMillis() - start < timeoutMs);
  }

  private long waitForHybridNeuralAsyncSettleAndReturnUiFirstChangeUptimeMsBestEffort(
      int baselineListenerCount,
      @Nullable JSONObject initialTelemetry,
      long timeoutMs,
      @NonNull List<String> baselineSuggestions) {
    if (baselineSuggestions.isEmpty() || mScenario == null) {
      waitForHybridNeuralAsyncSettleBestEffort(baselineListenerCount, initialTelemetry, timeoutMs);
      return -1L;
    }

    if (initialTelemetry == null) {
      return waitForTopSuggestionsToChangeBestEffort(baselineSuggestions, timeoutMs);
    }

    final String contextKey = initialTelemetry.optString("contextKey", "");
    if (contextKey.isEmpty()) {
      return waitForTopSuggestionsToChangeBestEffort(baselineSuggestions, timeoutMs);
    }

    final boolean cacheHit =
        initialTelemetry.optBoolean("lastRequestCacheHit", false)
            || "cache_hit".equals(initialTelemetry.optString("lastRequestNotScheduledReason", ""));
    if (cacheHit) return -1L;

    long generation = initialTelemetry.optLong("lastRequestGeneration", 0L);
    if (generation == 0L) {
      final long inFlightGen = initialTelemetry.optLong("inFlightGeneration", 0L);
      final String inFlightKey = initialTelemetry.optString("inFlightContextKey", "");
      if (contextKey.equals(inFlightKey)) generation = inFlightGen;
    }
    if (generation == 0L) {
      return waitForTopSuggestionsToChangeBestEffort(baselineSuggestions, timeoutMs);
    }

    final int scan = baselineSuggestions.size();
    long uiFirstChangeUptimeMs = -1L;
    final long start = SystemClock.uptimeMillis();
    do {
      if (uiFirstChangeUptimeMs <= 0) {
        final List<String> current = readTopSuggestions(scan);
        if (!current.isEmpty() && !current.equals(baselineSuggestions)) {
          uiFirstChangeUptimeMs = SystemClock.uptimeMillis();
        }
      }

      final JSONObject telemetry = readHybridNeuralAsyncTelemetryBestEffort();
      if (telemetry == null) return uiFirstChangeUptimeMs;

      final long computeGen = telemetry.optLong("lastComputeGeneration", 0L);
      final String computeKey = telemetry.optString("lastComputeContextKey", "");
      if (computeGen == generation && contextKey.equals(computeKey)) {
        if (!telemetry.optBoolean("lastComputeHadNormalized", false)) return uiFirstChangeUptimeMs;

        final int listenerCount = telemetry.optInt("listenerInvocationCount", 0);
        final long listenerGen = telemetry.optLong("lastListenerGeneration", 0L);
        final String listenerKey = telemetry.optString("lastListenerContextKey", "");
        if (listenerCount > baselineListenerCount
            && listenerGen == generation
            && contextKey.equals(listenerKey)) {
          return uiFirstChangeUptimeMs;
        }

        final String suppressed = telemetry.optString("lastListenerSuppressedReason", "");
        if (!suppressed.isEmpty()) return uiFirstChangeUptimeMs;
      }

      SystemClock.sleep(60);
    } while (SystemClock.uptimeMillis() - start < timeoutMs);
    return uiFirstChangeUptimeMs;
  }

  private void waitForNonEmptySuggestionsBestEffort() {
    if (mScenario == null) {
      return;
    }
    final long start = SystemClock.uptimeMillis();
    long lastRefresh = 0L;
    do {
      final int[] c = {0};
      mScenario.onActivity(activity -> c[0] = CandidateViewTestRegistry.getCount());
      if (c[0] > 0) return;
      final long now = SystemClock.uptimeMillis();
      if (now - lastRefresh >= 1000L) {
        lastRefresh = now;
        mScenario.onActivity(
            activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.forceNextWordFromCursor());
      }
      SystemClock.sleep(200);
    } while (SystemClock.uptimeMillis() - start < SUGGESTIONS_TIMEOUT_MS);
  }

  private long waitForTopSuggestionsToChangeBestEffort(
      @NonNull List<String> baseline, long timeoutMs) {
    if (baseline.isEmpty()) return -1L;
    if (mScenario == null) return -1L;

    final int scan = baseline.size();
    final long start = SystemClock.uptimeMillis();
    do {
      final List<String> current = readTopSuggestions(scan);
      if (!current.isEmpty() && !current.equals(baseline)) return SystemClock.uptimeMillis();
      SystemClock.sleep(60);
    } while (SystemClock.uptimeMillis() - start < timeoutMs);
    return -1L;
  }

  @Nullable
  private String waitForEditorTextToChange(@NonNull String previousText) {
    final long start = SystemClock.uptimeMillis();
    do {
      final String text = getEditorText();
      if (!text.equals(previousText)) return text;
      SystemClock.sleep(80);
    } while (SystemClock.uptimeMillis() - start < EDITOR_UPDATE_TIMEOUT_MS);
    return null;
  }

  private void waitForEditorTextToEndWithSpaceBestEffort() {
    final long start = SystemClock.uptimeMillis();
    do {
      final String text = getEditorText();
      if (text.endsWith(" ")) return;
      SystemClock.sleep(80);
    } while (SystemClock.uptimeMillis() - start < 3000);
  }

  @NonNull
  private String getEditorText() {
    final UiObject2 editor = mDevice.findObject(By.res(resId("test_edit_text")));
    if (editor == null) return "";
    final String text = editor.getText();
    return text == null ? "" : text;
  }

  @NonNull
  private String safeGetEditorText() {
    try {
      return getEditorText();
    } catch (Exception e) {
      return "";
    }
  }

  private void launchTestHarness(boolean noPersonalizedLearning, boolean noSuggestionsFlag) {
    launchTestHarness(
        noPersonalizedLearning,
        noSuggestionsFlag,
        /* simulateInvisibleComposing= */ false,
        /* simulateTypeNull= */ false);
  }

  private void launchTestHarness(
      boolean noPersonalizedLearning,
      boolean noSuggestionsFlag,
      boolean simulateInvisibleComposing,
      boolean simulateTypeNull) {
    launchTestHarness(
        noPersonalizedLearning,
        noSuggestionsFlag,
        simulateInvisibleComposing,
        simulateTypeNull,
        /* prefillText= */ null,
        /* prefillCursorPosition= */ -1,
        /* editorInputTypeOverride= */ -1);
  }

  private void launchTestHarness(
      boolean noPersonalizedLearning,
      boolean noSuggestionsFlag,
      boolean simulateInvisibleComposing,
      boolean simulateTypeNull,
      @Nullable String prefillText,
      int prefillCursorPosition,
      int editorInputTypeOverride) {
    final Context context = ApplicationProvider.getApplicationContext();
    final Intent intent = new Intent(context, TestInputActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.putExtra(
        TestInputActivity.EXTRA_IME_FLAG_NO_PERSONALIZED_LEARNING, noPersonalizedLearning);
    intent.putExtra(TestInputActivity.EXTRA_TYPE_TEXT_FLAG_NO_SUGGESTIONS, noSuggestionsFlag);
    intent.putExtra(
        TestInputActivity.EXTRA_SIMULATE_INVISIBLE_COMPOSING, simulateInvisibleComposing);
    intent.putExtra(TestInputActivity.EXTRA_SIMULATE_TYPE_NULL, simulateTypeNull);
    if (editorInputTypeOverride >= 0) {
      intent.putExtra(TestInputActivity.EXTRA_EDITOR_INPUT_TYPE_OVERRIDE, editorInputTypeOverride);
    }
    if (prefillText != null) {
      intent.putExtra(TestInputActivity.EXTRA_PREFILL_TEXT, prefillText);
      intent.putExtra(TestInputActivity.EXTRA_PREFILL_CURSOR_POSITION, prefillCursorPosition);
    }

    boolean editorVisible = false;
    for (int attempt = 0; attempt < 2 && !editorVisible; attempt++) {
      closeScenarioIfOpen();
      try {
        wakeAndUnlockDevice();
      } catch (Throwable ignored) {
        // best-effort only
      }
      if (attempt > 0) {
        try {
          mDevice.pressHome();
        } catch (Throwable ignored) {
          // best-effort only
        }
        SystemClock.sleep(300);
      }
      mScenario = ActivityScenario.launch(intent);
      editorVisible = waitForEditorVisible();
    }
    if (!editorVisible) {
      // Best-effort: throw an Exception type so runCaseSingleScenario can record it instead of
      // failing the whole report with an AssertionError.
      throw new RuntimeException("Test editor not visible");
    }

    if (prefillText != null) {
      final int desiredCursor = prefillCursorPosition;
      mScenario.onActivity(
          activity -> {
            final EditText editText = activity.findViewById(R.id.test_edit_text);
            if (editText == null) return;
            editText.requestFocus();
            int cursor = desiredCursor;
            if (cursor < 0 || cursor > editText.length()) cursor = editText.length();
            editText.setSelection(cursor);
          });
      SystemClock.sleep(250);
    } else {
      focusEditor();
    }
    mScenario.onActivity(TestInputActivity::forceShowKeyboard);
    SystemClock.sleep(SHORT_WAIT_MS);
    if (!waitForKeyboardVisible()) {
      throw new RuntimeException("Keyboard window not visible");
    }
  }

  private void closeScenarioIfOpen() {
    if (mScenario != null) {
      mScenario.close();
      mScenario = null;
    }
  }

  private boolean waitForEditorVisible() {
    return mDevice.wait(Until.hasObject(By.res(resId("test_edit_text"))), READY_TIMEOUT_MS);
  }

  private boolean waitForKeyboardVisible() {
    boolean visible =
        mDevice.wait(Until.hasObject(By.res(resId("AnyKeyboardMainView"))), READY_TIMEOUT_MS);
    if (!visible) {
      visible = mDevice.wait(Until.hasObject(By.res(resId("candidate_view"))), READY_TIMEOUT_MS);
    }
    return visible;
  }

  private void focusEditor() {
    UiObject2 editor = mDevice.wait(Until.findObject(By.res(resId("test_edit_text"))), 3000);
    if (editor != null) {
      editor.click();
      SystemClock.sleep(250);
    }
  }

  private void clearLogcat() throws IOException {
    executeShellCommand("logcat -c");
  }

  private void ensureImeEnabledAndSelected() throws IOException {
    mImeComponent = resolveImeComponentId();
    executeShellCommand("ime enable --user 0 " + mImeComponent);
    executeShellCommand("ime set --user 0 " + mImeComponent);

    String enabled = executeShellCommand("settings get secure enabled_input_methods").trim();
    String expanded = expandComponent(mImeComponent);
    if (!enabled.contains(mImeComponent) && !enabled.contains(expanded)) {
      String prefix = enabled.isEmpty() ? "" : enabled + ":";
      executeShellCommand(
          "settings put secure enabled_input_methods \"" + prefix + mImeComponent + "\"");
    }
    executeShellCommand("settings put secure show_ime_with_hard_keyboard 1");
    SystemClock.sleep(350);
  }

  private void assertImeSelected() throws IOException {
    String current = executeShellCommand("settings get secure default_input_method").trim();
    String expanded = expandComponent(mImeComponent);
    if (!(current.equals(mImeComponent) || current.equals(expanded))) {
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
    executeShellCommand("input keyevent 82");
    SystemClock.sleep(250);
  }

  private void ensureNgramModelActive(Context context) {
    // Reuse the small fixture KenLM model for deterministic evaluation.
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
          "Presage predictor failed to activate: " + manager.getLastActivationError());
    }
    manager.deactivate();
  }

  private void ensureMixedcaseNeuralModelActive(Context context) throws Exception {
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
            53_587_027L,
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
      throw new AssertionError("Neural predictor failed to activate with mixedcase model");
    }
    manager.deactivate();
  }

  @NonNull
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

  @NonNull
  private static List<String> repeat(@NonNull String value, int times) {
    final List<String> out = new ArrayList<>(Math.max(0, times));
    for (int i = 0; i < times; i++) out.add(value);
    return out;
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

  private static final class RunOutput {
    @NonNull String caseId = "";
    @NonNull String mode = "";
    boolean noPersonalizedLearning;
    boolean noSuggestionsFlag;
    @NonNull String seed = "";
    int picks;
    @Nullable List<String> startSuggestions;
    @Nullable List<String> initialSuggestions;
    @NonNull String initialSuggestionsCsv = "";
    @Nullable JSONObject hybridTelemetryInitial;
    @Nullable JSONObject nextWordPipelineInitial;
    @Nullable List<String> settledSuggestions;
    @NonNull String settledSuggestionsCsv = "";
    @Nullable JSONObject hybridTelemetrySettled;
    @Nullable JSONObject nextWordPipelineSettled;
    @Nullable JSONObject editorSeedAttempt;
    @Nullable JSONObject imeLifecycle;
    @Nullable JSONArray neuralInferenceSamples;
    @Nullable JSONObject timing;
    @NonNull String startSuggestionsCsv = "";
    @Nullable List<String> pickedTokens;
    @Nullable String finalText;
    @Nullable String error;
    @NonNull String renderedOutput = "";
  }
}
