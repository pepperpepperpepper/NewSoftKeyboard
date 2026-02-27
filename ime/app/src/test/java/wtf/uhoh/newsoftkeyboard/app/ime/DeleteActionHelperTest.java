package wtf.uhoh.newsoftkeyboard.app.ime;

import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.dictionaries.WordComposer;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class DeleteActionHelperTest {

  @Test
  public void handleDeleteLastCharacter_noComposing_deletesSurroundingText() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    Mockito.when(inputConnection.setComposingText(Mockito.any(), Mockito.anyInt()))
        .thenReturn(false);
    Mockito.when(inputConnection.deleteSurroundingText(Mockito.anyInt(), Mockito.anyInt()))
        .thenReturn(true);

    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);
    router.setComposingText("x", 1); // marks composing as unsupported

    final WordComposer wordComposer = new WordComposer();
    wordComposer.simulateTypedWord("k");

    final FakeHost host = new FakeHost();
    host.predictionOn = true;

    DeleteActionHelper.handleDeleteLastCharacter(host, router, wordComposer, false);

    Assert.assertTrue(host.expectingSelectionUpdateMarked);
    Assert.assertTrue(host.updateSuggestionsPosted);
    Mockito.verify(inputConnection).deleteSurroundingText(1, 0);
    Mockito.verify(inputConnection, Mockito.times(1))
        .setComposingText(Mockito.any(), Mockito.anyInt());
    Assert.assertTrue(wordComposer.isEmpty());
  }

  @Test
  public void handleDeleteLastCharacter_setComposingTextFails_fallsBackToDeleteSurroundingText() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    Mockito.when(inputConnection.setComposingText(Mockito.any(), Mockito.anyInt()))
        .thenReturn(false);
    Mockito.when(inputConnection.deleteSurroundingText(Mockito.anyInt(), Mockito.anyInt()))
        .thenReturn(true);

    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);

    final WordComposer wordComposer = new WordComposer();
    wordComposer.simulateTypedWord("k");

    final FakeHost host = new FakeHost();
    host.predictionOn = true;

    DeleteActionHelper.handleDeleteLastCharacter(host, router, wordComposer, false);

    Assert.assertFalse(router.isComposingTextSupported());
    Assert.assertTrue(host.updateSuggestionsPosted);
    Mockito.verify(inputConnection).setComposingText(Mockito.any(), Mockito.anyInt());
    Mockito.verify(inputConnection).deleteSurroundingText(1, 0);
  }

  @Test
  public void handleForwardDelete_setComposingTextFails_fallsBackToDeleteSurroundingText() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    Mockito.when(inputConnection.setComposingText(Mockito.any(), Mockito.anyInt()))
        .thenReturn(false);
    Mockito.when(inputConnection.deleteSurroundingText(Mockito.anyInt(), Mockito.anyInt()))
        .thenReturn(true);

    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);

    final WordComposer wordComposer = new WordComposer();
    wordComposer.simulateTypedWord("ab");
    wordComposer.setCursorPosition(0);

    final FakeHost host = new FakeHost();
    host.predictionOn = true;

    DeleteActionHelper.handleForwardDelete(host, router, wordComposer);

    Assert.assertFalse(router.isComposingTextSupported());
    Assert.assertTrue(host.updateSuggestionsPosted);
    Mockito.verify(inputConnection).setComposingText(Mockito.any(), Mockito.anyInt());
    Mockito.verify(inputConnection).deleteSurroundingText(0, 1);
  }

  private static final class FakeHost implements DeleteActionHelper.Host {
    private boolean predictionOn = false;
    private boolean selectionUpdateDelayed = false;
    private boolean expectingSelectionUpdateMarked = false;
    private boolean updateSuggestionsPosted = false;
    private int lastDownUpKeyCode = 0;

    @Override
    public boolean isPredictionOn() {
      return predictionOn;
    }

    @Override
    public int getCursorPosition() {
      return 0;
    }

    @Override
    public boolean isSelectionUpdateDelayed() {
      return selectionUpdateDelayed;
    }

    @Override
    public void markExpectingSelectionUpdate() {
      expectingSelectionUpdateMarked = true;
    }

    @Override
    public void postUpdateSuggestions() {
      updateSuggestionsPosted = true;
    }

    @Override
    public void sendDownUpKeyEvents(int keyCode) {
      lastDownUpKeyCode = keyCode;
    }

    public boolean sentDeleteKeyEvent() {
      return lastDownUpKeyCode == KeyEvent.KEYCODE_DEL;
    }
  }
}
