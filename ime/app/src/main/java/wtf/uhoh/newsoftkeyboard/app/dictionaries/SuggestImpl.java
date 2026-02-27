/*
 * Copyright (c) 2013 Menny Even-Danan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wtf.uhoh.newsoftkeyboard.app.dictionaries;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import wtf.uhoh.newsoftkeyboard.BuildConfig;
import wtf.uhoh.newsoftkeyboard.app.quicktextkeys.TagsExtractor;
import wtf.uhoh.newsoftkeyboard.app.quicktextkeys.TagsExtractorImpl;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.dictionaries.AbbreviationSuggestionCallback;
import wtf.uhoh.newsoftkeyboard.dictionaries.AutoTextSuggestionCallback;
import wtf.uhoh.newsoftkeyboard.dictionaries.Dictionary;
import wtf.uhoh.newsoftkeyboard.dictionaries.DictionaryBackgroundLoader;
import wtf.uhoh.newsoftkeyboard.dictionaries.SuggestionWordMatcher;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordComposer;
import wtf.uhoh.newsoftkeyboard.nextword.prediction.NextWordContextTokenizer;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;
import wtf.uhoh.newsoftkeyboard.utils.IMEUtil;

/**
 * This class loads a dictionary and provides a list of suggestions for a given sequence of
 * characters. This includes corrections and completions.
 */
@SuppressWarnings("this-escape")
public class SuggestImpl implements Suggest, ContextProfileAwareSuggest {
  private static final String TAG = "NSKSuggest";
  private static final int POSSIBLE_FIX_THRESHOLD_FREQUENCY = Integer.MAX_VALUE / 2;
  private static final int MAX_PREFIX_MATCHING_NEXT_WORDS_IN_TYPED_SUGGESTIONS = 3;
  private static final int MAX_PREFIX_MATCHING_TYPED_SUGGESTIONS_TO_RERANK_BY_CONTEXT = 8;
  private static final int ABBREVIATION_TEXT_FREQUENCY = Integer.MAX_VALUE - 10;
  private static final int AUTO_TEXT_FREQUENCY = Integer.MAX_VALUE - 20;
  private static final int VALID_TYPED_WORD_FREQUENCY = Integer.MAX_VALUE - 25;
  private static final int FIXED_TYPED_WORD_FREQUENCY = Integer.MAX_VALUE - 30;
  private static final int TYPED_WORD_FREQUENCY = Integer.MAX_VALUE; // always the first
  @NonNull private final SuggestionsProvider mSuggestionsProvider;
  private final List<CharSequence> mSuggestions = new ArrayList<>();
  private final List<CharSequence> mNextSuggestions = new ArrayList<>();
  private final List<CharSequence> mNextSuggestionCandidates = new ArrayList<>();
  private final List<CharSequence> mStringPool = new ArrayList<>();
  private final Dictionary.WordCallback mAutoTextWordCallback;
  private final Dictionary.WordCallback mAbbreviationWordCallback;
  private final Dictionary.WordCallback mTypingDictionaryWordCallback;
  private final SubWordSuggestionCallback mSubWordDictionaryWordCallback;

  @NonNull private final Locale mLocale = Locale.getDefault();
  private int mPrefMaxSuggestions = 12;
  @NonNull private TagsExtractor mTagsSearcher = TagsExtractorImpl.NO_OP;
  @NonNull private int[] mPriorities = new int[mPrefMaxSuggestions];
  private int mCorrectSuggestionIndex = -1;
  @NonNull private String mLowerOriginalWord = "";
  @NonNull private String mTypedOriginalWord = "";
  private boolean mIsFirstCharCapitalized;
  private boolean mIsAllUpperCase;
  private int mCommonalityMaxLengthDiff = 1;
  private int mCommonalityMaxDistance = 1;
  private boolean mEnabledSuggestions;
  private boolean mSplitWords;

