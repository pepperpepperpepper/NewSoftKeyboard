package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.SharedPreferences;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Stores user-selected theme appearance overrides per theme id. */
abstract class KeyboardThemeUserOverridesColorsPrefs {
  private static final String PREF_TOKEN_PRIMARY_TEXT_COLOR_PREFIX =
      "theme_user_token_primary_text_color::";
  private static final String PREF_TOKEN_SECONDARY_TEXT_COLOR_PREFIX =
      "theme_user_token_secondary_text_color::";
  private static final String PREF_TOKEN_ACCENT_COLOR_PREFIX = "theme_user_token_accent_color::";
  private static final String PREF_TOKEN_KEY_SURFACE_COLOR_PREFIX =
      "theme_user_token_key_surface_color::";
  private static final String PREF_TOKEN_BACKGROUND_COLOR_PREFIX =
      "theme_user_token_background_color::";

  private static final String PREF_KEY_TEXT_COLOR_PREFIX = "theme_user_key_text_color::";
  private static final String PREF_SPECIAL_KEY_TEXT_COLOR_PREFIX =
      "theme_user_special_key_text_color::";
  private static final String PREF_SPACEBAR_TEXT_COLOR_PREFIX = "theme_user_spacebar_text_color::";
  private static final String PREF_MODIFIER_KEY_TEXT_COLOR_PREFIX =
      "theme_user_modifier_key_text_color::";
  private static final String PREF_ENTER_KEY_TEXT_COLOR_PREFIX =
      "theme_user_enter_key_text_color::";
  private static final String PREF_HINT_TEXT_COLOR_PREFIX = "theme_user_hint_text_color::";
  private static final String PREF_KEY_BG_TINT_PREFIX = "theme_user_key_bg_tint::";
  private static final String PREF_SPECIAL_KEY_BG_TINT_PREFIX = "theme_user_special_key_bg_tint::";
  private static final String PREF_SPACEBAR_BG_TINT_PREFIX = "theme_user_spacebar_bg_tint::";
  private static final String PREF_MODIFIER_KEY_BG_TINT_PREFIX =
      "theme_user_modifier_key_bg_tint::";
  private static final String PREF_ENTER_KEY_BG_TINT_PREFIX = "theme_user_enter_key_bg_tint::";
  private static final String PREF_KEYBOARD_BG_TINT_PREFIX = "theme_user_keyboard_bg_tint::";
  private static final String PREF_KEY_BG_OPACITY_PERCENT_PREFIX =
      "theme_user_key_bg_opacity_percent::";
  private static final String PREF_KEYBOARD_BG_OPACITY_PERCENT_PREFIX =
      "theme_user_keyboard_bg_opacity_percent::";
  private static final String PREF_ENSURE_READABLE_TEXT_ENABLED_PREFIX =
      "theme_user_ensure_readable_text_enabled::";

  final SharedPreferences prefs;

  KeyboardThemeUserOverridesColorsPrefs(@NonNull SharedPreferences prefs) {
    this.prefs = prefs;
  }

  @NonNull
  protected static String keyTextColorKey(@NonNull String themeId) {
    return PREF_KEY_TEXT_COLOR_PREFIX + themeId;
  }

  @NonNull
  protected static String specialKeyTextColorKey(@NonNull String themeId) {
    return PREF_SPECIAL_KEY_TEXT_COLOR_PREFIX + themeId;
  }

  @NonNull
  protected static String spacebarTextColorKey(@NonNull String themeId) {
    return PREF_SPACEBAR_TEXT_COLOR_PREFIX + themeId;
  }

  @NonNull
  protected static String modifierKeyTextColorKey(@NonNull String themeId) {
    return PREF_MODIFIER_KEY_TEXT_COLOR_PREFIX + themeId;
  }

  @NonNull
  protected static String enterKeyTextColorKey(@NonNull String themeId) {
    return PREF_ENTER_KEY_TEXT_COLOR_PREFIX + themeId;
  }

