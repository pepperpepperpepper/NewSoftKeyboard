package wtf.uhoh.newsoftkeyboard.nextword.prediction;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class NextWordContextTokenizerTest {

  @Test
  public void tokenizeTextBeforeCursor_basicWords() {
    Assert.assertEquals(
        List.of("alpha", "beta", "gamma"),
        NextWordContextTokenizer.tokenizeTextBeforeCursor("alpha beta gamma", 10));
  }

  @Test
  public void tokenizeTextBeforeCursor_trimsWhitespaceAndLimits() {
    Assert.assertEquals(
        List.of("beta", "gamma"),
        NextWordContextTokenizer.tokenizeTextBeforeCursor("  alpha   beta  gamma  ", 2));
  }

  @Test
  public void tokenizeTextBeforeCursor_stopsAtSentenceBoundary() {
    Assert.assertEquals(
        List.of(), NextWordContextTokenizer.tokenizeTextBeforeCursor("hello world.", 10));
    Assert.assertEquals(
        List.of(), NextWordContextTokenizer.tokenizeTextBeforeCursor("hello world.  ", 10));
  }

  @Test
  public void tokenizeTextBeforeCursor_usesTokensAfterLastSentenceBoundary() {
    Assert.assertEquals(
        List.of("again", "here"),
        NextWordContextTokenizer.tokenizeTextBeforeCursor("hello world. again here", 10));
  }

  @Test
  public void tokenizeTextBeforeCursor_preservesDomainLikeTokens() {
    Assert.assertEquals(
        List.of("visit", "example.com"),
        NextWordContextTokenizer.tokenizeTextBeforeCursor("visit example.com", 10));
  }
}
