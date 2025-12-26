package wtf.uhoh.newsoftkeyboard.app.ime;

import android.text.TextUtils;

final class CursorTouchChecker {

  interface WordSeparatorChecker {
    boolean isWordSeparator(int codePoint);
  }

  boolean isCursorTouchingWord(
      InputConnectionRouter inputConnectionRouter, WordSeparatorChecker separatorChecker) {
    if (inputConnectionRouter.current() == null) return false;

    CharSequence toLeft = inputConnectionRouter.getTextBeforeCursor(1, 0);
    if (!TextUtils.isEmpty(toLeft) && !separatorChecker.isWordSeparator(toLeft.charAt(0))) {
      return true;
    }

    CharSequence toRight = inputConnectionRouter.getTextAfterCursor(1, 0);
    if (!TextUtils.isEmpty(toRight) && !separatorChecker.isWordSeparator(toRight.charAt(0))) {
      return true;
    }

    return false;
  }
}
