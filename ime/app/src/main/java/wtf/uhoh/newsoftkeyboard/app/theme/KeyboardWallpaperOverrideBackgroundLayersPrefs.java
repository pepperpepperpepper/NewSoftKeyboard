package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;

abstract class KeyboardWallpaperOverrideBackgroundLayersPrefs
    extends KeyboardWallpaperOverrideKeyLayersPrefs {

  static final String PREF_DIM_PREFIX = "photo_wallpaper_dim::";
  static final String PREF_BACKGROUND_LAYER_ORDER_PREFIX =
      "photo_wallpaper_background_layer_order::";
  static final String PREF_BACKGROUND_LAYER_STACK_PREFIX =
      "photo_wallpaper_background_layer_stack::";
  static final String PREF_BACKGROUND_TINT_BLEND_MODE_PREFIX =
      "photo_wallpaper_background_tint_blend_mode::";
  static final String PREF_BACKGROUND_DIM_BLEND_MODE_PREFIX =
      "photo_wallpaper_background_dim_blend_mode::";
  static final String PREF_BACKGROUND_GRADIENT_BLEND_MODE_PREFIX =
      "photo_wallpaper_background_gradient_blend_mode::";
  static final String PREF_BACKGROUND_VIGNETTE_BLEND_MODE_PREFIX =
      "photo_wallpaper_background_vignette_blend_mode::";
  static final String PREF_BACKGROUND_GRAIN_BLEND_MODE_PREFIX =
      "photo_wallpaper_background_grain_blend_mode::";

  private static final int[] DEFAULT_BACKGROUND_LAYER_ORDER =
      new int[] {
        KeyboardWallpaperOverrideConstants.BACKGROUND_LAYER_TINT,
        KeyboardWallpaperOverrideConstants.BACKGROUND_LAYER_DIM,
        KeyboardWallpaperOverrideConstants.BACKGROUND_LAYER_GRADIENT,
        KeyboardWallpaperOverrideConstants.BACKGROUND_LAYER_VIGNETTE,
        KeyboardWallpaperOverrideConstants.BACKGROUND_LAYER_GRAIN
      };

  KeyboardWallpaperOverrideBackgroundLayersPrefs(@NonNull SharedPreferences prefs) {
    super(prefs);
  }

  @NonNull
  static String dimKey(@NonNull String themeId) {
    return PREF_DIM_PREFIX + themeId;
  }

  @NonNull
  static String backgroundLayerOrderKey(@NonNull String themeId) {
    return PREF_BACKGROUND_LAYER_ORDER_PREFIX + themeId;
  }

  @NonNull
  static String backgroundLayerStackKey(@NonNull String themeId) {
    return PREF_BACKGROUND_LAYER_STACK_PREFIX + themeId;
  }

  @NonNull
  static String backgroundTintBlendModeKey(@NonNull String themeId) {
    return PREF_BACKGROUND_TINT_BLEND_MODE_PREFIX + themeId;
  }

  @NonNull
  static String backgroundDimBlendModeKey(@NonNull String themeId) {
    return PREF_BACKGROUND_DIM_BLEND_MODE_PREFIX + themeId;
  }

  @NonNull
  static String backgroundGradientBlendModeKey(@NonNull String themeId) {
    return PREF_BACKGROUND_GRADIENT_BLEND_MODE_PREFIX + themeId;
  }

  @NonNull
  static String backgroundVignetteBlendModeKey(@NonNull String themeId) {
    return PREF_BACKGROUND_VIGNETTE_BLEND_MODE_PREFIX + themeId;
  }

  @NonNull
  static String backgroundGrainBlendModeKey(@NonNull String themeId) {
    return PREF_BACKGROUND_GRAIN_BLEND_MODE_PREFIX + themeId;
  }

  int getDimPercent(@NonNull String themeId) {
    return prefs.getInt(dimKey(themeId), 0);
  }

  void setDimPercent(@NonNull String themeId, int dimPercent) {
    prefs.edit().putInt(dimKey(themeId), clampPercent(dimPercent)).apply();
  }

  @NonNull
  int[] getBackgroundLayerOrder(@NonNull String themeId) {
    final String key = backgroundLayerOrderKey(themeId);
    if (!prefs.contains(key)) return DEFAULT_BACKGROUND_LAYER_ORDER.clone();
    return KeyboardWallpaperLayerStackCodec.parseLayerOrder(
        prefs.getString(key, ""), DEFAULT_BACKGROUND_LAYER_ORDER, 5, /* allowEmpty= */ true);
  }

  boolean hasBackgroundLayerOrderOverride(@NonNull String themeId) {
    return prefs.contains(backgroundLayerOrderKey(themeId));
  }

  boolean hasBackgroundLayerStackOverride(@NonNull String themeId) {
    return prefs.contains(backgroundLayerStackKey(themeId));
  }

  @NonNull
  KeyboardWallpaperLayer[] getBackgroundLayerStack(@NonNull String themeId) {
    final String raw = prefs.getString(backgroundLayerStackKey(themeId), null);
    final KeyboardWallpaperLayer[] parsed = KeyboardWallpaperLayerStackCodec.parseLayerStack(raw);
    if (parsed != null) return parsed;
    return KeyboardWallpaperLegacyLayerStackBuilder.buildLegacyBackgroundLayerStack(this, themeId);
  }

  void setBackgroundLayerStack(@NonNull String themeId, @NonNull KeyboardWallpaperLayer[] layers) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putString(
        backgroundLayerStackKey(themeId),
        KeyboardWallpaperLayerStackCodec.serializeLayerStack(layers));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  void clearBackgroundLayerStack(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(backgroundLayerStackKey(themeId));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  void setBackgroundLayerOrder(@NonNull String themeId, @NonNull int[] layerOrder) {
    final int[] normalized =
        KeyboardWallpaperLayerStackCodec.normalizeLayerOrder(
            layerOrder, DEFAULT_BACKGROUND_LAYER_ORDER, 5, /* allowEmpty= */ true);
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putString(
        backgroundLayerOrderKey(themeId),
        KeyboardWallpaperLayerStackCodec.serializeLayerOrder(normalized));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  void clearBackgroundLayerOrder(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(backgroundLayerOrderKey(themeId));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getBackgroundTintBlendMode(@NonNull String themeId) {
    return KeyboardWallpaperOverrideConstants.normalizeBlendMode(
        prefs.getInt(
            backgroundTintBlendModeKey(themeId),
            KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL));
  }

  void setBackgroundTintBlendMode(@NonNull String themeId, int blendMode) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(
        backgroundTintBlendModeKey(themeId),
        KeyboardWallpaperOverrideConstants.normalizeBlendMode(blendMode));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getBackgroundDimBlendMode(@NonNull String themeId) {
    return KeyboardWallpaperOverrideConstants.normalizeBlendMode(
        prefs.getInt(
            backgroundDimBlendModeKey(themeId),
            KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL));
  }

  void setBackgroundDimBlendMode(@NonNull String themeId, int blendMode) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(
        backgroundDimBlendModeKey(themeId),
        KeyboardWallpaperOverrideConstants.normalizeBlendMode(blendMode));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getBackgroundGradientBlendMode(@NonNull String themeId) {
    return KeyboardWallpaperOverrideConstants.normalizeBlendMode(
        prefs.getInt(
            backgroundGradientBlendModeKey(themeId),
            KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL));
  }

  void setBackgroundGradientBlendMode(@NonNull String themeId, int blendMode) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(
        backgroundGradientBlendModeKey(themeId),
        KeyboardWallpaperOverrideConstants.normalizeBlendMode(blendMode));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getBackgroundVignetteBlendMode(@NonNull String themeId) {
    return KeyboardWallpaperOverrideConstants.normalizeBlendMode(
        prefs.getInt(
            backgroundVignetteBlendModeKey(themeId),
            KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL));
  }

  void setBackgroundVignetteBlendMode(@NonNull String themeId, int blendMode) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(
        backgroundVignetteBlendModeKey(themeId),
        KeyboardWallpaperOverrideConstants.normalizeBlendMode(blendMode));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getBackgroundGrainBlendMode(@NonNull String themeId) {
    return KeyboardWallpaperOverrideConstants.normalizeBlendMode(
        prefs.getInt(
            backgroundGrainBlendModeKey(themeId),
            KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL));
  }

  void setBackgroundGrainBlendMode(@NonNull String themeId, int blendMode) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(
        backgroundGrainBlendModeKey(themeId),
        KeyboardWallpaperOverrideConstants.normalizeBlendMode(blendMode));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }
}
