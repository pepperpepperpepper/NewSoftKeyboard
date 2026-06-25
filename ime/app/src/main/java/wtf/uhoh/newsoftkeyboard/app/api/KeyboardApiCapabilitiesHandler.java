package wtf.uhoh.newsoftkeyboard.app.api;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.api.KeyboardApiContract;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class KeyboardApiCapabilitiesHandler {

  private KeyboardApiCapabilitiesHandler() {}

  @NonNull
  static Bundle getCapabilities(@NonNull Context context) {
    final Bundle out = KeyboardApiCallSupport.ok();

    final ArrayList<String> methods = new ArrayList<>();
    methods.add(KeyboardApiContract.METHOD_GET_API_VERSION);
    methods.add(KeyboardApiContract.METHOD_GET_CAPABILITIES);
    methods.add(KeyboardApiContract.METHOD_PING);
    methods.add(KeyboardApiContract.METHOD_GET_KEYBOARD_STATUS);
    methods.add(KeyboardApiContract.METHOD_GET_PREFERENCE);
    methods.add(KeyboardApiContract.METHOD_SET_PREFERENCE);
    methods.add(KeyboardApiContract.METHOD_SET_PREFERENCES);
    methods.add(KeyboardApiContract.METHOD_SET_SECRET);
    methods.add(KeyboardApiContract.METHOD_GET_SECRET_STATUS);
    methods.add(KeyboardApiContract.METHOD_TOGGLE_INCOGNITO);
    methods.add(KeyboardApiContract.METHOD_SWITCH_LANGUAGE);
    methods.add(KeyboardApiContract.METHOD_SWITCH_KEYBOARD_MODE);
    methods.add(KeyboardApiContract.METHOD_SEND_NAVIGATION_KEY);
    methods.add(KeyboardApiContract.METHOD_SEND_TAB);
    methods.add(KeyboardApiContract.METHOD_SEND_ESCAPE);
    methods.add(KeyboardApiContract.METHOD_CLIPBOARD_COPY);
    methods.add(KeyboardApiContract.METHOD_CLIPBOARD_CUT);
    methods.add(KeyboardApiContract.METHOD_CLIPBOARD_PASTE);
    methods.add(KeyboardApiContract.METHOD_CLIPBOARD_SELECT_ALL);
    methods.add(KeyboardApiContract.METHOD_UNDO);
    methods.add(KeyboardApiContract.METHOD_REDO);
    methods.add(KeyboardApiContract.METHOD_RUN_SNIPPET);
    methods.add(KeyboardApiContract.METHOD_CLEAR_LEARNING_DATA);
    methods.add(KeyboardApiContract.METHOD_CLEAR_QUICK_TEXT_HISTORY);
    methods.add(KeyboardApiContract.METHOD_CLEAR_CLIPBOARD_HISTORY);
    methods.add(KeyboardApiContract.METHOD_OPEN_MEDIA_INSERTION_UI);
    methods.add(KeyboardApiContract.METHOD_OPEN_SETTINGS);
    methods.add(KeyboardApiContract.METHOD_RELOAD_SETTINGS);
    methods.add(KeyboardApiContract.METHOD_REQUEST_PAIRING);
    methods.add(KeyboardApiContract.METHOD_GET_PAIRING_STATUS);
    methods.add(KeyboardApiContract.METHOD_GET_AUDIT_LOG);
    methods.add(KeyboardApiContract.METHOD_CLEAR_AUDIT_LOG);
    methods.add(KeyboardApiContract.METHOD_SET_SESSION_PRESET);
    methods.add(KeyboardApiContract.METHOD_SET_SESSION_THEME_PRESET);
    methods.add(KeyboardApiContract.METHOD_SET_SESSION_KEYBOARD_ID);
    methods.add(KeyboardApiContract.METHOD_CLEAR_SESSION_OVERRIDES);
    methods.add(KeyboardApiContract.METHOD_RUN_MACRO);
    out.putStringArrayList(KeyboardApiContract.EXTRA_SUPPORTED_METHODS, methods);

    final ArrayList<String> scopes = new ArrayList<>();
    scopes.add(KeyboardApiContract.SCOPE_CAPABILITIES_READ);
    scopes.add(KeyboardApiContract.SCOPE_STATUS_READ);
    scopes.add(KeyboardApiContract.SCOPE_PREFS_READ);
    scopes.add(KeyboardApiContract.SCOPE_PREFS_WRITE_EFFECTS);
    scopes.add(KeyboardApiContract.SCOPE_PREFS_WRITE_TYPING);
    scopes.add(KeyboardApiContract.SCOPE_PREFS_WRITE_GESTURES);
    scopes.add(KeyboardApiContract.SCOPE_PREFS_WRITE_UI);
    scopes.add(KeyboardApiContract.SCOPE_PREFS_WRITE_CLIPBOARD);
    scopes.add(KeyboardApiContract.SCOPE_PREFS_WRITE_VOICE);
    scopes.add(KeyboardApiContract.SCOPE_SECRETS_WRITE);
    scopes.add(KeyboardApiContract.SCOPE_SECRETS_STATUS);
    scopes.add(KeyboardApiContract.SCOPE_ACTION_OPEN_SETTINGS);
    scopes.add(KeyboardApiContract.SCOPE_ACTION_RELOAD_SETTINGS);
    scopes.add(KeyboardApiContract.SCOPE_ACTION_MEDIA_INSERTION_OPEN);
    scopes.add(KeyboardApiContract.SCOPE_ACTION_DATA_CLEAR);
    scopes.add(KeyboardApiContract.SCOPE_ACTION_CLIPBOARD_CLEAR);
    scopes.add(KeyboardApiContract.SCOPE_ACTION_INCOGNITO);
    scopes.add(KeyboardApiContract.SCOPE_ACTION_SWITCH_LANGUAGE);
    scopes.add(KeyboardApiContract.SCOPE_ACTION_SWITCH_KEYBOARD_MODE);
    scopes.add(KeyboardApiContract.SCOPE_IME_INJECT_NAVIGATION);
    scopes.add(KeyboardApiContract.SCOPE_IME_INJECT_CLIPBOARD);
    scopes.add(KeyboardApiContract.SCOPE_IME_INJECT_SNIPPETS);
    scopes.add(KeyboardApiContract.SCOPE_CONTROLLERS_PAIR);
    scopes.add(KeyboardApiContract.SCOPE_AUDIT_READ);
    scopes.add(KeyboardApiContract.SCOPE_AUDIT_CLEAR);
    scopes.add(KeyboardApiContract.SCOPE_CONTEXT_SESSION_PRESET);
    scopes.add(KeyboardApiContract.SCOPE_CONTEXT_SESSION_THEME);
    scopes.add(KeyboardApiContract.SCOPE_CONTEXT_SESSION_LAYOUT);
    scopes.add(KeyboardApiContract.SCOPE_AUTOMATION_MACRO);
    out.putStringArrayList(KeyboardApiContract.EXTRA_SUPPORTED_SCOPES, scopes);

    final Map<String, KeyboardApiPreferenceAllowList.PrefSpec> allowList =
        KeyboardApiPreferenceAllowList.build(context);
    final List<String> prefKeys = KeyboardApiPreferenceAllowList.getAllowedPrefKeys(allowList);
    out.putStringArrayList(KeyboardApiContract.EXTRA_ALLOWED_PREF_KEYS, new ArrayList<>(prefKeys));
    out.putStringArrayList(
        KeyboardApiContract.EXTRA_ALLOWED_PREF_TYPES,
        new ArrayList<>(KeyboardApiPreferenceAllowList.getAllowedPrefTypes(prefKeys, allowList)));
    return out;
  }
}
