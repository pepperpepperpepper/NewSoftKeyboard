package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

/** Stores user-selected keyboard wallpaper overrides per theme id. */
public class KeyboardWallpaperOverrideStore {

  public static final int WALLPAPER_MODE_BACKGROUND_ONLY =
      KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY;
  public static final int WALLPAPER_MODE_BACKGROUND_KEY_TINT =
      KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_KEY_TINT;
  public static final int WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE =
      KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE;

  public static final int WALLPAPER_BLEND_MODE_NORMAL =
      KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL;
  public static final int WALLPAPER_BLEND_MODE_MULTIPLY =
      KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_MULTIPLY;
  public static final int WALLPAPER_BLEND_MODE_SCREEN =
      KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_SCREEN;
  public static final int WALLPAPER_BLEND_MODE_OVERLAY =
      KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_OVERLAY;
  public static final int WALLPAPER_BLEND_MODE_SOFT_LIGHT =
      KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_SOFT_LIGHT;

  public static final int KEY_LAYER_COLOR_WASH =
      KeyboardWallpaperOverrideConstants.KEY_LAYER_COLOR_WASH;
  public static final int KEY_LAYER_HIGHLIGHT =
      KeyboardWallpaperOverrideConstants.KEY_LAYER_HIGHLIGHT;
  public static final int KEY_LAYER_GRADIENT =
      KeyboardWallpaperOverrideConstants.KEY_LAYER_GRADIENT;
  public static final int KEY_LAYER_VIGNETTE =
      KeyboardWallpaperOverrideConstants.KEY_LAYER_VIGNETTE;
  public static final int KEY_LAYER_GRAIN = KeyboardWallpaperOverrideConstants.KEY_LAYER_GRAIN;

  public static final int BACKGROUND_LAYER_TINT =
      KeyboardWallpaperOverrideConstants.BACKGROUND_LAYER_TINT;
  public static final int BACKGROUND_LAYER_DIM =
      KeyboardWallpaperOverrideConstants.BACKGROUND_LAYER_DIM;
  public static final int BACKGROUND_LAYER_GRADIENT =
      KeyboardWallpaperOverrideConstants.BACKGROUND_LAYER_GRADIENT;
  public static final int BACKGROUND_LAYER_VIGNETTE =
      KeyboardWallpaperOverrideConstants.BACKGROUND_LAYER_VIGNETTE;
  public static final int BACKGROUND_LAYER_GRAIN =
      KeyboardWallpaperOverrideConstants.BACKGROUND_LAYER_GRAIN;

  public static final int WALLPAPER_QUALITY_LOW =
      KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_LOW;
  public static final int WALLPAPER_QUALITY_BALANCED =
      KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_BALANCED;
  public static final int WALLPAPER_QUALITY_HIGH =
      KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_HIGH;

  public static final int WALLPAPER_SCALE_MODE_CROP =
      KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_CROP;
  public static final int WALLPAPER_SCALE_MODE_FIT =
      KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_FIT;
  public static final int WALLPAPER_SCALE_MODE_STRETCH =
      KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_STRETCH;
  public static final int WALLPAPER_SCALE_MODE_TILE =
      KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_TILE;
  public static final int WALLPAPER_SCALE_MODE_MIRROR =
      KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_MIRROR;

  public static final int WALLPAPER_ANCHOR_TOP_LEFT =
      KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_TOP_LEFT;
  public static final int WALLPAPER_ANCHOR_TOP =
      KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_TOP;
  public static final int WALLPAPER_ANCHOR_TOP_RIGHT =
      KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_TOP_RIGHT;
  public static final int WALLPAPER_ANCHOR_LEFT =
      KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_LEFT;
  public static final int WALLPAPER_ANCHOR_CENTER =
      KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_CENTER;
  public static final int WALLPAPER_ANCHOR_RIGHT =
      KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_RIGHT;
  public static final int WALLPAPER_ANCHOR_BOTTOM_LEFT =
      KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_BOTTOM_LEFT;
  public static final int WALLPAPER_ANCHOR_BOTTOM =
      KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_BOTTOM;
  public static final int WALLPAPER_ANCHOR_BOTTOM_RIGHT =
      KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_BOTTOM_RIGHT;

  public static final int DEFAULT_KEY_ALPHA_PERCENT =
      KeyboardWallpaperOverrideConstants.DEFAULT_KEY_ALPHA_PERCENT;

  public static int normalizeBlendMode(int blendMode) {
    return KeyboardWallpaperOverrideConstants.normalizeBlendMode(blendMode);
  }

