/*
 * Copyright (c) 2013 Menny Even-Danan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.TwoStatePreference;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;

public class EffectsSettingsFragment extends PreferenceFragmentCompat {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.prefs_effects_prefs);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    final Preference powerSaving = findPreference("nav:power_saving_settings");
    if (powerSaving != null) {
      AppearanceOwnerNavigation.bindPreference(powerSaving, view, "nav:power_saving_settings");
    }

    final Preference nightMode = findPreference("nav:night_mode_settings");
    if (nightMode != null) {
      AppearanceOwnerNavigation.bindPreference(nightMode, view, "nav:night_mode_settings");
    }

    if (Build.VERSION.SDK_INT >= 35) {
      Preference navBarPref = findPreference(getText(R.string.settings_key_colorize_nav_bar));
      navBarPref.setVisible(false);
      navBarPref.setSelectable(false);
    }

    bindSystemVibrationFallbackPref();
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, getString(R.string.effects_group));
  }

  private void bindSystemVibrationFallbackPref() {
    final Preference vibrationDuration =
        findPreference(getString(R.string.settings_key_vibrate_on_key_press_duration_int));
    final Preference fallbackDuration =
        findPreference(getString(R.string.settings_key_system_vibration_fallback_duration_int));
    final Preference longPress =
        findPreference(getString(R.string.settings_key_vibrate_on_long_press));

    final boolean fallbackBindable =
        fallbackDuration != null && Build.VERSION.SDK_INT >= 29 && vibrationDuration != null;
    if (fallbackDuration != null && !fallbackBindable) {
      fallbackDuration.setVisible(false);
      fallbackDuration.setEnabled(false);
      fallbackDuration.setSelectable(false);
    }
    if (vibrationDuration == null) return;

    final Preference fallbackTarget = fallbackBindable ? fallbackDuration : null;
    final int initialDuration =
        NskApplicationBase.prefs(requireContext())
            .getInteger(
                R.string.settings_key_vibrate_on_key_press_duration_int,
                R.integer.settings_default_vibrate_on_key_press_duration_int)
            .get();
    applyVibrationDependentState(initialDuration, fallbackTarget, longPress);

    vibrationDuration.setOnPreferenceChangeListener(
        (preference, newValue) -> {
          if (newValue instanceof Integer i) {
            applyVibrationDependentState(i, fallbackTarget, longPress);
          }
          return true;
        });
  }

  private void applyVibrationDependentState(
      int sliderValue, @Nullable Preference fallbackDuration, @Nullable Preference longPress) {
    if (fallbackDuration != null) {
      final boolean systemMode = sliderValue < 0;
      fallbackDuration.setVisible(systemMode);
      fallbackDuration.setEnabled(systemMode);
      fallbackDuration.setSelectable(systemMode);
    }
    if (longPress instanceof TwoStatePreference twoState) {
      // Slider at "Off" makes long-press a no-op (the long-press subscriber follows the slider).
      // Disable the checkbox so that's discoverable instead of silently ignored.
      final boolean active = sliderValue != 0;
      twoState.setEnabled(active);
      if (active) {
        twoState.setSummaryOn(getString(R.string.vibrate_on_long_press_summary_on));
        twoState.setSummaryOff("");
      } else {
        final String reason = getString(R.string.vibrate_on_long_press_summary_disabled);
        twoState.setSummaryOn(reason);
        twoState.setSummaryOff(reason);
      }
    }
  }
}
