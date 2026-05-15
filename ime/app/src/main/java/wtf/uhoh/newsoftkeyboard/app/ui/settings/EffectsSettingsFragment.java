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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.TwoStatePreference;
import io.reactivex.disposables.CompositeDisposable;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;

public class EffectsSettingsFragment extends PreferenceFragmentCompat {

  private final CompositeDisposable mViewDisposables = new CompositeDisposable();

  @Nullable private Preference customKeypressSoundPref;
  @Nullable private ActivityResultLauncher<String[]> customSoundPickerLauncher;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    customSoundPickerLauncher =
        registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::onCustomSoundPicked);
  }

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
    bindCustomKeypressSoundPref();
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, getString(R.string.effects_group));
  }

  @Override
  public void onResume() {
    super.onResume();
    refreshCustomKeypressSoundSummary();
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

    mViewDisposables.add(VibrationPowerSavingHint.bind(requireContext(), vibrationDuration));
  }

  @Override
  public void onDestroyView() {
    mViewDisposables.clear();
    super.onDestroyView();
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

  private void bindCustomKeypressSoundPref() {
    customKeypressSoundPref =
        findPreference(getString(R.string.settings_key_custom_keypress_sound_uri));
    if (customKeypressSoundPref == null) return;
    customKeypressSoundPref.setOnPreferenceClickListener(
        ignored -> {
          onCustomKeypressSoundPrefClick();
          return true;
        });
    refreshCustomKeypressSoundSummary();
  }

  private void onCustomKeypressSoundPrefClick() {
    final String currentUri = readCustomSoundUri();
    if (TextUtils.isEmpty(currentUri)) {
      launchCustomSoundPicker();
      return;
    }
    new AlertDialog.Builder(requireContext(), R.style.Theme_NskAlertDialog)
        .setTitle(R.string.custom_keypress_sound_title)
        .setMessage(R.string.custom_keypress_sound_clear_message)
        .setPositiveButton(
            R.string.custom_keypress_sound_chooser_title,
            (dialog, which) -> {
              dialog.dismiss();
              launchCustomSoundPicker();
            })
        .setNeutralButton(
            R.string.custom_keypress_sound_clear_title,
            (dialog, which) -> {
              clearCustomKeypressSound(currentUri);
              dialog.dismiss();
            })
        .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel())
        .show();
  }

  private void launchCustomSoundPicker() {
    if (customSoundPickerLauncher == null) return;
    try {
      customSoundPickerLauncher.launch(new String[] {"audio/*"});
    } catch (Throwable t) {
      Toast.makeText(
              requireContext(), R.string.custom_keypress_sound_load_failed, Toast.LENGTH_SHORT)
          .show();
    }
  }

  private void onCustomSoundPicked(@Nullable Uri uri) {
    if (uri == null) return;
    final Context context = requireContext();
    try {
      context
          .getContentResolver()
          .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
    } catch (Throwable t) {
      // Some providers don't support persistable permissions — fall back to a one-shot grant.
    }
    final String previousUri = readCustomSoundUri();
    if (!TextUtils.isEmpty(previousUri) && !previousUri.equals(uri.toString())) {
      releasePersistableUri(Uri.parse(previousUri));
    }
    NskApplicationBase.prefs(context)
        .getString(R.string.settings_key_custom_keypress_sound_uri, R.string.settings_default_empty)
        .set(uri.toString());
    refreshCustomKeypressSoundSummary();
  }

  private void clearCustomKeypressSound(@NonNull String previousUriString) {
    releasePersistableUri(Uri.parse(previousUriString));
    NskApplicationBase.prefs(requireContext())
        .getString(R.string.settings_key_custom_keypress_sound_uri, R.string.settings_default_empty)
        .set("");
    refreshCustomKeypressSoundSummary();
  }

  private void releasePersistableUri(@NonNull Uri uri) {
    try {
      requireContext()
          .getContentResolver()
          .releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
    } catch (Throwable ignored) {
      // Best effort — the URI may have already lost its persistable grant.
    }
  }

  private void refreshCustomKeypressSoundSummary() {
    final Preference pref = customKeypressSoundPref;
    if (pref == null) return;
    final String uriString = readCustomSoundUri();
    if (TextUtils.isEmpty(uriString)) {
      pref.setSummary(R.string.custom_keypress_sound_summary_default);
      return;
    }
    final String displayName = queryDisplayName(Uri.parse(uriString));
    pref.setSummary(getString(R.string.custom_keypress_sound_summary_set, displayName));
  }

  @NonNull
  private String readCustomSoundUri() {
    final String value =
        NskApplicationBase.prefs(requireContext())
            .getString(
                R.string.settings_key_custom_keypress_sound_uri, R.string.settings_default_empty)
            .get();
    return value == null ? "" : value;
  }

  @NonNull
  private String queryDisplayName(@NonNull Uri uri) {
    try (Cursor cursor =
        requireContext()
            .getContentResolver()
            .query(uri, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null)) {
      if (cursor != null && cursor.moveToFirst()) {
        final int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        if (idx >= 0) {
          final String name = cursor.getString(idx);
          if (!TextUtils.isEmpty(name)) return name;
        }
      }
    } catch (Throwable ignored) {
      // Provider may have revoked access — fall through to last-segment fallback.
    }
    final String last = uri.getLastPathSegment();
    return last == null ? uri.toString() : last;
  }
}
