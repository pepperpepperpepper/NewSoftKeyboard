package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;

final class KeyboardThemeCustomizationTypographySection {

  interface Host {
    @Nullable
    String getActiveThemeIdOrNull();

    boolean isAdded();

    void refreshState();

    @Nullable
    ActivityResultLauncher<String[]> getPickKeyFontLauncher();
  }

  @NonNull private final Host host;
  @Nullable private final KeyboardThemeUserOverridesStore themeOverridesStore;
  @NonNull private final KeyboardThemeCustomizationCustomKeyFontController customKeyFontController;
  @NonNull private final KeyboardThemeCustomizationTypographyPreferencesUi preferencesUi;

  @Nullable private Preference resetTypographyPref;

  KeyboardThemeCustomizationTypographySection(
      @NonNull Host host, @Nullable KeyboardThemeUserOverridesStore themeOverridesStore) {
    this.host = host;
    this.themeOverridesStore = themeOverridesStore;
    this.customKeyFontController =
        new KeyboardThemeCustomizationCustomKeyFontController(host, themeOverridesStore);
    this.preferencesUi =
        new KeyboardThemeCustomizationTypographyPreferencesUi(
            host, themeOverridesStore, customKeyFontController);
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceScreen screen) {
    final PreferenceCategory typography = new PreferenceCategory(context);
    typography.setKey("section:typography");
    typography.setTitle(R.string.keyboard_theme_appearance_typography_title);
    screen.addPreference(typography);

    preferencesUi.addPreferences(context, typography);
    customKeyFontController.addPreferences(context, typography);

    resetTypographyPref = new Preference(context);
    resetTypographyPref.setTitle(R.string.keyboard_theme_appearance_reset_typography_title);
    resetTypographyPref.setSummary(R.string.keyboard_theme_appearance_reset_typography_summary);
    resetTypographyPref.setOnPreferenceClickListener(
        ignored -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null || themeOverridesStore == null) return true;
          themeOverridesStore.clearTypographyOverrides(themeId);
          Toast.makeText(
                  context,
                  R.string.keyboard_theme_appearance_reset_typography_toast,
                  Toast.LENGTH_SHORT)
              .show();
          host.refreshState();
          return true;
        });
    typography.addPreference(resetTypographyPref);
  }

  boolean isFontImportInProgress() {
    return customKeyFontController.isFontImportInProgress();
  }

  boolean hasAnyTypographyOverride(@NonNull String themeId) {
    final KeyboardThemeUserOverridesStore store = themeOverridesStore;
    if (store == null) return false;
    return store.getTokenSecondaryFontFamily(themeId) != null
        || store.getTokenSecondaryFontStyle(themeId) != null
        || store.getTokenSecondaryTextSizePercent(themeId) != null
        || store.getKeyFontFamily(themeId) != null
        || store.getKeyFontStyle(themeId) != null
        || store.getHintFontFamily(themeId) != null
        || store.getHintFontStyle(themeId) != null
        || store.getSuggestionFontFamily(themeId) != null
        || store.getSuggestionFontStyle(themeId) != null
        || store.getKeyboardNameFontFamily(themeId) != null
        || store.getKeyboardNameFontStyle(themeId) != null
        || store.hasCustomKeyFont(themeId)
        || store.getKeyLabelAutoFitEnabled(themeId) != null
        || store.getKeyLabelAutoFitMinSizePercent(themeId) != null
        || store.getKeyLabelEllipsizeEnabled(themeId) != null
        || store.getKeyLabelTextSizePercent(themeId) != null
        || store.getHintTextSizePercent(themeId) != null
        || store.getSuggestionTextSizePercent(themeId) != null
        || store.getKeyboardNameTextSizePercent(themeId) != null;
  }

  void refreshState(@NonNull String themeId, boolean importInProgress) {
    preferencesUi.refreshState(themeId, importInProgress);
    customKeyFontController.refreshState(themeId, importInProgress);
    if (resetTypographyPref != null) {
      resetTypographyPref.setEnabled(!importInProgress && hasAnyTypographyOverride(themeId));
    }
  }

  void onCustomKeyFontPicked(@NonNull Context context, @Nullable Uri uri) {
    customKeyFontController.onCustomKeyFontPicked(context, uri);
  }

  void dispose() {
    preferencesUi.dispose();
    customKeyFontController.dispose();
  }
}
