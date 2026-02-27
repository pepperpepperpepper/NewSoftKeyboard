package wtf.uhoh.newsoftkeyboard.app.theme;

import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;

final class KeyboardWallpaperBackgroundLayerStackApplier {

  @NonNull private final KeyboardWallpaperOverrideStore overrideStore;
  @NonNull private final WallpaperBitmapLoader wallpaperBitmapLoader;

  @Nullable private int[] cachedPhotoBackgroundLayerTypes;
  @Nullable private Drawable[] cachedPhotoBackgroundLayerDrawables;
  @Nullable private LayerDrawable cachedPhotoLayeredDrawable;

  KeyboardWallpaperBackgroundLayerStackApplier(
      @NonNull KeyboardWallpaperOverrideStore overrideStore,
      @NonNull WallpaperBitmapLoader wallpaperBitmapLoader) {
    this.overrideStore = overrideStore;
    this.wallpaperBitmapLoader = wallpaperBitmapLoader;
  }

  void clearCache() {
    cachedPhotoBackgroundLayerTypes = null;
    cachedPhotoBackgroundLayerDrawables = null;
    cachedPhotoLayeredDrawable = null;
  }

  @NonNull
  Drawable applyPhotoOverlaysCached(
      @NonNull String themeId,
      @NonNull WallpaperBitmapDrawable baseDrawable,
      @Nullable Integer keyboardBackgroundTint,
      long wallpaperLastModified,
      int wallpaperBaseBucketPx) {
    final KeyboardWallpaperLayer[] stack = overrideStore.getBackgroundLayerStack(themeId);
    if (stack.length == 0) return baseDrawable;

    final Integer resolvedTint = resolveTintOverlayOrNull(keyboardBackgroundTint);

    boolean hasAnyOverlay = false;
    for (KeyboardWallpaperLayer layer : stack) {
      if (layer == null || !layer.enabled()) continue;
      final int opacityPercent = KeyboardWallpaperRenderMath.clampPercent(layer.opacityPercent());
      if (opacityPercent <= 0) continue;
      switch (layer.type()) {
        case KeyboardWallpaperLayer.TYPE_THEME_TINT:
          if (resolvedTint != null) hasAnyOverlay = true;
          break;
        case KeyboardWallpaperLayer.TYPE_SOLID_COLOR:
          final Integer argb = layer.argb();
          if (argb != null && ((argb >>> 24) != 0)) hasAnyOverlay = true;
          break;
        case KeyboardWallpaperLayer.TYPE_DOTS:
          final Integer dotsArgb = layer.argb();
          if (dotsArgb != null && ((dotsArgb >>> 24) != 0)) hasAnyOverlay = true;
          break;
        case KeyboardWallpaperLayer.TYPE_GRID:
          final Integer gridArgb = layer.argb();
          if (gridArgb != null && ((gridArgb >>> 24) != 0)) hasAnyOverlay = true;
          break;
        case KeyboardWallpaperLayer.TYPE_STRIPES:
          final Integer stripesArgb = layer.argb();
          if (stripesArgb != null && ((stripesArgb >>> 24) != 0)) hasAnyOverlay = true;
          break;
        case KeyboardWallpaperLayer.TYPE_DIAGONAL_STRIPES:
          final Integer diagonalStripesArgb = layer.argb();
          if (diagonalStripesArgb != null && ((diagonalStripesArgb >>> 24) != 0))
            hasAnyOverlay = true;
          break;
        case KeyboardWallpaperLayer.TYPE_TRIANGLES:
          final Integer trianglesArgb = layer.argb();
          if (trianglesArgb != null && ((trianglesArgb >>> 24) != 0)) hasAnyOverlay = true;
          break;
        case KeyboardWallpaperLayer.TYPE_HEX:
          final Integer hexArgb = layer.argb();
          if (hexArgb != null && ((hexArgb >>> 24) != 0)) hasAnyOverlay = true;
          break;
        case KeyboardWallpaperLayer.TYPE_CHECKER:
          final Integer checkerArgb = layer.argb();
          if (checkerArgb != null && ((checkerArgb >>> 24) != 0)) hasAnyOverlay = true;
          break;
        case KeyboardWallpaperLayer.TYPE_DIM:
        case KeyboardWallpaperLayer.TYPE_GRADIENT:
        case KeyboardWallpaperLayer.TYPE_VIGNETTE:
        case KeyboardWallpaperLayer.TYPE_GRAIN:
        case KeyboardWallpaperLayer.TYPE_BLUR:
          hasAnyOverlay = true;
          break;
        default:
          break;
      }
      if (hasAnyOverlay) break;
    }
    if (!hasAnyOverlay) return baseDrawable;

    final int[] desiredTypes = new int[stack.length];
    for (int i = 0; i < stack.length; i++) {
      final KeyboardWallpaperLayer layer = stack[i];
      desiredTypes[i] = layer != null ? layer.type() : -1;
    }

    if (cachedPhotoLayeredDrawable == null
        || cachedPhotoBackgroundLayerTypes == null
        || cachedPhotoBackgroundLayerDrawables == null
        || cachedPhotoBackgroundLayerDrawables.length != stack.length
        || !java.util.Arrays.equals(cachedPhotoBackgroundLayerTypes, desiredTypes)) {
      cachedPhotoBackgroundLayerTypes = desiredTypes;
      cachedPhotoBackgroundLayerDrawables = new Drawable[stack.length];
      final Drawable[] layers = new Drawable[stack.length + 1];
      layers[0] = baseDrawable;
      for (int i = 0; i < stack.length; i++) {
        final Drawable overlay = createBackgroundOverlayDrawable(stack[i]);
        cachedPhotoBackgroundLayerDrawables[i] = overlay;
        layers[i + 1] = overlay;
      }
      cachedPhotoLayeredDrawable = new LayerDrawable(layers);
    }

    final Drawable[] overlays = cachedPhotoBackgroundLayerDrawables;
    if (overlays != null) {
      final boolean hasBlurLayer = hasBackgroundBlurLayer(stack);
      final boolean allowBlur =
          hasBlurLayer
              && overrideStore.getWallpaperQuality(themeId)
                  != KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_LOW;
      final File wallpaperFile = hasBlurLayer ? overrideStore.getWallpaperFile(themeId) : null;
      final int rotationDegrees =
          hasBlurLayer ? overrideStore.getWallpaperRotationDegrees(themeId) : 0;
      final int scaleMode = hasBlurLayer ? overrideStore.getWallpaperScaleMode(themeId) : 0;
      final int anchor = hasBlurLayer ? overrideStore.getWallpaperAnchor(themeId) : 0;
      final ColorFilter photoColorFilter;
      if (hasBlurLayer) {
        final int saturationPercent = overrideStore.getSaturationPercent(themeId);
        final int contrastPercent = overrideStore.getContrastPercent(themeId);
        final int brightnessPercent = overrideStore.getBrightnessPercent(themeId);
        final int temperaturePercent = overrideStore.getTemperaturePercent(themeId);
        photoColorFilter =
            KeyboardWallpaperRenderMath.createPhotoColorFilter(
                saturationPercent, contrastPercent, brightnessPercent, temperaturePercent);
      } else {
        photoColorFilter = null;
      }

      for (int i = 0; i < overlays.length && i < stack.length; i++) {
        final KeyboardWallpaperLayer layer = stack[i];
        final Drawable overlay = overlays[i];
        if (layer == null || overlay == null) continue;
        if (hasBlurLayer
            && layer.type() == KeyboardWallpaperLayer.TYPE_BLUR
            && overlay instanceof BlurPhotoDrawable blurPhotoDrawable
            && wallpaperFile != null) {
          if (allowBlur) {
            applyBackgroundBlurLayerToDrawable(
                layer,
                blurPhotoDrawable,
                wallpaperFile,
                wallpaperLastModified,
                wallpaperBaseBucketPx,
                rotationDegrees,
                scaleMode,
                anchor,
                photoColorFilter);
          } else {
            blurPhotoDrawable.setAlpha(0);
          }
          continue;
        }
        applyBackgroundLayerToDrawable(layer, overlay, resolvedTint);
      }
    }

    return cachedPhotoLayeredDrawable != null ? cachedPhotoLayeredDrawable : baseDrawable;
  }