  @VisibleForTesting
  public SuggestImpl(@NonNull SuggestionsProvider provider) {
    mSuggestionsProvider = provider;
    final SuggestionCallback basicWordCallback = new SuggestionCallback();
    mTypingDictionaryWordCallback = new DictionarySuggestionCallback(basicWordCallback);
    mSubWordDictionaryWordCallback =
        new SubWordSuggestionCallback(basicWordCallback, TAG, POSSIBLE_FIX_THRESHOLD_FREQUENCY);
    mAutoTextWordCallback = new AutoTextSuggestionCallback(basicWordCallback, AUTO_TEXT_FREQUENCY);
    mAbbreviationWordCallback =
        new AbbreviationSuggestionCallback(basicWordCallback, ABBREVIATION_TEXT_FREQUENCY);
    setMaxSuggestions(mPrefMaxSuggestions);
  }

  public SuggestImpl(@NonNull Context context) {
    this(new SuggestionsProvider(context));
  }

  @Override
  public void setCorrectionMode(
      boolean enabledSuggestions, int maxLengthDiff, int maxDistance, boolean splitWords) {
    mEnabledSuggestions = enabledSuggestions;

    // making sure it is not negative or zero
    mCommonalityMaxLengthDiff = maxLengthDiff;
    mCommonalityMaxDistance = maxDistance;
    mSplitWords = splitWords;
  }

  @Override
  @VisibleForTesting
  public boolean isSuggestionsEnabled() {
    return mEnabledSuggestions;
  }

  @Override
  public void closeDictionaries() {
    mSuggestionsProvider.close();
  }

  @Override
  public void setupSuggestionsForKeyboard(
      @NonNull List<DictionaryAddOnAndBuilder> dictionaryBuilders,
      @NonNull DictionaryBackgroundLoader.Listener cb) {
    if (mEnabledSuggestions && dictionaryBuilders.size() > 0) {
      mSuggestionsProvider.setupSuggestionsForKeyboard(dictionaryBuilders, cb);
    } else {
      closeDictionaries();
    }
  }

  @Override
  public void setMaxSuggestions(int maxSuggestions) {
    if (maxSuggestions < 1 || maxSuggestions > 100) {
      throw new IllegalArgumentException("maxSuggestions must be between 1 and 100");
    }
    mPrefMaxSuggestions = maxSuggestions;
    mPriorities = new int[mPrefMaxSuggestions];
    collectGarbage();
    while (mStringPool.size() < mPrefMaxSuggestions) {
      StringBuilder sb = new StringBuilder(32);
      mStringPool.add(sb);
    }
  }

  @Override
  public void resetNextWordSentence() {
    mNextSuggestions.clear();
    mNextSuggestionCandidates.clear();
    mSuggestionsProvider.resetNextWordSentence();
  }

