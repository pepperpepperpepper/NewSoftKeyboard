package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

/**
 * Stores user-created theme presets.
 *
 * <p>A preset is an identifier used to scope per-theme appearance overrides (wallpaper, colors,
 * typography, etc.) and can be used to keep multiple named "looks" for the same base theme.
 */
public class KeyboardThemePresetStore {

  private static final String PREF_ACTIVE_PRESET_PREFIX = "theme_preset_active::";
  private static final String PREF_PRESET_IDS_PREFIX = "theme_preset_ids::";
  private static final String PREF_PRESET_NAME_PREFIX = "theme_preset_name::";
  private static final String PREF_PRESET_BASE_THEME_PREFIX = "theme_preset_base_theme::";
  private static final String PREF_PRESET_CREATED_AT_PREFIX = "theme_preset_created_at::";

  private static final String USER_PRESET_ID_PREFIX = "user_preset::";

  private final SharedPreferences prefs;
  private final KeyboardThemePresetAppBindingStore appBindingStore;
  private final KeyboardThemePresetPreviewStore previewStore;

  public KeyboardThemePresetStore(@NonNull Context context) {
    prefs = DirectBootAwareSharedPreferences.create(context.getApplicationContext());
    appBindingStore = new KeyboardThemePresetAppBindingStore(prefs);
    previewStore = new KeyboardThemePresetPreviewStore(context.getApplicationContext());
  }

  @NonNull
  public static String activePresetKey(@NonNull String baseThemeId) {
    return PREF_ACTIVE_PRESET_PREFIX + baseThemeId;
  }

  @NonNull
  private static String presetIdsKey(@NonNull String baseThemeId) {
    return PREF_PRESET_IDS_PREFIX + baseThemeId;
  }

  @NonNull
  private static String presetNameKey(@NonNull String presetId) {
    return PREF_PRESET_NAME_PREFIX + presetId;
  }

  @NonNull
  private static String presetBaseThemeKey(@NonNull String presetId) {
    return PREF_PRESET_BASE_THEME_PREFIX + presetId;
  }

  @NonNull
  private static String presetCreatedAtKey(@NonNull String presetId) {
    return PREF_PRESET_CREATED_AT_PREFIX + presetId;
  }

  @NonNull
  public String getActivePresetId(@NonNull String baseThemeId) {
    final String globalPresetId = resolveStoredActivePresetId(baseThemeId);

    final String packageName = appBindingStore.getCurrentImePackageName();
    if (packageName == null) return globalPresetId;

    final String sessionOverrideId =
        KeyboardThemeSessionOverrideStore.getOverridePresetId(packageName, baseThemeId);
    if (sessionOverrideId != null) {
      if (sessionOverrideId.equals(baseThemeId)) return baseThemeId;
      return isValidUserPresetIdForBaseTheme(baseThemeId, sessionOverrideId)
          ? sessionOverrideId
          : globalPresetId;
    }

    final String boundPresetId = appBindingStore.getBoundPresetId(baseThemeId, packageName);
    if (boundPresetId == null) return globalPresetId;
    if (boundPresetId.equals(baseThemeId)) return baseThemeId;

    return isValidUserPresetIdForBaseTheme(baseThemeId, boundPresetId)
        ? boundPresetId
        : globalPresetId;
  }

  public void setActivePresetId(@NonNull String baseThemeId, @NonNull String presetId) {
    if (!presetId.equals(baseThemeId)) {
      final Set<String> presets =
          prefs.getStringSet(presetIdsKey(baseThemeId), Collections.emptySet());
      if (presets == null || !presets.contains(presetId)) {
        throw new IllegalArgumentException(
            "Unknown preset " + presetId + " for base theme " + baseThemeId);
      }
    }
    prefs.edit().putString(activePresetKey(baseThemeId), presetId).apply();
  }

  @NonNull
  public List<Preset> listPresets(@NonNull String baseThemeId) {
    final List<Preset> result = new ArrayList<>();
    result.add(Preset.defaultPreset(baseThemeId));

    final Set<String> ids = prefs.getStringSet(presetIdsKey(baseThemeId), Collections.emptySet());
    if (ids == null || ids.isEmpty()) return result;

    final List<Preset> userPresets = new ArrayList<>(ids.size());
    for (String id : ids) {
      if (id == null) continue;
      final String presetBaseThemeId = prefs.getString(presetBaseThemeKey(id), null);
      if (!baseThemeId.equals(presetBaseThemeId)) continue;
      userPresets.add(
          new Preset(
              id,
              prefs.getString(presetNameKey(id), ""),
              baseThemeId,
              prefs.getLong(presetCreatedAtKey(id), 0L),
              false));
    }
    userPresets.sort(Comparator.comparingLong(Preset::createdAtMillis).reversed());
    result.addAll(userPresets);
    return result;
  }

