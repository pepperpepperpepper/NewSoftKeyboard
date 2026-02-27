package wtf.uhoh.newsoftkeyboard.engine.neural;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class NeuralPredictionManagerTokenFilteringTest {

  @Test
  public void selectTopTokensTrimsAndFiltersNonWords() throws Exception {
    final Gpt2Tokenizer tokenizer = createTokenizer();

    final float[] logits = new float[4];
    // 0: " hello" (via Ġ marker)
    logits[0] = 0.80f;
    // 1: "." (punctuation-only, should be filtered)
    logits[1] = 0.99f;
    // 2: "123" (digits, should be filtered)
    logits[2] = 0.95f;
    // 3: "can't" (word, should remain)
    logits[3] = 0.85f;

    final List<String> out = NeuralPredictionManager.selectTopTokens(logits, 2, tokenizer);

    assertEquals(List.of("can't", "hello"), out);
    for (String token : out) {
      assertTrue(
          "Expected word-like token, got: '" + token + "'", token.matches("[A-Za-z'.]{1,24}"));
      assertTrue("Expected trimmed token, got: '" + token + "'", token.equals(token.trim()));
    }
  }

  @Test
  public void dropShortPrefixCandidatesDropsLikelySubwordFragments() throws Exception {
    final Method method =
        NeuralPredictionManager.class.getDeclaredMethod("dropShortPrefixCandidates", List.class);
    method.setAccessible(true);

    @SuppressWarnings("unchecked")
    final List<String> out = (List<String>) method.invoke(null, List.of("pizz", "pizza", "and"));

    assertEquals(List.of("pizza", "and"), out);
  }

  @Test
  public void filterCandidatesForKeyboardUx_filtersDomainLikeTokensInNormalProse()
      throws Exception {
    final Method method =
        NeuralPredictionManager.class.getDeclaredMethod(
            "filterCandidatesForKeyboardUx", String[].class, List.class);
    method.setAccessible(true);

    @SuppressWarnings("unchecked")
    final List<String> out =
        (List<String>) method.invoke(null, new String[] {"keep", "me"}, List.of("com"));

    assertTrue(out.isEmpty());
  }

  @Test
  public void filterCandidatesForKeyboardUx_allowsDomainLikeTokensInDomainContext()
      throws Exception {
    final Method method =
        NeuralPredictionManager.class.getDeclaredMethod(
            "filterCandidatesForKeyboardUx", String[].class, List.class);
    method.setAccessible(true);

    @SuppressWarnings("unchecked")
    final List<String> out =
        (List<String>) method.invoke(null, new String[] {"example.com"}, List.of("com"));

    assertEquals(List.of("com"), out);
  }

  private static Gpt2Tokenizer createTokenizer() throws Exception {
    final File vocab = File.createTempFile("nsk-tokenizer", ".vocab.json");
    final File merges = File.createTempFile("nsk-tokenizer", ".merges.txt");
    vocab.deleteOnExit();
    merges.deleteOnExit();

    final String json =
        "{" + "\"" + "\u0120hello" + "\":0," + "\".\":1," + "\"123\":2," + "\"can't\":3" + "}";
    try (FileOutputStream out = new FileOutputStream(vocab)) {
      out.write(json.getBytes(StandardCharsets.UTF_8));
    }
    try (FileOutputStream out = new FileOutputStream(merges)) {
      out.write("#version: 0.2\n".getBytes(StandardCharsets.UTF_8));
    }

    return new Gpt2Tokenizer(vocab, merges);
  }
}
