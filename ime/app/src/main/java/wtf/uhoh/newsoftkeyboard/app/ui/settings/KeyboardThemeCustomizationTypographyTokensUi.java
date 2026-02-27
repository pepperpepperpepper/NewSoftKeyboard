package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.KeyboardThemeCustomizationCustomKeyFontController.CustomFontTarget;

final class KeyboardThemeCustomizationTypographyTokensUi {

  @NonNull private final KeyboardThemeCustomizationTypographySection.Host host;
  @Nullable private final KeyboardThemeUserOverridesStore themeOverridesStore;
  @NonNull private final KeyboardThemeCustomizationCustomKeyFontController customKeyFontController;

  @Nullable private ListPreference tokenSecondaryFontFamilyPref;
  @Nullable private ListPreference tokenSecondaryFontStylePref;
  @Nullable private ListPreference tokenSecondaryTextSizePref;

  KeyboardThemeCustomizationTypographyTokensUi(
      @NonNull KeyboardThemeCustomizationTypographySection.Host host,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore,
      @NonNull KeyboardThemeCustomizationCustomKeyFontController customKeyFontController) {
    this.host = host;
    this.themeOverridesStore = themeOverridesStore;
    this.customKeyFontController = customKeyFontController;
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceCategory typography) {
    final PreferenceCategory typographyTokens = new PreferenceCategory(context);
    typographyTokens.setTitle(R.string.keyboard_theme_appearance_typography_tokens_title);
    typography.addPreference(typographyTokens);

    tokenSecondaryFontFamilyPref = new ListPreference(context);
    tokenSecondaryFontFamilyPref.setKey("keyboard_theme_token_secondary_font_family");
    tokenSecondaryFontFamilyPref.setPersistent(false);
    tokenSecondaryFontFamilyPref.setTitle(
        R.string.keyboard_theme_appearance_token_secondary_font_family_title);
    final CharSequence tokenSecondaryFontFamilySummaryBase =
        context.getText(R.string.keyboard_theme_appearance_token_secondary_font_family_summary);
    tokenSecondaryFontFamilyPref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return tokenSecondaryFontFamilySummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return tokenSecondaryFontFamilySummaryBase;
          return tokenSecondaryFontFamilySummaryBase + "\n" + entry;
        });
    tokenSecondaryFontFamilyPref.setEntries(
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_appearance_font_family_follow_key_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_default_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_sans_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_serif_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_monospace_entry),
          context.getString(R.string.keyboard_theme_appearance_font_family_custom_entry)
        });
    tokenSecondaryFontFamilyPref.setEntryValues(
        new CharSequence[] {
          "key",
          "default",
          "sans",
          "serif",
          "monospace",
          KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM
        });
    tokenSecondaryFontFamilyPref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.equals("key")) {
            themeOverridesStore.clearTokenSecondaryFontFamily(themeId);
            host.refreshState();
            return true;
          }

          if (raw.equals(KeyboardThemeUserOverridesStore.KEY_FONT_FAMILY_CUSTOM)
              && !customKeyFontController.ensureCustomKeyFontAvailableOrStartPick(
                  context, themeId, CustomFontTarget.TOKEN_SECONDARY_FONT_FAMILY)) {
            return false;
          }

          themeOverridesStore.setTokenSecondaryFontFamily(themeId, raw);
          host.refreshState();
          return true;
        });
    typographyTokens.addPreference(tokenSecondaryFontFamilyPref);

    tokenSecondaryFontStylePref = new ListPreference(context);
    tokenSecondaryFontStylePref.setKey("keyboard_theme_token_secondary_font_style");
    tokenSecondaryFontStylePref.setPersistent(false);
    tokenSecondaryFontStylePref.setTitle(
        R.string.keyboard_theme_appearance_token_secondary_font_style_title);
    final CharSequence tokenSecondaryFontStyleSummaryBase =
        context.getText(R.string.keyboard_theme_appearance_token_secondary_font_style_summary);
    tokenSecondaryFontStylePref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return tokenSecondaryFontStyleSummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return tokenSecondaryFontStyleSummaryBase;
          return tokenSecondaryFontStyleSummaryBase + "\n" + entry;
        });
    tokenSecondaryFontStylePref.setEntries(
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_appearance_font_style_follow_key_entry),
          context.getString(R.string.keyboard_theme_appearance_font_style_normal_entry),
          context.getString(R.string.keyboard_theme_appearance_font_style_bold_entry),
          context.getString(R.string.keyboard_theme_appearance_font_style_italic_entry),
          context.getString(R.string.keyboard_theme_appearance_font_style_bold_italic_entry)
        });
    tokenSecondaryFontStylePref.setEntryValues(new CharSequence[] {"key", "0", "1", "2", "3"});
    tokenSecondaryFontStylePref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.equals("key")) {
            themeOverridesStore.clearTokenSecondaryFontStyle(themeId);
            host.refreshState();
            return true;
          }
          try {
            final int style = Integer.parseInt(raw);
            themeOverridesStore.setTokenSecondaryFontStyle(themeId, style);
            host.refreshState();
            return true;
          } catch (NumberFormatException e) {
            return false;
          }
        });
    typographyTokens.addPreference(tokenSecondaryFontStylePref);

    tokenSecondaryTextSizePref = new ListPreference(context);
    tokenSecondaryTextSizePref.setKey("keyboard_theme_token_secondary_text_size");
    tokenSecondaryTextSizePref.setPersistent(false);
    tokenSecondaryTextSizePref.setTitle(
        R.string.keyboard_theme_appearance_token_secondary_text_size_title);
    final CharSequence tokenSecondaryTextSizeSummaryBase =
        context.getText(R.string.keyboard_theme_appearance_token_secondary_text_size_summary);
    tokenSecondaryTextSizePref.setSummaryProvider(
        pref -> {
          if (!(pref instanceof ListPreference lp)) return tokenSecondaryTextSizeSummaryBase;
          final CharSequence entry = lp.getEntry();
          if (entry == null) return tokenSecondaryTextSizeSummaryBase;
          return tokenSecondaryTextSizeSummaryBase + "\n" + entry;
        });
    tokenSecondaryTextSizePref.setEntries(
        new CharSequence[] {
          context.getString(R.string.keyboard_theme_appearance_text_size_follow_key_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_theme_default_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_smaller_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_small_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_large_entry),
          context.getString(R.string.keyboard_theme_appearance_text_size_larger_entry)
        });
    tokenSecondaryTextSizePref.setEntryValues(
        new CharSequence[] {"key", "100", "80", "90", "110", "120"});
    tokenSecondaryTextSizePref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.equals("key")) {
            themeOverridesStore.clearTokenSecondaryTextSizePercent(themeId);
            host.refreshState();
            return true;
          }
          try {
            themeOverridesStore.setTokenSecondaryTextSizePercent(themeId, Integer.parseInt(raw));
            host.refreshState();
            return true;
          } catch (NumberFormatException e) {
            return false;
          }
        });
    typographyTokens.addPreference(tokenSecondaryTextSizePref);
  }

  void refreshState(@NonNull String themeId, boolean importInProgress) {
    final String tokenSecondaryFontFamily =
        themeOverridesStore != null
            ? themeOverridesStore.getTokenSecondaryFontFamily(themeId)
            : null;
    final Integer tokenSecondaryFontStyle =
        themeOverridesStore != null
            ? themeOverridesStore.getTokenSecondaryFontStyle(themeId)
            : null;
    final Integer tokenSecondaryTextSizePercent =
        themeOverridesStore != null
            ? themeOverridesStore.getTokenSecondaryTextSizePercent(themeId)
            : null;

    if (tokenSecondaryFontFamilyPref != null) {
      tokenSecondaryFontFamilyPref.setEnabled(!importInProgress);
      tokenSecondaryFontFamilyPref.setValue(
          tokenSecondaryFontFamily != null ? tokenSecondaryFontFamily : "key");
    }
    if (tokenSecondaryFontStylePref != null) {
      tokenSecondaryFontStylePref.setEnabled(!importInProgress);
      tokenSecondaryFontStylePref.setValue(
          tokenSecondaryFontStyle != null ? String.valueOf(tokenSecondaryFontStyle) : "key");
    }
    if (tokenSecondaryTextSizePref != null) {
      tokenSecondaryTextSizePref.setEnabled(!importInProgress);
      tokenSecondaryTextSizePref.setValue(
          tokenSecondaryTextSizePercent != null
              ? String.valueOf(tokenSecondaryTextSizePercent)
              : "key");
    }
  }

  void dispose() {}
}
