package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.DemoKeyboardView;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;

final class KeyboardThemeCustomizationColorsSection {

  interface Host {
    @Nullable
    String getActiveThemeIdOrNull();

    boolean isAdded();

    void refreshState();

    void updateLivePreview();

    void updateResetEnabledStates(@NonNull String themeId);

    @Nullable
    DemoKeyboardView getLivePreviewKeyboardView();

    void attachColorPickerDialog(@NonNull EditTextPreference preference);
  }

  @NonNull private final Host host;
  @NonNull private final KeyboardWallpaperOverrideStore wallpaperStore;
  @Nullable private final KeyboardThemeUserOverridesStore themeOverridesStore;

  @NonNull private final KeyboardThemeCustomizationReadabilityController readabilityController;
  @NonNull private final KeyboardThemeCustomizationAutoPhotoThemeDialog autoPhotoThemeDialog;
  @NonNull private final KeyboardThemeCustomizationTokensController tokensController;
  @NonNull private final KeyboardThemeCustomizationManualColorsController manualColorsController;

  KeyboardThemeCustomizationColorsSection(
      @NonNull Host host,
      @NonNull KeyboardWallpaperOverrideStore wallpaperStore,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore) {
    this.host = host;
    this.wallpaperStore = wallpaperStore;
    this.themeOverridesStore = themeOverridesStore;
    this.readabilityController =
        new KeyboardThemeCustomizationReadabilityController(
            host, wallpaperStore, themeOverridesStore);
    this.autoPhotoThemeDialog =
        new KeyboardThemeCustomizationAutoPhotoThemeDialog(
            host, wallpaperStore, themeOverridesStore);
    this.tokensController =
        new KeyboardThemeCustomizationTokensController(host, themeOverridesStore);
    this.manualColorsController =
        new KeyboardThemeCustomizationManualColorsController(host, themeOverridesStore);
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceScreen screen) {
    final PreferenceCategory colors = new PreferenceCategory(context);
    colors.setKey("section:colors");
    colors.setTitle(R.string.keyboard_theme_appearance_colors_title);
    screen.addPreference(colors);

    readabilityController.addPreferences(context, colors);
    autoPhotoThemeDialog.addPreferences(context, colors);
    tokensController.addPreferences(context, colors);
    manualColorsController.addPreferences(context, colors);
  }

  boolean hasAnyColorOverride(@NonNull String themeId) {
    return tokensController.hasAnyTokenOverride(themeId)
        || manualColorsController.hasAnyColorOverride(themeId);
  }

  void refreshState(@NonNull String themeId, boolean importInProgress) {
    readabilityController.refreshState(themeId, importInProgress);
    autoPhotoThemeDialog.refreshState(themeId, importInProgress);
    tokensController.refreshState(themeId);
    manualColorsController.refreshState(themeId);
  }

  void scheduleEnsureReadableUpdateIfEnabled(@NonNull String themeId) {
    readabilityController.scheduleUpdateIfEnabled(themeId);
  }

  void dispose() {
    readabilityController.dispose();
    autoPhotoThemeDialog.dispose();
  }
}
