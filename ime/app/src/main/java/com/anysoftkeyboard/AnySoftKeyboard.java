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

package com.anysoftkeyboard;

import android.content.Intent;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.ExternalDictionaryFactory;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.ime.AnySoftKeyboardColorizeNavBar;
import com.anysoftkeyboard.ime.CondenseModeManager;
import com.anysoftkeyboard.ime.DictionaryOverrideDialog;
import com.anysoftkeyboard.ime.EmojiSearchController;
import com.anysoftkeyboard.ime.FullscreenModeDecider;
import com.anysoftkeyboard.ime.FunctionKeyHandler;
import com.anysoftkeyboard.ime.InputViewLifecycleHandler;
import com.anysoftkeyboard.ime.KeyboardSwitchHandler;
import com.anysoftkeyboard.ime.NavigationKeyHandler;
import com.anysoftkeyboard.ime.InputViewBinder;
import com.anysoftkeyboard.ime.LanguageSelectionDialog;
import com.anysoftkeyboard.ime.ModifierKeyStateHandler;
import com.anysoftkeyboard.ime.MultiTapEditCoordinator;
import com.anysoftkeyboard.ime.OptionsMenuLauncher;
import com.anysoftkeyboard.ime.PackageBroadcastRegistrar;
import com.anysoftkeyboard.ime.SettingsLauncher;
import com.anysoftkeyboard.ime.StatusIconController;
import com.anysoftkeyboard.ime.StatusIconHelper;
import com.anysoftkeyboard.ime.VoiceInputController;
import com.anysoftkeyboard.ime.VoiceKeyUiUpdater;
import com.anysoftkeyboard.ime.VoiceStatusRenderer;
import com.anysoftkeyboard.ime.VoiceUiHelper;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.keyboards.CondenseType;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.KeyboardSwitcher;
import com.anysoftkeyboard.keyboards.views.AnyKeyboardView;
import com.anysoftkeyboard.keyboards.views.AnyKeyboardViewBase;
import com.anysoftkeyboard.ime.WindowAnimationSetter;
import com.anysoftkeyboard.ui.dev.DevStripActionProvider;
import com.anysoftkeyboard.ui.dev.DeveloperUtils;
import com.anysoftkeyboard.ime.VoiceInputController.VoiceInputState;
import com.anysoftkeyboard.utils.IMEUtil;
import com.google.android.voiceime.VoiceRecognitionTrigger;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.BuildConfig;
import com.menny.android.anysoftkeyboard.R;
import com.anysoftkeyboard.debug.ImeStateTracker;
import wtf.uhoh.newsoftkeyboard.ime.EmojiSearchControllerHost;
import wtf.uhoh.newsoftkeyboard.ime.KeyboardSwitchHandlerHost;
import wtf.uhoh.newsoftkeyboard.ime.NavigationKeyHandlerHost;
import java.util.Locale;
import net.evendanan.pixel.GeneralDialogController;

/** Input method implementation for QWERTY-ish keyboard. */
public abstract class AnySoftKeyboard extends AnySoftKeyboardColorizeNavBar {

  private PackageBroadcastRegistrar packageBroadcastRegistrar;

  private final StringBuilder mTextCapitalizerWorkspace = new StringBuilder();
  private boolean mShowKeyboardIconInStatusBar;

  @NonNull private final SpecialWrapHelper specialWrapHelper = new SpecialWrapHelper();

  private FunctionKeyHandler functionKeyHandler;
  private FunctionKeyHandler.Host functionKeyHandlerHost;
  private ModifierKeyStateHandler modifierKeyStateHandler;
  private InputViewLifecycleHandler inputViewLifecycleHandler;

  private DevStripActionProvider mDevToolsAction;
  private CondenseModeManager condenseModeManager;
  private KeyboardSwitchHandler keyboardSwitchHandler;
  private NavigationKeyHandler navigationKeyHandler;
  private InputMethodManager mInputMethodManager;
  private StatusIconController statusIconController;
  private StatusIconHelper statusIconHelper;
  private VoiceRecognitionTrigger mVoiceRecognitionTrigger;
  private VoiceInputController voiceInputController;
  private VoiceStatusRenderer voiceStatusRenderer = new VoiceStatusRenderer();
  private VoiceUiHelper voiceUiHelper;
  private final VoiceKeyUiUpdater voiceKeyUiUpdater = new VoiceKeyUiUpdater();
  private final FullscreenModeDecider fullscreenModeDecider = new FullscreenModeDecider();
  private View mFullScreenExtractView;
  private EditText mFullScreenExtractTextView;

