package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.graphics.Color;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;

final class KeyboardThemeCustomizationTokensController {

  @NonNull private final KeyboardThemeCustomizationColorsSection.Host host;
  @Nullable private final KeyboardThemeUserOverridesStore themeOverridesStore;

  @Nullable private EditTextPreference primaryTextColorPref;
  @Nullable private EditTextPreference secondaryTextColorPref;
  @Nullable private EditTextPreference accentColorPref;
  @Nullable private EditTextPreference keySurfaceColorPref;
  @Nullable private EditTextPreference backgroundColorPref;

  KeyboardThemeCustomizationTokensController(
      @NonNull KeyboardThemeCustomizationColorsSection.Host host,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore) {
    this.host = host;
    this.themeOverridesStore = themeOverridesStore;
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceCategory colors) {
    final PreferenceCategory tokens = new PreferenceCategory(context);
    tokens.setTitle(R.string.keyboard_theme_appearance_tokens_title);
    colors.addPreference(tokens);

    final Preference info = new Preference(context);
    info.setSelectable(false);
    info.setTitle(R.string.keyboard_theme_appearance_tokens_info_title);
    info.setSummary(R.string.keyboard_theme_appearance_tokens_info_summary);
    tokens.addPreference(info);

    primaryTextColorPref =
        createTokenColorPreference(
            context,
            "keyboard_theme_token_primary_text_color",
            R.string.keyboard_theme_appearance_token_primary_text_color_title,
            (themeId, argb) -> themeOverridesStore.setTokenPrimaryTextColor(themeId, argb),
            themeId -> themeOverridesStore.clearTokenPrimaryTextColor(themeId));
    tokens.addPreference(primaryTextColorPref);
    host.attachColorPickerDialog(primaryTextColorPref);

    secondaryTextColorPref =
        createTokenColorPreference(
            context,
            "keyboard_theme_token_secondary_text_color",
            R.string.keyboard_theme_appearance_token_secondary_text_color_title,
            (themeId, argb) -> themeOverridesStore.setTokenSecondaryTextColor(themeId, argb),
            themeId -> themeOverridesStore.clearTokenSecondaryTextColor(themeId));
    tokens.addPreference(secondaryTextColorPref);
    host.attachColorPickerDialog(secondaryTextColorPref);

    accentColorPref =
        createTokenColorPreference(
            context,
            "keyboard_theme_token_accent_color",
            R.string.keyboard_theme_appearance_token_accent_color_title,
            (themeId, argb) -> themeOverridesStore.setTokenAccentColor(themeId, argb),
            themeId -> themeOverridesStore.clearTokenAccentColor(themeId));
    tokens.addPreference(accentColorPref);
    host.attachColorPickerDialog(accentColorPref);

    keySurfaceColorPref =
        createTokenColorPreference(
            context,
            "keyboard_theme_token_key_surface_color",
            R.string.keyboard_theme_appearance_token_key_surface_color_title,
            (themeId, argb) -> themeOverridesStore.setTokenKeySurfaceColor(themeId, argb),
            themeId -> themeOverridesStore.clearTokenKeySurfaceColor(themeId));
    tokens.addPreference(keySurfaceColorPref);
    host.attachColorPickerDialog(keySurfaceColorPref);

    backgroundColorPref =
        createTokenColorPreference(
            context,
            "keyboard_theme_token_background_color",
            R.string.keyboard_theme_appearance_token_background_color_title,
            (themeId, argb) -> themeOverridesStore.setTokenBackgroundColor(themeId, argb),
            themeId -> themeOverridesStore.clearTokenBackgroundColor(themeId));
    tokens.addPreference(backgroundColorPref);
    host.attachColorPickerDialog(backgroundColorPref);
  }

  boolean hasAnyTokenOverride(@NonNull String themeId) {
    final KeyboardThemeUserOverridesStore store = themeOverridesStore;
    if (store == null) return false;
    return store.getTokenPrimaryTextColor(themeId) != null
        || store.getTokenSecondaryTextColor(themeId) != null
        || store.getTokenAccentColor(themeId) != null
        || store.getTokenKeySurfaceColor(themeId) != null
        || store.getTokenBackgroundColor(themeId) != null;
  }

