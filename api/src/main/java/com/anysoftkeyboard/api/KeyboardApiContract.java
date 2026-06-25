package com.anysoftkeyboard.api;

/**
 * Stable contract for controlling AnySoftKeyboard/NewSoftKeyboard via {@code ContentProvider.call}.
 *
 * <p>Security note: this contract is intentionally designed for one-way control. It must never be
 * extended to expose typed text, composing text, clipboard content, or target-app metadata.
 */
public class KeyboardApiContract {

  public static final int API_VERSION = 3;

  public static final String AUTHORITY_SUFFIX = ".keyboardapi";

  // call() methods
  public static final String METHOD_GET_API_VERSION = "getApiVersion";
  public static final String METHOD_GET_CAPABILITIES = "getCapabilities";
  public static final String METHOD_PING = "ping";
  public static final String METHOD_GET_KEYBOARD_STATUS = "getKeyboardStatus";
  public static final String METHOD_GET_PREFERENCE = "getPreference";
  public static final String METHOD_SET_PREFERENCE = "setPreference";
  public static final String METHOD_SET_PREFERENCES = "setPreferences";
  public static final String METHOD_TOGGLE_INCOGNITO = "toggleIncognito";
  public static final String METHOD_SWITCH_LANGUAGE = "switchLanguage";
  public static final String METHOD_SWITCH_KEYBOARD_MODE = "switchKeyboardMode";
  public static final String METHOD_SEND_NAVIGATION_KEY = "sendNavigationKey";
  public static final String METHOD_SEND_TAB = "sendTab";
  public static final String METHOD_SEND_ESCAPE = "sendEscape";
  public static final String METHOD_CLIPBOARD_COPY = "clipboardCopy";
  public static final String METHOD_CLIPBOARD_CUT = "clipboardCut";
  public static final String METHOD_CLIPBOARD_PASTE = "clipboardPaste";
  public static final String METHOD_CLIPBOARD_SELECT_ALL = "clipboardSelectAll";
  public static final String METHOD_UNDO = "undo";
  public static final String METHOD_REDO = "redo";
  public static final String METHOD_RUN_SNIPPET = "runSnippet";
  public static final String METHOD_SET_SECRET = "setSecret";
  public static final String METHOD_GET_SECRET_STATUS = "getSecretStatus";
  public static final String METHOD_OPEN_MEDIA_INSERTION_UI = "openMediaInsertionUi";
  public static final String METHOD_CLEAR_LEARNING_DATA = "clearLearningData";
  public static final String METHOD_CLEAR_QUICK_TEXT_HISTORY = "clearQuickTextHistory";
  public static final String METHOD_CLEAR_CLIPBOARD_HISTORY = "clearClipboardHistory";
  public static final String METHOD_OPEN_SETTINGS = "openSettings";
  public static final String METHOD_RELOAD_SETTINGS = "reloadSettings";
  public static final String METHOD_REQUEST_PAIRING = "requestPairing";
  public static final String METHOD_GET_PAIRING_STATUS = "getPairingStatus";
  public static final String METHOD_GET_AUDIT_LOG = "getAuditLog";
  public static final String METHOD_CLEAR_AUDIT_LOG = "clearAuditLog";
  public static final String METHOD_SET_SESSION_PRESET = "setSessionPreset";
  public static final String METHOD_SET_SESSION_THEME_PRESET = "setSessionThemePreset";
  public static final String METHOD_SET_SESSION_KEYBOARD_ID = "setSessionKeyboardId";
  public static final String METHOD_CLEAR_SESSION_OVERRIDES = "clearSessionOverrides";
  public static final String METHOD_RUN_MACRO = "runMacro";

  // Request extras
  public static final String EXTRA_CALLER_PACKAGE = "caller_package";

  public static final String EXTRA_PREF_KEY = "pref_key";
  public static final String EXTRA_PREF_BOOL = "pref_bool";
  public static final String EXTRA_PREF_INT = "pref_int";
  public static final String EXTRA_PREF_STRING = "pref_string";
  public static final String EXTRA_PREF_CHANGES = "pref_changes";
  public static final String EXTRA_PREF_RESULTS = "pref_results";

  public static final String EXTRA_LANGUAGE_DIRECTION = "language_direction";
  public static final String EXTRA_KEYBOARD_MODE = "keyboard_mode";
  public static final String EXTRA_KEYBOARD_ID = "keyboard_id";
  public static final String EXTRA_NAVIGATION_KEY = "navigation_key";
  public static final String EXTRA_SNIPPET_ID = "snippet_id";
  public static final String EXTRA_SECRET_ID = "secret_id";
  public static final String EXTRA_SECRET_VALUE = "secret_value";

