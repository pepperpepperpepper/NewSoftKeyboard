package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeySpec;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardModel;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardRow;

public class CustomKeyboardLayoutModelEditorTest {

  private static KeySpec key(String label) {
    return CustomKeyboardLayoutModelEditor.createKeySpec(
        label, Integer.toString(label.codePointAt(0)), null, null);
  }

  private static KeySpec key(String label, String edgeFlags, String horizontalGap) {
    Map<String, String> extra =
        horizontalGap == null
            ? Collections.emptyMap()
            : Collections.singletonMap(CustomKeyboardLayoutEditor.ATTR_HORIZONTAL_GAP, horizontalGap);
    return CustomKeyboardLayoutModelEditor.createKeySpec(
        label, Integer.toString(label.codePointAt(0)), edgeFlags, null, null, extra);
  }

  private static KeyboardModel modelWithRow(KeySpec... keys) {
    return new KeyboardModel(
        Collections.emptyMap(),
        Collections.singletonList(new KeyboardRow(Arrays.asList(keys))));
  }

  private static List<String> labels(KeyboardModel model, int rowIndex) {
    List<String> result = new ArrayList<>();
    for (KeySpec key : model.rows().get(rowIndex).keys()) result.add(key.label());
    return result;
  }

  private static String attr(KeyboardModel model, int rowIndex, int keyIndex, String name) {
    return model.rows().get(rowIndex).keys().get(keyIndex).rawAttributes().get(name);
  }

  @Test
  public void testMoveKeyKeepsHorizontalGapAtSlot() {
    // 'a' carries the row indent (the template's 5%p home-row indent).
    KeyboardModel model = modelWithRow(key("a", null, "5%p"), key("s", null, null), key("d", null, null));

    KeyboardModel moved = CustomKeyboardLayoutModelEditor.moveKey(model, 0, 0, +1);

    Assert.assertEquals(Arrays.asList("s", "a", "d"), labels(moved, 0));
    Assert.assertEquals(
        "5%p", attr(moved, 0, 0, CustomKeyboardLayoutEditor.ATTR_HORIZONTAL_GAP));
    Assert.assertNull(attr(moved, 0, 1, CustomKeyboardLayoutEditor.ATTR_HORIZONTAL_GAP));
  }

  @Test
  public void testMoveKeyReanchorsEdgeFlags() {
    KeyboardModel model = modelWithRow(key("q", "left", null), key("w", null, null), key("e", "right", null));

    KeyboardModel moved = CustomKeyboardLayoutModelEditor.moveKey(model, 0, 2, -1);

    Assert.assertEquals(Arrays.asList("q", "e", "w"), labels(moved, 0));
    Assert.assertEquals("left", attr(moved, 0, 0, CustomKeyboardLayoutEditor.ATTR_KEY_EDGE_FLAGS));
    Assert.assertNull(attr(moved, 0, 1, CustomKeyboardLayoutEditor.ATTR_KEY_EDGE_FLAGS));
    Assert.assertEquals("right", attr(moved, 0, 2, CustomKeyboardLayoutEditor.ATTR_KEY_EDGE_FLAGS));
  }

  @Test
  public void testMoveKeyWithoutEdgeFlagsAddsNone() {
    KeyboardModel model = modelWithRow(key("a"), key("b"), key("c"));

    KeyboardModel moved = CustomKeyboardLayoutModelEditor.moveKey(model, 0, 0, +1);

    for (int i = 0; i < 3; i++) {
      Assert.assertNull(attr(moved, 0, i, CustomKeyboardLayoutEditor.ATTR_KEY_EDGE_FLAGS));
    }
  }

  @Test
  public void testMoveKeyOutOfBoundsReturnsSameInstance() {
    KeyboardModel model = modelWithRow(key("a"), key("b"));
    Assert.assertSame(model, CustomKeyboardLayoutModelEditor.moveKey(model, 0, 1, +1));
    Assert.assertSame(model, CustomKeyboardLayoutModelEditor.moveKey(model, 0, 0, -1));
  }

  @Test
  public void testDeleteFirstKeyKeepsRowIndent() {
    KeyboardModel model = modelWithRow(key("a", "left", "5%p"), key("s", null, null), key("d", "right", null));

    KeyboardModel deleted = CustomKeyboardLayoutModelEditor.deleteKey(model, 0, 0);

    Assert.assertEquals(Arrays.asList("s", "d"), labels(deleted, 0));
    Assert.assertEquals(
        "5%p", attr(deleted, 0, 0, CustomKeyboardLayoutEditor.ATTR_HORIZONTAL_GAP));
    Assert.assertEquals("left", attr(deleted, 0, 0, CustomKeyboardLayoutEditor.ATTR_KEY_EDGE_FLAGS));
    Assert.assertEquals("right", attr(deleted, 0, 1, CustomKeyboardLayoutEditor.ATTR_KEY_EDGE_FLAGS));
  }

