package wtf.uhoh.newsoftkeyboard.app.theme;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;

final class KeyboardThemePresetArchiveOverrides {

  private static final String KEY_OVERRIDES = "overrides";

  private KeyboardThemePresetArchiveOverrides() {}

  @Nullable
  static JSONObject readOverridesJson(@NonNull JSONObject manifest) {
    return manifest.optJSONObject(KEY_OVERRIDES);
  }

  static boolean writeToManifest(
      @NonNull JSONObject manifest,
      @NonNull KeyboardThemeUserOverridesStore overridesStore,
      @NonNull String presetId)
      throws JSONException {
    final JSONObject overrides = new JSONObject();
    KeyboardThemePresetArchiveColorOverrides.writeToJson(overrides, overridesStore, presetId);
    final boolean usesCustomFont =
        KeyboardThemePresetArchiveTypographyOverrides.writeToJson(
            overrides, overridesStore, presetId);
    KeyboardThemePresetArchiveShadowOverrides.writeToJson(overrides, overridesStore, presetId);

    if (overrides.length() > 0) {
      manifest.put(KEY_OVERRIDES, overrides);
    }

    return usesCustomFont;
  }

  static void applyFromManifest(
      @NonNull JSONObject manifest,
      @NonNull KeyboardThemeUserOverridesStore overridesStore,
      @NonNull String presetId,
      @Nullable File tempKeyFontFile)
      throws IOException {
    final JSONObject overridesJson = manifest.optJSONObject(KEY_OVERRIDES);
    if (overridesJson == null) return;

    KeyboardThemePresetArchiveColorOverrides.applyFromJson(overridesStore, presetId, overridesJson);
    KeyboardThemePresetArchiveTypographyOverrides.applyFromJson(
        overridesStore, presetId, overridesJson);
    KeyboardThemePresetArchiveShadowOverrides.applyFromJson(
        overridesStore, presetId, overridesJson);

    if (KeyboardThemePresetArchiveTypographyOverrides.hasCustomFont(overridesJson)) {
      if (tempKeyFontFile == null || !tempKeyFontFile.isFile()) {
        throw new IOException("Missing custom key font file in archive.");
      }
      overridesStore.importCustomKeyFontFromFile(
          presetId,
          tempKeyFontFile,
          KeyboardThemePresetArchiveTypographyOverrides.customFontName(overridesJson));
    }
  }

  static boolean hasColors(@Nullable JSONObject overridesJson) {
    return KeyboardThemePresetArchiveColorOverrides.hasColors(overridesJson);
  }

  static boolean hasTypography(@Nullable JSONObject overridesJson) {
    return KeyboardThemePresetArchiveTypographyOverrides.hasTypography(overridesJson);
  }

  static boolean hasShadows(@Nullable JSONObject overridesJson) {
    return KeyboardThemePresetArchiveShadowOverrides.hasShadows(overridesJson);
  }

  static boolean hasCustomFont(@Nullable JSONObject overridesJson) {
    return KeyboardThemePresetArchiveTypographyOverrides.hasCustomFont(overridesJson);
  }

  @Nullable
  static String customFontName(@Nullable JSONObject overridesJson) {
    return KeyboardThemePresetArchiveTypographyOverrides.customFontName(overridesJson);
  }
}
