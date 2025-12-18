package com.anysoftkeyboard.keyboards;

import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class KeyboardRowModeResolver {

  /** Maps between requested mode and enabled mode. */
  @Keyboard.KeyboardRowModeId
  private final int[] rowModesMapping =
      new int[] {
        Keyboard.KEYBOARD_ROW_MODE_NONE,
        Keyboard.KEYBOARD_ROW_MODE_NORMAL,
        Keyboard.KEYBOARD_ROW_MODE_IM,
        Keyboard.KEYBOARD_ROW_MODE_URL,
        Keyboard.KEYBOARD_ROW_MODE_EMAIL,
        Keyboard.KEYBOARD_ROW_MODE_PASSWORD
      };

  @NonNull
  int[] getRowModesMapping() {
    return rowModesMapping;
  }

  @Keyboard.KeyboardRowModeId
  int resolve(@Nullable EditorInfo attr) {
    if (attr == null) return Keyboard.KEYBOARD_ROW_MODE_NORMAL;

    int variation = attr.inputType & EditorInfo.TYPE_MASK_VARIATION;

    switch (variation) {
      case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
      case EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS:
        return returnModeIfEnabled(Keyboard.KEYBOARD_ROW_MODE_EMAIL);
      case EditorInfo.TYPE_TEXT_VARIATION_URI:
        return returnModeIfEnabled(Keyboard.KEYBOARD_ROW_MODE_URL);
      case EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE:
      case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_SUBJECT:
      case EditorInfo.TYPE_TEXT_VARIATION_LONG_MESSAGE:
        return returnModeIfEnabled(Keyboard.KEYBOARD_ROW_MODE_IM);
      case EditorInfo.TYPE_TEXT_VARIATION_PASSWORD:
      case EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
      case EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD:
        return returnModeIfEnabled(Keyboard.KEYBOARD_ROW_MODE_PASSWORD);
      default:
        return Keyboard.KEYBOARD_ROW_MODE_NORMAL;
    }
  }

  @Keyboard.KeyboardRowModeId
  private int returnModeIfEnabled(@Keyboard.KeyboardRowModeId int modeId) {
    return rowModesMapping[modeId];
  }
}

