package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.SharedPreferences;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Stores user-selected theme appearance overrides per theme id. */
abstract class KeyboardThemeUserOverridesKeyBackgroundShadowTokensPrefs
    extends KeyboardThemeUserOverridesTextShadowsPrefs {
  private static final String PREF_TOKEN_SECONDARY_KEY_BACKGROUND_SHADOW_COLOR_PREFIX =
      "theme_user_token_secondary_key_background_shadow_color::";
  private static final String PREF_TOKEN_SECONDARY_KEY_BACKGROUND_SHADOW_OFFSET_X_DP_PREFIX =
      "theme_user_token_secondary_key_background_shadow_offset_x_dp::";
  private static final String PREF_TOKEN_SECONDARY_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP_PREFIX =
      "theme_user_token_secondary_key_background_shadow_offset_y_dp::";
  private static final String PREF_TOKEN_SECONDARY_KEY_BACKGROUND_SHADOW_SPREAD_DP_PREFIX =
      "theme_user_token_secondary_key_background_shadow_spread_dp::";

  KeyboardThemeUserOverridesKeyBackgroundShadowTokensPrefs(@NonNull SharedPreferences prefs) {
    super(prefs);
  }

  @NonNull
  private static String tokenSecondaryKeyBackgroundShadowColorKey(@NonNull String themeId) {
    return PREF_TOKEN_SECONDARY_KEY_BACKGROUND_SHADOW_COLOR_PREFIX + themeId;
  }

  @NonNull
  private static String tokenSecondaryKeyBackgroundShadowOffsetXDpKey(@NonNull String themeId) {
    return PREF_TOKEN_SECONDARY_KEY_BACKGROUND_SHADOW_OFFSET_X_DP_PREFIX + themeId;
  }

  @NonNull
  private static String tokenSecondaryKeyBackgroundShadowOffsetYDpKey(@NonNull String themeId) {
    return PREF_TOKEN_SECONDARY_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP_PREFIX + themeId;
  }

  @NonNull
  private static String tokenSecondaryKeyBackgroundShadowSpreadDpKey(@NonNull String themeId) {
    return PREF_TOKEN_SECONDARY_KEY_BACKGROUND_SHADOW_SPREAD_DP_PREFIX + themeId;
  }

  @Nullable
  public Integer getTokenSecondaryKeyBackgroundShadowColor(@NonNull String themeId) {
    return getOptionalInt(tokenSecondaryKeyBackgroundShadowColorKey(themeId));
  }

  public void setTokenSecondaryKeyBackgroundShadowColor(
      @NonNull String themeId, @ColorInt int argb) {
    putIntAndMarkChanged(themeId, tokenSecondaryKeyBackgroundShadowColorKey(themeId), argb);
  }

  public void clearTokenSecondaryKeyBackgroundShadowColor(@NonNull String themeId) {
    removeAndMarkChanged(themeId, tokenSecondaryKeyBackgroundShadowColorKey(themeId));
  }

  @Nullable
  public Integer getTokenSecondaryKeyBackgroundShadowOffsetXDp(@NonNull String themeId) {
    return getOptionalInt(tokenSecondaryKeyBackgroundShadowOffsetXDpKey(themeId));
  }

  public void setTokenSecondaryKeyBackgroundShadowOffsetXDp(@NonNull String themeId, int offsetDp) {
    putIntAndMarkChanged(themeId, tokenSecondaryKeyBackgroundShadowOffsetXDpKey(themeId), offsetDp);
  }

  public void clearTokenSecondaryKeyBackgroundShadowOffsetXDp(@NonNull String themeId) {
    removeAndMarkChanged(themeId, tokenSecondaryKeyBackgroundShadowOffsetXDpKey(themeId));
  }

  @Nullable
  public Integer getTokenSecondaryKeyBackgroundShadowOffsetYDp(@NonNull String themeId) {
    return getOptionalInt(tokenSecondaryKeyBackgroundShadowOffsetYDpKey(themeId));
  }

  public void setTokenSecondaryKeyBackgroundShadowOffsetYDp(@NonNull String themeId, int offsetDp) {
    putIntAndMarkChanged(themeId, tokenSecondaryKeyBackgroundShadowOffsetYDpKey(themeId), offsetDp);
  }

  public void clearTokenSecondaryKeyBackgroundShadowOffsetYDp(@NonNull String themeId) {
    removeAndMarkChanged(themeId, tokenSecondaryKeyBackgroundShadowOffsetYDpKey(themeId));
  }

  @Nullable
  public Integer getTokenSecondaryKeyBackgroundShadowSpreadDp(@NonNull String themeId) {
    return getOptionalInt(tokenSecondaryKeyBackgroundShadowSpreadDpKey(themeId));
  }

  public void setTokenSecondaryKeyBackgroundShadowSpreadDp(@NonNull String themeId, int spreadDp) {
    putIntAndMarkChanged(themeId, tokenSecondaryKeyBackgroundShadowSpreadDpKey(themeId), spreadDp);
  }

  public void clearTokenSecondaryKeyBackgroundShadowSpreadDp(@NonNull String themeId) {
    removeAndMarkChanged(themeId, tokenSecondaryKeyBackgroundShadowSpreadDpKey(themeId));
  }

  protected final void removeTokenSecondaryKeyBackgroundShadowOverridesNoChange(
      @NonNull String themeId, @NonNull SharedPreferences.Editor editor) {
    editor.remove(tokenSecondaryKeyBackgroundShadowColorKey(themeId));
    editor.remove(tokenSecondaryKeyBackgroundShadowOffsetXDpKey(themeId));
    editor.remove(tokenSecondaryKeyBackgroundShadowOffsetYDpKey(themeId));
    editor.remove(tokenSecondaryKeyBackgroundShadowSpreadDpKey(themeId));
  }

  protected final void copyTokenSecondaryKeyBackgroundShadowOverridesNoChange(
      @NonNull SharedPreferences.Editor editor,
      @NonNull String sourceThemeId,
      @NonNull String targetThemeId) {
    copyIntOrRemove(
        editor,
        tokenSecondaryKeyBackgroundShadowColorKey(sourceThemeId),
        tokenSecondaryKeyBackgroundShadowColorKey(targetThemeId));
    copyIntOrRemove(
        editor,
        tokenSecondaryKeyBackgroundShadowOffsetXDpKey(sourceThemeId),
        tokenSecondaryKeyBackgroundShadowOffsetXDpKey(targetThemeId));
    copyIntOrRemove(
        editor,
        tokenSecondaryKeyBackgroundShadowOffsetYDpKey(sourceThemeId),
        tokenSecondaryKeyBackgroundShadowOffsetYDpKey(targetThemeId));
    copyIntOrRemove(
        editor,
        tokenSecondaryKeyBackgroundShadowSpreadDpKey(sourceThemeId),
        tokenSecondaryKeyBackgroundShadowSpreadDpKey(targetThemeId));
  }
}
