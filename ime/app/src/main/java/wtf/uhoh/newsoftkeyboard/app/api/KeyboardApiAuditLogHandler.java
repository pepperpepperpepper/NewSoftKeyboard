package wtf.uhoh.newsoftkeyboard.app.api;

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.api.KeyboardApiContract;
import java.util.ArrayList;

final class KeyboardApiAuditLogHandler {

  private KeyboardApiAuditLogHandler() {}

  @NonNull
  static Bundle getAuditLog(
      @NonNull KeyboardApiAuditLogStore auditLogStore,
      @NonNull String callingPackage,
      @NonNull Bundle extras) {
    int limit = extras.getInt(KeyboardApiContract.EXTRA_AUDIT_LIMIT, 50);
    limit = Math.max(0, Math.min(100, limit));

    final Bundle out = KeyboardApiCallSupport.ok();
    out.putStringArrayList(
        KeyboardApiContract.EXTRA_AUDIT_ENTRIES,
        new ArrayList<>(auditLogStore.getLatestEntriesForPackage(callingPackage, limit)));
    return out;
  }

  @NonNull
  static Bundle clearAuditLog(
      @NonNull KeyboardApiAuditLogStore auditLogStore, @NonNull String callingPackage) {
    auditLogStore.clearForPackage(callingPackage);
    return KeyboardApiCallSupport.ok();
  }
}
