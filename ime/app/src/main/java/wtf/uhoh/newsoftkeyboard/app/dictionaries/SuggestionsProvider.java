package wtf.uhoh.newsoftkeyboard.app.dictionaries;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import io.reactivex.disposables.CompositeDisposable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.BuildConfig;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.content.ContactsDictionary;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.sqlite.ContextProfileWordListDictionary;
import wtf.uhoh.newsoftkeyboard.dictionaries.Dictionary;
import wtf.uhoh.newsoftkeyboard.dictionaries.DictionaryBackgroundLoader;
import wtf.uhoh.newsoftkeyboard.dictionaries.EditableDictionary;
import wtf.uhoh.newsoftkeyboard.dictionaries.KeyCodesProvider;
import wtf.uhoh.newsoftkeyboard.nextword.NextWordSuggestions;
import wtf.uhoh.newsoftkeyboard.nextword.pipeline.NextWordSuggestionsPipeline;
import wtf.uhoh.newsoftkeyboard.nextword.prediction.NextWordPredictionEngines;
import wtf.uhoh.newsoftkeyboard.prefs.RxSharedPrefs;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;

@SuppressWarnings("this-escape")
public class SuggestionsProvider {

  private static final String TAG = "SuggestionsProvider";

  @NonNull private final Context mContext;
  @NonNull private final SuggestionsDictionariesManager mDictionariesManager;

  @NonNull
  private final List<NextWordSuggestions> mUserNextWordDictionariesForPipeline = new ArrayList<>();

  @NonNull
  private ContextProfilesStore.SafeToggles mContextSafeToggles =
      ContextProfilesStore.SafeToggles.DEFAULT;

  @NonNull
  private NextWordSuggestionsPipeline.Config mNextWordConfig =
      new NextWordSuggestionsPipeline.Config(
          /* enabled= */ false,
          /* alsoSuggestNextPunctuations= */ false,
          /* maxNextWordSuggestionsCount= */ 3,
          /* minWordUsage= */ 5);

  private boolean mIncognitoMode;
  private final CompositeDisposable mPrefsDisposables = new CompositeDisposable();

  @NonNull private final NextWordPredictionEngines mPredictionEngines;
  @NonNull private final NextWordSuggestionsPipeline mNextWordPipeline;

  public SuggestionsProvider(@NonNull Context context) {
    mContext = context.getApplicationContext();
    mDictionariesManager =
        new SuggestionsDictionariesManager(
            mContext,
            this::createUserDictionaryForLocale,
            this::createContextProfileWordListDictionaryForLocale,
            this::createRealContactsDictionary);
    final RxSharedPrefs rxSharedPrefs = NskApplicationBase.prefs(mContext);
    mPredictionEngines =
        new NextWordPredictionEngines(
            mContext, rxSharedPrefs, TAG, BuildConfig.DEBUG, BuildConfig.TESTING_BUILD);
    mNextWordPipeline =
        new NextWordSuggestionsPipeline(
            mPredictionEngines,
            mUserNextWordDictionariesForPipeline,
            mDictionariesManager::contactsNextWordDictionary,
            mDictionariesManager.initialSuggestionsList(),
            BuildConfig.TESTING_BUILD);
    SuggestionsProviderPrefsBinder.wire(
        rxSharedPrefs,
        mPrefsDisposables,
        mDictionariesManager::setQuickFixesEnabled,
        mDictionariesManager::setQuickFixesSecondDisabled,
        mDictionariesManager::setContactsDictionaryEnabled,
        mDictionariesManager::setUserDictionaryEnabled,
        this::onPredictionEngineModeChanged,
        this::setPredictionContextWindowWords,
        this::setNextWordAggressiveness,
        this::setNextWordDictionaryType);
  }

  private void setPredictionContextWindowWords(int contextWindowWords) {
    mPredictionEngines.setContextWindowWords(contextWindowWords);
  }

  public void setContextProfileSafeToggles(@Nullable ContextProfilesStore.SafeToggles safeToggles) {
    final ContextProfilesStore.SafeToggles normalized =
        safeToggles == null ? ContextProfilesStore.SafeToggles.DEFAULT : safeToggles;
    if (mContextSafeToggles.equals(normalized)) return;
    mContextSafeToggles = normalized;
    mDictionariesManager.setContextProfileSafeToggles(normalized);
    syncNextWordUserDictionariesForPipeline();
  }

  public void setContextProfileWordList(@Nullable String presetId, long generation) {
    mDictionariesManager.setContextProfileWordList(presetId, generation);
  }

  private void syncNextWordUserDictionariesForPipeline() {
    mUserNextWordDictionariesForPipeline.clear();
    if (mContextSafeToggles.disableUserDictionary) return;
    mUserNextWordDictionariesForPipeline.addAll(mDictionariesManager.userNextWordDictionaries());
  }