  public static int normalizeRotationDegrees(int rotationDegrees) {
    return KeyboardWallpaperOverrideConstants.normalizeRotationDegrees(rotationDegrees);
  }

  private final KeyboardWallpaperFileStore fileStore;
  private final KeyboardWallpaperOverridePrefs prefsDelegate;

  public KeyboardWallpaperOverrideStore(@NonNull Context context) {
    final Context appContext = context.getApplicationContext();
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(appContext);
    fileStore = new KeyboardWallpaperFileStore(appContext);
    prefsDelegate = new KeyboardWallpaperOverridePrefs(prefs, fileStore);
  }

  public int getDimPercent(@NonNull String themeId) {
    return prefsDelegate.getDimPercent(themeId);
  }

  public void setDimPercent(@NonNull String themeId, int dimPercent) {
    prefsDelegate.setDimPercent(themeId, dimPercent);
  }

  public int getWallpaperMode(@NonNull String themeId) {
    return prefsDelegate.getWallpaperMode(themeId);
  }

  public void setWallpaperMode(@NonNull String themeId, int mode) {
    prefsDelegate.setWallpaperMode(themeId, mode);
  }

  public int getKeyAlphaPercent(@NonNull String themeId) {
    return prefsDelegate.getKeyAlphaPercent(themeId);
  }

  public void setKeyAlphaPercent(@NonNull String themeId, int alphaPercent) {
    prefsDelegate.setKeyAlphaPercent(themeId, alphaPercent);
  }

  public int getKeyBlendMode(@NonNull String themeId) {
    return prefsDelegate.getKeyBlendMode(themeId);
  }

  public void setKeyBlendMode(@NonNull String themeId, int blendMode) {
    prefsDelegate.setKeyBlendMode(themeId, blendMode);
  }

  public boolean hasKeyLayerStackOverride(@NonNull String themeId) {
    return prefsDelegate.hasKeyLayerStackOverride(themeId);
  }

  @NonNull
  public KeyboardWallpaperLayer[] getKeyLayerStack(@NonNull String themeId) {
    return prefsDelegate.getKeyLayerStack(themeId);
  }

  public void setKeyLayerStack(@NonNull String themeId, @NonNull KeyboardWallpaperLayer[] layers) {
    prefsDelegate.setKeyLayerStack(themeId, layers);
  }

  public void clearKeyLayerStack(@NonNull String themeId) {
    prefsDelegate.clearKeyLayerStack(themeId);
  }

  @NonNull
  public int[] getKeyLayerOrder(@NonNull String themeId) {
    return prefsDelegate.getKeyLayerOrder(themeId);
  }

  public boolean hasKeyLayerOrderOverride(@NonNull String themeId) {
    return prefsDelegate.hasKeyLayerOrderOverride(themeId);
  }

  public void setKeyLayerOrder(@NonNull String themeId, @NonNull int[] layerOrder) {
    prefsDelegate.setKeyLayerOrder(themeId, layerOrder);
  }

  public void clearKeyLayerOrder(@NonNull String themeId) {
    prefsDelegate.clearKeyLayerOrder(themeId);
  }

  public boolean hasKeyColorWashColorOverride(@NonNull String themeId) {
    return prefsDelegate.hasKeyColorWashColorOverride(themeId);
  }

  @Nullable
  public Integer getKeyColorWashColor(@NonNull String themeId) {
    return prefsDelegate.getKeyColorWashColor(themeId);
  }

  public void setKeyColorWashColor(@NonNull String themeId, @NonNull Integer argbColor) {
    prefsDelegate.setKeyColorWashColor(themeId, argbColor);
  }

  public void clearKeyColorWashColor(@NonNull String themeId) {
    prefsDelegate.clearKeyColorWashColor(themeId);
  }

  public int getKeyColorWashBlendMode(@NonNull String themeId) {
    return prefsDelegate.getKeyColorWashBlendMode(themeId);
  }

  public void setKeyColorWashBlendMode(@NonNull String themeId, int blendMode) {
    prefsDelegate.setKeyColorWashBlendMode(themeId, blendMode);
  }

  public int getKeyHighlightPercent(@NonNull String themeId) {
    return prefsDelegate.getKeyHighlightPercent(themeId);
  }

  public void setKeyHighlightPercent(@NonNull String themeId, int highlightPercent) {
    prefsDelegate.setKeyHighlightPercent(themeId, highlightPercent);
  }

  public int getKeyHighlightBlendMode(@NonNull String themeId) {
    return prefsDelegate.getKeyHighlightBlendMode(themeId);
  }

