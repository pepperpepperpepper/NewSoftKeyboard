package wtf.uhoh.newsoftkeyboard.app.theme;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

final class KeyboardThemePresetArchiveShadowOverrides {

  private static final String KEY_TOKEN_SECONDARY_TEXT_SHADOW_COLOR =
      "token_secondary_text_shadow_color";
  private static final String KEY_TOKEN_SECONDARY_TEXT_SHADOW_RADIUS_DP =
      "token_secondary_text_shadow_radius_dp";
  private static final String KEY_TOKEN_SECONDARY_TEXT_SHADOW_OFFSET_X_DP =
      "token_secondary_text_shadow_offset_x_dp";
  private static final String KEY_TOKEN_SECONDARY_TEXT_SHADOW_OFFSET_Y_DP =
      "token_secondary_text_shadow_offset_y_dp";
  private static final String KEY_TOKEN_SECONDARY_KEY_SHADOW_COLOR =
      "token_secondary_key_shadow_color";
  private static final String KEY_TOKEN_SECONDARY_KEY_SHADOW_OFFSET_X_DP =
      "token_secondary_key_shadow_offset_x_dp";
  private static final String KEY_TOKEN_SECONDARY_KEY_SHADOW_OFFSET_Y_DP =
      "token_secondary_key_shadow_offset_y_dp";
  private static final String KEY_TOKEN_SECONDARY_KEY_SHADOW_SPREAD_DP =
      "token_secondary_key_shadow_spread_dp";

  private static final String KEY_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY =
      "key_text_shadow_use_token_secondary";
  private static final String KEY_SPECIAL_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY =
      "special_key_text_shadow_use_token_secondary";
  private static final String KEY_SPACEBAR_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY =
      "spacebar_key_text_shadow_use_token_secondary";
  private static final String KEY_MODIFIER_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY =
      "modifier_key_text_shadow_use_token_secondary";
  private static final String KEY_ENTER_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY =
      "enter_key_text_shadow_use_token_secondary";

  private static final String KEY_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY =
      "key_background_shadow_use_token_secondary";
  private static final String KEY_SPECIAL_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY =
      "special_key_background_shadow_use_token_secondary";
  private static final String KEY_SPACEBAR_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY =
      "spacebar_key_background_shadow_use_token_secondary";
  private static final String KEY_MODIFIER_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY =
      "modifier_key_background_shadow_use_token_secondary";
  private static final String KEY_ENTER_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY =
      "enter_key_background_shadow_use_token_secondary";

