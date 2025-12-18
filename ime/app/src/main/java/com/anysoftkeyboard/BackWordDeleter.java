package com.anysoftkeyboard;

import android.text.TextUtils;
import android.view.inputmethod.InputConnection;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.dictionaries.Suggest;
import com.anysoftkeyboard.dictionaries.WordComposer;

final class BackWordDeleter {

  private BackWordDeleter() {}

  static void handleBackWord(
      @NonNull InputConnection inputConnection,
      @NonNull Runnable markExpectingSelectionUpdate,
      @NonNull Runnable postUpdateSuggestions,
      @NonNull WordComposer currentComposedWord,
      boolean isPredictionOn,
      @NonNull Suggest suggest) {
    markExpectingSelectionUpdate.run();
    if (isPredictionOn && currentComposedWord.cursorPosition() > 0 && !currentComposedWord.isEmpty()) {
      // sp#ace -> ace
      // cursor == 2
      // length == 5
      // textAfterCursor = word.substring(2, 3) -> word.substring(cursor, length - cursor)
      final CharSequence textAfterCursor =
          currentComposedWord
              .getTypedWord()
              .subSequence(currentComposedWord.cursorPosition(), currentComposedWord.charCount());
      currentComposedWord.reset();
      suggest.resetNextWordSentence();
      inputConnection.setComposingText(textAfterCursor, 0);
      postUpdateSuggestions.run();
      return;
    }
    // I will not delete more than 128 characters. Just a safe-guard.
    // this will also allow me do just one call to getTextBeforeCursor!
    // Which is always good. This is a part of issue 951.
    CharSequence beforeCursor = inputConnection.getTextBeforeCursor(128, 0);
    if (TextUtils.isEmpty(beforeCursor)) {
      return; // nothing to delete
    }
    // TWO OPTIONS
    // 1) Either we do like Linux and Windows (and probably ALL desktop
    // OSes):
    // Delete all the characters till a complete word was deleted:
    /*
     * What to do: We delete until we find a separator (the function
     * isBackWordDeleteCodePoint). Note that we MUST delete a delete a whole word!
     * So if the back-word starts at separators, we'll delete those, and then
     * the word before: "test this,       ," -> "test "
     */
    // Pro: same as desktop
    // Con: when auto-caps is on (the default), this will delete the
    // previous word, which can be annoying..
    // E.g., Writing a sentence, then a period, then ASK will auto-caps,
    // then when the user press backspace (for some reason),
    // the entire previous word deletes.

    // 2) Or we delete all whitespaces and then all the characters
    // till we encounter a separator, but delete at least one character.
    /*
     * What to do: We first delete all whitespaces, and then we delete until we find
     * a separator (the function isBackWordDeleteCodePoint).
     * Note that we MUST delete at least one character "test this, " -> "test this" -> "test "
     */
    // Pro: Supports auto-caps, and mostly similar to desktop OSes
    // Con: Not all desktop use-cases are here.

    // For now, I go with option 2, but I'm open for discussion.

    // 2b) "test this, " -> "test this"

    final int inputLength = beforeCursor.length();
    int idx = inputLength;
    int lastCodePoint = Character.codePointBefore(beforeCursor, idx);
    // First delete all trailing whitespaces, if there are any...
    while (Character.isWhitespace(lastCodePoint)) {
      idx -= Character.charCount(lastCodePoint);
      if (idx == 0) break;
      lastCodePoint = Character.codePointBefore(beforeCursor, idx);
    }
    // If there is still something left to delete...
    if (idx > 0) {
      final int remainingLength = idx;

      // This while-loop isn't guaranteed to run even once...
      while (isBackWordDeleteCodePoint(lastCodePoint)) {
        idx -= Character.charCount(lastCodePoint);
        if (idx == 0) break;
        lastCodePoint = Character.codePointBefore(beforeCursor, idx);
      }

      // but we're supposed to delete at least one Unicode codepoint.
      if (idx == remainingLength) {
        idx -= Character.charCount(lastCodePoint);
      }
    }
    inputConnection.deleteSurroundingText(inputLength - idx, 0); // it is always > 0 !
  }

  private static boolean isBackWordDeleteCodePoint(int c) {
    return Character.isLetterOrDigit(c);
  }
}

