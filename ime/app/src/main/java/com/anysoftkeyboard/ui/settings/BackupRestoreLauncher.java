package com.anysoftkeyboard.ui.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.core.util.Pair;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.prefs.GlobalPrefsBackup;
import com.anysoftkeyboard.rx.RxSchedulers;
import com.menny.android.anysoftkeyboard.R;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import java.util.List;
import net.evendanan.pixel.RxProgressDialog;

/** Extracts backup/restore intent launch + execution from MainFragment. */
final class BackupRestoreLauncher {

  interface DialogHost {
    void showDialog(int dialogId, Object data);
  }

  private BackupRestoreLauncher() {}

  static void startChooser(
      @NonNull Fragment fragment,
      int optionId,
      @NonNull CharSequence[] providersTitles,
      @NonNull boolean[] initialChecked,
      @NonNull Boolean[] checked) {
    final String intentAction;
    final int requestCode;
    final @StringRes int errorMsg;
    switch (optionId) {
      case R.id.backup_prefs -> {
        intentAction = Intent.ACTION_CREATE_DOCUMENT;
        requestCode = MainFragment.BACKUP_REQUEST_ID;
        errorMsg = R.string.toast_error_custom_path_backup;
      }
      case R.id.restore_prefs -> {
        intentAction = Intent.ACTION_OPEN_DOCUMENT;
        requestCode = MainFragment.RESTORE_REQUEST_ID;
        errorMsg = R.string.toast_error_custom_path_backup;
      }
      default -> throw new IllegalArgumentException("Unsupported optionId " + optionId);
    }

    Intent dataToFileChooser = new Intent();
    dataToFileChooser.setType("text/xml");
    dataToFileChooser.addCategory(Intent.CATEGORY_OPENABLE);
    dataToFileChooser.putExtra(Intent.EXTRA_TITLE, GlobalPrefsBackup.GLOBAL_BACKUP_FILENAME);
    dataToFileChooser.setAction(intentAction);
    dataToFileChooser.putExtra("checked", checked);
    try {
      fragment.startActivityForResult(dataToFileChooser, requestCode);
    } catch (ActivityNotFoundException e) {
      Toast.makeText(fragment.requireContext(), errorMsg, Toast.LENGTH_LONG).show();
      Logger.e("BackupRestoreLauncher", "Could not launch the custom path activity", e);
    }
  }

  static Disposable launch(
      @NonNull Fragment fragment,
      @NonNull DialogHost dialogHost,
      boolean isBackup,
      @NonNull Uri filePath,
      @NonNull List<GlobalPrefsBackup.ProviderDetails> providers,
      @NonNull Boolean[] checked) {

    final Function<
            Pair<List<GlobalPrefsBackup.ProviderDetails>, Boolean[]>,
            ObservableSource<GlobalPrefsBackup.ProviderDetails>>
        action;
    if (isBackup) {
      action =
          listPair ->
              GlobalPrefsBackup.backup(
                  listPair, fragment.requireContext().getContentResolver().openOutputStream(filePath));
    } else {
      action =
          listPair ->
              GlobalPrefsBackup.restore(
                  listPair, fragment.requireContext().getContentResolver().openInputStream(filePath));
    }

    return RxProgressDialog.create(
            new Pair<>(providers, checked),
            fragment.requireActivity(),
            fragment.getText(R.string.take_a_while_progress_message),
            R.layout.progress_window)
        .subscribeOn(RxSchedulers.background())
        .flatMap(action)
        .observeOn(RxSchedulers.mainThread())
        .subscribe(
            providerDetails ->
                Logger.i(
                    "BackupRestoreLauncher",
                    "Finished backing up %s",
                    providerDetails.provider.providerId()),
            e -> dialogHost.showDialog(
                isBackup ? MainFragment.DIALOG_SAVE_FAILED : MainFragment.DIALOG_LOAD_FAILED,
                e.getMessage()),
            () ->
                dialogHost.showDialog(
                    isBackup ? MainFragment.DIALOG_SAVE_SUCCESS : MainFragment.DIALOG_LOAD_SUCCESS,
                    filePath));
  }
}
