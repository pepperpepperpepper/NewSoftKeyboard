package wtf.uhoh.newsoftkeyboard.app.ime;

import android.text.TextUtils;
import android.view.KeyEvent;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;

/** Handles committing a picked suggestion to the input connection. */
public final class SuggestionCommitter {

  public interface Host {
    InputConnectionRouter inputConnectionRouter();

    boolean isSelectionUpdateDelayed();

    void markExpectingSelectionUpdate();

    int getCursorPosition();

    void clearSuggestions();
  }

  private final Host host;

  public SuggestionCommitter(Host host) {
    this.host = host;
  }

  public void commitWordToInput(CharSequence wordToCommit, CharSequence typedWord) {
    final InputConnectionRouter inputConnectionRouter = host.inputConnectionRouter();
    if (!inputConnectionRouter.hasConnection()) {
      host.clearSuggestions();
      return;
    }

    final boolean delayedUpdates = host.isSelectionUpdateDelayed();
    host.markExpectingSelectionUpdate();
    if (!inputConnectionRouter.isComposingTextSupported()) {
      commitWithoutComposingText(inputConnectionRouter, wordToCommit, typedWord);
      host.clearSuggestions();
      return;
    }
    if (TextUtils.equals(wordToCommit, typedWord) || delayedUpdates) {
      inputConnectionRouter.commitText(wordToCommit, 1);
    } else {
      NskApplicationBase.getDeviceSpecific()
          .commitCorrectionToInputConnection(
              inputConnectionRouter.current(),
              host.getCursorPosition() - typedWord.length(),
              typedWord,
              wordToCommit);
    }

    host.clearSuggestions();
  }

  public void commitManuallyPickedWordToInput(
      CharSequence wordToCommit, CharSequence typedWordInEditor) {
    final InputConnectionRouter inputConnectionRouter = host.inputConnectionRouter();
    if (!inputConnectionRouter.hasConnection()) {
      host.clearSuggestions();
      return;
    }

    // Manual picks may be preceded by a "multi-edit" expectation (word + optional leading/trailing
    // spaces). Avoid shrinking that expectation budget mid-flow.
    if (!host.isSelectionUpdateDelayed()) {
      host.markExpectingSelectionUpdate();
    }
    if (typedWordInEditor.length() == 0) {
      maybeInsertLeadingSpaceForNextWordPick(inputConnectionRouter);
    }
    if (!inputConnectionRouter.isComposingTextSupported()) {
      if (!TextUtils.equals(wordToCommit, typedWordInEditor)) {
        if (typedWordInEditor.length() > 0) {
          inputConnectionRouter.deleteSurroundingText(typedWordInEditor.length(), 0);
        }
        inputConnectionRouter.commitText(wordToCommit, 1);
      }
      host.clearSuggestions();
      return;
    }

    inputConnectionRouter.commitText(wordToCommit, 1);
    host.clearSuggestions();
  }

  private static void maybeInsertLeadingSpaceForNextWordPick(
      InputConnectionRouter inputConnectionRouter) {
    final CharSequence beforeCursor = inputConnectionRouter.getTextBeforeCursor(1, 0);
    if (beforeCursor == null || beforeCursor.length() == 0) {
      return;
    }

    final char c = beforeCursor.charAt(beforeCursor.length() - 1);
    if (!Character.isLetterOrDigit(c)) {
      return;
    }

    final boolean committed = inputConnectionRouter.commitText(" ", 1);
    if (!committed) {
      inputConnectionRouter.sendKeyDown(KeyEvent.KEYCODE_SPACE);
      inputConnectionRouter.sendKeyUp(KeyEvent.KEYCODE_SPACE);
    }
  }

  private static void commitWithoutComposingText(
      InputConnectionRouter inputConnectionRouter,
      CharSequence wordToCommit,
      CharSequence typedWord) {
    if (TextUtils.equals(wordToCommit, typedWord)) {
      return;
    }
    if (typedWord.length() > 0) {
      inputConnectionRouter.deleteSurroundingText(typedWord.length(), 0);
    }
    inputConnectionRouter.commitText(wordToCommit, 1);
  }
}
