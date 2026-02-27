package wtf.uhoh.newsoftkeyboard.engine.neural;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class NeuralPredictionManagerWordCandidateSearchWindowTest {

  @Test
  public void extractsWordStartTokenEvenWhenNotInTopSmallWindow() throws Exception {
    final int vocabSize = 300;
    final int wordStartTokenId = 200;
    final Gpt2Tokenizer tokenizer = createTokenizer(vocabSize, wordStartTokenId);

    final float[] logits = new float[vocabSize];
    // Make the top logits dominated by non-word-start tokens.
    for (int i = 0; i < 20; i++) {
      logits[i] = 1.0f - (i * 0.001f);
    }
    // Place a word-start token outside the "old" small window, but within the new search window.
    logits[wordStartTokenId] = 0.5f;

    final List<String> out =
        NeuralPredictionManager.extractTopWordsFromLogitsForTests(logits, 1, tokenizer);
    assertEquals(List.of(tokenForId(wordStartTokenId)), out);
  }

  private static Gpt2Tokenizer createTokenizer(int vocabSize, int wordStartTokenId)
      throws Exception {
    final File vocab = File.createTempFile("nsk-tokenizer", ".vocab.json");
    final File merges = File.createTempFile("nsk-tokenizer", ".merges.txt");
    vocab.deleteOnExit();
    merges.deleteOnExit();

    final StringBuilder json = new StringBuilder();
    json.append('{');
    for (int i = 0; i < vocabSize; i++) {
      if (i > 0) json.append(',');
      final String token = (i == wordStartTokenId) ? ("\u0120" + tokenForId(i)) : tokenForId(i);
      json.append('"').append(token).append('"').append(':').append(i);
    }
    json.append('}');

    try (FileOutputStream out = new FileOutputStream(vocab)) {
      out.write(json.toString().getBytes(StandardCharsets.UTF_8));
    }
    try (FileOutputStream out = new FileOutputStream(merges)) {
      out.write("#version: 0.2\n".getBytes(StandardCharsets.UTF_8));
    }

    return new Gpt2Tokenizer(vocab, merges);
  }

  private static String tokenForId(int id) {
    final StringBuilder out = new StringBuilder();
    int n = id;
    do {
      out.append((char) ('a' + (n % 26)));
      n /= 26;
    } while (n > 0);
    return "t" + out;
  }
}
