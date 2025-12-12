package com.anysoftkeyboard.ime;

import android.text.TextUtils;
import android.view.inputmethod.InputConnection;
import com.menny.android.anysoftkeyboard.AnyApplication;

/** Handles committing a picked suggestion to the input connection. */
public final class SuggestionCommitter {

  public interface Host {
    InputConnection currentInputConnection();

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
    final InputConnection ic = host.currentInputConnection();
    if (ic != null) {
      final boolean delayedUpdates = host.isSelectionUpdateDelayed();
      host.markExpectingSelectionUpdate();
      if (TextUtils.equals(wordToCommit, typedWord) || delayedUpdates) {
        ic.commitText(wordToCommit, 1);
      } else {
        AnyApplication.getDeviceSpecific()
            .commitCorrectionToInputConnection(
                ic, host.getCursorPosition() - typedWord.length(), typedWord, wordToCommit);
      }
    }

    host.clearSuggestions();
  }
}