  @NonNull
  protected static String hintTextColorKey(@NonNull String themeId) {
    return PREF_HINT_TEXT_COLOR_PREFIX + themeId;
  }

  @NonNull
  protected static String keyBackgroundTintKey(@NonNull String themeId) {
    return PREF_KEY_BG_TINT_PREFIX + themeId;
  }

  @NonNull
  protected static String specialKeyBackgroundTintKey(@NonNull String themeId) {
    return PREF_SPECIAL_KEY_BG_TINT_PREFIX + themeId;
  }

  @NonNull
  protected static String spacebarBackgroundTintKey(@NonNull String themeId) {
    return PREF_SPACEBAR_BG_TINT_PREFIX + themeId;
  }

  @NonNull
  protected static String modifierKeyBackgroundTintKey(@NonNull String themeId) {
    return PREF_MODIFIER_KEY_BG_TINT_PREFIX + themeId;
  }

  @NonNull
  protected static String enterKeyBackgroundTintKey(@NonNull String themeId) {
    return PREF_ENTER_KEY_BG_TINT_PREFIX + themeId;
  }

  @NonNull
  protected static String keyboardBackgroundTintKey(@NonNull String themeId) {
    return PREF_KEYBOARD_BG_TINT_PREFIX + themeId;
  }

  @NonNull
  protected static String keyBackgroundOpacityPercentKey(@NonNull String themeId) {
    return PREF_KEY_BG_OPACITY_PERCENT_PREFIX + themeId;
  }

  @NonNull
  protected static String keyboardBackgroundOpacityPercentKey(@NonNull String themeId) {
    return PREF_KEYBOARD_BG_OPACITY_PERCENT_PREFIX + themeId;
  }

  @NonNull
  protected static String ensureReadableTextEnabledKey(@NonNull String themeId) {
    return PREF_ENSURE_READABLE_TEXT_ENABLED_PREFIX + themeId;
  }

  @NonNull
  protected static String tokenPrimaryTextColorKey(@NonNull String themeId) {
    return PREF_TOKEN_PRIMARY_TEXT_COLOR_PREFIX + themeId;
  }

  @NonNull
  protected static String tokenSecondaryTextColorKey(@NonNull String themeId) {
    return PREF_TOKEN_SECONDARY_TEXT_COLOR_PREFIX + themeId;
  }

  @NonNull
  protected static String tokenAccentColorKey(@NonNull String themeId) {
    return PREF_TOKEN_ACCENT_COLOR_PREFIX + themeId;
  }

  @NonNull
  protected static String tokenKeySurfaceColorKey(@NonNull String themeId) {
    return PREF_TOKEN_KEY_SURFACE_COLOR_PREFIX + themeId;
  }

  @NonNull
  protected static String tokenBackgroundColorKey(@NonNull String themeId) {
    return PREF_TOKEN_BACKGROUND_COLOR_PREFIX + themeId;
  }

  public int getChangeToken(@NonNull String themeId) {
    return prefs.getInt(KeyboardThemeUserOverridesStore.changeKey(themeId), 0);
  }