  @Nullable private DeleteActionHelper.Host deleteActionHost;

  private EmojiSearchController emojiSearchController;

  private boolean mAutoCap = true;
  private boolean mKeyboardAutoCap;
  private MultiTapEditCoordinator multiTapEditCoordinator;

  private static CondenseType parseCondenseType(String prefCondenseType) {
    switch (prefCondenseType) {
      case "split":
        return CondenseType.Split;
      case "compact_right":
        return CondenseType.CompactToRight;
      case "compact_left":
        return CondenseType.CompactToLeft;
      default:
        return CondenseType.None;
    }
  }

  protected AnySoftKeyboard() {
    super();
  }

  @Nullable
  InputConnection currentInputConnectionForFunctionKeyHandler() {
    return currentInputConnection();
  }

  @NonNull
  private DeleteActionHelper.Host getDeleteActionHost() {
    if (deleteActionHost == null) {
      deleteActionHost = new AnySoftKeyboardDeleteActionHost(this);
    }
    return deleteActionHost;
  }

  boolean isPredictionOnForDeleteActionHelperHost() {
    return isPredictionOn();
  }

  int getCursorPositionForDeleteActionHelperHost() {
    return getCursorPosition();
  }

  boolean isSelectionUpdateDelayedForDeleteActionHelperHost() {
    return isSelectionUpdateDelayed();
  }

  void markExpectingSelectionUpdateForDeleteActionHelperHost() {
    markExpectingSelectionUpdate();
  }

  void postUpdateSuggestionsForDeleteActionHelperHost() {
    postUpdateSuggestions();
  }

  void sendDownUpKeyEventsForDeleteActionHelperHost(int keyCode) {
    sendDownUpKeyEvents(keyCode);
  }

  boolean isFunctionKeyActiveForFunctionKeyHandler() {
    return mFunctionKeyState.isActive();
  }

  boolean isFunctionKeyLockedForFunctionKeyHandler() {
    return mFunctionKeyState.isLocked();
  }

  void consumeOneShotFunctionKeyForFunctionKeyHandler() {
    if (mFunctionKeyState.isActive() && !mFunctionKeyState.isLocked()) {
      mFunctionKeyState.setActiveState(false);
      handleFunction();
    }
  }

  boolean shouldBackWordDeleteForFunctionKeyHandler() {
    return mUseBackWord && mShiftKeyState.isPressed() && !mShiftKeyState.isLocked();
  }

  void toggleShiftLockedForFunctionKeyHandler() {
    mShiftKeyState.toggleLocked();
  }

  void abortCorrectionAndResetPredictionStateForFunctionKeyHandler(
      boolean disabledUntilNextInputStart) {
    abortCorrectionAndResetPredictionState(disabledUntilNextInputStart);
  }

  boolean isVoiceRecognitionInstalledForFunctionKeyHandler() {
    return mVoiceRecognitionTrigger != null && mVoiceRecognitionTrigger.isInstalled();
  }

  @NonNull
  String defaultDictionaryLocaleForFunctionKeyHandler() {
    final AnyKeyboard keyboard = getCurrentAlphabetKeyboard();
    if (keyboard == null) return Locale.ROOT.toString();
    return keyboard.getDefaultDictionaryLocale();
  }

  void startVoiceRecognitionForFunctionKeyHandler(@NonNull String locale) {
    if (mVoiceRecognitionTrigger != null) {
      mVoiceRecognitionTrigger.startVoiceRecognition(locale);
    }
  }

  void onQuickTextRequestedForFunctionKeyHandler(@Nullable Keyboard.Key key) {
    onQuickTextRequested(key);
  }

