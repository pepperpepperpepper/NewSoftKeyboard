package wtf.uhoh.newsoftkeyboard.app.ime.hosts;

import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import wtf.uhoh.newsoftkeyboard.app.ime.KeyboardSwitchHandler;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardSwitcher;
import wtf.uhoh.newsoftkeyboard.app.keyboards.NextKeyboardType;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.InputViewBinder;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardView;

public final class KeyboardSwitchHandlerHost implements KeyboardSwitchHandler.Host {

  private final Supplier<KeyboardSwitcher> keyboardSwitcher;
  private final Supplier<KeyboardDefinition> currentKeyboard;
  private final Supplier<KeyboardDefinition> currentAlphabetKeyboard;
  private final Consumer<KeyboardDefinition> setKeyboardForView;
  private final Runnable showLanguageSelectionDialog;
  private final BiConsumer<Integer, Boolean> showToastMessage;
  private final BiConsumer<EditorInfo, NextKeyboardType> nextKeyboard;
  private final Consumer<EditorInfo> nextAlterKeyboard;
  private final Supplier<EditorInfo> currentEditorInfo;
  private final Supplier<InputViewBinder> inputViewBinder;

  public KeyboardSwitchHandlerHost(
      @NonNull Supplier<KeyboardSwitcher> keyboardSwitcher,
      @NonNull Supplier<KeyboardDefinition> currentKeyboard,
      @NonNull Supplier<KeyboardDefinition> currentAlphabetKeyboard,
      @NonNull Consumer<KeyboardDefinition> setKeyboardForView,
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
  public KeyboardDefinition getCurrentKeyboard() {
    return currentKeyboard.get();
  }

  @NonNull
  @Override
  public KeyboardDefinition getCurrentAlphabetKeyboard() {
    return currentAlphabetKeyboard.get();
  }

  @Override
  public void setKeyboardForView(@NonNull KeyboardDefinition keyboard) {
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
  public KeyboardView getInputView() {
    final InputViewBinder binder = inputViewBinder.get();
    return binder instanceof KeyboardView ? (KeyboardView) binder : null;
  }
}