  private static final String KEY_KEY_TEXT_SHADOW_COLOR = "key_text_shadow_color";
  private static final String KEY_KEY_TEXT_SHADOW_RADIUS_DP = "key_text_shadow_radius_dp";
  private static final String KEY_KEY_TEXT_SHADOW_OFFSET_X_DP = "key_text_shadow_offset_x_dp";
  private static final String KEY_KEY_TEXT_SHADOW_OFFSET_Y_DP = "key_text_shadow_offset_y_dp";
  private static final String KEY_SPECIAL_KEY_TEXT_SHADOW_COLOR = "special_key_text_shadow_color";
  private static final String KEY_SPECIAL_KEY_TEXT_SHADOW_RADIUS_DP =
      "special_key_text_shadow_radius_dp";
  private static final String KEY_SPECIAL_KEY_TEXT_SHADOW_OFFSET_X_DP =
      "special_key_text_shadow_offset_x_dp";
  private static final String KEY_SPECIAL_KEY_TEXT_SHADOW_OFFSET_Y_DP =
      "special_key_text_shadow_offset_y_dp";
  private static final String KEY_SPACEBAR_KEY_TEXT_SHADOW_COLOR = "spacebar_key_text_shadow_color";
  private static final String KEY_SPACEBAR_KEY_TEXT_SHADOW_RADIUS_DP =
      "spacebar_key_text_shadow_radius_dp";
  private static final String KEY_SPACEBAR_KEY_TEXT_SHADOW_OFFSET_X_DP =
      "spacebar_key_text_shadow_offset_x_dp";
  private static final String KEY_SPACEBAR_KEY_TEXT_SHADOW_OFFSET_Y_DP =
      "spacebar_key_text_shadow_offset_y_dp";
  private static final String KEY_MODIFIER_KEY_TEXT_SHADOW_COLOR = "modifier_key_text_shadow_color";
  private static final String KEY_MODIFIER_KEY_TEXT_SHADOW_RADIUS_DP =
      "modifier_key_text_shadow_radius_dp";
  private static final String KEY_MODIFIER_KEY_TEXT_SHADOW_OFFSET_X_DP =
      "modifier_key_text_shadow_offset_x_dp";
  private static final String KEY_MODIFIER_KEY_TEXT_SHADOW_OFFSET_Y_DP =
      "modifier_key_text_shadow_offset_y_dp";
  private static final String KEY_ENTER_KEY_TEXT_SHADOW_COLOR = "enter_key_text_shadow_color";
  private static final String KEY_ENTER_KEY_TEXT_SHADOW_RADIUS_DP =
      "enter_key_text_shadow_radius_dp";
  private static final String KEY_ENTER_KEY_TEXT_SHADOW_OFFSET_X_DP =
      "enter_key_text_shadow_offset_x_dp";
  private static final String KEY_ENTER_KEY_TEXT_SHADOW_OFFSET_Y_DP =
      "enter_key_text_shadow_offset_y_dp";
  private static final String KEY_KEY_BACKGROUND_SHADOW_COLOR = "key_background_shadow_color";
  private static final String KEY_KEY_BACKGROUND_SHADOW_OFFSET_X_DP =
      "key_background_shadow_offset_x_dp";
  private static final String KEY_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP =
      "key_background_shadow_offset_y_dp";
  private static final String KEY_KEY_BACKGROUND_SHADOW_SPREAD_DP =
      "key_background_shadow_spread_dp";
  private static final String KEY_SPECIAL_KEY_BACKGROUND_SHADOW_COLOR =
      "special_key_background_shadow_color";
  private static final String KEY_SPECIAL_KEY_BACKGROUND_SHADOW_OFFSET_X_DP =
      "special_key_background_shadow_offset_x_dp";
  private static final String KEY_SPECIAL_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP =
      "special_key_background_shadow_offset_y_dp";
  private static final String KEY_SPACEBAR_KEY_BACKGROUND_SHADOW_COLOR =
      "spacebar_key_background_shadow_color";
  private static final String KEY_SPACEBAR_KEY_BACKGROUND_SHADOW_OFFSET_X_DP =
      "spacebar_key_background_shadow_offset_x_dp";
  private static final String KEY_SPACEBAR_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP =
      "spacebar_key_background_shadow_offset_y_dp";
  private static final String KEY_MODIFIER_KEY_BACKGROUND_SHADOW_COLOR =
      "modifier_key_background_shadow_color";
  private static final String KEY_MODIFIER_KEY_BACKGROUND_SHADOW_OFFSET_X_DP =
      "modifier_key_background_shadow_offset_x_dp";
  private static final String KEY_MODIFIER_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP =
      "modifier_key_background_shadow_offset_y_dp";
  private static final String KEY_ENTER_KEY_BACKGROUND_SHADOW_COLOR =
      "enter_key_background_shadow_color";
  private static final String KEY_ENTER_KEY_BACKGROUND_SHADOW_OFFSET_X_DP =
      "enter_key_background_shadow_offset_x_dp";
  private static final String KEY_ENTER_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP =
      "enter_key_background_shadow_offset_y_dp";

  private KeyboardThemePresetArchiveShadowOverrides() {}

