package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.Paint;
import android.graphics.Typeface;
import androidx.annotation.NonNull;

/** Centralizes text/label paint setup and sizing. */
final class LabelPaintConfigurator {
  private final TextWidthCache textWidthCache;

  LabelPaintConfigurator(TextWidthCache textWidthCache) {
    this.textWidthCache = textWidthCache;
  }

  float adjustTextSizeForLabel(
      @NonNull Paint paint,
      @NonNull CharSequence label,
      int width,
      float keyTextSize,
      float minScale) {
    return textWidthCache.getOrMeasure(paint, label, width, keyTextSize, minScale);
  }

  void setPaintForLabelText(Paint paint, float labelTextSize, @NonNull Typeface labelTypeface) {
    paint.setTextSize(labelTextSize);
    paint.setTypeface(labelTypeface);
  }

  void setPaintToKeyText(Paint paint, float keyTextSize, Typeface keyTextStyle) {
    paint.setTextSize(keyTextSize);
    paint.setTypeface(keyTextStyle);
  }
}
