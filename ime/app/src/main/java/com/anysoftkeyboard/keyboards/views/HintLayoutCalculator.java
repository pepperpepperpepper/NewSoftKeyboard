package com.anysoftkeyboard.keyboards.views;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import androidx.annotation.NonNull;

final class HintLayoutCalculator {

  int resolveHintAlign(int customHintGravity, int themeHintLabelAlign) {
    return customHintGravity == Gravity.NO_GRAVITY
        ? themeHintLabelAlign
        : customHintGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
  }

  int resolveHintVAlign(int customHintGravity, int themeHintLabelVAlign) {
    return customHintGravity == Gravity.NO_GRAVITY
        ? themeHintLabelVAlign
        : customHintGravity & Gravity.VERTICAL_GRAVITY_MASK;
  }

  void placeHintIcon(
      @NonNull Rect keyBackgroundPadding,
      int keyWidth,
      int keyHeight,
      int hintAlign,
      int hintVAlign,
      int iconWidth,
      int iconHeight,
      @NonNull Drawable drawable) {
    final int iconLeft;
    if (hintAlign == Gravity.START) {
      iconLeft = (int) (keyBackgroundPadding.left + 0.5f);
    } else if (hintAlign == Gravity.CENTER_HORIZONTAL) {
      iconLeft =
          (int)
              (keyBackgroundPadding.left
                  + (keyWidth - keyBackgroundPadding.left - keyBackgroundPadding.right - iconWidth)
                      / 2f);
    } else {
      iconLeft = (int) (keyWidth - keyBackgroundPadding.right - iconWidth - 0.5f);
    }
    final int iconTop;
    if (hintVAlign == Gravity.TOP) {
      iconTop = (int) (keyBackgroundPadding.top + 0.5f);
    } else {
      iconTop =
          (int)
              (keyHeight - keyBackgroundPadding.bottom - iconHeight - 0.5f);
    }
    drawable.setBounds(iconLeft, iconTop, iconLeft + iconWidth, iconTop + iconHeight);
  }
}