  private static boolean hasBackgroundBlurLayer(@NonNull KeyboardWallpaperLayer[] stack) {
    for (KeyboardWallpaperLayer layer : stack) {
      if (layer != null && layer.type() == KeyboardWallpaperLayer.TYPE_BLUR) return true;
    }
    return false;
  }

  @NonNull
  private static Drawable createBackgroundOverlayDrawable(@Nullable KeyboardWallpaperLayer layer) {
    if (layer == null) return new SolidOverlayDrawable(Color.TRANSPARENT);
    return switch (layer.type()) {
      case KeyboardWallpaperLayer.TYPE_THEME_TINT, KeyboardWallpaperLayer.TYPE_SOLID_COLOR ->
          new SolidOverlayDrawable(Color.TRANSPARENT);
      case KeyboardWallpaperLayer.TYPE_DIM -> new SolidOverlayDrawable(Color.BLACK);
      case KeyboardWallpaperLayer.TYPE_GRADIENT -> new GradientOverlayDrawable();
      case KeyboardWallpaperLayer.TYPE_VIGNETTE -> new VignetteDrawable();
      case KeyboardWallpaperLayer.TYPE_GRAIN -> new GrainDrawable();
      case KeyboardWallpaperLayer.TYPE_DOTS -> new DotsDrawable();
      case KeyboardWallpaperLayer.TYPE_GRID -> new GridDrawable();
      case KeyboardWallpaperLayer.TYPE_STRIPES -> new StripesDrawable();
      case KeyboardWallpaperLayer.TYPE_DIAGONAL_STRIPES -> new DiagonalStripesDrawable();
      case KeyboardWallpaperLayer.TYPE_TRIANGLES -> new TrianglesDrawable();
      case KeyboardWallpaperLayer.TYPE_HEX -> new HexDrawable();
      case KeyboardWallpaperLayer.TYPE_BLUR -> new BlurPhotoDrawable();
      case KeyboardWallpaperLayer.TYPE_CHECKER -> new CheckerDrawable();
      default -> new SolidOverlayDrawable(Color.TRANSPARENT);
    };
  }

