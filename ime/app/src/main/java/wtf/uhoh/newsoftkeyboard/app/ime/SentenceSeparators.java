package wtf.uhoh.newsoftkeyboard.app.ime;

import android.util.SparseBooleanArray;

/**
 * Small helper to track which code points are considered sentence separators for the current
 * keyboard layout.
 */
final class SentenceSeparators {

  private final SparseBooleanArray separators = new SparseBooleanArray();

  void updateFrom(char[] chars) {
    separators.clear();
    for (char separator : chars) {
      separators.put(separator, true);
    }
  }

  void add(int codePoint) {
    separators.put(codePoint, true);
  }

  void clear() {
    separators.clear();
  }

  boolean isSeparator(int codePoint) {
    return separators.get(codePoint, false);
  }
}
