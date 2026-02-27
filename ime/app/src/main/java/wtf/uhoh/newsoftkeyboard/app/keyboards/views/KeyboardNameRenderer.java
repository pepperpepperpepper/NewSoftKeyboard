package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.text.TextUtils;

/**
 * Small helper to keep keyboard-name rendering details out of {@link KeyboardViewBase}.
 *
 * <p>It decides when to substitute the keyboard name as the space-key label and prepares the
 * paint/metrics for drawing it.
 */
final class KeyboardNameRenderer {

  CharSequence applyKeyboardNameIfNeeded(
      CharSequence currentLabel,
      boolean keyIsSpace,
      boolean drawKeyboardNameText,
      CharSequence keyboardName) {
    // Space is special: users expect to see the keyboard name on the spacebar even when an icon is
    // drawn for that key. This must not change the key model; it is only a draw-time substitution.
    if (keyIsSpace && drawKeyboardNameText) {
      if (currentLabel == null) return keyboardName;

      // Many layouts/themes use a "single space" as a placeholder label; treat whitespace-only as
      // empty so the keyboard-name feature can still work.
      if (TextUtils.getTrimmedLength(currentLabel) == 0) {
        return keyboardName;
      }
    }
    return currentLabel;
  }
}
