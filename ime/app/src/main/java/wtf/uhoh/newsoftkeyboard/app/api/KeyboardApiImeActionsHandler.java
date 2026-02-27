package wtf.uhoh.newsoftkeyboard.app.api;

import android.os.Bundle;
import android.view.KeyEvent;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.api.KeyboardApiContract;
import wtf.uhoh.newsoftkeyboard.app.ime.ImeOptionsMenuHost;
import wtf.uhoh.newsoftkeyboard.app.ime.ImeServiceBase;

final class KeyboardApiImeActionsHandler {

  private KeyboardApiImeActionsHandler() {}

  @NonNull
  static Bundle toggleIncognito(@NonNull Bundle extras) {
    final ImeServiceBase ime = ImeServiceBase.getInstance();
    if (ime == null)
      return KeyboardApiCallSupport.error(KeyboardApiContract.ERR_IME_NOT_ACTIVE, "IME not active");

    final ImeOptionsMenuHost host = new ImeOptionsMenuHost(ime);
    final boolean current = host.isIncognito();
    final boolean enabled;

    if (extras.containsKey(KeyboardApiContract.EXTRA_INCOGNITO_ENABLED)) {
      final Object v = extras.get(KeyboardApiContract.EXTRA_INCOGNITO_ENABLED);
      if (!(v instanceof Boolean)) {
        return KeyboardApiCallSupport.error(
            KeyboardApiContract.ERR_BAD_ARGUMENTS, "incognito_enabled must be bool");
      }
      enabled = (Boolean) v;
    } else {
      enabled = !current;
    }

    host.setIncognito(enabled, true);

    final Bundle out = KeyboardApiCallSupport.ok();
    out.putBoolean(KeyboardApiContract.EXTRA_INCOGNITO_ENABLED, enabled);
    return out;
  }

  @NonNull
  static Bundle switchLanguage(@NonNull Bundle extras) {
    if (ImeServiceBase.getInstance() == null) {
      return KeyboardApiCallSupport.error(KeyboardApiContract.ERR_IME_NOT_ACTIVE, "IME not active");
    }

    final String directionRaw = extras.getString(KeyboardApiContract.EXTRA_LANGUAGE_DIRECTION);
    final String direction =
        directionRaw == null || directionRaw.trim().isEmpty()
            ? KeyboardApiContract.LANGUAGE_DIRECTION_NEXT
            : directionRaw;

    final boolean previous;
    if (KeyboardApiContract.LANGUAGE_DIRECTION_NEXT.equals(direction)) {
      previous = false;
    } else if (KeyboardApiContract.LANGUAGE_DIRECTION_PREVIOUS.equals(direction)) {
      previous = true;
    } else {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "language_direction must be 'next' or 'previous'");
    }

    final String keyboardId = extras.getString(KeyboardApiContract.EXTRA_KEYBOARD_ID);

    return KeyboardApiCallSupport.runOnMainThreadBlocking(
        () -> {
          final ImeServiceBase ime = ImeServiceBase.getInstance();
          if (ime == null) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_IME_NOT_ACTIVE, "IME not active");
          }

