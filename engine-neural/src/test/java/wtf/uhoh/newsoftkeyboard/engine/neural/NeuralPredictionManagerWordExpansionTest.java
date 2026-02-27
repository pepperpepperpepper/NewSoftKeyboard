package wtf.uhoh.newsoftkeyboard.engine.neural;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class NeuralPredictionManagerWordExpansionTest {

  @Test
  public void doesNotForceWordContinuationWhenBestTokenStartsNewWord() throws Exception {
    final Gpt2Tokenizer tokenizer = createTokenizer();

    // 0: " the" (word boundary)
    // 1: "the" (continuation segment)
    final float[] logits = new float[] {0.9f, 0.8f};

    final int nextId =
        NeuralPredictionManager.chooseBestWordContinuationTokenId(logits, 64, tokenizer);
    assertEquals(-1, nextId);
  }

  @Test
  public void continuesWordWhenBestTokenIsContinuationSegment() throws Exception {
    final Gpt2Tokenizer tokenizer = createTokenizer();

    final float[] logits = new float[] {0.8f, 0.9f};

    final int nextId =
        NeuralPredictionManager.chooseBestWordContinuationTokenId(logits, 64, tokenizer);
    assertEquals(1, nextId);
  }

  private static Gpt2Tokenizer createTokenizer() throws Exception {
    final File vocab = File.createTempFile("nsk-tokenizer", ".vocab.json");
    final File merges = File.createTempFile("nsk-tokenizer", ".merges.txt");
    vocab.deleteOnExit();
    merges.deleteOnExit();

    final String json = "{" + "\"\u0120the\":0," + "\"the\":1" + "}";
    try (FileOutputStream out = new FileOutputStream(vocab)) {
      out.write(json.getBytes(StandardCharsets.UTF_8));
    }
    try (FileOutputStream out = new FileOutputStream(merges)) {
      out.write("#version: 0.2\n".getBytes(StandardCharsets.UTF_8));
    }

    return new Gpt2Tokenizer(vocab, merges);
  }
}
