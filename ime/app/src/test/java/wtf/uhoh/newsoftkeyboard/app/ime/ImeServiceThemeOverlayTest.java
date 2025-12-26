package wtf.uhoh.newsoftkeyboard.app.ime;

import android.content.ComponentName;
import android.view.inputmethod.EditorInfo;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.android.PowerSavingTest;
import wtf.uhoh.newsoftkeyboard.app.testing.TestableImeService;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.MainSettingsActivity;
import wtf.uhoh.newsoftkeyboard.overlay.OverlayData;
import wtf.uhoh.newsoftkeyboard.overlay.OverlyDataCreator;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceThemeOverlayTest extends ImeServiceBaseTest {

  @Test
  public void testDefaultAppliesInvalidOverlayAndDoesNotInteractWithCreator() {
    simulateOnStartInputFlow();

    OverlayData appliedData = captureOverlay();
    Assert.assertFalse(appliedData.isValid());
    Assert.assertSame(ImeThemeOverlay.INVALID_OVERLAY_DATA, appliedData);
  }

  @Test
  public void testWhenEnabledAppliesOverlayFromCreator() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_apply_remote_app_colors, true);
    Mockito.reset(mImeServiceUnderTest.getMockOverlayDataCreator());

    final EditorInfo editorInfo = createEditorInfoTextWithSuggestionsForSetUp();
    simulateOnStartInputFlow(false, editorInfo);
    ArgumentCaptor<ComponentName> componentNameArgumentCaptor =
        ArgumentCaptor.forClass(ComponentName.class);
    Mockito.verify(mImeServiceUnderTest.getMockOverlayDataCreator())
        .createOverlayData(componentNameArgumentCaptor.capture());
    Assert.assertEquals(
        editorInfo.packageName, componentNameArgumentCaptor.getValue().getPackageName());

    OverlayData appliedData = captureOverlay();
    Assert.assertTrue(appliedData.isValid());
  }

  @Test
  public void testStartsEnabledStopsApplyingAfterDisabled() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_apply_remote_app_colors, true);

    simulateOnStartInputFlow(false, createEditorInfoTextWithSuggestionsForSetUp());

    Assert.assertTrue(captureOverlay().isValid());

    simulateFinishInputFlow();

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_apply_remote_app_colors, false);
    simulateOnStartInputFlow(false, createEditorInfoTextWithSuggestionsForSetUp());
    Assert.assertSame(captureOverlay(), ImeThemeOverlay.INVALID_OVERLAY_DATA);
  }

  @Test
  public void testAppliesInvalidIfRemotePackageDoesNotHaveIntent() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_apply_remote_app_colors, true);

    final EditorInfo editorInfo = createEditorInfoTextWithSuggestionsForSetUp();
    editorInfo.packageName = "com.is.not.there";
    simulateOnStartInputFlow(false, editorInfo);

    OverlayData appliedData = captureOverlay();
    Assert.assertFalse(appliedData.isValid());
    Assert.assertSame(ImeThemeOverlay.INVALID_OVERLAY_DATA, appliedData);
  }

  @Test
  public void testSwitchesBetweenApps() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_apply_remote_app_colors, true);

    simulateFinishInputFlow();
    simulateOnStartInputFlow();

    Assert.assertTrue(captureOverlay().isValid());

    simulateFinishInputFlow();

    final EditorInfo editorInfo = createEditorInfoTextWithSuggestionsForSetUp();
    editorInfo.packageName = "com.is.not.there";
    simulateOnStartInputFlow(false, editorInfo);

    Assert.assertFalse(captureOverlay().isValid());

    simulateFinishInputFlow();
    // again, a valid app
    simulateOnStartInputFlow();
    Assert.assertTrue(captureOverlay().isValid());
  }

  @Test
  public void testRestartsInputField() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_apply_remote_app_colors, true);

    simulateFinishInputFlow();
    simulateOnStartInputFlow();

    Assert.assertTrue(captureOverlay().isValid());

    simulateFinishInputFlow();
    simulateOnStartInputFlow();

    Assert.assertTrue(captureOverlay().isValid());

    simulateFinishInputFlow();
    simulateOnStartInputFlow();

    Assert.assertTrue(captureOverlay().isValid());

    simulateFinishInputFlow();
    simulateOnStartInputFlow();

    Assert.assertTrue(captureOverlay().isValid());

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_apply_remote_app_colors, false);

    simulateFinishInputFlow();
    simulateOnStartInputFlow();

    Assert.assertFalse(captureOverlay().isValid());

    simulateFinishInputFlow();

    simulateOnStartInputFlow();

    Assert.assertFalse(captureOverlay().isValid());
  }

  @Test
  public void testDoesNotFailWithEmptyPackageName() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_apply_remote_app_colors, true);

    simulateFinishInputFlow();
    simulateOnStartInputFlow();

    Assert.assertTrue(captureOverlay().isValid());

    simulateFinishInputFlow();

    final EditorInfo editorInfo = createEditorInfoTextWithSuggestionsForSetUp();
    editorInfo.packageName = null;
    simulateOnStartInputFlow(false, editorInfo);

    Assert.assertFalse(captureOverlay().isValid());

    simulateFinishInputFlow();
    // again, a valid app
    simulateOnStartInputFlow();
    Assert.assertTrue(captureOverlay().isValid());
  }

  @Test
  public void testPowerSavingPriority() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_power_save_mode_theme_control, true);
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_apply_remote_app_colors, true);

    final OverlyDataCreator originalOverlayDataCreator =
        mImeServiceUnderTest.getOriginalOverlayDataCreator();

    final OverlayData normal =
        originalOverlayDataCreator.createOverlayData(
            new ComponentName(
                ApplicationProvider.getApplicationContext(), MainSettingsActivity.class));
    Assert.assertTrue(normal.isValid());
    Assert.assertEquals(0xFF0000FF, normal.getPrimaryColor());
    Assert.assertEquals(0xFF0000B2, normal.getPrimaryDarkColor());
    Assert.assertEquals(0xFFFFFFFF, normal.getPrimaryTextColor());

    PowerSavingTest.sendBatteryState(true);

    final OverlayData powerSaving =
        originalOverlayDataCreator.createOverlayData(
            new ComponentName(
                ApplicationProvider.getApplicationContext(), MainSettingsActivity.class));
    Assert.assertTrue(powerSaving.isValid());
    Assert.assertEquals(0xFF000000, powerSaving.getPrimaryColor());
    Assert.assertEquals(0xFF000000, powerSaving.getPrimaryDarkColor());
    Assert.assertEquals(0xFF888888, powerSaving.getPrimaryTextColor());
  }

  private OverlayData captureOverlay() {
    return captureOverlay(mImeServiceUnderTest);
  }

  public static OverlayData captureOverlay(TestableImeService testableImeService) {
    ArgumentCaptor<OverlayData> captor = ArgumentCaptor.forClass(OverlayData.class);
    Mockito.verify(testableImeService.getInputView(), Mockito.atLeastOnce())
        .setThemeOverlay(captor.capture());

    return captor.getValue();
  }
}
