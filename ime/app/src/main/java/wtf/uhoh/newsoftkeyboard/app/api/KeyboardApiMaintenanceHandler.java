package wtf.uhoh.newsoftkeyboard.app.api;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.ime.ImeServiceBase;

final class KeyboardApiMaintenanceHandler {

  private KeyboardApiMaintenanceHandler() {}

  @NonNull
  static Bundle clearLearningData(@NonNull Context context) {
    // Always delete persisted next-word files (covers all locales, including ones not currently
    // active).
    deleteInternalFilesWithPrefix(context, "next_words_");

    final ImeServiceBase ime = ImeServiceBase.getInstance();
    if (ime != null) {
      // Clear in-memory learning data so the effect is immediate.
      ime.suggest().clearLearningData();
      ime.abortCorrectionAndResetPredictionState(false);
      return KeyboardApiCallSupport.ok();
    }

    // If the IME isn't active, we can safely delete the auto-dictionary database.
    context.deleteDatabase("auto_dict_2.db");
    return KeyboardApiCallSupport.ok();
  }

  @NonNull
  static Bundle clearClipboardHistory(@NonNull Context context) {
    final ImeServiceBase ime = ImeServiceBase.getInstance();
    if (ime != null) {
      ime.clearClipboardHistoryForProgrammableApi();
      return KeyboardApiCallSupport.ok();
    }

    clearOsClipboard(context);
    return KeyboardApiCallSupport.ok();
  }

  @NonNull
  static Bundle clearQuickTextHistory(@NonNull Context context) {
    final String key = context.getString(R.string.settings_key_quick_text_history);
    final android.content.SharedPreferences sharedPreferences =
        wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences.create(context);
    sharedPreferences.edit().remove(key).apply();
    return KeyboardApiCallSupport.ok();
  }

  private static void clearOsClipboard(@NonNull Context context) {
    final Object svc = context.getSystemService(Context.CLIPBOARD_SERVICE);
    if (!(svc instanceof android.content.ClipboardManager)) return;
    final android.content.ClipboardManager clipboardManager =
        (android.content.ClipboardManager) svc;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      clipboardManager.clearPrimaryClip();
    } else {
      clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("", ""));
    }
  }

  private static int deleteInternalFilesWithPrefix(
      @NonNull Context context, @NonNull String prefix) {
    int deleted = 0;
    final String[] files = context.fileList();
    if (files == null) return 0;
    for (String name : files) {
      if (name != null && name.startsWith(prefix)) {
        if (context.deleteFile(name)) deleted++;
      }
    }
    return deleted;
  }
}
