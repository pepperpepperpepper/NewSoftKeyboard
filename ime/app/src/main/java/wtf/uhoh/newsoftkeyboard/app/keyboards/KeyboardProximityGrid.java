package wtf.uhoh.newsoftkeyboard.app.keyboards;

import java.util.List;

final class KeyboardProximityGrid {

  private static final int GRID_WIDTH = 10;
  private static final int GRID_HEIGHT = 5;
  private static final int GRID_SIZE = GRID_WIDTH * GRID_HEIGHT;
  private static final int[] EMPTY = new int[0];

  private int cellWidth;
  private int cellHeight;
  private int[][] gridNeighbors;

  void compute(
      List<Keyboard.Key> keys, int keyboardWidth, int keyboardHeight, int proximityThreshold) {
    if (keyboardWidth <= 0 || keyboardHeight <= 0 || keys.isEmpty()) {
      gridNeighbors = new int[GRID_SIZE][];
      for (int i = 0; i < GRID_SIZE; i++) {
        gridNeighbors[i] = EMPTY;
      }
      cellWidth = 1;
      cellHeight = 1;
      return;
    }

    // Round-up so we don't have any pixels outside the grid
    cellWidth = (keyboardWidth + GRID_WIDTH - 1) / GRID_WIDTH;
    cellHeight = (keyboardHeight + GRID_HEIGHT - 1) / GRID_HEIGHT;

    gridNeighbors = new int[GRID_SIZE][];
    int[] indices = new int[keys.size()];
    final int gridWidth = GRID_WIDTH * cellWidth;
    final int gridHeight = GRID_HEIGHT * cellHeight;
    for (int x = 0; x < gridWidth; x += cellWidth) {
      for (int y = 0; y < gridHeight; y += cellHeight) {
        int count = 0;
        for (int i = 0; i < keys.size(); i++) {
          final Keyboard.Key key = keys.get(i);
          if (key.squaredDistanceFrom(x, y) < proximityThreshold
              || key.squaredDistanceFrom(x + cellWidth - 1, y) < proximityThreshold
              || key.squaredDistanceFrom(x + cellWidth - 1, y + cellHeight - 1) < proximityThreshold
              || key.squaredDistanceFrom(x, y + cellHeight - 1) < proximityThreshold) {
            indices[count++] = i;
          }
        }
        int[] cell = new int[count];
        System.arraycopy(indices, 0, cell, 0, count);
        gridNeighbors[(y / cellHeight) * GRID_WIDTH + (x / cellWidth)] = cell;
      }
    }
  }

  int[] getNearestKeysIndices(
      List<Keyboard.Key> keys,
      int keyboardWidth,
      int keyboardHeight,
      int proximityThreshold,
      int x,
      int y) {
    if (gridNeighbors == null) {
      compute(keys, keyboardWidth, keyboardHeight, proximityThreshold);
    }
    if (x >= 0 && x < keyboardWidth && y >= 0 && y < keyboardHeight) {
      int index = (y / cellHeight) * GRID_WIDTH + (x / cellWidth);
      if (index < GRID_SIZE) {
        return gridNeighbors[index];
      }
    }
    return EMPTY;
  }
}
