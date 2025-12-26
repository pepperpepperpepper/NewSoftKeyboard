package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.testing.RobolectricFragmentTestCase;
import wtf.uhoh.newsoftkeyboard.app.testing.ViewTestUtils;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;
import wtf.uhoh.newsoftkeyboard.testing.GeneralDialogTestUtil;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;

@RunWith(NskRobolectricTestRunner.class)
public class AdditionalUiSettingsFragmentTest
    extends RobolectricFragmentTestCase<AdditionalUiSettingsFragment> {

  @Override
  protected int getStartFragmentNavigationId() {
    return R.id.additionalUiSettingsFragment;
  }

  @Test
  public void testNavigationCommonTopRow() {
    final AdditionalUiSettingsFragment fragment = startFragment();

    ViewTestUtils.performClick(fragment.findPreference("settings_key_ext_kbd_top_row_key"));

    TestRxSchedulers.foregroundFlushAllJobs();
    final Fragment next = getCurrentFragment();
    Assert.assertTrue(next instanceof AdditionalUiSettingsFragment.TopRowAddOnBrowserFragment);
    Assert.assertFalse(next.hasOptionsMenu());
  }

  @Test
  public void testNavigationCommonExtensionKeyboard() {
    final AdditionalUiSettingsFragment fragment = startFragment();

    ViewTestUtils.performClick(fragment.findPreference("settings_key_ext_kbd_extension_key"));

    TestRxSchedulers.foregroundFlushAllJobs();
    final Fragment next = getCurrentFragment();
    Assert.assertTrue(next instanceof AdditionalUiSettingsFragment.ExtensionAddOnBrowserFragment);
    Assert.assertFalse(next.hasOptionsMenu());
  }

  @Test
  public void testNavigationCommonBottomRow() {
    final AdditionalUiSettingsFragment fragment = startFragment();

    ViewTestUtils.performClick(fragment.findPreference("settings_key_ext_kbd_bottom_row_key"));

    TestRxSchedulers.foregroundFlushAllJobs();
    final Fragment next = getCurrentFragment();
    Assert.assertTrue(next instanceof AdditionalUiSettingsFragment.BottomRowAddOnBrowserFragment);
    Assert.assertFalse(next.hasOptionsMenu());
  }

  @Test
  public void testNavigationTweaks() {
    final AdditionalUiSettingsFragment fragment = startFragment();

    ViewTestUtils.performClick(fragment.findPreference("tweaks"));

    TestRxSchedulers.foregroundFlushAllJobs();
    final Fragment next = getCurrentFragment();
    Assert.assertTrue(next instanceof UiTweaksFragment);
  }

  @Test
  public void testNavigationSupportedRowsAndHappyPath() {
    final AdditionalUiSettingsFragment fragment = startFragment();

    ViewTestUtils.performClick(fragment.findPreference("settings_key_supported_row_modes"));

    TestRxSchedulers.foregroundFlushAllJobs();

    AlertDialog latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertNotNull(latestAlertDialog);

    Assert.assertEquals(4, latestAlertDialog.getListView().getAdapter().getCount());
    Assert.assertEquals(
        "Messaging input field", latestAlertDialog.getListView().getAdapter().getItem(0));
    Assert.assertEquals("URL input field", latestAlertDialog.getListView().getAdapter().getItem(1));
    Assert.assertEquals(
        "Email input field", latestAlertDialog.getListView().getAdapter().getItem(2));
    Assert.assertEquals(
        "Password input field", latestAlertDialog.getListView().getAdapter().getItem(3));

    Assert.assertTrue(
        SharedPrefsHelper.getPrefValue(
            Keyboard.getPrefKeyForEnabledRowMode(Keyboard.KEYBOARD_ROW_MODE_EMAIL), true));
    Shadows.shadowOf(latestAlertDialog.getListView()).performItemClick(2);
    Assert.assertTrue(latestAlertDialog.isShowing());
    latestAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
    TestRxSchedulers.foregroundAdvanceBy(1);
    Assert.assertFalse(latestAlertDialog.isShowing());
    Assert.assertFalse(
        SharedPrefsHelper.getPrefValue(
            Keyboard.getPrefKeyForEnabledRowMode(Keyboard.KEYBOARD_ROW_MODE_EMAIL), true));
  }

  @Test
  public void testNavigationSupportedRowsAndCancel() {
    final AdditionalUiSettingsFragment fragment = startFragment();

    ViewTestUtils.performClick(fragment.findPreference("settings_key_supported_row_modes"));

    TestRxSchedulers.foregroundFlushAllJobs();

    AlertDialog latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertNotNull(latestAlertDialog);

    Assert.assertTrue(
        SharedPrefsHelper.getPrefValue(
            Keyboard.getPrefKeyForEnabledRowMode(Keyboard.KEYBOARD_ROW_MODE_EMAIL), true));
    Shadows.shadowOf(latestAlertDialog.getListView()).performItemClick(2);
    latestAlertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
    TestRxSchedulers.foregroundAdvanceBy(1);
    Assert.assertFalse(latestAlertDialog.isShowing());
  }
}
