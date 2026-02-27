package wtf.uhoh.newsoftkeyboard.app.theme;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class GradientOverlayDrawable extends Drawable {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF rect = new RectF();
  private int blendMode = KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL;
  private int strengthPercent = 0;
  private int direction = KeyboardWallpaperLayer.DIRECTION_VERTICAL;
  @Nullable private Integer startArgb;
  @Nullable private Integer endArgb;
  @Nullable private java.util.List<KeyboardWallpaperLayer.GradientStop> stops;
  @Nullable private LinearGradient shader;

  GradientOverlayDrawable() {
    paint.setStyle(Paint.Style.FILL);
  }

  void setBlendMode(int blendMode) {
    final int normalized = KeyboardWallpaperOverrideConstants.normalizeBlendMode(blendMode);
    if (this.blendMode == normalized) return;
    this.blendMode = normalized;
    KeyboardWallpaperRenderMath.configureBlendMode(paint, normalized);
    invalidateSelf();
  }

  void setDirection(int direction) {
    final int normalized =
        switch (direction) {
          case KeyboardWallpaperLayer.DIRECTION_HORIZONTAL,
              KeyboardWallpaperLayer.DIRECTION_HORIZONTAL_REVERSE,
              KeyboardWallpaperLayer.DIRECTION_VERTICAL_REVERSE ->
              direction;
          case KeyboardWallpaperLayer.DIRECTION_VERTICAL ->
              KeyboardWallpaperLayer.DIRECTION_VERTICAL;
          default -> KeyboardWallpaperLayer.DIRECTION_VERTICAL;
        };
    if (this.direction == normalized) return;
    this.direction = normalized;
    updateShader();
    invalidateSelf();
  }

  void setStops(@Nullable java.util.List<KeyboardWallpaperLayer.GradientStop> stops) {
    final java.util.List<KeyboardWallpaperLayer.GradientStop> normalized =
        normalizeStopsOrNull(stops);
    if (java.util.Objects.equals(this.stops, normalized)) return;
    this.stops = normalized;
    updateShader();
    invalidateSelf();
  }

  void setColors(@Nullable Integer startArgb, @Nullable Integer endArgb) {
    if ((this.startArgb == null && startArgb == null)
        || (this.startArgb != null && this.startArgb.equals(startArgb))) {
      if ((this.endArgb == null && endArgb == null)
          || (this.endArgb != null && this.endArgb.equals(endArgb))) {
        return;
      }
    }
    this.startArgb = startArgb;
    this.endArgb = endArgb;
    if (this.stops != null) return;
    updateShader();
    invalidateSelf();
  }

  void setStrengthPercent(int strengthPercent) {
    final int clamped = KeyboardWallpaperRenderMath.clampPercent(strengthPercent);
    if (this.strengthPercent == clamped) return;
    this.strengthPercent = clamped;
    updateShader();
    invalidateSelf();
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);
    rect.set(bounds);
    updateShader();
  }

  private void updateShader() {
    if (strengthPercent <= 0) {
      shader = null;
      paint.setShader(null);
      return;
    }

    final Rect bounds = getBounds();
    final int w = bounds.width();
    final int h = bounds.height();
    if (w <= 0 || h <= 0) {
      shader = null;
      paint.setShader(null);
      return;
    }

    final int endAlpha = KeyboardWallpaperRenderMath.alphaForPercent(strengthPercent);
    final float x0;
    final float y0;
    final float x1;
    final float y1;
    switch (direction) {
      case KeyboardWallpaperLayer.DIRECTION_HORIZONTAL:
        x0 = bounds.left;
        y0 = bounds.top;
        x1 = bounds.right;
        y1 = bounds.top;
        break;
      case KeyboardWallpaperLayer.DIRECTION_HORIZONTAL_REVERSE:
        x0 = bounds.right;
        y0 = bounds.top;
        x1 = bounds.left;
        y1 = bounds.top;
        break;
      case KeyboardWallpaperLayer.DIRECTION_VERTICAL_REVERSE:
        x0 = bounds.left;
        y0 = bounds.bottom;
        x1 = bounds.left;
        y1 = bounds.top;
        break;
      case KeyboardWallpaperLayer.DIRECTION_VERTICAL:
      default:
        x0 = bounds.left;
        y0 = bounds.top;
        x1 = bounds.left;
        y1 = bounds.bottom;
        break;
    }

    final java.util.List<KeyboardWallpaperLayer.GradientStop> localStops = stops;
    if (localStops != null && localStops.size() >= 2) {
      final int count = localStops.size();
      final int[] colors = new int[count];
      final float[] positions = new float[count];
      for (int i = 0; i < count; i++) {
        final KeyboardWallpaperLayer.GradientStop stop = localStops.get(i);
        if (stop == null) {
          colors[i] = Color.TRANSPARENT;
          positions[i] = 0f;
          continue;
        }
        final int rawColor = stop.argb();
        final int baseAlpha = (rawColor >>> 24) & 0xFF;
        final int effectiveAlpha = (endAlpha * baseAlpha) / 255;
        colors[i] = (rawColor & 0x00FFFFFF) | (effectiveAlpha << 24);
        positions[i] = Math.max(0f, Math.min(1f, stop.positionPercent() / 100f));
      }
      shader = new LinearGradient(x0, y0, x1, y1, colors, positions, Shader.TileMode.CLAMP);
    } else {
      final int rawEndColor = endArgb != null ? endArgb : Color.BLACK;
      final int rawStartColor =
          startArgb != null ? startArgb : (rawEndColor & 0x00FFFFFF); // transparent
      final int startBaseAlpha = (rawStartColor >>> 24) & 0xFF;
      final int endBaseAlpha = (rawEndColor >>> 24) & 0xFF;
      final int startAlpha = (endAlpha * startBaseAlpha) / 255;
      final int endAlphaEffective = (endAlpha * endBaseAlpha) / 255;
      final int startColor = (rawStartColor & 0x00FFFFFF) | (startAlpha << 24);
      final int endColor = (rawEndColor & 0x00FFFFFF) | (endAlphaEffective << 24);
      shader =
          new LinearGradient(
              x0,
              y0,
              x1,
              y1,
              new int[] {startColor, endColor},
              new float[] {0f, 1f},
              Shader.TileMode.CLAMP);
    }
    paint.setShader(shader);
  }

  @Nullable
  private static java.util.List<KeyboardWallpaperLayer.GradientStop> normalizeStopsOrNull(
      @Nullable java.util.List<KeyboardWallpaperLayer.GradientStop> input) {
    if (input == null || input.size() < 2) return null;
    final java.util.ArrayList<KeyboardWallpaperLayer.GradientStop> out =
        new java.util.ArrayList<>();
    for (KeyboardWallpaperLayer.GradientStop stop : input) {
      if (stop != null) out.add(stop);
    }
    if (out.size() < 2) return null;
    out.sort(
        java.util.Comparator.comparingInt(KeyboardWallpaperLayer.GradientStop::positionPercent));
    return java.util.List.copyOf(out);
  }

  @Override
  public void draw(@NonNull Canvas canvas) {
    if (strengthPercent <= 0 || shader == null) return;
    canvas.drawRect(rect, paint);
  }

  @Override
  public void setAlpha(int alpha) {
    // no-op: strength controls alpha
  }

  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {
    paint.setColorFilter(colorFilter);
    invalidateSelf();
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }
}
