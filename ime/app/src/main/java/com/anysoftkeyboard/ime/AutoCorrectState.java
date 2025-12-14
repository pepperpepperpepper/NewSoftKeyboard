package com.anysoftkeyboard.ime;

/** Tracks auto-correct related flags and revert length. */
final class AutoCorrectState {
  int wordRevertLength = 0;
  boolean justAutoAddedWord = false;

  void reset() {
    wordRevertLength = 0;
    justAutoAddedWord = false;
  }

  boolean shouldRevertOnDelete() {
    return wordRevertLength > 0;
  }
}