  @Override
  public List<CharSequence> getNextSuggestions(
      final CharSequence previousWord, final boolean inAllUpperCaseState) {
    final String currentWord = previousWord.toString().trim();
    mNextSuggestions.clear();
    mNextSuggestionCandidates.clear();
    if (currentWord.isEmpty()) {
      return Collections.emptyList();
    }

    mIsAllUpperCase = inAllUpperCaseState;

    final boolean validWord = isValidWord(currentWord);
    final boolean presageEnabled = mSuggestionsProvider.isPresageEnabled();
    final boolean neuralEnabled = mSuggestionsProvider.isNeuralEnabled();

    if (!validWord && !(presageEnabled || neuralEnabled)) {
      Logger.d(
          TAG,
          "getNextSuggestions for '%s' is invalid and no third-party engine (ngram/neural) is"
              + " active. Falling back to legacy next-word sources.",
          currentWord);
    }

    final int candidatePoolLimit =
        Math.max(mPrefMaxSuggestions, mSuggestionsProvider.nextWordCandidatePoolLimit());
    mSuggestionsProvider.getNextWords(currentWord, mNextSuggestionCandidates, candidatePoolLimit);
    final int displayLimit = Math.min(mPrefMaxSuggestions, mNextSuggestionCandidates.size());
    for (int i = 0; i < displayLimit; i++) {
      mNextSuggestions.add(mNextSuggestionCandidates.get(i));
    }
    if (BuildConfig.DEBUG) {
      Logger.d(
          TAG,
          "getNextSuggestions sources for '%s' (capital? %s, valid? %s, presage? %s, neural? %s):",
          previousWord,
          mIsAllUpperCase,
          validWord,
          presageEnabled,
          neuralEnabled);
      for (int suggestionIndex = 0; suggestionIndex < mNextSuggestions.size(); suggestionIndex++) {
        Logger.d(
            TAG,
            "* getNextSuggestions #%d :''%s'",
            suggestionIndex,
            mNextSuggestions.get(suggestionIndex));
      }
    }

    if (mIsAllUpperCase) {
      for (int suggestionIndex = 0;
          suggestionIndex < mNextSuggestionCandidates.size();
          suggestionIndex++) {
        mNextSuggestionCandidates.set(
            suggestionIndex,
            mNextSuggestionCandidates.get(suggestionIndex).toString().toUpperCase(mLocale));
      }
      for (int suggestionIndex = 0; suggestionIndex < mNextSuggestions.size(); suggestionIndex++) {
        mNextSuggestions.set(
            suggestionIndex, mNextSuggestions.get(suggestionIndex).toString().toUpperCase(mLocale));
      }
    }

    return mNextSuggestions;
  }

  @Override
  public void notifyWordCommitted(@NonNull CharSequence committedWord) {
    final String token = committedWord.toString().trim();
    if (token.isEmpty()) return;
    mSuggestionsProvider.notifyWordCommitted(token);
  }

  @Override
  public void seedNextWordEngineContextFromEditorText(@NonNull CharSequence textBeforeCursor) {
    if (TextUtils.isEmpty(textBeforeCursor)) return;
    final List<String> tokens =
        NextWordContextTokenizer.tokenizeTextBeforeCursor(textBeforeCursor, 64);
    if (tokens.isEmpty()) return;
    mSuggestionsProvider.seedNextWordEngineContextTokens(tokens);
  }