          final var api = ime.getProgrammableApiController();
          final boolean switched = api.switchLanguageForProgrammableApi(keyboardId, previous);
          if (!switched) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_BAD_ARGUMENTS, "Failed to switch language");
          }

          final Bundle out = KeyboardApiCallSupport.ok();
          out.putBoolean(KeyboardApiContract.EXTRA_IME_RUNNING, true);
          out.putBoolean(KeyboardApiContract.EXTRA_IME_ACTIVE, true);
          out.putBoolean(
              KeyboardApiContract.EXTRA_IN_ALPHABET_MODE, api.isInAlphabetModeForProgrammableApi());
          out.putBoolean(
              KeyboardApiContract.EXTRA_IN_PASSWORD_FIELD,
              api.isInPasswordFieldForProgrammableApi());
          out.putString(
              KeyboardApiContract.EXTRA_CURRENT_LOCALE, api.getCurrentLocaleForProgrammableApi());
          out.putString(
              KeyboardApiContract.EXTRA_CURRENT_KEYBOARD_ID,
              api.getCurrentKeyboardIdForProgrammableApi());
          return out;
        });
  }

  @NonNull
  static Bundle switchKeyboardMode(@NonNull Bundle extras) {
    if (ImeServiceBase.getInstance() == null) {
      return KeyboardApiCallSupport.error(KeyboardApiContract.ERR_IME_NOT_ACTIVE, "IME not active");
    }

    final String modeRaw = extras.getString(KeyboardApiContract.EXTRA_KEYBOARD_MODE);
    final String mode =
        modeRaw == null || modeRaw.trim().isEmpty()
            ? KeyboardApiContract.KEYBOARD_MODE_TOGGLE
            : modeRaw;

    if (!KeyboardApiContract.KEYBOARD_MODE_TOGGLE.equals(mode)
        && !KeyboardApiContract.KEYBOARD_MODE_ALPHABET.equals(mode)
        && !KeyboardApiContract.KEYBOARD_MODE_SYMBOLS.equals(mode)) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS,
          "keyboard_mode must be 'toggle', 'alphabet', or 'symbols'");
    }

    return KeyboardApiCallSupport.runOnMainThreadBlocking(
        () -> {
          final ImeServiceBase ime = ImeServiceBase.getInstance();
          if (ime == null) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_IME_NOT_ACTIVE, "IME not active");
          }

          final var api = ime.getProgrammableApiController();
          if (!api.switchKeyboardModeForProgrammableApi(mode)) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_BAD_ARGUMENTS, "Failed to switch keyboard mode");
          }

          final Bundle out = KeyboardApiCallSupport.ok();
          out.putBoolean(KeyboardApiContract.EXTRA_IME_RUNNING, true);
          out.putBoolean(KeyboardApiContract.EXTRA_IME_ACTIVE, true);
          out.putBoolean(
              KeyboardApiContract.EXTRA_IN_ALPHABET_MODE, api.isInAlphabetModeForProgrammableApi());
          out.putBoolean(
              KeyboardApiContract.EXTRA_IN_PASSWORD_FIELD,
              api.isInPasswordFieldForProgrammableApi());
          out.putString(
              KeyboardApiContract.EXTRA_CURRENT_LOCALE, api.getCurrentLocaleForProgrammableApi());
          out.putString(
              KeyboardApiContract.EXTRA_CURRENT_KEYBOARD_ID,
              api.getCurrentKeyboardIdForProgrammableApi());
          return out;
        });
  }

  @NonNull
  static Bundle sendNavigationKey(@NonNull KeyboardApiPrefs prefs, @NonNull Bundle extras) {
    if (!prefs.isHighRiskActionsEnabled()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_HIGH_RISK_DISABLED, "High-risk actions disabled");
    }

    final String navigationKeyRaw = extras.getString(KeyboardApiContract.EXTRA_NAVIGATION_KEY);
    final String navigationKey = navigationKeyRaw == null ? null : navigationKeyRaw.trim();
    if (navigationKey == null || navigationKey.isEmpty()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "navigation_key required");
    }

    final int keyCode;
    switch (navigationKey) {
      case KeyboardApiContract.NAVIGATION_KEY_LEFT:
        keyCode = KeyEvent.KEYCODE_DPAD_LEFT;
        break;
      case KeyboardApiContract.NAVIGATION_KEY_RIGHT:
        keyCode = KeyEvent.KEYCODE_DPAD_RIGHT;
        break;
      case KeyboardApiContract.NAVIGATION_KEY_UP:
        keyCode = KeyEvent.KEYCODE_DPAD_UP;
        break;
      case KeyboardApiContract.NAVIGATION_KEY_DOWN:
        keyCode = KeyEvent.KEYCODE_DPAD_DOWN;
        break;
      case KeyboardApiContract.NAVIGATION_KEY_HOME:
        keyCode = KeyEvent.KEYCODE_MOVE_HOME;
        break;
      case KeyboardApiContract.NAVIGATION_KEY_END:
        keyCode = KeyEvent.KEYCODE_MOVE_END;
        break;
      case KeyboardApiContract.NAVIGATION_KEY_PAGE_UP:
        keyCode = KeyEvent.KEYCODE_PAGE_UP;
        break;
      case KeyboardApiContract.NAVIGATION_KEY_PAGE_DOWN:
        keyCode = KeyEvent.KEYCODE_PAGE_DOWN;
        break;
      default:
        return KeyboardApiCallSupport.error(
            KeyboardApiContract.ERR_BAD_ARGUMENTS, "Unknown navigation_key");
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
          if (!api.sendNavigationKeyForProgrammableApi(keyCode)) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_INTERNAL, "Failed to send key");
          }
          final Bundle out = KeyboardApiCallSupport.ok();
          out.putString(KeyboardApiContract.EXTRA_NAVIGATION_KEY, navigationKey);
          return out;
        });
  }

  @NonNull
  static Bundle sendTab(@NonNull KeyboardApiPrefs prefs) {
    if (!prefs.isHighRiskActionsEnabled()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_HIGH_RISK_DISABLED, "High-risk actions disabled");
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
          if (!api.sendTabForProgrammableApi()) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_INTERNAL, "Failed to send tab");
          }
          return KeyboardApiCallSupport.ok();
        });
  }

  @NonNull
  static Bundle sendEscape(@NonNull KeyboardApiPrefs prefs) {
    if (!prefs.isHighRiskActionsEnabled()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_HIGH_RISK_DISABLED, "High-risk actions disabled");
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
          if (!api.sendEscapeForProgrammableApi()) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_INTERNAL, "Failed to send escape");
          }
          return KeyboardApiCallSupport.ok();
        });
  }

  @NonNull
  static Bundle clipboardCopy(@NonNull KeyboardApiPrefs prefs) {
    if (!prefs.isHighRiskActionsEnabled()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_HIGH_RISK_DISABLED, "High-risk actions disabled");
    }
    if (!prefs.isClipboardCopyCutEnabled()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_CLIPBOARD_COPY_CUT_DISABLED, "Clipboard copy/cut disabled");
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
          if (!api.clipboardCopyForProgrammableApi()) {
            return KeyboardApiCallSupport.error(KeyboardApiContract.ERR_INTERNAL, "Failed to copy");
          }
          return KeyboardApiCallSupport.ok();
        });
  }

  @NonNull
  static Bundle clipboardCut(@NonNull KeyboardApiPrefs prefs) {
    if (!prefs.isHighRiskActionsEnabled()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_HIGH_RISK_DISABLED, "High-risk actions disabled");
    }
    if (!prefs.isClipboardCopyCutEnabled()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_CLIPBOARD_COPY_CUT_DISABLED, "Clipboard copy/cut disabled");
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
          if (!api.clipboardCutForProgrammableApi()) {
            return KeyboardApiCallSupport.error(KeyboardApiContract.ERR_INTERNAL, "Failed to cut");
          }
          return KeyboardApiCallSupport.ok();
        });
  }

  @NonNull
  static Bundle clipboardPaste(@NonNull KeyboardApiPrefs prefs) {
    if (!prefs.isHighRiskActionsEnabled()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_HIGH_RISK_DISABLED, "High-risk actions disabled");
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
          if (!api.clipboardPasteForProgrammableApi()) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_INTERNAL, "Failed to paste");
          }
          return KeyboardApiCallSupport.ok();
        });
  }

  @NonNull
  static Bundle clipboardSelectAll(@NonNull KeyboardApiPrefs prefs) {
    if (!prefs.isHighRiskActionsEnabled()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_HIGH_RISK_DISABLED, "High-risk actions disabled");
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
          if (!api.clipboardSelectAllForProgrammableApi()) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_INTERNAL, "Failed to select all");
          }
          return KeyboardApiCallSupport.ok();
        });
  }

  @NonNull
  static Bundle undo(@NonNull KeyboardApiPrefs prefs) {
    if (!prefs.isHighRiskActionsEnabled()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_HIGH_RISK_DISABLED, "High-risk actions disabled");
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
          if (!api.undoForProgrammableApi()) {
            return KeyboardApiCallSupport.error(KeyboardApiContract.ERR_INTERNAL, "Failed to undo");
          }
          return KeyboardApiCallSupport.ok();
        });
  }

  @NonNull
  static Bundle redo(@NonNull KeyboardApiPrefs prefs) {
    if (!prefs.isHighRiskActionsEnabled()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_HIGH_RISK_DISABLED, "High-risk actions disabled");
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
          if (!api.redoForProgrammableApi()) {
            return KeyboardApiCallSupport.error(KeyboardApiContract.ERR_INTERNAL, "Failed to redo");
          }
          return KeyboardApiCallSupport.ok();
        });
  }

  @NonNull
  static Bundle runSnippet(@NonNull KeyboardApiPrefs prefs, @NonNull Bundle extras) {
    if (!prefs.isHighRiskActionsEnabled()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_HIGH_RISK_DISABLED, "High-risk actions disabled");
    }

    final String snippetIdRaw = extras.getString(KeyboardApiContract.EXTRA_SNIPPET_ID);
    final String snippetId = snippetIdRaw == null ? null : snippetIdRaw.trim();
    if (snippetId == null || snippetId.isEmpty()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "snippet_id required");
    }
    if (snippetId.length() > 256) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "snippet_id too long");
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
          if (!api.runSnippetForProgrammableApi(snippetId)) {
            return KeyboardApiCallSupport.error(
                KeyboardApiContract.ERR_BAD_ARGUMENTS, "Unknown snippet_id");
          }
          final Bundle out = KeyboardApiCallSupport.ok();
          out.putString(KeyboardApiContract.EXTRA_SNIPPET_ID, snippetId);
          return out;
        });
  }
}