  private void applyBackgroundBlurLayerToDrawable(
      @NonNull KeyboardWallpaperLayer layer,
      @NonNull BlurPhotoDrawable drawable,
      @NonNull File file,
      long lastModified,
      int baseBucketPx,
      int rotationDegrees,
      int scaleMode,
      int anchor,
      @Nullable ColorFilter photoColorFilter) {
    final int clampedOpacity = KeyboardWallpaperRenderMath.clampPercent(layer.opacityPercent());
    final int alpha = KeyboardWallpaperRenderMath.alphaForPercent(clampedOpacity);
    final boolean enabled = layer.enabled() && alpha > 0;
    final int blendMode = KeyboardWallpaperOverrideConstants.normalizeBlendMode(layer.blendMode());

    drawable.setBlendMode(blendMode);
    drawable.setTransform(rotationDegrees, scaleMode, anchor);
    drawable.setColorFilter(photoColorFilter);
    drawable.setAlpha(enabled ? alpha : 0);

    if (!enabled || baseBucketPx <= 0) return;

    drawable.setSource(
        wallpaperBitmapLoader, file, lastModified, baseBucketPx, layer.scalePercent());
  }

  private static void applyBackgroundLayerToDrawable(
      @NonNull KeyboardWallpaperLayer layer,
      @NonNull Drawable drawable,
      @Nullable Integer resolvedTint) {
    final int clampedOpacity = KeyboardWallpaperRenderMath.clampPercent(layer.opacityPercent());
    final int alpha = KeyboardWallpaperRenderMath.alphaForPercent(clampedOpacity);
    final boolean enabled = layer.enabled() && alpha > 0;
    final int blendMode = KeyboardWallpaperOverrideConstants.normalizeBlendMode(layer.blendMode());

    switch (layer.type()) {
      case KeyboardWallpaperLayer.TYPE_THEME_TINT:
        if (drawable instanceof SolidOverlayDrawable solid) {
          solid.setBlendMode(blendMode);
          solid.setColor(resolvedTint != null ? resolvedTint : Color.TRANSPARENT);
          solid.setAlpha(enabled && resolvedTint != null ? alpha : 0);
        }
        break;
      case KeyboardWallpaperLayer.TYPE_SOLID_COLOR:
        if (drawable instanceof SolidOverlayDrawable solid) {
          final Integer argb = layer.argb();
          solid.setBlendMode(blendMode);
          solid.setColor(argb != null ? argb : Color.TRANSPARENT);
          solid.setAlpha(enabled && argb != null ? alpha : 0);
        }
        break;
      case KeyboardWallpaperLayer.TYPE_DIM:
        if (drawable instanceof SolidOverlayDrawable solid) {
          solid.setBlendMode(blendMode);
          solid.setAlpha(enabled ? alpha : 0);
        }
        break;
      case KeyboardWallpaperLayer.TYPE_GRADIENT:
        if (drawable instanceof GradientOverlayDrawable gradient) {
          gradient.setBlendMode(blendMode);
          gradient.setStops(layer.gradientStops());
          gradient.setColors(layer.argb2(), layer.argb());
          gradient.setDirection(layer.direction());
          gradient.setStrengthPercent(enabled ? clampedOpacity : 0);
        }
        break;
      case KeyboardWallpaperLayer.TYPE_VIGNETTE:
        if (drawable instanceof VignetteDrawable vignette) {
          vignette.setBlendMode(blendMode);
          vignette.setColor(layer.argb());
          vignette.setStrengthPercent(enabled ? clampedOpacity : 0);
        }
        break;
      case KeyboardWallpaperLayer.TYPE_GRAIN:
        if (drawable instanceof GrainDrawable grain) {
          grain.setBlendMode(blendMode);
          grain.setScalePercent(layer.scalePercent());
          grain.setStrengthPercent(enabled ? clampedOpacity : 0);
        }
        break;
      case KeyboardWallpaperLayer.TYPE_DOTS:
        if (drawable instanceof DotsDrawable dots) {
          final Integer argb = layer.argb();
          dots.setBlendMode(blendMode);
          dots.setScalePercent(layer.scalePercent());
          dots.setColor(argb);
          dots.setAlpha(enabled && argb != null ? alpha : 0);
        }
        break;
      case KeyboardWallpaperLayer.TYPE_GRID:
        if (drawable instanceof GridDrawable grid) {
          final Integer argb = layer.argb();
          grid.setBlendMode(blendMode);
          grid.setScalePercent(layer.scalePercent());
          grid.setColor(argb);
          grid.setAlpha(enabled && argb != null ? alpha : 0);
        }
        break;
      case KeyboardWallpaperLayer.TYPE_STRIPES:
        if (drawable instanceof StripesDrawable stripes) {
          final Integer argb = layer.argb();
          stripes.setBlendMode(blendMode);
          stripes.setScalePercent(layer.scalePercent());
          stripes.setDirection(layer.direction());
          stripes.setColor(argb);
          stripes.setAlpha(enabled && argb != null ? alpha : 0);
        }
        break;
      case KeyboardWallpaperLayer.TYPE_DIAGONAL_STRIPES:
        if (drawable instanceof DiagonalStripesDrawable stripes) {
          final Integer argb = layer.argb();
          stripes.setBlendMode(blendMode);
          stripes.setScalePercent(layer.scalePercent());
          stripes.setColor(argb);
          stripes.setAlpha(enabled && argb != null ? alpha : 0);
        }
        break;
      case KeyboardWallpaperLayer.TYPE_TRIANGLES:
        if (drawable instanceof TrianglesDrawable triangles) {
          final Integer argb = layer.argb();
          triangles.setBlendMode(blendMode);
          triangles.setScalePercent(layer.scalePercent());
          triangles.setColor(argb);
          triangles.setAlpha(enabled && argb != null ? alpha : 0);
        }
        break;
      case KeyboardWallpaperLayer.TYPE_HEX:
        if (drawable instanceof HexDrawable hex) {
          final Integer argb = layer.argb();
          hex.setBlendMode(blendMode);
          hex.setScalePercent(layer.scalePercent());
          hex.setColor(argb);
          hex.setAlpha(enabled && argb != null ? alpha : 0);
        }
        break;
      case KeyboardWallpaperLayer.TYPE_CHECKER:
        if (drawable instanceof CheckerDrawable checker) {
          final Integer argb = layer.argb();
          final Integer argb2 = layer.argb2();
          checker.setBlendMode(blendMode);
          checker.setScalePercent(layer.scalePercent());
          checker.setColors(argb, argb2);
          checker.setAlpha(enabled && (argb != null || argb2 != null) ? alpha : 0);
        }
        break;
      default:
        break;
    }
  }

  @Nullable
  private static Integer resolveTintOverlayOrNull(@Nullable Integer color) {
    if (color == null) return null;
    return ((color >>> 24) != 0) ? color : null;
  }
}
