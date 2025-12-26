package wtf.uhoh.newsoftkeyboard.app.ime.gesturetyping;

import androidx.annotation.NonNull;
import com.anysoftkeyboard.api.KeyCodes;
import wtf.uhoh.newsoftkeyboard.app.ime.InputConnectionRouter;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordComposer;

public final class GestureTypingCommitter {
  private static final String TAG = "NSKGestureCommitter";

  private GestureTypingCommitter() {}

  public static boolean commitComposingWord(
      @NonNull InputConnectionRouter inputConnectionRouter,
      @NonNull WordComposer currentComposedWord,
      @NonNull CharSequence word,
      boolean autoCapitalized,
      int maxCharsToLookBackForSeparator) {
    currentComposedWord.reset();
    currentComposedWord.setAutoCapitalized(autoCapitalized);
    currentComposedWord.simulateTypedWord(word);
    currentComposedWord.setPreferredWord(currentComposedWord.getTypedWord());

    // If there's any non-separator before the cursor, add a space:
    // TODO: Improve the detection of mid-word separations (not hardcode a hyphen and an
    // apostrophe),
    // and disable this check on URL text fields.
    CharSequence toLeft =
        inputConnectionRouter.getTextBeforeCursor(maxCharsToLookBackForSeparator, 0);
    if (toLeft == null) {
      Logger.w(
          TAG,
          "InputConnection was not null, but returned null from getTextBeforeCursor. Assuming the"
              + " connection is dead.");
      return false;
    } else if (toLeft.length() > 0) {
      int lastCodePoint = Character.codePointBefore(toLeft, toLeft.length());
      if (!(Character.isWhitespace(lastCodePoint)
          || lastCodePoint == (int) '\''
          || lastCodePoint == (int) '-')) {
        inputConnectionRouter.commitText(new String(new int[] {KeyCodes.SPACE}, 0, 1), 1);
      }
    }

    inputConnectionRouter.setComposingText(currentComposedWord.getTypedWord(), 1);
    return true;
  }
}