  private void onPredictionEngineModeChanged(@NonNull String modeValue) {
    mDictionariesManager.invalidateSetupHash();
    mPredictionEngines.updatePredictionEngine(modeValue);
  }

  private void setNextWordAggressiveness(@NonNull String aggressiveness) {
    int maxNextWordSuggestionsCount;
    int minWordUsage;
    switch (aggressiveness) {
      case "medium_aggressiveness":
        // This value is intentionally larger than the UI max-suggestions count so the pipeline
        // can collect a broader candidate pool for reranking and for prefix-conditioned UX while
        // composing (next-word candidates injected into typed suggestions).
        maxNextWordSuggestionsCount = 24;
        minWordUsage = 3;
        break;
      case "maximum_aggressiveness":
        maxNextWordSuggestionsCount = 48;
        minWordUsage = 1;
        break;
      case "minimal_aggressiveness":
      default:
        maxNextWordSuggestionsCount = 8;
        minWordUsage = 5;
        break;
    }
    mNextWordConfig =
        new NextWordSuggestionsPipeline.Config(
            mNextWordConfig.enabled,
            mNextWordConfig.alsoSuggestNextPunctuations,
            maxNextWordSuggestionsCount,
            minWordUsage);
  }

  int nextWordCandidatePoolLimit() {
    return mNextWordConfig.maxNextWordSuggestionsCount;
  }

  private void setNextWordDictionaryType(@NonNull String type) {
    boolean enabled;
    boolean alsoSuggestNextPunctuations;
    switch (type) {
      case "off":
        enabled = false;
        alsoSuggestNextPunctuations = false;
        break;
      case "words_punctuations":
        enabled = true;
        alsoSuggestNextPunctuations = true;
        break;
      case "word":
      default:
        enabled = true;
        alsoSuggestNextPunctuations = false;
        break;
    }
    mNextWordConfig =
        new NextWordSuggestionsPipeline.Config(
            enabled,
            alsoSuggestNextPunctuations,
            mNextWordConfig.maxNextWordSuggestionsCount,
            mNextWordConfig.minWordUsage);
  }

  public void setupSuggestionsForKeyboard(
      @NonNull List<DictionaryAddOnAndBuilder> dictionaryBuilders,
      @NonNull DictionaryBackgroundLoader.Listener cb) {
    final int newSetupHashCode =
        SuggestionsDictionariesManager.calculateHashCodeForBuilders(dictionaryBuilders);
    if (newSetupHashCode == mDictionariesManager.currentSetupHashCode()) {
      // no need to load, since we have all the same dictionaries,
      // but, we do need to notify the dictionary loaded listeners.
      mDictionariesManager.simulateDictionaryLoad(cb);
      return;
    }

    close();

    mDictionariesManager.setCurrentSetupHashCode(newSetupHashCode);
    mDictionariesManager.buildDictionaries(dictionaryBuilders, cb, BuildConfig.TESTING_BUILD);
    syncNextWordUserDictionariesForPipeline();
    mPredictionEngines.activatePresageIfNeeded();
    mPredictionEngines.warmUpNeuralIfNeeded();
  }

  @NonNull
  @VisibleForTesting
  protected ContactsDictionary createRealContactsDictionary() {
    return new ContactsDictionary(mContext);
  }

  @NonNull
  protected UserDictionary createUserDictionaryForLocale(@NonNull String locale) {
    return new UserDictionary(mContext, locale);
  }

  @NonNull
  protected EditableDictionary createContextProfileWordListDictionaryForLocale(
      @NonNull String presetId, @NonNull String locale) {
    return new ContextProfileWordListDictionary(mContext, presetId, locale);
  }

  public void removeWordFromUserDictionary(String word) {
    mDictionariesManager.removeWordFromUserDictionary(word);
  }

  public void clearLearningData() {
    mDictionariesManager.clearLearningData();
    mPredictionEngines.resetSentence();
  }

  public boolean addWordToUserDictionary(String word) {
    if (mIncognitoMode) return false;
    return mDictionariesManager.addWordToUserDictionary(word);
  }

  public boolean isValidWord(CharSequence word) {
    return mDictionariesManager.isValidWord(word);
  }

  public void setIncognitoMode(boolean incognitoMode) {
    mIncognitoMode = incognitoMode;
    mPredictionEngines.setIncognitoMode(incognitoMode);
  }

  public boolean isIncognitoMode() {
    return mIncognitoMode;
  }

  public void close() {
    // Closing dictionaries is a resource-management action and should not implicitly reset the
    // next-word sentence context. Sentence/context resets are handled explicitly when switching
    // editors (see IME lifecycle hooks).
    mDictionariesManager.closeDictionariesForShutdown(() -> {});
    mPredictionEngines.hibernate();
    mUserNextWordDictionariesForPipeline.clear();
  }

  public void destroy() {
    close();
    mPrefsDisposables.dispose();
  }

  public void resetNextWordSentence() {
    mNextWordPipeline.resetSentence();
  }

