package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.Paint;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Small cache for measured label widths keyed by text and key width. */
final class TextWidthCache {

  private final Map<Key, Value> cache = new ConcurrentHashMap<>();

  float getOrMeasure(
      @NonNull Paint paint,
      @NonNull CharSequence label,
      int width,
      float baseSize,
      float minScale) {
    if (label.length() == 0) {
      return 0f;
    }
    if (baseSize <= 0f) {
      paint.setTextSize(0f);
      return 0f;
    }

    final float effectiveMinScale =
        Float.isNaN(minScale) || Float.isInfinite(minScale)
            ? 0.3f
            : Math.max(0.1f, Math.min(1f, minScale));
    final int clampedWidth = Math.max(1, width);
    final int baseSizeKey = Math.round(baseSize * 100f);
    final int minScaleKey = Math.round(effectiveMinScale * 1000f);
    final int typefaceKey = paint.getTypeface() != null ? paint.getTypeface().hashCode() : 0;
    Key key = new Key(label, clampedWidth, baseSizeKey, minScaleKey, typefaceKey);
    Value cached = cache.get(key);
    if (cached != null) {
      return cached.applyTo(paint);
    }

    float bestSize = baseSize;
    paint.setTextSize(bestSize);
    float bestWidth = paint.measureText(label, 0, label.length());
    if (bestWidth > clampedWidth) {
      final float minSize = Math.min(baseSize, Math.max(1f, baseSize * effectiveMinScale));
      float low = minSize;
      float high = baseSize;
      bestSize = minSize;
      paint.setTextSize(bestSize);
      bestWidth = paint.measureText(label, 0, label.length());

      for (int i = 0; i < 8; i++) {
        final float mid = (low + high) / 2f;
        paint.setTextSize(mid);
        final float measured = paint.measureText(label, 0, label.length());
        if (measured <= clampedWidth) {
          bestSize = mid;
          bestWidth = measured;
          low = mid;
        } else {
          high = mid;
        }
      }

      paint.setTextSize(bestSize);
    }

    cache.put(key, new Value(bestSize, bestWidth));
    return bestWidth;
  }

  void clear() {
    cache.clear();
  }

  private record Key(
      CharSequence label, int width, int baseSizeKey, int minScaleKey, int typefaceKey) {
    @Override
    public int hashCode() {
      int result = label.hashCode();
      result = (result * 31) + width;
      result = (result * 31) + baseSizeKey;
      result = (result * 31) + minScaleKey;
      result = (result * 31) + typefaceKey;
      return result;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof Key other
          && other.width == width
          && other.baseSizeKey == baseSizeKey
          && other.minScaleKey == minScaleKey
          && other.typefaceKey == typefaceKey
          && TextUtils.equals(other.label, label);
    }
  }

  private record Value(float textSize, float textWidth) {
    float applyTo(Paint paint) {
      paint.setTextSize(textSize);
      return textWidth;
    }
  }
}
