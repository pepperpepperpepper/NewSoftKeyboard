package com.anysoftkeyboard;

import android.content.Context;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.ime.LanguageSelectionDialog;
import com.anysoftkeyboard.keyboards.KeyboardSwitcher;

final class AnySoftKeyboardLanguageSelectionDialogHost implements LanguageSelectionDialog.Host {

  @NonNull private final AnySoftKeyboard ime;

  AnySoftKeyboardLanguageSelectionDialogHost(@NonNull AnySoftKeyboard ime) {
    this.ime = ime;
  }

  @NonNull
  @Override
  public KeyboardSwitcher getKeyboardSwitcher() {
    return ime.getKeyboardSwitcherForLanguageSelectionDialogHost();
  }

  @Override
  public void showOptionsDialogWithData(
      int titleResId,
      int iconResId,
      CharSequence[] items,
      android.content.DialogInterface.OnClickListener listener) {
    ime.showOptionsDialogWithDataForLanguageSelectionDialogHost(
        titleResId, iconResId, items, listener);
  }

  @Override
  public EditorInfo getCurrentInputEditorInfo() {
    return ime.getCurrentInputEditorInfoForLanguageSelectionDialogHost();
  }

  @NonNull
  @Override
  public Context getContext() {
    return ime;
  }
}
