package com.anysoftkeyboard.ime.hosts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.ime.InputViewLifecycleHandler;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.keyboards.views.AnyKeyboardViewBase;
import com.anysoftkeyboard.keyboards.views.InputViewBinder;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;
import com.google.android.voiceime.VoiceImeController;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class AnySoftKeyboardInputViewLifecycleHost implements InputViewLifecycleHandler.Host {

  @NonNull private final Supplier<AnyKeyboard> currentAlphabetKeyboard;
  @NonNull private final Supplier<AnyKeyboard> currentKeyboard;
  @NonNull private final Supplier<InputViewBinder> inputView;
  @NonNull private final Supplier<KeyboardViewContainerView> inputViewContainer;
  @NonNull private final Supplier<VoiceImeController> voiceImeController;
  @NonNull private final Runnable updateVoiceKeyState;
  @NonNull private final Consumer<AnyKeyboard> setKeyboardForView;
  @NonNull private final Runnable updateShiftStateNow;

  public AnySoftKeyboardInputViewLifecycleHost(
      @NonNull Supplier<AnyKeyboard> currentAlphabetKeyboard,
      @NonNull Supplier<AnyKeyboard> currentKeyboard,
      @NonNull Supplier<InputViewBinder> inputView,
      @NonNull Supplier<KeyboardViewContainerView> inputViewContainer,
      @NonNull Supplier<VoiceImeController> voiceImeController,
      @NonNull Runnable updateVoiceKeyState,
      @NonNull Consumer<AnyKeyboard> setKeyboardForView,
      @NonNull Runnable updateShiftStateNow) {
    this.currentAlphabetKeyboard = currentAlphabetKeyboard;
    this.currentKeyboard = currentKeyboard;
    this.inputView = inputView;
    this.inputViewContainer = inputViewContainer;
    this.voiceImeController = voiceImeController;
    this.updateVoiceKeyState = updateVoiceKeyState;
    this.setKeyboardForView = setKeyboardForView;
    this.updateShiftStateNow = updateShiftStateNow;
  }

  @Override
  @Nullable
  public AnyKeyboard getCurrentAlphabetKeyboard() {
    return currentAlphabetKeyboard.get();
  }

  @Override
  @Nullable
  public AnyKeyboard getCurrentKeyboard() {
    return currentKeyboard.get();
  }

  @Override
  @NonNull
  public InputViewBinder getInputView() {
    return Objects.requireNonNull(inputView.get());
  }

  @Override
  @NonNull
  public KeyboardViewContainerView getInputViewContainer() {
    return Objects.requireNonNull(inputViewContainer.get());
  }

  @Override
  @Nullable
  public VoiceImeController getVoiceImeController() {
    return voiceImeController.get();
  }

  @Override
  public void updateVoiceKeyState() {
    updateVoiceKeyState.run();
  }

  @Override
  public void resubmitCurrentKeyboardToView() {
    final InputViewBinder inputView = getInputView();
    if (inputView instanceof AnyKeyboardViewBase
        && ((AnyKeyboardViewBase) inputView).getKeyboard() != null) {
      return;
    }

    final AnyKeyboard current = getCurrentKeyboard();
    if (current != null) {
      setKeyboardForView.accept(current);
    }
  }

  @Override
  public void updateShiftStateNow() {
    updateShiftStateNow.run();
  }
}