  public static final String EXTRA_DESTINATION_ID = "destination_id";
  public static final String EXTRA_SCROLL_TO_PREF_KEY = "scroll_to_pref_key";
  public static final String EXTRA_START_ACTIVITY = "start_activity";

  public static final String EXTRA_REQUESTED_SCOPES = "requested_scopes";
  public static final String EXTRA_CONTROLLER_TOKEN = "controller_token";
  public static final String EXTRA_AUDIT_LIMIT = "audit_limit";
  public static final String EXTRA_INCOGNITO_ENABLED = "incognito_enabled";
  public static final String EXTRA_SESSION_PRESET_ID = "session_preset_id";
  public static final String EXTRA_SESSION_THEME_PRESET_ID = "session_theme_preset_id";

  // runMacro extras. macro_steps is a JSON array of {"method":..,"args":{..}} objects, where each
  // args key is one of the existing EXTRA_* names for that method. macro_results (response) is a
  // JSON array of per-step outcomes: {"i":n,"ok":bool[,"error_code":int]} or {"i":n,"skipped":true}.
  public static final String EXTRA_MACRO_STEPS = "macro_steps";
  public static final String EXTRA_MACRO_STOP_ON_ERROR = "macro_stop_on_error";
  public static final String EXTRA_MACRO_RESULTS = "macro_results";

  // The most steps a single runMacro call may contain.
  public static final int MAX_MACRO_STEPS = 16;

  // Response extras
  public static final String EXTRA_API_VERSION = "api_version";
  public static final String EXTRA_OK = "ok";
  public static final String EXTRA_ERROR_CODE = "error_code";
  public static final String EXTRA_ERROR_MESSAGE = "error_message";
  public static final String EXTRA_RETRY_AFTER_MS = "retry_after_ms";

  // Status extras
  public static final String EXTRA_IME_RUNNING = "ime_running";
  public static final String EXTRA_IME_ACTIVE = "ime_active";
  public static final String EXTRA_API_ENABLED = "api_enabled";
  public static final String EXTRA_IN_ALPHABET_MODE = "in_alphabet_mode";
  public static final String EXTRA_CURRENT_LOCALE = "current_locale";
  public static final String EXTRA_IN_PASSWORD_FIELD = "in_password_field";
  public static final String EXTRA_CURRENT_KEYBOARD_ID = "current_keyboard_id";

  // Pairing extras
  public static final String EXTRA_CONTROLLER_PACKAGE = "controller_package";
  public static final String EXTRA_PAIRING_STATUS = "pairing_status";
  public static final String EXTRA_APPROVED_SCOPES = "approved_scopes";
  public static final String EXTRA_AUDIT_ENTRIES = "audit_entries";

  public static final String PAIRING_STATUS_PENDING = "pending";
  public static final String PAIRING_STATUS_APPROVED = "approved";
  public static final String PAIRING_STATUS_DENIED = "denied";

  // Switch language modes
  public static final String LANGUAGE_DIRECTION_NEXT = "next";
  public static final String LANGUAGE_DIRECTION_PREVIOUS = "previous";

  // Switch keyboard modes
  public static final String KEYBOARD_MODE_TOGGLE = "toggle";
  public static final String KEYBOARD_MODE_ALPHABET = "alphabet";
  public static final String KEYBOARD_MODE_SYMBOLS = "symbols";

  // Navigation key values
  public static final String NAVIGATION_KEY_LEFT = "left";
  public static final String NAVIGATION_KEY_RIGHT = "right";
  public static final String NAVIGATION_KEY_UP = "up";
  public static final String NAVIGATION_KEY_DOWN = "down";
  public static final String NAVIGATION_KEY_HOME = "home";
  public static final String NAVIGATION_KEY_END = "end";
  public static final String NAVIGATION_KEY_PAGE_UP = "page_up";
  public static final String NAVIGATION_KEY_PAGE_DOWN = "page_down";

  // Capabilities extras
  public static final String EXTRA_SUPPORTED_METHODS = "supported_methods";
  public static final String EXTRA_SUPPORTED_SCOPES = "supported_scopes";
  public static final String EXTRA_ALLOWED_PREF_KEYS = "allowed_pref_keys";
  public static final String EXTRA_ALLOWED_PREF_TYPES = "allowed_pref_types";
  public static final String EXTRA_DEEPLINK_URI = "deeplink_uri";
  public static final String EXTRA_HAS_SECRET = "has_secret";

  // Secret values are write-only (never returned by this API).
  public static final String SECRET_ID_OPENAI_API_KEY = "openai_api_key";
  public static final String SECRET_ID_ELEVENLABS_API_KEY = "elevenlabs_api_key";

