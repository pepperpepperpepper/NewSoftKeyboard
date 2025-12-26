package wtf.uhoh.newsoftkeyboard.app.dictionaries.presage;

import static org.junit.Assert.*;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.engine.EngineType;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelDefinition;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelDownloader;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelStore;
import wtf.uhoh.newsoftkeyboard.engine.neural.NeuralPredictionManager;

@RunWith(AndroidJUnit4.class)
public class MixedcaseInstallTest {

  @Test
  public void testInstallAndActivateMixedcaseModel() throws Exception {
    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    final ModelStore store = new ModelStore(context);

    // Build a direct CatalogEntry for the mixedcase bundle to bypass any CDN caching.
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
    final ModelDefinition def = DownloaderCompat.run(downloader, target);
    assertEquals("distilgpt2_mixedcase_sanity", def.getId());

    store.persistSelectedModelId(EngineType.NEURAL, def.getId());

    final NeuralPredictionManager manager = new NeuralPredictionManager(context);
    assertTrue("Neural predictor failed to activate", manager.activate());
    final List<String> predictions = manager.predictNextWords(new String[] {"Hello", "how"}, 5);
    assertFalse("Expected predictions from mixedcase model", predictions.isEmpty());
    manager.deactivate();
  }
}
