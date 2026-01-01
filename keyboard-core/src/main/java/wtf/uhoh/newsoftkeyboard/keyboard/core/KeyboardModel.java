package wtf.uhoh.newsoftkeyboard.keyboard.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class KeyboardModel {
  private final Map<String, String> rawKeyboardAttributes;
  private final List<KeyboardRow> rows;

  public KeyboardModel(List<KeyboardRow> rows) {
    this(Collections.emptyMap(), rows);
  }

  public KeyboardModel(Map<String, String> rawKeyboardAttributes, List<KeyboardRow> rows) {
    this.rawKeyboardAttributes =
        Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(rawKeyboardAttributes)));
    this.rows = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(rows)));
  }

  public Map<String, String> rawKeyboardAttributes() {
    return rawKeyboardAttributes;
  }

  public List<KeyboardRow> rows() {
    return rows;
  }
}