  @Nullable
  public Integer getTokenPrimaryTextColor(@NonNull String themeId) {
    final String key = tokenPrimaryTextColorKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setTokenPrimaryTextColor(@NonNull String themeId, @ColorInt int argb) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(tokenPrimaryTextColorKey(themeId), argb);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearTokenPrimaryTextColor(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(tokenPrimaryTextColorKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getTokenSecondaryTextColor(@NonNull String themeId) {
    final String key = tokenSecondaryTextColorKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setTokenSecondaryTextColor(@NonNull String themeId, @ColorInt int argb) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(tokenSecondaryTextColorKey(themeId), argb);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearTokenSecondaryTextColor(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(tokenSecondaryTextColorKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getTokenAccentColor(@NonNull String themeId) {
    final String key = tokenAccentColorKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setTokenAccentColor(@NonNull String themeId, @ColorInt int argb) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(tokenAccentColorKey(themeId), argb);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearTokenAccentColor(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(tokenAccentColorKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getTokenKeySurfaceColor(@NonNull String themeId) {
    final String key = tokenKeySurfaceColorKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setTokenKeySurfaceColor(@NonNull String themeId, @ColorInt int argb) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(tokenKeySurfaceColorKey(themeId), argb);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearTokenKeySurfaceColor(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(tokenKeySurfaceColorKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getTokenBackgroundColor(@NonNull String themeId) {
    final String key = tokenBackgroundColorKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setTokenBackgroundColor(@NonNull String themeId, @ColorInt int argb) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(tokenBackgroundColorKey(themeId), argb);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearTokenBackgroundColor(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(tokenBackgroundColorKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getKeyTextColor(@NonNull String themeId) {
    final String key = keyTextColorKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setKeyTextColor(@NonNull String themeId, @ColorInt int argb) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(keyTextColorKey(themeId), argb);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearKeyTextColor(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(keyTextColorKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getSpecialKeyTextColor(@NonNull String themeId) {
    final String key = specialKeyTextColorKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setSpecialKeyTextColor(@NonNull String themeId, @ColorInt int argb) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(specialKeyTextColorKey(themeId), argb);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearSpecialKeyTextColor(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(specialKeyTextColorKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getSpacebarTextColor(@NonNull String themeId) {
    final String key = spacebarTextColorKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setSpacebarTextColor(@NonNull String themeId, @ColorInt int argb) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(spacebarTextColorKey(themeId), argb);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearSpacebarTextColor(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(spacebarTextColorKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getModifierKeyTextColor(@NonNull String themeId) {
    final String key = modifierKeyTextColorKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setModifierKeyTextColor(@NonNull String themeId, @ColorInt int argb) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(modifierKeyTextColorKey(themeId), argb);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearModifierKeyTextColor(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(modifierKeyTextColorKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getEnterKeyTextColor(@NonNull String themeId) {
    final String key = enterKeyTextColorKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setEnterKeyTextColor(@NonNull String themeId, @ColorInt int argb) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(enterKeyTextColorKey(themeId), argb);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearEnterKeyTextColor(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(enterKeyTextColorKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getHintTextColor(@NonNull String themeId) {
    final String key = hintTextColorKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setHintTextColor(@NonNull String themeId, @ColorInt int argb) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(hintTextColorKey(themeId), argb);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearHintTextColor(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(hintTextColorKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getKeyBackgroundTint(@NonNull String themeId) {
    final String key = keyBackgroundTintKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setKeyBackgroundTint(@NonNull String themeId, @ColorInt int argb) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(keyBackgroundTintKey(themeId), argb);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearKeyBackgroundTint(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(keyBackgroundTintKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getSpecialKeyBackgroundTint(@NonNull String themeId) {
    final String key = specialKeyBackgroundTintKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setSpecialKeyBackgroundTint(@NonNull String themeId, @ColorInt int argb) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(specialKeyBackgroundTintKey(themeId), argb);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearSpecialKeyBackgroundTint(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(specialKeyBackgroundTintKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getSpacebarBackgroundTint(@NonNull String themeId) {
    final String key = spacebarBackgroundTintKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setSpacebarBackgroundTint(@NonNull String themeId, @ColorInt int argb) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(spacebarBackgroundTintKey(themeId), argb);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearSpacebarBackgroundTint(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(spacebarBackgroundTintKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getModifierKeyBackgroundTint(@NonNull String themeId) {
    final String key = modifierKeyBackgroundTintKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setModifierKeyBackgroundTint(@NonNull String themeId, @ColorInt int argb) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(modifierKeyBackgroundTintKey(themeId), argb);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearModifierKeyBackgroundTint(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(modifierKeyBackgroundTintKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getEnterKeyBackgroundTint(@NonNull String themeId) {
    final String key = enterKeyBackgroundTintKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setEnterKeyBackgroundTint(@NonNull String themeId, @ColorInt int argb) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(enterKeyBackgroundTintKey(themeId), argb);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearEnterKeyBackgroundTint(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(enterKeyBackgroundTintKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getKeyboardBackgroundTint(@NonNull String themeId) {
    final String key = keyboardBackgroundTintKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setKeyboardBackgroundTint(@NonNull String themeId, @ColorInt int argb) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(keyboardBackgroundTintKey(themeId), argb);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearKeyboardBackgroundTint(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(keyboardBackgroundTintKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getKeyBackgroundOpacityPercent(@NonNull String themeId) {
    final String key = keyBackgroundOpacityPercentKey(themeId);
    if (!prefs.contains(key)) return null;
    return clampPercent(prefs.getInt(key, 100));
  }

  public void setKeyBackgroundOpacityPercent(@NonNull String themeId, int opacityPercent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(keyBackgroundOpacityPercentKey(themeId), clampPercent(opacityPercent));
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearKeyBackgroundOpacityPercent(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(keyBackgroundOpacityPercentKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getKeyboardBackgroundOpacityPercent(@NonNull String themeId) {
    final String key = keyboardBackgroundOpacityPercentKey(themeId);
    if (!prefs.contains(key)) return null;
    return clampPercent(prefs.getInt(key, 100));
  }

  public void setKeyboardBackgroundOpacityPercent(@NonNull String themeId, int opacityPercent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(keyboardBackgroundOpacityPercentKey(themeId), clampPercent(opacityPercent));
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearKeyboardBackgroundOpacityPercent(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(keyboardBackgroundOpacityPercentKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Boolean getEnsureReadableTextEnabled(@NonNull String themeId) {
    final String key = ensureReadableTextEnabledKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getBoolean(key, false);
  }

  public boolean isEnsureReadableTextEnabled(@NonNull String themeId) {
    return Boolean.TRUE.equals(getEnsureReadableTextEnabled(themeId));
  }

  public void setEnsureReadableTextEnabled(@NonNull String themeId, boolean enabled) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean(ensureReadableTextEnabledKey(themeId), enabled);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearEnsureReadableTextEnabled(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(ensureReadableTextEnabledKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearColorOverrides(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(ensureReadableTextEnabledKey(themeId));
    editor.remove(tokenPrimaryTextColorKey(themeId));
    editor.remove(tokenSecondaryTextColorKey(themeId));
    editor.remove(tokenAccentColorKey(themeId));
    editor.remove(tokenKeySurfaceColorKey(themeId));
    editor.remove(tokenBackgroundColorKey(themeId));
    editor.remove(keyTextColorKey(themeId));
    editor.remove(specialKeyTextColorKey(themeId));
    editor.remove(spacebarTextColorKey(themeId));
    editor.remove(modifierKeyTextColorKey(themeId));
    editor.remove(enterKeyTextColorKey(themeId));
    editor.remove(hintTextColorKey(themeId));
    editor.remove(keyBackgroundTintKey(themeId));
    editor.remove(specialKeyBackgroundTintKey(themeId));
    editor.remove(spacebarBackgroundTintKey(themeId));
    editor.remove(modifierKeyBackgroundTintKey(themeId));
    editor.remove(enterKeyBackgroundTintKey(themeId));
    editor.remove(keyboardBackgroundTintKey(themeId));
    editor.remove(keyBackgroundOpacityPercentKey(themeId));
    editor.remove(keyboardBackgroundOpacityPercentKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  void markChanged(@NonNull String themeId, @NonNull SharedPreferences.Editor editor) {
    markChanged(prefs, themeId, editor);
  }

  static void markChanged(
      @NonNull SharedPreferences prefs,
      @NonNull String themeId,
      @NonNull SharedPreferences.Editor editor) {
    final String key = KeyboardThemeUserOverridesStore.changeKey(themeId);
    final int current = prefs.getInt(key, 0);
    editor.putInt(key, current + 1);
  }

  private static int clampPercent(int value) {
    return Math.max(0, Math.min(100, value));
  }
}
