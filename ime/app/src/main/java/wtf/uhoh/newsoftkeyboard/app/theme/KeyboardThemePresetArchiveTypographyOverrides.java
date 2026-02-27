package wtf.uhoh.newsoftkeyboard.app.theme;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

final class KeyboardThemePresetArchiveTypographyOverrides {

  private static final String KEY_KEY_FONT_FAMILY = "key_font_family";
  private static final String KEY_KEY_FONT_STYLE = "key_font_style";
  private static final String KEY_HINT_FONT_FAMILY = "hint_font_family";
  private static final String KEY_HINT_FONT_STYLE = "hint_font_style";
  private static final String KEY_SUGGESTION_FONT_FAMILY = "suggestion_font_family";
  private static final String KEY_SUGGESTION_FONT_STYLE = "suggestion_font_style";
  private static final String KEY_KEYBOARD_NAME_FONT_FAMILY = "keyboard_name_font_family";
  private static final String KEY_KEYBOARD_NAME_FONT_STYLE = "keyboard_name_font_style";
  private static final String KEY_TOKEN_SECONDARY_FONT_FAMILY = "token_secondary_font_family";
  private static final String KEY_TOKEN_SECONDARY_FONT_STYLE = "token_secondary_font_style";
  private static final String KEY_TOKEN_SECONDARY_TEXT_SIZE_PERCENT =
      "token_secondary_text_size_percent";
  private static final String KEY_KEY_CUSTOM_FONT_NAME = "key_custom_font_name";
  private static final String KEY_ENSURE_READABLE_TEXT_ENABLED = "ensure_readable_text_enabled";
  private static final String KEY_KEY_LABEL_AUTO_FIT_ENABLED = "key_label_auto_fit_enabled";
  private static final String KEY_KEY_LABEL_AUTO_FIT_MIN_SIZE_PERCENT =
      "key_label_auto_fit_min_size_percent";
  private static final String KEY_KEY_LABEL_ELLIPSIZE_ENABLED = "key_label_ellipsize_enabled";
  private static final String KEY_KEY_LABEL_TEXT_SIZE_PERCENT = "key_label_text_size_percent";
  private static final String KEY_HINT_TEXT_SIZE_PERCENT = "hint_text_size_percent";
  private static final String KEY_SUGGESTION_TEXT_SIZE_PERCENT = "suggestion_text_size_percent";
  private static final String KEY_KEYBOARD_NAME_TEXT_SIZE_PERCENT =
      "keyboard_name_text_size_percent";

  private KeyboardThemePresetArchiveTypographyOverrides() {}

