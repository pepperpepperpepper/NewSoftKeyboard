package wtf.uhoh.newsoftkeyboard.app.keyboards.packs;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

public final class CustomKeyboardPrefs {
  private CustomKeyboardPrefs() {}

  @NonNull
  public static Set<String> getEnabledKeyboardIds(@NonNull Context context) {
    final SharedPreferences prefs =
        DirectBootAwareSharedPreferences.create(Objects.requireNonNull(context));
    final String key = context.getString(R.string.settings_key_custom_keyboards_enabled_ids);
    final Set<String> stored = prefs.getStringSet(key, null);
    if (stored == null || stored.isEmpty()) return Collections.emptySet();
    return Collections.unmodifiableSet(new HashSet<>(stored));
  }

  public static boolean isKeyboardEnabled(@NonNull Context context, @NonNull String keyboardId) {
    Objects.requireNonNull(context);
    Objects.requireNonNull(keyboardId);
    return getEnabledKeyboardIds(context).contains(keyboardId);
  }

  public static void setKeyboardEnabled(
      @NonNull Context context, @NonNull String keyboardId, boolean enabled) {
    final SharedPreferences prefs =
        DirectBootAwareSharedPreferences.create(Objects.requireNonNull(context));
    final String key = context.getString(R.string.settings_key_custom_keyboards_enabled_ids);
    final Set<String> next = new HashSet<>(prefs.getStringSet(key, Collections.emptySet()));
    if (enabled) {
      next.add(Objects.requireNonNull(keyboardId));
    } else {
      next.remove(Objects.requireNonNull(keyboardId));
    }
    prefs.edit().putStringSet(key, next).apply();
    bumpGeneration(context);
  }

  public static void clearEnabledForPack(@NonNull Context context, @NonNull String packId) {
    Objects.requireNonNull(context);
    Objects.requireNonNull(packId);
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(context);
    final String key = context.getString(R.string.settings_key_custom_keyboards_enabled_ids);
    final Set<String> next = new HashSet<>(prefs.getStringSet(key, Collections.emptySet()));
    final String prefix = "pack::" + packId + "::";
    final Set<String> toRemove = new HashSet<>();
    for (String id : next) {
      if (id != null && id.startsWith(prefix)) {
        toRemove.add(id);
      }
    }
    next.removeAll(toRemove);
    prefs.edit().putStringSet(key, next).apply();
    bumpGeneration(context);
  }

  public static void removeAllDataForPack(@NonNull Context context, @NonNull String packId) {
    clearEnabledForPack(context, packId);
    setSelectedThemeIdForPack(context, packId, null);
  }

  public static int enabledCount(@NonNull Context context) {
    return getEnabledKeyboardIds(context).size();
  }

  /** Whether the editor's "how to use" hint is collapsed. Defaults to expanded for first use. */
  public static boolean isInstructionsCollapsed(@NonNull Context context) {
    final SharedPreferences prefs =
        DirectBootAwareSharedPreferences.create(Objects.requireNonNull(context));
    final String key =
        context.getString(R.string.settings_key_custom_keyboards_instructions_collapsed);
    return prefs.getBoolean(key, false);
  }

  public static void setInstructionsCollapsed(@NonNull Context context, boolean collapsed) {
    final SharedPreferences prefs =
        DirectBootAwareSharedPreferences.create(Objects.requireNonNull(context));
    final String key =
        context.getString(R.string.settings_key_custom_keyboards_instructions_collapsed);
    prefs.edit().putBoolean(key, collapsed).apply();
  }

  public static void bumpGeneration(@NonNull Context context) {
    final SharedPreferences prefs =
        DirectBootAwareSharedPreferences.create(Objects.requireNonNull(context));
    final String key = context.getString(R.string.settings_key_custom_keyboards_generation);
    prefs.edit().putString(key, Long.toString(System.currentTimeMillis())).apply();
  }

  @Nullable
  public static String getSelectedThemeIdForPack(@NonNull Context context, @NonNull String packId) {
    Objects.requireNonNull(context);
    Objects.requireNonNull(packId);
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(context);
    final String key = context.getString(R.string.settings_key_custom_keyboards_pack_theme_mapping);
    final Set<String> stored = prefs.getStringSet(key, null);
    if (stored == null || stored.isEmpty()) return null;

    final String prefix = packId + "=";
    for (String entry : stored) {
      if (entry != null && entry.startsWith(prefix)) {
        String themeId = entry.substring(prefix.length());
        return themeId.isEmpty() ? null : themeId;
      }
    }
    return null;
  }

  public static void setSelectedThemeIdForPack(
      @NonNull Context context, @NonNull String packId, @Nullable String themeId) {
    Objects.requireNonNull(context);
    Objects.requireNonNull(packId);
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(context);
    final String key = context.getString(R.string.settings_key_custom_keyboards_pack_theme_mapping);
    final Set<String> next = new HashSet<>(prefs.getStringSet(key, Collections.emptySet()));

    final String prefix = packId + "=";
    final Set<String> toRemove = new HashSet<>();
    for (String item : next) {
      if (item != null && item.startsWith(prefix)) {
        toRemove.add(item);
      }
    }
    next.removeAll(toRemove);

    if (themeId != null && !themeId.trim().isEmpty()) {
      next.add(prefix + themeId.trim());
    }
    prefs.edit().putStringSet(key, next).apply();
  }
}
