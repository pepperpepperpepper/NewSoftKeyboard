package wtf.uhoh.newsoftkeyboard.app.ime;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static wtf.uhoh.newsoftkeyboard.app.testing.TestableImeService.createEditorInfo;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.os.Build;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.anysoftkeyboard.api.KeyCodes;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSystemClock;
import org.robolectric.shadows.ShadowToast;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;
import wtf.uhoh.newsoftkeyboard.testing.GeneralDialogTestUtil;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceClipboardTest extends ImeServiceBaseTest {

  private ClipboardManager mClipboardManager;

  @Before
  public void setUpClipboard() {
    mClipboardManager =
        (ClipboardManager) getApplicationContext().getSystemService(Service.CLIPBOARD_SERVICE);
  }

  @Test
  public void testSelectsAllText() {
    final String expectedText = "testing something very long";
    mImeServiceUnderTest.simulateTextTyping(expectedText);

    Assert.assertEquals("", mImeServiceUnderTest.getCurrentSelectedText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_SELECT_ALL);
    Assert.assertEquals(expectedText, mImeServiceUnderTest.getCurrentSelectedText());
  }

  @Test
  public void testClipboardCopy() {
    final String expectedText = "testing something very long";
    mImeServiceUnderTest.simulateTextTyping(expectedText);
    mImeServiceUnderTest.setSelectedText("testing ".length(), "testing something".length(), true);
    Assert.assertEquals("something", mImeServiceUnderTest.getCurrentSelectedText());

    Assert.assertNull(mClipboardManager.getPrimaryClip());

    Assert.assertEquals(expectedText, mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);

    // text stays the same
    Assert.assertEquals(expectedText, mImeServiceUnderTest.getCurrentInputConnectionText());
    // and clipboard has the copied text
    Assert.assertEquals(1, mClipboardManager.getPrimaryClip().getItemCount());
    Assert.assertEquals(
        "something", mClipboardManager.getPrimaryClip().getItemAt(0).getText().toString());
  }

  @Test
  public void testClipboardCut() {
    final String originalText = "testing something very long";
    final String textToCut = "something";
    final String expectedText = "testing  very long";
    mImeServiceUnderTest.simulateTextTyping(originalText);
    mImeServiceUnderTest.setSelectedText("testing ".length(), "testing something".length(), true);
    Assert.assertEquals(textToCut, mImeServiceUnderTest.getCurrentSelectedText());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_CUT);

    // text without "something"
    Assert.assertEquals(expectedText, mImeServiceUnderTest.getCurrentInputConnectionText());
    // and clipboard has the copied text
    Assert.assertEquals(1, mClipboardManager.getPrimaryClip().getItemCount());
    Assert.assertEquals(
        "something", mClipboardManager.getPrimaryClip().getItemAt(0).getText().toString());
  }

  @Test
  public void testClipboardPaste() {
    final String expectedText = "some text";
    mClipboardManager.setPrimaryClip(
        new ClipData("ask", new String[] {"text"}, new ClipData.Item(expectedText)));

    Assert.assertEquals("", mImeServiceUnderTest.getCurrentSelectedText());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE);
    Assert.assertEquals(expectedText, mImeServiceUnderTest.getCurrentInputConnectionText());
    // and backspace DOES NOT deletes the pasted text
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals(
        expectedText.substring(0, expectedText.length() - 1),
        mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testClipboardPasteWhenEmptyClipboard() {
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE);
    Assert.assertEquals("", mImeServiceUnderTest.getCurrentInputConnectionText());
    Assert.assertEquals(
        mImeServiceUnderTest.getText(R.string.clipboard_is_empty_toast),
        ShadowToast.getTextOfLatestToast());
  }

  @Test
  public void testSelectionExpending_AtEndOfInput() {
    mImeServiceUnderTest.simulateTextTyping("some text in the input connection");

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_SELECT);
    Assert.assertEquals("", mImeServiceUnderTest.getCurrentSelectedText());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ARROW_LEFT);
    Assert.assertEquals("n", mImeServiceUnderTest.getCurrentSelectedText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ARROW_LEFT);
    Assert.assertEquals("on", mImeServiceUnderTest.getCurrentSelectedText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ARROW_RIGHT);
    Assert.assertEquals("on", mImeServiceUnderTest.getCurrentSelectedText());
  }

  @Test
  public void testSelectionExpending_AtMiddleOfInput() {
    mImeServiceUnderTest.simulateTextTyping("some text in the input connection");
    mImeServiceUnderTest.moveCursorToPosition("some ".length(), true);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_SELECT);
    Assert.assertEquals("", mImeServiceUnderTest.getCurrentSelectedText());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ARROW_RIGHT);
    Assert.assertEquals("t", mImeServiceUnderTest.getCurrentSelectedText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ARROW_RIGHT);
    Assert.assertEquals("te", mImeServiceUnderTest.getCurrentSelectedText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ARROW_LEFT);
    Assert.assertEquals(" te", mImeServiceUnderTest.getCurrentSelectedText());
  }

  @Test
  public void testSelectionExpendingCancel() {
    mImeServiceUnderTest.simulateTextTyping("some text he the input connection");
    mImeServiceUnderTest.moveCursorToPosition("some ".length(), true);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_SELECT);
    Assert.assertEquals("", mImeServiceUnderTest.getCurrentSelectedText());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ARROW_RIGHT);
    Assert.assertEquals("t", mImeServiceUnderTest.getCurrentSelectedText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ARROW_RIGHT);
    Assert.assertEquals("te", mImeServiceUnderTest.getCurrentSelectedText());

    mImeServiceUnderTest.simulateKeyPress('k');
    // selection ('te') was replaced with the letter 'k'
    Assert.assertEquals("", mImeServiceUnderTest.getCurrentSelectedText());
    Assert.assertEquals(
        "some kxt he the input connection", mImeServiceUnderTest.getCurrentInputConnectionText());
    Assert.assertEquals(
        "some k".length(),
        mImeServiceUnderTest.getCurrentTestInputConnection().getCurrentStartPosition());
    // and we are no longer is select state
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ARROW_RIGHT);
    Assert.assertEquals("", mImeServiceUnderTest.getCurrentSelectedText());
  }

  @Test
  public void testSelectionExpendingWithAlreadySelectedText() {
    mImeServiceUnderTest.simulateTextTyping("some text he the input connection");
    mImeServiceUnderTest.setSelectedText("some ".length(), "some text".length(), true);
    // we already have selection set
    Assert.assertEquals("text", mImeServiceUnderTest.getCurrentSelectedText());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_SELECT);
    Assert.assertEquals("text", mImeServiceUnderTest.getCurrentSelectedText());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ARROW_RIGHT);
    Assert.assertEquals("text ", mImeServiceUnderTest.getCurrentSelectedText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ARROW_RIGHT);
    Assert.assertEquals("text h", mImeServiceUnderTest.getCurrentSelectedText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.ARROW_LEFT);
    Assert.assertEquals(" text h", mImeServiceUnderTest.getCurrentSelectedText());
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.TIRAMISU)
  @Ignore("Failing to attach window in setup")
  public void testDoesNotShowToastInAndroid33() {
    final String expectedText = "testing something very long";
    mImeServiceUnderTest.simulateTextTyping(expectedText);
    mImeServiceUnderTest.setSelectedText("testing ".length(), "testing something".length(), true);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);
    Assert.assertEquals(0, ShadowToast.shownToastCount());
  }

  @Test
  public void testClipboardFineSelectToast() {
    final String expectedText = "testing something very long";
    mImeServiceUnderTest.simulateTextTyping(expectedText);
    mImeServiceUnderTest.setSelectedText("testing ".length(), "testing something".length(), true);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);
    Assert.assertEquals(
        getApplicationContext().getString(R.string.clipboard_copy_done_toast),
        ShadowToast.getTextOfLatestToast());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_SELECT);
    final Toast latestToast = ShadowToast.getLatestToast();
    Assert.assertNotNull(latestToast);
    Assert.assertEquals(Toast.LENGTH_SHORT, latestToast.getDuration());
    Assert.assertEquals(
        getApplicationContext().getString(R.string.clipboard_fine_select_enabled_toast),
        ShadowToast.getTextOfLatestToast());

    // and if we try to copy again, we should not see the long-press tip
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);
    Assert.assertEquals(
        getApplicationContext().getString(R.string.clipboard_copy_done_toast),
        ShadowToast.getTextOfLatestToast());
  }

  @Test
  public void testClipboardShowsOptionsToCopy() {
    final String expectedText = "testing something very long";
    mImeServiceUnderTest.simulateTextTyping(expectedText);
    mImeServiceUnderTest.setSelectedText(
        "testing ".length(), "testing something very".length(), true);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);
    mImeServiceUnderTest.setSelectedText(0, "testing ".length(), true);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);

    // now, we'll do long-press
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE_POPUP);

    final AlertDialog latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertNotNull(latestAlertDialog);
    Assert.assertEquals(
        "Pick text to paste", GeneralDialogTestUtil.getTitleFromDialog(latestAlertDialog));
    Assert.assertEquals(2, latestAlertDialog.getListView().getAdapter().getCount());
    Assert.assertEquals(
        "testing ", latestAlertDialog.getListView().getAdapter().getItem(0).toString());
    Assert.assertEquals(
        "something very", latestAlertDialog.getListView().getAdapter().getItem(1).toString());
  }

  @Test
  public void testClipboardShowsOptionsPasteFirstItem() {
    final String expectedText = "testing something very long";
    mImeServiceUnderTest.simulateTextTyping(expectedText);
    mImeServiceUnderTest.setSelectedText(
        "testing ".length(), "testing something very".length(), true);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);
    mImeServiceUnderTest.setSelectedText(0, "testing ".length(), true);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);

    // moving the cursor to the end of the textbox.
    mImeServiceUnderTest.setSelectedText(expectedText.length(), expectedText.length(), true);

    // now, we'll do long-press
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE_POPUP);

    final AlertDialog latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertNotNull(latestAlertDialog);
    Assert.assertEquals(2, latestAlertDialog.getListView().getAdapter().getCount());
    Assert.assertTrue(Shadows.shadowOf(latestAlertDialog.getListView()).performItemClick(0));

    List<KeyEvent> sentKeyEvents = mImeServiceUnderTest.getTestInputConnection().getSentKeyEvents();
    KeyEvent releaseEvent = sentKeyEvents.get(sentKeyEvents.size() - 1);
    KeyEvent downEvent = sentKeyEvents.get(sentKeyEvents.size() - 2);
    Assert.assertEquals(KeyEvent.KEYCODE_V, downEvent.getKeyCode());
    Assert.assertEquals(KeyEvent.META_CTRL_ON, downEvent.getMetaState());
    Assert.assertEquals(KeyEvent.ACTION_DOWN, downEvent.getAction());
    Assert.assertEquals(KeyEvent.KEYCODE_V, releaseEvent.getKeyCode());
    Assert.assertEquals(KeyEvent.META_CTRL_ON, releaseEvent.getMetaState());
    Assert.assertEquals(KeyEvent.ACTION_UP, releaseEvent.getAction());
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.Q)
  public void testClipboardShowsOptionsPasteFirstItemWhenOsClipboardIsEmpty() {
    final String expectedText = "testing something very long";
    mImeServiceUnderTest.simulateTextTyping(expectedText);
    mImeServiceUnderTest.setSelectedText(
        "testing ".length(), "testing something very".length(), true);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);
    mImeServiceUnderTest.setSelectedText(0, "testing".length(), true);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);

    // moving the cursor to the end of the textbox.
    mImeServiceUnderTest.setSelectedText(expectedText.length(), expectedText.length(), true);

    // clearing the OS clipboard
    mClipboardManager.clearPrimaryClip();
    // now, we'll do long-press
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE_POPUP);

    final AlertDialog latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertNotNull(latestAlertDialog);
    // still shows both items
    Assert.assertEquals(2, latestAlertDialog.getListView().getAdapter().getCount());
    Assert.assertTrue(Shadows.shadowOf(latestAlertDialog.getListView()).performItemClick(0));

    // this time it will paste from memory
    Assert.assertEquals(
        "testing something very long" + "testing",
        mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testClipboardShowsOptionsPasteNoneFirstItem() {
    final String expectedText = "testing something very long";
    mImeServiceUnderTest.simulateTextTyping(expectedText);
    mImeServiceUnderTest.setSelectedText(
        "testing ".length(), "testing something very".length(), true);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);
    mImeServiceUnderTest.setSelectedText(0, "testing ".length(), true);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);

    // moving the cursor to the end of the textbox.
    mImeServiceUnderTest.setSelectedText(expectedText.length(), expectedText.length(), true);

    // now, we'll do long-press
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE_POPUP);

    final AlertDialog latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertNotNull(latestAlertDialog);
    Assert.assertEquals(2, latestAlertDialog.getListView().getAdapter().getCount());
    Assert.assertTrue(Shadows.shadowOf(latestAlertDialog.getListView()).performItemClick(1));

    Assert.assertEquals(
        "testing something very long" + "something very",
        mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testClipboardShowsOptionsToCopyButNotDuplicates() {
    final String expectedText = "testing something very long";
    mImeServiceUnderTest.simulateTextTyping(expectedText);
    mImeServiceUnderTest.setSelectedText(
        "testing ".length(), "testing something very".length(), true);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);
    mImeServiceUnderTest.setSelectedText(0, "testing ".length(), true);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);

    // now, we'll do long-press
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE_POPUP);

    final AlertDialog latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertNotNull(latestAlertDialog);
    Assert.assertEquals(
        "Pick text to paste", GeneralDialogTestUtil.getTitleFromDialog(latestAlertDialog));
    Assert.assertEquals(2, latestAlertDialog.getListView().getAdapter().getCount());
    Assert.assertEquals(
        "testing ", latestAlertDialog.getListView().getAdapter().getItem(0).toString());
    Assert.assertEquals(
        "something very", latestAlertDialog.getListView().getAdapter().getItem(1).toString());
  }

  @Test
  public void testDeleteFirstEntry() {
    final String expectedText = "testing something very long";
    mImeServiceUnderTest.simulateTextTyping(expectedText);
    mImeServiceUnderTest.setSelectedText(
        "testing ".length(), "testing something very".length(), true);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);
    mImeServiceUnderTest.setSelectedText(0, "testing ".length(), true);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE_POPUP);

    AlertDialog latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertNotNull(latestAlertDialog);
    Assert.assertEquals(2, latestAlertDialog.getListView().getAdapter().getCount());
    latestAlertDialog
        .getListView()
        .getAdapter()
        .getView(0, null, latestAlertDialog.getListView())
        .findViewById(R.id.clipboard_entry_delete)
        .performClick();

    Assert.assertFalse(latestAlertDialog.isShowing());
    // only in API 28 do we delete the clip
    Assert.assertTrue(mClipboardManager.hasPrimaryClip());
    // but we do clear the text
    Assert.assertEquals("", mClipboardManager.getPrimaryClip().getItemAt(0).getText());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE_POPUP);
    latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertEquals(1, latestAlertDialog.getListView().getAdapter().getCount());
    Assert.assertEquals(
        "something very", latestAlertDialog.getListView().getAdapter().getItem(0).toString());

    latestAlertDialog.dismiss();

    // we changed the primary entry to "" (prior to API 28)
    Assert.assertEquals(
        "testing something very long", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE);
    // clipboard holds ""
    Assert.assertEquals(
        "something very long", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  @TargetApi(Build.VERSION_CODES.P)
  @Config(sdk = Build.VERSION_CODES.P)
  public void testDeleteFirstEntryForApi28() {
    final String expectedText = "testing something very long";
    mImeServiceUnderTest.simulateTextTyping(expectedText);
    mImeServiceUnderTest.setSelectedText(
        "testing ".length(), "testing something very".length(), true);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);
    mImeServiceUnderTest.setSelectedText(0, "testing ".length(), true);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE_POPUP);

    AlertDialog latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertNotNull(latestAlertDialog);
    Assert.assertEquals(2, latestAlertDialog.getListView().getAdapter().getCount());
    latestAlertDialog
        .getListView()
        .getAdapter()
        .getView(0, null, latestAlertDialog.getListView())
        .findViewById(R.id.clipboard_entry_delete)
        .performClick();

    Assert.assertFalse(latestAlertDialog.isShowing());
    // seems like this is a bug with Robolectric (they have not implemented clearPrimaryClip)
    // Assert.assertFalse(mClipboardManager.hasPrimaryClip());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE_POPUP);
    latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertEquals(1, latestAlertDialog.getListView().getAdapter().getCount());
    Assert.assertEquals(
        "something very", latestAlertDialog.getListView().getAdapter().getItem(0).toString());

    latestAlertDialog.dismiss();

    // also, pasting should paste nothing (we deleted the primary clip)
    Assert.assertEquals(
        "testing something very long", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE);
    Assert.assertEquals(
        "testing something very long", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testDeleteNotFirstEntry() {
    final String expectedText = "testing something very long";
    mImeServiceUnderTest.simulateTextTyping(expectedText);
    mImeServiceUnderTest.setSelectedText(
        "testing ".length(), "testing something very".length(), true);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);
    mImeServiceUnderTest.setSelectedText(0, "testing ".length(), true);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE_POPUP);

    AlertDialog latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertNotNull(latestAlertDialog);
    Assert.assertEquals(2, latestAlertDialog.getListView().getAdapter().getCount());
    latestAlertDialog
        .getListView()
        .getAdapter()
        .getView(1, null, latestAlertDialog.getListView())
        .findViewById(R.id.clipboard_entry_delete)
        .performClick();

    Assert.assertFalse(latestAlertDialog.isShowing());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE_POPUP);
    latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertEquals(1, latestAlertDialog.getListView().getAdapter().getCount());
    Assert.assertEquals(
        "testing ", latestAlertDialog.getListView().getAdapter().getItem(0).toString());

    Assert.assertEquals(
        "testing ", mClipboardManager.getPrimaryClip().getItemAt(0).getText().toString());
  }

  @Test
  public void testDeleteAllEntries() {
    final String expectedText = "testing something very long";
    mImeServiceUnderTest.simulateTextTyping(expectedText);
    mImeServiceUnderTest.setSelectedText(
        "testing ".length(), "testing something very".length(), true);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);
    mImeServiceUnderTest.setSelectedText(0, "testing ".length(), true);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE_POPUP);
    Assert.assertEquals(2, ShadowToast.shownToastCount());

    AlertDialog latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertNotNull(latestAlertDialog);
    Assert.assertEquals(2, latestAlertDialog.getListView().getAdapter().getCount());
    latestAlertDialog.getButton(DialogInterface.BUTTON_NEUTRAL).performClick();

    Assert.assertSame(
        GeneralDialogTestUtil.NO_DIALOG, GeneralDialogTestUtil.getLatestShownDialog());
    Assert.assertEquals(2, ShadowToast.shownToastCount());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE_POPUP);
    Assert.assertEquals(3, ShadowToast.shownToastCount());
    Assert.assertEquals(
        "Clipboard is empty, there is nothing to paste.", ShadowToast.getTextOfLatestToast());
  }

  @Test
  public void testClipboardShowsOptionsWhenPrimaryClipChanged() {
    mClipboardManager.setPrimaryClip(
        new ClipData("text 1", new String[0], new ClipData.Item("text 1")));
    mClipboardManager.setPrimaryClip(
        new ClipData("text 2", new String[0], new ClipData.Item("text 2")));

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE_POPUP);

    AlertDialog latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertNotNull(latestAlertDialog);
    Assert.assertEquals(2, latestAlertDialog.getListView().getAdapter().getCount());
    Assert.assertEquals(
        "text 2", latestAlertDialog.getListView().getAdapter().getItem(0).toString());
    Assert.assertEquals(
        "text 1", latestAlertDialog.getListView().getAdapter().getItem(1).toString());

    latestAlertDialog.cancel();

    mImeServiceUnderTest.simulateTextTyping("text 3");
    mImeServiceUnderTest.setSelectedText(1, 4, true);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_COPY);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE_POPUP);

    latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertNotNull(latestAlertDialog);
    Assert.assertEquals(3, latestAlertDialog.getListView().getAdapter().getCount());
    Assert.assertEquals("ext", latestAlertDialog.getListView().getAdapter().getItem(0).toString());
    Assert.assertEquals(
        "text 2", latestAlertDialog.getListView().getAdapter().getItem(1).toString());
    Assert.assertEquals(
        "text 1", latestAlertDialog.getListView().getAdapter().getItem(2).toString());

    latestAlertDialog.cancel();

    for (int clipIndex = 0; clipIndex < 100; clipIndex++) {
      mClipboardManager.setPrimaryClip(
          new ClipData("text " + clipIndex, new String[0], new ClipData.Item("text " + clipIndex)));
    }

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE_POPUP);

    latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertNotNull(latestAlertDialog);
    Assert.assertEquals(15, latestAlertDialog.getListView().getAdapter().getCount());
    Assert.assertEquals(
        "text 99", latestAlertDialog.getListView().getAdapter().getItem(0).toString());
    Assert.assertEquals(
        "text 98", latestAlertDialog.getListView().getAdapter().getItem(1).toString());
    Assert.assertEquals(
        "text 97", latestAlertDialog.getListView().getAdapter().getItem(2).toString());
    Assert.assertEquals(
        "text 96", latestAlertDialog.getListView().getAdapter().getItem(3).toString());
  }

  @Test
  public void testClipboardDoesNotShowsOptionsWhenPrimaryClipChangedAndSyncIsDisabled() {
    mClipboardManager.setPrimaryClip(
        new ClipData("text 1", new String[0], new ClipData.Item("text 1")));

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_os_clipboard_sync, false);

    mClipboardManager.setPrimaryClip(
        new ClipData("text 2", new String[0], new ClipData.Item("text 2")));

    Assert.assertNull(ShadowToast.getLatestToast());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE_POPUP);

    Assert.assertNotNull(ShadowToast.getLatestToast());
    ShadowToast.reset();

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_os_clipboard_sync, true);

    mClipboardManager.setPrimaryClip(
        new ClipData("text 3", new String[0], new ClipData.Item("text 3")));

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.CLIPBOARD_PASTE_POPUP);

    Assert.assertNull(ShadowToast.getLatestToast());
    AlertDialog latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertNotNull(latestAlertDialog);
    Assert.assertEquals(1, latestAlertDialog.getListView().getAdapter().getCount());
    Assert.assertEquals(
        "text 3", latestAlertDialog.getListView().getAdapter().getItem(0).toString());
  }

  @Test
  public void testUndo() {
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.UNDO);
    ArgumentCaptor<KeyEvent> keyEventArgumentCaptor = ArgumentCaptor.forClass(KeyEvent.class);
    Mockito.verify(mImeServiceUnderTest.getCurrentTestInputConnection(), Mockito.times(2))
        .sendKeyEvent(keyEventArgumentCaptor.capture());

    Assert.assertEquals(
        KeyEvent.ACTION_DOWN, keyEventArgumentCaptor.getAllValues().get(0).getAction());
    Assert.assertEquals(
        KeyEvent.META_CTRL_ON, keyEventArgumentCaptor.getAllValues().get(0).getMetaState());
    Assert.assertEquals(
        KeyEvent.KEYCODE_Z, keyEventArgumentCaptor.getAllValues().get(0).getKeyCode());

    Assert.assertEquals(
        KeyEvent.ACTION_UP, keyEventArgumentCaptor.getAllValues().get(1).getAction());
    Assert.assertEquals(
        KeyEvent.META_CTRL_ON, keyEventArgumentCaptor.getAllValues().get(1).getMetaState());
    Assert.assertEquals(
        KeyEvent.KEYCODE_Z, keyEventArgumentCaptor.getAllValues().get(1).getKeyCode());
  }

  @Test
  public void testRedo() {
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.REDO);
    ArgumentCaptor<KeyEvent> keyEventArgumentCaptor = ArgumentCaptor.forClass(KeyEvent.class);
    Mockito.verify(mImeServiceUnderTest.getCurrentTestInputConnection(), Mockito.times(2))
        .sendKeyEvent(keyEventArgumentCaptor.capture());

    Assert.assertEquals(
        KeyEvent.ACTION_DOWN, keyEventArgumentCaptor.getAllValues().get(0).getAction());
    Assert.assertEquals(
        KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON,
        keyEventArgumentCaptor.getAllValues().get(0).getMetaState());
    Assert.assertEquals(
        KeyEvent.KEYCODE_Z, keyEventArgumentCaptor.getAllValues().get(0).getKeyCode());

    Assert.assertEquals(
        KeyEvent.ACTION_UP, keyEventArgumentCaptor.getAllValues().get(1).getAction());
    Assert.assertEquals(
        KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON,
        keyEventArgumentCaptor.getAllValues().get(1).getMetaState());
    Assert.assertEquals(
        KeyEvent.KEYCODE_Z, keyEventArgumentCaptor.getAllValues().get(1).getKeyCode());
  }

  @Test
  public void testBasicStripActionIfClipboard() {
    Assert.assertNotNull(mImeServiceUnderTest.getClipboardActionOwnerImpl());
    Assert.assertSame(
        mImeServiceUnderTest.getClipboardActionOwnerImpl().getContext(), mImeServiceUnderTest);
    Assert.assertNotNull(mImeServiceUnderTest.getClipboardStripActionProvider());

    final View rootStripView =
        mImeServiceUnderTest
            .getClipboardStripActionProvider()
            .inflateActionView(new LinearLayout(mImeServiceUnderTest));
    Assert.assertNotNull(rootStripView);
    Assert.assertNotNull(rootStripView.findViewById(R.id.clipboard_suggestion_text));
    Assert.assertTrue(mImeServiceUnderTest.getClipboardStripActionProvider().isVisible());
    mImeServiceUnderTest.getClipboardStripActionProvider().onRemoved();
    Assert.assertFalse(mImeServiceUnderTest.getClipboardStripActionProvider().isVisible());
  }

  @Test
  public void testDoesNotShowStripActionIfClipboardIsEmpty() {
    simulateFinishInputFlow();
    simulateOnStartInputFlow();
    Assert.assertNull(
        mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.clipboard_suggestion_text));
    Assert.assertFalse(mImeServiceUnderTest.getClipboardStripActionProvider().isVisible());
  }

  @Test
  public void testShowStripActionIfClipboardIsNotEmptyHappyPath() {
    mClipboardManager.setPrimaryClip(
        new ClipData("text 1", new String[0], new ClipData.Item("text 1")));

    simulateOnStartInputFlow();
    Assert.assertTrue(mImeServiceUnderTest.getClipboardStripActionProvider().isVisible());
    Assert.assertEquals("", mImeServiceUnderTest.getCurrentInputConnectionText());

    final TextView clipboardView =
        mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.clipboard_suggestion_text);
    Assert.assertNotNull(clipboardView);
    Assert.assertEquals("text 1", clipboardView.getText().toString());
    ((View) clipboardView.getParent()).performClick();
    TestRxSchedulers.foregroundAdvanceBy(1000); // animation
    Assert.assertEquals("text 1", mImeServiceUnderTest.getCurrentInputConnectionText());
    Assert.assertNotNull(
        mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.clipboard_suggestion_text));
    Assert.assertTrue(mImeServiceUnderTest.getClipboardStripActionProvider().isVisible());
    Assert.assertFalse(mImeServiceUnderTest.getClipboardStripActionProvider().isFullyVisible());
  }

  @Test
  public void testShowActionOnLiveClipboard() {
    Assert.assertFalse(mImeServiceUnderTest.getClipboardStripActionProvider().isVisible());

    mClipboardManager.setPrimaryClip(
        new ClipData("text 1", new String[0], new ClipData.Item("text 1")));

    Assert.assertTrue(mImeServiceUnderTest.getClipboardStripActionProvider().isVisible());
    Assert.assertTrue(mImeServiceUnderTest.getClipboardStripActionProvider().isFullyVisible());
  }

  @Test
  public void testUpdateClipboardOnChange() {
    mClipboardManager.setPrimaryClip(
        new ClipData("text 1", new String[0], new ClipData.Item("text 1")));

    simulateOnStartInputFlow();
    Assert.assertTrue(mImeServiceUnderTest.getClipboardStripActionProvider().isVisible());
    final TextView clipboardView =
        mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.clipboard_suggestion_text);
    Assert.assertNotNull(clipboardView);
    Assert.assertEquals("text 1", clipboardView.getText().toString());

    mClipboardManager.setPrimaryClip(
        new ClipData("text 2", new String[0], new ClipData.Item("text 2")));

    Assert.assertEquals("text 2", clipboardView.getText().toString());
    ((View) clipboardView.getParent()).performClick();
    TestRxSchedulers.foregroundAdvanceBy(1000); // animation
    Assert.assertEquals("text 2", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testHidesActionIconIfClipboardIsEmpty() {
    mClipboardManager.setPrimaryClip(
        new ClipData("text 1", new String[0], new ClipData.Item("text 1")));

    simulateOnStartInputFlow();
    Assert.assertTrue(mImeServiceUnderTest.getClipboardStripActionProvider().isVisible());

    mClipboardManager.setPrimaryClip(null /*I know what I'm doing with this null*/);
    Assert.assertFalse(mImeServiceUnderTest.getClipboardStripActionProvider().isVisible());
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.Q)
  public void testHidesActionIconIfClipboardIsEmptyAndroid28() {
    mClipboardManager.setPrimaryClip(
        new ClipData("text 1", new String[0], new ClipData.Item("text 1")));

    simulateOnStartInputFlow();
    Assert.assertTrue(mImeServiceUnderTest.getClipboardStripActionProvider().isVisible());

    mClipboardManager.clearPrimaryClip();
    Assert.assertFalse(mImeServiceUnderTest.getClipboardStripActionProvider().isVisible());
  }

  @Test
  public void testHideActionIfKeyPressedButLeavesHintForDuration() {
    mClipboardManager.setPrimaryClip(
        new ClipData("text 1", new String[0], new ClipData.Item("text 1")));

    simulateOnStartInputFlow();
    Assert.assertTrue(mImeServiceUnderTest.getClipboardStripActionProvider().isFullyVisible());
    Assert.assertNotNull(
        mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.clipboard_suggestion_text));
    mImeServiceUnderTest.simulateKeyPress('a');
    Assert.assertFalse(mImeServiceUnderTest.getClipboardStripActionProvider().isFullyVisible());
    Assert.assertTrue(mImeServiceUnderTest.getClipboardStripActionProvider().isVisible());
    ShadowSystemClock.advanceBy(Duration.of(2, ChronoUnit.MINUTES));
    mImeServiceUnderTest.simulateKeyPress('a');
    Assert.assertFalse(mImeServiceUnderTest.getClipboardStripActionProvider().isFullyVisible());
    Assert.assertFalse(mImeServiceUnderTest.getClipboardStripActionProvider().isVisible());
    Assert.assertNull(
        mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.clipboard_suggestion_text));
  }

  @Test
  public void testShowStripActionAsPasswordIfClipboardIsNotEmptyInPasswordField() {
    simulateFinishInputFlow();
    mClipboardManager.setPrimaryClip(
        new ClipData("text 1", new String[0], new ClipData.Item("text 1")));

    int[] variations =
        new int[] {
          InputType.TYPE_TEXT_VARIATION_PASSWORD,
          InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
          InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        };

    for (int variation : variations) {
      simulateOnStartInputFlow(
          false,
          createEditorInfo(EditorInfo.IME_ACTION_NONE, InputType.TYPE_CLASS_TEXT | variation));

      final TextView clipboardView =
          mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.clipboard_suggestion_text);
      Assert.assertNotNull("for " + variation, clipboardView);
      Assert.assertEquals("for " + variation, "**********", clipboardView.getText().toString());

      simulateFinishInputFlow();
    }
  }

  @Test
  public void testShowStripActionAsPasswordIfClipboardWasOriginatedInPassword() {
    simulateFinishInputFlow();

    simulateOnStartInputFlow(
        false,
        createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));
    mClipboardManager.setPrimaryClip(
        new ClipData("text 1", new String[0], new ClipData.Item("text 1")));

    simulateFinishInputFlow();
    simulateOnStartInputFlow();

    final TextView clipboardView =
        mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.clipboard_suggestion_text);
    Assert.assertNotNull(clipboardView);
    Assert.assertEquals("**********", clipboardView.getText().toString());

    simulateFinishInputFlow();
  }

  @Test
  public void testShowStripActionAsNonPasswordIfClipboardIsNotEmptyInNonPasswordField() {
    simulateFinishInputFlow();
    mClipboardManager.setPrimaryClip(
        new ClipData("text 1", new String[0], new ClipData.Item("text 1")));

    int[] variations =
        new int[] {
          InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT,
          InputType.TYPE_TEXT_VARIATION_FILTER,
          InputType.TYPE_TEXT_VARIATION_PHONETIC,
          InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
          InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS,
          InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
          InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE,
          InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE,
          InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT,
          InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
          InputType.TYPE_TEXT_VARIATION_URI,
          InputType.TYPE_TEXT_VARIATION_NORMAL,
        };

    for (int variation : variations) {
      simulateOnStartInputFlow(
          false,
          createEditorInfo(EditorInfo.IME_ACTION_NONE, InputType.TYPE_CLASS_TEXT | variation));

      final TextView clipboardView =
          mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.clipboard_suggestion_text);
      Assert.assertNotNull("for " + variation, clipboardView);
      Assert.assertEquals("for " + variation, "text 1", clipboardView.getText().toString());

      simulateFinishInputFlow();
    }
  }

  @Test
  public void testDoesNotShowStripActionIfClipboardEntryIsOld() {
    simulateFinishInputFlow();
    mClipboardManager.setPrimaryClip(
        new ClipData("text 1", new String[0], new ClipData.Item("text 1")));
    ShadowSystemClock.advanceBy(Duration.of(121, ChronoUnit.SECONDS));
    simulateOnStartInputFlow();
    Assert.assertNull(
        mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.clipboard_suggestion_text));
    Assert.assertFalse(mImeServiceUnderTest.getClipboardStripActionProvider().isVisible());
  }

  @Test
  public void testShowHintStripActionIfClipboardEntryIsKindaOld() {
    simulateFinishInputFlow();
    mClipboardManager.setPrimaryClip(
        new ClipData("text 1", new String[0], new ClipData.Item("text 1")));
    ShadowSystemClock.advanceBy(Duration.of(16, ChronoUnit.SECONDS));
    simulateOnStartInputFlow();
    Assert.assertNotNull(
        mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.clipboard_suggestion_text));
    Assert.assertFalse(mImeServiceUnderTest.getClipboardStripActionProvider().isFullyVisible());
    Assert.assertTrue(mImeServiceUnderTest.getClipboardStripActionProvider().isVisible());
    ShadowSystemClock.advanceBy(Duration.of(120, ChronoUnit.SECONDS));
    Assert.assertFalse(mImeServiceUnderTest.getClipboardStripActionProvider().isFullyVisible());
    Assert.assertTrue(mImeServiceUnderTest.getClipboardStripActionProvider().isVisible());
    Assert.assertNotNull(
        mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.clipboard_suggestion_text));
    mImeServiceUnderTest.simulateKeyPress('a');
    Assert.assertFalse(mImeServiceUnderTest.getClipboardStripActionProvider().isVisible());
    Assert.assertNull(
        mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.clipboard_suggestion_text));
  }

  @Test
  public void testShowPopupWhenLongPress() {
    simulateFinishInputFlow();
    mClipboardManager.setPrimaryClip(
        new ClipData("text 1", new String[0], new ClipData.Item("text 1")));
    simulateOnStartInputFlow();
    View rootView =
        (View)
            mImeServiceUnderTest
                .getInputViewContainer()
                .findViewById(R.id.clipboard_suggestion_text)
                .getParent();

    Shadows.shadowOf(rootView).getOnLongClickListener().onLongClick(rootView);

    Assert.assertEquals("", getCurrentTestInputConnection().getCurrentTextInInputConnection());
    final AlertDialog latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertNotNull(latestAlertDialog);
    Assert.assertEquals(
        "Pick text to paste", GeneralDialogTestUtil.getTitleFromDialog(latestAlertDialog));

    Assert.assertFalse(mImeServiceUnderTest.getClipboardStripActionProvider().isFullyVisible());
    Assert.assertTrue(mImeServiceUnderTest.getClipboardStripActionProvider().isVisible());
  }

  @Test
  public void testOutputClipboardEntryOnViewClick() {
    simulateFinishInputFlow();
    mClipboardManager.setPrimaryClip(
        new ClipData("text 1", new String[0], new ClipData.Item("text 1")));
    simulateOnStartInputFlow();
    View rootView =
        (View)
            mImeServiceUnderTest
                .getInputViewContainer()
                .findViewById(R.id.clipboard_suggestion_text)
                .getParent();

    Shadows.shadowOf(rootView).getOnClickListener().onClick(rootView);
    TestRxSchedulers.foregroundAdvanceBy(1000); // animation
    Assert.assertEquals(
        "text 1", getCurrentTestInputConnection().getCurrentTextInInputConnection());

    Assert.assertFalse(mImeServiceUnderTest.getClipboardStripActionProvider().isFullyVisible());
    Assert.assertTrue(mImeServiceUnderTest.getClipboardStripActionProvider().isVisible());
  }
}