  static boolean writeToJson(
      @NonNull JSONObject overrides,
      @NonNull KeyboardThemeUserOverridesStore overridesStore,
      @NonNull String presetId)
      throws JSONException {
    final String keyFontFamily = overridesStore.getKeyFontFamily(presetId);
    final String tokenSecondaryFontFamily = overridesStore.getTokenSecondaryFontFamily(presetId);
    final String hintFontFamily = overridesStore.getHintFontFamily(presetId);
    final String suggestionFontFamily = overridesStore.getSuggestionFontFamily(presetId);
    final String keyboardNameFontFamily = overridesStore.getKeyboardNameFontFamily(presetId);
    KeyboardThemePresetArchiveJsonSupport.putStringIfNotNull(
        overrides, KEY_KEY_FONT_FAMILY, keyFontFamily);
    KeyboardThemePresetArchiveJsonSupport.putStringIfNotNull(
        overrides, KEY_TOKEN_SECONDARY_FONT_FAMILY, tokenSecondaryFontFamily);
    KeyboardThemePresetArchiveJsonSupport.putStringIfNotNull(
        overrides, KEY_HINT_FONT_FAMILY, hintFontFamily);
    KeyboardThemePresetArchiveJsonSupport.putStringIfNotNull(
        overrides, KEY_SUGGESTION_FONT_FAMILY, suggestionFontFamily);
    KeyboardThemePresetArchiveJsonSupport.putStringIfNotNull(
        overrides, KEY_KEYBOARD_NAME_FONT_FAMILY, keyboardNameFontFamily);

    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_TOKEN_SECONDARY_FONT_STYLE,
        overridesStore.getTokenSecondaryFontStyle(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_TOKEN_SECONDARY_TEXT_SIZE_PERCENT,
        overridesStore.getTokenSecondaryTextSizePercent(presetId));

    final boolean usesCustomFont =
        usesCustomFont(
            keyFontFamily,
            tokenSecondaryFontFamily,
            hintFontFamily,
            suggestionFontFamily,
            keyboardNameFontFamily);
    if (usesCustomFont) {
      KeyboardThemePresetArchiveJsonSupport.putStringIfNotNull(
          overrides,
          KEY_KEY_CUSTOM_FONT_NAME,
          overridesStore.getCustomKeyFontDisplayName(presetId));
    }

    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides, KEY_KEY_FONT_STYLE, overridesStore.getKeyFontStyle(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides, KEY_HINT_FONT_STYLE, overridesStore.getHintFontStyle(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides, KEY_SUGGESTION_FONT_STYLE, overridesStore.getSuggestionFontStyle(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides, KEY_KEYBOARD_NAME_FONT_STYLE, overridesStore.getKeyboardNameFontStyle(presetId));

    final Boolean ensureReadableTextEnabled = overridesStore.getEnsureReadableTextEnabled(presetId);
    if (ensureReadableTextEnabled != null) {
      overrides.put(KEY_ENSURE_READABLE_TEXT_ENABLED, ensureReadableTextEnabled.booleanValue());
    }

    final Boolean autoFitKeyLabels = overridesStore.getKeyLabelAutoFitEnabled(presetId);
    if (autoFitKeyLabels != null) {
      overrides.put(KEY_KEY_LABEL_AUTO_FIT_ENABLED, autoFitKeyLabels.booleanValue());
    }

    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_KEY_LABEL_AUTO_FIT_MIN_SIZE_PERCENT,
        overridesStore.getKeyLabelAutoFitMinSizePercent(presetId));

    final Boolean ellipsizeKeyLabels = overridesStore.getKeyLabelEllipsizeEnabled(presetId);
    if (ellipsizeKeyLabels != null) {
      overrides.put(KEY_KEY_LABEL_ELLIPSIZE_ENABLED, ellipsizeKeyLabels.booleanValue());
    }

    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_KEY_LABEL_TEXT_SIZE_PERCENT,
        overridesStore.getKeyLabelTextSizePercent(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides, KEY_HINT_TEXT_SIZE_PERCENT, overridesStore.getHintTextSizePercent(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_SUGGESTION_TEXT_SIZE_PERCENT,
        overridesStore.getSuggestionTextSizePercent(presetId));
    KeyboardThemePresetArchiveJsonSupport.putIntIfNotNull(
        overrides,
        KEY_KEYBOARD_NAME_TEXT_SIZE_PERCENT,
        overridesStore.getKeyboardNameTextSizePercent(presetId));

    return usesCustomFont;
  }

  static void applyFromJson(
      @NonNull KeyboardThemeUserOverridesStore store,
      @NonNull String presetId,
      @NonNull JSONObject json) {
    final String fontFamily = json.optString(KEY_KEY_FONT_FAMILY, null);
    if (fontFamily != null && !fontFamily.trim().isEmpty()) {
      store.setKeyFontFamily(presetId, fontFamily.trim());
    }
    if (json.has(KEY_KEY_FONT_STYLE)) {
      store.setKeyFontStyle(presetId, json.optInt(KEY_KEY_FONT_STYLE, 0));
    }

    final String tokenSecondaryFontFamily = json.optString(KEY_TOKEN_SECONDARY_FONT_FAMILY, null);
    if (tokenSecondaryFontFamily != null && !tokenSecondaryFontFamily.trim().isEmpty()) {
      store.setTokenSecondaryFontFamily(presetId, tokenSecondaryFontFamily.trim());
    }
    if (json.has(KEY_TOKEN_SECONDARY_FONT_STYLE)) {
      store.setTokenSecondaryFontStyle(presetId, json.optInt(KEY_TOKEN_SECONDARY_FONT_STYLE, 0));
    }
    if (json.has(KEY_TOKEN_SECONDARY_TEXT_SIZE_PERCENT)) {
      store.setTokenSecondaryTextSizePercent(
          presetId, json.optInt(KEY_TOKEN_SECONDARY_TEXT_SIZE_PERCENT, 100));
    }

    final String hintFontFamily = json.optString(KEY_HINT_FONT_FAMILY, null);
    if (hintFontFamily != null && !hintFontFamily.trim().isEmpty()) {
      store.setHintFontFamily(presetId, hintFontFamily.trim());
    }
    if (json.has(KEY_HINT_FONT_STYLE)) {
      store.setHintFontStyle(presetId, json.optInt(KEY_HINT_FONT_STYLE, 0));
    }

    final String suggestionFontFamily = json.optString(KEY_SUGGESTION_FONT_FAMILY, null);
    if (suggestionFontFamily != null && !suggestionFontFamily.trim().isEmpty()) {
      store.setSuggestionFontFamily(presetId, suggestionFontFamily.trim());
    }
    if (json.has(KEY_SUGGESTION_FONT_STYLE)) {
      store.setSuggestionFontStyle(presetId, json.optInt(KEY_SUGGESTION_FONT_STYLE, 0));
    }

    final String keyboardNameFontFamily = json.optString(KEY_KEYBOARD_NAME_FONT_FAMILY, null);
    if (keyboardNameFontFamily != null && !keyboardNameFontFamily.trim().isEmpty()) {
      store.setKeyboardNameFontFamily(presetId, keyboardNameFontFamily.trim());
    }
    if (json.has(KEY_KEYBOARD_NAME_FONT_STYLE)) {
      store.setKeyboardNameFontStyle(presetId, json.optInt(KEY_KEYBOARD_NAME_FONT_STYLE, 0));
    }

    if (json.has(KEY_ENSURE_READABLE_TEXT_ENABLED)) {
      store.setEnsureReadableTextEnabled(
          presetId, json.optBoolean(KEY_ENSURE_READABLE_TEXT_ENABLED, false));
    }

    if (json.has(KEY_KEY_LABEL_AUTO_FIT_ENABLED)) {
      store.setKeyLabelAutoFitEnabled(
          presetId, json.optBoolean(KEY_KEY_LABEL_AUTO_FIT_ENABLED, true));
    }
    if (json.has(KEY_KEY_LABEL_AUTO_FIT_MIN_SIZE_PERCENT)) {
      store.setKeyLabelAutoFitMinSizePercent(
          presetId, json.optInt(KEY_KEY_LABEL_AUTO_FIT_MIN_SIZE_PERCENT, 30));
    }
    if (json.has(KEY_KEY_LABEL_ELLIPSIZE_ENABLED)) {
      store.setKeyLabelEllipsizeEnabled(
          presetId, json.optBoolean(KEY_KEY_LABEL_ELLIPSIZE_ENABLED, true));
    }
    if (json.has(KEY_KEY_LABEL_TEXT_SIZE_PERCENT)) {
      store.setKeyLabelTextSizePercent(presetId, json.optInt(KEY_KEY_LABEL_TEXT_SIZE_PERCENT, 100));
    }
    if (json.has(KEY_HINT_TEXT_SIZE_PERCENT)) {
      store.setHintTextSizePercent(presetId, json.optInt(KEY_HINT_TEXT_SIZE_PERCENT, 100));
    }
    if (json.has(KEY_SUGGESTION_TEXT_SIZE_PERCENT)) {
      store.setSuggestionTextSizePercent(
          presetId, json.optInt(KEY_SUGGESTION_TEXT_SIZE_PERCENT, 100));
    }
    if (json.has(KEY_KEYBOARD_NAME_TEXT_SIZE_PERCENT)) {
      store.setKeyboardNameTextSizePercent(
          presetId, json.optInt(KEY_KEYBOARD_NAME_TEXT_SIZE_PERCENT, 100));
    }
  }

  static boolean hasTypography(@Nullable JSONObject overridesJson) {
    return overridesJson != null
        && KeyboardThemePresetArchiveJsonSupport.hasAny(
            overridesJson,
            KEY_TOKEN_SECONDARY_FONT_FAMILY,
            KEY_TOKEN_SECONDARY_FONT_STYLE,
            KEY_TOKEN_SECONDARY_TEXT_SIZE_PERCENT,
            KEY_KEY_FONT_FAMILY,
            KEY_KEY_FONT_STYLE,
            KEY_HINT_FONT_FAMILY,
            KEY_HINT_FONT_STYLE,
            KEY_SUGGESTION_FONT_FAMILY,
            KEY_SUGGESTION_FONT_STYLE,
            KEY_KEYBOARD_NAME_FONT_FAMILY,
            KEY_KEYBOARD_NAME_FONT_STYLE,
            KEY_KEY_LABEL_TEXT_SIZE_PERCENT,
            KEY_HINT_TEXT_SIZE_PERCENT,
            KEY_SUGGESTION_TEXT_SIZE_PERCENT,
            KEY_KEYBOARD_NAME_TEXT_SIZE_PERCENT,
            KEY_KEY_LABEL_AUTO_FIT_ENABLED,
            KEY_KEY_LABEL_AUTO_FIT_MIN_SIZE_PERCENT,
            KEY_KEY_LABEL_ELLIPSIZE_ENABLED);
  }

  static boolean hasCustomFont(@Nullable JSONObject overridesJson) {
    return overridesJson != null && usesCustomFont(overridesJson);
  }

  @Nullable
  static String customFontName(@Nullable JSONObject overridesJson) {
    if (overridesJson == null) return null;
    return usesCustomFont(overridesJson)
        ? overridesJson.optString(KEY_KEY_CUSTOM_FONT_NAME, null)
        : null;
  }

  private static boolean usesCustomFont(@NonNull JSONObject overridesJson) {
    final String keyFontFamily = overridesJson.optString(KEY_KEY_FONT_FAMILY, null);
    final String tokenSecondaryFontFamily =
        overridesJson.optString(KEY_TOKEN_SECONDARY_FONT_FAMILY, null);
    final String hintFontFamily = overridesJson.optString(KEY_HINT_FONT_FAMILY, null);
    final String suggestionFontFamily = overridesJson.optString(KEY_SUGGESTION_FONT_FAMILY, null);
    final String keyboardNameFontFamily =
        overridesJson.optString(KEY_KEYBOARD_NAME_FONT_FAMILY, null);
    return usesCustomFont(
        keyFontFamily,
        tokenSecondaryFontFamily,
        hintFontFamily,
        suggestionFontFamily,
        keyboardNameFontFamily);
  }

  private static boolean usesCustomFont(
      @Nullable String keyFontFamily,
      @Nullable String tokenSecondaryFontFamily,
      @Nullable String hintFontFamily,
      @Nullable String suggestionFontFamily,
      @Nullable String keyboardNameFontFamily) {
    final String resolvedHintFontFamily =
        KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_TOKEN_SECONDARY.equals(hintFontFamily)
            ? tokenSecondaryFontFamily
            : hintFontFamily;
    final String resolvedSuggestionFontFamily =
        KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_TOKEN_SECONDARY.equals(suggestionFontFamily)
            ? tokenSecondaryFontFamily
            : suggestionFontFamily;
    final String resolvedKeyboardNameFontFamily =
        KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_TOKEN_SECONDARY.equals(
                keyboardNameFontFamily)
            ? tokenSecondaryFontFamily
            : keyboardNameFontFamily;
    return KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM.equals(keyFontFamily)
        || KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM.equals(tokenSecondaryFontFamily)
        || KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM.equals(resolvedHintFontFamily)
        || KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM.equals(
            resolvedSuggestionFontFamily)
        || KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM.equals(
            resolvedKeyboardNameFontFamily);
  }
}
