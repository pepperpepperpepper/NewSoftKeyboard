package com.anysoftkeyboard.ime;

import com.anysoftkeyboard.keyboards.Keyboard;

/** Simulates character-by-character typing for injected text (used by onTyping). */
final class TypingSimulator {

  interface Host {
    InputConnectionRouter inputConnectionRouter();

    Keyboard.Key lastKey();

    void onKey(
        int primaryCode, Keyboard.Key key, int multiTapIndex, int[] nearByKeyCodes, boolean fromUI);

    void clearSpaceTimeTracker();

    boolean isAutoCorrectOn();

    void setAutoCorrectOn(boolean on);
  }

  void simulate(CharSequence text, Host host) {
    final InputConnectionRouter router = host.inputConnectionRouter();
    if (router.current() == null) return;
    router.beginBatchEdit();

    final boolean originalAutoCorrect = host.isAutoCorrectOn();
    host.setAutoCorrectOn(false);
    for (int pointCodeIndex = 0; pointCodeIndex < text.length(); ) {
      int pointCode = Character.codePointAt(text, pointCodeIndex);
      pointCodeIndex += Character.charCount(pointCode);
      host.clearSpaceTimeTracker(); // ensure double spaces don't count
      host.onKey(pointCode, host.lastKey(), 0, new int[] {pointCode}, true);
    }
    host.setAutoCorrectOn(originalAutoCorrect);

    router.endBatchEdit();
  }
}
