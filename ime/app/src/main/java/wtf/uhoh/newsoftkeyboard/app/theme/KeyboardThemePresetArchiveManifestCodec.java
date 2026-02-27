package wtf.uhoh.newsoftkeyboard.app.theme;

import android.graphics.BitmapFactory;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;

final class KeyboardThemePresetArchiveManifestCodec {

  static final int ARCHIVE_VERSION = 1;

  static final String ENTRY_MANIFEST = "manifest.json";
  static final String ENTRY_PREVIEW = "preview.png";
  static final String ENTRY_KEY_FONT = "key_font.ttf";

  private static final String KEY_VERSION = "version";
  private static final String KEY_EXPORTED_AT_MILLIS = "exported_at_millis";
  private static final String KEY_EXPORTED_BY_APP_ID = "exported_by_app_id";
  private static final String KEY_EXPORTED_BY_VERSION_NAME = "exported_by_version_name";
  private static final String KEY_EXPORTED_BY_VERSION_CODE = "exported_by_version_code";
  private static final String KEY_BASE_THEME_ID = "base_theme_id";
  private static final String KEY_BASE_THEME_NAME = "base_theme_name";
  private static final String KEY_PRESET_NAME = "preset_name";

  private static final String KEY_PREVIEW = "preview";
  private static final String KEY_PREVIEW_HAS = "has";
  private static final String KEY_PREVIEW_ENTRY = "entry";
  private static final String KEY_PREVIEW_WIDTH_PX = "width_px";
  private static final String KEY_PREVIEW_HEIGHT_PX = "height_px";

  private KeyboardThemePresetArchiveManifestCodec() {}

  @NonNull
  static JSONObject parseManifest(@NonNull String manifestText) throws IOException {
    try {
      return new JSONObject(manifestText);
    } catch (JSONException e) {
      throw new IOException("Failed to parse preset manifest.", e);
    }
  }

  static void writeExportMetadataToManifest(
      @NonNull JSONObject manifest,
      long exportedAtMillis,
      @NonNull String exportedByAppId,
      @NonNull String exportedByVersionName,
      long exportedByVersionCode,
      @NonNull String baseThemeId,
      @Nullable String baseThemeName,
      @NonNull String presetName)
      throws JSONException {
    manifest.put(KEY_VERSION, ARCHIVE_VERSION);
    manifest.put(KEY_EXPORTED_AT_MILLIS, exportedAtMillis);
    manifest.put(KEY_EXPORTED_BY_APP_ID, exportedByAppId);
    manifest.put(KEY_EXPORTED_BY_VERSION_NAME, exportedByVersionName);
    manifest.put(KEY_EXPORTED_BY_VERSION_CODE, exportedByVersionCode);
    manifest.put(KEY_BASE_THEME_ID, baseThemeId);
    manifest.put(KEY_PRESET_NAME, presetName);
    if (baseThemeName != null) {
      manifest.put(KEY_BASE_THEME_NAME, baseThemeName);
    }
  }

  static void writePreviewToManifest(
      @NonNull JSONObject manifest, boolean includePreview, @Nullable byte[] previewPngBytes)
      throws JSONException {
    final JSONObject preview = new JSONObject();
    preview.put(KEY_PREVIEW_HAS, includePreview);
    if (includePreview) {
      preview.put(KEY_PREVIEW_ENTRY, ENTRY_PREVIEW);
      if (previewPngBytes != null) {
        final int[] previewSize = readBitmapSizeSafe(previewPngBytes);
        if (previewSize != null) {
          preview.put(KEY_PREVIEW_WIDTH_PX, previewSize[0]);
          preview.put(KEY_PREVIEW_HEIGHT_PX, previewSize[1]);
        }
      }
    }
    manifest.put(KEY_PREVIEW, preview);
  }

