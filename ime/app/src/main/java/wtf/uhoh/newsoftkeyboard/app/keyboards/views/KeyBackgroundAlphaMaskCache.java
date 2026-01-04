package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Trace;
import android.util.SparseArray;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.WeakHashMap;

/**
 * Caches alpha masks for key background drawables so key-face photo overlays can optionally match
 * arbitrary theme silhouettes (including bitmap/9-patch key backgrounds).
 *
 * <p>Designed for the draw loop: no allocations on cache hits.
 */
final class KeyBackgroundAlphaMaskCache {

  private static final int MAX_MASKS_PER_DRAWABLE = 12;
  private static final PorterDuffXfermode DST_IN = new PorterDuffXfermode(PorterDuff.Mode.DST_IN);

  private static final WeakHashMap<Object, SparseArray<Bitmap>> sAlphaMasksByDrawable =
      new WeakHashMap<>();

  private static final ThreadLocal<Paint> sDstInPaint =
      ThreadLocal.withInitial(
          () -> {
            final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setFilterBitmap(true);
            paint.setXfermode(DST_IN);
            return paint;
          });

  private static final ThreadLocal<Rect> sOldBounds = ThreadLocal.withInitial(Rect::new);

  private KeyBackgroundAlphaMaskCache() {}

  @NonNull
  static Paint dstInPaint() {
    return sDstInPaint.get();
  }

  @Nullable
  static Bitmap resolveAlphaMask(@NonNull Drawable keyBackground, int width, int height) {
    if (width <= 0 || height <= 0) return null;

    final Object drawableKey =
        keyBackground.getConstantState() != null ? keyBackground.getConstantState() : keyBackground;
    final int sizeKey = packSize(width, height);

    final Bitmap cached = getFromCache(drawableKey, sizeKey, width, height);
    if (cached != null) return cached;

    final Bitmap created = createAlphaMask(keyBackground, width, height);
    if (created == null) return null;
    putInCache(drawableKey, sizeKey, created);
    return created;
  }

  private static int packSize(int width, int height) {
    return ((width & 0xFFFF) << 16) | (height & 0xFFFF);
  }

  @Nullable
  private static Bitmap getFromCache(Object drawableKey, int sizeKey, int width, int height) {
    synchronized (sAlphaMasksByDrawable) {
      final SparseArray<Bitmap> bySize = sAlphaMasksByDrawable.get(drawableKey);
      if (bySize == null) return null;

      final Bitmap cached = bySize.get(sizeKey);
      if (cached == null) return null;
      if (cached.isRecycled()) {
        bySize.remove(sizeKey);
        return null;
      }
      if (cached.getWidth() != width || cached.getHeight() != height) {
        bySize.remove(sizeKey);
        return null;
      }
      return cached;
    }
  }

  private static void putInCache(Object drawableKey, int sizeKey, @NonNull Bitmap created) {
    synchronized (sAlphaMasksByDrawable) {
      SparseArray<Bitmap> bySize = sAlphaMasksByDrawable.get(drawableKey);
      if (bySize == null) {
        bySize = new SparseArray<>();
        sAlphaMasksByDrawable.put(drawableKey, bySize);
      }

      if (bySize.size() >= MAX_MASKS_PER_DRAWABLE) {
        bySize.removeAt(0);
      }
      bySize.put(sizeKey, created);
    }
  }

  @Nullable
  private static Bitmap createAlphaMask(@NonNull Drawable drawable, int width, int height) {
    Trace.beginSection("NSK.KeyAlphaMaskCreate");
    try {
      final Bitmap argb;
      try {
        argb = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
      } catch (OutOfMemoryError oom) {
        return null;
      }

      final Rect oldBounds = sOldBounds.get();
      oldBounds.set(drawable.getBounds());

      final Canvas canvas = new Canvas(argb);
      drawable.setBounds(0, 0, width, height);
      try {
        drawable.draw(canvas);
      } catch (RuntimeException ignored) {
        drawable.setBounds(oldBounds);
        argb.recycle();
        return null;
      } finally {
        drawable.setBounds(oldBounds);
      }

      final Bitmap alpha;
      try {
        alpha = argb.extractAlpha();
      } catch (RuntimeException e) {
        argb.recycle();
        return null;
      }
      argb.recycle();
      return alpha;
    } finally {
      Trace.endSection();
    }
  }
}