  @Test
  public void testDeleteLastKeyMovesRightFlag() {
    KeyboardModel model = modelWithRow(key("q", "left", null), key("w", null, null), key("e", "right", null));

    KeyboardModel deleted = CustomKeyboardLayoutModelEditor.deleteKey(model, 0, 2);

    Assert.assertEquals(Arrays.asList("q", "w"), labels(deleted, 0));
    Assert.assertEquals("left", attr(deleted, 0, 0, CustomKeyboardLayoutEditor.ATTR_KEY_EDGE_FLAGS));
    Assert.assertEquals("right", attr(deleted, 0, 1, CustomKeyboardLayoutEditor.ATTR_KEY_EDGE_FLAGS));
  }

  @Test
  public void testInsertKeyAtEndTakesOverRightFlag() {
    KeyboardModel model = modelWithRow(key("q", "left", null), key("w", "right", null));

    KeyboardModel inserted = CustomKeyboardLayoutModelEditor.insertKeyAfter(model, 0, 1);

    Assert.assertEquals(3, inserted.rows().get(0).keys().size());
    Assert.assertEquals("left", attr(inserted, 0, 0, CustomKeyboardLayoutEditor.ATTR_KEY_EDGE_FLAGS));
    Assert.assertNull(attr(inserted, 0, 1, CustomKeyboardLayoutEditor.ATTR_KEY_EDGE_FLAGS));
    Assert.assertEquals("right", attr(inserted, 0, 2, CustomKeyboardLayoutEditor.ATTR_KEY_EDGE_FLAGS));
  }

  @Test
  public void testSingleKeyRowKeepsCombinedEdgeFlags() {
    KeyboardModel model = modelWithRow(key("a", "left|right", null), key("b", null, null));

    KeyboardModel deleted = CustomKeyboardLayoutModelEditor.deleteKey(model, 0, 1);

    Assert.assertEquals(
        "left|right", attr(deleted, 0, 0, CustomKeyboardLayoutEditor.ATTR_KEY_EDGE_FLAGS));
  }

  @Test
  public void testMoveRowAnchorsRowEdgeFlagsToPosition() {
    KeyboardRow letters = new KeyboardRow(Collections.singletonList(key("a")));
    KeyboardRow bottom =
        new KeyboardRow(
            Collections.singletonMap(CustomKeyboardLayoutEditor.ATTR_ROW_EDGE_FLAGS, "bottom"),
            Collections.singletonList(key("z")));
    KeyboardModel model =
        new KeyboardModel(Collections.emptyMap(), Arrays.asList(letters, bottom));

    KeyboardModel moved = CustomKeyboardLayoutModelEditor.moveRow(model, 1, -1);

    // Content swapped, but "bottom" stays on whichever row is now at the bottom.
    Assert.assertEquals("z", moved.rows().get(0).keys().get(0).label());
    Assert.assertNull(
        moved.rows().get(0).rawRowAttributes().get(CustomKeyboardLayoutEditor.ATTR_ROW_EDGE_FLAGS));
    Assert.assertEquals(
        "bottom",
        moved.rows().get(1).rawRowAttributes().get(CustomKeyboardLayoutEditor.ATTR_ROW_EDGE_FLAGS));
  }

  @Test
  public void testInsertRowAboveInsertsAtIndex() {
    KeyboardModel model =
        new KeyboardModel(
            Collections.emptyMap(),
            Arrays.asList(
                new KeyboardRow(Collections.singletonList(key("a"))),
                new KeyboardRow(Collections.singletonList(key("b")))));

    KeyboardModel inserted = CustomKeyboardLayoutModelEditor.insertRowAbove(model, 1);

    Assert.assertEquals(3, inserted.rows().size());
    Assert.assertEquals("a", inserted.rows().get(0).keys().get(0).label());
    Assert.assertEquals("?", inserted.rows().get(1).keys().get(0).label());
    Assert.assertEquals("b", inserted.rows().get(2).keys().get(0).label());
  }

  @Test
  public void testDeleteRowNoOpReturnsSameInstance() {
    KeyboardModel model = modelWithRow(key("a"));
    Assert.assertSame(model, CustomKeyboardLayoutModelEditor.deleteRow(model, 0));
  }

