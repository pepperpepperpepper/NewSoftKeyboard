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
import java.io.File;

final class BlurPhotoDrawable extends Drawable {
  private final Paint paint;
  private final Matrix shaderMatrix = new Matrix();
  @Nullable private Bitmap bitmap;
  @Nullable private android.graphics.BitmapShader bitmapShader;
  private int alpha = 0xFF;
  private int blendMode = KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL;
  private int rotationDegrees = 0;
  private int scaleMode = KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_CROP;
  private int anchor = KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_CENTER;

  @Nullable private String sourcePath;
  private long sourceLastModified = 0L;
  private int sourceBlurBucketPx = 0;

  BlurPhotoDrawable() {
    this.paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    KeyboardWallpaperRenderMath.configureBlendMode(paint, blendMode);
    paint.setStyle(Paint.Style.FILL);
  }

  void setBlendMode(int blendMode) {
    final int normalized = KeyboardWallpaperOverrideConstants.normalizeBlendMode(blendMode);
    if (this.blendMode == normalized) return;
    this.blendMode = normalized;
    KeyboardWallpaperRenderMath.configureBlendMode(paint, normalized);
    invalidateSelf();
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
      if (bitmap != null) {
        this.bitmapShader = createShader(bitmap, this.scaleMode);
        this.paint.setShader(this.bitmapShader);
      } else {
        this.paint.setShader(null);
        this.bitmapShader = null;
      }
      changed = true;
    }
    if (this.anchor != normalizedAnchor) {
      this.anchor = normalizedAnchor;
      changed = true;
    }
    if (changed) updateShaderMatrix(getBounds());
  }

  void setSource(
      @NonNull WallpaperBitmapLoader loader,
      @NonNull File file,
      long lastModified,
      int baseBucketPx,
      int blurScalePercent) {
    final int downscaleFactor =
        KeyboardWallpaperRenderMath.blurDownscaleFactorForScalePercent(blurScalePercent);
    final int blurBucketPx = Math.max(256, Math.max(1, baseBucketPx) / downscaleFactor);
    final String path = file.getAbsolutePath();
    if (path.equals(sourcePath)
        && sourceLastModified == lastModified
        && sourceBlurBucketPx == blurBucketPx) {
      return;
    }

    sourcePath = path;
    sourceLastModified = lastModified;
    sourceBlurBucketPx = blurBucketPx;

    final Bitmap cached = loader.getCached(file, lastModified, blurBucketPx);
    if (cached != null && !cached.isRecycled()) {
      setBitmap(cached);
      return;
    }

    setBitmap(null);
    loader.loadAsync(
        file,
        lastModified,
        blurBucketPx,
        decoded -> {
          if (decoded == null || decoded.isRecycled()) return;
          if (!path.equals(sourcePath)
              || sourceLastModified != lastModified
              || sourceBlurBucketPx != blurBucketPx) {
            return;
          }
          setBitmap(decoded);
        });
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);
    updateShaderMatrix(bounds);
  }

  private void setBitmap(@Nullable Bitmap bitmap) {
    if (this.bitmap == bitmap && (bitmap == null || bitmapShader != null)) return;
    this.bitmap = bitmap;
    if (bitmap == null) {
      bitmapShader = null;
      paint.setShader(null);
      invalidateSelf();
      return;
    }

    bitmapShader = createShader(bitmap, scaleMode);
    paint.setShader(bitmapShader);
    updateShaderMatrix(getBounds());
  }

  private void updateShaderMatrix(@NonNull Rect bounds) {
    if (bitmap == null || bitmapShader == null) return;
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
    if (bitmapShader == null) return;
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
