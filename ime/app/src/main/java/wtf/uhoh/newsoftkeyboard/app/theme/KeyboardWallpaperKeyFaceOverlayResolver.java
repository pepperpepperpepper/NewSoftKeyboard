package wtf.uhoh.newsoftkeyboard.app.theme;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;

final class KeyboardWallpaperKeyFaceOverlayResolver {

  private static final KeyboardWallpaperLayer[] EMPTY_LAYER_STACK = new KeyboardWallpaperLayer[0];

  @NonNull private final KeyboardWallpaperOverrideStore overrideStore;
  @NonNull private final WallpaperBitmapLoader wallpaperBitmapLoader;

  private int cachedKeyFaceOverlayDimPercent = Integer.MIN_VALUE;
  private int cachedKeyFaceOverlaySaturationPercent = Integer.MIN_VALUE;
  private int cachedKeyFaceOverlayContrastPercent = Integer.MIN_VALUE;
  private int cachedKeyFaceOverlayBrightnessPercent = Integer.MIN_VALUE;
  private int cachedKeyFaceOverlayTemperaturePercent = Integer.MIN_VALUE;
  private int cachedKeyFaceOverlayAlpha = Integer.MIN_VALUE;
  private int cachedKeyFaceOverlayRotationDegrees = Integer.MIN_VALUE;
  private int cachedKeyFaceOverlayScaleMode = Integer.MIN_VALUE;
  private int cachedKeyFaceOverlayAnchor = Integer.MIN_VALUE;
  @Nullable private Paint cachedKeyFaceOverlayPaint;
  @Nullable private Shader cachedKeyFaceOverlayShader;
  private final Rect cachedKeyFaceOverlayBounds = new Rect();
  private final Matrix cachedKeyFaceOverlayMatrix = new Matrix();

  @NonNull private final KeyFaceOverlay keyFaceOverlay = new KeyFaceOverlay();

  @Nullable private String lastRequestedKeyFaceThemeId;
  private long lastRequestedKeyFaceLastModified = 0L;
  private int lastRequestedKeyFaceBucketPx = 0;

  KeyboardWallpaperKeyFaceOverlayResolver(
      @NonNull KeyboardWallpaperOverrideStore overrideStore,
      @NonNull WallpaperBitmapLoader wallpaperBitmapLoader) {
    this.overrideStore = overrideStore;
    this.wallpaperBitmapLoader = wallpaperBitmapLoader;
  }

  void clearCache() {
    cachedKeyFaceOverlayDimPercent = Integer.MIN_VALUE;
    cachedKeyFaceOverlaySaturationPercent = Integer.MIN_VALUE;
    cachedKeyFaceOverlayContrastPercent = Integer.MIN_VALUE;
    cachedKeyFaceOverlayBrightnessPercent = Integer.MIN_VALUE;
    cachedKeyFaceOverlayTemperaturePercent = Integer.MIN_VALUE;
    cachedKeyFaceOverlayAlpha = Integer.MIN_VALUE;
    cachedKeyFaceOverlayRotationDegrees = Integer.MIN_VALUE;
    cachedKeyFaceOverlayScaleMode = Integer.MIN_VALUE;
    cachedKeyFaceOverlayAnchor = Integer.MIN_VALUE;
    cachedKeyFaceOverlayPaint = null;
    cachedKeyFaceOverlayShader = null;
    cachedKeyFaceOverlayBounds.setEmpty();
    keyFaceOverlay.reset();

    lastRequestedKeyFaceThemeId = null;
    lastRequestedKeyFaceLastModified = 0L;
    lastRequestedKeyFaceBucketPx = 0;
  }

  @NonNull
  KeyFaceOverlay empty() {
    keyFaceOverlay.reset();
    return keyFaceOverlay;
  }

