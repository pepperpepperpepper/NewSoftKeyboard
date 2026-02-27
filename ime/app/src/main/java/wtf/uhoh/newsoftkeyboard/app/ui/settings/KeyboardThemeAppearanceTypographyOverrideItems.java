package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;

final class KeyboardThemeAppearanceTypographyOverrideItems {

  @NonNull
  static List<KeyboardThemeAppearanceOverrideItem> build(
      @NonNull Context context,
      @NonNull String presetId,
      @Nullable KeyboardThemeUserOverridesStore store) {
    if (store == null) return Collections.emptyList();
    final List<KeyboardThemeAppearanceOverrideItem> out = new ArrayList<>();

    final String tokenSecondaryFontFamily = store.getTokenSecondaryFontFamily(presetId);
    if (tokenSecondaryFontFamily != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(
                  R.string.keyboard_theme_appearance_token_secondary_font_family_title),
              fontFamilyLabel(context, tokenSecondaryFontFamily),
              "keyboard_theme_token_secondary_font_family",
              () -> store.clearTokenSecondaryFontFamily(presetId)));
    }
    final Integer tokenSecondaryFontStyle = store.getTokenSecondaryFontStyle(presetId);
    if (tokenSecondaryFontStyle != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(
                  R.string.keyboard_theme_appearance_token_secondary_font_style_title),
              fontStyleLabel(context, tokenSecondaryFontStyle),
              "keyboard_theme_token_secondary_font_style",
              () -> store.clearTokenSecondaryFontStyle(presetId)));
    }
    final Integer tokenSecondaryTextSizePercent = store.getTokenSecondaryTextSizePercent(presetId);
    if (tokenSecondaryTextSizePercent != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_appearance_token_secondary_text_size_title),
              tokenSecondaryTextSizePercent + "%",
              "keyboard_theme_token_secondary_text_size",
              () -> store.clearTokenSecondaryTextSizePercent(presetId)));
    }

    final String keyFontFamily = store.getKeyFontFamily(presetId);
    if (keyFontFamily != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_appearance_key_font_family_title),
              fontFamilyLabel(context, keyFontFamily),
              "keyboard_theme_override_key_font_family",
              () -> store.clearKeyFontFamily(presetId)));
    }
    final Integer keyFontStyle = store.getKeyFontStyle(presetId);
    if (keyFontStyle != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_appearance_key_font_style_title),
              fontStyleLabel(context, keyFontStyle),
              "keyboard_theme_override_key_font_style",
              () -> store.clearKeyFontStyle(presetId)));
    }

    final String hintFontFamily = store.getHintFontFamily(presetId);
    if (hintFontFamily != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_appearance_hint_font_family_title),
              fontFamilyLabel(context, hintFontFamily),
              "keyboard_theme_override_hint_font_family",
              () -> store.clearHintFontFamily(presetId)));
    }
    final Integer hintFontStyle = store.getHintFontStyle(presetId);
    if (hintFontStyle != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_appearance_hint_font_style_title),
              fontStyleLabel(context, hintFontStyle),
              "keyboard_theme_override_hint_font_style",
              () -> store.clearHintFontStyle(presetId)));
    }

    final String suggestionFontFamily = store.getSuggestionFontFamily(presetId);
    if (suggestionFontFamily != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_appearance_suggestion_font_family_title),
              fontFamilyLabel(context, suggestionFontFamily),
              "keyboard_theme_override_suggestion_font_family",
              () -> store.clearSuggestionFontFamily(presetId)));
    }
    final Integer suggestionFontStyle = store.getSuggestionFontStyle(presetId);
    if (suggestionFontStyle != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_appearance_suggestion_font_style_title),
              fontStyleLabel(context, suggestionFontStyle),
              "keyboard_theme_override_suggestion_font_style",
              () -> store.clearSuggestionFontStyle(presetId)));
    }

    final String keyboardNameFontFamily = store.getKeyboardNameFontFamily(presetId);
    if (keyboardNameFontFamily != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_appearance_keyboard_name_font_family_title),
              fontFamilyLabel(context, keyboardNameFontFamily),
              "keyboard_theme_override_keyboard_name_font_family",
              () -> store.clearKeyboardNameFontFamily(presetId)));
    }
    final Integer keyboardNameFontStyle = store.getKeyboardNameFontStyle(presetId);
    if (keyboardNameFontStyle != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_appearance_keyboard_name_font_style_title),
              fontStyleLabel(context, keyboardNameFontStyle),
              "keyboard_theme_override_keyboard_name_font_style",
              () -> store.clearKeyboardNameFontStyle(presetId)));
    }

    if (store.hasCustomKeyFont(presetId)) {
      final String name = store.getCustomKeyFontDisplayName(presetId);
      final CharSequence summary =
          name == null || name.trim().isEmpty()
              ? context.getString(
                  R.string.keyboard_theme_appearance_custom_font_import_summary_imported_unknown)
              : context.getString(
                  R.string.keyboard_theme_appearance_custom_font_import_summary_imported, name);
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_appearance_custom_font_import_title),
              summary,
              "keyboard_theme_override_key_custom_font_import",
              () -> store.clearCustomKeyFont(presetId)));
    }

    final Integer keyLabelSize = store.getKeyLabelTextSizePercent(presetId);
    if (keyLabelSize != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_appearance_key_label_text_size_title),
              keyLabelSize + "%",
              "keyboard_theme_override_key_label_text_size",
              () -> store.clearKeyLabelTextSizePercent(presetId)));
    }
    final Integer hintSize = store.getHintTextSizePercent(presetId);
    if (hintSize != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_appearance_hint_text_size_title),
              hintSize == KeyboardThemeUserOverridesStore.TOKEN_SECONDARY_INT
                  ? context.getString(
                      R.string.keyboard_theme_appearance_typography_secondary_token_entry)
                  : hintSize + "%",
              "keyboard_theme_override_hint_text_size",
              () -> store.clearHintTextSizePercent(presetId)));
    }
    final Integer suggestionSize = store.getSuggestionTextSizePercent(presetId);
    if (suggestionSize != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_appearance_suggestion_text_size_title),
              suggestionSize == KeyboardThemeUserOverridesStore.TOKEN_SECONDARY_INT
                  ? context.getString(
                      R.string.keyboard_theme_appearance_typography_secondary_token_entry)
                  : suggestionSize + "%",
              "keyboard_theme_override_suggestion_text_size",
              () -> store.clearSuggestionTextSizePercent(presetId)));
    }
    final Integer keyboardNameSize = store.getKeyboardNameTextSizePercent(presetId);
    if (keyboardNameSize != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_appearance_keyboard_name_text_size_title),
              keyboardNameSize == KeyboardThemeUserOverridesStore.TOKEN_SECONDARY_INT
                  ? context.getString(
                      R.string.keyboard_theme_appearance_typography_secondary_token_entry)
                  : keyboardNameSize + "%",
              "keyboard_theme_override_keyboard_name_text_size",
              () -> store.clearKeyboardNameTextSizePercent(presetId)));
    }

    final Boolean autoFit = store.getKeyLabelAutoFitEnabled(presetId);
    if (autoFit != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_appearance_auto_fit_key_labels_title),
              autoFit
                  ? context.getString(android.R.string.yes)
                  : context.getString(android.R.string.no),
              "keyboard_theme_override_key_label_auto_fit",
              () -> store.clearKeyLabelAutoFitEnabled(presetId)));
    }

    final Integer minSize = store.getKeyLabelAutoFitMinSizePercent(presetId);
    if (minSize != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(
                  R.string.keyboard_theme_appearance_auto_fit_key_labels_min_size_title),
              minSize + "%",
              "keyboard_theme_override_key_label_auto_fit_min_size_percent",
              () -> store.clearKeyLabelAutoFitMinSizePercent(presetId)));
    }

    final Boolean ellipsize = store.getKeyLabelEllipsizeEnabled(presetId);
    if (ellipsize != null) {
      out.add(
          new KeyboardThemeAppearanceOverrideItem(
              context.getString(R.string.keyboard_theme_appearance_ellipsize_key_labels_title),
              ellipsize
                  ? context.getString(android.R.string.yes)
                  : context.getString(android.R.string.no),
              "keyboard_theme_override_key_label_ellipsize",
              () -> store.clearKeyLabelEllipsizeEnabled(presetId)));
    }

    return out;
  }

  @NonNull
  private static String fontFamilyLabel(@NonNull Context context, @NonNull String id) {
    return switch (id) {
      case KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_TOKEN_SECONDARY ->
          context.getString(R.string.keyboard_theme_appearance_typography_secondary_token_entry);
      case "default" ->
          context.getString(R.string.keyboard_theme_appearance_font_family_default_entry);
      case "sans" -> context.getString(R.string.keyboard_theme_appearance_font_family_sans_entry);
      case "serif" -> context.getString(R.string.keyboard_theme_appearance_font_family_serif_entry);
      case "monospace" ->
          context.getString(R.string.keyboard_theme_appearance_font_family_monospace_entry);
      case KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM ->
          context.getString(R.string.keyboard_theme_appearance_font_family_custom_entry);
      default -> id;
    };
  }

  @NonNull
  private static String fontStyleLabel(@NonNull Context context, int style) {
    if (style == KeyboardThemeUserOverridesStore.TOKEN_SECONDARY_INT) {
      return context.getString(R.string.keyboard_theme_appearance_typography_secondary_token_entry);
    }
    return switch (style) {
      case 0 -> context.getString(R.string.keyboard_theme_appearance_font_style_normal_entry);
      case 1 -> context.getString(R.string.keyboard_theme_appearance_font_style_bold_entry);
      case 2 -> context.getString(R.string.keyboard_theme_appearance_font_style_italic_entry);
      case 3 -> context.getString(R.string.keyboard_theme_appearance_font_style_bold_italic_entry);
      default -> String.valueOf(style);
    };
  }
}
