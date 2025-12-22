package com.anysoftkeyboard.nextword.pipeline;

import androidx.annotation.NonNull;
import com.anysoftkeyboard.nextword.NextWordSuggestions;
import com.anysoftkeyboard.nextword.prediction.NextWordPredictionEngines;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Orchestrates next-word suggestions across on-device engines and legacy next-word dictionaries.
 */
public final class NextWordSuggestionsPipeline {

  public static final class Config {
    public final boolean enabled;
    public final boolean alsoSuggestNextPunctuations;
    public final int maxNextWordSuggestionsCount;
    public final int minWordUsage;

    public Config(
        boolean enabled,
        boolean alsoSuggestNextPunctuations,
        int maxNextWordSuggestionsCount,
        int minWordUsage) {
      this.enabled = enabled;
      this.alsoSuggestNextPunctuations = alsoSuggestNextPunctuations;
      this.maxNextWordSuggestionsCount = maxNextWordSuggestionsCount;
      this.minWordUsage = minWordUsage;
    }
  }

  @NonNull private final NextWordPredictionEngines mPredictionEngines;
  @NonNull private final List<NextWordSuggestions> mUserNextWordDictionaries;
  @NonNull private final Supplier<NextWordSuggestions> mContactsNextWordDictionary;
  @NonNull private final List<String> mInitialPunctuationSuggestions;

  public NextWordSuggestionsPipeline(
      @NonNull NextWordPredictionEngines predictionEngines,
      @NonNull List<NextWordSuggestions> userNextWordDictionaries,
      @NonNull Supplier<NextWordSuggestions> contactsNextWordDictionary,
      @NonNull List<String> initialPunctuationSuggestions) {
    mPredictionEngines = predictionEngines;
    mUserNextWordDictionaries = userNextWordDictionaries;
    mContactsNextWordDictionary = contactsNextWordDictionary;
    mInitialPunctuationSuggestions = initialPunctuationSuggestions;
  }

  public void resetSentence() {
    for (NextWordSuggestions nextWordSuggestions : mUserNextWordDictionaries) {
      nextWordSuggestions.resetSentence();
    }
    mContactsNextWordDictionary.get().resetSentence();
    mPredictionEngines.resetSentence();
  }

  public void appendNextWords(
      @NonNull String currentWord,
      @NonNull Collection<CharSequence> suggestionsHolder,
      int maxSuggestions,
      boolean incognitoMode,
      @NonNull Config config) {
    if (!config.enabled) return;

    int remainingSuggestions = maxSuggestions;
    final NextWordPredictionEngines.Outcome engineOutcome =
        mPredictionEngines.appendNextWords(
            currentWord,
            suggestionsHolder,
            remainingSuggestions,
            incognitoMode,
            config.maxNextWordSuggestionsCount);
    remainingSuggestions -= engineOutcome.added;
    if (remainingSuggestions <= 0) return;

    if (engineOutcome.shouldIncludeLegacyNextWords) {
      final int sizeBeforeUser = suggestionsHolder.size();
      appendLegacyNextWords(
          currentWord,
          suggestionsHolder,
          remainingSuggestions,
          incognitoMode,
          config.maxNextWordSuggestionsCount,
          config.minWordUsage);
      remainingSuggestions -= suggestionsHolder.size() - sizeBeforeUser;
      if (remainingSuggestions <= 0) return;
    } else if (!incognitoMode) {
      for (NextWordSuggestions nextWordDictionary : mUserNextWordDictionaries) {
        nextWordDictionary.notifyNextTypedWord(currentWord);
      }
    }

    for (String nextWordSuggestion :
        mContactsNextWordDictionary
            .get()
            .getNextWords(currentWord, config.maxNextWordSuggestionsCount, config.minWordUsage)) {
      suggestionsHolder.add(nextWordSuggestion);
      remainingSuggestions--;
      if (remainingSuggestions == 0) return;
    }

    if (config.alsoSuggestNextPunctuations) {
      for (String evenMoreSuggestions : mInitialPunctuationSuggestions) {
        suggestionsHolder.add(evenMoreSuggestions);
        remainingSuggestions--;
        if (remainingSuggestions == 0) return;
      }
    }
  }

  private void appendLegacyNextWords(
      @NonNull String currentWord,
      @NonNull Collection<CharSequence> suggestionsHolder,
      int maxSuggestions,
      boolean incognitoMode,
      int maxNextWordSuggestionsCount,
      int minWordUsage) {
    for (NextWordSuggestions nextWordDictionary : mUserNextWordDictionaries) {
      if (!incognitoMode) {
        nextWordDictionary.notifyNextTypedWord(currentWord);
      }

      for (String nextWordSuggestion :
          nextWordDictionary.getNextWords(currentWord, maxNextWordSuggestionsCount, minWordUsage)) {
        suggestionsHolder.add(nextWordSuggestion);
        maxSuggestions--;
        if (maxSuggestions == 0) return;
      }
    }
  }
}
