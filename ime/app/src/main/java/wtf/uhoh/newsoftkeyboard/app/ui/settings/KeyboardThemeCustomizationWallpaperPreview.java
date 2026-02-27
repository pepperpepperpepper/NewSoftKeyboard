package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperTransform;

final class KeyboardThemeCustomizationWallpaperPreview {

  private KeyboardThemeCustomizationWallpaperPreview() {}

  @Nullable
  static android.graphics.drawable.Drawable createDrawable(
      @NonNull File file,
      int targetSizePx,
      int dimPercent,
      int rotationDegrees,
      int scaleMode,
      int anchor,
      int saturationPercent,
      int contrastPercent,
      int brightnessPercent,
      int temperaturePercent) {
    final android.graphics.Bitmap bitmap = decodeThumbnail(file, targetSizePx);
    if (bitmap == null) return null;

    final android.graphics.drawable.Drawable baseDrawable =
        new WallpaperPreviewDrawable(bitmap, rotationDegrees, scaleMode, anchor);
    baseDrawable.setColorFilter(
        createPhotoColorFilter(
            saturationPercent, contrastPercent, brightnessPercent, temperaturePercent));

    if (dimPercent <= 0) {
      return baseDrawable;
    }

    final android.graphics.drawable.ColorDrawable dim =
        new android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK);
    dim.setAlpha(Math.round(255f * (Math.max(0, Math.min(100, dimPercent)) / 100f)));
    return new android.graphics.drawable.LayerDrawable(
        new android.graphics.drawable.Drawable[] {baseDrawable, dim});
  }

  @Nullable
  private static android.graphics.ColorFilter createPhotoColorFilter(
      int saturationPercent, int contrastPercent, int brightnessPercent, int temperaturePercent) {
    final int clampedSaturation = clampPercent0To200(saturationPercent);
    final int clampedContrast = clampPercent0To200(contrastPercent);
    final int clampedBrightness = clampPercent0To200(brightnessPercent);
    final int clampedTemperature = clampPercent0To200(temperaturePercent);
    if (clampedSaturation == 100
        && clampedContrast == 100
        && clampedBrightness == 100
        && clampedTemperature == 100) {
      return null;
    }

    final android.graphics.ColorMatrix combined = new android.graphics.ColorMatrix();
    boolean hasMatrix = false;

    if (clampedContrast != 100) {
      final float factor = clampedContrast / 100f;
      final float translate = 128f * (1f - factor);
      combined.set(
          new float[] {
            factor, 0f, 0f, 0f, translate, 0f, factor, 0f, 0f, translate, 0f, 0f, factor, 0f,
            translate, 0f, 0f, 0f, 1f, 0f
          });
      hasMatrix = true;
    }

    if (clampedBrightness != 100) {
      final float translate = ((clampedBrightness - 100) / 100f) * 128f;
      final android.graphics.ColorMatrix brightness =
          new android.graphics.ColorMatrix(
              new float[] {
                1f, 0f, 0f, 0f, translate,
                0f, 1f, 0f, 0f, translate,
                0f, 0f, 1f, 0f, translate,
                0f, 0f, 0f, 1f, 0f
              });
      if (hasMatrix) {
        combined.postConcat(brightness);
      } else {
        combined.set(brightness);
        hasMatrix = true;
      }
    }

    if (clampedTemperature != 100) {
      final float t = (clampedTemperature - 100) / 100f;
      final float offset = t * 40f;
      final android.graphics.ColorMatrix temperature =
          new android.graphics.ColorMatrix(
              new float[] {
                1f, 0f, 0f, 0f, offset, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, -offset, 0f, 0f, 0f, 1f,
                0f
              });
      if (hasMatrix) {
        combined.postConcat(temperature);
      } else {
        combined.set(temperature);
        hasMatrix = true;
      }
    }

    if (clampedSaturation != 100) {
      final android.graphics.ColorMatrix saturation = new android.graphics.ColorMatrix();
      saturation.setSaturation(clampedSaturation / 100f);
      if (hasMatrix) {
        combined.postConcat(saturation);
      } else {
        combined.set(saturation);
        hasMatrix = true;
      }
    }

    return hasMatrix ? new android.graphics.ColorMatrixColorFilter(combined) : null;
  }

  private static int clampPercent0To200(int percent) {
    return Math.max(0, Math.min(200, percent));
  }

  @Nullable
  private static android.graphics.Bitmap decodeThumbnail(@NonNull File file, int targetSizePx) {
    final android.graphics.BitmapFactory.Options bounds =
        new android.graphics.BitmapFactory.Options();
    bounds.inJustDecodeBounds = true;
    android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

    final int requested = Math.max(1, targetSizePx);
    final android.graphics.BitmapFactory.Options options =
        new android.graphics.BitmapFactory.Options();
    options.inSampleSize =
        calculateInSampleSize(bounds.outWidth, bounds.outHeight, requested, requested);
    options.inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888;

    final android.graphics.Bitmap decoded;
    try {
      decoded = android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    } catch (OutOfMemoryError oom) {
      return null;
    }
    if (decoded == null) return null;

    final int w = decoded.getWidth();
    final int h = decoded.getHeight();
    final int maxDim = Math.max(w, h);
    if (maxDim <= requested) return decoded;

    final float scale = requested / (float) maxDim;
    final int scaledW = Math.max(1, Math.round(w * scale));
    final int scaledH = Math.max(1, Math.round(h * scale));

    final android.graphics.Bitmap scaled;
    try {
      scaled = android.graphics.Bitmap.createScaledBitmap(decoded, scaledW, scaledH, true);
    } catch (OutOfMemoryError oom) {
      decoded.recycle();
      return null;
    }
    if (scaled != decoded) decoded.recycle();
    return scaled;
  }

  private static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
    int inSampleSize = 1;
    while ((height / inSampleSize) > reqHeight || (width / inSampleSize) > reqWidth) {
      inSampleSize *= 2;
    }
    return Math.max(1, inSampleSize);
  }

  private static final class WallpaperPreviewDrawable extends android.graphics.drawable.Drawable {
    @NonNull private final android.graphics.Bitmap bitmap;
    @NonNull private final android.graphics.Paint paint;
    @NonNull private final android.graphics.BitmapShader shader;
    @NonNull private final android.graphics.Matrix shaderMatrix = new android.graphics.Matrix();
    private final int rotationDegrees;
    private final int scaleMode;
    private final int anchor;
    private int alpha = 0xFF;

    WallpaperPreviewDrawable(
        @NonNull android.graphics.Bitmap bitmap, int rotationDegrees, int scaleMode, int anchor) {
      this.bitmap = bitmap;
      this.rotationDegrees = rotationDegrees;
      this.scaleMode = scaleMode;
      this.anchor = anchor;
      this.paint = new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG);
      final android.graphics.Shader.TileMode tileMode =
          KeyboardWallpaperTransform.tileModeForScaleMode(scaleMode);
      this.shader = new android.graphics.BitmapShader(bitmap, tileMode, tileMode);
      this.paint.setShader(shader);
    }

    @Override
    protected void onBoundsChange(android.graphics.Rect bounds) {
      super.onBoundsChange(bounds);
      KeyboardWallpaperTransform.updateShaderMatrix(
          shaderMatrix,
          bitmap.getWidth(),
          bitmap.getHeight(),
          bounds,
          rotationDegrees,
          scaleMode,
          anchor);
      shader.setLocalMatrix(shaderMatrix);
    }

    @Override
    public void draw(@NonNull android.graphics.Canvas canvas) {
      paint.setAlpha(alpha);
      canvas.drawRect(getBounds(), paint);
    }

    @Override
    public void setAlpha(int alpha) {
      this.alpha = alpha;
      invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable android.graphics.ColorFilter colorFilter) {
      paint.setColorFilter(colorFilter);
      invalidateSelf();
    }

    @Override
    public int getOpacity() {
      return android.graphics.PixelFormat.TRANSLUCENT;
    }
  }
}
