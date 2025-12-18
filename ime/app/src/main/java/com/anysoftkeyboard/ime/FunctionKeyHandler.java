package com.anysoftkeyboard.ime;

import android.view.inputmethod.InputConnection;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.menny.android.anysoftkeyboard.BuildConfig;

public final class FunctionKeyHandler {

  private static final String TAG = "NSKFunctionKeyHandler";

  public interface Host {
    @Nullable
    InputConnection currentInputConnection();

    boolean isFunctionKeyActive();

    boolean isFunctionKeyLocked();

    void consumeOneShotFunctionKey();

    boolean shouldBackWordDelete();

    void handleBackWord(@NonNull InputConnection ic);

    void handleDeleteLastCharacter();

    void handleShift();

    void toggleShiftLocked();

    void sendSyntheticPressAndRelease(int primaryCode);

    void handleForwardDelete(@NonNull InputConnection ic);

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
        @Nullable Keyboard.Key key, int primaryCode, @NonNull InputConnection ic);

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

  public void handle(final int primaryCode, @Nullable final Keyboard.Key key, final boolean fromUI) {
    final InputConnection ic = host.currentInputConnection();

    if (navigationKeyHandler.handle(
        primaryCode, ic, host.isFunctionKeyActive(), host.isFunctionKeyLocked(), host::consumeOneShotFunctionKey)) {
      return;
    }

    switch (primaryCode) {
      case KeyCodes.DELETE:
        if (ic != null) {
          // we do back-word if the shift is pressed while pressing
          // backspace (like in a PC)
          if (host.shouldBackWordDelete()) {
            host.handleBackWord(ic);
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
        if (ic != null) {
          host.handleBackWord(ic);
        }
        break;
      case KeyCodes.FORWARD_DELETE:
        if (ic != null) {
          host.handleForwardDelete(ic);
        }
        break;
      case KeyCodes.CLEAR_INPUT:
        if (ic != null) {
          ic.beginBatchEdit();
          host.abortCorrectionAndResetPredictionState(false);
          ic.deleteSurroundingText(Integer.MAX_VALUE, Integer.MAX_VALUE);
          ic.endBatchEdit();
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
        if (keyboardSwitchHandler != null && keyboardSwitchHandler.handle(primaryCode, key, fromUI)) {
          break;
        }
        handleOtherFunctionKey(primaryCode, key, ic);
    }
  }

  private void handleOtherFunctionKey(
      final int primaryCode, @Nullable final Keyboard.Key key, @Nullable final InputConnection ic) {
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
        if (ic != null) {
          host.handleClipboardOperation(key, primaryCode, ic);
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
