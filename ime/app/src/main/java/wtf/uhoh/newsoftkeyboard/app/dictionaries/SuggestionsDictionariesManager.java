package wtf.uhoh.newsoftkeyboard.app.dictionaries;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import io.reactivex.disposables.CompositeDisposable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.content.ContactsDictionary;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.sqlite.AbbreviationsDictionary;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.sqlite.AutoDictionary;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.dictionaries.AutoText;
import wtf.uhoh.newsoftkeyboard.dictionaries.Dictionary;
import wtf.uhoh.newsoftkeyboard.dictionaries.DictionaryBackgroundLoader;
import wtf.uhoh.newsoftkeyboard.dictionaries.EditableDictionary;
import wtf.uhoh.newsoftkeyboard.dictionaries.KeyCodesProvider;
import wtf.uhoh.newsoftkeyboard.nextword.NextWordDictionary;
import wtf.uhoh.newsoftkeyboard.nextword.NextWordSuggestions;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;

final class SuggestionsDictionariesManager {

  private static final String TAG = "SuggestionsProvider";

  @NonNull private final Context mContext;
  @NonNull private final Function<String, UserDictionary> mUserDictionaryFactory;

  @NonNull
  private final BiFunction<String, String, EditableDictionary> mContextProfileWordListFactory;

  @NonNull private final Supplier<ContactsDictionary> mContactsDictionaryFactory;

  @NonNull private final List<String> mInitialSuggestionsList = new ArrayList<>();
  @NonNull private final List<Dictionary> mMainDictionary = new ArrayList<>();
  @NonNull private final List<EditableDictionary> mUserDictionary = new ArrayList<>();
  @NonNull private final List<EditableDictionary> mContextProfileWordList = new ArrayList<>();
  @NonNull private final List<NextWordSuggestions> mUserNextWordDictionary = new ArrayList<>();
  @NonNull private final List<Dictionary> mAbbreviationDictionary = new ArrayList<>();
  @NonNull private final List<AutoText> mQuickFixesAutoText = new ArrayList<>();

  @NonNull private CompositeDisposable mDictionaryDisposables = new CompositeDisposable();

  @NonNull
  private CompositeDisposable mContextProfileWordListDisposables = new CompositeDisposable();

  private int mCurrentSetupHashCode;
  private boolean mQuickFixesEnabled;
  private boolean mQuickFixesSecondDisabled;
  private boolean mContactsDictionaryEnabled;
  private boolean mUserDictionaryEnabled;
  private boolean mContextAllowsContactsDictionary = true;
  private boolean mContextAllowsUserDictionary = true;
  private boolean mContextAllowsMainDictionary = true;
  @Nullable private String mContextProfileWordListPresetId;
  private long mContextProfileWordListGeneration;
  @NonNull private final List<String> mCurrentLocales = new ArrayList<>();

  @NonNull private EditableDictionary mAutoDictionary = SuggestionsProviderNulls.NULL_DICTIONARY;
  @NonNull private Dictionary mContactsDictionary = SuggestionsProviderNulls.NULL_DICTIONARY;

  @NonNull
  private NextWordSuggestions mContactsNextWordDictionary =
      SuggestionsProviderNulls.NULL_NEXT_WORD_SUGGESTIONS;

  private final ContactsDictionaryLoaderListener mContactsDictionaryListener =
      new ContactsDictionaryLoaderListener(
          () -> mContactsDictionary,
          () -> {
            mContactsDictionary = SuggestionsProviderNulls.NULL_DICTIONARY;
            mContactsNextWordDictionary = SuggestionsProviderNulls.NULL_NEXT_WORD_SUGGESTIONS;
          });

  SuggestionsDictionariesManager(
      @NonNull Context context,
      @NonNull Function<String, UserDictionary> userDictionaryFactory,
      @NonNull BiFunction<String, String, EditableDictionary> contextProfileWordListFactory,
      @NonNull Supplier<ContactsDictionary> contactsDictionaryFactory) {
    mContext = context.getApplicationContext();
    mUserDictionaryFactory = userDictionaryFactory;
    mContextProfileWordListFactory = contextProfileWordListFactory;
    mContactsDictionaryFactory = contactsDictionaryFactory;
  }

  @VisibleForTesting
  static int calculateHashCodeForBuilders(List<DictionaryAddOnAndBuilder> dictionaryBuilders) {
    return Arrays.hashCode(dictionaryBuilders.toArray());
  }

  int currentSetupHashCode() {
    return mCurrentSetupHashCode;
  }