  public void setKeyHighlightBlendMode(@NonNull String themeId, int blendMode) {
    prefsDelegate.setKeyHighlightBlendMode(themeId, blendMode);
  }

  public int getKeyGradientBlendMode(@NonNull String themeId) {
    return prefsDelegate.getKeyGradientBlendMode(themeId);
  }

  public void setKeyGradientBlendMode(@NonNull String themeId, int blendMode) {
    prefsDelegate.setKeyGradientBlendMode(themeId, blendMode);
  }

  public int getKeyVignetteBlendMode(@NonNull String themeId) {
    return prefsDelegate.getKeyVignetteBlendMode(themeId);
  }

  public void setKeyVignetteBlendMode(@NonNull String themeId, int blendMode) {
    prefsDelegate.setKeyVignetteBlendMode(themeId, blendMode);
  }

  public int getKeyGrainBlendMode(@NonNull String themeId) {
    return prefsDelegate.getKeyGrainBlendMode(themeId);
  }

  public void setKeyGrainBlendMode(@NonNull String themeId, int blendMode) {
    prefsDelegate.setKeyGrainBlendMode(themeId, blendMode);
  }

  @NonNull
  public int[] getBackgroundLayerOrder(@NonNull String themeId) {
    return prefsDelegate.getBackgroundLayerOrder(themeId);
  }

  public boolean hasBackgroundLayerOrderOverride(@NonNull String themeId) {
    return prefsDelegate.hasBackgroundLayerOrderOverride(themeId);
  }

  public boolean hasBackgroundLayerStackOverride(@NonNull String themeId) {
    return prefsDelegate.hasBackgroundLayerStackOverride(themeId);
  }

  @NonNull
  public KeyboardWallpaperLayer[] getBackgroundLayerStack(@NonNull String themeId) {
    return prefsDelegate.getBackgroundLayerStack(themeId);
  }

  public void setBackgroundLayerStack(
      @NonNull String themeId, @NonNull KeyboardWallpaperLayer[] layers) {
    prefsDelegate.setBackgroundLayerStack(themeId, layers);
  }

  public void clearBackgroundLayerStack(@NonNull String themeId) {
    prefsDelegate.clearBackgroundLayerStack(themeId);
  }

  public void setBackgroundLayerOrder(@NonNull String themeId, @NonNull int[] layerOrder) {
    prefsDelegate.setBackgroundLayerOrder(themeId, layerOrder);
  }

  public void clearBackgroundLayerOrder(@NonNull String themeId) {
    prefsDelegate.clearBackgroundLayerOrder(themeId);
  }

  public int getBackgroundTintBlendMode(@NonNull String themeId) {
    return prefsDelegate.getBackgroundTintBlendMode(themeId);
  }

  public void setBackgroundTintBlendMode(@NonNull String themeId, int blendMode) {
    prefsDelegate.setBackgroundTintBlendMode(themeId, blendMode);
  }

  public int getBackgroundDimBlendMode(@NonNull String themeId) {
    return prefsDelegate.getBackgroundDimBlendMode(themeId);
  }

  public void setBackgroundDimBlendMode(@NonNull String themeId, int blendMode) {
    prefsDelegate.setBackgroundDimBlendMode(themeId, blendMode);
  }

  public int getBackgroundGradientBlendMode(@NonNull String themeId) {
    return prefsDelegate.getBackgroundGradientBlendMode(themeId);
  }

  public void setBackgroundGradientBlendMode(@NonNull String themeId, int blendMode) {
    prefsDelegate.setBackgroundGradientBlendMode(themeId, blendMode);
  }

  public int getBackgroundVignetteBlendMode(@NonNull String themeId) {
    return prefsDelegate.getBackgroundVignetteBlendMode(themeId);
  }

  public void setBackgroundVignetteBlendMode(@NonNull String themeId, int blendMode) {
    prefsDelegate.setBackgroundVignetteBlendMode(themeId, blendMode);
  }

  public int getBackgroundGrainBlendMode(@NonNull String themeId) {
    return prefsDelegate.getBackgroundGrainBlendMode(themeId);
  }

  public void setBackgroundGrainBlendMode(@NonNull String themeId, int blendMode) {
    prefsDelegate.setBackgroundGrainBlendMode(themeId, blendMode);
  }

  public boolean hasSpecialKeyAlphaPercentOverride(@NonNull String themeId) {
    return prefsDelegate.hasSpecialKeyAlphaPercentOverride(themeId);
  }

  public int getSpecialKeyAlphaPercent(@NonNull String themeId) {
    return prefsDelegate.getSpecialKeyAlphaPercent(themeId);
  }

