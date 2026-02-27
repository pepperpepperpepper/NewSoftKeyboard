package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemePresetAppBindingStore;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemePresetStore;

final class KeyboardThemePresetPerAppBindingsController {

  @NonNull private final KeyboardThemeCustomizationPresetsSection.Host host;
  @NonNull private final KeyboardThemePresetStore presetStore;

  @Nullable private KeyboardThemePresetAppBindingStore presetAppBindingStore;
  @Nullable private KeyboardThemePresetPerAppBindingsDialogsUi dialogsUi;

  @Nullable private Preference perAppBindingsHeaderPref;
  @Nullable private Preference bindLastUsedAppPref;
  @Nullable private Preference clearLastUsedAppBindingPref;
  @Nullable private Preference bindAnyAppPref;

  @Nullable private String currentPresetId;

  KeyboardThemePresetPerAppBindingsController(
      @NonNull KeyboardThemeCustomizationPresetsSection.Host host,
      @NonNull KeyboardThemePresetStore presetStore) {
    this.host = host;
    this.presetStore = presetStore;
  }

  void addPreferences(@NonNull Context context, @NonNull PreferenceCategory presets) {
    if (presetAppBindingStore == null) {
      presetAppBindingStore = new KeyboardThemePresetAppBindingStore(context);
    }
    if (dialogsUi == null && presetAppBindingStore != null) {
      dialogsUi =
          new KeyboardThemePresetPerAppBindingsDialogsUi(host, presetStore, presetAppBindingStore);
    }

    perAppBindingsHeaderPref = new Preference(context);
    perAppBindingsHeaderPref.setKey("keyboard_theme_presets_per_app_bindings");
    perAppBindingsHeaderPref.setTitle(R.string.keyboard_theme_presets_per_app_bindings_title);
    perAppBindingsHeaderPref.setOnPreferenceClickListener(
        ignored -> {
          showManagePerAppBindingsDialogIfPossible(context);
          return true;
        });
    presets.addPreference(perAppBindingsHeaderPref);

    bindLastUsedAppPref = new Preference(context);
    bindLastUsedAppPref.setKey("keyboard_theme_presets_bind_last_used_app");
    bindLastUsedAppPref.setTitle(R.string.keyboard_theme_presets_bind_last_used_app_title);
    bindLastUsedAppPref.setSummary(R.string.keyboard_theme_presets_bind_last_used_app_summary);
    bindLastUsedAppPref.setOnPreferenceClickListener(
        ignored -> {
          bindLastUsedAppToCurrentPreset(context);
          return true;
        });
    presets.addPreference(bindLastUsedAppPref);

    clearLastUsedAppBindingPref = new Preference(context);
    clearLastUsedAppBindingPref.setKey("keyboard_theme_presets_clear_last_used_app_binding");
    clearLastUsedAppBindingPref.setTitle(
        R.string.keyboard_theme_presets_clear_last_used_app_binding_title);
    clearLastUsedAppBindingPref.setSummary(
        R.string.keyboard_theme_presets_clear_last_used_app_binding_summary);
    clearLastUsedAppBindingPref.setOnPreferenceClickListener(
        ignored -> {
          clearLastUsedAppBinding(context);
          return true;
        });
    presets.addPreference(clearLastUsedAppBindingPref);

    bindAnyAppPref = new Preference(context);
    bindAnyAppPref.setKey("keyboard_theme_presets_bind_any_app");
    bindAnyAppPref.setTitle(R.string.keyboard_theme_presets_bind_any_app_title);
    bindAnyAppPref.setSummary(R.string.keyboard_theme_presets_bind_any_app_summary);
    bindAnyAppPref.setOnPreferenceClickListener(
        ignored -> {
          showBindAnyAppDialogIfPossible(context);
          return true;
        });
    presets.addPreference(bindAnyAppPref);
  }

