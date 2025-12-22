package com.anysoftkeyboard.dictionaries;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.neural.NeuralPredictionManager;
import com.anysoftkeyboard.dictionaries.presage.DownloaderCompat;
import com.anysoftkeyboard.dictionaries.presage.PresageModelCatalog;
import com.anysoftkeyboard.dictionaries.presage.PresageModelCatalog.CatalogEntry;
import com.anysoftkeyboard.engine.models.ModelDownloader;
import com.anysoftkeyboard.engine.models.ModelStore;
import java.util.Arrays;
import java.util.List;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.engine.EngineType;

@RunWith(AndroidJUnit4.class)
public class NeuralPredictionManagerTest {

  @Test
  public void testNeuralPredictions() throws Exception {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final ModelStore store = new ModelStore(context);
    ensureNeuralModelInstalled(context, store);

    final NeuralPredictionManager manager = new NeuralPredictionManager(context);
    assertTrue("Neural predictor failed to activate", manager.activate());

    final List<String> predictions = manager.predictNextWords(new String[] {"Hello", "how"}, 5);
    assertFalse("Expected neural predictions", predictions.isEmpty());
    manager.deactivate();
  }

  @Test
  public void testTinyLlamaPredictions() throws Exception {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final ModelStore store = new ModelStore(context);
    store.persistSelectedModelId(EngineType.NEURAL, "tinyllama_transformer_q4f16");
    store.removeModel("tinyllama_transformer_q4f16");
    ensureNeuralModelInstalled(context, store);

    final NeuralPredictionManager manager = new NeuralPredictionManager(context);
    assertTrue("Neural predictor failed to activate", manager.activate());

    final List<String> predictions = manager.predictNextWords(new String[] {"Hello", "how"}, 5);
    assertFalse("Expected neural predictions", predictions.isEmpty());
    manager.deactivate();
  }

  @Test
  public void testMixedcaseDistilGpt2Predictions() throws Exception {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final ModelStore store = new ModelStore(context);
    // Force-select our mixedcase sanity bundle so downloader/installer targets it.
    store.persistSelectedModelId(EngineType.NEURAL, "distilgpt2_mixedcase_sanity");
    ensureNeuralModelInstalled(context, store);

    final NeuralPredictionManager manager = new NeuralPredictionManager(context);
    assertTrue("Neural predictor failed to activate", manager.activate());

    final java.util.List<String> predictions =
        manager.predictNextWords(new String[] {"Hello", "how"}, 5);
    assertFalse("Expected neural predictions from mixedcase model", predictions.isEmpty());
    manager.deactivate();
  }

  @Test
  public void testNeuralLatencyAndAccuracyMetrics() throws Exception {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final ModelStore store = new ModelStore(context);
    ensureNeuralModelInstalled(context, store);

    final NeuralPredictionManager manager = new NeuralPredictionManager(context);
    assertTrue("Neural predictor failed to activate", manager.activate());

    final String[][] contexts =
        new String[][] {
          {"Hello", "how"},
          {"I", "am"},
          {"The", "quick"},
          {"We", "are"},
          {"It", "is"}
        };
    final String[] expected = new String[] {"are", "going", "brown", "going", "a"};

    long totalLatency = 0L;
    int hitCount = 0;
    for (int index = 0; index < contexts.length; index++) {
      final String[] sampleContext = contexts[index];
      final long start = SystemClock.elapsedRealtime();
      final List<String> predictions = manager.predictNextWords(sampleContext, 5);
      final long latency = SystemClock.elapsedRealtime() - start;
      totalLatency += latency;
      final String expectedWord = expected[index];
      if (predictions != null) {
        for (String candidate : predictions) {
          if (candidate != null && candidate.equalsIgnoreCase(expectedWord)) {
            hitCount++;
            break;
          }
        }
      }
      Logger.i(
          "NeuralPredictionManagerTest",
          "Context "
              + Arrays.toString(sampleContext)
              + " latency="
              + latency
              + "ms predictions="
              + predictions);
    }

    final float averageLatency = totalLatency / (float) contexts.length;
    Logger.i(
        "NeuralPredictionManagerTest",
        "Average neural latency="
            + averageLatency
            + "ms, expected-hit-rate="
            + hitCount
            + "/"
            + contexts.length);
    manager.deactivate();
    // Genymotion/emulators have highly variable performance; keep this as an informative metric
    // and enforce the threshold only on real devices.
    Assume.assumeTrue("Skipping performance assertion on emulator", !isEmulator());
    assertTrue("Neural inference too slow: " + averageLatency + "ms", averageLatency < 50f);
  }

  private static boolean isEmulator() {
    final String fingerprint = Build.FINGERPRINT == null ? "" : Build.FINGERPRINT;
    final String hardware = Build.HARDWARE == null ? "" : Build.HARDWARE;
    return fingerprint.contains("cloud_arm")
        || fingerprint.contains("generic")
        || hardware.startsWith("vbox");
  }

  private void ensureNeuralModelInstalled(Context context, ModelStore store) throws Exception {
    final String preferredId = store.getSelectedModelId(EngineType.NEURAL);
    android.util.Log.i("NeuralPredictionManagerTest", "Preferred model id: " + preferredId);
    store.removeModel("distilgpt2_transformer_v1");
    store.removeModel("distilgpt2_transformer_v2");
    final ModelStore.ActiveModel existing = store.ensureActiveModel(EngineType.NEURAL);
    if (existing != null) {
      return;
    }

    final PresageModelCatalog catalog = new PresageModelCatalog(context);
    final List<CatalogEntry> entries = catalog.fetchCatalog();
    assertFalse("No models found in catalog", entries.isEmpty());

    CatalogEntry targetEntry = null;
    CatalogEntry fallbackEntry = null;
    for (CatalogEntry entry : entries) {
      android.util.Log.i(
          "NeuralPredictionManagerTest", "Catalog entry id=" + entry.getDefinition().getId());
      if (preferredId != null && preferredId.equals(entry.getDefinition().getId())) {
        targetEntry = entry;
        break;
      }
      if (fallbackEntry == null && entry.getDefinition().getEngineType() == EngineType.NEURAL) {
        fallbackEntry = entry;
      }
    }
    if (targetEntry == null) {
      targetEntry = fallbackEntry;
    }
    if (targetEntry == null) {
      throw new AssertionError("Catalog does not contain a neural language model entry");
    }

    final ModelDownloader downloader = new ModelDownloader(context, store);
    DownloaderCompat.run(downloader, targetEntry);
  }
}
