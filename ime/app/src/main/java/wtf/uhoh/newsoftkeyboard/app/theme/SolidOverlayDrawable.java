package wtf.uhoh.newsoftkeyboard.app.theme;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class SolidOverlayDrawable extends Drawable {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private int color;
  private int alpha = 0xFF;
  private int blendMode = KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL;

  SolidOverlayDrawable(int initialColor) {
    color = initialColor;
    paint.setStyle(Paint.Style.FILL);
    updatePaintColor();
  }

  void setColor(int argbColor) {
    if (color == argbColor) return;
    color = argbColor;
    updatePaintColor();
    invalidateSelf();
  }

  void setBlendMode(int blendMode) {
    final int normalized = KeyboardWallpaperOverrideConstants.normalizeBlendMode(blendMode);
    if (this.blendMode == normalized) return;
    this.blendMode = normalized;
    KeyboardWallpaperRenderMath.configureBlendMode(paint, normalized);
    invalidateSelf();
  }

  private void updatePaintColor() {
    final int baseAlpha = (color >>> 24) & 0xFF;
    final int effectiveAlpha = (baseAlpha * alpha) / 255;
    paint.setColor((color & 0x00FFFFFF) | (effectiveAlpha << 24));
  }

  @Override
  public void draw(@NonNull Canvas canvas) {
    if (((paint.getColor() >>> 24) & 0xFF) == 0) return;
    canvas.drawRect(getBounds(), paint);
  }

  @Override
  public void setAlpha(int alpha) {
    final int clamped = Math.max(0, Math.min(0xFF, alpha));
    if (this.alpha == clamped) return;
    this.alpha = clamped;
    updatePaintColor();
    invalidateSelf();
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
