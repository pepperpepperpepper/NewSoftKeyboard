package wtf.uhoh.newsoftkeyboard.app.ime;

import android.view.inputmethod.InputConnection;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateView;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordComposer;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class CharacterInputHandlerTest {

  @Test
  public void predictionOn_setComposingTextFails_fallsBackToSendKeyChar() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    Mockito.when(inputConnection.setComposingText(Mockito.any(), Mockito.anyInt()))
        .thenReturn(false);
    Mockito.when(inputConnection.beginBatchEdit()).thenReturn(true);
    Mockito.when(inputConnection.endBatchEdit()).thenReturn(true);

    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);
    final FakeHost host = new FakeHost(router);
    host.predictionOn = true;

    new CharacterInputHandler()
        .handleCharacter(
            (int) 'k', Mockito.mock(Keyboard.Key.class), 0, new int[] {'k'}, "test", host);

    Assert.assertEquals("k", host.committedText.toString());
    Assert.assertFalse(router.isComposingTextSupported());
    Mockito.verify(inputConnection).setComposingText(Mockito.any(), Mockito.anyInt());
  }

  @Test
  public void predictionOn_setComposingTextSucceeds_doesNotSendKeyChar() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    Mockito.when(inputConnection.setComposingText(Mockito.any(), Mockito.anyInt()))
        .thenReturn(true);
    Mockito.when(inputConnection.getTextBeforeCursor(Mockito.anyInt(), Mockito.anyInt()))
        .thenReturn("k");
    Mockito.when(inputConnection.beginBatchEdit()).thenReturn(true);
    Mockito.when(inputConnection.endBatchEdit()).thenReturn(true);

    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);
    final FakeHost host = new FakeHost(router);
    host.predictionOn = true;

    new CharacterInputHandler()
        .handleCharacter(
            (int) 'k', Mockito.mock(Keyboard.Key.class), 0, new int[] {'k'}, "test", host);

    Assert.assertEquals("", host.committedText.toString());
    Assert.assertTrue(router.isComposingTextSupported());
    Mockito.verify(inputConnection).setComposingText(Mockito.any(), Mockito.anyInt());
  }

  @Test
  public void
      predictionOn_setComposingTextReturnsTrueButGetTextBeforeCursorIsNull_fallsBackToSendKeyChar() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    Mockito.when(inputConnection.setComposingText(Mockito.any(), Mockito.anyInt()))
        .thenReturn(true);
    Mockito.when(inputConnection.getTextBeforeCursor(Mockito.anyInt(), Mockito.anyInt()))
        .thenReturn(null);
    Mockito.when(inputConnection.beginBatchEdit()).thenReturn(true);
    Mockito.when(inputConnection.endBatchEdit()).thenReturn(true);

    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);
    final FakeHost host = new FakeHost(router);
    host.predictionOn = true;

    new CharacterInputHandler()
        .handleCharacter(
            (int) 'k', Mockito.mock(Keyboard.Key.class), 0, new int[] {'k'}, "test", host);

    Assert.assertEquals("k", host.committedText.toString());
    Assert.assertFalse(router.isComposingTextSupported());
  }

  @Test
  public void predictionOn_setComposingTextReturnsTrueButTextNotVisible_fallsBackToSendKeyChar() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    Mockito.when(inputConnection.setComposingText(Mockito.any(), Mockito.anyInt()))
        .thenReturn(true);
    Mockito.when(inputConnection.getTextBeforeCursor(Mockito.anyInt(), Mockito.anyInt()))
        .thenReturn("");
    Mockito.when(inputConnection.beginBatchEdit()).thenReturn(true);
    Mockito.when(inputConnection.endBatchEdit()).thenReturn(true);

    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);
    final FakeHost host = new FakeHost(router);
    host.predictionOn = true;

    new CharacterInputHandler()
        .handleCharacter(
            (int) 'k', Mockito.mock(Keyboard.Key.class), 0, new int[] {'k'}, "test", host);

    Assert.assertEquals("k", host.committedText.toString());
    Assert.assertFalse(router.isComposingTextSupported());
  }

  @Test
  public void terminalMode_predictionOn_neverCallsSetComposingText() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);
    router.forceComposingUnsupported();

    final FakeHost host = new FakeHost(router);
    host.predictionOn = true;

    new CharacterInputHandler()
        .handleCharacter(
            (int) 'o', Mockito.mock(Keyboard.Key.class), 0, new int[] {'o'}, "test", host);

    Assert.assertEquals("o", host.committedText.toString());
    Assert.assertEquals("o", host.word().getTypedWord().toString());
    Mockito.verify(inputConnection, Mockito.never())
        .setComposingText(Mockito.any(), Mockito.anyInt());
  }

  @Test
  public void terminalMode_predictionOn_keepsTrackingTypedWordForSuggestions() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);
    router.forceComposingUnsupported();

    final FakeHost host = new FakeHost(router);
    host.predictionOn = true;

    final CharacterInputHandler handler = new CharacterInputHandler();
    handler.handleCharacter(
        (int) 't', Mockito.mock(Keyboard.Key.class), 0, new int[] {'t'}, "test", host);
    handler.handleCharacter(
        (int) 'e', Mockito.mock(Keyboard.Key.class), 0, new int[] {'e'}, "test", host);
    handler.handleCharacter(
        (int) 's', Mockito.mock(Keyboard.Key.class), 0, new int[] {'s'}, "test", host);
    handler.handleCharacter(
        (int) 't', Mockito.mock(Keyboard.Key.class), 0, new int[] {'t'}, "test", host);

    Assert.assertEquals("test", host.committedText.toString());
    Assert.assertEquals("test", host.word().getTypedWord().toString());
    Mockito.verify(inputConnection, Mockito.never())
        .setComposingText(Mockito.any(), Mockito.anyInt());
  }

  private static final class FakeHost implements CharacterInputHandler.Host {
    private final WordComposer wordComposer = new WordComposer();
    private final AutoCorrectState autoCorrectState = new AutoCorrectState();
    private final PredictionState predictionState = new PredictionState();
    private final StringBuilder committedText = new StringBuilder();
    private final InputConnectionRouter router;

    private boolean predictionOn = true;

    private FakeHost(InputConnectionRouter router) {
      this.router = router;
    }

    @Override
    public WordComposer word() {
      return wordComposer;
    }

    @Override
    public AutoCorrectState autoCorrectState() {
      return autoCorrectState;
    }

    @Override
    public PredictionState predictionState() {
      return predictionState;
    }

    @Override
    public boolean isPredictionOn() {
      return predictionOn;
    }

    @Override
    public boolean isSuggestionAffectingCharacter(int code) {
      return false;
    }

    @Override
    public boolean isAlphabet(int code) {
      return true;
    }

    @Override
    public boolean isShiftActive() {
      return false;
    }

    @Override
    public int getCursorPosition() {
      return 0;
    }

    @Override
    public void postUpdateSuggestions() {
      // no-op
    }

    @Override
    public void clearSuggestions() {
      // no-op
    }

    @Nullable
    @Override
    public CandidateView candidateView() {
      return null;
    }

    @NonNull
    @Override
    public InputConnectionRouter inputConnectionRouter() {
      return router;
    }

    @Override
    public void markExpectingSelectionUpdate() {
      // no-op
    }

    @Override
    public void sendKeyChar(char c) {
      committedText.append(c);
    }

    @Override
    public void setLastCharacterWasShifted(boolean shifted) {
      // no-op
    }
  }
}
