package wtf.uhoh.newsoftkeyboard.app.ime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;
import com.anysoftkeyboard.api.KeyCodes;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.app.testing.TestableImeService;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.utils.ModifierKeyState;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceStickyModifiersTest extends ImeServiceBaseTest {

  @Override
  protected Class<? extends TestableImeService> getServiceClass() {
    return StickyModifiersTestableKeyboard.class;
  }

  @Test
  public void testCtrlAltRemainActiveAcrossModifierPresses() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mImeServiceUnderTest;

    mImeServiceUnderTest.onPress(KeyCodes.CTRL);
    mImeServiceUnderTest.onRelease(KeyCodes.CTRL);
    assertTrue(keyboard.getControlState().isActive());

    mImeServiceUnderTest.onPress(KeyCodes.ALT_MODIFIER);
    mImeServiceUnderTest.onRelease(KeyCodes.ALT_MODIFIER);
    assertTrue("CTRL should remain active after ALT press", keyboard.getControlState().isActive());
    assertTrue("ALT should be active after release", keyboard.getAltState().isActive());

    mImeServiceUnderTest.onPress('a');
    mImeServiceUnderTest.onRelease('a');
    assertFalse("CTRL should clear after non-modifier key", keyboard.getControlState().isActive());
    assertFalse("ALT should clear after non-modifier key", keyboard.getAltState().isActive());
  }

  @Test
  public void testCtrlNeverLocks() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mImeServiceUnderTest;

    mImeServiceUnderTest.onPress(KeyCodes.CTRL);
    mImeServiceUnderTest.onRelease(KeyCodes.CTRL);
    assertFalse("CTRL should not lock after first press", keyboard.getControlState().isLocked());

    mImeServiceUnderTest.onPress(KeyCodes.CTRL);
    mImeServiceUnderTest.onRelease(KeyCodes.CTRL);
    assertFalse("CTRL should not lock after double press", keyboard.getControlState().isLocked());
  }

  @Test
  public void testAltNeverLocks() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mImeServiceUnderTest;

    mImeServiceUnderTest.onPress(KeyCodes.ALT_MODIFIER);
    mImeServiceUnderTest.onRelease(KeyCodes.ALT_MODIFIER);
    assertFalse("ALT should not lock after first press", keyboard.getAltState().isLocked());

    mImeServiceUnderTest.onPress(KeyCodes.ALT_MODIFIER);
    mImeServiceUnderTest.onRelease(KeyCodes.ALT_MODIFIER);
    assertFalse("ALT should not lock after double press", keyboard.getAltState().isLocked());
  }

  @Test
  public void testShiftFunctionRemainActiveUntilNonModifier() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mImeServiceUnderTest;

    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.onRelease(KeyCodes.SHIFT);
    assertTrue("SHIFT should be active after toggle", keyboard.getShiftState().isActive());

    mImeServiceUnderTest.onPress(KeyCodes.FUNCTION);
    mImeServiceUnderTest.onRelease(KeyCodes.FUNCTION);
    assertTrue("SHIFT should remain active after FUNCTION", keyboard.getShiftState().isActive());
    assertTrue("FUNCTION should be active after release", keyboard.getFunctionState().isActive());

    mImeServiceUnderTest.onPress('b');
    mImeServiceUnderTest.onRelease('b');
    assertFalse("SHIFT should clear after character", keyboard.getShiftState().isActive());
    assertFalse("FUNCTION should clear after character", keyboard.getFunctionState().isActive());
  }

  @Test
  public void testFunctionArrowUpSendsPageUp() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mImeServiceUnderTest;
    keyboard.resetLastSentKeyCode();

    mImeServiceUnderTest.onPress(KeyCodes.FUNCTION);
    mImeServiceUnderTest.onRelease(KeyCodes.FUNCTION);
    mImeServiceUnderTest.onKey(KeyCodes.ARROW_UP, null, 0, new int[] {KeyCodes.ARROW_UP}, true);

    assertEquals(KeyEvent.KEYCODE_PAGE_UP, keyboard.getLastSentKeyCode());
  }

  @Test
  public void testFunctionArrowDownSendsPageDown() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mImeServiceUnderTest;
    keyboard.resetLastSentKeyCode();

    mImeServiceUnderTest.onPress(KeyCodes.FUNCTION);
    mImeServiceUnderTest.onRelease(KeyCodes.FUNCTION);
    mImeServiceUnderTest.onKey(KeyCodes.ARROW_DOWN, null, 0, new int[] {KeyCodes.ARROW_DOWN}, true);

    assertEquals(KeyEvent.KEYCODE_PAGE_DOWN, keyboard.getLastSentKeyCode());
  }

  @Test
  public void testFunctionArrowLeftSendsHome() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mImeServiceUnderTest;
    keyboard.resetLastSentKeyCode();

    mImeServiceUnderTest.onPress(KeyCodes.FUNCTION);
    mImeServiceUnderTest.onRelease(KeyCodes.FUNCTION);
    mImeServiceUnderTest.onKey(KeyCodes.ARROW_LEFT, null, 0, new int[] {KeyCodes.ARROW_LEFT}, true);

    assertEquals(KeyEvent.KEYCODE_MOVE_HOME, keyboard.getLastSentKeyCode());
  }

  @Test
  public void testFunctionArrowRightSendsEnd() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mImeServiceUnderTest;
    keyboard.resetLastSentKeyCode();

    mImeServiceUnderTest.onPress(KeyCodes.FUNCTION);
    mImeServiceUnderTest.onRelease(KeyCodes.FUNCTION);
    mImeServiceUnderTest.onKey(
        KeyCodes.ARROW_RIGHT, null, 0, new int[] {KeyCodes.ARROW_RIGHT}, true);

    assertEquals(KeyEvent.KEYCODE_MOVE_END, keyboard.getLastSentKeyCode());
  }

  @Test
  public void testShiftArrowUpKeepsArrowNavigation() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mImeServiceUnderTest;
    keyboard.resetLastSentKeyCode();

    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.onRelease(KeyCodes.SHIFT);
    mImeServiceUnderTest.onKey(KeyCodes.ARROW_UP, null, 0, new int[] {KeyCodes.ARROW_UP}, true);

    assertEquals(KeyEvent.KEYCODE_DPAD_UP, keyboard.getLastSentKeyCode());
  }

  @Test
  public void testShiftArrowDownKeepsArrowNavigation() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mImeServiceUnderTest;
    keyboard.resetLastSentKeyCode();

    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.onRelease(KeyCodes.SHIFT);
    mImeServiceUnderTest.onKey(KeyCodes.ARROW_DOWN, null, 0, new int[] {KeyCodes.ARROW_DOWN}, true);

    assertEquals(KeyEvent.KEYCODE_DPAD_DOWN, keyboard.getLastSentKeyCode());
  }

  @Test
  public void testShiftArrowLeftKeepsArrowNavigation() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mImeServiceUnderTest;
    keyboard.resetLastSentKeyCode();

    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.onRelease(KeyCodes.SHIFT);
    mImeServiceUnderTest.onKey(KeyCodes.ARROW_LEFT, null, 0, new int[] {KeyCodes.ARROW_LEFT}, true);

    assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, keyboard.getLastSentKeyCode());
  }

  @Test
  public void testShiftArrowRightKeepsArrowNavigation() {
    StickyModifiersTestableKeyboard keyboard =
        (StickyModifiersTestableKeyboard) mImeServiceUnderTest;
    keyboard.resetLastSentKeyCode();

    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.onRelease(KeyCodes.SHIFT);
    mImeServiceUnderTest.onKey(
        KeyCodes.ARROW_RIGHT, null, 0, new int[] {KeyCodes.ARROW_RIGHT}, true);

    assertEquals(KeyEvent.KEYCODE_DPAD_RIGHT, keyboard.getLastSentKeyCode());
  }

  public static class StickyModifiersTestableKeyboard extends TestableImeService {
    private int mLastSentKeyCode = Integer.MIN_VALUE;

    @Override
    public void sendDownUpKeyEvents(int keyEventCode) {
      mLastSentKeyCode = keyEventCode;
      super.sendDownUpKeyEvents(keyEventCode);
    }

    public void resetLastSentKeyCode() {
      mLastSentKeyCode = Integer.MIN_VALUE;
    }

    public int getLastSentKeyCode() {
      return mLastSentKeyCode;
    }

    public ModifierKeyState getShiftState() {
      return mShiftKeyState;
    }

    public ModifierKeyState getControlState() {
      return mControlKeyState;
    }

    public ModifierKeyState getAltState() {
      return mAltKeyState;
    }

    public ModifierKeyState getFunctionState() {
      return mFunctionKeyState;
    }
  }
}
