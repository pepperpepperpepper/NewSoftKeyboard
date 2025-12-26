package wtf.uhoh.newsoftkeyboard.app.ime;

import static wtf.uhoh.newsoftkeyboard.app.ime.ImeServiceThemeOverlayTest.captureOverlay;

import android.content.ComponentName;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.android.PowerSavingTest;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.InputViewBinder;
import wtf.uhoh.newsoftkeyboard.app.testing.ViewTestUtils;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.MainSettingsActivity;
import wtf.uhoh.newsoftkeyboard.overlay.OverlayData;
import wtf.uhoh.newsoftkeyboard.overlay.OverlyDataCreator;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServicePowerSavingTest extends ImeServiceBaseTest {

  @Test
  public void testDoesNotAskForSuggestionsIfInLowBattery() {
    PowerSavingTest.sendBatteryState(true);
    mImeServiceUnderTest.resetMockCandidateView();
    mImeServiceUnderTest.simulateTextTyping("h");
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping("e");
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping("l");
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping(" ");

    mImeServiceUnderTest.resetMockCandidateView();
    PowerSavingTest.sendBatteryState(false);
    mImeServiceUnderTest.simulateTextTyping("h");
    verifySuggestions(true, "h", "he");
    mImeServiceUnderTest.simulateTextTyping("e");
    verifySuggestions(true, "he", "hell", "hello", "he'll");
    mImeServiceUnderTest.simulateTextTyping("l");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");
  }

  @Test
  public void testAskForSuggestionsIfInLowBatteryButPrefIsDisabled() {
    SharedPrefsHelper.setPrefsValue(
        R.string.settings_key_power_save_mode_suggestions_control, false);
    PowerSavingTest.sendBatteryState(true);
    mImeServiceUnderTest.simulateTextTyping("h");
    verifySuggestions(true, "h", "he");
    mImeServiceUnderTest.simulateTextTyping("e");
    verifySuggestions(true, "he", "hell", "hello", "he'll");
    mImeServiceUnderTest.simulateTextTyping("l");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");

    mImeServiceUnderTest.simulateTextTyping(" ");
    mImeServiceUnderTest.resetMockCandidateView();

    PowerSavingTest.sendBatteryState(false);
    mImeServiceUnderTest.simulateTextTyping("h");
    verifySuggestions(true, "h", "he");
    mImeServiceUnderTest.simulateTextTyping("e");
    verifySuggestions(true, "he", "hell", "hello", "he'll");
    mImeServiceUnderTest.simulateTextTyping("l");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");
  }

  @Test
  public void testDoesNotAskForSuggestionsIfPowerSavingAlways() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_power_save_mode, "always");
    PowerSavingTest.sendBatteryState(false);

    mImeServiceUnderTest.resetMockCandidateView();
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping("h");
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping("e");
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping("l");
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping(" ");

    mImeServiceUnderTest.resetMockCandidateView();
    PowerSavingTest.sendBatteryState(true);
    mImeServiceUnderTest.simulateTextTyping("h");
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping("e");
    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping("l");
    verifyNoSuggestionsInteractions();
  }

  @Test
  public void testAskForSuggestionsIfPowerSavingNever() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_power_save_mode, "never");
    PowerSavingTest.sendBatteryState(true);

    verifyNoSuggestionsInteractions();
    mImeServiceUnderTest.simulateTextTyping("h");
    verifySuggestions(true, "h", "he");
    mImeServiceUnderTest.simulateTextTyping("e");
    verifySuggestions(true, "he", "hell", "hello", "he'll");
    mImeServiceUnderTest.simulateTextTyping("l");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");
    mImeServiceUnderTest.simulateTextTyping(" ");

    PowerSavingTest.sendBatteryState(false);
    mImeServiceUnderTest.simulateTextTyping("h");
    verifySuggestions(true, "h", "he");
    mImeServiceUnderTest.simulateTextTyping("e");
    verifySuggestions(true, "he", "hell", "hello", "he'll");
    mImeServiceUnderTest.simulateTextTyping("l");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");
    mImeServiceUnderTest.simulateTextTyping(" ");

    PowerSavingTest.sendBatteryState(true);
    mImeServiceUnderTest.simulateTextTyping("h");
    verifySuggestions(true, "h", "he");
    mImeServiceUnderTest.simulateTextTyping("e");
    verifySuggestions(true, "he", "hell", "hello", "he'll");
    mImeServiceUnderTest.simulateTextTyping("l");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");
  }

  @Test
  public void testDictionariesStateCycle() {
    Assert.assertTrue(mImeServiceUnderTest.getSuggest().isSuggestionsEnabled());
    Mockito.reset(mImeServiceUnderTest.getSuggest());

    PowerSavingTest.sendBatteryState(true);
    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isSuggestionsEnabled());
    Mockito.verify(mImeServiceUnderTest.getSuggest()).closeDictionaries();
    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never())
        .setupSuggestionsForKeyboard(Mockito.anyList(), Mockito.any());
    Mockito.reset(mImeServiceUnderTest.getSuggest());

    PowerSavingTest.sendBatteryState(false);
    Assert.assertTrue(mImeServiceUnderTest.getSuggest().isSuggestionsEnabled());
    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never()).closeDictionaries();
    Mockito.verify(mImeServiceUnderTest.getSuggest())
        .setupSuggestionsForKeyboard(Mockito.anyList(), Mockito.any());
    Mockito.reset(mImeServiceUnderTest.getSuggest());

    SharedPrefsHelper.setPrefsValue("candidates_on", false);
    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isSuggestionsEnabled());
    Mockito.verify(mImeServiceUnderTest.getSuggest()).closeDictionaries();
    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never())
        .setupSuggestionsForKeyboard(Mockito.anyList(), Mockito.any());
    Mockito.reset(mImeServiceUnderTest.getSuggest());

    SharedPrefsHelper.setPrefsValue("candidates_on", true);
    Assert.assertTrue(mImeServiceUnderTest.getSuggest().isSuggestionsEnabled());
    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never()).closeDictionaries();
    Mockito.verify(mImeServiceUnderTest.getSuggest())
        .setupSuggestionsForKeyboard(Mockito.anyList(), Mockito.any());
    Mockito.reset(mImeServiceUnderTest.getSuggest());
  }

  @Test
  public void testIconShownWhenTriggered() throws Exception {
    // initial watermark
    ViewTestUtils.assertCurrentWatermarkDoesNotHaveDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_power_saving);

    Mockito.reset(mImeServiceUnderTest.getInputView());

    PowerSavingTest.sendBatteryState(true);

    ViewTestUtils.assertCurrentWatermarkHasDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_power_saving);

    Mockito.reset(mImeServiceUnderTest.getInputView());

    PowerSavingTest.sendBatteryState(false);

    ViewTestUtils.assertCurrentWatermarkDoesNotHaveDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_power_saving);
  }

  @Test
  public void testIconShownWhenAlwaysOn() throws Exception {
    Mockito.reset(mImeServiceUnderTest.getInputView());
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_power_save_mode, "always");
    // initial watermark
    ViewTestUtils.assertCurrentWatermarkHasDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_power_saving);

    Mockito.reset(mImeServiceUnderTest.getInputView());

    PowerSavingTest.sendBatteryState(true);

    // does not change (since it's still `always`)
    Mockito.verify(mImeServiceUnderTest.getInputView(), Mockito.never())
        .setWatermark(Mockito.anyList());

    Mockito.reset(mImeServiceUnderTest.getInputView());

    PowerSavingTest.sendBatteryState(false);

    // does not change (since it's still `always`
    Mockito.verify(mImeServiceUnderTest.getInputView(), Mockito.never())
        .setWatermark(Mockito.anyList());
  }

  @Test
  public void testIconShownWhenNeverOn() throws Exception {
    Mockito.reset(mImeServiceUnderTest.getInputView());
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_power_save_mode, "never");
    // initial watermark
    Mockito.verify(mImeServiceUnderTest.getInputView(), Mockito.never())
        .setWatermark(Mockito.anyList());

    Mockito.reset(mImeServiceUnderTest.getInputView());

    PowerSavingTest.sendBatteryState(true);

    Mockito.verify(mImeServiceUnderTest.getInputView(), Mockito.never())
        .setWatermark(Mockito.anyList());

    Mockito.reset(mImeServiceUnderTest.getInputView());

    PowerSavingTest.sendBatteryState(false);

    Mockito.verify(mImeServiceUnderTest.getInputView(), Mockito.never())
        .setWatermark(Mockito.anyList());
  }

  @Test
  public void testCallOverlayOnPowerSavingSwitchEvenIfOverlaySettingOff() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_apply_remote_app_colors, false);
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_power_save_mode_theme_control, true);

    simulateOnStartInputFlow();
    Assert.assertFalse(captureOverlay(mImeServiceUnderTest).isValid());

    PowerSavingTest.sendBatteryState(true);
    Assert.assertFalse(mImeServiceUnderTest.isKeyboardViewHidden());

    // switched overlay on the fly
    final OverlayData powerSaving = captureOverlay(mImeServiceUnderTest);
    Assert.assertTrue(powerSaving.isValid());
    Assert.assertEquals(0xFF000000, powerSaving.getPrimaryColor());
    Assert.assertEquals(0xFF000000, powerSaving.getPrimaryDarkColor());
    Assert.assertEquals(0xFF888888, powerSaving.getPrimaryTextColor());

    PowerSavingTest.sendBatteryState(false);
    Assert.assertFalse(mImeServiceUnderTest.isKeyboardViewHidden());

    Assert.assertFalse(captureOverlay(mImeServiceUnderTest).isValid());
  }

  @Test
  public void testSetPowerSavingOverlayWhenLowBattery() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_power_save_mode_theme_control, true);

    final OverlyDataCreator originalOverlayDataCreator =
        mImeServiceUnderTest.getOriginalOverlayDataCreator();

    Assert.assertTrue(originalOverlayDataCreator instanceof ImeThemeOverlay.ToggleOverlayCreator);

    final OverlayData normal =
        originalOverlayDataCreator.createOverlayData(
            new ComponentName(
                ApplicationProvider.getApplicationContext(), MainSettingsActivity.class));
    Assert.assertNotEquals(0xFF000000, normal.getPrimaryColor());

    PowerSavingTest.sendBatteryState(true);

    final OverlayData powerSaving =
        originalOverlayDataCreator.createOverlayData(
            new ComponentName(
                ApplicationProvider.getApplicationContext(), MainSettingsActivity.class));
    Assert.assertTrue(powerSaving.isValid());
    Assert.assertEquals(0xFF000000, powerSaving.getPrimaryColor());
    Assert.assertEquals(0xFF000000, powerSaving.getPrimaryDarkColor());
    Assert.assertEquals(0xFF888888, powerSaving.getPrimaryTextColor());

    PowerSavingTest.sendBatteryState(false);

    final OverlayData normal2 =
        originalOverlayDataCreator.createOverlayData(
            new ComponentName(
                ApplicationProvider.getApplicationContext(), MainSettingsActivity.class));
    Assert.assertNotEquals(0xFF000000, normal2.getPrimaryColor());
  }

  @Test
  public void testDisablesGestureTypingOnLowPower() {
    Assert.assertEquals(0, mImeServiceUnderTest.mGestureTypingDetectors.size());
    simulateFinishInputFlow();

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_gesture_typing, true);
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_power_save_mode_gesture_control, true);

    simulateOnStartInputFlow();

    Assert.assertEquals(1, mImeServiceUnderTest.mGestureTypingDetectors.size());

    PowerSavingTest.sendBatteryState(true);

    Assert.assertEquals(0, mImeServiceUnderTest.mGestureTypingDetectors.size());
    simulateFinishInputFlow();
    simulateOnStartInputFlow();

    Assert.assertEquals(0, mImeServiceUnderTest.mGestureTypingDetectors.size());

    PowerSavingTest.sendBatteryState(false);

    simulateFinishInputFlow();
    simulateOnStartInputFlow();

    Assert.assertEquals(1, mImeServiceUnderTest.mGestureTypingDetectors.size());
  }

  @Test
  public void testDoesNotSetPowerSavingThemeWhenLowBatteryIfPrefDisabled() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_power_save_mode_theme_control, false);
    // this is the default behavior
    InputViewBinder keyboardView = mImeServiceUnderTest.getInputView();
    Assert.assertNotNull(keyboardView);

    Mockito.reset(keyboardView);

    Assert.assertFalse(mImeServiceUnderTest.isKeyboardViewHidden());

    PowerSavingTest.sendBatteryState(true);

    Assert.assertFalse(mImeServiceUnderTest.isKeyboardViewHidden());

    keyboardView = mImeServiceUnderTest.getInputView();
    Mockito.verify(keyboardView, Mockito.never()).setKeyboardTheme(Mockito.any());

    PowerSavingTest.sendBatteryState(false);

    Assert.assertFalse(mImeServiceUnderTest.isKeyboardViewHidden());

    keyboardView = mImeServiceUnderTest.getInputView();
    Mockito.verify(keyboardView, Mockito.never()).setKeyboardTheme(Mockito.any());
  }
}