  void onQuickTextKeyboardRequestedForFunctionKeyHandler(@Nullable Keyboard.Key key) {
    onQuickTextKeyboardRequested(key);
  }

  void handleClipboardOperationForFunctionKeyHandler(
      @Nullable Keyboard.Key key, int primaryCode, @NonNull InputConnection ic) {
    handleClipboardOperation(key, primaryCode, ic);
  }

  void handleMediaInsertionKeyForFunctionKeyHandler() {
    handleMediaInsertionKey();
  }

  void clearQuickTextHistoryForFunctionKeyHandler() {
    getQuickKeyHistoryRecords().clearHistory();
  }

  @NonNull
  KeyboardSwitcher getKeyboardSwitcherForLanguageSelectionDialogHost() {
    return getKeyboardSwitcher();
  }

  void showOptionsDialogWithDataForLanguageSelectionDialogHost(
      int titleResId,
      int iconResId,
      CharSequence[] items,
      android.content.DialogInterface.OnClickListener listener) {
    showOptionsDialogWithData(titleResId, iconResId, items, listener);
  }

  @Nullable
  EditorInfo getCurrentInputEditorInfoForLanguageSelectionDialogHost() {
    return getCurrentInputEditorInfo();
  }

  @Nullable
  AnyKeyboard getCurrentAlphabetKeyboardForDictionaryOverrideDialogHost() {
    return getCurrentAlphabetKeyboard();
  }

  void showOptionsDialogWithDataForDictionaryOverrideDialogHost(
      CharSequence title,
      int iconRes,
      CharSequence[] items,
      android.content.DialogInterface.OnClickListener listener,
      GeneralDialogController.DialogPresenter presenter) {
    showOptionsDialogWithData(title, iconRes, items, listener, presenter);
  }

  boolean isIncognitoForOptionsMenuHost() {
    return getSuggest().isIncognitoMode();
  }

  void setIncognitoForOptionsMenuHost(boolean incognito, boolean notify) {
    setIncognito(incognito, notify);
  }

  void launchSettingsForOptionsMenuHost() {
    launchSettings();
  }

  void launchDictionaryOverridingForOptionsMenuHost() {
    launchDictionaryOverriding();
  }

