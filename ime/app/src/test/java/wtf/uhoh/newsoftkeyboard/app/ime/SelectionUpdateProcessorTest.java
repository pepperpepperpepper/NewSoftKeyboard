package wtf.uhoh.newsoftkeyboard.app.ime;

import android.os.SystemClock;
import android.view.inputmethod.InputConnection;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordComposer;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class SelectionUpdateProcessorTest {

  @Test
  public void testExpectedSelectionUpdateDoesNotRestartWordSuggestion() {
    final SelectionUpdateProcessor processor = new SelectionUpdateProcessor();
    final FakeHost host = new FakeHost();
    host.predictionOn = true;
    host.currentlyPredicting = false;
    host.selectionUpdateDelayed = true;
    host.expectingSelectionUpdateBy = SystemClock.uptimeMillis() + 5000;

    processor.onUpdateSelection(0, 0, 1, 1, 0, 0, host);
    Assert.assertEquals(1, host.markSelectionUpdateReceivedCalls);
    Assert.assertEquals(0, host.clearExpectingCalls);
    Assert.assertEquals(0, host.postRestartWordSuggestionCalls);
    Assert.assertEquals(0, host.abortCorrectionCalls);
    Assert.assertEquals(0, host.unexpectedCursorMoveWhileNotPredictingCalls);

    // Once we've received the expected selection update, further cursor updates should not be
    // discarded. This allows us to react to genuine cursor moves or external edits quickly.
    processor.onUpdateSelection(1, 1, 2, 2, 0, 0, host);
    Assert.assertEquals(1, host.markSelectionUpdateReceivedCalls);
    Assert.assertEquals(1, host.clearExpectingCalls);
    Assert.assertEquals(1, host.postRestartWordSuggestionCalls);
    Assert.assertEquals(1, host.unexpectedCursorMoveWhileNotPredictingCalls);
  }

  @Test
  public void testUnexpectedSelectionUpdateClearsExpectationAndMayRestart() {
    final SelectionUpdateProcessor processor = new SelectionUpdateProcessor();
    final FakeHost host = new FakeHost();
    host.predictionOn = true;
    host.currentlyPredicting = false;
    host.expectingSelectionUpdateBy = SystemClock.uptimeMillis() - 1;

    processor.onUpdateSelection(0, 0, 1, 1, 0, 0, host);
    Assert.assertEquals(1, host.clearExpectingCalls);
    Assert.assertEquals(1, host.postRestartWordSuggestionCalls);
    Assert.assertEquals(1, host.unexpectedCursorMoveWhileNotPredictingCalls);
  }

  private static final class FakeHost implements SelectionUpdateProcessor.Host {
    boolean predictionOn;
    boolean currentlyPredicting;
    boolean selectionUpdateDelayed;
    long expectingSelectionUpdateBy;

    int abortCorrectionCalls;
    int postRestartWordSuggestionCalls;
    int clearExpectingCalls;
    int markSelectionUpdateReceivedCalls;
    int unexpectedCursorMoveWhileNotPredictingCalls;

    final InputConnectionRouter router =
        new InputConnectionRouter(() -> Mockito.mock(InputConnection.class));

    final WordComposer wordComposer = new WordComposer();

    @Override
    public boolean isPredictionOn() {
      return predictionOn;
    }

    @Override
    public boolean isCurrentlyPredicting() {
      return currentlyPredicting;
    }

    @Override
    public void onUnexpectedCursorMoveWhileNotPredicting() {
      unexpectedCursorMoveWhileNotPredictingCalls++;
    }

    @Override
    public boolean isSelectionUpdateDelayed() {
      return selectionUpdateDelayed;
    }

    @Override
    public InputConnectionRouter inputConnectionRouter() {
      return router;
    }

    @Override
    public void abortCorrectionAndResetPredictionState(boolean force) {
      abortCorrectionCalls++;
    }

    @Override
    public void postRestartWordSuggestion() {
      postRestartWordSuggestionCalls++;
    }

    @Override
    public boolean shouldRevertOnDelete() {
      return false;
    }

    @Override
    public void setWordRevertLength(int length) {}

    @Override
    public int getWordRevertLength() {
      return 0;
    }

    @Override
    public void resetLastSpaceTimeStamp() {}

    @Override
    public long getExpectingSelectionUpdateBy() {
      return expectingSelectionUpdateBy;
    }

    @Override
    public void clearExpectingSelectionUpdate() {
      clearExpectingCalls++;
      expectingSelectionUpdateBy = -1;
      selectionUpdateDelayed = false;
    }

    @Override
    public void markSelectionUpdateReceived() {
      markSelectionUpdateReceivedCalls++;
      selectionUpdateDelayed = false;
    }

    @Override
    public void setExpectingSelectionUpdateBy(long value) {
      expectingSelectionUpdateBy = value;
      selectionUpdateDelayed = true;
    }

    @Override
    public int getCandidateStartPositionDangerous() {
      return 0;
    }

    @Override
    public int getCandidateEndPositionDangerous() {
      return 0;
    }

    @Override
    public WordComposer getCurrentWord() {
      return wordComposer;
    }

    @Override
    public String logTag() {
      return "SelectionUpdateProcessorTest";
    }
  }
}
