package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.KeyboardThemeCustomizationCustomKeyFontController.CustomFontTarget;

final class KeyboardThemeCustomizationTypographyFontOverridesUi {

  @NonNull private final KeyboardThemeCustomizationTypographySection.Host host;
  @Nullable private final KeyboardThemeUserOverridesStore themeOverridesStore;
  @NonNull private final KeyboardThemeCustomizationCustomKeyFontController customKeyFontController;

  @Nullable private ListPreference keyFontFamilyPref;
  @Nullable private ListPreference keyFontStylePref;
  @Nullable private ListPreference hintFontFamilyPref;
  @Nullable private ListPreference hintFontStylePref;
  @Nullable private ListPreference suggestionFontFamilyPref;
  @Nullable private ListPreference suggestionFontStylePref;
  @Nullable private ListPreference keyboardNameFontFamilyPref;
  @Nullable private ListPreference keyboardNameFontStylePref;

  KeyboardThemeCustomizationTypographyFontOverridesUi(
      @NonNull KeyboardThemeCustomizationTypographySection.Host host,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore,
      @NonNull KeyboardThemeCustomizationCustomKeyFontController customKeyFontController) {
    this.host = host;
    this.themeOverridesStore = themeOverridesStore;
    this.customKeyFontController = customKeyFontController;
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceCategory typography) {
    keyFontFamilyPref = new ListPreference(context);
    keyFontFamilyPref.setKey("keyboard_theme_override_key_font_family");
    keyFontFamilyPref.setPersistent(false);
    keyFontFamilyPref.setTitle(R.string.keyboard_theme_appearance_key_font_family_title);
    final CharSequence fontFamilySummaryBase =
        context.getText(R.string.keyboard_theme_appearance_key_font_family_summary);
    keyFontFamilyPref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return fontFamilySummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return fontFamilySummaryBase;
          return fontFamilySummaryBase + "\n" + entry;
        });
    keyFontFamilyPref.setEntries(
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_appearance_theme_default_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_default_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_sans_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_serif_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_monospace_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_custom_entry)
        });
    keyFontFamilyPref.setEntryValues(
        new CharSequence[] {
          "theme",
          "default",
          "sans",
          "serif",
          "monospace",
          KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM
        });
    keyFontFamilyPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.equals("theme")) {
            themeOverridesStore.clearKeyFontFamily(themeId);
            host.refreshState();
            return true;
          }

          if (raw.equals(KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM)
              && !customKeyFontController.ensureCustomKeyFontAvailableOrStartPick(
                  context, themeId, CustomFontTarget.KEY_FONT_FAMILY)) {
            return false;
          }

          themeOverridesStore.setKeyFontFamily(themeId, raw);
          host.refreshState();
          return true;
        });
    typography.addPreference(keyFontFamilyPref);

    keyFontStylePref = new ListPreference(context);
    keyFontStylePref.setKey("keyboard_theme_override_key_font_style");
    keyFontStylePref.setPersistent(false);
    keyFontStylePref.setTitle(R.string.keyboard_theme_appearance_key_font_style_title);
    final CharSequence fontStyleSummaryBase =
        context.getText(R.string.keyboard_theme_appearance_key_font_style_summary);
    keyFontStylePref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return fontStyleSummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return fontStyleSummaryBase;
          return fontStyleSummaryBase + "\n" + entry;
        });
    keyFontStylePref.setEntries(
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_appearance_theme_default_entry),
          context.getString(R.string.keyboard_theme_appearance_font_style_normal_entry),
          context.getString(R.string.keyboard_theme_appearance_font_style_bold_entry),
          context.getString(R.string.keyboard_theme_appearance_font_style_italic_entry),
          context.getString(R.string.keyboard_theme_appearance_font_style_bold_italic_entry)
        });
    keyFontStylePref.setEntryValues(new CharSequence[] {"theme", "0", "1", "2", "3"});
    keyFontStylePref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.equals("theme")) {
            themeOverridesStore.clearKeyFontStyle(themeId);
            host.refreshState();
            return true;
          }
          try {
            final int style = Integer.parseInt(raw);
            themeOverridesStore.setKeyFontStyle(themeId, style);
            host.refreshState();
            return true;
          } catch (NumberFormatException e) {
            return false;
          }
        });
    typography.addPreference(keyFontStylePref);

    hintFontFamilyPref = new ListPreference(context);
    hintFontFamilyPref.setKey("keyboard_theme_override_hint_font_family");
    hintFontFamilyPref.setPersistent(false);
    hintFontFamilyPref.setTitle(R.string.keyboard_theme_appearance_hint_font_family_title);
    final CharSequence hintFontFamilySummaryBase =
        context.getText(R.string.keyboard_theme_appearance_hint_font_family_summary);
    hintFontFamilyPref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return hintFontFamilySummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return hintFontFamilySummaryBase;
          return hintFontFamilySummaryBase + "\n" + entry;
        });
    hintFontFamilyPref.setEntries(
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_appearance_font_family_follow_key_entry),
          context.getString(R.string.keyboard_theme_appearance_typography_secondary_token_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_default_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_sans_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_serif_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_monospace_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_custom_entry)
        });
    hintFontFamilyPref.setEntryValues(
        new CharSequence[] {
          "key",
          KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_TOKEN_SECONDARY,
          "default",
          "sans",
          "serif",
          "monospace",
          KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM
        });
    hintFontFamilyPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.equals("key")) {
            themeOverridesStore.clearHintFontFamily(themeId);
            host.refreshState();
            return true;
          }

          if (raw.equals(KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM)
              && !customKeyFontController.ensureCustomKeyFontAvailableOrStartPick(
                  context, themeId, CustomFontTarget.HINT_FONT_FAMILY)) {
            return false;
          }

          themeOverridesStore.setHintFontFamily(themeId, raw);
          host.refreshState();
          return true;
        });
    typography.addPreference(hintFontFamilyPref);

    hintFontStylePref = new ListPreference(context);
    hintFontStylePref.setKey("keyboard_theme_override_hint_font_style");
    hintFontStylePref.setPersistent(false);
    hintFontStylePref.setTitle(R.string.keyboard_theme_appearance_hint_font_style_title);
    final CharSequence hintFontStyleSummaryBase =
        context.getText(R.string.keyboard_theme_appearance_hint_font_style_summary);
    hintFontStylePref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return hintFontStyleSummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return hintFontStyleSummaryBase;
          return hintFontStyleSummaryBase + "\n" + entry;
        });
    hintFontStylePref.setEntries(
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_appearance_font_style_follow_key_entry),
          context.getString(R.string.keyboard_theme_appearance_typography_secondary_token_entry),
          context.getString(R.string.keyboard_theme_appearance_font_style_normal_entry),
          context.getString(R.string.keyboard_theme_appearance_font_style_bold_entry),
          context.getString(R.string.keyboard_theme_appearance_font_style_italic_entry),
          context.getString(R.string.keyboard_theme_appearance_font_style_bold_italic_entry)
        });
    hintFontStylePref.setEntryValues(
        new CharSequence[] {
          "key",
          String.valueOf(KeyboardThemeUserOverridesStore.TOKEN_SECONDARY_INT),
          "0",
          "1",
          "2",
          "3"
        });
    hintFontStylePref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.equals("key")) {
            themeOverridesStore.clearHintFontStyle(themeId);
            host.refreshState();
            return true;
          }
          try {
            final int style = Integer.parseInt(raw);
            themeOverridesStore.setHintFontStyle(themeId, style);
            host.refreshState();
            return true;
          } catch (NumberFormatException e) {
            return false;
          }
        });
    typography.addPreference(hintFontStylePref);

    suggestionFontFamilyPref = new ListPreference(context);
    suggestionFontFamilyPref.setKey("keyboard_theme_override_suggestion_font_family");
    suggestionFontFamilyPref.setPersistent(false);
    suggestionFontFamilyPref.setTitle(
        R.string.keyboard_theme_appearance_suggestion_font_family_title);
    final CharSequence suggestionFontFamilySummaryBase =
        context.getText(R.string.keyboard_theme_appearance_suggestion_font_family_summary);
    suggestionFontFamilyPref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return suggestionFontFamilySummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return suggestionFontFamilySummaryBase;
          return suggestionFontFamilySummaryBase + "\n" + entry;
        });
    suggestionFontFamilyPref.setEntries(
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_appearance_font_family_follow_key_entry),
          context.getString(R.string.keyboard_theme_appearance_typography_secondary_token_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_default_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_sans_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_serif_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_monospace_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_custom_entry)
        });
    suggestionFontFamilyPref.setEntryValues(
        new CharSequence[] {
          "key",
          KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_TOKEN_SECONDARY,
          "default",
          "sans",
          "serif",
          "monospace",
          KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM
        });
    suggestionFontFamilyPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.equals("key")) {
            themeOverridesStore.clearSuggestionFontFamily(themeId);
            host.refreshState();
            return true;
          }

          if (raw.equals(KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM)
              && !customKeyFontController.ensureCustomKeyFontAvailableOrStartPick(
                  context, themeId, CustomFontTarget.SUGGESTION_FONT_FAMILY)) {
            return false;
          }

          themeOverridesStore.setSuggestionFontFamily(themeId, raw);
          host.refreshState();
          return true;
        });
    typography.addPreference(suggestionFontFamilyPref);

    suggestionFontStylePref = new ListPreference(context);
    suggestionFontStylePref.setKey("keyboard_theme_override_suggestion_font_style");
    suggestionFontStylePref.setPersistent(false);
    suggestionFontStylePref.setTitle(
        R.string.keyboard_theme_appearance_suggestion_font_style_title);
    final CharSequence suggestionFontStyleSummaryBase =
        context.getText(R.string.keyboard_theme_appearance_suggestion_font_style_summary);
    suggestionFontStylePref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return suggestionFontStyleSummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return suggestionFontStyleSummaryBase;
          return suggestionFontStyleSummaryBase + "\n" + entry;
        });
    suggestionFontStylePref.setEntries(
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_appearance_font_style_follow_key_entry),
          context.getString(R.string.keyboard_theme_appearance_typography_secondary_token_entry),
          context.getString(R.string.keyboard_theme_appearance_font_style_normal_entry),
          context.getString(R.string.keyboard_theme_appearance_font_style_bold_entry),
          context.getString(R.string.keyboard_theme_appearance_font_style_italic_entry),
          context.getString(R.string.keyboard_theme_appearance_font_style_bold_italic_entry)
        });
    suggestionFontStylePref.setEntryValues(
        new CharSequence[] {
          "key",
          String.valueOf(KeyboardThemeUserOverridesStore.TOKEN_SECONDARY_INT),
          "0",
          "1",
          "2",
          "3"
        });
    suggestionFontStylePref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.equals("key")) {
            themeOverridesStore.clearSuggestionFontStyle(themeId);
            host.refreshState();
            return true;
          }
          try {
            final int style = Integer.parseInt(raw);
            themeOverridesStore.setSuggestionFontStyle(themeId, style);
            host.refreshState();
            return true;
          } catch (NumberFormatException e) {
            return false;
          }
        });
    typography.addPreference(suggestionFontStylePref);

    keyboardNameFontFamilyPref = new ListPreference(context);
    keyboardNameFontFamilyPref.setKey("keyboard_theme_override_keyboard_name_font_family");
    keyboardNameFontFamilyPref.setPersistent(false);
    keyboardNameFontFamilyPref.setTitle(
        R.string.keyboard_theme_appearance_keyboard_name_font_family_title);
    final CharSequence keyboardNameFontFamilySummaryBase =
        context.getText(R.string.keyboard_theme_appearance_keyboard_name_font_family_summary);
    keyboardNameFontFamilyPref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return keyboardNameFontFamilySummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return keyboardNameFontFamilySummaryBase;
          return keyboardNameFontFamilySummaryBase + "\n" + entry;
        });
    keyboardNameFontFamilyPref.setEntries(
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_appearance_font_family_follow_label_entry),
          context.getString(R.string.keyboard_theme_appearance_typography_secondary_token_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_default_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_sans_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_serif_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_monospace_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_custom_entry)
        });
    keyboardNameFontFamilyPref.setEntryValues(
        new CharSequence[] {
          "label",
          KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_TOKEN_SECONDARY,
          "default",
          "sans",
          "serif",
          "monospace",
          KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM
        });
    keyboardNameFontFamilyPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.equals("label")) {
            themeOverridesStore.clearKeyboardNameFontFamily(themeId);
            host.refreshState();
            return true;
          }

          if (raw.equals(KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM)
              && !customKeyFontController.ensureCustomKeyFontAvailableOrStartPick(
                  context, themeId, CustomFontTarget.KEYBOARD_NAME_FONT_FAMILY)) {
            return false;
          }

          themeOverridesStore.setKeyboardNameFontFamily(themeId, raw);
          host.refreshState();
          return true;
        });
    typography.addPreference(keyboardNameFontFamilyPref);

    keyboardNameFontStylePref = new ListPreference(context);
    keyboardNameFontStylePref.setKey("keyboard_theme_override_keyboard_name_font_style");
    keyboardNameFontStylePref.setPersistent(false);
    keyboardNameFontStylePref.setTitle(
        R.string.keyboard_theme_appearance_keyboard_name_font_style_title);
    final CharSequence keyboardNameFontStyleSummaryBase =
        context.getText(R.string.keyboard_theme_appearance_keyboard_name_font_style_summary);
    keyboardNameFontStylePref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return keyboardNameFontStyleSummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return keyboardNameFontStyleSummaryBase;
          return keyboardNameFontStyleSummaryBase + "\n" + entry;
        });
    keyboardNameFontStylePref.setEntries(
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_appearance_font_style_follow_label_entry),
          context.getString(R.string.keyboard_theme_appearance_typography_secondary_token_entry),
          context.getString(R.string.keyboard_theme_appearance_font_style_normal_entry),
          context.getString(R.string.keyboard_theme_appearance_font_style_bold_entry),
          context.getString(R.string.keyboard_theme_appearance_font_style_italic_entry),
          context.getString(R.string.keyboard_theme_appearance_font_style_bold_italic_entry)
        });
    keyboardNameFontStylePref.setEntryValues(
        new CharSequence[] {
          "label",
          String.valueOf(KeyboardThemeUserOverridesStore.TOKEN_SECONDARY_INT),
          "0",
          "1",
          "2",
          "3"
        });
    keyboardNameFontStylePref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.equals("label")) {
            themeOverridesStore.clearKeyboardNameFontStyle(themeId);
            host.refreshState();
            return true;
          }
          try {
            final int style = Integer.parseInt(raw);
            themeOverridesStore.setKeyboardNameFontStyle(themeId, style);
            host.refreshState();
            return true;
          } catch (NumberFormatException e) {
            return false;
          }
        });
    typography.addPreference(keyboardNameFontStylePref);
  }

  void refreshState(@NonNull String themeId, boolean importInProgress) {
    final String keyFontFamily =
        themeOverridesStore != null ? themeOverridesStore.getKeyFontFamily(themeId) : null;
    final Integer keyFontStyle =
        themeOverridesStore != null ? themeOverridesStore.getKeyFontStyle(themeId) : null;
    final String hintFontFamily =
        themeOverridesStore != null ? themeOverridesStore.getHintFontFamily(themeId) : null;
    final Integer hintFontStyle =
        themeOverridesStore != null ? themeOverridesStore.getHintFontStyle(themeId) : null;
    final String suggestionFontFamily =
        themeOverridesStore != null ? themeOverridesStore.getSuggestionFontFamily(themeId) : null;
    final Integer suggestionFontStyle =
        themeOverridesStore != null ? themeOverridesStore.getSuggestionFontStyle(themeId) : null;
    final String keyboardNameFontFamily =
        themeOverridesStore != null ? themeOverridesStore.getKeyboardNameFontFamily(themeId) : null;
    final Integer keyboardNameFontStyle =
        themeOverridesStore != null ? themeOverridesStore.getKeyboardNameFontStyle(themeId) : null;

    if (keyFontFamilyPref != null) {
      keyFontFamilyPref.setEnabled(!importInProgress);
      keyFontFamilyPref.setValue(keyFontFamily != null ? keyFontFamily : "theme");
    }
    if (keyFontStylePref != null) {
      keyFontStylePref.setEnabled(!importInProgress);
      keyFontStylePref.setValue(keyFontStyle != null ? String.valueOf(keyFontStyle) : "theme");
    }
    if (hintFontFamilyPref != null) {
      hintFontFamilyPref.setEnabled(!importInProgress);
      hintFontFamilyPref.setValue(hintFontFamily != null ? hintFontFamily : "key");
    }
    if (hintFontStylePref != null) {
      hintFontStylePref.setEnabled(!importInProgress);
      hintFontStylePref.setValue(hintFontStyle != null ? String.valueOf(hintFontStyle) : "key");
    }
    if (suggestionFontFamilyPref != null) {
      suggestionFontFamilyPref.setEnabled(!importInProgress);
      suggestionFontFamilyPref.setValue(
          suggestionFontFamily != null ? suggestionFontFamily : "key");
    }
    if (suggestionFontStylePref != null) {
      suggestionFontStylePref.setEnabled(!importInProgress);
      suggestionFontStylePref.setValue(
          suggestionFontStyle != null ? String.valueOf(suggestionFontStyle) : "key");
    }
    if (keyboardNameFontFamilyPref != null) {
      keyboardNameFontFamilyPref.setEnabled(!importInProgress);
      keyboardNameFontFamilyPref.setValue(
          keyboardNameFontFamily != null ? keyboardNameFontFamily : "label");
    }
    if (keyboardNameFontStylePref != null) {
      keyboardNameFontStylePref.setEnabled(!importInProgress);
      keyboardNameFontStylePref.setValue(
          keyboardNameFontStyle != null ? String.valueOf(keyboardNameFontStyle) : "label");
    }
  }

  void dispose() {}
}
