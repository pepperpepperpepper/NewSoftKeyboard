package wtf.uhoh.newsoftkeyboard.app.api;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyboardApiContract;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Parses and validates {@code runMacro} request payloads.
 *
 * <p>This helper owns the JSON wire format and the step-eligibility whitelist <em>only</em>. It
 * deliberately does not perform any dispatch: every step is re-run by {@link KeyboardApiProvider}
 * through the same guard stack as an individual {@code call()}, so a macro can do nothing a
 * sequence of ordinary calls could not. See {@code keyboard_api_macro_plan.md}.
 */
final class KeyboardApiMacroHandler {

  /** Maximum raw {@code macro_steps} JSON length accepted before parsing. */
  static final int MAX_RAW_STEPS_LENGTH = 8 * 1024;

  /**
   * Verbs that may appear as macro steps. These are the side-effecting control verbs; meta/auth,
   * reads, destructive, and UI-trampoline methods are excluded so the batch can't circumvent their
   * single-call semantics (e.g. the 2s UI cooldown). Per-step scope checks still apply on top.
   */
  private static final Set<String> ELIGIBLE_METHODS =
      Collections.unmodifiableSet(
          new HashSet<>(
              Arrays.asList(
                  KeyboardApiContract.METHOD_TOGGLE_INCOGNITO,
                  KeyboardApiContract.METHOD_SWITCH_LANGUAGE,
                  KeyboardApiContract.METHOD_SWITCH_KEYBOARD_MODE,
                  KeyboardApiContract.METHOD_SEND_NAVIGATION_KEY,
                  KeyboardApiContract.METHOD_SEND_TAB,
                  KeyboardApiContract.METHOD_SEND_ESCAPE,
                  KeyboardApiContract.METHOD_CLIPBOARD_COPY,
                  KeyboardApiContract.METHOD_CLIPBOARD_CUT,
                  KeyboardApiContract.METHOD_CLIPBOARD_PASTE,
                  KeyboardApiContract.METHOD_CLIPBOARD_SELECT_ALL,
                  KeyboardApiContract.METHOD_UNDO,
                  KeyboardApiContract.METHOD_REDO,
                  KeyboardApiContract.METHOD_RUN_SNIPPET,
                  KeyboardApiContract.METHOD_SET_PREFERENCE,
                  KeyboardApiContract.METHOD_SET_PREFERENCES,
                  KeyboardApiContract.METHOD_SET_SESSION_PRESET,
                  KeyboardApiContract.METHOD_SET_SESSION_THEME_PRESET,
                  KeyboardApiContract.METHOD_SET_SESSION_KEYBOARD_ID,
                  KeyboardApiContract.METHOD_CLEAR_SESSION_OVERRIDES,
                  KeyboardApiContract.METHOD_RELOAD_SETTINGS)));

  private KeyboardApiMacroHandler() {}

  /** True when a method is allowed to appear as a macro step. */
  static boolean isEligible(@NonNull String method) {
    return ELIGIBLE_METHODS.contains(method);
  }

  /** A single parsed macro step: a method name and the per-step extras to dispatch with. */
  static final class Step {
    @NonNull final String method;
    @NonNull final Bundle extras;

    Step(@NonNull String method, @NonNull Bundle extras) {
      this.method = method;
      this.extras = extras;
    }
  }

  /** Thrown when {@code macro_steps} is malformed, oversized, or too long. */
  static final class MacroParseException extends Exception {
    private static final long serialVersionUID = 1L;
    final int errorCode;

    MacroParseException(int errorCode, @NonNull String message) {
      super(message);
      this.errorCode = errorCode;
    }
  }

  /**
   * Parses the {@code macro_steps} JSON array into ordered {@link Step}s. Does not check
   * eligibility or scopes (the provider does that per step at dispatch time).
   */
  @NonNull
  static List<Step> parseSteps(@NonNull Bundle extras) throws MacroParseException {
    final String raw = extras.getString(KeyboardApiContract.EXTRA_MACRO_STEPS);
    if (raw == null || raw.trim().isEmpty()) {
      throw new MacroParseException(KeyboardApiContract.ERR_BAD_ARGUMENTS, "macro_steps required");
    }
    if (raw.length() > MAX_RAW_STEPS_LENGTH) {
      throw new MacroParseException(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "macro_steps too large");
    }

    final JSONArray array;
    try {
      array = new JSONArray(raw);
    } catch (JSONException e) {
      throw new MacroParseException(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "macro_steps is not a JSON array");
    }

    if (array.length() == 0) {
      throw new MacroParseException(KeyboardApiContract.ERR_BAD_ARGUMENTS, "macro_steps is empty");
    }
    if (array.length() > KeyboardApiContract.MAX_MACRO_STEPS) {
      throw new MacroParseException(
          KeyboardApiContract.ERR_MACRO_TOO_LONG, "macro_steps exceeds MAX_MACRO_STEPS");
    }

    final java.util.ArrayList<Step> steps = new java.util.ArrayList<>(array.length());
    for (int i = 0; i < array.length(); i++) {
      final JSONObject stepObject = array.optJSONObject(i);
      if (stepObject == null) {
        throw new MacroParseException(
            KeyboardApiContract.ERR_BAD_ARGUMENTS, "macro step " + i + " is not an object");
      }
      final String method = stepObject.optString("method", null);
      if (method == null || method.trim().isEmpty()) {
        throw new MacroParseException(
            KeyboardApiContract.ERR_BAD_ARGUMENTS, "macro step " + i + " missing method");
      }
      steps.add(new Step(method, argsToBundle(stepObject.optJSONObject("args"))));
    }
    return steps;
  }

  /**
   * Maps a JSON {@code args} object onto a Bundle of EXTRA_* values. JSON strings, booleans, and
   * integers map to the matching Bundle put; nested objects are supported for {@code pref_changes}.
   */
  @NonNull
  private static Bundle argsToBundle(@Nullable JSONObject args) throws MacroParseException {
    final Bundle bundle = new Bundle();
    if (args == null) return bundle;
    final java.util.Iterator<String> keys = args.keys();
    while (keys.hasNext()) {
      final String key = keys.next();
      final Object value = args.opt(key);
      if (value instanceof Boolean) {
        bundle.putBoolean(key, (Boolean) value);
      } else if (value instanceof Integer) {
        bundle.putInt(key, (Integer) value);
      } else if (value instanceof Long) {
        bundle.putInt(key, (int) (long) (Long) value);
      } else if (value instanceof String) {
        bundle.putString(key, (String) value);
      } else if (value instanceof JSONObject) {
        bundle.putBundle(key, argsToBundle((JSONObject) value));
      } else {
        throw new MacroParseException(
            KeyboardApiContract.ERR_BAD_ARGUMENTS, "unsupported arg type for '" + key + "'");
      }
    }
    return bundle;
  }
}
