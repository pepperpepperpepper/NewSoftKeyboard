package com.anysoftkeyboard;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.ime.OptionsMenuLauncher;

final class AnySoftKeyboardOptionsMenuHost implements OptionsMenuLauncher.Host {

  @NonNull private final AnySoftKeyboard ime;

  AnySoftKeyboardOptionsMenuHost(@NonNull AnySoftKeyboard ime) {
    this.ime = ime;
  }

  @Override
  public void showOptionsDialogWithData(
      int titleResId,
      int iconResId,
      CharSequence[] items,
      android.content.DialogInterface.OnClickListener listener) {
    ime.showOptionsDialogWithDataForOptionsMenuHost(titleResId, iconResId, items, listener);
  }

  @Override
  public InputMethodManager getInputMethodManager() {
    return (InputMethodManager) ime.getSystemService(Context.INPUT_METHOD_SERVICE);
  }

  @Override
  public boolean isIncognito() {
    return ime.isIncognitoForOptionsMenuHost();
  }

  @Override
  public void setIncognito(boolean incognito, boolean notify) {
    ime.setIncognitoForOptionsMenuHost(incognito, notify);
  }

  @Override
  public void launchSettings() {
    ime.launchSettingsForOptionsMenuHost();
  }

  @Override
  public void launchDictionaryOverriding() {
    ime.launchDictionaryOverridingForOptionsMenuHost();
  }

  @Override
  public Context getContext() {
    return ime.getApplicationContext();
  }
}
