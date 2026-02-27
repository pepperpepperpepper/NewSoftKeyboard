package wtf.uhoh.newsoftkeyboard.app.dictionaries;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import wtf.uhoh.newsoftkeyboard.dictionaries.Dictionary;
import wtf.uhoh.newsoftkeyboard.dictionaries.KeyCodesProvider;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordComposer;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class SuggestImplTest {

  @Test
  public void
      getSuggestions_injectsUpToThreePrefixMatchingNextWordsAfterStableCompletionsForSingleLetterPrefix() {
    final SuggestionsProvider provider = mock(SuggestionsProvider.class);
    when(provider.isValidWord(any(CharSequence.class))).thenReturn(true);

    doAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              final Collection<CharSequence> holder = invocation.getArgument(1);
              holder.addAll(List.of("informed", "incredible", "inside", "into", "island"));
              return null;
            })
        .when(provider)
        .getNextWords(eq("keep"), any(), anyInt());

    doAnswer(
            invocation -> {
              final Dictionary.WordCallback callback = invocation.getArgument(1);
              addWord(callback, "it", 100);
              addWord(callback, "in", 90);
              addWord(callback, "is", 80);
              return null;
            })
        .when(provider)
        .getSuggestions(any(KeyCodesProvider.class), any(Dictionary.WordCallback.class));

    final SuggestImpl suggest = new SuggestImpl(provider);
    suggest.setCorrectionMode(true, 1, 1, false);

    suggest.getNextSuggestions("keep", false);

    final WordComposer composer = new WordComposer();
    composer.simulateTypedWord("i");

    assertEquals(
        List.of("i", "it", "in", "is", "informed", "incredible", "inside"),
        asStrings(suggest.getSuggestions(composer)));
  }

  @Test
  public void getSuggestions_doesNotInsertNextWordCandidatesBeforeBestCorrectionCandidate() {
    final SuggestionsProvider provider = mock(SuggestionsProvider.class);
    when(provider.isValidWord(any(CharSequence.class))).thenReturn(true);

    doAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              final Collection<CharSequence> holder = invocation.getArgument(1);
              holder.addAll(List.of("there", "then", "those"));
              return null;
            })
        .when(provider)
        .getNextWords(eq("keep"), any(), anyInt());

    doAnswer(
            invocation -> {
              final Dictionary.WordCallback callback = invocation.getArgument(1);
              addWord(callback, "the", Integer.MAX_VALUE / 2);
              return null;
            })
        .when(provider)
        .getSuggestions(any(KeyCodesProvider.class), any(Dictionary.WordCallback.class));

    final SuggestImpl suggest = new SuggestImpl(provider);
    suggest.setCorrectionMode(true, 1, 1, false);

    suggest.getNextSuggestions("keep", false);

    final WordComposer composer = new WordComposer();
    composer.simulateTypedWord("Th");
    composer.setFirstCharCapitalized(true);

    assertEquals(
        List.of("Th", "The", "There", "Then", "Those"),
        asStrings(suggest.getSuggestions(composer)));
  }

  @Test
  public void getSuggestions_promotesAlreadyPresentNextWordCandidatesAboveOtherCompletions() {
    final SuggestionsProvider provider = mock(SuggestionsProvider.class);
    when(provider.isValidWord(any(CharSequence.class))).thenReturn(true);

    doAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              final Collection<CharSequence> holder = invocation.getArgument(1);
              holder.addAll(List.of("there", "then", "their"));
              return null;
            })
        .when(provider)
        .getNextWords(eq("keep"), any(), anyInt());

    doAnswer(
            invocation -> {
              final Dictionary.WordCallback callback = invocation.getArgument(1);
              addWord(callback, "thanks", 100);
              addWord(callback, "thing", 90);
              addWord(callback, "there", 10);
              return null;
            })
        .when(provider)
        .getSuggestions(any(KeyCodesProvider.class), any(Dictionary.WordCallback.class));

    final SuggestImpl suggest = new SuggestImpl(provider);
    suggest.setCorrectionMode(true, 1, 1, false);

    suggest.getNextSuggestions("keep", false);

    final WordComposer composer = new WordComposer();
    composer.simulateTypedWord("th");

    assertEquals(
        List.of("th", "there", "then", "their", "thanks", "thing"),
        asStrings(suggest.getSuggestions(composer)));
  }

  @Test
  public void getSuggestions_reranksPrefixMatchingTypedSuggestionsByContextForTwoLetterPrefix() {
    final SuggestionsProvider provider = mock(SuggestionsProvider.class);
    when(provider.isValidWord(any(CharSequence.class))).thenReturn(true);

    doAnswer(
            invocation -> {
              final Dictionary.WordCallback callback = invocation.getArgument(1);
              addWord(callback, "thanks", 100);
              addWord(callback, "there", 90);
              addWord(callback, "then", 80);
              return null;
            })
        .when(provider)
        .getSuggestions(any(KeyCodesProvider.class), any(Dictionary.WordCallback.class));

    when(provider.sortCandidatesByNeuralFirstTokenLogProbIfAvailable(any()))
        .thenReturn(List.of("there", "then", "thanks"));

    final SuggestImpl suggest = new SuggestImpl(provider);
    suggest.setCorrectionMode(true, 1, 1, false);

    final WordComposer composer = new WordComposer();
    composer.simulateTypedWord("th");

    assertEquals(
        List.of("th", "there", "then", "thanks"), asStrings(suggest.getSuggestions(composer)));
  }

  @Test
  public void
      getSuggestions_reranksPrefixMatchingTypedSuggestionsByContextPoolWhenNeuralUnavailable() {
    final SuggestionsProvider provider = mock(SuggestionsProvider.class);
    when(provider.isValidWord(any(CharSequence.class))).thenReturn(true);

    doAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              final Collection<CharSequence> holder = invocation.getArgument(1);
              holder.addAll(List.of("there", "then", "their", "those", "thanks"));
              return null;
            })
        .when(provider)
        .getNextWords(eq("keep"), any(), anyInt());

    doAnswer(
            invocation -> {
              final Dictionary.WordCallback callback = invocation.getArgument(1);
              addWord(callback, "thanks", 100);
              addWord(callback, "those", 90);
              addWord(callback, "there", 80);
              addWord(callback, "then", 70);
              addWord(callback, "their", 60);
              return null;
            })
        .when(provider)
        .getSuggestions(any(KeyCodesProvider.class), any(Dictionary.WordCallback.class));

    when(provider.sortCandidatesByNeuralFirstTokenLogProbIfAvailable(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    final SuggestImpl suggest = new SuggestImpl(provider);
    suggest.setCorrectionMode(true, 1, 1, false);

    suggest.getNextSuggestions("keep", false);

    final WordComposer composer = new WordComposer();
    composer.simulateTypedWord("th");

    assertEquals(
        List.of("th", "there", "then", "their", "those", "thanks"),
        asStrings(suggest.getSuggestions(composer)));
  }

  @Test
  public void getSuggestions_doesNotRerankTypedSuggestionsByContextForSingleLetterPrefix() {
    final SuggestionsProvider provider = mock(SuggestionsProvider.class);
    when(provider.isValidWord(any(CharSequence.class))).thenReturn(true);

    doAnswer(
            invocation -> {
              final Dictionary.WordCallback callback = invocation.getArgument(1);
              addWord(callback, "thanks", 100);
              addWord(callback, "there", 90);
              addWord(callback, "then", 80);
              return null;
            })
        .when(provider)
        .getSuggestions(any(KeyCodesProvider.class), any(Dictionary.WordCallback.class));

    when(provider.sortCandidatesByNeuralFirstTokenLogProbIfAvailable(any()))
        .thenReturn(List.of("there", "then", "thanks"));

    final SuggestImpl suggest = new SuggestImpl(provider);
    suggest.setCorrectionMode(true, 1, 1, false);

    final WordComposer composer = new WordComposer();
    composer.simulateTypedWord("t");

    assertEquals(
        List.of("t", "thanks", "there", "then"), asStrings(suggest.getSuggestions(composer)));
    verify(provider, never()).sortCandidatesByNeuralFirstTokenLogProbIfAvailable(any());
  }

  @Test
  public void getSuggestions_filtersAllCapsAcronymsWhenTypedIsLowercase() {
    final SuggestionsProvider provider = mock(SuggestionsProvider.class);
    when(provider.isValidWord(any(CharSequence.class))).thenReturn(true);

    doAnswer(
            invocation -> {
              final Dictionary.WordCallback callback = invocation.getArgument(1);
              addWord(callback, "that", 100);
              addWord(callback, "than", 90);
              addWord(callback, "Thai", 80);
              addWord(callback, "TNA", 70);
              return null;
            })
        .when(provider)
        .getSuggestions(any(KeyCodesProvider.class), any(Dictionary.WordCallback.class));

    final SuggestImpl suggest = new SuggestImpl(provider);
    suggest.setCorrectionMode(true, 1, 1, false);

    final WordComposer composer = new WordComposer();
    composer.simulateTypedWord("tha");

    assertEquals(
        List.of("tha", "that", "than", "Thai"), asStrings(suggest.getSuggestions(composer)));
  }

  @Test
  public void getSuggestions_injectsPrefixMatchesFromCandidatePoolBeyondDisplayedNextWords() {
    final SuggestionsProvider provider = mock(SuggestionsProvider.class);
    when(provider.isValidWord(any(CharSequence.class))).thenReturn(true);
    when(provider.nextWordCandidatePoolLimit()).thenReturn(24);

    doAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              final Collection<CharSequence> holder = invocation.getArgument(1);
              holder.addAll(
                  List.of(
                      "alpha", "beta", "gamma", "delta", "epsilon", "eta", "theta", "iota", "kappa",
                      "lambda", "mu", "nu", "zebra", "zesty", "zoo"));
              return null;
            })
        .when(provider)
        .getNextWords(eq("keep"), any(), anyInt());

    doAnswer(
            invocation -> {
              final Dictionary.WordCallback callback = invocation.getArgument(1);
              addWord(callback, "zip", 100);
              addWord(callback, "zone", 90);
              addWord(callback, "zinc", 80);
              return null;
            })
        .when(provider)
        .getSuggestions(any(KeyCodesProvider.class), any(Dictionary.WordCallback.class));

    final SuggestImpl suggest = new SuggestImpl(provider);
    suggest.setCorrectionMode(true, 1, 1, false);

    assertEquals(
        List.of(
            "alpha", "beta", "gamma", "delta", "epsilon", "eta", "theta", "iota", "kappa", "lambda",
            "mu", "nu"),
        asStrings(suggest.getNextSuggestions("keep", false)));

    final WordComposer composer = new WordComposer();
    composer.simulateTypedWord("z");

    assertEquals(
        List.of("z", "zip", "zone", "zinc", "zebra", "zesty", "zoo"),
        asStrings(suggest.getSuggestions(composer)));
  }

  @Test
  public void
      getNextSuggestions_fallsBackToLegacySourcesEvenWhenPreviousWordIsInvalidAndEnginesInactive() {
    final SuggestionsProvider provider = mock(SuggestionsProvider.class);
    when(provider.isValidWord(any(CharSequence.class))).thenReturn(false);
    when(provider.isPresageEnabled()).thenReturn(false);
    when(provider.isNeuralEnabled()).thenReturn(false);
    when(provider.nextWordCandidatePoolLimit()).thenReturn(8);

    doAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              final Collection<CharSequence> holder = invocation.getArgument(1);
              holder.addAll(List.of("ross", "tyson"));
              return null;
            })
        .when(provider)
        .getNextWords(eq("mike"), any(), anyInt());

    final SuggestImpl suggest = new SuggestImpl(provider);
    suggest.setCorrectionMode(true, 1, 1, false);

    assertEquals(List.of("ross", "tyson"), asStrings(suggest.getNextSuggestions("mike", false)));
    verify(provider).getNextWords(eq("mike"), any(), anyInt());
  }

  @Test
  public void seedNextWordEngineContextFromEditorText_tokenizesAndDelegatesToProvider() {
    final SuggestionsProvider provider = mock(SuggestionsProvider.class);
    final SuggestImpl suggest = new SuggestImpl(provider);

    suggest.seedNextWordEngineContextFromEditorText("hello world. again here");

    verify(provider).seedNextWordEngineContextTokens(eq(List.of("again", "here")));
  }

  private static void addWord(Dictionary.WordCallback callback, String word, int frequency) {
    callback.addWord(word.toCharArray(), 0, word.length(), frequency, mock(Dictionary.class));
  }

  private static List<String> asStrings(List<CharSequence> suggestions) {
    final List<String> out = new ArrayList<>(suggestions.size());
    for (CharSequence suggestion : suggestions) {
      if (suggestion != null) out.add(suggestion.toString());
    }
    return out;
  }
}
