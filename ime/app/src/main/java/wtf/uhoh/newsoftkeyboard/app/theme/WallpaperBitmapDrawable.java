package wtf.uhoh.newsoftkeyboard.app.theme;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class WallpaperBitmapDrawable extends Drawable {
  private final Bitmap bitmap;
  private final Paint paint;
  private android.graphics.BitmapShader bitmapShader;
  private final Matrix shaderMatrix = new Matrix();
  private int alpha = 0xFF;
  private int rotationDegrees = 0;
  private int scaleMode = KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_CROP;
  private int anchor = KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_CENTER;

  WallpaperBitmapDrawable(@NonNull Bitmap bitmap) {
    this.bitmap = bitmap;
    this.paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    this.bitmapShader = createShader(bitmap, scaleMode);
    this.paint.setShader(this.bitmapShader);
  }

  @NonNull
  Bitmap getBitmap() {
    return bitmap;
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);
    updateShaderMatrix(bounds);
  }

  void setTransform(int rotationDegrees, int scaleMode, int anchor) {
    final int normalizedRotation =
        KeyboardWallpaperOverrideConstants.normalizeRotationDegrees(rotationDegrees);
    final int normalizedScaleMode = normalizeScaleMode(scaleMode);
    final int normalizedAnchor = normalizeAnchor(anchor);

    boolean changed = false;
    if (this.rotationDegrees != normalizedRotation) {
      this.rotationDegrees = normalizedRotation;
      changed = true;
    }
    if (this.scaleMode != normalizedScaleMode) {
      this.scaleMode = normalizedScaleMode;
      this.bitmapShader = createShader(bitmap, this.scaleMode);
      this.paint.setShader(this.bitmapShader);
      changed = true;
    }
    if (this.anchor != normalizedAnchor) {
      this.anchor = normalizedAnchor;
      changed = true;
    }
    if (changed) updateShaderMatrix(getBounds());
  }

  private void updateShaderMatrix(@NonNull Rect bounds) {
    KeyboardWallpaperTransform.updateShaderMatrix(
        shaderMatrix,
        bitmap.getWidth(),
        bitmap.getHeight(),
        bounds,
        rotationDegrees,
        scaleMode,
        anchor);
    bitmapShader.setLocalMatrix(shaderMatrix);
    invalidateSelf();
  }

  @Override
  public void draw(@NonNull Canvas canvas) {
    paint.setAlpha(alpha);
    canvas.drawRect(getBounds(), paint);
  }

  @Override
  public void setAlpha(int alpha) {
    this.alpha = alpha;
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

  private static android.graphics.BitmapShader createShader(@NonNull Bitmap bitmap, int scaleMode) {
    final Shader.TileMode tileMode = KeyboardWallpaperTransform.tileModeForScaleMode(scaleMode);
    return new android.graphics.BitmapShader(bitmap, tileMode, tileMode);
  }

  private static int normalizeScaleMode(int scaleMode) {
    switch (scaleMode) {
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_CROP:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_FIT:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_STRETCH:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_TILE:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_MIRROR:
        return scaleMode;
      default:
        return KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_CROP;
    }
  }

  private static int normalizeAnchor(int anchor) {
    return anchor >= KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_TOP_LEFT
            && anchor <= KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_BOTTOM_RIGHT
        ? anchor
        : KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_CENTER;
  }
}
