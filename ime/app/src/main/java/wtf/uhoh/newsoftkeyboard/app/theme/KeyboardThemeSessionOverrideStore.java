package wtf.uhoh.newsoftkeyboard.app.theme;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** In-memory, session-only overrides for theme preset selection (per focused app). */
public final class KeyboardThemeSessionOverrideStore {

  private static final Object LOCK = new Object();

  @Nullable private static String sPackageName;
  @Nullable private static String sBaseThemeId;
  @Nullable private static String sPresetId;

  private KeyboardThemeSessionOverrideStore() {}

  public static void setOverride(
      @NonNull String packageName, @NonNull String baseThemeId, @NonNull String presetId) {
    synchronized (LOCK) {
      sPackageName = packageName.trim();
      sBaseThemeId = baseThemeId.trim();
      sPresetId = presetId.trim();
    }
  }

  @Nullable
  public static String getOverridePresetId(
      @NonNull String packageName, @NonNull String baseThemeId) {
    synchronized (LOCK) {
      if (sPackageName == null || sBaseThemeId == null || sPresetId == null) return null;
      if (!sPackageName.equals(packageName)) return null;
      if (!sBaseThemeId.equals(baseThemeId)) return null;
      final String trimmed = sPresetId.trim();
      return trimmed.isEmpty() ? null : trimmed;
    }
  }

  public static void clearForPackage(@NonNull String packageName) {
    synchronized (LOCK) {
      if (sPackageName == null) return;
      if (!sPackageName.equals(packageName)) return;
      sPackageName = null;
      sBaseThemeId = null;
      sPresetId = null;
    }
  }

  public static void clearAll() {
    synchronized (LOCK) {
      sPackageName = null;
      sBaseThemeId = null;
      sPresetId = null;
    }
  }

  public static boolean hasAnyOverride() {
    synchronized (LOCK) {
      return sPackageName != null && sBaseThemeId != null && sPresetId != null;
    }
  }
}
