package wtf.uhoh.newsoftkeyboard.app.theme;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.palette.graphics.Palette;
import java.io.File;

/** Extracts a simple, fast color scheme from a keyboard background photo. */
public final class WallpaperColorSchemeExtractor {

  private WallpaperColorSchemeExtractor() {}

  public enum PaletteSource {
    DOMINANT,
    VIBRANT,
    MUTED,
    DARK_VIBRANT,
    LIGHT_VIBRANT,
    DARK_MUTED,
    LIGHT_MUTED
  }

  private static final int DEFAULT_MAX_DIM_PX = 96;
  private static final int HISTOGRAM_BUCKETS = 1 << 15; // 5 bits per channel.
  private static final int MIN_ALPHA = 0x10;

  @NonNull
  public static Result extract(@NonNull File file, int dimPercent) {
    return extract(file, dimPercent, PaletteSource.DOMINANT);
  }

  @NonNull
  public static Result extract(@NonNull File file, int dimPercent, @NonNull PaletteSource source) {
    final Bitmap bitmap = decodeDownsampled(file, DEFAULT_MAX_DIM_PX);
    if (bitmap == null) return Result.fallback();
    try {
      return extract(bitmap, dimPercent, source);
    } finally {
      bitmap.recycle();
    }
  }

  @NonNull
  @VisibleForTesting
  static Result extract(@NonNull Bitmap bitmap, int dimPercent) {
    return extract(bitmap, dimPercent, PaletteSource.DOMINANT);
  }

  @NonNull
  @VisibleForTesting
  static Result extract(@NonNull Bitmap bitmap, int dimPercent, @NonNull PaletteSource source) {
    final int width = bitmap.getWidth();
    final int height = bitmap.getHeight();
    if (width <= 0 || height <= 0) return Result.fallback();

    final int step = Math.max(1, Math.min(width, height) / 32);
    final int[] histogram = new int[HISTOGRAM_BUCKETS];

    long luminanceSum = 0L;
    long count = 0L;
    for (int y = 0; y < height; y += step) {
      for (int x = 0; x < width; x += step) {
        final int c = bitmap.getPixel(x, y);
        final int a = (c >>> 24) & 0xFF;
        if (a < MIN_ALPHA) continue;

        final int r = (c >> 16) & 0xFF;
        final int g = (c >> 8) & 0xFF;
        final int b = c & 0xFF;

        final int lum = (r * 2126 + g * 7152 + b * 722) / 10000;
        luminanceSum += lum;
        count++;

        final int bucket = ((r >> 3) << 10) | ((g >> 3) << 5) | (b >> 3);
        histogram[bucket]++;
      }
    }
    if (count <= 0) return Result.fallback();

    final int clampedDim = Math.max(0, Math.min(100, dimPercent));
    final float dimFactor = (100f - clampedDim) / 100f;
    final float avgLum = (luminanceSum / (float) count) * dimFactor;
    final boolean dark = avgLum < 128f;

    int dominantBucket = -1;
    int maxCount = 0;
    for (int i = 0; i < histogram.length; i++) {
      final int value = histogram[i];
      if (value <= maxCount) continue;
      dominantBucket = i;
      maxCount = value;
    }
    if (dominantBucket < 0 || maxCount <= 0) return Result.fallback();

    final int histogramAccent = bucketToColor(dominantBucket);
    final int accent = resolveAccentColor(bitmap, source, histogramAccent);
    final int keyTint = accent;
    final int keyboardTint = accent;

    final int textColor = dark ? Color.WHITE : Color.BLACK;
    final int hintColor = dark ? 0xCCFFFFFF : 0xCC000000;
    final int shadowColor = dark ? 0xB0000000 : 0xBFFFFFFF;

    final int specialKeyTint =
        dark ? blend(accent, Color.WHITE, 0.12f) : blend(accent, Color.BLACK, 0.12f);
    final int spacebarTint =
        dark ? blend(accent, Color.WHITE, 0.24f) : blend(accent, Color.BLACK, 0.24f);

    final int keyBackgroundOpacityPercent = 85;
    final int keyboardBackgroundOpacityPercent = 60;

    return new Result(
        accent,
        keyTint,
        specialKeyTint,
        spacebarTint,
        keyboardTint,
        keyBackgroundOpacityPercent,
        keyboardBackgroundOpacityPercent,
        textColor,
        hintColor,
        shadowColor);
  }

