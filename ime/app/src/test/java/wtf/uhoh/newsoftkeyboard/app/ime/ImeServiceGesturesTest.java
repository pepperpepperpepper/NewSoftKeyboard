package wtf.uhoh.newsoftkeyboard.app.ime;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import com.anysoftkeyboard.api.KeyCodes;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.testing.AddOnTestUtils;
import wtf.uhoh.newsoftkeyboard.app.testing.TestInputConnection;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceGesturesTest extends ImeServiceBaseTest {

  @Before
  @Override
  public void setUpForImeServiceBase() throws Exception {
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(1, true);
    super.setUpForImeServiceBase();
  }

  @Test
  public void testSwipeLeftFromBackSpace() {
    KeyboardDefinition currentKeyboard = mImeServiceUnderTest.getCurrentKeyboardForTests();
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hello");
    mImeServiceUnderTest.simulateKeyPress(' ');
    mImeServiceUnderTest.simulateTextTyping("hello");
    Assert.assertEquals("hello hello", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.onFirstDownKey(KeyCodes.DELETE);
    mImeServiceUnderTest.onSwipeLeft(false);
    Assert.assertEquals("hello ", inputConnection.getCurrentTextInInputConnection());
    // still same keyboard
    Assert.assertEquals(
        currentKeyboard.getKeyboardId(),
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());
  }

  @Test
  public void testSwipeRightFromBackSpace() {
    KeyboardDefinition currentKeyboard = mImeServiceUnderTest.getCurrentKeyboardForTests();
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hello");
    mImeServiceUnderTest.simulateKeyPress(' ');
    mImeServiceUnderTest.simulateTextTyping("hello");
    Assert.assertEquals("hello hello", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.onFirstDownKey(KeyCodes.DELETE);
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals("hello ", inputConnection.getCurrentTextInInputConnection());
    // still same keyboard
    Assert.assertEquals(
        currentKeyboard.getKeyboardId(),
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());
  }

  @Test
  public void testSwipeLeft() {
    KeyboardDefinition currentKeyboard = mImeServiceUnderTest.getCurrentKeyboardForTests();
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hello");
    mImeServiceUnderTest.simulateKeyPress(' ');
    mImeServiceUnderTest.simulateTextTyping("hello");
    Assert.assertEquals("hello hello", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeLeft(false);
    Assert.assertEquals("hello hello", inputConnection.getCurrentTextInInputConnection());
    // switched keyboard
    Assert.assertNotEquals(
        currentKeyboard.getKeyboardId(),
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());
    Assert.assertEquals(
        "symbols_keyboard",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());
  }

  @Test
  public void testSwipeRight() {
    KeyboardDefinition currentKeyboard = mImeServiceUnderTest.getCurrentKeyboardForTests();
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hello");
    mImeServiceUnderTest.simulateKeyPress(' ');
    mImeServiceUnderTest.simulateTextTyping("hello");
    Assert.assertEquals("hello hello", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals("hello hello", inputConnection.getCurrentTextInInputConnection());
    // switched keyboard
    Assert.assertNotEquals(
        currentKeyboard.getKeyboardId(),
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());
  }

  @Test
  public void testSwipeWithSpaceOutput() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action), "space");
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_double_space_to_period), true);
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.SPACE, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
    Assert.assertEquals(" ", mImeServiceUnderTest.getCurrentInputConnectionText());

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.SPACE, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
    Assert.assertEquals(". ", mImeServiceUnderTest.getCurrentInputConnectionText());

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.SPACE, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
    Assert.assertEquals(".. ", mImeServiceUnderTest.getCurrentInputConnectionText());

    mImeServiceUnderTest.simulateKeyPress('x');
    Assert.assertEquals('x', mImeServiceUnderTest.getLastOnKeyPrimaryCode());
    Assert.assertEquals(".. x", mImeServiceUnderTest.getCurrentInputConnectionText());

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.SPACE, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
    Assert.assertEquals(".. x ", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testSwipeLeftFromSpace() {
    KeyboardDefinition currentKeyboard = mImeServiceUnderTest.getCurrentKeyboardForTests();
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hello");
    mImeServiceUnderTest.simulateKeyPress(' ');
    mImeServiceUnderTest.simulateTextTyping("hello");
    Assert.assertEquals("hello hello", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.onFirstDownKey(' ');
    mImeServiceUnderTest.onSwipeLeft(false);
    Assert.assertEquals("hello hello", inputConnection.getCurrentTextInInputConnection());
    // switched keyboard
    Assert.assertNotEquals(
        currentKeyboard.getKeyboardId(),
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());
    Assert.assertEquals(
        "symbols_keyboard",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());
  }

  @Test
  public void testSwipeRightFromSpace() {
    KeyboardDefinition currentKeyboard = mImeServiceUnderTest.getCurrentKeyboardForTests();
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hello");
    mImeServiceUnderTest.simulateKeyPress(' ');
    mImeServiceUnderTest.simulateTextTyping("hello");
    Assert.assertEquals("hello hello", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.onFirstDownKey(' ');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals("hello hello", inputConnection.getCurrentTextInInputConnection());
    // switched keyboard
    Assert.assertNotEquals(
        currentKeyboard.getKeyboardId(),
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());
    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());
  }

  @Test
  public void testSwipeUp() {
    KeyboardDefinition currentKeyboard = mImeServiceUnderTest.getCurrentKeyboardForTests();
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hello");
    mImeServiceUnderTest.simulateKeyPress(' ');
    mImeServiceUnderTest.simulateTextTyping("hello");
    Assert.assertEquals("hello hello", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.onFirstDownKey('x');
    Assert.assertEquals(false, mImeServiceUnderTest.getCurrentKeyboardForTests().isShifted());

    mImeServiceUnderTest.onSwipeUp();
    Assert.assertEquals("hello hello", inputConnection.getCurrentTextInInputConnection());
    // same keyboard, shift on
    Assert.assertEquals(
        currentKeyboard.getKeyboardId(),
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId().toString());
    Assert.assertEquals(true, mImeServiceUnderTest.getCurrentKeyboardForTests().isShifted());
  }

  @Test
  public void testSwipeDown() {
    KeyboardDefinition currentKeyboard = mImeServiceUnderTest.getCurrentKeyboardForTests();
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hello");
    mImeServiceUnderTest.simulateKeyPress(' ');
    mImeServiceUnderTest.simulateTextTyping("hello");
    Assert.assertEquals("hello hello", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.onFirstDownKey('x');
    Assert.assertFalse(mImeServiceUnderTest.isKeyboardViewHidden());

    mImeServiceUnderTest.onSwipeDown();
    Assert.assertEquals("hello hello", inputConnection.getCurrentTextInInputConnection());
    // same keyboard
    Assert.assertEquals(
        currentKeyboard.getKeyboardId(),
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    Assert.assertTrue(mImeServiceUnderTest.isKeyboardViewHidden());
  }

  @Test
  public void testSwipeDownCustomizable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_down_action), "clear_input");
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeDown();
    Assert.assertEquals(KeyCodes.CLEAR_INPUT, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeUpCustomizable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_up_action), "clear_input");
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeUp();
    Assert.assertEquals(KeyCodes.CLEAR_INPUT, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeUpFromSpaceCustomizable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_up_from_spacebar_action),
        "clear_input");
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey(' ');
    mImeServiceUnderTest.onSwipeUp();
    Assert.assertEquals(KeyCodes.CLEAR_INPUT, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeLeftCustomizable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_left_action), "clear_input");
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeLeft(false);
    Assert.assertEquals(KeyCodes.CLEAR_INPUT, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeLeftFromSpaceCustomizable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_left_space_bar_action),
        "clear_input");
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey(' ');
    mImeServiceUnderTest.onSwipeLeft(false);
    Assert.assertEquals(KeyCodes.CLEAR_INPUT, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeRightCustomizable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action), "clear_input");
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.CLEAR_INPUT, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeRightFromSpaceCustomizable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_space_bar_action),
        "clear_input");
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey(' ');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.CLEAR_INPUT, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionNoneConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_none));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(0, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionNextAlphabetConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_next_alphabet));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.MODE_ALPHABET, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionNextSymbolsConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_next_symbols));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.MODE_SYMBOLS, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionCycleInModeConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_left_action),
        getApplicationContext().getString(R.string.swipe_action_value_next_inside_mode));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeLeft(false);
    Assert.assertEquals(
        KeyCodes.KEYBOARD_CYCLE_INSIDE_MODE, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionSwitchModeConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_switch_keyboard_mode));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(
        KeyCodes.KEYBOARD_MODE_CHANGE, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionCycleKeyboardsConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_cycle_keyboards));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.KEYBOARD_CYCLE, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionCycleReverseConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_reverse_cycle_keyboards));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(
        KeyCodes.KEYBOARD_REVERSE_CYCLE, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionQuickTextPopIpConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_quick_text_popup));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.QUICK_TEXT_POPUP, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionShiftConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_shift));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.SHIFT, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionHideConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_hide));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.CANCEL, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionBackspaceConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_backspace));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.DELETE, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionBackWordConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_backword));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.DELETE_WORD, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionClearInputConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_clear_input));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.CLEAR_INPUT, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionArrowUpConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_cursor_up));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.ARROW_UP, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionArrowDownConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_cursor_down));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.ARROW_DOWN, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionArrowLeftConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_cursor_left));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.ARROW_LEFT, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionArrowRightConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_cursor_right));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.ARROW_RIGHT, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionSplitLayoutConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_split_layout));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.SPLIT_LAYOUT, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionMergeLayoutConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_merge_layout));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.MERGE_LAYOUT, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionCompactLayoutRightConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_compact_layout_to_right));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(
        KeyCodes.COMPACT_LAYOUT_TO_RIGHT, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionCompactLayoutLeftConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_compact_layout_to_left));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(
        KeyCodes.COMPACT_LAYOUT_TO_LEFT, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionUtilityKeyboardConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_utility_keyboard));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.UTILITY_KEYBOARD, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }

  @Test
  public void testSwipeForActionSpaceConfigurable() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext().getString(R.string.settings_key_swipe_right_action),
        getApplicationContext().getString(R.string.swipe_action_value_space));
    simulateOnStartInputFlow();

    mImeServiceUnderTest.onFirstDownKey('x');
    mImeServiceUnderTest.onSwipeRight(false);
    Assert.assertEquals(KeyCodes.SPACE, mImeServiceUnderTest.getLastOnKeyPrimaryCode());
  }
}
