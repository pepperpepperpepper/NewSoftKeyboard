package wtf.uhoh.newsoftkeyboard.app.api;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

public final class KeyboardApiPrefs {

  public static final String KEY_API_ENABLED = "keyboard_api_enabled";
  public static final String KEY_HIGH_RISK_ACTIONS_ENABLED =
      "keyboard_api_high_risk_actions_enabled";
  public static final String KEY_CLIPBOARD_COPY_CUT_ENABLED =
      "keyboard_api_clipboard_copy_cut_enabled";
  public static final String KEY_AUTOMATION_CONTROLLERS_ENABLED =
      "keyboard_api_automation_controllers_enabled";

  public static final String KEY_CONTROLLERS_PACKAGES = "keyboard_api_controllers_packages";

  private static final String KEY_CONTROLLER_PREFIX = "keyboard_api_controller.";
  private static final String KEY_CONTROLLER_SUFFIX_SCOPES = ".scopes";
  private static final String KEY_CONTROLLER_SUFFIX_CERT_DIGESTS = ".cert_digests_sha256";
  private static final String KEY_CONTROLLER_SUFFIX_LABEL = ".label";
  private static final String KEY_CONTROLLER_SUFFIX_ALLOWED_SESSION_PRESET_IDS =
      ".allowed_session_preset_ids";
  private static final String KEY_CONTROLLER_SUFFIX_ALLOWED_SESSION_THEME_PRESET_IDS =
      ".allowed_session_theme_preset_ids";
  private static final String KEY_CONTROLLER_SUFFIX_ALLOWED_SESSION_KEYBOARD_IDS =
      ".allowed_session_keyboard_ids";
  private static final String KEY_CONTROLLER_SUFFIX_ALLOWED_SESSION_TARGET_PACKAGES =
      ".allowed_session_target_packages";

  @NonNull private final SharedPreferences mPrefs;

  public KeyboardApiPrefs(@NonNull Context context) {
    mPrefs = DirectBootAwareSharedPreferences.create(context);
  }

  public boolean isApiEnabled() {
    return mPrefs.getBoolean(KEY_API_ENABLED, false);
  }

  public boolean isHighRiskActionsEnabled() {
    return mPrefs.getBoolean(KEY_HIGH_RISK_ACTIONS_ENABLED, false);
  }

  public boolean isClipboardCopyCutEnabled() {
    return mPrefs.getBoolean(KEY_CLIPBOARD_COPY_CUT_ENABLED, false);
  }

  public boolean isAutomationControllersEnabled() {
    return mPrefs.getBoolean(KEY_AUTOMATION_CONTROLLERS_ENABLED, false);
  }

  @NonNull
  public Set<String> getAllowListedPackages() {
    final Set<String> packages = mPrefs.getStringSet(KEY_CONTROLLERS_PACKAGES, null);
    if (packages == null || packages.isEmpty()) return Collections.emptySet();
    return new HashSet<>(packages);
  }

  public boolean isPackageAllowListed(@NonNull String packageName) {
    return getAllowListedPackages().contains(packageName);
  }

  @NonNull
  public Set<String> getControllerScopes(@NonNull String packageName) {
    final Set<String> scopes =
        mPrefs.getStringSet(
            KEY_CONTROLLER_PREFIX + packageName + KEY_CONTROLLER_SUFFIX_SCOPES, null);
    if (scopes == null || scopes.isEmpty()) return Collections.emptySet();
    return new HashSet<>(scopes);
  }

  @NonNull
  public Set<String> getControllerCertDigestsSha256(@NonNull String packageName) {
    final Set<String> digests =
        mPrefs.getStringSet(
            KEY_CONTROLLER_PREFIX + packageName + KEY_CONTROLLER_SUFFIX_CERT_DIGESTS, null);
    if (digests == null || digests.isEmpty()) return Collections.emptySet();
    return new HashSet<>(digests);
  }

  @Nullable
  public String getControllerLabel(@NonNull String packageName) {
    return mPrefs.getString(
        KEY_CONTROLLER_PREFIX + packageName + KEY_CONTROLLER_SUFFIX_LABEL, null);
  }

  public void addOrUpdateController(
      @NonNull String packageName,
      @Nullable String label,
      @NonNull Set<String> certDigestsSha256,
      @NonNull Set<String> scopes) {
    final HashSet<String> newControllers = new HashSet<>(getAllowListedPackages());
    newControllers.add(packageName);
    mPrefs
        .edit()
        .putStringSet(KEY_CONTROLLERS_PACKAGES, newControllers)
        .putString(KEY_CONTROLLER_PREFIX + packageName + KEY_CONTROLLER_SUFFIX_LABEL, label)
        .putStringSet(
            KEY_CONTROLLER_PREFIX + packageName + KEY_CONTROLLER_SUFFIX_CERT_DIGESTS,
            new HashSet<>(certDigestsSha256))
        .putStringSet(
            KEY_CONTROLLER_PREFIX + packageName + KEY_CONTROLLER_SUFFIX_SCOPES,
            new HashSet<>(scopes))
        .apply();
  }

