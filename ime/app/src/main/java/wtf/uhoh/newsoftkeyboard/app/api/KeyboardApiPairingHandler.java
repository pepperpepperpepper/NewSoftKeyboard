package wtf.uhoh.newsoftkeyboard.app.api;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.api.KeyboardApiContract;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class KeyboardApiPairingHandler {

  private static final Set<String> SUPPORTED_GRANTABLE_SCOPES =
      Collections.unmodifiableSet(
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
                  KeyboardApiContract.SCOPE_CONTEXT_SESSION_LAYOUT)));

  private KeyboardApiPairingHandler() {}

  @NonNull
  static Bundle requestPairing(
      @NonNull Context context,
      @NonNull KeyboardApiPrefs prefs,
      @NonNull KeyboardApiPairingStore pairingStore,
      @NonNull String callingPackage,
      @NonNull Bundle extras) {
    if (prefs.isPackageAllowListed(callingPackage)) {
      final Bundle out = KeyboardApiCallSupport.ok();
      out.putString(KeyboardApiContract.EXTRA_CONTROLLER_PACKAGE, callingPackage);
      out.putString(
          KeyboardApiContract.EXTRA_PAIRING_STATUS, KeyboardApiContract.PAIRING_STATUS_APPROVED);
      out.putStringArrayList(
          KeyboardApiContract.EXTRA_APPROVED_SCOPES,
          new ArrayList<>(prefs.getControllerScopes(callingPackage)));
      return out;
    }

    final ArrayList<String> requested =
        extras.getStringArrayList(KeyboardApiContract.EXTRA_REQUESTED_SCOPES);
    final HashSet<String> requestedScopes = new HashSet<>();
    if (requested != null) requestedScopes.addAll(requested);

    if (requestedScopes.isEmpty()) {
      requestedScopes.add(KeyboardApiContract.SCOPE_CAPABILITIES_READ);
    }

    requestedScopes.retainAll(SUPPORTED_GRANTABLE_SCOPES);
    if (requestedScopes.isEmpty()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "No supported requested_scopes");
    }

    final KeyboardApiPairingStore.RequestDecision decision =
        pairingStore.recordRequest(callingPackage, requestedScopes, System.currentTimeMillis());
    if (!decision.allowed) {
      final Bundle out =
          KeyboardApiCallSupport.error(
              KeyboardApiContract.ERR_RATE_LIMITED, String.valueOf(decision.message));
      out.putLong(KeyboardApiContract.EXTRA_RETRY_AFTER_MS, decision.retryAfterMs);
      return out;
    }

    KeyboardApiPairingNotifier.maybeNotifyNewRequest(context, pairingStore, callingPackage);

    final Bundle out = KeyboardApiCallSupport.ok();
    out.putString(KeyboardApiContract.EXTRA_CONTROLLER_PACKAGE, callingPackage);
    out.putString(
        KeyboardApiContract.EXTRA_PAIRING_STATUS, KeyboardApiContract.PAIRING_STATUS_PENDING);
    out.putStringArrayList(
        KeyboardApiContract.EXTRA_REQUESTED_SCOPES, new ArrayList<>(requestedScopes));
    return out;
  }

  @NonNull
  static Bundle getPairingStatus(
      @NonNull Context context,
      @NonNull KeyboardApiPrefs prefs,
      @NonNull KeyboardApiPairingStore pairingStore,
      @NonNull String callingPackage) {
    if (!pairingStore.hasRequest(callingPackage)) {
      if (prefs.isPackageAllowListed(callingPackage)) {
        final Bundle out = KeyboardApiCallSupport.ok();
        out.putString(KeyboardApiContract.EXTRA_CONTROLLER_PACKAGE, callingPackage);
        out.putString(
            KeyboardApiContract.EXTRA_PAIRING_STATUS, KeyboardApiContract.PAIRING_STATUS_APPROVED);
        out.putStringArrayList(
            KeyboardApiContract.EXTRA_APPROVED_SCOPES,
            new ArrayList<>(prefs.getControllerScopes(callingPackage)));
        return out;
      }
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_PAIRING_NOT_FOUND, "No pairing request");
    }

    final String status = pairingStore.getStatus(callingPackage);
    if (KeyboardApiPairingStore.STATUS_DENIED.equals(status)) {
      return KeyboardApiCallSupport.error(KeyboardApiContract.ERR_PAIRING_DENIED, "Pairing denied");
    }

    if (KeyboardApiPairingStore.STATUS_APPROVED.equals(status)) {
      final Bundle out = KeyboardApiCallSupport.ok();
      out.putString(KeyboardApiContract.EXTRA_CONTROLLER_PACKAGE, callingPackage);
      out.putString(
          KeyboardApiContract.EXTRA_PAIRING_STATUS, KeyboardApiContract.PAIRING_STATUS_APPROVED);
      out.putStringArrayList(
          KeyboardApiContract.EXTRA_APPROVED_SCOPES,
          new ArrayList<>(pairingStore.getApprovedScopes(callingPackage)));

      if (!pairingStore.isTokenDelivered(callingPackage)
          && KeyboardApiControllerTokensStore.hasToken(context, callingPackage)) {
        final String token = KeyboardApiControllerTokensStore.getToken(context, callingPackage);
        if (token != null && !token.trim().isEmpty()) {
          out.putString(KeyboardApiContract.EXTRA_CONTROLLER_TOKEN, token);
          pairingStore.markTokenDelivered(callingPackage, System.currentTimeMillis());
        }
      }

      return out;
    }

    final Bundle out = KeyboardApiCallSupport.ok();
    out.putString(KeyboardApiContract.EXTRA_CONTROLLER_PACKAGE, callingPackage);
    out.putString(
        KeyboardApiContract.EXTRA_PAIRING_STATUS, KeyboardApiContract.PAIRING_STATUS_PENDING);
    out.putStringArrayList(
        KeyboardApiContract.EXTRA_REQUESTED_SCOPES,
        new ArrayList<>(pairingStore.getRequestedScopes(callingPackage)));
    return out;
  }
}
