package wtf.uhoh.newsoftkeyboard.app.ime;

import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import com.anysoftkeyboard.api.KeyCodes;
import java.util.Collections;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.BuildConfig;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.Suggest;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.SuggestImpl;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateView;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateViewHost;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.InputViewBinder;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewContainerView;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.dictionaries.DictionaryBackgroundLoader;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordComposer;
import wtf.uhoh.newsoftkeyboard.nextword.prediction.NextWordContextTokenizer;

@SuppressWarnings("this-escape")
public abstract class ImeSuggestionsController extends ImeKeyboardSwitchedListener
    implements CandidateViewHost {

  @VisibleForTesting public static final long MAX_TIME_TO_EXPECT_SELECTION_UPDATE = 1500;
  private static final long CLOSE_DICTIONARIES_DELAY = 10 * ONE_FRAME_DELAY;
  private static final long NEVER_TIME_STAMP = -1L * 365L * 24L * 60L * 60L * 1000L; // a year ago.
  @VisibleForTesting public static final long GET_SUGGESTIONS_DELAY = 5 * ONE_FRAME_DELAY;

  @VisibleForTesting
  final KeyboardUIStateHandler mKeyboardHandler = new KeyboardUIStateHandler(this);

  private final SuggestionsSessionState suggestionsSessionState =
      new SuggestionsSessionState(NEVER_TIME_STAMP);
  @VisibleForTesting int mOnStartInputViewCountForTest = 0;
  @VisibleForTesting boolean mLastOnStartInputViewRestartingForTest = false;
  @VisibleForTesting int mLastOnStartInputViewInputTypeForTest = 0;
  @VisibleForTesting int mLastOnStartInputViewImeOptionsForTest = 0;
  @VisibleForTesting long mLastOnStartInputViewUptimeMsForTest = 0L;
  Suggest mSuggest;
  CandidateView mCandidateView;
  private boolean mFrenchSpacePunctuationBehavior;
  private final KeyboardDictionariesLoader keyboardDictionariesLoader =
      new KeyboardDictionariesLoader();

  @VisibleForTesting
  final CancelSuggestionsAction mCancelSuggestionsAction =
      new CancelSuggestionsAction(() -> abortCorrectionAndResetPredictionState(true));

  private final InputFieldConfigurator inputFieldConfigurator = new InputFieldConfigurator();
  private final SelectionUpdateProcessor selectionUpdateProcessor = new SelectionUpdateProcessor();
  private SuggestionStripController suggestionStripController;
  final CompletionHandler completionHandler = new CompletionHandler();
  private final WordRestartCoordinator wordRestartCoordinator = new WordRestartCoordinator();
  private final SeparatorOutputHandler separatorOutputHandler = new SeparatorOutputHandler();
  private final CursorTouchChecker cursorTouchChecker = new CursorTouchChecker();
  private final WordRestartGate wordRestartGate = new WordRestartGate();
  private final SuggestionCommitter suggestionCommitter =
      new SuggestionCommitter(new SuggestionCommitterHost(this));
  private final SuggestionPicker suggestionPicker =
      new SuggestionPicker(new SuggestionPickerHost(this));
  private final SuggestionRefresher suggestionRefresher = new SuggestionRefresher();
  private final SpaceSwapDecider spaceSwapDecider = new SpaceSwapDecider();
  private final SuggestionsUpdater suggestionsUpdater =
      new SuggestionsUpdater(
          mKeyboardHandler,
          this::performUpdateSuggestions,
          GET_SUGGESTIONS_DELAY,
          KeyboardUIStateHandler.MSG_UPDATE_SUGGESTIONS);
  private final SuggestionSettingsController suggestionSettingsController =
      new SuggestionSettingsController();
  private final WordRevertHandler wordRevertHandler = new WordRevertHandler();
  private final UserDictionaryWorker userDictionaryWorker =
      new UserDictionaryWorker(new UserDictionaryHost(() -> mSuggest, () -> mCandidateView));
  private final AddToDictionaryHintController addToDictionaryHintController =
      new AddToDictionaryHintController(
          new AddToDictionaryHintHost(
              () -> mCandidateView,
              () -> mSuggest,
              this::getCurrentAlphabetKeyboard,
              (suggestions, highlightedIndex) -> {
                ImeSuggestionsController.this.markKeepSuggestionsStripWhileIdle();
                ImeSuggestionsController.this.setSuggestions(suggestions, highlightedIndex);
              }));
  private final TypingSimulator typingSimulator = new TypingSimulator();
  private final PredictionGate predictionGate = new PredictionGate();
  private final SeparatorHandler separatorHandler = new SeparatorHandler();
  private final SeparatorHandlerHost separatorHandlerHost =
      new SeparatorHandlerHost(
          this,
          suggestionsSessionState.spaceTimeTracker,
          separatorOutputHandler,
          suggestionsSessionState.sentenceSeparators,
          suggestionsSessionState.predictionState);
  private final PredictionStateUpdater predictionStateUpdater = new PredictionStateUpdater();
  private final CharacterInputHandler characterInputHandler = new CharacterInputHandler();
  private final TextInputDispatcher textInputDispatcher = new TextInputDispatcher(typingSimulator);
  private final CharacterInputHandler.Host characterInputHost =
      new CharacterInputHost(
          this,
          suggestionsSessionState.autoCorrectState,
          suggestionsSessionState.predictionState,
          suggestionsSessionState.shiftStateTracker);
  private final TextInputDispatcher.Host textInputHost =
      new TextInputHost(
          this, suggestionsSessionState.autoCorrectState, suggestionsSessionState.predictionState);
  private final WordRestartCoordinator.Host wordRestartHost = new WordRestartHost(this);
  private final SuggestionRefresher.Host suggestionRefresherHost =
      new SuggestionRefresherHost(this);
  private final WordRevertHandler.Host wordRevertHost = new WordRevertHost(this);

  @Nullable
  protected Keyboard.Key getLastUsedKey() {
    return suggestionsSessionState.lastKeyTracker.lastKey();
  }

  void setAllowSuggestionsRestart(boolean allow) {
    suggestionsSessionState.predictionState.allowSuggestionsRestart = allow;
  }

  void applySuggestionSettings(
      boolean showSuggestions,
      boolean autoComplete,
      int commonalityMaxLengthDiff,
      int commonalityMaxDistance,
      boolean trySplitting) {
    predictionStateUpdater.applySuggestionSettings(
        suggestionsSessionState.predictionState,
        mSuggest,
        showSuggestions,
        autoComplete,
        commonalityMaxLengthDiff,
        commonalityMaxDistance,
        trySplitting,
        keyboardDictionariesLoader::reset,
        this::setDictionariesForCurrentKeyboard,
        this::closeDictionaries);
  }

  @Override
  public void onCreate() {
    super.onCreate();

    mSuggest = createSuggest();
    if (mSuggest instanceof SuggestImpl) {
      ((SuggestImpl) mSuggest).setAsyncHybridNeuralListener(this::refreshNextWordSuggestionsIfIdle);
    }

    if (BuildConfig.TESTING_BUILD) {
      try {
        // expose a tiny test-only API to help instrumentation seed context
        wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.setService(this);
      } catch (Throwable ignore) {
        // class not present in release builds
      }
    }

    suggestionSettingsController.attach(this, mSuggest);
    // apply current prefs synchronously so tests have suggestions before async streams emit
    suggestionSettingsController.applySnapshot(this, mSuggest);
  }

  private void refreshNextWordSuggestionsIfIdle() {
    if (!isPredictionOn() || !suggestionsSessionState.predictionState.showSuggestions) return;
    if (!suggestionsSessionState.keepSuggestionsStripWhileIdle) return;
    if (!suggestionsSessionState.wordComposerTracker.currentWord().isEmpty()) return;
    final CharSequence last = lastCommittedWordForNextSuggestions();
    if (last.length() == 0) return;
    markKeepSuggestionsStripWhileIdle();
    setSuggestions(mSuggest.getNextSuggestions(last, mShiftKeyState.isLocked()), -1);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mKeyboardHandler.removeAllMessages();
    mSuggest.destroy();
  }

  @Override
  public void onStartInput(EditorInfo attribute, boolean restarting) {
    super.onStartInput(attribute, restarting);
    // removing close request (if it was asked for a previous onFinishInput).
    mKeyboardHandler.removeMessages(KeyboardUIStateHandler.MSG_CLOSE_DICTIONARIES);

    // Avoid wiping next-word context on onStartInput(...). Editor changes and privacy boundaries
    // are
    // handled in onStartInputView(...) where EditorInfo is stable and we can safely decide whether
    // to reset/preserve the next-word sentence.
    abortCorrectionAndResetPredictionState(false, /* resetNextWordSentence= */ false);
    keyboardDictionariesLoader.reset();
  }

  /** Hook for subclasses to preserve next-word context across input restarts when safe. */
  protected boolean shouldResetNextWordContextOnStartInput(
      @NonNull EditorInfo attribute, boolean restarting) {
    return true;
  }

  @Override
  public void onStartInputView(final EditorInfo attribute, final boolean restarting) {
    super.onStartInputView(attribute, restarting);
    mOnStartInputViewCountForTest++;
    mLastOnStartInputViewRestartingForTest = restarting;
    mLastOnStartInputViewInputTypeForTest = attribute.inputType;
    mLastOnStartInputViewImeOptionsForTest = attribute.imeOptions;
    mLastOnStartInputViewUptimeMsForTest = SystemClock.uptimeMillis();

    suggestionsSessionState.predictionState.predictionOn = false;
    keyboardDictionariesLoader.reset();
    completionHandler.reset();
    suggestionsSessionState.predictionState.inputFieldSupportsAutoPick = false;

    final boolean respectNoSuggestionsFlag =
        prefs()
            .getBoolean(
                R.string.settings_key_respect_app_no_suggestions_flag,
                R.bool.settings_default_respect_app_no_suggestions_flag)
            .get();
    InputFieldConfigurator.Result inputConfig =
        inputFieldConfigurator.configure(
            attribute,
            restarting,
            getKeyboardSwitcher(),
            mPrefsAutoSpace,
            respectNoSuggestionsFlag,
            TAG);

    if (mSuggest instanceof SuggestImpl) {
      final int inputClass = attribute.inputType & EditorInfo.TYPE_MASK_CLASS;
      final int variation = attribute.inputType & EditorInfo.TYPE_MASK_VARIATION;
      final boolean isInternetInputField =
          inputClass == EditorInfo.TYPE_CLASS_TEXT
              && (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                  || variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
                  || variation == EditorInfo.TYPE_TEXT_VARIATION_URI);
      ((SuggestImpl) mSuggest).setInternetInputField(isInternetInputField);
    }

    predictionStateUpdater.applyInputFieldConfig(
        suggestionsSessionState.predictionState, inputConfig, predictionGate);

    final boolean showNoSuggestionsAction =
        shouldShowNoSuggestionsAction(attribute, respectNoSuggestionsFlag);
    Logger.d(
        TAG,
        "Suggestion strip: predictionOn=%s showNoSuggestionsAction=%s",
        isPredictionOn(),
        showNoSuggestionsAction);
    suggestionStripController.attachToStrip(getInputViewContainer());
    suggestionStripController.showStrip(
        isPredictionOn(), showNoSuggestionsAction, getInputViewContainer());
    clearSuggestions();
    setDictionariesForCurrentKeyboard();
  }

  @VisibleForTesting
  int getOnStartInputViewCountForTest() {
    return mOnStartInputViewCountForTest;
  }

  @VisibleForTesting
  boolean getLastOnStartInputViewRestartingForTest() {
    return mLastOnStartInputViewRestartingForTest;
  }

  @VisibleForTesting
  int getLastOnStartInputViewInputTypeForTest() {
    return mLastOnStartInputViewInputTypeForTest;
  }

  @VisibleForTesting
  int getLastOnStartInputViewImeOptionsForTest() {
    return mLastOnStartInputViewImeOptionsForTest;
  }

  @VisibleForTesting
  long getLastOnStartInputViewUptimeMsForTest() {
    return mLastOnStartInputViewUptimeMsForTest;
  }

  private boolean shouldShowNoSuggestionsAction(
      EditorInfo attribute, boolean respectNoSuggestions) {
    if (!respectNoSuggestions) return false;
    if (!suggestionsSessionState.predictionState.showSuggestions) return false;

    final int inputType = attribute.inputType;
    if ((inputType & EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS) == 0) return false;
    final int inputClass = inputType & EditorInfo.TYPE_MASK_CLASS;
    if (inputClass != 0 && inputClass != EditorInfo.TYPE_CLASS_TEXT) return false;

    final int textVariation = inputType & EditorInfo.TYPE_MASK_VARIATION;
    return textVariation != EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
        && textVariation != EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        && textVariation != EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD;
  }

  @Override
  public void onFinishInput() {
    super.onFinishInput();
    mCancelSuggestionsAction.setCancelIconVisible(false);
    suggestionsSessionState.predictionState.predictionOn = false;
    mKeyboardHandler.sendEmptyMessageDelayed(
        KeyboardUIStateHandler.MSG_CLOSE_DICTIONARIES, CLOSE_DICTIONARIES_DELAY);
    suggestionsSessionState.selectionExpectationTracker.clear();
    keyboardDictionariesLoader.reset();
  }

  @Override
  public void onFinishInputView(boolean finishingInput) {
    super.onFinishInputView(finishingInput);
    // Avoid wiping next-word context on view tear-down. If we actually switched editors,
    // onStartInputView(...) will clear context for the new editor as needed.
    abortCorrectionAndResetPredictionState(true, /* resetNextWordSentence= */ false);
  }

  /*
   * this function is called EVERY TIME them selection is changed. This also
   * includes the underlined suggestions.
   */
  @Override
  public void onUpdateSelection(
      int oldSelStart,
      int oldSelEnd,
      int newSelStart,
      int newSelEnd,
      int candidatesStart,
      int candidatesEnd) {
    final int oldCandidateStart = getCandidateStartPositionDangerous();
    final int oldCandidateEnd = getCandidateEndPositionDangerous();
    super.onUpdateSelection(
        oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);

    selectionUpdateProcessor.onUpdateSelection(
        oldSelStart,
        oldSelEnd,
        newSelStart,
        newSelEnd,
        candidatesStart,
        candidatesEnd,
        new SelectionUpdateHost(this, oldCandidateStart, oldCandidateEnd));
  }

  @Override
  public View onCreateInputView() {
    final View view = super.onCreateInputView();
    mCandidateView = getInputViewContainer().getCandidateView();
    mCancelSuggestionsAction.setOwningCandidateView(mCandidateView);
    suggestionStripController =
        new SuggestionStripController(mCancelSuggestionsAction, mCandidateView);
    suggestionStripController.setHost(this);
    return view;
  }

  protected WordComposer getCurrentComposedWord() {
    return suggestionsSessionState.wordComposerTracker.currentWord();
  }

  @Override
  @CallSuper
  public void onKey(
      int primaryCode, Keyboard.Key key, int multiTapIndex, int[] nearByKeyCodes, boolean fromUI) {
    suggestionsSessionState.lastKeyTracker.record(key, primaryCode);
    super.onKey(primaryCode, key, multiTapIndex, nearByKeyCodes, fromUI);
    if (primaryCode != KeyCodes.DELETE) {
      suggestionsSessionState.autoCorrectState.wordRevertLength = 0;
    }
    mCandidateView.dismissAddToDictionaryHint();
  }

  protected void resetLastPressedKey() {
    suggestionsSessionState.lastKeyTracker.reset();
  }

  @Override
  public void onRelease(int primaryCode) {
    // not allowing undo on-text in clipboard paste operations.
    if (primaryCode == KeyCodes.CLIPBOARD_PASTE)
      suggestionsSessionState.autoCorrectState.wordRevertLength = 0;
    if (suggestionsSessionState.lastKeyTracker.shouldMarkSpaceTime(primaryCode)) {
      setSpaceTimeStamp(primaryCode == KeyCodes.SPACE);
    }
    if (!isCurrentlyPredicting()
        && (primaryCode == KeyCodes.DELETE
            || primaryCode == KeyCodes.DELETE_WORD
            || primaryCode == KeyCodes.FORWARD_DELETE)) {
      postRestartWordSuggestion();
    }
  }

  protected void postRestartWordSuggestion() {
    mKeyboardHandler.removeMessages(KeyboardUIStateHandler.MSG_UPDATE_SUGGESTIONS);
    mKeyboardHandler.removeMessages(KeyboardUIStateHandler.MSG_RESTART_NEW_WORD_SUGGESTIONS);
    mKeyboardHandler.sendEmptyMessageDelayed(
        KeyboardUIStateHandler.MSG_RESTART_NEW_WORD_SUGGESTIONS, 10 * ONE_FRAME_DELAY);
  }

  @Override
  @CallSuper
  public void onMultiTapStarted() {
    final InputViewBinder inputView = getInputView();
    if (inputView != null) {
      inputView.setShifted(suggestionsSessionState.shiftStateTracker.lastCharacterWasShifted());
    }
  }

  @Override
  protected boolean isSelectionUpdateDelayed() {
    return suggestionsSessionState.selectionExpectationTracker.isSelectionUpdatePending();
  }

  protected boolean shouldRevertOnDelete() {
    return suggestionsSessionState.autoCorrectState.shouldRevertOnDelete();
  }

  void clearSpaceTimeTracker() {
    suggestionsSessionState.spaceTimeTracker.clear();
  }

  long getExpectingSelectionUpdateBy() {
    return suggestionsSessionState.selectionExpectationTracker.getExpectingSelectionUpdateBy();
  }

  void clearExpectingSelectionUpdate() {
    suggestionsSessionState.selectionExpectationTracker.clear();
  }

  void markSelectionUpdateReceived() {
    suggestionsSessionState.selectionExpectationTracker.markSelectionUpdateReceived();
  }

  void setExpectingSelectionUpdateBy(long value) {
    suggestionsSessionState.selectionExpectationTracker.setExpectingSelectionUpdateBy(value);
  }

  WordComposer getCurrentWord() {
    return suggestionsSessionState.wordComposerTracker.currentWord();
  }

  void setPreviousWord(@NonNull WordComposer word) {
    suggestionsSessionState.wordComposerTracker.setPreviousWord(word);
  }

  public void setWordRevertLength(int length) {
    suggestionsSessionState.autoCorrectState.wordRevertLength = length;
  }

  int getWordRevertLength() {
    return suggestionsSessionState.autoCorrectState.wordRevertLength;
  }

  protected void handleCharacter(
      final int primaryCode,
      final Keyboard.Key key,
      final int multiTapIndex,
      int[] nearByKeyCodes) {
    clearKeepSuggestionsStripWhileIdle();
    characterInputHandler.handleCharacter(
        primaryCode, key, multiTapIndex, nearByKeyCodes, TAG, characterInputHost);
  }

  // Make sure to call this BEFORE actually making changes, and not after.
  // the event might arrive immediately as changes occur.
  public void markExpectingSelectionUpdate() {
    markExpectingSelectionUpdate(1);
  }

  public void markExpectingSelectionUpdate(int expectedSelectionUpdates) {
    suggestionsSessionState.selectionExpectationTracker.markExpectingUntil(
        SystemClock.uptimeMillis() + MAX_TIME_TO_EXPECT_SELECTION_UPDATE, expectedSelectionUpdates);
  }

  public void handleSeparator(int primaryCode) {
    if (BuildConfig.DEBUG) {
      Logger.d(
          TAG,
          "handleSeparator code=%d isSpace=%s lastSpace=%s swapCandidate=%s",
          primaryCode,
          primaryCode == KeyCodes.SPACE,
          suggestionsSessionState.spaceTimeTracker.hadSpace(),
          isSpaceSwapCharacter(primaryCode));
    }

    separatorHandler.handleSeparator(primaryCode, separatorHandlerHost);
  }

  @NonNull
  public WordComposer prepareWordComposerForNextWord() {
    return suggestionsSessionState.wordComposerTracker.prepareWordComposerForNextWord();
  }

  public boolean isSpaceSwapCharacter(int primaryCode) {
    if (!mSwapPunctuationAndSpace) return false;
    return spaceSwapDecider.isSpaceSwapCharacter(
        primaryCode, mFrenchSpacePunctuationBehavior, suggestionsSessionState.sentenceSeparators);
  }

  public void performRestartWordSuggestion() {
    mKeyboardHandler.removeMessages(KeyboardUIStateHandler.MSG_RESTART_NEW_WORD_SUGGESTIONS);
    mKeyboardHandler.removeMessages(KeyboardUIStateHandler.MSG_UPDATE_SUGGESTIONS);

    wordRestartCoordinator.performRestartWordSuggestion(
        getImeSessionState().getInputConnectionRouter(), wordRestartHost);
  }

  @Override
  public void onText(Keyboard.Key key, CharSequence text) {
    clearKeepSuggestionsStripWhileIdle();
    textInputDispatcher.onText(text, textInputHost, TAG);
  }

  @Override
  public void onTyping(Keyboard.Key key, CharSequence text) {
    textInputDispatcher.onTyping(key, text, textInputHost, TAG);
  }

  protected void setDictionariesForCurrentKeyboard() {
    if (keyboardDictionariesLoader.isLoaded()) return;

    keyboardDictionariesLoader.ensureLoaded(
        this,
        suggestionsSessionState.predictionState,
        shouldLoadDictionariesForGestureTyping(),
        getCurrentAlphabetKeyboard(),
        isInAlphabetKeyboardMode(),
        suggestionsSessionState.sentenceSeparators,
        mSuggest,
        this::getDictionaryLoadedListener);
  }

  @NonNull
  protected DictionaryBackgroundLoader.Listener getDictionaryLoadedListener(
      @NonNull KeyboardDefinition currentAlphabetKeyboard) {
    return DictionaryBackgroundLoader.SILENT_LISTENER;
  }

  /**
   * Allows subclasses (e.g., gesture typing) to force dictionary loading even when predictions are
   * off.
   */
  protected boolean shouldLoadDictionariesForGestureTyping() {
    return false;
  }

  @Override
  protected void onOrientationChanged(int oldOrientation, int newOrientation) {
    super.onOrientationChanged(oldOrientation, newOrientation);
    abortCorrectionAndResetPredictionState(false);

    String sentenceSeparatorsForCurrentKeyboard =
        getKeyboardSwitcher().getCurrentKeyboardSentenceSeparators();
    if (sentenceSeparatorsForCurrentKeyboard == null) {
      suggestionsSessionState.sentenceSeparators.clear();
    } else {
      suggestionsSessionState.sentenceSeparators.updateFrom(
          sentenceSeparatorsForCurrentKeyboard.toCharArray());
    }
  }

  @CallSuper
  public void abortCorrectionAndResetPredictionState(boolean disabledUntilNextInputStart) {
    abortCorrectionAndResetPredictionState(
        disabledUntilNextInputStart, /* resetNextWordSentence= */ true);
  }

  @CallSuper
  public void abortCorrectionAndResetPredictionState(
      boolean disabledUntilNextInputStart, boolean resetNextWordSentence) {
    if (resetNextWordSentence) {
      mSuggest.resetNextWordSentence();
    }
    clearKeepSuggestionsStripWhileIdle();

    suggestionsSessionState.spaceTimeTracker.clear();
    suggestionsSessionState.autoCorrectState.justAutoAddedWord = false;
    mKeyboardHandler.removeAllSuggestionMessages();

    markExpectingSelectionUpdate();
    getImeSessionState().getInputConnectionRouter().finishComposingText();

    clearSuggestions();

    suggestionsSessionState.wordComposerTracker.resetCurrentWord();
    suggestionsSessionState.autoCorrectState.reset();
    if (disabledUntilNextInputStart) {
      Logger.d(TAG, "abortCorrection will abort correct forever");
      final KeyboardViewContainerView inputViewContainer = getInputViewContainer();
      if (inputViewContainer != null) {
        inputViewContainer.removeStripAction(mCancelSuggestionsAction);
      }
      suggestionsSessionState.predictionState.predictionOn = false;
    }
  }

  /** Allows subclasses to force a reload of keyboard dictionaries. */
  protected void invalidateDictionariesForCurrentKeyboard() {
    keyboardDictionariesLoader.reset();
  }

  public void clearSuggestions() {
    clearKeepSuggestionsStripWhileIdle();
    mKeyboardHandler.removeAllSuggestionMessages();
    setSuggestions(Collections.emptyList(), -1);
  }

  void markKeepSuggestionsStripWhileIdle() {
    suggestionsSessionState.keepSuggestionsStripWhileIdle = true;
  }

  void clearKeepSuggestionsStripWhileIdle() {
    suggestionsSessionState.keepSuggestionsStripWhileIdle = false;
  }

  @VisibleForTesting
  void setAutoSpaceEnabledForTest(boolean enabled) {
    mPrefsAutoSpace = enabled;
    suggestionsSessionState.predictionState.autoSpace = enabled;
  }

  @VisibleForTesting
  boolean isAutoSpaceEnabledForTest() {
    return suggestionsSessionState.predictionState.autoSpace;
  }

  public void setSuggestions(
      @NonNull List<? extends CharSequence> suggestions, int highlightedSuggestionIndex) {
    mCancelSuggestionsAction.setCancelIconVisible(!suggestions.isEmpty());
    if (mCandidateView != null) {
      mCandidateView.setSuggestions(suggestions, highlightedSuggestionIndex);
    }
  }

  Suggest getSuggestForTests() {
    return mSuggest;
  }

  CandidateView getCandidateViewForTests() {
    return mCandidateView;
  }

  @NonNull
  protected Suggest getSuggest() {
    return mSuggest;
  }

  public Suggest suggest() {
    return mSuggest;
  }

  @Override
  @NonNull
  protected List<Drawable> generateWatermark() {
    final List<Drawable> watermark = super.generateWatermark();
    if (mSuggest.isIncognitoMode()) {
      watermark.add(ContextCompat.getDrawable(this, R.drawable.ic_watermark_incognito));
    }
    return watermark;
  }

  @NonNull
  protected Suggest createSuggest() {
    return new SuggestImpl(this);
  }

  protected abstract boolean isAlphabet(int code);

  public void addWordToDictionary(String word) {
    userDictionaryWorker.addWordToDictionary(word, mInputSessionDisposables::add);
  }

  /** posts an update suggestions request to the messages queue. Removes any previous request. */
  protected void postUpdateSuggestions() {
    suggestionsUpdater.postUpdateSuggestions();
  }

  protected boolean isPredictionOn() {
    return suggestionsSessionState.predictionState.isPredictionOn();
  }

  public boolean isAutoCorrect() {
    return suggestionsSessionState.predictionState.isAutoCorrect();
  }

  public boolean isCurrentlyPredicting() {
    return isPredictionOn() && !suggestionsSessionState.wordComposerTracker.currentWord().isEmpty();
  }

  boolean isAutoCompleteEnabled() {
    return suggestionsSessionState.predictionState.autoComplete;
  }

  public void performUpdateSuggestions() {
    mKeyboardHandler.removeMessages(KeyboardUIStateHandler.MSG_UPDATE_SUGGESTIONS);

    // If we're not composing a word but the strip is already populated (for example, showing
    // next-word suggestions after a manual pick), avoid clobbering the strip with an update that
    // was scheduled earlier while composing. This helps keep next-word suggestions visible while
    // idle in "heavier" editors where message timing can be inconsistent.
    if (suggestionsSessionState.keepSuggestionsStripWhileIdle
        && suggestionsSessionState.predictionState.isPredictionOn()
        && suggestionsSessionState.predictionState.showSuggestions
        && suggestionsSessionState.wordComposerTracker.currentWord().isEmpty()
        && mCandidateView != null
        && !mCandidateView.getSuggestions().isEmpty()) {
      return;
    }

    suggestionRefresher.performUpdateSuggestions(
        suggestionsSessionState.predictionState,
        suggestionsSessionState.wordComposerTracker.currentWord(),
        mSuggest,
        suggestionRefresherHost);
  }

  public void pickSuggestionManually(int index, CharSequence suggestion) {
    pickSuggestionManually(index, suggestion, suggestionsSessionState.predictionState.autoSpace);
  }

  /**
   * Triggers haptic feedback for non-keyboard-view interactions (for example, tapping the
   * suggestions strip). Default implementation is a no-op and can be overridden by subclasses that
   * manage vibration/haptics.
   */
  protected void performHapticFeedbackForUserAction() {
    // no-op by default
  }

  @CallSuper
  public void pickSuggestionManually(
      int index, CharSequence suggestion, boolean withAutoSpaceEnabled) {
    performHapticFeedbackForUserAction();
    // A manual pick (and optional auto-space) typically causes a selection update from the editor.
    // If we don't mark it as expected, some editors can trigger the "restart word suggestion"
    // flow, which clears the next-word strip (making it look like next-word suggestions only
    // appear after typing the next letter).
    markExpectingSelectionUpdate(3);
    // Cancel any pending "update suggestions" / "restart word" messages that were scheduled while
    // composing. Otherwise, a delayed MSG_UPDATE_SUGGESTIONS can run after the pick, and since the
    // word-composer is now empty it may clobber the next-word strip (observed in some apps).
    mKeyboardHandler.removeAllSuggestionMessages();
    suggestionsSessionState.autoCorrectState.wordRevertLength = 0; // no reverts
    final WordComposer typedWord = prepareWordComposerForNextWord();

    suggestionPicker.pickSuggestionManually(
        typedWord,
        withAutoSpaceEnabled,
        index,
        suggestion,
        suggestionsSessionState.predictionState.showSuggestions,
        suggestionsSessionState.autoCorrectState.justAutoAddedWord,
        typedWord.isAtTagsSearchState());
    getImeSessionState().getInputConnectionRouter().requestComposingTextRevalidation();
  }

  /**
   * Commits the chosen word to the text field and saves it for later retrieval.
   *
   * @param wordToCommit the suggestion picked by the user to be committed to the text field
   * @param typedWord the word the user typed.
   */
  @CallSuper
  public void commitWordToInput(
      @NonNull CharSequence wordToCommit, @NonNull CharSequence typedWord) {
    suggestionCommitter.commitWordToInput(wordToCommit, typedWord);
    recordLastCommittedWordForNextSuggestions(wordToCommit);
    mSuggest.notifyWordCommitted(wordToCommit);
  }

  void commitManuallyPickedWordToInput(
      @NonNull CharSequence wordToCommit, @NonNull CharSequence typedWordInEditor) {
    suggestionCommitter.commitManuallyPickedWordToInput(wordToCommit, typedWordInEditor);
    recordLastCommittedWordForNextSuggestions(wordToCommit);
    mSuggest.notifyWordCommitted(wordToCommit);
  }

  @NonNull
  CharSequence lastCommittedWordForNextSuggestions() {
    return suggestionsSessionState.lastCommittedWordForNextSuggestions;
  }

  void clearLastCommittedWordForNextSuggestions() {
    suggestionsSessionState.lastCommittedWordForNextSuggestions = "";
  }

  void seedLastCommittedWordForNextSuggestionsFromEditorText(@NonNull CharSequence previousWord) {
    final String token = previousWord.toString().trim();
    if (token.isEmpty()) return;
    suggestionsSessionState.lastCommittedWordForNextSuggestions = token;
  }

  void onUnexpectedCursorMoveWhileNotPredicting() {
    // When the user moves the cursor while idle, we should not keep next-word state from the prior
    // cursor location. Otherwise, separator-driven "fallback" next-word requests can use a stale
    // token, and next-word learning can connect unrelated words across cursor moves.
    mSuggest.resetNextWordSentence();
    clearLastCommittedWordForNextSuggestions();
    clearSuggestions();

    maybeSeedNextWordContextFromEditorAfterCursorMove();
  }

  private void maybeSeedNextWordContextFromEditorAfterCursorMove() {
    if (!suggestionsSessionState.predictionState.showSuggestions) return;

    // If the cursor is touching a word, we expect the word-restart flow to handle suggestion
    // restart for completions/corrections. Avoid extracting a partial token (cursor inside a word)
    // as a "previous word" for next-word suggestions.
    if (isCursorTouchingWord()) return;

    final EditorInfo attribute = currentInputEditorInfo();
    if (attribute == null) return;
    if (ImeClipboard.isTextPassword(attribute) || ImeIncognito.isNumberPassword(attribute)) return;
    if ((attribute.imeOptions & EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0) return;
    if ((attribute.inputType & EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) return;
    if (mSuggest.isIncognitoMode()) return;

    final InputConnectionRouter router = getInputConnectionRouter();
    if (!router.hasConnection()) return;

    CharSequence beforeCursor = null;
    try {
      beforeCursor = router.getTextBeforeCursor(4096, 0);
    } catch (Throwable t) {
      // Best-effort only; some editors can throw while querying context.
    }
    if (TextUtils.isEmpty(beforeCursor)) {
      try {
        final ExtractedTextRequest request = new ExtractedTextRequest();
        request.hintMaxChars = 4096;
        request.hintMaxLines = 10;
        final ExtractedText extracted = router.getExtractedText(request);
        if (extracted != null
            && !TextUtils.isEmpty(extracted.text)
            && extracted.selectionEnd >= 0) {
          final int selEnd = Math.min(extracted.selectionEnd, extracted.text.length());
          final int start = Math.max(0, selEnd - 4096);
          beforeCursor = extracted.text.subSequence(start, selEnd);
        }
      } catch (Throwable t) {
        // Best-effort only.
        return;
      }
    }
    if (TextUtils.isEmpty(beforeCursor)) return;

    final List<String> tokens = NextWordContextTokenizer.tokenizeTextBeforeCursor(beforeCursor, 64);
    if (tokens.isEmpty()) return;
    final String previousWord = tokens.get(tokens.size() - 1);
    if (previousWord.isEmpty()) return;

    // Seed the engine context window without learning/persisting from editor text.
    mSuggest.seedNextWordEngineContextFromEditorText(beforeCursor);
    suggestionsSessionState.lastCommittedWordForNextSuggestions = previousWord;

    markKeepSuggestionsStripWhileIdle();
    setSuggestions(mSuggest.getNextSuggestions(previousWord, mShiftKeyState.isLocked()), -1);
  }

  private void recordLastCommittedWordForNextSuggestions(@NonNull CharSequence wordToCommit) {
    final String token = wordToCommit.toString().trim();
    suggestionsSessionState.lastCommittedWordForNextSuggestions = token;
  }

  @VisibleForTesting
  void recordLastCommittedWordForNextSuggestionsForTest(@NonNull CharSequence wordToCommit) {
    recordLastCommittedWordForNextSuggestions(wordToCommit);
  }

  protected boolean canRestartWordSuggestion() {
    final InputViewBinder inputView = getInputView();
    if (!wordRestartGate.canRestartWordSuggestion(
        isPredictionOn(),
        suggestionsSessionState.predictionState.allowSuggestionsRestart,
        inputView)) {
      return false;
    } else if (!isCursorTouchingWord()) {
      Logger.d(TAG, "User moved cursor to no-man land. Bye bye.");
      return false;
    }

    return true;
  }

  private boolean isCursorTouchingWord() {
    return cursorTouchChecker.isCursorTouchingWord(
        getImeSessionState().getInputConnectionRouter(), this::isWordSeparator);
  }

  protected void setSpaceTimeStamp(boolean isSpace) {
    if (isSpace) {
      suggestionsSessionState.spaceTimeTracker.markSpace();
    } else {
      suggestionsSessionState.spaceTimeTracker.clear();
    }
  }

  @Override
  public void onAlphabetKeyboardSet(@NonNull KeyboardDefinition keyboard) {
    super.onAlphabetKeyboardSet(keyboard);
    keyboardDictionariesLoader.reset();

    mFrenchSpacePunctuationBehavior =
        FrenchSpacePunctuationDecider.shouldEnable(mSwapPunctuationAndSpace, keyboard.getLocale());
  }

  public void revertLastWord() {
    WordRevertHandler.Result result =
        wordRevertHandler.revertLastWord(
            suggestionsSessionState.autoCorrectState,
            suggestionsSessionState.predictionState,
            suggestionsSessionState.wordComposerTracker.currentWord(),
            suggestionsSessionState.wordComposerTracker.previousWord(),
            wordRevertHost);
    suggestionsSessionState.wordComposerTracker.setWords(
        result.currentWord(), result.previousWord());
  }

  protected boolean isWordSeparator(int code) {
    return !isAlphabet(code);
  }

  public boolean preferCapitalization() {
    return suggestionsSessionState.wordComposerTracker.currentWord().isFirstCharCapitalized();
  }

  public void closeDictionaries() {
    mSuggest.closeDictionaries();
  }

  @Override
  public void onDisplayCompletions(CompletionInfo[] completions) {
    completionHandler.onDisplayCompletions(
        completions,
        new CompletionHostAdapter(
            this::isFullscreenMode,
            this::clearSuggestions,
            suggestions -> setSuggestions(suggestions.suggestions, suggestions.highlightedIndex),
            () ->
                suggestionsSessionState.wordComposerTracker.currentWord().setPreferredWord(null)));
  }

  public void checkAddToDictionaryWithAutoDictionary(
      CharSequence newWord, Suggest.AdditionType type) {
    suggestionsSessionState.autoCorrectState.justAutoAddedWord = false;

    // unfortunately, has to do it on the main-thread (because we checking mJustAutoAddedWord)
    if (mSuggest.tryToLearnNewWord(newWord, type)) {
      addWordToDictionary(newWord.toString());
      suggestionsSessionState.autoCorrectState.justAutoAddedWord = true;
    }
  }

  @CallSuper
  protected boolean isSuggestionAffectingCharacter(int code) {
    return Character.isLetter(code);
  }

  public void removeFromUserDictionary(String wordToRemove) {
    userDictionaryWorker.removeFromUserDictionary(wordToRemove, mInputSessionDisposables::add);
    suggestionsSessionState.autoCorrectState.justAutoAddedWord = false;
    abortCorrectionAndResetPredictionState(false);
  }

  AddToDictionaryHintController addToDictionaryHintController() {
    return addToDictionaryHintController;
  }
}
