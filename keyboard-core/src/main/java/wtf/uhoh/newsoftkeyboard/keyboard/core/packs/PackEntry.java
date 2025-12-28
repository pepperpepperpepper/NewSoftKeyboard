package wtf.uhoh.newsoftkeyboard.keyboard.core.packs;

import java.util.Objects;

public record PackEntry(String id, PackPath path) {
  public PackEntry {
    Objects.requireNonNull(id);
    Objects.requireNonNull(path);
    if (id.trim().isEmpty()) {
      throw new IllegalArgumentException("Entry id is empty");
    }
  }
}
