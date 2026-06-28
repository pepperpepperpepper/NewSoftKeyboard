package wtf.uhoh.newsoftkeyboard.engine.neural;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.engine.EngineType;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelDefinition;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelStore;

/**
 * On-device (Path B) benchmark for the real per-keystroke prefix-completion latency — the metric the
 * host eval cannot represent, since the host eval recomputes the context KV every call on desktop
 * CPU. Here, real {@code onnxruntime-android} runs on the device (XNNPACK from #3), and we measure
 * the warm path: the context KV is built once when a word begins, then reused for each subsequent
 * keystroke (#2). We report the cold (first-keystroke) cost separately from the warm
 * (subsequent-keystroke) distribution, and assert the KV-reuse invariant — one context forward pass
 * per word regardless of how many keystrokes it takes.
 *
 * <p>Not a normal unit test. {@code connectedDebugAndroidTest} uninstalls the APK afterward (and the
 * shared orchestrator clears app data between tests), which wipes a staged model — so drive it
 * directly with {@code am instrument}, which leaves storage intact:
 *
 * <pre>
 *   ./gradlew :engine-neural:installDebugAndroidTest
 *   M=/sdcard/Android/data/wtf.uhoh.newsoftkeyboard.engine.neural.test/files/neural-model
 *   adb shell mkdir -p "$M"
 *   adb push engine-neural/build/neural_test_model/{model_int8.onnx,vocab.json,merges.txt} "$M"/
 *   adb shell chmod 777 "$M"          # let the app's uid traverse the shell-created dir
 *   adb shell am instrument -w -r \
 *     -e class wtf.uhoh.newsoftkeyboard.engine.neural.NeuralPrefixCompletionLatencyInstrumentationTest \
 *     wtf.uhoh.newsoftkeyboard.engine.neural.test/androidx.test.runner.AndroidJUnitRunner
 *   adb logcat -d | grep -A5 'On-device prefix-completion latency'
 * </pre>
 *
 * Model resolution order: instrumentation arg {@code neuralModelDir}; then the app's external files
 * dir ({@code .../neural-model}, the recipe above); then a one-time on-device download to the cache
 * dir. Skips (assumption) when no model can be resolved.
 */
@RunWith(AndroidJUnit4.class)
public class NeuralPrefixCompletionLatencyInstrumentationTest {

  private static final String MODEL_BUNDLE_URL =
      "https://fdroid.uh-oh.wtf/models/distilgpt2_mixedcase_sanity_v1.zip";
  private static final int MAX_RESULTS = 8;

  // A few realistic (context, word-being-typed) cases. Context is the committed words; we type the
  // target word keystroke by keystroke (prefix lengths 2..len-1) against a stable context.
  private static final String[][] CONTEXTS = {
    {"i", "would", "like", "to"},
    {"please", "send", "me", "the"},
    {"the", "weather", "today", "is"},
    {"we", "should", "schedule", "the"},
  };
  private static final String[] TARGET_WORDS = {
    "recommendation", "information", "available", "appointment",
  };

