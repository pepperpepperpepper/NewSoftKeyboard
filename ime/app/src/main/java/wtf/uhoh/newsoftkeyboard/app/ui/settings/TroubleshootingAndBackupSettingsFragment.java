package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import net.evendanan.pixel.GeneralDialogController;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;

public class TroubleshootingAndBackupSettingsFragment extends PreferenceFragmentCompat {

  private static final String KEY_RESET_LOOK_AND_FEEL_SLIDERS = "reset_look_and_feel_sliders";

  private final PrefsBackupRestoreController mPrefsBackupRestoreController =
      new PrefsBackupRestoreController();
  @NonNull private final CompositeDisposable mDisposable = new CompositeDisposable();

  @Nullable private GeneralDialogController mDialogController;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.prefs_troubleshooting_backup_settings, rootKey);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    mDialogController =
        new GeneralDialogController(
            requireActivity(), R.style.Theme_NskAlertDialog, this::onSetupDialogRequired);

    bindDialogAction("nav:backup_prefs", R.id.backup_prefs);
    bindDialogAction("nav:restore_prefs", R.id.restore_prefs);

    bindOwnerShortcut(view, "nav:power_saving_settings");
    bindNav("nav:developer_tools", R.id.developerToolsFragment);
    bindNav("nav:logcat_viewer", R.id.logCatViewFragment);
    bindLookAndFeelSlidersReset();

    refreshEnabledStates();
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, R.string.settings_category_troubleshooting_backup);
  }

  @Override
  public void onResume() {
    super.onResume();
    refreshEnabledStates();
    scrollToRequestedPreferenceIfNeeded();
  }

  private void refreshEnabledStates() {
    final Preference allowRestart =
        findPreference(getString(R.string.settings_key_allow_suggestions_restart));
    if (allowRestart != null) {
      final boolean suggestionsEnabled =
          getPreferenceManager().getSharedPreferences().getBoolean("candidates_on", true);
      allowRestart.setEnabled(suggestionsEnabled);
    }
  }

  private void onSetupDialogRequired(
      Context context, AlertDialog.Builder builder, int optionId, @Nullable Object data) {
    if (mPrefsBackupRestoreController.onSetupDialogRequired(this, builder, optionId, data)) {
      return;
    }
    throw new IllegalArgumentException("Unsupported option-id " + optionId);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    final GeneralDialogController dialogController = mDialogController;
    if (dialogController == null) return;
    final Disposable operation =
        mPrefsBackupRestoreController.handleActivityResult(
            this, dialogController, requestCode, resultCode, data);
    if (operation != null) {
      mDisposable.add(operation);
    }
  }

  @Override
  public void onDestroyView() {
    final GeneralDialogController dialogController = mDialogController;
    if (dialogController != null) {
      dialogController.dismiss();
      mDialogController = null;
    }
    super.onDestroyView();
  }

  @Override
  public void onDestroy() {
    mDisposable.dispose();
    super.onDestroy();
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

  private void bindDialogAction(@NonNull String key, int optionId) {
    final Preference preference = findPreference(key);
    if (preference == null) return;
    preference.setOnPreferenceClickListener(
        ignored -> {
          final GeneralDialogController dialogController = mDialogController;
          if (dialogController != null) dialogController.showDialog(optionId);
          return true;
        });
  }

  private void bindLookAndFeelSlidersReset() {
    final Preference preference = findPreference(KEY_RESET_LOOK_AND_FEEL_SLIDERS);
    if (preference == null) return;
    preference.setOnPreferenceClickListener(
        ignored -> {
          confirmResetLookAndFeelSliders();
          return true;
        });
  }

  private void confirmResetLookAndFeelSliders() {
    new AlertDialog.Builder(requireContext(), R.style.Theme_NskAlertDialog)
        .setTitle(R.string.reset_look_and_feel_sliders_title)
        .setMessage(R.string.reset_look_and_feel_sliders_confirm_message)
        .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
        .setPositiveButton(
            R.string.reset_look_and_feel_sliders_confirm_action,
            (d, w) -> {
              d.dismiss();
              resetLookAndFeelSliders();
            })
        .show();
  }

  private void resetLookAndFeelSliders() {
    final var prefs = NskApplicationBase.prefs(requireContext());
    prefs
        .getInteger(
            R.string.settings_key_zoom_percent_in_portrait,
            R.integer.settings_default_zoom_percent_in_portrait)
        .set(getResources().getInteger(R.integer.settings_default_zoom_percent_in_portrait));
    prefs
        .getInteger(
            R.string.settings_key_zoom_percent_in_landscape,
            R.integer.settings_default_zoom_percent_in_landscape)
        .set(getResources().getInteger(R.integer.settings_default_zoom_percent_in_landscape));
    prefs
        .getInteger(
            R.string.settings_key_bottom_extra_padding_in_portrait,
            R.integer.settings_default_bottom_extra_padding_in_portrait)
        .set(
            getResources().getInteger(R.integer.settings_default_bottom_extra_padding_in_portrait));
    prefs
        .getInteger(
            R.string.settings_key_vibrate_on_key_press_duration_int,
            R.integer.settings_default_vibrate_on_key_press_duration_int)
        .set(
            getResources()
                .getInteger(R.integer.settings_default_vibrate_on_key_press_duration_int));
    prefs
        .getInteger(
            R.string.settings_key_system_vibration_fallback_duration_int,
            R.integer.settings_default_system_vibration_fallback_duration_int)
        .set(
            getResources()
                .getInteger(R.integer.settings_default_system_vibration_fallback_duration_int));
    prefs
        .getInteger(
            R.string.settings_key_custom_sound_volume,
            R.integer.settings_default_custom_volume_level)
        .set(getResources().getInteger(R.integer.settings_default_custom_volume_level));

    Toast.makeText(
            requireContext(), R.string.reset_look_and_feel_sliders_done_toast, Toast.LENGTH_SHORT)
        .show();
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
