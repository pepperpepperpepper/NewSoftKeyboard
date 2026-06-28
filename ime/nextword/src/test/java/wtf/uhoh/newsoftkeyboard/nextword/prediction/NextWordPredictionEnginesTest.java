package wtf.uhoh.newsoftkeyboard.nextword.prediction;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.prefs.RxSharedPrefs;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class NextWordPredictionEnginesTest {

  @Test
  public void testKeepsEphemeralContextInIncognitoField() {
    final RxSharedPrefs prefs = new RxSharedPrefs(getApplicationContext(), input -> {});
    final NextWordPredictionEngines engines =
        new NextWordPredictionEngines(getApplicationContext(), prefs, "test", false, false);

    engines.setIncognitoMode(true);
    Assert.assertArrayEquals(new String[0], engines.getContextTokensForTests());

    engines.notifyWordCommitted("keep");
    Assert.assertArrayEquals(new String[] {"keep"}, engines.getContextTokensForTests());

    engines.notifyWordCommitted("me");
    Assert.assertArrayEquals(new String[] {"keep", "me"}, engines.getContextTokensForTests());

    engines.setIncognitoMode(false);
    Assert.assertArrayEquals(new String[0], engines.getContextTokensForTests());

    engines.notifyWordCommitted("hello");
    Assert.assertArrayEquals(new String[] {"hello"}, engines.getContextTokensForTests());
  }

  @Test
  public void getOrScheduleWordCompletions_guardsReturnEmptyAndNeverBlock() {
    final RxSharedPrefs prefs = new RxSharedPrefs(getApplicationContext(), input -> {});
    final NextWordPredictionEngines engines =
        new NextWordPredictionEngines(getApplicationContext(), prefs, "test", false, false);

    // Cold context: nothing to condition on, so no completion.
    Assert.assertTrue(engines.getOrScheduleWordCompletions("rec", 3).isEmpty());

    engines.notifyWordCommitted("hello");

    // Prefix too short to be worth a model call.
    Assert.assertTrue(engines.getOrScheduleWordCompletions("r", 3).isEmpty());
    // Non-positive maxResults.
    Assert.assertTrue(engines.getOrScheduleWordCompletions("rec", 0).isEmpty());
    // Valid request with no model installed: returns empty synchronously (schedules async, which
    // fails activation gracefully) rather than blocking the suggestion thread.
    Assert.assertTrue(engines.getOrScheduleWordCompletions("rec", 3).isEmpty());
  }

  @Test
  public void appendNextWords_doesNotMutateContext() {
    final RxSharedPrefs prefs = new RxSharedPrefs(getApplicationContext(), input -> {});
    final NextWordPredictionEngines engines =
        new NextWordPredictionEngines(getApplicationContext(), prefs, "test", false, false);

    engines.notifyWordCommitted("keep");
    Assert.assertArrayEquals(new String[] {"keep"}, engines.getContextTokensForTests());

    engines.appendNextWords("keep", new java.util.ArrayList<>(), 0, false, 8);
    engines.appendNextWords("keep", new java.util.ArrayList<>(), 0, false, 8);
    engines.appendNextWords("other", new java.util.ArrayList<>(), 0, false, 8);
    Assert.assertArrayEquals(new String[] {"keep"}, engines.getContextTokensForTests());
  }

  @Test
  public void interleaveHybridSuggestions_neuralComesFirstAndAlternates() {
    final ArrayList<CharSequence> suggestions = new ArrayList<>();
    suggestions.addAll(List.of("p0", "p1", "p2", "n0", "n1"));

    NextWordPredictionEngines.interleaveHybridSuggestions(suggestions, 0, 3, 2);

    Assert.assertEquals(List.of("n0", "p0", "n1", "p1", "p2"), suggestions);
  }

  @Test
  public void seedContextTokens_overwritesContext() {
    final RxSharedPrefs prefs = new RxSharedPrefs(getApplicationContext(), input -> {});
    final NextWordPredictionEngines engines =
        new NextWordPredictionEngines(getApplicationContext(), prefs, "test", false, false);

    engines.notifyWordCommitted("hello");
    Assert.assertArrayEquals(new String[] {"hello"}, engines.getContextTokensForTests());

    engines.seedContextTokens(List.of("alpha", "beta"));
    Assert.assertArrayEquals(new String[] {"alpha", "beta"}, engines.getContextTokensForTests());

    engines.seedContextTokens(List.of());
    Assert.assertArrayEquals(new String[0], engines.getContextTokensForTests());
  }

  @Test
  public void seedContextTokens_modeNoneKeepsContextEmpty() {
    final RxSharedPrefs prefs = new RxSharedPrefs(getApplicationContext(), input -> {});
    final NextWordPredictionEngines engines =
        new NextWordPredictionEngines(getApplicationContext(), prefs, "test", false, false);

    engines.updatePredictionEngine("none");
    engines.seedContextTokens(List.of("alpha", "beta"));
    Assert.assertArrayEquals(new String[0], engines.getContextTokensForTests());
  }
}
