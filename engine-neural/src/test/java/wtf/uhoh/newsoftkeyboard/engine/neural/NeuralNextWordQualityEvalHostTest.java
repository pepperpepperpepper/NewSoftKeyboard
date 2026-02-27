package wtf.uhoh.newsoftkeyboard.engine.neural;

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
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
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

/** Best-effort local metrics harness for neural next-word quality. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class NeuralNextWordQualityEvalHostTest {

  private static final String DEFAULT_TEST_MODEL_ID = "distilgpt2_mixedcase_sanity";
  private static final String DEFAULT_TEST_MODEL_BUNDLE_URL =
      "https://fdroid.uh-oh.wtf/models/distilgpt2_mixedcase_sanity_v1.zip";

  private static final String EVAL_ENABLED_ENV = "RUN_NEXTWORD_QUALITY_EVAL";
  private static final String MAX_CASES_ENV = "NEXTWORD_QUALITY_MAX_CASES";
  private static final int DEFAULT_MAX_CASES = 200;
  private static final int CONTEXT_WINDOW_WORDS = 20;
  private static final int MAX_RESULTS = 8;
  private static final int CHAIN_TARGET_WORDS = 40;

  @Test
  public void evaluateNeuralNextWordQualityOnSmallCorpus() throws Exception {
    assumeTrue(
        "Set RUN_NEXTWORD_QUALITY_EVAL=1 to run quality evaluation locally.",
        isQualityEvalEnabled());
    assumeTrue("ONNX runtime not available on host", isOnnxRuntimeAvailable());

    final File modelDir = resolveModelDirectory();
    assumeTrue(
        "Missing NEURAL_MODEL_DIR with model files", modelDir != null && modelDir.isDirectory());

    final ActiveModelSpec model = activeModelSpec(modelDir);
    assumeTrue(
        "Model directory missing required files (model_int8.onnx/vocab.json/merges.txt).",
        model != null);

    final Context context = ApplicationProvider.getApplicationContext();
    final NeuralPredictionManager manager = new NeuralPredictionManager(context, model.store);
    assumeTrue("Activation failed: " + manager.getLastActivationError(), manager.activate());

    final List<List<String>> sentences = loadCorpusSentences();
    assumeTrue("Missing evaluation corpus sentences.", !sentences.isEmpty());

    final int maxCases = resolveMaxCases();
    final Metrics metrics = new Metrics();
    int cases = 0;
    for (List<String> sentence : sentences) {
      for (int i = 1; i < sentence.size(); i++) {
        if (cases >= maxCases) break;
        final int contextStart = Math.max(0, i - CONTEXT_WINDOW_WORDS);
        final List<String> contextTokens = sentence.subList(contextStart, i);
        final String expected = sentence.get(i);
        final String lastContext = contextTokens.get(contextTokens.size() - 1);

        final long startNs = System.nanoTime();
        final List<String> predictions =
            manager.predictNextWords(contextTokens.toArray(new String[0]), MAX_RESULTS);
        final long latencyNs = System.nanoTime() - startNs;

        metrics.recordCase(lastContext, expected, predictions, latencyNs);
        cases++;
      }
      if (cases >= maxCases) break;
    }

    final ChainMetrics chainMetrics = evaluateGreedyChain(manager);

    manager.deactivate();

    final String summary = metrics.renderSummary(model.definition.getId(), cases, chainMetrics);
    final String corpusSha256 = computeCorpusSha256();
    final String json = metricsJsonFromSummary(model.definition.getId(), corpusSha256, summary);
    System.out.println(summary);
    writeMetricsReport(summary, json);
  }

  @NonNull
  private ChainMetrics evaluateGreedyChain(@NonNull NeuralPredictionManager manager) {
    final ChainMetrics out = new ChainMetrics();
    final ArrayList<String> context = new ArrayList<>();
    context.add("the");
    out.appendToken("the");

    while (out.generatedWords < CHAIN_TARGET_WORDS) {
      out.picksAttempted++;
      final List<String> predictions =
          manager.predictNextWords(context.toArray(new String[0]), MAX_RESULTS);
      if (predictions == null || predictions.isEmpty()) {
        break;
      }
      final String head = predictions.get(0);
      if (head == null) break;

      final List<String> tokens = tokenizeSentence(head);
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
        context.add(token);
        while (context.size() > CONTEXT_WINDOW_WORDS) {
          context.remove(0);
        }
        if (out.generatedWords >= CHAIN_TARGET_WORDS) break;
      }
    }

    return out;
  }

  private boolean isQualityEvalEnabled() {
    final String env = System.getenv(EVAL_ENABLED_ENV);
    if ("1".equals(env) || "true".equalsIgnoreCase(env)) return true;
    final String property = System.getProperty(EVAL_ENABLED_ENV);
    return "1".equals(property) || "true".equalsIgnoreCase(property);
  }

  private int resolveMaxCases() {
    final String env = System.getenv(MAX_CASES_ENV);
    if (env != null && !env.trim().isEmpty()) {
      try {
        return Math.max(1, Integer.parseInt(env.trim()));
      } catch (NumberFormatException ignored) {
        // fall through
      }
    }
    final String property = System.getProperty(MAX_CASES_ENV);
    if (property != null && !property.trim().isEmpty()) {
      try {
        return Math.max(1, Integer.parseInt(property.trim()));
      } catch (NumberFormatException ignored) {
        // fall through
      }
    }
    return DEFAULT_MAX_CASES;
  }

  private static final class ActiveModelSpec {
    @NonNull final ModelDefinition definition;
    @NonNull final ModelStore store;

    private ActiveModelSpec(@NonNull ModelDefinition definition, @NonNull ModelStore store) {
      this.definition = definition;
      this.store = store;
    }
  }

  @Nullable
  private ActiveModelSpec activeModelSpec(@NonNull File modelDir) {
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
    final ModelStore fakeStore =
        new ModelStore(context) {
          @Override
          public ActiveModel ensureActiveModel(EngineType engineType) {
            return engineType == EngineType.NEURAL ? activeModel : null;
          }
        };

    return new ActiveModelSpec(definition, fakeStore);
  }

  @NonNull
  private List<List<String>> loadCorpusSentences() throws IOException {
    final InputStream input =
        NeuralNextWordQualityEvalHostTest.class.getResourceAsStream("/eval/english_sentences.txt");
    if (input == null) return java.util.Collections.emptyList();

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
    if (sentence == null) return java.util.Collections.emptyList();
    final String cleaned = sentence.toLowerCase(Locale.US).replaceAll("[^a-z']+", " ").trim();
    if (cleaned.isEmpty()) return java.util.Collections.emptyList();
    return Arrays.asList(cleaned.split("\\s+"));
  }

  @NonNull
  private String metricsJsonFromSummary(
      @NonNull String modelId, @Nullable String corpusSha256, @NonNull String summary) {
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
    out.append("  \"harness\": \"host\",\n");
    out.append("  \"engine_mode\": \"neural\",\n");
    out.append("  \"neural_model_id\": ").append(jsonString(modelId)).append(",\n");
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
    final InputStream input =
        NeuralNextWordQualityEvalHostTest.class.getResourceAsStream("/eval/english_sentences.txt");
    if (input == null) return null;
    try (InputStream in = input) {
      return sha256Hex(in);
    } catch (IOException ignored) {
      return null;
    }
  }

  @Nullable
  private static String sha256Hex(@NonNull InputStream input) throws IOException {
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

  private void writeMetricsReport(@NonNull String summary, @NonNull String json) {
    final File reportsDir = new File("build/reports/nextword-quality");
    if (!reportsDir.exists() && !reportsDir.mkdirs()) {
      return;
    }
    final long timestampMs = System.currentTimeMillis();
    final File outTextFile = new File(reportsDir, "neural-quality-" + timestampMs + ".txt");
    final File outJsonFile = new File(reportsDir, "neural-quality-" + timestampMs + ".json");
    try (FileOutputStream output = new FileOutputStream(outTextFile)) {
      output.write(summary.getBytes(StandardCharsets.UTF_8));
    } catch (IOException ignored) {
      // best effort
    }
    try (FileOutputStream output = new FileOutputStream(outJsonFile)) {
      output.write(json.getBytes(StandardCharsets.UTF_8));
    } catch (IOException ignored) {
      // best effort
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
    final File baseDir = new File(System.getProperty("java.io.tmpdir"), "nsk-neural-quality-model");
    final File modelDir = new File(baseDir, DEFAULT_TEST_MODEL_ID);
    if (isModelDirectoryUsable(modelDir)) {
      return modelDir;
    }

    try {
      downloadAndExtractZipToDirectory(DEFAULT_TEST_MODEL_BUNDLE_URL, modelDir);
    } catch (Exception e) {
      // This is a best-effort helper; if we can't fetch, we skip the eval test.
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
      byte[] buffer = new byte[8 * 1024];
      while ((entry = zipInputStream.getNextEntry()) != null) {
        if (entry.isDirectory()) continue;

        final String entryName = entry.getName();
        if (entryName == null || entryName.trim().isEmpty()) continue;
        final String leafName;
        final int lastSlash = Math.max(entryName.lastIndexOf('/'), entryName.lastIndexOf('\\'));
        if (lastSlash >= 0) {
          leafName = entryName.substring(lastSlash + 1);
        } else {
          leafName = entryName;
        }
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

  private boolean isOnnxRuntimeAvailable() {
    try {
      System.loadLibrary("onnxruntime");
      return true;
    } catch (UnsatisfiedLinkError e) {
      return false;
    }
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
        @NonNull String modelId, int usedCases, @NonNull ChainMetrics chainMetrics) {
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
          + "Neural next-word quality metrics (best-effort)\n"
          + "Model: "
          + modelId
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

    private static long percentile(@NonNull List<Long> sorted, int percentile) {
      if (sorted.isEmpty()) return 0L;
      final double rank = percentile / 100.0 * sorted.size();
      final int index = Math.max(0, Math.min(sorted.size() - 1, (int) Math.ceil(rank) - 1));
      return sorted.get(index);
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
