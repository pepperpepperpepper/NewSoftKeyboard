/*
 * Copyright (c) 2015 Menny Even-Danan
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

package wtf.uhoh.newsoftkeyboard.app.ime;

import android.content.Intent;
import android.os.IBinder;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import com.google.android.voiceime.VoiceImeController.VoiceInputState;
import com.google.android.voiceime.VoiceImeTextPrecommitProcessor;
import java.util.List;
import java.util.Locale;
import wtf.uhoh.newsoftkeyboard.BuildConfig;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.debug.ImeStateTracker;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.ExternalDictionaryFactory;
import wtf.uhoh.newsoftkeyboard.app.ime.context.ContextProfilesController;
import wtf.uhoh.newsoftkeyboard.app.ime.hosts.ImeFunctionKeyHost;
import wtf.uhoh.newsoftkeyboard.app.ime.hosts.ImeModifierKeyStateHost;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.NextKeyboardType;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.InputViewBinder;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewBase;
import wtf.uhoh.newsoftkeyboard.app.ui.dev.DevStripActionProvider;
import wtf.uhoh.newsoftkeyboard.app.ui.dev.DeveloperUtils;
import wtf.uhoh.newsoftkeyboard.app.ui.support.VoiceInputNotInstalledActivity;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.nextword.prediction.NextWordContextTokenizer;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;

/** Input method implementation for QWERTY-ish keyboard. */
public abstract class ImeServiceBase extends ImeColorizeNavBar
    implements VoiceImeTextPrecommitProcessor {

  private static final Object INSTANCE_LOCK = new Object();
  @Nullable private static ImeServiceBase sInstance;

  @Nullable
  public static ImeServiceBase getInstance() {
    synchronized (INSTANCE_LOCK) {
      return sInstance;
    }
  }

  @Nullable
  public KeyboardViewBase getCurrentKeyboardViewForDebug() {
    final InputViewBinder binder = getInputView();
    if (binder instanceof KeyboardViewBase) {
      return (KeyboardViewBase) binder;
    }
    return null;
  }

  private PackageBroadcastRegistrar packageBroadcastRegistrar;

  private final StringBuilder mTextCapitalizerWorkspace = new StringBuilder();
  private boolean mShowKeyboardIconInStatusBar;

  private FunctionKeyHandler functionKeyHandler;
  private FunctionKeyHandler.Host functionKeyHandlerHost;
  @NonNull private final NonFunctionKeyHandler nonFunctionKeyHandler = new NonFunctionKeyHandler();
  private ModifierKeyStateHandler modifierKeyStateHandler;
  private InputViewLifecycleHandler inputViewLifecycleHandler;

  private DevStripActionProvider mDevToolsAction;
  private CondenseModeManager condenseModeManager;
  private KeyboardSwitchHandler keyboardSwitchHandler;
  private NavigationKeyHandler navigationKeyHandler;
  private StatusIconController statusIconController;
  private StatusIconHelper statusIconHelper;
  private final FullscreenModeDecider fullscreenModeDecider = new FullscreenModeDecider();
  private final FullscreenExtractViewController fullscreenExtractViewController =
      new FullscreenExtractViewController();
  @Nullable private ImeVoiceController voiceController;
  @Nullable private ImeContextProfilesEffectsController contextProfilesEffectsController;
  @Nullable private ImeSessionOverridesController sessionOverridesController;
  @Nullable private ImeProgrammableApiController programmableApiController;

  @Nullable private String lastEditorKeyForNextWordContext;

  @Nullable private DeleteActionHelper.Host deleteActionHost;

  private EmojiSearchController emojiSearchController;

  private boolean mAutoCap = true;
  private boolean mKeyboardAutoCap;
  private MultiTapEditCoordinator multiTapEditCoordinator;
  @Nullable private ShiftStateController shiftStateController;

  protected ImeServiceBase() {
    super();
  }

  @NonNull
  public ImeProgrammableApiController getProgrammableApiController() {
    if (programmableApiController == null) {
      programmableApiController = new ImeProgrammableApiController(this);
    }
    return programmableApiController;
  }

  @NonNull
  ImeSessionOverridesController getSessionOverridesController() {
    if (sessionOverridesController == null) {
      sessionOverridesController = new ImeSessionOverridesController(this);
    }
    return sessionOverridesController;
  }

  @NonNull
  private ImeFunctionKeyHost.ImeActions createFunctionKeyImeActions() {
    return new ImeFunctionKeyHost.ImeActions(
        this::handleFunction,
        this::handleBackWord,
        () -> handleDeleteLastCharacter(false),
        this::handleShift,
        primaryCode -> {
          onPress(primaryCode);
          onRelease(primaryCode);
        },
        this::handleForwardDelete,
        disabledUntilNextInputStart ->
            abortCorrectionAndResetPredictionState(disabledUntilNextInputStart),
        this::handleControl,
        this::handleAlt,
        this::updateVoiceKeyState,
        () -> {
          final Intent intent =
              new Intent(getApplicationContext(), VoiceInputNotInstalledActivity.class);
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(intent);
        },
        this::launchOpenAISettings,
        this::handleCloseRequest,
        this::hideWindow,
        this::showOptionsMenu,
        this::handleEmojiSearchRequest);
  }

  @NonNull
  private ImeModifierKeyStateHost.Actions createModifierKeyStateActions() {
    return new ImeModifierKeyStateHost.Actions(
        this::toggleCaseOfSelectedCharacters,
        this::handleShift,
        this::handleControl,
        this::handleAlt,
        this::handleFunction,
        this::updateShiftStateNow,
        this::updateVoiceKeyState);
  }

  @NonNull
  private DeleteActionHelper.Host getDeleteActionHost() {
    if (deleteActionHost == null) {
      deleteActionHost = new ImeDeleteActionHost(this);
    }
    return deleteActionHost;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    synchronized (INSTANCE_LOCK) {
      sInstance = this;
    }
    voiceController = new ImeVoiceController(this);
    contextProfilesEffectsController = new ImeContextProfilesEffectsController(this);
    getShiftStateController();
    if (!BuildConfig.DEBUG && BuildConfig.VERSION_NAME.endsWith("-SNAPSHOT")) {
      throw new RuntimeException("You can not run a 'RELEASE' build with a SNAPSHOT postfix!");
    }

    addDisposable(WindowAnimationSetter.subscribe(this, getWindow().getWindow()));

    final ImeServiceInitializer.Result init =
        ImeServiceInitializer.initialize(
            this,
            super.getInputMethodManager(),
            this::setKeyboardForView,
            this::handleCloseRequest,
            createModifierKeyStateActions(),
            createFunctionKeyImeActions(),
            this::updateVoiceKeyState,
            this::updateShiftStateNow,
            this::commitEmojiFromSearch,
            this::showLanguageSelectionDialog,
            this::nextKeyboard,
            this::nextAlterKeyboard,
            this::sendNavigationKeyEvent,
            this::sendDownUpKeyEvents,
            aBoolean -> mAutoCap = aBoolean,
            aBoolean -> mShowKeyboardIconInStatusBar = aBoolean,
            () -> mShowKeyboardIconInStatusBar,
            this::updateSpaceBarRecordingStatus,
            this::updateVoiceInputStatus);
    multiTapEditCoordinator = init.multiTapEditCoordinator();
    modifierKeyStateHandler = init.modifierKeyStateHandler();
    inputViewLifecycleHandler = init.inputViewLifecycleHandler();
    emojiSearchController = init.emojiSearchController();
    condenseModeManager = init.condenseModeManager();
    keyboardSwitchHandler = init.keyboardSwitchHandler();
    navigationKeyHandler = init.navigationKeyHandler();
    functionKeyHandlerHost = init.functionKeyHandlerHost();
    functionKeyHandler = init.functionKeyHandler();
    statusIconController = init.statusIconController();
    statusIconHelper = init.statusIconHelper();
    packageBroadcastRegistrar = init.packageBroadcastRegistrar();
    final ImeVoiceController voiceControllerRef = voiceController;
    if (voiceControllerRef != null) {
      voiceControllerRef.onVoiceInitialized(
          init.voiceRecognitionTrigger(), init.voiceImeController());
    }

    mDevToolsAction = new DevStripActionProvider(this);
  }

  @NonNull
  private ShiftStateController getShiftStateController() {
    if (shiftStateController == null) {
      shiftStateController =
          new ShiftStateController(
              mShiftKeyState,
              () -> mAutoCap,
              this::getCurrentKeyboard,
              this::getCurrentAlphabetKeyboard,
              this::getInputView,
              getImeSessionState().getInputConnectionRouter(),
              getImeSessionState()::currentEditorInfo,
              TAG);
    }
    return shiftStateController;
  }

  @Override
  public void onDestroy() {
    Logger.i(TAG, "ImeServiceBase has been destroyed! Cleaning resources..");
    final ImeVoiceController controller = voiceController;
    if (controller != null) controller.onDestroy();
    if (packageBroadcastRegistrar != null) {
      packageBroadcastRegistrar.unregister();
    }

    final IBinder imeToken = getImeToken();
    if (imeToken != null) super.getInputMethodManager().hideStatusIcon(imeToken);

    hideWindow();

    if (DeveloperUtils.hasTracingStarted()) {
      DeveloperUtils.stopTracing();
      Toast.makeText(
              getApplicationContext(),
              getString(R.string.debug_tracing_finished, DeveloperUtils.getTraceFile()),
              Toast.LENGTH_SHORT)
          .show();
    }

    synchronized (INSTANCE_LOCK) {
      if (sInstance == this) {
        sInstance = null;
      }
    }
    super.onDestroy();
  }

  public void onCriticalPackageChanged(Intent eventIntent) {
    if (((NskApplicationBase) getApplication()).onPackageChanged(eventIntent)) {
      onAddOnsCriticalChange();
    }
  }

  @Override
  public void onStartInput(EditorInfo attribute, boolean restarting) {
    super.onStartInput(attribute, restarting);
    statusIconHelper.onStartInput();
  }

  @Override
  protected boolean shouldResetNextWordContextOnStartInput(
      @NonNull EditorInfo attribute, boolean restarting) {
    final String previousKey = lastEditorKeyForNextWordContext;
    final String currentKey = buildEditorKeyForNextWordContext(attribute);
    // Some editors/devices can provide incomplete EditorInfo during onStartInput(...) (e.g. missing
    // packageName), and we don't want to wipe next-word context based on an unstable identity.
    // Defer the reset decision to onStartInputView(...), where EditorInfo is typically stable.
    if (currentKey == null) return false;
    final boolean sameEditor = currentKey != null && currentKey.equals(previousKey);
    final boolean readbackBlocked = isEditorTextReadbackBlockedForNextWordContext(attribute);
    final boolean cursorHasContext = attribute.initialSelStart > 0 || attribute.initialSelEnd > 0;
    final boolean preserveNextWordContext =
        restarting || (readbackBlocked && sameEditor && cursorHasContext);
    return !preserveNextWordContext;
  }

  @Override
  public View onCreateInputView() {
    final View view = super.onCreateInputView();
    getSessionOverridesController().updateSessionOverridesIndicator();
    return view;
  }

  @Nullable
  private static String buildEditorKeyForNextWordContext(@NonNull EditorInfo editorInfo) {
    final String pkg = editorInfo.packageName;
    if (pkg == null || pkg.isEmpty()) return null;
    // Keep this compact and stable. Avoid storing editor text/hints (privacy).
    return pkg
        + ":"
        + editorInfo.fieldId
        + ":"
        + editorInfo.inputType
        + ":"
        + editorInfo.imeOptions;
  }

  private boolean isEditorTextReadbackBlockedForNextWordContext(@NonNull EditorInfo editorInfo) {
    if (isTextPassword(editorInfo) || isNumberPassword(editorInfo)) return true;
    if ((editorInfo.imeOptions & EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0) return true;
    if ((editorInfo.inputType & EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) return true;
    return getSuggest().isIncognitoMode();
  }

  @Override
  public void onStartInputView(final EditorInfo attribute, final boolean restarting) {
    Logger.v(
        TAG,
        "onStartInputView(EditorInfo{imeOptions %d, inputType %d}, restarting %s",
        attribute.imeOptions,
        attribute.inputType,
        restarting);

    super.onStartInputView(attribute, restarting);
    getSessionOverridesController().maybeClearSessionOverridesForNewEditor(attribute);
    getSessionOverridesController().applySessionKeyboardOverrideIfAny(attribute);
    final ImeContextProfilesEffectsController contextController = contextProfilesEffectsController;
    if (contextController != null) contextController.onStartInputView(attribute);
    inputViewLifecycleHandler.onStartInputView(TAG, attribute, restarting, mDevToolsAction);
    getSessionOverridesController().updateSessionOverridesIndicator();

    // Ensure next-word context does not leak across editors when editor seeding is blocked. In
    // those privacy-restricted cases we should preserve in-memory context when we appear to be
    // returning to the same editor, even if `restarting=false` (some apps/devices restart the IME
    // view this way on pause/resume).
    final String previousKey = lastEditorKeyForNextWordContext;
    final String currentKey = buildEditorKeyForNextWordContext(attribute);
    final boolean sameEditor = currentKey != null && currentKey.equals(previousKey);
    final boolean readbackBlocked = isEditorTextReadbackBlockedForNextWordContext(attribute);
    final boolean cursorHasContext = attribute.initialSelStart > 0 || attribute.initialSelEnd > 0;
    final boolean preserveNextWordContext =
        restarting || (readbackBlocked && sameEditor && cursorHasContext);
    lastEditorKeyForNextWordContext = currentKey;
    if (!preserveNextWordContext) {
      getSuggest().resetNextWordSentence();
      clearLastCommittedWordForNextSuggestions();
    }

    maybeSeedNextWordEngineContextFromEditor(attribute);
  }

  private void maybeSeedNextWordEngineContextFromEditor(@NonNull EditorInfo attribute) {
    if (isTextPassword(attribute) || isNumberPassword(attribute)) return;
    if ((attribute.imeOptions & EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0) return;
    if ((attribute.inputType & EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) return;
    if (getSuggest().isIncognitoMode()) return;

    final InputConnectionRouter router = getImeSessionState().getInputConnectionRouter();
    if (!router.hasConnection()) return;
    CharSequence beforeCursor = null;
    ExtractedText extractedText = null;
    try {
      beforeCursor = router.getTextBeforeCursor(4096, 0);
    } catch (Throwable t) {
      // Some editors can throw while we query context. This is best-effort only.
    }
    if (beforeCursor == null || beforeCursor.length() == 0) {
      try {
        final ExtractedTextRequest request = new ExtractedTextRequest();
        request.hintMaxChars = 4096;
        request.hintMaxLines = 10;
        extractedText = router.getExtractedText(request);
        if (extractedText != null
            && extractedText.text != null
            && extractedText.text.length() > 0) {
          final int selEnd =
              Math.max(0, Math.min(extractedText.selectionEnd, extractedText.text.length()));
          final int start = Math.max(0, selEnd - 4096);
          beforeCursor = extractedText.text.subSequence(start, selEnd);
        }
      } catch (Throwable t) {
        // Best-effort only; some editors can throw while we query extracted text.
        return;
      }
    }
    if (beforeCursor == null || beforeCursor.length() == 0) return;
    CharSequence afterCursor = null;
    try {
      afterCursor = router.getTextAfterCursor(2, 0);
    } catch (Throwable t) {
      // Best-effort only; some editors can throw while we query context.
    }

    CharSequence seedTextBeforeCursor = beforeCursor;
    if (isCursorInsideTokenForContextSeeding(beforeCursor, afterCursor, extractedText)) {
      seedTextBeforeCursor = trimTrailingTokenFragment(beforeCursor);
    }
    getSuggest().seedNextWordEngineContextFromEditorText(seedTextBeforeCursor);

    // If we have no in-session committed token yet, seed a best-effort "previous word" from the
    // editor text so separator-driven next-word fallback requests work when the user resumes
    // typing in an existing field.
    if (lastCommittedWordForNextSuggestions().length() > 0) return;
    if (afterCursor != null
        && afterCursor.length() > 0
        && !isWordSeparator(afterCursor.charAt(0))) {
      return; // cursor is at the start/inside a token; let completion/correction flows handle it
    }
    if (afterCursor == null) {
      // We couldn't read the next character. Best-effort: use ExtractedText if available to
      // determine whether the cursor is touching a token.
      if (extractedText == null) {
        try {
          final ExtractedTextRequest request = new ExtractedTextRequest();
          request.hintMaxChars = 256;
          request.hintMaxLines = 3;
          extractedText = router.getExtractedText(request);
        } catch (Throwable t) {
          extractedText = null;
        }
      }
      if (extractedText == null || extractedText.text == null || extractedText.text.length() == 0)
        return;
      final int selEnd = extractedText.selectionEnd;
      if (selEnd < 0) return;
      if (selEnd < extractedText.text.length()
          && !isWordSeparator(extractedText.text.charAt(selEnd))) {
        return; // cursor is at the start/inside a token
      }
    }
    final List<String> tokens = NextWordContextTokenizer.tokenizeTextBeforeCursor(beforeCursor, 64);
    if (tokens.isEmpty()) return;
    seedLastCommittedWordForNextSuggestionsFromEditorText(tokens.get(tokens.size() - 1));
  }

  private static boolean isContextTokenChar(char c) {
    if (Character.isLetterOrDigit(c)) return true;
    // allow common intra-token joiners (matches NextWordContextTokenizer)
    return c == '\'' || c == '\u2019' || c == '-' || c == '_';
  }

  private static boolean isCursorInsideTokenForContextSeeding(
      @NonNull CharSequence beforeCursor,
      @Nullable CharSequence afterCursor,
      @Nullable ExtractedText extractedText) {
    if (beforeCursor.length() == 0) return false;
    final char lastBefore = beforeCursor.charAt(beforeCursor.length() - 1);
    if (!isContextTokenChar(lastBefore)) return false;

    if (afterCursor != null && afterCursor.length() > 0) {
      return isContextTokenChar(afterCursor.charAt(0));
    }
    if (afterCursor == null
        && extractedText != null
        && extractedText.text != null
        && extractedText.text.length() > 0) {
      final int selEnd = extractedText.selectionEnd;
      if (selEnd >= 0 && selEnd < extractedText.text.length()) {
        return isContextTokenChar(extractedText.text.charAt(selEnd));
      }
    }
    return false;
  }

  @NonNull
  private static CharSequence trimTrailingTokenFragment(@NonNull CharSequence beforeCursor) {
    int i = beforeCursor.length() - 1;
    while (i >= 0 && isContextTokenChar(beforeCursor.charAt(i))) i--;
    return beforeCursor.subSequence(0, i + 1);
  }

  @Nullable
  ContextProfilesController getContextProfilesController() {
    final ImeContextProfilesEffectsController controller = contextProfilesEffectsController;
    return controller != null ? controller.getContextProfilesController() : null;
  }

  @Nullable
  ContextProfilesStore getContextProfilesStore() {
    final ImeContextProfilesEffectsController controller = contextProfilesEffectsController;
    return controller != null ? controller.getContextProfilesStore() : null;
  }

  boolean areContextProfilesTemporarilyDisabled() {
    final ImeContextProfilesEffectsController controller = contextProfilesEffectsController;
    return controller != null && controller.areTemporarilyDisabled();
  }

  void applyContextProfileOverridesForCurrentEditor(@NonNull EditorInfo editorInfo) {
    final ImeContextProfilesEffectsController controller = contextProfilesEffectsController;
    if (controller != null) controller.applyContextProfileOverridesForCurrentEditor(editorInfo);
  }

  void clearContextProfileOverrides() {
    final ImeContextProfilesEffectsController controller = contextProfilesEffectsController;
    if (controller != null) controller.clearContextProfileOverrides();
  }

  @Override
  @NonNull
  public String onVoiceTextPreCommit(@NonNull String formattedText) {
    final ImeContextProfilesEffectsController controller = contextProfilesEffectsController;
    return controller != null ? controller.onVoiceTextPreCommit(formattedText) : formattedText;
  }

  void applyContextProfileTypedRulesForEditorAction(@Nullable EditorInfo editorInfo) {
    final ImeContextProfilesEffectsController controller = contextProfilesEffectsController;
    if (controller != null) controller.applyContextProfileTypedRulesForEditorAction(editorInfo);
  }

  @Override
  public void onFinishInput() {
    super.onFinishInput();

    statusIconHelper.onFinishInput();
  }

  @Override
  public void onFinishInputView(boolean finishingInput) {
    ImeStateTracker.onKeyboardHidden();
    getSessionOverridesController().clearSessionOverridesOnFinishInputView();
    super.onFinishInputView(finishingInput);
    inputViewLifecycleHandler.onFinishInputView(mDevToolsAction);
  }

  @Override
  public boolean onEvaluateFullscreenMode() {
    return fullscreenModeDecider.shouldUseFullscreen(
        currentInputEditorInfo(),
        getCurrentOrientation(),
        mUseFullScreenInputInPortrait,
        mUseFullScreenInputInLandscape);
  }

  /** Helper to determine if a given character code is alphabetic. */
  @Override
  protected boolean isAlphabet(int code) {
    if (super.isAlphabet(code)) return true;
    // inner letters have more options: ' in English. " in Hebrew, and spacing and non-spacing
    // combining characters.
    final KeyboardDefinition currentAlphabetKeyboard = getCurrentAlphabetKeyboard();
    if (currentAlphabetKeyboard == null) return false;

    if (getCurrentComposedWord().isEmpty()) {
      return currentAlphabetKeyboard.isStartOfWordLetter(code);
    } else {
      return currentAlphabetKeyboard.isInnerWordLetter(code);
    }
  }

  @Override
  public void onMultiTapStarted() {
    multiTapEditCoordinator.onMultiTapStarted(
        () -> {
          handleDeleteLastCharacter(true);
          super.onMultiTapStarted();
        });
  }

  @Override
  public void onMultiTapEnded() {
    multiTapEditCoordinator.onMultiTapEnded(this::updateShiftStateNow);
  }

  void updateVoiceKeyState() {
    final ImeVoiceController controller = voiceController;
    if (controller != null) controller.updateVoiceKeyState();
  }

  /**
   * Updates the space bar text to show recording status. This provides clear visual feedback when
   * voice recording is active.
   */
  void updateSpaceBarRecordingStatus(boolean isRecording) {
    final ImeVoiceController controller = voiceController;
    if (controller != null) controller.updateSpaceBarRecordingStatus(isRecording);
  }

  void updateVoiceInputStatus(VoiceInputState newState) {
    final ImeVoiceController controller = voiceController;
    if (controller != null) controller.updateVoiceInputStatus(newState);
  }

  void handleEmojiSearchRequest() {
    emojiSearchController.requestShow();
  }

  private void commitEmojiFromSearch(CharSequence emoji) {
    super.onText(null, emoji);
  }

  // convert ASCII codes to Android KeyEvent codes
  // ASCII Codes Table: https://ascii.cl
  @Override
  public void onKey(
      int primaryCode, Keyboard.Key key, int multiTapIndex, int[] nearByKeyCodes, boolean fromUI) {
    // Ensure editor state tracker is in sync before applying wrap/separator logic.
    getCursorPosition();
    final ImeContextProfilesEffectsController contextController = contextProfilesEffectsController;
    if (contextController != null) contextController.clearVoiceSuggestionState();

    final InputConnectionRouter inputConnectionRouter =
        getImeSessionState().getInputConnectionRouter();
    try (final InputConnectionRouter.BatchEditScope batchEdit = inputConnectionRouter.batchEdit()) {
      batchEdit.noop();
      boolean handledByOverlay = emojiSearchController.handleOverlayKey(primaryCode, key);
      if (!handledByOverlay) {
        super.onKey(primaryCode, key, multiTapIndex, nearByKeyCodes, fromUI);
        if (primaryCode > 0) {
          nonFunctionKeyHandler.handle(
              this,
              primaryCode,
              key,
              multiTapIndex,
              nearByKeyCodes,
              this::sendDownUpKeyEvents,
              () -> sendKeyChar((char) 27));
        } else {
          if (BuildConfig.DEBUG) Logger.d(TAG, "onFunctionKey %d", primaryCode);
          functionKeyHandler.handle(primaryCode, key, fromUI);
        }
      }
    }
  }

  @Override
  public void onText(Keyboard.Key key, CharSequence text) {
    if (emojiSearchController.handleOverlayText(text)) {
      return;
    }
    final ImeContextProfilesEffectsController contextController = contextProfilesEffectsController;
    if (contextController != null) contextController.clearVoiceSuggestionState();
    super.onText(key, text);
  }

  @Override
  public void pickSuggestionManually(
      int index, CharSequence suggestion, boolean withAutoSpaceEnabled) {
    final ImeContextProfilesEffectsController controller = contextProfilesEffectsController;
    if (controller != null
        && controller.handlePickSuggestionManually(index, suggestion, withAutoSpaceEnabled)) {
      return;
    }
    super.pickSuggestionManually(index, suggestion, withAutoSpaceEnabled);
  }

  /* package */ void pickSuggestionManuallyFromContextProfilesController(
      int index, @Nullable CharSequence suggestion, boolean withAutoSpaceEnabled) {
    super.pickSuggestionManually(index, suggestion, withAutoSpaceEnabled);
  }

  @Override
  public void handleSeparator(int primaryCode) {
    super.handleSeparator(primaryCode);
    final EditorInfo editorInfo = currentInputEditorInfo();
    if (editorInfo == null) return;
    final ImeContextProfilesEffectsController controller = contextProfilesEffectsController;
    if (controller != null) controller.applyContextProfileTypedRulesOnSeparator(editorInfo);
  }

  @Override
  public void onAlphabetKeyboardSet(@NonNull KeyboardDefinition keyboard) {
    super.onAlphabetKeyboardSet(keyboard);
    setKeyboardFinalStuff();
    mKeyboardAutoCap = keyboard.autoCap;
    ImeStateTracker.onKeyboardVisible(keyboard, currentInputEditorInfo());
    InputViewBinder inputView = getInputView();
    if (inputView instanceof wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewBase) {
      ImeStateTracker.reportKeyboardView(
          (wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewBase) inputView);
    } else {
      ImeStateTracker.reportKeyboardView(null);
    }
  }

  @Override
  protected void setKeyboardForView(@NonNull KeyboardDefinition currentKeyboard) {
    currentKeyboard.setCondensedKeys(condenseModeManager.getCurrentMode());
    super.setKeyboardForView(currentKeyboard);
  }

  private void showLanguageSelectionDialog() {
    LanguageSelectionDialog.show(new ImeLanguageSelectionDialogHost(this));
  }

  @Override
  public View onCreateExtractTextView() {
    return fullscreenExtractViewController.onCreateExtractTextView(super.onCreateExtractTextView());
  }

  @Override
  public void updateFullscreenMode() {
    super.updateFullscreenMode();
    fullscreenExtractViewController.updateFullscreenMode(getInputView());
  }

  @Override
  protected void handleBackWord() {
    BackWordDeleter.handleBackWord(
        getImeSessionState().getInputConnectionRouter(),
        this::markExpectingSelectionUpdate,
        this::postUpdateSuggestions,
        getCurrentComposedWord(),
        isPredictionOn(),
        getSuggest());
  }

  void handleDeleteLastCharacter(boolean forMultiTap) {
    if (shouldRevertOnDelete()) {
      revertLastWord();
      return;
    }
    DeleteActionHelper.handleDeleteLastCharacter(
        getDeleteActionHost(),
        getImeSessionState().getInputConnectionRouter(),
        getCurrentComposedWord(),
        forMultiTap);
  }

  void handleForwardDelete() {
    DeleteActionHelper.handleForwardDelete(
        getDeleteActionHost(),
        getImeSessionState().getInputConnectionRouter(),
        getCurrentComposedWord());
  }

  void handleControl() {
    if (getInputView() != null) {
      getInputView().setControl(mControlKeyState.isActive());
    }
  }

  void handleAlt() {
    if (getInputView() != null) {
      getInputView().setAlt(mAltKeyState.isActive(), mAltKeyState.isLocked());
    }
  }

  void handleFunction() {
    if (getInputView() != null) {
      getInputView().setFunction(mFunctionKeyState.isActive(), mFunctionKeyState.isLocked());
    }
  }

  private void sendNavigationKeyEvent(int keyEventCode) {
    final boolean temporarilyDisableShift = getInputView() != null && getInputView().isShifted();
    if (temporarilyDisableShift) {
      getInputView().setShifted(false);
    }
    sendDownUpKeyEvents(keyEventCode);
    if (temporarilyDisableShift) {
      handleShift();
    }
  }

  void handleShift() {
    getShiftStateController().applyShiftStateToKeyboardAndView();
  }

  void toggleCaseOfSelectedCharacters() {
    if (getSelectionStartPositionDangerous() == getCursorPosition()) return;
    final ExtractedText et = getExtractedText();
    KeyboardDefinition currentAlphabetKeyboard = getCurrentAlphabetKeyboard();
    @NonNull
    Locale locale =
        currentAlphabetKeyboard != null ? currentAlphabetKeyboard.getLocale() : Locale.ROOT;
    SelectionEditHelper.toggleCaseOfSelectedCharacters(
        et, getImeSessionState().getInputConnectionRouter(), mTextCapitalizerWorkspace, locale);
  }

  @Override
  protected boolean handleCloseRequest() {
    return emojiSearchController.dismissOverlay()
        || super.handleCloseRequest()
        || (getInputView() != null && getInputView().resetInputView());
  }

  @Override
  public void onWindowHidden() {
    super.onWindowHidden();
    emojiSearchController.onWindowHidden();

    // Window-hide is often a transient lifecycle event (pause/resume, app switching, etc.). Avoid
    // wiping next-word context here; editor-boundary + privacy resets are handled in
    // onStartInputView(...).
    abortCorrectionAndResetPredictionState(true, /* resetNextWordSentence= */ false);
  }

  private void nextAlterKeyboard(EditorInfo currentEditorInfo) {
    getKeyboardSwitcher().nextAlterKeyboard(currentEditorInfo);

    Logger.d(
        TAG,
        "nextAlterKeyboard: Setting next keyboard to: %s",
        getCurrentSymbolsKeyboard().getKeyboardName());
  }

  private void nextKeyboard(EditorInfo currentEditorInfo, NextKeyboardType type) {
    getKeyboardSwitcher().nextKeyboard(currentEditorInfo, type);
  }

  private void setKeyboardFinalStuff() {
    mShiftKeyState.reset();
    mControlKeyState.reset();
    mVoiceKeyState.reset();
    mAltKeyState.reset();
    mFunctionKeyState.reset();
    // changing dictionary
    setDictionariesForCurrentKeyboard();
    // Notifying if needed
    statusIconHelper.onStartInput();
    clearSuggestions();
    updateShiftStateNow();
    handleControl();
    handleAlt();
    handleFunction();
  }

  @Override
  public void onPress(int primaryCode) {
    super.onPress(primaryCode);
    if (primaryCode == KeyCodes.SHIFT || primaryCode == KeyCodes.SHIFT_LOCK) {
      getShiftStateController().markManualShiftState();
    }
    modifierKeyStateHandler.onPress(primaryCode);
  }

  @Override
  public void onRelease(int primaryCode) {
    super.onRelease(primaryCode);
    modifierKeyStateHandler.onRelease(primaryCode, mMultiTapTimeout, mLongPressTimeout);
    if (isWordSeparator(primaryCode)) {
      updateShiftStateNow();
    }
  }

  private void launchSettings() {
    hideWindow();
    SettingsLauncher.launch(this);
  }

  void launchOpenAISettings() {
    hideWindow();
    SettingsLauncher.launchOpenAI(this);
  }

  private void launchDictionaryOverriding() {
    DictionaryOverrideDialog.show(new ImeDictionaryOverrideDialogHost(this));
  }

  void showOptionsMenu() {
    OptionsMenuLauncher.show(new ImeOptionsMenuHost(this));
  }

  boolean isContextProfilesGloballyEnabledForOptionsMenu() {
    final ImeContextProfilesEffectsController controller = contextProfilesEffectsController;
    return controller != null && controller.isContextProfilesGloballyEnabledForOptionsMenu();
  }

  boolean isContextProfilesEnabledForOptionsMenu() {
    final ImeContextProfilesEffectsController controller = contextProfilesEffectsController;
    return controller != null && controller.isContextProfilesEnabledForOptionsMenu();
  }

  void setContextProfilesTemporarilyDisabledForOptionsMenu(boolean disabled) {
    final ImeContextProfilesEffectsController controller = contextProfilesEffectsController;
    if (controller != null) controller.setTemporarilyDisabledForOptionsMenu(disabled);
  }

  @Override
  protected void onOrientationChanged(int oldOrientation, int newOrientation) {
    super.onOrientationChanged(oldOrientation, newOrientation);
    condenseModeManager.updateForOrientation(newOrientation);
  }

  @Override
  public void onSharedPreferenceChange(String key) {
    if (ExternalDictionaryFactory.isOverrideDictionaryPrefKey(key)) {
      invalidateDictionariesForCurrentKeyboard();
      setDictionariesForCurrentKeyboard();
    } else {
      super.onSharedPreferenceChange(key);
    }
  }

  @Override
  public void deleteLastCharactersFromInput(int countToDelete) {
    DeleteActionHelper.deleteLastCharactersFromInput(
        getDeleteActionHost(),
        getImeSessionState().getInputConnectionRouter(),
        getCurrentComposedWord(),
        countToDelete);
  }

  @Override
  public void onUpdateSelection(
      int oldSelStart,
      int oldSelEnd,
      int newSelStart,
      int newSelEnd,
      int candidatesStart,
      int candidatesEnd) {
    // only updating if the cursor moved
    if (oldSelStart != newSelStart) {
      updateShiftStateNow();
    }
    super.onUpdateSelection(
        oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
    final ImeContextProfilesEffectsController controller = contextProfilesEffectsController;
    if (controller != null) controller.showPendingVoiceSuggestionIfAny();
  }

  void updateShiftStateNow() {
    getShiftStateController().updateShiftStateNow();
  }
}
