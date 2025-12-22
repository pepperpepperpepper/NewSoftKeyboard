package com.anysoftkeyboard.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.prefs.GlobalPrefsBackup;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.disposables.Disposable;
import java.util.List;
import net.evendanan.pixel.GeneralDialogController;

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
    builder.setPositiveButton(
        actionTitle,
        (dialog, which) ->
            BackupRestoreLauncher.startChooser(
                fragment, optionId, providersTitles, initialChecked, checked));
  }
}
