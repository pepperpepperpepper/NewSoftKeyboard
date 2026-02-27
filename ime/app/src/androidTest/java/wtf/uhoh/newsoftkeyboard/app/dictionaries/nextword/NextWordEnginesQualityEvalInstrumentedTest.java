package wtf.uhoh.newsoftkeyboard.app.dictionaries.nextword;

import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.presage.DownloaderCompat;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.presage.PresageModelCatalog;
import wtf.uhoh.newsoftkeyboard.engine.EngineType;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelDefinition;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelDownloader;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelStore;
import wtf.uhoh.newsoftkeyboard.nextword.NextWordSuggestions;
import wtf.uhoh.newsoftkeyboard.nextword.pipeline.NextWordSuggestionsPipeline;
import wtf.uhoh.newsoftkeyboard.nextword.prediction.NextWordPredictionEngines;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;
import wtf.uhoh.newsoftkeyboard.prefs.RxSharedPrefs;

/**
 * Best-effort on-device quality metrics harness for next-word suggestions.
 *
 * <p>This is opt-in (skipped by default) because it can be slow and may require downloading models.
 */
@RunWith(AndroidJUnit4.class)
public class NextWordEnginesQualityEvalInstrumentedTest {

  private static final String TAG = "NextWordQualityEval";

  private static final String EVAL_ENABLED_ARG = "RUN_NEXTWORD_QUALITY_EVAL";
  private static final String MAX_CASES_ARG = "NEXTWORD_QUALITY_MAX_CASES";

  private static final int DEFAULT_MAX_CASES = 200;
  private static final int MAX_RESULTS = 8;
  private static final int CHAIN_TARGET_WORDS = 40;

  private static final String DEFAULT_TEST_NEURAL_MODEL_ID = "distilgpt2_mixedcase_sanity";
  private static final String DEFAULT_TEST_NEURAL_MODEL_BUNDLE_URL =
      "https://fdroid.uh-oh.wtf/models/distilgpt2_mixedcase_sanity_v1.zip";

  private static final String FIXTURE_NGRAM_MODEL_ID = "fixture_kenlm_the_nonsense_3gram";

  @Nullable private NextWordPredictionEngines mEngines;
  @Nullable private NextWordSuggestionsPipeline mPipeline;

  @Nullable private ModelStore mModelStore;
  @Nullable private SharedPreferences mPrefs;

  @Nullable private String mPrevEngineModePref;
  @Nullable private String mPrevNgramModelId;
  @Nullable private String mPrevNeuralModelId;

  @Before
  public void setUp() throws Exception {
    assumeTrue(
        "Set RUN_NEXTWORD_QUALITY_EVAL=1 to run the on-device quality evaluation.",
        isQualityEvalEnabled());

    final Context context = ApplicationProvider.getApplicationContext();
    mPrefs = DirectBootAwareSharedPreferences.create(context);
    mModelStore = new ModelStore(context);

    mPrevEngineModePref =
        mPrefs.getString(context.getString(R.string.settings_key_prediction_engine_mode), "");
    mPrevNgramModelId = mModelStore.getSelectedModelId(EngineType.NGRAM);
    mPrevNeuralModelId = mModelStore.getSelectedModelId(EngineType.NEURAL);

    ensureNgramModelActive(context);
    // Best-effort; we can still evaluate NGRAM if neural is unavailable.
    ensureMixedcaseNeuralModelActive(context);

    final RxSharedPrefs rxPrefs = NskApplicationBase.prefs(context);
    mEngines = new NextWordPredictionEngines(context, rxPrefs, TAG, false, false);
    mPipeline =
        new NextWordSuggestionsPipeline(
            mEngines,
            java.util.Collections.emptyList(),
            EmptyNextWordSuggestions::new,
            java.util.Collections.emptyList(),
            /* enableTestLogging= */ false);
  }