  void showOptionsDialogWithDataForOptionsMenuHost(
      int titleResId,
      int iconResId,
      CharSequence[] items,
      android.content.DialogInterface.OnClickListener listener) {
    showOptionsDialogWithData(titleResId, iconResId, items, listener);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    multiTapEditCoordinator = new MultiTapEditCoordinator(mInputConnectionRouter);
    modifierKeyStateHandler =
        new ModifierKeyStateHandler(
            new AnySoftKeyboardModifierKeyStateHost(this),
            mInputConnectionRouter,
            mShiftKeyState,
            mControlKeyState,
            mAltKeyState,
            mFunctionKeyState,
            mVoiceKeyState);
    inputViewLifecycleHandler =
        new InputViewLifecycleHandler(new AnySoftKeyboardInputViewLifecycleHost(this));
    if (!BuildConfig.DEBUG && DeveloperUtils.hasTracingRequested(getApplicationContext())) {
      try {
        DeveloperUtils.startTracing();
        Toast.makeText(getApplicationContext(), R.string.debug_tracing_starting, Toast.LENGTH_SHORT)
            .show();
      } catch (Exception e) {
        // see issue https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/105
        // I might get a "Permission denied" error.
        e.printStackTrace();
        Toast.makeText(
                getApplicationContext(), R.string.debug_tracing_starting_failed, Toast.LENGTH_LONG)
            .show();
      }
    }
    if (!BuildConfig.DEBUG && BuildConfig.VERSION_NAME.endsWith("-SNAPSHOT")) {
      throw new RuntimeException("You can not run a 'RELEASE' build with a SNAPSHOT postfix!");
    }

    addDisposable(WindowAnimationSetter.subscribe(this, getWindow().getWindow()));

    emojiSearchController =
        new EmojiSearchController(
            new EmojiSearchControllerHost(
                this::getQuickTextTagsSearcher,
                this::getQuickKeyHistoryRecords,
                this::handleCloseRequest,
                this::showToastMessage,
                this::getInputViewContainer,
                this::commitEmojiFromSearch,
                () -> this));

    condenseModeManager =
        new CondenseModeManager(
            () -> {
              getKeyboardSwitcher().flushKeyboardsCache();
              hideWindow();
            });

    keyboardSwitchHandler =
        new KeyboardSwitchHandler(
            new KeyboardSwitchHandlerHost(
                this::getKeyboardSwitcher,
                this::getCurrentKeyboard,
                this::getCurrentAlphabetKeyboard,
                this::setKeyboardForView,
                this::showLanguageSelectionDialog,
                this::showToastMessage,
                this::nextKeyboard,
                this::nextAlterKeyboard,
                this::getCurrentInputEditorInfo,
                this::getInputView),
            condenseModeManager);
    navigationKeyHandler =
        new NavigationKeyHandler(
            new NavigationKeyHandlerHost(
                this::handleSelectionExpending,
                this::sendNavigationKeyEvent,
                this::sendDownUpKeyEvents));
    functionKeyHandlerHost = new AnySoftKeyboardFunctionKeyHost(this);
    functionKeyHandler =
        new FunctionKeyHandler(functionKeyHandlerHost, navigationKeyHandler, keyboardSwitchHandler);

    AnySoftKeyboardPrefsBinder.wire(
        prefs(),
        this::addDisposable,
        aBoolean -> mAutoCap = aBoolean,
        condenseModeManager,
        this::getCurrentOrientation,
        AnySoftKeyboard::parseCondenseType,
        aBoolean -> mShowKeyboardIconInStatusBar = aBoolean);

    condenseModeManager.updateForOrientation(getCurrentOrientation());

    mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    statusIconController = new StatusIconController(mInputMethodManager);
    statusIconHelper =
        new StatusIconHelper(
            statusIconController,
            () -> mShowKeyboardIconInStatusBar,
            this::getImeToken,
            this::getCurrentAlphabetKeyboard);
    packageBroadcastRegistrar = new PackageBroadcastRegistrar(this, this::onCriticalPackageChanged);
    packageBroadcastRegistrar.register();

    mVoiceRecognitionTrigger = new VoiceRecognitionTrigger(this);
    voiceUiHelper = new VoiceUiHelper(voiceStatusRenderer, mVoiceRecognitionTrigger);
    voiceInputController =
        new VoiceInputController(
            mVoiceRecognitionTrigger,
            new VoiceInputController.HostCallbacks() {
              @Override
              public void updateVoiceKeyState() {
                AnySoftKeyboard.this.updateVoiceKeyState();
              }

              @Override
              public void updateSpaceBarRecordingStatus(boolean isRecording) {
                AnySoftKeyboard.this.updateSpaceBarRecordingStatus(isRecording);
              }

              @Override
              public void updateVoiceInputStatus(VoiceInputState state) {
                AnySoftKeyboard.this.updateVoiceInputStatus(state);
              }

              @Override
              public android.content.Context getContext() {
                return AnySoftKeyboard.this;
              }
            });
    voiceInputController.attachCallbacks();

    mDevToolsAction = new DevStripActionProvider(this);
  }

  @Override
  public void onDestroy() {
    Logger.i(TAG, "AnySoftKeyboard has been destroyed! Cleaning resources..");
    if (packageBroadcastRegistrar != null) {
      packageBroadcastRegistrar.unregister();
    }

    final IBinder imeToken = getImeToken();
    if (imeToken != null) mInputMethodManager.hideStatusIcon(imeToken);

    hideWindow();

    if (DeveloperUtils.hasTracingStarted()) {
      DeveloperUtils.stopTracing();
      Toast.makeText(
              getApplicationContext(),
              getString(R.string.debug_tracing_finished, DeveloperUtils.getTraceFile()),
              Toast.LENGTH_SHORT)
          .show();
    }

    super.onDestroy();
  }

  public void onCriticalPackageChanged(Intent eventIntent) {
    if (((AnyApplication) getApplication()).onPackageChanged(eventIntent)) {
      onAddOnsCriticalChange();
    }
  }