  // Pref types (strings to keep the Bundle payload simple for controllers)
  public static final String PREF_TYPE_BOOL = "bool";
  public static final String PREF_TYPE_INT = "int";
  public static final String PREF_TYPE_STRING = "string";

  // Error codes
  public static final int ERR_OK = 0;
  public static final int ERR_DISABLED = 1;
  public static final int ERR_USER_LOCKED = 2;
  public static final int ERR_CALLER_NOT_ALLOWED = 3;
  public static final int ERR_CALLER_UNCLEAR = 4;
  public static final int ERR_SIGNATURE_MISMATCH = 5;
  public static final int ERR_SCOPE_DENIED = 6;
  public static final int ERR_BAD_ARGUMENTS = 7;
  public static final int ERR_PREF_NOT_ALLOWED = 8;
  public static final int ERR_UNSUPPORTED_METHOD = 9;
  public static final int ERR_RATE_LIMITED = 10;
  public static final int ERR_IME_NOT_ACTIVE = 11;
  public static final int ERR_INTERNAL = 12;
  public static final int ERR_PAIRING_NOT_FOUND = 13;
  public static final int ERR_PAIRING_DENIED = 14;
  public static final int ERR_AUTH_TOKEN_REQUIRED = 15;
  public static final int ERR_AUTH_TOKEN_INVALID = 16;
  public static final int ERR_DISALLOWED_CONTEXT = 17;
  public static final int ERR_HIGH_RISK_DISABLED = 18;
  public static final int ERR_CLIPBOARD_COPY_CUT_DISABLED = 19;
  public static final int ERR_MACRO_TOO_LONG = 20;
  public static final int ERR_MACRO_STEP_NOT_ALLOWED = 21;

  // Scopes
  public static final String SCOPE_CAPABILITIES_READ = "capabilities.read";
  public static final String SCOPE_STATUS_READ = "status.read";
  public static final String SCOPE_PREFS_READ = "prefs.read";
  public static final String SCOPE_PREFS_WRITE_EFFECTS = "prefs.write.effects";
  public static final String SCOPE_PREFS_WRITE_TYPING = "prefs.write.typing";
  public static final String SCOPE_PREFS_WRITE_GESTURES = "prefs.write.gestures";
  public static final String SCOPE_PREFS_WRITE_UI = "prefs.write.ui";
  public static final String SCOPE_PREFS_WRITE_CLIPBOARD = "prefs.write.clipboard";
  public static final String SCOPE_PREFS_WRITE_VOICE = "prefs.write.voice";
  public static final String SCOPE_ACTION_OPEN_SETTINGS = "action.open_settings";
  public static final String SCOPE_ACTION_RELOAD_SETTINGS = "action.reload_settings";
  public static final String SCOPE_ACTION_MEDIA_INSERTION_OPEN = "action.media_insertion.open";
  public static final String SCOPE_ACTION_DATA_CLEAR = "action.data_clear";
  public static final String SCOPE_ACTION_CLIPBOARD_CLEAR = "action.clipboard_clear";
  public static final String SCOPE_ACTION_INCOGNITO = "action.incognito";
  public static final String SCOPE_ACTION_SWITCH_LANGUAGE = "action.switch_language";
  public static final String SCOPE_ACTION_SWITCH_KEYBOARD_MODE = "action.switch_keyboard_mode";
  public static final String SCOPE_IME_INJECT_NAVIGATION = "ime.inject.navigation";
  public static final String SCOPE_IME_INJECT_CLIPBOARD = "ime.inject.clipboard";
  public static final String SCOPE_IME_INJECT_SNIPPETS = "ime.inject.snippets";
  public static final String SCOPE_SECRETS_WRITE = "secrets.write";
  public static final String SCOPE_SECRETS_STATUS = "secrets.status";
  public static final String SCOPE_CONTROLLERS_PAIR = "controllers.pair";
  public static final String SCOPE_AUDIT_READ = "audit.read";
  public static final String SCOPE_AUDIT_CLEAR = "audit.clear";
  public static final String SCOPE_CONTEXT_SESSION_PRESET = "context.session.preset";
  public static final String SCOPE_CONTEXT_SESSION_THEME = "context.session.theme";
  public static final String SCOPE_CONTEXT_SESSION_LAYOUT = "context.session.layout";

  // Outer gate for runMacro. A macro can only batch verbs the caller already holds per-step scopes
  // for; this scope merely permits using the batching primitive at all.
  public static final String SCOPE_AUTOMATION_MACRO = "automation.macro";
}
