package wtf.uhoh.newsoftkeyboard.keyboard.core;

import java.util.List;
import java.util.Objects;

public final class KeyboardModel {
  private final List<KeyboardRow> rows;

  public KeyboardModel(List<KeyboardRow> rows) {
    this.rows = List.copyOf(Objects.requireNonNull(rows));
  }

  public List<KeyboardRow> rows() {
    return rows;
  }
}
