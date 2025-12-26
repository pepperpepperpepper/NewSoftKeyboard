package wtf.uhoh.newsoftkeyboard.app.ime;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static wtf.uhoh.newsoftkeyboard.app.keyboards.ExternalKeyboardTest.SIMPLE_KeyboardDimens;

import android.content.res.Configuration;
import android.os.SystemClock;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import androidx.test.core.app.ApplicationProvider;
import com.anysoftkeyboard.api.KeyCodes;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.robolectric.annotation.Config;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.ExternalKeyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardKey;
import wtf.uhoh.newsoftkeyboard.app.testing.TestInputConnection;
import wtf.uhoh.newsoftkeyboard.app.testing.TestableImeService;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceGimmicksTest extends ImeServiceBaseTest {

  @Test
  public void testDoubleSpace() {
    final String expectedText = "testing";
    mImeServiceUnderTest.simulateTextTyping(expectedText);

    Assert.assertEquals(expectedText, mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(' ');
    Assert.assertEquals(expectedText + " ", mImeServiceUnderTest.getCurrentInputConnectionText());
    // double space
    mImeServiceUnderTest.simulateKeyPress(' ');
    Assert.assertEquals(expectedText + ". ", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  // https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/2526
  @Test
  public void testDoubleSpaceMoveCommaDoesNotDeletePreviousCharacter() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();
    mImeServiceUnderTest.simulateTextTyping("hello");
    mImeServiceUnderTest.simulateTextTyping("  ");
    // this will produce double-space->dot
    Assert.assertEquals("hello. ", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals("hello. ".length(), inputConnection.getCurrentStartPosition());
    // moving to the beginning of the word
    mImeServiceUnderTest.moveCursorToPosition("hello".length(), true);
    TestRxSchedulers.foregroundFlushAllJobs();
    Assert.assertEquals("hello".length(), inputConnection.getCurrentStartPosition());

    mImeServiceUnderTest.simulateKeyPress(',');
    Assert.assertEquals("hello,. ", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals("hello,".length(), inputConnection.getCurrentStartPosition());
  }

  @Test
  public void testDoubleSpaceNotDoneOnTimeOut() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();
    final String expectedText = "testing";
    inputConnection.commitText(expectedText, 1);

    Assert.assertEquals(expectedText, inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress(' ');
    Assert.assertEquals(expectedText + " ", inputConnection.getCurrentTextInInputConnection());
    // double space very late
    SystemClock.sleep(
        Integer.parseInt(getResText(R.string.settings_default_multitap_timeout).toString()) + 1);
    mImeServiceUnderTest.simulateKeyPress(' ');
    Assert.assertEquals(expectedText + "  ", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testDoubleSpaceNotDoneOnSpaceXSpace() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();
    final String expectedText = "testing";
    inputConnection.commitText(expectedText, 1);

    Assert.assertEquals(expectedText, inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress(' ');
    Assert.assertEquals(expectedText + " ", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress('X');
    Assert.assertEquals(expectedText + " X", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress(' ');
    Assert.assertEquals(expectedText + " X ", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress(' ');
    Assert.assertEquals(expectedText + " X. ", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testDoubleSpaceReDotOnAdditionalSpace() {
    final String expectedText = "testing";
    mImeServiceUnderTest.simulateTextTyping(expectedText);

    Assert.assertEquals(expectedText, mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(' ');
    Assert.assertEquals(expectedText + " ", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(' ');
    Assert.assertEquals(expectedText + ". ", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(' ');
    Assert.assertEquals(expectedText + ".. ", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(' ');
    Assert.assertEquals(
        expectedText + "... ", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testSwitchesFromSymbolsToAlphabetOnSpaceAfterSymbolUsed() {
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    Assert.assertEquals(
        "symbols_keyboard", mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    mImeServiceUnderTest.simulateKeyPress('1');
    Assert.assertEquals(
        "symbols_keyboard", mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    Assert.assertEquals("1 ", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testSticksInSymbolsUntilSymbolPressed() {
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    Assert.assertEquals(
        "symbols_keyboard", mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals(
        "symbols_keyboard", mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    mImeServiceUnderTest.simulateKeyPress('1');
    Assert.assertEquals(
        "symbols_keyboard", mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    Assert.assertEquals(" 1", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    Assert.assertEquals(" 1 ", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testSticksInSymbolsUntilSymbolPressedDouble() {
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    Assert.assertEquals(
        "symbols_keyboard", mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals(
        "symbols_keyboard", mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals(
        "symbols_keyboard", mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    mImeServiceUnderTest.simulateKeyPress('1');
    Assert.assertEquals(
        "symbols_keyboard", mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    Assert.assertEquals(". 1 ", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testSticksInSymbolsWhenSettingIsDisabled() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_switch_keyboard_on_space, false);
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    Assert.assertEquals(
        "symbols_keyboard", mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals(
        "symbols_keyboard", mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    mImeServiceUnderTest.simulateKeyPress('1');
    Assert.assertEquals(
        "symbols_keyboard", mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals(
        "symbols_keyboard", mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    Assert.assertEquals(" 1 ", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testManualPickWordAndAnotherSpaceAndBackspace() {
    mImeServiceUnderTest.simulateTextTyping("h");
    mImeServiceUnderTest.simulateTextTyping("e");
    mImeServiceUnderTest.pickSuggestionManually(2, "hell");
    TestRxSchedulers.foregroundFlushAllJobs();
    // should have the picked word with an auto-added space
    Assert.assertEquals("hell ", mImeServiceUnderTest.getCurrentInputConnectionText());
    // another space should add a dot
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("hell. ", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("hell.. ", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("hell... ", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testSwapPunctuationWithAutoSpaceOnManuallyPicked() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");

    mImeServiceUnderTest.pickSuggestionManually(2, "hello");
    Assert.assertEquals("hello ", inputConnection.getCurrentTextInInputConnection());
    // typing punctuation
    mImeServiceUnderTest.simulateKeyPress('.');
    Assert.assertEquals("hello. ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress('h');
    Assert.assertEquals("hello. h", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testVerifyVariousPunctuationsSwapping() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    final var symbolsToVerify = Arrays.asList('.', ',', ':', ';', '?', '!', ')');

    final var expected = new StringBuilder();

    for (char punc : symbolsToVerify) {
      mImeServiceUnderTest.simulateTextTyping("hel");
      verifySuggestions(true, "hel", "he'll", "hello", "hell");

      mImeServiceUnderTest.pickSuggestionManually(2, "hello");
      expected.append("hello ");
      Assert.assertEquals(expected.toString(), inputConnection.getCurrentTextInInputConnection());
      // typing punctuation
      mImeServiceUnderTest.simulateKeyPress(punc);
      expected.setLength(expected.length() - 1);
      expected.append(punc).append(' ');
      Assert.assertEquals(expected.toString(), inputConnection.getCurrentTextInInputConnection());
    }
  }

  @Test
  public void testVerifyVariousPunctuationsSwappingFromOtherKeyboard() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    final var symbolsToVerify = Arrays.asList('.', ':', '?', '!', ')');

    final var expected = new StringBuilder();

    for (char punc : symbolsToVerify) {
      mImeServiceUnderTest.simulateTextTyping("hel");
      verifySuggestions(true, "hel", "he'll", "hello", "hell");

      mImeServiceUnderTest.pickSuggestionManually(2, "hello");
      expected.append("hello ");
      Assert.assertEquals(expected.toString(), inputConnection.getCurrentTextInInputConnection());
      // switching to symbols
      mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
      Assert.assertNotNull(mImeServiceUnderTest.findKeyWithPrimaryKeyCode(punc));
      // typing punctuation
      mImeServiceUnderTest.simulateKeyPress(punc);
      expected.setLength(expected.length() - 1);
      expected.append(punc).append(' ');
      Assert.assertEquals(expected.toString(), inputConnection.getCurrentTextInInputConnection());
      // switching to alphabet
      mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    }
  }

  @Test
  public void testVerifyVariousPunctuationsDoNotSwap() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    final var symbolsToVerify = Arrays.asList('\'', '\"', '(');

    final var expected = new StringBuilder();

    for (char punc : symbolsToVerify) {
      mImeServiceUnderTest.simulateTextTyping(" hel");
      verifySuggestions(true, "hel", "he'll", "hello", "hell");

      mImeServiceUnderTest.pickSuggestionManually(2, "hello");
      expected.append(" hello ");
      Assert.assertEquals(expected.toString(), inputConnection.getCurrentTextInInputConnection());
      // typing punctuation
      mImeServiceUnderTest.simulateKeyPress(punc);
      expected.append(punc);
      Assert.assertEquals(expected.toString(), inputConnection.getCurrentTextInInputConnection());
    }
  }

  @Test
  public void testSwapPunctuationWithAutoSpaceOnAutoCorrected() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("he'll ", inputConnection.getCurrentTextInInputConnection());
    // typing punctuation
    mImeServiceUnderTest.simulateKeyPress(',');
    Assert.assertEquals("he'll, ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress('h');
    Assert.assertEquals("he'll, h", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testDoNotSwapNonPunctuationWithAutoSpaceOnAutoCorrected() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("he'll ", inputConnection.getCurrentTextInInputConnection());
    // typing punctuation
    mImeServiceUnderTest.simulateKeyPress('2');
    Assert.assertEquals("he'll 2", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);

    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("he'll 2 he'll ", inputConnection.getCurrentTextInInputConnection());
    // typing punctuation
    mImeServiceUnderTest.simulateKeyPress('^');
    Assert.assertEquals("he'll 2 he'll ^", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testDoNotSwapPunctuationWithOnText() {
    mImeServiceUnderTest.simulateTextTyping("hel");

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals(
        "he'll ", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    // typing punctuation
    mImeServiceUnderTest.onText(null, ":)");
    Assert.assertEquals(
        "he'll :)", getCurrentTestInputConnection().getCurrentTextInInputConnection());
  }

  @Test
  public void testDoNotSwapPunctuationIfSwapPrefDisabled() {
    SharedPrefsHelper.setPrefsValue(
        getApplicationContext()
            .getString(R.string.settings_key_bool_should_swap_punctuation_and_space),
        false);
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hel");

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("he'll ", inputConnection.getCurrentTextInInputConnection());
    // typing punctuation
    mImeServiceUnderTest.simulateKeyPress(',');
    Assert.assertEquals("he'll, ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress('h');
    Assert.assertEquals("he'll, h", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testSwapPunctuationWithAutoSpaceOnAutoPicked() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hell");
    verifySuggestions(true, "hell", "hello");

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("hell ", inputConnection.getCurrentTextInInputConnection());
    // typing punctuation
    mImeServiceUnderTest.simulateKeyPress('?');
    Assert.assertEquals("hell? ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress('h');
    Assert.assertEquals("hell? h", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testSendsENTERKeyEventIfShiftIsNotPressedAndImeDoesNotHaveAction() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ENTER);

    ArgumentCaptor<KeyEvent> keyEventArgumentCaptor = ArgumentCaptor.forClass(KeyEvent.class);
    Mockito.verify(inputConnection, Mockito.times(2))
        .sendKeyEvent(keyEventArgumentCaptor.capture());

    Assert.assertEquals(2 /*down and up*/, keyEventArgumentCaptor.getAllValues().size());
    Assert.assertEquals(
        KeyEvent.KEYCODE_ENTER, keyEventArgumentCaptor.getAllValues().get(0).getKeyCode());
    Assert.assertEquals(
        KeyEvent.ACTION_DOWN, keyEventArgumentCaptor.getAllValues().get(0).getAction());
    Assert.assertEquals(
        KeyEvent.KEYCODE_ENTER, keyEventArgumentCaptor.getAllValues().get(1).getKeyCode());
    Assert.assertEquals(
        KeyEvent.ACTION_UP, keyEventArgumentCaptor.getAllValues().get(1).getAction());
    // and never the ENTER character
    Assert.assertEquals("\n", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testSendsENTERKeyEventIfShiftIsPressedAndImeDoesNotHaveAction() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ENTER);

    ArgumentCaptor<KeyEvent> keyEventArgumentCaptor = ArgumentCaptor.forClass(KeyEvent.class);
    Mockito.verify(inputConnection, Mockito.times(2))
        .sendKeyEvent(keyEventArgumentCaptor.capture());

    Assert.assertEquals(2 /*down and up*/, keyEventArgumentCaptor.getAllValues().size());
    Assert.assertEquals(
        KeyEvent.KEYCODE_ENTER, keyEventArgumentCaptor.getAllValues().get(0).getKeyCode());
    Assert.assertEquals(
        KeyEvent.ACTION_DOWN, keyEventArgumentCaptor.getAllValues().get(0).getAction());
    Assert.assertEquals(
        KeyEvent.KEYCODE_ENTER, keyEventArgumentCaptor.getAllValues().get(1).getKeyCode());
    Assert.assertEquals(
        KeyEvent.ACTION_UP, keyEventArgumentCaptor.getAllValues().get(1).getAction());
    // and we have ENTER in the input-connection
    Assert.assertEquals("\n", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testSendsENTERCharacterIfShiftIsPressedAndImeHasAction() {
    mImeServiceUnderTest.onFinishInputView(true);
    mImeServiceUnderTest.onFinishInput();

    EditorInfo editorInfo = createEditorInfoTextWithSuggestionsForSetUp();
    editorInfo.imeOptions = EditorInfo.IME_ACTION_GO;
    mImeServiceUnderTest.onStartInput(editorInfo, false);
    mImeServiceUnderTest.onStartInputView(editorInfo, false);

    TestInputConnection inputConnection = getCurrentTestInputConnection();
    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ENTER);

    Mockito.verify(inputConnection).commitText("\n", 1);
    // and never the key-events
    Mockito.verify(inputConnection, Mockito.never()).sendKeyEvent(Mockito.any(KeyEvent.class));
  }

  @Test
  public void testShiftEnterSendsNewLine() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();
    mImeServiceUnderTest.simulateTextTyping("this he a test");
    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ENTER);
    mImeServiceUnderTest.simulateTextTyping("this he a test\n");
    InOrder inOrder = Mockito.inOrder(inputConnection);
    inOrder.verify(inputConnection).beginBatchEdit();
    inOrder.verify(inputConnection).commitText("this", 1);
    inOrder.verify(inputConnection).commitText("he", 1);
    inOrder.verify(inputConnection).commitText("a", 1);
    // test is not committed, it is just done composing.
    inOrder
        .verify(inputConnection, Mockito.never())
        .commitText(Mockito.eq("test"), Mockito.anyInt());
    inOrder.verify(inputConnection).finishComposingText();
    inOrder.verify(inputConnection).commitText("\n", 1);
    inOrder.verify(inputConnection).endBatchEdit();
  }

  @Test
  public void testDeleteWholeWordWhenShiftAndBackSpaceArePressed() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hello");
    Assert.assertEquals("hello", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertEquals("", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testDoesNotDeleteEntireWordWhenShiftDeleteInsideWord() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateTextTyping("Auto");
    mImeServiceUnderTest.simulateKeyPress(' ');
    mImeServiceUnderTest.simulateTextTyping("space");
    Assert.assertEquals("Auto space", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.moveCursorToPosition(7, true);

    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertEquals("Auto ace", inputConnection.getCurrentTextInInputConnection());

    Assert.assertEquals(5, getCurrentTestInputConnection().getCurrentStartPosition());
  }

  @Test
  public void testDoesNotDeleteEntireWordWhenShiftDeleteInsideWordWhenNotPredicting() {
    simulateFinishInputFlow();

    mImeServiceUnderTest.getResources().getConfiguration().keyboard = Configuration.KEYBOARD_NOKEYS;

    simulateOnStartInputFlow(
        false,
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS));

    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateTextTyping("Auto");
    mImeServiceUnderTest.simulateKeyPress(' ');
    mImeServiceUnderTest.simulateTextTyping("space");
    Assert.assertEquals("Auto space", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.moveCursorToPosition(7, true);

    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertEquals("Auto ace", inputConnection.getCurrentTextInInputConnection());

    Assert.assertEquals(5, getCurrentTestInputConnection().getCurrentStartPosition());
  }

  @Test
  public void testHappyPathBackWordWhenNotPredicting() {
    simulateFinishInputFlow();

    mImeServiceUnderTest.getResources().getConfiguration().keyboard = Configuration.KEYBOARD_NOKEYS;

    simulateOnStartInputFlow(
        false,
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS));

    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateTextTyping("Auto");
    mImeServiceUnderTest.simulateKeyPress(' ');
    mImeServiceUnderTest.simulateTextTyping("space");
    Assert.assertEquals("Auto space", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertEquals("Auto ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertEquals("", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testHappyPathBackWordWhenPredicting() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateTextTyping("Auto");
    mImeServiceUnderTest.simulateKeyPress(' ');
    mImeServiceUnderTest.simulateTextTyping("space");
    Assert.assertEquals("Auto space", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertEquals("Auto ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertEquals("", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testDeleteCharacterWhenNoShiftAndBackSpaceArePressed() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hello");
    Assert.assertEquals("hello", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertEquals("hell", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testDeleteWholeTextFromOnText() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hello ");
    Assert.assertEquals("hello ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.onText(null, "text");

    Assert.assertEquals("hello text", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertEquals("hello ", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testDeleteCharacterWhenShiftAndBackSpaceArePressedAndOptionDisabled() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_use_backword, false);

    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hello");
    Assert.assertEquals("hello", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertEquals("hell", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testDeleteCharacterWhenShiftLockedAndBackSpaceArePressed() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hello");
    Assert.assertEquals("hello", inputConnection.getCurrentTextInInputConnection());

    Assert.assertFalse(mImeServiceUnderTest.getCurrentKeyboardForTests().isShifted());
    Assert.assertFalse(mImeServiceUnderTest.getCurrentKeyboardForTests().isShiftLocked());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    Assert.assertTrue(mImeServiceUnderTest.getCurrentKeyboardForTests().isShifted());
    Assert.assertFalse(mImeServiceUnderTest.getCurrentKeyboardForTests().isShiftLocked());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    // now it is locked
    Assert.assertTrue(mImeServiceUnderTest.getCurrentKeyboardForTests().isShifted());
    Assert.assertTrue(mImeServiceUnderTest.getCurrentKeyboardForTests().isShiftLocked());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertEquals("hell", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testDeleteCharacterWhenShiftLockedAndHeldAndBackSpaceArePressed() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hello");
    Assert.assertEquals("hello", inputConnection.getCurrentTextInInputConnection());

    Assert.assertFalse(mImeServiceUnderTest.getCurrentKeyboardForTests().isShifted());
    Assert.assertFalse(mImeServiceUnderTest.getCurrentKeyboardForTests().isShiftLocked());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    Assert.assertTrue(mImeServiceUnderTest.getCurrentKeyboardForTests().isShifted());
    Assert.assertFalse(mImeServiceUnderTest.getCurrentKeyboardForTests().isShiftLocked());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    // now it is locked
    Assert.assertTrue(mImeServiceUnderTest.getCurrentKeyboardForTests().isShifted());
    Assert.assertTrue(mImeServiceUnderTest.getCurrentKeyboardForTests().isShiftLocked());

    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertEquals("", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testDeleteCharacterWhenNoShiftAndBackSpaceArePressedAndOptionDisabled() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_use_backword, false);
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hello");
    Assert.assertEquals("hello", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertEquals("hell", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testSwapPunctuationWithAutoSpaceOnAutoCorrectedWithPunctuation() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");

    // typing punctuation
    mImeServiceUnderTest.simulateKeyPress('!');
    Assert.assertEquals("he'll!", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress(' ');
    Assert.assertEquals("he'll! ", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testSwapPunctuationWithAutoSpaceOnAutoPickedWithPunctuation() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");

    // typing punctuation
    mImeServiceUnderTest.simulateKeyPress('.');
    Assert.assertEquals("he'll.", inputConnection.getCurrentTextInInputConnection());
    // typing punctuation
    mImeServiceUnderTest.simulateKeyPress('h');
    Assert.assertEquals("he'll.h", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testSwapPunctuationWithAutoSpaceOnAutoPickedWithDoublePunctuation() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");

    // typing punctuation
    mImeServiceUnderTest.simulateKeyPress('.');
    Assert.assertEquals("he'll.", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress('.');
    Assert.assertEquals("he'll..", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress(' ');
    Assert.assertEquals("he'll.. ", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testPrintsParenthesisAsIsWithLTRKeyboard() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateKeyPress('(');
    Assert.assertEquals("(", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress(')');
    Assert.assertEquals("()", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testPrintsParenthesisReversedWithRTLKeyboard() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    KeyboardDefinition fakeRtlKeyboard =
        Mockito.spy(mImeServiceUnderTest.getCurrentKeyboardForTests());
    Mockito.doReturn(false).when(fakeRtlKeyboard).isLeftToRightLanguage();
    mImeServiceUnderTest.onAlphabetKeyboardSet(fakeRtlKeyboard);

    mImeServiceUnderTest.simulateKeyPress('(');
    Assert.assertEquals(")", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress(')');
    Assert.assertEquals(")(", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testShiftBehaviorForLetters() throws Exception {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("q", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);

    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("qQ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("qQq", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);

    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("qQqQ", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("qQqQQ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);

    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("qQqQQq", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);

    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("qQqQQqQ", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("qQqQQqQQ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.onRelease(KeyCodes.SHIFT);

    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("qQqQQqQQq", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testLongShiftBehaviorForLetters() throws Exception {
    final int longPressTime =
        Integer.parseInt(getResText(R.string.settings_default_long_press_timeout).toString()) + 20;

    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("q", inputConnection.getCurrentTextInInputConnection());

    // long press should switch to caps-lock
    KeyboardKey shiftKey =
        (KeyboardKey) mImeServiceUnderTest.findKeyWithPrimaryKeyCode(KeyCodes.SHIFT);
    Assert.assertNotNull(shiftKey);

    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    SystemClock.sleep(longPressTime);
    mImeServiceUnderTest.onRelease(KeyCodes.SHIFT);
    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("qQ", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("qQQ", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("qQQQ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("qQQQq", inputConnection.getCurrentTextInInputConnection());

    // now from lock to unlock with just shift
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT_LOCK);
    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("qQQQqQ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("qQQQqQq", inputConnection.getCurrentTextInInputConnection());

    // and now long-press but multi-touch typing
    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    SystemClock.sleep(longPressTime);

    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("qQQQqQqQ", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("qQQQqQqQQ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.onRelease(KeyCodes.SHIFT);

    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("qQQQqQqQQq", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    SystemClock.sleep(longPressTime);
    mImeServiceUnderTest.onRelease(KeyCodes.SHIFT);

    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("qQQQqQqQQqQ", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("qQQQqQqQQqQQ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("qQQQqQqQQqQQq", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress('q');
    Assert.assertEquals("qQQQqQqQQqQQqq", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testShiftBehaviorForNonLetters() throws Exception {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateKeyPress('\'');
    Assert.assertEquals("'", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);

    mImeServiceUnderTest.simulateKeyPress('\'');
    Assert.assertEquals("''", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress('\'');
    Assert.assertEquals("'''", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);

    mImeServiceUnderTest.simulateKeyPress('\'');
    Assert.assertEquals("''''", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress('\'');
    Assert.assertEquals("'''''", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);

    mImeServiceUnderTest.simulateKeyPress('\'');
    Assert.assertEquals("''''''", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    mImeServiceUnderTest.getCurrentKeyboardForTests().getShiftKey().onPressed();

    mImeServiceUnderTest.simulateKeyPress('\'');
    Assert.assertEquals("''''''\"", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress('\'');
    Assert.assertEquals("''''''\"\"", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.onRelease(KeyCodes.SHIFT);
    mImeServiceUnderTest.getCurrentKeyboardForTests().getShiftKey().onReleased();

    mImeServiceUnderTest.simulateKeyPress('\'');
    Assert.assertEquals("''''''\"\"'", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testEditorPerformsActionIfImeOptionsSpecified() throws Exception {
    mImeServiceUnderTest.onFinishInputView(true);
    mImeServiceUnderTest.onFinishInput();

    EditorInfo editorInfo =
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_DONE,
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    mImeServiceUnderTest.onStartInput(editorInfo, false);
    mImeServiceUnderTest.onStartInputView(editorInfo, false);
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    Assert.assertEquals(0, inputConnection.getLastEditorAction());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ENTER);
    Assert.assertEquals(EditorInfo.IME_ACTION_DONE, inputConnection.getLastEditorAction());
    // did not passed the ENTER to the IC
    Assert.assertEquals("", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testEditorPerformsActionIfActionLabelSpecified() throws Exception {
    mImeServiceUnderTest.onFinishInputView(true);
    mImeServiceUnderTest.onFinishInput();

    EditorInfo editorInfo =
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_UNSPECIFIED,
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    editorInfo.actionId = 99;
    editorInfo.actionLabel = "test label";
    mImeServiceUnderTest.onStartInput(editorInfo, false);
    mImeServiceUnderTest.onStartInputView(editorInfo, false);
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    Assert.assertEquals(0, inputConnection.getLastEditorAction());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ENTER);
    Assert.assertEquals(99, inputConnection.getLastEditorAction());
    // did not passed the ENTER to the IC
    Assert.assertEquals("", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testEditorDoesNotPerformsActionIfNoEnterActionFlagIsSet() throws Exception {
    mImeServiceUnderTest.onFinishInputView(true);
    mImeServiceUnderTest.onFinishInput();

    EditorInfo editorInfo =
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_ENTER_ACTION,
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    mImeServiceUnderTest.onStartInput(editorInfo, false);
    mImeServiceUnderTest.onStartInputView(editorInfo, false);
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    Assert.assertEquals(0, inputConnection.getLastEditorAction());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ENTER);
    // did not perform action
    Assert.assertEquals(0, inputConnection.getLastEditorAction());
    // passed the ENTER to the IC
    Assert.assertEquals("\n", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testEditorDoesPerformsActionImeIsUnSpecified() throws Exception {
    mImeServiceUnderTest.onFinishInputView(true);
    mImeServiceUnderTest.onFinishInput();

    EditorInfo editorInfo =
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_UNSPECIFIED,
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    mImeServiceUnderTest.onStartInput(editorInfo, false);
    mImeServiceUnderTest.onStartInputView(editorInfo, false);
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    Assert.assertEquals(0, inputConnection.getLastEditorAction());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ENTER);
    // did not perform action
    Assert.assertEquals(EditorInfo.IME_ACTION_UNSPECIFIED, inputConnection.getLastEditorAction());
    // did not passed the ENTER to the IC
    Assert.assertEquals("", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testEditorPerformsActionIfSpecifiedButNotSendingEnter() throws Exception {
    mImeServiceUnderTest.onFinishInputView(true);
    mImeServiceUnderTest.onFinishInput();

    EditorInfo editorInfo =
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_DONE,
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    mImeServiceUnderTest.onStartInput(editorInfo, false);
    mImeServiceUnderTest.onStartInputView(editorInfo, false);
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    Assert.assertEquals(0, inputConnection.getLastEditorAction());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals(0, inputConnection.getLastEditorAction());
    Assert.assertEquals(" ", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testSendsEnterIfNoneAction() throws Exception {
    mImeServiceUnderTest.onFinishInputView(true);
    mImeServiceUnderTest.onFinishInput();

    EditorInfo editorInfo =
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    mImeServiceUnderTest.onStartInput(editorInfo, false);
    mImeServiceUnderTest.onStartInputView(editorInfo, false);
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    Assert.assertEquals(0, inputConnection.getLastEditorAction());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ENTER);
    Assert.assertEquals(0, inputConnection.getLastEditorAction());
    // passed the ENTER to the IC
    Assert.assertEquals("\n", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testSendsEnterIfUnspecificAction() throws Exception {
    mImeServiceUnderTest.onFinishInputView(true);
    mImeServiceUnderTest.onFinishInput();

    EditorInfo editorInfo =
        TestableImeService.createEditorInfo(EditorInfo.IME_ACTION_UNSPECIFIED, 0);
    mImeServiceUnderTest.onStartInput(editorInfo, false);
    mImeServiceUnderTest.onStartInputView(editorInfo, false);
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    Assert.assertEquals(0, inputConnection.getLastEditorAction());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ENTER);
    Assert.assertEquals(0, inputConnection.getLastEditorAction());
  }

  @Test
  @Config(qualifiers = "w480dp-h640dp-port-mdpi")
  public void testSplitStatesPortrait() {
    getApplicationContext().getResources().getConfiguration().keyboard =
        Configuration.KEYBOARD_NOKEYS;

    // verify device config, to ensure test is valid
    Assert.assertEquals(160, getApplicationContext().getResources().getConfiguration().densityDpi);
    Assert.assertEquals(
        480, getApplicationContext().getResources().getConfiguration().screenWidthDp);
    Assert.assertEquals(
        640, getApplicationContext().getResources().getConfiguration().screenHeightDp);
    Assert.assertEquals(
        Configuration.ORIENTATION_PORTRAIT,
        getApplicationContext().getResources().getConfiguration().orientation);

    simulateFinishInputFlow();
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_default_split_state_portrait, "split");
    Assert.assertTrue(mImeServiceUnderTest.isKeyboardViewHidden());

    simulateOnStartInputFlow(true, createEditorInfoTextWithSuggestionsForSetUp());

    Assert.assertFalse(mImeServiceUnderTest.isKeyboardViewHidden());
    mImeServiceUnderTest.getKeyboardSwitcherForTests().verifyKeyboardsFlushed();

    assertKeyDimensions(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeys().get(0), 2, 5, 130);

    SharedPrefsHelper.setPrefsValue(
        R.string.settings_key_default_split_state_portrait, "compact_right");
    Assert.assertTrue(mImeServiceUnderTest.isKeyboardViewHidden());

    simulateOnStartInputFlow(true, createEditorInfoTextWithSuggestionsForSetUp());

    Assert.assertFalse(mImeServiceUnderTest.isKeyboardViewHidden());
    mImeServiceUnderTest.getKeyboardSwitcherForTests().verifyKeyboardsFlushed();
    assertKeyDimensions(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeys().get(0), 168, 5, 105);

    SharedPrefsHelper.setPrefsValue(
        R.string.settings_key_default_split_state_portrait, "compact_left");
    Assert.assertTrue(mImeServiceUnderTest.isKeyboardViewHidden());

    simulateOnStartInputFlow(true, createEditorInfoTextWithSuggestionsForSetUp());

    Assert.assertFalse(mImeServiceUnderTest.isKeyboardViewHidden());
    mImeServiceUnderTest.getKeyboardSwitcherForTests().verifyKeyboardsFlushed();
    assertKeyDimensions(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeys().get(0), 2, 5, 105);

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_default_split_state_portrait, "merged");
    Assert.assertTrue(mImeServiceUnderTest.isKeyboardViewHidden());

    simulateOnStartInputFlow(true, createEditorInfoTextWithSuggestionsForSetUp());

    Assert.assertFalse(mImeServiceUnderTest.isKeyboardViewHidden());
    mImeServiceUnderTest.getKeyboardSwitcherForTests().verifyKeyboardsFlushed();
    assertKeyDimensions(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeys().get(0), 2, 5, 163);
  }

  @Test
  @Config(qualifiers = "w480dp-h640dp-land-mdpi")
  public void testSplitStatesLandscape() {
    getApplicationContext().getResources().getConfiguration().keyboard =
        Configuration.KEYBOARD_NOKEYS;

    // verify device config, to ensure test is valid
    Assert.assertEquals(160, getApplicationContext().getResources().getConfiguration().densityDpi);
    Assert.assertEquals(
        640, getApplicationContext().getResources().getConfiguration().screenWidthDp);
    Assert.assertEquals(
        480, getApplicationContext().getResources().getConfiguration().screenHeightDp);
    Assert.assertEquals(
        Configuration.ORIENTATION_LANDSCAPE,
        getApplicationContext().getResources().getConfiguration().orientation);

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_default_split_state_landscape, "split");

    Assert.assertTrue(mImeServiceUnderTest.isKeyboardViewHidden());

    simulateOnStartInputFlow(true, createEditorInfoTextWithSuggestionsForSetUp());

    Assert.assertFalse(mImeServiceUnderTest.isKeyboardViewHidden());
    mImeServiceUnderTest.getKeyboardSwitcherForTests().verifyKeyboardsFlushed();
    // split, since we switched to landscape
    assertKeyDimensions(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeys().get(0), 2, 5, 131);

    SharedPrefsHelper.setPrefsValue(
        R.string.settings_key_default_split_state_landscape, "compact_right");
    Assert.assertTrue(mImeServiceUnderTest.isKeyboardViewHidden());

    simulateOnStartInputFlow(true, createEditorInfoTextWithSuggestionsForSetUp());

    Assert.assertFalse(mImeServiceUnderTest.isKeyboardViewHidden());
    mImeServiceUnderTest.getKeyboardSwitcherForTests().verifyKeyboardsFlushed();
    assertKeyDimensions(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeys().get(0), 378, 5, 87);

    SharedPrefsHelper.setPrefsValue(
        R.string.settings_key_default_split_state_landscape, "compact_left");
    Assert.assertTrue(mImeServiceUnderTest.isKeyboardViewHidden());

    simulateOnStartInputFlow(true, createEditorInfoTextWithSuggestionsForSetUp());

    Assert.assertFalse(mImeServiceUnderTest.isKeyboardViewHidden());
    mImeServiceUnderTest.getKeyboardSwitcherForTests().verifyKeyboardsFlushed();
    assertKeyDimensions(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeys().get(0), 2, 5, 87);

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_default_split_state_landscape, "merged");
    Assert.assertTrue(mImeServiceUnderTest.isKeyboardViewHidden());

    simulateOnStartInputFlow(true, createEditorInfoTextWithSuggestionsForSetUp());

    Assert.assertFalse(mImeServiceUnderTest.isKeyboardViewHidden());
    mImeServiceUnderTest.getKeyboardSwitcherForTests().verifyKeyboardsFlushed();
    assertKeyDimensions(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeys().get(0), 2, 5, 219);
  }

  @Test
  public void testSwapDoublePunctuationsWhenNotInFrLocale() {
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hel");

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("he'll ", inputConnection.getCurrentTextInInputConnection());
    // typing punctuation
    mImeServiceUnderTest.simulateKeyPress('!');
    Assert.assertEquals("he'll! ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateTextTyping("hel");

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("he'll! he'll ", inputConnection.getCurrentTextInInputConnection());
    // typing punctuation
    mImeServiceUnderTest.simulateKeyPress('?');
    Assert.assertEquals("he'll! he'll? ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateTextTyping("hel");

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("he'll! he'll? he'll ", inputConnection.getCurrentTextInInputConnection());
    // typing punctuation
    mImeServiceUnderTest.simulateKeyPress(':');
    Assert.assertEquals("he'll! he'll? he'll: ", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testDoNotSwapDoublePunctuationsWhenInFrLocale() {
    final KeyboardDefinition currentKeyboard = mImeServiceUnderTest.getCurrentKeyboardForTests();
    ExternalKeyboard keyboard =
        new ExternalKeyboard(
            currentKeyboard.getKeyboardAddOn(),
            ApplicationProvider.getApplicationContext(),
            R.xml.qwerty,
            R.xml.qwerty,
            "fr",
            R.drawable.ic_status_english,
            0,
            "fr",
            "",
            new String(currentKeyboard.getSentenceSeparators()),
            currentKeyboard.getKeyboardMode());
    keyboard.loadKeyboard(SIMPLE_KeyboardDimens);

    mImeServiceUnderTest.onAlphabetKeyboardSet(keyboard);
    TestInputConnection inputConnection = getCurrentTestInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hel");

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("hel ", inputConnection.getCurrentTextInInputConnection());
    // typing punctuation
    mImeServiceUnderTest.simulateKeyPress('!');
    Assert.assertEquals("hel !", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);

    mImeServiceUnderTest.simulateTextTyping("hel");

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("hel ! hel ", inputConnection.getCurrentTextInInputConnection());
    // typing punctuation
    mImeServiceUnderTest.simulateKeyPress('?');
    Assert.assertEquals("hel ! hel ?", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);

    mImeServiceUnderTest.simulateTextTyping("hel");

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("hel ! hel ? hel ", inputConnection.getCurrentTextInInputConnection());
    // typing punctuation
    mImeServiceUnderTest.simulateKeyPress(':');
    Assert.assertEquals("hel ! hel ? hel :", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);

    mImeServiceUnderTest.simulateTextTyping("hel");

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals(
        "hel ! hel ? hel : hel ", inputConnection.getCurrentTextInInputConnection());
    // typing punctuation
    mImeServiceUnderTest.simulateKeyPress(';');
    Assert.assertEquals(
        "hel ! hel ? hel : hel ;", inputConnection.getCurrentTextInInputConnection());
  }

  private void assertKeyDimensions(Keyboard.Key key, int x, int y, int width) {
    Assert.assertEquals("X position is wrong", x, key.x, 5 /*pixel slop*/);
    Assert.assertEquals("Y position is wrong", y, key.y, 5 /*pixel slop*/);
    Assert.assertEquals("Key width is wrong", width, key.width, 5 /*pixel slop*/);
  }
}
