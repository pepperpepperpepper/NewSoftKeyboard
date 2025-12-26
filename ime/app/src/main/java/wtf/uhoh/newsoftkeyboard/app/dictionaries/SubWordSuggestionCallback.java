package wtf.uhoh.newsoftkeyboard.app.dictionaries;

import androidx.annotation.NonNull;
import com.anysoftkeyboard.api.KeyCodes;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.dictionaries.Dictionary;
import wtf.uhoh.newsoftkeyboard.dictionaries.KeyCodesProvider;
import wtf.uhoh.newsoftkeyboard.dictionaries.SuggestionWordMatcher;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordComposer;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordsSplitter;

final class SubWordSuggestionCallback implements Dictionary.WordCallback {
  private final WordsSplitter splitter = new WordsSplitter();
  private final Dictionary.WordCallback basicWordCallback;
  private final String logTag;
  private final int possibleFixThresholdFrequency;

  // This will be used to find the best per suggestion word for a possible split
  @NonNull private CharSequence currentSubWord = "";
  private final char[] currentBestSubWordSuggestion = new char[Dictionary.MAX_WORD_LENGTH];
  private int currentBestSubWordSuggestionLength;
  private int currentBestSubWordAdjustedFrequency;
  private int currentBestSubWordRawFrequency;

  // This will be used to identify the best split
  private final char[] currentMatchedWords =
      new char[WordsSplitter.MAX_SPLITS * Dictionary.MAX_WORD_LENGTH];

  // this will be used to hold the currently best split
  private final char[] bestMatchedWords =
      new char[WordsSplitter.MAX_SPLITS * Dictionary.MAX_WORD_LENGTH];

  SubWordSuggestionCallback(
      Dictionary.WordCallback callback, String logTag, int possibleFixThresholdFrequency) {
    this.basicWordCallback = callback;
    this.logTag = logTag;
    this.possibleFixThresholdFrequency = possibleFixThresholdFrequency;
  }

  void performSubWordsMatching(
      @NonNull WordComposer wordComposer, @NonNull SuggestionsProvider suggestionsProvider) {
    int bestAdjustedFrequency = 0;
    int bestMatchWordsLength = 0;
    Iterable<Iterable<KeyCodesProvider>> splits = splitter.split(wordComposer);
    for (var split : splits) {
      int currentSplitLength = 0;
      int currentSplitAdjustedFrequency = 0;
      // split is a possible word splitting.
      // we first need to ensure all words are real words and get their frequency
      // the values will be in mMatchedWords
      // NOTE: we only pick a possible split if ALL words match something in the
      // dictionary
      int wordCount = 0;
      for (var subWord : split) {
        wordCount++;
        currentSubWord = subWord.getTypedWord();
        currentBestSubWordAdjustedFrequency = 0;
        currentBestSubWordRawFrequency = 0;
        currentBestSubWordSuggestionLength = 0;
        suggestionsProvider.getSuggestions(subWord, this);
        // at this point, we have the best adjusted sub-word
        if (currentBestSubWordAdjustedFrequency == 0) {
          Logger.d(logTag, "Did not find a match for sub-word '%s'", currentSubWord);
          wordCount = -1;
          break;
        }
        currentSplitAdjustedFrequency += currentBestSubWordRawFrequency;
        if (currentSplitLength > 0) {
          // adding space after the previous word
          currentMatchedWords[currentSplitLength] = KeyCodes.SPACE;
          currentSplitLength++;
        }
        System.arraycopy(
            currentBestSubWordSuggestion,
            0,
            currentMatchedWords,
            currentSplitLength,
            currentBestSubWordSuggestionLength);
        currentSplitLength += currentBestSubWordSuggestionLength;
      }
      // at this point, we have the best constructed split in mCurrentMatchedWords
      if (wordCount > 0 && currentSplitAdjustedFrequency > bestAdjustedFrequency) {
        System.arraycopy(currentMatchedWords, 0, bestMatchedWords, 0, currentSplitLength);
        bestAdjustedFrequency = currentSplitAdjustedFrequency;
        bestMatchWordsLength = currentSplitLength;
      }
    }
    // at this point, we have the most suitable split in mBestMatchedWords
    if (bestMatchWordsLength > 0) {
      basicWordCallback.addWord(
          bestMatchedWords,
          0,
          bestMatchWordsLength,
          possibleFixThresholdFrequency + bestAdjustedFrequency,
          null);
    }
  }

  @Override
  public boolean addWord(
      char[] word, int wordOffset, int wordLength, final int frequency, Dictionary from) {
    int adjustedFrequency = 0;
    // giving bonuses
    if (SuggestionWordMatcher.compareCaseInsensitive(
        currentSubWord, word, wordOffset, wordLength)) {
      adjustedFrequency = frequency * 4;
    } else if (SuggestionWordMatcher.haveSufficientCommonality(
        1, 1, currentSubWord, word, wordOffset, wordLength)) {
      adjustedFrequency = frequency * 2;
    }
    // only passing if the suggested word is close to the sub-word
    if (adjustedFrequency > currentBestSubWordAdjustedFrequency) {
      System.arraycopy(word, wordOffset, currentBestSubWordSuggestion, 0, wordLength);
      currentBestSubWordSuggestionLength = wordLength;
      currentBestSubWordAdjustedFrequency = adjustedFrequency;
      currentBestSubWordRawFrequency = frequency;
    }
    return true; // next word
  }
}
