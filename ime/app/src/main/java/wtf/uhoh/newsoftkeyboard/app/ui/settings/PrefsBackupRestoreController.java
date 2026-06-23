package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import io.reactivex.disposables.Disposable;
import java.util.List;
import net.evendanan.pixel.GeneralDialogController;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.prefs.GlobalPrefsBackup;
import wtf.uhoh.newsoftkeyboard.app.prefs.SecretsBackupProvider;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;

/**
 * Owns preference backup/restore UI flow: provider selection dialog, file picker dispatch, and
 * applying the chosen action.
 *
 * <p>Keeps {@link MainFragment} focused on the settings landing page.
 */
final class PrefsBackupRestoreController {

  static final int DIALOG_SAVE_SUCCESS = 10;
  static final int DIALOG_SAVE_FAILED = 11;
  static final int DIALOG_LOAD_SUCCESS = 20;
  static final int DIALOG_LOAD_FAILED = 21;

  static final int BACKUP_REQUEST_ID = 1341;
  static final int RESTORE_REQUEST_ID = 1343;

  @Nullable private List<GlobalPrefsBackup.ProviderDetails> mSupportedProviders;
  @Nullable private Boolean[] mCheckedProviders;

  boolean onSetupDialogRequired(
      @NonNull Fragment fragment,
      @NonNull AlertDialog.Builder builder,
      int optionId,
      @Nullable Object data) {
    switch (optionId) {
      case R.id.backup_prefs, R.id.restore_prefs -> {
        onBackupRestoreDialogRequired(fragment, builder, optionId);
        return true;
      }
      case DIALOG_SAVE_SUCCESS -> {
        builder.setTitle(R.string.prefs_providers_operation_success);
        builder.setMessage(fragment.getString(R.string.prefs_providers_backed_up_to, data));
        builder.setPositiveButton(android.R.string.ok, null);
        return true;
      }
      case DIALOG_SAVE_FAILED -> {
        builder.setTitle(R.string.prefs_providers_operation_failed);
        builder.setMessage(fragment.getString(R.string.prefs_providers_failed_backup_due_to, data));
        builder.setPositiveButton(android.R.string.ok, null);
        return true;
      }
      case DIALOG_LOAD_SUCCESS -> {
        builder.setTitle(R.string.prefs_providers_operation_success);
        builder.setMessage(fragment.getString(R.string.prefs_providers_restored_to, data));
        builder.setPositiveButton(android.R.string.ok, null);
        return true;
      }
      case DIALOG_LOAD_FAILED -> {
        builder.setTitle(R.string.prefs_providers_operation_failed);
        builder.setMessage(
            fragment.getString(R.string.prefs_providers_failed_restore_due_to, data));
        builder.setPositiveButton(android.R.string.ok, null);
        return true;
      }
      default -> {
        return false;
      }
    }
  }

  @Nullable
  Disposable handleActivityResult(
      @NonNull Fragment fragment,
      @NonNull GeneralDialogController dialogController,
      int requestCode,
      int resultCode,
      @Nullable Intent data) {
    if ((requestCode != RESTORE_REQUEST_ID && requestCode != BACKUP_REQUEST_ID)
        || resultCode != Activity.RESULT_OK) {
      return null;
    }

    final List<GlobalPrefsBackup.ProviderDetails> providers = mSupportedProviders;
    final Boolean[] checked = mCheckedProviders;
    if (providers == null || checked == null) {
      Logger.w(
          "PrefsBackupRestoreController", "Missing providers state for backup/restore result.");
      return null;
    }

    try {
      final Uri filePath = data != null ? data.getData() : null;
      if (filePath == null) return null;

      final boolean isBackup = requestCode == BACKUP_REQUEST_ID;
      return BackupRestoreLauncher.launch(
          fragment, dialogController::showDialog, isBackup, filePath, providers, checked);
    } catch (Exception e) {
      Logger.d("PrefsBackupRestoreController", "Error when handling backup/restore result", e);
      return null;
    }
  }

