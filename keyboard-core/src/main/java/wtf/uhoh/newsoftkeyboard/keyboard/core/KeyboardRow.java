package wtf.uhoh.newsoftkeyboard.keyboard.core;

import java.util.List;
import java.util.Objects;

public final class KeyboardRow {
  private final List<KeySpec> keys;

  public KeyboardRow(List<KeySpec> keys) {
    this.keys = List.copyOf(Objects.requireNonNull(keys));
  }

  public List<KeySpec> keys() {
    return keys;
  }
}