  @After
  public void tearDown() {
    if (mEngines != null) {
      mEngines.close();
      mEngines = null;
    }
    mPipeline = null;

    final Context context = ApplicationProvider.getApplicationContext();
    if (mPrefs != null) {
      final SharedPreferences.Editor editor = mPrefs.edit();
      if (mPrevEngineModePref == null || mPrevEngineModePref.trim().isEmpty()) {
        editor.remove(context.getString(R.string.settings_key_prediction_engine_mode));
      } else {
        editor.putString(
            context.getString(R.string.settings_key_prediction_engine_mode), mPrevEngineModePref);
      }
      editor.apply();
      mPrefs = null;
    }

    if (mModelStore != null) {
      restoreSelectedModelId(mModelStore, EngineType.NGRAM, mPrevNgramModelId);
      restoreSelectedModelId(mModelStore, EngineType.NEURAL, mPrevNeuralModelId);
      mModelStore = null;
    }
  }

  @Test
  public void evaluateNextWordQuality_allEngineModes_bestEffort() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();
    final List<List<String>> sentences = loadCorpusSentences();
    assumeTrue("Missing evaluation corpus sentences.", !sentences.isEmpty());
    final String corpusSha256 = computeCorpusSha256();

    final int maxCases = resolveMaxCases();

    final ModelStore.ActiveModel ngramModel =
        new ModelStore(context).ensureActiveModel(EngineType.NGRAM);
    final ModelStore.ActiveModel neuralModel =
        new ModelStore(context).ensureActiveModel(EngineType.NEURAL);

    final String ngramModelId = ngramModel == null ? "none" : ngramModel.getDefinition().getId();
    final String neuralModelId = neuralModel == null ? "none" : neuralModel.getDefinition().getId();

