package wtf.uhoh.newsoftkeyboard.engine.neural;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import wtf.uhoh.newsoftkeyboard.engine.EngineType;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelDefinition;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelStore;

/**
 * Best-effort local metrics harness for neural <em>prefix completion</em> quality — the path added
 * in #1+#2 ({@link NeuralPredictionManager#completeWordWithScoringContext}). For each corpus word it
 * feeds the preceding context plus a truncated prefix of the word and checks whether the full word
 * is recovered, measuring hit rate / MRR, plus the hygiene invariants the path promises (every
 * candidate begins with the typed prefix and contains no whitespace).
 *
 * <p>Opt in with {@code RUN_PREFIX_COMPLETION_EVAL=1}; runs under {@code -PneuralHostOrt} (which puts
 * the desktop ONNX Runtime on the test classpath). Without those it skips, like the next-word eval.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class NeuralPrefixCompletionQualityEvalHostTest {

  private static final String DEFAULT_TEST_MODEL_ID = "distilgpt2_mixedcase_sanity";
  private static final String DEFAULT_TEST_MODEL_BUNDLE_URL =
      "https://fdroid.uh-oh.wtf/models/distilgpt2_mixedcase_sanity_v1.zip";

  private static final String EVAL_ENABLED_ENV = "RUN_PREFIX_COMPLETION_EVAL";
  private static final String MAX_CASES_ENV = "PREFIX_COMPLETION_MAX_CASES";
  private static final int DEFAULT_MAX_CASES = 200;
  private static final int CONTEXT_WINDOW_WORDS = 20;
  private static final int MAX_RESULTS = 8;
  private static final int MIN_TARGET_WORD_LEN = 4;

  @Test
  public void evaluateNeuralPrefixCompletionQualityOnSmallCorpus() throws Exception {
    assumeTrue(
        "Set RUN_PREFIX_COMPLETION_EVAL=1 to run prefix-completion evaluation locally.",
        isPrefixEvalEnabled());
    assumeTrue("ONNX runtime not available on host", isOnnxRuntimeAvailable());

    final File modelDir = resolveModelDirectory();
    assumeTrue(
        "Missing NEURAL_MODEL_DIR with model files", modelDir != null && modelDir.isDirectory());

    final ModelStore store = fakeStoreFor(modelDir);
    assumeTrue("Model directory missing required files.", store != null);

    final Context context = ApplicationProvider.getApplicationContext();
    final NeuralPredictionManager manager = new NeuralPredictionManager(context, store);
    assumeTrue("Activation failed: " + manager.getLastActivationError(), manager.activate());

    final List<List<String>> sentences = loadCorpusSentences();
    assumeTrue("Missing evaluation corpus sentences.", !sentences.isEmpty());

    final int maxCases = resolveMaxCases();
    final Metrics metrics = new Metrics();

    outer:
    for (List<String> sentence : sentences) {
      for (int idx = 1; idx < sentence.size(); idx++) {
        final String target = sentence.get(idx);
        if (target.length() < MIN_TARGET_WORD_LEN) continue;

        // "Mid-typing" probe: the user has typed roughly the first half of the word.
        final int prefixLen = Math.max(2, target.length() / 2);
        if (prefixLen >= target.length()) continue;
        final String prefix = target.substring(0, prefixLen);

        final int start = Math.max(0, idx - CONTEXT_WINDOW_WORDS);
        final String[] ctx = sentence.subList(start, idx).toArray(new String[0]);
        if (ctx.length == 0) continue;

        final long t0 = System.nanoTime();
        final List<String> completions =
            manager.completeWordWithScoringContext(ctx, prefix, MAX_RESULTS).candidates;
        metrics.recordLatencyMs((System.nanoTime() - t0) / 1_000_000.0);

        metrics.recordCase(target, prefix, completions);
        if (++metrics.cases >= maxCases) break outer;
      }
    }

    manager.deactivate();
    assumeTrue("No eligible prefix-completion cases in corpus.", metrics.cases > 0);

    final String summary = metrics.summarize();
    System.out.println(summary);

    // Hygiene invariants the completion path guarantees — these are correctness, not quality, so we
    // assert them rather than just report. (Quality hit-rates are reported for tracking, not gated.)
    assertEquals(
        "Every candidate must begin with the typed prefix",
        1.0,
        metrics.prefixConsistencyRate(),
        1e-9);
    assertEquals(
        "Candidates must not contain whitespace", 0.0, metrics.whitespaceRate(), 1e-9);
  }

  private static final class Metrics {
    int cases;
    int top1Hits;
    int topKHits;
    double mrrSum;
    int emptyCases;
    int prefixConsistentCases;
    int whitespaceCases;
    final List<Double> latenciesMs = new ArrayList<>();

    void recordLatencyMs(double ms) {
      latenciesMs.add(ms);
    }

    void recordCase(
        @NonNull String target, @NonNull String prefix, @NonNull List<String> completions) {
      if (completions.isEmpty()) {
        emptyCases++;
        // An empty result is trivially prefix-consistent and whitespace-free.
        prefixConsistentCases++;
        return;
      }
      boolean allPrefix = true;
      boolean anyWhitespace = false;
      int rank = -1;
      for (int i = 0; i < completions.size(); i++) {
        final String c = completions.get(i);
        if (c == null) continue;
        if (!c.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
          allPrefix = false;
        }
        for (int j = 0; j < c.length(); j++) {
          if (Character.isWhitespace(c.charAt(j))) anyWhitespace = true;
        }
        if (rank < 0 && c.equalsIgnoreCase(target)) {
          rank = i;
        }
      }
      if (allPrefix) prefixConsistentCases++;
      if (anyWhitespace) whitespaceCases++;
      if (rank == 0) top1Hits++;
      if (rank >= 0) {
        topKHits++;
        mrrSum += 1.0 / (rank + 1);
      }
    }

    double prefixConsistencyRate() {
      return cases == 0 ? 1.0 : (double) prefixConsistentCases / cases;
    }

    double whitespaceRate() {
      return cases == 0 ? 0.0 : (double) whitespaceCases / cases;
    }

    @NonNull
    String summarize() {
      final double[] lat = latencyStats();
      final StringBuilder sb = new StringBuilder();
      sb.append("Prefix-completion quality eval\n");
      sb.append("Cases: ").append(cases).append('\n');
      sb.append(fmt("top1_hit_rate", rate(top1Hits))).append('\n');
      sb.append(fmt("topk_hit_rate", rate(topKHits))).append('\n');
      sb.append(fmt("mrr", cases == 0 ? 0.0 : mrrSum / cases)).append('\n');
      sb.append(fmt("empty_rate", rate(emptyCases))).append('\n');
      sb.append(fmt("prefix_consistency_rate", prefixConsistencyRate())).append('\n');
      sb.append(fmt("whitespace_in_candidates_rate", whitespaceRate())).append('\n');
      sb.append(
          String.format(
              Locale.US, "latency_ms: mean=%.2f p50=%.2f p95=%.2f", lat[0], lat[1], lat[2]));
      return sb.toString();
    }

    private double rate(int hits) {
      return cases == 0 ? 0.0 : (double) hits / cases;
    }

    private String fmt(String key, double value) {
      return String.format(Locale.US, "%s: %.4f", key, value);
    }

    private double[] latencyStats() {
      if (latenciesMs.isEmpty()) return new double[] {0, 0, 0};
      final List<Double> sorted = new ArrayList<>(latenciesMs);
      Collections.sort(sorted);
      double sum = 0;
      for (double d : sorted) sum += d;
      final double mean = sum / sorted.size();
      final double p50 = sorted.get((int) (sorted.size() * 0.50));
      final double p95 = sorted.get(Math.min(sorted.size() - 1, (int) (sorted.size() * 0.95)));
      return new double[] {mean, p50, p95};
    }
  }

  private boolean isPrefixEvalEnabled() {
    final String env = System.getenv(EVAL_ENABLED_ENV);
    if ("1".equals(env) || "true".equalsIgnoreCase(env)) return true;
    final String property = System.getProperty(EVAL_ENABLED_ENV);
    return "1".equals(property) || "true".equalsIgnoreCase(property);
  }

  private int resolveMaxCases() {
    for (String raw :
        new String[] {System.getenv(MAX_CASES_ENV), System.getProperty(MAX_CASES_ENV)}) {
      if (raw != null && !raw.trim().isEmpty()) {
        try {
          return Math.max(1, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ignored) {
          // fall through
        }
      }
    }
    return DEFAULT_MAX_CASES;
  }

  @NonNull
  private List<List<String>> loadCorpusSentences() throws IOException {
    final InputStream input =
        NeuralPrefixCompletionQualityEvalHostTest.class.getResourceAsStream(
            "/eval/english_sentences.txt");
    if (input == null) return Collections.emptyList();

    final List<List<String>> out = new ArrayList<>();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        final List<String> tokens = tokenizeSentence(line);
        if (tokens.size() >= 2) {
          out.add(tokens);
        }
      }
    }
    return out;
  }

  @NonNull
  private List<String> tokenizeSentence(@Nullable String sentence) {
    if (sentence == null) return Collections.emptyList();
    final String cleaned = sentence.toLowerCase(Locale.US).replaceAll("[^a-z']+", " ").trim();
    if (cleaned.isEmpty()) return Collections.emptyList();
    return Arrays.asList(cleaned.split("\\s+"));
  }

  @Nullable
  private ModelStore fakeStoreFor(@NonNull File modelDir) {
    final File onnx = new File(modelDir, "model_int8.onnx");
    final File vocab = new File(modelDir, "vocab.json");
    final File merges = new File(modelDir, "merges.txt");
    if (!onnx.exists() || !vocab.exists() || !merges.exists()) {
      return null;
    }

    final ModelDefinition definition =
        ModelDefinition.builder(modelDir.getName())
            .setLabel("host-test-model")
            .setEngineType(EngineType.NEURAL)
            .setOnnxFile(onnx.getName(), null, null)
            .setTokenizerVocabFile(vocab.getName(), null, null)
            .setTokenizerMergesFile(merges.getName(), null, null)
            .build();

    final LinkedHashMap<String, File> files = new LinkedHashMap<>();
    files.put("onnx", onnx);
    files.put("tokenizer.vocab", vocab);
    files.put("tokenizer.merges", merges);

    final ModelStore.ActiveModel activeModel =
        new ModelStore.ActiveModel(definition, modelDir, files);

    final Context context = ApplicationProvider.getApplicationContext();
    return new ModelStore(context) {
      @Override
      public ActiveModel ensureActiveModel(EngineType engineType) {
        return engineType == EngineType.NEURAL ? activeModel : null;
      }
    };
  }

  private boolean isOnnxRuntimeAvailable() {
    // See NeuralPredictionManagerHostTest: probe OrtEnvironment, not System.loadLibrary, so the
    // desktop ONNX Runtime artifact (-PneuralHostOrt) is detected on a supported host.
    try {
      return ai.onnxruntime.OrtEnvironment.getEnvironment() != null;
    } catch (Throwable t) {
      return false;
    }
  }

  @Nullable
  private File resolveModelDirectory() {
    final String propertyPath = System.getProperty("NEURAL_MODEL_DIR");
    if (propertyPath != null && !propertyPath.trim().isEmpty()) {
      return new File(propertyPath);
    }
    final String envPath = System.getenv("NEURAL_MODEL_DIR");
    if (envPath != null && !envPath.trim().isEmpty()) {
      return new File(envPath);
    }
    return prepareDownloadedModelDirectory();
  }

  @Nullable
  private File prepareDownloadedModelDirectory() {
    final File baseDir = new File(System.getProperty("java.io.tmpdir"), "nsk-neural-prefix-model");
    final File modelDir = new File(baseDir, DEFAULT_TEST_MODEL_ID);
    if (isModelDirectoryUsable(modelDir)) {
      return modelDir;
    }
    try {
      downloadAndExtractZipToDirectory(DEFAULT_TEST_MODEL_BUNDLE_URL, modelDir);
    } catch (Exception e) {
      return null;
    }
    return isModelDirectoryUsable(modelDir) ? modelDir : null;
  }

  private boolean isModelDirectoryUsable(@NonNull File modelDir) {
    return new File(modelDir, "model_int8.onnx").exists()
        && new File(modelDir, "vocab.json").exists()
        && new File(modelDir, "merges.txt").exists();
  }

  private void downloadAndExtractZipToDirectory(@NonNull String url, @NonNull File modelDir)
      throws IOException {
    if (!modelDir.exists() && !modelDir.mkdirs()) {
      throw new IOException("Failed creating model directory: " + modelDir);
    }
    try (BufferedInputStream input = new BufferedInputStream(new URL(url).openStream());
        ZipInputStream zipInputStream = new ZipInputStream(input)) {
      ZipEntry entry;
      final byte[] buffer = new byte[8 * 1024];
      while ((entry = zipInputStream.getNextEntry()) != null) {
        if (entry.isDirectory()) continue;
        final String entryName = entry.getName();
        if (entryName == null || entryName.trim().isEmpty()) continue;
        final int lastSlash = Math.max(entryName.lastIndexOf('/'), entryName.lastIndexOf('\\'));
        final String leafName = lastSlash >= 0 ? entryName.substring(lastSlash + 1) : entryName;
        if (leafName.trim().isEmpty()) continue;
        final File outFile = new File(modelDir, leafName);
        try (FileOutputStream output = new FileOutputStream(outFile)) {
          int read;
          while ((read = zipInputStream.read(buffer)) > 0) {
            output.write(buffer, 0, read);
          }
        }
      }
    }
  }
}
