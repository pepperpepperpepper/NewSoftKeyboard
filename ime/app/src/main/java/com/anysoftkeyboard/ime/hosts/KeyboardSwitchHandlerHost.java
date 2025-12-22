package com.anysoftkeyboard.ime.hosts;

import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.ime.KeyboardSwitchHandler;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.keyboards.KeyboardSwitcher;
import com.anysoftkeyboard.keyboards.NextKeyboardType;
import com.anysoftkeyboard.keyboards.views.AnyKeyboardView;
import com.anysoftkeyboard.keyboards.views.InputViewBinder;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class KeyboardSwitchHandlerHost implements KeyboardSwitchHandler.Host {

  private final Supplier<KeyboardSwitcher> keyboardSwitcher;
  private final Supplier<AnyKeyboard> currentKeyboard;
  private final Supplier<AnyKeyboard> currentAlphabetKeyboard;
  private final Consumer<AnyKeyboard> setKeyboardForView;
  private final Runnable showLanguageSelectionDialog;
  private final BiConsumer<Integer, Boolean> showToastMessage;
  private final BiConsumer<EditorInfo, NextKeyboardType> nextKeyboard;
  private final Consumer<EditorInfo> nextAlterKeyboard;
  private final Supplier<EditorInfo> currentEditorInfo;
  private final Supplier<InputViewBinder> inputViewBinder;

  public KeyboardSwitchHandlerHost(
      @NonNull Supplier<KeyboardSwitcher> keyboardSwitcher,
      @NonNull Supplier<AnyKeyboard> currentKeyboard,
      @NonNull Supplier<AnyKeyboard> currentAlphabetKeyboard,
      @NonNull Consumer<AnyKeyboard> setKeyboardForView,
      @NonNull Runnable showLanguageSelectionDialog,
      @NonNull BiConsumer<Integer, Boolean> showToastMessage,
      @NonNull BiConsumer<EditorInfo, NextKeyboardType> nextKeyboard,
      @NonNull Consumer<EditorInfo> nextAlterKeyboard,
      @NonNull Supplier<EditorInfo> currentEditorInfo,
      @NonNull Supplier<InputViewBinder> inputViewBinder) {
    this.keyboardSwitcher = keyboardSwitcher;
    this.currentKeyboard = currentKeyboard;
    this.currentAlphabetKeyboard = currentAlphabetKeyboard;
    this.setKeyboardForView = setKeyboardForView;
    this.showLanguageSelectionDialog = showLanguageSelectionDialog;
    this.showToastMessage = showToastMessage;
    this.nextKeyboard = nextKeyboard;
    this.nextAlterKeyboard = nextAlterKeyboard;
    this.currentEditorInfo = currentEditorInfo;
    this.inputViewBinder = inputViewBinder;
  }

  @NonNull
  @Override
  public KeyboardSwitcher getKeyboardSwitcher() {
    return keyboardSwitcher.get();
  }

  @Nullable
  @Override
  public AnyKeyboard getCurrentKeyboard() {
    return currentKeyboard.get();
  }

  @NonNull
  @Override
  public AnyKeyboard getCurrentAlphabetKeyboard() {
    return currentAlphabetKeyboard.get();
  }

  @Override
  public void setKeyboardForView(@NonNull AnyKeyboard keyboard) {
    setKeyboardForView.accept(keyboard);
  }

  @Override
  public void showLanguageSelectionDialog() {
    showLanguageSelectionDialog.run();
  }

  @Override
  public void showToastMessage(int resId, boolean important) {
    showToastMessage.accept(resId, important);
  }

  @Override
  public void nextKeyboard(@Nullable EditorInfo editorInfo, @NonNull NextKeyboardType type) {
    nextKeyboard.accept(editorInfo != null ? editorInfo : currentEditorInfo.get(), type);
  }

  @Override
  public void nextAlterKeyboard(@Nullable EditorInfo editorInfo) {
    nextAlterKeyboard.accept(editorInfo != null ? editorInfo : currentEditorInfo.get());
  }

  @Nullable
  @Override
  public AnyKeyboardView getInputView() {
    final InputViewBinder binder = inputViewBinder.get();
    return binder instanceof AnyKeyboardView ? (AnyKeyboardView) binder : null;
  }
}
