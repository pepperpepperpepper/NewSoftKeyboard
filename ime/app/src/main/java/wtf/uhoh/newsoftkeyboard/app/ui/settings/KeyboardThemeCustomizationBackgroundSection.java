package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceScreen;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;

final class KeyboardThemeCustomizationBackgroundSection {

  interface Host {
    @Nullable
    String getActiveThemeIdOrNull();

    boolean isAdded();

    void refreshState();

    void updateLivePreview();

    void scheduleEnsureReadableUpdateIfEnabled(@NonNull String themeId);

    void startTryNow();

    @Nullable
    ActivityResultLauncher<String[]> getPickWallpaperLauncher();

    void attachColorPickerDialog(@NonNull EditTextPreference preference);
  }

  @NonNull private final Host host;
  @NonNull private final KeyboardThemeCustomizationBackgroundPreferencesUi preferencesUi;
  @NonNull private final KeyboardThemeCustomizationWallpaperImportController importController;

  KeyboardThemeCustomizationBackgroundSection(
      @NonNull Host host,
      @NonNull KeyboardWallpaperOverrideStore wallpaperStore,
      @Nullable KeyboardThemeUserOverridesStore themeOverridesStore) {
    this.host = host;
    this.preferencesUi =
        new KeyboardThemeCustomizationBackgroundPreferencesUi(
            host, wallpaperStore, themeOverridesStore);
    this.importController =
        new KeyboardThemeCustomizationWallpaperImportController(host, wallpaperStore);
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceScreen screen) {
    preferencesUi.addPreferences(context, screen, this::applyWallpaperToAllThemes);
  }

  boolean isWallpaperImportInProgress() {
    return importController.isWallpaperImportInProgress();
  }

  void onPhotoPicked(@NonNull Context context, @Nullable Uri uri) {
    if (uri == null) return;
    final String themeId = host.getActiveThemeIdOrNull();
    if (themeId == null) return;

    final var metrics = context.getResources().getDisplayMetrics();
    final int maxSize =
        Math.max(2048, Math.min(4096, Math.max(metrics.widthPixels, metrics.heightPixels)));

    importController.disposeImport();
    preferencesUi.setWallpaperImportInProgressUi();
    importController.importWallpaperFromUri(
        context,
        themeId,
        uri,
        maxSize,
        error -> preferencesUi.showPickFailedDialog(context, error));
  }

  void refreshState(@NonNull String themeId, boolean importInProgress) {
    preferencesUi.refreshState(themeId, importInProgress);
  }

  boolean hasAnyWallpaperOverride(@NonNull String themeId) {
    return preferencesUi.hasAnyWallpaperOverride(themeId);
  }

  void dispose() {
    preferencesUi.dispose();
    importController.disposeImport();
  }

  private void applyWallpaperToAllThemes(@NonNull Context context, @NonNull String sourceThemeId) {
    importController.disposeImport();
    preferencesUi.setWallpaperImportInProgressUi();
    importController.applyWallpaperToAllThemes(context, sourceThemeId);
  }
}
