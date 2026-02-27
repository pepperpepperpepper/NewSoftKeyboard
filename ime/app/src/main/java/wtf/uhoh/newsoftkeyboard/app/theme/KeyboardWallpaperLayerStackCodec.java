package wtf.uhoh.newsoftkeyboard.app.theme;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;

final class KeyboardWallpaperLayerStackCodec {

  private static final int LAYER_STACK_VERSION = 1;
  private static final String LAYER_STACK_JSON_KEY_VERSION = "version";
  private static final String LAYER_STACK_JSON_KEY_LAYERS = "layers";

  private KeyboardWallpaperLayerStackCodec() {}

  @Nullable
  static KeyboardWallpaperLayer[] parseLayerStack(@Nullable String rawJson) {
    if (rawJson == null || rawJson.trim().isEmpty()) return null;
    try {
      final org.json.JSONObject root = new org.json.JSONObject(rawJson);
      final int version = root.optInt(LAYER_STACK_JSON_KEY_VERSION, 0);
      if (version != LAYER_STACK_VERSION) return null;
      final org.json.JSONArray layers = root.optJSONArray(LAYER_STACK_JSON_KEY_LAYERS);
      if (layers == null) return null;
      final ArrayList<KeyboardWallpaperLayer> out = new ArrayList<>();
      for (int i = 0; i < layers.length(); i++) {
        final org.json.JSONObject layerJson = layers.optJSONObject(i);
        final KeyboardWallpaperLayer layer = KeyboardWallpaperLayer.fromJson(layerJson);
        if (layer != null) out.add(layer);
      }
      return out.toArray(new KeyboardWallpaperLayer[0]);
    } catch (org.json.JSONException e) {
      return null;
    }
  }

  @NonNull
  static String serializeLayerStack(@NonNull KeyboardWallpaperLayer[] layers) {
    try {
      final org.json.JSONObject root = new org.json.JSONObject();
      root.put(LAYER_STACK_JSON_KEY_VERSION, LAYER_STACK_VERSION);
      final org.json.JSONArray out = new org.json.JSONArray();
      for (KeyboardWallpaperLayer layer : layers) {
        if (layer == null) continue;
        out.put(layer.toJson());
      }
      root.put(LAYER_STACK_JSON_KEY_LAYERS, out);
      return root.toString();
    } catch (org.json.JSONException e) {
      return "{\""
          + LAYER_STACK_JSON_KEY_VERSION
          + "\":"
          + LAYER_STACK_VERSION
          + ",\""
          + LAYER_STACK_JSON_KEY_LAYERS
          + "\":[]}";
    }
  }

  @NonNull
  static int[] parseLayerOrder(
      @Nullable String raw, @NonNull int[] defaultOrder, int expectedSize, boolean allowEmpty) {
    if (raw == null || raw.trim().isEmpty()) {
      return allowEmpty ? new int[0] : defaultOrder.clone();
    }
    try {
      final String[] parts = raw.split(",");
      final int[] parsed = new int[parts.length];
      int count = 0;
      for (String part : parts) {
        if (part == null) continue;
        final String trimmed = part.trim();
        if (trimmed.isEmpty()) continue;
        parsed[count++] = Integer.parseInt(trimmed);
      }
      if (count <= 0) return defaultOrder.clone();
      final int[] normalized =
          normalizeLayerOrder(
              java.util.Arrays.copyOf(parsed, count), defaultOrder, expectedSize, allowEmpty);
      if (normalized.length == 0 && !allowEmpty) return defaultOrder.clone();
      return normalized;
    } catch (RuntimeException ignored) {
      return defaultOrder.clone();
    }
  }

  @NonNull
  static int[] normalizeLayerOrder(
      @NonNull int[] order, @NonNull int[] defaultOrder, int expectedSize, boolean allowEmpty) {
    final boolean[] seen = new boolean[expectedSize];
    final int[] out = new int[expectedSize];
    int outIndex = 0;
    for (int value : order) {
      if (value < 0 || value >= expectedSize) continue;
      if (seen[value]) continue;
      seen[value] = true;
      out[outIndex++] = value;
    }
    if (allowEmpty) {
      // If the caller gave us an empty list, they explicitly want "no layers".
      if (order.length == 0) return new int[0];
      // Non-empty input that normalized to an empty set means data was invalid/corrupt; fall back.
      if (outIndex == 0) return defaultOrder.clone();
      return java.util.Arrays.copyOf(out, outIndex);
    }

    for (int value : defaultOrder) {
      if (value < 0 || value >= expectedSize) continue;
      if (seen[value]) continue;
      seen[value] = true;
      out[outIndex++] = value;
    }
    if (outIndex != expectedSize) {
      return defaultOrder.clone();
    }
    return out;
  }

  @NonNull
  static String serializeLayerOrder(@NonNull int[] order) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < order.length; i++) {
      if (i > 0) sb.append(',');
      sb.append(order[i]);
    }
    return sb.toString();
  }
}
