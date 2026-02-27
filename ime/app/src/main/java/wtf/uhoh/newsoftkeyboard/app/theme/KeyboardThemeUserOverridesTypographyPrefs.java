package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Stores user-selected theme appearance overrides per theme id. */
abstract class KeyboardThemeUserOverridesTypographyPrefs
    extends KeyboardThemeUserOverridesColorsPrefs {
  private static final String PREF_TOKEN_SECONDARY_FONT_FAMILY_PREFIX =
      "theme_user_token_secondary_font_family::";
  private static final String PREF_TOKEN_SECONDARY_FONT_STYLE_PREFIX =
      "theme_user_token_secondary_font_style::";
  private static final String PREF_TOKEN_SECONDARY_TEXT_SIZE_PERCENT_PREFIX =
      "theme_user_token_secondary_text_size_percent::";

  private static final String PREF_KEY_FONT_FAMILY_PREFIX = "theme_user_key_font_family::";
  private static final String PREF_KEY_FONT_STYLE_PREFIX = "theme_user_key_font_style::";
  private static final String PREF_HINT_FONT_FAMILY_PREFIX = "theme_user_hint_font_family::";
  private static final String PREF_HINT_FONT_STYLE_PREFIX = "theme_user_hint_font_style::";
  private static final String PREF_SUGGESTION_FONT_FAMILY_PREFIX =
      "theme_user_suggestion_font_family::";
  private static final String PREF_SUGGESTION_FONT_STYLE_PREFIX =
      "theme_user_suggestion_font_style::";
  private static final String PREF_KEYBOARD_NAME_FONT_FAMILY_PREFIX =
      "theme_user_keyboard_name_font_family::";
  private static final String PREF_KEYBOARD_NAME_FONT_STYLE_PREFIX =
      "theme_user_keyboard_name_font_style::";
  private static final String PREF_KEY_CUSTOM_FONT_NAME_PREFIX =
      "theme_user_key_custom_font_name::";
  private static final String PREF_KEY_LABEL_AUTO_FIT_ENABLED_PREFIX =
      "theme_user_key_label_auto_fit_enabled::";
  private static final String PREF_KEY_LABEL_AUTO_FIT_MIN_SIZE_PERCENT_PREFIX =
      "theme_user_key_label_auto_fit_min_size_percent::";
  private static final String PREF_KEY_LABEL_ELLIPSIZE_ENABLED_PREFIX =
      "theme_user_key_label_ellipsize_enabled::";
  private static final String PREF_KEY_LABEL_TEXT_SIZE_PERCENT_PREFIX =
      "theme_user_key_label_text_size_percent::";
  private static final String PREF_HINT_TEXT_SIZE_PERCENT_PREFIX =
      "theme_user_hint_text_size_percent::";
  private static final String PREF_SUGGESTION_TEXT_SIZE_PERCENT_PREFIX =
      "theme_user_suggestion_text_size_percent::";
  private static final String PREF_KEYBOARD_NAME_TEXT_SIZE_PERCENT_PREFIX =
      "theme_user_keyboard_name_text_size_percent::";

  KeyboardThemeUserOverridesTypographyPrefs(@NonNull SharedPreferences prefs) {
    super(prefs);
  }

  @NonNull
  protected static String keyFontFamilyKey(@NonNull String themeId) {
    return PREF_KEY_FONT_FAMILY_PREFIX + themeId;
  }

  @NonNull
  protected static String tokenSecondaryFontFamilyKey(@NonNull String themeId) {
    return PREF_TOKEN_SECONDARY_FONT_FAMILY_PREFIX + themeId;
  }

  @NonNull
  protected static String tokenSecondaryFontStyleKey(@NonNull String themeId) {
    return PREF_TOKEN_SECONDARY_FONT_STYLE_PREFIX + themeId;
  }

  @NonNull
  protected static String tokenSecondaryTextSizePercentKey(@NonNull String themeId) {
    return PREF_TOKEN_SECONDARY_TEXT_SIZE_PERCENT_PREFIX + themeId;
  }

  @NonNull
  protected static String keyFontStyleKey(@NonNull String themeId) {
    return PREF_KEY_FONT_STYLE_PREFIX + themeId;
  }

  @NonNull
  protected static String hintFontFamilyKey(@NonNull String themeId) {
    return PREF_HINT_FONT_FAMILY_PREFIX + themeId;
  }

  @NonNull
  protected static String hintFontStyleKey(@NonNull String themeId) {
    return PREF_HINT_FONT_STYLE_PREFIX + themeId;
  }

  @NonNull
  protected static String suggestionFontFamilyKey(@NonNull String themeId) {
    return PREF_SUGGESTION_FONT_FAMILY_PREFIX + themeId;
  }

  @NonNull
  protected static String suggestionFontStyleKey(@NonNull String themeId) {
    return PREF_SUGGESTION_FONT_STYLE_PREFIX + themeId;
  }

  @NonNull
  protected static String keyboardNameFontFamilyKey(@NonNull String themeId) {
    return PREF_KEYBOARD_NAME_FONT_FAMILY_PREFIX + themeId;
  }

  @NonNull
  protected static String keyboardNameFontStyleKey(@NonNull String themeId) {
    return PREF_KEYBOARD_NAME_FONT_STYLE_PREFIX + themeId;
  }

  @NonNull
  static String keyCustomFontNameKey(@NonNull String themeId) {
    return PREF_KEY_CUSTOM_FONT_NAME_PREFIX + themeId;
  }

  @NonNull
  protected static String keyLabelAutoFitEnabledKey(@NonNull String themeId) {
    return PREF_KEY_LABEL_AUTO_FIT_ENABLED_PREFIX + themeId;
  }

  @NonNull
  protected static String keyLabelAutoFitMinSizePercentKey(@NonNull String themeId) {
    return PREF_KEY_LABEL_AUTO_FIT_MIN_SIZE_PERCENT_PREFIX + themeId;
  }

  @NonNull
  protected static String keyLabelEllipsizeEnabledKey(@NonNull String themeId) {
    return PREF_KEY_LABEL_ELLIPSIZE_ENABLED_PREFIX + themeId;
  }

  @NonNull
  protected static String keyLabelTextSizePercentKey(@NonNull String themeId) {
    return PREF_KEY_LABEL_TEXT_SIZE_PERCENT_PREFIX + themeId;
  }

  @NonNull
  protected static String hintTextSizePercentKey(@NonNull String themeId) {
    return PREF_HINT_TEXT_SIZE_PERCENT_PREFIX + themeId;
  }

  @NonNull
  protected static String suggestionTextSizePercentKey(@NonNull String themeId) {
    return PREF_SUGGESTION_TEXT_SIZE_PERCENT_PREFIX + themeId;
  }

  @NonNull
  protected static String keyboardNameTextSizePercentKey(@NonNull String themeId) {
    return PREF_KEYBOARD_NAME_TEXT_SIZE_PERCENT_PREFIX + themeId;
  }

  @Nullable
  public String getTokenSecondaryFontFamily(@NonNull String themeId) {
    final String key = tokenSecondaryFontFamilyKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getString(key, null);
  }

  public void setTokenSecondaryFontFamily(@NonNull String themeId, @NonNull String fontFamilyId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putString(tokenSecondaryFontFamilyKey(themeId), fontFamilyId);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearTokenSecondaryFontFamily(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(tokenSecondaryFontFamilyKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getTokenSecondaryFontStyle(@NonNull String themeId) {
    final String key = tokenSecondaryFontStyleKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setTokenSecondaryFontStyle(@NonNull String themeId, int fontStyle) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(tokenSecondaryFontStyleKey(themeId), fontStyle);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearTokenSecondaryFontStyle(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(tokenSecondaryFontStyleKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getTokenSecondaryTextSizePercent(@NonNull String themeId) {
    final String key = tokenSecondaryTextSizePercentKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setTokenSecondaryTextSizePercent(@NonNull String themeId, int percent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(tokenSecondaryTextSizePercentKey(themeId), percent);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearTokenSecondaryTextSizePercent(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(tokenSecondaryTextSizePercentKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Boolean getKeyLabelAutoFitEnabled(@NonNull String themeId) {
    final String key = keyLabelAutoFitEnabledKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getBoolean(key, true);
  }

  public boolean isKeyLabelAutoFitEnabled(@NonNull String themeId) {
    final Boolean value = getKeyLabelAutoFitEnabled(themeId);
    return value == null || value;
  }

  public void setKeyLabelAutoFitEnabled(@NonNull String themeId, boolean enabled) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean(keyLabelAutoFitEnabledKey(themeId), enabled);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearKeyLabelAutoFitEnabled(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(keyLabelAutoFitEnabledKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getKeyLabelAutoFitMinSizePercent(@NonNull String themeId) {
    final String key = keyLabelAutoFitMinSizePercentKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public int getKeyLabelAutoFitMinSizePercentOrDefault(@NonNull String themeId) {
    final Integer value = getKeyLabelAutoFitMinSizePercent(themeId);
    if (value == null) return 30;
    return Math.max(10, Math.min(100, value));
  }

  public void setKeyLabelAutoFitMinSizePercent(@NonNull String themeId, int percent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(keyLabelAutoFitMinSizePercentKey(themeId), Math.max(10, Math.min(100, percent)));
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearKeyLabelAutoFitMinSizePercent(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(keyLabelAutoFitMinSizePercentKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Boolean getKeyLabelEllipsizeEnabled(@NonNull String themeId) {
    final String key = keyLabelEllipsizeEnabledKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getBoolean(key, true);
  }

  public boolean isKeyLabelEllipsizeEnabled(@NonNull String themeId) {
    final Boolean value = getKeyLabelEllipsizeEnabled(themeId);
    return value == null || value;
  }

  public void setKeyLabelEllipsizeEnabled(@NonNull String themeId, boolean enabled) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean(keyLabelEllipsizeEnabledKey(themeId), enabled);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearKeyLabelEllipsizeEnabled(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(keyLabelEllipsizeEnabledKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getKeyLabelTextSizePercent(@NonNull String themeId) {
    final String key = keyLabelTextSizePercentKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setKeyLabelTextSizePercent(@NonNull String themeId, int percent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(keyLabelTextSizePercentKey(themeId), percent);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearKeyLabelTextSizePercent(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(keyLabelTextSizePercentKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getHintTextSizePercent(@NonNull String themeId) {
    final String key = hintTextSizePercentKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setHintTextSizePercent(@NonNull String themeId, int percent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(hintTextSizePercentKey(themeId), percent);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearHintTextSizePercent(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(hintTextSizePercentKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getSuggestionTextSizePercent(@NonNull String themeId) {
    final String key = suggestionTextSizePercentKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setSuggestionTextSizePercent(@NonNull String themeId, int percent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(suggestionTextSizePercentKey(themeId), percent);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearSuggestionTextSizePercent(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(suggestionTextSizePercentKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getKeyboardNameTextSizePercent(@NonNull String themeId) {
    final String key = keyboardNameTextSizePercentKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setKeyboardNameTextSizePercent(@NonNull String themeId, int percent) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(keyboardNameTextSizePercentKey(themeId), percent);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearKeyboardNameTextSizePercent(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(keyboardNameTextSizePercentKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public String getKeyFontFamily(@NonNull String themeId) {
    final String key = keyFontFamilyKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getString(key, null);
  }

  public void setKeyFontFamily(@NonNull String themeId, @NonNull String fontFamilyId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putString(keyFontFamilyKey(themeId), fontFamilyId);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearKeyFontFamily(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(keyFontFamilyKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getKeyFontStyle(@NonNull String themeId) {
    final String key = keyFontStyleKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setKeyFontStyle(@NonNull String themeId, int fontStyle) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(keyFontStyleKey(themeId), fontStyle);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearKeyFontStyle(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(keyFontStyleKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public String getHintFontFamily(@NonNull String themeId) {
    final String key = hintFontFamilyKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getString(key, null);
  }

  public void setHintFontFamily(@NonNull String themeId, @NonNull String fontFamilyId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putString(hintFontFamilyKey(themeId), fontFamilyId);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearHintFontFamily(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(hintFontFamilyKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getHintFontStyle(@NonNull String themeId) {
    final String key = hintFontStyleKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setHintFontStyle(@NonNull String themeId, int fontStyle) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(hintFontStyleKey(themeId), fontStyle);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearHintFontStyle(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(hintFontStyleKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public String getSuggestionFontFamily(@NonNull String themeId) {
    final String key = suggestionFontFamilyKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getString(key, null);
  }

  public void setSuggestionFontFamily(@NonNull String themeId, @NonNull String fontFamilyId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putString(suggestionFontFamilyKey(themeId), fontFamilyId);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearSuggestionFontFamily(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(suggestionFontFamilyKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getSuggestionFontStyle(@NonNull String themeId) {
    final String key = suggestionFontStyleKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setSuggestionFontStyle(@NonNull String themeId, int fontStyle) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(suggestionFontStyleKey(themeId), fontStyle);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearSuggestionFontStyle(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(suggestionFontStyleKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public String getKeyboardNameFontFamily(@NonNull String themeId) {
    final String key = keyboardNameFontFamilyKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getString(key, null);
  }

  public void setKeyboardNameFontFamily(@NonNull String themeId, @NonNull String fontFamilyId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putString(keyboardNameFontFamilyKey(themeId), fontFamilyId);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearKeyboardNameFontFamily(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(keyboardNameFontFamilyKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  @Nullable
  public Integer getKeyboardNameFontStyle(@NonNull String themeId) {
    final String key = keyboardNameFontStyleKey(themeId);
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  public void setKeyboardNameFontStyle(@NonNull String themeId, int fontStyle) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(keyboardNameFontStyleKey(themeId), fontStyle);
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearKeyboardNameFontStyle(@NonNull String themeId) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(keyboardNameFontStyleKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearTypographyOverrides(@NonNull String themeId) {
    clearCustomKeyFontFileNoChange(themeId);
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(tokenSecondaryFontFamilyKey(themeId));
    editor.remove(tokenSecondaryFontStyleKey(themeId));
    editor.remove(tokenSecondaryTextSizePercentKey(themeId));
    editor.remove(keyFontFamilyKey(themeId));
    editor.remove(keyFontStyleKey(themeId));
    editor.remove(hintFontFamilyKey(themeId));
    editor.remove(hintFontStyleKey(themeId));
    editor.remove(suggestionFontFamilyKey(themeId));
    editor.remove(suggestionFontStyleKey(themeId));
    editor.remove(keyboardNameFontFamilyKey(themeId));
    editor.remove(keyboardNameFontStyleKey(themeId));
    editor.remove(keyCustomFontNameKey(themeId));
    editor.remove(keyLabelAutoFitEnabledKey(themeId));
    editor.remove(keyLabelAutoFitMinSizePercentKey(themeId));
    editor.remove(keyLabelEllipsizeEnabledKey(themeId));
    editor.remove(keyLabelTextSizePercentKey(themeId));
    editor.remove(hintTextSizePercentKey(themeId));
    editor.remove(suggestionTextSizePercentKey(themeId));
    editor.remove(keyboardNameTextSizePercentKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  protected abstract void clearCustomKeyFontFileNoChange(@NonNull String themeId);

  protected abstract void copyCustomKeyFontFile(
      @NonNull String sourceThemeId, @NonNull String targetThemeId);
}
