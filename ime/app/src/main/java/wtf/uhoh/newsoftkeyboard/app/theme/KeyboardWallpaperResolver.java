package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Trace;
import android.view.View;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.core.os.UserManagerCompat;
import java.io.File;
import java.util.WeakHashMap;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.addons.AddOn;

/** Resolves the IME container wallpaper for a given {@link KeyboardTheme}. */
public class KeyboardWallpaperResolver {

  private static final WallpaperBitmapLoader DEFAULT_WALLPAPER_BITMAP_LOADER =
      WallpaperBitmapRepository.getInstance();
  private static final KeyboardWallpaperLayer[] EMPTY_LAYER_STACK = new KeyboardWallpaperLayer[0];

  @Keep
  @VisibleForTesting
  public static void resetMainThreadDecodeCounterForTests() {
    WallpaperBitmapRepository.resetMainThreadDecodeCountForTests();
  }

  @Keep
  @VisibleForTesting
  public static int getMainThreadDecodeCountForTests() {
    return WallpaperBitmapRepository.getMainThreadDecodeCountForTests();
  }

  private final Context appContext;
  private final KeyboardWallpaperOverrideStore overrideStore;
  private final KeyboardThemeUserOverridesStore themeOverridesStore;
  private final WallpaperBitmapLoader wallpaperBitmapLoader;
  private final KeyboardThemePresetStore presetStore;
  private final KeyboardWallpaperBackgroundLayerStackApplier backgroundLayerStackApplier;
  private final KeyboardWallpaperKeyFaceOverlayResolver keyFaceOverlayResolver;

  private String cachedPhotoThemeId;
  private long cachedPhotoLastModified;
  private int cachedPhotoMaxDimBucketPx;
  @Nullable private Bitmap cachedPhotoBitmap;
  @Nullable private WallpaperBitmapDrawable cachedPhotoBaseDrawable;
  private int cachedPhotoSaturationPercent;
  private int cachedPhotoContrastPercent;
  private int cachedPhotoBrightnessPercent;
  private int cachedPhotoTemperaturePercent;

  private final WeakHashMap<View, Integer> backgroundApplyTokens = new WeakHashMap<>();
  private int nextBackgroundApplyToken = 1;

  public KeyboardWallpaperResolver(@NonNull Context context) {
    this(context, DEFAULT_WALLPAPER_BITMAP_LOADER);
  }

  KeyboardWallpaperResolver(@NonNull Context context, @NonNull WallpaperBitmapLoader bitmapLoader) {
    appContext = context.getApplicationContext();
    overrideStore = new KeyboardWallpaperOverrideStore(appContext);
    themeOverridesStore = new KeyboardThemeUserOverridesStore(appContext);
    wallpaperBitmapLoader = bitmapLoader;
    presetStore = new KeyboardThemePresetStore(appContext);
    backgroundLayerStackApplier =
        new KeyboardWallpaperBackgroundLayerStackApplier(overrideStore, wallpaperBitmapLoader);
    keyFaceOverlayResolver =
        new KeyboardWallpaperKeyFaceOverlayResolver(overrideStore, wallpaperBitmapLoader);
    cachedPhotoMaxDimBucketPx = 0;
    cachedPhotoSaturationPercent = Integer.MIN_VALUE;
    cachedPhotoContrastPercent = Integer.MIN_VALUE;
    cachedPhotoBrightnessPercent = Integer.MIN_VALUE;
    cachedPhotoTemperaturePercent = Integer.MIN_VALUE;
  }

  @NonNull
  public Drawable resolveImeWallpaper(@Nullable KeyboardTheme theme) {
    final Drawable fallback = ContextCompat.getDrawable(appContext, R.drawable.nsk_wallpaper);
    if (theme == null || fallback == null) return fallback;

    final Drawable photoOverride = resolvePhotoOverrideInternal(theme);
    if (photoOverride != null) return applyKeyboardBackgroundOpacityIfAny(theme, photoOverride);

    final Drawable themeWallpaper = resolveThemeWallpaper(theme);
    return applyKeyboardBackgroundOpacityIfAny(
        theme, themeWallpaper != null ? themeWallpaper : fallback);
  }

