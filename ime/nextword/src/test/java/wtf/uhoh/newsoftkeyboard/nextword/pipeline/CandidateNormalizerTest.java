package wtf.uhoh.newsoftkeyboard.nextword.pipeline;

import java.util.ArrayDeque;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class CandidateNormalizerTest {

  @Test
  public void normalizeForNextWordUx_filtersDomainLikeTokensInNormalProse() {
    final ArrayDeque<String> context = new ArrayDeque<>();
    context.add("keep");
    context.add("me");

    final List<String> out =
        CandidateNormalizer.normalizeForNextWordUx(
            context, List.of("com", "informed", "www", "http", "https", "google.com"));

    Assert.assertEquals(List.of("informed"), out);
  }

  @Test
  public void normalizeForNextWordUx_filtersDomainLikeTokens_caseInsensitivelyAndTrims() {
    final ArrayDeque<String> context = new ArrayDeque<>();
    context.add("keep");
    context.add("me");

    final List<String> out = CandidateNormalizer.normalizeForNextWordUx(context, List.of(" COM "));

    Assert.assertTrue(out.isEmpty());
  }

  @Test
  public void normalizeForNextWordUx_allowsDomainLikeTokensInDomainContext() {
    final ArrayDeque<String> context = new ArrayDeque<>();
    context.add("example.com");

    final List<String> out =
        CandidateNormalizer.normalizeForNextWordUx(
            context, List.of("com", "www", "cat", "gmail.com"));

    Assert.assertEquals(List.of("com", "www", "cat", "gmail.com"), out);
  }
}