  @Override
  public void onStartInput(EditorInfo attribute, boolean restarting) {
    super.onStartInput(attribute, restarting);
    statusIconHelper.onStartInput();
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
    inputViewLifecycleHandler.onStartInputView(TAG, attribute, restarting, mDevToolsAction);
  }

  @Override
  public void onFinishInput() {
    super.onFinishInput();

    statusIconHelper.onFinishInput();
  }

  @Override
  public void onFinishInputView(boolean finishingInput) {
    ImeStateTracker.onKeyboardHidden();
    super.onFinishInputView(finishingInput);
    inputViewLifecycleHandler.onFinishInputView(mDevToolsAction);
  }

  @Override
  public boolean onEvaluateFullscreenMode() {
    return fullscreenModeDecider.shouldUseFullscreen(
        getCurrentInputEditorInfo(),
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
    final AnyKeyboard currentAlphabetKeyboard = getCurrentAlphabetKeyboard();
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
    voiceUiHelper.updateVoiceKeyState(getCurrentAlphabetKeyboard(), getInputView());
  }

  void resubmitCurrentKeyboardToViewForInputViewLifecycleHandler() {
    final InputViewBinder inputView = getInputView();
    if (inputView instanceof AnyKeyboardViewBase && ((AnyKeyboardViewBase) inputView).getKeyboard() != null) {
      return;
    }
    final AnyKeyboard current = getCurrentKeyboard();
    if (current != null) {
      setKeyboardForView(current);
    }
  }

  @Nullable
  AnyKeyboard getCurrentAlphabetKeyboardForInputViewLifecycleHandler() {
    return getCurrentAlphabetKeyboard();
  }

  @Nullable
  AnyKeyboard getCurrentKeyboardForInputViewLifecycleHandler() {
    return getCurrentKeyboard();
  }

  @Nullable
  VoiceRecognitionTrigger getVoiceRecognitionTriggerForInputViewLifecycleHandler() {
    return mVoiceRecognitionTrigger;
  }

  /**
   * Updates the space bar text to show recording status.
   * This provides clear visual feedback when voice recording is active.
   */
  private void updateSpaceBarRecordingStatus(boolean isRecording) {
    voiceUiHelper.updateSpaceBarRecordingStatus(
        isRecording, getCurrentAlphabetKeyboard(), getInputView());
  }
  
  private void updateVoiceInputStatus(VoiceInputState newState) {
    voiceUiHelper.updateVoiceInputStatus(newState, getCurrentAlphabetKeyboard(), getInputView());
  }

  void handleEmojiSearchRequest() {
    emojiSearchController.requestShow();
  }

  private void commitEmojiFromSearch(CharSequence emoji) {
    super.onText(null, emoji);
  }

  // convert ASCII codes to Android KeyEvent codes
  // ASCII Codes Table: https://ascii.cl
  private boolean handleFunctionCombination(int primaryCode, @Nullable Keyboard.Key key) {
    return ModifierKeyEventHelper.handleFunctionCombination(
        primaryCode, key, this::sendDownUpKeyEvents);
  }

  private boolean handleAltCombination(int primaryCode, @Nullable InputConnection ic) {
    return ModifierKeyEventHelper.handleAltCombination(primaryCode, ic);
  }

  // send key events with meta state
  private void sendKeyEvent(InputConnection ic, int action, int keyCode, int meta) {
    if (ic == null) return;
    long now = System.currentTimeMillis();
    ic.sendKeyEvent(new KeyEvent(now, now, action, keyCode, 0, meta));
  }

  private void onNonFunctionKey(
      final int primaryCode,
      final Keyboard.Key key,
      final int multiTapIndex,
      final int[] nearByKeyCodes) {
    if (BuildConfig.DEBUG) Logger.d(TAG, "onNonFunctionKey %d", primaryCode);

    final InputConnection ic = currentInputConnection();

    if (mFunctionKeyState.isActive()) {
      if (handleFunctionCombination(primaryCode, key)) {
        if (!mFunctionKeyState.isLocked()) {
          mFunctionKeyState.setActiveState(false);
          handleFunction();
        }
        return;
      }
    }

    if (mAltKeyState.isActive()) {
      if (handleAltCombination(primaryCode, ic)) {
        if (!mAltKeyState.isLocked()) {
          mAltKeyState.setActiveState(false);
          handleAlt();
          mInputConnectionRouter.sendKeyUp(KeyEvent.KEYCODE_ALT_LEFT);
        }
        return;
      }
    }

    switch (primaryCode) {
      case KeyCodes.ENTER:
        handleEnterKey(ic);
        break;
      case KeyCodes.TAB:
        sendTab();
        break;
      case KeyCodes.ESCAPE:
        sendEscape();
        break;
      default:
        if (getSelectionStartPositionDangerous() != getCursorPosition()
            && specialWrapHelper.hasWrapCharacters(primaryCode)) {
          int[] wrapCharacters = specialWrapHelper.getWrapCharacters(primaryCode);
          wrapSelectionWithCharacters(wrapCharacters[0], wrapCharacters[1]);
        } else if (isWordSeparator(primaryCode)) {
          handleSeparator(primaryCode);
        } else if (mControlKeyState.isActive()) {
          boolean consumed =
              ModifierKeyEventHelper.handleControlCombination(
                  primaryCode, ic, this::sendTab, TAG);
          if (!consumed) {
            handleCharacter(primaryCode, key, multiTapIndex, nearByKeyCodes);
          }
          mControlKeyState.setActiveState(false);
          handleControl();
        } else {
          handleCharacter(primaryCode, key, multiTapIndex, nearByKeyCodes);
        }
        break;
    }
  }

  private void handleEnterKey(@Nullable InputConnection ic) {
    if (mShiftKeyState.isPressed() && ic != null) {
      // power-users feature ahead: Shift+Enter
      // getting away from firing the default editor action, by forcing newline
      abortCorrectionAndResetPredictionState(false);
      ic.commitText("\n", 1);
      return;
    }

    final EditorInfo editorInfo = getCurrentInputEditorInfo();
    final int imeOptionsActionId = IMEUtil.getImeOptionsActionIdFromEditorInfo(editorInfo);
    if (ic != null && IMEUtil.IME_ACTION_CUSTOM_LABEL == imeOptionsActionId) {
      // Either we have an actionLabel and we should performEditorAction with actionId regardless
      // of its value.
      ic.performEditorAction(editorInfo.actionId);
    } else if (ic != null && EditorInfo.IME_ACTION_NONE != imeOptionsActionId) {
      // We didn't have an actionLabel, but we had another action to execute.
      // EditorInfo.IME_ACTION_NONE explicitly means no action. In contrast,
      // EditorInfo.IME_ACTION_UNSPECIFIED is the default value for an action, so it
      // means there should be an action and the app didn't bother to set a specific
      // code for it - presumably it only handles one. It does not have to be treated
      // in any specific way: anything that is not IME_ACTION_NONE should be sent to
      // performEditorAction.
      ic.performEditorAction(imeOptionsActionId);
    } else {
      handleSeparator(KeyCodes.ENTER);
    }
  }

  @Override
  public void onKey(
      int primaryCode, Keyboard.Key key, int multiTapIndex, int[] nearByKeyCodes, boolean fromUI) {
    // Ensure editor state tracker is in sync before applying wrap/separator logic.
    getCursorPosition();

    final InputConnection ic = mInputConnectionRouter.current();
    mInputConnectionRouter.beginBatchEdit();
    boolean handledByOverlay = emojiSearchController.handleOverlayKey(primaryCode, key);
    if (!handledByOverlay) {
      super.onKey(primaryCode, key, multiTapIndex, nearByKeyCodes, fromUI);
      if (primaryCode > 0) {
        onNonFunctionKey(primaryCode, key, multiTapIndex, nearByKeyCodes);
      } else {
        if (BuildConfig.DEBUG) Logger.d(TAG, "onFunctionKey %d", primaryCode);
        functionKeyHandler.handle(primaryCode, key, fromUI);
      }
    }
    mInputConnectionRouter.endBatchEdit();
  }

  @Override
  public void onText(Keyboard.Key key, CharSequence text) {
    if (emojiSearchController.handleOverlayText(text)) {
      return;
    }
    super.onText(key, text);
  }

  private void sendTab() {
    InputConnection ic = currentInputConnection();
    if (ic == null) {
      return;
    }
    TerminalKeySender.sendTab(ic, TerminalKeySender.isTerminalEmulation(getCurrentInputEditorInfo()));
  }

  private void sendEscape() {
    InputConnection ic = currentInputConnection();
    if (ic == null) {
      return;
    }
    final boolean terminalEmulation = TerminalKeySender.isTerminalEmulation(getCurrentInputEditorInfo());
    TerminalKeySender.sendEscape(ic, terminalEmulation, () -> sendKeyChar((char) 27));
  }

  @Override
  public void onAlphabetKeyboardSet(@NonNull AnyKeyboard keyboard) {
    super.onAlphabetKeyboardSet(keyboard);
    setKeyboardFinalStuff();
    mKeyboardAutoCap = keyboard.autoCap;
    ImeStateTracker.onKeyboardVisible(keyboard, getCurrentInputEditorInfo());
    InputViewBinder inputView = getInputView();
    if (inputView instanceof com.anysoftkeyboard.keyboards.views.AnyKeyboardViewBase) {
      ImeStateTracker.reportKeyboardView(
          (com.anysoftkeyboard.keyboards.views.AnyKeyboardViewBase) inputView);
    } else {
      ImeStateTracker.reportKeyboardView(null);
    }
  }

  @Override
  protected void setKeyboardForView(@NonNull AnyKeyboard currentKeyboard) {
    currentKeyboard.setCondensedKeys(condenseModeManager.getCurrentMode());
    super.setKeyboardForView(currentKeyboard);
  }

  private void showLanguageSelectionDialog() {
    LanguageSelectionDialog.show(new AnySoftKeyboardLanguageSelectionDialogHost(this));
  }

  @Override
  public View onCreateExtractTextView() {
    mFullScreenExtractView = super.onCreateExtractTextView();
    if (mFullScreenExtractView != null) {
      mFullScreenExtractTextView =
          mFullScreenExtractView.findViewById(android.R.id.inputExtractEditText);
    }

    return mFullScreenExtractView;
  }

  @Override
  public void updateFullscreenMode() {
    super.updateFullscreenMode();
    InputViewBinder inputViewBinder = getInputView();
    if (mFullScreenExtractView != null && inputViewBinder != null) {
      final AnyKeyboardView anyKeyboardView = (AnyKeyboardView) inputViewBinder;
      ViewCompat.setBackground(mFullScreenExtractView, anyKeyboardView.getBackground());
      if (mFullScreenExtractTextView != null) {
        mFullScreenExtractTextView.setTextColor(
            anyKeyboardView.getCurrentResourcesHolder().getKeyTextColor());
      }
    }
  }

  @Override
  protected void handleBackWord(InputConnection ic) {
    if (ic == null) {
      return;
    }
    BackWordDeleter.handleBackWord(
        ic,
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
        mInputConnectionRouter,
        currentInputConnection(),
        getCurrentComposedWord(),
        forMultiTap);
  }

  void handleForwardDelete(InputConnection ic) {
    DeleteActionHelper.handleForwardDelete(
        getDeleteActionHost(), mInputConnectionRouter, ic, getCurrentComposedWord());
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
    final boolean temporarilyDisableShift =
        getInputView() != null && getInputView().isShifted();
    if (temporarilyDisableShift) {
      getInputView().setShifted(false);
    }
    sendDownUpKeyEvents(keyEventCode);
    if (temporarilyDisableShift) {
      handleShift();
    }
  }

  private void handleVoice() {
    voiceKeyUiUpdater.applyState(
        getInputView(), mVoiceKeyState.isActive(), mVoiceKeyState.isLocked());
  }

  private boolean mManualShiftState = false;

  void handleShift() {
    final AnyKeyboard currentKeyboard = getCurrentKeyboard();
    if (currentKeyboard != null) {
      currentKeyboard.setShifted(mShiftKeyState.isActive());
      currentKeyboard.setShiftLocked(mShiftKeyState.isLocked());
    }
    if (getInputView() != null) {
      Logger.d(
          TAG,
          "shift Setting UI active:%s, locked: %s",
          mShiftKeyState.isActive(),
          mShiftKeyState.isLocked());
      getInputView().setShifted(mShiftKeyState.isActive());
      getInputView().setShiftLocked(mShiftKeyState.isLocked());
    }
  }

  void toggleCaseOfSelectedCharacters() {
    if (getSelectionStartPositionDangerous() == getCursorPosition()) return;
    final ExtractedText et = getExtractedText();
    AnyKeyboard currentAlphabetKeyboard = getCurrentAlphabetKeyboard();
    @NonNull
    Locale locale = currentAlphabetKeyboard != null ? currentAlphabetKeyboard.getLocale() : Locale.ROOT;
    SelectionEditHelper.toggleCaseOfSelectedCharacters(
        et, mInputConnectionRouter, mTextCapitalizerWorkspace, locale);
  }

  private void wrapSelectionWithCharacters(int prefix, int postfix) {
    final ExtractedText et = getExtractedText();
    SelectionEditHelper.wrapSelectionWithCharacters(et, mInputConnectionRouter, prefix, postfix);
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

    abortCorrectionAndResetPredictionState(true);
  }

  private void nextAlterKeyboard(EditorInfo currentEditorInfo) {
    getKeyboardSwitcher().nextAlterKeyboard(currentEditorInfo);

    Logger.d(
        TAG,
        "nextAlterKeyboard: Setting next keyboard to: %s",
        getCurrentSymbolsKeyboard().getKeyboardName());
  }

  private void nextKeyboard(EditorInfo currentEditorInfo, KeyboardSwitcher.NextKeyboardType type) {
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
      mManualShiftState = true;
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
    DictionaryOverrideDialog.show(new AnySoftKeyboardDictionaryOverrideDialogHost(this));
  }

  void showOptionsMenu() {
    OptionsMenuLauncher.show(new AnySoftKeyboardOptionsMenuHost(this));
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
    if (countToDelete == 0) {
      return;
    }

    final WordComposer currentComposedWord = getCurrentComposedWord();
    final int currentLength = currentComposedWord.codePointCount();
    boolean shouldDeleteUsingCompletion;
    if (currentLength > 0) {
      shouldDeleteUsingCompletion = true;
      if (currentLength > countToDelete) {
        int deletesLeft = countToDelete;
        while (deletesLeft > 0) {
          currentComposedWord.deleteCodePointAtCurrentPosition();
          deletesLeft--;
        }
      } else {
        currentComposedWord.reset();
      }
    } else {
      shouldDeleteUsingCompletion = false;
    }
    InputConnection ic = currentInputConnection();
    if (ic != null) {
      if (isPredictionOn() && shouldDeleteUsingCompletion) {
        ic.setComposingText(currentComposedWord.getTypedWord() /* mComposing */, 1);
      } else {
        ic.deleteSurroundingText(countToDelete, 0);
      }
    }
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
  }

  void updateShiftStateNow() {
    final InputConnection ic = currentInputConnection();
    EditorInfo ei = getCurrentInputEditorInfo();
    final int caps;
    final AnyKeyboard currentAlphabetKeyboard = getCurrentAlphabetKeyboard();
    final boolean keyboardAutoCap = currentAlphabetKeyboard != null && currentAlphabetKeyboard.autoCap;
    if (keyboardAutoCap
        && mAutoCap
        && ic != null
        && ei != null
        && ei.inputType != EditorInfo.TYPE_NULL) {
      caps = ic.getCursorCapsMode(ei.inputType);
    } else {
      caps = 0;
    }
    final boolean inputSaysCaps = caps != 0;
    Logger.d(TAG, "shift updateShiftStateNow inputSaysCaps=%s", inputSaysCaps);
    if (inputSaysCaps) {
      if (!mShiftKeyState.isActive()) {
        mManualShiftState = false;
        mShiftKeyState.setActiveState(true);
      }
    } else if (!mManualShiftState) {
      mShiftKeyState.setActiveState(false);
    }
    if (!mShiftKeyState.isActive()) {
      mManualShiftState = false;
    }
    handleShift();
  }

}
