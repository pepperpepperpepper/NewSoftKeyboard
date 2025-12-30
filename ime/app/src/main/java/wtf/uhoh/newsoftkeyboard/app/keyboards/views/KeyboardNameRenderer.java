package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.Paint;
import android.graphics.Typeface;
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
    if (keyIsSpace && drawKeyboardNameText && TextUtils.isEmpty(currentLabel)) {
      return keyboardName;
    }
    return currentLabel;
  }

  void preparePaintForKeyboardName(Paint paint, float textSize) {
    paint.setTextSize(textSize);
    paint.setTypeface(Typeface.DEFAULT_BOLD);
  }
}
