package com.anysoftkeyboard;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.ime.InputViewBinder;
import com.anysoftkeyboard.ime.InputViewLifecycleHandler;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;
import com.google.android.voiceime.VoiceRecognitionTrigger;
import java.util.Objects;

final class AnySoftKeyboardInputViewLifecycleHost implements InputViewLifecycleHandler.Host {

  @NonNull private final AnySoftKeyboard keyboard;

  AnySoftKeyboardInputViewLifecycleHost(@NonNull AnySoftKeyboard keyboard) {
    this.keyboard = keyboard;
  }

  @Override
  @Nullable
  public AnyKeyboard getCurrentAlphabetKeyboard() {
    return keyboard.getCurrentAlphabetKeyboardForInputViewLifecycleHandler();
  }

  @Override
  @Nullable
  public AnyKeyboard getCurrentKeyboard() {
    return keyboard.getCurrentKeyboardForInputViewLifecycleHandler();
  }

  @Override
  @NonNull
  public InputViewBinder getInputView() {
    return Objects.requireNonNull(keyboard.getInputView());
  }

  @Override
  @NonNull
  public KeyboardViewContainerView getInputViewContainer() {
    return Objects.requireNonNull(keyboard.getInputViewContainer());
  }

  @Override
  @Nullable
  public VoiceRecognitionTrigger getVoiceRecognitionTrigger() {
    return keyboard.getVoiceRecognitionTriggerForInputViewLifecycleHandler();
  }

  @Override
  public void updateVoiceKeyState() {
    keyboard.updateVoiceKeyState();
  }

  @Override
  public void resubmitCurrentKeyboardToView() {
    keyboard.resubmitCurrentKeyboardToViewForInputViewLifecycleHandler();
  }

  @Override
  public void updateShiftStateNow() {
    keyboard.updateShiftStateNow();
  }
}
