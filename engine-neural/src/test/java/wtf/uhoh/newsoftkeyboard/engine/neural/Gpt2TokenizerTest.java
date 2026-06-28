package wtf.uhoh.newsoftkeyboard.engine.neural;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.List;
import org.junit.Test;

public final class Gpt2TokenizerTest {

  private File resourceFile(String name) throws Exception {
    final URL url = getClass().getClassLoader().getResource(name);
    if (url == null) throw new IllegalStateException("Missing test resource " + name);
    return new File(url.toURI());
  }

  @Test
  public void encodeAndDecodeMatchDistilGpt2() throws Exception {
    final File vocab = resourceFile("tokenizer/distilgpt2/vocab.json");
    final File merges = resourceFile("tokenizer/distilgpt2/merges.txt");
    final Gpt2Tokenizer tokenizer = new Gpt2Tokenizer(vocab, merges);

    assertEquals(50257, tokenizer.getVocabSize());
    assertEquals(" the", tokenizer.decodeId(262));
    assertEquals(" cat", tokenizer.decodeId(3797));

    final int[] ids = tokenizer.encode(" the cat");
    assertArrayEquals(new int[] {262, 3797}, ids);
  }

  @Test
  public void tokenIdsForWordPrefixReturnsOnlyCompatibleWordStartTokens() throws Exception {
    final File vocab = resourceFile("tokenizer/distilgpt2/vocab.json");
    final File merges = resourceFile("tokenizer/distilgpt2/merges.txt");
    final Gpt2Tokenizer tokenizer = new Gpt2Tokenizer(vocab, merges);

    final List<Integer> recIds = tokenizer.tokenIdsForWordPrefix("rec", 0);
    // " rec" is itself a word-start token in the GPT-2 vocab, so the prefix must yield candidates.
    assertFalse(recIds.isEmpty());
    for (int id : recIds) {
      final String surface = tokenizer.decodeId(id);
      // Every candidate is a word-start token (leading space) whose surface extends the prefix.
      assertEquals(" ", surface.substring(0, 1));
      assertTrue(
          "Unexpected candidate '" + surface + "'",
          surface.trim().toLowerCase().startsWith("rec"));
    }

    // Case-insensitive: an uppercase typed prefix still matches lowercase word-start tokens.
    assertFalse(tokenizer.tokenIdsForWordPrefix("REC", 0).isEmpty());

    // Cap is honored.
    assertTrue(tokenizer.tokenIdsForWordPrefix("a", 3).size() <= 3);

    // Empty / whitespace prefixes yield nothing.
    assertTrue(tokenizer.tokenIdsForWordPrefix("", 0).isEmpty());
    assertTrue(tokenizer.tokenIdsForWordPrefix("   ", 0).isEmpty());
  }

  @Test
  public void selectTopTokensOrdersByLogitValue() {
    final float[] logits = new float[] {-1f, 0.1f, 3f, 2f};
    final List<String> top = NeuralPredictionManager.selectTopTokens(logits, 2, null);
    assertEquals(2, top.size());
    assertEquals("2", top.get(0)); // highest logit
    assertEquals("3", top.get(1)); // second highest
  }
}
