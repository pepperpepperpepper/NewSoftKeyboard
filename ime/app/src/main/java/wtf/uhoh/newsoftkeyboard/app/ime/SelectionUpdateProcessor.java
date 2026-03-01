package wtf.uhoh.newsoftkeyboard.app.ime;

import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordComposer;

/**
 * Extracted handler for {@link ImeSuggestionsController#onUpdateSelection}. Keeps the branching
 * logic out of the main IME class while preserving behavior.
 */
final class SelectionUpdateProcessor {

  interface Host {
    boolean isPredictionOn();

    boolean isCurrentlyPredicting();

    /**
     * Called when the user moved the cursor/selection unexpectedly while we are idle (not composing
     * a word). This is a good time to reset next-word state and prevent stale fallback suggestions.
     */
    void onUnexpectedCursorMoveWhileNotPredicting();

    /** Returns true when we're still waiting for a selection update from our own edits. */
    boolean isSelectionUpdateDelayed();

    InputConnectionRouter inputConnectionRouter();

    void abortCorrectionAndResetPredictionState(boolean force);

    void postRestartWordSuggestion();

    boolean shouldRevertOnDelete();

    void setWordRevertLength(int length);

    int getWordRevertLength();

    void resetLastSpaceTimeStamp();

    long getExpectingSelectionUpdateBy();

    void clearExpectingSelectionUpdate();

    void markSelectionUpdateReceived();

    void setExpectingSelectionUpdateBy(long value);

    int getCandidateStartPositionDangerous();

    int getCandidateEndPositionDangerous();

    WordComposer getCurrentWord();

    String logTag();
  }

  void onUpdateSelection(
      int oldSelStart,
      int oldSelEnd,
      int newSelStart,
      int newSelEnd,
      int candidatesStart,
      int candidatesEnd,
      Host host) {
    final int oldCandidateStart = host.getCandidateStartPositionDangerous();
    final int oldCandidateEnd = host.getCandidateEndPositionDangerous();

    Logger.v(
        host.logTag(),
        "onUpdateSelection: word '%s', position %d.",
        host.getCurrentWord().getTypedWord(),
        host.getCurrentWord().cursorPosition());

    final boolean noChange =
        newSelStart == oldSelStart
            && oldSelEnd == newSelEnd
            && oldCandidateStart == candidatesStart
            && oldCandidateEnd == candidatesEnd;
    final boolean isExpectedEvent = host.isSelectionUpdateDelayed();
    final boolean cursorMovedUnexpectedly = (oldSelStart != newSelStart || oldSelEnd != newSelEnd);
    if (noChange) {
      Logger.v(host.logTag(), "onUpdateSelection: no-change. Discarding.");
      return;
    }

    if (isExpectedEvent) {
      boolean discardExpectedEvent = true;
      if (cursorMovedUnexpectedly
          && host.isPredictionOn()
          && host.inputConnectionRouter().hasConnection()) {
        if (newSelStart != newSelEnd) {
          discardExpectedEvent = false;
        } else if (host.isCurrentlyPredicting()) {
          final WordComposer currentWord = host.getCurrentWord();
          final boolean composingSupported =
              host.inputConnectionRouter().isComposingTextSupported();

          if (composingSupported && oldCandidateStart >= 0 && candidatesStart < 0) {
            discardExpectedEvent = false;
          } else if (candidatesStart >= 0 && candidatesEnd >= candidatesStart) {
            final int newPosition = newSelEnd - candidatesStart;
            final boolean insideCurrentWord =
                newSelStart >= candidatesStart
                    && newSelStart <= candidatesEnd
                    && newPosition >= 0
                    && newPosition <= currentWord.charCount();
            if (insideCurrentWord) {
              currentWord.setCursorPosition(newPosition);
            } else {
              discardExpectedEvent = false;
            }
          }
        }
      }

      if (discardExpectedEvent) {
        host.markSelectionUpdateReceived();
        Logger.v(host.logTag(), "onUpdateSelection: Expected event. Discarding.");
        return;
      }

      host.clearExpectingSelectionUpdate();
    } else {
      host.clearExpectingSelectionUpdate();
    }

    if (cursorMovedUnexpectedly) {
      host.resetLastSpaceTimeStamp();
      if (host.shouldRevertOnDelete()) {
        Logger.d(
            host.logTag(),
            "onUpdateSelection: user moved cursor from a undo-commit sensitive position. Will not"
                + " be able to undo-commit.");
        host.setWordRevertLength(0);
      }
    }

    if (!host.isPredictionOn()) {
      return; // not relevant if no prediction is needed.
    }

    if (!host.inputConnectionRouter().hasConnection()) {
      return; // can't do anything without this connection
    }

    if (cursorMovedUnexpectedly && !host.isCurrentlyPredicting()) {
      host.onUnexpectedCursorMoveWhileNotPredicting();
    }

    Logger.d(host.logTag(), "onUpdateSelection: ok, let's see what can be done");

    if (newSelStart != newSelEnd) {
      Logger.d(host.logTag(), "onUpdateSelection: text selection.");
      host.abortCorrectionAndResetPredictionState(false);
    } else if (cursorMovedUnexpectedly) {
      if (host.isCurrentlyPredicting()) {
        final var newPosition = newSelEnd - candidatesStart;
        if (newSelStart >= candidatesStart
            && newSelStart <= candidatesEnd
            && newPosition >= 0
            && newPosition <= host.getCurrentWord().charCount()) {
          Logger.d(
              host.logTag(),
              "onUpdateSelection: inside the currently typed word to location %d.",
              newPosition);
          host.getCurrentWord().setCursorPosition(newPosition);
        } else {
          Logger.d(
              host.logTag(),
              "onUpdateSelection: cursor moving outside the currently predicting word");
          host.abortCorrectionAndResetPredictionState(false);
          host.postRestartWordSuggestion();
        }
      } else {
        Logger.d(
            host.logTag(),
            "onUpdateSelection: not predicting at this moment, maybe the cursor is now at a new"
                + " word?");
        host.postRestartWordSuggestion();
      }
    } else {
      Logger.v(host.logTag(), "onUpdateSelection: cursor moved expectedly");
    }
  }
}
