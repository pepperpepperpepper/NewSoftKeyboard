package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideConstants;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;

final class KeyboardThemeCustomizationBackgroundPreferencesUi {

  @FunctionalInterface
  interface ApplyToAllHandler {
    void apply(@NonNull Context context, @NonNull String sourceThemeId);
  }

  @NonNull private final KeyboardWallpaperOverrideStore wallpaperStore;
  @NonNull private final KeyboardThemeCustomizationWallpaperPhotoUi wallpaperPhotoUi;
  @NonNull private final KeyboardThemeCustomizationWallpaperKeyOverlayUi keyOverlayUi;

  KeyboardThemeCustomizationBackgroundPreferencesUi(
      @NonNull KeyboardThemeCustomizationBackgroundSection.Host host,
      @NonNull KeyboardWallpaperOverrideStore wallpaperStore,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore) {
    this.wallpaperStore = wallpaperStore;
    this.wallpaperPhotoUi = new KeyboardThemeCustomizationWallpaperPhotoUi(host, wallpaperStore);
    this.keyOverlayUi =
        new KeyboardThemeCustomizationWallpaperKeyOverlayUi(
            host, wallpaperStore, themeOverridesStore);
  }

  void addPreferences(
      @NonNull Context context,
      @NonNull PreferenceScreen screen,
      @NonNull ApplyToAllHandler applyToAllHandler) {
    final PreferenceCategory background = new PreferenceCategory(context);
    background.setKey("section:background");
    background.setTitle(R.string.keyboard_theme_appearance_background_title);
    screen.addPreference(background);

    wallpaperPhotoUi.addPreferences(context, background);
    keyOverlayUi.addPreferences(context, background, applyToAllHandler);
  }

  void setWallpaperImportInProgressUi() {
    wallpaperPhotoUi.setWallpaperImportInProgressUi();
    keyOverlayUi.setWallpaperImportInProgressUi();
  }

  void refreshState(@NonNull String themeId, boolean importInProgress) {
    wallpaperPhotoUi.refreshState(themeId, importInProgress);
    keyOverlayUi.refreshState(themeId, importInProgress, hasAnyWallpaperOverride(themeId));
  }

  boolean hasAnyWallpaperOverride(@NonNull String themeId) {
    if (wallpaperStore.hasWallpaper(themeId) || wallpaperStore.isWallpaperInvalid(themeId))
      return true;

    if (wallpaperStore.hasBackgroundLayerStackOverride(themeId)) return true;
    if (wallpaperStore.hasKeyLayerStackOverride(themeId)) return true;

    if (wallpaperStore.getDimPercent(themeId) > 0) return true;
    if (wallpaperStore.getGradientPercent(themeId) > 0) return true;
    if (wallpaperStore.getVignettePercent(themeId) > 0) return true;
    if (wallpaperStore.getGrainPercent(themeId) > 0) return true;
    if (wallpaperStore.getSaturationPercent(themeId) != 100) return true;
    if (wallpaperStore.getContrastPercent(themeId) != 100) return true;

    if (wallpaperStore.getWallpaperRotationDegrees(themeId) != 0) return true;
    if (wallpaperStore.getWallpaperScaleMode(themeId)
        != KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_CROP) return true;
    if (wallpaperStore.getWallpaperAnchor(themeId)
        != KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_CENTER) return true;
    if (wallpaperStore.getWallpaperMode(themeId)
        != KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY) return true;
    if (wallpaperStore.getKeyAlphaPercent(themeId)
        != KeyboardWallpaperOverrideConstants.DEFAULT_KEY_ALPHA_PERCENT) return true;
    if (wallpaperStore.getKeyBlendMode(themeId)
        != KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL) return true;
    if (wallpaperStore.hasSpecialKeyAlphaPercentOverride(themeId)) return true;
    if (wallpaperStore.hasSpacebarAlphaPercentOverride(themeId)) return true;
    if (wallpaperStore.hasModifierKeyAlphaPercentOverride(themeId)) return true;
    if (wallpaperStore.hasEnterKeyAlphaPercentOverride(themeId)) return true;
    if (wallpaperStore.isMatchKeyShapeEnabled(themeId)) return true;
    if (wallpaperStore.getWallpaperQuality(themeId)
        != KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_BALANCED) return true;

    if (wallpaperStore.hasKeyColorWashColorOverride(themeId)) return true;
    if (wallpaperStore.getKeyColorWashBlendMode(themeId)
        != KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL) return true;
    if (wallpaperStore.getKeyHighlightPercent(themeId) > 0) return true;
    if (wallpaperStore.getKeyHighlightBlendMode(themeId)
        != KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL) return true;

    if (wallpaperStore.getKeyGradientBlendMode(themeId)
        != KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL) return true;

    if (wallpaperStore.getKeyVignetteBlendMode(themeId)
        != KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL) return true;
    if (wallpaperStore.getKeyGrainBlendMode(themeId)
        != KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL) return true;
    if (wallpaperStore.getBackgroundTintBlendMode(themeId)
        != KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL) return true;
    if (wallpaperStore.getBackgroundDimBlendMode(themeId)
        != KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL) return true;
    if (wallpaperStore.getBackgroundGradientBlendMode(themeId)
        != KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL) return true;
    if (wallpaperStore.getBackgroundVignetteBlendMode(themeId)
        != KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL) return true;
    if (wallpaperStore.getBackgroundGrainBlendMode(themeId)
        != KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL) return true;

    if (wallpaperStore.hasKeyLayerOrderOverride(themeId)) return true;
    return wallpaperStore.hasBackgroundLayerOrderOverride(themeId);
  }

  void showPickFailedDialog(@NonNull Context context, @NonNull Throwable error) {
    wallpaperPhotoUi.showPickFailedDialog(context, error);
  }

  void dispose() {
    wallpaperPhotoUi.dispose();
    keyOverlayUi.dispose();
  }
}
