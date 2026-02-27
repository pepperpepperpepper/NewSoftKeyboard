package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import java.util.List;
import net.evendanan.pixel.UiUtils;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.sqlite.ContextProfileWordListDictionary;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;

public class ContextProfilesSettingsFragment extends PreferenceFragmentCompat {

  private static final String KEY_ENABLED = "context_profiles_enabled";
  private static final String KEY_MANAGE_PRESETS = "context_profiles_manage_presets";
  private static final String KEY_MANAGE_BINDINGS = "context_profiles_manage_bindings";
  private static final String KEY_RESET = "context_profiles_reset";

  @NonNull private ContextProfilesStore store;
  @NonNull private ContextProfilesPresetsController presetsController;
  @NonNull private ContextProfilesBindingsController bindingsController;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.prefs_context_profiles, rootKey);
    store = new ContextProfilesStore(requireContext().getApplicationContext());

    presetsController =
        new ContextProfilesPresetsController(
            new ContextProfilesPresetsController.Host() {
              @NonNull
              @Override
              public Context requireContext() {
                return ContextProfilesSettingsFragment.this.requireContext();
              }

              @NonNull
              @Override
              public ContextProfilesStore store() {
                return store;
              }

              @Override
              public void refreshSummaries() {
                ContextProfilesSettingsFragment.this.refreshSummaries();
              }

              @Override
              public void openWordListEditor(@NonNull String presetId, @NonNull String presetName) {
                ContextProfilesSettingsFragment.this.openWordListEditor(presetId, presetName);
              }
            });

    bindingsController =
        new ContextProfilesBindingsController(
            new ContextProfilesBindingsController.Host() {
              @NonNull
              @Override
              public Context requireContext() {
                return ContextProfilesSettingsFragment.this.requireContext();
              }

              @Override
              public boolean isAdded() {
                return ContextProfilesSettingsFragment.this.isAdded();
              }

              @NonNull
              @Override
              public ContextProfilesStore store() {
                return store;
              }

              @Override
              public void refreshSummaries() {
                ContextProfilesSettingsFragment.this.refreshSummaries();
              }

              @Override
              public void showCreatePresetDialog() {
                presetsController.showCreatePresetDialog();
              }
            });
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    final SwitchPreferenceCompat enabled = findPreference(KEY_ENABLED);
    if (enabled != null) {
      enabled.setChecked(store.isEnabled());
      enabled.setOnPreferenceChangeListener(
          (preference, newValue) -> {
            store.setEnabled(Boolean.TRUE.equals(newValue));
            refreshSummaries();
            return true;
          });
    }

    final Preference presets = findPreference(KEY_MANAGE_PRESETS);
    if (presets != null) {
      presets.setOnPreferenceClickListener(
          ignored -> {
            presetsController.showPresetsDialog();
            return true;
          });
    }

    final Preference bindings = findPreference(KEY_MANAGE_BINDINGS);
    if (bindings != null) {
      bindings.setOnPreferenceClickListener(
          ignored -> {
            bindingsController.showBindingsLoadingDialog();
            return true;
          });
    }

    final Preference reset = findPreference(KEY_RESET);
    if (reset != null) {
      reset.setOnPreferenceClickListener(
          ignored -> {
            confirmReset();
            return true;
          });
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, R.string.context_profiles_settings_title);
  }

  @Override
  public void onResume() {
    super.onResume();
    refreshSummaries();
  }

  private void refreshSummaries() {
    final Preference presets = findPreference(KEY_MANAGE_PRESETS);
    if (presets != null) {
      final int count = store.listPresets().size();
      presets.setSummary(
          count == 0
              ? getString(R.string.context_profiles_manage_presets_summary_empty)
              : getString(R.string.context_profiles_manage_presets_summary_count, count));
    }

    final Preference bindings = findPreference(KEY_MANAGE_BINDINGS);
    if (bindings != null) {
      final int count = store.listBindings().size();
      bindings.setSummary(
          count == 0
              ? getString(R.string.context_profiles_manage_bindings_summary_empty)
              : getString(R.string.context_profiles_manage_bindings_summary_count, count));
    }
  }

  private void confirmReset() {
    new AlertDialog.Builder(requireContext(), R.style.Theme_NskAlertDialog)
        .setTitle(R.string.context_profiles_reset_title)
        .setMessage(R.string.context_profiles_reset_confirm_message)
        .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
        .setPositiveButton(
            R.string.context_profiles_reset_confirm_action,
            (d, w) -> {
              d.dismiss();
              resetAll();
            })
        .show();
  }

  private void resetAll() {
    store.setEnabled(false);
    store.clearAllBindings();
    final List<ContextProfilesStore.Preset> presets = store.listPresets();
    for (ContextProfilesStore.Preset preset : presets) {
      store.deletePreset(preset.id);
      ContextProfileWordListDictionary.deleteStorageForPreset(requireContext(), preset.id);
    }
    refreshSummaries();
    final SwitchPreferenceCompat enabled = findPreference(KEY_ENABLED);
    if (enabled != null) enabled.setChecked(false);
    Toast.makeText(requireContext(), R.string.context_profiles_reset_done_toast, Toast.LENGTH_SHORT)
        .show();
  }

  private void openWordListEditor(@NonNull String presetId, @NonNull String presetName) {
    final Bundle args = new Bundle();
    args.putString("preset_id", presetId);
    args.putString("preset_name", presetName);
    Navigation.findNavController(requireView())
        .navigate(
            R.id.action_contextProfilesSettingsFragment_to_contextProfileWordListEditorFragment,
            args);
  }
}