  @NonNull
  KeyFaceOverlay resolveKeyFaceOverlay(
      @NonNull KeyboardWallpaperResolver resolver,
      @NonNull String themeId,
      @NonNull Rect keyboardViewBounds,
      boolean allowMatchKeyShape,
      @NonNull Runnable requestInvalidate) {
    keyFaceOverlay.reset();

    final int quality = overrideStore.getWallpaperQuality(themeId);

    final int mode = overrideStore.getWallpaperMode(themeId);
    if (mode == KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY)
      return keyFaceOverlay;

    final int alphaPercent = overrideStore.getKeyAlphaPercent(themeId);
    if (alphaPercent <= 0) return keyFaceOverlay;

    if (overrideStore.isWallpaperInvalid(themeId)) {
      resolver.clearPhotoCacheIfThemeMatches(themeId);
      return keyFaceOverlay;
    }

    final File file = overrideStore.getWallpaperFile(themeId);
    if (!file.isFile()) {
      resolver.clearPhotoCacheIfThemeMatches(themeId);
      return keyFaceOverlay;
    }

    final int maxDimPx =
        resolver.applyQualityToMaxDimPx(
            themeId, Math.max(keyboardViewBounds.width(), keyboardViewBounds.height()));
    final long lastModified = file.lastModified();
    final WallpaperBitmapDrawable baseDrawable =
        resolver.ensurePhotoLoaded(themeId, file, lastModified, maxDimPx);
    if (baseDrawable == null) {
      requestKeyFaceBitmapLoadIfNeeded(
          resolver, themeId, file, lastModified, maxDimPx, requestInvalidate);
      return keyFaceOverlay;
    }
    final Bitmap bitmap = baseDrawable.getBitmap();

    final int rotationDegrees = overrideStore.getWallpaperRotationDegrees(themeId);
    final int scaleMode = overrideStore.getWallpaperScaleMode(themeId);
    final int anchor = overrideStore.getWallpaperAnchor(themeId);

    if (cachedKeyFaceOverlayPaint == null
        || cachedKeyFaceOverlayShader == null
        || cachedKeyFaceOverlayScaleMode != scaleMode) {
      final Shader.TileMode tileMode = KeyboardWallpaperTransform.tileModeForScaleMode(scaleMode);
      cachedKeyFaceOverlayShader = new android.graphics.BitmapShader(bitmap, tileMode, tileMode);
      cachedKeyFaceOverlayPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
      cachedKeyFaceOverlayPaint.setShader(cachedKeyFaceOverlayShader);
      cachedKeyFaceOverlayBounds.setEmpty();
      cachedKeyFaceOverlayDimPercent = Integer.MIN_VALUE;
      cachedKeyFaceOverlaySaturationPercent = Integer.MIN_VALUE;
      cachedKeyFaceOverlayContrastPercent = Integer.MIN_VALUE;
      cachedKeyFaceOverlayAlpha = Integer.MIN_VALUE;
      cachedKeyFaceOverlayRotationDegrees = Integer.MIN_VALUE;
      cachedKeyFaceOverlayScaleMode = scaleMode;
      cachedKeyFaceOverlayAnchor = Integer.MIN_VALUE;
    }

    // Ensure the shader mapping is anchored to the full keyboard view bounds, not per-key bounds.
    if (!cachedKeyFaceOverlayBounds.equals(keyboardViewBounds)
        || cachedKeyFaceOverlayRotationDegrees != rotationDegrees
        || cachedKeyFaceOverlayScaleMode != scaleMode
        || cachedKeyFaceOverlayAnchor != anchor) {
      cachedKeyFaceOverlayBounds.set(keyboardViewBounds);
      KeyboardWallpaperTransform.updateShaderMatrix(
          cachedKeyFaceOverlayMatrix,
          bitmap.getWidth(),
          bitmap.getHeight(),
          cachedKeyFaceOverlayBounds,
          rotationDegrees,
          scaleMode,
          anchor);
      if (cachedKeyFaceOverlayShader instanceof android.graphics.BitmapShader bitmapShader) {
        bitmapShader.setLocalMatrix(cachedKeyFaceOverlayMatrix);
      }
      cachedKeyFaceOverlayRotationDegrees = rotationDegrees;
      cachedKeyFaceOverlayScaleMode = scaleMode;
      cachedKeyFaceOverlayAnchor = anchor;
    }

    if (cachedKeyFaceOverlayAlpha != alphaPercent) {
      cachedKeyFaceOverlayPaint.setAlpha(KeyboardWallpaperRenderMath.alphaForPercent(alphaPercent));
      cachedKeyFaceOverlayAlpha = alphaPercent;
    }

    keyFaceOverlay.setSpecialKeyAlpha(
        KeyboardWallpaperRenderMath.alphaForPercent(
            overrideStore.getSpecialKeyAlphaPercent(themeId)));
    keyFaceOverlay.setSpacebarAlpha(
        KeyboardWallpaperRenderMath.alphaForPercent(
            overrideStore.getSpacebarAlphaPercent(themeId)));
    keyFaceOverlay.setModifierKeyAlpha(
        KeyboardWallpaperRenderMath.alphaForPercent(
            overrideStore.getModifierKeyAlphaPercent(themeId)));
    keyFaceOverlay.setEnterKeyAlpha(
        KeyboardWallpaperRenderMath.alphaForPercent(
            overrideStore.getEnterKeyAlphaPercent(themeId)));
    if (allowMatchKeyShape) {
      keyFaceOverlay.setLayerStack(overrideStore.getKeyLayerStack(themeId));
      keyFaceOverlay.setBlendMode(overrideStore.getKeyBlendMode(themeId));
    } else {
      // Cheap preview mode / power-saver mode: skip optional key-face overlays that would require
      // additional masking/compositing work in the keyboard draw loop.
      keyFaceOverlay.setLayerStack(EMPTY_LAYER_STACK);
      keyFaceOverlay.setBlendMode(KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL);
    }

    final int dimPercent = overrideStore.getDimPercent(themeId);
    final int saturationPercent = overrideStore.getSaturationPercent(themeId);
    final int contrastPercent = overrideStore.getContrastPercent(themeId);
    final int brightnessPercent = overrideStore.getBrightnessPercent(themeId);
    final int temperaturePercent = overrideStore.getTemperaturePercent(themeId);
    if (cachedKeyFaceOverlayDimPercent != dimPercent
        || cachedKeyFaceOverlaySaturationPercent != saturationPercent
        || cachedKeyFaceOverlayContrastPercent != contrastPercent
        || cachedKeyFaceOverlayBrightnessPercent != brightnessPercent
        || cachedKeyFaceOverlayTemperaturePercent != temperaturePercent) {
      cachedKeyFaceOverlayPaint.setColorFilter(
          KeyboardWallpaperRenderMath.createPhotoColorFilterWithDim(
              dimPercent,
              saturationPercent,
              contrastPercent,
              brightnessPercent,
              temperaturePercent));
      cachedKeyFaceOverlayDimPercent = dimPercent;
      cachedKeyFaceOverlaySaturationPercent = saturationPercent;
      cachedKeyFaceOverlayContrastPercent = contrastPercent;
      cachedKeyFaceOverlayBrightnessPercent = brightnessPercent;
      cachedKeyFaceOverlayTemperaturePercent = temperaturePercent;
    }

    keyFaceOverlay.setMode(mode);
    keyFaceOverlay.setPaint(cachedKeyFaceOverlayPaint);
    keyFaceOverlay.setMatchKeyShape(
        allowMatchKeyShape
            && quality == KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_HIGH
            && mode == KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE
            && overrideStore.isMatchKeyShapeEnabled(themeId));
    return keyFaceOverlay;
  }

  private void requestKeyFaceBitmapLoadIfNeeded(
      @NonNull KeyboardWallpaperResolver resolver,
      @NonNull String themeId,
      @NonNull File file,
      long lastModified,
      int maxDimPx,
      @NonNull Runnable requestInvalidate) {
    final int bucket = WallpaperBitmapRepository.bucketMaxDimPx(maxDimPx);
    if (bucket <= 0) return;
    if (themeId.equals(lastRequestedKeyFaceThemeId)
        && lastRequestedKeyFaceLastModified == lastModified
        && lastRequestedKeyFaceBucketPx == bucket) {
      return;
    }
    lastRequestedKeyFaceThemeId = themeId;
    lastRequestedKeyFaceLastModified = lastModified;
    lastRequestedKeyFaceBucketPx = bucket;

    wallpaperBitmapLoader.loadAsync(
        file,
        lastModified,
        bucket,
        bitmap -> {
          if (bitmap == null) {
            // treat as invalid/corrupt and fall back to theme wallpaper
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            overrideStore.markWallpaperInvalid(themeId);
            resolver.clearPhotoCacheIfThemeMatches(themeId);
            return;
          }
          requestInvalidate.run();
        });
  }
}
