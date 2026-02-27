package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceCategory;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;

final class KeyboardThemeCustomizationTypographyOverridesUi {

  @NonNull private final KeyboardThemeCustomizationTypographyFontOverridesUi fontOverridesUi;
  @NonNull private final KeyboardThemeCustomizationTypographySizeOverridesUi sizeOverridesUi;

  KeyboardThemeCustomizationTypographyOverridesUi(
      @NonNull KeyboardThemeCustomizationTypographySection.Host host,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore,
      @NonNull KeyboardThemeCustomizationCustomKeyFontController customKeyFontController) {
    fontOverridesUi =
        new KeyboardThemeCustomizationTypographyFontOverridesUi(
            host, themeOverridesStore, customKeyFontController);
    sizeOverridesUi =
        new KeyboardThemeCustomizationTypographySizeOverridesUi(host, themeOverridesStore);
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceCategory typography) {
    fontOverridesUi.addPreferences(context, typography);
    sizeOverridesUi.addPreferences(context, typography);
  }

  void refreshState(@NonNull String themeId, boolean importInProgress) {
    fontOverridesUi.refreshState(themeId, importInProgress);
    sizeOverridesUi.refreshState(themeId, importInProgress);
  }

  void dispose() {
    fontOverridesUi.dispose();
    sizeOverridesUi.dispose();
  }
}
