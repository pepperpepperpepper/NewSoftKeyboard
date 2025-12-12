package com.anysoftkeyboard.ime;

import android.view.inputmethod.InputConnection;
import android.text.TextUtils;

final class CursorTouchChecker {

  interface WordSeparatorChecker {
    boolean isWordSeparator(int codePoint);
  }

  boolean isCursorTouchingWord(InputConnection ic, WordSeparatorChecker separatorChecker) {
    if (ic == null) return false;

    CharSequence toLeft = ic.getTextBeforeCursor(1, 0);
    if (!TextUtils.isEmpty(toLeft) && !separatorChecker.isWordSeparator(toLeft.charAt(0))) {
      return true;
    }

    CharSequence toRight = ic.getTextAfterCursor(1, 0);
    if (!TextUtils.isEmpty(toRight) && !separatorChecker.isWordSeparator(toRight.charAt(0))) {
      return true;
    }

    return false;
  }
}
