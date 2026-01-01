package wtf.uhoh.newsoftkeyboard.keyboard.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class KeyboardRow {
  private final Map<String, String> rawRowAttributes;
  private final List<KeySpec> keys;

  public KeyboardRow(List<KeySpec> keys) {
    this(Collections.emptyMap(), keys);
  }

  public KeyboardRow(Map<String, String> rawRowAttributes, List<KeySpec> keys) {
    this.rawRowAttributes =
        Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(rawRowAttributes)));
    this.keys = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(keys)));
  }

  public Map<String, String> rawRowAttributes() {
    return rawRowAttributes;
  }

  public List<KeySpec> keys() {
    return keys;
  }
}
