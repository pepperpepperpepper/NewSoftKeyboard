package wtf.uhoh.newsoftkeyboard.app.ime;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.api.KeyCodes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;

/**
 * Builds live-typing-equivalent nearby-key code rows for characters of a restarted word, so words
 * resumed via cursor moves get the same proximity-based correction as words typed key-by-key
 * (which receive their rows from ProximityKeyDetector at touch time).
 */
final class KeyProximityRows {
  // Matches BinaryDictionary.MAX_ALTERNATIVES — the native side truncates longer rows anyway.
  private static final int MAX_ROW_CODES = 16;

  @Nullable private Keyboard mKeyboard;
  private int mProximityThresholdSquare;
  private final Map<Integer, int[]> mRowsByCodePoint = new HashMap<>();

  /**
   * Returns the code row for the given code point: the code point itself at slot 0, then primary
   * codes of nearby keys ordered by distance from the matching key's center. Falls back to a
   * single-element row when the code point has no key on the layout (symbols, foreign chars).
   *
   * <p>Rows are cached and shared across calls; the code point staying at slot 0 also guarantees
   * WordComposer.correctPrimaryJuxtapos never swaps (mutates) a cached row.
   */
  @NonNull
  int[] rowFor(@Nullable Keyboard keyboard, int codePoint) {
    if (keyboard == null) return new int[] {codePoint};
    if (keyboard != mKeyboard) {
      mKeyboard = keyboard;
      mProximityThresholdSquare = computeProximityThresholdSquare(keyboard);
      mRowsByCodePoint.clear();
    }
    int[] row = mRowsByCodePoint.get(codePoint);
    if (row == null) {
      row = buildRow(keyboard, codePoint, mProximityThresholdSquare);
      mRowsByCodePoint.put(codePoint, row);
    }
    return row;
  }

  // Same formula as ProximityCalculator.computeProximityThreshold, which bounds the rows
  // ProximityKeyDetector produces for live typing.
  private static int computeProximityThresholdSquare(@NonNull Keyboard keyboard) {
    final List<Keyboard.Key> keys = keyboard.getKeys();
    if (keys.isEmpty()) return 0;
    int dimensionSum = 0;
    for (Keyboard.Key key : keys) {
      dimensionSum += Math.min(key.width, key.height) + key.gap;
    }
    if (dimensionSum < 0) return 0;
    final int threshold = (int) (dimensionSum * 1.4f / keys.size());
    return threshold * threshold;
  }

  @NonNull
  private static int[] buildRow(
      @NonNull Keyboard keyboard, int codePoint, int proximityThresholdSquare) {
    final int lowerCodePoint = Character.toLowerCase(codePoint);
    final List<Keyboard.Key> keys = keyboard.getKeys();
    Keyboard.Key sourceKey = null;
    for (Keyboard.Key key : keys) {
      if (matchesCodePoint(key.getPrimaryCode(), codePoint, lowerCodePoint)) {
        sourceKey = key;
        break;
      }
    }
    if (sourceKey == null) return new int[] {codePoint};

    final int centerX = sourceKey.x + sourceKey.width / 2;
    final int centerY = sourceKey.y + sourceKey.height / 2;
    final List<int[]> codeAndDistance = new ArrayList<>();
    for (int keyIndex : keyboard.getNearestKeysIndices(centerX, centerY)) {
      final Keyboard.Key key = keys.get(keyIndex);
      final int primary = key.getPrimaryCode();
      // No function keys and no space — same filter as ProximityKeyDetector.
      if (primary <= KeyCodes.SPACE) continue;
      if (matchesCodePoint(primary, codePoint, lowerCodePoint)) continue;
      final int distance = key.squaredDistanceFrom(centerX, centerY);
      // Same cutoff live typing applies — without it the grid hands back far-away keys.
      if (distance >= proximityThresholdSquare) continue;
      codeAndDistance.add(new int[] {primary, distance});
    }
    codeAndDistance.sort((a, b) -> Integer.compare(a[1], b[1]));

    final int rowLength = Math.min(MAX_ROW_CODES, 1 + codeAndDistance.size());
    final int[] row = new int[rowLength];
    row[0] = codePoint;
    for (int i = 1; i < rowLength; i++) {
      row[i] = codeAndDistance.get(i - 1)[0];
    }
    return row;
  }

  private static boolean matchesCodePoint(int primaryCode, int codePoint, int lowerCodePoint) {
    return primaryCode == codePoint || Character.toLowerCase(primaryCode) == lowerCodePoint;
  }
}
