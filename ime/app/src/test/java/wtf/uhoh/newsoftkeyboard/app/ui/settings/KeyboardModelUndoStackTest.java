package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;
import wtf.uhoh.newsoftkeyboard.keyboard.core.KeyboardModel;

public class KeyboardModelUndoStackTest {

  private static KeyboardModel model(String marker) {
    return new KeyboardModel(
        Collections.singletonMap("android:keyWidth", marker), Collections.emptyList());
  }

  @Test
  public void testPopsInLifoOrderAndEmptiesOut() {
    KeyboardModelUndoStack stack = new KeyboardModelUndoStack();
    Assert.assertTrue(stack.isEmpty());
    Assert.assertNull(stack.pop());

    KeyboardModel first = model("1");
    KeyboardModel second = model("2");
    stack.push(first);
    stack.push(second);

    Assert.assertFalse(stack.isEmpty());
    Assert.assertSame(second, stack.pop());
    Assert.assertSame(first, stack.pop());
    Assert.assertTrue(stack.isEmpty());
    Assert.assertNull(stack.pop());
  }

  @Test
  public void testDropsOldestEntryAtCapacity() {
    KeyboardModelUndoStack stack = new KeyboardModelUndoStack();
    KeyboardModel oldest = model("oldest");
    stack.push(oldest);
    for (int i = 0; i < 50; i++) stack.push(model("m" + i));

    KeyboardModel popped = null;
    while (!stack.isEmpty()) popped = stack.pop();
    Assert.assertNotNull(popped);
    Assert.assertNotSame(oldest, popped);
    Assert.assertEquals("m0", popped.rawKeyboardAttributes().get("android:keyWidth"));
  }

  @Test
  public void testClear() {
    KeyboardModelUndoStack stack = new KeyboardModelUndoStack();
    stack.push(model("1"));
    stack.clear();
    Assert.assertTrue(stack.isEmpty());
    Assert.assertNull(stack.pop());
  }
}
