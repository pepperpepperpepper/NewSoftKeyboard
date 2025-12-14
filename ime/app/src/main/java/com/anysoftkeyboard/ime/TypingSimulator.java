package com.anysoftkeyboard.ime;

import android.view.inputmethod.InputConnection;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.keyboards.Keyboard;

/** Simulates character-by-character typing for injected text (used by onTyping). */
final class TypingSimulator {

  interface Host {
    InputConnection currentInputConnection();

    Keyboard.Key lastKey();

    void onKey(int primaryCode, Keyboard.Key key, int multiTapIndex, int[] nearByKeyCodes, boolean fromUI);

    void clearSpaceTimeTracker();

    boolean isAutoCorrectOn();

    void setAutoCorrectOn(boolean on);
  }

  void simulate(CharSequence text, Host host) {
    InputConnection ic = host.currentInputConnection();
    if (ic == null) return;

    ic.beginBatchEdit();

    final boolean originalAutoCorrect = host.isAutoCorrectOn();
    host.setAutoCorrectOn(false);
    for (int pointCodeIndex = 0; pointCodeIndex < text.length(); ) {
      int pointCode = Character.codePointAt(text, pointCodeIndex);
      pointCodeIndex += Character.charCount(pointCode);
      host.clearSpaceTimeTracker(); // ensure double spaces don't count
      host.onKey(pointCode, host.lastKey(), 0, new int[] {pointCode}, true);
    }
    host.setAutoCorrectOn(originalAutoCorrect);

    ic.endBatchEdit();
  }
}
