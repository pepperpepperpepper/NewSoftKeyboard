package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.api.KeyboardApiAuditLogStore;

final class ProgrammableApiAuditLogUi {

  private static final String KEY_AUDIT_VIEW = "keyboard_api_audit_view";
  private static final String KEY_AUDIT_CLEAR = "keyboard_api_audit_clear";

  @NonNull private final PreferenceFragmentCompat mFragment;
  @NonNull private final KeyboardApiAuditLogStore mAuditLogStore;

  ProgrammableApiAuditLogUi(
      @NonNull PreferenceFragmentCompat fragment, @NonNull KeyboardApiAuditLogStore auditLogStore) {
    mFragment = fragment;
    mAuditLogStore = auditLogStore;
  }

  void bindPreferences() {
    final Preference auditView = mFragment.findPreference(KEY_AUDIT_VIEW);
    if (auditView != null) {
      auditView.setOnPreferenceClickListener(
          p -> {
            showAuditLogDialog();
            return true;
          });
    }

    final Preference auditClear = mFragment.findPreference(KEY_AUDIT_CLEAR);
    if (auditClear != null) {
      auditClear.setOnPreferenceClickListener(
          p -> {
            confirmClearAuditLog();
            return true;
          });
    }
  }

  private void showAuditLogDialog() {
    final List<String> entries = mAuditLogStore.getLatestEntries(50);
    if (entries.isEmpty()) {
      showSimpleMessageDialog(
          mFragment.getString(R.string.keyboard_api_audit_title),
          mFragment.getString(R.string.keyboard_api_audit_empty));
      return;
    }

    final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
    final StringBuilder message = new StringBuilder();
    for (String entry : entries) {
      // entry format: ts\tpkg\tmethod\terrorCode
      final String[] parts = entry.split("\t", 4);
      if (parts.length != 4) continue;
      final long ts;
      try {
        ts = Long.parseLong(parts[0]);
      } catch (NumberFormatException e) {
        continue;
      }
      message.append(df.format(new Date(ts)));
      message.append("\n");
      message.append(parts[1]).append("  ").append(parts[2]).append("  code=").append(parts[3]);
      message.append("\n\n");
    }

    new AlertDialog.Builder(mFragment.requireContext())
        .setTitle(R.string.keyboard_api_audit_title)
        .setMessage(message.toString())
        .setPositiveButton(android.R.string.ok, null)
        .show();
  }

  private void confirmClearAuditLog() {
    new AlertDialog.Builder(mFragment.requireContext())
        .setTitle(R.string.keyboard_api_audit_clear_title)
        .setMessage(R.string.keyboard_api_audit_clear_summary)
        .setPositiveButton(android.R.string.ok, (d, which) -> mAuditLogStore.clearAll())
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void showSimpleMessageDialog(@NonNull String title, @NonNull String message) {
    new AlertDialog.Builder(mFragment.requireContext())
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, null)
        .show();
  }
}
