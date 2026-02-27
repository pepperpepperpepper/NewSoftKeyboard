package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceCategory;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;

final class KeyboardThemeCustomizationTypographyPreferencesUi {

  @NonNull private final KeyboardThemeCustomizationTypographyTokensUi tokensUi;
  @NonNull private final KeyboardThemeCustomizationTypographyOverridesUi overridesUi;

  KeyboardThemeCustomizationTypographyPreferencesUi(
      @NonNull KeyboardThemeCustomizationTypographySection.Host host,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore,
      @NonNull KeyboardThemeCustomizationCustomKeyFontController customKeyFontController) {
    tokensUi =
        new KeyboardThemeCustomizationTypographyTokensUi(
            host, themeOverridesStore, customKeyFontController);
    overridesUi =
        new KeyboardThemeCustomizationTypographyOverridesUi(
            host, themeOverridesStore, customKeyFontController);
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceCategory typography) {
    tokensUi.addPreferences(context, typography);
    overridesUi.addPreferences(context, typography);
  }

  void refreshState(@NonNull String themeId, boolean importInProgress) {
    tokensUi.refreshState(themeId, importInProgress);
    overridesUi.refreshState(themeId, importInProgress);
  }

  void dispose() {
    tokensUi.dispose();
    overridesUi.dispose();
  }
}
