package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;

final class KeyboardThemeCustomizationManualColorsController {

  @NonNull private final KeyboardThemeCustomizationColorsSection.Host host;
  @Nullable private final KeyboardThemeUserOverridesStore themeOverridesStore;
  @NonNull private final KeyboardThemeCustomizationManualTextColorsUi textColorsUi;
  @NonNull private final KeyboardThemeCustomizationManualBackgroundColorsUi backgroundColorsUi;

  @Nullable private Preference resetColorsPref;

  KeyboardThemeCustomizationManualColorsController(
      @NonNull KeyboardThemeCustomizationColorsSection.Host host,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore) {
    this.host = host;
    this.themeOverridesStore = themeOverridesStore;
    this.textColorsUi = new KeyboardThemeCustomizationManualTextColorsUi(host, themeOverridesStore);
    this.backgroundColorsUi =
        new KeyboardThemeCustomizationManualBackgroundColorsUi(
            host, themeOverridesStore, this::updateResetColorsEnabledState);
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceCategory colors) {
    textColorsUi.addPreferences(context, colors);
    backgroundColorsUi.addPreferences(context, colors);

    resetColorsPref = new Preference(context);
    resetColorsPref.setTitle(R.string.keyboard_theme_appearance_reset_colors_title);
    resetColorsPref.setSummary(R.string.keyboard_theme_appearance_reset_colors_summary);
    resetColorsPref.setOnPreferenceClickListener(
        ignored -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return true;
          themeOverridesStore.clearColorOverrides(themeId);
          android.widget.Toast.makeText(
                  context,
                  R.string.keyboard_theme_appearance_reset_colors_toast,
                  android.widget.Toast.LENGTH_SHORT)
              .show();
          host.refreshState();
          return true;
        });
    colors.addPreference(resetColorsPref);
  }

  boolean hasAnyColorOverride(@NonNull String themeId) {
    final KeyboardThemeUserOverridesStore store = themeOverridesStore;
    if (store == null) return false;
    return store.getTokenPrimaryTextColor(themeId) != null
        || store.getTokenSecondaryTextColor(themeId) != null
        || store.getTokenAccentColor(themeId) != null
        || store.getTokenKeySurfaceColor(themeId) != null
        || store.getTokenBackgroundColor(themeId) != null
        || store.getKeyTextColor(themeId) != null
        || store.getSpecialKeyTextColor(themeId) != null
        || store.getModifierKeyTextColor(themeId) != null
        || store.getEnterKeyTextColor(themeId) != null
        || store.getSpacebarTextColor(themeId) != null
        || store.getHintTextColor(themeId) != null
        || store.getKeyBackgroundTint(themeId) != null
        || store.getSpecialKeyBackgroundTint(themeId) != null
        || store.getModifierKeyBackgroundTint(themeId) != null
        || store.getEnterKeyBackgroundTint(themeId) != null
        || store.getSpacebarBackgroundTint(themeId) != null
        || store.getKeyboardBackgroundTint(themeId) != null
        || store.getKeyBackgroundOpacityPercent(themeId) != null
        || store.getKeyboardBackgroundOpacityPercent(themeId) != null;
  }

  void refreshState(@NonNull String themeId) {
    textColorsUi.refreshState(themeId);
    backgroundColorsUi.refreshState(themeId);
    updateResetColorsEnabledState(themeId);
  }

  private void updateResetColorsEnabledState(@NonNull String themeId) {
    if (resetColorsPref != null) {
      resetColorsPref.setEnabled(hasAnyColorOverride(themeId));
    }
  }
}