  public void removeController(@NonNull String packageName) {
    final HashSet<String> newControllers = new HashSet<>(getAllowListedPackages());
    newControllers.remove(packageName);
    mPrefs
        .edit()
        .putStringSet(KEY_CONTROLLERS_PACKAGES, newControllers)
        .remove(KEY_CONTROLLER_PREFIX + packageName + KEY_CONTROLLER_SUFFIX_LABEL)
        .remove(KEY_CONTROLLER_PREFIX + packageName + KEY_CONTROLLER_SUFFIX_CERT_DIGESTS)
        .remove(KEY_CONTROLLER_PREFIX + packageName + KEY_CONTROLLER_SUFFIX_SCOPES)
        .remove(
            KEY_CONTROLLER_PREFIX + packageName + KEY_CONTROLLER_SUFFIX_ALLOWED_SESSION_PRESET_IDS)
        .remove(
            KEY_CONTROLLER_PREFIX
                + packageName
                + KEY_CONTROLLER_SUFFIX_ALLOWED_SESSION_THEME_PRESET_IDS)
        .remove(
            KEY_CONTROLLER_PREFIX
                + packageName
                + KEY_CONTROLLER_SUFFIX_ALLOWED_SESSION_KEYBOARD_IDS)
        .remove(
            KEY_CONTROLLER_PREFIX
                + packageName
                + KEY_CONTROLLER_SUFFIX_ALLOWED_SESSION_TARGET_PACKAGES)
        .apply();
  }

  @NonNull
  public Set<String> getAllowedSessionPresetIds(@NonNull String packageName) {
    return getStringSet(
        KEY_CONTROLLER_PREFIX + packageName + KEY_CONTROLLER_SUFFIX_ALLOWED_SESSION_PRESET_IDS);
  }

  public void setAllowedSessionPresetIds(
      @NonNull String packageName, @NonNull Set<String> presetIds) {
    mPrefs
        .edit()
        .putStringSet(
            KEY_CONTROLLER_PREFIX + packageName + KEY_CONTROLLER_SUFFIX_ALLOWED_SESSION_PRESET_IDS,
            new HashSet<>(presetIds))
        .apply();
  }

  public boolean isSessionPresetIdAllowListed(
      @NonNull String packageName, @NonNull String presetId) {
    return getAllowedSessionPresetIds(packageName).contains(presetId);
  }

  @NonNull
  public Set<String> getAllowedSessionThemePresetIds(@NonNull String packageName) {
    return getStringSet(
        KEY_CONTROLLER_PREFIX
            + packageName
            + KEY_CONTROLLER_SUFFIX_ALLOWED_SESSION_THEME_PRESET_IDS);
  }

  public void setAllowedSessionThemePresetIds(
      @NonNull String packageName, @NonNull Set<String> presetIds) {
    mPrefs
        .edit()
        .putStringSet(
            KEY_CONTROLLER_PREFIX
                + packageName
                + KEY_CONTROLLER_SUFFIX_ALLOWED_SESSION_THEME_PRESET_IDS,
            new HashSet<>(presetIds))
        .apply();
  }

  public boolean isSessionThemePresetIdAllowListed(
      @NonNull String packageName, @NonNull String presetId) {
    return getAllowedSessionThemePresetIds(packageName).contains(presetId);
  }

  @NonNull
  public Set<String> getAllowedSessionKeyboardIds(@NonNull String packageName) {
    return getStringSet(
        KEY_CONTROLLER_PREFIX + packageName + KEY_CONTROLLER_SUFFIX_ALLOWED_SESSION_KEYBOARD_IDS);
  }

  public void setAllowedSessionKeyboardIds(
      @NonNull String packageName, @NonNull Set<String> keyboardIds) {
    mPrefs
        .edit()
        .putStringSet(
            KEY_CONTROLLER_PREFIX
                + packageName
                + KEY_CONTROLLER_SUFFIX_ALLOWED_SESSION_KEYBOARD_IDS,
            new HashSet<>(keyboardIds))
        .apply();
  }

  public boolean isSessionKeyboardIdAllowListed(
      @NonNull String packageName, @NonNull String keyboardId) {
    return getAllowedSessionKeyboardIds(packageName).contains(keyboardId);
  }

  @NonNull
  public Set<String> getAllowedSessionTargetPackages(@NonNull String packageName) {
    return getStringSet(
        KEY_CONTROLLER_PREFIX
            + packageName
            + KEY_CONTROLLER_SUFFIX_ALLOWED_SESSION_TARGET_PACKAGES);
  }

  public void setAllowedSessionTargetPackages(
      @NonNull String packageName, @NonNull Set<String> targetPackages) {
    mPrefs
        .edit()
        .putStringSet(
            KEY_CONTROLLER_PREFIX
                + packageName
                + KEY_CONTROLLER_SUFFIX_ALLOWED_SESSION_TARGET_PACKAGES,
            new HashSet<>(targetPackages))
        .apply();
  }

  public boolean isSessionTargetPackageAllowListed(
      @NonNull String packageName, @NonNull String targetPackageName) {
    return getAllowedSessionTargetPackages(packageName).contains(targetPackageName);
  }

  @NonNull
  private Set<String> getStringSet(@NonNull String key) {
    final Set<String> values = mPrefs.getStringSet(key, null);
    if (values == null || values.isEmpty()) return Collections.emptySet();
    return new HashSet<>(values);
  }
}