  /**
   * Hard-clears the neural completion context window. Called on editor change so the larger,
   * soft-reset neural context (#5) never carries text across fields.
   */
  public void resetNeuralCompletionContext() {
    mPredictionEngines.resetNeuralCompletionContext();
  }

  public void getSuggestions(KeyCodesProvider wordComposer, Dictionary.WordCallback wordCallback) {
    mDictionariesManager.getSuggestions(wordComposer, wordCallback);
  }

  public void getAbbreviations(
      KeyCodesProvider wordComposer, Dictionary.WordCallback wordCallback) {
    if (mContextSafeToggles.disableQuickFixes) return;
    mDictionariesManager.getAbbreviations(wordComposer, wordCallback);
  }

  public void getAutoText(KeyCodesProvider wordComposer, Dictionary.WordCallback wordCallback) {
    if (mContextSafeToggles.disableQuickFixes) return;
    mDictionariesManager.getAutoText(wordComposer, wordCallback);
  }

  public void getNextWords(
      String currentWord, Collection<CharSequence> suggestionsHolder, int maxSuggestions) {
    if (mContextSafeToggles.disableNextWordSuggestions) return;
    mNextWordPipeline.appendNextWords(
        currentWord, suggestionsHolder, maxSuggestions, mIncognitoMode, mNextWordConfig);
  }

  public void notifyWordCommitted(@NonNull String committedWord) {
    if (mContextSafeToggles.disableNextWordSuggestions) return;
    if (!mNextWordConfig.enabled) return;
    mNextWordPipeline.notifyWordCommitted(committedWord, mIncognitoMode);
  }

  /**
   * Seeds the next-word prediction engines' in-memory context window from existing editor text.
   *
   * <p>This is intentionally <b>not</b> wired into next-word dictionaries (no
   * learning/persistence).
   */
  public void seedNextWordEngineContextTokens(@NonNull Collection<String> contextTokens) {
    if (mIncognitoMode) return;
    if (mContextSafeToggles.disableNextWordSuggestions) return;
    if (!mNextWordConfig.enabled) return;
    mPredictionEngines.seedContextTokens(contextTokens);
  }

  public boolean isPresageEnabled() {
    return mPredictionEngines.isPresageEnabled();
  }

  /** Returns true when the neural predictor should be considered active in the pipeline. */
  public boolean isNeuralEnabled() {
    return mPredictionEngines.isNeuralEnabled();
  }

  public void setAsyncHybridNeuralListener(@Nullable Runnable listener) {
    mPredictionEngines.setAsyncHybridNeuralListener(listener);
  }

  @VisibleForTesting
  public int getHybridNeuralAsyncListenerInvocationCountForTest() {
    return mPredictionEngines.getHybridNeuralAsyncListenerInvocationCountForTest();
  }

  @VisibleForTesting
  @NonNull
  public String dumpHybridNeuralAsyncDebugStateForTest() {
    return mPredictionEngines.dumpHybridNeuralAsyncDebugStateForTest();
  }

  @VisibleForTesting
  public void resetNeuralInferenceSamplesForTest() {
    mPredictionEngines.resetNeuralInferenceSamplesForTest();
  }

  @VisibleForTesting
  @NonNull
  public String dumpNeuralInferenceSamplesForTest() {
    return mPredictionEngines.dumpNeuralInferenceSamplesForTest();
  }

  @VisibleForTesting
  public void clearNextWordPipelineDebugStateForTest() {
    mNextWordPipeline.clearNextWordPipelineDebugStateForTest();
  }

  @VisibleForTesting
  @NonNull
  public String dumpNextWordPipelineDebugStateForTest() {
    return mNextWordPipeline.dumpNextWordPipelineDebugStateForTest();
  }

  @VisibleForTesting
  @NonNull
  public java.util.Deque<String> getNextWordEngineContextSnapshotForTest() {
    return mPredictionEngines.getContextSnapshot();
  }

  @NonNull
  public List<String> sortCandidatesByNeuralFirstTokenLogProbIfAvailable(
      @NonNull List<String> candidates) {
    return mPredictionEngines.sortCandidatesByNeuralFirstTokenLogProbIfAvailable(candidates);
  }

  /**
   * Returns ready neural prefix-completions for the in-progress word, or schedules an async
   * computation and returns empty (never blocks on inference). See {@link
   * NextWordPredictionEngines#getOrScheduleWordCompletions}.
   */
  @NonNull
  public List<String> getOrScheduleWordCompletions(@NonNull String prefix, int maxResults) {
    return mPredictionEngines.getOrScheduleWordCompletions(prefix, maxResults);
  }

  public boolean tryToLearnNewWord(CharSequence newWord, int frequencyDelta) {
    if (mIncognitoMode
        || !mNextWordConfig.enabled
        || mContextSafeToggles.disableNextWordSuggestions) return false;
    return mDictionariesManager.tryToLearnNewWord(newWord, frequencyDelta);
  }
}
