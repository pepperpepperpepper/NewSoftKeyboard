package wtf.uhoh.newsoftkeyboard.app.ime;

import com.anysoftkeyboard.api.KeyCodes;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;

/** Tracks last pressed key/code to keep {@link ImeSuggestionsController} lean. */
final class LastKeyTracker {

  private Keyboard.Key lastKey;
  private int lastPrimaryKey = Integer.MIN_VALUE;

  void record(Keyboard.Key key, int primaryCode) {
    lastKey = key;
    lastPrimaryKey = primaryCode;
  }

  void reset() {
    lastKey = null;
  }

  Keyboard.Key lastKey() {
    return lastKey;
  }

  boolean shouldMarkSpaceTime(int primaryCode) {
    return lastPrimaryKey == primaryCode && KeyCodes.isOutputKeyCode(primaryCode);
  }
}
