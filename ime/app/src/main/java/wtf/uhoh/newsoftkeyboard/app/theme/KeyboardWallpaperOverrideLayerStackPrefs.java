package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;

abstract class KeyboardWallpaperOverrideLayerStackPrefs
    extends KeyboardWallpaperOverrideBackgroundLayersPrefs {

  private static final String PREF_MODE_PREFIX = "photo_wallpaper_mode::";
  private static final String PREF_SPECIAL_KEY_ALPHA_PREFIX = "photo_wallpaper_special_key_alpha::";
  private static final String PREF_SPACEBAR_ALPHA_PREFIX = "photo_wallpaper_spacebar_alpha::";
  private static final String PREF_MODIFIER_KEY_ALPHA_PREFIX =
      "photo_wallpaper_modifier_key_alpha::";
  private static final String PREF_ENTER_KEY_ALPHA_PREFIX = "photo_wallpaper_enter_key_alpha::";

  private static final String[] THEME_PREF_PREFIXES_TO_CLEAR =
      new String[] {
        PREF_DIM_PREFIX,
        PREF_MODE_PREFIX,
        PREF_KEY_ALPHA_PREFIX,
        PREF_SPECIAL_KEY_ALPHA_PREFIX,
        PREF_SPACEBAR_ALPHA_PREFIX,
        PREF_MODIFIER_KEY_ALPHA_PREFIX,
        PREF_ENTER_KEY_ALPHA_PREFIX,
        PREF_KEY_BLEND_MODE_PREFIX,
        PREF_KEY_LAYER_ORDER_PREFIX,
        PREF_KEY_LAYER_STACK_PREFIX,
        PREF_KEY_COLOR_WASH_COLOR_PREFIX,
        PREF_KEY_COLOR_WASH_BLEND_MODE_PREFIX,
        PREF_KEY_HIGHLIGHT_PERCENT_PREFIX,
        PREF_KEY_HIGHLIGHT_BLEND_MODE_PREFIX,
        PREF_KEY_GRADIENT_BLEND_MODE_PREFIX,
        PREF_KEY_VIGNETTE_BLEND_MODE_PREFIX,
        PREF_KEY_GRAIN_BLEND_MODE_PREFIX,
        PREF_BACKGROUND_LAYER_ORDER_PREFIX,
        PREF_BACKGROUND_LAYER_STACK_PREFIX,
        PREF_BACKGROUND_TINT_BLEND_MODE_PREFIX,
        PREF_BACKGROUND_DIM_BLEND_MODE_PREFIX,
        PREF_BACKGROUND_GRADIENT_BLEND_MODE_PREFIX,
        PREF_BACKGROUND_VIGNETTE_BLEND_MODE_PREFIX,
        PREF_BACKGROUND_GRAIN_BLEND_MODE_PREFIX,
        PREF_ROTATION_PREFIX,
        PREF_SCALE_MODE_PREFIX,
        PREF_ANCHOR_PREFIX,
        PREF_MATCH_KEY_SHAPE_PREFIX,
        PREF_QUALITY_PREFIX,
        PREF_VIGNETTE_PREFIX,
        PREF_GRADIENT_PREFIX,
        PREF_GRAIN_PREFIX,
        PREF_SATURATION_PREFIX,
        PREF_CONTRAST_PREFIX,
        PREF_BRIGHTNESS_PREFIX,
        PREF_TEMPERATURE_PREFIX,
        PREF_INVALID_PREFIX
      };

  private final KeyboardWallpaperFileStore fileStore;

  KeyboardWallpaperOverrideLayerStackPrefs(
      @NonNull SharedPreferences prefs, @NonNull KeyboardWallpaperFileStore fileStore) {
    super(prefs);
    this.fileStore = fileStore;
  }

  @NonNull
  static String modeKey(@NonNull String themeId) {
    return PREF_MODE_PREFIX + themeId;
  }

  @NonNull
  static String specialKeyAlphaKey(@NonNull String themeId) {
    return PREF_SPECIAL_KEY_ALPHA_PREFIX + themeId;
  }

  @NonNull
  static String spacebarAlphaKey(@NonNull String themeId) {
    return PREF_SPACEBAR_ALPHA_PREFIX + themeId;
  }

  @NonNull
  static String modifierKeyAlphaKey(@NonNull String themeId) {
    return PREF_MODIFIER_KEY_ALPHA_PREFIX + themeId;
  }

  @NonNull
  static String enterKeyAlphaKey(@NonNull String themeId) {
    return PREF_ENTER_KEY_ALPHA_PREFIX + themeId;
  }

  int getWallpaperMode(@NonNull String themeId) {
    final boolean hasModeOverride = prefs.contains(modeKey(themeId));
    final int defaultMode =
        hasModeOverride || !fileStore.hasWallpaper(themeId)
            ? KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY
            : KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE;
    return normalizeMode(prefs.getInt(modeKey(themeId), defaultMode));
  }

  void setWallpaperMode(@NonNull String themeId, int mode) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(modeKey(themeId), normalizeMode(mode));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  boolean hasSpecialKeyAlphaPercentOverride(@NonNull String themeId) {
    return prefs.contains(specialKeyAlphaKey(themeId));
  }

  int getSpecialKeyAlphaPercent(@NonNull String themeId) {
    final String key = specialKeyAlphaKey(themeId);
    if (!prefs.contains(key)) return getKeyAlphaPercent(themeId);
    return clampPercent(prefs.getInt(key, getKeyAlphaPercent(themeId)));
  }

  void setSpecialKeyAlphaPercent(@NonNull String themeId, int alphaPercent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(specialKeyAlphaKey(themeId), clampPercent(alphaPercent));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  void clearSpecialKeyAlphaPercent(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(specialKeyAlphaKey(themeId));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  boolean hasSpacebarAlphaPercentOverride(@NonNull String themeId) {
    return prefs.contains(spacebarAlphaKey(themeId));
  }

  int getSpacebarAlphaPercent(@NonNull String themeId) {
    final String key = spacebarAlphaKey(themeId);
    if (!prefs.contains(key)) return getKeyAlphaPercent(themeId);
    return clampPercent(prefs.getInt(key, getKeyAlphaPercent(themeId)));
  }

  void setSpacebarAlphaPercent(@NonNull String themeId, int alphaPercent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(spacebarAlphaKey(themeId), clampPercent(alphaPercent));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  void clearSpacebarAlphaPercent(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(spacebarAlphaKey(themeId));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  boolean hasModifierKeyAlphaPercentOverride(@NonNull String themeId) {
    return prefs.contains(modifierKeyAlphaKey(themeId));
  }

  int getModifierKeyAlphaPercent(@NonNull String themeId) {
    final String key = modifierKeyAlphaKey(themeId);
    final int fallback = getSpecialKeyAlphaPercent(themeId);
    if (!prefs.contains(key)) return fallback;
    return clampPercent(prefs.getInt(key, fallback));
  }

  void setModifierKeyAlphaPercent(@NonNull String themeId, int alphaPercent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(modifierKeyAlphaKey(themeId), clampPercent(alphaPercent));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  void clearModifierKeyAlphaPercent(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(modifierKeyAlphaKey(themeId));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  boolean hasEnterKeyAlphaPercentOverride(@NonNull String themeId) {
    return prefs.contains(enterKeyAlphaKey(themeId));
  }

  int getEnterKeyAlphaPercent(@NonNull String themeId) {
    final String key = enterKeyAlphaKey(themeId);
    final int fallback = getSpecialKeyAlphaPercent(themeId);
    if (!prefs.contains(key)) return fallback;
    return clampPercent(prefs.getInt(key, fallback));
  }

  void setEnterKeyAlphaPercent(@NonNull String themeId, int alphaPercent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(enterKeyAlphaKey(themeId), clampPercent(alphaPercent));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  void clearEnterKeyAlphaPercent(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(enterKeyAlphaKey(themeId));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  void clearPrefs(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    for (String prefix : THEME_PREF_PREFIXES_TO_CLEAR) {
      editor.remove(prefix + themeId);
    }
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  void copyToTheme(@NonNull String sourceThemeId, @NonNull String targetThemeId) {
    if (sourceThemeId.equals(targetThemeId)) return;

    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(dimKey(targetThemeId), getDimPercent(sourceThemeId));
    editor.putInt(modeKey(targetThemeId), getWallpaperMode(sourceThemeId));
    editor.putInt(keyAlphaKey(targetThemeId), getKeyAlphaPercent(sourceThemeId));
    editor.putInt(keyBlendModeKey(targetThemeId), getKeyBlendMode(sourceThemeId));
    editor.putString(
        keyLayerOrderKey(targetThemeId),
        KeyboardWallpaperLayerStackCodec.serializeLayerOrder(getKeyLayerOrder(sourceThemeId)));
    if (hasKeyLayerStackOverride(sourceThemeId)) {
      editor.putString(
          keyLayerStackKey(targetThemeId), prefs.getString(keyLayerStackKey(sourceThemeId), null));
    } else {
      editor.remove(keyLayerStackKey(targetThemeId));
    }
    if (hasKeyColorWashColorOverride(sourceThemeId)) {
      editor.putInt(keyColorWashColorKey(targetThemeId), getKeyColorWashColor(sourceThemeId));
    } else {
      editor.remove(keyColorWashColorKey(targetThemeId));
    }
    editor.putInt(keyColorWashBlendModeKey(targetThemeId), getKeyColorWashBlendMode(sourceThemeId));
    editor.putInt(keyHighlightPercentKey(targetThemeId), getKeyHighlightPercent(sourceThemeId));
    editor.putInt(keyHighlightBlendModeKey(targetThemeId), getKeyHighlightBlendMode(sourceThemeId));
    editor.putInt(keyGradientBlendModeKey(targetThemeId), getKeyGradientBlendMode(sourceThemeId));
    editor.putInt(keyVignetteBlendModeKey(targetThemeId), getKeyVignetteBlendMode(sourceThemeId));
    editor.putInt(keyGrainBlendModeKey(targetThemeId), getKeyGrainBlendMode(sourceThemeId));
    editor.putString(
        backgroundLayerOrderKey(targetThemeId),
        KeyboardWallpaperLayerStackCodec.serializeLayerOrder(
            getBackgroundLayerOrder(sourceThemeId)));
    if (hasBackgroundLayerStackOverride(sourceThemeId)) {
      editor.putString(
          backgroundLayerStackKey(targetThemeId),
          prefs.getString(backgroundLayerStackKey(sourceThemeId), null));
    } else {
      editor.remove(backgroundLayerStackKey(targetThemeId));
    }
    editor.putInt(
        backgroundTintBlendModeKey(targetThemeId), getBackgroundTintBlendMode(sourceThemeId));
    editor.putInt(
        backgroundDimBlendModeKey(targetThemeId), getBackgroundDimBlendMode(sourceThemeId));
    editor.putInt(
        backgroundGradientBlendModeKey(targetThemeId),
        getBackgroundGradientBlendMode(sourceThemeId));
    editor.putInt(
        backgroundVignetteBlendModeKey(targetThemeId),
        getBackgroundVignetteBlendMode(sourceThemeId));
    editor.putInt(
        backgroundGrainBlendModeKey(targetThemeId), getBackgroundGrainBlendMode(sourceThemeId));
    if (hasSpecialKeyAlphaPercentOverride(sourceThemeId)) {
      editor.putInt(specialKeyAlphaKey(targetThemeId), getSpecialKeyAlphaPercent(sourceThemeId));
    } else {
      editor.remove(specialKeyAlphaKey(targetThemeId));
    }
    if (hasSpacebarAlphaPercentOverride(sourceThemeId)) {
      editor.putInt(spacebarAlphaKey(targetThemeId), getSpacebarAlphaPercent(sourceThemeId));
    } else {
      editor.remove(spacebarAlphaKey(targetThemeId));
    }
    if (hasModifierKeyAlphaPercentOverride(sourceThemeId)) {
      editor.putInt(modifierKeyAlphaKey(targetThemeId), getModifierKeyAlphaPercent(sourceThemeId));
    } else {
      editor.remove(modifierKeyAlphaKey(targetThemeId));
    }
    if (hasEnterKeyAlphaPercentOverride(sourceThemeId)) {
      editor.putInt(enterKeyAlphaKey(targetThemeId), getEnterKeyAlphaPercent(sourceThemeId));
    } else {
      editor.remove(enterKeyAlphaKey(targetThemeId));
    }
    editor.putInt(rotationKey(targetThemeId), getWallpaperRotationDegrees(sourceThemeId));
    editor.putInt(scaleModeKey(targetThemeId), getWallpaperScaleMode(sourceThemeId));
    editor.putInt(anchorKey(targetThemeId), getWallpaperAnchor(sourceThemeId));
    editor.putBoolean(matchKeyShapeKey(targetThemeId), isMatchKeyShapeEnabled(sourceThemeId));
    editor.putInt(qualityKey(targetThemeId), getWallpaperQuality(sourceThemeId));
    editor.putInt(vignetteKey(targetThemeId), getVignettePercent(sourceThemeId));
    editor.putInt(gradientKey(targetThemeId), getGradientPercent(sourceThemeId));
    editor.putInt(grainKey(targetThemeId), getGrainPercent(sourceThemeId));
    editor.putInt(saturationKey(targetThemeId), getSaturationPercent(sourceThemeId));
    editor.putInt(contrastKey(targetThemeId), getContrastPercent(sourceThemeId));
    editor.putInt(brightnessKey(targetThemeId), getBrightnessPercent(sourceThemeId));
    editor.putInt(temperatureKey(targetThemeId), getTemperaturePercent(sourceThemeId));
    editor.remove(invalidKey(targetThemeId));
    markWallpaperChanged(targetThemeId, editor);
    editor.apply();
  }

  void onWallpaperImported(
      @NonNull String themeId, boolean hadExistingWallpaper, int exifRotationDegrees) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(invalidKey(themeId));
    // Default to a visible mode when a user first imports a wallpaper, since many themes have an
    // opaque keyboard background and "background only" would appear to do nothing.
    // Use key tint by default since it is significantly cheaper than key texture.
    if (!prefs.contains(modeKey(themeId))
        || (!hadExistingWallpaper
            && normalizeMode(
                    prefs.getInt(
                        modeKey(themeId),
                        KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY))
                == KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY)) {
      editor.putInt(
          modeKey(themeId), KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_KEY_TINT);
    }
    // When importing a wallpaper for the first time, default the key overlay opacity to a visible
    // value so the photo doesn't appear "invisible" on opaque themes.
    if (!hadExistingWallpaper && !prefs.contains(keyAlphaKey(themeId))) {
      editor.putInt(keyAlphaKey(themeId), 60);
    }
    if (exifRotationDegrees != 0) {
      editor.putInt(rotationKey(themeId), exifRotationDegrees);
    } else {
      editor.remove(rotationKey(themeId));
    }
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  private static int normalizeMode(int mode) {
    return switch (mode) {
      case KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_KEY_TINT,
          KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_KEY_TEXTURE,
          KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY ->
          mode;
      default -> KeyboardWallpaperOverrideConstants.WALLPAPER_MODE_BACKGROUND_ONLY;
    };
  }
}
