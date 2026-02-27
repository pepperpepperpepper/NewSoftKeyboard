package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;

final class KeyboardThemeCustomizationTypographySizeOverridesUi {

  @NonNull private final KeyboardThemeCustomizationTypographySection.Host host;
  @Nullable private final KeyboardThemeUserOverridesStore themeOverridesStore;

  @Nullable private ListPreference keyLabelTextSizePref;
  @Nullable private ListPreference hintTextSizePref;
  @Nullable private ListPreference suggestionTextSizePref;
  @Nullable private ListPreference keyboardNameTextSizePref;
  @Nullable private CheckBoxPreference autoFitKeyLabelsPref;
  @Nullable private ListPreference keyLabelAutoFitMinSizePref;
  @Nullable private CheckBoxPreference ellipsizeKeyLabelsPref;

  KeyboardThemeCustomizationTypographySizeOverridesUi(
      @NonNull KeyboardThemeCustomizationTypographySection.Host host,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore) {
    this.host = host;
    this.themeOverridesStore = themeOverridesStore;
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceCategory typography) {
    keyLabelTextSizePref = new ListPreference(context);
    keyLabelTextSizePref.setKey("keyboard_theme_override_key_label_text_size");
    keyLabelTextSizePref.setPersistent(false);
    keyLabelTextSizePref.setTitle(R.string.keyboard_theme_appearance_key_label_text_size_title);
    final CharSequence keyLabelTextSizeSummaryBase =
        context.getText(R.string.keyboard_theme_appearance_key_label_text_size_summary);
    keyLabelTextSizePref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return keyLabelTextSizeSummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return keyLabelTextSizeSummaryBase;
          return keyLabelTextSizeSummaryBase + "\n" + entry;
        });
    keyLabelTextSizePref.setEntries(
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_appearance_theme_default_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_smaller_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_small_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_large_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_larger_entry)
        });
    keyLabelTextSizePref.setEntryValues(new CharSequence[] {"theme", "80", "90", "110", "120"});
    keyLabelTextSizePref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.equals("theme")) {
            themeOverridesStore.clearKeyLabelTextSizePercent(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setKeyLabelTextSizePercent(themeId, Integer.parseInt(raw));
            host.refreshState();
            return true;
          } catch (NumberFormatException e) {
            return false;
          }
        });
    typography.addPreference(keyLabelTextSizePref);

    hintTextSizePref = new ListPreference(context);
    hintTextSizePref.setKey("keyboard_theme_override_hint_text_size");
    hintTextSizePref.setPersistent(false);
    hintTextSizePref.setTitle(R.string.keyboard_theme_appearance_hint_text_size_title);
    final CharSequence hintTextSizeSummaryBase =
        context.getText(R.string.keyboard_theme_appearance_hint_text_size_summary);
    hintTextSizePref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return hintTextSizeSummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return hintTextSizeSummaryBase;
          return hintTextSizeSummaryBase + "\n" + entry;
        });
    hintTextSizePref.setEntries(
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_appearance_text_size_follow_key_entry),
          context.getString(R.string.keyboard_theme_appearance_typography_secondary_token_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_theme_default_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_smaller_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_small_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_large_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_larger_entry)
        });
    hintTextSizePref.setEntryValues(
        new CharSequence[] {
          "key",
          String.valueOf(KeyboardThemeUserOverridesStore.TOKEN_SECONDARY_INT),
          "100",
          "80",
          "90",
          "110",
          "120"
        });
    hintTextSizePref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.equals("key")) {
            themeOverridesStore.clearHintTextSizePercent(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setHintTextSizePercent(themeId, Integer.parseInt(raw));
            host.refreshState();
            return true;
          } catch (NumberFormatException e) {
            return false;
          }
        });
    typography.addPreference(hintTextSizePref);

    suggestionTextSizePref = new ListPreference(context);
    suggestionTextSizePref.setKey("keyboard_theme_override_suggestion_text_size");
    suggestionTextSizePref.setPersistent(false);
    suggestionTextSizePref.setTitle(R.string.keyboard_theme_appearance_suggestion_text_size_title);
    final CharSequence suggestionTextSizeSummaryBase =
        context.getText(R.string.keyboard_theme_appearance_suggestion_text_size_summary);
    suggestionTextSizePref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return suggestionTextSizeSummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return suggestionTextSizeSummaryBase;
          return suggestionTextSizeSummaryBase + "\n" + entry;
        });
    suggestionTextSizePref.setEntries(
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_appearance_text_size_follow_key_entry),
          context.getString(R.string.keyboard_theme_appearance_typography_secondary_token_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_theme_default_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_smaller_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_small_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_large_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_larger_entry)
        });
    suggestionTextSizePref.setEntryValues(
        new CharSequence[] {
          "key",
          String.valueOf(KeyboardThemeUserOverridesStore.TOKEN_SECONDARY_INT),
          "100",
          "80",
          "90",
          "110",
          "120"
        });
    suggestionTextSizePref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.equals("key")) {
            themeOverridesStore.clearSuggestionTextSizePercent(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setSuggestionTextSizePercent(themeId, Integer.parseInt(raw));
            host.refreshState();
            return true;
          } catch (NumberFormatException e) {
            return false;
          }
        });
    typography.addPreference(suggestionTextSizePref);

    keyboardNameTextSizePref = new ListPreference(context);
    keyboardNameTextSizePref.setKey("keyboard_theme_override_keyboard_name_text_size");
    keyboardNameTextSizePref.setPersistent(false);
    keyboardNameTextSizePref.setTitle(
        R.string.keyboard_theme_appearance_keyboard_name_text_size_title);
    final CharSequence keyboardNameTextSizeSummaryBase =
        context.getText(R.string.keyboard_theme_appearance_keyboard_name_text_size_summary);
    keyboardNameTextSizePref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return keyboardNameTextSizeSummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return keyboardNameTextSizeSummaryBase;
          return keyboardNameTextSizeSummaryBase + "\n" + entry;
        });
    keyboardNameTextSizePref.setEntries(
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_appearance_text_size_follow_key_entry),
          context.getString(R.string.keyboard_theme_appearance_typography_secondary_token_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_theme_default_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_smaller_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_small_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_large_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_larger_entry)
        });
    keyboardNameTextSizePref.setEntryValues(
        new CharSequence[] {
          "key",
          String.valueOf(KeyboardThemeUserOverridesStore.TOKEN_SECONDARY_INT),
          "100",
          "80",
          "90",
          "110",
          "120"
        });
    keyboardNameTextSizePref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.equals("key")) {
            themeOverridesStore.clearKeyboardNameTextSizePercent(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setKeyboardNameTextSizePercent(themeId, Integer.parseInt(raw));
            host.refreshState();
            return true;
          } catch (NumberFormatException e) {
            return false;
          }
        });
    typography.addPreference(keyboardNameTextSizePref);

    autoFitKeyLabelsPref = new CheckBoxPreference(context);
    autoFitKeyLabelsPref.setKey("keyboard_theme_override_key_label_auto_fit");
    autoFitKeyLabelsPref.setPersistent(false);
    autoFitKeyLabelsPref.setTitle(R.string.keyboard_theme_appearance_auto_fit_key_labels_title);
    autoFitKeyLabelsPref.setSummary(R.string.keyboard_theme_appearance_auto_fit_key_labels_summary);
    autoFitKeyLabelsPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          themeOverridesStore.setKeyLabelAutoFitEnabled(themeId, Boolean.TRUE.equals(newValue));
          host.refreshState();
          return true;
        });
    typography.addPreference(autoFitKeyLabelsPref);

    keyLabelAutoFitMinSizePref = new ListPreference(context);
    keyLabelAutoFitMinSizePref.setKey(
        "keyboard_theme_override_key_label_auto_fit_min_size_percent");
    keyLabelAutoFitMinSizePref.setPersistent(false);
    keyLabelAutoFitMinSizePref.setTitle(
        R.string.keyboard_theme_appearance_auto_fit_key_labels_min_size_title);
    final CharSequence minSizeSummaryBase =
        context.getText(R.string.keyboard_theme_appearance_auto_fit_key_labels_min_size_summary);
    keyLabelAutoFitMinSizePref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return minSizeSummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return minSizeSummaryBase;
          return minSizeSummaryBase + "\n" + entry;
        });
    keyLabelAutoFitMinSizePref.setEntries(
        new CharSequence[] {
          context.getString(
              R.string.keyboard_theme_appearance_auto_fit_key_labels_min_size_default_entry),
          "40%",
          "50%",
          "60%"
        });
    keyLabelAutoFitMinSizePref.setEntryValues(new CharSequence[] {"30", "40", "50", "60"});
    keyLabelAutoFitMinSizePref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          try {
            final int percent = Integer.parseInt(String.valueOf(newValue));
            if (percent == 30) {
              themeOverridesStore.clearKeyLabelAutoFitMinSizePercent(themeId);
            } else {
              themeOverridesStore.setKeyLabelAutoFitMinSizePercent(themeId, percent);
            }
            host.refreshState();
            return true;
          } catch (NumberFormatException e) {
            return false;
          }
        });
    typography.addPreference(keyLabelAutoFitMinSizePref);

    ellipsizeKeyLabelsPref = new CheckBoxPreference(context);
    ellipsizeKeyLabelsPref.setKey("keyboard_theme_override_key_label_ellipsize");
    ellipsizeKeyLabelsPref.setPersistent(false);
    ellipsizeKeyLabelsPref.setTitle(R.string.keyboard_theme_appearance_ellipsize_key_labels_title);
    ellipsizeKeyLabelsPref.setSummary(
        R.string.keyboard_theme_appearance_ellipsize_key_labels_summary);
    ellipsizeKeyLabelsPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final boolean enabled = Boolean.TRUE.equals(newValue);
          if (enabled) {
            themeOverridesStore.clearKeyLabelEllipsizeEnabled(themeId);
          } else {
            themeOverridesStore.setKeyLabelEllipsizeEnabled(themeId, false);
          }
          host.refreshState();
          return true;
        });
    typography.addPreference(ellipsizeKeyLabelsPref);
  }

  void refreshState(@NonNull String themeId, boolean importInProgress) {
    final Boolean autoFitKeyLabels =
        themeOverridesStore != null ? themeOverridesStore.getKeyLabelAutoFitEnabled(themeId) : null;
    final Integer keyLabelAutoFitMinSizePercent =
        themeOverridesStore != null
            ? themeOverridesStore.getKeyLabelAutoFitMinSizePercent(themeId)
            : null;
    final Boolean ellipsizeKeyLabels =
        themeOverridesStore != null
            ? themeOverridesStore.getKeyLabelEllipsizeEnabled(themeId)
            : null;
    final Integer keyLabelTextSizePercent =
        themeOverridesStore != null
            ? themeOverridesStore.getKeyLabelTextSizePercent(themeId)
            : null;
    final Integer hintTextSizePercent =
        themeOverridesStore != null ? themeOverridesStore.getHintTextSizePercent(themeId) : null;
    final Integer suggestionTextSizePercent =
        themeOverridesStore != null
            ? themeOverridesStore.getSuggestionTextSizePercent(themeId)
            : null;
    final Integer keyboardNameTextSizePercent =
        themeOverridesStore != null
            ? themeOverridesStore.getKeyboardNameTextSizePercent(themeId)
            : null;

    if (keyLabelTextSizePref != null) {
      keyLabelTextSizePref.setEnabled(!importInProgress);
      keyLabelTextSizePref.setValue(
          keyLabelTextSizePercent != null ? String.valueOf(keyLabelTextSizePercent) : "theme");
    }
    if (hintTextSizePref != null) {
      hintTextSizePref.setEnabled(!importInProgress);
      hintTextSizePref.setValue(
          hintTextSizePercent != null ? String.valueOf(hintTextSizePercent) : "key");
    }
    if (suggestionTextSizePref != null) {
      suggestionTextSizePref.setEnabled(!importInProgress);
      suggestionTextSizePref.setValue(
          suggestionTextSizePercent != null ? String.valueOf(suggestionTextSizePercent) : "key");
    }
    if (keyboardNameTextSizePref != null) {
      keyboardNameTextSizePref.setEnabled(!importInProgress);
      keyboardNameTextSizePref.setValue(
          keyboardNameTextSizePercent != null
              ? String.valueOf(keyboardNameTextSizePercent)
              : "key");
    }
    if (autoFitKeyLabelsPref != null) {
      autoFitKeyLabelsPref.setEnabled(!importInProgress);
      final boolean autoFitEnabled = autoFitKeyLabels == null || autoFitKeyLabels;
      autoFitKeyLabelsPref.setChecked(autoFitEnabled);
      if (keyLabelAutoFitMinSizePref != null) {
        keyLabelAutoFitMinSizePref.setEnabled(!importInProgress && autoFitEnabled);
        keyLabelAutoFitMinSizePref.setValue(
            String.valueOf(
                keyLabelAutoFitMinSizePercent != null ? keyLabelAutoFitMinSizePercent : 30));
      }
    }
    if (keyLabelAutoFitMinSizePref != null && autoFitKeyLabelsPref == null) {
      keyLabelAutoFitMinSizePref.setEnabled(!importInProgress);
      keyLabelAutoFitMinSizePref.setValue(
          String.valueOf(
              keyLabelAutoFitMinSizePercent != null ? keyLabelAutoFitMinSizePercent : 30));
    }
    if (ellipsizeKeyLabelsPref != null) {
      ellipsizeKeyLabelsPref.setEnabled(!importInProgress);
      ellipsizeKeyLabelsPref.setChecked(ellipsizeKeyLabels == null || ellipsizeKeyLabels);
    }
  }

  void dispose() {}
}