  @Test
  public void testMoveKeyToLocationSameRowReordersAndKeepsSlotGaps() {
    KeyboardModel model =
        modelWithRow(key("a", "left", "5%p"), key("s", null, null), key("d", "right", null));

    // Drag 'd' to the front (insert before slot 0).
    KeyboardModel moved = CustomKeyboardLayoutModelEditor.moveKeyToLocation(model, 0, 2, 0, 0);

    Assert.assertEquals(Arrays.asList("d", "a", "s"), labels(moved, 0));
    Assert.assertEquals("5%p", attr(moved, 0, 0, CustomKeyboardLayoutEditor.ATTR_HORIZONTAL_GAP));
    Assert.assertNull(attr(moved, 0, 1, CustomKeyboardLayoutEditor.ATTR_HORIZONTAL_GAP));
    Assert.assertEquals("left", attr(moved, 0, 0, CustomKeyboardLayoutEditor.ATTR_KEY_EDGE_FLAGS));
    Assert.assertEquals("right", attr(moved, 0, 2, CustomKeyboardLayoutEditor.ATTR_KEY_EDGE_FLAGS));
  }

  @Test
  public void testMoveKeyToLocationOwnSlotIsNoOp() {
    KeyboardModel model = modelWithRow(key("a"), key("b"), key("c"));
    Assert.assertSame(model, CustomKeyboardLayoutModelEditor.moveKeyToLocation(model, 0, 1, 0, 1));
    Assert.assertSame(model, CustomKeyboardLayoutModelEditor.moveKeyToLocation(model, 0, 1, 0, 2));
  }

  @Test
  public void testMoveKeyToLocationAcrossRows() {
    KeyboardModel model =
        new KeyboardModel(
            Collections.emptyMap(),
            Arrays.asList(
                new KeyboardRow(
                    Arrays.asList(key("q", "left", null), key("w", null, null), key("e", "right", null))),
                new KeyboardRow(
                    Arrays.asList(key("a", "left", "5%p"), key("s", "right", null)))));

    // Drag 'w' from row 0 into row 1 between 'a' and 's'.
    KeyboardModel moved = CustomKeyboardLayoutModelEditor.moveKeyToLocation(model, 0, 1, 1, 1);

    Assert.assertEquals(Arrays.asList("q", "e"), labels(moved, 0));
    Assert.assertEquals(Arrays.asList("a", "w", "s"), labels(moved, 1));
    // Source row edges re-anchored.
    Assert.assertEquals("left", attr(moved, 0, 0, CustomKeyboardLayoutEditor.ATTR_KEY_EDGE_FLAGS));
    Assert.assertEquals("right", attr(moved, 0, 1, CustomKeyboardLayoutEditor.ATTR_KEY_EDGE_FLAGS));
    // Target row keeps its indent and edges; the arrival has no stale gap or flags.
    Assert.assertEquals("5%p", attr(moved, 1, 0, CustomKeyboardLayoutEditor.ATTR_HORIZONTAL_GAP));
    Assert.assertNull(attr(moved, 1, 1, CustomKeyboardLayoutEditor.ATTR_HORIZONTAL_GAP));
    Assert.assertNull(attr(moved, 1, 1, CustomKeyboardLayoutEditor.ATTR_KEY_EDGE_FLAGS));
    Assert.assertEquals("right", attr(moved, 1, 2, CustomKeyboardLayoutEditor.ATTR_KEY_EDGE_FLAGS));
  }

  @Test
  public void testMoveKeyToLocationRefusesEmptyingARow() {
    KeyboardModel model =
        new KeyboardModel(
            Collections.emptyMap(),
            Arrays.asList(
                new KeyboardRow(Collections.singletonList(key("a"))),
                new KeyboardRow(Collections.singletonList(key("b")))));
    Assert.assertSame(model, CustomKeyboardLayoutModelEditor.moveKeyToLocation(model, 0, 0, 1, 0));
  }

  @Test
  public void testClampLocation() {
    KeyboardModel model =
        new KeyboardModel(
            Collections.emptyMap(),
            Arrays.asList(
                new KeyboardRow(Arrays.asList(key("a"), key("b"))),
                new KeyboardRow(Collections.singletonList(key("c")))));

    Assert.assertArrayEquals(
        new int[] {0, 1}, CustomKeyboardLayoutModelEditor.clampLocation(model, 0, 5));
    Assert.assertArrayEquals(
        new int[] {1, 0}, CustomKeyboardLayoutModelEditor.clampLocation(model, 7, 3));
    Assert.assertArrayEquals(
        new int[] {0, 0}, CustomKeyboardLayoutModelEditor.clampLocation(model, 0, 0));
    Assert.assertNull(
        CustomKeyboardLayoutModelEditor.clampLocation(
            new KeyboardModel(Collections.emptyMap(), Collections.emptyList()), 0, 0));
  }
}