  void setCurrentSetupHashCode(int value) {
    mCurrentSetupHashCode = value;
  }

  void invalidateSetupHash() {
    mCurrentSetupHashCode = 0;
  }

  @NonNull
  List<NextWordSuggestions> userNextWordDictionaries() {
    return mUserNextWordDictionary;
  }

  @NonNull
  List<String> initialSuggestionsList() {
    return mInitialSuggestionsList;
  }

  @NonNull
  NextWordSuggestions contactsNextWordDictionary() {
    return mContextAllowsContactsDictionary
        ? mContactsNextWordDictionary
        : SuggestionsProviderNulls.NULL_NEXT_WORD_SUGGESTIONS;
  }

  void setContextProfileSafeToggles(@NonNull ContextProfilesStore.SafeToggles safeToggles) {
    mContextAllowsContactsDictionary = !safeToggles.disableContactsDictionary;
    mContextAllowsUserDictionary = !safeToggles.disableUserDictionary;
    mContextAllowsMainDictionary = !safeToggles.disableMainDictionary;
  }

  void setContextProfileWordList(@Nullable String presetId, long generation) {
    final String normalized = presetId == null ? null : presetId.trim();
    final boolean samePreset = Objects.equals(mContextProfileWordListPresetId, normalized);
    final boolean sameGeneration = mContextProfileWordListGeneration == generation;
    if (samePreset && sameGeneration) return;

    mContextProfileWordListPresetId =
        (normalized == null || normalized.isEmpty()) ? null : normalized;
    mContextProfileWordListGeneration = generation;

    reloadContextProfileWordListDictionaries();
  }

  private void reloadContextProfileWordListDictionaries() {
    closeContextProfileWordListDictionaries();

    final String presetId = mContextProfileWordListPresetId;
    if (presetId == null) return;

    for (String locale : mCurrentLocales) {
      final EditableDictionary dictionary = mContextProfileWordListFactory.apply(presetId, locale);
      mContextProfileWordList.add(dictionary);
      mContextProfileWordListDisposables.add(
          DictionaryBackgroundLoader.loadDictionaryInBackground(
              DictionaryBackgroundLoader.SILENT_LISTENER, dictionary));
    }
  }

  private void closeContextProfileWordListDictionaries() {
    for (EditableDictionary dictionary : mContextProfileWordList) {
      dictionary.close();
    }
    mContextProfileWordList.clear();
    mContextProfileWordListDisposables.dispose();
    mContextProfileWordListDisposables = new CompositeDisposable();
  }

  void setQuickFixesEnabled(boolean enabled) {
    invalidateSetupHash();
    mQuickFixesEnabled = enabled;
  }

  void setQuickFixesSecondDisabled(boolean disabled) {
    invalidateSetupHash();
    mQuickFixesSecondDisabled = disabled;
  }

  void setContactsDictionaryEnabled(boolean enabled) {
    invalidateSetupHash();
    mContactsDictionaryEnabled = enabled;
    if (!enabled) {
      mContactsDictionary.close();
      mContactsDictionary = SuggestionsProviderNulls.NULL_DICTIONARY;
      mContactsNextWordDictionary = SuggestionsProviderNulls.NULL_NEXT_WORD_SUGGESTIONS;
    }
  }

  void setUserDictionaryEnabled(boolean enabled) {
    invalidateSetupHash();
    mUserDictionaryEnabled = enabled;
  }

  void simulateDictionaryLoad(@NonNull DictionaryBackgroundLoader.Listener cb) {
    final List<Dictionary> dictionariesToSimulateLoad =
        new ArrayList<>(mMainDictionary.size() + mUserDictionary.size() + 1 /*for contacts*/);
    dictionariesToSimulateLoad.addAll(mMainDictionary);
    dictionariesToSimulateLoad.addAll(mUserDictionary);
    if (mContactsDictionaryEnabled) dictionariesToSimulateLoad.add(mContactsDictionary);

    for (Dictionary dictionary : dictionariesToSimulateLoad) {
      cb.onDictionaryLoadingStarted(dictionary);
    }
    for (Dictionary dictionary : dictionariesToSimulateLoad) {
      cb.onDictionaryLoadingDone(dictionary);
    }
  }

