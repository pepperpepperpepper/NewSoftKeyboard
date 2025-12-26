package wtf.uhoh.newsoftkeyboard.app.ime;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static wtf.uhoh.newsoftkeyboard.app.ime.KeyboardUIStateHandler.MSG_RESTART_NEW_WORD_SUGGESTIONS;
import static wtf.uhoh.newsoftkeyboard.app.testing.TestableImeService.createEditorInfo;

import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import com.anysoftkeyboard.api.KeyCodes;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Shadows;
import org.robolectric.annotation.LooperMode;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewContainerView;
import wtf.uhoh.newsoftkeyboard.app.testing.TestableImeService;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeFactory;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceSuggestionsTest extends ImeServiceBaseTest {

  @Test
  public void testPredictionOnInDefaultTextField() {
    final EditorInfo editorInfo =
        createEditorInfo(EditorInfo.IME_ACTION_NONE, EditorInfo.TYPE_CLASS_TEXT);
    simulateOnStartInputFlow(false, editorInfo);
    Assert.assertTrue(mImeServiceUnderTest.isPredictionOn());
  }

  @Test
  public void testStripActionLifeCycle() {
    Assert.assertNotNull(
        mImeServiceUnderTest
            .getInputViewContainer()
            .findViewById(R.id.close_suggestions_strip_text));

    simulateFinishInputFlow();

    Assert.assertNull(
        mImeServiceUnderTest
            .getInputViewContainer()
            .findViewById(R.id.close_suggestions_strip_text));
  }

  @Test
  public void testStripActionRemovedWhenAbortingPrediction() {
    Assert.assertNotNull(
        mImeServiceUnderTest
            .getInputViewContainer()
            .findViewById(R.id.close_suggestions_strip_text));

    mImeServiceUnderTest.abortCorrectionAndResetPredictionState(true);

    Assert.assertNull(
        mImeServiceUnderTest
            .getInputViewContainer()
            .findViewById(R.id.close_suggestions_strip_text));
  }

  @Test
  public void testStripActionNotRemovedWhenAbortingPredictionNotForever() {
    Assert.assertNotNull(
        mImeServiceUnderTest
            .getInputViewContainer()
            .findViewById(R.id.close_suggestions_strip_text));

    mImeServiceUnderTest.abortCorrectionAndResetPredictionState(false);

    Assert.assertNotNull(
        mImeServiceUnderTest
            .getInputViewContainer()
            .findViewById(R.id.close_suggestions_strip_text));
  }

  @Test
  public void testStripActionNotAddedWhenInNonPredictiveField() {
    simulateFinishInputFlow();

    Assert.assertNull(
        mImeServiceUnderTest
            .getInputViewContainer()
            .findViewById(R.id.close_suggestions_strip_text));

    final EditorInfo editorInfo =
        createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
    simulateOnStartInputFlow(false, editorInfo);

    Assert.assertNull(
        mImeServiceUnderTest
            .getInputViewContainer()
            .findViewById(R.id.close_suggestions_strip_text));
  }

  @Test
  public void testStripActionNotAddedWhenInSuggestionsDisabled() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_show_suggestions, false);
    simulateFinishInputFlow();
    Assert.assertNull(
        mImeServiceUnderTest
            .getInputViewContainer()
            .findViewById(R.id.close_suggestions_strip_text));

    simulateOnStartInputFlow();

    Assert.assertNull(
        mImeServiceUnderTest
            .getInputViewContainer()
            .findViewById(R.id.close_suggestions_strip_text));
  }

  @Test
  public void testNextWordHappyPath() {
    mImeServiceUnderTest.simulateTextTyping("hello face hello face hello face hello face ");
    mImeServiceUnderTest.simulateTextTyping("hello ");
    verifySuggestions(true, "face");
    mImeServiceUnderTest.pickSuggestionManually(0, "face");
    TestRxSchedulers.drainAllTasks();
    Assert.assertEquals(
        "hello face hello face hello face hello face hello face ",
        getCurrentTestInputConnection().getCurrentTextInInputConnection());
    verifySuggestions(true, "hello");
  }

  @Test
  public void testNextWordPicksTypedIfValidButHasLongerNext() {
    mImeServiceUnderTest.simulateTextTyping("hell");
    verifySuggestions(true, "hell", "hello");
    mImeServiceUnderTest.simulateTextTyping("o ");
    mImeServiceUnderTest.simulateTextTyping("face hello face hello face hello face ");
    mImeServiceUnderTest.simulateTextTyping("face ");
    // next word suggest is "hello"
    verifySuggestions(true, "hello");
    // typing "hell", which is a valid word
    mImeServiceUnderTest.simulateTextTyping("hell");
    // seeing both suggesting
    verifySuggestions(true, "hell", "hello");
    Assert.assertEquals(
        "hello face hello face hello face hello face face hell",
        getCurrentTestInputConnection().getCurrentTextInInputConnection());
    // doing auto-pick
    mImeServiceUnderTest.simulateTextTyping(" ");
    Assert.assertEquals(
        "hello face hello face hello face hello face face hell ",
        getCurrentTestInputConnection().getCurrentTextInInputConnection());
  }

  @Test
  public void testNextWordDeleteAfterPick() {
    mImeServiceUnderTest.simulateTextTyping("hello face hello face hello face hello face ");
    mImeServiceUnderTest.simulateTextTyping("hello ");
    verifySuggestions(true, "face");
    mImeServiceUnderTest.pickSuggestionManually(0, "face");
    TestRxSchedulers.drainAllTasks();
    Assert.assertEquals(
        "hello face hello face hello face hello face hello face ",
        getCurrentTestInputConnection().getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals(
        "hello face hello face hello face hello face hello face",
        getCurrentTestInputConnection().getCurrentTextInInputConnection());
  }

  @Test
  @LooperMode(LooperMode.Mode.LEGACY) /*sensitive to animations*/
  public void testClickingCancelPredicationHappyPath() {
    TestRxSchedulers.drainAllTasks();
    TestRxSchedulers.foregroundAdvanceBy(10000);
    final KeyboardViewContainerView.StripActionProvider provider =
        ((ImeSuggestionsController) mImeServiceUnderTest).mCancelSuggestionsAction;
    View rootActionView =
        provider
            .inflateActionView(mImeServiceUnderTest.getInputViewContainer())
            .findViewById(R.id.close_suggestions_strip_root);
    final View.OnClickListener onClickListener =
        Shadows.shadowOf(rootActionView).getOnClickListener();
    final View image = rootActionView.findViewById(R.id.close_suggestions_strip_icon);
    final View text = rootActionView.findViewById(R.id.close_suggestions_strip_text);

    Assert.assertEquals(View.VISIBLE, image.getVisibility());
    Assert.assertEquals(View.GONE, text.getVisibility());

    Shadows.shadowOf(Looper.getMainLooper()).pause();
    onClickListener.onClick(rootActionView);
    // should be shown for some time
    Assert.assertEquals(View.VISIBLE, text.getVisibility());
    // strip is not removed
    Assert.assertNotNull(
        mImeServiceUnderTest
            .getInputViewContainer()
            .findViewById(R.id.close_suggestions_strip_text));

    Assert.assertTrue(mImeServiceUnderTest.isPredictionOn());
    Shadows.shadowOf(Looper.getMainLooper()).unPause();
    Assert.assertEquals(View.GONE, text.getVisibility());

    Shadows.shadowOf(Looper.getMainLooper()).pause();
    onClickListener.onClick(rootActionView);
    Assert.assertEquals(View.VISIBLE, text.getVisibility());
    Assert.assertNotNull(
        mImeServiceUnderTest
            .getInputViewContainer()
            .findViewById(R.id.close_suggestions_strip_text));

    // removing
    onClickListener.onClick(rootActionView);
    Shadows.shadowOf(Looper.getMainLooper()).unPause();
    Assert.assertNull(
        mImeServiceUnderTest
            .getInputViewContainer()
            .findViewById(R.id.close_suggestions_strip_text));
    Assert.assertFalse(mImeServiceUnderTest.isPredictionOn());
  }

  @Test
  public void testStripTheming() {
    final KeyboardThemeFactory keyboardThemeFactory =
        NskApplicationBase.getKeyboardThemeFactory(getApplicationContext());
    simulateFinishInputFlow();
    mImeServiceUnderTest.resetMockCandidateView();

    // switching to light icon
    keyboardThemeFactory.setAddOnEnabled("18c558ef-bc8c-433a-a36e-92c3ca3be4dd", true);
    simulateOnStartInputFlow();
    Mockito.verify(mImeServiceUnderTest.getMockCandidateView(), Mockito.atLeastOnce())
        .setKeyboardTheme(Mockito.same(keyboardThemeFactory.getEnabledAddOn()));
    Mockito.verify(mImeServiceUnderTest.getMockCandidateView(), Mockito.atLeastOnce())
        .setThemeOverlay(Mockito.notNull());
    Mockito.verify(mImeServiceUnderTest.getMockCandidateView()).getCloseIcon();

    simulateFinishInputFlow();
    mImeServiceUnderTest.resetMockCandidateView();

    // switching to dark icon
    keyboardThemeFactory.setAddOnEnabled("8774f99e-fb4a-49fa-b8d0-4083f762250a", true);
    simulateOnStartInputFlow();
    Mockito.verify(mImeServiceUnderTest.getMockCandidateView(), Mockito.atLeastOnce())
        .setKeyboardTheme(Mockito.same(keyboardThemeFactory.getEnabledAddOn()));
    Mockito.verify(mImeServiceUnderTest.getMockCandidateView(), Mockito.atLeastOnce())
        .setThemeOverlay(Mockito.notNull());
    Mockito.verify(mImeServiceUnderTest.getMockCandidateView()).getCloseIcon();
  }

  @Test
  public void testSuggestionsRestartWhenMovingCursor() {
    simulateFinishInputFlow();
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_allow_suggestions_restart, true);
    simulateOnStartInputFlow();

    mImeServiceUnderTest.simulateTextTyping("hell yes");
    Assert.assertEquals(
        "hell yes", getCurrentTestInputConnection().getCurrentTextInInputConnection());

    mImeServiceUnderTest.resetMockCandidateView();
    mImeServiceUnderTest.moveCursorToPosition(2, true);
    TestRxSchedulers.drainAllTasksUntilEnd();
    Assert.assertEquals(2, mImeServiceUnderTest.getCurrentComposedWord().cursorPosition());
    Assert.assertEquals(
        "hell yes", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    verifySuggestions(true, "hell", "hello");
    Assert.assertEquals(
        "hell", mImeServiceUnderTest.getCurrentComposedWord().getTypedWord().toString());
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals(1, mImeServiceUnderTest.getCurrentComposedWord().cursorPosition());
    Assert.assertEquals(1, getCurrentTestInputConnection().getCurrentStartPosition());
    TestRxSchedulers.foregroundFlushAllJobs();
    TestRxSchedulers.foregroundFlushAllJobs();
    Assert.assertEquals(
        "hll yes", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(1, getCurrentTestInputConnection().getCurrentStartPosition());
    Assert.assertEquals(
        "hll", mImeServiceUnderTest.getCurrentComposedWord().getTypedWord().toString());

    mImeServiceUnderTest.simulateKeyPress('e');
    Assert.assertEquals(
        "hell yes", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());
    Assert.assertEquals(2, mImeServiceUnderTest.getCurrentComposedWord().cursorPosition());
    Assert.assertEquals(
        "hell", mImeServiceUnderTest.getCurrentComposedWord().getTypedWord().toString());
  }

  @Test
  public void testSuggestionsRestartWhenMovingCursorEvenWhenRestarting() {
    simulateFinishInputFlow();
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_allow_suggestions_restart, true);
    simulateOnStartInputFlow(
        true, createEditorInfo(EditorInfo.IME_ACTION_NONE, EditorInfo.TYPE_CLASS_TEXT));

    mImeServiceUnderTest.simulateTextTyping("hell yes");
    Assert.assertEquals(
        "hell yes", getCurrentTestInputConnection().getCurrentTextInInputConnection());

    mImeServiceUnderTest.resetMockCandidateView();
    mImeServiceUnderTest.moveCursorToPosition(2, true);
    TestRxSchedulers.drainAllTasksUntilEnd();
    Assert.assertEquals(2, mImeServiceUnderTest.getCurrentComposedWord().cursorPosition());
    Assert.assertEquals(
        "hell yes", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    verifySuggestions(true, "hell", "hello");
    Assert.assertEquals(
        "hell", mImeServiceUnderTest.getCurrentComposedWord().getTypedWord().toString());
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());
  }

  @Test
  public void testDoesNotPostRestartOnBackspaceWhilePredicting() {
    simulateFinishInputFlow();
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_allow_suggestions_restart, true);
    simulateOnStartInputFlow();

    mImeServiceUnderTest.simulateTextTyping("hel");
    Assert.assertTrue(mImeServiceUnderTest.isCurrentlyPredicting());
    Assert.assertFalse(
        ((ImeSuggestionsController) mImeServiceUnderTest)
            .mKeyboardHandler.hasMessages(MSG_RESTART_NEW_WORD_SUGGESTIONS));
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE, false);
    Assert.assertFalse(
        ((ImeSuggestionsController) mImeServiceUnderTest)
            .mKeyboardHandler.hasMessages(MSG_RESTART_NEW_WORD_SUGGESTIONS));
    SystemClock.sleep(5);
    Assert.assertFalse(
        ((ImeSuggestionsController) mImeServiceUnderTest)
            .mKeyboardHandler.hasMessages(MSG_RESTART_NEW_WORD_SUGGESTIONS));
  }

  @Test
  public void testDeletesCorrectlyIfPredictingButDelayedPositionUpdate() {
    mImeServiceUnderTest.simulateTextTyping("abcd efgh");
    Assert.assertTrue(mImeServiceUnderTest.isCurrentlyPredicting());
    mImeServiceUnderTest.setUpdateSelectionDelay(500);
    Assert.assertEquals("abcd efgh", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE, false);
    Assert.assertEquals("abcd efg", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE, true);
    Assert.assertEquals("abcd ef", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE, false);
    Assert.assertEquals("abcd e", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE, true);
    Assert.assertEquals("abcd ", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE, false);
    Assert.assertEquals("abcd", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE, true);
    Assert.assertEquals("abc", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE, false);
    Assert.assertEquals("ab", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE, true);
    Assert.assertEquals("a", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE, false);
    Assert.assertEquals("", mImeServiceUnderTest.getCurrentInputConnectionText());
    // extra
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE, true);
    Assert.assertEquals("", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testSuggestionsRestartWhenBackSpace() {
    simulateFinishInputFlow();
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_allow_suggestions_restart, true);
    simulateOnStartInputFlow();

    mImeServiceUnderTest.simulateTextTyping("hell face");
    verifySuggestions(true, "face");
    mImeServiceUnderTest.simulateKeyPress(' ');
    Assert.assertEquals(
        "hell face ", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    verifySuggestions(true);

    mImeServiceUnderTest.resetMockCandidateView();
    for (int deleteKeyPress = 6; deleteKeyPress > 0; deleteKeyPress--) {
      // really quickly
      mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE, false);
      TestRxSchedulers.foregroundAdvanceBy(50 /*that's the key-repeat delay in KeyboardViewBase*/);
    }
    TestRxSchedulers.drainAllTasksUntilEnd(); // lots of events in the queue...
    TestRxSchedulers.foregroundAdvanceBy(100);
    verifySuggestions(true, "hell", "hello");
    Assert.assertEquals("hell", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(4, getCurrentTestInputConnection().getCurrentStartPosition());
    Assert.assertEquals(4, mImeServiceUnderTest.getCurrentComposedWord().cursorPosition());
    Assert.assertEquals(
        "hell", mImeServiceUnderTest.getCurrentComposedWord().getTypedWord().toString());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hel", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(3, getCurrentTestInputConnection().getCurrentStartPosition());
    Assert.assertEquals(
        "hel", mImeServiceUnderTest.getCurrentComposedWord().getTypedWord().toString());
    verifySuggestions(true, "hel", "he'll", "hello", "hell");

    mImeServiceUnderTest.simulateKeyPress('l');
    Assert.assertEquals("hell", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    verifySuggestions(true, "hell", "hello");
    Assert.assertEquals(4, getCurrentTestInputConnection().getCurrentStartPosition());
    Assert.assertEquals(
        "hell", mImeServiceUnderTest.getCurrentComposedWord().getTypedWord().toString());
  }

  @Test
  public void testHandleCompleteCandidateUpdateFromExternalAndBackSpaceWithoutRestart() {
    simulateFinishInputFlow();
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_allow_suggestions_restart, false);
    simulateOnStartInputFlow();
    mImeServiceUnderTest.simulateTextTyping("he");
    Assert.assertEquals("he", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    var currentState = getCurrentTestInputConnection().getCurrentState();
    Assert.assertEquals(2, currentState.selectionStart);
    Assert.assertEquals(2, currentState.selectionEnd);
    Assert.assertEquals(0, currentState.candidateStart);
    Assert.assertEquals(2, currentState.candidateEnd);
    Assert.assertTrue(mImeServiceUnderTest.isCurrentlyPredicting());
    // simulating external change
    getCurrentTestInputConnection().setComposingText("hell is here ", 1);

    TestRxSchedulers.foregroundAdvanceBy(100);

    Assert.assertEquals(
        "hell is here ", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    currentState = getCurrentTestInputConnection().getCurrentState();
    Assert.assertEquals(13, currentState.selectionStart);
    Assert.assertEquals(13, currentState.selectionEnd);
    Assert.assertEquals(13, currentState.candidateStart);
    Assert.assertEquals(13, currentState.candidateEnd);
    Assert.assertFalse(mImeServiceUnderTest.isCurrentlyPredicting());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    TestRxSchedulers.drainAllTasksUntilEnd();

    Assert.assertEquals(
        "hell is here", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    currentState = getCurrentTestInputConnection().getCurrentState();

    Assert.assertEquals(12, currentState.selectionStart);
    Assert.assertEquals(12, currentState.selectionEnd);
    Assert.assertEquals(12, currentState.candidateStart);
    Assert.assertEquals(12, currentState.candidateEnd);
    Assert.assertFalse(mImeServiceUnderTest.isCurrentlyPredicting());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    TestRxSchedulers.drainAllTasksUntilEnd();

    Assert.assertEquals(
        "hell is her", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertFalse(mImeServiceUnderTest.isCurrentlyPredicting());
  }

  @Test
  public void testHandleCompleteCandidateUpdateFromExternalAndBackSpaceWithRestart() {
    simulateFinishInputFlow();
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_allow_suggestions_restart, true);
    simulateOnStartInputFlow();
    mImeServiceUnderTest.simulateTextTyping("he");
    Assert.assertEquals("he", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    var currentState = getCurrentTestInputConnection().getCurrentState();
    Assert.assertEquals(2, currentState.selectionStart);
    Assert.assertEquals(2, currentState.selectionEnd);
    Assert.assertEquals(0, currentState.candidateStart);
    Assert.assertEquals(2, currentState.candidateEnd);
    Assert.assertTrue(mImeServiceUnderTest.isCurrentlyPredicting());
    // simulating external change
    getCurrentTestInputConnection().setComposingText("hell is here ", 1);

    TestRxSchedulers.foregroundAdvanceBy(100);

    Assert.assertEquals(
        "hell is here ", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    currentState = getCurrentTestInputConnection().getCurrentState();
    Assert.assertEquals(13, currentState.selectionStart);
    Assert.assertEquals(13, currentState.selectionEnd);
    Assert.assertEquals(13, currentState.candidateStart);
    Assert.assertEquals(13, currentState.candidateEnd);
    Assert.assertFalse(mImeServiceUnderTest.isCurrentlyPredicting());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    TestRxSchedulers.drainAllTasksUntilEnd();

    Assert.assertEquals(
        "hell is here", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    currentState = getCurrentTestInputConnection().getCurrentState();

    Assert.assertEquals(12, currentState.selectionStart);
    Assert.assertEquals(12, currentState.selectionEnd);
    Assert.assertEquals(8, currentState.candidateStart);
    Assert.assertEquals(12, currentState.candidateEnd);
    Assert.assertTrue(mImeServiceUnderTest.isCurrentlyPredicting());
    verifySuggestions(true, "here");

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    TestRxSchedulers.drainAllTasksUntilEnd();

    Assert.assertEquals(
        "hell is her", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertTrue(mImeServiceUnderTest.isCurrentlyPredicting());
    verifySuggestions(true, "her");
  }

  @Test
  public void testSuggestionsRestartHappyPathWhenDisabled() {
    simulateFinishInputFlow();
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_allow_suggestions_restart, false);
    simulateOnStartInputFlow();

    mImeServiceUnderTest.simulateTextTyping("hell yes");
    Assert.assertEquals(
        "hell yes", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    mImeServiceUnderTest.resetMockCandidateView();

    mImeServiceUnderTest.moveCursorToPosition(2, true);
    verifySuggestions(true);
    Assert.assertEquals(
        "", mImeServiceUnderTest.getCurrentComposedWord().getTypedWord().toString());
    Assert.assertEquals(0, mImeServiceUnderTest.getCurrentComposedWord().cursorPosition());
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());

    mImeServiceUnderTest.simulateKeyPress('r');
    Assert.assertEquals(
        "herll yes", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    verifySuggestions(true, "r");
    Assert.assertEquals(3, getCurrentTestInputConnection().getCurrentStartPosition());
    Assert.assertEquals(
        "r", mImeServiceUnderTest.getCurrentComposedWord().getTypedWord().toString());

    mImeServiceUnderTest.simulateKeyPress('d');
    Assert.assertEquals(
        "herdll yes", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    verifySuggestions(true, "rd");
    Assert.assertEquals(4, getCurrentTestInputConnection().getCurrentStartPosition());
    Assert.assertEquals(
        "rd", mImeServiceUnderTest.getCurrentComposedWord().getTypedWord().toString());
  }

  @Test
  public void testCorrectlyOutputCharactersWhenCongestedCursorUpdates() {
    Assert.assertEquals(0, getCurrentTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress('g');
    Assert.assertEquals("g", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(1, getCurrentTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress('o');
    Assert.assertEquals("go", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());

    getCurrentTestInputConnection().setUpdateSelectionDelay(1000L);
    mImeServiceUnderTest.simulateKeyPress('i');
    Assert.assertEquals("goi", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress('n');
    Assert.assertEquals("goin", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    getCurrentTestInputConnection().executeOnSelectionUpdateEvent();
    Assert.assertEquals("goin", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    getCurrentTestInputConnection().executeOnSelectionUpdateEvent();
    Assert.assertEquals("goin", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateKeyPress('g');
    getCurrentTestInputConnection().setUpdateSelectionDelay(1L);
    TestRxSchedulers.foregroundFlushAllJobs();
    Assert.assertEquals("going", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(5, getCurrentTestInputConnection().getCurrentStartPosition());
  }

  @Test
  @Ignore("Again, not sure what's the issue.")
  public void testCorrectlyOutputCharactersWhenVeryCongestedCursorUpdates() {
    Assert.assertEquals(0, getCurrentTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.simulateTextTyping("go");
    Assert.assertEquals("go", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());

    getCurrentTestInputConnection().setUpdateSelectionDelay(1000L);
    mImeServiceUnderTest.simulateTextTyping("ing to work");
    Assert.assertEquals(
        "going to work", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(
        "going to work".length(), getCurrentTestInputConnection().getCurrentStartPosition());

    getCurrentTestInputConnection().executeOnSelectionUpdateEvent();
    Assert.assertEquals(
        "going to work", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(
        "going to work".length(), getCurrentTestInputConnection().getCurrentStartPosition());

    mImeServiceUnderTest.simulateTextTyping("i");
    Assert.assertEquals(
        "going to worki", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateTextTyping("n");
    Assert.assertEquals(
        "going to workin", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    mImeServiceUnderTest.simulateTextTyping("g");
    Assert.assertEquals(
        "going to working", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(
        "going to working".length(), getCurrentTestInputConnection().getCurrentStartPosition());
    getCurrentTestInputConnection().setUpdateSelectionDelay(1L);
    TestRxSchedulers.foregroundFlushAllJobs();
    Assert.assertEquals(
        "going to working", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(
        "going to working".length(), getCurrentTestInputConnection().getCurrentStartPosition());
  }

  @Test
  public void testCorrectlyOutputCharactersWhenExtremelyCongestedCursorUpdates() {
    Assert.assertEquals(0, getCurrentTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress('g');
    Assert.assertEquals("g", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(1, getCurrentTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress('o');
    Assert.assertEquals("go", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());

    getCurrentTestInputConnection().setUpdateSelectionDelay(1000L);
    mImeServiceUnderTest.simulateKeyPress('i');
    Assert.assertEquals("goi", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(3, getCurrentTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress('n');
    Assert.assertEquals("goin", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(4, getCurrentTestInputConnection().getCurrentStartPosition());
    Assert.assertEquals("goin", mImeServiceUnderTest.getCurrentComposedWord().getTypedWord());
    Assert.assertEquals(4, mImeServiceUnderTest.getCurrentComposedWord().cursorPosition());

    getCurrentTestInputConnection().executeOnSelectionUpdateEvent();
    Assert.assertEquals("goin", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(4, getCurrentTestInputConnection().getCurrentStartPosition());
    Assert.assertEquals("goin", mImeServiceUnderTest.getCurrentComposedWord().getTypedWord());
    Assert.assertEquals(4, mImeServiceUnderTest.getCurrentComposedWord().cursorPosition());
    mImeServiceUnderTest.simulateKeyPress('g');
    getCurrentTestInputConnection().setUpdateSelectionDelay(1L);
    TestRxSchedulers.foregroundFlushAllJobs();
    Assert.assertEquals("going", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(5, getCurrentTestInputConnection().getCurrentStartPosition());
    Assert.assertEquals("going", mImeServiceUnderTest.getCurrentComposedWord().getTypedWord());
    Assert.assertEquals(5, mImeServiceUnderTest.getCurrentComposedWord().cursorPosition());
  }

  @Test
  public void testCorrectlyOutputCharactersWhenDelayedCursorUpdates() {
    Assert.assertEquals(0, getCurrentTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress('g');
    Assert.assertEquals("g", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(1, getCurrentTestInputConnection().getCurrentStartPosition());

    getCurrentTestInputConnection().setUpdateSelectionDelay(1000L);
    mImeServiceUnderTest.simulateKeyPress('o');
    Assert.assertEquals("go", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(2, getCurrentTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress('i');
    Assert.assertEquals("goi", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(3, getCurrentTestInputConnection().getCurrentStartPosition());

    getCurrentTestInputConnection().setUpdateSelectionDelay(1L);
    TestRxSchedulers.foregroundFlushAllJobs();
    mImeServiceUnderTest.simulateKeyPress('n');
    Assert.assertEquals("goin", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(4, getCurrentTestInputConnection().getCurrentStartPosition());
    mImeServiceUnderTest.simulateKeyPress('g');
    Assert.assertEquals("going", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(5, getCurrentTestInputConnection().getCurrentStartPosition());

    getCurrentTestInputConnection().setUpdateSelectionDelay(1000L);
    mImeServiceUnderTest.simulateKeyPress('g');
    Assert.assertEquals(
        "goingg", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(6, getCurrentTestInputConnection().getCurrentStartPosition());
  }

  private void testDelayedOnSelectionUpdate(long delay) {
    final String testText =
        "typing 1 2 3 working hel kjasldkjalskdjasd hel fac ksdjflksd smile fac fac hel hel"
            + " aklsjdas gggggg hello fac hel face hel";
    final String expectedText =
        "typing 1 2 3 working he'll kjasldkjalskdjasd he'll face ksdjflksd smile face face"
            + " he'll he'll aklsjdas gggggg hello face he'll face hel";
    mImeServiceUnderTest.setUpdateSelectionDelay(delay + 1);
    mImeServiceUnderTest.simulateTextTyping(testText);
    // TestRxSchedulers.drainAllTasks();
    // the first two hel are corrected
    Assert.assertEquals(
        expectedText, getCurrentTestInputConnection().getCurrentTextInInputConnection());
    Assert.assertEquals(
        expectedText.length(), getCurrentTestInputConnection().getCurrentStartPosition());
  }

  @Test
  public void testNoDelayedOnSelectionUpdateFastTyping() {
    mImeServiceUnderTest.setDelayBetweenTyping(25);
    testDelayedOnSelectionUpdate(1);
  }

  @Test
  public void testSmallDelayedOnSelectionUpdateFastTyping() {
    mImeServiceUnderTest.setDelayBetweenTyping(25);
    testDelayedOnSelectionUpdate(TestableImeService.DELAY_BETWEEN_TYPING + 3);
  }

  @Test
  public void testSmallDelayedOnSelectionUpdate() {
    testDelayedOnSelectionUpdate(TestableImeService.DELAY_BETWEEN_TYPING);
  }

  @Test
  public void testSmallPlusDelayedOnSelectionUpdate() {
    testDelayedOnSelectionUpdate(TestableImeService.DELAY_BETWEEN_TYPING + 3);
  }

  @Test
  @Ignore("Robolectric scheduler issues. I can't figure how to correctly simulate this.")
  public void testAnnoyingDelayedOnSelectionUpdate() {
    testDelayedOnSelectionUpdate(TestableImeService.DELAY_BETWEEN_TYPING * 3);
  }

  @Test
  @Ignore("Robolectric scheduler issues. I can't figure how to correctly simulate this.")
  public void testCrazyDelayedOnSelectionUpdate() {
    testDelayedOnSelectionUpdate(TestableImeService.DELAY_BETWEEN_TYPING * 6);
  }

  @Test
  @Ignore("Robolectric scheduler issues. I can't figure how to correctly simulate this.")
  public void testOverExpectedDelayedOnSelectionUpdate() {
    testDelayedOnSelectionUpdate(TestableImeService.MAX_TIME_TO_EXPECT_SELECTION_UPDATE + 1);
  }

  @Test
  @Ignore("Robolectric scheduler issues. I can't figure how to correctly simulate this.")
  public void testWayOverExpectedDelayedOnSelectionUpdate() {
    testDelayedOnSelectionUpdate(TestableImeService.MAX_TIME_TO_EXPECT_SELECTION_UPDATE * 2);
  }
}
