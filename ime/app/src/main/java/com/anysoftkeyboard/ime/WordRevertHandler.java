package com.anysoftkeyboard.ime;

import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.dictionaries.WordComposer;

/** Handles revert-last-word behavior to keep the service slimmer. */
final class WordRevertHandler {

  interface Host {
    InputConnection currentInputConnection();

    int getCursorPosition();

    void sendDownUpKeyEvents(int keyCode);

    void performUpdateSuggestions();

    void removeFromUserDictionary(String word);
  }

  record Result(@NonNull WordComposer currentWord, @NonNull WordComposer previousWord) {}

  Result revertLastWord(
      @NonNull AutoCorrectState autoCorrectState,
      @NonNull PredictionState predictionState,
      @NonNull WordComposer currentWord,
      @NonNull WordComposer previousWord,
      @NonNull Host host) {
    if (autoCorrectState.wordRevertLength == 0) {
      host.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
      return new Result(currentWord, previousWord);
    }

    final int length = autoCorrectState.wordRevertLength;
    predictionState.autoCorrectOn = false;
    final InputConnection ic = host.currentInputConnection();
    if (ic == null) {
      autoCorrectState.wordRevertLength = 0;
      return new Result(currentWord, previousWord);
    }

    final int globalCursorPosition = host.getCursorPosition();
    ic.setComposingRegion(globalCursorPosition - length, globalCursorPosition);
    WordComposer newCurrentWord = previousWord;
    WordComposer newPreviousWord = currentWord;
    autoCorrectState.wordRevertLength = 0;
    final CharSequence typedWord = newCurrentWord.getTypedWord();
    ic.setComposingText(typedWord, 1);
    host.performUpdateSuggestions();
    if (autoCorrectState.justAutoAddedWord) {
      host.removeFromUserDictionary(typedWord.toString());
    }
    return new Result(newCurrentWord, newPreviousWord);
  }
}
