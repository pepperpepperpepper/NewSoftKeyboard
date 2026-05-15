package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.TwoStatePreference;
import io.reactivex.disposables.CompositeDisposable;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

public class LookAndFeelSettingsFragment extends PreferenceFragmentCompat {

  private static final String KEY_THEME_SELECTOR = "nav:theme_selector";
  private static final String KEY_KEYBOARD_WALLPAPER = "nav:keyboard_theme_wallpaper_customization";
  private static final String KEY_NIGHT_MODE_SETTINGS = "nav:night_mode_settings";

  private static final String KEY_TOOLBAR_TOP_ROW = "nav:toolbar_top_row_selector";
  private static final String KEY_TOOLBAR_SWIPE_ROW = "nav:toolbar_swipe_row_selector";
  private static final String KEY_TOOLBAR_BOTTOM_ROW = "nav:toolbar_bottom_row_selector";
  private static final String KEY_TOOLBAR_ROW_MODES = "nav:toolbar_input_field_modes";

  @Nullable private Preference applyRemoteAppColorsPref;
  @Nullable private Preference customKeypressSoundPref;
  @Nullable private ActivityResultLauncher<String[]> customSoundPickerLauncher;
  private final CompositeDisposable mViewDisposables = new CompositeDisposable();

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    customSoundPickerLauncher =
        registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::onCustomSoundPicked);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.prefs_look_and_feel_settings, rootKey);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    bindOwnerShortcut(view, KEY_THEME_SELECTOR);
    bindOwnerShortcut(view, KEY_KEYBOARD_WALLPAPER);
    bindOwnerShortcut(view, KEY_NIGHT_MODE_SETTINGS);
    bindNav(KEY_TOOLBAR_TOP_ROW, R.id.topRowAddOnBrowserFragment);
    bindNav(KEY_TOOLBAR_SWIPE_ROW, R.id.extensionAddOnBrowserFragment);
    bindNav(KEY_TOOLBAR_BOTTOM_ROW, R.id.bottomRowAddOnBrowserFragment);

    applyRemoteAppColorsPref =
        findPreference(getString(R.string.settings_key_apply_remote_app_colors));
    if (applyRemoteAppColorsPref != null) {
      AppearanceOwnerNavigation.bindPreference(
          applyRemoteAppColorsPref, view, getString(R.string.settings_key_apply_remote_app_colors));
    }

    final Preference rowModes = findPreference(KEY_TOOLBAR_ROW_MODES);
    if (rowModes != null) {
      rowModes.setOnPreferenceClickListener(
          ignored -> {
            showRowModesDialog();
            return true;
          });
    }

    applySdkVisibilityRules();
    bindSystemVibrationFallbackPref();
    bindCustomKeypressSoundPref();
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, R.string.settings_category_look_and_feel);
    updateToolbarSummaries();
    updateThemeOverlaySummaries();
  }

  @Override
  public void onResume() {
    super.onResume();
    updateToolbarSummaries();
    updateThemeOverlaySummaries();
    refreshCustomKeypressSoundSummary();
    scrollToRequestedPreferenceIfNeeded();
  }

  private void updateToolbarSummaries() {
    final Preference topRow = findPreference(KEY_TOOLBAR_TOP_ROW);
    if (topRow != null) {
      topRow.setSummary(
          getString(
              R.string.top_generic_row_summary,
              NskApplicationBase.getTopRowFactory(requireContext()).getEnabledAddOn().getName()));
    }

    final Preference swipeRow = findPreference(KEY_TOOLBAR_SWIPE_ROW);
    if (swipeRow != null) {
      swipeRow.setSummary(
          getString(
              R.string.extension_generic_keyboard_summary,
              NskApplicationBase.getKeyboardExtensionFactory(requireContext())
                  .getEnabledAddOn()
                  .getName()));
    }

    final Preference bottomRow = findPreference(KEY_TOOLBAR_BOTTOM_ROW);
    if (bottomRow != null) {
      bottomRow.setSummary(
          getString(
              R.string.bottom_generic_row_summary,
              NskApplicationBase.getBottomRowFactory(requireContext())
                  .getEnabledAddOn()
                  .getName()));
    }
  }

  private void updateThemeOverlaySummaries() {
    final Preference preference = applyRemoteAppColorsPref;
    if (preference == null) return;

    final boolean enabled =
        NskApplicationBase.prefs(requireContext())
            .getBoolean(
                R.string.settings_key_apply_remote_app_colors,
                R.bool.settings_default_apply_remote_app_colors)
            .get();
    preference.setSummary(
        enabled ? R.string.apply_overlay_summary_on : R.string.apply_overlay_summary_off);
  }

  private void applySdkVisibilityRules() {
    if (Build.VERSION.SDK_INT >= 35) {
      hidePref(findPreference(getString(R.string.settings_key_colorize_nav_bar)));
      hidePref(findPreference(getString(R.string.settings_key_bottom_extra_padding_in_portrait)));
    }
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
      hidePref(fallbackDuration);
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
    // Already set — let the user pick a different file or clear back to default.
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
              requireContext(),
              R.string.custom_keypress_sound_load_failed,
              Toast.LENGTH_SHORT)
          .show();
    }
  }

  private void onCustomSoundPicked(@Nullable Uri uri) {
    if (uri == null) return; // user cancelled
    final Context context = requireContext();
    try {
      context
          .getContentResolver()
          .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
    } catch (Throwable t) {
      // Some providers don't support persistable permissions — fall back to a one-shot grant.
      // We'll still try to load it; if loading fails later, the IME falls back to default.
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
    pref.setSummary(
        getString(R.string.custom_keypress_sound_summary_set, displayName));
  }

  @NonNull
  private String readCustomSoundUri() {
    final String value =
        NskApplicationBase.prefs(requireContext())
            .getString(R.string.settings_key_custom_keypress_sound_uri, R.string.settings_default_empty)
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

  private void showRowModesDialog() {
    final Context context = requireContext();
    final SharedPreferences sharedPreferences = DirectBootAwareSharedPreferences.create(context);

    final boolean[] enableStateForRowModes =
        new boolean[] {
          sharedPreferences.getBoolean(
              Keyboard.PREF_KEY_ROW_MODE_ENABLED_PREFIX + Keyboard.KEYBOARD_ROW_MODE_IM, true),
          sharedPreferences.getBoolean(
              Keyboard.PREF_KEY_ROW_MODE_ENABLED_PREFIX + Keyboard.KEYBOARD_ROW_MODE_URL, true),
          sharedPreferences.getBoolean(
              Keyboard.PREF_KEY_ROW_MODE_ENABLED_PREFIX + Keyboard.KEYBOARD_ROW_MODE_EMAIL, true),
          sharedPreferences.getBoolean(
              Keyboard.PREF_KEY_ROW_MODE_ENABLED_PREFIX + Keyboard.KEYBOARD_ROW_MODE_PASSWORD, true)
        };

    final AlertDialog.Builder builder =
        new AlertDialog.Builder(requireContext(), R.style.Theme_NskAlertDialog);
    builder.setIcon(R.drawable.ic_settings_language);
    builder.setTitle(R.string.supported_keyboard_row_modes_title);
    builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());
    builder.setPositiveButton(
        R.string.label_done_key,
        (dialog, which) -> {
          dialog.dismiss();
          SharedPreferences.Editor edit = sharedPreferences.edit();
          for (int modeIndex = 0; modeIndex < enableStateForRowModes.length; modeIndex++) {
            edit.putBoolean(
                Keyboard.PREF_KEY_ROW_MODE_ENABLED_PREFIX + (modeIndex + 2),
                enableStateForRowModes[modeIndex]);
          }
          edit.apply();
        });

    builder.setMultiChoiceItems(
        R.array.all_input_field_modes,
        enableStateForRowModes,
        (dialog, which, isChecked) -> enableStateForRowModes[which] = isChecked);
    builder.setCancelable(true);
    builder.show();
  }

  private void bindNav(@NonNull String key, int destinationId) {
    final Preference preference = findPreference(key);
    if (preference == null) return;
    preference.setOnPreferenceClickListener(
        ignored -> {
          Navigation.findNavController(requireView()).navigate(destinationId);
          return true;
        });
  }

  private void bindOwnerShortcut(@NonNull View view, @NonNull String key) {
    final Preference preference = findPreference(key);
    if (preference == null) return;
    AppearanceOwnerNavigation.bindPreference(preference, view, key);
  }

  private static void hidePref(@Nullable Preference preference) {
    if (preference == null) return;
    preference.setVisible(false);
    preference.setEnabled(false);
    preference.setSelectable(false);
  }

  private void scrollToRequestedPreferenceIfNeeded() {
    final Bundle args = getArguments();
    if (args == null) return;
    final String key = args.getString(SettingsSearchFragment.ARG_SCROLL_TO_PREFERENCE_KEY);
    if (TextUtils.isEmpty(key)) return;
    scrollToPreference(key);
    args.remove(SettingsSearchFragment.ARG_SCROLL_TO_PREFERENCE_KEY);
  }
}