  @NonNull
  static KeyboardThemePresetTransfer.PresetArchiveInfo readArchiveInfo(@NonNull JSONObject manifest)
      throws IOException {
    final int version = manifest.optInt(KEY_VERSION, 0);
    final String baseThemeId = manifest.optString(KEY_BASE_THEME_ID, "");
    if (version != ARCHIVE_VERSION) {
      throw new IOException("Unsupported preset archive version: " + version);
    }
    if (baseThemeId.isEmpty()) {
      throw new IOException("Missing base_theme_id in preset manifest.");
    }
    String presetName = manifest.optString(KEY_PRESET_NAME, "");
    presetName = presetName == null ? "" : presetName.trim();
    if (presetName.isEmpty()) presetName = "Preset";

    @Nullable String baseThemeName = manifest.optString(KEY_BASE_THEME_NAME, null);
    baseThemeName = baseThemeName == null ? null : baseThemeName.trim();
    if (baseThemeName != null && baseThemeName.isEmpty()) baseThemeName = null;

    final long exportedAtMillis = manifest.optLong(KEY_EXPORTED_AT_MILLIS, 0L);

    @Nullable String exportedByVersionName = manifest.optString(KEY_EXPORTED_BY_VERSION_NAME, null);
    exportedByVersionName = exportedByVersionName == null ? null : exportedByVersionName.trim();
    if (exportedByVersionName != null && exportedByVersionName.isEmpty()) {
      exportedByVersionName = null;
    }

    final boolean hasPreviewImage = hasPreviewImage(manifest);

    final boolean hasWallpaper = KeyboardThemePresetArchiveWallpaper.hasWallpaper(manifest);

    final JSONObject overridesJson =
        KeyboardThemePresetArchiveOverrides.readOverridesJson(manifest);
    final boolean hasColors = KeyboardThemePresetArchiveOverrides.hasColors(overridesJson);
    final boolean hasTypography = KeyboardThemePresetArchiveOverrides.hasTypography(overridesJson);
    final boolean hasShadows = KeyboardThemePresetArchiveOverrides.hasShadows(overridesJson);
    final boolean hasCustomFont = KeyboardThemePresetArchiveOverrides.hasCustomFont(overridesJson);
    @Nullable
    final String customFontName = KeyboardThemePresetArchiveOverrides.customFontName(overridesJson);

    return new KeyboardThemePresetTransfer.PresetArchiveInfo(
        version,
        baseThemeId,
        baseThemeName,
        presetName,
        exportedAtMillis,
        exportedByVersionName,
        hasWallpaper,
        hasColors,
        hasTypography,
        hasShadows,
        hasPreviewImage,
        hasCustomFont,
        customFontName);
  }

  static void validateForImport(@NonNull JSONObject manifest, @NonNull String expectedBaseThemeId)
      throws IOException {
    final int version = manifest.optInt(KEY_VERSION, 0);
    if (version != ARCHIVE_VERSION) {
      throw new IOException("Unsupported preset archive version: " + version);
    }

    final String baseThemeId = manifest.optString(KEY_BASE_THEME_ID, "");
    if (baseThemeId.isEmpty()) {
      throw new IOException("Missing base_theme_id in preset manifest.");
    }
    if (!expectedBaseThemeId.equals(baseThemeId)) {
      throw new IOException("Preset belongs to a different base theme: " + baseThemeId);
    }
  }

  static boolean expectsKeyFont(@NonNull JSONObject manifest) {
    final JSONObject overridesJson =
        KeyboardThemePresetArchiveOverrides.readOverridesJson(manifest);
    return KeyboardThemePresetArchiveOverrides.hasCustomFont(overridesJson);
  }

  static boolean expectsWallpaper(@NonNull JSONObject manifest) {
    return KeyboardThemePresetArchiveWallpaper.hasWallpaper(manifest);
  }

  @NonNull
  static String resolveRequestedNameForImport(
      @NonNull JSONObject manifest, @Nullable String requestedNameOverride) {
    String requestedName =
        requestedNameOverride != null
            ? requestedNameOverride
            : manifest.optString(KEY_PRESET_NAME, "");
    requestedName = requestedName == null ? "" : requestedName.trim();
    if (requestedName.isEmpty()) requestedName = "Imported preset";
    return requestedName;
  }

  private static boolean hasPreviewImage(@NonNull JSONObject manifest) {
    final JSONObject previewJson = manifest.optJSONObject(KEY_PREVIEW);
    return previewJson != null
        && previewJson.optBoolean(KEY_PREVIEW_HAS, false)
        && ENTRY_PREVIEW.equals(previewJson.optString(KEY_PREVIEW_ENTRY, ""));
  }

  @Nullable
  private static int[] readBitmapSizeSafe(@NonNull byte[] bitmapBytes) {
    final BitmapFactory.Options bounds = new BitmapFactory.Options();
    bounds.inJustDecodeBounds = true;
    BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length, bounds);
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
    return new int[] {bounds.outWidth, bounds.outHeight};
  }
}
