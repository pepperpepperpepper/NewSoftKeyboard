package wtf.uhoh.newsoftkeyboard.app.theme;

import android.content.Context;
import android.graphics.BitmapFactory;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class KeyboardThemePresetArchiveWallpaper {

  static final String ENTRY_WALLPAPER = "wallpaper.webp";
  static final long MAX_WALLPAPER_BYTES = 30L * 1024L * 1024L;

  private static final String KEY_WALLPAPER = "wallpaper";
  private static final String KEY_WALLPAPER_HAS = "has_wallpaper";
  private static final String KEY_WALLPAPER_ENTRY = "wallpaper_entry";
  private static final String KEY_WALLPAPER_DIM_PERCENT = "dim_percent";
  private static final String KEY_WALLPAPER_MODE = "mode";
  private static final String KEY_WALLPAPER_KEY_ALPHA_PERCENT = "key_alpha_percent";
  private static final String KEY_WALLPAPER_KEY_BLEND_MODE = "key_blend_mode";
  private static final String KEY_WALLPAPER_BACKGROUND_LAYER_ORDER = "background_layer_order";
  private static final String KEY_WALLPAPER_KEY_LAYER_ORDER = "key_layer_order";
  private static final String KEY_WALLPAPER_BACKGROUND_LAYER_STACK = "background_layer_stack";
  private static final String KEY_WALLPAPER_KEY_LAYER_STACK = "key_layer_stack";
  private static final String KEY_WALLPAPER_KEY_COLOR_WASH_COLOR = "key_color_wash_color";
  private static final String KEY_WALLPAPER_KEY_COLOR_WASH_BLEND_MODE = "key_color_wash_blend_mode";
  private static final String KEY_WALLPAPER_KEY_HIGHLIGHT_PERCENT = "key_highlight_percent";
  private static final String KEY_WALLPAPER_KEY_HIGHLIGHT_BLEND_MODE = "key_highlight_blend_mode";
  private static final String KEY_WALLPAPER_KEY_GRADIENT_BLEND_MODE = "key_gradient_blend_mode";
  private static final String KEY_WALLPAPER_KEY_VIGNETTE_BLEND_MODE = "key_vignette_blend_mode";
  private static final String KEY_WALLPAPER_KEY_GRAIN_BLEND_MODE = "key_grain_blend_mode";
  private static final String KEY_WALLPAPER_BACKGROUND_TINT_BLEND_MODE =
      "background_tint_blend_mode";
  private static final String KEY_WALLPAPER_BACKGROUND_DIM_BLEND_MODE = "background_dim_blend_mode";
  private static final String KEY_WALLPAPER_BACKGROUND_GRADIENT_BLEND_MODE =
      "background_gradient_blend_mode";
  private static final String KEY_WALLPAPER_BACKGROUND_VIGNETTE_BLEND_MODE =
      "background_vignette_blend_mode";
  private static final String KEY_WALLPAPER_BACKGROUND_GRAIN_BLEND_MODE =
      "background_grain_blend_mode";
  private static final String KEY_WALLPAPER_SPECIAL_KEY_ALPHA_PERCENT = "special_key_alpha_percent";
  private static final String KEY_WALLPAPER_SPACEBAR_ALPHA_PERCENT = "spacebar_alpha_percent";
  private static final String KEY_WALLPAPER_MODIFIER_KEY_ALPHA_PERCENT =
      "modifier_key_alpha_percent";
  private static final String KEY_WALLPAPER_ENTER_KEY_ALPHA_PERCENT = "enter_key_alpha_percent";
  private static final String KEY_WALLPAPER_ROTATION_DEGREES = "rotation_degrees";
  private static final String KEY_WALLPAPER_SCALE_MODE = "scale_mode";
  private static final String KEY_WALLPAPER_ANCHOR = "anchor";
  private static final String KEY_WALLPAPER_MATCH_KEY_SHAPE = "match_key_shape";
  private static final String KEY_WALLPAPER_QUALITY = "quality";
  private static final String KEY_WALLPAPER_GRADIENT_PERCENT = "gradient_percent";
  private static final String KEY_WALLPAPER_VIGNETTE_PERCENT = "vignette_percent";
  private static final String KEY_WALLPAPER_GRAIN_PERCENT = "grain_percent";
  private static final String KEY_WALLPAPER_SATURATION_PERCENT = "saturation_percent";
  private static final String KEY_WALLPAPER_CONTRAST_PERCENT = "contrast_percent";
  private static final String KEY_WALLPAPER_BRIGHTNESS_PERCENT = "brightness_percent";
  private static final String KEY_WALLPAPER_TEMPERATURE_PERCENT = "temperature_percent";

  private KeyboardThemePresetArchiveWallpaper() {}

  static boolean hasWallpaper(@NonNull JSONObject manifest) {
    final JSONObject wallpaperJson = manifest.optJSONObject(KEY_WALLPAPER);
    return wallpaperJson != null && wallpaperJson.optBoolean(KEY_WALLPAPER_HAS, false);
  }

  static void writeToManifest(
      @NonNull JSONObject manifest,
      @NonNull KeyboardWallpaperOverrideStore wallpaperStore,
      @NonNull String presetId,
      boolean hasWallpaper)
      throws JSONException {
    final JSONObject wallpaper = new JSONObject();
    wallpaper.put(KEY_WALLPAPER_HAS, hasWallpaper);
    if (hasWallpaper) {
      wallpaper.put(KEY_WALLPAPER_ENTRY, ENTRY_WALLPAPER);
      wallpaper.put(KEY_WALLPAPER_DIM_PERCENT, wallpaperStore.getDimPercent(presetId));
      wallpaper.put(KEY_WALLPAPER_MODE, wallpaperStore.getWallpaperMode(presetId));
      wallpaper.put(KEY_WALLPAPER_KEY_ALPHA_PERCENT, wallpaperStore.getKeyAlphaPercent(presetId));
      wallpaper.put(KEY_WALLPAPER_KEY_BLEND_MODE, wallpaperStore.getKeyBlendMode(presetId));

      final JSONArray backgroundLayerOrder = new JSONArray();
      for (int layer : wallpaperStore.getBackgroundLayerOrder(presetId)) {
        backgroundLayerOrder.put(layer);
      }
      wallpaper.put(KEY_WALLPAPER_BACKGROUND_LAYER_ORDER, backgroundLayerOrder);

      final JSONArray keyLayerOrder = new JSONArray();
      for (int layer : wallpaperStore.getKeyLayerOrder(presetId)) {
        keyLayerOrder.put(layer);
      }
      wallpaper.put(KEY_WALLPAPER_KEY_LAYER_ORDER, keyLayerOrder);

      if (wallpaperStore.hasBackgroundLayerStackOverride(presetId)) {
        wallpaper.put(
            KEY_WALLPAPER_BACKGROUND_LAYER_STACK,
            serializeLayerStack(wallpaperStore.getBackgroundLayerStack(presetId)));
      }
      if (wallpaperStore.hasKeyLayerStackOverride(presetId)) {
        wallpaper.put(
            KEY_WALLPAPER_KEY_LAYER_STACK,
            serializeLayerStack(wallpaperStore.getKeyLayerStack(presetId)));
      }
      if (wallpaperStore.hasKeyColorWashColorOverride(presetId)) {
        final Integer color = wallpaperStore.getKeyColorWashColor(presetId);
        if (color != null) {
          wallpaper.put(KEY_WALLPAPER_KEY_COLOR_WASH_COLOR, color);
        }
      }
      wallpaper.put(
          KEY_WALLPAPER_KEY_COLOR_WASH_BLEND_MODE,
          wallpaperStore.getKeyColorWashBlendMode(presetId));
      wallpaper.put(
          KEY_WALLPAPER_KEY_HIGHLIGHT_PERCENT, wallpaperStore.getKeyHighlightPercent(presetId));
      wallpaper.put(
          KEY_WALLPAPER_KEY_HIGHLIGHT_BLEND_MODE,
          wallpaperStore.getKeyHighlightBlendMode(presetId));
      wallpaper.put(
          KEY_WALLPAPER_KEY_GRADIENT_BLEND_MODE, wallpaperStore.getKeyGradientBlendMode(presetId));
      wallpaper.put(
          KEY_WALLPAPER_KEY_VIGNETTE_BLEND_MODE, wallpaperStore.getKeyVignetteBlendMode(presetId));
      wallpaper.put(
          KEY_WALLPAPER_KEY_GRAIN_BLEND_MODE, wallpaperStore.getKeyGrainBlendMode(presetId));
      wallpaper.put(
          KEY_WALLPAPER_BACKGROUND_TINT_BLEND_MODE,
          wallpaperStore.getBackgroundTintBlendMode(presetId));
      wallpaper.put(
          KEY_WALLPAPER_BACKGROUND_DIM_BLEND_MODE,
          wallpaperStore.getBackgroundDimBlendMode(presetId));
      wallpaper.put(
          KEY_WALLPAPER_BACKGROUND_GRADIENT_BLEND_MODE,
          wallpaperStore.getBackgroundGradientBlendMode(presetId));
      wallpaper.put(
          KEY_WALLPAPER_BACKGROUND_VIGNETTE_BLEND_MODE,
          wallpaperStore.getBackgroundVignetteBlendMode(presetId));
      wallpaper.put(
          KEY_WALLPAPER_BACKGROUND_GRAIN_BLEND_MODE,
          wallpaperStore.getBackgroundGrainBlendMode(presetId));
      if (wallpaperStore.hasSpecialKeyAlphaPercentOverride(presetId)) {
        wallpaper.put(
            KEY_WALLPAPER_SPECIAL_KEY_ALPHA_PERCENT,
            wallpaperStore.getSpecialKeyAlphaPercent(presetId));
      }
      if (wallpaperStore.hasSpacebarAlphaPercentOverride(presetId)) {
        wallpaper.put(
            KEY_WALLPAPER_SPACEBAR_ALPHA_PERCENT, wallpaperStore.getSpacebarAlphaPercent(presetId));
      }
      if (wallpaperStore.hasModifierKeyAlphaPercentOverride(presetId)) {
        wallpaper.put(
            KEY_WALLPAPER_MODIFIER_KEY_ALPHA_PERCENT,
            wallpaperStore.getModifierKeyAlphaPercent(presetId));
      }
      if (wallpaperStore.hasEnterKeyAlphaPercentOverride(presetId)) {
        wallpaper.put(
            KEY_WALLPAPER_ENTER_KEY_ALPHA_PERCENT,
            wallpaperStore.getEnterKeyAlphaPercent(presetId));
      }
      wallpaper.put(
          KEY_WALLPAPER_ROTATION_DEGREES, wallpaperStore.getWallpaperRotationDegrees(presetId));
      wallpaper.put(KEY_WALLPAPER_SCALE_MODE, wallpaperStore.getWallpaperScaleMode(presetId));
      wallpaper.put(KEY_WALLPAPER_ANCHOR, wallpaperStore.getWallpaperAnchor(presetId));
      wallpaper.put(KEY_WALLPAPER_MATCH_KEY_SHAPE, wallpaperStore.isMatchKeyShapeEnabled(presetId));
      wallpaper.put(KEY_WALLPAPER_QUALITY, wallpaperStore.getWallpaperQuality(presetId));
      wallpaper.put(KEY_WALLPAPER_GRADIENT_PERCENT, wallpaperStore.getGradientPercent(presetId));
      wallpaper.put(KEY_WALLPAPER_VIGNETTE_PERCENT, wallpaperStore.getVignettePercent(presetId));
      wallpaper.put(KEY_WALLPAPER_GRAIN_PERCENT, wallpaperStore.getGrainPercent(presetId));
      wallpaper.put(
          KEY_WALLPAPER_SATURATION_PERCENT, wallpaperStore.getSaturationPercent(presetId));
      wallpaper.put(KEY_WALLPAPER_CONTRAST_PERCENT, wallpaperStore.getContrastPercent(presetId));
      wallpaper.put(
          KEY_WALLPAPER_BRIGHTNESS_PERCENT, wallpaperStore.getBrightnessPercent(presetId));
      wallpaper.put(
          KEY_WALLPAPER_TEMPERATURE_PERCENT, wallpaperStore.getTemperaturePercent(presetId));
    }

    manifest.put(KEY_WALLPAPER, wallpaper);
  }

  static void applyFromManifest(
      @NonNull Context context,
      @NonNull KeyboardWallpaperOverrideStore store,
      @NonNull String presetId,
      @NonNull JSONObject manifest,
      @Nullable File tempWallpaperFile)
      throws IOException {
    final JSONObject wallpaperJson = manifest.optJSONObject(KEY_WALLPAPER);
    if (wallpaperJson == null) return;
    applyFromJson(context, store, presetId, wallpaperJson, tempWallpaperFile);
  }

  @NonNull
  private static JSONArray serializeLayerStack(@NonNull KeyboardWallpaperLayer[] layers) {
    final JSONArray out = new JSONArray();
    for (KeyboardWallpaperLayer layer : layers) {
      if (layer == null) continue;
      out.put(layer.toJson());
    }
    return out;
  }

  @Nullable
  private static KeyboardWallpaperLayer[] parseLayerStack(@Nullable JSONArray json) {
    if (json == null) return null;
    final java.util.ArrayList<KeyboardWallpaperLayer> layers = new java.util.ArrayList<>();
    for (int i = 0; i < json.length(); i++) {
      final JSONObject layerJson = json.optJSONObject(i);
      final KeyboardWallpaperLayer layer = KeyboardWallpaperLayer.fromJson(layerJson);
      if (layer != null) layers.add(layer);
    }
    return layers.toArray(new KeyboardWallpaperLayer[0]);
  }

  private interface IntConsumer {
    void accept(int value);
  }

  private static void setIntIfPresent(
      @NonNull JSONObject json, @NonNull String key, @NonNull IntConsumer consumer) {
    if (!json.has(key)) return;
    consumer.accept(json.optInt(key, 0));
  }

  private static void applyFromJson(
      @NonNull Context context,
      @NonNull KeyboardWallpaperOverrideStore store,
      @NonNull String presetId,
      @NonNull JSONObject json,
      @Nullable File tempWallpaperFile)
      throws IOException {
    final boolean hasWallpaper = json.optBoolean(KEY_WALLPAPER_HAS, false);
    final String entryName = json.optString(KEY_WALLPAPER_ENTRY, "");
    if (!hasWallpaper) return;

    if (!ENTRY_WALLPAPER.equals(entryName)) {
      throw new IOException("Unsupported wallpaper entry: " + entryName);
    }
    if (tempWallpaperFile == null || !tempWallpaperFile.isFile()) {
      throw new IOException("Missing wallpaper file in archive.");
    }

    final File target = store.getWallpaperFile(presetId);
    try (InputStream in = new FileInputStream(tempWallpaperFile);
        OutputStream out = new FileOutputStream(target)) {
      KeyboardThemePresetTransfer.copy(in, out, MAX_WALLPAPER_BYTES);
    }

    final BitmapFactory.Options bounds = new BitmapFactory.Options();
    bounds.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(target.getAbsolutePath(), bounds);
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
      //noinspection ResultOfMethodCallIgnored
      target.delete();
      throw new IOException("Wallpaper file is not a valid bitmap.");
    }

    store.setDimPercent(presetId, json.optInt(KEY_WALLPAPER_DIM_PERCENT, 0));
    store.setWallpaperMode(presetId, json.optInt(KEY_WALLPAPER_MODE, 0));
    store.setKeyAlphaPercent(presetId, json.optInt(KEY_WALLPAPER_KEY_ALPHA_PERCENT, 0));
    store.setKeyBlendMode(
        presetId,
        json.optInt(
            KEY_WALLPAPER_KEY_BLEND_MODE,
            KeyboardWallpaperOverrideConstants.WALLPAPER_BLEND_MODE_NORMAL));
    final JSONArray keyLayerOrder = json.optJSONArray(KEY_WALLPAPER_KEY_LAYER_ORDER);
    if (keyLayerOrder != null) {
      final int[] order = new int[keyLayerOrder.length()];
      for (int i = 0; i < keyLayerOrder.length(); i++) {
        order[i] = keyLayerOrder.optInt(i, -1);
      }
      store.setKeyLayerOrder(presetId, order);
    }
    final JSONArray backgroundLayerOrder = json.optJSONArray(KEY_WALLPAPER_BACKGROUND_LAYER_ORDER);
    if (backgroundLayerOrder != null) {
      final int[] order = new int[backgroundLayerOrder.length()];
      for (int i = 0; i < backgroundLayerOrder.length(); i++) {
        order[i] = backgroundLayerOrder.optInt(i, -1);
      }
      store.setBackgroundLayerOrder(presetId, order);
    }
    final KeyboardWallpaperLayer[] backgroundLayerStack =
        parseLayerStack(json.optJSONArray(KEY_WALLPAPER_BACKGROUND_LAYER_STACK));
    if (backgroundLayerStack != null) {
      store.setBackgroundLayerStack(presetId, backgroundLayerStack);
    }
    final KeyboardWallpaperLayer[] keyLayerStack =
        parseLayerStack(json.optJSONArray(KEY_WALLPAPER_KEY_LAYER_STACK));
    if (keyLayerStack != null) {
      store.setKeyLayerStack(presetId, keyLayerStack);
    }
    setIntIfPresent(
        json,
        KEY_WALLPAPER_KEY_COLOR_WASH_COLOR,
        value -> store.setKeyColorWashColor(presetId, value));
    setIntIfPresent(
        json,
        KEY_WALLPAPER_KEY_COLOR_WASH_BLEND_MODE,
        value -> store.setKeyColorWashBlendMode(presetId, value));
    setIntIfPresent(
        json,
        KEY_WALLPAPER_KEY_HIGHLIGHT_PERCENT,
        value -> store.setKeyHighlightPercent(presetId, value));
    setIntIfPresent(
        json,
        KEY_WALLPAPER_KEY_HIGHLIGHT_BLEND_MODE,
        value -> store.setKeyHighlightBlendMode(presetId, value));
    setIntIfPresent(
        json,
        KEY_WALLPAPER_KEY_GRADIENT_BLEND_MODE,
        value -> store.setKeyGradientBlendMode(presetId, value));
    setIntIfPresent(
        json,
        KEY_WALLPAPER_KEY_VIGNETTE_BLEND_MODE,
        value -> store.setKeyVignetteBlendMode(presetId, value));
    setIntIfPresent(
        json,
        KEY_WALLPAPER_KEY_GRAIN_BLEND_MODE,
        value -> store.setKeyGrainBlendMode(presetId, value));
    setIntIfPresent(
        json,
        KEY_WALLPAPER_BACKGROUND_TINT_BLEND_MODE,
        value -> store.setBackgroundTintBlendMode(presetId, value));
    setIntIfPresent(
        json,
        KEY_WALLPAPER_BACKGROUND_DIM_BLEND_MODE,
        value -> store.setBackgroundDimBlendMode(presetId, value));
    setIntIfPresent(
        json,
        KEY_WALLPAPER_BACKGROUND_GRADIENT_BLEND_MODE,
        value -> store.setBackgroundGradientBlendMode(presetId, value));
    setIntIfPresent(
        json,
        KEY_WALLPAPER_BACKGROUND_VIGNETTE_BLEND_MODE,
        value -> store.setBackgroundVignetteBlendMode(presetId, value));
    setIntIfPresent(
        json,
        KEY_WALLPAPER_BACKGROUND_GRAIN_BLEND_MODE,
        value -> store.setBackgroundGrainBlendMode(presetId, value));
    setIntIfPresent(
        json,
        KEY_WALLPAPER_SPECIAL_KEY_ALPHA_PERCENT,
        value -> store.setSpecialKeyAlphaPercent(presetId, value));
    setIntIfPresent(
        json,
        KEY_WALLPAPER_SPACEBAR_ALPHA_PERCENT,
        value -> store.setSpacebarAlphaPercent(presetId, value));
    setIntIfPresent(
        json,
        KEY_WALLPAPER_MODIFIER_KEY_ALPHA_PERCENT,
        value -> store.setModifierKeyAlphaPercent(presetId, value));
    setIntIfPresent(
        json,
        KEY_WALLPAPER_ENTER_KEY_ALPHA_PERCENT,
        value -> store.setEnterKeyAlphaPercent(presetId, value));
    store.setWallpaperRotationDegrees(presetId, json.optInt(KEY_WALLPAPER_ROTATION_DEGREES, 0));
    store.setWallpaperScaleMode(presetId, json.optInt(KEY_WALLPAPER_SCALE_MODE, 0));
    store.setWallpaperAnchor(presetId, json.optInt(KEY_WALLPAPER_ANCHOR, 0));
    final boolean matchKeyShape = json.optBoolean(KEY_WALLPAPER_MATCH_KEY_SHAPE, false);
    if (json.has(KEY_WALLPAPER_QUALITY)) {
      int quality =
          json.optInt(
              KEY_WALLPAPER_QUALITY, KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_BALANCED);
      if (matchKeyShape) quality = KeyboardWallpaperOverrideConstants.WALLPAPER_QUALITY_HIGH;
      store.setWallpaperQuality(presetId, quality);
    }
    store.setMatchKeyShapeEnabled(presetId, matchKeyShape);
    store.setGradientPercent(presetId, json.optInt(KEY_WALLPAPER_GRADIENT_PERCENT, 0));
    store.setVignettePercent(presetId, json.optInt(KEY_WALLPAPER_VIGNETTE_PERCENT, 0));
    store.setGrainPercent(presetId, json.optInt(KEY_WALLPAPER_GRAIN_PERCENT, 0));
    store.setSaturationPercent(presetId, json.optInt(KEY_WALLPAPER_SATURATION_PERCENT, 100));
    store.setContrastPercent(presetId, json.optInt(KEY_WALLPAPER_CONTRAST_PERCENT, 100));
    store.setBrightnessPercent(presetId, json.optInt(KEY_WALLPAPER_BRIGHTNESS_PERCENT, 100));
    store.setTemperaturePercent(presetId, json.optInt(KEY_WALLPAPER_TEMPERATURE_PERCENT, 100));
  }
}
