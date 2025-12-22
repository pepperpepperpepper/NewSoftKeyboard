package com.anysoftkeyboard.ime;

import androidx.annotation.NonNull;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.WordComposer;

/** Orchestrates restart of word suggestions after cursor moves. */
final class WordRestartCoordinator {

  private final WordRestartHelper wordRestartHelper = new WordRestartHelper();

  void performRestartWordSuggestion(
      @NonNull InputConnectionRouter inputConnectionRouter, @NonNull Host host) {
    if (!host.canRestartWordSuggestion()) {
      Logger.d(host.logTag(), "performRestartWordSuggestion canRestartWordSuggestion == false");
      return;
    }

    if (inputConnectionRouter.current() == null) {
      Logger.d(host.logTag(), "performRestartWordSuggestion no InputConnection");
      return;
    }
    inputConnectionRouter.beginBatchEdit();
    try {
      host.abortCorrectionAndResetPredictionState(false);

      wordRestartHelper.restartWordFromCursor(
          inputConnectionRouter,
          host.currentWord(),
          new WordRestartHelper.Host() {
            @Override
            public boolean isWordSeparator(int codePoint) {
              return host.isWordSeparator(codePoint);
            }

            @Override
            public int getCursorPosition() {
              return host.getCursorPosition();
            }

            @Override
            public void markExpectingSelectionUpdate() {
              host.markExpectingSelectionUpdate();
            }

            @Override
            public void performUpdateSuggestions() {
              host.performUpdateSuggestions();
            }

            @Override
            public String logTag() {
              return host.logTag();
            }
          });
    } finally {
      inputConnectionRouter.endBatchEdit();
    }
  }

  interface Host {
    boolean canRestartWordSuggestion();

    void abortCorrectionAndResetPredictionState(boolean disabledUntilNextInputStart);

    boolean isWordSeparator(int codePoint);

    int getCursorPosition();

    void markExpectingSelectionUpdate();

    void performUpdateSuggestions();

    String logTag();

    WordComposer currentWord();
  }
}
