package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.app.AlertDialog;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import com.anysoftkeyboard.api.KeyboardApiContract;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import wtf.uhoh.newsoftkeyboard.R;

final class ProgrammableApiScopesUi {

  private static final List<String> ALL_SCOPES =
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
          KeyboardApiContract.SCOPE_CONTEXT_SESSION_LAYOUT);

  private ProgrammableApiScopesUi() {}

  static void showScopesDialog(
      @NonNull Context context,
      @NonNull String title,
      @NonNull Set<String> preselectedScopes,
      @NonNull Consumer<Set<String>> onSaved) {
    final String[] scopeValues = ALL_SCOPES.toArray(new String[0]);
    final String[] scopeLabels = new String[scopeValues.length];
    for (int i = 0; i < scopeValues.length; i++) {
      scopeLabels[i] = toScopeLabel(context, scopeValues[i]);
    }

    final boolean[] checked = new boolean[scopeValues.length];
    for (int i = 0; i < scopeValues.length; i++) {
      checked[i] = preselectedScopes.contains(scopeValues[i]);
    }

    new AlertDialog.Builder(context)
        .setTitle(title)
        .setMultiChoiceItems(
            scopeLabels, checked, (d, which, isChecked) -> checked[which] = isChecked)
        .setPositiveButton(
            android.R.string.ok,
            (d, which) -> {
              final HashSet<String> selected = new HashSet<>();
              for (int i = 0; i < scopeValues.length; i++) {
                if (checked[i]) selected.add(scopeValues[i]);
              }
              onSaved.accept(selected);
            })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  @NonNull
  static Set<String> defaultScopes() {
    return new HashSet<>(
        Arrays.asList(
            KeyboardApiContract.SCOPE_CAPABILITIES_READ,
            KeyboardApiContract.SCOPE_STATUS_READ,
            KeyboardApiContract.SCOPE_PREFS_READ,
            KeyboardApiContract.SCOPE_ACTION_OPEN_SETTINGS));
  }

  @NonNull
  private static String toScopeLabel(@NonNull Context context, @NonNull String scope) {
    final int labelRes;
    switch (scope) {
      case KeyboardApiContract.SCOPE_CAPABILITIES_READ:
        labelRes = R.string.keyboard_api_scope_capabilities_read;
        break;
      case KeyboardApiContract.SCOPE_STATUS_READ:
        labelRes = R.string.keyboard_api_scope_status_read;
        break;
      case KeyboardApiContract.SCOPE_PREFS_READ:
        labelRes = R.string.keyboard_api_scope_prefs_read;
        break;
      case KeyboardApiContract.SCOPE_PREFS_WRITE_EFFECTS:
        labelRes = R.string.keyboard_api_scope_prefs_write_effects;
        break;
      case KeyboardApiContract.SCOPE_PREFS_WRITE_TYPING:
        labelRes = R.string.keyboard_api_scope_prefs_write_typing;
        break;
      case KeyboardApiContract.SCOPE_PREFS_WRITE_GESTURES:
        labelRes = R.string.keyboard_api_scope_prefs_write_gestures;
        break;
      case KeyboardApiContract.SCOPE_PREFS_WRITE_UI:
        labelRes = R.string.keyboard_api_scope_prefs_write_ui;
        break;
      case KeyboardApiContract.SCOPE_PREFS_WRITE_CLIPBOARD:
        labelRes = R.string.keyboard_api_scope_prefs_write_clipboard;
        break;
      case KeyboardApiContract.SCOPE_PREFS_WRITE_VOICE:
        labelRes = R.string.keyboard_api_scope_prefs_write_voice;
        break;
      case KeyboardApiContract.SCOPE_SECRETS_WRITE:
        labelRes = R.string.keyboard_api_scope_secrets_write;
        break;
      case KeyboardApiContract.SCOPE_SECRETS_STATUS:
        labelRes = R.string.keyboard_api_scope_secrets_status;
        break;
      case KeyboardApiContract.SCOPE_ACTION_OPEN_SETTINGS:
        labelRes = R.string.keyboard_api_scope_open_settings;
        break;
      case KeyboardApiContract.SCOPE_ACTION_RELOAD_SETTINGS:
        labelRes = R.string.keyboard_api_scope_reload_settings;
        break;
      case KeyboardApiContract.SCOPE_ACTION_MEDIA_INSERTION_OPEN:
        labelRes = R.string.keyboard_api_scope_media_insertion_open;
        break;
      case KeyboardApiContract.SCOPE_ACTION_DATA_CLEAR:
        labelRes = R.string.keyboard_api_scope_action_data_clear;
        break;
      case KeyboardApiContract.SCOPE_ACTION_CLIPBOARD_CLEAR:
        labelRes = R.string.keyboard_api_scope_action_clipboard_clear;
        break;
      case KeyboardApiContract.SCOPE_ACTION_INCOGNITO:
        labelRes = R.string.keyboard_api_scope_action_incognito;
        break;
      case KeyboardApiContract.SCOPE_ACTION_SWITCH_LANGUAGE:
        labelRes = R.string.keyboard_api_scope_action_switch_language;
        break;
      case KeyboardApiContract.SCOPE_ACTION_SWITCH_KEYBOARD_MODE:
        labelRes = R.string.keyboard_api_scope_action_switch_keyboard_mode;
        break;
      case KeyboardApiContract.SCOPE_IME_INJECT_NAVIGATION:
        labelRes = R.string.keyboard_api_scope_ime_inject_navigation;
        break;
      case KeyboardApiContract.SCOPE_IME_INJECT_CLIPBOARD:
        labelRes = R.string.keyboard_api_scope_ime_inject_clipboard;
        break;
      case KeyboardApiContract.SCOPE_IME_INJECT_SNIPPETS:
        labelRes = R.string.keyboard_api_scope_ime_inject_snippets;
        break;
      case KeyboardApiContract.SCOPE_AUDIT_READ:
        labelRes = R.string.keyboard_api_scope_audit_read;
        break;
      case KeyboardApiContract.SCOPE_AUDIT_CLEAR:
        labelRes = R.string.keyboard_api_scope_audit_clear;
        break;
      case KeyboardApiContract.SCOPE_CONTEXT_SESSION_PRESET:
        labelRes = R.string.keyboard_api_scope_context_session_preset;
        break;
      case KeyboardApiContract.SCOPE_CONTEXT_SESSION_THEME:
        labelRes = R.string.keyboard_api_scope_context_session_theme;
        break;
      case KeyboardApiContract.SCOPE_CONTEXT_SESSION_LAYOUT:
        labelRes = R.string.keyboard_api_scope_context_session_layout;
        break;
      default:
        return scope;
    }
    return context.getString(labelRes) + "\n" + scope;
  }
}