  public void setSpecialKeyAlphaPercent(@NonNull String themeId, int alphaPercent) {
    prefsDelegate.setSpecialKeyAlphaPercent(themeId, alphaPercent);
  }

  public void clearSpecialKeyAlphaPercent(@NonNull String themeId) {
    prefsDelegate.clearSpecialKeyAlphaPercent(themeId);
  }

  public boolean hasSpacebarAlphaPercentOverride(@NonNull String themeId) {
    return prefsDelegate.hasSpacebarAlphaPercentOverride(themeId);
  }

  public int getSpacebarAlphaPercent(@NonNull String themeId) {
    return prefsDelegate.getSpacebarAlphaPercent(themeId);
  }

  public void setSpacebarAlphaPercent(@NonNull String themeId, int alphaPercent) {
    prefsDelegate.setSpacebarAlphaPercent(themeId, alphaPercent);
  }

  public void clearSpacebarAlphaPercent(@NonNull String themeId) {
    prefsDelegate.clearSpacebarAlphaPercent(themeId);
  }

  public boolean hasModifierKeyAlphaPercentOverride(@NonNull String themeId) {
    return prefsDelegate.hasModifierKeyAlphaPercentOverride(themeId);
  }

  public int getModifierKeyAlphaPercent(@NonNull String themeId) {
    return prefsDelegate.getModifierKeyAlphaPercent(themeId);
  }

  public void setModifierKeyAlphaPercent(@NonNull String themeId, int alphaPercent) {
    prefsDelegate.setModifierKeyAlphaPercent(themeId, alphaPercent);
  }

  public void clearModifierKeyAlphaPercent(@NonNull String themeId) {
    prefsDelegate.clearModifierKeyAlphaPercent(themeId);
  }

  public boolean hasEnterKeyAlphaPercentOverride(@NonNull String themeId) {
    return prefsDelegate.hasEnterKeyAlphaPercentOverride(themeId);
  }

  public int getEnterKeyAlphaPercent(@NonNull String themeId) {
    return prefsDelegate.getEnterKeyAlphaPercent(themeId);
  }

  public void setEnterKeyAlphaPercent(@NonNull String themeId, int alphaPercent) {
    prefsDelegate.setEnterKeyAlphaPercent(themeId, alphaPercent);
  }

  public void clearEnterKeyAlphaPercent(@NonNull String themeId) {
    prefsDelegate.clearEnterKeyAlphaPercent(themeId);
  }

  public int getWallpaperRotationDegrees(@NonNull String themeId) {
    return prefsDelegate.getWallpaperRotationDegrees(themeId);
  }

  public void setWallpaperRotationDegrees(@NonNull String themeId, int rotationDegrees) {
    prefsDelegate.setWallpaperRotationDegrees(themeId, rotationDegrees);
  }

  public void rotateWallpaperClockwise90(@NonNull String themeId) {
    final int current = getWallpaperRotationDegrees(themeId);
    final int next = (current + 90) % 360;
    setWallpaperRotationDegrees(themeId, next);
  }

  public int getWallpaperScaleMode(@NonNull String themeId) {
    return prefsDelegate.getWallpaperScaleMode(themeId);
  }

  public void setWallpaperScaleMode(@NonNull String themeId, int scaleMode) {
    prefsDelegate.setWallpaperScaleMode(themeId, scaleMode);
  }

  public int getWallpaperAnchor(@NonNull String themeId) {
    return prefsDelegate.getWallpaperAnchor(themeId);
  }

  public void setWallpaperAnchor(@NonNull String themeId, int anchor) {
    prefsDelegate.setWallpaperAnchor(themeId, anchor);
  }

  public boolean isMatchKeyShapeEnabled(@NonNull String themeId) {
    return prefsDelegate.isMatchKeyShapeEnabled(themeId);
  }

  public void setMatchKeyShapeEnabled(@NonNull String themeId, boolean enabled) {
    prefsDelegate.setMatchKeyShapeEnabled(themeId, enabled);
  }

  public int getWallpaperQuality(@NonNull String themeId) {
    return prefsDelegate.getWallpaperQuality(themeId);
  }

  public void setWallpaperQuality(@NonNull String themeId, int quality) {
    prefsDelegate.setWallpaperQuality(themeId, quality);
  }

  public int getVignettePercent(@NonNull String themeId) {
    return prefsDelegate.getVignettePercent(themeId);
  }

  public void setVignettePercent(@NonNull String themeId, int vignettePercent) {
    prefsDelegate.setVignettePercent(themeId, vignettePercent);
  }