  void refreshState(@NonNull String themeId) {
    final KeyboardThemeUserOverridesStore store = themeOverridesStore;
    final boolean ensureReadableEnabled =
        store != null && store.isEnsureReadableTextEnabled(themeId);

    final Integer primaryText = store != null ? store.getTokenPrimaryTextColor(themeId) : null;
    final Integer secondaryText = store != null ? store.getTokenSecondaryTextColor(themeId) : null;
    final Integer accent = store != null ? store.getTokenAccentColor(themeId) : null;
    final Integer keySurface = store != null ? store.getTokenKeySurfaceColor(themeId) : null;
    final Integer background = store != null ? store.getTokenBackgroundColor(themeId) : null;

    final boolean tokenTextEnabled = !ensureReadableEnabled;
    if (primaryTextColorPref != null) primaryTextColorPref.setEnabled(tokenTextEnabled);
    if (secondaryTextColorPref != null) secondaryTextColorPref.setEnabled(tokenTextEnabled);
    if (accentColorPref != null) accentColorPref.setEnabled(tokenTextEnabled);

    if (primaryTextColorPref != null) {
      primaryTextColorPref.setText(
          primaryText != null
              ? KeyboardThemeCustomizationColorUiUtil.formatColor(primaryText)
              : "");
      primaryTextColorPref.setSummary(
          primaryText != null
              ? KeyboardThemeCustomizationColorUiUtil.formatColor(primaryText)
              : KeyboardThemeCustomizationColorUiUtil.contextString(
                  primaryTextColorPref, R.string.keyboard_theme_appearance_color_default_summary));
      KeyboardThemeCustomizationColorUiUtil.setColorIcon(primaryTextColorPref, primaryText);
    }

    if (secondaryTextColorPref != null) {
      secondaryTextColorPref.setText(
          secondaryText != null
              ? KeyboardThemeCustomizationColorUiUtil.formatColor(secondaryText)
              : "");
      secondaryTextColorPref.setSummary(
          secondaryText != null
              ? KeyboardThemeCustomizationColorUiUtil.formatColor(secondaryText)
              : KeyboardThemeCustomizationColorUiUtil.contextString(
                  secondaryTextColorPref,
                  R.string.keyboard_theme_appearance_color_default_summary));
      KeyboardThemeCustomizationColorUiUtil.setColorIcon(secondaryTextColorPref, secondaryText);
    }

    if (accentColorPref != null) {
      accentColorPref.setText(
          accent != null ? KeyboardThemeCustomizationColorUiUtil.formatColor(accent) : "");
      accentColorPref.setSummary(
          accent != null
              ? KeyboardThemeCustomizationColorUiUtil.formatColor(accent)
              : KeyboardThemeCustomizationColorUiUtil.contextString(
                  accentColorPref, R.string.keyboard_theme_appearance_color_default_summary));
      KeyboardThemeCustomizationColorUiUtil.setColorIcon(accentColorPref, accent);
    }

    if (keySurfaceColorPref != null) {
      keySurfaceColorPref.setText(
          keySurface != null ? KeyboardThemeCustomizationColorUiUtil.formatColor(keySurface) : "");
      keySurfaceColorPref.setSummary(
          keySurface != null
              ? KeyboardThemeCustomizationColorUiUtil.formatColor(keySurface)
              : KeyboardThemeCustomizationColorUiUtil.contextString(
                  keySurfaceColorPref, R.string.keyboard_theme_appearance_color_default_summary));
      KeyboardThemeCustomizationColorUiUtil.setColorIcon(keySurfaceColorPref, keySurface);
    }

    if (backgroundColorPref != null) {
      backgroundColorPref.setText(
          background != null ? KeyboardThemeCustomizationColorUiUtil.formatColor(background) : "");
      backgroundColorPref.setSummary(
          background != null
              ? KeyboardThemeCustomizationColorUiUtil.formatColor(background)
              : KeyboardThemeCustomizationColorUiUtil.contextString(
                  backgroundColorPref, R.string.keyboard_theme_appearance_color_default_summary));
      KeyboardThemeCustomizationColorUiUtil.setColorIcon(backgroundColorPref, background);
    }
  }

  @NonNull
  private EditTextPreference createTokenColorPreference(
      @NonNull Context context,
      @NonNull String key,
      int titleResId,
      @NonNull TokenColorSetter setter,
      @NonNull TokenClearer clearer) {
    final EditTextPreference pref = new EditTextPreference(context);
    pref.setKey(key);
    pref.setPersistent(false);
    pref.setTitle(titleResId);
    pref.setDialogTitle(titleResId);
    pref.setOnBindEditTextListener(
        edit -> {
          edit.setSingleLine(true);
          edit.setHint("#RRGGBB");
        });
    pref.setOnPreferenceChangeListener(
        (ignored, newValue) -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return false;
          final String raw = String.valueOf(newValue).trim();
          if (raw.isEmpty()) {
            clearer.clear(themeId);
            host.refreshState();
            return true;
          }
          try {
            setter.set(themeId, Color.parseColor(raw));
            host.refreshState();
            return true;
          } catch (IllegalArgumentException e) {
            Toast.makeText(
                    context,
                    R.string.keyboard_theme_appearance_invalid_color_toast,
                    Toast.LENGTH_SHORT)
                .show();
            return false;
          }
        });
    return pref;
  }

  private interface TokenColorSetter {
    void set(@NonNull String themeId, int argb);
  }

  private interface TokenClearer {
    void clear(@NonNull String themeId);
  }
}
