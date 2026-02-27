package wtf.uhoh.newsoftkeyboard.app.ime;

import android.view.inputmethod.InputConnection;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class SuggestionCommitterTest {

  @Test
  public void commitWordToInput_noComposing_noopWhenSameWord() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    Mockito.when(inputConnection.setComposingText(Mockito.any(), Mockito.anyInt()))
        .thenReturn(false);
    Mockito.when(inputConnection.beginBatchEdit()).thenReturn(true);
    Mockito.when(inputConnection.endBatchEdit()).thenReturn(true);
    Mockito.when(inputConnection.commitText(Mockito.any(), Mockito.anyInt())).thenReturn(true);
    Mockito.when(inputConnection.deleteSurroundingText(Mockito.anyInt(), Mockito.anyInt()))
        .thenReturn(true);

    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);
    router.setComposingText("x", 1); // marks composing as unsupported

    final FakeHost host = new FakeHost(router);
    final SuggestionCommitter committer = new SuggestionCommitter(host);

    committer.commitWordToInput("keep", "keep");

    Assert.assertTrue(host.clearedSuggestions);
    Mockito.verify(inputConnection, Mockito.never()).commitText(Mockito.any(), Mockito.anyInt());
    Mockito.verify(inputConnection, Mockito.never())
        .deleteSurroundingText(Mockito.anyInt(), Mockito.anyInt());
  }

  @Test
  public void commitWordToInput_noComposing_replacesTypedWord() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    Mockito.when(inputConnection.setComposingText(Mockito.any(), Mockito.anyInt()))
        .thenReturn(false);
    Mockito.when(inputConnection.beginBatchEdit()).thenReturn(true);
    Mockito.when(inputConnection.endBatchEdit()).thenReturn(true);
    Mockito.when(inputConnection.commitText(Mockito.any(), Mockito.anyInt())).thenReturn(true);
    Mockito.when(inputConnection.deleteSurroundingText(Mockito.anyInt(), Mockito.anyInt()))
        .thenReturn(true);

    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);
    router.setComposingText("x", 1); // marks composing as unsupported

    final FakeHost host = new FakeHost(router);
    final SuggestionCommitter committer = new SuggestionCommitter(host);

    committer.commitWordToInput("keep", "kep");

    Assert.assertTrue(host.clearedSuggestions);
    Mockito.verify(inputConnection).deleteSurroundingText(3, 0);
    Mockito.verify(inputConnection).commitText("keep", 1);
  }

  @Test
  public void commitManuallyPickedWordToInput_noComposing_replacesTypedPrefix() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    Mockito.when(inputConnection.setComposingText(Mockito.any(), Mockito.anyInt()))
        .thenReturn(false);
    Mockito.when(inputConnection.beginBatchEdit()).thenReturn(true);
    Mockito.when(inputConnection.endBatchEdit()).thenReturn(true);
    Mockito.when(inputConnection.commitText(Mockito.any(), Mockito.anyInt())).thenReturn(true);
    Mockito.when(inputConnection.deleteSurroundingText(Mockito.anyInt(), Mockito.anyInt()))
        .thenReturn(true);

    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);
    router.setComposingText("x", 1); // marks composing as unsupported

    final FakeHost host = new FakeHost(router);
    final SuggestionCommitter committer = new SuggestionCommitter(host);

    committer.commitManuallyPickedWordToInput("keep", "k");

    Assert.assertTrue(host.clearedSuggestions);
    Mockito.verify(inputConnection).deleteSurroundingText(1, 0);
    Mockito.verify(inputConnection).commitText("keep", 1);
  }

  @Test
  public void commitManuallyPickedWordToInput_withComposing_commitsWithoutDelete() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    Mockito.when(inputConnection.setComposingText(Mockito.any(), Mockito.anyInt()))
        .thenReturn(true);
    Mockito.when(inputConnection.beginBatchEdit()).thenReturn(true);
    Mockito.when(inputConnection.endBatchEdit()).thenReturn(true);
    Mockito.when(inputConnection.commitText(Mockito.any(), Mockito.anyInt())).thenReturn(true);
    Mockito.when(inputConnection.deleteSurroundingText(Mockito.anyInt(), Mockito.anyInt()))
        .thenReturn(true);

    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);
    router.resetComposingTextSupport();

    final FakeHost host = new FakeHost(router);
    final SuggestionCommitter committer = new SuggestionCommitter(host);

    committer.commitManuallyPickedWordToInput("keep", "k");

    Assert.assertTrue(host.clearedSuggestions);
    Mockito.verify(inputConnection, Mockito.never())
        .deleteSurroundingText(Mockito.anyInt(), Mockito.anyInt());
    Mockito.verify(inputConnection).commitText("keep", 1);
  }

  @Test
  public void
      commitManuallyPickedWordToInput_insertsLeadingSpaceWhenPickingNextWordWithoutSeparator() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    Mockito.when(inputConnection.setComposingText(Mockito.any(), Mockito.anyInt()))
        .thenReturn(true);
    Mockito.when(inputConnection.beginBatchEdit()).thenReturn(true);
    Mockito.when(inputConnection.endBatchEdit()).thenReturn(true);
    Mockito.when(inputConnection.commitText(Mockito.any(), Mockito.anyInt())).thenReturn(true);
    Mockito.when(inputConnection.getTextBeforeCursor(Mockito.eq(1), Mockito.eq(0))).thenReturn("e");

    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);
    router.resetComposingTextSupport();

    final FakeHost host = new FakeHost(router);
    final SuggestionCommitter committer = new SuggestionCommitter(host);

    committer.commitManuallyPickedWordToInput("me", "");

    Assert.assertTrue(host.clearedSuggestions);
    final InOrder inOrder = Mockito.inOrder(inputConnection);
    inOrder.verify(inputConnection).getTextBeforeCursor(1, 0);
    inOrder.verify(inputConnection).commitText(" ", 1);
    inOrder.verify(inputConnection).commitText("me", 1);
  }

  @Test
  public void commitManuallyPickedWordToInput_doesNotInsertLeadingSpaceWhenAlreadySeparated() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    Mockito.when(inputConnection.setComposingText(Mockito.any(), Mockito.anyInt()))
        .thenReturn(true);
    Mockito.when(inputConnection.beginBatchEdit()).thenReturn(true);
    Mockito.when(inputConnection.endBatchEdit()).thenReturn(true);
    Mockito.when(inputConnection.commitText(Mockito.any(), Mockito.anyInt())).thenReturn(true);
    Mockito.when(inputConnection.getTextBeforeCursor(Mockito.eq(1), Mockito.eq(0))).thenReturn(" ");

    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);
    router.resetComposingTextSupport();

    final FakeHost host = new FakeHost(router);
    final SuggestionCommitter committer = new SuggestionCommitter(host);

    committer.commitManuallyPickedWordToInput("me", "");

    Assert.assertTrue(host.clearedSuggestions);
    Mockito.verify(inputConnection, Mockito.never()).commitText(" ", 1);
    Mockito.verify(inputConnection).commitText("me", 1);
  }

  private static final class FakeHost implements SuggestionCommitter.Host {
    private final InputConnectionRouter router;
    private boolean clearedSuggestions = false;

    private FakeHost(InputConnectionRouter router) {
      this.router = router;
    }

    @Override
    public InputConnectionRouter inputConnectionRouter() {
      return router;
    }

    @Override
    public boolean isSelectionUpdateDelayed() {
      return false;
    }

    @Override
    public void markExpectingSelectionUpdate() {
      // no-op
    }

    @Override
    public int getCursorPosition() {
      return 0;
    }

    @Override
    public void clearSuggestions() {
      clearedSuggestions = true;
    }
  }
}
