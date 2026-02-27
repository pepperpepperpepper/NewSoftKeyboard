package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.sqlite.ContextProfileWordListDictionary;
import wtf.uhoh.newsoftkeyboard.prefs.context.ContextProfilesStore;

final class ContextProfilesPresetsController {

  interface Host {
    @NonNull
    Context requireContext();

    @NonNull
    ContextProfilesStore store();

    void refreshSummaries();

    void openWordListEditor(@NonNull String presetId, @NonNull String presetName);
  }

  @NonNull private final Host host;
  @NonNull private final ContextProfilesPresetOptionsUi presetOptionsUi;
  @NonNull private final ContextProfilesPresetRulesUi presetRulesUi;

  ContextProfilesPresetsController(@NonNull Host host) {
    this.host = host;
    presetOptionsUi = new ContextProfilesPresetOptionsUi(host);
    presetRulesUi = new ContextProfilesPresetRulesUi(host);
  }

  void showPresetsDialog() {
    final Context context = host.requireContext();
    final ContextProfilesStore store = host.store();
    final List<ContextProfilesStore.Preset> presets = store.listPresets();

    if (presets.isEmpty()) {
      new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
          .setTitle(R.string.context_profiles_manage_presets_title)
          .setMessage(R.string.context_profiles_manage_presets_empty_message)
          .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
          .setPositiveButton(
              R.string.context_profiles_preset_create_action,
              (d, w) -> {
                d.dismiss();
                showCreatePresetDialog();
              })
          .show();
      return;
    }

    final ArrayList<String> labels = new ArrayList<>(presets.size());
    for (ContextProfilesStore.Preset preset : presets) {
      labels.add(preset.name);
    }

    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(R.string.context_profiles_manage_presets_title)
        .setItems(
            labels.toArray(new CharSequence[0]),
            (d, which) -> {
              d.dismiss();
              if (which < 0 || which >= presets.size()) return;
              showPresetActionsDialog(presets.get(which));
            })
        .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
        .setPositiveButton(
            R.string.context_profiles_preset_create_action,
            (d, w) -> {
              d.dismiss();
              showCreatePresetDialog();
            })
        .show();
  }

  void showCreatePresetDialog() {
    final Context context = host.requireContext();
    final ContextProfilesStore store = host.store();
    final EditText input = new EditText(context);
    input.setHint(R.string.context_profiles_preset_name_hint);
    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(R.string.context_profiles_preset_create_title)
        .setView(input)
        .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
        .setPositiveButton(
            android.R.string.ok,
            (d, w) -> {
              d.dismiss();
              final String name = String.valueOf(input.getText()).trim();
              if (name.isEmpty()) return;
              final ContextProfilesStore.Preset created;
              try {
                created = store.createPreset(name);
              } catch (IllegalStateException e) {
                Toast.makeText(
                        context,
                        context.getString(
                            R.string.context_profiles_limit_too_many_presets_toast,
                            ContextProfilesStore.MAX_PRESETS),
                        Toast.LENGTH_SHORT)
                    .show();
                return;
              }
              host.refreshSummaries();
              presetRulesUi.showVoiceRulesDialog(created.id);
            })
        .show();
  }

  private void showPresetActionsDialog(@NonNull ContextProfilesStore.Preset preset) {
    final Context context = host.requireContext();
    final CharSequence[] items =
        new CharSequence[] {
          context.getText(R.string.context_profiles_preset_action_edit_voice_rules),
          context.getText(R.string.context_profiles_preset_action_edit_typed_rules),
          context.getText(R.string.context_profiles_preset_action_edit_safe_toggles),
          context.getText(R.string.context_profiles_preset_action_edit_security_options),
          context.getText(R.string.context_profiles_preset_action_edit_word_list),
          context.getText(R.string.context_profiles_preset_action_rename),
          context.getText(R.string.context_profiles_preset_action_duplicate),
          context.getText(R.string.context_profiles_preset_action_delete)
        };

    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(preset.name)
        .setItems(
            items,
            (d, which) -> {
              d.dismiss();
              switch (which) {
                case 0:
                  presetRulesUi.showVoiceRulesDialog(preset.id);
                  break;
                case 1:
                  presetRulesUi.showTypedRulesDialog(preset.id);
                  break;
                case 2:
                  presetOptionsUi.showSafeTogglesDialog(preset.id);
                  break;
                case 3:
                  presetOptionsUi.showSecurityOptionsDialog(preset.id);
                  break;
                case 4:
                  host.openWordListEditor(preset.id, preset.name);
                  break;
                case 5:
                  showRenamePresetDialog(preset.id, preset.name);
                  break;
                case 6:
                  showDuplicatePresetDialog(preset.id, preset.name);
                  break;
                case 7:
                  confirmDeletePreset(preset.id, preset.name);
                  break;
                default:
                  break;
              }
            })
        .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
        .show();
  }

  private void showRenamePresetDialog(@NonNull String presetId, @NonNull String currentName) {
    final Context context = host.requireContext();
    final ContextProfilesStore store = host.store();
    final EditText input = new EditText(context);
    input.setText(currentName);
    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(R.string.context_profiles_preset_rename_title)
        .setView(input)
        .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
        .setPositiveButton(
            android.R.string.ok,
            (d, w) -> {
              d.dismiss();
              final String name = String.valueOf(input.getText()).trim();
              if (name.isEmpty()) return;
              store.renamePreset(presetId, name);
              host.refreshSummaries();
            })
        .show();
  }

  private void showDuplicatePresetDialog(@NonNull String presetId, @NonNull String currentName) {
    final Context context = host.requireContext();
    final ContextProfilesStore store = host.store();
    final EditText input = new EditText(context);
    input.setText(
        context.getString(R.string.context_profiles_preset_duplicate_default_name, currentName));
    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(R.string.context_profiles_preset_duplicate_title)
        .setView(input)
        .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
        .setPositiveButton(
            android.R.string.ok,
            (d, w) -> {
              d.dismiss();
              final ContextProfilesStore.Preset original = store.getPreset(presetId);
              if (original == null) return;
              final String name = String.valueOf(input.getText()).trim();
              if (name.isEmpty()) return;
              final ContextProfilesStore.Preset created;
              try {
                created = store.createPreset(name);
              } catch (IllegalStateException e) {
                Toast.makeText(
                        context,
                        context.getString(
                            R.string.context_profiles_limit_too_many_presets_toast,
                            ContextProfilesStore.MAX_PRESETS),
                        Toast.LENGTH_SHORT)
                    .show();
                return;
              }
              store.setVoiceRules(created.id, original.voiceRules);
              store.setTypedRules(created.id, original.typedRules);
              store.setSafeToggles(created.id, original.safeToggles);
              store.setSecurityOptions(
                  created.id,
                  original.containsPersonalContent,
                  original.allowAutoApplyInNoSuggestionsFields);
              host.refreshSummaries();
            })
        .show();
  }

  private void confirmDeletePreset(@NonNull String presetId, @NonNull String name) {
    final Context context = host.requireContext();
    final ContextProfilesStore store = host.store();
    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(R.string.context_profiles_preset_delete_title)
        .setMessage(context.getString(R.string.context_profiles_preset_delete_message, name))
        .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
        .setPositiveButton(
            R.string.context_profiles_preset_delete_action,
            (d, w) -> {
              d.dismiss();
              store.deletePreset(presetId);
              ContextProfileWordListDictionary.deleteStorageForPreset(context, presetId);
              host.refreshSummaries();
            })
        .show();
  }
}
