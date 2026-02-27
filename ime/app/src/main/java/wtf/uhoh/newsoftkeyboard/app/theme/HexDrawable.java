package wtf.uhoh.newsoftkeyboard.app.theme;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class HexDrawable extends Drawable {
  private static final int HEX_SIZE_PX = 64;
  @Nullable private static Bitmap cachedHexBitmap;

  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final android.graphics.BitmapShader shader;
  private final Matrix shaderMatrix = new Matrix();
  private int blendMode = KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL;
  @Nullable private Integer argb;
  private int alpha = 0xFF;
  private int scalePercent = KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT;

  HexDrawable() {
    paint.setStyle(Paint.Style.FILL);
    paint.setFilterBitmap(false);
    paint.setDither(false);
    shader = createHexShader();
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
  private static android.graphics.BitmapShader createHexShader() {
    final Bitmap bitmap = getHexBitmap();
    return new android.graphics.BitmapShader(
        bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
  }

  @NonNull
  private static Bitmap getHexBitmap() {
    if (cachedHexBitmap != null && !cachedHexBitmap.isRecycled()) return cachedHexBitmap;
    synchronized (HexDrawable.class) {
      if (cachedHexBitmap != null && !cachedHexBitmap.isRecycled()) return cachedHexBitmap;
      cachedHexBitmap = generateHexBitmap();
      return cachedHexBitmap;
    }
  }

  @NonNull
  private static Bitmap generateHexBitmap() {
    final int size = HEX_SIZE_PX;
    final Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    final Canvas canvas = new Canvas(bitmap);
    final Paint hexPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    hexPaint.setStyle(Paint.Style.STROKE);
    hexPaint.setColor(Color.WHITE);
    hexPaint.setStrokeWidth(Math.max(1f, size * 0.05f));
    hexPaint.setStrokeJoin(Paint.Join.ROUND);
    hexPaint.setStrokeCap(Paint.Cap.ROUND);

    final float r = size / 7f;
    final float dx = (float) (Math.sqrt(3f) * r);
    final float dy = 1.5f * r;
    final float xOffset = r;
    final float yOffset = r;

    final int cols = (int) Math.ceil((size + r) / dx) + 2;
    final int rows = (int) Math.ceil((size + r) / dy) + 2;
    for (int col = -1; col <= cols; col++) {
      final float cx = xOffset + col * dx;
      final float colOffsetY = ((col & 1) == 0) ? 0f : (dy / 2f);
      for (int row = -1; row <= rows; row++) {
        final float cy = yOffset + row * dy + colOffsetY;
        final Path path = new Path();
        for (int i = 0; i < 6; i++) {
          final double angle = (Math.PI / 3d) * i + (Math.PI / 6d);
          final float vx = cx + (float) (r * Math.cos(angle));
          final float vy = cy + (float) (r * Math.sin(angle));
          if (i == 0) {
            path.moveTo(vx, vy);
          } else {
            path.lineTo(vx, vy);
          }
        }
        path.close();
        canvas.drawPath(path, hexPaint);
      }
    }
    return bitmap;
  }
}
