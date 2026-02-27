package wtf.uhoh.newsoftkeyboard.app.api;

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.api.KeyboardApiContract;
import wtf.uhoh.newsoftkeyboard.app.ime.ImeServiceBase;

final class KeyboardApiStatusHandler {

  private KeyboardApiStatusHandler() {}

  @NonNull
  static Bundle getKeyboardStatus() {
    if (ImeServiceBase.getInstance() == null) {
      final Bundle out = KeyboardApiCallSupport.ok();
      out.putBoolean(KeyboardApiContract.EXTRA_IME_RUNNING, false);
      out.putBoolean(KeyboardApiContract.EXTRA_IME_ACTIVE, false);
      out.putBoolean(KeyboardApiContract.EXTRA_API_ENABLED, true);
      return out;
    }

    return KeyboardApiCallSupport.runOnMainThreadBlocking(
        () -> {
          final ImeServiceBase ime = ImeServiceBase.getInstance();
          final Bundle out = KeyboardApiCallSupport.ok();
          final boolean active = ime != null;
          out.putBoolean(KeyboardApiContract.EXTRA_IME_RUNNING, active);
          out.putBoolean(KeyboardApiContract.EXTRA_IME_ACTIVE, active);
          out.putBoolean(KeyboardApiContract.EXTRA_API_ENABLED, true);
          if (ime != null) {
            final var api = ime.getProgrammableApiController();
            out.putBoolean(
                KeyboardApiContract.EXTRA_INCOGNITO_ENABLED, ime.suggest().isIncognitoMode());
            out.putBoolean(
                KeyboardApiContract.EXTRA_IN_ALPHABET_MODE,
                api.isInAlphabetModeForProgrammableApi());
            out.putBoolean(
                KeyboardApiContract.EXTRA_IN_PASSWORD_FIELD,
                api.isInPasswordFieldForProgrammableApi());
            out.putString(
                KeyboardApiContract.EXTRA_CURRENT_LOCALE, api.getCurrentLocaleForProgrammableApi());
            out.putString(
                KeyboardApiContract.EXTRA_CURRENT_KEYBOARD_ID,
                api.getCurrentKeyboardIdForProgrammableApi());
          }
          return out;
        });
  }
}
