package com.anysoftkeyboard.ime.gesturetyping;

import androidx.annotation.NonNull;
import com.anysoftkeyboard.gesturetyping.GestureTypingDetector;
import com.anysoftkeyboard.keyboards.Keyboard;
import java.util.ArrayList;
import java.util.List;

public final class KeyboardGestureKeys {

  private KeyboardGestureKeys() {}

  @NonNull
  public static List<GestureTypingDetector.GestureKey> fromKeyboardKeys(
      @NonNull List<Keyboard.Key> keys) {
    final ArrayList<GestureTypingDetector.GestureKey> gestureKeys = new ArrayList<>(keys.size());
    for (Keyboard.Key key : keys) {
      gestureKeys.add(new KeyboardGestureKey(key));
    }
    return gestureKeys;
  }

  private static final class KeyboardGestureKey implements GestureTypingDetector.GestureKey {
    private final Keyboard.Key key;

    private KeyboardGestureKey(Keyboard.Key key) {
      this.key = key;
    }

    @Override
    public int getCodesCount() {
      return key.getCodesCount();
    }

    @Override
    public int getCodeAtIndex(int index) {
      return key.getCodeAtIndex(index, false);
    }

    @Override
    public boolean isInside(int x, int y) {
      return key.isInside(x, y);
    }

    @Override
    public int getCenterX() {
      return Keyboard.Key.getCenterX(key);
    }

    @Override
    public int getCenterY() {
      return Keyboard.Key.getCenterY(key);
    }
  }
}
