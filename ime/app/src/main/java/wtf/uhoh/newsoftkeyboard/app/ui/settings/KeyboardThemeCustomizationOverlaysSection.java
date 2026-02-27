package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.content.res.Configuration;
import android.os.PowerManager;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;

final class KeyboardThemeCustomizationOverlaysSection {

  interface Host {
    @NonNull
    View requireView();
  }

  @NonNull private final Host host;

  @Nullable private Preference adaptToAppColorsPref;
  @Nullable private Preference nightModeOverlayPref;
  @Nullable private Preference powerSavingOverlayPref;

  KeyboardThemeCustomizationOverlaysSection(@NonNull Host host) {
    this.host = host;
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceScreen screen) {
    final PreferenceCategory overlays = new PreferenceCategory(context);
    overlays.setKey("section:overlays");
    overlays.setTitle(R.string.keyboard_theme_appearance_overlays_title);
    screen.addPreference(overlays);

    adaptToAppColorsPref = new Preference(context);
    adaptToAppColorsPref.setTitle(R.string.apply_remote_app_colors_to_theme);
    adaptToAppColorsPref.setOnPreferenceClickListener(
        ignored -> {
          AppearanceOwnerNavigation.navigateToOwner(
              host.requireView(), context.getString(R.string.settings_key_apply_remote_app_colors));
          return true;
        });
    overlays.addPreference(adaptToAppColorsPref);

    nightModeOverlayPref = new Preference(context);
    nightModeOverlayPref.setTitle(R.string.night_mode_screen);
    nightModeOverlayPref.setOnPreferenceClickListener(
        ignored -> {
          AppearanceOwnerNavigation.navigateToOwner(
              host.requireView(),
              context.getString(R.string.settings_key_night_mode_theme_control));
          return true;
        });
    overlays.addPreference(nightModeOverlayPref);

    powerSavingOverlayPref = new Preference(context);
    powerSavingOverlayPref.setTitle(R.string.power_save_mode_screen);
    powerSavingOverlayPref.setOnPreferenceClickListener(
        ignored -> {
          AppearanceOwnerNavigation.navigateToOwner(
              host.requireView(),
              context.getString(R.string.settings_key_power_save_mode_theme_control));
          return true;
        });
    overlays.addPreference(powerSavingOverlayPref);
  }

  void refreshState() {
    final Context context = resolveContextOrNull();
    if (context == null) return;

    final var prefs = NskApplicationBase.prefs(context);

    if (adaptToAppColorsPref != null) {
      final boolean enabled =
          prefs
              .getBoolean(
                  R.string.settings_key_apply_remote_app_colors,
                  R.bool.settings_default_apply_remote_app_colors)
              .get();
      adaptToAppColorsPref.setSummary(
          enabled ? R.string.apply_overlay_summary_on : R.string.apply_overlay_summary_off);
    }

    if (nightModeOverlayPref != null) {
      final boolean enabled =
          prefs
              .getBoolean(
                  R.string.settings_key_night_mode_theme_control, R.bool.settings_default_false)
              .get();
      final boolean active;
      if (!enabled) {
        active = false;
      } else {
        final String mode =
            prefs
                .getString(
                    R.string.settings_key_night_mode, R.string.settings_default_night_mode_value)
                .get();
        if ("never".equals(mode)) {
          active = false;
        } else if ("always".equals(mode)) {
          active = true;
        } else {
          final boolean systemNight =
              (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                  == Configuration.UI_MODE_NIGHT_YES;
          active = systemNight;
        }
      }

      nightModeOverlayPref.setSummary(
          !enabled
              ? R.string.keyboard_theme_appearance_overlay_status_off
              : active
                  ? R.string.keyboard_theme_appearance_overlay_status_active
                  : R.string.keyboard_theme_appearance_overlay_status_inactive);
    }

    if (powerSavingOverlayPref != null) {
      final boolean enabled =
          prefs
              .getBoolean(
                  R.string.settings_key_power_save_mode_theme_control, R.bool.settings_default_true)
              .get();
      final PowerManager powerManager =
          (PowerManager) context.getSystemService(Context.POWER_SERVICE);
      final boolean powerSavingActive = powerManager != null && powerManager.isPowerSaveMode();
      final boolean overlayActive = enabled && powerSavingActive;
      powerSavingOverlayPref.setSummary(
          !enabled
              ? R.string.keyboard_theme_appearance_overlay_status_off
              : overlayActive
                  ? R.string.keyboard_theme_appearance_overlay_status_active
                  : R.string.keyboard_theme_appearance_overlay_status_inactive);
    }
  }

  void dispose() {
    adaptToAppColorsPref = null;
    nightModeOverlayPref = null;
    powerSavingOverlayPref = null;
  }

  @Nullable
  private Context resolveContextOrNull() {
    final Preference pref =
        adaptToAppColorsPref != null
            ? adaptToAppColorsPref
            : nightModeOverlayPref != null ? nightModeOverlayPref : powerSavingOverlayPref;
    return pref != null ? pref.getContext() : null;
  }
}
