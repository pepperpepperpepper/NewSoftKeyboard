package wtf.uhoh.newsoftkeyboard.app.theme;

import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class KeyboardWallpaperRenderMath {

  private KeyboardWallpaperRenderMath() {}

  static int clampPercent(int value) {
    if (value < 0) return 0;
    if (value > 100) return 100;
    return value;
  }

  static int clampPercent0To200(int value) {
    if (value < 0) return 0;
    if (value > 200) return 200;
    return value;
  }

  static int alphaForPercent(int percent) {
    final int clamped = clampPercent(percent);
    // Integer rounding: (255 * p / 100) with proper half-up rounding.
    return (255 * clamped + 50) / 100;
  }

  private static final PorterDuffXfermode XFER_MULTIPLY =
      new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY);
  private static final PorterDuffXfermode XFER_SCREEN =
      new PorterDuffXfermode(PorterDuff.Mode.SCREEN);
  private static final PorterDuffXfermode XFER_OVERLAY =
      new PorterDuffXfermode(PorterDuff.Mode.OVERLAY);

  static void configureBlendMode(@NonNull Paint paint, int blendMode) {
    paint.setXfermode(null);
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
      paint.setBlendMode(null);
    }
    switch (blendMode) {
      case KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_MULTIPLY:
        paint.setXfermode(XFER_MULTIPLY);
        break;
      case KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_SCREEN:
        paint.setXfermode(XFER_SCREEN);
        break;
      case KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_OVERLAY:
        paint.setXfermode(XFER_OVERLAY);
        break;
      case KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_SOFT_LIGHT:
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
          paint.setBlendMode(android.graphics.BlendMode.SOFT_LIGHT);
        } else {
          paint.setXfermode(XFER_OVERLAY);
        }
        break;
      case KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL:
      default:
        break;
    }
  }

  static int blurDownscaleFactorForScalePercent(int scalePercent) {
    final int clamped = Math.max(25, Math.min(400, scalePercent));
    final int computed = Math.round((clamped / 100f) * 4f);
    return Math.max(2, Math.min(16, computed));
  }

  @Nullable
  static ColorFilter createPhotoColorFilter(int saturationPercent, int contrastPercent) {
    return createPhotoColorFilter(saturationPercent, contrastPercent, 100, 100);
  }

  @Nullable
  static ColorFilter createPhotoColorFilter(
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

    final ColorMatrix combined = new ColorMatrix();
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
      final ColorMatrix brightness =
          new ColorMatrix(
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
      final ColorMatrix temperature =
          new ColorMatrix(
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
      final ColorMatrix saturation = new ColorMatrix();
      saturation.setSaturation(clampedSaturation / 100f);
      if (hasMatrix) {
        combined.postConcat(saturation);
      } else {
        combined.set(saturation);
        hasMatrix = true;
      }
    }

    return hasMatrix ? new ColorMatrixColorFilter(combined) : null;
  }

  @Nullable
  static ColorFilter createPhotoColorFilterWithDim(
      int dimPercent,
      int saturationPercent,
      int contrastPercent,
      int brightnessPercent,
      int temperaturePercent) {
    final int clampedDim = clampPercent(dimPercent);
    final int clampedSaturation = clampPercent0To200(saturationPercent);
    final int clampedContrast = clampPercent0To200(contrastPercent);
    final int clampedBrightness = clampPercent0To200(brightnessPercent);
    final int clampedTemperature = clampPercent0To200(temperaturePercent);
    if (clampedDim <= 0
        && clampedSaturation == 100
        && clampedContrast == 100
        && clampedBrightness == 100
        && clampedTemperature == 100) {
      return null;
    }

    final ColorMatrix combined = new ColorMatrix();
    boolean hasMatrix = false;

    if (clampedDim > 0) {
      final float factor = (100f - clampedDim) / 100f;
      combined.set(
          new float[] {
            factor, 0f, 0f, 0f, 0f, 0f, factor, 0f, 0f, 0f, 0f, 0f, factor, 0f, 0f, 0f, 0f, 0f, 1f,
            0f
          });
      hasMatrix = true;
    }

    if (clampedContrast != 100) {
      final float factor = clampedContrast / 100f;
      final float translate = 128f * (1f - factor);
      final ColorMatrix contrast =
          new ColorMatrix(
              new float[] {
                factor, 0f, 0f, 0f, translate, 0f, factor, 0f, 0f, translate, 0f, 0f, factor, 0f,
                translate, 0f, 0f, 0f, 1f, 0f
              });
      if (hasMatrix) {
        combined.postConcat(contrast);
      } else {
        combined.set(contrast);
        hasMatrix = true;
      }
    }

    if (clampedBrightness != 100) {
      final float translate = ((clampedBrightness - 100) / 100f) * 128f;
      final ColorMatrix brightness =
          new ColorMatrix(
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
      final ColorMatrix temperature =
          new ColorMatrix(
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
      final ColorMatrix saturation = new ColorMatrix();
      saturation.setSaturation(clampedSaturation / 100f);
      if (hasMatrix) {
        combined.postConcat(saturation);
      } else {
        combined.set(saturation);
        hasMatrix = true;
      }
    }

    return hasMatrix ? new ColorMatrixColorFilter(combined) : null;
  }
}
