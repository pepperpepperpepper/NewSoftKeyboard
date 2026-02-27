package wtf.uhoh.newsoftkeyboard.app.api;

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.api.KeyboardApiContract;
import wtf.uhoh.newsoftkeyboard.app.ime.ImeServiceBase;

final class KeyboardApiSessionOverridesHandler {

  private KeyboardApiSessionOverridesHandler() {}

  @NonNull
  static Bundle setSessionPreset(
      @NonNull KeyboardApiPrefs prefs, @NonNull String callingPackage, @NonNull Bundle extras) {
    final KeyboardApiRateLimiter.Decision uiDecision =
        KeyboardApiRateLimiter.checkUiAction(callingPackage);
    if (!uiDecision.allowed) {
      final Bundle out =
          KeyboardApiCallSupport.error(
              KeyboardApiContract.ERR_RATE_LIMITED, "UI action rate limited");
      out.putLong(KeyboardApiContract.EXTRA_RETRY_AFTER_MS, uiDecision.retryAfterMs);
      return out;
    }

    final String presetIdRaw = extras.getString(KeyboardApiContract.EXTRA_SESSION_PRESET_ID);
    final String presetId = presetIdRaw == null ? null : presetIdRaw.trim();
    if (presetId == null || presetId.isEmpty()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "session_preset_id required");
    }
    if (presetId.length() > 256) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "session_preset_id too long");
    }
    if (!prefs.isSessionPresetIdAllowListed(callingPackage, presetId)) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_SCOPE_DENIED, "session_preset_id not allow-listed");
    }

    return KeyboardApiCallSupport.runOnMainThreadBlocking(
        () -> {
          final ImeServiceBase ime = ImeServiceBase.getInstance();
          if (ime == null) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_IME_NOT_ACTIVE, "IME not active");
          }

          if (!isAllowedSessionOverrideCaller(ime, prefs, callingPackage)) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_CALLER_NOT_ALLOWED,
                "Caller must be focused app or an allow-listed automation controller");
          }
          final var api = ime.getProgrammableApiController();
          if (api.isInPasswordFieldForProgrammableApi() || ime.suggest().isIncognitoMode()) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_DISALLOWED_CONTEXT, "Disallowed in secure context");
          }
          if (!api.setSessionPresetForProgrammableApi(presetId)) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_BAD_ARGUMENTS, "Unknown session_preset_id");
          }

          final Bundle out = KeyboardApiCallSupport.ok();
          out.putString(KeyboardApiContract.EXTRA_SESSION_PRESET_ID, presetId);
          return out;
        });
  }

  @NonNull
  static Bundle setSessionThemePreset(
      @NonNull KeyboardApiPrefs prefs, @NonNull String callingPackage, @NonNull Bundle extras) {
    if (!prefs.isHighRiskActionsEnabled()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_HIGH_RISK_DISABLED, "High-risk actions disabled");
    }

    final KeyboardApiRateLimiter.Decision uiDecision =
        KeyboardApiRateLimiter.checkUiAction(callingPackage);
    if (!uiDecision.allowed) {
      final Bundle out =
          KeyboardApiCallSupport.error(
              KeyboardApiContract.ERR_RATE_LIMITED, "UI action rate limited");
      out.putLong(KeyboardApiContract.EXTRA_RETRY_AFTER_MS, uiDecision.retryAfterMs);
      return out;
    }

    final String presetIdRaw = extras.getString(KeyboardApiContract.EXTRA_SESSION_THEME_PRESET_ID);
    final String presetId = presetIdRaw == null ? null : presetIdRaw.trim();
    if (presetId == null || presetId.isEmpty()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "session_theme_preset_id required");
    }
    if (presetId.length() > 256) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "session_theme_preset_id too long");
    }
    if (!prefs.isSessionThemePresetIdAllowListed(callingPackage, presetId)) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_SCOPE_DENIED, "session_theme_preset_id not allow-listed");
    }

    return KeyboardApiCallSupport.runOnMainThreadBlocking(
        () -> {
          final ImeServiceBase ime = ImeServiceBase.getInstance();
          if (ime == null) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_IME_NOT_ACTIVE, "IME not active");
          }

          if (!isAllowedSessionOverrideCaller(ime, prefs, callingPackage)) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_CALLER_NOT_ALLOWED,
                "Caller must be focused app or an allow-listed automation controller");
          }
          final var api = ime.getProgrammableApiController();
          if (api.isInPasswordFieldForProgrammableApi() || ime.suggest().isIncognitoMode()) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_DISALLOWED_CONTEXT, "Disallowed in secure context");
          }
          if (!api.setSessionThemePresetForProgrammableApi(presetId)) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_BAD_ARGUMENTS, "Unknown session_theme_preset_id");
          }

          final Bundle out = KeyboardApiCallSupport.ok();
          out.putString(KeyboardApiContract.EXTRA_SESSION_THEME_PRESET_ID, presetId);
          return out;
        });
  }

  @NonNull
  static Bundle setSessionKeyboardId(
      @NonNull KeyboardApiPrefs prefs, @NonNull String callingPackage, @NonNull Bundle extras) {
    if (!prefs.isHighRiskActionsEnabled()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_HIGH_RISK_DISABLED, "High-risk actions disabled");
    }

    final KeyboardApiRateLimiter.Decision uiDecision =
        KeyboardApiRateLimiter.checkUiAction(callingPackage);
    if (!uiDecision.allowed) {
      final Bundle out =
          KeyboardApiCallSupport.error(
              KeyboardApiContract.ERR_RATE_LIMITED, "UI action rate limited");
      out.putLong(KeyboardApiContract.EXTRA_RETRY_AFTER_MS, uiDecision.retryAfterMs);
      return out;
    }

    final String keyboardIdRaw = extras.getString(KeyboardApiContract.EXTRA_KEYBOARD_ID);
    final String keyboardId = keyboardIdRaw == null ? null : keyboardIdRaw.trim();
    if (keyboardId == null || keyboardId.isEmpty()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "keyboard_id required");
    }
    if (keyboardId.length() > 256) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "keyboard_id too long");
    }
    if (!prefs.isSessionKeyboardIdAllowListed(callingPackage, keyboardId)) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_SCOPE_DENIED, "keyboard_id not allow-listed");
    }

    return KeyboardApiCallSupport.runOnMainThreadBlocking(
        () -> {
          final ImeServiceBase ime = ImeServiceBase.getInstance();
          if (ime == null) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_IME_NOT_ACTIVE, "IME not active");
          }

          if (!isAllowedSessionOverrideCaller(ime, prefs, callingPackage)) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_CALLER_NOT_ALLOWED,
                "Caller must be focused app or an allow-listed automation controller");
          }
          final var api = ime.getProgrammableApiController();
          if (api.isInPasswordFieldForProgrammableApi() || ime.suggest().isIncognitoMode()) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_DISALLOWED_CONTEXT, "Disallowed in secure context");
          }
          if (!api.setSessionKeyboardIdForProgrammableApi(keyboardId)) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_BAD_ARGUMENTS, "Unknown keyboard_id");
          }

          final Bundle out = KeyboardApiCallSupport.ok();
          out.putString(KeyboardApiContract.EXTRA_KEYBOARD_ID, keyboardId);
          return out;
        });
  }

  @NonNull
  static Bundle clearSessionOverrides(
      @NonNull KeyboardApiPrefs prefs, @NonNull String callingPackage) {
    return KeyboardApiCallSupport.runOnMainThreadBlocking(
        () -> {
          final ImeServiceBase ime = ImeServiceBase.getInstance();
          if (ime == null) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_IME_NOT_ACTIVE, "IME not active");
          }

          if (!isAllowedSessionOverrideCaller(ime, prefs, callingPackage)) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_CALLER_NOT_ALLOWED,
                "Caller must be focused app or an allow-listed automation controller");
          }

          ime.getProgrammableApiController().clearSessionOverridesForProgrammableApi();
          return KeyboardApiCallSupport.ok();
        });
  }

  private static boolean isAllowedSessionOverrideCaller(
      @NonNull ImeServiceBase ime,
      @NonNull KeyboardApiPrefs prefs,
      @NonNull String callingPackage) {
    if (isFocusedAppSessionOverrideCaller(ime, callingPackage)) return true;
    return isAutomationControllerSessionOverrideCaller(ime, prefs, callingPackage);
  }

  private static boolean isFocusedAppSessionOverrideCaller(
      @NonNull ImeServiceBase ime, @NonNull String callingPackage) {
    final String current =
        ime.getProgrammableApiController().getCurrentEditorPackageNameForProgrammableApi();
    final String caller = callingPackage.trim();
    return current != null && !caller.isEmpty() && current.trim().equals(caller);
  }

  private static boolean isAutomationControllerSessionOverrideCaller(
      @NonNull ImeServiceBase ime,
      @NonNull KeyboardApiPrefs prefs,
      @NonNull String callingPackage) {
    if (!prefs.isHighRiskActionsEnabled()) return false;
    if (!prefs.isAutomationControllersEnabled()) return false;
    final String caller = callingPackage.trim();
    if (caller.isEmpty()) return false;
    final String current =
        ime.getProgrammableApiController().getCurrentEditorPackageNameForProgrammableApi();
    final String target = current == null ? null : current.trim();
    if (target == null || target.isEmpty()) return false;
    return prefs.isSessionTargetPackageAllowListed(caller, target);
  }
}
