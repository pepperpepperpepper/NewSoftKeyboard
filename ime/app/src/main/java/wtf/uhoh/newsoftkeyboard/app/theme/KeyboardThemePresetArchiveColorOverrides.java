package wtf.uhoh.newsoftkeyboard.app.theme;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

final class KeyboardThemePresetArchiveColorOverrides {

  private static final String KEY_TOKEN_PRIMARY_TEXT_COLOR = "token_primary_text_color";
  private static final String KEY_TOKEN_SECONDARY_TEXT_COLOR = "token_secondary_text_color";
  private static final String KEY_TOKEN_ACCENT_COLOR = "token_accent_color";
  private static final String KEY_TOKEN_KEY_SURFACE_COLOR = "token_key_surface_color";
  private static final String KEY_TOKEN_BACKGROUND_COLOR = "token_background_color";
  private static final String KEY_KEY_TEXT_COLOR = "key_text_color";
  private static final String KEY_SPECIAL_KEY_TEXT_COLOR = "special_key_text_color";
  private static final String KEY_SPACEBAR_TEXT_COLOR = "spacebar_text_color";
  private static final String KEY_MODIFIER_KEY_TEXT_COLOR = "modifier_key_text_color";
  private static final String KEY_ENTER_KEY_TEXT_COLOR = "enter_key_text_color";
  private static final String KEY_HINT_TEXT_COLOR = "hint_text_color";
  private static final String KEY_KEY_BACKGROUND_TINT = "key_background_tint";
  private static final String KEY_SPECIAL_KEY_BACKGROUND_TINT = "special_key_background_tint";
  private static final String KEY_SPACEBAR_BACKGROUND_TINT = "spacebar_background_tint";
  private static final String KEY_MODIFIER_KEY_BACKGROUND_TINT = "modifier_key_background_tint";
  private static final String KEY_ENTER_KEY_BACKGROUND_TINT = "enter_key_background_tint";
  private static final String KEY_KEYBOARD_BACKGROUND_TINT = "keyboard_background_tint";
  private static final String KEY_KEY_BACKGROUND_OPACITY_PERCENT = "key_background_opacity_percent";
  private static final String KEY_KEYBOARD_BACKGROUND_OPACITY_PERCENT =
      "keyboard_background_opacity_percent";

  private KeyboardThemePresetArchiveColorOverrides() {}