  private static int resolveAccentColor(
      @NonNull Bitmap bitmap, @NonNull PaletteSource source, @ColorInt int fallbackAccent) {
    if (source == PaletteSource.DOMINANT) return fallbackAccent;

    try {
      final Palette palette = Palette.from(bitmap).maximumColorCount(16).generate();
      return switch (source) {
        case VIBRANT -> palette.getVibrantColor(fallbackAccent);
        case MUTED -> palette.getMutedColor(fallbackAccent);
        case DARK_VIBRANT -> palette.getDarkVibrantColor(fallbackAccent);
        case LIGHT_VIBRANT -> palette.getLightVibrantColor(fallbackAccent);
        case DARK_MUTED -> palette.getDarkMutedColor(fallbackAccent);
        case LIGHT_MUTED -> palette.getLightMutedColor(fallbackAccent);
        case DOMINANT -> fallbackAccent;
      };
    } catch (RuntimeException ignored) {
      return fallbackAccent;
    }
  }

  private static int bucketToColor(int bucket) {
    final int r5 = (bucket >> 10) & 0x1F;
    final int g5 = (bucket >> 5) & 0x1F;
    final int b5 = bucket & 0x1F;
    final int r = (r5 << 3) | (r5 >> 2);
    final int g = (g5 << 3) | (g5 >> 2);
    final int b = (b5 << 3) | (b5 >> 2);
    return Color.rgb(r, g, b);
  }

  @Nullable
  private static Bitmap decodeDownsampled(@NonNull File file, int maxDimPx) {
    if (!file.isFile()) return null;

    final BitmapFactory.Options bounds = new BitmapFactory.Options();
    bounds.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
    final int outWidth = bounds.outWidth;
    final int outHeight = bounds.outHeight;
    if (outWidth <= 0 || outHeight <= 0) return null;

    int sampleSize = 1;
    final int maxDim = Math.max(outWidth, outHeight);
    while ((maxDim / sampleSize) > maxDimPx) {
      sampleSize *= 2;
    }

    final BitmapFactory.Options decode = new BitmapFactory.Options();
    decode.inSampleSize = sampleSize;
    decode.inPreferredConfig = Bitmap.Config.ARGB_8888;
    return BitmapFactory.decodeFile(file.getAbsolutePath(), decode);
  }

  private static int blend(@ColorInt int from, @ColorInt int to, float toAmount) {
    final float clamped = Math.max(0f, Math.min(1f, toAmount));
    final float fromAmount = 1f - clamped;

    final int fr = (from >> 16) & 0xFF;
    final int fg = (from >> 8) & 0xFF;
    final int fb = from & 0xFF;

    final int tr = (to >> 16) & 0xFF;
    final int tg = (to >> 8) & 0xFF;
    final int tb = to & 0xFF;

    final int r = Math.round(fr * fromAmount + tr * clamped);
    final int g = Math.round(fg * fromAmount + tg * clamped);
    final int b = Math.round(fb * fromAmount + tb * clamped);
    return Color.rgb(r, g, b);
  }

  public static final class Result {
    @ColorInt public final int accentColor;
    @ColorInt public final int keyBackgroundTint;
    @ColorInt public final int specialKeyBackgroundTint;
    @ColorInt public final int spacebarBackgroundTint;
    @ColorInt public final int keyboardBackgroundTint;
    public final int keyBackgroundOpacityPercent;
    public final int keyboardBackgroundOpacityPercent;
    @ColorInt public final int keyTextColor;
    @ColorInt public final int hintTextColor;
    @ColorInt public final int keyTextShadowColor;

    private Result(
        @ColorInt int accentColor,
        @ColorInt int keyBackgroundTint,
        @ColorInt int specialKeyBackgroundTint,
        @ColorInt int spacebarBackgroundTint,
        @ColorInt int keyboardBackgroundTint,
        int keyBackgroundOpacityPercent,
        int keyboardBackgroundOpacityPercent,
        @ColorInt int keyTextColor,
        @ColorInt int hintTextColor,
        @ColorInt int keyTextShadowColor) {
      this.accentColor = accentColor;
      this.keyBackgroundTint = keyBackgroundTint;
      this.specialKeyBackgroundTint = specialKeyBackgroundTint;
      this.spacebarBackgroundTint = spacebarBackgroundTint;
      this.keyboardBackgroundTint = keyboardBackgroundTint;
      this.keyBackgroundOpacityPercent = keyBackgroundOpacityPercent;
      this.keyboardBackgroundOpacityPercent = keyboardBackgroundOpacityPercent;
      this.keyTextColor = keyTextColor;
      this.hintTextColor = hintTextColor;
      this.keyTextShadowColor = keyTextShadowColor;
    }

    @NonNull
    static Result fallback() {
      return new Result(
          0xFF444444,
          0xFF444444,
          0xFF555555,
          0xFF666666,
          0xFF444444,
          85,
          60,
          Color.WHITE,
          0xCCFFFFFF,
          0xB0000000);
    }
  }
}
