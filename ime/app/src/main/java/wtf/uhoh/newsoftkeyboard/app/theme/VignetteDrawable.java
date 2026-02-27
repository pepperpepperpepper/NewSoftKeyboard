package wtf.uhoh.newsoftkeyboard.app.theme;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class VignetteDrawable extends Drawable {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF rect = new RectF();
  private int blendMode = KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL;
  private int strengthPercent = 0;
  @Nullable private Integer argb;
  @Nullable private RadialGradient shader;

  VignetteDrawable() {
    paint.setStyle(Paint.Style.FILL);
  }

  void setBlendMode(int blendMode) {
    final int normalized = KeyboardWallpaperOverrideConstants.normalizeBlendMode(blendMode);
    if (this.blendMode == normalized) return;
    this.blendMode = normalized;
    KeyboardWallpaperRenderMath.configureBlendMode(paint, normalized);
    invalidateSelf();
  }

  void setStrengthPercent(int strengthPercent) {
    final int clamped = KeyboardWallpaperRenderMath.clampPercent(strengthPercent);
    if (this.strengthPercent == clamped) return;
    this.strengthPercent = clamped;
    updateShader();
    invalidateSelf();
  }

  void setColor(@Nullable Integer argb) {
    if ((this.argb == null && argb == null) || (this.argb != null && this.argb.equals(argb))) {
      return;
    }
    this.argb = argb;
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

    final float cx = bounds.exactCenterX();
    final float cy = bounds.exactCenterY();
    final float radius = (float) Math.hypot(w, h) / 2f;
    final int edgeAlpha = KeyboardWallpaperRenderMath.alphaForPercent(strengthPercent);
    final int baseColor = argb != null ? argb : Color.BLACK;
    final int baseAlpha = (baseColor >>> 24) & 0xFF;
    final int effectiveAlpha = (edgeAlpha * baseAlpha) / 255;
    final int edgeColor = (baseColor & 0x00FFFFFF) | (effectiveAlpha << 24);
    final int centerColor = (baseColor & 0x00FFFFFF);

    shader =
        new RadialGradient(
            cx,
            cy,
            radius,
            new int[] {centerColor, edgeColor},
            new float[] {0f, 1f},
            Shader.TileMode.CLAMP);
    paint.setShader(shader);
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
