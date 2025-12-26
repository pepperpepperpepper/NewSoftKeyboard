package wtf.uhoh.newsoftkeyboard.app.keyboards;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static java.util.Arrays.asList;
import static wtf.uhoh.newsoftkeyboard.app.keyboards.ExternalKeyboardTest.SIMPLE_KeyboardDimens;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.addons.DefaultAddOn;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class PopupListKeyboardTest {
  @Test
  public void testEmptyCodes() {
    PopupListKeyboard keyboard =
        new PopupListKeyboard(
            new DefaultAddOn(getApplicationContext(), getApplicationContext()),
            getApplicationContext(),
            SIMPLE_KeyboardDimens,
            asList("one", "two", "three"),
            asList("v-one", "v-two", "v-three"),
            "POP_KEYBOARD");
    for (int keyIndex = 0; keyIndex < keyboard.getKeys().size(); keyIndex++) {
      Assert.assertEquals(0, keyboard.getKeys().get(keyIndex).getCodeAtIndex(0, false));
    }

    for (int keyIndex = 0; keyIndex < keyboard.getKeys().size(); keyIndex++) {
      Assert.assertEquals(0, keyboard.getKeys().get(keyIndex).getCodeAtIndex(0, true));
    }
  }
}
