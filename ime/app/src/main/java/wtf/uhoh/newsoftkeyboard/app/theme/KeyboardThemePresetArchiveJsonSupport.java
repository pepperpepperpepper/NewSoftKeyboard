package wtf.uhoh.newsoftkeyboard.app.theme;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

final class KeyboardThemePresetArchiveJsonSupport {

  interface IntConsumer {
    void accept(int value);
  }

  private KeyboardThemePresetArchiveJsonSupport() {}

  static void setIntIfPresent(
      @NonNull JSONObject json, @NonNull String key, @NonNull IntConsumer consumer) {
    if (!json.has(key)) return;
    consumer.accept(json.optInt(key, 0));
  }

  static void putIntIfNotNull(
      @NonNull JSONObject json, @NonNull String key, @Nullable Integer value) throws JSONException {
    if (value != null) json.put(key, value.intValue());
  }

  static void putStringIfNotNull(
      @NonNull JSONObject json, @NonNull String key, @Nullable String value) throws JSONException {
    if (value != null) json.put(key, value);
  }

  static boolean hasAny(@NonNull JSONObject json, @NonNull String... keys) {
    for (String key : keys) {
      if (json.has(key)) return true;
    }
    return false;
  }
}
