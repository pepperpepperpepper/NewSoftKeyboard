package com.anysoftkeyboard;

import android.content.Context;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.dictionaries.ExternalDictionaryFactory;
import com.anysoftkeyboard.ime.DictionaryOverrideDialog;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.menny.android.anysoftkeyboard.AnyApplication;
import net.evendanan.pixel.GeneralDialogController;

final class AnySoftKeyboardDictionaryOverrideDialogHost implements DictionaryOverrideDialog.Host {

  @NonNull private final AnySoftKeyboard ime;

  AnySoftKeyboardDictionaryOverrideDialogHost(@NonNull AnySoftKeyboard ime) {
    this.ime = ime;
  }

  @Override
  public AnyKeyboard getCurrentAlphabetKeyboard() {
    return ime.getCurrentAlphabetKeyboardForDictionaryOverrideDialogHost();
  }

  @Override
  public ExternalDictionaryFactory getExternalDictionaryFactory() {
    return AnyApplication.getExternalDictionaryFactory(ime);
  }

  @Override
  public void showOptionsDialogWithData(
      CharSequence title,
      int iconRes,
      CharSequence[] items,
      android.content.DialogInterface.OnClickListener listener,
      GeneralDialogController.DialogPresenter presenter) {
    ime.showOptionsDialogWithDataForDictionaryOverrideDialogHost(
        title, iconRes, items, listener, presenter);
  }

  @Override
  public Context getContext() {
    return ime;
  }
}
