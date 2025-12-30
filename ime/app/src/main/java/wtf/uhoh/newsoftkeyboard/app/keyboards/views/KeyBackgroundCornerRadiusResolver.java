package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RotateDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Build;
import androidx.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.WeakHashMap;

/** Best-effort resolver for matching key-face overlays to the active theme key background shape. */
final class KeyBackgroundCornerRadiusResolver {

  private static final ThreadLocal<Outline> sOutlineScratch = ThreadLocal.withInitial(Outline::new);
  private static final ThreadLocal<Rect> sRectScratch = ThreadLocal.withInitial(Rect::new);
  private static final WeakHashMap<Drawable, Float> sCornerRadiusCache = new WeakHashMap<>();
  private static final float CACHE_NO_VALUE = Float.NaN;
  @Nullable private static final Method sOutlineGetRect;
  @Nullable private static final Method sOutlineGetRadius;

  static {
    sOutlineGetRect = tryResolveOutlineGetRectMethod();
    sOutlineGetRadius = tryResolveOutlineGetRadiusMethod();
  }

  private KeyBackgroundCornerRadiusResolver() {}

  static float resolveCornerRadiusOrFallback(Drawable keyBackground, int keyWidth, int keyHeight) {
    final Float resolved = tryResolveCornerRadiusCached(keyBackground);
    if (resolved != null) return Math.max(0f, resolved);

    // Historical behavior: an approximation that works reasonably well across themes.
    return Math.min(keyWidth, keyHeight) * 0.12f;
  }

  @Nullable
  private static Float tryResolveCornerRadiusCached(Drawable keyBackground) {
    Drawable current = keyBackground.getCurrent();
    synchronized (sCornerRadiusCache) {
      final Float cached = sCornerRadiusCache.get(current);
      if (cached != null) return cached.isNaN() ? null : cached;
    }

    final Float resolved = tryResolveCornerRadius(current);
    synchronized (sCornerRadiusCache) {
      sCornerRadiusCache.put(current, resolved == null ? CACHE_NO_VALUE : resolved);
    }

    return resolved;
  }

  @Nullable
  private static Float tryResolveCornerRadius(Drawable keyBackground) {
    Drawable current = keyBackground.getCurrent();
    for (int unwraps = 0; unwraps < 8; unwraps++) {
      if (current instanceof InsetDrawable inset) {
        current = inset.getDrawable();
        continue;
      }

      if (current instanceof ClipDrawable clip) {
        current = clip.getDrawable();
        continue;
      }

      if (current instanceof ScaleDrawable scale) {
        current = scale.getDrawable();
        continue;
      }

      if (current instanceof RotateDrawable rotate) {
        current = rotate.getDrawable();
        continue;
      }

      if (current instanceof LayerDrawable layer) {
        final int count = layer.getNumberOfLayers();
        Float found = null;
        for (int i = 0; i < count; i++) {
          final Drawable layerDrawable = layer.getDrawable(i);
          final Float layerRadius = tryResolveCornerRadius(layerDrawable);
          if (layerRadius != null) found = layerRadius;
        }
        if (found != null) return found;
        break;
      }

      break;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      final Float outlineRadius = tryResolveCornerRadiusFromOutline(current);
      if (outlineRadius != null) return outlineRadius;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && current instanceof GradientDrawable gd) {
      return gd.getCornerRadius();
    }

    return null;
  }

  @Nullable
  private static Float tryResolveCornerRadiusFromOutline(Drawable drawable) {
    if (sOutlineGetRect == null || sOutlineGetRadius == null) return null;

    final Outline outline = sOutlineScratch.get();
    outline.setEmpty();
    drawable.getOutline(outline);

    final Rect rect = sRectScratch.get();
    final boolean hasRect;
    try {
      hasRect = (boolean) sOutlineGetRect.invoke(outline, rect);
    } catch (IllegalAccessException | InvocationTargetException e) {
      return null;
    }
    if (!hasRect || rect.isEmpty()) return null;

    final float radius;
    try {
      radius = ((Number) sOutlineGetRadius.invoke(outline)).floatValue();
    } catch (IllegalAccessException | InvocationTargetException e) {
      return null;
    }

    return radius;
  }

  @Nullable
  private static Method tryResolveOutlineGetRectMethod() {
    try {
      return Outline.class.getMethod("getRect", Rect.class);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  @Nullable
  private static Method tryResolveOutlineGetRadiusMethod() {
    try {
      return Outline.class.getMethod("getRadius");
    } catch (NoSuchMethodException e) {
      return null;
    }
  }
}