  /** Returns the theme-provided wallpaper (if any), without applying user photo overrides. */
  @NonNull
  public Drawable resolveThemeWallpaperOrFallback(@Nullable KeyboardTheme theme) {
    final Drawable fallback = ContextCompat.getDrawable(appContext, R.drawable.nsk_wallpaper);
    if (theme == null || fallback == null) return fallback;

    final Drawable themeWallpaper = resolveThemeWallpaper(theme);
    return applyKeyboardBackgroundOpacityIfAny(
        theme, themeWallpaper != null ? themeWallpaper : fallback);
  }

  @Nullable
  public Drawable resolvePhotoOverrideIfAny(@Nullable KeyboardTheme theme) {
    if (theme == null) return null;
    return resolvePhotoOverrideInternal(theme);
  }

  /**
   * Applies the user photo override (if any) as this view's background without blocking the UI
   * thread.
   *
   * <p>The view background is updated only after it has non-zero bounds.
   */
  public void applyPhotoOverrideIfAnyAsync(@NonNull View view, @Nullable KeyboardTheme theme) {
    if (theme == null) return;

    // Locked decision: don't attempt to load user photos on direct-boot / locked user.
    if (!UserManagerCompat.isUserUnlocked(appContext)) {
      clearPhotoCache();
      return;
    }

    final String themeId = presetStore.getActivePresetId(theme.getId());
    if (overrideStore.isWallpaperInvalid(themeId)) {
      if (themeId.equals(cachedPhotoThemeId)) clearPhotoCache();
      return;
    }

    final File file = overrideStore.getWallpaperFile(themeId);
    if (!file.isFile()) {
      if (themeId.equals(cachedPhotoThemeId)) clearPhotoCache();
      return;
    }

    final long lastModified = file.lastModified();
    final int rotationDegrees = overrideStore.getWallpaperRotationDegrees(themeId);
    final int scaleMode = overrideStore.getWallpaperScaleMode(themeId);
    final int anchor = overrideStore.getWallpaperAnchor(themeId);
    final Integer keyboardBackgroundTint = resolveKeyboardBackgroundTint(themeId);
    final int keyboardBackgroundAlpha = resolveKeyboardBackgroundOpacityAlpha(themeId);
    final int saturationPercent = overrideStore.getSaturationPercent(themeId);
    final int contrastPercent = overrideStore.getContrastPercent(themeId);
    final int brightnessPercent = overrideStore.getBrightnessPercent(themeId);
    final int temperaturePercent = overrideStore.getTemperaturePercent(themeId);

    final int token = nextBackgroundApplyToken++;
    backgroundApplyTokens.put(view, token);

    final Runnable start =
        () -> {
          final Integer currentToken = backgroundApplyTokens.get(view);
          if (currentToken == null || currentToken != token) return;

          final int width = view.getWidth();
          final int height = view.getHeight();
          if (width <= 0 || height <= 0) return;

          final int maxDimBucket =
              WallpaperBitmapRepository.bucketMaxDimPx(
                  applyQualityToMaxDimPx(themeId, Math.max(width, height)));
          final Bitmap cached = wallpaperBitmapLoader.getCached(file, lastModified, maxDimBucket);
          if (cached != null) {
            applyCachedPhotoToView(
                view,
                themeId,
                lastModified,
                maxDimBucket,
                cached,
                rotationDegrees,
                scaleMode,
                anchor,
                keyboardBackgroundTint,
                keyboardBackgroundAlpha,
                saturationPercent,
                contrastPercent,
                brightnessPercent,
                temperaturePercent,
                token);
            return;
          }

          wallpaperBitmapLoader.loadAsync(
              file,
              lastModified,
              maxDimBucket,
              bitmap -> {
                final Integer stillCurrent = backgroundApplyTokens.get(view);
                if (stillCurrent == null || stillCurrent != token) return;

                if (bitmap == null) {
                  // treat as invalid/corrupt and fall back to theme wallpaper
                  //noinspection ResultOfMethodCallIgnored
                  file.delete();
                  overrideStore.markWallpaperInvalid(themeId);
                  if (themeId.equals(cachedPhotoThemeId)) clearPhotoCache();
                  return;
                }

                applyCachedPhotoToView(
                    view,
                    themeId,
                    lastModified,
                    maxDimBucket,
                    bitmap,
                    rotationDegrees,
                    scaleMode,
                    anchor,
                    keyboardBackgroundTint,
                    keyboardBackgroundAlpha,
                    saturationPercent,
                    contrastPercent,
                    brightnessPercent,
                    temperaturePercent,
                    token);
              });
        };

    if (view.getWidth() > 0 && view.getHeight() > 0) {
      start.run();
    } else {
      view.addOnLayoutChangeListener(
          new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(
                View v,
                int left,
                int top,
                int right,
                int bottom,
                int oldLeft,
                int oldTop,
                int oldRight,
                int oldBottom) {
              v.removeOnLayoutChangeListener(this);
              start.run();
            }
          });
    }
  }

  private void applyCachedPhotoToView(
      @NonNull View view,
      @NonNull String themeId,
      long lastModified,
      int maxDimBucketPx,
      @NonNull Bitmap bitmap,
      int rotationDegrees,
      int scaleMode,
      int anchor,
      @Nullable Integer keyboardBackgroundTint,
      int keyboardBackgroundAlpha,
      int saturationPercent,
      int contrastPercent,
      int brightnessPercent,
      int temperaturePercent,
      int expectedToken) {
    Trace.beginSection("NSK.WallpaperApply");
    try {
      final Integer token = backgroundApplyTokens.get(view);
      if (token == null || token != expectedToken) return;

      final WallpaperBitmapDrawable baseDrawable =
          cacheLoadedPhoto(themeId, lastModified, maxDimBucketPx, bitmap);
      baseDrawable.setTransform(rotationDegrees, scaleMode, anchor);
      applyColorAdjustmentsToPhotoIfNeeded(
          baseDrawable, saturationPercent, contrastPercent, brightnessPercent, temperaturePercent);
      final Drawable background =
          backgroundLayerStackApplier.applyPhotoOverlaysCached(
              themeId, baseDrawable, keyboardBackgroundTint, lastModified, maxDimBucketPx);
      background.setAlpha(keyboardBackgroundAlpha);
      view.setBackground(background);
    } finally {
      Trace.endSection();
    }
  }

  private void applyColorAdjustmentsToPhotoIfNeeded(
      @NonNull WallpaperBitmapDrawable baseDrawable,
      int saturationPercent,
      int contrastPercent,
      int brightnessPercent,
      int temperaturePercent) {
    final int clampedSaturation = KeyboardWallpaperRenderMath.clampPercent0To200(saturationPercent);
    final int clampedContrast = KeyboardWallpaperRenderMath.clampPercent0To200(contrastPercent);
    final int clampedBrightness = KeyboardWallpaperRenderMath.clampPercent0To200(brightnessPercent);
    final int clampedTemperature =
        KeyboardWallpaperRenderMath.clampPercent0To200(temperaturePercent);
    if (cachedPhotoSaturationPercent == clampedSaturation
        && cachedPhotoContrastPercent == clampedContrast
        && cachedPhotoBrightnessPercent == clampedBrightness
        && cachedPhotoTemperaturePercent == clampedTemperature) {
      return;
    }
    baseDrawable.setColorFilter(
        KeyboardWallpaperRenderMath.createPhotoColorFilter(
            clampedSaturation, clampedContrast, clampedBrightness, clampedTemperature));
    cachedPhotoSaturationPercent = clampedSaturation;
    cachedPhotoContrastPercent = clampedContrast;
    cachedPhotoBrightnessPercent = clampedBrightness;
    cachedPhotoTemperaturePercent = clampedTemperature;
  }

  @Nullable
  private Drawable resolvePhotoOverrideInternal(@NonNull KeyboardTheme theme) {
    // Locked decision: don't attempt to load user photos on direct-boot / locked user.
    if (!UserManagerCompat.isUserUnlocked(appContext)) {
      clearPhotoCache();
      return null;
    }

    final String themeId = presetStore.getActivePresetId(theme.getId());
    if (cachedPhotoThemeId != null && !cachedPhotoThemeId.equals(themeId)) {
      clearPhotoCache();
    }

    if (overrideStore.isWallpaperInvalid(themeId)) {
      if (themeId.equals(cachedPhotoThemeId)) clearPhotoCache();
      return null;
    }

    final File file = overrideStore.getWallpaperFile(themeId);
    if (!file.isFile()) {
      if (themeId.equals(cachedPhotoThemeId)) clearPhotoCache();
      return null;
    }

    final long lastModified = file.lastModified();
    final int rotationDegrees = overrideStore.getWallpaperRotationDegrees(themeId);
    final int scaleMode = overrideStore.getWallpaperScaleMode(themeId);
    final int anchor = overrideStore.getWallpaperAnchor(themeId);
    final Integer keyboardBackgroundTint = resolveKeyboardBackgroundTint(themeId);
    final int keyboardBackgroundAlpha = resolveKeyboardBackgroundOpacityAlpha(themeId);
    final int saturationPercent = overrideStore.getSaturationPercent(themeId);
    final int contrastPercent = overrideStore.getContrastPercent(themeId);
    final int brightnessPercent = overrideStore.getBrightnessPercent(themeId);
    final int temperaturePercent = overrideStore.getTemperaturePercent(themeId);
    final int maxDim =
        Math.max(
            appContext.getResources().getDisplayMetrics().widthPixels,
            appContext.getResources().getDisplayMetrics().heightPixels);
    final int maxDimPx = applyQualityToMaxDimPx(themeId, maxDim);
    final WallpaperBitmapDrawable baseDrawable =
        ensurePhotoLoaded(themeId, file, lastModified, maxDimPx);
    if (baseDrawable == null) return null;

    baseDrawable.setTransform(rotationDegrees, scaleMode, anchor);
    applyColorAdjustmentsToPhotoIfNeeded(
        baseDrawable, saturationPercent, contrastPercent, brightnessPercent, temperaturePercent);
    final Drawable layered =
        backgroundLayerStackApplier.applyPhotoOverlaysCached(
            themeId,
            baseDrawable,
            keyboardBackgroundTint,
            lastModified,
            WallpaperBitmapRepository.bucketMaxDimPx(maxDimPx));
    layered.setAlpha(keyboardBackgroundAlpha);
    return layered;
  }

  @Nullable
  private Drawable resolveThemeWallpaper(@NonNull KeyboardTheme theme) {
    final Context packageContext = theme.getPackageContext();
    if (packageContext == null) return null;

    final AddOn.AddOnResourceMapping resourceMapping = theme.getResourceMapping();
    final int[] remoteKeyboardThemeStyleable =
        resourceMapping.getRemoteStyleableArrayFromLocal(R.styleable.AnyKeyboardViewTheme);
    if (remoteKeyboardThemeStyleable.length == 0) return null;

    final TypedArray a =
        packageContext.obtainStyledAttributes(theme.getThemeResId(), remoteKeyboardThemeStyleable);
    try {
      final int attrCount = a.getIndexCount();
      for (int i = 0; i < attrCount; i++) {
        final int remoteIndex = a.getIndex(i);
        final int localAttrId =
            resourceMapping.getLocalAttrId(remoteKeyboardThemeStyleable[remoteIndex]);
        if (localAttrId == R.attr.keyboardWallpaper) {
          return a.getDrawable(remoteIndex);
        }
      }
      return null;
    } finally {
      a.recycle();
    }
  }

  int applyQualityToMaxDimPx(@NonNull String themeId, int maxDimPx) {
    final int clamped = Math.max(1, maxDimPx);
    switch (overrideStore.getWallpaperQuality(themeId)) {
      case KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_LOW:
        return Math.max(1, clamped / 2);
      case KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_HIGH:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_BALANCED:
      default:
        return clamped;
    }
  }

  private void clearPhotoCache() {
    cachedPhotoThemeId = null;
    cachedPhotoLastModified = 0L;
    cachedPhotoMaxDimBucketPx = 0;
    cachedPhotoSaturationPercent = Integer.MIN_VALUE;
    cachedPhotoContrastPercent = Integer.MIN_VALUE;
    cachedPhotoBrightnessPercent = Integer.MIN_VALUE;
    cachedPhotoTemperaturePercent = Integer.MIN_VALUE;
    cachedPhotoBitmap = null;
    cachedPhotoBaseDrawable = null;
    backgroundLayerStackApplier.clearCache();
    keyFaceOverlayResolver.clearCache();
  }

  @Nullable
  WallpaperBitmapDrawable ensurePhotoLoaded(
      @NonNull String themeId, @NonNull File file, long lastModified, int maxDimPx) {
    final int bucket = WallpaperBitmapRepository.bucketMaxDimPx(maxDimPx);
    if (cachedPhotoBaseDrawable != null
        && themeId.equals(cachedPhotoThemeId)
        && cachedPhotoLastModified == lastModified
        && cachedPhotoMaxDimBucketPx == bucket) {
      return cachedPhotoBaseDrawable;
    }

    final Bitmap bitmap = wallpaperBitmapLoader.getCached(file, lastModified, bucket);
    if (bitmap == null) return null;

    return cacheLoadedPhoto(themeId, lastModified, bucket, bitmap);
  }

  void clearPhotoCacheIfThemeMatches(@NonNull String themeId) {
    if (themeId.equals(cachedPhotoThemeId)) clearPhotoCache();
  }

  @NonNull
  private WallpaperBitmapDrawable cacheLoadedPhoto(
      @NonNull String themeId, long lastModified, int maxDimBucketPx, @NonNull Bitmap bitmap) {
    if (cachedPhotoBaseDrawable != null
        && themeId.equals(cachedPhotoThemeId)
        && cachedPhotoLastModified == lastModified
        && cachedPhotoMaxDimBucketPx == maxDimBucketPx
        && cachedPhotoBitmap == bitmap) {
      return cachedPhotoBaseDrawable;
    }

    cachedPhotoThemeId = themeId;
    cachedPhotoLastModified = lastModified;
    cachedPhotoMaxDimBucketPx = maxDimBucketPx;
    cachedPhotoBitmap = bitmap;
    cachedPhotoBaseDrawable = new WallpaperBitmapDrawable(bitmap);
    backgroundLayerStackApplier.clearCache();
    cachedPhotoSaturationPercent = Integer.MIN_VALUE;
    cachedPhotoContrastPercent = Integer.MIN_VALUE;
    cachedPhotoBrightnessPercent = Integer.MIN_VALUE;
    cachedPhotoTemperaturePercent = Integer.MIN_VALUE;
    keyFaceOverlayResolver.clearCache();

    return cachedPhotoBaseDrawable;
  }

  @Nullable
  private Integer resolveKeyboardBackgroundTint(@NonNull String themeId) {
    final Integer override = themeOverridesStore.getKeyboardBackgroundTint(themeId);
    if (override != null) return override;
    return themeOverridesStore.getTokenBackgroundColor(themeId);
  }

  private int resolveKeyboardBackgroundOpacityAlpha(@NonNull String themeId) {
    final Integer opacityPercent = themeOverridesStore.getKeyboardBackgroundOpacityPercent(themeId);
    return KeyboardWallpaperRenderMath.alphaForPercent(
        opacityPercent != null ? opacityPercent : 100);
  }

  @NonNull
  private Drawable applyKeyboardBackgroundOpacityIfAny(
      @NonNull KeyboardTheme theme, @NonNull Drawable drawable) {
    final String themeId = presetStore.getActivePresetId(theme.getId());
    final int alpha = resolveKeyboardBackgroundOpacityAlpha(themeId);
    drawable.mutate();
    drawable.setAlpha(alpha);
    return drawable;
  }

  /**
   * Resolves the user photo overlay to be drawn on top of key backgrounds (key faces).
   *
   * <p>This is intentionally separate from the view background drawable so the overlay can be
   * anchored to the keyboard view bounds and appear continuous across keys.
   */
  @NonNull
  public KeyFaceOverlay resolveKeyFaceOverlay(
      @Nullable KeyboardTheme theme,
      @NonNull Rect keyboardViewBounds,
      @NonNull Runnable requestInvalidate) {
    return resolveKeyFaceOverlay(theme, keyboardViewBounds, true, requestInvalidate);
  }

  @NonNull
  public KeyFaceOverlay resolveKeyFaceOverlay(
      @Nullable KeyboardTheme theme,
      @NonNull Rect keyboardViewBounds,
      boolean allowMatchKeyShape,
      @NonNull Runnable requestInvalidate) {
    if (theme == null) return keyFaceOverlayResolver.empty();

    // Locked decision: don't attempt to load user photos on direct-boot / locked user.
    if (!UserManagerCompat.isUserUnlocked(appContext)) {
      clearPhotoCache();
      return keyFaceOverlayResolver.empty();
    }

    final String themeId = presetStore.getActivePresetId(theme.getId());
    if (cachedPhotoThemeId != null && !cachedPhotoThemeId.equals(themeId)) {
      clearPhotoCache();
    }

    return keyFaceOverlayResolver.resolveKeyFaceOverlay(
        this, themeId, keyboardViewBounds, allowMatchKeyShape, requestInvalidate);
  }
}
