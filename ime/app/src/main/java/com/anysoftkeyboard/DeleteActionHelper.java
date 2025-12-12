package com.anysoftkeyboard;

import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.ime.InputConnectionRouter;
import com.anysoftkeyboard.dictionaries.WordComposer;

/**
 * Extracted deletion logic to slim {@link AnySoftKeyboard}. Encapsulates the
 * multi-tap/backspace/forward-delete handling paths.
 */
public final class DeleteActionHelper {

  private static final int MAX_CHARS_PER_CODE_POINT = 2;

  private DeleteActionHelper() {
    // no instances
  }

  public interface Host {
    boolean isPredictionOn();

    int getCursorPosition();

    boolean isSelectionUpdateDelayed();

    void markExpectingSelectionUpdate();

    void postUpdateSuggestions();

    void sendDownUpKeyEvents(int keyCode);
  }

  /**
   * Handles delete/backspace with optional multi-tap override. Mirrors previous behavior.
   */
  public static void handleDeleteLastCharacter(
      Host host,
      InputConnectionRouter inputConnectionRouter,
      @Nullable InputConnection ic,
      WordComposer currentComposedWord,
      boolean forMultiTap) {
    final boolean wordManipulation =
        host.isPredictionOn()
            && currentComposedWord.cursorPosition() > 0
            && !currentComposedWord.isEmpty();
    if (host.isSelectionUpdateDelayed() || ic == null) {
      host.markExpectingSelectionUpdate();
      if (wordManipulation) currentComposedWord.deleteCodePointAtCurrentPosition();
      host.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
      return;
    }

    host.markExpectingSelectionUpdate();

    if (wordManipulation) {
      // NOTE: cannot use deleteSurroundingText with composing text.
      final int charsToDelete = currentComposedWord.deleteCodePointAtCurrentPosition();
      final int cursorPosition =
          currentComposedWord.cursorPosition() != currentComposedWord.charCount()
              ? host.getCursorPosition()
              : -1;

      if (cursorPosition >= 0) {
        ic.beginBatchEdit();
      }

      ic.setComposingText(currentComposedWord.getTypedWord(), 1);
      if (cursorPosition >= 0 && !currentComposedWord.isEmpty()) {
        ic.setSelection(cursorPosition - charsToDelete, cursorPosition - charsToDelete);
      }

      if (cursorPosition >= 0) {
        ic.endBatchEdit();
      }

      host.postUpdateSuggestions();
    } else {
      if (!forMultiTap) {
        host.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
      } else {
        // multi-tap path
        final CharSequence beforeText =
            ic.getTextBeforeCursor(MAX_CHARS_PER_CODE_POINT, 0);
        final int textLengthBeforeDelete =
            TextUtils.isEmpty(beforeText)
                ? 0
                : Character.charCount(
                    Character.codePointBefore(beforeText, beforeText.length()));
        if (textLengthBeforeDelete > 0) {
          ic.deleteSurroundingText(textLengthBeforeDelete, 0);
        } else {
          host.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
        }
      }
    }
  }

  /**
   * Handles forward delete (DEL) respecting composing text behavior.
   */
  public static void handleForwardDelete(
      Host host,
      InputConnectionRouter inputConnectionRouter,
      @Nullable InputConnection ic,
      WordComposer currentComposedWord) {
    if (ic == null) {
      host.sendDownUpKeyEvents(KeyEvent.KEYCODE_FORWARD_DEL);
      return;
    }

    final boolean wordManipulation =
        host.isPredictionOn()
            && currentComposedWord.cursorPosition() < currentComposedWord.charCount()
            && !currentComposedWord.isEmpty();

    if (wordManipulation) {
      currentComposedWord.deleteForward();
      final int cursorPosition =
          currentComposedWord.cursorPosition() != currentComposedWord.charCount()
              ? host.getCursorPosition()
              : -1;

      if (cursorPosition >= 0) {
        ic.beginBatchEdit();
      }

      host.markExpectingSelectionUpdate();
      ic.setComposingText(currentComposedWord.getTypedWord(), 1);
      if (cursorPosition >= 0 && !currentComposedWord.isEmpty()) {
        ic.setSelection(cursorPosition, cursorPosition);
      }

      if (cursorPosition >= 0) {
        ic.endBatchEdit();
      }

      host.postUpdateSuggestions();
    } else {
      host.sendDownUpKeyEvents(KeyEvent.KEYCODE_FORWARD_DEL);
    }
  }
}
