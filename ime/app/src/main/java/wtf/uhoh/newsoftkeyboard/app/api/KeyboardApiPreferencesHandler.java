package wtf.uhoh.newsoftkeyboard.app.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.api.KeyboardApiContract;
import java.util.Map;
import java.util.Set;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

final class KeyboardApiPreferencesHandler {

  private KeyboardApiPreferencesHandler() {}

  @NonNull
  static Bundle getPreference(
      @NonNull Context context, @NonNull Set<String> scopes, @NonNull Bundle extras) {
    if (!scopes.contains(KeyboardApiContract.SCOPE_PREFS_READ)) {
      return KeyboardApiCallSupport.error(KeyboardApiContract.ERR_SCOPE_DENIED, "Scope denied");
    }

    final String prefKey = extras.getString(KeyboardApiContract.EXTRA_PREF_KEY);
    if (prefKey == null || prefKey.trim().isEmpty()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "Missing pref_key");
    }

    final Map<String, KeyboardApiPreferenceAllowList.PrefSpec> allowList =
        KeyboardApiPreferenceAllowList.build(context);
    final KeyboardApiPreferenceAllowList.PrefSpec spec = allowList.get(prefKey);
    if (spec == null) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_PREF_NOT_ALLOWED, "Preference not allowed");
    }
    if (!scopes.contains(spec.readScope)) {
      return KeyboardApiCallSupport.error(KeyboardApiContract.ERR_SCOPE_DENIED, "Scope denied");
    }

    final SharedPreferences sharedPreferences = DirectBootAwareSharedPreferences.create(context);
    final Bundle out = KeyboardApiCallSupport.ok();
    out.putString(KeyboardApiContract.EXTRA_PREF_KEY, prefKey);
    switch (spec.type) {
      case KeyboardApiContract.PREF_TYPE_BOOL:
        out.putBoolean(
            KeyboardApiContract.EXTRA_PREF_BOOL, sharedPreferences.getBoolean(prefKey, false));
        return out;
      case KeyboardApiContract.PREF_TYPE_INT:
        out.putInt(KeyboardApiContract.EXTRA_PREF_INT, sharedPreferences.getInt(prefKey, 0));
        return out;
      case KeyboardApiContract.PREF_TYPE_STRING:
        out.putString(
            KeyboardApiContract.EXTRA_PREF_STRING, sharedPreferences.getString(prefKey, null));
        return out;
      default:
        return KeyboardApiCallSupport.error(KeyboardApiContract.ERR_INTERNAL, "Unknown pref type");
    }
  }

  @NonNull
  static Bundle setPreference(
      @NonNull Context context, @NonNull Set<String> scopes, @NonNull Bundle extras) {
    final String prefKey = extras.getString(KeyboardApiContract.EXTRA_PREF_KEY);
    if (prefKey == null || prefKey.trim().isEmpty()) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "Missing pref_key");
    }

    final Map<String, KeyboardApiPreferenceAllowList.PrefSpec> allowList =
        KeyboardApiPreferenceAllowList.build(context);
    final KeyboardApiPreferenceAllowList.PrefSpec spec = allowList.get(prefKey);
    if (spec == null) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_PREF_NOT_ALLOWED, "Preference not allowed");
    }
    if (spec.writeScope == null) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_SCOPE_DENIED, "Preference is read-only");
    }
    if (!scopes.contains(spec.writeScope)) {
      return KeyboardApiCallSupport.error(KeyboardApiContract.ERR_SCOPE_DENIED, "Scope denied");
    }

    final SharedPreferences sharedPreferences = DirectBootAwareSharedPreferences.create(context);
    final SharedPreferences.Editor editor = sharedPreferences.edit();

    switch (spec.type) {
      case KeyboardApiContract.PREF_TYPE_BOOL:
        if (!extras.containsKey(KeyboardApiContract.EXTRA_PREF_BOOL)) {
          return KeyboardApiCallSupport.error(
              KeyboardApiContract.ERR_BAD_ARGUMENTS, "Missing pref_bool");
        }
        editor.putBoolean(prefKey, extras.getBoolean(KeyboardApiContract.EXTRA_PREF_BOOL));
        break;
      case KeyboardApiContract.PREF_TYPE_INT:
        if (!extras.containsKey(KeyboardApiContract.EXTRA_PREF_INT)) {
          return KeyboardApiCallSupport.error(
              KeyboardApiContract.ERR_BAD_ARGUMENTS, "Missing pref_int");
        }
        final int v = extras.getInt(KeyboardApiContract.EXTRA_PREF_INT);
        if (v < spec.minInt || v > spec.maxInt) {
          return KeyboardApiCallSupport.error(
              KeyboardApiContract.ERR_BAD_ARGUMENTS,
              "pref_int out of range (" + spec.minInt + ".." + spec.maxInt + ")");
        }
        editor.putInt(prefKey, v);
        break;
      case KeyboardApiContract.PREF_TYPE_STRING:
        if (!extras.containsKey(KeyboardApiContract.EXTRA_PREF_STRING)) {
          return KeyboardApiCallSupport.error(
              KeyboardApiContract.ERR_BAD_ARGUMENTS, "Missing pref_string");
        }
        final Object sObj = extras.get(KeyboardApiContract.EXTRA_PREF_STRING);
        if (!(sObj instanceof String)) {
          return KeyboardApiCallSupport.error(
              KeyboardApiContract.ERR_BAD_ARGUMENTS, "pref_string must be a string");
        }
        final String s = (String) sObj;
        if (s.length() > 128) {
          return KeyboardApiCallSupport.error(
              KeyboardApiContract.ERR_BAD_ARGUMENTS, "pref_string too long");
        }
        if (spec.allowedStringValues != null && !spec.allowedStringValues.contains(s)) {
          return KeyboardApiCallSupport.error(
              KeyboardApiContract.ERR_BAD_ARGUMENTS, "pref_string not allowed");
        }
        editor.putString(prefKey, s);
        break;
      default:
        return KeyboardApiCallSupport.error(KeyboardApiContract.ERR_INTERNAL, "Unknown pref type");
    }

    editor.apply();
    return KeyboardApiCallSupport.ok();
  }

  @NonNull
  static Bundle setPreferences(
      @NonNull Context context, @NonNull Set<String> scopes, @NonNull Bundle extras) {
    final Bundle changes = extras.getBundle(KeyboardApiContract.EXTRA_PREF_CHANGES);
    if (changes == null) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "Missing pref_changes");
    }

    final int changeCount = changes.keySet().size();
    if (changeCount == 0) {
      final Bundle out = KeyboardApiCallSupport.ok();
      out.putBundle(KeyboardApiContract.EXTRA_PREF_RESULTS, new Bundle());
      return out;
    }
    if (changeCount > 20) {
      return KeyboardApiCallSupport.error(
          KeyboardApiContract.ERR_BAD_ARGUMENTS, "Too many changes");
    }

    final Map<String, KeyboardApiPreferenceAllowList.PrefSpec> allowList =
        KeyboardApiPreferenceAllowList.build(context);
    final SharedPreferences sharedPreferences = DirectBootAwareSharedPreferences.create(context);
    final SharedPreferences.Editor editor = sharedPreferences.edit();

    final Bundle results = new Bundle();

    for (String key : changes.keySet()) {
      if (key == null || key.trim().isEmpty()) continue;

      final KeyboardApiPreferenceAllowList.PrefSpec spec = allowList.get(key);
      if (spec == null) {
        results.putInt(key, KeyboardApiContract.ERR_PREF_NOT_ALLOWED);
        continue;
      }
      if (spec.writeScope == null || !scopes.contains(spec.writeScope)) {
        results.putInt(key, KeyboardApiContract.ERR_SCOPE_DENIED);
        continue;
      }

      final Object value = changes.get(key);
      switch (spec.type) {
        case KeyboardApiContract.PREF_TYPE_BOOL:
          if (!(value instanceof Boolean)) {
            results.putInt(key, KeyboardApiContract.ERR_BAD_ARGUMENTS);
            continue;
          }
          editor.putBoolean(key, (Boolean) value);
          results.putInt(key, KeyboardApiContract.ERR_OK);
          break;
        case KeyboardApiContract.PREF_TYPE_INT:
          if (!(value instanceof Integer)) {
            results.putInt(key, KeyboardApiContract.ERR_BAD_ARGUMENTS);
            continue;
          }
          final int v = (Integer) value;
          if (v < spec.minInt || v > spec.maxInt) {
            results.putInt(key, KeyboardApiContract.ERR_BAD_ARGUMENTS);
            continue;
          }
          editor.putInt(key, v);
          results.putInt(key, KeyboardApiContract.ERR_OK);
          break;
        case KeyboardApiContract.PREF_TYPE_STRING:
          if (!(value instanceof String)) {
            results.putInt(key, KeyboardApiContract.ERR_BAD_ARGUMENTS);
            continue;
          }
          final String s = (String) value;
          if (s.length() > 128) {
            results.putInt(key, KeyboardApiContract.ERR_BAD_ARGUMENTS);
            continue;
          }
          if (spec.allowedStringValues != null && !spec.allowedStringValues.contains(s)) {
            results.putInt(key, KeyboardApiContract.ERR_BAD_ARGUMENTS);
            continue;
          }
          editor.putString(key, s);
          results.putInt(key, KeyboardApiContract.ERR_OK);
          break;
        default:
          results.putInt(key, KeyboardApiContract.ERR_INTERNAL);
      }
    }

    editor.apply();

    final Bundle out = KeyboardApiCallSupport.ok();
    out.putBundle(KeyboardApiContract.EXTRA_PREF_RESULTS, results);
    return out;
  }
}