  @Override
  public List<CharSequence> getSuggestions(WordComposer wordComposer) {
    if (!mEnabledSuggestions) return Collections.emptyList();

    mCorrectSuggestionIndex = -1;
    mIsFirstCharCapitalized = wordComposer.isFirstCharCapitalized();
    mIsAllUpperCase = wordComposer.isAllUpperCase();
    collectGarbage();
    Arrays.fill(mPriorities, 0);

    // Save a lowercase version of the original word
    mTypedOriginalWord = wordComposer.getTypedWord().toString();
    if (mTypedOriginalWord.length() > 0) {
      mLowerOriginalWord = mTypedOriginalWord.toLowerCase(mLocale);
    } else {
      mLowerOriginalWord = "";
    }

    if (wordComposer.isAtTagsSearchState() && mTagsSearcher.isEnabled()) {
      final CharSequence typedTagToSearch = mLowerOriginalWord.substring(1);
      return mTagsSearcher.getOutputForTag(typedTagToSearch, wordComposer);
    }

    mSuggestions.add(0, mTypedOriginalWord);
    mPriorities[0] = TYPED_WORD_FREQUENCY;

    // searching dictionaries by priority order:
    // abbreviations
    mSuggestionsProvider.getAbbreviations(wordComposer, mAbbreviationWordCallback);
    // auto-text
    mSuggestionsProvider.getAutoText(wordComposer, mAutoTextWordCallback);
    // for sub-word matching:
    // only if ALL words match, we should use the sub-words as an suggestion
    // only exact matches (for now) will be considered
    if (mSplitWords)
      mSubWordDictionaryWordCallback.performSubWordsMatching(wordComposer, mSuggestionsProvider);
    // contacts, user and main dictionaries
    mSuggestionsProvider.getSuggestions(wordComposer, mTypingDictionaryWordCallback);

    // now, we'll look at the next-words-suggestions list, and add all the ones that begins
    // with the typed word. These suggestions are top priority, so they will be added
    // at the top of the list
    final List<CharSequence> nextSuggestionCandidates =
        mNextSuggestionCandidates.isEmpty() ? mNextSuggestions : mNextSuggestionCandidates;
    final int typedWordLength = mLowerOriginalWord.length();
    int nextWordInsertionIndex = 1; // keep the typed word at the top while composing
    if (typedWordLength == 1) {
      // For 1-letter prefixes, prefer stable "word completion" ordering from the dictionaries
      // (otherwise next-word injection can dominate and feel random).
      nextWordInsertionIndex = Math.min(mSuggestions.size(), 1 + 3);
    }
    if (mCorrectSuggestionIndex >= 1 && nextWordInsertionIndex <= mCorrectSuggestionIndex) {
      // Never insert before the best correction candidate: it keeps auto-correct highlighting
      // stable and avoids confusing suggestion ordering when quick-fix candidates exist.
      nextWordInsertionIndex = mCorrectSuggestionIndex + 1;
    }
    int injectedCount = 0;
    // Next-word suggestions are ordered by usage/probability; keep that order, but limit how much
    // they dominate the list when only a short prefix is typed (prevents the strip from feeling
    // "random" or overly context-heavy).
    for (CharSequence nextWordSuggestion : nextSuggestionCandidates) {
      if (injectedCount >= MAX_PREFIX_MATCHING_NEXT_WORDS_IN_TYPED_SUGGESTIONS) break;
      if (nextWordSuggestion == null) continue;
      if (nextWordSuggestion.length() < typedWordLength) continue;
      final String suggestionText = nextWordSuggestion.toString();
      if (!suggestionText.regionMatches(
          /* ignoreCase= */ true,
          /* toffset= */ 0,
          mTypedOriginalWord,
          /* ooffset= */ 0,
          typedWordLength)) {
        continue;
      }
      final CharSequence cased = applyTypedCasingToSuggestion(suggestionText);
      final String casedString = cased.toString();
      int existingIndex = -1;
      // Prefer promoting candidates which already exist below the injection point (this gives
      // "SwiftKey-like" behavior where context re-ranks typed completions, instead of only
      // appending extra items).
      for (int suggestionIndex = nextWordInsertionIndex;
          suggestionIndex < mSuggestions.size();
          suggestionIndex++) {
        final CharSequence existing = mSuggestions.get(suggestionIndex);
        if (existing == null) continue;
        if (TextUtils.equals(existing, cased)
            || existing.toString().equalsIgnoreCase(casedString)) {
          existingIndex = suggestionIndex;
          break;
        }
      }
      if (existingIndex != -1) {
        mSuggestions.remove(existingIndex);
        mSuggestions.add(nextWordInsertionIndex++, cased);
        injectedCount++;
        continue;
      }
      // If the candidate already exists above the injection point, skip it (don't insert a dupe).
      for (int suggestionIndex = 0;
          suggestionIndex < Math.min(nextWordInsertionIndex, mSuggestions.size());
          suggestionIndex++) {
        final CharSequence existing = mSuggestions.get(suggestionIndex);
        if (existing == null) continue;
        if (TextUtils.equals(existing, cased)
            || existing.toString().equalsIgnoreCase(casedString)) {
          existingIndex = suggestionIndex;
          break;
        }
      }
      if (existingIndex != -1) continue;
      mSuggestions.add(nextWordInsertionIndex++, cased);
      injectedCount++;
    }

    maybeRerankPrefixMatchingTypedSuggestionsByContext(typedWordLength);

    // removing possible duplicates to typed.
    IMEUtil.removeDupes(mSuggestions, mStringPool);
    IMEUtil.tripSuggestions(mSuggestions, mPrefMaxSuggestions, mStringPool);

    return mSuggestions;
  }

  @Override
  public int getLastValidSuggestionIndex() {
    return mCorrectSuggestionIndex;
  }

  @Override
  public boolean isValidWord(final CharSequence word) {
    return mSuggestionsProvider.isValidWord(word);
  }

