package wtf.uhoh.newsoftkeyboard.app.api;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

public final class KeyboardApiPairingStore {

  private static final int MAX_PENDING_REQUESTS = 10;
  private static final long REQUEST_COOLDOWN_MS = 30_000;

  private static final String KEY_PENDING_PACKAGES = "keyboard_api_pairing_pending_packages";

  private static final String KEY_REQUEST_PREFIX = "keyboard_api_pairing_request.";
  private static final String KEY_SUFFIX_STATUS = ".status";
  private static final String KEY_SUFFIX_REQUESTED_SCOPES = ".requested_scopes";
  private static final String KEY_SUFFIX_APPROVED_SCOPES = ".approved_scopes";
  private static final String KEY_SUFFIX_CREATED_AT_MS = ".created_at_ms";
  private static final String KEY_SUFFIX_UPDATED_AT_MS = ".updated_at_ms";
  private static final String KEY_SUFFIX_TOKEN_DELIVERED = ".token_delivered";
  private static final String KEY_SUFFIX_LAST_REQUEST_AT_MS = ".last_request_at_ms";
  private static final String KEY_SUFFIX_NOTIFIED = ".notified";

  static final String STATUS_PENDING = "pending";
  static final String STATUS_APPROVED = "approved";
  static final String STATUS_DENIED = "denied";

  @NonNull private final SharedPreferences mPrefs;

  public KeyboardApiPairingStore(@NonNull Context context) {
    mPrefs = DirectBootAwareSharedPreferences.create(context);
  }

  @NonNull
  public Set<String> getPendingPackages() {
    final Set<String> set = mPrefs.getStringSet(KEY_PENDING_PACKAGES, null);
    if (set == null || set.isEmpty()) return Collections.emptySet();
    return new HashSet<>(set);
  }

  public boolean hasRequest(@NonNull String packageName) {
    return mPrefs.contains(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_STATUS);
  }

  @NonNull
  public String getStatus(@NonNull String packageName) {
    return String.valueOf(
        mPrefs.getString(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_STATUS, ""));
  }

  @NonNull
  public Set<String> getRequestedScopes(@NonNull String packageName) {
    return getStringSet(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_REQUESTED_SCOPES);
  }

  @NonNull
  public Set<String> getApprovedScopes(@NonNull String packageName) {
    return getStringSet(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_APPROVED_SCOPES);
  }

  public boolean isTokenDelivered(@NonNull String packageName) {
    return mPrefs.getBoolean(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_TOKEN_DELIVERED, false);
  }

  public boolean isNotified(@NonNull String packageName) {
    return mPrefs.getBoolean(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_NOTIFIED, false);
  }

  public long getCreatedAtMs(@NonNull String packageName) {
    return mPrefs.getLong(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_CREATED_AT_MS, 0L);
  }

  public long getUpdatedAtMs(@NonNull String packageName) {
    return mPrefs.getLong(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_UPDATED_AT_MS, 0L);
  }

  @NonNull
  public RequestDecision recordRequest(
      @NonNull String packageName, @NonNull Set<String> requestedScopes, long nowMs) {
    final long lastRequestAt =
        mPrefs.getLong(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_LAST_REQUEST_AT_MS, 0L);
    if (nowMs - lastRequestAt < REQUEST_COOLDOWN_MS) {
      final long retryAfterMs = REQUEST_COOLDOWN_MS - Math.max(0, nowMs - lastRequestAt);
      return RequestDecision.rateLimited(retryAfterMs);
    }

    final Set<String> pending = getPendingPackages();
    if (!pending.contains(packageName) && pending.size() >= MAX_PENDING_REQUESTS) {
      return RequestDecision.tooManyPending();
    }

    final boolean alreadyPending = pending.contains(packageName);
    final HashSet<String> newPending = new HashSet<>(pending);
    newPending.add(packageName);

    final SharedPreferences.Editor editor =
        mPrefs
            .edit()
            .putStringSet(KEY_PENDING_PACKAGES, newPending)
            .putString(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_STATUS, STATUS_PENDING)
            .putStringSet(
                KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_REQUESTED_SCOPES,
                new HashSet<>(requestedScopes))
            .putLong(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_CREATED_AT_MS, nowMs)
            .putLong(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_UPDATED_AT_MS, nowMs)
            .putLong(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_LAST_REQUEST_AT_MS, nowMs)
            .putBoolean(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_TOKEN_DELIVERED, false);
    if (!alreadyPending) {
      editor.putBoolean(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_NOTIFIED, false);
    }
    editor.apply();

    return RequestDecision.ok();
  }

