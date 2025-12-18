package com.anysoftkeyboard;

import android.content.Intent;
import android.view.inputmethod.InputConnection;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.ime.FunctionKeyHandler;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.ui.VoiceInputNotInstalledActivity;

final class AnySoftKeyboardFunctionKeyHost implements FunctionKeyHandler.Host {

  @NonNull private final AnySoftKeyboard ime;

  AnySoftKeyboardFunctionKeyHost(@NonNull AnySoftKeyboard ime) {
    this.ime = ime;
  }

  @Nullable
  @Override
  public InputConnection currentInputConnection() {
    return ime.currentInputConnectionForFunctionKeyHandler();
  }

  @Override
  public boolean isFunctionKeyActive() {
    return ime.isFunctionKeyActiveForFunctionKeyHandler();
  }

  @Override
  public boolean isFunctionKeyLocked() {
    return ime.isFunctionKeyLockedForFunctionKeyHandler();
  }

  @Override
  public void consumeOneShotFunctionKey() {
    ime.consumeOneShotFunctionKeyForFunctionKeyHandler();
  }

  @Override
  public boolean shouldBackWordDelete() {
    return ime.shouldBackWordDeleteForFunctionKeyHandler();
  }

  @Override
  public void handleBackWord(@NonNull InputConnection ic) {
    ime.handleBackWord(ic);
  }

  @Override
  public void handleDeleteLastCharacter() {
    ime.handleDeleteLastCharacter(false);
  }

  @Override
  public void handleShift() {
    ime.handleShift();
  }

  @Override
  public void toggleShiftLocked() {
    ime.toggleShiftLockedForFunctionKeyHandler();
  }

  @Override
  public void sendSyntheticPressAndRelease(int primaryCode) {
    ime.onPress(primaryCode);
    ime.onRelease(primaryCode);
  }

  @Override
  public void handleForwardDelete(@NonNull InputConnection ic) {
    ime.handleForwardDelete(ic);
  }

  @Override
  public void abortCorrectionAndResetPredictionState(boolean disabledUntilNextInputStart) {
    ime.abortCorrectionAndResetPredictionStateForFunctionKeyHandler(disabledUntilNextInputStart);
  }

  @Override
  public void handleControl() {
    ime.handleControl();
  }

  @Override
  public void handleAlt() {
    ime.handleAlt();
  }

  @Override
  public void handleFunction() {
    ime.handleFunction();
  }

  @Override
  public boolean isVoiceRecognitionInstalled() {
    return ime.isVoiceRecognitionInstalledForFunctionKeyHandler();
  }

  @NonNull
  @Override
  public String getDefaultDictionaryLocale() {
    return ime.defaultDictionaryLocaleForFunctionKeyHandler();
  }

  @Override
  public void startVoiceRecognition(@NonNull String locale) {
    ime.startVoiceRecognitionForFunctionKeyHandler(locale);
  }

  @Override
  public void updateVoiceKeyState() {
    ime.updateVoiceKeyState();
  }

  @Override
  public void showVoiceInputNotInstalledUi() {
    Intent voiceInputNotInstalledIntent =
        new Intent(ime.getApplicationContext(), VoiceInputNotInstalledActivity.class);
    voiceInputNotInstalledIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    ime.startActivity(voiceInputNotInstalledIntent);
  }

  @Override
  public void launchOpenAISettings() {
    ime.launchOpenAISettings();
  }

  @Override
  public boolean handleCloseRequest() {
    return ime.handleCloseRequest();
  }

  @Override
  public void hideWindow() {
    ime.hideWindow();
  }

  @Override
  public void showOptionsMenu() {
    ime.showOptionsMenu();
  }

  @Override
  public void onQuickTextRequested(@Nullable Keyboard.Key key) {
    ime.onQuickTextRequestedForFunctionKeyHandler(key);
  }

  @Override
  public void onQuickTextKeyboardRequested(@Nullable Keyboard.Key key) {
    ime.onQuickTextKeyboardRequestedForFunctionKeyHandler(key);
  }

  @Override
  public void handleEmojiSearchRequest() {
    ime.handleEmojiSearchRequest();
  }

  @Override
  public void handleClipboardOperation(
      @Nullable Keyboard.Key key, int primaryCode, @NonNull InputConnection ic) {
    ime.handleClipboardOperationForFunctionKeyHandler(key, primaryCode, ic);
  }

  @Override
  public void handleMediaInsertionKey() {
    ime.handleMediaInsertionKeyForFunctionKeyHandler();
  }

  @Override
  public void clearQuickTextHistory() {
    ime.clearQuickTextHistoryForFunctionKeyHandler();
  }
}