  public int getGradientPercent(@NonNull String themeId) {
    return prefsDelegate.getGradientPercent(themeId);
  }

  public void setGradientPercent(@NonNull String themeId, int gradientPercent) {
    prefsDelegate.setGradientPercent(themeId, gradientPercent);
  }

  public int getGrainPercent(@NonNull String themeId) {
    return prefsDelegate.getGrainPercent(themeId);
  }

  public void setGrainPercent(@NonNull String themeId, int grainPercent) {
    prefsDelegate.setGrainPercent(themeId, grainPercent);
  }

  public int getSaturationPercent(@NonNull String themeId) {
    return prefsDelegate.getSaturationPercent(themeId);
  }

  public void setSaturationPercent(@NonNull String themeId, int saturationPercent) {
    prefsDelegate.setSaturationPercent(themeId, saturationPercent);
  }

  public int getContrastPercent(@NonNull String themeId) {
    return prefsDelegate.getContrastPercent(themeId);
  }

  public void setContrastPercent(@NonNull String themeId, int contrastPercent) {
    prefsDelegate.setContrastPercent(themeId, contrastPercent);
  }

  public int getBrightnessPercent(@NonNull String themeId) {
    return prefsDelegate.getBrightnessPercent(themeId);
  }

  public void setBrightnessPercent(@NonNull String themeId, int brightnessPercent) {
    prefsDelegate.setBrightnessPercent(themeId, brightnessPercent);
  }

  public int getTemperaturePercent(@NonNull String themeId) {
    return prefsDelegate.getTemperaturePercent(themeId);
  }

  public void setTemperaturePercent(@NonNull String themeId, int temperaturePercent) {
    prefsDelegate.setTemperaturePercent(themeId, temperaturePercent);
  }

  public boolean isWallpaperInvalid(@NonNull String themeId) {
    return prefsDelegate.isWallpaperInvalid(themeId);
  }

  public void markWallpaperInvalid(@NonNull String themeId) {
    prefsDelegate.markWallpaperInvalid(themeId);
  }

  public int getWallpaperChangeToken(@NonNull String themeId) {
    return prefsDelegate.getWallpaperChangeToken(themeId);
  }

  public boolean isHighQualityImportEnabled() {
    return prefsDelegate.isHighQualityImportEnabled();
  }

  public void setHighQualityImportEnabled(boolean enabled) {
    prefsDelegate.setHighQualityImportEnabled(enabled);
  }

  public void clear(@NonNull String themeId) {
    fileStore.deleteWallpaperBestEffort(themeId);
    prefsDelegate.clearPrefs(themeId);
  }

  /** Deletes the stored wallpaper bitmap but keeps other per-preset overrides. */
  public void clearWallpaperFile(@NonNull String themeId) {
    fileStore.deleteWallpaperBestEffort(themeId);
    prefsDelegate.onWallpaperFileDeleted(themeId);
  }

  public void copyToTheme(@NonNull String sourceThemeId, @NonNull String targetThemeId)
      throws IOException {
    if (sourceThemeId.equals(targetThemeId)) return;
    fileStore.copyToTheme(sourceThemeId, targetThemeId);
    prefsDelegate.copyToTheme(sourceThemeId, targetThemeId);
  }

  @NonNull
  public File getWallpaperFile(@NonNull String themeId) {
    return fileStore.getWallpaperFile(themeId);
  }

  public boolean hasWallpaper(@NonNull String themeId) {
    return fileStore.hasWallpaper(themeId);
  }

  /**
   * Reads the image from the given {@link Uri}, scales it down, and stores it as an app-private
   * processed copy.
   */
  public void importFromUri(@NonNull String themeId, @NonNull Uri sourceUri) throws IOException {
    importFromUri(themeId, sourceUri, 2048, 2048);
  }

  /**
   * Reads the image from the given {@link Uri}, scales it down, and stores it as an app-private
   * processed copy.
   *
   * @param maxWidth The maximum width of the stored bitmap.
   * @param maxHeight The maximum height of the stored bitmap.
   */
  public void importFromUri(
      @NonNull String themeId, @NonNull Uri sourceUri, int maxWidth, int maxHeight)
      throws IOException {
    final KeyboardWallpaperFileStore.ImportResult result =
        fileStore.importFromUri(
            themeId, sourceUri, maxWidth, maxHeight, prefsDelegate.isHighQualityImportEnabled());
    prefsDelegate.onWallpaperImported(
        themeId, result.hadExistingWallpaper, result.exifRotationDegrees);
  }
}
