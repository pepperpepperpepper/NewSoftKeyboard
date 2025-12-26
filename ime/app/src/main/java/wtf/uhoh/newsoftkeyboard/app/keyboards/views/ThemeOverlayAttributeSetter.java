package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.overlay.ThemeOverlayCombiner;

/** Applies theme overlay attributes such as key background, vertical correction, and dim amount. */
final class ThemeOverlayAttributeSetter {

  private ThemeOverlayAttributeSetter() {}

  interface FloatConsumer {
    void accept(float value);
  }

  static boolean apply(
      int localAttrId,
      TypedArray remoteTypedArray,
      int remoteTypedArrayIndex,
      ThemeOverlayCombiner overlayCombiner,
      java.util.function.IntConsumer setVerticalCorrection,
      FloatConsumer setBackgroundDimAmount) {

    return switch (localAttrId) {
      case R.attr.keyBackground -> {
        Drawable keyBackground = remoteTypedArray.getDrawable(remoteTypedArrayIndex);
        if (keyBackground == null) {
          yield false;
        }
        overlayCombiner.setThemeKeyBackground(keyBackground);
        yield true;
      }
      case R.attr.verticalCorrection -> {
        int correction = remoteTypedArray.getDimensionPixelOffset(remoteTypedArrayIndex, -1);
        if (correction == -1) yield false;
        setVerticalCorrection.accept(correction);
        yield true;
      }
      case R.attr.backgroundDimAmount -> {
        float dim = remoteTypedArray.getFloat(remoteTypedArrayIndex, -1f);
        if (dim == -1f) yield false;
        setBackgroundDimAmount.accept(dim);
        yield true;
      }
      default -> false;
    };
  }
}
