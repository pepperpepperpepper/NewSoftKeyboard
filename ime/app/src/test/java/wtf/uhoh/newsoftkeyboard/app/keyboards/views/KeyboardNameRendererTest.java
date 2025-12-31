package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class KeyboardNameRendererTest {

  @Test
  public void testUsesKeyboardNameForNullLabelOnSpaceWhenEnabled() {
    KeyboardNameRenderer renderer = new KeyboardNameRenderer();
    assertEquals("NSK", renderer.applyKeyboardNameIfNeeded(null, true, true, "NSK").toString());
  }

  @Test
  public void testDoesNotOverrideNullLabelOnNonSpaceKeys() {
    KeyboardNameRenderer renderer = new KeyboardNameRenderer();
    assertNull(renderer.applyKeyboardNameIfNeeded(null, false, true, "NSK"));
  }

  @Test
  public void testAppliesKeyboardNameWhenLabelIsWhitespaceOnly() {
    KeyboardNameRenderer renderer = new KeyboardNameRenderer();
    assertEquals("NSK", renderer.applyKeyboardNameIfNeeded(" ", true, true, "NSK").toString());
  }

  @Test
  public void testDoesNotOverrideNonEmptyLabel() {
    KeyboardNameRenderer renderer = new KeyboardNameRenderer();
    assertEquals(
        "English", renderer.applyKeyboardNameIfNeeded("English", true, true, "NSK").toString());
  }
}
