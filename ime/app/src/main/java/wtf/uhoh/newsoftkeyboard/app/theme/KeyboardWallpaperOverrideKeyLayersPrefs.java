package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

abstract class KeyboardWallpaperOverrideKeyLayersPrefs
    extends KeyboardWallpaperOverrideImageTransformPrefs {

  static final String PREF_KEY_ALPHA_PREFIX = "photo_wallpaper_key_alpha::";
  static final String PREF_KEY_BLEND_MODE_PREFIX = "photo_wallpaper_key_blend_mode::";
  static final String PREF_KEY_LAYER_ORDER_PREFIX = "photo_wallpaper_key_layer_order::";
  static final String PREF_KEY_LAYER_STACK_PREFIX = "photo_wallpaper_key_layer_stack::";
  static final String PREF_KEY_COLOR_WASH_COLOR_PREFIX = "photo_wallpaper_key_color_wash_color::";
  static final String PREF_KEY_COLOR_WASH_BLEND_MODE_PREFIX =
      "photo_wallpaper_key_color_wash_blend_mode::";
  static final String PREF_KEY_HIGHLIGHT_PERCENT_PREFIX = "photo_wallpaper_key_highlight_percent::";
  static final String PREF_KEY_HIGHLIGHT_BLEND_MODE_PREFIX =
      "photo_wallpaper_key_highlight_blend_mode::";
  static final String PREF_KEY_GRADIENT_BLEND_MODE_PREFIX =
      "photo_wallpaper_key_gradient_blend_mode::";
  static final String PREF_KEY_VIGNETTE_BLEND_MODE_PREFIX =
      "photo_wallpaper_key_vignette_blend_mode::";
  static final String PREF_KEY_GRAIN_BLEND_MODE_PREFIX = "photo_wallpaper_key_grain_blend_mode::";

  private static final int[] DEFAULT_KEY_LAYER_ORDER =
      new int[] {
        KeyboardWallpaperOverrideConstants.KEY_LAYER_COLOR_WASH,
        KeyboardWallpaperOverrideConstants.KEY_LAYER_HIGHLIGHT,
        KeyboardWallpaperOverrideConstants.KEY_LAYER_GRADIENT,
        KeyboardWallpaperOverrideConstants.KEY_LAYER_VIGNETTE,
        KeyboardWallpaperOverrideConstants.KEY_LAYER_GRAIN
      };

  KeyboardWallpaperOverrideKeyLayersPrefs(@NonNull SharedPreferences prefs) {
    super(prefs);
  }

  @NonNull
  static String keyAlphaKey(@NonNull String themeId) {
    return PREF_KEY_ALPHA_PREFIX + themeId;
  }

  @NonNull
  static String keyBlendModeKey(@NonNull String themeId) {
    return PREF_KEY_BLEND_MODE_PREFIX + themeId;
  }

  @NonNull
  static String keyLayerOrderKey(@NonNull String themeId) {
    return PREF_KEY_LAYER_ORDER_PREFIX + themeId;
  }

  @NonNull
  static String keyLayerStackKey(@NonNull String themeId) {
    return PREF_KEY_LAYER_STACK_PREFIX + themeId;
  }

  @NonNull
  static String keyColorWashColorKey(@NonNull String themeId) {
    return PREF_KEY_COLOR_WASH_COLOR_PREFIX + themeId;
  }

  @NonNull
  static String keyColorWashBlendModeKey(@NonNull String themeId) {
    return PREF_KEY_COLOR_WASH_BLEND_MODE_PREFIX + themeId;
  }

  @NonNull
  static String keyHighlightPercentKey(@NonNull String themeId) {
    return PREF_KEY_HIGHLIGHT_PERCENT_PREFIX + themeId;
  }

  @NonNull
  static String keyHighlightBlendModeKey(@NonNull String themeId) {
    return PREF_KEY_HIGHLIGHT_BLEND_MODE_PREFIX + themeId;
  }

  @NonNull
  static String keyGradientBlendModeKey(@NonNull String themeId) {
    return PREF_KEY_GRADIENT_BLEND_MODE_PREFIX + themeId;
  }

  @NonNull
  static String keyVignetteBlendModeKey(@NonNull String themeId) {
    return PREF_KEY_VIGNETTE_BLEND_MODE_PREFIX + themeId;
  }

  @NonNull
  static String keyGrainBlendModeKey(@NonNull String themeId) {
    return PREF_KEY_GRAIN_BLEND_MODE_PREFIX + themeId;
  }

  int getKeyAlphaPercent(@NonNull String themeId) {
    return clampPercent(
        prefs.getInt(
            keyAlphaKey(themeId), KeyboardWallpaperOverrideConstants.DEFAULT_KEY_ALPHA_PERCENT));
  }

  void setKeyAlphaPercent(@NonNull String themeId, int alphaPercent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(keyAlphaKey(themeId), clampPercent(alphaPercent));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getKeyBlendMode(@NonNull String themeId) {
    return KeyboardWallpaperOverrideConstants.normalizeBlendMode(
        prefs.getInt(
            keyBlendModeKey(themeId),
            KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL));
  }

  void setKeyBlendMode(@NonNull String themeId, int blendMode) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(
        keyBlendModeKey(themeId), KeyboardWallpaperOverrideConstants.normalizeBlendMode(blendMode));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  boolean hasKeyLayerStackOverride(@NonNull String themeId) {
    return prefs.contains(keyLayerStackKey(themeId));
  }

  @NonNull
  KeyboardWallpaperLayer[] getKeyLayerStack(@NonNull String themeId) {
    final String raw = prefs.getString(keyLayerStackKey(themeId), null);
    final KeyboardWallpaperLayer[] parsed = KeyboardWallpaperLayerStackCodec.parseLayerStack(raw);
    if (parsed != null) return parsed;
    return KeyboardWallpaperLegacyLayerStackBuilder.buildLegacyKeyLayerStack(this, themeId);
  }

  void setKeyLayerStack(@NonNull String themeId, @NonNull KeyboardWallpaperLayer[] layers) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putString(
        keyLayerStackKey(themeId), KeyboardWallpaperLayerStackCodec.serializeLayerStack(layers));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  void clearKeyLayerStack(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(keyLayerStackKey(themeId));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  @NonNull
  int[] getKeyLayerOrder(@NonNull String themeId) {
    final String key = keyLayerOrderKey(themeId);
    if (!prefs.contains(key)) return DEFAULT_KEY_LAYER_ORDER.clone();
    return KeyboardWallpaperLayerStackCodec.parseLayerOrder(
        prefs.getString(key, ""), DEFAULT_KEY_LAYER_ORDER, 5, /* allowEmpty= */ true);
  }

  boolean hasKeyLayerOrderOverride(@NonNull String themeId) {
    return prefs.contains(keyLayerOrderKey(themeId));
  }

  void setKeyLayerOrder(@NonNull String themeId, @NonNull int[] layerOrder) {
    final int[] normalized =
        KeyboardWallpaperLayerStackCodec.normalizeLayerOrder(
            layerOrder, DEFAULT_KEY_LAYER_ORDER, 5, /* allowEmpty= */ true);
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putString(
        keyLayerOrderKey(themeId),
        KeyboardWallpaperLayerStackCodec.serializeLayerOrder(normalized));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  void clearKeyLayerOrder(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(keyLayerOrderKey(themeId));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  boolean hasKeyColorWashColorOverride(@NonNull String themeId) {
    return prefs.contains(keyColorWashColorKey(themeId));
  }

  @Nullable
  Integer getKeyColorWashColor(@NonNull String themeId) {
    final String key = keyColorWashColorKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  void setKeyColorWashColor(@NonNull String themeId, @NonNull Integer argbColor) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(keyColorWashColorKey(themeId), argbColor);
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  void clearKeyColorWashColor(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(keyColorWashColorKey(themeId));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getKeyColorWashBlendMode(@NonNull String themeId) {
    return KeyboardWallpaperOverrideConstants.normalizeBlendMode(
        prefs.getInt(
            keyColorWashBlendModeKey(themeId),
            KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL));
  }

  void setKeyColorWashBlendMode(@NonNull String themeId, int blendMode) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(
        keyColorWashBlendModeKey(themeId),
        KeyboardWallpaperOverrideConstants.normalizeBlendMode(blendMode));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getKeyHighlightPercent(@NonNull String themeId) {
    return clampPercent(prefs.getInt(keyHighlightPercentKey(themeId), 0));
  }

  void setKeyHighlightPercent(@NonNull String themeId, int highlightPercent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(keyHighlightPercentKey(themeId), clampPercent(highlightPercent));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getKeyHighlightBlendMode(@NonNull String themeId) {
    return KeyboardWallpaperOverrideConstants.normalizeBlendMode(
        prefs.getInt(
            keyHighlightBlendModeKey(themeId),
            KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL));
  }

  void setKeyHighlightBlendMode(@NonNull String themeId, int blendMode) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(
        keyHighlightBlendModeKey(themeId),
        KeyboardWallpaperOverrideConstants.normalizeBlendMode(blendMode));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getKeyGradientBlendMode(@NonNull String themeId) {
    return KeyboardWallpaperOverrideConstants.normalizeBlendMode(
        prefs.getInt(
            keyGradientBlendModeKey(themeId),
            KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL));
  }

  void setKeyGradientBlendMode(@NonNull String themeId, int blendMode) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(
        keyGradientBlendModeKey(themeId),
        KeyboardWallpaperOverrideConstants.normalizeBlendMode(blendMode));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getKeyVignetteBlendMode(@NonNull String themeId) {
    return KeyboardWallpaperOverrideConstants.normalizeBlendMode(
        prefs.getInt(
            keyVignetteBlendModeKey(themeId),
            KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL));
  }

  void setKeyVignetteBlendMode(@NonNull String themeId, int blendMode) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(
        keyVignetteBlendModeKey(themeId),
        KeyboardWallpaperOverrideConstants.normalizeBlendMode(blendMode));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }

  int getKeyGrainBlendMode(@NonNull String themeId) {
    return KeyboardWallpaperOverrideConstants.normalizeBlendMode(
        prefs.getInt(
            keyGrainBlendModeKey(themeId),
            KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL));
  }

  void setKeyGrainBlendMode(@NonNull String themeId, int blendMode) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(
        keyGrainBlendModeKey(themeId),
        KeyboardWallpaperOverrideConstants.normalizeBlendMode(blendMode));
    markWallpaperChanged(themeId, editor);
    editor.apply();
  }
}
