package wtf.uhoh.newsoftkeyboard.app.api;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.UserManagerCompat;
import com.anysoftkeyboard.api.KeyboardApiContract;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import wtf.uhoh.newsoftkeyboard.app.ime.ImeServiceBase;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;

public class KeyboardApiProvider extends ContentProvider {

  private static final String TAG = "KeyboardApiProvider";

  @Nullable private KeyboardApiPrefs mPrefs;
  @Nullable private KeyboardApiPairingStore mPairingStore;
  @Nullable private KeyboardApiAuditLogStore mAuditLog;

  @Override
  public boolean onCreate() {
    final Context context = getContext();
    if (context == null) return false;
    mPrefs = new KeyboardApiPrefs(context);
    mPairingStore = new KeyboardApiPairingStore(context);
    mAuditLog = new KeyboardApiAuditLogStore(context);
    // Defense-in-depth: if the provider component was enabled externally while the API toggle is
    // off, disable the component as soon as it's loaded.
    if (mPrefs != null && !mPrefs.isApiEnabled()) {
      KeyboardApiComponentController.setProviderEnabled(context, false);
    }
    return true;
  }

  @Override
  public @Nullable Bundle call(
      @NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
    final Context context = getContext();
    final KeyboardApiPrefs prefs = mPrefs;
    final KeyboardApiPairingStore pairingStore = mPairingStore;
    final KeyboardApiAuditLogStore auditLogStore = mAuditLog;
    if (context == null || prefs == null || pairingStore == null || auditLogStore == null) {
      return error(KeyboardApiContract.ERR_INTERNAL, "No context");
    }

    final Bundle safeExtras = extras == null ? new Bundle() : extras;
    final CallerIdentity callerForAudit = resolveCallerIdentity(context, safeExtras);
    final String callingPackageForAudit = callerForAudit.callingPackage;

    Bundle result = error(KeyboardApiContract.ERR_INTERNAL, "Internal error");
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
          && !UserManagerCompat.isUserUnlocked(context)) {
        result = error(KeyboardApiContract.ERR_USER_LOCKED, "User locked");
        return result;
      }

      if (!prefs.isApiEnabled()) {
        result = error(KeyboardApiContract.ERR_DISABLED, "API disabled");
        return result;
      }

      if (KeyboardApiContract.METHOD_REQUEST_PAIRING.equals(method)) {
        if (!callerForAudit.allowed) {
          result = error(callerForAudit.errorCode, callerForAudit.errorMessage);
        } else {
          result =
              KeyboardApiPairingHandler.requestPairing(
                  context, prefs, pairingStore, callerForAudit.callingPackage, safeExtras);
        }
        return result;
      } else if (KeyboardApiContract.METHOD_GET_PAIRING_STATUS.equals(method)) {
        if (!callerForAudit.allowed) {
          result = error(callerForAudit.errorCode, callerForAudit.errorMessage);
        } else {
          result =
              KeyboardApiPairingHandler.getPairingStatus(
                  context, prefs, pairingStore, callerForAudit.callingPackage);
        }
        return result;
      }

      final Authorization authorization = authorize(context, prefs, method, extras);
      if (!authorization.allowed) {
        result = error(authorization.errorCode, authorization.errorMessage);
        return result;
      }

      final KeyboardApiRateLimiter.Decision rateDecision =
          KeyboardApiRateLimiter.check(authorization.callingPackage);
      if (!rateDecision.allowed) {
        result = error(KeyboardApiContract.ERR_RATE_LIMITED, "Rate limited");
        result.putLong(KeyboardApiContract.EXTRA_RETRY_AFTER_MS, rateDecision.retryAfterMs);
        return result;
      }

