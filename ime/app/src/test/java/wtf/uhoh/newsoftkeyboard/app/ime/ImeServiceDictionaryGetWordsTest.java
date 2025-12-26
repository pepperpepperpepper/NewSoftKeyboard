package wtf.uhoh.newsoftkeyboard.app.ime;

import android.view.inputmethod.EditorInfo;
import com.anysoftkeyboard.api.KeyCodes;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.testing.TestInputConnection;
import wtf.uhoh.newsoftkeyboard.app.testing.TestableImeService;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceDictionaryGetWordsTest extends ImeServiceBaseTest {

  @Test
  public void testAskForSuggestions() {
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping("h");
    verifySuggestions(true, "h", "he");
    mImeServiceUnderTest.simulateTextTyping("e");
    verifySuggestions(true, "he", "hell", "hello", "he'll");
    mImeServiceUnderTest.simulateTextTyping("l");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");
  }

  @Test
  public void testPerformUpdateSuggestionsOnSeparatorQuickly() {
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping("h");
    verifySuggestions(true, "h", "he");
    mImeServiceUnderTest.simulateTextTyping("e");
    verifySuggestions(true, "he", "hell", "hello", "he'll");
    mImeServiceUnderTest.simulateKeyPress('l', false);
    Assert.assertEquals("hel", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(' ', false);
    // correctly auto-picked
    Assert.assertEquals("he'll ", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testAskForSuggestionsWithoutInputConnectionUpdates() {
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateKeyPress('h');
    verifySuggestions(true, "h", "he");
    mImeServiceUnderTest.simulateKeyPress('e');
    verifySuggestions(true, "he", "hell", "hello", "he'll");
    mImeServiceUnderTest.simulateKeyPress('l');
    verifySuggestions(true, "hel", "he'll", "hello", "hell");
  }

  @Test
  public void testAskForSuggestionsWithDelayedInputConnectionUpdates() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();
    inputConnection.setUpdateSelectionDelay(1000000L);
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateKeyPress('h');
    verifySuggestions(true, "h", "he");
    mImeServiceUnderTest.simulateKeyPress('e');
    verifySuggestions(true, "he", "hell", "hello", "he'll");
    // sending a delayed event from the input-connection.
    // this can happen when the user is clicking fast (in ASK thread), but the other side (the
    // app thread)
    // is too slow, or busy with something to send out events.
    inputConnection.setUpdateSelectionDelay(1L);
    TestRxSchedulers.foregroundFlushAllJobs();

    mImeServiceUnderTest.simulateKeyPress('l');
    verifySuggestions(true, "hel", "he'll", "hello", "hell");
  }

  @Test
  public void testAskForSuggestionsWhenCursorInsideWord() {
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping("h");
    verifySuggestions(true, "h", "he");
    mImeServiceUnderTest.simulateTextTyping("l");
    verifySuggestions(true, "hl");
    // moving one character back, and fixing the word to 'hel'
    mImeServiceUnderTest.setSelectedText(1, 1, true);
    mImeServiceUnderTest.simulateTextTyping("e");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");
  }

  @Test
  public void testAutoPickWordWhenCursorAtTheEndOfTheWord() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping("h");
    verifySuggestions(true, "h", "he");
    mImeServiceUnderTest.simulateTextTyping("e");
    verifySuggestions(true, "he", "hell", "hello", "he'll");
    mImeServiceUnderTest.simulateTextTyping("l");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");

    Assert.assertEquals("", inputConnection.getLastCommitCorrection());
    mImeServiceUnderTest.simulateKeyPress(' ');
    Assert.assertEquals("he'll", inputConnection.getLastCommitCorrection());
    // we should also see the space
    Assert.assertEquals("he'll ", inputConnection.getCurrentTextInInputConnection());
    // now, if we press DELETE, the word should be reverted
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hel", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testManualPickWordAndShouldNotRevert() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();
    mImeServiceUnderTest.simulateTextTyping("h");
    mImeServiceUnderTest.simulateTextTyping("e");
    mImeServiceUnderTest.pickSuggestionManually(2, "hell");
    Assert.assertEquals("hell ", inputConnection.getCurrentTextInInputConnection());
    // now, if we press DELETE, the word should be reverted
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hell", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testManualPickWordAndAnotherSpaceAndBackspace() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();
    mImeServiceUnderTest.simulateTextTyping("h");
    mImeServiceUnderTest.simulateTextTyping("e");
    mImeServiceUnderTest.pickSuggestionManually(2, "hell");
    // another space
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("hell. ", inputConnection.getCurrentTextInInputConnection());
    // now, if we press DELETE, the word should NOT be reverted
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hell.", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testManualPickUnknownWordAndThenBackspace() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();
    mImeServiceUnderTest.simulateTextTyping("hellp");
    mImeServiceUnderTest.pickSuggestionManually(0, "hellp");

    Assert.assertEquals("hellp ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hellp", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testManualPickUnknownWordAndPunctuationAndThenBackspace() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();
    mImeServiceUnderTest.simulateTextTyping("hellp");
    mImeServiceUnderTest.pickSuggestionManually(0, "hellp");

    Assert.assertEquals("hellp ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateTextTyping("!");

    Assert.assertEquals("hellp! ", inputConnection.getCurrentTextInInputConnection());
    // now, if we press DELETE, the word should NOT be reverted
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hellp!", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testSpaceAutoPickWordAndAnotherSpaceAndBackspace() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();
    mImeServiceUnderTest.simulateTextTyping("h");
    mImeServiceUnderTest.simulateTextTyping("e");
    mImeServiceUnderTest.simulateTextTyping("l");
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("he'll ", inputConnection.getCurrentTextInInputConnection());
    // another space
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("he'll. ", inputConnection.getCurrentTextInInputConnection());
    // now, if we press DELETE, the word should NOT be reverted
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("he'll.", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testSpaceAutoDisabledAutoCorrectAndBackSpace() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();
    mImeServiceUnderTest.simulateTextTyping("h");
    mImeServiceUnderTest.simulateTextTyping("e");
    mImeServiceUnderTest.simulateTextTyping("l");
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("he'll ", inputConnection.getCurrentTextInInputConnection());
    // another space
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("he'll. ", inputConnection.getCurrentTextInInputConnection());
    // now, if we press DELETE, the word should NOT be reverted
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("he'll.", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testAutoPickWordWhenCursorAtTheEndOfTheWordWithWordSeparator() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping("h");
    verifySuggestions(true, "h", "he");
    mImeServiceUnderTest.simulateTextTyping("e");
    verifySuggestions(true, "he", "hell", "hello", "he'll");
    mImeServiceUnderTest.simulateTextTyping("l");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");

    Assert.assertEquals("", inputConnection.getLastCommitCorrection());
    mImeServiceUnderTest.simulateKeyPress('?');
    Assert.assertEquals("he'll", inputConnection.getLastCommitCorrection());
    // we should also see the question mark
    Assert.assertEquals("he'll?", inputConnection.getCurrentTextInInputConnection());
    // now, if we press DELETE, the word should be reverted
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hel", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testDoesNotAutoPickWordWhenCursorNotAtTheEndOfTheWord() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping("h");
    verifySuggestions(true, "h", "he");
    mImeServiceUnderTest.simulateTextTyping("l");
    verifySuggestions(true, "hl");
    Assert.assertEquals("hl", inputConnection.getCurrentTextInInputConnection());
    // moving one character back, and fixing the word to 'hel'
    mImeServiceUnderTest.setSelectedText(1, 1, true);
    mImeServiceUnderTest.simulateKeyPress('e');
    Assert.assertEquals("hel", inputConnection.getCurrentTextInInputConnection());
    verifySuggestions(true, "hel", "he'll", "hello", "hell");

    Mockito.reset(inputConnection); // clearing any previous interactions with finishComposingText
    Assert.assertEquals("", inputConnection.getLastCommitCorrection());
    mImeServiceUnderTest.simulateKeyPress(' ');
    // this time, it will not auto-pick since the cursor is inside the word (and not at the end)
    Assert.assertEquals("", inputConnection.getLastCommitCorrection());
    // will stop composing in the input-connection
    Mockito.verify(inputConnection).finishComposingText();
    // also, it will abort suggestions
    verifySuggestions(true);
  }

  @Test
  public void testBackSpaceCorrectlyWhenEditingManuallyPickedWord() {
    // related to https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/585
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");

    Assert.assertEquals("", inputConnection.getLastCommitCorrection());
    mImeServiceUnderTest.pickSuggestionManually(0, "hel");
    // at this point, the candidates view will show a hint
    Assert.assertEquals("hel ", inputConnection.getCurrentTextInInputConnection());
    // now, navigating to to the 'e'
    mImeServiceUnderTest.setSelectedText(2, 2, true);
    Assert.assertEquals("hel ", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals(2, inputConnection.getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hl ", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals(1, inputConnection.getCurrentStartPosition());
  }

  @Test
  public void testBackSpaceCorrectlyAfterEnter() {
    // related to https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/920
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");
    Assert.assertEquals("hel", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals("", inputConnection.getLastCommitCorrection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ENTER);
    // note: there shouldn't be any correction possible here.
    Assert.assertEquals("", inputConnection.getLastCommitCorrection());
    Assert.assertEquals("hel\n", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hel", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals(3, inputConnection.getCurrentStartPosition());
  }

  @Test
  public void testBackSpaceCorrectlyAfterAutoSpaceAndEnter() {
    // related to https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/920
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");
    Assert.assertEquals("hel", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.pickSuggestionManually(2, "hello");
    Assert.assertEquals("hello ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateTextTyping("hel");
    Assert.assertEquals("hello hel", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ENTER);
    // note: there shouldn't be any correction possible here.
    Assert.assertEquals("", inputConnection.getLastCommitCorrection());
    Assert.assertEquals("hello hel\n", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hello hel", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals(9, inputConnection.getCurrentStartPosition());
  }

  @Test
  public void testBackSpaceCorrectlyAfterAutoSpaceAndEnterWithDelayedUpdates() throws Exception {
    // related to https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/920
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();
    inputConnection.setUpdateSelectionDelay(10000000L);

    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");
    Assert.assertEquals("hel", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.pickSuggestionManually(2, "hello");
    Assert.assertEquals("hello ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateTextTyping("hel");
    Assert.assertEquals("hello hel", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ENTER);
    // note: there shouldn't be any correction possible here.
    Assert.assertEquals("", inputConnection.getLastCommitCorrection());
    Assert.assertEquals("hello hel\n", inputConnection.getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE, false);
    Assert.assertEquals("hello hel", inputConnection.getCurrentTextInInputConnection());

    TestRxSchedulers.foregroundFlushAllJobs();
    Assert.assertEquals("hello hel", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals(9, inputConnection.getCurrentStartPosition());
  }

  @Test
  public void testBackSpaceCorrectlyWhenEditingAutoCorrectedWord() {
    // related to https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/585
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");

    Assert.assertEquals("", inputConnection.getLastCommitCorrection());
    mImeServiceUnderTest.simulateKeyPress(' ');
    Assert.assertEquals("he'll ", inputConnection.getCurrentTextInInputConnection());
    // now, navigating to to the 'e'
    mImeServiceUnderTest.setSelectedText(2, 2, true);
    Assert.assertEquals("he'll ", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals(2, inputConnection.getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("h'll ", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals(1, inputConnection.getCurrentStartPosition());
  }

  @Test
  public void testBackSpaceAfterAutoPickingAutoSpaceAndEnter() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");

    mImeServiceUnderTest.simulateKeyPress(' ');

    Assert.assertEquals("he'll ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ENTER);

    Assert.assertEquals("he'll\n", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals(6, inputConnection.getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("he'll", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals(5, inputConnection.getCurrentStartPosition());
  }

  @Test
  public void testBackSpaceAfterAutoPickingAndEnterWithoutAutoSpace() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_auto_space, false);

    simulateFinishInputFlow();
    simulateOnStartInputFlow(
        false, TestableImeService.createEditorInfo(EditorInfo.IME_ACTION_NONE, 0));

    verifySuggestions(true);
    mImeServiceUnderTest.simulateTextTyping("hel");

    mImeServiceUnderTest.simulateKeyPress(' ');

    Assert.assertEquals("hel ", mImeServiceUnderTest.getCurrentInputConnectionText());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ENTER);

    Assert.assertEquals("hel\n", mImeServiceUnderTest.getCurrentInputConnectionText());
    Assert.assertEquals(4, mImeServiceUnderTest.getTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hel", mImeServiceUnderTest.getCurrentInputConnectionText());
    Assert.assertEquals(3, mImeServiceUnderTest.getTestInputConnection().getCurrentStartPosition());
  }

  @Test
  public void testBackSpaceAfterAutoPickingWithoutAutoSpace() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_auto_space, false);

    simulateFinishInputFlow();
    simulateOnStartInputFlow();

    mImeServiceUnderTest.simulateTextTyping("hell hell");

    mImeServiceUnderTest.simulateKeyPress(' ');

    Assert.assertEquals("hell hell ", mImeServiceUnderTest.getCurrentInputConnectionText());
    Assert.assertEquals(
        "hell hell ".length(),
        mImeServiceUnderTest.getTestInputConnection().getCurrentStartPosition());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    Assert.assertEquals("hell hell", mImeServiceUnderTest.getCurrentInputConnectionText());
    Assert.assertEquals(
        "hell hell".length(),
        mImeServiceUnderTest.getTestInputConnection().getCurrentStartPosition());

    mImeServiceUnderTest.simulateKeyPress('l');
    Assert.assertEquals("hell helll", mImeServiceUnderTest.getCurrentInputConnectionText());
    Assert.assertEquals(
        "hell helll".length(),
        mImeServiceUnderTest.getTestInputConnection().getCurrentStartPosition());
  }

  @Test
  public void testBackSpaceAfterManualPickingAutoSpaceAndEnter() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");

    mImeServiceUnderTest.pickSuggestionManually(1, "hell");

    Assert.assertEquals("hell ", inputConnection.getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ENTER);

    Assert.assertEquals("hell\n", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals(5, inputConnection.getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hell", inputConnection.getCurrentTextInInputConnection());
    Assert.assertEquals(4, inputConnection.getCurrentStartPosition());
  }

  @Test
  public void testBackSpaceAfterManualPickingWithoutAutoSpaceAndEnter() {
    SharedPrefsHelper.setPrefsValue("insert_space_after_word_suggestion_selection", false);

    simulateFinishInputFlow();
    simulateOnStartInputFlow();

    verifySuggestions(true);
    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");

    mImeServiceUnderTest.pickSuggestionManually(1, "hell");

    Assert.assertEquals("hell", mImeServiceUnderTest.getCurrentInputConnectionText());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ENTER);

    Assert.assertEquals("hell\n", mImeServiceUnderTest.getCurrentInputConnectionText());
    Assert.assertEquals(5, mImeServiceUnderTest.getTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hell", mImeServiceUnderTest.getCurrentInputConnectionText());
    Assert.assertEquals(4, mImeServiceUnderTest.getTestInputConnection().getCurrentStartPosition());
  }

  @Test
  public void testManualPickWordLongerWordAndBackspaceAndTypeCharacter() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();
    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");
    mImeServiceUnderTest.pickSuggestionManually(1, "hell");
    Assert.assertEquals("hell ", inputConnection.getCurrentTextInInputConnection());
    // backspace
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hell", inputConnection.getCurrentTextInInputConnection());
    // some random character now
    mImeServiceUnderTest.simulateKeyPress('k');
    Assert.assertEquals("hellk", inputConnection.getCurrentTextInInputConnection());
  }

  @Test
  public void testDoesNotSuggestInPasswordField() {
    simulateFinishInputFlow();

    EditorInfo editorInfo =
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NEXT,
            EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);

    simulateOnStartInputFlow(false, editorInfo);

    mImeServiceUnderTest.resetMockCandidateView();

    mImeServiceUnderTest.simulateTextTyping("hel");
    verifyNoSuggestionsInteractions();
    Assert.assertEquals("hel", mImeServiceUnderTest.getCurrentInputConnectionText());

    mImeServiceUnderTest.simulateKeyPress(' ');
    verifySuggestions(true);
    Assert.assertEquals("hel ", mImeServiceUnderTest.getCurrentInputConnectionText());

    mImeServiceUnderTest.simulateTextTyping("hel");
    verifyNoSuggestionsInteractions();
    Assert.assertEquals("hel hel", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testForwardDelete() {
    mImeServiceUnderTest.simulateTextTyping("hello");
    Assert.assertEquals("hello", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(5, getCurrentTestInputConnection().getCurrentStartPosition());

    mImeServiceUnderTest.setSelectedText(2, 2, true);
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.FORWARD_DELETE);
    Assert.assertEquals("helo", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.FORWARD_DELETE);
    Assert.assertEquals("heo", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.FORWARD_DELETE);
    Assert.assertEquals("he", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());

    verifySuggestions(true, "he", "hell", "hello", "he'll");

    // should not do anything
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.FORWARD_DELETE);
    Assert.assertEquals("he", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());
  }

  @Test
  public void testForwardDeleteAcrossWords() {
    mImeServiceUnderTest.simulateTextTyping("hello you all");

    mImeServiceUnderTest.setSelectedText(2, 2, true);
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.FORWARD_DELETE);
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.FORWARD_DELETE);
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.FORWARD_DELETE);
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.FORWARD_DELETE);
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.FORWARD_DELETE);
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.FORWARD_DELETE);

    Assert.assertEquals(
        "heu all", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());
  }

  @Test
  public void testTypeWordFixInnerMoveToEndAndDelete() {
    mImeServiceUnderTest.simulateTextTyping("hllo");
    Assert.assertEquals("hllo", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    mImeServiceUnderTest.setSelectedText(1, 1, true);
    Assert.assertEquals(1, getCurrentTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress('e');
    Assert.assertEquals("hello", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());

    mImeServiceUnderTest.setSelectedText(5, 5, true);
    Assert.assertEquals("hello", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(5, getCurrentTestInputConnection().getCurrentStartPosition());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hell", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(4, getCurrentTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hel", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(3, getCurrentTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("he", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("h", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(1, getCurrentTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(0, getCurrentTestInputConnection().getCurrentStartPosition());

    mImeServiceUnderTest.simulateKeyPress('d');
    Assert.assertEquals("d", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(1, getCurrentTestInputConnection().getCurrentStartPosition());
  }

  @Test
  public void testJumpToMiddleAndThenBackToEnd() {
    mImeServiceUnderTest.simulateTextTyping("hello");
    Assert.assertEquals("hello", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    mImeServiceUnderTest.setSelectedText(1, 1, true);
    Assert.assertEquals(1, getCurrentTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.setSelectedText(5, 5, true);
    Assert.assertEquals(5, getCurrentTestInputConnection().getCurrentStartPosition());
    Assert.assertEquals("hello", getCurrentTestInputConnection().getCurrentTextInInputConnection());

    mImeServiceUnderTest.simulateKeyPress('d');
    Assert.assertEquals(
        "hellod", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(6, getCurrentTestInputConnection().getCurrentStartPosition());
  }
}