  void buildDictionaries(
      @NonNull List<DictionaryAddOnAndBuilder> dictionaryBuilders,
      @NonNull DictionaryBackgroundLoader.Listener cb,
      boolean logDictionarySetup) {
    final CompositeDisposable disposablesHolder = mDictionaryDisposables;
    mCurrentLocales.clear();

    if (logDictionarySetup) {
      Logger.d(TAG, "setupSuggestionsFor %d dictionaries", dictionaryBuilders.size());
      for (DictionaryAddOnAndBuilder dictionaryBuilder : dictionaryBuilders) {
        Logger.d(
            TAG,
            " * dictionary %s (%s)",
            dictionaryBuilder.getId(),
            dictionaryBuilder.getLanguage());
      }
    }

    for (int i = 0; i < dictionaryBuilders.size(); i++) {
      DictionaryAddOnAndBuilder dictionaryBuilder = dictionaryBuilders.get(i);
      final String language = dictionaryBuilder.getLanguage();
      if (!mCurrentLocales.contains(language)) mCurrentLocales.add(language);

      try {
        Logger.d(
            TAG,
            " Creating dictionary %s (%s)...",
            dictionaryBuilder.getId(),
            dictionaryBuilder.getLanguage());
        final Dictionary dictionary = dictionaryBuilder.createDictionary();
        mMainDictionary.add(dictionary);
        Logger.d(
            TAG,
            " Loading dictionary %s (%s)...",
            dictionaryBuilder.getId(),
            dictionaryBuilder.getLanguage());
        disposablesHolder.add(
            DictionaryBackgroundLoader.loadDictionaryInBackground(cb, dictionary));
      } catch (Exception e) {
        Logger.e(TAG, e, "Failed to create dictionary %s", dictionaryBuilder.getId());
      }

      if (mUserDictionaryEnabled) {
        final UserDictionary userDictionary = mUserDictionaryFactory.apply(language);
        mUserDictionary.add(userDictionary);
        Logger.d(TAG, " Loading user dictionary for %s...", language);
        disposablesHolder.add(
            DictionaryBackgroundLoader.loadDictionaryInBackground(cb, userDictionary));
        mUserNextWordDictionary.add(userDictionary.getUserNextWordGetter());
      } else {
        Logger.d(TAG, " User does not want user dictionary, skipping...");
      }

      final String contextProfilePresetId = mContextProfileWordListPresetId;
      if (contextProfilePresetId != null) {
        final EditableDictionary dictionary =
            mContextProfileWordListFactory.apply(contextProfilePresetId, language);
        mContextProfileWordList.add(dictionary);
        mContextProfileWordListDisposables.add(
            DictionaryBackgroundLoader.loadDictionaryInBackground(
                DictionaryBackgroundLoader.SILENT_LISTENER, dictionary));
      }
      // if mQuickFixesEnabled and mQuickFixesSecondDisabled are true
      // it activates autotext only to the current keyboard layout language
      if (mQuickFixesEnabled && (i == 0 || !mQuickFixesSecondDisabled)) {
        final AutoText autoText = dictionaryBuilder.createAutoText();
        if (autoText != null) {
          mQuickFixesAutoText.add(autoText);
        }
        final AbbreviationsDictionary abbreviationsDictionary =
            new AbbreviationsDictionary(mContext, dictionaryBuilder.getLanguage());
        mAbbreviationDictionary.add(abbreviationsDictionary);
        Logger.d(TAG, " Loading abbr dictionary for %s...", dictionaryBuilder.getLanguage());
        disposablesHolder.add(
            DictionaryBackgroundLoader.loadDictionaryInBackground(abbreviationsDictionary));
      }

      mInitialSuggestionsList.addAll(dictionaryBuilder.createInitialSuggestions());

      // only one auto-dictionary. There is no way to know to which language the typed word belongs.
      mAutoDictionary = new AutoDictionary(mContext, language);
      Logger.d(TAG, " Loading auto dictionary for %s...", language);
      disposablesHolder.add(DictionaryBackgroundLoader.loadDictionaryInBackground(mAutoDictionary));
    }

    if (mContactsDictionaryEnabled
        && mContactsDictionary == SuggestionsProviderNulls.NULL_DICTIONARY) {
      mContactsDictionaryListener.setDelegate(cb);
      final ContactsDictionary realContactsDictionary = mContactsDictionaryFactory.get();
      mContactsDictionary = realContactsDictionary;
      mContactsNextWordDictionary = realContactsDictionary;
      disposablesHolder.add(
          DictionaryBackgroundLoader.loadDictionaryInBackground(
              mContactsDictionaryListener, mContactsDictionary));
    }
  }

