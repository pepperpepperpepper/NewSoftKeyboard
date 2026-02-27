package wtf.uhoh.newsoftkeyboard.app.ime.context;

import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class TypedRulesApplierTest {

  @Test
  public void testAutoApplyReplacesSuffixAndKeepsWhitespaceTail() {
    final var rules =
        Collections.singletonList(
            new ContextProfilesStore.TypedRule("how are you", "hru?", true, true));

    final TypedRulesApplier.Result result = TypedRulesApplier.resolve("How are you ", rules, true);
    Assert.assertNotNull(result.autoReplacement());
    Assert.assertNull(result.suggestion());

    Assert.assertEquals("How are you ", result.autoReplacement().committedText());
    Assert.assertEquals("hru? ", result.autoReplacement().replacementText());
  }

  @Test
  public void testSuggestsReplacementWhenAllowed() {
    final var rules =
        Collections.singletonList(
            new ContextProfilesStore.TypedRule("how are you", "hru?", false, true));

    final TypedRulesApplier.Result result = TypedRulesApplier.resolve("how are you ", rules, true);
    Assert.assertNull(result.autoReplacement());
    Assert.assertNotNull(result.suggestion());

    Assert.assertEquals("hru?", result.suggestion().suggestionText());
    Assert.assertEquals("how are you ", result.suggestion().committedText());
    Assert.assertEquals("hru? ", result.suggestion().replacementText());
  }

  @Test
  public void testSuggestionIsSuppressedWhenNotAllowed() {
    final var rules =
        Collections.singletonList(
            new ContextProfilesStore.TypedRule("how are you", "hru?", false, true));

    final TypedRulesApplier.Result result =
        TypedRulesApplier.resolve("how are you ", rules, false /*allowSuggestions*/);
    Assert.assertNull(result.autoReplacement());
    Assert.assertNull(result.suggestion());
  }

  @Test
  public void testDoesNotMatchInsideWord() {
    final var rules =
        Collections.singletonList(new ContextProfilesStore.TypedRule("he", "HE", true, true));

    final TypedRulesApplier.Result result = TypedRulesApplier.resolve("the ", rules, true);
    Assert.assertNull(result.autoReplacement());
    Assert.assertNull(result.suggestion());
  }

  @Test
  public void testCanMatchInsideWordWhenWholeWordDisabled() {
    final var rules =
        Collections.singletonList(
            new ContextProfilesStore.TypedRule(
                "he", "HE", true, true, false /*matchCaseSensitive*/, false /*matchWholeWord*/));

    final TypedRulesApplier.Result result = TypedRulesApplier.resolve("the ", rules, true);
    Assert.assertNotNull(result.autoReplacement());
    Assert.assertEquals("he ", result.autoReplacement().committedText());
    Assert.assertEquals("HE ", result.autoReplacement().replacementText());
  }

  @Test
  public void testCaseSensitiveRuleDoesNotMatchDifferentCase() {
    final var rules =
        Collections.singletonList(
            new ContextProfilesStore.TypedRule(
                "ASAP", "as soon as possible", true, true, true /*matchCaseSensitive*/, true));

    final TypedRulesApplier.Result result = TypedRulesApplier.resolve("asap ", rules, true);
    Assert.assertNull(result.autoReplacement());
    Assert.assertNull(result.suggestion());
  }

  @Test
  public void testMatchesWithTrailingPunctuationAndAvoidsDuplicatePunctuation() {
    final var rules =
        Collections.singletonList(
            new ContextProfilesStore.TypedRule("how are you", "hru?", false, true));

    final TypedRulesApplier.Result result = TypedRulesApplier.resolve("how are you? ", rules, true);
    Assert.assertNull(result.autoReplacement());
    Assert.assertNotNull(result.suggestion());

    Assert.assertEquals("hru?", result.suggestion().suggestionText());
    Assert.assertEquals("how are you? ", result.suggestion().committedText());
    Assert.assertEquals("hru? ", result.suggestion().replacementText());
  }
}