  public void approve(
      @NonNull String packageName, @NonNull Set<String> approvedScopes, long nowMs) {
    removeFromPendingSet(packageName);
    mPrefs
        .edit()
        .putString(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_STATUS, STATUS_APPROVED)
        .putStringSet(
            KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_APPROVED_SCOPES,
            new HashSet<>(approvedScopes))
        .putLong(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_UPDATED_AT_MS, nowMs)
        .putBoolean(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_TOKEN_DELIVERED, false)
        .apply();
  }

  public void deny(@NonNull String packageName, long nowMs) {
    removeFromPendingSet(packageName);
    mPrefs
        .edit()
        .putString(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_STATUS, STATUS_DENIED)
        .putLong(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_UPDATED_AT_MS, nowMs)
        .apply();
  }

  public void markTokenDelivered(@NonNull String packageName, long nowMs) {
    mPrefs
        .edit()
        .putBoolean(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_TOKEN_DELIVERED, true)
        .putLong(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_UPDATED_AT_MS, nowMs)
        .apply();
  }

  public void markNotified(@NonNull String packageName, long nowMs) {
    mPrefs
        .edit()
        .putBoolean(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_NOTIFIED, true)
        .putLong(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_UPDATED_AT_MS, nowMs)
        .apply();
  }

  public void clear(@NonNull String packageName) {
    removeFromPendingSet(packageName);
    mPrefs
        .edit()
        .remove(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_STATUS)
        .remove(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_REQUESTED_SCOPES)
        .remove(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_APPROVED_SCOPES)
        .remove(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_CREATED_AT_MS)
        .remove(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_UPDATED_AT_MS)
        .remove(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_TOKEN_DELIVERED)
        .remove(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_LAST_REQUEST_AT_MS)
        .remove(KEY_REQUEST_PREFIX + packageName + KEY_SUFFIX_NOTIFIED)
        .apply();
  }

  private void removeFromPendingSet(@NonNull String packageName) {
    final HashSet<String> pending = new HashSet<>(getPendingPackages());
    if (!pending.remove(packageName)) return;
    mPrefs.edit().putStringSet(KEY_PENDING_PACKAGES, pending).apply();
  }

  @NonNull
  private Set<String> getStringSet(@NonNull String key) {
    final Set<String> values = mPrefs.getStringSet(key, null);
    if (values == null || values.isEmpty()) return Collections.emptySet();
    return new HashSet<>(values);
  }

  public static final class RequestDecision {
    public final boolean allowed;
    public final long retryAfterMs;
    @Nullable public final String message;

    private RequestDecision(boolean allowed, long retryAfterMs, @Nullable String message) {
      this.allowed = allowed;
      this.retryAfterMs = retryAfterMs;
      this.message = message;
    }

    @NonNull
    static RequestDecision ok() {
      return new RequestDecision(true, 0L, null);
    }

    @NonNull
    static RequestDecision rateLimited(long retryAfterMs) {
      return new RequestDecision(false, Math.max(0L, retryAfterMs), "Pairing request rate limited");
    }

    @NonNull
    static RequestDecision tooManyPending() {
      return new RequestDecision(false, REQUEST_COOLDOWN_MS, "Too many pending pairing requests");
    }
  }
}
