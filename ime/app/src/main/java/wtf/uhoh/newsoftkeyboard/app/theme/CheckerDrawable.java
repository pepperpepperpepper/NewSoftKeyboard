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

final class CheckerDrawable extends Drawable {
  private static final int CHECKER_SIZE_PX = 64;
  private static final int CHECKER_SQUARE_STEP_PX = CHECKER_SIZE_PX / 4;
  @Nullable private static Bitmap cachedCheckerBitmap;
  private final Paint paintA = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint paintB = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final android.graphics.BitmapShader shaderA;
  private final android.graphics.BitmapShader shaderB;
  private final Matrix shaderMatrixA = new Matrix();
  private final Matrix shaderMatrixB = new Matrix();
  private int blendMode = KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL;
  @Nullable private Integer argb;
  @Nullable private Integer argb2;
  private int alpha = 0xFF;
  private int scalePercent = KeyboardWallpaperLayer.DEFAULT_SCALE_PERCENT;

  CheckerDrawable() {
    paintA.setStyle(Paint.Style.FILL);
    paintA.setFilterBitmap(false);
    paintA.setDither(false);
    paintB.setStyle(Paint.Style.FILL);
    paintB.setFilterBitmap(false);
    paintB.setDither(false);

    shaderA = createCheckerShader();
    shaderB = createCheckerShader();
    paintA.setShader(shaderA);
    paintB.setShader(shaderB);
    updateShaderMatrix();
    updateColorFilter();
  }

  void setBlendMode(int blendMode) {
    final int normalized = KeyboardWallpaperOverrideConstants.normalizeBlendMode(blendMode);
    if (this.blendMode == normalized) return;
    this.blendMode = normalized;
    KeyboardWallpaperRenderMath.configureBlendMode(paintA, normalized);
    KeyboardWallpaperRenderMath.configureBlendMode(paintB, normalized);
    invalidateSelf();
  }

  void setColors(@Nullable Integer argb, @Nullable Integer argb2) {
    if (java.util.Objects.equals(this.argb, argb) && java.util.Objects.equals(this.argb2, argb2)) {
      return;
    }
    this.argb = argb;
    this.argb2 = argb2;
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
      paintA.setColorFilter(null);
    } else {
      paintA.setColorFilter(
          new android.graphics.PorterDuffColorFilter(argb, PorterDuff.Mode.SRC_IN));
    }
    if (argb2 == null) {
      paintB.setColorFilter(null);
    } else {
      paintB.setColorFilter(
          new android.graphics.PorterDuffColorFilter(argb2, PorterDuff.Mode.SRC_IN));
    }
  }

  private void updateShaderMatrix() {
    final float scale = scalePercent / 100f;
    shaderMatrixA.reset();
    shaderMatrixA.setScale(scale, scale);
    shaderA.setLocalMatrix(shaderMatrixA);

    shaderMatrixB.reset();
    shaderMatrixB.setScale(scale, scale);
    shaderMatrixB.postTranslate(CHECKER_SQUARE_STEP_PX, 0f);
    shaderB.setLocalMatrix(shaderMatrixB);
  }

  @Override
  public void draw(@NonNull Canvas canvas) {
    if (alpha <= 0) return;
    if (argb != null) {
      canvas.drawRect(getBounds(), paintA);
    }
    if (argb2 != null) {
      canvas.drawRect(getBounds(), paintB);
    }
  }

  @Override
  public void setAlpha(int alpha) {
    final int clamped = Math.max(0, Math.min(0xFF, alpha));
    if (this.alpha == clamped) return;
    this.alpha = clamped;
    paintA.setAlpha(clamped);
    paintB.setAlpha(clamped);
    invalidateSelf();
  }

  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {
    paintA.setColorFilter(colorFilter);
    paintB.setColorFilter(colorFilter);
    invalidateSelf();
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  @NonNull
  private static android.graphics.BitmapShader createCheckerShader() {
    final Bitmap bitmap = getCheckerBitmap();
    return new android.graphics.BitmapShader(
        bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
  }

  @NonNull
  private static Bitmap getCheckerBitmap() {
    if (cachedCheckerBitmap != null && !cachedCheckerBitmap.isRecycled())
      return cachedCheckerBitmap;
    synchronized (CheckerDrawable.class) {
      if (cachedCheckerBitmap != null && !cachedCheckerBitmap.isRecycled())
        return cachedCheckerBitmap;
      cachedCheckerBitmap = generateCheckerBitmap();
      return cachedCheckerBitmap;
    }
  }

  @NonNull
  private static Bitmap generateCheckerBitmap() {
    final int size = CHECKER_SIZE_PX;
    final Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    final Canvas canvas = new Canvas(bitmap);
    final Paint squarePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    squarePaint.setStyle(Paint.Style.FILL);
    squarePaint.setColor(Color.WHITE);

    final int squares = 4;
    final float step = size / (float) squares;
    for (int y = 0; y < squares; y++) {
      for (int x = 0; x < squares; x++) {
        if (((x + y) & 1) != 0) continue;
        final float left = x * step;
        final float top = y * step;
        canvas.drawRect(left, top, left + step, top + step, squarePaint);
      }
    }
    return bitmap;
  }
}
