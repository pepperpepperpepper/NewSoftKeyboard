package wtf.uhoh.newsoftkeyboard.app.ime;

import android.view.inputmethod.InputConnection;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordComposer;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class WordRevertHandlerTest {

  @Test
  public void revertLastWord_noRevertLength_sendsDeleteKeyEvent() {
    final AutoCorrectState autoCorrectState = new AutoCorrectState();
    autoCorrectState.wordRevertLength = 0;
    final PredictionState predictionState = new PredictionState();
    predictionState.autoCorrectOn = true;

    final WordComposer currentWord = new WordComposer();
    currentWord.simulateTypedWord("hello");
    final WordComposer previousWord = new WordComposer();
    previousWord.simulateTypedWord("world");

    final FakeHost host = new FakeHost(new InputConnectionRouter(() -> null), 0);
    final WordRevertHandler handler = new WordRevertHandler();

    final WordRevertHandler.Result result =
        handler.revertLastWord(autoCorrectState, predictionState, currentWord, previousWord, host);

    Assert.assertSame(currentWord, result.currentWord());
    Assert.assertSame(previousWord, result.previousWord());
    Assert.assertEquals(1, host.deleteKeyEventsSent);
    Assert.assertEquals(0, host.markExpectedSelectionCalls);
    Assert.assertFalse(host.clearedSpaceTimeTracker);
  }

  @Test
  public void revertLastWord_withComposing_replacesCorrectedWordAndClearsSpaceTimeTracker() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    Mockito.when(inputConnection.setComposingRegion(Mockito.anyInt(), Mockito.anyInt()))
        .thenReturn(true);
    Mockito.when(inputConnection.setComposingText(Mockito.any(), Mockito.anyInt()))
        .thenReturn(true);
    Mockito.when(inputConnection.getTextBeforeCursor(Mockito.eq(6), Mockito.eq(0)))
        .thenReturn("audren");
    Mockito.when(inputConnection.setSelection(Mockito.anyInt(), Mockito.anyInt())).thenReturn(true);
    Mockito.when(inputConnection.commitText(Mockito.any(), Mockito.anyInt())).thenReturn(true);

    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);
    router.resetComposingTextSupport();

    final AutoCorrectState autoCorrectState = new AutoCorrectState();
    autoCorrectState.wordRevertLength = 7; // "sudden " length
    final PredictionState predictionState = new PredictionState();
    predictionState.autoCorrectOn = true;

    final WordComposer currentWord = new WordComposer();
    final WordComposer previousWord = new WordComposer();
    previousWord.simulateTypedWord("audren");

    final FakeHost host = new FakeHost(router, /* cursorPosition= */ 17);
    final WordRevertHandler handler = new WordRevertHandler();

    final WordRevertHandler.Result result =
        handler.revertLastWord(autoCorrectState, predictionState, currentWord, previousWord, host);

    Assert.assertFalse(predictionState.autoCorrectOn);
    Assert.assertEquals(0, autoCorrectState.wordRevertLength);
    Assert.assertSame(previousWord, result.currentWord());
    Assert.assertSame(currentWord, result.previousWord());
    Assert.assertEquals(1, host.markExpectedSelectionCalls);
    Assert.assertTrue(host.clearedSpaceTimeTracker);
    Assert.assertEquals(1, host.updateSuggestionsCalls);

    Mockito.verify(inputConnection).setComposingRegion(10, 17);
    Mockito.verify(inputConnection).setComposingText("audren", 1);
    Mockito.verify(inputConnection).setSelection(16, 16);
    Mockito.verify(inputConnection, Mockito.never())
        .deleteSurroundingText(Mockito.anyInt(), Mockito.anyInt());
    Mockito.verify(inputConnection, Mockito.never())
        .commitText(Mockito.eq("audren"), Mockito.anyInt());
  }

  @Test
  public void revertLastWord_withoutComposing_deletesAndCommitsTypedWord() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    Mockito.when(inputConnection.setComposingRegion(Mockito.anyInt(), Mockito.anyInt()))
        .thenReturn(true);
    Mockito.when(inputConnection.setComposingText(Mockito.any(), Mockito.anyInt()))
        .thenReturn(false);
    Mockito.when(inputConnection.deleteSurroundingText(Mockito.anyInt(), Mockito.anyInt()))
        .thenReturn(true);
    Mockito.when(inputConnection.commitText(Mockito.any(), Mockito.anyInt())).thenReturn(true);
    Mockito.when(inputConnection.setSelection(Mockito.anyInt(), Mockito.anyInt())).thenReturn(true);

    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);
    router.resetComposingTextSupport();

    final AutoCorrectState autoCorrectState = new AutoCorrectState();
    autoCorrectState.wordRevertLength = 7; // "sudden " length
    final PredictionState predictionState = new PredictionState();
    predictionState.autoCorrectOn = true;

    final WordComposer currentWord = new WordComposer();
    final WordComposer previousWord = new WordComposer();
    previousWord.simulateTypedWord("audren");

    final FakeHost host = new FakeHost(router, /* cursorPosition= */ 17);
    final WordRevertHandler handler = new WordRevertHandler();

    handler.revertLastWord(autoCorrectState, predictionState, currentWord, previousWord, host);

    Mockito.verify(inputConnection).deleteSurroundingText(7, 0);
    Mockito.verify(inputConnection).commitText("audren", 1);
    Mockito.verify(inputConnection).setSelection(16, 16);
  }

  private static final class FakeHost implements WordRevertHandler.Host {
    private final InputConnectionRouter router;
    private final int cursorPosition;

    private int deleteKeyEventsSent = 0;
    private int markExpectedSelectionCalls = 0;
    private int updateSuggestionsCalls = 0;
    private boolean clearedSpaceTimeTracker = false;

    private FakeHost(InputConnectionRouter router, int cursorPosition) {
      this.router = router;
      this.cursorPosition = cursorPosition;
    }

    @Override
    public InputConnectionRouter inputConnectionRouter() {
      return router;
    }

    @Override
    public int getCursorPosition() {
      return cursorPosition;
    }

    @Override
    public void markExpectingSelectionUpdate() {
      markExpectedSelectionCalls++;
    }

    @Override
    public void sendDownUpKeyEvents(int keyCode) {
      deleteKeyEventsSent++;
    }

    @Override
    public void performUpdateSuggestions() {
      updateSuggestionsCalls++;
    }

    @Override
    public void clearSpaceTimeTracker() {
      clearedSpaceTimeTracker = true;
    }

    @Override
    public void removeFromUserDictionary(String word) {
      // no-op
    }
  }
}
