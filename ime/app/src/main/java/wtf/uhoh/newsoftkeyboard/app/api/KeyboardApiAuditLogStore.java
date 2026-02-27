package wtf.uhoh.newsoftkeyboard.app.api;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

public final class KeyboardApiAuditLogStore {

  private static final String PREFS_NAME = "keyboard_api_audit_log";

  private static final String KEY_NEXT_INDEX = "next_index";
  private static final String KEY_COUNT = "count";
  private static final String KEY_ENTRY_PREFIX = "entry_";

  private static final int MAX_ENTRIES = 200;

  @NonNull private final SharedPreferences mPrefs;
  @NonNull private final Object mLock = new Object();

  public KeyboardApiAuditLogStore(@NonNull Context context) {
    final Context appContext =
        context.getApplicationContext() != null ? context.getApplicationContext() : context;
    mPrefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
  }

  public void record(@NonNull String callingPackage, @NonNull String method, int errorCode) {
    final long ts = System.currentTimeMillis();
    final String entry = ts + "\t" + callingPackage + "\t" + method + "\t" + errorCode;

    synchronized (mLock) {
      addEntryLocked(entry);
    }
  }

  @NonNull
  public List<String> getLatestEntries(int limit) {
    return getLatestEntriesInternal(null, limit);
  }

  @NonNull
  public List<String> getLatestEntriesForPackage(@NonNull String callingPackage, int limit) {
    return getLatestEntriesInternal(callingPackage, limit);
  }

  public void clearAll() {
    synchronized (mLock) {
      mPrefs.edit().clear().apply();
    }
  }

  public void clearForPackage(@NonNull String callingPackage) {
    synchronized (mLock) {
      final List<String> newestFirst = getLatestEntriesInternal(null, MAX_ENTRIES);
      mPrefs.edit().clear().apply();

      for (int i = newestFirst.size() - 1; i >= 0; i--) {
        final String entry = newestFirst.get(i);
        final ParsedEntry parsed = parse(entry);
        if (parsed == null) continue;
        if (!callingPackage.equals(parsed.callingPackage)) {
          addEntryLocked(entry);
        }
      }
    }
  }

  @NonNull
  private List<String> getLatestEntriesInternal(String callingPackageOrNull, int limit) {
    final ArrayList<String> out = new ArrayList<>();
    if (limit <= 0) return out;

    synchronized (mLock) {
      final int count = Math.max(0, mPrefs.getInt(KEY_COUNT, 0));
      if (count == 0) return out;

      final int cappedLimit = Math.min(limit, count);
      final int next = positiveMod(mPrefs.getInt(KEY_NEXT_INDEX, 0), MAX_ENTRIES);
      final int earliest = count == MAX_ENTRIES ? next : 0;

      // Walk newest to oldest and filter.
      int scanned = 0;
      int i = 0;
      while (scanned < count && out.size() < cappedLimit) {
        final int index = positiveMod(earliest + (count - 1 - i), MAX_ENTRIES);
        final String entry = mPrefs.getString(KEY_ENTRY_PREFIX + index, null);
        if (entry != null) {
          if (callingPackageOrNull == null) {
            out.add(entry);
          } else {
            final ParsedEntry parsed = parse(entry);
            if (parsed != null && callingPackageOrNull.equals(parsed.callingPackage)) {
              out.add(entry);
            }
          }
        }
        i++;
        scanned++;
      }
    }

    return out;
  }

  private void addEntryLocked(@NonNull String entry) {
    final int next = positiveMod(mPrefs.getInt(KEY_NEXT_INDEX, 0), MAX_ENTRIES);
    final int count = Math.max(0, mPrefs.getInt(KEY_COUNT, 0));

    mPrefs
        .edit()
        .putString(KEY_ENTRY_PREFIX + next, entry)
        .putInt(KEY_NEXT_INDEX, (next + 1) % MAX_ENTRIES)
        .putInt(KEY_COUNT, Math.min(MAX_ENTRIES, count + 1))
        .apply();
  }

  private static int positiveMod(int value, int mod) {
    if (mod <= 0) return 0;
    final int r = value % mod;
    return r < 0 ? r + mod : r;
  }

  private static final class ParsedEntry {
    @NonNull final String callingPackage;
    @NonNull final String method;
    final int errorCode;

    ParsedEntry(@NonNull String callingPackage, @NonNull String method, int errorCode) {
      this.callingPackage = callingPackage;
      this.method = method;
      this.errorCode = errorCode;
    }
  }

  private static ParsedEntry parse(@NonNull String entry) {
    // Expected: ts\tpkg\tmethod\terrorCode
    final String[] parts = entry.split("\t", 4);
    if (parts.length != 4) return null;
    final String pkg = parts[1];
    final String method = parts[2];
    final int code;
    try {
      code = Integer.parseInt(parts[3]);
    } catch (NumberFormatException e) {
      return null;
    }
    return new ParsedEntry(pkg, method, code);
  }
}
