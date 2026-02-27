package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.SharedPreferences;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Stores user-selected theme appearance overrides per theme id. */
abstract class KeyboardThemeUserOverridesTextShadowTokensPrefs
    extends KeyboardThemeUserOverridesTypographyPrefs {
  private static final String PREF_TOKEN_SECONDARY_TEXT_SHADOW_COLOR_PREFIX =
      "theme_user_token_secondary_text_shadow_color::";
  private static final String PREF_TOKEN_SECONDARY_TEXT_SHADOW_RADIUS_DP_PREFIX =
      "theme_user_token_secondary_text_shadow_radius_dp::";
  private static final String PREF_TOKEN_SECONDARY_TEXT_SHADOW_OFFSET_X_DP_PREFIX =
      "theme_user_token_secondary_text_shadow_offset_x_dp::";
  private static final String PREF_TOKEN_SECONDARY_TEXT_SHADOW_OFFSET_Y_DP_PREFIX =
      "theme_user_token_secondary_text_shadow_offset_y_dp::";

  KeyboardThemeUserOverridesTextShadowTokensPrefs(@NonNull SharedPreferences prefs) {
    super(prefs);
  }

  @NonNull
  private static String tokenSecondaryTextShadowColorKey(@NonNull String themeId) {
    return PREF_TOKEN_SECONDARY_TEXT_SHADOW_COLOR_PREFIX + themeId;
  }

  @NonNull
  private static String tokenSecondaryTextShadowRadiusDpKey(@NonNull String themeId) {
    return PREF_TOKEN_SECONDARY_TEXT_SHADOW_RADIUS_DP_PREFIX + themeId;
  }

  @NonNull
  private static String tokenSecondaryTextShadowOffsetXDpKey(@NonNull String themeId) {
    return PREF_TOKEN_SECONDARY_TEXT_SHADOW_OFFSET_X_DP_PREFIX + themeId;
  }

  @NonNull
  private static String tokenSecondaryTextShadowOffsetYDpKey(@NonNull String themeId) {
    return PREF_TOKEN_SECONDARY_TEXT_SHADOW_OFFSET_Y_DP_PREFIX + themeId;
  }

  @Nullable
  public Integer getTokenSecondaryTextShadowColor(@NonNull String themeId) {
    return getOptionalInt(tokenSecondaryTextShadowColorKey(themeId));
  }

  public void setTokenSecondaryTextShadowColor(@NonNull String themeId, @ColorInt int argb) {
    putIntAndMarkChanged(themeId, tokenSecondaryTextShadowColorKey(themeId), argb);
  }

  public void clearTokenSecondaryTextShadowColor(@NonNull String themeId) {
    removeAndMarkChanged(themeId, tokenSecondaryTextShadowColorKey(themeId));
  }

  @Nullable
  public Integer getTokenSecondaryTextShadowRadiusDp(@NonNull String themeId) {
    return getOptionalInt(tokenSecondaryTextShadowRadiusDpKey(themeId));
  }

  public void setTokenSecondaryTextShadowRadiusDp(@NonNull String themeId, int radiusDp) {
    putIntAndMarkChanged(themeId, tokenSecondaryTextShadowRadiusDpKey(themeId), radiusDp);
  }

  public void clearTokenSecondaryTextShadowRadiusDp(@NonNull String themeId) {
    removeAndMarkChanged(themeId, tokenSecondaryTextShadowRadiusDpKey(themeId));
  }

  @Nullable
  public Integer getTokenSecondaryTextShadowOffsetXDp(@NonNull String themeId) {
    return getOptionalInt(tokenSecondaryTextShadowOffsetXDpKey(themeId));
  }

  public void setTokenSecondaryTextShadowOffsetXDp(@NonNull String themeId, int offsetDp) {
    putIntAndMarkChanged(themeId, tokenSecondaryTextShadowOffsetXDpKey(themeId), offsetDp);
  }

  public void clearTokenSecondaryTextShadowOffsetXDp(@NonNull String themeId) {
    removeAndMarkChanged(themeId, tokenSecondaryTextShadowOffsetXDpKey(themeId));
  }

  @Nullable
  public Integer getTokenSecondaryTextShadowOffsetYDp(@NonNull String themeId) {
    return getOptionalInt(tokenSecondaryTextShadowOffsetYDpKey(themeId));
  }

  public void setTokenSecondaryTextShadowOffsetYDp(@NonNull String themeId, int offsetDp) {
    putIntAndMarkChanged(themeId, tokenSecondaryTextShadowOffsetYDpKey(themeId), offsetDp);
  }

  public void clearTokenSecondaryTextShadowOffsetYDp(@NonNull String themeId) {
    removeAndMarkChanged(themeId, tokenSecondaryTextShadowOffsetYDpKey(themeId));
  }

  protected final void removeTokenSecondaryTextShadowOverridesNoChange(
      @NonNull String themeId, @NonNull SharedPreferences.Editor editor) {
    editor.remove(tokenSecondaryTextShadowColorKey(themeId));
    editor.remove(tokenSecondaryTextShadowRadiusDpKey(themeId));
    editor.remove(tokenSecondaryTextShadowOffsetXDpKey(themeId));
    editor.remove(tokenSecondaryTextShadowOffsetYDpKey(themeId));
  }

  protected final void copyTokenSecondaryTextShadowOverridesNoChange(
      @NonNull SharedPreferences.Editor editor,
      @NonNull String sourceThemeId,
      @NonNull String targetThemeId) {
    copyIntOrRemove(
        editor,
        tokenSecondaryTextShadowColorKey(sourceThemeId),
        tokenSecondaryTextShadowColorKey(targetThemeId));
    copyIntOrRemove(
        editor,
        tokenSecondaryTextShadowRadiusDpKey(sourceThemeId),
        tokenSecondaryTextShadowRadiusDpKey(targetThemeId));
    copyIntOrRemove(
        editor,
        tokenSecondaryTextShadowOffsetXDpKey(sourceThemeId),
        tokenSecondaryTextShadowOffsetXDpKey(targetThemeId));
    copyIntOrRemove(
        editor,
        tokenSecondaryTextShadowOffsetYDpKey(sourceThemeId),
        tokenSecondaryTextShadowOffsetYDpKey(targetThemeId));
  }

  @Nullable
  protected final Integer getOptionalInt(@NonNull String key) {
    if (!prefs.contains(key)) return null;
    return prefs.getInt(key, 0);
  }

  protected final void putIntAndMarkChanged(
      @NonNull String themeId, @NonNull String key, int value) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.putInt(key, value);
    markChanged(themeId, editor);
    editor.apply();
  }

  protected final void removeAndMarkChanged(@NonNull String themeId, @NonNull String key) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(key);
    markChanged(themeId, editor);
    editor.apply();
  }

  protected final void putBooleanOrRemoveAndMarkChanged(
      @NonNull String themeId, @NonNull String key, boolean enabled) {
    final SharedPreferences.Editor editor = prefs.edit();
    if (enabled) {
      editor.putBoolean(key, true);
    } else {
      editor.remove(key);
    }
    markChanged(themeId, editor);
    editor.apply();
  }

  protected final void copyIntOrRemove(
      @NonNull SharedPreferences.Editor editor,
      @NonNull String sourceKey,
      @NonNull String targetKey) {
    if (prefs.contains(sourceKey)) {
      editor.putInt(targetKey, prefs.getInt(sourceKey, 0));
    } else {
      editor.remove(targetKey);
    }
  }

  protected final void copyStringOrRemove(
      @NonNull SharedPreferences.Editor editor,
      @NonNull String sourceKey,
      @NonNull String targetKey) {
    if (prefs.contains(sourceKey)) {
      editor.putString(targetKey, prefs.getString(sourceKey, null));
    } else {
      editor.remove(targetKey);
    }
  }

  protected final void copyBooleanOrRemove(
      @NonNull SharedPreferences.Editor editor,
      @NonNull String sourceKey,
      @NonNull String targetKey) {
    if (prefs.contains(sourceKey)) {
      editor.putBoolean(targetKey, prefs.getBoolean(sourceKey, true));
    } else {
      editor.remove(targetKey);
    }
  }
}
