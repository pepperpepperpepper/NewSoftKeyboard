package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceFragmentCompat;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.api.KeyboardApiAuditLogStore;
import wtf.uhoh.newsoftkeyboard.app.api.KeyboardApiComponentController;
import wtf.uhoh.newsoftkeyboard.app.api.KeyboardApiPairingStore;
import wtf.uhoh.newsoftkeyboard.app.api.KeyboardApiPrefs;

public class ProgrammableApiSettingsFragment extends PreferenceFragmentCompat {

  private KeyboardApiPrefs mPrefs;
  private KeyboardApiPairingStore mPairingStore;
  private KeyboardApiAuditLogStore mAuditLogStore;
  private ProgrammableApiControllersUi mControllersUi;
  private ProgrammableApiAuditLogUi mAuditLogUi;
  private boolean mSuppressHighRiskEnableConfirmation;
  private boolean mSuppressClipboardCopyCutEnableConfirmation;
  private boolean mSuppressAutomationControllersEnableConfirmation;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.prefs_programmable_api);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    mPrefs = new KeyboardApiPrefs(requireContext());
    mPairingStore = new KeyboardApiPairingStore(requireContext());
    mAuditLogStore = new KeyboardApiAuditLogStore(requireContext());
    mControllersUi = new ProgrammableApiControllersUi(this, mPrefs, mPairingStore);
    mControllersUi.bindPreferences();
    mAuditLogUi = new ProgrammableApiAuditLogUi(this, mAuditLogStore);
    mAuditLogUi.bindPreferences();

    final CheckBoxPreference enabledPref =
        findPreference(getString(R.string.settings_key_keyboard_api_enabled));
    if (enabledPref != null) {
      enabledPref.setOnPreferenceChangeListener(
          (p, newValue) -> {
            if (!(newValue instanceof Boolean)) return false;
            KeyboardApiComponentController.setProviderEnabled(requireContext(), (Boolean) newValue);
            return true;
          });
      KeyboardApiComponentController.setProviderEnabled(requireContext(), enabledPref.isChecked());
    }

    final CheckBoxPreference highRiskPref =
        findPreference(getString(R.string.settings_key_keyboard_api_high_risk_actions_enabled));
    if (highRiskPref != null) {
      highRiskPref.setOnPreferenceChangeListener(
          (p, newValue) -> {
            if (!(newValue instanceof Boolean)) return false;
            final boolean enableHighRisk = (Boolean) newValue;
            if (!enableHighRisk) return true;

            if (mSuppressHighRiskEnableConfirmation) {
              mSuppressHighRiskEnableConfirmation = false;
              return true;
            }

            new AlertDialog.Builder(requireContext())
                .setTitle(R.string.keyboard_api_high_risk_enabled_title)
                .setMessage(R.string.keyboard_api_high_risk_enabled_confirm_message)
                .setPositiveButton(
                    android.R.string.ok,
                    (d, which) -> {
                      mSuppressHighRiskEnableConfirmation = true;
                      highRiskPref.setChecked(true);
                    })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
            return false;
          });
    }

    final CheckBoxPreference copyCutPref =
        findPreference(getString(R.string.settings_key_keyboard_api_clipboard_copy_cut_enabled));
    if (copyCutPref != null) {
      copyCutPref.setOnPreferenceChangeListener(
          (p, newValue) -> {
            if (!(newValue instanceof Boolean)) return false;
            final boolean enableCopyCut = (Boolean) newValue;
            if (!enableCopyCut) return true;

            if (mSuppressClipboardCopyCutEnableConfirmation) {
              mSuppressClipboardCopyCutEnableConfirmation = false;
              return true;
            }

            new AlertDialog.Builder(requireContext())
                .setTitle(R.string.keyboard_api_clipboard_copy_cut_enabled_title)
                .setMessage(R.string.keyboard_api_clipboard_copy_cut_enabled_confirm_message)
                .setPositiveButton(
                    android.R.string.ok,
                    (d, which) -> {
                      mSuppressClipboardCopyCutEnableConfirmation = true;
                      copyCutPref.setChecked(true);
                    })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
            return false;
          });
    }

    final CheckBoxPreference automationControllersPref =
        findPreference(
            getString(R.string.settings_key_keyboard_api_automation_controllers_enabled));
    if (automationControllersPref != null) {
      automationControllersPref.setOnPreferenceChangeListener(
          (p, newValue) -> {
            if (!(newValue instanceof Boolean)) return false;
            final boolean enableAutomationControllers = (Boolean) newValue;
            if (!enableAutomationControllers) return true;

            if (mSuppressAutomationControllersEnableConfirmation) {
              mSuppressAutomationControllersEnableConfirmation = false;
              return true;
            }

            new AlertDialog.Builder(requireContext())
                .setTitle(R.string.keyboard_api_automation_controllers_enabled_title)
                .setMessage(R.string.keyboard_api_automation_controllers_enabled_confirm_message)
                .setPositiveButton(
                    android.R.string.ok,
                    (d, which) -> {
                      mSuppressAutomationControllersEnableConfirmation = true;
                      automationControllersPref.setChecked(true);
                    })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
            return false;
          });
    }

    mControllersUi.refreshControllers();
    mControllersUi.refreshPairingRequests();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (mControllersUi != null) {
      mControllersUi.refreshControllers();
      mControllersUi.refreshPairingRequests();
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, getString(R.string.keyboard_api_settings_title));
  }
}
