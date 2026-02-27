package wtf.uhoh.newsoftkeyboard.nextword.pipeline;

import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import org.json.JSONArray;
import org.json.JSONObject;
import wtf.uhoh.newsoftkeyboard.nextword.NextWordSuggestions;
import wtf.uhoh.newsoftkeyboard.nextword.prediction.NextWordPredictionEngines;

/**
 * Orchestrates next-word suggestions across on-device engines and legacy next-word dictionaries.
 */
public final class NextWordSuggestionsPipeline {

  private static final int MAX_LEGACY_WORDS_WHEN_ENGINES_PRESENT = 2;
  private static final int MAX_CONTACT_WORDS_WHEN_ENGINES_PRESENT = 1;

  private final boolean mEnableTestLogging;
  private final Object mDebugLock = new Object();
  @Nullable private String mLastDebugStateForTest;

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
      @NonNull List<String> initialPunctuationSuggestions,
      boolean enableTestLogging) {
    mPredictionEngines = predictionEngines;
    mUserNextWordDictionaries = userNextWordDictionaries;
    mContactsNextWordDictionary = contactsNextWordDictionary;
    mInitialPunctuationSuggestions = initialPunctuationSuggestions;
    mEnableTestLogging = enableTestLogging;
  }

  public void resetSentence() {
    for (NextWordSuggestions nextWordSuggestions : mUserNextWordDictionaries) {
      nextWordSuggestions.resetSentence();
    }
    mContactsNextWordDictionary.get().resetSentence();
    mPredictionEngines.resetSentence();
    clearNextWordPipelineDebugStateForTest();
  }

  public void notifyWordCommitted(@NonNull String committedWord, boolean incognitoMode) {
    if (committedWord.isEmpty()) return;
    mPredictionEngines.notifyWordCommitted(committedWord);
    if (incognitoMode) return;

    for (NextWordSuggestions nextWordDictionary : mUserNextWordDictionaries) {
      nextWordDictionary.notifyNextTypedWord(committedWord);
    }
    mContactsNextWordDictionary.get().notifyNextTypedWord(committedWord);
  }

  public void appendNextWords(
      @NonNull String currentWord,
      @NonNull Collection<CharSequence> suggestionsHolder,
      int maxSuggestions,
      boolean incognitoMode,
      @NonNull Config config) {
    if (!config.enabled) return;

    // Collect a larger candidate pool than we can display so the neural scorer + reranker can
    // choose a better top row and reduce repetition/stopword-only strips.
    final int candidatePoolLimit = Math.max(maxSuggestions, config.maxNextWordSuggestionsCount);

    final List<String> engineWords = new java.util.ArrayList<>(Math.max(0, candidatePoolLimit));
    final List<String> legacyWords = new java.util.ArrayList<>(Math.max(0, candidatePoolLimit));
    final List<String> contactWords = new java.util.ArrayList<>(Math.max(0, candidatePoolLimit));
    final java.util.ArrayList<String> punctuationsAddedForTest = new java.util.ArrayList<>();

    final java.util.ArrayList<CharSequence> engineHolder =
        new java.util.ArrayList<>(candidatePoolLimit);
    final NextWordPredictionEngines.Outcome engineOutcome =
        mPredictionEngines.appendNextWords(
            currentWord,
            engineHolder,
            candidatePoolLimit,
            incognitoMode,
            config.maxNextWordSuggestionsCount);
    for (CharSequence s : engineHolder) {
      if (s == null) continue;
      engineWords.add(s.toString());
    }

    final boolean enginesPresent = !engineWords.isEmpty();
    final boolean allowPersonalSources = !incognitoMode;
    if (allowPersonalSources) {
      final int legacyLimit =
          engineOutcome.shouldIncludeLegacyNextWords
              ? candidatePoolLimit
              : MAX_LEGACY_WORDS_WHEN_ENGINES_PRESENT;
      collectLegacyNextWords(
          currentWord,
          legacyWords,
          legacyLimit,
          config.maxNextWordSuggestionsCount,
          config.minWordUsage);

      final int contactLimit =
          enginesPresent ? MAX_CONTACT_WORDS_WHEN_ENGINES_PRESENT : candidatePoolLimit;
      collectContactsNextWords(
          currentWord,
          contactWords,
          contactLimit,
          config.maxNextWordSuggestionsCount,
          config.minWordUsage);
    }

    final List<String> scoredLegacyWords =
        mPredictionEngines.sortCandidatesByNeuralFirstTokenLogProbIfAvailable(legacyWords);
    final List<String> scoredContactWords =
        mPredictionEngines.sortCandidatesByNeuralFirstTokenLogProbIfAvailable(contactWords);

    final java.util.ArrayList<String> combined =
        new java.util.ArrayList<>(
            engineWords.size() + scoredLegacyWords.size() + scoredContactWords.size());

    // When engines are producing normalized candidates, still surface a small amount of legacy
    // personalization so HYBRID doesn't feel like "neural or n-gram only".
    int engineIndex = 0;
    int legacyIndex = 0;
    int contactIndex = 0;
    if (engineIndex < engineWords.size()) {
      combined.add(engineWords.get(engineIndex++));
    }
    if (legacyIndex < scoredLegacyWords.size()) {
      combined.add(scoredLegacyWords.get(legacyIndex++));
    }
    if (engineIndex < engineWords.size()) {
      combined.add(engineWords.get(engineIndex++));
    }
    if (contactIndex < scoredContactWords.size()) {
      combined.add(scoredContactWords.get(contactIndex++));
    }
    while (engineIndex < engineWords.size()) {
      combined.add(engineWords.get(engineIndex++));
    }
    while (legacyIndex < scoredLegacyWords.size()) {
      combined.add(scoredLegacyWords.get(legacyIndex++));
    }
    while (contactIndex < scoredContactWords.size()) {
      combined.add(scoredContactWords.get(contactIndex++));
    }

    final java.util.List<String> normalized = normalizeAndFilterImmediateRepeat(combined);
    final java.util.List<String> scored =
        mPredictionEngines.sortCandidatesByNeuralFirstTokenLogProbIfAvailable(normalized);
    final java.util.Deque<String> contextSnapshot = mPredictionEngines.getContextSnapshot();
    final java.util.List<String> reranked =
        contextSnapshot.isEmpty() ? scored : CandidateReranker.rerank(contextSnapshot, scored);

    suggestionsHolder.clear();
    int remainingSuggestions = maxSuggestions;
    for (String s : reranked) {
      if (s == null || s.isEmpty()) continue;
      suggestionsHolder.add(s);
      remainingSuggestions--;
      if (remainingSuggestions == 0) break;
    }

    if (remainingSuggestions <= 0) {
      recordDebugStateForTest(
          config,
          maxSuggestions,
          candidatePoolLimit,
          engineOutcome,
          engineWords,
          legacyWords,
          contactWords,
          punctuationsAddedForTest,
          suggestionsHolder);
      return;
    }
    if (config.alsoSuggestNextPunctuations) {
      for (String punctuation : mInitialPunctuationSuggestions) {
        suggestionsHolder.add(punctuation);
        if (mEnableTestLogging) punctuationsAddedForTest.add(punctuation);
        remainingSuggestions--;
        if (remainingSuggestions == 0) {
          recordDebugStateForTest(
              config,
              maxSuggestions,
              candidatePoolLimit,
              engineOutcome,
              engineWords,
              legacyWords,
              contactWords,
              punctuationsAddedForTest,
              suggestionsHolder);
          return;
        }
      }
    }

    recordDebugStateForTest(
        config,
        maxSuggestions,
        candidatePoolLimit,
        engineOutcome,
        engineWords,
        legacyWords,
        contactWords,
        punctuationsAddedForTest,
        suggestionsHolder);
  }

  @VisibleForTesting
  public void clearNextWordPipelineDebugStateForTest() {
    if (!mEnableTestLogging) return;
    synchronized (mDebugLock) {
      mLastDebugStateForTest = null;
    }
  }

  @VisibleForTesting
  @NonNull
  public String dumpNextWordPipelineDebugStateForTest() {
    if (!mEnableTestLogging) return "{}";
    synchronized (mDebugLock) {
      return mLastDebugStateForTest == null ? "{}" : mLastDebugStateForTest;
    }
  }

  private void recordDebugStateForTest(
      @NonNull Config config,
      int maxSuggestions,
      int candidatePoolLimit,
      @NonNull NextWordPredictionEngines.Outcome engineOutcome,
      @NonNull List<String> engineWords,
      @NonNull List<String> legacyWords,
      @NonNull List<String> contactWords,
      @NonNull List<String> punctuationAdded,
      @NonNull Collection<CharSequence> finalSuggestions) {
    if (!mEnableTestLogging) return;
    try {
      final NextWordPredictionEngines.EngineCandidateSourcesForTest engineSources =
          mPredictionEngines.snapshotEngineCandidateSourcesForTest();

      final java.util.HashSet<String> neural =
          new java.util.HashSet<>(engineSources.neuralCandidates);
      final java.util.HashSet<String> presage =
          new java.util.HashSet<>(engineSources.presageCandidates);
      final java.util.HashSet<String> legacy = new java.util.HashSet<>(legacyWords);
      final java.util.HashSet<String> contacts = new java.util.HashSet<>(contactWords);
      final java.util.HashSet<String> punctuation = new java.util.HashSet<>(punctuationAdded);

      final JSONObject root = new JSONObject();
      root.put("uptimeMs", SystemClock.uptimeMillis());
      root.put("engineMode", engineSources.mode);
      root.put("maxSuggestions", maxSuggestions);
      root.put("candidatePoolLimit", candidatePoolLimit);
      final JSONArray contextTokens = new JSONArray();
      for (String t : mPredictionEngines.getContextSnapshot()) {
        if (t == null) continue;
        contextTokens.put(t);
      }
      root.put("contextTokens", contextTokens);

      final JSONObject cfg = new JSONObject();
      cfg.put("enabled", config.enabled);
      cfg.put("alsoSuggestNextPunctuations", config.alsoSuggestNextPunctuations);
      cfg.put("maxNextWordSuggestionsCount", config.maxNextWordSuggestionsCount);
      cfg.put("minWordUsage", config.minWordUsage);
      root.put("config", cfg);

      final JSONObject engine = new JSONObject();
      final JSONArray engineOut = new JSONArray();
      for (String w : engineWords) engineOut.put(w);
      engine.put("words", engineOut);
      final JSONArray presageOut = new JSONArray();
      for (String w : engineSources.presageCandidates) presageOut.put(w);
      engine.put("presageCandidates", presageOut);
      final JSONArray neuralOut = new JSONArray();
      for (String w : engineSources.neuralCandidates) neuralOut.put(w);
      engine.put("neuralCandidates", neuralOut);
      engine.put("shouldIncludeLegacyNextWords", engineOutcome.shouldIncludeLegacyNextWords);
      root.put("engine", engine);

      final JSONArray legacyOut = new JSONArray();
      for (String w : legacyWords) legacyOut.put(w);
      root.put("legacyWords", legacyOut);
      final JSONArray contactsOut = new JSONArray();
      for (String w : contactWords) contactsOut.put(w);
      root.put("contactWords", contactsOut);
      final JSONArray punctuationOut = new JSONArray();
      for (String w : punctuationAdded) punctuationOut.put(w);
      root.put("punctuationAdded", punctuationOut);

      final JSONArray finalOut = new JSONArray();
      final JSONArray finalSources = new JSONArray();
      for (CharSequence s : finalSuggestions) {
        if (s == null) continue;
        final String text = s.toString();
        if (text.isEmpty()) continue;
        finalOut.put(text);

        final String source;
        if (punctuation.contains(text)) source = "punctuation";
        else if (neural.contains(text)) source = "neural";
        else if (presage.contains(text)) source = "ngram";
        else if (contacts.contains(text)) source = "contacts";
        else if (legacy.contains(text)) source = "legacy";
        else source = "unknown";
        finalSources.put(source);
      }
      root.put("finalSuggestions", finalOut);
      root.put("finalSuggestionSources", finalSources);

      synchronized (mDebugLock) {
        mLastDebugStateForTest = root.toString();
      }
    } catch (Exception ignored) {
      // best-effort only
    }
  }

  private void collectLegacyNextWords(
      @NonNull String currentWord,
      @NonNull java.util.List<String> out,
      int maxSuggestions,
      int maxNextWordSuggestionsCount,
      int minWordUsage) {
    for (NextWordSuggestions nextWordDictionary : mUserNextWordDictionaries) {
      for (String nextWordSuggestion :
          nextWordDictionary.getNextWords(currentWord, maxNextWordSuggestionsCount, minWordUsage)) {
        out.add(nextWordSuggestion);
        maxSuggestions--;
        if (maxSuggestions == 0) return;
      }
    }
  }

  private void collectContactsNextWords(
      @NonNull String currentWord,
      @NonNull java.util.List<String> out,
      int maxSuggestions,
      int maxNextWordSuggestionsCount,
      int minWordUsage) {
    for (String nextWordSuggestion :
        mContactsNextWordDictionary
            .get()
            .getNextWords(currentWord, maxNextWordSuggestionsCount, minWordUsage)) {
      out.add(nextWordSuggestion);
      maxSuggestions--;
      if (maxSuggestions == 0) return;
    }
  }

  @NonNull
  private java.util.List<String> normalizeAndFilterImmediateRepeat(
      @NonNull java.util.List<String> rawCandidates) {
    final java.util.Deque<String> context = mPredictionEngines.getContextSnapshot();
    final String last = context.peekLast();
    if (last == null || last.isEmpty()) {
      return CandidateNormalizer.normalizeForNextWordUx(context, rawCandidates);
    }

    final java.util.List<String> filtered =
        CandidateNormalizer.normalizeForNextWordUx(
            context,
            rawCandidates,
            java.util.Collections.singleton(last.toLowerCase(java.util.Locale.ROOT)));
    if (!filtered.isEmpty()) {
      return filtered;
    }
    // If everything is filtered out, fall back to normalized candidates rather than returning an
    // empty strip.
    return CandidateNormalizer.normalizeForNextWordUx(context, rawCandidates);
  }
}