      try {
        result = dispatch(context, authorization, method, arg, safeExtras);
        return result;
      } catch (SecurityException e) {
        result = error(KeyboardApiContract.ERR_SCOPE_DENIED, "Scope denied");
        return result;
      } catch (RuntimeException e) {
        Logger.w(
            TAG,
            "Unhandled exception for method '%s' (caller %s)",
            method,
            authorization.callingPackage);
        Logger.w(TAG, "Unhandled exception", e);
        result = error(KeyboardApiContract.ERR_INTERNAL, "Internal error");
        return result;
      }
    } finally {
      auditLogStore.record(callingPackageForAudit, method, errorCodeFromResult(result));
    }
  }

  @NonNull
  private Bundle dispatch(
      @NonNull Context context,
      @NonNull Authorization authorization,
      @NonNull String method,
      @Nullable String arg,
      @NonNull Bundle extras) {
    switch (method) {
      case KeyboardApiContract.METHOD_GET_API_VERSION:
        requireScope(authorization, KeyboardApiContract.SCOPE_CAPABILITIES_READ);
        final Bundle versionOut = ok();
        versionOut.putInt(KeyboardApiContract.EXTRA_API_VERSION, KeyboardApiContract.API_VERSION);
        return versionOut;
      case KeyboardApiContract.METHOD_PING:
        requireScope(authorization, KeyboardApiContract.SCOPE_CAPABILITIES_READ);
        return ok();
      case KeyboardApiContract.METHOD_GET_CAPABILITIES:
        requireScope(authorization, KeyboardApiContract.SCOPE_CAPABILITIES_READ);
        return KeyboardApiCapabilitiesHandler.getCapabilities(context);
      case KeyboardApiContract.METHOD_GET_KEYBOARD_STATUS:
        requireScope(authorization, KeyboardApiContract.SCOPE_STATUS_READ);
        return KeyboardApiStatusHandler.getKeyboardStatus();
      case KeyboardApiContract.METHOD_GET_PREFERENCE:
        return KeyboardApiPreferencesHandler.getPreference(context, authorization.scopes, extras);
      case KeyboardApiContract.METHOD_SET_PREFERENCE:
        return KeyboardApiPreferencesHandler.setPreference(context, authorization.scopes, extras);
      case KeyboardApiContract.METHOD_SET_PREFERENCES:
        return KeyboardApiPreferencesHandler.setPreferences(context, authorization.scopes, extras);
      case KeyboardApiContract.METHOD_SET_SECRET:
        requireScope(authorization, KeyboardApiContract.SCOPE_SECRETS_WRITE);
        return KeyboardApiSecretsHandler.setSecret(context, extras);
      case KeyboardApiContract.METHOD_GET_SECRET_STATUS:
        requireScope(authorization, KeyboardApiContract.SCOPE_SECRETS_STATUS);
        return KeyboardApiSecretsHandler.getSecretStatus(context, extras);
      case KeyboardApiContract.METHOD_TOGGLE_INCOGNITO:
        requireScope(authorization, KeyboardApiContract.SCOPE_ACTION_INCOGNITO);
        return KeyboardApiImeActionsHandler.toggleIncognito(extras);
      case KeyboardApiContract.METHOD_SWITCH_LANGUAGE:
        requireScope(authorization, KeyboardApiContract.SCOPE_ACTION_SWITCH_LANGUAGE);
        return KeyboardApiImeActionsHandler.switchLanguage(extras);
      case KeyboardApiContract.METHOD_SWITCH_KEYBOARD_MODE:
        requireScope(authorization, KeyboardApiContract.SCOPE_ACTION_SWITCH_KEYBOARD_MODE);
        return KeyboardApiImeActionsHandler.switchKeyboardMode(extras);
      case KeyboardApiContract.METHOD_SEND_NAVIGATION_KEY:
        requireScope(authorization, KeyboardApiContract.SCOPE_IME_INJECT_NAVIGATION);
        {
          final KeyboardApiPrefs prefs = mPrefs;
          if (prefs == null) return error(KeyboardApiContract.ERR_INTERNAL, "No prefs");
          return KeyboardApiImeActionsHandler.sendNavigationKey(prefs, extras);
        }
      case KeyboardApiContract.METHOD_SEND_TAB:
        requireScope(authorization, KeyboardApiContract.SCOPE_IME_INJECT_NAVIGATION);
        {
          final KeyboardApiPrefs prefs = mPrefs;
          if (prefs == null) return error(KeyboardApiContract.ERR_INTERNAL, "No prefs");
          return KeyboardApiImeActionsHandler.sendTab(prefs);
        }
      case KeyboardApiContract.METHOD_SEND_ESCAPE:
        requireScope(authorization, KeyboardApiContract.SCOPE_IME_INJECT_NAVIGATION);
        {
          final KeyboardApiPrefs prefs = mPrefs;
          if (prefs == null) return error(KeyboardApiContract.ERR_INTERNAL, "No prefs");
          return KeyboardApiImeActionsHandler.sendEscape(prefs);
        }
      case KeyboardApiContract.METHOD_CLIPBOARD_COPY:
        requireScope(authorization, KeyboardApiContract.SCOPE_IME_INJECT_CLIPBOARD);
        {
          final KeyboardApiPrefs prefs = mPrefs;
          if (prefs == null) return error(KeyboardApiContract.ERR_INTERNAL, "No prefs");
          return KeyboardApiImeActionsHandler.clipboardCopy(prefs);
        }
      case KeyboardApiContract.METHOD_CLIPBOARD_CUT:
        requireScope(authorization, KeyboardApiContract.SCOPE_IME_INJECT_CLIPBOARD);
        {
          final KeyboardApiPrefs prefs = mPrefs;
          if (prefs == null) return error(KeyboardApiContract.ERR_INTERNAL, "No prefs");
          return KeyboardApiImeActionsHandler.clipboardCut(prefs);
        }
      case KeyboardApiContract.METHOD_CLIPBOARD_PASTE:
        requireScope(authorization, KeyboardApiContract.SCOPE_IME_INJECT_CLIPBOARD);
        {
          final KeyboardApiPrefs prefs = mPrefs;
          if (prefs == null) return error(KeyboardApiContract.ERR_INTERNAL, "No prefs");
          return KeyboardApiImeActionsHandler.clipboardPaste(prefs);
        }
      case KeyboardApiContract.METHOD_CLIPBOARD_SELECT_ALL:
        requireScope(authorization, KeyboardApiContract.SCOPE_IME_INJECT_CLIPBOARD);
        {
          final KeyboardApiPrefs prefs = mPrefs;
          if (prefs == null) return error(KeyboardApiContract.ERR_INTERNAL, "No prefs");
          return KeyboardApiImeActionsHandler.clipboardSelectAll(prefs);
        }
      case KeyboardApiContract.METHOD_UNDO:
        requireScope(authorization, KeyboardApiContract.SCOPE_IME_INJECT_CLIPBOARD);
        {
          final KeyboardApiPrefs prefs = mPrefs;
          if (prefs == null) return error(KeyboardApiContract.ERR_INTERNAL, "No prefs");
          return KeyboardApiImeActionsHandler.undo(prefs);
        }
      case KeyboardApiContract.METHOD_REDO:
        requireScope(authorization, KeyboardApiContract.SCOPE_IME_INJECT_CLIPBOARD);
        {
          final KeyboardApiPrefs prefs = mPrefs;
          if (prefs == null) return error(KeyboardApiContract.ERR_INTERNAL, "No prefs");
          return KeyboardApiImeActionsHandler.redo(prefs);
        }
      case KeyboardApiContract.METHOD_RUN_SNIPPET:
        requireScope(authorization, KeyboardApiContract.SCOPE_IME_INJECT_SNIPPETS);
        {
          final KeyboardApiPrefs prefs = mPrefs;
          if (prefs == null) return error(KeyboardApiContract.ERR_INTERNAL, "No prefs");
          return KeyboardApiImeActionsHandler.runSnippet(prefs, extras);
        }
      case KeyboardApiContract.METHOD_CLEAR_LEARNING_DATA:
        requireScope(authorization, KeyboardApiContract.SCOPE_ACTION_DATA_CLEAR);
        return KeyboardApiMaintenanceHandler.clearLearningData(context);
      case KeyboardApiContract.METHOD_CLEAR_QUICK_TEXT_HISTORY:
        requireScope(authorization, KeyboardApiContract.SCOPE_ACTION_DATA_CLEAR);
        return KeyboardApiMaintenanceHandler.clearQuickTextHistory(context);
      case KeyboardApiContract.METHOD_CLEAR_CLIPBOARD_HISTORY:
        requireScope(authorization, KeyboardApiContract.SCOPE_ACTION_CLIPBOARD_CLEAR);
        return KeyboardApiMaintenanceHandler.clearClipboardHistory(context);
      case KeyboardApiContract.METHOD_OPEN_MEDIA_INSERTION_UI:
        requireScope(authorization, KeyboardApiContract.SCOPE_ACTION_MEDIA_INSERTION_OPEN);
        return KeyboardApiUiActionsHandler.openMediaInsertionUi(authorization.callingPackage);
      case KeyboardApiContract.METHOD_OPEN_SETTINGS:
        requireScope(authorization, KeyboardApiContract.SCOPE_ACTION_OPEN_SETTINGS);
        return KeyboardApiUiActionsHandler.openSettings(
            context, authorization.callingPackage, extras);
      case KeyboardApiContract.METHOD_RELOAD_SETTINGS:
        requireScope(authorization, KeyboardApiContract.SCOPE_ACTION_RELOAD_SETTINGS);
        return reloadSettings();
      case KeyboardApiContract.METHOD_GET_AUDIT_LOG:
        requireScope(authorization, KeyboardApiContract.SCOPE_AUDIT_READ);
        {
          final KeyboardApiAuditLogStore auditLogStore = mAuditLog;
          if (auditLogStore == null) return error(KeyboardApiContract.ERR_INTERNAL, "No audit log");
          return KeyboardApiAuditLogHandler.getAuditLog(
              auditLogStore, authorization.callingPackage, extras);
        }
      case KeyboardApiContract.METHOD_CLEAR_AUDIT_LOG:
        requireScope(authorization, KeyboardApiContract.SCOPE_AUDIT_CLEAR);
        {
          final KeyboardApiAuditLogStore auditLogStore = mAuditLog;
          if (auditLogStore == null) return error(KeyboardApiContract.ERR_INTERNAL, "No audit log");
          return KeyboardApiAuditLogHandler.clearAuditLog(
              auditLogStore, authorization.callingPackage);
        }
      case KeyboardApiContract.METHOD_SET_SESSION_PRESET:
        requireScope(authorization, KeyboardApiContract.SCOPE_CONTEXT_SESSION_PRESET);
        {
          final KeyboardApiPrefs prefs = mPrefs;
          if (prefs == null) return error(KeyboardApiContract.ERR_INTERNAL, "No prefs");
          return KeyboardApiSessionOverridesHandler.setSessionPreset(
              prefs, authorization.callingPackage, extras);
        }
      case KeyboardApiContract.METHOD_SET_SESSION_THEME_PRESET:
        requireScope(authorization, KeyboardApiContract.SCOPE_CONTEXT_SESSION_THEME);
        {
          final KeyboardApiPrefs prefs = mPrefs;
          if (prefs == null) return error(KeyboardApiContract.ERR_INTERNAL, "No prefs");
          return KeyboardApiSessionOverridesHandler.setSessionThemePreset(
              prefs, authorization.callingPackage, extras);
        }
      case KeyboardApiContract.METHOD_SET_SESSION_KEYBOARD_ID:
        requireScope(authorization, KeyboardApiContract.SCOPE_CONTEXT_SESSION_LAYOUT);
        {
          final KeyboardApiPrefs prefs = mPrefs;
          if (prefs == null) return error(KeyboardApiContract.ERR_INTERNAL, "No prefs");
          return KeyboardApiSessionOverridesHandler.setSessionKeyboardId(
              prefs, authorization.callingPackage, extras);
        }
      case KeyboardApiContract.METHOD_CLEAR_SESSION_OVERRIDES:
        if (!authorization.scopes.contains(KeyboardApiContract.SCOPE_CONTEXT_SESSION_PRESET)
            && !authorization.scopes.contains(KeyboardApiContract.SCOPE_CONTEXT_SESSION_THEME)
            && !authorization.scopes.contains(KeyboardApiContract.SCOPE_CONTEXT_SESSION_LAYOUT)) {
          throw new SecurityException("Missing session override scope");
        }
        {
          final KeyboardApiPrefs prefs = mPrefs;
          if (prefs == null) return error(KeyboardApiContract.ERR_INTERNAL, "No prefs");
          return KeyboardApiSessionOverridesHandler.clearSessionOverrides(
              prefs, authorization.callingPackage);
        }
      case KeyboardApiContract.METHOD_RUN_MACRO:
        requireScope(authorization, KeyboardApiContract.SCOPE_AUTOMATION_MACRO);
        return runMacro(context, authorization, extras);
      default:
        return error(KeyboardApiContract.ERR_UNSUPPORTED_METHOD, "Unsupported method");
    }
  }

  @NonNull
  private Bundle reloadSettings() {
    // Preference writes already notify listeners. This method is currently a no-op but is kept as a
    // stable hook for future behavior changes.
    final Bundle out = ok();
    out.putBoolean(KeyboardApiContract.EXTRA_IME_ACTIVE, ImeServiceBase.getInstance() != null);
    return out;
  }

  /**
   * Runs a bounded sequence of eligible verbs, re-entering {@link #dispatch} for each step so every
   * one re-checks its own scope and per-method guards (password field, high-risk toggle, incognito,
   * IME-active). Adds no capability beyond issuing the same calls individually. See {@code
   * keyboard_api_macro_plan.md}.
   */
  @NonNull
  private Bundle runMacro(
      @NonNull Context context, @NonNull Authorization authorization, @NonNull Bundle extras) {
    final java.util.List<KeyboardApiMacroHandler.Step> steps;
    try {
      steps = KeyboardApiMacroHandler.parseSteps(extras);
    } catch (KeyboardApiMacroHandler.MacroParseException e) {
      return error(e.errorCode, String.valueOf(e.getMessage()));
    }

    // Bill the whole batch to the rate limiter up front; nothing runs if it can't be afforded.
    final KeyboardApiRateLimiter.Decision rateDecision =
        KeyboardApiRateLimiter.check(authorization.callingPackage, steps.size());
    if (!rateDecision.allowed) {
      final Bundle limited = error(KeyboardApiContract.ERR_RATE_LIMITED, "Rate limited");
      limited.putLong(KeyboardApiContract.EXTRA_RETRY_AFTER_MS, rateDecision.retryAfterMs);
      return limited;
    }

    final boolean stopOnError =
        extras.getBoolean(KeyboardApiContract.EXTRA_MACRO_STOP_ON_ERROR, true);
    final KeyboardApiAuditLogStore auditLogStore = mAuditLog;

    final org.json.JSONArray results = new org.json.JSONArray();
    int firstErrorCode = KeyboardApiContract.ERR_OK;
    boolean halted = false;

    for (int i = 0; i < steps.size(); i++) {
      final KeyboardApiMacroHandler.Step step = steps.get(i);

      if (halted) {
        results.put(stepSkipped(i));
        continue;
      }

      if (!KeyboardApiMacroHandler.isEligible(step.method)) {
        if (auditLogStore != null) {
          auditLogStore.record(
              authorization.callingPackage,
              "runMacro:" + step.method,
              KeyboardApiContract.ERR_MACRO_STEP_NOT_ALLOWED);
        }
        results.put(stepResult(i, KeyboardApiContract.ERR_MACRO_STEP_NOT_ALLOWED));
        if (firstErrorCode == KeyboardApiContract.ERR_OK) {
          firstErrorCode = KeyboardApiContract.ERR_MACRO_STEP_NOT_ALLOWED;
        }
        if (stopOnError) halted = true;
        continue;
      }

      int code;
      try {
        final Bundle stepResult = dispatch(context, authorization, step.method, null, step.extras);
        code = errorCodeFromResult(stepResult);
      } catch (SecurityException e) {
        code = KeyboardApiContract.ERR_SCOPE_DENIED;
      } catch (RuntimeException e) {
        Logger.w(TAG, "Macro step '%s' threw", step.method);
        Logger.w(TAG, "Macro step exception", e);
        code = KeyboardApiContract.ERR_INTERNAL;
      }

      if (auditLogStore != null) {
        auditLogStore.record(authorization.callingPackage, "runMacro:" + step.method, code);
      }
      results.put(stepResult(i, code));

      if (code != KeyboardApiContract.ERR_OK) {
        if (firstErrorCode == KeyboardApiContract.ERR_OK) firstErrorCode = code;
        if (stopOnError) halted = true;
      }
    }

    final Bundle out =
        firstErrorCode == KeyboardApiContract.ERR_OK
            ? ok()
            : error(firstErrorCode, "Macro step failed");
    out.putString(KeyboardApiContract.EXTRA_MACRO_RESULTS, results.toString());
    return out;
  }

  @NonNull
  private static org.json.JSONObject stepResult(int index, int code) {
    final org.json.JSONObject entry = new org.json.JSONObject();
    try {
      entry.put("i", index);
      entry.put("ok", code == KeyboardApiContract.ERR_OK);
      if (code != KeyboardApiContract.ERR_OK) entry.put("error_code", code);
    } catch (org.json.JSONException ignored) {
      // Keys are constant and values primitive; this cannot actually throw.
    }
    return entry;
  }

  @NonNull
  private static org.json.JSONObject stepSkipped(int index) {
    final org.json.JSONObject entry = new org.json.JSONObject();
    try {
      entry.put("i", index);
      entry.put("skipped", true);
    } catch (org.json.JSONException ignored) {
      // Constant keys / primitive values.
    }
    return entry;
  }

  private static void requireScope(@NonNull Authorization authorization, @NonNull String scope) {
    if (!authorization.scopes.contains(scope)) {
      throw new SecurityException("Missing scope " + scope);
    }
  }

  @NonNull
  static Bundle ok() {
    return KeyboardApiCallSupport.ok();
  }

  @NonNull
  static Bundle error(int code, @NonNull String message) {
    return KeyboardApiCallSupport.error(code, message);
  }

  private static int errorCodeFromResult(@Nullable Bundle result) {
    if (result == null) return KeyboardApiContract.ERR_INTERNAL;
    final boolean ok = result.getBoolean(KeyboardApiContract.EXTRA_OK, false);
    if (ok) return KeyboardApiContract.ERR_OK;
    return result.getInt(KeyboardApiContract.EXTRA_ERROR_CODE, KeyboardApiContract.ERR_INTERNAL);
  }

  private static final class Authorization {
    final boolean allowed;
    @NonNull final String callingPackage;
    @NonNull final Set<String> scopes;
    final int errorCode;
    @NonNull final String errorMessage;

    Authorization(
        boolean allowed,
        @NonNull String callingPackage,
        @NonNull Set<String> scopes,
        int errorCode,
        @NonNull String errorMessage) {
      this.allowed = allowed;
      this.callingPackage = callingPackage;
      this.scopes = scopes;
      this.errorCode = errorCode;
      this.errorMessage = errorMessage;
    }
  }

  private static final class CallerIdentity {
    final boolean allowed;
    @NonNull final String callingPackage;
    final int errorCode;
    @NonNull final String errorMessage;

    CallerIdentity(
        boolean allowed,
        @NonNull String callingPackage,
        int errorCode,
        @NonNull String errorMessage) {
      this.allowed = allowed;
      this.callingPackage = callingPackage;
      this.errorCode = errorCode;
      this.errorMessage = errorMessage;
    }
  }

  @NonNull
  private static CallerIdentity resolveCallerIdentity(
      @NonNull Context context, @Nullable Bundle extras) {
    final int uid = Binder.getCallingUid();
    if (uid == Process.myUid()) {
      return new CallerIdentity(true, context.getPackageName(), KeyboardApiContract.ERR_OK, "");
    }

    final String[] packages = context.getPackageManager().getPackagesForUid(uid);
    if (packages == null || packages.length == 0) {
      return new CallerIdentity(
          false, "unknown", KeyboardApiContract.ERR_CALLER_NOT_ALLOWED, "Unknown caller");
    }

    final String effectivePackage;
    if (packages.length == 1) {
      effectivePackage = packages[0];
    } else {
      final String requested =
          extras == null ? null : extras.getString(KeyboardApiContract.EXTRA_CALLER_PACKAGE);
      if (requested == null) {
        return new CallerIdentity(
            false,
            packages[0],
            KeyboardApiContract.ERR_CALLER_UNCLEAR,
            "Multiple packages for UID; specify caller_package");
      }
      boolean found = false;
      for (String p : packages) {
        if (Objects.equals(p, requested)) {
          found = true;
          break;
        }
      }
      if (!found) {
        return new CallerIdentity(
            false,
            packages[0],
            KeyboardApiContract.ERR_CALLER_NOT_ALLOWED,
            "caller_package does not match UID");
      }
      effectivePackage = requested;
    }

    return new CallerIdentity(true, effectivePackage, KeyboardApiContract.ERR_OK, "");
  }

  @NonNull
  private static Authorization authorize(
      @NonNull Context context,
      @NonNull KeyboardApiPrefs prefs,
      @NonNull String method,
      @Nullable Bundle extras) {
    final int uid = Binder.getCallingUid();
    if (uid == Process.myUid()) {
      // Internal calls are trusted, but still go through scope checks to avoid accidental widening.
      final HashSet<String> internalScopes =
          new HashSet<>(
              Arrays.asList(
                  KeyboardApiContract.SCOPE_CAPABILITIES_READ,
                  KeyboardApiContract.SCOPE_STATUS_READ,
                  KeyboardApiContract.SCOPE_PREFS_READ,
                  KeyboardApiContract.SCOPE_PREFS_WRITE_EFFECTS,
                  KeyboardApiContract.SCOPE_PREFS_WRITE_TYPING,
                  KeyboardApiContract.SCOPE_PREFS_WRITE_GESTURES,
                  KeyboardApiContract.SCOPE_PREFS_WRITE_UI,
                  KeyboardApiContract.SCOPE_PREFS_WRITE_CLIPBOARD,
                  KeyboardApiContract.SCOPE_PREFS_WRITE_VOICE,
                  KeyboardApiContract.SCOPE_SECRETS_WRITE,
                  KeyboardApiContract.SCOPE_SECRETS_STATUS,
                  KeyboardApiContract.SCOPE_ACTION_OPEN_SETTINGS,
                  KeyboardApiContract.SCOPE_ACTION_RELOAD_SETTINGS,
                  KeyboardApiContract.SCOPE_ACTION_MEDIA_INSERTION_OPEN,
                  KeyboardApiContract.SCOPE_ACTION_DATA_CLEAR,
                  KeyboardApiContract.SCOPE_ACTION_CLIPBOARD_CLEAR,
                  KeyboardApiContract.SCOPE_ACTION_INCOGNITO,
                  KeyboardApiContract.SCOPE_ACTION_SWITCH_LANGUAGE,
                  KeyboardApiContract.SCOPE_ACTION_SWITCH_KEYBOARD_MODE,
                  KeyboardApiContract.SCOPE_IME_INJECT_NAVIGATION,
                  KeyboardApiContract.SCOPE_IME_INJECT_CLIPBOARD,
                  KeyboardApiContract.SCOPE_IME_INJECT_SNIPPETS,
                  KeyboardApiContract.SCOPE_AUDIT_READ,
                  KeyboardApiContract.SCOPE_AUDIT_CLEAR,
                  KeyboardApiContract.SCOPE_CONTEXT_SESSION_PRESET,
                  KeyboardApiContract.SCOPE_CONTEXT_SESSION_THEME,
                  KeyboardApiContract.SCOPE_CONTEXT_SESSION_LAYOUT,
                  KeyboardApiContract.SCOPE_AUTOMATION_MACRO));
      return new Authorization(
          true, context.getPackageName(), internalScopes, KeyboardApiContract.ERR_OK, "");
    }

    final CallerIdentity callerIdentity = resolveCallerIdentity(context, extras);
    if (!callerIdentity.allowed) {
      return new Authorization(
          false,
          callerIdentity.callingPackage,
          Collections.emptySet(),
          callerIdentity.errorCode,
          callerIdentity.errorMessage);
    }
    final String effectivePackage = callerIdentity.callingPackage;

    if (!prefs.isPackageAllowListed(effectivePackage)) {
      return new Authorization(
          false,
          effectivePackage,
          Collections.emptySet(),
          KeyboardApiContract.ERR_CALLER_NOT_ALLOWED,
          "Caller not allow-listed");
    }

    final Set<String> allowedDigests = prefs.getControllerCertDigestsSha256(effectivePackage);
    if (allowedDigests.isEmpty()) {
      return new Authorization(
          false,
          effectivePackage,
          Collections.emptySet(),
          KeyboardApiContract.ERR_SIGNATURE_MISMATCH,
          "No allowed signatures configured");
    }

    final Set<String> actualDigests;
    try {
      actualDigests =
          KeyboardApiSignatureUtils.getSigningCertDigestsSha256(context, effectivePackage);
    } catch (PackageManager.NameNotFoundException e) {
      return new Authorization(
          false,
          effectivePackage,
          Collections.emptySet(),
          KeyboardApiContract.ERR_CALLER_NOT_ALLOWED,
          "Caller package not found");
    }

    boolean signatureMatch = false;
    for (String d : actualDigests) {
      if (allowedDigests.contains(d)) {
        signatureMatch = true;
        break;
      }
    }
    if (!signatureMatch) {
      return new Authorization(
          false,
          effectivePackage,
          Collections.emptySet(),
          KeyboardApiContract.ERR_SIGNATURE_MISMATCH,
          "Signature mismatch");
    }

    final Set<String> scopes = prefs.getControllerScopes(effectivePackage);
    if (scopes.isEmpty()) {
      return new Authorization(
          false,
          effectivePackage,
          Collections.emptySet(),
          KeyboardApiContract.ERR_SCOPE_DENIED,
          "No scopes granted");
    }

    // Fail fast if the requested method is obviously not covered by scopes.
    final String requiredScope = requiredScopeForMethod(method);
    if (requiredScope != null && !scopes.contains(requiredScope)) {
      return new Authorization(
          false, effectivePackage, scopes, KeyboardApiContract.ERR_SCOPE_DENIED, "Missing scope");
    }

    if (KeyboardApiControllerTokensStore.hasToken(context, effectivePackage)) {
      final String providedToken =
          extras == null ? null : extras.getString(KeyboardApiContract.EXTRA_CONTROLLER_TOKEN);
      if (providedToken == null || providedToken.trim().isEmpty()) {
        return new Authorization(
            false,
            effectivePackage,
            scopes,
            KeyboardApiContract.ERR_AUTH_TOKEN_REQUIRED,
            "controller_token required");
      }
      final String expectedToken =
          KeyboardApiControllerTokensStore.getToken(context, effectivePackage);
      if (!Objects.equals(expectedToken, providedToken)) {
        return new Authorization(
            false,
            effectivePackage,
            scopes,
            KeyboardApiContract.ERR_AUTH_TOKEN_INVALID,
            "controller_token invalid");
      }
    }

    return new Authorization(true, effectivePackage, scopes, KeyboardApiContract.ERR_OK, "");
  }

  @Nullable
  private static String requiredScopeForMethod(@NonNull String method) {
    switch (method) {
      case KeyboardApiContract.METHOD_GET_API_VERSION:
      case KeyboardApiContract.METHOD_PING:
      case KeyboardApiContract.METHOD_GET_CAPABILITIES:
        return KeyboardApiContract.SCOPE_CAPABILITIES_READ;
      case KeyboardApiContract.METHOD_GET_KEYBOARD_STATUS:
        return KeyboardApiContract.SCOPE_STATUS_READ;
      case KeyboardApiContract.METHOD_GET_PREFERENCE:
        return KeyboardApiContract.SCOPE_PREFS_READ;
      case KeyboardApiContract.METHOD_SET_SECRET:
        return KeyboardApiContract.SCOPE_SECRETS_WRITE;
      case KeyboardApiContract.METHOD_GET_SECRET_STATUS:
        return KeyboardApiContract.SCOPE_SECRETS_STATUS;
      case KeyboardApiContract.METHOD_TOGGLE_INCOGNITO:
        return KeyboardApiContract.SCOPE_ACTION_INCOGNITO;
      case KeyboardApiContract.METHOD_SWITCH_LANGUAGE:
        return KeyboardApiContract.SCOPE_ACTION_SWITCH_LANGUAGE;
      case KeyboardApiContract.METHOD_SWITCH_KEYBOARD_MODE:
        return KeyboardApiContract.SCOPE_ACTION_SWITCH_KEYBOARD_MODE;
      case KeyboardApiContract.METHOD_SEND_NAVIGATION_KEY:
      case KeyboardApiContract.METHOD_SEND_TAB:
      case KeyboardApiContract.METHOD_SEND_ESCAPE:
        return KeyboardApiContract.SCOPE_IME_INJECT_NAVIGATION;
      case KeyboardApiContract.METHOD_CLIPBOARD_COPY:
      case KeyboardApiContract.METHOD_CLIPBOARD_CUT:
      case KeyboardApiContract.METHOD_CLIPBOARD_PASTE:
      case KeyboardApiContract.METHOD_CLIPBOARD_SELECT_ALL:
      case KeyboardApiContract.METHOD_UNDO:
      case KeyboardApiContract.METHOD_REDO:
        return KeyboardApiContract.SCOPE_IME_INJECT_CLIPBOARD;
      case KeyboardApiContract.METHOD_RUN_SNIPPET:
        return KeyboardApiContract.SCOPE_IME_INJECT_SNIPPETS;
      case KeyboardApiContract.METHOD_CLEAR_LEARNING_DATA:
      case KeyboardApiContract.METHOD_CLEAR_QUICK_TEXT_HISTORY:
        return KeyboardApiContract.SCOPE_ACTION_DATA_CLEAR;
      case KeyboardApiContract.METHOD_CLEAR_CLIPBOARD_HISTORY:
        return KeyboardApiContract.SCOPE_ACTION_CLIPBOARD_CLEAR;
      case KeyboardApiContract.METHOD_OPEN_SETTINGS:
        return KeyboardApiContract.SCOPE_ACTION_OPEN_SETTINGS;
      case KeyboardApiContract.METHOD_RELOAD_SETTINGS:
        return KeyboardApiContract.SCOPE_ACTION_RELOAD_SETTINGS;
      case KeyboardApiContract.METHOD_OPEN_MEDIA_INSERTION_UI:
        return KeyboardApiContract.SCOPE_ACTION_MEDIA_INSERTION_OPEN;
      case KeyboardApiContract.METHOD_GET_AUDIT_LOG:
        return KeyboardApiContract.SCOPE_AUDIT_READ;
      case KeyboardApiContract.METHOD_CLEAR_AUDIT_LOG:
        return KeyboardApiContract.SCOPE_AUDIT_CLEAR;
      case KeyboardApiContract.METHOD_SET_SESSION_PRESET:
        return KeyboardApiContract.SCOPE_CONTEXT_SESSION_PRESET;
      case KeyboardApiContract.METHOD_SET_SESSION_THEME_PRESET:
        return KeyboardApiContract.SCOPE_CONTEXT_SESSION_THEME;
      case KeyboardApiContract.METHOD_SET_SESSION_KEYBOARD_ID:
        return KeyboardApiContract.SCOPE_CONTEXT_SESSION_LAYOUT;
      case KeyboardApiContract.METHOD_RUN_MACRO:
        return KeyboardApiContract.SCOPE_AUTOMATION_MACRO;
      default:
        return null;
    }
  }

  // Not a "real" content provider; we only support call().
  @Override
  public @Nullable Cursor query(
      @NonNull Uri uri,
      @Nullable String[] projection,
      @Nullable String selection,
      @Nullable String[] selectionArgs,
      @Nullable String sortOrder) {
    throw new UnsupportedOperationException("query not supported");
  }

  @Override
  public @Nullable String getType(@NonNull Uri uri) {
    throw new UnsupportedOperationException("getType not supported");
  }

  @Override
  public @Nullable Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
    throw new UnsupportedOperationException("insert not supported");
  }

  @Override
  public int delete(
      @NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
    throw new UnsupportedOperationException("delete not supported");
  }

  @Override
  public int update(
      @NonNull Uri uri,
      @Nullable ContentValues values,
      @Nullable String selection,
      @Nullable String[] selectionArgs) {
    throw new UnsupportedOperationException("update not supported");
  }
}