  static void writeToJson(
      @NonNull JSONObject overrides,
      @NonNull KeyboardThemeUserOverridesStore overridesStore,
      @NonNull String presetId)
      throws JSONException {
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides, KEY_TOKEN_PRIMARY_TEXT_COLOR, overridesStore.getTokenPrimaryTextColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_TOKEN_SECONDARY_TEXT_COLOR,
        overridesStore.getTokenSecondaryTextColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides, KEY_TOKEN_ACCENT_COLOR, overridesStore.getTokenAccentColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides, KEY_TOKEN_KEY_SURFACE_COLOR, overridesStore.getTokenKeySurfaceColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides, KEY_TOKEN_BACKGROUND_COLOR, overridesStore.getTokenBackgroundColor(presetId));

    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides, KEY_KEY_TEXT_COLOR, overridesStore.getKeyTextColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides, KEY_SPECIAL_KEY_TEXT_COLOR, overridesStore.getSpecialKeyTextColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides, KEY_SPACEBAR_TEXT_COLOR, overridesStore.getSpacebarTextColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides, KEY_MODIFIER_KEY_TEXT_COLOR, overridesStore.getModifierKeyTextColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides, KEY_ENTER_KEY_TEXT_COLOR, overridesStore.getEnterKeyTextColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides, KEY_HINT_TEXT_COLOR, overridesStore.getHintTextColor(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides, KEY_KEY_BACKGROUND_TINT, overridesStore.getKeyBackgroundTint(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_SPECIAL_KEY_BACKGROUND_TINT,
        overridesStore.getSpecialKeyBackgroundTint(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_SPACEBAR_BACKGROUND_TINT,
        overridesStore.getSpacebarBackgroundTint(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_MODIFIER_KEY_BACKGROUND_TINT,
        overridesStore.getModifierKeyBackgroundTint(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_ENTER_KEY_BACKGROUND_TINT,
        overridesStore.getEnterKeyBackgroundTint(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_KEYBOARD_BACKGROUND_TINT,
        overridesStore.getKeyboardBackgroundTint(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_KEY_BACKGROUND_OPACITY_PERCENT,
        overridesStore.getKeyBackgroundOpacityPercent(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_KEYBOARD_BACKGROUND_OPACITY_PERCENT,
        overridesStore.getKeyboardBackgroundOpacityPercent(presetId));
  }

  static void applyFromJson(
      @NonNull KeyboardThemeUserOverridesStore store,
      @NonNull String presetId,
      @NonNull JSONObject json) {
    KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
        json,
        KEY_TOKEN_PRIMARY_TEXT_COLOR,
        value -> store.setTokenPrimaryTextColor(presetId, value));
    KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
        json,
        KEY_TOKEN_SECONDARY_TEXT_COLOR,
        value -> store.setTokenSecondaryTextColor(presetId, value));
    KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
        json, KEY_TOKEN_ACCENT_COLOR, value -> store.setTokenAccentColor(presetId, value));
    KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
        json, KEY_TOKEN_KEY_SURFACE_COLOR, value -> store.setTokenKeySurfaceColor(presetId, value));
    KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
        json, KEY_TOKEN_BACKGROUND_COLOR, value -> store.setTokenBackgroundColor(presetId, value));

    KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
        json, KEY_KEY_TEXT_COLOR, value -> store.setKeyTextColor(presetId, value));
    KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
        json, KEY_SPECIAL_KEY_TEXT_COLOR, value -> store.setSpecialKeyTextColor(presetId, value));
    KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
        json, KEY_SPACEBAR_TEXT_COLOR, value -> store.setSpacebarTextColor(presetId, value));
    KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
        json, KEY_MODIFIER_KEY_TEXT_COLOR, value -> store.setModifierKeyTextColor(presetId, value));
    KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
        json, KEY_ENTER_KEY_TEXT_COLOR, value -> store.setEnterKeyTextColor(presetId, value));
    KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
        json, KEY_HINT_TEXT_COLOR, value -> store.setHintTextColor(presetId, value));
    KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
        json, KEY_KEY_BACKGROUND_TINT, value -> store.setKeyBackgroundTint(presetId, value));
    KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
        json,
        KEY_SPECIAL_KEY_BACKGROUND_TINT,
        value -> store.setSpecialKeyBackgroundTint(presetId, value));
    KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
        json,
        KEY_SPACEBAR_BACKGROUND_TINT,
        value -> store.setSpacebarBackgroundTint(presetId, value));
    KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
        json,
        KEY_MODIFIER_KEY_BACKGROUND_TINT,
        value -> store.setModifierKeyBackgroundTint(presetId, value));
    KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
        json,
        KEY_ENTER_KEY_BACKGROUND_TINT,
        value -> store.setEnterKeyBackgroundTint(presetId, value));
    KeyboardThemePresetArchiveJsonSupport.setIntIfPresent(
        json,
        KEY_KEYBOARD_BACKGROUND_TINT,
        value -> store.setKeyboardBackgroundTint(presetId, value));
    if (json.has(KEY_KEY_BACKGROUND_OPACITY_PERCENT)) {
      store.setKeyBackgroundOpacityPercent(
          presetId, json.optInt(KEY_KEY_BACKGROUND_OPACITY_PERCENT, 100));
    }
    if (json.has(KEY_KEYBOARD_BACKGROUND_OPACITY_PERCENT)) {
      store.setKeyboardBackgroundOpacityPercent(
          presetId, json.optInt(KEY_KEYBOARD_BACKGROUND_OPACITY_PERCENT, 100));
    }
  }

  static boolean hasColors(@Nullable JSONObject overridesJson) {
    return overridesJson != null
        && KeyboardThemePresetArchiveJsonSupport.hasAny(
            overridesJson,
            KEY_TOKEN_PRIMARY_TEXT_COLOR,
            KEY_TOKEN_SECONDARY_TEXT_COLOR,
            KEY_TOKEN_ACCENT_COLOR,
            KEY_TOKEN_KEY_SURFACE_COLOR,
            KEY_TOKEN_BACKGROUND_COLOR,
            KEY_KEY_TEXT_COLOR,
            KEY_SPECIAL_KEY_TEXT_COLOR,
            KEY_SPACEBAR_TEXT_COLOR,
            KEY_MODIFIER_KEY_TEXT_COLOR,
            KEY_ENTER_KEY_TEXT_COLOR,
            KEY_HINT_TEXT_COLOR,
            KEY_KEY_BACKGROUND_TINT,
            KEY_SPECIAL_KEY_BACKGROUND_TINT,
            KEY_SPACEBAR_BACKGROUND_TINT,
            KEY_MODIFIER_KEY_BACKGROUND_TINT,
            KEY_ENTER_KEY_BACKGROUND_TINT,
            KEY_KEYBOARD_BACKGROUND_TINT,
            KEY_KEY_BACKGROUND_OPACITY_PERCENT,
            KEY_KEYBOARD_BACKGROUND_OPACITY_PERCENT);
  }
}
