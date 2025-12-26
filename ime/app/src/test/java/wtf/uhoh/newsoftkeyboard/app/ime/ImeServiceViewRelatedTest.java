package wtf.uhoh.newsoftkeyboard.app.ime;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.test.core.app.ApplicationProvider;
import com.anysoftkeyboard.api.KeyCodes;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.InputViewBinder;
import wtf.uhoh.newsoftkeyboard.app.testing.ViewTestUtils;
import wtf.uhoh.newsoftkeyboard.testing.GeneralDialogTestUtil;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceViewRelatedTest extends ImeServiceBaseTest {

  @Test
  public void testSettingsBasic() throws Exception {
    Assert.assertEquals(
        GeneralDialogTestUtil.NO_DIALOG, GeneralDialogTestUtil.getLatestShownDialog());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SETTINGS);
    final AlertDialog latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertNotNull(latestAlertDialog);

    Assert.assertEquals(
        ApplicationProvider.getApplicationContext().getText(R.string.ime_name),
        GeneralDialogTestUtil.getTitleFromDialog(latestAlertDialog));
    Assert.assertEquals(4, latestAlertDialog.getListView().getCount());
  }

  @Test
  public void testSettingsIncognito() throws Exception {
    // initial watermark
    ViewTestUtils.assertCurrentWatermarkDoesNotHaveDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_incognito);

    Mockito.reset(mImeServiceUnderTest.getInputView());

    Assert.assertEquals(
        GeneralDialogTestUtil.NO_DIALOG, GeneralDialogTestUtil.getLatestShownDialog());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SETTINGS);

    AlertDialog latestShownDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertEquals(
        "\uD83D\uDD75️ Incognito Mode", latestShownDialog.getListView().getAdapter().getItem(3));

    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
    Assert.assertFalse(mImeServiceUnderTest.getQuickKeyHistoryRecords().isIncognitoMode());

    Shadows.shadowOf(latestShownDialog.getListView()).performItemClick(3);

    Assert.assertTrue(mImeServiceUnderTest.getSuggest().isIncognitoMode());
    Assert.assertTrue(mImeServiceUnderTest.getQuickKeyHistoryRecords().isIncognitoMode());
    ViewTestUtils.assertCurrentWatermarkHasDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_incognito);

    Mockito.reset(mImeServiceUnderTest.getInputView());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SETTINGS);
    latestShownDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Shadows.shadowOf(latestShownDialog.getListView()).performItemClick(3);

    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
    Assert.assertFalse(mImeServiceUnderTest.getQuickKeyHistoryRecords().isIncognitoMode());
    ViewTestUtils.assertCurrentWatermarkDoesNotHaveDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_incognito);
  }

  @Test
  public void testSettingsOverrideDictionary() throws Exception {
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SETTINGS);
    final AlertDialog settingsAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();

    Assert.assertEquals(
        "Override default dictionary", settingsAlertDialog.getListView().getAdapter().getItem(1));

    Shadows.shadowOf(settingsAlertDialog.getListView()).performItemClick(1);

    final AlertDialog dictionaryAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertNotSame(dictionaryAlertDialog, settingsAlertDialog);

    Assert.assertEquals(
        "Override English dictionary",
        GeneralDialogTestUtil.getTitleFromDialog(dictionaryAlertDialog));
    View.OnClickListener positiveListener =
        Shadows.shadowOf(dictionaryAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE))
            .getOnClickListener();
    View.OnClickListener negativeListener =
        Shadows.shadowOf(dictionaryAlertDialog.getButton(AlertDialog.BUTTON_NEGATIVE))
            .getOnClickListener();
    View.OnClickListener clearListener =
        Shadows.shadowOf(dictionaryAlertDialog.getButton(AlertDialog.BUTTON_NEUTRAL))
            .getOnClickListener();

    Assert.assertNotNull(positiveListener);
    Assert.assertNotNull(negativeListener);
    Assert.assertNotNull(clearListener);
  }

  @Test
  public void testSetInputViewClippingIssues() throws Exception {
    Assert.assertFalse(mImeServiceUnderTest.isFullscreenMode());
    final Window window = mImeServiceUnderTest.getWindow().getWindow();
    Assert.assertNotNull(window);
    Assert.assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, window.getAttributes().height);

    final View inputArea = window.findViewById(android.R.id.inputArea);
    Assert.assertNotNull(inputArea);
    Assert.assertNotNull(inputArea.getParent());

    final View parentView = (View) inputArea.getParent();
    Assert.assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, parentView.getLayoutParams().height);
    Assert.assertEquals(
        Gravity.BOTTOM, ((FrameLayout.LayoutParams) parentView.getLayoutParams()).gravity);
  }

  @Test
  @Config(qualifiers = "w420dp-h640dp-land-mdpi")
  public void testSetInputViewClippingIssuesInLandscape() throws Exception {
    Assert.assertTrue(mImeServiceUnderTest.isFullscreenMode());
    final Window window = mImeServiceUnderTest.getWindow().getWindow();
    Assert.assertNotNull(window);
    Assert.assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, window.getAttributes().height);

    final View inputArea = window.findViewById(android.R.id.inputArea);
    Assert.assertNotNull(inputArea);
    Assert.assertNotNull(inputArea.getParent());

    final View parentView = (View) inputArea.getParent();
    Assert.assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, parentView.getLayoutParams().height);
    Assert.assertEquals(
        Gravity.BOTTOM, ((FrameLayout.LayoutParams) parentView.getLayoutParams()).gravity);
  }

  @Test
  public void testResetViewOnAddOnChange() throws Exception {
    final InputViewBinder inputView = mImeServiceUnderTest.getInputView();
    Assert.assertNotNull(inputView);
    mImeServiceUnderTest.onAddOnsCriticalChange();
    Assert.assertNotNull(mImeServiceUnderTest.getInputView());
    Assert.assertSame(inputView, mImeServiceUnderTest.getInputView());
  }
}
