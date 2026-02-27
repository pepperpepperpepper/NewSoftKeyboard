package wtf.uhoh.newsoftkeyboard.app.dictionaries;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.nextword.pipeline.NextWordSuggestionsPipeline;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class SuggestionsProviderTest {

  @Test
  public void nextWordAggressivenessMappingHasEnoughCandidatesForReranking() throws Exception {
    final SuggestionsProvider provider = new SuggestionsProvider(getApplicationContext());

    final Method setNextWordAggressiveness =
        SuggestionsProvider.class.getDeclaredMethod("setNextWordAggressiveness", String.class);
    setNextWordAggressiveness.setAccessible(true);

    final Field nextWordConfigField = SuggestionsProvider.class.getDeclaredField("mNextWordConfig");
    nextWordConfigField.setAccessible(true);

    setNextWordAggressiveness.invoke(provider, "minimal_aggressiveness");
    NextWordSuggestionsPipeline.Config config =
        (NextWordSuggestionsPipeline.Config) nextWordConfigField.get(provider);
    Assert.assertEquals(8, config.maxNextWordSuggestionsCount);

    setNextWordAggressiveness.invoke(provider, "medium_aggressiveness");
    config = (NextWordSuggestionsPipeline.Config) nextWordConfigField.get(provider);
    Assert.assertEquals(24, config.maxNextWordSuggestionsCount);

    setNextWordAggressiveness.invoke(provider, "maximum_aggressiveness");
    config = (NextWordSuggestionsPipeline.Config) nextWordConfigField.get(provider);
    Assert.assertEquals(48, config.maxNextWordSuggestionsCount);

    provider.destroy();
  }
}
