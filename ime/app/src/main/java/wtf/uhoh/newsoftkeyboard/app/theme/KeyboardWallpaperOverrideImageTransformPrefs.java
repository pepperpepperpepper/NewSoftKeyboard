package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;

abstract class KeyboardWallpaperOverrideImageTransformPrefs {

  private static final String PREF_CHANGE_PREFIX = "photo_wallpaper_change::";
  static final String PREF_INVALID_PREFIX = "photo_wallpaper_invalid::";

  static final String PREF_ROTATION_PREFIX = "photo_wallpaper_rotation::";
  static final String PREF_SCALE_MODE_PREFIX = "photo_wallpaper_scale_mode::";
  static final String PREF_ANCHOR_PREFIX = "photo_wallpaper_anchor::";
  static final String PREF_MATCH_KEY_SHAPE_PREFIX = "photo_wallpaper_match_key_shape::";
  static final String PREF_VIGNETTE_PREFIX = "photo_wallpaper_vignette::";
  static final String PREF_GRADIENT_PREFIX = "photo_wallpaper_gradient::";
  static final String PREF_GRAIN_PREFIX = "photo_wallpaper_grain::";
  static final String PREF_SATURATION_PREFIX = "photo_wallpaper_saturation::";
  static final String PREF_CONTRAST_PREFIX = "photo_wallpaper_contrast::";
  static final String PREF_BRIGHTNESS_PREFIX = "photo_wallpaper_brightness::";
  static final String PREF_TEMPERATURE_PREFIX = "photo_wallpaper_temperature::";
  static final String PREF_QUALITY_PREFIX = "photo_wallpaper_quality::";
  static final String PREF_IMPORT_HIGH_QUALITY = "photo_wallpaper_import_high_quality";

  final SharedPreferences prefs;

  KeyboardWallpaperOverrideImageTransformPrefs(@NonNull SharedPreferences prefs) {
    this.prefs = prefs;
  }

  @NonNull
  static String changeKey(@NonNull String themeId) {
    return PREF_CHANGE_PREFIX + themeId;
  }

  @NonNull
  static String invalidKey(@NonNull String themeId) {
    return PREF_INVALID_PREFIX + themeId;
  }

  @NonNull
  static String rotationKey(@NonNull String themeId) {
    return PREF_ROTATION_PREFIX + themeId;
  }

  @NonNull
  static String scaleModeKey(@NonNull String themeId) {
    return PREF_SCALE_MODE_PREFIX + themeId;
  }

  @NonNull
  static String anchorKey(@NonNull String themeId) {
    return PREF_ANCHOR_PREFIX + themeId;
  }

  @NonNull
  static String matchKeyShapeKey(@NonNull String themeId) {
    return PREF_MATCH_KEY_SHAPE_PREFIX + themeId;
  }

  @NonNull
  static String qualityKey(@NonNull String themeId) {
    return PREF_QUALITY_PREFIX + themeId;
  }

  @NonNull
  static String vignetteKey(@NonNull String themeId) {
    return PREF_VIGNETTE_PREFIX + themeId;
  }

  @NonNull
  static String gradientKey(@NonNull String themeId) {
    return PREF_GRADIENT_PREFIX + themeId;
  }

  @NonNull
  static String grainKey(@NonNull String themeId) {
    return PREF_GRAIN_PREFIX + themeId;
  }

  @NonNull
  static String saturationKey(@NonNull String themeId) {
    return PREF_SATURATION_PREFIX + themeId;
  }

  @NonNull
  static String contrastKey(@NonNull String themeId) {
    return PREF_CONTRAST_PREFIX + themeId;
  }

  @NonNull
  static String brightnessKey(@NonNull String themeId) {
    return PREF_BRIGHTNESS_PREFIX + themeId;
  }

  @NonNull
  static String temperatureKey(@NonNull String themeId) {
    return PREF_TEMPERATURE_PREFIX + themeId;
  }

  final void markWallpaperChanged(
      @NonNull String themeId, @NonNull SharedPreferences.Editor editor) {
    final String key = changeKey(themeId);
    final int current = prefs.getInt(key, 0);
    editor.putInt(key, current + 1);
  }

  static int clampPercent(int value) {
    return Math.max(0, Math.min(100, value));
  }

  int getWallpaperRotationDegrees(@NonNull String themeId) {
    return KeyboardWallpaperOverrideConstants.normalizeRotationDegrees(
        prefs.getInt(rotationKey(themeId), 0));
  }

  void setWallpaperRotationDegrees(@NonNull String themeId, int rotationDegrees) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(
        rotationKey(themeId),
        KeyboardWallpaperOverrideConstants.normalizeRotationDegrees(rotationDegrees));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getWallpaperScaleMode(@NonNull String themeId) {
    return normalizeScaleMode(
        prefs.getInt(
            scaleModeKey(themeId), KeyboardWallpaperOverrideConstants.WALLPAPER_SCALE_MODE_CROP));
  }