  private void collectGarbage() {
    int poolSize = mStringPool.size();
    int garbageSize = mSuggestions.size();
    while (poolSize < mPrefMaxSuggestions && garbageSize > 0) {
      CharSequence garbage = mSuggestions.get(garbageSize - 1);
      if (garbage instanceof StringBuilder) {
        mStringPool.add(garbage);
        poolSize++;
      }
      garbageSize--;
    }
    if (poolSize == mPrefMaxSuggestions + 1) {
      Logger.w(TAG, "String pool got too big: " + poolSize);
    }
    mSuggestions.clear();
  }

  private boolean shouldFilterAllCapsAcronymSuggestion(@NonNull CharSequence candidate) {
    // When the user is typing in lowercase, avoid surfacing unrelated ALL-CAPS acronyms as
    // correction/completion candidates. They are often low-signal (contacts/learned tokens) and
    // can look like the engine is "hallucinating".
    if (mTypedOriginalWord.length() < 2) return false;
    if (!TextUtils.equals(mTypedOriginalWord, mLowerOriginalWord)) return false;
    if (candidate.length() < 3) return false;
    if (candidate.toString().equalsIgnoreCase(mTypedOriginalWord)) return false;
    return looksLikeAllCapsAcronym(candidate);
  }

  private static boolean looksLikeAllCapsAcronym(@NonNull CharSequence candidate) {
    boolean hasLetter = false;
    for (int i = 0; i < candidate.length(); i++) {
      final char c = candidate.charAt(i);
      if (!Character.isLetter(c)) {
        return false;
      }
      hasLetter = true;
      if (!Character.isUpperCase(c)) {
        return false;
      }
    }
    return hasLetter;
  }

  @NonNull
  private CharSequence applyTypedCasingToSuggestion(@NonNull String suggestion) {
    if (mIsAllUpperCase) {
      return suggestion.toUpperCase(mLocale);
    }
    if (mIsFirstCharCapitalized && suggestion.length() > 0) {
      final char first = suggestion.charAt(0);
      final char upper = Character.toUpperCase(first);
      if (first == upper) {
        return suggestion;
      }
      if (suggestion.length() == 1) {
        return String.valueOf(upper);
      }
      return upper + suggestion.substring(1);
    }
    return suggestion;
  }

