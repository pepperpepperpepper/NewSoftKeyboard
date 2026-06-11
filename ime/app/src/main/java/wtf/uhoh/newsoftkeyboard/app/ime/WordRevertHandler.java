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

    if (!inputConnectionRouter.isComposingTextSupported()) {
      // Terminal mode (or any editor without composing): the revert round-trip
      // (deleteSurroundingText + commitText) duplicates already-committed characters in editors
      // that don't honour deleteSurroundingText (e.g. ConnectBot). Treat backspace as a plain
      // KEYCODE_DEL and clear the revert state so it doesn't fire again.
      autoCorrectState.wordRevertLength = 0;
      host.sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
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
    // Remember the rejected correction before performUpdateSuggestions nulls the preferred word
    // (getPreferredWord falls back to the typed word, so equality means "no real correction").
    final CharSequence revertedCorrection = newCurrentWord.getPreferredWord();
    if (revertedCorrection != null
        && !revertedCorrection.toString().equalsIgnoreCase(typedWord.toString())) {
      autoCorrectState.recordRejectedCorrection(typedWord, revertedCorrection);
    }
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