  void refreshState(@NonNull String baseThemeId, @NonNull String presetId, boolean busy) {
    currentPresetId = presetId;
    updatePerAppBindingsUi(baseThemeId, busy);
  }

  private void updatePerAppBindingsUi(@NonNull String baseThemeId, boolean busy) {
    final KeyboardThemePresetAppBindingStore store = presetAppBindingStore;
    if (store == null) return;

    final List<KeyboardThemePresetAppBindingStore.AppBinding> bindings =
        store.listBindings(baseThemeId);
    final String lastPackage = store.getLastImePackageName();

    final Preference header = perAppBindingsHeaderPref;
    if (header != null) {
      if (bindings.isEmpty()) {
        header.setSummary(R.string.keyboard_theme_presets_per_app_bindings_summary_none);
      } else {
        header.setSummary(
            header
                .getContext()
                .getString(
                    R.string.keyboard_theme_presets_per_app_bindings_summary_some,
                    bindings.size()));
      }
      header.setEnabled(!busy);
    }

    if (bindLastUsedAppPref != null) {
      bindLastUsedAppPref.setEnabled(!busy && lastPackage != null);
    }
    if (clearLastUsedAppBindingPref != null) {
      clearLastUsedAppBindingPref.setEnabled(!busy && lastPackage != null);
    }
    if (bindAnyAppPref != null) {
      bindAnyAppPref.setEnabled(!busy);
    }
  }

  private void showManagePerAppBindingsDialogIfPossible(@NonNull Context context) {
    final KeyboardThemePresetPerAppBindingsDialogsUi dialogs = dialogsUi;
    if (dialogs == null) return;

    final String baseThemeId = host.getBaseThemeIdOrNull();
    final String presetId = currentPresetId;
    if (baseThemeId == null || presetId == null) return;

    dialogs.showManagePerAppBindingsDialog(context, baseThemeId, presetId);
  }

  private void showBindAnyAppDialogIfPossible(@NonNull Context context) {
    final KeyboardThemePresetPerAppBindingsDialogsUi dialogs = dialogsUi;
    if (dialogs == null) return;

    final String baseThemeId = host.getBaseThemeIdOrNull();
    final String presetId = currentPresetId;
    if (baseThemeId == null || presetId == null) return;

    dialogs.showBindAnyAppDialog(context, baseThemeId, presetId);
  }

  private void bindLastUsedAppToCurrentPreset(@NonNull Context context) {
    final KeyboardThemePresetAppBindingStore store = presetAppBindingStore;
    if (store == null) return;

    final String baseThemeId = host.getBaseThemeIdOrNull();
    final String presetId = currentPresetId;
    if (baseThemeId == null || presetId == null) return;

    final String lastPackage = store.getLastImePackageName();
    if (lastPackage == null) {
      Toast.makeText(
              context,
              R.string.keyboard_theme_presets_bind_last_used_app_missing,
              Toast.LENGTH_SHORT)
          .show();
      return;
    }

    store.bindAppToPreset(baseThemeId, lastPackage, presetId);
    Toast.makeText(
            context, R.string.keyboard_theme_presets_bind_last_used_app_toast, Toast.LENGTH_SHORT)
        .show();
    host.refreshState();
  }

  private void clearLastUsedAppBinding(@NonNull Context context) {
    final KeyboardThemePresetAppBindingStore store = presetAppBindingStore;
    if (store == null) return;

    final String baseThemeId = host.getBaseThemeIdOrNull();
    if (baseThemeId == null) return;

    final String lastPackage = store.getLastImePackageName();
    if (lastPackage == null) {
      Toast.makeText(
              context,
              R.string.keyboard_theme_presets_bind_last_used_app_missing,
              Toast.LENGTH_SHORT)
          .show();
      return;
    }

    store.unbindApp(baseThemeId, lastPackage);
    Toast.makeText(
            context,
            R.string.keyboard_theme_presets_clear_last_used_app_binding_toast,
            Toast.LENGTH_SHORT)
        .show();
    host.refreshState();
  }
}
