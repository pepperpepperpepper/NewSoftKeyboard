package wtf.uhoh.newsoftkeyboard.app.ime;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import wtf.uhoh.newsoftkeyboard.BuildConfig;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateView;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordComposer;

/** Handles character input routing, composing, and prediction-aware output. */
final class CharacterInputHandler {

  interface Host {
    WordComposer word();

    AutoCorrectState autoCorrectState();

    PredictionState predictionState();

    boolean isPredictionOn();

    boolean isSuggestionAffectingCharacter(int code);

    boolean isAlphabet(int code);

    boolean isShiftActive();

    int getCursorPosition();

    void postUpdateSuggestions();

    void clearSuggestions();

    @Nullable
    CandidateView candidateView();

    @NonNull
    InputConnectionRouter inputConnectionRouter();

    void markExpectingSelectionUpdate();

    void sendKeyChar(char c);

    void setLastCharacterWasShifted(boolean shifted);
  }

  void handleCharacter(
      int primaryCode,
      Keyboard.Key key,
      int multiTapIndex,
      int[] nearByKeyCodes,
      String logTag,
      Host host) {
    if (BuildConfig.DEBUG) {
      Logger.d(
          logTag,
          "handleCharacter: %d, isPredictionOn: %s, isCurrentlyPredicting: %s",
          primaryCode,
          host.isPredictionOn(),
          host.isPredictionOn() && !host.word().isEmpty());
    }

    initializeWordIfNeeded(primaryCode, host);

    host.setLastCharacterWasShifted(host.isShiftActive());

    final InputConnectionRouter router = host.inputConnectionRouter();
    host.word().add(primaryCode, nearByKeyCodes);
    if (host.isPredictionOn()) {
      updateComposingForPrediction(primaryCode, key, multiTapIndex, router, host);
    } else {
      outputWithoutPrediction(primaryCode, router, host);
    }
    host.autoCorrectState().justAutoAddedWord = false;
  }

  private void initializeWordIfNeeded(int primaryCode, Host host) {
    final WordComposer word = host.word();
    if (word.charCount() != 0 || !host.isAlphabet(primaryCode)) {
      return;
    }
    host.autoCorrectState().wordRevertLength = 0;
    word.reset();
    final PredictionState predictionState = host.predictionState();
    predictionState.autoCorrectOn =
        host.isPredictionOn()
            && predictionState.autoComplete
            && predictionState.inputFieldSupportsAutoPick;
    if (host.isShiftActive()) {
      word.setFirstCharCapitalized(true);
    }
  }

  private void updateComposingForPrediction(
      int primaryCode,
      Keyboard.Key key,
      int multiTapIndex,
      @NonNull InputConnectionRouter router,
      Host host) {
    final WordComposer word = host.word();
    if (router.current() != null) {
      final int newCursorPosition =
          computeCursorPositionAfterChar(primaryCode, key, multiTapIndex, word, host);
      if (newCursorPosition > 0) {
        router.beginBatchEdit();
      }

      host.markExpectingSelectionUpdate();
      router.setComposingText(word.getTypedWord(), 1);
      if (newCursorPosition > 0) {
        router.setSelection(newCursorPosition, newCursorPosition);
        router.endBatchEdit();
      }
    }
    if (host.isSuggestionAffectingCharacter(primaryCode)) {
      if (!host.isPredictionOn()) {
        host.clearSuggestions();
      } else {
        host.postUpdateSuggestions();
      }
    } else {
      final CandidateView candidateView = host.candidateView();
      if (candidateView != null) {
        candidateView.replaceTypedWord(word.getTypedWord());
      }
    }
  }

  private void outputWithoutPrediction(
      int primaryCode, @NonNull InputConnectionRouter router, Host host) {
    final boolean hasConnection = router.current() != null;
    if (hasConnection) {
      router.beginBatchEdit();
    }
    host.markExpectingSelectionUpdate();
    for (char c : Character.toChars(primaryCode)) {
      host.sendKeyChar(c);
    }
    if (hasConnection) {
      router.endBatchEdit();
    }
  }

  private int computeCursorPositionAfterChar(
      int primaryCode, Keyboard.Key key, int multiTapIndex, @NonNull WordComposer word, Host host) {
    if (word.cursorPosition() == word.charCount()) {
      return -1;
    }

    int newCursorPosition;
    if (multiTapIndex > 0) {
      final int previousKeyCode = key.getMultiTapCode(multiTapIndex - 1);
      newCursorPosition = Character.charCount(primaryCode) - Character.charCount(previousKeyCode);
    } else {
      newCursorPosition = Character.charCount(primaryCode);
    }
    newCursorPosition += host.getCursorPosition();
    return newCursorPosition;
  }
}
