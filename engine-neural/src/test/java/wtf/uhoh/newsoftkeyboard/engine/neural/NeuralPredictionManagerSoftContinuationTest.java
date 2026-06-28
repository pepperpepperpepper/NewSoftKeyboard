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
public class NeuralPredictionManagerSoftContinuationTest {

  @Test
  public void softContinuesShortNonCommonPrefixWhenBestTokenStartsWhitespace() throws Exception {
    final Gpt2Tokenizer tokenizer = createTokenizer();

    // 0: " the" (word boundary)
    // 1: "puter" (continuation segment)
    final float[] logits = new float[] {0.9f, 0.85f};

    final int nextId =
        NeuralPredictionManager.chooseBestWordContinuationTokenId(logits, 64, tokenizer, "com");
    assertEquals(1, nextId);
  }

  @Test
  public void softContinuesFourLetterNonCommonPrefixWhenBestTokenStartsWhitespace()
      throws Exception {
    final Gpt2Tokenizer tokenizer = createTokenizer();

    final float[] logits = new float[] {0.9f, 0.85f};

    final int nextId =
        NeuralPredictionManager.chooseBestWordContinuationTokenId(logits, 64, tokenizer, "pizz");
    assertEquals(1, nextId);
  }

  @Test
  public void doesNotSoftContinueForCommonShortWordWhenBestTokenStartsWhitespace()
      throws Exception {
    final Gpt2Tokenizer tokenizer = createTokenizer();

    final float[] logits = new float[] {0.9f, 0.85f};

    final int nextId =
        NeuralPredictionManager.chooseBestWordContinuationTokenId(logits, 64, tokenizer, "of");
    assertEquals(-1, nextId);
  }

  @Test
  public void doesNotSoftContinueWhenContinuationTooFarFromBest() throws Exception {
    final Gpt2Tokenizer tokenizer = createTokenizer();

    final float[] logits = new float[] {0.9f, 0.0f};

    final int nextId =
        NeuralPredictionManager.chooseBestWordContinuationTokenId(logits, 64, tokenizer, "com");
    assertEquals(-1, nextId);
  }

  @Test
  public void topWordContinuationReturnsUpToWidthContinuationsAndReducesToGreedyAtWidthOne()
      throws Exception {
    final Gpt2Tokenizer tokenizer = createMultiTokenizer();
    // 0: "ing" (continuation), 1: "ed" (continuation), 2: " the" (boundary)
    final float[] logits = new float[] {0.9f, 0.85f, 0.1f};

    // Width 2 keeps both close continuations, in logit order.
    assertEquals(
        java.util.Arrays.asList(0, 1),
        NeuralPredictionManager.topWordContinuationTokenIds(logits, 64, tokenizer, "runn", 2));

    // Width 1 reproduces the single-best greedy choice.
    assertEquals(
        java.util.Collections.singletonList(0),
        NeuralPredictionManager.topWordContinuationTokenIds(logits, 64, tokenizer, "runn", 1));
    assertEquals(
        0, NeuralPredictionManager.chooseBestWordContinuationTokenId(logits, 64, tokenizer, "runn"));
  }

  @Test
  public void topWordContinuationStopsAtBoundaryAndPrunesFarContinuations() throws Exception {
    final Gpt2Tokenizer tokenizer = createMultiTokenizer();

    // Best token is the boundary " the"; a common short word is not nudged onward -> no continuation.
    final float[] boundaryBest = new float[] {0.1f, 0.05f, 0.9f};
    assertEquals(
        java.util.Collections.emptyList(),
        NeuralPredictionManager.topWordContinuationTokenIds(boundaryBest, 64, tokenizer, "of", 2));

    // Best is a continuation, but the second continuation is far below it -> pruned by the delta.
    // Use a 2-letter prefix so the narrower (0.75) continuation-delta budget applies.
    final float[] farSecond = new float[] {0.9f, 0.0f, 0.1f};
    assertEquals(
        java.util.Collections.singletonList(0),
        NeuralPredictionManager.topWordContinuationTokenIds(farSecond, 64, tokenizer, "ru", 2));
  }

  private static Gpt2Tokenizer createMultiTokenizer() throws Exception {
    final File vocab = File.createTempFile("nsk-tokenizer-multi", ".vocab.json");
    final File merges = File.createTempFile("nsk-tokenizer-multi", ".merges.txt");
    vocab.deleteOnExit();
    merges.deleteOnExit();

    final String json = "{" + "\"ing\":0," + "\"ed\":1," + "\"Ġthe\":2" + "}";
    try (FileOutputStream out = new FileOutputStream(vocab)) {
      out.write(json.getBytes(StandardCharsets.UTF_8));
    }
    try (FileOutputStream out = new FileOutputStream(merges)) {
      out.write("#version: 0.2\n".getBytes(StandardCharsets.UTF_8));
    }
    return new Gpt2Tokenizer(vocab, merges);
  }

  private static Gpt2Tokenizer createTokenizer() throws Exception {
    final File vocab = File.createTempFile("nsk-tokenizer", ".vocab.json");
    final File merges = File.createTempFile("nsk-tokenizer", ".merges.txt");
    vocab.deleteOnExit();
    merges.deleteOnExit();

    final String json = "{" + "\"\u0120the\":0," + "\"puter\":1" + "}";
    try (FileOutputStream out = new FileOutputStream(vocab)) {
      out.write(json.getBytes(StandardCharsets.UTF_8));
    }
    try (FileOutputStream out = new FileOutputStream(merges)) {
      out.write("#version: 0.2\n".getBytes(StandardCharsets.UTF_8));
    }

    return new Gpt2Tokenizer(vocab, merges);
  }
}
