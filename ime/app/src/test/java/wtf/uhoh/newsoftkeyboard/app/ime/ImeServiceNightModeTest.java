package wtf.uhoh.newsoftkeyboard.app.ime;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static wtf.uhoh.newsoftkeyboard.app.android.NightModeTest.configurationForNightMode;

import android.content.ComponentName;
import android.content.res.Configuration;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.testing.ViewTestUtils;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.MainSettingsActivity;
import wtf.uhoh.newsoftkeyboard.overlay.OverlayData;
import wtf.uhoh.newsoftkeyboard.overlay.OverlyDataCreator;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceNightModeTest extends ImeServiceBaseTest {

  @Test
  public void testIconShownWhenTriggered() throws Exception {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_night_mode, "follow_system");
    NskApplicationBase application = getApplicationContext();
    // initial watermark
    ViewTestUtils.assertCurrentWatermarkDoesNotHaveDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_night_mode);

    Mockito.reset(mImeServiceUnderTest.getInputView());

    application.onConfigurationChanged(configurationForNightMode(Configuration.UI_MODE_NIGHT_YES));

    ViewTestUtils.assertCurrentWatermarkHasDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_night_mode);

    Mockito.reset(mImeServiceUnderTest.getInputView());

    application.onConfigurationChanged(configurationForNightMode(Configuration.UI_MODE_NIGHT_NO));

    ViewTestUtils.assertCurrentWatermarkDoesNotHaveDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_night_mode);
  }

  @Test
  public void testIconShownWhenAlwaysOn() throws Exception {
    Mockito.reset(mImeServiceUnderTest.getInputView());
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_night_mode, "always");
    ViewTestUtils.assertCurrentWatermarkHasDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_night_mode);
  }

  @Test
  public void testIconShownWhenNever() throws Exception {
    Mockito.reset(mImeServiceUnderTest.getInputView());
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_night_mode, "never");
    NskApplicationBase application = getApplicationContext();
    ViewTestUtils.assertZeroWatermarkInteractions(mImeServiceUnderTest.getInputView());

    application.onConfigurationChanged(configurationForNightMode(Configuration.UI_MODE_NIGHT_YES));

    ViewTestUtils.assertZeroWatermarkInteractions(mImeServiceUnderTest.getInputView());
  }

  @Test
  public void testSetNightModeOverlay() {
    NskApplicationBase application = getApplicationContext();
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_night_mode, "follow_system");
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_night_mode_theme_control, true);

    final OverlyDataCreator originalOverlayDataCreator =
        mImeServiceUnderTest.getOriginalOverlayDataCreator();

    Assert.assertTrue(originalOverlayDataCreator instanceof ImeThemeOverlay.ToggleOverlayCreator);

    final OverlayData normal =
        originalOverlayDataCreator.createOverlayData(
            new ComponentName(
                ApplicationProvider.getApplicationContext(), MainSettingsActivity.class));
    Assert.assertNotEquals(0xFF222222, normal.getPrimaryColor());

    application.onConfigurationChanged(configurationForNightMode(Configuration.UI_MODE_NIGHT_YES));

    final OverlayData nightModeOverlay =
        originalOverlayDataCreator.createOverlayData(
            new ComponentName(
                ApplicationProvider.getApplicationContext(), MainSettingsActivity.class));
    Assert.assertTrue(nightModeOverlay.isValid());
    Assert.assertEquals(0xFF222222, nightModeOverlay.getPrimaryColor());
  }
}
