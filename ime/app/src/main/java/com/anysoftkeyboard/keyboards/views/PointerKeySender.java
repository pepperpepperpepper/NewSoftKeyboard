package com.anysoftkeyboard.keyboards.views;

import androidx.annotation.Nullable;
import com.anysoftkeyboard.keyboards.Keyboard;

/**
 * Sends key actions/text to the IME listener for a single pointer.
 *
 * <p>Owned by {@link PointerTracker}. Extracted to keep {@link PointerTracker} focused on
 * per-pointer state transitions.
 */
final class PointerKeySender {

  private final KeyDetector keyDetector;
  private final PointerMultiTapHandler multiTapHandler;

  PointerKeySender(KeyDetector keyDetector, PointerMultiTapHandler multiTapHandler) {
    this.keyDetector = keyDetector;
    this.multiTapHandler = multiTapHandler;
  }

  void sendKey(
      @Nullable OnKeyboardActionListener listener,
      int index,
      @Nullable Keyboard.Key key,
      int x,
      int y,
      long eventTime,
      boolean withRelease) {
    if (key == null) {
      if (listener != null) {
        listener.onCancel();
      }
      return;
    }

    final boolean isShifted = keyDetector.isKeyShifted(key);

    if ((key.typedText != null && !isShifted) || (key.shiftedTypedText != null && isShifted)) {
      if (listener != null) {
        multiTapHandler.resetTapCount();

        final CharSequence text = isShifted ? key.shiftedTypedText : key.typedText;
        listener.onText(key, text);
        if (withRelease) listener.onRelease(key.getPrimaryCode());
      }
    } else if ((key.text != null && !isShifted) || (key.shiftedText != null && isShifted)) {
      if (listener != null) {
        multiTapHandler.resetTapCount();

        final CharSequence text = isShifted ? key.shiftedText : key.text;
        listener.onText(key, text);
        if (withRelease) listener.onRelease(key.getPrimaryCode());
      }
    } else {
      final int code;
      int[] nearByKeyCodes = keyDetector.newCodeArray();
      keyDetector.getKeyIndexAndNearbyCodes(x, y, nearByKeyCodes);
      boolean multiTapStarted = false;
      // Multi-tap
      if (multiTapHandler.isInMultiTap()) {
        if (multiTapHandler.tapCount() != -1) {
          multiTapStarted = true;
          if (listener != null) listener.onMultiTapStarted();
        } else {
          multiTapHandler.resetTapCount();
        }
        code = key.getMultiTapCode(multiTapHandler.tapCount());
      } else {
        code = key.getCodeAtIndex(0, isShifted);
      }
      /*
       * Swap the first and second values in the codes array if the primary code is not the first
       * value but the second value in the array. This happens when key debouncing is in effect.
       */
      if (nearByKeyCodes.length >= 2 && nearByKeyCodes[0] != code && nearByKeyCodes[1] == code) {
        nearByKeyCodes[1] = nearByKeyCodes[0];
        nearByKeyCodes[0] = code;
      }
      if (listener != null) {
        listener.onKey(code, key, multiTapHandler.tapCount(), nearByKeyCodes, x >= 0 || y >= 0);
        if (withRelease) listener.onRelease(code);

        if (multiTapStarted) {
          listener.onMultiTapEnded();
        }
      }
    }

    multiTapHandler.markKeySent(index, eventTime);
  }
}
