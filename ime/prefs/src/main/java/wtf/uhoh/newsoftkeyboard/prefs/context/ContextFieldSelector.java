package wtf.uhoh.newsoftkeyboard.prefs.context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Field selectors used for per-app context-profile bindings. */
public enum ContextFieldSelector {
  ALL_FIELDS("all_fields"),
  TEXT("text"),
  EMAIL("email"),
  URL("url"),
  IM("im"),
  SEARCH("search");

  @NonNull private final String id;

  ContextFieldSelector(@NonNull String id) {
    this.id = id;
  }

  @NonNull
  public String id() {
    return id;
  }

  @Nullable
  public static ContextFieldSelector fromId(@Nullable String raw) {
    if (raw == null) return null;
    final String trimmed = raw.trim();
    if (trimmed.isEmpty()) return null;
    for (ContextFieldSelector selector : values()) {
      if (selector.id.equals(trimmed)) return selector;
    }
    return null;
  }
}
