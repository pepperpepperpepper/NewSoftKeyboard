package wtf.uhoh.newsoftkeyboard.app.api;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.api.KeyboardApiContract;
import wtf.uhoh.newsoftkeyboard.app.ime.ImeServiceBase;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.MainSettingsActivity;

final class KeyboardApiUiActionsHandler {

  private KeyboardApiUiActionsHandler() {}

  @NonNull
  static Bundle openMediaInsertionUi(@NonNull String callingPackage) {
    if (ImeServiceBase.getInstance() == null) {
      return KeyboardApiCallSupport.error(KeyboardApiContract.ERR_IME_NOT_ACTIVE, "IME not active");
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

    return KeyboardApiCallSupport.runOnMainThreadBlocking(
        () -> {
          final ImeServiceBase ime = ImeServiceBase.getInstance();
          if (ime == null) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_IME_NOT_ACTIVE, "IME not active");
          }
          final var api = ime.getProgrammableApiController();
          if (api.isInPasswordFieldForProgrammableApi()) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_DISALLOWED_CONTEXT, "Disallowed in password field");
          }
          if (!api.openMediaInsertionUiForProgrammableApi()) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_BAD_ARGUMENTS, "Media insertion not supported");
          }
          return KeyboardApiCallSupport.ok();
        });
  }

  @NonNull
  static Bundle openSettings(
      @NonNull Context context, @NonNull String callingPackage, @NonNull Bundle extras) {
    final String destinationId = extras.getString(KeyboardApiContract.EXTRA_DESTINATION_ID);
    final String scrollToPrefKey = extras.getString(KeyboardApiContract.EXTRA_SCROLL_TO_PREF_KEY);
    final boolean startActivity =
        extras.getBoolean(KeyboardApiContract.EXTRA_START_ACTIVITY, false);

    if (scrollToPrefKey != null && scrollToPrefKey.length() > 256) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "scroll_to_pref_key too long");
    }

    final String deeplinkUri;
    if (destinationId == null || destinationId.trim().isEmpty()) {
      deeplinkUri = null;
    } else {
      // For v1, only accept deep-links that we explicitly own to avoid a generic "open any URI"
      // trampoline (UI spam risk).
      deeplinkUri = KeyboardApiSettingsDeepLinks.toDeeplinkUri(context, destinationId);
      if (deeplinkUri == null) {
        return KeyboardApiCallSupport.error(
            KeyboardApiContract.ERR_BAD_ARGUMENTS, "Unknown destination_id");
      }
    }

    final Bundle out = KeyboardApiCallSupport.ok();
    out.putString(KeyboardApiContract.EXTRA_DESTINATION_ID, destinationId);
    out.putString(KeyboardApiContract.EXTRA_DEEPLINK_URI, deeplinkUri);
    out.putString(KeyboardApiContract.EXTRA_SCROLL_TO_PREF_KEY, scrollToPrefKey);
    if (startActivity) {
      final KeyboardApiRateLimiter.Decision uiDecision =
          KeyboardApiRateLimiter.checkUiAction(callingPackage);
      if (!uiDecision.allowed) {
        final Bundle limited =
            KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_RATE_LIMITED, "UI action rate limited");
        limited.putLong(KeyboardApiContract.EXTRA_RETRY_AFTER_MS, uiDecision.retryAfterMs);
        return limited;
      }

      final Intent intent = new Intent(context, MainSettingsActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      if (destinationId != null && !destinationId.trim().isEmpty()) {
        intent.putExtra(KeyboardApiContract.EXTRA_DESTINATION_ID, destinationId);
      }
      if (scrollToPrefKey != null && !scrollToPrefKey.trim().isEmpty()) {
        intent.putExtra(KeyboardApiContract.EXTRA_SCROLL_TO_PREF_KEY, scrollToPrefKey);
      }
      try {
        context.startActivity(intent);
      } catch (RuntimeException e) {
        return KeyboardApiCallSupport.error(
            KeyboardApiContract.ERR_INTERNAL, "Failed to start settings");
      }
    }
    return out;
  }
}
