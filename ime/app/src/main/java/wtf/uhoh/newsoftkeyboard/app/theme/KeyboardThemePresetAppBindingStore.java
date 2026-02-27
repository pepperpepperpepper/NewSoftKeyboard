package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

/** Stores mappings from app package name to a theme preset id (per base theme). */
public class KeyboardThemePresetAppBindingStore {

  private static final String PREF_APP_BINDINGS_JSON_PREFIX = "theme_preset_app_bindings::";
  private static final String PREF_LAST_IME_PACKAGE_NAME = "theme_preset_last_ime_package_name";
  private static final String PREF_CURRENT_IME_PACKAGE_NAME =
      "theme_preset_current_ime_package_name";

  private final SharedPreferences prefs;

  public KeyboardThemePresetAppBindingStore(@NonNull Context context) {
    prefs = DirectBootAwareSharedPreferences.create(context.getApplicationContext());
  }

  KeyboardThemePresetAppBindingStore(@NonNull SharedPreferences prefs) {
    this.prefs = prefs;
  }

  public void setLastImePackageName(@Nullable String packageName) {
    if (packageName == null || packageName.trim().isEmpty()) {
      prefs.edit().remove(PREF_LAST_IME_PACKAGE_NAME).apply();
      return;
    }
    prefs.edit().putString(PREF_LAST_IME_PACKAGE_NAME, packageName).apply();
  }

  @Nullable
  public String getLastImePackageName() {
    final String value = prefs.getString(PREF_LAST_IME_PACKAGE_NAME, null);
    return value == null || value.trim().isEmpty() ? null : value.trim();
  }

  public void setCurrentImePackageName(@Nullable String packageName) {
    if (packageName == null || packageName.trim().isEmpty()) {
      prefs.edit().remove(PREF_CURRENT_IME_PACKAGE_NAME).apply();
      return;
    }
    prefs.edit().putString(PREF_CURRENT_IME_PACKAGE_NAME, packageName).apply();
  }

  @Nullable
  public String getCurrentImePackageName() {
    final String value = prefs.getString(PREF_CURRENT_IME_PACKAGE_NAME, null);
    return value == null || value.trim().isEmpty() ? null : value.trim();
  }

  @Nullable
  public String getBoundPresetId(@NonNull String baseThemeId, @NonNull String packageName) {
    final JSONObject json = readBindings(baseThemeId);
    final String value = json.optString(packageName, null);
    return value == null || value.trim().isEmpty() ? null : value.trim();
  }

  public void bindAppToPreset(
      @NonNull String baseThemeId, @NonNull String packageName, @NonNull String presetId) {
    final JSONObject json = readBindings(baseThemeId);
    try {
      json.put(packageName, presetId);
    } catch (JSONException e) {
      throw new IllegalStateException("Failed to write app binding.", e);
    }
    writeBindings(baseThemeId, json);
  }

  public void unbindApp(@NonNull String baseThemeId, @NonNull String packageName) {
    final JSONObject json = readBindings(baseThemeId);
    json.remove(packageName);
    writeBindings(baseThemeId, json);
  }

  public void clearBindings(@NonNull String baseThemeId) {
    prefs.edit().remove(bindingsKey(baseThemeId)).apply();
  }

  public void removePresetFromBindings(@NonNull String baseThemeId, @NonNull String presetId) {
    final JSONObject json = readBindings(baseThemeId);
    if (json.length() == 0) return;

    final List<String> toRemove = new ArrayList<>();
    for (Iterator<String> it = json.keys(); it.hasNext(); ) {
      final String packageName = it.next();
      if (presetId.equals(json.optString(packageName, ""))) toRemove.add(packageName);
    }
    if (toRemove.isEmpty()) return;
    for (String packageName : toRemove) json.remove(packageName);
    writeBindings(baseThemeId, json);
  }

  @NonNull
  public List<AppBinding> listBindings(@NonNull String baseThemeId) {
    final JSONObject json = readBindings(baseThemeId);
    if (json.length() == 0) return Collections.emptyList();

    final List<AppBinding> out = new ArrayList<>(json.length());
    for (Iterator<String> it = json.keys(); it.hasNext(); ) {
      final String packageName = it.next();
      final String presetId = json.optString(packageName, null);
      if (presetId == null || presetId.trim().isEmpty()) continue;
      out.add(new AppBinding(packageName, presetId.trim()));
    }
    out.sort(Comparator.comparing(AppBinding::packageName));
    return out;
  }

  @NonNull
  private static String bindingsKey(@NonNull String baseThemeId) {
    return PREF_APP_BINDINGS_JSON_PREFIX + baseThemeId;
  }

  @NonNull
  private JSONObject readBindings(@NonNull String baseThemeId) {
    final String raw = prefs.getString(bindingsKey(baseThemeId), null);
    if (raw == null || raw.trim().isEmpty()) return new JSONObject();
    try {
      return new JSONObject(raw);
    } catch (JSONException e) {
      // Corrupt JSON; wipe it and continue with an empty map.
      prefs.edit().remove(bindingsKey(baseThemeId)).apply();
      return new JSONObject();
    }
  }

  private void writeBindings(@NonNull String baseThemeId, @NonNull JSONObject bindings) {
    prefs.edit().putString(bindingsKey(baseThemeId), bindings.toString()).apply();
  }

  public static final class AppBinding {
    @NonNull private final String packageName;
    @NonNull private final String presetId;

    AppBinding(@NonNull String packageName, @NonNull String presetId) {
      this.packageName = packageName;
      this.presetId = presetId;
    }

    @NonNull
    public String packageName() {
      return packageName;
    }

    @NonNull
    public String presetId() {
      return presetId;
    }
  }
}