  private void onBackupRestoreDialogRequired(
      @NonNull Fragment fragment, @NonNull AlertDialog.Builder builder, int optionId) {
    final @StringRes int actionTitle;

    switch (optionId) {
      case R.id.backup_prefs -> {
        actionTitle = R.string.word_editor_action_backup_words;
        builder.setTitle(R.string.pick_prefs_providers_to_backup);
      }
      case R.id.restore_prefs -> {
        actionTitle = R.string.word_editor_action_restore_words;
        builder.setTitle(R.string.pick_prefs_providers_to_restore);
      }
      default -> throw new IllegalArgumentException("Unsupported optionId " + optionId);
    }

    final List<GlobalPrefsBackup.ProviderDetails> supportedProviders =
        GlobalPrefsBackup.getAllPrefsProviders(fragment.requireContext());
    mSupportedProviders = supportedProviders;

    final CharSequence[] providersTitles = new CharSequence[supportedProviders.size()];
    final boolean[] initialChecked = new boolean[supportedProviders.size()];
    final Boolean[] checked = new Boolean[supportedProviders.size()];
    mCheckedProviders = checked;

    for (int providerIndex = 0; providerIndex < supportedProviders.size(); providerIndex++) {
      // starting with everything checked
      checked[providerIndex] = initialChecked[providerIndex] = true;
      providersTitles[providerIndex] =
          fragment.getText(supportedProviders.get(providerIndex).providerTitle);
    }

    builder.setMultiChoiceItems(
        providersTitles, initialChecked, (dialogInterface, i, b) -> checked[i] = b);
    builder.setNegativeButton(android.R.string.cancel, null);
    builder.setCancelable(true);
    final boolean isBackup = optionId == R.id.backup_prefs;
    builder.setPositiveButton(
        actionTitle,
        (dialog, which) -> {
          final SecretsBackupProvider secrets = checkedSecretsProvider(supportedProviders, checked);
          // On backup with no stored keys there is nothing to encrypt, so skip the passphrase
          // prompt; on restore we always prompt because the file contents aren't known yet.
          final boolean needsPassphrase =
              secrets != null
                  && (!isBackup || SecretsBackupProvider.hasAnySecret(fragment.requireContext()));
          if (needsPassphrase) {
            // The secrets provider needs a passphrase before the (file-picker -> launch) flow runs.
            promptForPassphrase(
                fragment,
                isBackup,
                passphrase -> {
                  secrets.setPassphrase(passphrase);
                  BackupRestoreLauncher.startChooser(
                      fragment, optionId, providersTitles, initialChecked, checked);
                });
          } else {
            BackupRestoreLauncher.startChooser(
                fragment, optionId, providersTitles, initialChecked, checked);
          }
        });
  }

  @Nullable
  private static SecretsBackupProvider checkedSecretsProvider(
      @NonNull List<GlobalPrefsBackup.ProviderDetails> providers, @NonNull Boolean[] checked) {
    for (int i = 0; i < providers.size() && i < checked.length; i++) {
      if (Boolean.TRUE.equals(checked[i])
          && providers.get(i).provider instanceof SecretsBackupProvider secrets) {
        return secrets;
      }
    }
    return null;
  }

  /**
   * Prompts for the backup/restore passphrase. On backup the user must confirm it; {@code onAccept}
   * is invoked with the passphrase only when validation passes.
   */
  private static void promptForPassphrase(
      @NonNull Fragment fragment, boolean isBackup, @NonNull Consumer<char[]> onAccept) {
    final Context context = fragment.requireContext();
    final int padding = (int) (16 * context.getResources().getDisplayMetrics().density);

    final LinearLayout layout = new LinearLayout(context);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(padding, padding / 2, padding, 0);

    final EditText passphraseField = new EditText(context);
    passphraseField.setInputType(
        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    passphraseField.setHint(R.string.secrets_passphrase_hint);
    layout.addView(passphraseField);

    final EditText confirmField;
    if (isBackup) {
      confirmField = new EditText(context);
      confirmField.setInputType(
          InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
      confirmField.setHint(R.string.secrets_passphrase_confirm_hint);
      layout.addView(confirmField);
    } else {
      confirmField = null;
    }

    new AlertDialog.Builder(context, R.style.Theme_NskAlertDialog)
        .setTitle(R.string.secrets_backup_passphrase_title)
        .setMessage(
            isBackup
                ? R.string.secrets_backup_passphrase_message
                : R.string.secrets_restore_passphrase_message)
        .setView(layout)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(
            android.R.string.ok,
            (d, w) -> {
              final char[] passphrase = textToChars(passphraseField);
              if (passphrase.length == 0) {
                Toast.makeText(context, R.string.secrets_passphrase_empty, Toast.LENGTH_LONG)
                    .show();
                return;
              }
              if (confirmField != null) {
                final char[] confirm = textToChars(confirmField);
                final boolean matches = java.util.Arrays.equals(passphrase, confirm);
                java.util.Arrays.fill(confirm, '\0');
                if (!matches) {
                  java.util.Arrays.fill(passphrase, '\0');
                  Toast.makeText(context, R.string.secrets_passphrase_mismatch, Toast.LENGTH_LONG)
                      .show();
                  return;
                }
              }
              onAccept.accept(passphrase);
            })
        .show();
  }

  @NonNull
  private static char[] textToChars(@NonNull EditText editText) {
    final CharSequence text = editText.getText();
    if (TextUtils.isEmpty(text)) return new char[0];
    final char[] out = new char[text.length()];
    for (int i = 0; i < text.length(); i++) {
      out[i] = text.charAt(i);
    }
    return out;
  }
}
