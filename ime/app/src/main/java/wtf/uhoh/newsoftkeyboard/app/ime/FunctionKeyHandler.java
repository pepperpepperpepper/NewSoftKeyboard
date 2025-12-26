package wtf.uhoh.newsoftkeyboard.app.ime;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import wtf.uhoh.newsoftkeyboard.BuildConfig;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;

public final class FunctionKeyHandler {

  private static final String TAG = "NSKFunctionKeyHandler";

  public interface Host {
    @NonNull
    InputConnectionRouter inputConnectionRouter();

    boolean isFunctionKeyActive();

    boolean isFunctionKeyLocked();

    void consumeOneShotFunctionKey();

    boolean shouldBackWordDelete();

    void handleBackWord();

    void handleDeleteLastCharacter();

    void handleShift();

    void toggleShiftLocked();

    void sendSyntheticPressAndRelease(int primaryCode);

    void handleForwardDelete();

    void abortCorrectionAndResetPredictionState(boolean disabledUntilNextInputStart);

    void handleControl();

    void handleAlt();

    void handleFunction();

    boolean isVoiceRecognitionInstalled();

    @NonNull
    String getDefaultDictionaryLocale();

    void startVoiceRecognition(@NonNull String locale);

    void updateVoiceKeyState();

    void showVoiceInputNotInstalledUi();

    void launchOpenAISettings();

    boolean handleCloseRequest();

    void hideWindow();

    void showOptionsMenu();

    void onQuickTextRequested(@Nullable Keyboard.Key key);

    void onQuickTextKeyboardRequested(@Nullable Keyboard.Key key);

    void handleEmojiSearchRequest();

    void handleClipboardOperation(
        @Nullable Keyboard.Key key,
        int primaryCode,
        @NonNull InputConnectionRouter inputConnectionRouter);

    void handleMediaInsertionKey();

    void clearQuickTextHistory();
  }

  @NonNull private final Host host;
  @NonNull private final NavigationKeyHandler navigationKeyHandler;
  @Nullable private final KeyboardSwitchHandler keyboardSwitchHandler;

  public FunctionKeyHandler(
      @NonNull Host host,
      @NonNull NavigationKeyHandler navigationKeyHandler,
      @Nullable KeyboardSwitchHandler keyboardSwitchHandler) {
    this.host = host;
    this.navigationKeyHandler = navigationKeyHandler;
    this.keyboardSwitchHandler = keyboardSwitchHandler;
  }

  public void handle(
      final int primaryCode, @Nullable final Keyboard.Key key, final boolean fromUI) {
    final InputConnectionRouter inputConnectionRouter = host.inputConnectionRouter();

    if (navigationKeyHandler.handle(
        primaryCode,
        inputConnectionRouter,
        host.isFunctionKeyActive(),
        host.isFunctionKeyLocked(),
        host::consumeOneShotFunctionKey)) {
      return;
    }

    switch (primaryCode) {
      case KeyCodes.DELETE:
        if (inputConnectionRouter.hasConnection()) {
          // we do back-word if the shift is pressed while pressing
          // backspace (like in a PC)
          if (host.shouldBackWordDelete()) {
            host.handleBackWord();
          } else {
            host.handleDeleteLastCharacter();
          }
        }
        break;
      case KeyCodes.SHIFT:
        if (fromUI) {
          host.handleShift();
        } else {
          // not from UI (user not actually pressed that button)
          host.sendSyntheticPressAndRelease(primaryCode);
        }
        break;
      case KeyCodes.SHIFT_LOCK:
        host.toggleShiftLocked();
        host.handleShift();
        break;
      case KeyCodes.DELETE_WORD:
        if (inputConnectionRouter.hasConnection()) {
          host.handleBackWord();
        }
        break;
      case KeyCodes.FORWARD_DELETE:
        if (inputConnectionRouter.hasConnection()) {
          host.handleForwardDelete();
        }
        break;
      case KeyCodes.CLEAR_INPUT:
        if (inputConnectionRouter.hasConnection()) {
          inputConnectionRouter.beginBatchEdit();
          host.abortCorrectionAndResetPredictionState(false);
          inputConnectionRouter.deleteSurroundingText(Integer.MAX_VALUE, Integer.MAX_VALUE);
          inputConnectionRouter.endBatchEdit();
        }
        break;
      case KeyCodes.CTRL:
      case KeyCodes.CTRL_LOCK:
        if (fromUI) {
          host.handleControl();
        } else {
          host.sendSyntheticPressAndRelease(primaryCode);
        }
        break;
      case KeyCodes.ALT_MODIFIER:
        if (fromUI) {
          host.handleAlt();
        } else {
          host.sendSyntheticPressAndRelease(primaryCode);
        }
        break;
      case KeyCodes.FUNCTION:
        if (fromUI) {
          host.handleFunction();
        } else {
          host.sendSyntheticPressAndRelease(primaryCode);
        }
        break;
      case KeyCodes.VOICE_INPUT:
        if (host.isVoiceRecognitionInstalled()) {
          host.startVoiceRecognition(host.getDefaultDictionaryLocale());
          // Update voice key state based on recording state
          host.updateVoiceKeyState();
        } else {
          host.showVoiceInputNotInstalledUi();
        }
        break;
      case KeyCodes.MICROPHONE_LONG_PRESS:
        host.launchOpenAISettings();
        break;
      case KeyCodes.CANCEL:
        if (!host.handleCloseRequest()) {
          host.hideWindow();
        }
        break;
      case KeyCodes.SETTINGS:
        host.showOptionsMenu();
        break;
      default:
        if (keyboardSwitchHandler != null
            && keyboardSwitchHandler.handle(primaryCode, key, fromUI)) {
          break;
        }
        handleOtherFunctionKey(primaryCode, key, inputConnectionRouter);
    }
  }

  private void handleOtherFunctionKey(
      final int primaryCode,
      @Nullable final Keyboard.Key key,
      @NonNull final InputConnectionRouter inputConnectionRouter) {
    switch (primaryCode) {
      case KeyCodes.QUICK_TEXT:
        host.onQuickTextRequested(key);
        break;
      case KeyCodes.QUICK_TEXT_POPUP:
        host.onQuickTextKeyboardRequested(key);
        break;
      case KeyCodes.EMOJI_SEARCH:
        host.handleEmojiSearchRequest();
        break;
      case KeyCodes.CLIPBOARD_COPY:
      case KeyCodes.CLIPBOARD_PASTE:
      case KeyCodes.CLIPBOARD_CUT:
      case KeyCodes.CLIPBOARD_SELECT_ALL:
      case KeyCodes.CLIPBOARD_PASTE_POPUP:
      case KeyCodes.CLIPBOARD_SELECT:
      case KeyCodes.UNDO:
      case KeyCodes.REDO:
        if (inputConnectionRouter.hasConnection()) {
          host.handleClipboardOperation(key, primaryCode, inputConnectionRouter);
        }
        break;
      case KeyCodes.IMAGE_MEDIA_POPUP:
        host.handleMediaInsertionKey();
        break;
      case KeyCodes.CLEAR_QUICK_TEXT_HISTORY:
        host.clearQuickTextHistory();
        break;
      case KeyCodes.DISABLED:
        Logger.d(TAG, "Disabled key was pressed.");
        break;
      default:
        if (BuildConfig.DEBUG) {
          // this should not happen! We should handle ALL function keys.
          throw new RuntimeException("UNHANDLED FUNCTION KEY! primary code " + primaryCode);
        } else {
          Logger.w(TAG, "UNHANDLED FUNCTION KEY! primary code %d. Ignoring.", primaryCode);
        }
    }
  }
}
