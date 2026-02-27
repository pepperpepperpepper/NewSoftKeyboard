package wtf.uhoh.newsoftkeyboard.app.theme;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class GridDrawable extends Drawable {
  private static final int GRID_SIZE_PX = 64;
  @Nullable private static Bitmap cachedGridBitmap;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final android.graphics.BitmapShader shader;
  private final Matrix shaderMatrix = new Matrix();
  private int blendMode = KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL;
  @Nullable private Integer argb;
  private int alpha = 0xFF;
  private int scalePercent = KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT;

  GridDrawable() {
    paint.setStyle(Paint.Style.FILL);
    paint.setFilterBitmap(false);
    paint.setDither(false);
    shader = createGridShader();
    paint.setShader(shader);
    updateShaderMatrix();
    updateColorFilter();
  }

  void setBlendMode(int blendMode) {
    final int normalized = KeyboardWallpaperOverrideConstants.normalizeBlendMode(blendMode);
    if (this.blendMode == normalized) return;
    this.blendMode = normalized;
    KeyboardWallpaperRenderMath.configureBlendMode(paint, normalized);
    invalidateSelf();
  }

  void setColor(@Nullable Integer argb) {
    if ((this.argb == null && argb == null) || (this.argb != null && this.argb.equals(argb))) {
      return;
    }
    this.argb = argb;
    updateColorFilter();
    invalidateSelf();
  }

  void setScalePercent(int scalePercent) {
    final int clamped = Math.max(25, Math.min(400, scalePercent));
    if (this.scalePercent == clamped) return;
    this.scalePercent = clamped;
    updateShaderMatrix();
    invalidateSelf();
  }

  private void updateColorFilter() {
    if (argb == null) {
      paint.setColorFilter(null);
    } else {
      paint.setColorFilter(
          new android.graphics.PorterDuffColorFilter(argb, PorterDuff.Mode.SRC_IN));
    }
  }

  private void updateShaderMatrix() {
    final float scale = scalePercent / 100f;
    shaderMatrix.reset();
    shaderMatrix.setScale(scale, scale);
    shader.setLocalMatrix(shaderMatrix);
  }

  @Override
  public void draw(@NonNull Canvas canvas) {
    if (argb == null || alpha <= 0) return;
    canvas.drawRect(getBounds(), paint);
  }

  @Override
  public void setAlpha(int alpha) {
    final int clamped = Math.max(0, Math.min(0xFF, alpha));
    if (this.alpha == clamped) return;
    this.alpha = clamped;
    paint.setAlpha(clamped);
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

  @NonNull
  private static android.graphics.BitmapShader createGridShader() {
    final Bitmap bitmap = getGridBitmap();
    return new android.graphics.BitmapShader(
        bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
  }

  @NonNull
  private static Bitmap getGridBitmap() {
    if (cachedGridBitmap != null && !cachedGridBitmap.isRecycled()) return cachedGridBitmap;
    synchronized (GridDrawable.class) {
      if (cachedGridBitmap != null && !cachedGridBitmap.isRecycled()) return cachedGridBitmap;
      cachedGridBitmap = generateGridBitmap();
      return cachedGridBitmap;
    }
  }

  @NonNull
  private static Bitmap generateGridBitmap() {
    final int size = GRID_SIZE_PX;
    final Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    final Canvas canvas = new Canvas(bitmap);
    final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    linePaint.setStyle(Paint.Style.STROKE);
    linePaint.setColor(Color.WHITE);
    linePaint.setStrokeWidth(Math.max(1f, size * 0.05f));
    // Draw only top+left to avoid doubling thickness at tile seams.
    canvas.drawLine(0f, 0f, size, 0f, linePaint);
    canvas.drawLine(0f, 0f, 0f, size, linePaint);
    return bitmap;
  }
}