  void closeDictionariesForShutdown(@NonNull Runnable resetNextWordSentence) {
    Logger.d(TAG, "closeDictionaries");
    mCurrentSetupHashCode = 0;
    mMainDictionary.clear();
    mAbbreviationDictionary.clear();
    mUserDictionary.clear();
    mQuickFixesAutoText.clear();
    mUserNextWordDictionary.clear();
    mInitialSuggestionsList.clear();
    mCurrentLocales.clear();

    resetNextWordSentence.run();

    closeContextProfileWordListDictionaries();

    mContactsNextWordDictionary = SuggestionsProviderNulls.NULL_NEXT_WORD_SUGGESTIONS;
    mAutoDictionary = SuggestionsProviderNulls.NULL_DICTIONARY;
    mContactsDictionary = SuggestionsProviderNulls.NULL_DICTIONARY;

    mDictionaryDisposables.dispose();
    mDictionaryDisposables = new CompositeDisposable();
  }

  void getSuggestions(
      @NonNull KeyCodesProvider wordComposer, @NonNull Dictionary.WordCallback callback) {
    if (mContextAllowsContactsDictionary) {
      mContactsDictionary.getSuggestions(wordComposer, callback);
    }
    if (mContextAllowsUserDictionary) {
      allDictionariesGetWords(mUserDictionary, wordComposer, callback);
    }
    allDictionariesGetWords(mContextProfileWordList, wordComposer, callback);
    // isValidWord intentionally keeps consulting the main dictionary even when muted, so typing
    // a regular language word in a muted profile never triggers auto-correct.
    if (mContextAllowsMainDictionary) {
      allDictionariesGetWords(mMainDictionary, wordComposer, callback);
    }
  }

  void getAbbreviations(
      @NonNull KeyCodesProvider wordComposer, @NonNull Dictionary.WordCallback callback) {
    allDictionariesGetWords(mAbbreviationDictionary, wordComposer, callback);
  }

  void getAutoText(
      @NonNull KeyCodesProvider wordComposer, @NonNull Dictionary.WordCallback callback) {
    final CharSequence word = wordComposer.getTypedWord();
    for (AutoText autoText : mQuickFixesAutoText) {
      final String fix = autoText.lookup(word);
      if (fix != null) callback.addWord(fix.toCharArray(), 0, fix.length(), 255, null);
    }
  }

  boolean isValidWord(@NonNull CharSequence word) {
    if (TextUtils.isEmpty(word)) {
      return false;
    }

    return allDictionariesIsValid(mMainDictionary, word)
        || allDictionariesIsValid(mContextProfileWordList, word)
        || (mContextAllowsUserDictionary && allDictionariesIsValid(mUserDictionary, word))
        || (mContextAllowsContactsDictionary && mContactsDictionary.isValidWord(word));
  }

  void removeWordFromUserDictionary(@NonNull String word) {
    for (EditableDictionary dictionary : mUserDictionary) {
      dictionary.deleteWord(word);
    }
  }

  void clearLearningData() {
    if (mAutoDictionary instanceof AutoDictionary) {
      ((AutoDictionary) mAutoDictionary).clearAllWords();
    }

    for (NextWordSuggestions suggestions : mUserNextWordDictionary) {
      if (suggestions instanceof NextWordDictionary) {
        final NextWordDictionary nextWordDictionary = (NextWordDictionary) suggestions;
        nextWordDictionary.clearData();
        nextWordDictionary.close();
      } else {
        suggestions.resetSentence();
      }
    }
  }

  boolean addWordToUserDictionary(@NonNull String word) {
    if (mUserDictionary.size() > 0) {
      return mUserDictionary.get(0).addWord(word, 128);
    } else {
      return false;
    }
  }

  boolean tryToLearnNewWord(@NonNull CharSequence newWord, int frequencyDelta) {
    if (!isValidWord(newWord)) {
      return mAutoDictionary.addWord(newWord.toString(), frequencyDelta);
    }

    return false;
  }

  private static boolean allDictionariesIsValid(
      @NonNull List<? extends Dictionary> dictionaries, @NonNull CharSequence word) {
    for (Dictionary dictionary : dictionaries) {
      if (dictionary.isValidWord(word)) return true;
    }

    return false;
  }

  private static void allDictionariesGetWords(
      @NonNull List<? extends Dictionary> dictionaries,
      @NonNull KeyCodesProvider wordComposer,
      @NonNull Dictionary.WordCallback wordCallback) {
    for (Dictionary dictionary : dictionaries) {
      dictionary.getSuggestions(wordComposer, wordCallback);
    }
  }
}