  private void maybeRerankPrefixMatchingTypedSuggestionsByContext(int typedWordLength) {
    if (typedWordLength < 2) return;

    final int rerankStartIndex = mCorrectSuggestionIndex >= 1 ? mCorrectSuggestionIndex + 1 : 1;
    if (rerankStartIndex >= mSuggestions.size() - 1) return;

    final int rerankEndExclusive =
        Math.min(
            mSuggestions.size(),
            rerankStartIndex + MAX_PREFIX_MATCHING_TYPED_SUGGESTIONS_TO_RERANK_BY_CONTEXT);

    final List<Integer> suggestionIndicesToRerank = new ArrayList<>();
    final List<String> normalizedCandidates = new ArrayList<>();
    for (int suggestionIndex = rerankStartIndex;
        suggestionIndex < rerankEndExclusive;
        suggestionIndex++) {
      final CharSequence candidate = mSuggestions.get(suggestionIndex);
      if (candidate == null) continue;
      if (candidate.length() < typedWordLength) continue;
      final String candidateString = candidate.toString();
      if (!candidateString.regionMatches(
          /* ignoreCase= */ true,
          /* toffset= */ 0,
          mTypedOriginalWord,
          /* ooffset= */ 0,
          typedWordLength)) {
        continue;
      }
      suggestionIndicesToRerank.add(suggestionIndex);
      normalizedCandidates.add(candidateString.toLowerCase(mLocale));
    }

    if (normalizedCandidates.size() <= 1) return;

    final List<String> candidatesToScore = new ArrayList<>(normalizedCandidates);
    final List<String> sortedCandidates =
        mSuggestionsProvider.sortCandidatesByNeuralFirstTokenLogProbIfAvailable(candidatesToScore);
    if (sortedCandidates == null || sortedCandidates.size() != normalizedCandidates.size()) return;

    final boolean neuralReordered = !isSameStringOrder(sortedCandidates, normalizedCandidates);
    final List<String> effectiveSortedCandidates;
    if (neuralReordered) {
      effectiveSortedCandidates = sortedCandidates;
    } else {
      final List<String> contextSorted =
          sortCandidatesByNextWordContextOrderIfAvailable(normalizedCandidates, typedWordLength);
      if (contextSorted == null || contextSorted.size() != normalizedCandidates.size()) return;
      if (isSameStringOrder(contextSorted, normalizedCandidates)) return;
      effectiveSortedCandidates = contextSorted;
    }

    final List<CharSequence> sortedSuggestions = new ArrayList<>(suggestionIndicesToRerank.size());
    final boolean[] used = new boolean[normalizedCandidates.size()];
    for (String sortedCandidate : effectiveSortedCandidates) {
      int matchedIndex = -1;
      for (int i = 0; i < normalizedCandidates.size(); i++) {
        if (used[i]) continue;
        if (TextUtils.equals(normalizedCandidates.get(i), sortedCandidate)) {
          matchedIndex = i;
          break;
        }
      }
      if (matchedIndex == -1) return;
      used[matchedIndex] = true;
      sortedSuggestions.add(mSuggestions.get(suggestionIndicesToRerank.get(matchedIndex)));
    }

    for (int i = 0; i < suggestionIndicesToRerank.size(); i++) {
      mSuggestions.set(suggestionIndicesToRerank.get(i), sortedSuggestions.get(i));
    }
  }

