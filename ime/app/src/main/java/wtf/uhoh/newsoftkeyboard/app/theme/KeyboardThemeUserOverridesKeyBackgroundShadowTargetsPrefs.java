package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.SharedPreferences;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Stores user-selected theme appearance overrides per theme id. */
abstract class KeyboardThemeUserOverridesKeyBackgroundShadowTargetsPrefs
    extends KeyboardThemeUserOverridesKeyBackgroundShadowTokensPrefs {
  private static final String PREF_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY_PREFIX =
      "theme_user_key_background_shadow_use_token_secondary::";
  private static final String PREF_SPECIAL_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY_PREFIX =
      "theme_user_special_key_background_shadow_use_token_secondary::";
  private static final String PREF_SPACEBAR_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY_PREFIX =
      "theme_user_spacebar_key_background_shadow_use_token_secondary::";
  private static final String PREF_MODIFIER_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY_PREFIX =
      "theme_user_modifier_key_background_shadow_use_token_secondary::";
  private static final String PREF_ENTER_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY_PREFIX =
      "theme_user_enter_key_background_shadow_use_token_secondary::";

  private static final String PREF_KEY_BACKGROUND_SHADOW_COLOR_PREFIX =
      "theme_user_key_background_shadow_color::";
  private static final String PREF_KEY_BACKGROUND_SHADOW_OFFSET_X_DP_PREFIX =
      "theme_user_key_background_shadow_offset_x_dp::";
  private static final String PREF_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP_PREFIX =
      "theme_user_key_background_shadow_offset_y_dp::";
  private static final String PREF_SPECIAL_KEY_BACKGROUND_SHADOW_COLOR_PREFIX =
      "theme_user_special_key_background_shadow_color::";
  private static final String PREF_SPECIAL_KEY_BACKGROUND_SHADOW_OFFSET_X_DP_PREFIX =
      "theme_user_special_key_background_shadow_offset_x_dp::";
  private static final String PREF_SPECIAL_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP_PREFIX =
      "theme_user_special_key_background_shadow_offset_y_dp::";
  private static final String PREF_SPACEBAR_KEY_BACKGROUND_SHADOW_COLOR_PREFIX =
      "theme_user_spacebar_key_background_shadow_color::";
  private static final String PREF_SPACEBAR_KEY_BACKGROUND_SHADOW_OFFSET_X_DP_PREFIX =
      "theme_user_spacebar_key_background_shadow_offset_x_dp::";
  private static final String PREF_SPACEBAR_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP_PREFIX =
      "theme_user_spacebar_key_background_shadow_offset_y_dp::";
  private static final String PREF_MODIFIER_KEY_BACKGROUND_SHADOW_COLOR_PREFIX =
      "theme_user_modifier_key_background_shadow_color::";
  private static final String PREF_MODIFIER_KEY_BACKGROUND_SHADOW_OFFSET_X_DP_PREFIX =
      "theme_user_modifier_key_background_shadow_offset_x_dp::";
  private static final String PREF_MODIFIER_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP_PREFIX =
      "theme_user_modifier_key_background_shadow_offset_y_dp::";
  private static final String PREF_ENTER_KEY_BACKGROUND_SHADOW_COLOR_PREFIX =
      "theme_user_enter_key_background_shadow_color::";
  private static final String PREF_ENTER_KEY_BACKGROUND_SHADOW_OFFSET_X_DP_PREFIX =
      "theme_user_enter_key_background_shadow_offset_x_dp::";
  private static final String PREF_ENTER_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP_PREFIX =
      "theme_user_enter_key_background_shadow_offset_y_dp::";
  private static final String PREF_KEY_BACKGROUND_SHADOW_SPREAD_DP_PREFIX =
      "theme_user_key_background_shadow_spread_dp::";

  private enum KeyBackgroundShadowTarget {
    KEY(
        PREF_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY_PREFIX,
        PREF_KEY_BACKGROUND_SHADOW_COLOR_PREFIX,
        PREF_KEY_BACKGROUND_SHADOW_OFFSET_X_DP_PREFIX,
        PREF_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP_PREFIX,
        PREF_KEY_BACKGROUND_SHADOW_SPREAD_DP_PREFIX),
    SPECIAL_KEY(
        PREF_SPECIAL_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY_PREFIX,
        PREF_SPECIAL_KEY_BACKGROUND_SHADOW_COLOR_PREFIX,
        PREF_SPECIAL_KEY_BACKGROUND_SHADOW_OFFSET_X_DP_PREFIX,
        PREF_SPECIAL_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP_PREFIX,
        null),
    SPACEBAR_KEY(
        PREF_SPACEBAR_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY_PREFIX,
        PREF_SPACEBAR_KEY_BACKGROUND_SHADOW_COLOR_PREFIX,
        PREF_SPACEBAR_KEY_BACKGROUND_SHADOW_OFFSET_X_DP_PREFIX,
        PREF_SPACEBAR_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP_PREFIX,
        null),
    MODIFIER_KEY(
        PREF_MODIFIER_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY_PREFIX,
        PREF_MODIFIER_KEY_BACKGROUND_SHADOW_COLOR_PREFIX,
        PREF_MODIFIER_KEY_BACKGROUND_SHADOW_OFFSET_X_DP_PREFIX,
        PREF_MODIFIER_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP_PREFIX,
        null),
    ENTER_KEY(
        PREF_ENTER_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY_PREFIX,
        PREF_ENTER_KEY_BACKGROUND_SHADOW_COLOR_PREFIX,
        PREF_ENTER_KEY_BACKGROUND_SHADOW_OFFSET_X_DP_PREFIX,
        PREF_ENTER_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP_PREFIX,
        null);

    @NonNull private final String useTokenSecondaryPrefix;
    @NonNull private final String colorPrefix;
    @NonNull private final String offsetXDpPrefix;
    @NonNull private final String offsetYDpPrefix;
    @Nullable private final String spreadDpPrefix;

    KeyBackgroundShadowTarget(
        @NonNull String useTokenSecondaryPrefix,
        @NonNull String colorPrefix,
        @NonNull String offsetXDpPrefix,
        @NonNull String offsetYDpPrefix,
        @Nullable String spreadDpPrefix) {
      this.useTokenSecondaryPrefix = useTokenSecondaryPrefix;
      this.colorPrefix = colorPrefix;
      this.offsetXDpPrefix = offsetXDpPrefix;
      this.offsetYDpPrefix = offsetYDpPrefix;
      this.spreadDpPrefix = spreadDpPrefix;
    }

    @NonNull
    String useTokenSecondaryKey(@NonNull String themeId) {
      return useTokenSecondaryPrefix + themeId;
    }

    @NonNull
    String colorKey(@NonNull String themeId) {
      return colorPrefix + themeId;
    }

    @NonNull
    String offsetXDpKey(@NonNull String themeId) {
      return offsetXDpPrefix + themeId;
    }

    @NonNull
    String offsetYDpKey(@NonNull String themeId) {
      return offsetYDpPrefix + themeId;
    }

    @Nullable
    String spreadDpKeyOrNull(@NonNull String themeId) {
      if (spreadDpPrefix == null) return null;
      return spreadDpPrefix + themeId;
    }
  }

  KeyboardThemeUserOverridesKeyBackgroundShadowTargetsPrefs(@NonNull SharedPreferences prefs) {
    super(prefs);
  }

  private boolean isKeyBackgroundShadowUseTokenSecondary(
      @NonNull String themeId, @NonNull KeyBackgroundShadowTarget target) {
    return prefs.getBoolean(target.useTokenSecondaryKey(themeId), false);
  }

  private void setKeyBackgroundShadowUseTokenSecondary(
      @NonNull String themeId, @NonNull KeyBackgroundShadowTarget target, boolean enabled) {
    putBooleanOrRemoveAndMarkChanged(themeId, target.useTokenSecondaryKey(themeId), enabled);
  }

  @Nullable
  private Integer getKeyBackgroundShadowColor(
      @NonNull String themeId, @NonNull KeyBackgroundShadowTarget target) {
    return getOptionalInt(target.colorKey(themeId));
  }

  private void setKeyBackgroundShadowColor(
      @NonNull String themeId, @NonNull KeyBackgroundShadowTarget target, @ColorInt int argb) {
    putIntAndMarkChanged(themeId, target.colorKey(themeId), argb);
  }

  private void clearKeyBackgroundShadowColor(
      @NonNull String themeId, @NonNull KeyBackgroundShadowTarget target) {
    removeAndMarkChanged(themeId, target.colorKey(themeId));
  }

  @Nullable
  private Integer getKeyBackgroundShadowOffsetXDp(
      @NonNull String themeId, @NonNull KeyBackgroundShadowTarget target) {
    return getOptionalInt(target.offsetXDpKey(themeId));
  }

  private void setKeyBackgroundShadowOffsetXDp(
      @NonNull String themeId, @NonNull KeyBackgroundShadowTarget target, int offsetDp) {
    putIntAndMarkChanged(themeId, target.offsetXDpKey(themeId), offsetDp);
  }

  private void clearKeyBackgroundShadowOffsetXDp(
      @NonNull String themeId, @NonNull KeyBackgroundShadowTarget target) {
    removeAndMarkChanged(themeId, target.offsetXDpKey(themeId));
  }

  @Nullable
  private Integer getKeyBackgroundShadowOffsetYDp(
      @NonNull String themeId, @NonNull KeyBackgroundShadowTarget target) {
    return getOptionalInt(target.offsetYDpKey(themeId));
  }

  private void setKeyBackgroundShadowOffsetYDp(
      @NonNull String themeId, @NonNull KeyBackgroundShadowTarget target, int offsetDp) {
    putIntAndMarkChanged(themeId, target.offsetYDpKey(themeId), offsetDp);
  }

  private void clearKeyBackgroundShadowOffsetYDp(
      @NonNull String themeId, @NonNull KeyBackgroundShadowTarget target) {
    removeAndMarkChanged(themeId, target.offsetYDpKey(themeId));
  }

  @Nullable
  private Integer getKeyBackgroundShadowSpreadDpOrNull(
      @NonNull String themeId, @NonNull KeyBackgroundShadowTarget target) {
    final String key = target.spreadDpKeyOrNull(themeId);
    if (key == null) return null;
    return getOptionalInt(key);
  }

  private void setKeyBackgroundShadowSpreadDp(
      @NonNull String themeId, @NonNull KeyBackgroundShadowTarget target, int spreadDp) {
    final String key = target.spreadDpKeyOrNull(themeId);
    if (key == null) return;
    putIntAndMarkChanged(themeId, key, spreadDp);
  }

  private void clearKeyBackgroundShadowSpreadDp(
      @NonNull String themeId, @NonNull KeyBackgroundShadowTarget target) {
    final String key = target.spreadDpKeyOrNull(themeId);
    if (key == null) return;
    removeAndMarkChanged(themeId, key);
  }

  public boolean isKeyBackgroundShadowUseTokenSecondary(@NonNull String themeId) {
    return isKeyBackgroundShadowUseTokenSecondary(themeId, KeyBackgroundShadowTarget.KEY);
  }

  public void setKeyBackgroundShadowUseTokenSecondary(@NonNull String themeId, boolean enabled) {
    setKeyBackgroundShadowUseTokenSecondary(themeId, KeyBackgroundShadowTarget.KEY, enabled);
  }

  public boolean isSpecialKeyBackgroundShadowUseTokenSecondary(@NonNull String themeId) {
    return isKeyBackgroundShadowUseTokenSecondary(themeId, KeyBackgroundShadowTarget.SPECIAL_KEY);
  }

  public void setSpecialKeyBackgroundShadowUseTokenSecondary(
      @NonNull String themeId, boolean enabled) {
    setKeyBackgroundShadowUseTokenSecondary(
        themeId, KeyBackgroundShadowTarget.SPECIAL_KEY, enabled);
  }

  public boolean isSpacebarKeyBackgroundShadowUseTokenSecondary(@NonNull String themeId) {
    return isKeyBackgroundShadowUseTokenSecondary(themeId, KeyBackgroundShadowTarget.SPACEBAR_KEY);
  }

  public void setSpacebarKeyBackgroundShadowUseTokenSecondary(
      @NonNull String themeId, boolean enabled) {
    setKeyBackgroundShadowUseTokenSecondary(
        themeId, KeyBackgroundShadowTarget.SPACEBAR_KEY, enabled);
  }

  public boolean isModifierKeyBackgroundShadowUseTokenSecondary(@NonNull String themeId) {
    return isKeyBackgroundShadowUseTokenSecondary(themeId, KeyBackgroundShadowTarget.MODIFIER_KEY);
  }

  public void setModifierKeyBackgroundShadowUseTokenSecondary(
      @NonNull String themeId, boolean enabled) {
    setKeyBackgroundShadowUseTokenSecondary(
        themeId, KeyBackgroundShadowTarget.MODIFIER_KEY, enabled);
  }

  public boolean isEnterKeyBackgroundShadowUseTokenSecondary(@NonNull String themeId) {
    return isKeyBackgroundShadowUseTokenSecondary(themeId, KeyBackgroundShadowTarget.ENTER_KEY);
  }

  public void setEnterKeyBackgroundShadowUseTokenSecondary(
      @NonNull String themeId, boolean enabled) {
    setKeyBackgroundShadowUseTokenSecondary(themeId, KeyBackgroundShadowTarget.ENTER_KEY, enabled);
  }

  @Nullable
  public Integer getKeyBackgroundShadowColor(@NonNull String themeId) {
    return getKeyBackgroundShadowColor(themeId, KeyBackgroundShadowTarget.KEY);
  }

  public void setKeyBackgroundShadowColor(@NonNull String themeId, @ColorInt int argb) {
    setKeyBackgroundShadowColor(themeId, KeyBackgroundShadowTarget.KEY, argb);
  }

  public void clearKeyBackgroundShadowColor(@NonNull String themeId) {
    clearKeyBackgroundShadowColor(themeId, KeyBackgroundShadowTarget.KEY);
  }

  @Nullable
  public Integer getKeyBackgroundShadowOffsetXDp(@NonNull String themeId) {
    return getKeyBackgroundShadowOffsetXDp(themeId, KeyBackgroundShadowTarget.KEY);
  }

  public void setKeyBackgroundShadowOffsetXDp(@NonNull String themeId, int offsetDp) {
    setKeyBackgroundShadowOffsetXDp(themeId, KeyBackgroundShadowTarget.KEY, offsetDp);
  }

  public void clearKeyBackgroundShadowOffsetXDp(@NonNull String themeId) {
    clearKeyBackgroundShadowOffsetXDp(themeId, KeyBackgroundShadowTarget.KEY);
  }

  @Nullable
  public Integer getKeyBackgroundShadowOffsetYDp(@NonNull String themeId) {
    return getKeyBackgroundShadowOffsetYDp(themeId, KeyBackgroundShadowTarget.KEY);
  }

  public void setKeyBackgroundShadowOffsetYDp(@NonNull String themeId, int offsetDp) {
    setKeyBackgroundShadowOffsetYDp(themeId, KeyBackgroundShadowTarget.KEY, offsetDp);
  }

  public void clearKeyBackgroundShadowOffsetYDp(@NonNull String themeId) {
    clearKeyBackgroundShadowOffsetYDp(themeId, KeyBackgroundShadowTarget.KEY);
  }

  @Nullable
  public Integer getSpecialKeyBackgroundShadowColor(@NonNull String themeId) {
    return getKeyBackgroundShadowColor(themeId, KeyBackgroundShadowTarget.SPECIAL_KEY);
  }

  public void setSpecialKeyBackgroundShadowColor(@NonNull String themeId, @ColorInt int argb) {
    setKeyBackgroundShadowColor(themeId, KeyBackgroundShadowTarget.SPECIAL_KEY, argb);
  }

  public void clearSpecialKeyBackgroundShadowColor(@NonNull String themeId) {
    clearKeyBackgroundShadowColor(themeId, KeyBackgroundShadowTarget.SPECIAL_KEY);
  }

  @Nullable
  public Integer getSpecialKeyBackgroundShadowOffsetXDp(@NonNull String themeId) {
    return getKeyBackgroundShadowOffsetXDp(themeId, KeyBackgroundShadowTarget.SPECIAL_KEY);
  }

  public void setSpecialKeyBackgroundShadowOffsetXDp(@NonNull String themeId, int offsetDp) {
    setKeyBackgroundShadowOffsetXDp(themeId, KeyBackgroundShadowTarget.SPECIAL_KEY, offsetDp);
  }

  public void clearSpecialKeyBackgroundShadowOffsetXDp(@NonNull String themeId) {
    clearKeyBackgroundShadowOffsetXDp(themeId, KeyBackgroundShadowTarget.SPECIAL_KEY);
  }

  @Nullable
  public Integer getSpecialKeyBackgroundShadowOffsetYDp(@NonNull String themeId) {
    return getKeyBackgroundShadowOffsetYDp(themeId, KeyBackgroundShadowTarget.SPECIAL_KEY);
  }

  public void setSpecialKeyBackgroundShadowOffsetYDp(@NonNull String themeId, int offsetDp) {
    setKeyBackgroundShadowOffsetYDp(themeId, KeyBackgroundShadowTarget.SPECIAL_KEY, offsetDp);
  }

  public void clearSpecialKeyBackgroundShadowOffsetYDp(@NonNull String themeId) {
    clearKeyBackgroundShadowOffsetYDp(themeId, KeyBackgroundShadowTarget.SPECIAL_KEY);
  }

  @Nullable
  public Integer getSpacebarKeyBackgroundShadowColor(@NonNull String themeId) {
    return getKeyBackgroundShadowColor(themeId, KeyBackgroundShadowTarget.SPACEBAR_KEY);
  }

  public void setSpacebarKeyBackgroundShadowColor(@NonNull String themeId, @ColorInt int argb) {
    setKeyBackgroundShadowColor(themeId, KeyBackgroundShadowTarget.SPACEBAR_KEY, argb);
  }

  public void clearSpacebarKeyBackgroundShadowColor(@NonNull String themeId) {
    clearKeyBackgroundShadowColor(themeId, KeyBackgroundShadowTarget.SPACEBAR_KEY);
  }

  @Nullable
  public Integer getSpacebarKeyBackgroundShadowOffsetXDp(@NonNull String themeId) {
    return getKeyBackgroundShadowOffsetXDp(themeId, KeyBackgroundShadowTarget.SPACEBAR_KEY);
  }

  public void setSpacebarKeyBackgroundShadowOffsetXDp(@NonNull String themeId, int offsetDp) {
    setKeyBackgroundShadowOffsetXDp(themeId, KeyBackgroundShadowTarget.SPACEBAR_KEY, offsetDp);
  }

  public void clearSpacebarKeyBackgroundShadowOffsetXDp(@NonNull String themeId) {
    clearKeyBackgroundShadowOffsetXDp(themeId, KeyBackgroundShadowTarget.SPACEBAR_KEY);
  }

  @Nullable
  public Integer getSpacebarKeyBackgroundShadowOffsetYDp(@NonNull String themeId) {
    return getKeyBackgroundShadowOffsetYDp(themeId, KeyBackgroundShadowTarget.SPACEBAR_KEY);
  }

  public void setSpacebarKeyBackgroundShadowOffsetYDp(@NonNull String themeId, int offsetDp) {
    setKeyBackgroundShadowOffsetYDp(themeId, KeyBackgroundShadowTarget.SPACEBAR_KEY, offsetDp);
  }

  public void clearSpacebarKeyBackgroundShadowOffsetYDp(@NonNull String themeId) {
    clearKeyBackgroundShadowOffsetYDp(themeId, KeyBackgroundShadowTarget.SPACEBAR_KEY);
  }

  @Nullable
  public Integer getModifierKeyBackgroundShadowColor(@NonNull String themeId) {
    return getKeyBackgroundShadowColor(themeId, KeyBackgroundShadowTarget.MODIFIER_KEY);
  }

  public void setModifierKeyBackgroundShadowColor(@NonNull String themeId, @ColorInt int argb) {
    setKeyBackgroundShadowColor(themeId, KeyBackgroundShadowTarget.MODIFIER_KEY, argb);
  }

  public void clearModifierKeyBackgroundShadowColor(@NonNull String themeId) {
    clearKeyBackgroundShadowColor(themeId, KeyBackgroundShadowTarget.MODIFIER_KEY);
  }

  @Nullable
  public Integer getModifierKeyBackgroundShadowOffsetXDp(@NonNull String themeId) {
    return getKeyBackgroundShadowOffsetXDp(themeId, KeyBackgroundShadowTarget.MODIFIER_KEY);
  }

  public void setModifierKeyBackgroundShadowOffsetXDp(@NonNull String themeId, int offsetDp) {
    setKeyBackgroundShadowOffsetXDp(themeId, KeyBackgroundShadowTarget.MODIFIER_KEY, offsetDp);
  }

  public void clearModifierKeyBackgroundShadowOffsetXDp(@NonNull String themeId) {
    clearKeyBackgroundShadowOffsetXDp(themeId, KeyBackgroundShadowTarget.MODIFIER_KEY);
  }

  @Nullable
  public Integer getModifierKeyBackgroundShadowOffsetYDp(@NonNull String themeId) {
    return getKeyBackgroundShadowOffsetYDp(themeId, KeyBackgroundShadowTarget.MODIFIER_KEY);
  }

  public void setModifierKeyBackgroundShadowOffsetYDp(@NonNull String themeId, int offsetDp) {
    setKeyBackgroundShadowOffsetYDp(themeId, KeyBackgroundShadowTarget.MODIFIER_KEY, offsetDp);
  }

  public void clearModifierKeyBackgroundShadowOffsetYDp(@NonNull String themeId) {
    clearKeyBackgroundShadowOffsetYDp(themeId, KeyBackgroundShadowTarget.MODIFIER_KEY);
  }

  @Nullable
  public Integer getEnterKeyBackgroundShadowColor(@NonNull String themeId) {
    return getKeyBackgroundShadowColor(themeId, KeyBackgroundShadowTarget.ENTER_KEY);
  }

  public void setEnterKeyBackgroundShadowColor(@NonNull String themeId, @ColorInt int argb) {
    setKeyBackgroundShadowColor(themeId, KeyBackgroundShadowTarget.ENTER_KEY, argb);
  }

  public void clearEnterKeyBackgroundShadowColor(@NonNull String themeId) {
    clearKeyBackgroundShadowColor(themeId, KeyBackgroundShadowTarget.ENTER_KEY);
  }

  @Nullable
  public Integer getEnterKeyBackgroundShadowOffsetXDp(@NonNull String themeId) {
    return getKeyBackgroundShadowOffsetXDp(themeId, KeyBackgroundShadowTarget.ENTER_KEY);
  }

  public void setEnterKeyBackgroundShadowOffsetXDp(@NonNull String themeId, int offsetDp) {
    setKeyBackgroundShadowOffsetXDp(themeId, KeyBackgroundShadowTarget.ENTER_KEY, offsetDp);
  }

  public void clearEnterKeyBackgroundShadowOffsetXDp(@NonNull String themeId) {
    clearKeyBackgroundShadowOffsetXDp(themeId, KeyBackgroundShadowTarget.ENTER_KEY);
  }

  @Nullable
  public Integer getEnterKeyBackgroundShadowOffsetYDp(@NonNull String themeId) {
    return getKeyBackgroundShadowOffsetYDp(themeId, KeyBackgroundShadowTarget.ENTER_KEY);
  }

  public void setEnterKeyBackgroundShadowOffsetYDp(@NonNull String themeId, int offsetDp) {
    setKeyBackgroundShadowOffsetYDp(themeId, KeyBackgroundShadowTarget.ENTER_KEY, offsetDp);
  }

  public void clearEnterKeyBackgroundShadowOffsetYDp(@NonNull String themeId) {
    clearKeyBackgroundShadowOffsetYDp(themeId, KeyBackgroundShadowTarget.ENTER_KEY);
  }

  @Nullable
  public Integer getKeyBackgroundShadowSpreadDp(@NonNull String themeId) {
    return getKeyBackgroundShadowSpreadDpOrNull(themeId, KeyBackgroundShadowTarget.KEY);
  }

  public void setKeyBackgroundShadowSpreadDp(@NonNull String themeId, int spreadDp) {
    setKeyBackgroundShadowSpreadDp(themeId, KeyBackgroundShadowTarget.KEY, spreadDp);
  }

  public void clearKeyBackgroundShadowSpreadDp(@NonNull String themeId) {
    clearKeyBackgroundShadowSpreadDp(themeId, KeyBackgroundShadowTarget.KEY);
  }

  private void clearKeyBackgroundShadowOverrides(
      @NonNull String themeId, @NonNull KeyBackgroundShadowTarget target) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(target.useTokenSecondaryKey(themeId));
    editor.remove(target.colorKey(themeId));
    editor.remove(target.offsetXDpKey(themeId));
    editor.remove(target.offsetYDpKey(themeId));
    final String spreadKey = target.spreadDpKeyOrNull(themeId);
    if (spreadKey != null) {
      editor.remove(spreadKey);
    }
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearSpecialKeyBackgroundShadowOverrides(@NonNull String themeId) {
    clearKeyBackgroundShadowOverrides(themeId, KeyBackgroundShadowTarget.SPECIAL_KEY);
  }

  public void clearSpacebarKeyBackgroundShadowOverrides(@NonNull String themeId) {
    clearKeyBackgroundShadowOverrides(themeId, KeyBackgroundShadowTarget.SPACEBAR_KEY);
  }

  public void clearModifierKeyBackgroundShadowOverrides(@NonNull String themeId) {
    clearKeyBackgroundShadowOverrides(themeId, KeyBackgroundShadowTarget.MODIFIER_KEY);
  }

  public void clearEnterKeyBackgroundShadowOverrides(@NonNull String themeId) {
    clearKeyBackgroundShadowOverrides(themeId, KeyBackgroundShadowTarget.ENTER_KEY);
  }

  public void clearKeyBackgroundShadowOverrides(@NonNull String themeId) {
    clearKeyBackgroundShadowOverrides(themeId, KeyBackgroundShadowTarget.KEY);
  }

  protected final void removeAllKeyBackgroundShadowOverridesNoChange(
      @NonNull String themeId, @NonNull SharedPreferences.Editor editor) {
    removeTokenSecondaryKeyBackgroundShadowOverridesNoChange(themeId, editor);

    for (final KeyBackgroundShadowTarget target : KeyBackgroundShadowTarget.values()) {
      editor.remove(target.useTokenSecondaryKey(themeId));
      editor.remove(target.colorKey(themeId));
      editor.remove(target.offsetXDpKey(themeId));
      editor.remove(target.offsetYDpKey(themeId));
      final String spreadKey = target.spreadDpKeyOrNull(themeId);
      if (spreadKey != null) {
        editor.remove(spreadKey);
      }
    }
  }

  protected final void copyKeyBackgroundShadowOverridesNoChange(
      @NonNull SharedPreferences.Editor editor,
      @NonNull String sourceThemeId,
      @NonNull String targetThemeId) {
    copyTokenSecondaryKeyBackgroundShadowOverridesNoChange(editor, sourceThemeId, targetThemeId);

    for (final KeyBackgroundShadowTarget target : KeyBackgroundShadowTarget.values()) {
      copyBooleanOrRemove(
          editor,
          target.useTokenSecondaryKey(sourceThemeId),
          target.useTokenSecondaryKey(targetThemeId));
      copyIntOrRemove(editor, target.colorKey(sourceThemeId), target.colorKey(targetThemeId));
      copyIntOrRemove(
          editor, target.offsetXDpKey(sourceThemeId), target.offsetXDpKey(targetThemeId));
      copyIntOrRemove(
          editor, target.offsetYDpKey(sourceThemeId), target.offsetYDpKey(targetThemeId));
      final String sourceSpreadKey = target.spreadDpKeyOrNull(sourceThemeId);
      final String targetSpreadKey = target.spreadDpKeyOrNull(targetThemeId);
      if (sourceSpreadKey != null && targetSpreadKey != null) {
        copyIntOrRemove(editor, sourceSpreadKey, targetSpreadKey);
      }
    }
  }
}