  @NonNull
  public Preset createPreset(@NonNull String baseThemeId, @NonNull String name) {
    final String trimmed = name.trim();
    if (trimmed.isEmpty()) throw new IllegalArgumentException("Preset name must not be empty.");

    final String id = USER_PRESET_ID_PREFIX + UUID.randomUUID();
    final Set<String> existing = prefs.getStringSet(presetIdsKey(baseThemeId), null);
    final Set<String> updated = existing == null ? new HashSet<>() : new HashSet<>(existing);
    updated.add(id);

    final long now = System.currentTimeMillis();
    prefs
        .edit()
        .putStringSet(presetIdsKey(baseThemeId), updated)
        .putString(presetNameKey(id), trimmed)
        .putString(presetBaseThemeKey(id), baseThemeId)
        .putLong(presetCreatedAtKey(id), now)
        .apply();

    return new Preset(id, trimmed, baseThemeId, now, false);
  }

  public void renamePreset(@NonNull String presetId, @NonNull String newName) {
    final String trimmed = newName.trim();
    if (trimmed.isEmpty()) throw new IllegalArgumentException("Preset name must not be empty.");
    prefs.edit().putString(presetNameKey(presetId), trimmed).apply();
  }

  public void deletePreset(@NonNull String presetId) {
    final String baseThemeId = prefs.getString(presetBaseThemeKey(presetId), null);
    previewStore.deletePreviewBestEffort(presetId);
    if (baseThemeId == null) return;

    final Set<String> existing = prefs.getStringSet(presetIdsKey(baseThemeId), null);
    final Set<String> updated = existing == null ? new HashSet<>() : new HashSet<>(existing);
    updated.remove(presetId);

    final SharedPreferences.Editor editor = prefs.edit();
    editor.putStringSet(presetIdsKey(baseThemeId), updated);
    editor.remove(presetNameKey(presetId));
    editor.remove(presetBaseThemeKey(presetId));
    editor.remove(presetCreatedAtKey(presetId));

    final String active = prefs.getString(activePresetKey(baseThemeId), null);
    if (presetId.equals(active)) {
      editor.putString(activePresetKey(baseThemeId), baseThemeId);
    }

    editor.apply();

    appBindingStore.removePresetFromBindings(baseThemeId, presetId);
  }

  public boolean hasPresetPreview(@NonNull String presetId) {
    return previewStore.hasPreview(presetId);
  }

  public void savePresetPreviewPngBytesBestEffort(
      @NonNull String presetId, @Nullable byte[] pngBytes) {
    if (pngBytes == null || pngBytes.length == 0) return;
    try {
      previewStore.savePreviewPngBytes(presetId, pngBytes);
    } catch (IOException ignored) {
      // best-effort
    }
  }

  @Nullable
  public Bitmap readPresetPreviewBitmap(@NonNull String presetId, int maxWidthPx, int maxHeightPx) {
    return previewStore.readPreviewBitmap(presetId, maxWidthPx, maxHeightPx);
  }

  @Nullable
  public String getPresetName(@NonNull String presetId) {
    return prefs.getString(presetNameKey(presetId), null);
  }

  public boolean isDefaultPreset(@NonNull String baseThemeId, @NonNull String presetId) {
    return baseThemeId.equals(presetId);
  }

  @NonNull
  private String resolveStoredActivePresetId(@NonNull String baseThemeId) {
    final String stored = prefs.getString(activePresetKey(baseThemeId), null);
    if (stored == null || stored.equals(baseThemeId)) return baseThemeId;
    return isValidUserPresetIdForBaseTheme(baseThemeId, stored) ? stored : baseThemeId;
  }

  private boolean isValidUserPresetIdForBaseTheme(
      @NonNull String baseThemeId, @NonNull String presetId) {
    final String presetBaseThemeId = prefs.getString(presetBaseThemeKey(presetId), null);
    if (!baseThemeId.equals(presetBaseThemeId)) return false;

    final Set<String> presets =
        prefs.getStringSet(presetIdsKey(baseThemeId), Collections.emptySet());
    return presets != null && presets.contains(presetId);
  }

  public static final class Preset {
    @NonNull private final String id;
    @NonNull private final String name;
    @NonNull private final String baseThemeId;
    private final long createdAtMillis;
    private final boolean isDefault;

    private Preset(
        @NonNull String id,
        @NonNull String name,
        @NonNull String baseThemeId,
        long createdAtMillis,
        boolean isDefault) {
      this.id = id;
      this.name = name;
      this.baseThemeId = baseThemeId;
      this.createdAtMillis = createdAtMillis;
      this.isDefault = isDefault;
    }

    @NonNull
    public String id() {
      return id;
    }

    @NonNull
    public String name() {
      return name;
    }

    @NonNull
    public String baseThemeId() {
      return baseThemeId;
    }

    public long createdAtMillis() {
      return createdAtMillis;
    }

    public boolean isDefault() {
      return isDefault;
    }

    @NonNull
    public static Preset defaultPreset(@NonNull String baseThemeId) {
      return new Preset(baseThemeId, "Default", baseThemeId, 0L, true);
    }
  }
}
