package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.SharedPreferences;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Stores user-selected theme appearance overrides per theme id. */
abstract class KeyboardThemeUserOverridesTextShadowTargetsPrefs
    extends KeyboardThemeUserOverridesTextShadowTokensPrefs {
  private static final String PREF_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY_PREFIX =
      "theme_user_key_text_shadow_use_token_secondary::";
  private static final String PREF_SPECIAL_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY_PREFIX =
      "theme_user_special_key_text_shadow_use_token_secondary::";
  private static final String PREF_SPACEBAR_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY_PREFIX =
      "theme_user_spacebar_key_text_shadow_use_token_secondary::";
  private static final String PREF_MODIFIER_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY_PREFIX =
      "theme_user_modifier_key_text_shadow_use_token_secondary::";
  private static final String PREF_ENTER_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY_PREFIX =
      "theme_user_enter_key_text_shadow_use_token_secondary::";

  private static final String PREF_KEY_TEXT_SHADOW_COLOR_PREFIX = "theme_user_key_shadow_color::";
  private static final String PREF_KEY_TEXT_SHADOW_RADIUS_DP_PREFIX =
      "theme_user_key_shadow_radius_dp::";
  private static final String PREF_KEY_TEXT_SHADOW_OFFSET_X_DP_PREFIX =
      "theme_user_key_shadow_offset_x_dp::";
  private static final String PREF_KEY_TEXT_SHADOW_OFFSET_Y_DP_PREFIX =
      "theme_user_key_shadow_offset_y_dp::";
  private static final String PREF_SPECIAL_KEY_TEXT_SHADOW_COLOR_PREFIX =
      "theme_user_special_key_text_shadow_color::";
  private static final String PREF_SPECIAL_KEY_TEXT_SHADOW_RADIUS_DP_PREFIX =
      "theme_user_special_key_text_shadow_radius_dp::";
  private static final String PREF_SPECIAL_KEY_TEXT_SHADOW_OFFSET_X_DP_PREFIX =
      "theme_user_special_key_text_shadow_offset_x_dp::";
  private static final String PREF_SPECIAL_KEY_TEXT_SHADOW_OFFSET_Y_DP_PREFIX =
      "theme_user_special_key_text_shadow_offset_y_dp::";
  private static final String PREF_SPACEBAR_KEY_TEXT_SHADOW_COLOR_PREFIX =
      "theme_user_spacebar_key_text_shadow_color::";
  private static final String PREF_SPACEBAR_KEY_TEXT_SHADOW_RADIUS_DP_PREFIX =
      "theme_user_spacebar_key_text_shadow_radius_dp::";
  private static final String PREF_SPACEBAR_KEY_TEXT_SHADOW_OFFSET_X_DP_PREFIX =
      "theme_user_spacebar_key_text_shadow_offset_x_dp::";
  private static final String PREF_SPACEBAR_KEY_TEXT_SHADOW_OFFSET_Y_DP_PREFIX =
      "theme_user_spacebar_key_text_shadow_offset_y_dp::";
  private static final String PREF_MODIFIER_KEY_TEXT_SHADOW_COLOR_PREFIX =
      "theme_user_modifier_key_text_shadow_color::";
  private static final String PREF_MODIFIER_KEY_TEXT_SHADOW_RADIUS_DP_PREFIX =
      "theme_user_modifier_key_text_shadow_radius_dp::";
  private static final String PREF_MODIFIER_KEY_TEXT_SHADOW_OFFSET_X_DP_PREFIX =
      "theme_user_modifier_key_text_shadow_offset_x_dp::";
  private static final String PREF_MODIFIER_KEY_TEXT_SHADOW_OFFSET_Y_DP_PREFIX =
      "theme_user_modifier_key_text_shadow_offset_y_dp::";
  private static final String PREF_ENTER_KEY_TEXT_SHADOW_COLOR_PREFIX =
      "theme_user_enter_key_text_shadow_color::";
  private static final String PREF_ENTER_KEY_TEXT_SHADOW_RADIUS_DP_PREFIX =
      "theme_user_enter_key_text_shadow_radius_dp::";
  private static final String PREF_ENTER_KEY_TEXT_SHADOW_OFFSET_X_DP_PREFIX =
      "theme_user_enter_key_text_shadow_offset_x_dp::";
  private static final String PREF_ENTER_KEY_TEXT_SHADOW_OFFSET_Y_DP_PREFIX =
      "theme_user_enter_key_text_shadow_offset_y_dp::";

  private enum TextShadowTarget {
    KEY(
        PREF_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY_PREFIX,
        PREF_KEY_TEXT_SHADOW_COLOR_PREFIX,
        PREF_KEY_TEXT_SHADOW_RADIUS_DP_PREFIX,
        PREF_KEY_TEXT_SHADOW_OFFSET_X_DP_PREFIX,
        PREF_KEY_TEXT_SHADOW_OFFSET_Y_DP_PREFIX),
    SPECIAL_KEY(
        PREF_SPECIAL_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY_PREFIX,
        PREF_SPECIAL_KEY_TEXT_SHADOW_COLOR_PREFIX,
        PREF_SPECIAL_KEY_TEXT_SHADOW_RADIUS_DP_PREFIX,
        PREF_SPECIAL_KEY_TEXT_SHADOW_OFFSET_X_DP_PREFIX,
        PREF_SPECIAL_KEY_TEXT_SHADOW_OFFSET_Y_DP_PREFIX),
    SPACEBAR_KEY(
        PREF_SPACEBAR_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY_PREFIX,
        PREF_SPACEBAR_KEY_TEXT_SHADOW_COLOR_PREFIX,
        PREF_SPACEBAR_KEY_TEXT_SHADOW_RADIUS_DP_PREFIX,
        PREF_SPACEBAR_KEY_TEXT_SHADOW_OFFSET_X_DP_PREFIX,
        PREF_SPACEBAR_KEY_TEXT_SHADOW_OFFSET_Y_DP_PREFIX),
    MODIFIER_KEY(
        PREF_MODIFIER_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY_PREFIX,
        PREF_MODIFIER_KEY_TEXT_SHADOW_COLOR_PREFIX,
        PREF_MODIFIER_KEY_TEXT_SHADOW_RADIUS_DP_PREFIX,
        PREF_MODIFIER_KEY_TEXT_SHADOW_OFFSET_X_DP_PREFIX,
        PREF_MODIFIER_KEY_TEXT_SHADOW_OFFSET_Y_DP_PREFIX),
    ENTER_KEY(
        PREF_ENTER_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY_PREFIX,
        PREF_ENTER_KEY_TEXT_SHADOW_COLOR_PREFIX,
        PREF_ENTER_KEY_TEXT_SHADOW_RADIUS_DP_PREFIX,
        PREF_ENTER_KEY_TEXT_SHADOW_OFFSET_X_DP_PREFIX,
        PREF_ENTER_KEY_TEXT_SHADOW_OFFSET_Y_DP_PREFIX);

    @NonNull private final String useTokenSecondaryPrefix;
    @NonNull private final String colorPrefix;
    @NonNull private final String radiusDpPrefix;
    @NonNull private final String offsetXDpPrefix;
    @NonNull private final String offsetYDpPrefix;

    TextShadowTarget(
        @NonNull String useTokenSecondaryPrefix,
        @NonNull String colorPrefix,
        @NonNull String radiusDpPrefix,
        @NonNull String offsetXDpPrefix,
        @NonNull String offsetYDpPrefix) {
      this.useTokenSecondaryPrefix = useTokenSecondaryPrefix;
      this.colorPrefix = colorPrefix;
      this.radiusDpPrefix = radiusDpPrefix;
      this.offsetXDpPrefix = offsetXDpPrefix;
      this.offsetYDpPrefix = offsetYDpPrefix;
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
    String radiusDpKey(@NonNull String themeId) {
      return radiusDpPrefix + themeId;
    }

    @NonNull
    String offsetXDpKey(@NonNull String themeId) {
      return offsetXDpPrefix + themeId;
    }

    @NonNull
    String offsetYDpKey(@NonNull String themeId) {
      return offsetYDpPrefix + themeId;
    }
  }

  KeyboardThemeUserOverridesTextShadowTargetsPrefs(@NonNull SharedPreferences prefs) {
    super(prefs);
  }

  private boolean isTextShadowUseTokenSecondary(
      @NonNull String themeId, @NonNull TextShadowTarget target) {
    return prefs.getBoolean(target.useTokenSecondaryKey(themeId), false);
  }

  private void setTextShadowUseTokenSecondary(
      @NonNull String themeId, @NonNull TextShadowTarget target, boolean enabled) {
    putBooleanOrRemoveAndMarkChanged(themeId, target.useTokenSecondaryKey(themeId), enabled);
  }

  @Nullable
  private Integer getTextShadowColor(@NonNull String themeId, @NonNull TextShadowTarget target) {
    return getOptionalInt(target.colorKey(themeId));
  }

  private void setTextShadowColor(
      @NonNull String themeId, @NonNull TextShadowTarget target, @ColorInt int argb) {
    putIntAndMarkChanged(themeId, target.colorKey(themeId), argb);
  }

  private void clearTextShadowColor(@NonNull String themeId, @NonNull TextShadowTarget target) {
    removeAndMarkChanged(themeId, target.colorKey(themeId));
  }

  @Nullable
  private Integer getTextShadowRadiusDp(@NonNull String themeId, @NonNull TextShadowTarget target) {
    return getOptionalInt(target.radiusDpKey(themeId));
  }

  private void setTextShadowRadiusDp(
      @NonNull String themeId, @NonNull TextShadowTarget target, int radiusDp) {
    putIntAndMarkChanged(themeId, target.radiusDpKey(themeId), radiusDp);
  }

  private void clearTextShadowRadiusDp(@NonNull String themeId, @NonNull TextShadowTarget target) {
    removeAndMarkChanged(themeId, target.radiusDpKey(themeId));
  }

  @Nullable
  private Integer getTextShadowOffsetXDp(
      @NonNull String themeId, @NonNull TextShadowTarget target) {
    return getOptionalInt(target.offsetXDpKey(themeId));
  }

  private void setTextShadowOffsetXDp(
      @NonNull String themeId, @NonNull TextShadowTarget target, int offsetDp) {
    putIntAndMarkChanged(themeId, target.offsetXDpKey(themeId), offsetDp);
  }

  private void clearTextShadowOffsetXDp(@NonNull String themeId, @NonNull TextShadowTarget target) {
    removeAndMarkChanged(themeId, target.offsetXDpKey(themeId));
  }

  @Nullable
  private Integer getTextShadowOffsetYDp(
      @NonNull String themeId, @NonNull TextShadowTarget target) {
    return getOptionalInt(target.offsetYDpKey(themeId));
  }

  private void setTextShadowOffsetYDp(
      @NonNull String themeId, @NonNull TextShadowTarget target, int offsetDp) {
    putIntAndMarkChanged(themeId, target.offsetYDpKey(themeId), offsetDp);
  }

  private void clearTextShadowOffsetYDp(@NonNull String themeId, @NonNull TextShadowTarget target) {
    removeAndMarkChanged(themeId, target.offsetYDpKey(themeId));
  }

  public boolean isKeyTextShadowUseTokenSecondary(@NonNull String themeId) {
    return isTextShadowUseTokenSecondary(themeId, TextShadowTarget.KEY);
  }

  public void setKeyTextShadowUseTokenSecondary(@NonNull String themeId, boolean enabled) {
    setTextShadowUseTokenSecondary(themeId, TextShadowTarget.KEY, enabled);
  }

  public boolean isSpecialKeyTextShadowUseTokenSecondary(@NonNull String themeId) {
    return isTextShadowUseTokenSecondary(themeId, TextShadowTarget.SPECIAL_KEY);
  }

  public void setSpecialKeyTextShadowUseTokenSecondary(@NonNull String themeId, boolean enabled) {
    setTextShadowUseTokenSecondary(themeId, TextShadowTarget.SPECIAL_KEY, enabled);
  }

  public boolean isSpacebarKeyTextShadowUseTokenSecondary(@NonNull String themeId) {
    return isTextShadowUseTokenSecondary(themeId, TextShadowTarget.SPACEBAR_KEY);
  }

  public void setSpacebarKeyTextShadowUseTokenSecondary(@NonNull String themeId, boolean enabled) {
    setTextShadowUseTokenSecondary(themeId, TextShadowTarget.SPACEBAR_KEY, enabled);
  }

  public boolean isModifierKeyTextShadowUseTokenSecondary(@NonNull String themeId) {
    return isTextShadowUseTokenSecondary(themeId, TextShadowTarget.MODIFIER_KEY);
  }

  public void setModifierKeyTextShadowUseTokenSecondary(@NonNull String themeId, boolean enabled) {
    setTextShadowUseTokenSecondary(themeId, TextShadowTarget.MODIFIER_KEY, enabled);
  }

  public boolean isEnterKeyTextShadowUseTokenSecondary(@NonNull String themeId) {
    return isTextShadowUseTokenSecondary(themeId, TextShadowTarget.ENTER_KEY);
  }

  public void setEnterKeyTextShadowUseTokenSecondary(@NonNull String themeId, boolean enabled) {
    setTextShadowUseTokenSecondary(themeId, TextShadowTarget.ENTER_KEY, enabled);
  }

  @Nullable
  public Integer getKeyTextShadowColor(@NonNull String themeId) {
    return getTextShadowColor(themeId, TextShadowTarget.KEY);
  }

  public void setKeyTextShadowColor(@NonNull String themeId, @ColorInt int argb) {
    setTextShadowColor(themeId, TextShadowTarget.KEY, argb);
  }

  public void clearKeyTextShadowColor(@NonNull String themeId) {
    clearTextShadowColor(themeId, TextShadowTarget.KEY);
  }

  @Nullable
  public Integer getKeyTextShadowRadiusDp(@NonNull String themeId) {
    return getTextShadowRadiusDp(themeId, TextShadowTarget.KEY);
  }

  public void setKeyTextShadowRadiusDp(@NonNull String themeId, int radiusDp) {
    setTextShadowRadiusDp(themeId, TextShadowTarget.KEY, radiusDp);
  }

  public void clearKeyTextShadowRadiusDp(@NonNull String themeId) {
    clearTextShadowRadiusDp(themeId, TextShadowTarget.KEY);
  }

  @Nullable
  public Integer getKeyTextShadowOffsetXDp(@NonNull String themeId) {
    return getTextShadowOffsetXDp(themeId, TextShadowTarget.KEY);
  }

  public void setKeyTextShadowOffsetXDp(@NonNull String themeId, int offsetDp) {
    setTextShadowOffsetXDp(themeId, TextShadowTarget.KEY, offsetDp);
  }

  public void clearKeyTextShadowOffsetXDp(@NonNull String themeId) {
    clearTextShadowOffsetXDp(themeId, TextShadowTarget.KEY);
  }

  @Nullable
  public Integer getKeyTextShadowOffsetYDp(@NonNull String themeId) {
    return getTextShadowOffsetYDp(themeId, TextShadowTarget.KEY);
  }

  public void setKeyTextShadowOffsetYDp(@NonNull String themeId, int offsetDp) {
    setTextShadowOffsetYDp(themeId, TextShadowTarget.KEY, offsetDp);
  }

  public void clearKeyTextShadowOffsetYDp(@NonNull String themeId) {
    clearTextShadowOffsetYDp(themeId, TextShadowTarget.KEY);
  }

  @Nullable
  public Integer getSpecialKeyTextShadowColor(@NonNull String themeId) {
    return getTextShadowColor(themeId, TextShadowTarget.SPECIAL_KEY);
  }

  public void setSpecialKeyTextShadowColor(@NonNull String themeId, @ColorInt int argb) {
    setTextShadowColor(themeId, TextShadowTarget.SPECIAL_KEY, argb);
  }

  public void clearSpecialKeyTextShadowColor(@NonNull String themeId) {
    clearTextShadowColor(themeId, TextShadowTarget.SPECIAL_KEY);
  }

  @Nullable
  public Integer getSpecialKeyTextShadowRadiusDp(@NonNull String themeId) {
    return getTextShadowRadiusDp(themeId, TextShadowTarget.SPECIAL_KEY);
  }

  public void setSpecialKeyTextShadowRadiusDp(@NonNull String themeId, int radiusDp) {
    setTextShadowRadiusDp(themeId, TextShadowTarget.SPECIAL_KEY, radiusDp);
  }

  public void clearSpecialKeyTextShadowRadiusDp(@NonNull String themeId) {
    clearTextShadowRadiusDp(themeId, TextShadowTarget.SPECIAL_KEY);
  }

  @Nullable
  public Integer getSpecialKeyTextShadowOffsetXDp(@NonNull String themeId) {
    return getTextShadowOffsetXDp(themeId, TextShadowTarget.SPECIAL_KEY);
  }

  public void setSpecialKeyTextShadowOffsetXDp(@NonNull String themeId, int offsetDp) {
    setTextShadowOffsetXDp(themeId, TextShadowTarget.SPECIAL_KEY, offsetDp);
  }

  public void clearSpecialKeyTextShadowOffsetXDp(@NonNull String themeId) {
    clearTextShadowOffsetXDp(themeId, TextShadowTarget.SPECIAL_KEY);
  }

  @Nullable
  public Integer getSpecialKeyTextShadowOffsetYDp(@NonNull String themeId) {
    return getTextShadowOffsetYDp(themeId, TextShadowTarget.SPECIAL_KEY);
  }

  public void setSpecialKeyTextShadowOffsetYDp(@NonNull String themeId, int offsetDp) {
    setTextShadowOffsetYDp(themeId, TextShadowTarget.SPECIAL_KEY, offsetDp);
  }

  public void clearSpecialKeyTextShadowOffsetYDp(@NonNull String themeId) {
    clearTextShadowOffsetYDp(themeId, TextShadowTarget.SPECIAL_KEY);
  }

  @Nullable
  public Integer getSpacebarKeyTextShadowColor(@NonNull String themeId) {
    return getTextShadowColor(themeId, TextShadowTarget.SPACEBAR_KEY);
  }

  public void setSpacebarKeyTextShadowColor(@NonNull String themeId, @ColorInt int argb) {
    setTextShadowColor(themeId, TextShadowTarget.SPACEBAR_KEY, argb);
  }

  public void clearSpacebarKeyTextShadowColor(@NonNull String themeId) {
    clearTextShadowColor(themeId, TextShadowTarget.SPACEBAR_KEY);
  }

  @Nullable
  public Integer getSpacebarKeyTextShadowRadiusDp(@NonNull String themeId) {
    return getTextShadowRadiusDp(themeId, TextShadowTarget.SPACEBAR_KEY);
  }

  public void setSpacebarKeyTextShadowRadiusDp(@NonNull String themeId, int radiusDp) {
    setTextShadowRadiusDp(themeId, TextShadowTarget.SPACEBAR_KEY, radiusDp);
  }

  public void clearSpacebarKeyTextShadowRadiusDp(@NonNull String themeId) {
    clearTextShadowRadiusDp(themeId, TextShadowTarget.SPACEBAR_KEY);
  }

  @Nullable
  public Integer getSpacebarKeyTextShadowOffsetXDp(@NonNull String themeId) {
    return getTextShadowOffsetXDp(themeId, TextShadowTarget.SPACEBAR_KEY);
  }

  public void setSpacebarKeyTextShadowOffsetXDp(@NonNull String themeId, int offsetDp) {
    setTextShadowOffsetXDp(themeId, TextShadowTarget.SPACEBAR_KEY, offsetDp);
  }

  public void clearSpacebarKeyTextShadowOffsetXDp(@NonNull String themeId) {
    clearTextShadowOffsetXDp(themeId, TextShadowTarget.SPACEBAR_KEY);
  }

  @Nullable
  public Integer getSpacebarKeyTextShadowOffsetYDp(@NonNull String themeId) {
    return getTextShadowOffsetYDp(themeId, TextShadowTarget.SPACEBAR_KEY);
  }

  public void setSpacebarKeyTextShadowOffsetYDp(@NonNull String themeId, int offsetDp) {
    setTextShadowOffsetYDp(themeId, TextShadowTarget.SPACEBAR_KEY, offsetDp);
  }

  public void clearSpacebarKeyTextShadowOffsetYDp(@NonNull String themeId) {
    clearTextShadowOffsetYDp(themeId, TextShadowTarget.SPACEBAR_KEY);
  }

  @Nullable
  public Integer getModifierKeyTextShadowColor(@NonNull String themeId) {
    return getTextShadowColor(themeId, TextShadowTarget.MODIFIER_KEY);
  }

  public void setModifierKeyTextShadowColor(@NonNull String themeId, @ColorInt int argb) {
    setTextShadowColor(themeId, TextShadowTarget.MODIFIER_KEY, argb);
  }

  public void clearModifierKeyTextShadowColor(@NonNull String themeId) {
    clearTextShadowColor(themeId, TextShadowTarget.MODIFIER_KEY);
  }

  @Nullable
  public Integer getModifierKeyTextShadowRadiusDp(@NonNull String themeId) {
    return getTextShadowRadiusDp(themeId, TextShadowTarget.MODIFIER_KEY);
  }

  public void setModifierKeyTextShadowRadiusDp(@NonNull String themeId, int radiusDp) {
    setTextShadowRadiusDp(themeId, TextShadowTarget.MODIFIER_KEY, radiusDp);
  }

  public void clearModifierKeyTextShadowRadiusDp(@NonNull String themeId) {
    clearTextShadowRadiusDp(themeId, TextShadowTarget.MODIFIER_KEY);
  }

  @Nullable
  public Integer getModifierKeyTextShadowOffsetXDp(@NonNull String themeId) {
    return getTextShadowOffsetXDp(themeId, TextShadowTarget.MODIFIER_KEY);
  }

  public void setModifierKeyTextShadowOffsetXDp(@NonNull String themeId, int offsetDp) {
    setTextShadowOffsetXDp(themeId, TextShadowTarget.MODIFIER_KEY, offsetDp);
  }

  public void clearModifierKeyTextShadowOffsetXDp(@NonNull String themeId) {
    clearTextShadowOffsetXDp(themeId, TextShadowTarget.MODIFIER_KEY);
  }

  @Nullable
  public Integer getModifierKeyTextShadowOffsetYDp(@NonNull String themeId) {
    return getTextShadowOffsetYDp(themeId, TextShadowTarget.MODIFIER_KEY);
  }

  public void setModifierKeyTextShadowOffsetYDp(@NonNull String themeId, int offsetDp) {
    setTextShadowOffsetYDp(themeId, TextShadowTarget.MODIFIER_KEY, offsetDp);
  }

  public void clearModifierKeyTextShadowOffsetYDp(@NonNull String themeId) {
    clearTextShadowOffsetYDp(themeId, TextShadowTarget.MODIFIER_KEY);
  }

  @Nullable
  public Integer getEnterKeyTextShadowColor(@NonNull String themeId) {
    return getTextShadowColor(themeId, TextShadowTarget.ENTER_KEY);
  }

  public void setEnterKeyTextShadowColor(@NonNull String themeId, @ColorInt int argb) {
    setTextShadowColor(themeId, TextShadowTarget.ENTER_KEY, argb);
  }

  public void clearEnterKeyTextShadowColor(@NonNull String themeId) {
    clearTextShadowColor(themeId, TextShadowTarget.ENTER_KEY);
  }

  @Nullable
  public Integer getEnterKeyTextShadowRadiusDp(@NonNull String themeId) {
    return getTextShadowRadiusDp(themeId, TextShadowTarget.ENTER_KEY);
  }

  public void setEnterKeyTextShadowRadiusDp(@NonNull String themeId, int radiusDp) {
    setTextShadowRadiusDp(themeId, TextShadowTarget.ENTER_KEY, radiusDp);
  }

  public void clearEnterKeyTextShadowRadiusDp(@NonNull String themeId) {
    clearTextShadowRadiusDp(themeId, TextShadowTarget.ENTER_KEY);
  }

  @Nullable
  public Integer getEnterKeyTextShadowOffsetXDp(@NonNull String themeId) {
    return getTextShadowOffsetXDp(themeId, TextShadowTarget.ENTER_KEY);
  }

  public void setEnterKeyTextShadowOffsetXDp(@NonNull String themeId, int offsetDp) {
    setTextShadowOffsetXDp(themeId, TextShadowTarget.ENTER_KEY, offsetDp);
  }

  public void clearEnterKeyTextShadowOffsetXDp(@NonNull String themeId) {
    clearTextShadowOffsetXDp(themeId, TextShadowTarget.ENTER_KEY);
  }

  @Nullable
  public Integer getEnterKeyTextShadowOffsetYDp(@NonNull String themeId) {
    return getTextShadowOffsetYDp(themeId, TextShadowTarget.ENTER_KEY);
  }

  public void setEnterKeyTextShadowOffsetYDp(@NonNull String themeId, int offsetDp) {
    setTextShadowOffsetYDp(themeId, TextShadowTarget.ENTER_KEY, offsetDp);
  }

  public void clearEnterKeyTextShadowOffsetYDp(@NonNull String themeId) {
    clearTextShadowOffsetYDp(themeId, TextShadowTarget.ENTER_KEY);
  }

  private void clearTextShadowOverrides(@NonNull String themeId, @NonNull TextShadowTarget target) {
    final SharedPreferences.Editor editor = prefs.edit();
    editor.remove(target.useTokenSecondaryKey(themeId));
    editor.remove(target.colorKey(themeId));
    editor.remove(target.radiusDpKey(themeId));
    editor.remove(target.offsetXDpKey(themeId));
    editor.remove(target.offsetYDpKey(themeId));
    markChanged(themeId, editor);
    editor.apply();
  }

  public void clearSpecialKeyTextShadowOverrides(@NonNull String themeId) {
    clearTextShadowOverrides(themeId, TextShadowTarget.SPECIAL_KEY);
  }

  public void clearSpacebarKeyTextShadowOverrides(@NonNull String themeId) {
    clearTextShadowOverrides(themeId, TextShadowTarget.SPACEBAR_KEY);
  }

  public void clearModifierKeyTextShadowOverrides(@NonNull String themeId) {
    clearTextShadowOverrides(themeId, TextShadowTarget.MODIFIER_KEY);
  }

  public void clearEnterKeyTextShadowOverrides(@NonNull String themeId) {
    clearTextShadowOverrides(themeId, TextShadowTarget.ENTER_KEY);
  }

  public void clearTextShadowOverrides(@NonNull String themeId) {
    clearTextShadowOverrides(themeId, TextShadowTarget.KEY);
  }

  protected final void removeAllTextShadowOverridesNoChange(
      @NonNull String themeId, @NonNull SharedPreferences.Editor editor) {
    removeTokenSecondaryTextShadowOverridesNoChange(themeId, editor);

    for (final TextShadowTarget target : TextShadowTarget.values()) {
      editor.remove(target.useTokenSecondaryKey(themeId));
      editor.remove(target.colorKey(themeId));
      editor.remove(target.radiusDpKey(themeId));
      editor.remove(target.offsetXDpKey(themeId));
      editor.remove(target.offsetYDpKey(themeId));
    }
  }

  protected final void copyTextShadowOverridesNoChange(
      @NonNull SharedPreferences.Editor editor,
      @NonNull String sourceThemeId,
      @NonNull String targetThemeId) {
    copyTokenSecondaryTextShadowOverridesNoChange(editor, sourceThemeId, targetThemeId);

    for (final TextShadowTarget target : TextShadowTarget.values()) {
      copyBooleanOrRemove(
          editor,
          target.useTokenSecondaryKey(sourceThemeId),
          target.useTokenSecondaryKey(targetThemeId));
      copyIntOrRemove(editor, target.colorKey(sourceThemeId), target.colorKey(targetThemeId));
      copyIntOrRemove(editor, target.radiusDpKey(sourceThemeId), target.radiusDpKey(targetThemeId));
      copyIntOrRemove(
          editor, target.offsetXDpKey(sourceThemeId), target.offsetXDpKey(targetThemeId));
      copyIntOrRemove(
          editor, target.offsetYDpKey(sourceThemeId), target.offsetYDpKey(targetThemeId));
    }
  }
}