  private boolean isSameStringOrder(@NonNull List<String> a, @NonNull List<String> b) {
    if (a.size() != b.size()) return false;
    for (int i = 0; i < a.size(); i++) {
      if (!TextUtils.equals(a.get(i), b.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Nullable
  private List<String> sortCandidatesByNextWordContextOrderIfAvailable(
      @NonNull List<String> normalizedCandidates, int typedWordLength) {
    final List<CharSequence> nextSuggestionCandidates =
        mNextSuggestionCandidates.isEmpty() ? mNextSuggestions : mNextSuggestionCandidates;
    if (nextSuggestionCandidates.isEmpty()) {
      return null;
    }

    final java.util.HashMap<String, Integer> contextRankByCandidate = new java.util.HashMap<>();
    for (int i = 0; i < nextSuggestionCandidates.size(); i++) {
      final CharSequence candidate = nextSuggestionCandidates.get(i);
      if (candidate == null) continue;
      final String trimmed = candidate.toString().trim();
      if (trimmed.isEmpty()) continue;
      final String normalized = trimmed.toLowerCase(mLocale);
      contextRankByCandidate.putIfAbsent(normalized, i);
    }
    if (contextRankByCandidate.isEmpty()) {
      return null;
    }

    final int maxContextRankToConsider = typedWordLength == 2 ? 12 : Integer.MAX_VALUE;

    final List<Integer> indices = new ArrayList<>(normalizedCandidates.size());
    for (int i = 0; i < normalizedCandidates.size(); i++) {
      indices.add(i);
    }

    indices.sort(
        (leftIndex, rightIndex) -> {
          final String left = normalizedCandidates.get(leftIndex);
          final String right = normalizedCandidates.get(rightIndex);
          final int leftRank = contextRank(contextRankByCandidate, left, maxContextRankToConsider);
          final int rightRank =
              contextRank(contextRankByCandidate, right, maxContextRankToConsider);
          final int cmp = Integer.compare(leftRank, rightRank);
          if (cmp != 0) return cmp;
          return Integer.compare(leftIndex, rightIndex);
        });

    final List<String> sorted = new ArrayList<>(normalizedCandidates.size());
    for (int index : indices) {
      sorted.add(normalizedCandidates.get(index));
    }
    return sorted;
  }

  private int contextRank(
      @NonNull java.util.HashMap<String, Integer> contextRankByCandidate,
      @NonNull String candidate,
      int maxContextRankToConsider) {
    final Integer rank = contextRankByCandidate.get(candidate);
    if (rank == null) return Integer.MAX_VALUE;
    return rank < maxContextRankToConsider ? rank : Integer.MAX_VALUE;
  }

  @Override
  public boolean addWordToUserDictionary(String word) {
    return mSuggestionsProvider.addWordToUserDictionary(word);
  }

  @Override
  public void removeWordFromUserDictionary(String word) {
    mSuggestionsProvider.removeWordFromUserDictionary(word);
  }

  @Override
  public void clearLearningData() {
    mSuggestionsProvider.clearLearningData();
  }

  @Override
  public void setTagsSearcher(@NonNull TagsExtractor extractor) {
    mTagsSearcher = extractor;
  }

  @Override
  public boolean tryToLearnNewWord(CharSequence newWord, AdditionType additionType) {
    return mSuggestionsProvider.tryToLearnNewWord(newWord, additionType.getFrequencyDelta());
  }

  @Override
  public void setContextProfileSafeToggles(@Nullable ContextProfilesStore.SafeToggles safeToggles) {
    mSuggestionsProvider.setContextProfileSafeToggles(safeToggles);
  }

  @Override
  public void setContextProfileWordList(@Nullable String presetId, long generation) {
    mSuggestionsProvider.setContextProfileWordList(presetId, generation);
  }

  @Override
  public void setIncognitoMode(boolean incognitoMode) {
    mSuggestionsProvider.setIncognitoMode(incognitoMode);
  }

  public void setAsyncHybridNeuralListener(@Nullable Runnable listener) {
    mSuggestionsProvider.setAsyncHybridNeuralListener(listener);
  }

  @VisibleForTesting
  public int getHybridNeuralAsyncListenerInvocationCountForTest() {
    return mSuggestionsProvider.getHybridNeuralAsyncListenerInvocationCountForTest();
  }

  @VisibleForTesting
  @NonNull
  public String dumpHybridNeuralAsyncDebugStateForTest() {
    return mSuggestionsProvider.dumpHybridNeuralAsyncDebugStateForTest();
  }

  @VisibleForTesting
  public void resetNeuralInferenceSamplesForTest() {
    mSuggestionsProvider.resetNeuralInferenceSamplesForTest();
  }

  @VisibleForTesting
  @NonNull
  public String dumpNeuralInferenceSamplesForTest() {
    return mSuggestionsProvider.dumpNeuralInferenceSamplesForTest();
  }

  @VisibleForTesting
  public void clearNextWordPipelineDebugStateForTest() {
    mSuggestionsProvider.clearNextWordPipelineDebugStateForTest();
  }

  @VisibleForTesting
  @NonNull
  public String dumpNextWordPipelineDebugStateForTest() {
    return mSuggestionsProvider.dumpNextWordPipelineDebugStateForTest();
  }

  @VisibleForTesting
  @NonNull
  public java.util.Deque<String> getNextWordEngineContextSnapshotForTest() {
    return mSuggestionsProvider.getNextWordEngineContextSnapshotForTest();
  }

  @Override
  public boolean isIncognitoMode() {
    return mSuggestionsProvider.isIncognitoMode();
  }

  @Override
  public void destroy() {
    setAsyncHybridNeuralListener(null);
    closeDictionaries();
    mSuggestionsProvider.destroy();
  }

  private class DictionarySuggestionCallback implements Dictionary.WordCallback {
    private final Dictionary.WordCallback mBasicWordCallback;

    private DictionarySuggestionCallback(Dictionary.WordCallback callback) {
      mBasicWordCallback = callback;
    }

    @Override
    public boolean addWord(
        char[] word, int wordOffset, int wordLength, int frequency, Dictionary from) {
      // Check if it's the same word
      if (SuggestionWordMatcher.compareCaseInsensitive(
          mLowerOriginalWord, word, wordOffset, wordLength)) {
        frequency = FIXED_TYPED_WORD_FREQUENCY;
      } else if (SuggestionWordMatcher.haveSufficientCommonality(
          mCommonalityMaxLengthDiff,
          mCommonalityMaxDistance,
          mLowerOriginalWord,
          word,
          wordOffset,
          wordLength)) {
        frequency += POSSIBLE_FIX_THRESHOLD_FREQUENCY;
      }

      // we are not allowing the main dictionary to suggest fixes for 1 length words
      // (think just the alphabet letter)
      final boolean resetSuggestionsFix =
          mLowerOriginalWord.length() < 2 && mCorrectSuggestionIndex == -1;
      final boolean addWord =
          mBasicWordCallback.addWord(word, wordOffset, wordLength, frequency, from);
      if (resetSuggestionsFix) mCorrectSuggestionIndex = -1;
      return addWord;
    }
  }

  private class SuggestionCallback implements Dictionary.WordCallback {

    @Override
    public boolean addWord(
        char[] word, int wordOffset, int wordLength, int frequency, Dictionary from) {
      if (BuildConfig.DEBUG && TextUtils.isEmpty(mLowerOriginalWord))
        throw new IllegalStateException("mLowerOriginalWord is empty!!");

      int pos;
      final int[] priorities = mPriorities;
      final int prefMaxSuggestions = mPrefMaxSuggestions;

      StringBuilder sb = getStringBuilderFromPool(word, wordOffset, wordLength);
      if (shouldFilterAllCapsAcronymSuggestion(sb)) {
        mStringPool.add(sb);
        return true;
      }

      if (TextUtils.equals(mTypedOriginalWord, sb)) {
        frequency = VALID_TYPED_WORD_FREQUENCY;
        pos = 0;
      } else {
        // Check the last one's priority and bail
        if (priorities[prefMaxSuggestions - 1] >= frequency) return true;
        pos = 1; // never check with the first (typed) word
        // looking for the ordered position to insert the new word
        while (pos < prefMaxSuggestions) {
          if (priorities[pos] < frequency
              || (priorities[pos] == frequency && wordLength < mSuggestions.get(pos).length())) {
            break;
          }
          pos++;
        }

        if (pos >= prefMaxSuggestions) {
          // we reached a position which is outside the max, we'll skip
          // this word and ask for more (maybe next one will have higher frequency)
          return true;
        }

        System.arraycopy(priorities, pos, priorities, pos + 1, prefMaxSuggestions - pos - 1);
        mSuggestions.add(pos, sb);
        priorities[pos] = frequency;
      }
      // should we mark this as a possible suggestion fix?
      if (frequency >= POSSIBLE_FIX_THRESHOLD_FREQUENCY) {
        // this a suggestion that can be a fix
        if (mCorrectSuggestionIndex < 0 || priorities[mCorrectSuggestionIndex] < frequency) {
          mCorrectSuggestionIndex = pos;
        }
      }

      // removing excess suggestion
      IMEUtil.tripSuggestions(mSuggestions, prefMaxSuggestions, mStringPool);
      return true; // asking for more
    }
  }

  @NonNull
  private StringBuilder getStringBuilderFromPool(char[] word, int wordOffset, int wordLength) {
    int poolSize = mStringPool.size();
    StringBuilder sb =
        poolSize > 0
            ? (StringBuilder) mStringPool.remove(poolSize - 1)
            : new StringBuilder(Dictionary.MAX_WORD_LENGTH);
    sb.setLength(0);
    if (mIsAllUpperCase) {
      sb.append(new String(word, wordOffset, wordLength).toUpperCase(mLocale));
    } else if (mIsFirstCharCapitalized) {
      sb.append(Character.toUpperCase(word[wordOffset]));
      if (wordLength > 1) {
        sb.append(word, wordOffset + 1, wordLength - 1);
      }
    } else {
      sb.append(word, wordOffset, wordLength);
    }
    return sb;
  }
}
