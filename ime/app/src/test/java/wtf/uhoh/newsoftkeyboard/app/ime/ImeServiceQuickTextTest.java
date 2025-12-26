package wtf.uhoh.newsoftkeyboard.app.ime;

import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import com.anysoftkeyboard.api.KeyCodes;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.quicktextkeys.ui.QuickTextPagerView;
import wtf.uhoh.newsoftkeyboard.app.testing.TestInputConnection;
import wtf.uhoh.newsoftkeyboard.app.testing.TestableImeService;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;
import wtf.uhoh.newsoftkeyboard.testing.TestUtils;

@RunWith(NskRobolectricTestRunner.class)
/*since we are sensitive to actual latest unicode emojis*/
@Config(sdk = TestUtils.LATEST_WINDOW_SUPPORTING_API_LEVEL)
public class ImeServiceQuickTextTest extends ImeServiceBaseTest {
  private static final String KEY_OUTPUT = "\uD83D\uDE03";

  @Test
  public void testOutputTextKeyOutputText() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.QUICK_TEXT);

    Assert.assertEquals(KEY_OUTPUT, inputConnection.getCurrentTextInInputConnection());
    AtomicBoolean foundQuickTextView = new AtomicBoolean(false);
    for (int childIndex = 0;
        childIndex < mImeServiceUnderTest.getInputViewContainer().getChildCount();
        childIndex++) {
      if (mImeServiceUnderTest.getInputViewContainer().getChildAt(childIndex)
          instanceof QuickTextPagerView) {
        foundQuickTextView.set(true);
      }
    }
    Assert.assertFalse(foundQuickTextView.get());
    Assert.assertFalse(mImeServiceUnderTest.isCurrentlyPredicting());

    Assert.assertSame(
        mImeServiceUnderTest.getInputView(),
        mImeServiceUnderTest.getInputViewContainer().getChildAt(1));

    Assert.assertEquals(View.VISIBLE, ((View) mImeServiceUnderTest.getInputView()).getVisibility());
  }

  @Test
  public void testOutputTextKeyOutputShiftedTextWhenShifted() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    final Keyboard.Key aKey = mImeServiceUnderTest.findKeyWithPrimaryKeyCode('a');
    aKey.text = "this";
    aKey.shiftedText = "THiS";
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    Assert.assertTrue(mImeServiceUnderTest.mShiftKeyState.isActive());
    mImeServiceUnderTest.onText(aKey, aKey.shiftedText);
    TestRxSchedulers.foregroundFlushAllJobs();

    Assert.assertEquals("THiS", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals(4, mImeServiceUnderTest.getCursorPosition());
    Assert.assertFalse(mImeServiceUnderTest.isCurrentlyPredicting());
  }

  @Test
  public void testOutputTextKeyOutputTextWhenNotShifted() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    Assert.assertFalse(mImeServiceUnderTest.mShiftKeyState.isActive());
    final Keyboard.Key aKey = mImeServiceUnderTest.findKeyWithPrimaryKeyCode('a');
    aKey.text = "thisis";
    aKey.shiftedText = "THiS";
    mImeServiceUnderTest.onText(aKey, aKey.text);
    TestRxSchedulers.foregroundFlushAllJobs();

    Assert.assertEquals("thisis", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals(
        6, mImeServiceUnderTest.getCurrentTestInputConnection().getCurrentStartPosition());
    Assert.assertFalse(mImeServiceUnderTest.isCurrentlyPredicting());
  }

  @Test
  public void testOutputTextKeyOutputTextWhenShiftedButHasNoShiftedText() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    Assert.assertFalse(mImeServiceUnderTest.mShiftKeyState.isActive());
    final Keyboard.Key aKey = mImeServiceUnderTest.findKeyWithPrimaryKeyCode('a');
    aKey.text = "thisis";

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    Assert.assertTrue(mImeServiceUnderTest.mShiftKeyState.isActive());
    mImeServiceUnderTest.onText(aKey, aKey.text);
    TestRxSchedulers.foregroundFlushAllJobs();

    Assert.assertEquals("thisis", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals(
        6, mImeServiceUnderTest.getCurrentTestInputConnection().getCurrentStartPosition());
    Assert.assertFalse(mImeServiceUnderTest.isCurrentlyPredicting());
  }

  @Test
  public void testOutputTextKeyOutputTextWhenShiftLocked() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    final Keyboard.Key aKey = mImeServiceUnderTest.findKeyWithPrimaryKeyCode('a');
    aKey.text = "thisis";
    aKey.shiftedText = "THiS";
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    Assert.assertTrue(mImeServiceUnderTest.mShiftKeyState.isActive());
    Assert.assertTrue(mImeServiceUnderTest.mShiftKeyState.isLocked());
    mImeServiceUnderTest.onText(aKey, aKey.shiftedText);
    TestRxSchedulers.foregroundFlushAllJobs();

    Assert.assertEquals("THiS", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals(
        4, mImeServiceUnderTest.getCurrentTestInputConnection().getCurrentStartPosition());
    Assert.assertFalse(mImeServiceUnderTest.isCurrentlyPredicting());
  }

  @Test
  public void testOutputTextKeyOutputTextAndThenBackspace() {
    final Keyboard.Key aKey = mImeServiceUnderTest.findKeyWithPrimaryKeyCode('a');
    aKey.text = "thisis";
    aKey.shiftedText = "THiS";
    mImeServiceUnderTest.onText(aKey, aKey.text);
    TestRxSchedulers.foregroundFlushAllJobs();

    Assert.assertEquals("thisis", mImeServiceUnderTest.getCurrentInputConnectionText());
    Assert.assertEquals(
        6, mImeServiceUnderTest.getCurrentTestInputConnection().getCurrentStartPosition());
    Assert.assertFalse(mImeServiceUnderTest.isCurrentlyPredicting());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    // deletes all the output text
    Assert.assertEquals("", mImeServiceUnderTest.getCurrentInputConnectionText());
    Assert.assertEquals(
        0, mImeServiceUnderTest.getCurrentTestInputConnection().getCurrentStartPosition());
    Assert.assertFalse(mImeServiceUnderTest.isCurrentlyPredicting());
  }

  @Test
  public void testOutputTextKeyOverrideOutputText() {
    simulateFinishInputFlow();
    final String overrideText = "TEST ";
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_emoticon_default_text, overrideText);
    simulateOnStartInputFlow();

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.QUICK_TEXT);

    Assert.assertEquals(overrideText, mImeServiceUnderTest.getCurrentInputConnectionText());
    Assert.assertFalse(mImeServiceUnderTest.isCurrentlyPredicting());
  }

  @Test
  public void testOutputTextDeletesOnBackspace() {
    simulateFinishInputFlow();
    final String overrideText = "TEST ";
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_emoticon_default_text, overrideText);
    simulateOnStartInputFlow();

    final String initialText = "hello ";
    mImeServiceUnderTest.simulateTextTyping(initialText);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.QUICK_TEXT);

    Assert.assertEquals(
        initialText + overrideText, mImeServiceUnderTest.getCurrentInputConnectionText());
    Assert.assertFalse(mImeServiceUnderTest.isCurrentlyPredicting());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertEquals(initialText, mImeServiceUnderTest.getCurrentInputConnectionText());
    Assert.assertFalse(mImeServiceUnderTest.isCurrentlyPredicting());
  }

  @Test
  public void testOutputTextDoesNotAutoCorrect() {
    simulateFinishInputFlow();
    final String overrideText = ".";
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_emoticon_default_text, overrideText);
    simulateOnStartInputFlow();

    final String initialText = "hel";
    mImeServiceUnderTest.simulateTextTyping(initialText);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.QUICK_TEXT);
    Assert.assertFalse(mImeServiceUnderTest.isCurrentlyPredicting());

    Assert.assertEquals(
        initialText + overrideText, mImeServiceUnderTest.getCurrentInputConnectionText());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertEquals(initialText, mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testOutputTextDeletesOnBackspaceWhenSuggestionsOff() {
    simulateFinishInputFlow();
    final String overrideText = "TEST ";
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_emoticon_default_text, overrideText);
    SharedPrefsHelper.setPrefsValue("candidates_on", false);
    simulateOnStartInputFlow();

    final String initialText = "hello ";
    mImeServiceUnderTest.simulateTextTyping(initialText);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.QUICK_TEXT);

    Assert.assertEquals(
        initialText + overrideText, mImeServiceUnderTest.getCurrentInputConnectionText());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertEquals(initialText, mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testOutputTextDeletesOnBackspaceWithoutSpace() {
    simulateFinishInputFlow();
    final String overrideText = "TEST";
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_emoticon_default_text, overrideText);
    simulateOnStartInputFlow();
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    final String initialText = "hello ";
    mImeServiceUnderTest.simulateTextTyping(initialText);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.QUICK_TEXT);

    Assert.assertEquals(
        initialText + overrideText, inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertEquals(initialText, inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testOutputTextDeletesOnBackspaceWhenSuggestionsOffWithoutSpace() {
    simulateFinishInputFlow();
    final String overrideText = "TEST";
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_emoticon_default_text, overrideText);
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_show_suggestions, false);
    simulateOnStartInputFlow();

    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    final String initialText = "hello ";
    mImeServiceUnderTest.simulateTextTyping(initialText);

    Assert.assertEquals(initialText, inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.QUICK_TEXT);

    Assert.assertEquals(
        initialText + overrideText, inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertEquals(initialText, inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testOutputTextDoesNotDeletesOnBackspaceIfCursorMoves() {
    simulateFinishInputFlow();
    final String overrideText = "TEST ";
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_emoticon_default_text, overrideText);
    simulateOnStartInputFlow();
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    final String initialText = "hello Xello ";
    mImeServiceUnderTest.simulateTextTyping(initialText);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.QUICK_TEXT);

    Assert.assertEquals(
        initialText + overrideText, inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.moveCursorToPosition(7, true);
    Assert.assertEquals(7, inputConnection.getCurrentStartPosition());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertSame(mImeServiceUnderTest.getCurrentInputConnection(), inputConnection);
    Assert.assertEquals(
        (initialText + overrideText).replace("X", ""),
        inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testOutputTextDoesNotDeletesOnBackspaceIfCursorMovesWhenSuggestionsOff() {
    simulateFinishInputFlow();
    final String overrideText = "TEST ";
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_emoticon_default_text, overrideText);
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_show_suggestions, false);
    simulateOnStartInputFlow();

    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    final String initialText = "hello Xello ";
    mImeServiceUnderTest.simulateTextTyping(initialText);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.QUICK_TEXT);

    Assert.assertEquals(
        initialText + overrideText, inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.moveCursorToPosition(7, true);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertEquals(
        (initialText + overrideText).replace("X", ""),
        inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testOutputTextDoesNotDeletesOnCharacterIfCursorMoves() {
    simulateFinishInputFlow();
    final String overrideText = "TEST ";
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_emoticon_default_text, overrideText);
    simulateOnStartInputFlow();

    final String initialText = "hello Xello ";
    mImeServiceUnderTest.simulateTextTyping(initialText);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.QUICK_TEXT);

    Assert.assertEquals(
        initialText + overrideText, mImeServiceUnderTest.getCurrentInputConnectionText());

    mImeServiceUnderTest.moveCursorToPosition(7, true);

    mImeServiceUnderTest.simulateKeyPress('a');

    Assert.assertEquals(
        (initialText + overrideText).replace("X", "Xa"),
        mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testOutputTextKeySwitchKeyboardWhenFlipped() {
    simulateFinishInputFlow();
    SharedPrefsHelper.setPrefsValue(
        R.string.settings_key_do_not_flip_quick_key_codes_functionality, false);
    simulateOnStartInputFlow();

    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.QUICK_TEXT);

    Assert.assertEquals("", inputConnection.getCurrentTextInInputConnection());

    AtomicBoolean foundQuickTextView = new AtomicBoolean(false);
    for (int childIndex = 0;
        childIndex < mImeServiceUnderTest.getInputViewContainer().getChildCount();
        childIndex++) {
      if (mImeServiceUnderTest.getInputViewContainer().getChildAt(childIndex)
          instanceof QuickTextPagerView) {
        foundQuickTextView.set(true);
      }
    }
    Assert.assertTrue(foundQuickTextView.get());

    Assert.assertSame(
        mImeServiceUnderTest.getInputView(),
        mImeServiceUnderTest.getInputViewContainer().getChildAt(1));

    Assert.assertEquals(View.GONE, ((View) mImeServiceUnderTest.getInputView()).getVisibility());
  }

  @Test
  public void testPopupTextKeyOutputTextWhenFlipped() {
    SharedPrefsHelper.setPrefsValue(
        R.string.settings_key_do_not_flip_quick_key_codes_functionality, false);

    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.QUICK_TEXT_POPUP);

    Assert.assertEquals(KEY_OUTPUT, inputConnection.getCurrentTextInInputConnection());

    AtomicBoolean foundQuickTextView = new AtomicBoolean(false);
    for (int childIndex = 0;
        childIndex < mImeServiceUnderTest.getInputViewContainer().getChildCount();
        childIndex++) {
      if (mImeServiceUnderTest.getInputViewContainer().getChildAt(childIndex)
          instanceof QuickTextPagerView) {
        foundQuickTextView.set(true);
      }
    }
    Assert.assertFalse(foundQuickTextView.get());

    Assert.assertSame(
        mImeServiceUnderTest.getInputView(),
        mImeServiceUnderTest.getInputViewContainer().getStandardKeyboardView());

    Assert.assertEquals(View.VISIBLE, ((View) mImeServiceUnderTest.getInputView()).getVisibility());
  }

  @Test
  public void testPopupTextKeySwitchKeyboard() {
    Assert.assertEquals(View.VISIBLE, ((View) mImeServiceUnderTest.getInputView()).getVisibility());
    Assert.assertEquals(
        View.VISIBLE,
        mImeServiceUnderTest.getInputViewContainer().getCandidateView().getVisibility());

    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.QUICK_TEXT_POPUP);

    Assert.assertEquals("", inputConnection.getCurrentTextInInputConnection());

    AtomicBoolean foundQuickTextView = new AtomicBoolean(false);
    for (int childIndex = 0;
        childIndex < mImeServiceUnderTest.getInputViewContainer().getChildCount();
        childIndex++) {
      if (mImeServiceUnderTest.getInputViewContainer().getChildAt(childIndex)
          instanceof QuickTextPagerView) {
        foundQuickTextView.set(true);
      }
    }
    Assert.assertTrue(foundQuickTextView.get());

    Assert.assertSame(
        mImeServiceUnderTest.getInputView(),
        mImeServiceUnderTest.getInputViewContainer().getStandardKeyboardView());

    Assert.assertEquals(View.GONE, ((View) mImeServiceUnderTest.getInputView()).getVisibility());
    Assert.assertEquals(
        View.GONE, mImeServiceUnderTest.getInputViewContainer().getCandidateView().getVisibility());
  }

  @Test
  public void testSecondPressOnQuickTextKeyDoesNotCloseKeyboard() {
    Assert.assertFalse(mImeServiceUnderTest.isKeyboardViewHidden());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.QUICK_TEXT_POPUP);

    Assert.assertEquals(View.GONE, ((View) mImeServiceUnderTest.getInputView()).getVisibility());
    Assert.assertEquals(
        View.GONE, mImeServiceUnderTest.getInputViewContainer().getCandidateView().getVisibility());

    mImeServiceUnderTest.sendDownUpKeyEvents(KeyEvent.KEYCODE_BACK);

    Assert.assertFalse(mImeServiceUnderTest.isKeyboardViewHidden());
    Assert.assertEquals(View.VISIBLE, ((View) mImeServiceUnderTest.getInputView()).getVisibility());
    Assert.assertEquals(
        View.VISIBLE,
        mImeServiceUnderTest.getInputViewContainer().getCandidateView().getVisibility());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.QUICK_TEXT_POPUP);

    Assert.assertEquals(View.GONE, ((View) mImeServiceUnderTest.getInputView()).getVisibility());
    Assert.assertEquals(
        View.GONE, mImeServiceUnderTest.getInputViewContainer().getCandidateView().getVisibility());
    Assert.assertFalse(mImeServiceUnderTest.isKeyboardViewHidden());

    mImeServiceUnderTest.sendDownUpKeyEvents(KeyEvent.KEYCODE_BACK);

    Assert.assertEquals(View.VISIBLE, ((View) mImeServiceUnderTest.getInputView()).getVisibility());
    Assert.assertEquals(
        View.VISIBLE,
        mImeServiceUnderTest.getInputViewContainer().getCandidateView().getVisibility());
    Assert.assertFalse(mImeServiceUnderTest.isKeyboardViewHidden());

    mImeServiceUnderTest.hideWindow();
    Assert.assertTrue(mImeServiceUnderTest.isKeyboardViewHidden());
  }

  @Test
  public void testCloseQuickTextKeyboardOnInputReallyFinished() {
    Assert.assertEquals(View.VISIBLE, ((View) mImeServiceUnderTest.getInputView()).getVisibility());
    Assert.assertEquals(
        View.VISIBLE,
        mImeServiceUnderTest.getInputViewContainer().getCandidateView().getVisibility());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.QUICK_TEXT_POPUP);

    simulateFinishInputFlow();

    Assert.assertNull(
        mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.quick_text_pager_root));
  }

  @Test
  public void testCloseQuickTextKeyboardOnFinishInputView() {
    Assert.assertEquals(View.VISIBLE, ((View) mImeServiceUnderTest.getInputView()).getVisibility());
    Assert.assertEquals(
        View.VISIBLE,
        mImeServiceUnderTest.getInputViewContainer().getCandidateView().getVisibility());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.QUICK_TEXT_POPUP);
    Assert.assertEquals(
        View.VISIBLE,
        mImeServiceUnderTest
            .getInputViewContainer()
            .findViewById(R.id.quick_text_pager_root)
            .getVisibility());

    final EditorInfo editorInfo = mImeServiceUnderTest.getCurrentInputEditorInfo();
    mImeServiceUnderTest.onFinishInputView(false);

    Assert.assertNull(
        mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.quick_text_pager_root));
    mImeServiceUnderTest.onStartInputView(editorInfo, true);

    Assert.assertNull(
        mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.quick_text_pager_root));
  }

  @Test
  public void testDoesNotReShowCandidatesIfNoCandidatesToBeginWith() {
    simulateFinishInputFlow();
    simulateOnStartInputFlow(
        false,
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS));

    Assert.assertEquals(View.VISIBLE, ((View) mImeServiceUnderTest.getInputView()).getVisibility());
    Assert.assertEquals(
        View.GONE, mImeServiceUnderTest.getInputViewContainer().getCandidateView().getVisibility());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.QUICK_TEXT_POPUP);

    Assert.assertEquals(
        View.VISIBLE,
        mImeServiceUnderTest
            .getInputViewContainer()
            .findViewById(R.id.quick_text_pager_root)
            .getVisibility());
    Assert.assertEquals(View.GONE, ((View) mImeServiceUnderTest.getInputView()).getVisibility());
    Assert.assertEquals(
        View.GONE, mImeServiceUnderTest.getInputViewContainer().getCandidateView().getVisibility());

    mImeServiceUnderTest.sendDownUpKeyEvents(KeyEvent.KEYCODE_BACK);

    Assert.assertEquals(View.VISIBLE, ((View) mImeServiceUnderTest.getInputView()).getVisibility());
    Assert.assertEquals(
        View.GONE, mImeServiceUnderTest.getInputViewContainer().getCandidateView().getVisibility());
    Assert.assertNull(
        mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.quick_text_pager_root));
  }

  @Test
  public void testHomeOnQuickTextKeyClosesKeyboard() {
    Assert.assertFalse(mImeServiceUnderTest.isKeyboardViewHidden());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.QUICK_TEXT_POPUP);

    Assert.assertEquals(View.GONE, ((View) mImeServiceUnderTest.getInputView()).getVisibility());

    // hideWindow() is now, essentially, the same as pressing the HOME hardware key
    mImeServiceUnderTest.hideWindow();

    Assert.assertTrue(mImeServiceUnderTest.isKeyboardViewHidden());
    // we switched to the main-keyboard view
    Assert.assertEquals(View.VISIBLE, ((View) mImeServiceUnderTest.getInputView()).getVisibility());
  }

  @Test
  public void testOutputAsTypingKeyOutput() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    final Keyboard.Key aKey = mImeServiceUnderTest.findKeyWithPrimaryKeyCode('a');
    aKey.typedText = "this";
    aKey.shiftedTypedText = "THiS";
    Assert.assertFalse(mImeServiceUnderTest.mShiftKeyState.isActive());
    mImeServiceUnderTest.onTyping(aKey, aKey.typedText);
    TestRxSchedulers.foregroundFlushAllJobs();

    Assert.assertEquals("this", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals(
        4, mImeServiceUnderTest.getCurrentTestInputConnection().getCurrentStartPosition());
    Assert.assertTrue(mImeServiceUnderTest.isCurrentlyPredicting());
  }

  @Test
  public void testOutputAsTypingKeyOutputShifted() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    final Keyboard.Key aKey = mImeServiceUnderTest.findKeyWithPrimaryKeyCode('a');
    aKey.typedText = "this";
    aKey.shiftedTypedText = "THiS";
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    Assert.assertTrue(mImeServiceUnderTest.mShiftKeyState.isActive());
    mImeServiceUnderTest.onTyping(aKey, aKey.shiftedTypedText);
    TestRxSchedulers.foregroundFlushAllJobs();

    Assert.assertEquals("THiS", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals(
        4, mImeServiceUnderTest.getCurrentTestInputConnection().getCurrentStartPosition());
    Assert.assertTrue(mImeServiceUnderTest.isCurrentlyPredicting());
  }

  @Test
  public void testOutputTextKeyOutputTypingAndThenBackspace() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    final Keyboard.Key aKey = mImeServiceUnderTest.findKeyWithPrimaryKeyCode('a');
    aKey.typedText = "thisis";
    aKey.shiftedTypedText = "THiS";
    mImeServiceUnderTest.onTyping(aKey, aKey.typedText);
    TestRxSchedulers.drainAllTasksUntilEnd();

    Assert.assertEquals("thisis", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals(
        6, mImeServiceUnderTest.getCurrentTestInputConnection().getCurrentStartPosition());
    Assert.assertTrue(mImeServiceUnderTest.isCurrentlyPredicting());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    // deletes text as if was typed
    Assert.assertEquals("thisi", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals(
        5, mImeServiceUnderTest.getCurrentTestInputConnection().getCurrentStartPosition());
    Assert.assertTrue(mImeServiceUnderTest.isCurrentlyPredicting());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("this", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals(
        4, mImeServiceUnderTest.getCurrentTestInputConnection().getCurrentStartPosition());
    Assert.assertTrue(mImeServiceUnderTest.isCurrentlyPredicting());
  }
}
