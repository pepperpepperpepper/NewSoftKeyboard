package wtf.uhoh.newsoftkeyboard.app.keyboards;

import androidx.annotation.NonNull;
import java.util.List;

final class KeyEdgeFlagsFixer {

  private KeyEdgeFlagsFixer() {}

  static void fixEdgeFlags(@NonNull List<Keyboard.Key> keys) {
    if (keys.isEmpty()) return;

    // some assumptions:
    // 1) the first item in the keys list is at the top of the keyboard
    // 2) the last item is the bottom of the keyboard
    // 3) the first key in every row must be left
    // 4) the last key in every row must be right
    // 5) the keys are ordered from top to bottom, from left to right

    final int topY = keys.get(0).y;
    final int bottomY = keys.get(keys.size() - 1).y;

    Keyboard.Key previousKey = null;
    for (Keyboard.Key key : keys) {
      key.edgeFlags = 0;
      if (key.y == topY) key.edgeFlags |= Keyboard.EDGE_TOP;
      if (key.y == bottomY) key.edgeFlags |= Keyboard.EDGE_BOTTOM;

      if (previousKey == null || previousKey.y != key.y) {
        // new row
        key.edgeFlags |= Keyboard.EDGE_LEFT;
        if (previousKey != null) {
          previousKey.edgeFlags |= Keyboard.EDGE_RIGHT;
        }
      }

      previousKey = key;
    }

    // last key must be edge right
    if (previousKey != null) {
      previousKey.edgeFlags |= Keyboard.EDGE_RIGHT;
    }
  }
}