  static void writeToJson(
      @NonNull JSONObject overrides,
      @NonNull KeyboardThemeUserOverridesStore overridesStore,
      @NonNull String presetId)
      throws JSONException {
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_TOKEN_SECONDARY_TEXT_SHADOW_COLOR,
        overridesStore.getTokenSecondaryTextShadowColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_TOKEN_SECONDARY_TEXT_SHADOW_RADIUS_DP,
        overridesStore.getTokenSecondaryTextShadowRadiusDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_TOKEN_SECONDARY_TEXT_SHADOW_OFFSET_X_DP,
        overridesStore.getTokenSecondaryTextShadowOffsetXDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_TOKEN_SECONDARY_TEXT_SHADOW_OFFSET_Y_DP,
        overridesStore.getTokenSecondaryTextShadowOffsetYDp(presetId));

    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_TOKEN_SECONDARY_KEY_SHADOW_COLOR,
        overridesStore.getTokenSecondaryKeyBackgroundShadowColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_TOKEN_SECONDARY_KEY_SHADOW_OFFSET_X_DP,
        overridesStore.getTokenSecondaryKeyBackgroundShadowOffsetXDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_TOKEN_SECONDARY_KEY_SHADOW_OFFSET_Y_DP,
        overridesStore.getTokenSecondaryKeyBackgroundShadowOffsetYDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_TOKEN_SECONDARY_KEY_SHADOW_SPREAD_DP,
        overridesStore.getTokenSecondaryKeyBackgroundShadowSpreadDp(presetId));

    if (overridesStore.isKeyTextShadowUseTokenSecondary(presetId)) {
      overrides.put(KEY_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY, true);
    }
    if (overridesStore.isSpecialKeyTextShadowUseTokenSecondary(presetId)) {
      overrides.put(KEY_SPECIAL_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY, true);
    }
    if (overridesStore.isSpacebarKeyTextShadowUseTokenSecondary(presetId)) {
      overrides.put(KEY_SPACEBAR_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY, true);
    }
    if (overridesStore.isModifierKeyTextShadowUseTokenSecondary(presetId)) {
      overrides.put(KEY_MODIFIER_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY, true);
    }
    if (overridesStore.isEnterKeyTextShadowUseTokenSecondary(presetId)) {
      overrides.put(KEY_ENTER_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY, true);
    }

    if (overridesStore.isKeyBackgroundShadowUseTokenSecondary(presetId)) {
      overrides.put(KEY_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY, true);
    }
    if (overridesStore.isSpecialKeyBackgroundShadowUseTokenSecondary(presetId)) {
      overrides.put(KEY_SPECIAL_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY, true);
    }
    if (overridesStore.isSpacebarKeyBackgroundShadowUseTokenSecondary(presetId)) {
      overrides.put(KEY_SPACEBAR_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY, true);
    }
    if (overridesStore.isModifierKeyBackgroundShadowUseTokenSecondary(presetId)) {
      overrides.put(KEY_MODIFIER_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY, true);
    }
    if (overridesStore.isEnterKeyBackgroundShadowUseTokenSecondary(presetId)) {
      overrides.put(KEY_ENTER_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY, true);
    }

    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides, KEY_KEY_TEXT_SHADOW_COLOR, overridesStore.getKeyTextShadowColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_KEY_TEXT_SHADOW_RADIUS_DP,
        overridesStore.getKeyTextShadowRadiusDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_KEY_TEXT_SHADOW_OFFSET_X_DP,
        overridesStore.getKeyTextShadowOffsetXDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_KEY_TEXT_SHADOW_OFFSET_Y_DP,
        overridesStore.getKeyTextShadowOffsetYDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_SPECIAL_KEY_TEXT_SHADOW_COLOR,
        overridesStore.getSpecialKeyTextShadowColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_SPECIAL_KEY_TEXT_SHADOW_RADIUS_DP,
        overridesStore.getSpecialKeyTextShadowRadiusDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_SPECIAL_KEY_TEXT_SHADOW_OFFSET_X_DP,
        overridesStore.getSpecialKeyTextShadowOffsetXDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_SPECIAL_KEY_TEXT_SHADOW_OFFSET_Y_DP,
        overridesStore.getSpecialKeyTextShadowOffsetYDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_SPACEBAR_KEY_TEXT_SHADOW_COLOR,
        overridesStore.getSpacebarKeyTextShadowColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_SPACEBAR_KEY_TEXT_SHADOW_RADIUS_DP,
        overridesStore.getSpacebarKeyTextShadowRadiusDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_SPACEBAR_KEY_TEXT_SHADOW_OFFSET_X_DP,
        overridesStore.getSpacebarKeyTextShadowOffsetXDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_SPACEBAR_KEY_TEXT_SHADOW_OFFSET_Y_DP,
        overridesStore.getSpacebarKeyTextShadowOffsetYDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_MODIFIER_KEY_TEXT_SHADOW_COLOR,
        overridesStore.getModifierKeyTextShadowColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_MODIFIER_KEY_TEXT_SHADOW_RADIUS_DP,
        overridesStore.getModifierKeyTextShadowRadiusDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_MODIFIER_KEY_TEXT_SHADOW_OFFSET_X_DP,
        overridesStore.getModifierKeyTextShadowOffsetXDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_MODIFIER_KEY_TEXT_SHADOW_OFFSET_Y_DP,
        overridesStore.getModifierKeyTextShadowOffsetYDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_ENTER_KEY_TEXT_SHADOW_COLOR,
        overridesStore.getEnterKeyTextShadowColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_ENTER_KEY_TEXT_SHADOW_RADIUS_DP,
        overridesStore.getEnterKeyTextShadowRadiusDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_ENTER_KEY_TEXT_SHADOW_OFFSET_X_DP,
        overridesStore.getEnterKeyTextShadowOffsetXDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_ENTER_KEY_TEXT_SHADOW_OFFSET_Y_DP,
        overridesStore.getEnterKeyTextShadowOffsetYDp(presetId));

    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_KEY_BACKGROUND_SHADOW_COLOR,
        overridesStore.getKeyBackgroundShadowColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_KEY_BACKGROUND_SHADOW_OFFSET_X_DP,
        overridesStore.getKeyBackgroundShadowOffsetXDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP,
        overridesStore.getKeyBackgroundShadowOffsetYDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_KEY_BACKGROUND_SHADOW_SPREAD_DP,
        overridesStore.getKeyBackgroundShadowSpreadDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_SPECIAL_KEY_BACKGROUND_SHADOW_COLOR,
        overridesStore.getSpecialKeyBackgroundShadowColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_SPECIAL_KEY_BACKGROUND_SHADOW_OFFSET_X_DP,
        overridesStore.getSpecialKeyBackgroundShadowOffsetXDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_SPECIAL_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP,
        overridesStore.getSpecialKeyBackgroundShadowOffsetYDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_SPACEBAR_KEY_BACKGROUND_SHADOW_COLOR,
        overridesStore.getSpacebarKeyBackgroundShadowColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_SPACEBAR_KEY_BACKGROUND_SHADOW_OFFSET_X_DP,
        overridesStore.getSpacebarKeyBackgroundShadowOffsetXDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_SPACEBAR_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP,
        overridesStore.getSpacebarKeyBackgroundShadowOffsetYDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_MODIFIER_KEY_BACKGROUND_SHADOW_COLOR,
        overridesStore.getModifierKeyBackgroundShadowColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_MODIFIER_KEY_BACKGROUND_SHADOW_OFFSET_X_DP,
        overridesStore.getModifierKeyBackgroundShadowOffsetXDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_MODIFIER_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP,
        overridesStore.getModifierKeyBackgroundShadowOffsetYDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_ENTER_KEY_BACKGROUND_SHADOW_COLOR,
        overridesStore.getEnterKeyBackgroundShadowColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_ENTER_KEY_BACKGROUND_SHADOW_OFFSET_X_DP,
        overridesStore.getEnterKeyBackgroundShadowOffsetXDp(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_ENTER_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP,
        overridesStore.getEnterKeyBackgroundShadowOffsetYDp(presetId));
  }

  static void applyFromJson(
      @NonNull KeyboardThemeUserOverridesStore store,
      @NonNull String presetId,
      @NonNull JSONObject json) {
    KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
        json,
        KEY_TOKEN_SECONDARY_TEXT_SHADOW_COLOR,
        value -> store.setTokenSecondaryTextShadowColor(presetId, value));
    if (json.has(KEY_TOKEN_SECONDARY_TEXT_SHADOW_RADIUS_DP)) {
      store.setTokenSecondaryTextShadowRadiusDp(
          presetId, json.optInt(KEY_TOKEN_SECONDARY_TEXT_SHADOW_RADIUS_DP, 0));
    }
    if (json.has(KEY_TOKEN_SECONDARY_TEXT_SHADOW_OFFSET_X_DP)) {
      store.setTokenSecondaryTextShadowOffsetXDp(
          presetId, json.optInt(KEY_TOKEN_SECONDARY_TEXT_SHADOW_OFFSET_X_DP, 0));
    }
    if (json.has(KEY_TOKEN_SECONDARY_TEXT_SHADOW_OFFSET_Y_DP)) {
      store.setTokenSecondaryTextShadowOffsetYDp(
          presetId, json.optInt(KEY_TOKEN_SECONDARY_TEXT_SHADOW_OFFSET_Y_DP, 0));
    }

    KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
        json,
        KEY_TOKEN_SECONDARY_KEY_SHADOW_COLOR,
        value -> store.setTokenSecondaryKeyBackgroundShadowColor(presetId, value));
    if (json.has(KEY_TOKEN_SECONDARY_KEY_SHADOW_OFFSET_X_DP)) {
      store.setTokenSecondaryKeyBackgroundShadowOffsetXDp(
          presetId, json.optInt(KEY_TOKEN_SECONDARY_KEY_SHADOW_OFFSET_X_DP, 0));
    }
    if (json.has(KEY_TOKEN_SECONDARY_KEY_SHADOW_OFFSET_Y_DP)) {
      store.setTokenSecondaryKeyBackgroundShadowOffsetYDp(
          presetId, json.optInt(KEY_TOKEN_SECONDARY_KEY_SHADOW_OFFSET_Y_DP, 0));
    }
    if (json.has(KEY_TOKEN_SECONDARY_KEY_SHADOW_SPREAD_DP)) {
      store.setTokenSecondaryKeyBackgroundShadowSpreadDp(
          presetId, json.optInt(KEY_TOKEN_SECONDARY_KEY_SHADOW_SPREAD_DP, 0));
    }

    final boolean keyTextShadowUsesTokenSecondary =
        json.optBoolean(KEY_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY, false);
    final boolean specialKeyTextShadowUsesTokenSecondary =
        json.optBoolean(KEY_SPECIAL_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY, false);
    final boolean spacebarKeyTextShadowUsesTokenSecondary =
        json.optBoolean(KEY_SPACEBAR_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY, false);
    final boolean modifierKeyTextShadowUsesTokenSecondary =
        json.optBoolean(KEY_MODIFIER_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY, false);
    final boolean enterKeyTextShadowUsesTokenSecondary =
        json.optBoolean(KEY_ENTER_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY, false);
    if (keyTextShadowUsesTokenSecondary) {
      store.setKeyTextShadowUseTokenSecondary(presetId, true);
    }
    if (specialKeyTextShadowUsesTokenSecondary) {
      store.setSpecialKeyTextShadowUseTokenSecondary(presetId, true);
    }
    if (spacebarKeyTextShadowUsesTokenSecondary) {
      store.setSpacebarKeyTextShadowUseTokenSecondary(presetId, true);
    }
    if (modifierKeyTextShadowUsesTokenSecondary) {
      store.setModifierKeyTextShadowUseTokenSecondary(presetId, true);
    }
    if (enterKeyTextShadowUsesTokenSecondary) {
      store.setEnterKeyTextShadowUseTokenSecondary(presetId, true);
    }

    final boolean keyBackgroundShadowUsesTokenSecondary =
        json.optBoolean(KEY_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY, false);
    final boolean specialKeyBackgroundShadowUsesTokenSecondary =
        json.optBoolean(KEY_SPECIAL_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY, false);
    final boolean spacebarKeyBackgroundShadowUsesTokenSecondary =
        json.optBoolean(KEY_SPACEBAR_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY, false);
    final boolean modifierKeyBackgroundShadowUsesTokenSecondary =
        json.optBoolean(KEY_MODIFIER_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY, false);
    final boolean enterKeyBackgroundShadowUsesTokenSecondary =
        json.optBoolean(KEY_ENTER_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY, false);
    if (keyBackgroundShadowUsesTokenSecondary) {
      store.setKeyBackgroundShadowUseTokenSecondary(presetId, true);
    }
    if (specialKeyBackgroundShadowUsesTokenSecondary) {
      store.setSpecialKeyBackgroundShadowUseTokenSecondary(presetId, true);
    }
    if (spacebarKeyBackgroundShadowUsesTokenSecondary) {
      store.setSpacebarKeyBackgroundShadowUseTokenSecondary(presetId, true);
    }
    if (modifierKeyBackgroundShadowUsesTokenSecondary) {
      store.setModifierKeyBackgroundShadowUseTokenSecondary(presetId, true);
    }
    if (enterKeyBackgroundShadowUsesTokenSecondary) {
      store.setEnterKeyBackgroundShadowUseTokenSecondary(presetId, true);
    }

    if (!keyTextShadowUsesTokenSecondary) {
      KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
          json, KEY_KEY_TEXT_SHADOW_COLOR, value -> store.setKeyTextShadowColor(presetId, value));
      if (json.has(KEY_KEY_TEXT_SHADOW_RADIUS_DP)) {
        store.setKeyTextShadowRadiusDp(presetId, json.optInt(KEY_KEY_TEXT_SHADOW_RADIUS_DP, 0));
      }
      if (json.has(KEY_KEY_TEXT_SHADOW_OFFSET_X_DP)) {
        store.setKeyTextShadowOffsetXDp(presetId, json.optInt(KEY_KEY_TEXT_SHADOW_OFFSET_X_DP, 0));
      }
      if (json.has(KEY_KEY_TEXT_SHADOW_OFFSET_Y_DP)) {
        store.setKeyTextShadowOffsetYDp(presetId, json.optInt(KEY_KEY_TEXT_SHADOW_OFFSET_Y_DP, 0));
      }
    }

    if (!specialKeyTextShadowUsesTokenSecondary) {
      KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
          json,
          KEY_SPECIAL_KEY_TEXT_SHADOW_COLOR,
          value -> store.setSpecialKeyTextShadowColor(presetId, value));
      if (json.has(KEY_SPECIAL_KEY_TEXT_SHADOW_RADIUS_DP)) {
        store.setSpecialKeyTextShadowRadiusDp(
            presetId, json.optInt(KEY_SPECIAL_KEY_TEXT_SHADOW_RADIUS_DP, 0));
      }
      if (json.has(KEY_SPECIAL_KEY_TEXT_SHADOW_OFFSET_X_DP)) {
        store.setSpecialKeyTextShadowOffsetXDp(
            presetId, json.optInt(KEY_SPECIAL_KEY_TEXT_SHADOW_OFFSET_X_DP, 0));
      }
      if (json.has(KEY_SPECIAL_KEY_TEXT_SHADOW_OFFSET_Y_DP)) {
        store.setSpecialKeyTextShadowOffsetYDp(
            presetId, json.optInt(KEY_SPECIAL_KEY_TEXT_SHADOW_OFFSET_Y_DP, 0));
      }
    }

    if (!spacebarKeyTextShadowUsesTokenSecondary) {
      KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
          json,
          KEY_SPACEBAR_KEY_TEXT_SHADOW_COLOR,
          value -> store.setSpacebarKeyTextShadowColor(presetId, value));
      if (json.has(KEY_SPACEBAR_KEY_TEXT_SHADOW_RADIUS_DP)) {
        store.setSpacebarKeyTextShadowRadiusDp(
            presetId, json.optInt(KEY_SPACEBAR_KEY_TEXT_SHADOW_RADIUS_DP, 0));
      }
      if (json.has(KEY_SPACEBAR_KEY_TEXT_SHADOW_OFFSET_X_DP)) {
        store.setSpacebarKeyTextShadowOffsetXDp(
            presetId, json.optInt(KEY_SPACEBAR_KEY_TEXT_SHADOW_OFFSET_X_DP, 0));
      }
      if (json.has(KEY_SPACEBAR_KEY_TEXT_SHADOW_OFFSET_Y_DP)) {
        store.setSpacebarKeyTextShadowOffsetYDp(
            presetId, json.optInt(KEY_SPACEBAR_KEY_TEXT_SHADOW_OFFSET_Y_DP, 0));
      }
    }

    if (!modifierKeyTextShadowUsesTokenSecondary) {
      KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
          json,
          KEY_MODIFIER_KEY_TEXT_SHADOW_COLOR,
          value -> store.setModifierKeyTextShadowColor(presetId, value));
      if (json.has(KEY_MODIFIER_KEY_TEXT_SHADOW_RADIUS_DP)) {
        store.setModifierKeyTextShadowRadiusDp(
            presetId, json.optInt(KEY_MODIFIER_KEY_TEXT_SHADOW_RADIUS_DP, 0));
      }
      if (json.has(KEY_MODIFIER_KEY_TEXT_SHADOW_OFFSET_X_DP)) {
        store.setModifierKeyTextShadowOffsetXDp(
            presetId, json.optInt(KEY_MODIFIER_KEY_TEXT_SHADOW_OFFSET_X_DP, 0));
      }
      if (json.has(KEY_MODIFIER_KEY_TEXT_SHADOW_OFFSET_Y_DP)) {
        store.setModifierKeyTextShadowOffsetYDp(
            presetId, json.optInt(KEY_MODIFIER_KEY_TEXT_SHADOW_OFFSET_Y_DP, 0));
      }
    }

    if (!enterKeyTextShadowUsesTokenSecondary) {
      KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
          json,
          KEY_ENTER_KEY_TEXT_SHADOW_COLOR,
          value -> store.setEnterKeyTextShadowColor(presetId, value));
      if (json.has(KEY_ENTER_KEY_TEXT_SHADOW_RADIUS_DP)) {
        store.setEnterKeyTextShadowRadiusDp(
            presetId, json.optInt(KEY_ENTER_KEY_TEXT_SHADOW_RADIUS_DP, 0));
      }
      if (json.has(KEY_ENTER_KEY_TEXT_SHADOW_OFFSET_X_DP)) {
        store.setEnterKeyTextShadowOffsetXDp(
            presetId, json.optInt(KEY_ENTER_KEY_TEXT_SHADOW_OFFSET_X_DP, 0));
      }
      if (json.has(KEY_ENTER_KEY_TEXT_SHADOW_OFFSET_Y_DP)) {
        store.setEnterKeyTextShadowOffsetYDp(
            presetId, json.optInt(KEY_ENTER_KEY_TEXT_SHADOW_OFFSET_Y_DP, 0));
      }
    }

    if (!keyBackgroundShadowUsesTokenSecondary) {
      KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
          json,
          KEY_KEY_BACKGROUND_SHADOW_COLOR,
          value -> store.setKeyBackgroundShadowColor(presetId, value));
      if (json.has(KEY_KEY_BACKGROUND_SHADOW_OFFSET_X_DP)) {
        store.setKeyBackgroundShadowOffsetXDp(
            presetId, json.optInt(KEY_KEY_BACKGROUND_SHADOW_OFFSET_X_DP, 0));
      }
      if (json.has(KEY_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP)) {
        store.setKeyBackgroundShadowOffsetYDp(
            presetId, json.optInt(KEY_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP, 0));
      }
      if (json.has(KEY_KEY_BACKGROUND_SHADOW_SPREAD_DP)) {
        store.setKeyBackgroundShadowSpreadDp(
            presetId, json.optInt(KEY_KEY_BACKGROUND_SHADOW_SPREAD_DP, 0));
      }
    }

    if (!specialKeyBackgroundShadowUsesTokenSecondary) {
      KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
          json,
          KEY_SPECIAL_KEY_BACKGROUND_SHADOW_COLOR,
          value -> store.setSpecialKeyBackgroundShadowColor(presetId, value));
      if (json.has(KEY_SPECIAL_KEY_BACKGROUND_SHADOW_OFFSET_X_DP)) {
        store.setSpecialKeyBackgroundShadowOffsetXDp(
            presetId, json.optInt(KEY_SPECIAL_KEY_BACKGROUND_SHADOW_OFFSET_X_DP, 0));
      }
      if (json.has(KEY_SPECIAL_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP)) {
        store.setSpecialKeyBackgroundShadowOffsetYDp(
            presetId, json.optInt(KEY_SPECIAL_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP, 0));
      }
    }

    if (!spacebarKeyBackgroundShadowUsesTokenSecondary) {
      KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
          json,
          KEY_SPACEBAR_KEY_BACKGROUND_SHADOW_COLOR,
          value -> store.setSpacebarKeyBackgroundShadowColor(presetId, value));
      if (json.has(KEY_SPACEBAR_KEY_BACKGROUND_SHADOW_OFFSET_X_DP)) {
        store.setSpacebarKeyBackgroundShadowOffsetXDp(
            presetId, json.optInt(KEY_SPACEBAR_KEY_BACKGROUND_SHADOW_OFFSET_X_DP, 0));
      }
      if (json.has(KEY_SPACEBAR_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP)) {
        store.setSpacebarKeyBackgroundShadowOffsetYDp(
            presetId, json.optInt(KEY_SPACEBAR_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP, 0));
      }
    }

    if (!modifierKeyBackgroundShadowUsesTokenSecondary) {
      KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
          json,
          KEY_MODIFIER_KEY_BACKGROUND_SHADOW_COLOR,
          value -> store.setModifierKeyBackgroundShadowColor(presetId, value));
      if (json.has(KEY_MODIFIER_KEY_BACKGROUND_SHADOW_OFFSET_X_DP)) {
        store.setModifierKeyBackgroundShadowOffsetXDp(
            presetId, json.optInt(KEY_MODIFIER_KEY_BACKGROUND_SHADOW_OFFSET_X_DP, 0));
      }
      if (json.has(KEY_MODIFIER_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP)) {
        store.setModifierKeyBackgroundShadowOffsetYDp(
            presetId, json.optInt(KEY_MODIFIER_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP, 0));
      }
    }

    if (!enterKeyBackgroundShadowUsesTokenSecondary) {
      KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
          json,
          KEY_ENTER_KEY_BACKGROUND_SHADOW_COLOR,
          value -> store.setEnterKeyBackgroundShadowColor(presetId, value));
      if (json.has(KEY_ENTER_KEY_BACKGROUND_SHADOW_OFFSET_X_DP)) {
        store.setEnterKeyBackgroundShadowOffsetXDp(
            presetId, json.optInt(KEY_ENTER_KEY_BACKGROUND_SHADOW_OFFSET_X_DP, 0));
      }
      if (json.has(KEY_ENTER_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP)) {
        store.setEnterKeyBackgroundShadowOffsetYDp(
            presetId, json.optInt(KEY_ENTER_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP, 0));
      }
    }
  }

  static boolean hasShadows(@Nullable JSONObject overridesJson) {
    return overridesJson != null
        && KeyboardThemePresetArchiveJsonSupport.hasAny(
            overridesJson,
            KEY_TOKEN_SECONDARY_TEXT_SHADOW_COLOR,
            KEY_TOKEN_SECONDARY_TEXT_SHADOW_RADIUS_DP,
            KEY_TOKEN_SECONDARY_TEXT_SHADOW_OFFSET_X_DP,
            KEY_TOKEN_SECONDARY_TEXT_SHADOW_OFFSET_Y_DP,
            KEY_TOKEN_SECONDARY_KEY_SHADOW_COLOR,
            KEY_TOKEN_SECONDARY_KEY_SHADOW_OFFSET_X_DP,
            KEY_TOKEN_SECONDARY_KEY_SHADOW_OFFSET_Y_DP,
            KEY_TOKEN_SECONDARY_KEY_SHADOW_SPREAD_DP,
            KEY_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY,
            KEY_SPECIAL_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY,
            KEY_SPACEBAR_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY,
            KEY_MODIFIER_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY,
            KEY_ENTER_KEY_TEXT_SHADOW_USE_TOKEN_SECONDARY,
            KEY_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY,
            KEY_SPECIAL_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY,
            KEY_SPACEBAR_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY,
            KEY_MODIFIER_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY,
            KEY_ENTER_KEY_BACKGROUND_SHADOW_USE_TOKEN_SECONDARY,
            KEY_KEY_TEXT_SHADOW_COLOR,
            KEY_KEY_TEXT_SHADOW_RADIUS_DP,
            KEY_KEY_TEXT_SHADOW_OFFSET_X_DP,
            KEY_KEY_TEXT_SHADOW_OFFSET_Y_DP,
            KEY_SPECIAL_KEY_TEXT_SHADOW_COLOR,
            KEY_SPECIAL_KEY_TEXT_SHADOW_RADIUS_DP,
            KEY_SPECIAL_KEY_TEXT_SHADOW_OFFSET_X_DP,
            KEY_SPECIAL_KEY_TEXT_SHADOW_OFFSET_Y_DP,
            KEY_SPACEBAR_KEY_TEXT_SHADOW_COLOR,
            KEY_SPACEBAR_KEY_TEXT_SHADOW_RADIUS_DP,
            KEY_SPACEBAR_KEY_TEXT_SHADOW_OFFSET_X_DP,
            KEY_SPACEBAR_KEY_TEXT_SHADOW_OFFSET_Y_DP,
            KEY_MODIFIER_KEY_TEXT_SHADOW_COLOR,
            KEY_MODIFIER_KEY_TEXT_SHADOW_RADIUS_DP,
            KEY_MODIFIER_KEY_TEXT_SHADOW_OFFSET_X_DP,
            KEY_MODIFIER_KEY_TEXT_SHADOW_OFFSET_Y_DP,
            KEY_ENTER_KEY_TEXT_SHADOW_COLOR,
            KEY_ENTER_KEY_TEXT_SHADOW_RADIUS_DP,
            KEY_ENTER_KEY_TEXT_SHADOW_OFFSET_X_DP,
            KEY_ENTER_KEY_TEXT_SHADOW_OFFSET_Y_DP,
            KEY_KEY_BACKGROUND_SHADOW_COLOR,
            KEY_KEY_BACKGROUND_SHADOW_OFFSET_X_DP,
            KEY_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP,
            KEY_KEY_BACKGROUND_SHADOW_SPREAD_DP,
            KEY_SPECIAL_KEY_BACKGROUND_SHADOW_COLOR,
            KEY_SPECIAL_KEY_BACKGROUND_SHADOW_OFFSET_X_DP,
            KEY_SPECIAL_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP,
            KEY_SPACEBAR_KEY_BACKGROUND_SHADOW_COLOR,
            KEY_SPACEBAR_KEY_BACKGROUND_SHADOW_OFFSET_X_DP,
            KEY_SPACEBAR_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP,
            KEY_MODIFIER_KEY_BACKGROUND_SHADOW_COLOR,
            KEY_MODIFIER_KEY_BACKGROUND_SHADOW_OFFSET_X_DP,
            KEY_MODIFIER_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP,
            KEY_ENTER_KEY_BACKGROUND_SHADOW_COLOR,
            KEY_ENTER_KEY_BACKGROUND_SHADOW_OFFSET_X_DP,
            KEY_ENTER_KEY_BACKGROUND_SHADOW_OFFSET_Y_DP);
  }
}