    for (String mode : new String[] {"ngram", "neural", "hybrid"}) {
      if ("neural".equals(mode) || "hybrid".equals(mode)) {
        if (neuralModel == null) {
          Log.w(TAG, "Skipping mode=" + mode + " because no neural model is installed.");
          continue;
        }
      }
      final String summary =
          evaluateModeAndRenderSummary(mode, sentences, maxCases, ngramModelId, neuralModelId);
      final String json =
          metricsJsonFromSummary(mode, ngramModelId, neuralModelId, corpusSha256, summary);
      Log.i(TAG, summary);
      System.out.println(summary);
      writeMetricsReport(context, mode, summary, json);
    }
  }

  @NonNull
  private String evaluateModeAndRenderSummary(
      @NonNull String mode,
      @NonNull List<List<String>> sentences,
      int maxCases,
      @NonNull String ngramModelId,
      @NonNull String neuralModelId) {
    if (mEngines == null || mPipeline == null) {
      throw new AssertionError("Test harness not initialized.");
    }

    mEngines.updatePredictionEngine(mode);
    mPipeline.resetSentence();

    final NextWordSuggestionsPipeline.Config config =
        new NextWordSuggestionsPipeline.Config(
            /* enabled= */ true,
            /* alsoSuggestNextPunctuations= */ false,
            /* maxNextWordSuggestionsCount= */ MAX_RESULTS,
            /* minWordUsage= */ 1);

    final Metrics metrics = new Metrics();
    int cases = 0;
    for (List<String> sentence : sentences) {
      mPipeline.resetSentence();
      for (int index = 0; index + 1 < sentence.size(); index++) {
        if (cases >= maxCases) break;
        final String lastContextToken = sentence.get(index);
        final String expected = sentence.get(index + 1);
        mPipeline.notifyWordCommitted(lastContextToken, /* incognitoMode= */ true);

        final List<CharSequence> out = new ArrayList<>(MAX_RESULTS);
        final long startNs = System.nanoTime();
        mPipeline.appendNextWords(
            /* currentWord= */ "",
            out,
            /* maxSuggestions= */ MAX_RESULTS,
            /* incognitoMode= */ true,
            config);
        final long latencyNs = System.nanoTime() - startNs;

        final List<String> predictions = new ArrayList<>(out.size());
        for (CharSequence c : out) {
          if (c != null) predictions.add(c.toString());
        }
        metrics.recordCase(lastContextToken, expected, predictions, latencyNs);
        cases++;

        // Give the UI thread a chance to process any engine callbacks between calls; avoids
        // systematic underestimation of latencies on some devices.
        SystemClock.sleep(1);
      }
      if (cases >= maxCases) break;
    }

    final ChainMetrics chainMetrics = evaluateGreedyChain(config);
    return metrics.renderSummary(mode, ngramModelId, neuralModelId, cases, chainMetrics);
  }

  @NonNull
  private ChainMetrics evaluateGreedyChain(@NonNull NextWordSuggestionsPipeline.Config config) {
    if (mPipeline == null) {
      throw new AssertionError("Test harness not initialized.");
    }

    mPipeline.resetSentence();
    mPipeline.notifyWordCommitted("the", /* incognitoMode= */ true);

    final ChainMetrics out = new ChainMetrics();
    out.appendToken("the");

    final List<CharSequence> candidates = new ArrayList<>(MAX_RESULTS);
    while (out.generatedWords < CHAIN_TARGET_WORDS) {
      out.picksAttempted++;
      candidates.clear();
      mPipeline.appendNextWords(
          /* currentWord= */ "",
          candidates,
          /* maxSuggestions= */ MAX_RESULTS,
          /* incognitoMode= */ true,
          config);
      if (candidates.isEmpty()) {
        break;
      }
      final CharSequence head = candidates.get(0);
      if (head == null) {
        break;
      }
      final String raw = head.toString();
      final List<String> tokens = tokenizeSentence(raw);
      if (tokens.isEmpty()) {
        out.emptyOrNonWordPicks++;
        break;
      }
      if (tokens.size() > 1) {
        out.picksWithMultipleTokens++;
      }
      for (String token : tokens) {
        if (token == null || token.trim().isEmpty()) continue;
        out.appendToken(token);
        mPipeline.notifyWordCommitted(token, /* incognitoMode= */ true);
        if (out.generatedWords >= CHAIN_TARGET_WORDS) break;
      }
    }

    return out;
  }

  private boolean isQualityEvalEnabled() {
    final String arg = InstrumentationRegistry.getArguments().getString(EVAL_ENABLED_ARG);
    if ("1".equals(arg) || "true".equalsIgnoreCase(arg)) return true;
    final String property = System.getProperty(EVAL_ENABLED_ARG);
    return "1".equals(property) || "true".equalsIgnoreCase(property);
  }

  private int resolveMaxCases() {
    final String arg = InstrumentationRegistry.getArguments().getString(MAX_CASES_ARG);
    if (arg != null && !arg.trim().isEmpty()) {
      try {
        return Math.max(1, Integer.parseInt(arg.trim()));
      } catch (NumberFormatException ignored) {
        // fall through
      }
    }
    final String property = System.getProperty(MAX_CASES_ARG);
    if (property != null && !property.trim().isEmpty()) {
      try {
        return Math.max(1, Integer.parseInt(property.trim()));
      } catch (NumberFormatException ignored) {
        // fall through
      }
    }
    return DEFAULT_MAX_CASES;
  }

  @NonNull
  private List<List<String>> loadCorpusSentences() throws Exception {
    final Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
    try (InputStream inputStream = testContext.getAssets().open("eval/english_sentences.txt");
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      final List<List<String>> out = new ArrayList<>();
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().startsWith("#")) continue;
        final List<String> tokens = tokenizeSentence(line);
        if (tokens.size() >= 2) {
          out.add(tokens);
        }
      }
      return out;
    }
  }

  @NonNull
  private List<String> tokenizeSentence(@Nullable String sentence) {
    if (sentence == null) return java.util.Collections.emptyList();
    final String cleaned = sentence.toLowerCase(Locale.US).replaceAll("[^a-z']+", " ").trim();
    if (cleaned.isEmpty()) return java.util.Collections.emptyList();
    return Arrays.asList(cleaned.split("\\s+"));
  }

  @NonNull
  private String metricsJsonFromSummary(
      @NonNull String mode,
      @NonNull String ngramModelId,
      @NonNull String neuralModelId,
      @Nullable String corpusSha256,
      @NonNull String summary) {
    int cases = 0;
    double latencyMeanMs = 0.0;
    double latencyP50Ms = 0.0;
    double latencyP95Ms = 0.0;
    final java.util.LinkedHashMap<String, Double> metrics = new java.util.LinkedHashMap<>();
    String chainSample = "";

    final String[] lines = summary.split("\\n");
    for (String rawLine : lines) {
      final String line = rawLine == null ? "" : rawLine.trim();
      if (line.startsWith("Cases:")) {
        try {
          cases = Integer.parseInt(line.substring("Cases:".length()).trim());
        } catch (NumberFormatException ignored) {
          // best-effort
        }
        continue;
      }
      if (line.startsWith("latency_ms:")) {
        // latency_ms: mean=%.2f p50=%.2f p95=%.2f
        final String[] parts = line.substring("latency_ms:".length()).trim().split("\\s+");
        for (String part : parts) {
          final String[] kv = part.split("=", 2);
          if (kv.length != 2) continue;
          final String k = kv[0].trim();
          final String v = kv[1].trim();
          try {
            final double d = Double.parseDouble(v);
            if ("mean".equals(k)) latencyMeanMs = d;
            if ("p50".equals(k)) latencyP50Ms = d;
            if ("p95".equals(k)) latencyP95Ms = d;
          } catch (NumberFormatException ignored) {
            // best-effort
          }
        }
        continue;
      }
      if (line.startsWith("chain_sample:")) {
        chainSample = line.substring("chain_sample:".length()).trim();
        continue;
      }

      final int sep = line.indexOf(':');
      if (sep <= 0) continue;
      final String key = line.substring(0, sep).trim();
      final String value = line.substring(sep + 1).trim();
      if (key.isEmpty() || value.isEmpty()) continue;
      try {
        metrics.put(key, Double.parseDouble(value));
      } catch (NumberFormatException ignored) {
        // best-effort: we only export numeric scalar metrics from the report.
      }
    }

    final StringBuilder out = new StringBuilder();
    out.append("{\n");
    out.append("  \"schema\": \"nextword-quality\",\n");
    out.append("  \"harness\": \"instrumented\",\n");
    out.append("  \"engine_mode\": ").append(jsonString(mode)).append(",\n");
    out.append("  \"ngram_model_id\": ").append(jsonString(ngramModelId)).append(",\n");
    out.append("  \"neural_model_id\": ").append(jsonString(neuralModelId)).append(",\n");
    out.append("  \"corpus_sha256\": ").append(jsonString(corpusSha256)).append(",\n");
    out.append("  \"timestamp_ms\": ").append(System.currentTimeMillis()).append(",\n");
    out.append("  \"cases\": ").append(cases).append(",\n");
    out.append("  \"metrics\": {\n");
    boolean first = true;
    for (java.util.Map.Entry<String, Double> e : metrics.entrySet()) {
      if (!first) out.append(",\n");
      first = false;
      out.append("    ").append(jsonString(e.getKey())).append(": ");
      out.append(String.format(Locale.US, "%.6f", e.getValue()));
    }
    if (!metrics.isEmpty()) out.append(",\n");
    out.append("    \"latency_ms\": {\n");
    out.append(String.format(Locale.US, "      \"mean\": %.6f,%n", latencyMeanMs));
    out.append(String.format(Locale.US, "      \"p50\": %.6f,%n", latencyP50Ms));
    out.append(String.format(Locale.US, "      \"p95\": %.6f%n", latencyP95Ms));
    out.append("    }\n");
    out.append("  },\n");
    out.append("  \"samples\": {\n");
    out.append("    \"chain\": ").append(jsonString(chainSample)).append("\n");
    out.append("  },\n");
    out.append("  \"summary_text\": ").append(jsonString(summary)).append("\n");
    out.append("}\n");
    return out.toString();
  }

  @Nullable
  private String computeCorpusSha256() {
    final Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
    try (InputStream inputStream = testContext.getAssets().open("eval/english_sentences.txt")) {
      return sha256Hex(inputStream);
    } catch (Exception ignored) {
      return null;
    }
  }

  @Nullable
  private static String sha256Hex(@NonNull InputStream input) throws java.io.IOException {
    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (java.security.NoSuchAlgorithmException e) {
      return null;
    }
    final byte[] buffer = new byte[8192];
    int read;
    while ((read = input.read(buffer)) != -1) {
      digest.update(buffer, 0, read);
    }
    return hex(digest.digest());
  }

  @NonNull
  private static String hex(byte[] bytes) {
    final char[] out = new char[bytes.length * 2];
    final char[] digits = "0123456789abcdef".toCharArray();
    for (int i = 0; i < bytes.length; i++) {
      final int b = bytes[i] & 0xff;
      out[i * 2] = digits[b >>> 4];
      out[i * 2 + 1] = digits[b & 0xf];
    }
    return new String(out);
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

  private void writeMetricsReport(
      @NonNull Context context,
      @NonNull String mode,
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
    final File outTextFile = new File(reportsDir, mode + "-quality-" + timestampMs + ".txt");
    final File outJsonFile = new File(reportsDir, mode + "-quality-" + timestampMs + ".json");
    try (FileOutputStream output = new FileOutputStream(outTextFile)) {
      output.write(summary.getBytes(StandardCharsets.UTF_8));
      output.flush();
      Log.i(TAG, "Wrote next-word quality report to " + outTextFile.getAbsolutePath());
    } catch (Exception ignored) {
      // best-effort
    }
    try (FileOutputStream output = new FileOutputStream(outJsonFile)) {
      output.write(json.getBytes(StandardCharsets.UTF_8));
      output.flush();
      Log.i(TAG, "Wrote next-word quality JSON report to " + outJsonFile.getAbsolutePath());
    } catch (Exception ignored) {
      // best-effort
    }
  }

  private void restoreSelectedModelId(
      @NonNull ModelStore store, @NonNull EngineType engineType, @Nullable String modelId) {
    if (modelId == null || modelId.trim().isEmpty()) {
      store.clearSelectedModelId(engineType);
    } else {
      store.persistSelectedModelId(engineType, modelId);
    }
  }

  private void ensureNgramModelActive(@NonNull Context context) {
    final ModelStore store = new ModelStore(context);
    ModelStore.ActiveModel active = store.ensureActiveModel(EngineType.NGRAM);
    if (active == null) {
      final ModelDefinition definition = stageFixtureNgramModel(context);
      store.persistSelectedModelId(EngineType.NGRAM, definition.getId());
      active = store.ensureActiveModel(EngineType.NGRAM);
    }
    if (active == null) {
      throw new AssertionError("No n-gram model is available for evaluation.");
    }
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
        Log.w(TAG, "Neural model download failed; neural/hybrid eval will be skipped.", exception);
        return false;
      }
      Log.w(TAG, "Neural model download failed; continuing with installed model.", exception);
    }
    store.persistSelectedModelId(EngineType.NEURAL, DEFAULT_TEST_NEURAL_MODEL_ID);
    return store.ensureActiveModel(EngineType.NEURAL) != null;
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
      outputStream.flush();
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

  private static final class EmptyNextWordSuggestions implements NextWordSuggestions {

    @NonNull
    @Override
    public Iterable<String> getNextWords(
        @NonNull String currentWord, int maxResults, int minWordUsage) {
      return java.util.Collections.emptyList();
    }

    @Override
    public void notifyNextTypedWord(@NonNull String currentWord) {}

    @Override
    public void resetSentence() {}
  }

  private static final class Metrics {
    private int totalCases;
    private int top1Hits;
    private int top3Hits;
    private double reciprocalRankSum;
    private int top1ImmediateRepeat;
    private int top1JoinedStopwordPair;
    private int casesWithNoPredictions;
    private int candidatesWithWhitespace;
    private int candidatesWithJoinedStopwordPair;
    private int totalCandidatesScanned;
    private final List<Long> latenciesNs = new ArrayList<>();

    void recordCase(
        @NonNull String lastContextToken,
        @NonNull String expectedToken,
        @Nullable List<String> predictions,
        long latencyNs) {
      totalCases++;
      latenciesNs.add(latencyNs);

      if (predictions == null || predictions.isEmpty()) {
        casesWithNoPredictions++;
        return;
      }

      final String expected = normalize(expectedToken);
      final String lastContext = normalize(lastContextToken);
      final String head = normalize(predictions.get(0));
      if (head.equals(lastContext)) {
        top1ImmediateRepeat++;
      }
      if (containsJoinedStopwordPair(head)) {
        top1JoinedStopwordPair++;
      }

      int matchIndex = -1;
      for (int i = 0; i < predictions.size(); i++) {
        final String candidate = predictions.get(i);
        totalCandidatesScanned++;
        if (candidate != null && candidate.matches(".*\\s+.*")) {
          candidatesWithWhitespace++;
        }
        if (containsJoinedStopwordPair(normalize(candidate))) {
          candidatesWithJoinedStopwordPair++;
        }
        if (matchIndex == -1 && expected.equals(normalize(candidate))) {
          matchIndex = i;
        }
      }

      if (matchIndex >= 0) {
        reciprocalRankSum += 1.0 / (matchIndex + 1);
        if (matchIndex == 0) {
          top1Hits++;
        }
        if (matchIndex < 3) {
          top3Hits++;
        }
      }
    }

    @NonNull
    String renderSummary(
        @NonNull String engineMode,
        @NonNull String ngramModelId,
        @NonNull String neuralModelId,
        int usedCases,
        @NonNull ChainMetrics chainMetrics) {
      final int cases = Math.min(totalCases, usedCases);
      final double top1Rate = rate(top1Hits, cases);
      final double top3Rate = rate(top3Hits, cases);
      final double mrr = cases == 0 ? 0.0 : reciprocalRankSum / cases;
      final double emptyRate = rate(casesWithNoPredictions, cases);
      final double repeatRate = rate(top1ImmediateRepeat, cases);
      final double top1JoinedStopwordPairRate = rate(top1JoinedStopwordPair, cases);
      final double whitespaceRate = rate(candidatesWithWhitespace, totalCandidatesScanned);
      final double joinedStopwordPairRate =
          rate(candidatesWithJoinedStopwordPair, totalCandidatesScanned);

      final LatencyStats latencyStats = LatencyStats.from(latenciesNs);
      final int chainEvaluableWords = chainMetrics.evaluableWordCount();

      return ""
          + "Next-word quality metrics (instrumented, best-effort)\n"
          + "Engine: "
          + engineMode
          + "\n"
          + "NgramModel: "
          + ngramModelId
          + "\n"
          + "NeuralModel: "
          + neuralModelId
          + "\n"
          + "Cases: "
          + cases
          + "\n"
          + String.format(Locale.US, "top1_hit_rate: %.4f%n", top1Rate)
          + String.format(Locale.US, "top3_hit_rate: %.4f%n", top3Rate)
          + String.format(Locale.US, "mrr: %.4f%n", mrr)
          + String.format(Locale.US, "empty_prediction_rate: %.4f%n", emptyRate)
          + String.format(Locale.US, "top1_immediate_repeat_rate: %.4f%n", repeatRate)
          + String.format(
              Locale.US, "top1_joined_stopword_pair_rate: %.4f%n", top1JoinedStopwordPairRate)
          + String.format(Locale.US, "whitespace_in_candidates_rate: %.4f%n", whitespaceRate)
          + String.format(
              Locale.US, "joined_stopword_pair_in_candidates_rate: %.4f%n", joinedStopwordPairRate)
          + String.format(Locale.US, "chain_words_generated: %d%n", chainMetrics.generatedWords)
          + String.format(
              Locale.US,
              "chain_immediate_repeat_rate: %.4f%n",
              rate(chainMetrics.immediateRepeats, chainEvaluableWords))
          + String.format(
              Locale.US,
              "chain_bigram_loop_rate: %.4f%n",
              rate(chainMetrics.bigramLoops, chainEvaluableWords))
          + String.format(
              Locale.US,
              "chain_trigram_loop_rate: %.4f%n",
              rate(chainMetrics.trigramLoops, chainEvaluableWords))
          + String.format(
              Locale.US,
              "chain_joined_stopword_pair_rate: %.4f%n",
              rate(chainMetrics.joinedStopwordPairs, chainEvaluableWords))
          + String.format(
              Locale.US,
              "chain_multi_token_pick_rate: %.4f%n",
              rate(chainMetrics.picksWithMultipleTokens, chainMetrics.picksAttempted))
          + String.format(
              Locale.US,
              "chain_empty_or_non_word_pick_rate: %.4f%n",
              rate(chainMetrics.emptyOrNonWordPicks, chainMetrics.picksAttempted))
          + String.format(Locale.US, "chain_sample: %s%n", chainMetrics.sample())
          + String.format(
              Locale.US,
              "latency_ms: mean=%.2f p50=%.2f p95=%.2f%n",
              latencyStats.meanMs,
              latencyStats.p50Ms,
              latencyStats.p95Ms);
    }

    private double rate(int numerator, int denominator) {
      if (denominator <= 0) return 0.0;
      return (double) numerator / denominator;
    }

    @NonNull
    private String normalize(@Nullable String token) {
      if (token == null) return "";
      return token.toLowerCase(Locale.US).trim();
    }

    private boolean containsJoinedStopwordPair(@Nullable String normalizedToken) {
      if (normalizedToken == null || normalizedToken.isEmpty()) return false;
      for (String fused : ChainMetrics.JOINED_STOPWORD_PAIRS) {
        if (normalizedToken.contains(fused)) return true;
      }
      return false;
    }
  }

  private static final class LatencyStats {
    final double meanMs;
    final double p50Ms;
    final double p95Ms;

    private LatencyStats(double meanMs, double p50Ms, double p95Ms) {
      this.meanMs = meanMs;
      this.p50Ms = p50Ms;
      this.p95Ms = p95Ms;
    }

    static LatencyStats from(@NonNull List<Long> latenciesNs) {
      if (latenciesNs.isEmpty()) {
        return new LatencyStats(0.0, 0.0, 0.0);
      }
      long sum = 0L;
      for (Long value : latenciesNs) {
        sum += value == null ? 0L : value;
      }
      final double meanMs = nanosToMs((double) sum / latenciesNs.size());

      final ArrayList<Long> sorted = new ArrayList<>(latenciesNs);
      sorted.removeIf(v -> v == null);
      sorted.sort(Long::compareTo);
      final double p50Ms = nanosToMs(percentile(sorted, 50));
      final double p95Ms = nanosToMs(percentile(sorted, 95));
      return new LatencyStats(meanMs, p50Ms, p95Ms);
    }

    private static long percentile(@NonNull ArrayList<Long> values, int percentile) {
      if (values.isEmpty()) return 0L;
      final int index =
          Math.min(values.size() - 1, (int) Math.ceil((percentile / 100.0) * values.size()) - 1);
      return values.get(Math.max(0, index));
    }

    private static double nanosToMs(double nanos) {
      return nanos / 1_000_000.0;
    }
  }

  private static final class ChainMetrics {
    private static final List<String> JOINED_STOPWORD_PAIRS =
        java.util.Arrays.asList(
            "ofthe", "inthe", "tothe", "onthe", "andthe", "forthe", "fromthe", "withthe", "atthe");

    private final List<String> generatedTokens = new ArrayList<>();
    int picksAttempted;
    int generatedWords;
    int immediateRepeats;
    int bigramLoops;
    int trigramLoops;
    int joinedStopwordPairs;
    int picksWithMultipleTokens;
    int emptyOrNonWordPicks;

    int evaluableWordCount() {
      return Math.max(0, generatedWords - 1);
    }

    void appendToken(@NonNull String rawToken) {
      final String token = rawToken.toLowerCase(Locale.US).trim();
      if (token.isEmpty()) {
        return;
      }

      if (!generatedTokens.isEmpty()) {
        final String prev = generatedTokens.get(generatedTokens.size() - 1);
        if (token.equals(prev)) {
          immediateRepeats++;
        }

        if (generatedTokens.size() >= 3) {
          final int n = generatedTokens.size();
          final String w1 = generatedTokens.get(n - 3);
          final String w2 = generatedTokens.get(n - 2);
          final String w3 = generatedTokens.get(n - 1);
          if (w1.equals(w3) && token.equals(w2)) {
            bigramLoops++;
          }
        }

        if (generatedTokens.size() >= 5) {
          final int n = generatedTokens.size();
          final String w1 = generatedTokens.get(n - 5);
          final String w2 = generatedTokens.get(n - 4);
          final String w3 = generatedTokens.get(n - 3);
          final String w4 = generatedTokens.get(n - 2);
          final String w5 = generatedTokens.get(n - 1);
          if (w1.equals(w4) && w2.equals(w5) && token.equals(w3)) {
            trigramLoops++;
          }
        }
      }

      if (JOINED_STOPWORD_PAIRS.contains(token)) {
        joinedStopwordPairs++;
      }

      generatedTokens.add(token);
      generatedWords++;
    }

    @NonNull
    String sample() {
      final int limit = Math.min(20, generatedTokens.size());
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < limit; i++) {
        if (i > 0) sb.append(' ');
        sb.append(generatedTokens.get(i));
      }
      if (generatedTokens.size() > limit) {
        sb.append(" …");
      }
      return sb.toString();
    }
  }
}
