package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeUserOverridesStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardWallpaperOverrideStore;

final class KeyboardThemeCustomizationResetSection {

  interface Host {
    @Nullable
    String getActiveThemeIdOrNull();

    void refreshState();

    void updateLivePreview();
  }

  @NonNull private final Host host;
  @NonNull private final KeyboardWallpaperOverrideStore wallpaperStore;
  @NonNull private final KeyboardThemeUserOverridesStore themeOverridesStore;

  @Nullable private Preference resetPresetPref;
  @Nullable private Preference resetAllAppearancePref;

  KeyboardThemeCustomizationResetSection(
      @NonNull Host host,
      @NonNull KeyboardWallpaperOverrideStore wallpaperStore,
      @NonNull KeyboardThemeUserOverridesStore themeOverridesStore) {
    this.host = host;
    this.wallpaperStore = wallpaperStore;
    this.themeOverridesStore = themeOverridesStore;
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceScreen screen) {
    final PreferenceCategory reset = new PreferenceCategory(context);
    reset.setKey("section:reset");
    reset.setTitle(R.string.keyboard_theme_appearance_reset_section_title);
    screen.addPreference(reset);

    resetPresetPref = new Preference(context);
    resetPresetPref.setKey("keyboard_theme_appearance_reset_preset");
    resetPresetPref.setTitle(R.string.keyboard_theme_appearance_reset_preset_title);
    resetPresetPref.setSummary(R.string.keyboard_theme_appearance_reset_preset_summary);
    resetPresetPref.setOnPreferenceClickListener(
        ignored -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return true;
          new AlertDialog.Builder(context)
              .setTitle(R.string.keyboard_theme_appearance_reset_preset_dialog_title)
              .setMessage(R.string.keyboard_theme_appearance_reset_preset_dialog_message)
              .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
              .setPositiveButton(
                  R.string.keyboard_theme_appearance_reset_preset_action,
                  (dialog, which) -> {
                    dialog.dismiss();
                    wallpaperStore.clear(themeId);
                    themeOverridesStore.clearAllOverrides(themeId);
                    Toast.makeText(
                            context,
                            R.string.keyboard_theme_appearance_reset_preset_toast,
                            Toast.LENGTH_SHORT)
                        .show();
                    host.refreshState();
                    host.updateLivePreview();
                  })
              .show();
          return true;
        });
    reset.addPreference(resetPresetPref);

    resetAllAppearancePref = new Preference(context);
    resetAllAppearancePref.setKey("keyboard_theme_appearance_reset_all");
    resetAllAppearancePref.setTitle(R.string.keyboard_theme_appearance_reset_all_title);
    resetAllAppearancePref.setSummary(R.string.keyboard_theme_appearance_reset_all_summary);
    resetAllAppearancePref.setOnPreferenceClickListener(
        ignored -> {
          final String themeId = host.getActiveThemeIdOrNull();
          if (themeId == null) return true;
          themeOverridesStore.clearAllOverrides(themeId);
          Toast.makeText(
                  context, R.string.keyboard_theme_appearance_reset_all_toast, Toast.LENGTH_SHORT)
              .show();
          host.refreshState();
          return true;
        });
    reset.addPreference(resetAllAppearancePref);
  }

  void refreshState(
      boolean importInProgress, boolean hasAnyWallpaperOverride, boolean hasAnyAppearanceOverride) {
    if (resetPresetPref != null) {
      resetPresetPref.setEnabled(
          !importInProgress && (hasAnyWallpaperOverride || hasAnyAppearanceOverride));
    }

    if (resetAllAppearancePref != null) {
      resetAllAppearancePref.setEnabled(hasAnyAppearanceOverride);
    }
  }

  void dispose() {
    resetPresetPref = null;
    resetAllAppearancePref = null;
  }
}