  void setWallpaperScaleMode(@NonNull String themeId, int scaleMode) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(scaleModeKey(themeId), normalizeScaleMode(scaleMode));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getWallpaperAnchor(@NonNull String themeId) {
    return normalizeAnchor(
        prefs.getInt(
            anchorKey(themeId), KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_CENTER));
  }

  void setWallpaperAnchor(@NonNull String themeId, int anchor) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(anchorKey(themeId), normalizeAnchor(anchor));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  boolean isMatchKeyShapeEnabled(@NonNull String themeId) {
    return prefs.getBoolean(matchKeyShapeKey(themeId), false);
  }

  void setMatchKeyShapeEnabled(@NonNull String themeId, boolean enabled) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean(matchKeyShapeKey(themeId), enabled);
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getWallpaperQuality(@NonNull String themeId) {
    final String key = qualityKey(themeId);
    if (prefs.contains(key)) {
      return normalizeQuality(
          prefs.getInt(key, KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_BALANCED));
    }

    // Backwards-compat: match-key-shape existed before an explicit quality setting. If a user had
    // it enabled, treat the preset as high-quality by default.
    return isMatchKeyShapeEnabled(themeId)
        ? KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_HIGH
        : KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_BALANCED;
  }

  void setWallpaperQuality(@NonNull String themeId, int quality) {
    final int normalized = normalizeQuality(quality);
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(qualityKey(themeId), normalized);
    if (normalized != KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_HIGH) {
      editor.putBoolean(matchKeyShapeKey(themeId), false);
    }
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getVignettePercent(@NonNull String themeId) {
    return clampPercent(prefs.getInt(vignetteKey(themeId), 0));
  }

  void setVignettePercent(@NonNull String themeId, int vignettePercent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(vignetteKey(themeId), clampPercent(vignettePercent));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getGradientPercent(@NonNull String themeId) {
    return clampPercent(prefs.getInt(gradientKey(themeId), 0));
  }

  void setGradientPercent(@NonNull String themeId, int gradientPercent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(gradientKey(themeId), clampPercent(gradientPercent));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getGrainPercent(@NonNull String themeId) {
    return clampPercent(prefs.getInt(grainKey(themeId), 0));
  }

  void setGrainPercent(@NonNull String themeId, int grainPercent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(grainKey(themeId), clampPercent(grainPercent));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getSaturationPercent(@NonNull String themeId) {
    return clampPercent0To200(prefs.getInt(saturationKey(themeId), 100));
  }

  void setSaturationPercent(@NonNull String themeId, int saturationPercent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(saturationKey(themeId), clampPercent0To200(saturationPercent));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getContrastPercent(@NonNull String themeId) {
    return clampPercent0To200(prefs.getInt(contrastKey(themeId), 100));
  }

  void setContrastPercent(@NonNull String themeId, int contrastPercent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(contrastKey(themeId), clampPercent0To200(contrastPercent));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getBrightnessPercent(@NonNull String themeId) {
    return clampPercent0To200(prefs.getInt(brightnessKey(themeId), 100));
  }

  void setBrightnessPercent(@NonNull String themeId, int brightnessPercent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(brightnessKey(themeId), clampPercent0To200(brightnessPercent));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getTemperaturePercent(@NonNull String themeId) {
    return clampPercent0To200(prefs.getInt(temperatureKey(themeId), 100));
  }

  void setTemperaturePercent(@NonNull String themeId, int temperaturePercent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(temperatureKey(themeId), clampPercent0To200(temperaturePercent));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  boolean isWallpaperInvalid(@NonNull String themeId) {
    return prefs.getBoolean(invalidKey(themeId), false);
  }

  void markWallpaperInvalid(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean(invalidKey(themeId), true);
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  void onWallpaperFileDeleted(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(invalidKey(themeId));
    editor.remove(rotationKey(themeId));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getWallpaperChangeToken(@NonNull String themeId) {
    return prefs.getInt(changeKey(themeId), 0);
  }

  boolean isHighQualityImportEnabled() {
    return prefs.getBoolean(PREF_IMPORT_HIGH_QUALITY, false);
  }

  void setHighQualityImportEnabled(boolean enabled) {
    prefs.edit().putBoolean(PREF_IMPORT_HIGH_QUALITY, enabled).apply();
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
    switch (anchor) {
      case KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_TOP_LEFT:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_TOP:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_TOP_RIGHT:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_LEFT:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_CENTER:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_RIGHT:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_BOTTOM_LEFT:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_BOTTOM:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_BOTTOM_RIGHT:
        return anchor;
      default:
        return KeyboardWallpaperOverrideConstants.WALLPAPER_ANCHOR_CENTER;
    }
  }

  private static int normalizeQuality(int quality) {
    switch (quality) {
      case KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_LOW:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_BALANCED:
      case KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_HIGH:
        return quality;
      default:
        return KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_BALANCED;
    }
  }

  private static int clampPercent0To200(int value) {
    return Math.max(0, Math.min(200, value));
  }
}
