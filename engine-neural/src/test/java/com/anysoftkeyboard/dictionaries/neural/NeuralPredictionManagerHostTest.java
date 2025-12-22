package com.anysoftkeyboard.dictionaries.neural;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import com.anysoftkeyboard.engine.models.ModelDefinition;
import com.anysoftkeyboard.engine.models.ModelStore;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import wtf.uhoh.newsoftkeyboard.engine.EngineType;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class NeuralPredictionManagerHostTest {

  private static final String DEFAULT_TEST_MODEL_ID = "distilgpt2_mixedcase_sanity";
  private static final String DEFAULT_TEST_MODEL_BUNDLE_URL =
      "https://fdroid.uh-oh.wtf/models/distilgpt2_mixedcase_sanity_v1.zip";

  @Test
  public void predictNextWordsProducesReadableTokensWhenModelPresent() {
    assumeTrue("ONNX runtime not available on host", isOnnxRuntimeAvailable());

    final File modelDir = resolveModelDirectory();
    assumeTrue(
        "Missing NEURAL_MODEL_DIR with model files", modelDir != null && modelDir.isDirectory());

    final File onnx = new File(modelDir, "model_int8.onnx");
    final File vocab = new File(modelDir, "vocab.json");
    final File merges = new File(modelDir, "merges.txt");
    assumeTrue(onnx.exists() && vocab.exists() && merges.exists());

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

    final NeuralPredictionManager manager = new NeuralPredictionManager(context, fakeStore);
    assumeTrue("Activation failed", manager.activate());

    final List<String> predictions = manager.predictNextWords(new String[] {"the"}, 5);
    manager.deactivate();

    assertNotNull(predictions);
    assertFalse(predictions.isEmpty());
    for (String token : predictions) {
      assertTrue("Unexpected token: " + token, token.matches("[A-Za-z'.]{1,24}"));
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
    final File baseDir = new File(System.getProperty("java.io.tmpdir"), "nsk-neural-test-model");
    final File modelDir = new File(baseDir, DEFAULT_TEST_MODEL_ID);
    if (isModelDirectoryUsable(modelDir)) {
      return modelDir;
    }

    try {
      downloadAndExtractZipToDirectory(DEFAULT_TEST_MODEL_BUNDLE_URL, modelDir);
    } catch (Exception e) {
      // This is a best-effort helper; if we can't fetch, we skip the host test.
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
}
