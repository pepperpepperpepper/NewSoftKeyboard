package wtf.uhoh.newsoftkeyboard.app.ime;

import android.view.inputmethod.InputConnection;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class InputConnectionRouterTest {

  @Test
  public void forceComposingUnsupported_pinsUnsupportedForSession() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);

    Assert.assertTrue(router.isComposingTextSupported());

    router.forceComposingUnsupported();

    Assert.assertFalse(router.isComposingTextSupported());
  }

  @Test
  public void forceComposingUnsupported_persistsAcrossRevalidationRequests() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);

    router.forceComposingUnsupported();
    router.requestComposingTextRevalidation();

    Assert.assertFalse(router.isComposingTextSupported());
  }

  @Test
  public void resetComposingTextSupport_clearsForcedUnsupportedFlag() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);

    router.forceComposingUnsupported();
    router.resetComposingTextSupport();

    Assert.assertTrue(router.isComposingTextSupported());
  }

  @Test
  public void setComposingRegion_isSkippedWhenComposingUnsupported() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);
    router.forceComposingUnsupported();

    final boolean result = router.setComposingRegion(0, 2);

    Assert.assertFalse(result);
    Mockito.verify(inputConnection, Mockito.never()).setComposingRegion(Mockito.anyInt(), Mockito.anyInt());
  }

  @Test
  public void finishComposingText_isSkippedWhenComposingUnsupported() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);
    router.forceComposingUnsupported();

    final boolean result = router.finishComposingText();

    Assert.assertFalse(result);
    Mockito.verify(inputConnection, Mockito.never()).finishComposingText();
  }

  @Test
  public void setComposingRegion_passesThroughWhenComposingSupported() {
    final InputConnection inputConnection = Mockito.mock(InputConnection.class);
    Mockito.when(inputConnection.setComposingRegion(0, 2)).thenReturn(true);
    final InputConnectionRouter router = new InputConnectionRouter(() -> inputConnection);

    final boolean result = router.setComposingRegion(0, 2);

    Assert.assertTrue(result);
    Mockito.verify(inputConnection).setComposingRegion(0, 2);
  }
}
