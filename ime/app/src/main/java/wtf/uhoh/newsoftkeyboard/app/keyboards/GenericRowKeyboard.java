package wtf.uhoh.newsoftkeyboard.app.keyboards;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.anysoftkeyboard.api.KeyCodes;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.addons.AddOn;
import wtf.uhoh.newsoftkeyboard.app.keyboardextensions.KeyboardExtension;

@VisibleForTesting
final class GenericRowKeyboard extends KeyboardDefinition {

  private final boolean inAlphabetMode;

  GenericRowKeyboard(
      @NonNull KeyboardExtension keyboardExtension,
      @NonNull Context hostAppContext,
      @NonNull KeyboardDimens keyboardDimens,
      boolean inAlphabetMode,
      @KeyboardRowModeId int mode) {
    super(keyboardExtension, hostAppContext, keyboardExtension.getKeyboardResId(), mode);
    this.inAlphabetMode = inAlphabetMode;
    loadKeyboard(keyboardDimens);
  }

  @Override
  protected void addGenericRows(
      @NonNull KeyboardDimens keyboardDimens,
      @NonNull KeyboardExtension topRowPlugin,
      @NonNull KeyboardExtension bottomRowPlugin) {
    /*no-op*/
  }

  @Override
  public boolean isAlphabetKeyboard() {
    return inAlphabetMode;
  }

  @Override
  public String getDefaultDictionaryLocale() {
    return null;
  }

  @Override
  public char[] getSentenceSeparators() {
    return new char[0];
  }

  @NonNull
  @Override
  public CharSequence getKeyboardName() {
    return "not important";
  }

  @Override
  public int getKeyboardIconResId() {
    return AddOn.INVALID_RES_ID;
  }

  @NonNull
  @Override
  public String getKeyboardId() {
    return "no-important";
  }

  boolean hasNoKeys() {
    return getKeys().isEmpty();
  }

  @Override
  protected boolean setupKeyAfterCreation(KeyboardKey key) {
    if (!super.setupKeyAfterCreation(key)) {
      if (key.popupResId == 0 && inAlphabetMode) {
        switch (key.getPrimaryCode()) {
          case KeyCodes.MODE_SYMBOLS:
          case KeyCodes.KEYBOARD_MODE_CHANGE:
            key.popupResId = R.xml.ext_symbols;
            key.externalResourcePopupLayout = false;
            return true;
        }
      }
    }
    return false;
  }
}