  @Test
  public void warmPerKeystrokeLatencyAndKvReuse() throws Exception {
    final File modelDir = resolveModelDirectory();
    assumeTrue(
        "No model on device. Push one to the app external files dir (.../neural-model) or pass "
            + "-Pandroid.testInstrumentationRunnerArguments.neuralModelDir=...",
        modelDir != null && isModelDirectoryUsable(modelDir));

    final Context context = ApplicationProvider.getApplicationContext();
    final NeuralPredictionManager manager = new NeuralPredictionManager(context, fakeStore(modelDir));
    assumeTrue("Activation failed: " + manager.getLastActivationError(), manager.activate());

    final String provider = manager.getLastSessionProviderForTest();

    final List<Double> coldMs = new ArrayList<>();
    final List<Double> warmMs = new ArrayList<>();

    for (int c = 0; c < TARGET_WORDS.length; c++) {
      final String[] ctx = CONTEXTS[c];
      final String word = TARGET_WORDS[c];

      // Start the word cold: drop any retained context KV so the first keystroke pays the build.
      manager.invalidateContextCache();
      final int beforePasses = manager.getContextForwardPassCountForTest();

      for (int prefixLen = 2; prefixLen < word.length(); prefixLen++) {
        final String prefix = word.substring(0, prefixLen);
        final long t0 = System.nanoTime();
        manager.completeWordWithScoringContext(ctx, prefix, MAX_RESULTS);
        final double ms = (System.nanoTime() - t0) / 1_000_000.0;
        if (prefixLen == 2) {
          coldMs.add(ms); // first keystroke of the word builds the context KV
        } else {
          warmMs.add(ms); // subsequent keystrokes reuse it
        }
      }

      final int passes = manager.getContextForwardPassCountForTest() - beforePasses;
      // KV reuse (#2): the whole word — every keystroke — costs exactly one context forward pass.
      assertEquals(
          "Word '" + word + "' should trigger exactly one context forward pass", 1, passes);
    }

    manager.deactivate();

    final double[] cold = stats(coldMs);
    final double[] warm = stats(warmMs);
    final StringBuilder sb = new StringBuilder();
    sb.append("On-device prefix-completion latency\n");
    sb.append("provider: ").append(provider).append('\n');
    sb.append("words: ").append(TARGET_WORDS.length).append('\n');
    sb.append(String.format(Locale.US, "cold_keystroke_ms: mean=%.1f p50=%.1f p95=%.1f%n",
        cold[0], cold[1], cold[2]));
    sb.append(String.format(Locale.US, "warm_keystroke_ms: mean=%.1f p50=%.1f p95=%.1f (n=%d)",
        warm[0], warm[1], warm[2], warmMs.size()));
    System.out.println(sb);

    assertTrue("Expected at least one warm keystroke sample", !warmMs.isEmpty());
    assertTrue(
        "Provider must be a known execution provider", "xnnpack".equals(provider) || "cpu".equals(provider));
  }

  @NonNull
  private static double[] stats(@NonNull List<Double> values) {
    if (values.isEmpty()) return new double[] {0, 0, 0};
    final List<Double> sorted = new ArrayList<>(values);
    Collections.sort(sorted);
    double sum = 0;
    for (double d : sorted) sum += d;
    final double mean = sum / sorted.size();
    final double p50 = sorted.get((int) (sorted.size() * 0.50));
    final double p95 = sorted.get(Math.min(sorted.size() - 1, (int) (sorted.size() * 0.95)));
    return new double[] {mean, p50, p95};
  }

  @NonNull
  private ModelStore fakeStore(@NonNull File modelDir) {
    final File onnx = new File(modelDir, "model_int8.onnx");
    final File vocab = new File(modelDir, "vocab.json");
    final File merges = new File(modelDir, "merges.txt");

    final ModelDefinition definition =
        ModelDefinition.builder(modelDir.getName())
            .setLabel("device-test-model")
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

  @Nullable
  private File resolveModelDirectory() {
    final Bundle args = InstrumentationRegistry.getArguments();
    final String argPath = args == null ? null : args.getString("neuralModelDir");
    if (argPath != null && !argPath.trim().isEmpty()) {
      final File dir = new File(argPath.trim());
      if (isModelDirectoryUsable(dir)) return dir;
    }

    final Context context = ApplicationProvider.getApplicationContext();
    final File external = context.getExternalFilesDir(null);
    if (external != null) {
      final File dir = new File(external, "neural-model");
      if (isModelDirectoryUsable(dir)) return dir;
    }

    final File cacheDir = new File(context.getCacheDir(), "neural-model");
    if (isModelDirectoryUsable(cacheDir)) return cacheDir;
    try {
      downloadAndExtractZipToDirectory(MODEL_BUNDLE_URL, cacheDir);
    } catch (Exception e) {
      return null;
    }
    return isModelDirectoryUsable(cacheDir) ? cacheDir : null;
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
