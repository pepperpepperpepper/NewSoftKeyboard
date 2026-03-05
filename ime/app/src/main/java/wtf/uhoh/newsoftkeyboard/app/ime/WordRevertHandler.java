package wtf.uhoh.newsoftkeyboard.app.ime;

import android.view.KeyEvent;
import androidx.annotation.NonNull;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordComposer;

/** Handles revert-last-word behavior to keep the service slimmer. */
final class WordRevertHandler {

  interface Host {
    InputConnectionRouter inputConnectionRouter();

    int getCursorPosition();

    void markExpectingSelectionUpdate();

    void sendDownUpKeyEvents(int keyCode);

    void performUpdateSuggestions();

    void clearSpaceTimeTracker();

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
    final InputConnectionRouter inputConnectionRouter = host.inputConnectionRouter();
    if (!inputConnectionRouter.hasConnection()) {
      autoCorrectState.wordRevertLength = 0;
      return new Result(currentWord, previousWord);
    }

    final int globalCursorPosition = host.getCursorPosition();
    final int startPosition = Math.max(0, globalCursorPosition - length);
    final int lengthToDelete = globalCursorPosition - startPosition;
    host.markExpectingSelectionUpdate();

    WordComposer newCurrentWord = previousWord;
    WordComposer newPreviousWord = currentWord;
    autoCorrectState.wordRevertLength = 0;
    final CharSequence typedWord = newCurrentWord.getTypedWord();
    newCurrentWord.setCursorPosition(newCurrentWord.charCount());
    boolean reverted = false;
    if (inputConnectionRouter.isComposingTextSupported()) {
      if (inputConnectionRouter.setComposingRegion(startPosition, globalCursorPosition)) {
        reverted = inputConnectionRouter.setComposingText(typedWord, 1);
      }
    }
    if (!reverted) {
      if (lengthToDelete > 0) {
        inputConnectionRouter.deleteSurroundingText(lengthToDelete, 0);
      }
      inputConnectionRouter.commitText(typedWord, 1);
    }
    inputConnectionRouter.setSelection(
        startPosition + typedWord.length(), startPosition + typedWord.length());
    host.clearSpaceTimeTracker();
    host.performUpdateSuggestions();
    if (autoCorrectState.justAutoAddedWord) {
      host.removeFromUserDictionary(typedWord.toString());
    }
    return new Result(newCurrentWord, newPreviousWord);
  }
}
