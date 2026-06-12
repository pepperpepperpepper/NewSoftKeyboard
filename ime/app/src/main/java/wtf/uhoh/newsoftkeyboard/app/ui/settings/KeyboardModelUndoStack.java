package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardModel;

/**
 * Bounded stack of prior {@link KeyboardModel} snapshots for the layout editor. Models are
 * immutable, so each entry is just a reference; the stack belongs to a single keyboard file and
 * must be cleared when the editor loads a different one.
 */
final class KeyboardModelUndoStack {
  private static final int MAX_ENTRIES = 50;

  @NonNull private final Deque<KeyboardModel> entries = new ArrayDeque<>();

  void push(@NonNull KeyboardModel model) {
    if (entries.size() == MAX_ENTRIES) entries.removeLast();
    entries.addFirst(model);
  }

  @Nullable
  KeyboardModel pop() {
    return entries.pollFirst();
  }

  boolean isEmpty() {
    return entries.isEmpty();
  }

  void clear() {
    entries.clear();
  }
}
