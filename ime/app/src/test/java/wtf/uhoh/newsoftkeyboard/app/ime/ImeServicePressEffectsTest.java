package wtf.uhoh.newsoftkeyboard.app.ime;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static wtf.uhoh.newsoftkeyboard.app.android.NightModeTest.configurationForNightMode;
import static wtf.uhoh.newsoftkeyboard.app.testing.TestableImeService.createEditorInfo;

import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Looper;
import android.os.VibrationEffect;
import android.provider.Settings;
import android.view.inputmethod.EditorInfo;
import androidx.test.core.app.ApplicationProvider;
import com.anysoftkeyboard.api.KeyCodes;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowVibrator;
import org.robolectric.util.ReflectionHelpers;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.android.PowerSavingTest;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.InputViewBinder;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.preview.AboveKeyPositionCalculator;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.preview.AboveKeyboardPositionCalculator;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.preview.KeyPreviewsController;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.preview.KeyPreviewsManager;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.preview.NullKeyPreviewsManager;
import wtf.uhoh.newsoftkeyboard.app.testing.TestableImeService;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardThemeFactory;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.ShadowNskAudioManager;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServicePressEffectsTest extends ImeServiceBaseTest {

  @Override
  protected Class<? extends TestableImeService> getServiceClass() {
    return TestableImeServicePressEffects.class;
  }

  @Before
  public void setupSystemHaptic() {
    // Each test should start from a clean feedback baseline. These tests run on multiple SDKs and
    // share the same Robolectric static shadow state and default SharedPreferences.
    final var prefs = NskApplicationBase.prefs(getApplicationContext());
    prefs
        .getString(R.string.settings_key_night_mode, R.string.settings_default_night_mode_value)
        .set("never");
    prefs
        .getBoolean(
            R.string.settings_key_night_mode_vibration_control, R.bool.settings_default_false)
        .set(false);
    prefs
        .getString(
            R.string.settings_key_power_save_mode, R.string.settings_default_power_save_mode_value)
        .set("on_low_battery");
    prefs
        .getBoolean(
            R.string.settings_key_power_save_mode_vibration_control, R.bool.settings_default_false)
        .set(false);
    prefs
        .getBoolean(
            R.string.settings_key_use_system_vibration,
            R.bool.settings_default_use_system_vibration)
        .set(false);

    PowerSavingTest.sendBatteryState(false);
    PowerSavingTest.sendChargingState(false);

    NskApplicationBase application = getApplicationContext();
    application.onConfigurationChanged(configurationForNightMode(Configuration.UI_MODE_NIGHT_NO));

    mImeServiceUnderTest.getAudioManager().setRingerMode(AudioManager.RINGER_MODE_NORMAL);
    setSystemWideHaptic(true);
    TestRxSchedulers.drainAllTasksUntilEnd();
    ShadowVibrator.reset();
    Shadows.shadowOf(mImeServiceUnderTest.getVibrator()).setHasVibrator(true);
    ImePressEffects.sHapticFeedbackPerformer = (view, effect, flags) -> false;
  }

  @After
  public void teardownHapticPerformer() {
    ImePressEffects.sHapticFeedbackPerformer = null;
  }

  private void setSystemWideHaptic(boolean enabled) {
    Settings.Secure.putInt(
        ApplicationProvider.getApplicationContext().getContentResolver(),
        Settings.System.HAPTIC_FEEDBACK_ENABLED,
        enabled ? 1 : 0);
    Settings.System.putInt(
        ApplicationProvider.getApplicationContext().getContentResolver(),
        Settings.System.HAPTIC_FEEDBACK_ENABLED,
        enabled ? 1 : 0);
    Shadows.shadowOf(ApplicationProvider.getApplicationContext().getContentResolver())
        .getContentObservers(Settings.Secure.getUriFor(Settings.System.HAPTIC_FEEDBACK_ENABLED))
        .forEach(v -> v.onChange(false));
    Shadows.shadowOf(ApplicationProvider.getApplicationContext().getContentResolver())
        .getContentObservers(Settings.System.getUriFor(Settings.System.HAPTIC_FEEDBACK_ENABLED))
        .forEach(v -> v.onChange(false));
    TestRxSchedulers.drainAllTasksUntilEnd();
  }

  private static void resetVibratorState(ShadowVibrator shadowVibrator) {
    ShadowVibrator.reset();
    shadowVibrator.setHasVibrator(true);
  }

  @SuppressWarnings("unchecked")
  private static int getVibrationEffectSegmentsCount() {
    try {
      final Object segments =
          ReflectionHelpers.getStaticField(ShadowVibrator.class, "vibrationEffectSegments");
      if (segments instanceof java.util.List) return ((java.util.List<?>) segments).size();
    } catch (Throwable ignored) {
      // best effort
    }
    return 0;
  }

  private static void assertNoVibrationInvoked(ShadowVibrator shadowVibrator) {
    Assert.assertFalse(shadowVibrator.isVibrating());
    Assert.assertEquals(0, shadowVibrator.getMilliseconds());
    Assert.assertNull(shadowVibrator.getPattern());
    Assert.assertEquals(0, shadowVibrator.getEffectId());
    Assert.assertNull(shadowVibrator.getVibrationAttributesFromLastVibration());
    Assert.assertNull(shadowVibrator.getAudioAttributesFromLastVibration());
    Assert.assertTrue(shadowVibrator.getPrimitiveEffects().isEmpty());
    Assert.assertTrue(shadowVibrator.getPrimitiveSegmentsInPrimitiveEffects().isEmpty());
    Assert.assertEquals(0, getVibrationEffectSegmentsCount());
  }

  private static void assertVibrationInvoked(ShadowVibrator shadowVibrator) {
    final long milliseconds = shadowVibrator.getMilliseconds();
    final long[] pattern = shadowVibrator.getPattern();
    final int effectId = shadowVibrator.getEffectId();
    final Object vibrationAttributes = shadowVibrator.getVibrationAttributesFromLastVibration();
    final Object audioAttributes = shadowVibrator.getAudioAttributesFromLastVibration();
    final int primitiveEffectsCount = shadowVibrator.getPrimitiveEffects().size();
    final int primitiveSegmentsCount =
        shadowVibrator.getPrimitiveSegmentsInPrimitiveEffects().size();
    final int vibrationEffectSegmentsCount = getVibrationEffectSegmentsCount();
    final boolean invoked =
        shadowVibrator.isVibrating()
            || milliseconds != 0
            || pattern != null
            || effectId != 0
            || audioAttributes != null
            || vibrationAttributes != null
            || primitiveEffectsCount != 0
            || primitiveSegmentsCount != 0
            || vibrationEffectSegmentsCount != 0;
    Assert.assertTrue(
        "Expected vibrator to be invoked. ms="
            + milliseconds
            + " pattern="
            + (pattern == null ? "null" : pattern.length)
            + " effectId="
            + effectId
            + " audioAttributes="
            + audioAttributes
            + " vibrationAttributes="
            + vibrationAttributes
            + " primitiveEffects="
            + primitiveEffectsCount
            + " primitiveSegments="
            + primitiveSegmentsCount,
        invoked);
  }

  @Test
  public void testLoadAndUnloadSystemSounds() {
    ShadowNskAudioManager shadowAudioManager =
        (ShadowNskAudioManager) Shadows.shadowOf(mImeServiceUnderTest.getAudioManager());
    Assert.assertEquals(
        getApplicationContext().getResources().getBoolean(R.bool.settings_default_sound_on),
        shadowAudioManager.areSoundEffectsLoaded());

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_sound_on, true);
    Assert.assertTrue(shadowAudioManager.areSoundEffectsLoaded());
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_sound_on, false);
    Assert.assertFalse(shadowAudioManager.areSoundEffectsLoaded());
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_sound_on, true);
    Assert.assertTrue(shadowAudioManager.areSoundEffectsLoaded());

    mImeServiceController.destroy();
    Assert.assertFalse(shadowAudioManager.areSoundEffectsLoaded());
  }

  @Test
  public void testPlaysSoundIfEnabled() {
    ShadowNskAudioManager shadowAudioManager =
        (ShadowNskAudioManager) Shadows.shadowOf(mImeServiceUnderTest.getAudioManager());
    // consuming demo - if one took place at start up
    shadowAudioManager.getLastPlaySoundEffectType();
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_sound_on, true);
    // demo
    Assert.assertEquals(
        AudioManager.FX_KEYPRESS_SPACEBAR, shadowAudioManager.getLastPlaySoundEffectType());

    mImeServiceUnderTest.onPress('j');
    Assert.assertEquals(
        AudioManager.FX_KEYPRESS_STANDARD, shadowAudioManager.getLastPlaySoundEffectType());

    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    Assert.assertEquals(
        AudioManager.FX_KEYPRESS_SPACEBAR, shadowAudioManager.getLastPlaySoundEffectType());

    mImeServiceUnderTest.onPress(KeyCodes.ENTER);
    Assert.assertEquals(
        AudioManager.FX_KEYPRESS_RETURN, shadowAudioManager.getLastPlaySoundEffectType());

    mImeServiceUnderTest.onPress(KeyCodes.DELETE);
    Assert.assertEquals(
        AudioManager.FX_KEYPRESS_DELETE, shadowAudioManager.getLastPlaySoundEffectType());

    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    Assert.assertEquals(AudioManager.FX_KEY_CLICK, shadowAudioManager.getLastPlaySoundEffectType());
    mImeServiceUnderTest.onPress(KeyCodes.SHIFT_LOCK);
    Assert.assertEquals(AudioManager.FX_KEY_CLICK, shadowAudioManager.getLastPlaySoundEffectType());
    mImeServiceUnderTest.onPress(KeyCodes.CTRL);
    Assert.assertEquals(AudioManager.FX_KEY_CLICK, shadowAudioManager.getLastPlaySoundEffectType());
    mImeServiceUnderTest.onPress(KeyCodes.CTRL_LOCK);
    Assert.assertEquals(AudioManager.FX_KEY_CLICK, shadowAudioManager.getLastPlaySoundEffectType());
    mImeServiceUnderTest.onPress(KeyCodes.ALT);
    Assert.assertEquals(AudioManager.FX_KEY_CLICK, shadowAudioManager.getLastPlaySoundEffectType());
    mImeServiceUnderTest.onPress(KeyCodes.MODE_ALPHABET);
    Assert.assertEquals(AudioManager.FX_KEY_CLICK, shadowAudioManager.getLastPlaySoundEffectType());
    mImeServiceUnderTest.onPress(KeyCodes.MODE_SYMBOLS);
    Assert.assertEquals(AudioManager.FX_KEY_CLICK, shadowAudioManager.getLastPlaySoundEffectType());
    mImeServiceUnderTest.onPress(KeyCodes.KEYBOARD_CYCLE_INSIDE_MODE);
    Assert.assertEquals(AudioManager.FX_KEY_CLICK, shadowAudioManager.getLastPlaySoundEffectType());
    mImeServiceUnderTest.onPress(KeyCodes.KEYBOARD_MODE_CHANGE);
    Assert.assertEquals(AudioManager.FX_KEY_CLICK, shadowAudioManager.getLastPlaySoundEffectType());

    mImeServiceUnderTest.onPress(0);
    Assert.assertEquals(
        AudioManager.FX_KEYPRESS_STANDARD, shadowAudioManager.getLastPlaySoundEffectType());
  }

  @Test
  public void testPlaysSoundEvenWhenRingerModeNotNormal() {
    ShadowNskAudioManager shadowAudioManager =
        (ShadowNskAudioManager) Shadows.shadowOf(mImeServiceUnderTest.getAudioManager());

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_sound_on, true);
    shadowAudioManager.getLastPlaySoundEffectType(); // consuming any demo

    mImeServiceUnderTest.getAudioManager().setRingerMode(AudioManager.RINGER_MODE_SILENT);
    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    Assert.assertEquals(
        AudioManager.FX_KEYPRESS_SPACEBAR, shadowAudioManager.getLastPlaySoundEffectType());
  }

  @Test
  public void testPlaysSoundWithCustomVolumeWhenEnabled() {
    ShadowNskAudioManager shadowAudioManager =
        (ShadowNskAudioManager) Shadows.shadowOf(mImeServiceUnderTest.getAudioManager());

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_sound_on, true);
    shadowAudioManager.getLastPlaySoundEffectType(); // consuming any demo

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_use_custom_sound_volume, true);
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_custom_sound_volume, 60);
    shadowAudioManager.getLastPlaySoundEffectType(); // consuming any demo
    shadowAudioManager.getLastPlaySoundEffectVolume(); // consuming any demo

    mImeServiceUnderTest.onPress('a');
    Assert.assertEquals(
        AudioManager.FX_KEYPRESS_STANDARD, shadowAudioManager.getLastPlaySoundEffectType());
    Assert.assertEquals(0.60f, shadowAudioManager.getLastPlaySoundEffectVolume(), 0.001f);
  }

  @Test
  public void testDoNotPlaysSoundWhenLowPower() {
    ShadowNskAudioManager shadowAudioManager =
        (ShadowNskAudioManager) Shadows.shadowOf(mImeServiceUnderTest.getAudioManager());
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_power_save_mode_sound_control, true);
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_sound_on, true);
    shadowAudioManager.getLastPlaySoundEffectType();

    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    Assert.assertEquals(
        AudioManager.FX_KEYPRESS_SPACEBAR, shadowAudioManager.getLastPlaySoundEffectType());

    PowerSavingTest.sendBatteryState(true);

    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    Assert.assertEquals(Integer.MIN_VALUE, shadowAudioManager.getLastPlaySoundEffectType());

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_power_save_mode_sound_control, false);

    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    Assert.assertEquals(
        AudioManager.FX_KEYPRESS_SPACEBAR, shadowAudioManager.getLastPlaySoundEffectType());
  }

  @Test
  public void testDoNotPlaysSoundWhenNightTime() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_night_mode, "follow_system");
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_night_mode_sound_control, true);
    ShadowNskAudioManager shadowAudioManager =
        (ShadowNskAudioManager) Shadows.shadowOf(mImeServiceUnderTest.getAudioManager());
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_sound_on, true);
    shadowAudioManager.getLastPlaySoundEffectType();

    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    Assert.assertEquals(
        AudioManager.FX_KEYPRESS_SPACEBAR, shadowAudioManager.getLastPlaySoundEffectType());

    NskApplicationBase application = getApplicationContext();
    application.onConfigurationChanged(configurationForNightMode(Configuration.UI_MODE_NIGHT_YES));

    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    Assert.assertEquals(Integer.MIN_VALUE, shadowAudioManager.getLastPlaySoundEffectType());

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_night_mode_sound_control, false);

    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    Assert.assertEquals(
        AudioManager.FX_KEYPRESS_SPACEBAR, shadowAudioManager.getLastPlaySoundEffectType());
  }

  @Test
  @Config(sdk = {25, 26, 29})
  public void testNightModeDoesNotAffectVibration() {
    final var prefs = NskApplicationBase.prefs(getApplicationContext());
    prefs
        .getString(R.string.settings_key_night_mode, R.string.settings_default_night_mode_value)
        .set("follow_system");
    prefs
        .getBoolean(
            R.string.settings_key_night_mode_vibration_control, R.bool.settings_default_false)
        .set(true);
    Shadows.shadowOf(Looper.myLooper()).runToEndOfTasks();
    prefs
        .getInteger(
            R.string.settings_key_vibrate_on_key_press_duration_int,
            R.integer.settings_default_vibrate_on_key_press_duration_int)
        .set(10);
    ShadowVibrator shadowVibrator = Shadows.shadowOf(mImeServiceUnderTest.getVibrator());

    TestRxSchedulers.drainAllTasksUntilEnd();
    Shadows.shadowOf(Looper.myLooper()).runToEndOfTasks();
    Assert.assertFalse(shadowVibrator.isVibrating());

    resetVibratorState(shadowVibrator);
    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    assertVibrationInvoked(shadowVibrator);
    if (shadowVibrator.getMilliseconds() != 0) {
      Assert.assertEquals(10, shadowVibrator.getMilliseconds());
    }

    Shadows.shadowOf(Looper.myLooper()).runToEndOfTasks();

    NskApplicationBase application = getApplicationContext();
    application.onConfigurationChanged(configurationForNightMode(Configuration.UI_MODE_NIGHT_YES));

    TestRxSchedulers.drainAllTasksUntilEnd();
    Shadows.shadowOf(Looper.myLooper()).runToEndOfTasks();

    resetVibratorState(shadowVibrator);
    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    assertVibrationInvoked(shadowVibrator);
    if (shadowVibrator.getMilliseconds() != 0) {
      Assert.assertEquals(10, shadowVibrator.getMilliseconds());
    }

    Shadows.shadowOf(Looper.myLooper()).runToEndOfTasks();

    application.onConfigurationChanged(configurationForNightMode(Configuration.UI_MODE_NIGHT_NO));
    TestRxSchedulers.drainAllTasksUntilEnd();
    resetVibratorState(shadowVibrator);
    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    assertVibrationInvoked(shadowVibrator);
    if (shadowVibrator.getMilliseconds() != 0) {
      Assert.assertEquals(10, shadowVibrator.getMilliseconds());
    }

    Shadows.shadowOf(Looper.myLooper()).runToEndOfTasks();

    prefs
        .getBoolean(
            R.string.settings_key_night_mode_vibration_control, R.bool.settings_default_false)
        .set(false);
    application.onConfigurationChanged(configurationForNightMode(Configuration.UI_MODE_NIGHT_YES));

    TestRxSchedulers.drainAllTasksUntilEnd();
    Shadows.shadowOf(Looper.myLooper()).runToEndOfTasks();

    resetVibratorState(shadowVibrator);
    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    assertVibrationInvoked(shadowVibrator);
    if (shadowVibrator.getMilliseconds() != 0) {
      Assert.assertEquals(10, shadowVibrator.getMilliseconds());
    }
  }

  @Test
  public void testDoesNotPlaysSoundIfDisabled() {
    ShadowNskAudioManager shadowAudioManager =
        (ShadowNskAudioManager) Shadows.shadowOf(mImeServiceUnderTest.getAudioManager());
    // consuming demo - if one took place at start up
    shadowAudioManager.getLastPlaySoundEffectType();

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_sound_on, false);
    // no demo here
    Assert.assertEquals(Integer.MIN_VALUE, shadowAudioManager.getLastPlaySoundEffectType());

    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    Assert.assertEquals(Integer.MIN_VALUE, shadowAudioManager.getLastPlaySoundEffectType());

    mImeServiceUnderTest.onPress('j');
    Assert.assertEquals(Integer.MIN_VALUE, shadowAudioManager.getLastPlaySoundEffectType());

    mImeServiceUnderTest.onPress(KeyCodes.ENTER);
    Assert.assertEquals(Integer.MIN_VALUE, shadowAudioManager.getLastPlaySoundEffectType());

    mImeServiceUnderTest.onPress(KeyCodes.DELETE);
    Assert.assertEquals(Integer.MIN_VALUE, shadowAudioManager.getLastPlaySoundEffectType());

    mImeServiceUnderTest.onPress(0);
    Assert.assertEquals(Integer.MIN_VALUE, shadowAudioManager.getLastPlaySoundEffectType());

    mImeServiceUnderTest.onPress(KeyCodes.SHIFT);
    Assert.assertEquals(Integer.MIN_VALUE, shadowAudioManager.getLastPlaySoundEffectType());
  }

  @Test
  @Config(sdk = {25, 26, 29})
  public void testDoesNotVibrateDisabled() {
    TestRxSchedulers.foregroundFlushAllJobs();
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_use_system_vibration, false);
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_vibrate_on_key_press_duration_int, 0);
    ShadowVibrator shadowVibrator = Shadows.shadowOf(mImeServiceUnderTest.getVibrator());

    mImeServiceUnderTest.onPress(0);
    Assert.assertFalse(shadowVibrator.isVibrating());

    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    Assert.assertFalse(shadowVibrator.isVibrating());
  }

  @Test
  @Config(sdk = {25, 26, 29})
  public void testVibrateWhenEnabled() {
    TestRxSchedulers.foregroundFlushAllJobs();
    final var prefs = NskApplicationBase.prefs(getApplicationContext());
    prefs
        .getBoolean(
            R.string.settings_key_use_system_vibration,
            R.bool.settings_default_use_system_vibration)
        .set(false);
    prefs
        .getInteger(
            R.string.settings_key_vibrate_on_key_press_duration_int,
            R.integer.settings_default_vibrate_on_key_press_duration_int)
        .set(10);
    ShadowVibrator shadowVibrator = Shadows.shadowOf(mImeServiceUnderTest.getVibrator());

    TestRxSchedulers.drainAllTasksUntilEnd();
    resetVibratorState(shadowVibrator);
    mImeServiceUnderTest.onPress(0);
    assertVibrationInvoked(shadowVibrator);
    if (shadowVibrator.getMilliseconds() != 0) {
      Assert.assertEquals(10, shadowVibrator.getMilliseconds());
    }

    resetVibratorState(shadowVibrator);
    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    assertVibrationInvoked(shadowVibrator);
    if (shadowVibrator.getMilliseconds() != 0) {
      Assert.assertEquals(10, shadowVibrator.getMilliseconds());
    }
  }

  @Test
  @Config(sdk = {25, 26, 29})
  public void testDoesNotVibrateWhenPickingSuggestionManuallyIfDisabled() {
    TestRxSchedulers.foregroundFlushAllJobs();
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_use_system_vibration, false);
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_vibrate_on_key_press_duration_int, 0);

    ShadowVibrator shadowVibrator = Shadows.shadowOf(mImeServiceUnderTest.getVibrator());
    Shadows.shadowOf(Looper.myLooper()).runToEndOfTasks();
    Assert.assertFalse(shadowVibrator.isVibrating());

    mImeServiceUnderTest.pickSuggestionManually(0, "face");
    Assert.assertFalse(shadowVibrator.isVibrating());
  }

  @Test
  @Config(sdk = {25, 26, 29})
  public void testVibrateWhenPickingSuggestionManuallyIfEnabled() {
    TestRxSchedulers.foregroundFlushAllJobs();
    final var prefs = NskApplicationBase.prefs(getApplicationContext());
    prefs
        .getBoolean(
            R.string.settings_key_use_system_vibration,
            R.bool.settings_default_use_system_vibration)
        .set(false);
    prefs
        .getInteger(
            R.string.settings_key_vibrate_on_key_press_duration_int,
            R.integer.settings_default_vibrate_on_key_press_duration_int)
        .set(10);

    ShadowVibrator shadowVibrator = Shadows.shadowOf(mImeServiceUnderTest.getVibrator());
    TestRxSchedulers.drainAllTasksUntilEnd();
    Shadows.shadowOf(Looper.myLooper()).runToEndOfTasks();
    Assert.assertFalse(shadowVibrator.isVibrating());

    resetVibratorState(shadowVibrator);
    mImeServiceUnderTest.pickSuggestionManually(0, "face");
    assertVibrationInvoked(shadowVibrator);
    if (shadowVibrator.getMilliseconds() != 0) {
      Assert.assertEquals(10, shadowVibrator.getMilliseconds());
    }
  }

  @Test
  @Config(sdk = {29})
  public void testSystemVibrateWhenPickingSuggestionManuallyIfEnabled() {
    TestRxSchedulers.foregroundFlushAllJobs();
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_use_system_vibration, true);

    ShadowVibrator shadowVibrator = Shadows.shadowOf(mImeServiceUnderTest.getVibrator());
    Shadows.shadowOf(Looper.myLooper()).runToEndOfTasks();
    Assert.assertFalse(shadowVibrator.isVibrating());

    ShadowVibrator.reset();
    shadowVibrator.setHasVibrator(true);
    mImeServiceUnderTest.pickSuggestionManually(0, "face");
    Assert.assertEquals(VibrationEffect.EFFECT_CLICK, shadowVibrator.getEffectId());
  }

  @Test
  @Config(sdk = {29})
  public void testSystemVibrateWhenEnabled() {
    final Keyboard.Key key = Mockito.mock(Keyboard.Key.class);

    TestRxSchedulers.foregroundFlushAllJobs();
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_use_system_vibration, true);
    ShadowVibrator shadowVibrator = Shadows.shadowOf(mImeServiceUnderTest.getVibrator());
    // demo
    // milliseconds is set to -1 for pre-baked effects and setPrefsValue calls
    // foregroundFlushAllJobs, so the vibrator will already be finished vibrating
    // Assert.assertTrue(shadowVibrator.isVibrating());
    Assert.assertEquals(VibrationEffect.EFFECT_CLICK, shadowVibrator.getEffectId());
    Shadows.shadowOf(Looper.myLooper()).runToEndOfTasks();
    Assert.assertFalse(shadowVibrator.isVibrating());

    TestRxSchedulers.drainAllTasksUntilEnd();
    ShadowVibrator.reset();
    shadowVibrator.setHasVibrator(true);
    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    Assert.assertEquals(VibrationEffect.EFFECT_CLICK, shadowVibrator.getEffectId());
    Shadows.shadowOf(Looper.myLooper()).runToEndOfTasks();
    Assert.assertFalse(shadowVibrator.isVibrating());

    Mockito.doReturn(0).when(key).getPrimaryCode();
    ShadowVibrator.reset();
    shadowVibrator.setHasVibrator(true);
    mImeServiceUnderTest.onLongPressDone(key);
    Assert.assertNotEquals(
        "Expected some system vibration for long-press", 0, shadowVibrator.getMilliseconds());
    final int effectId0 = shadowVibrator.getEffectId();
    Assert.assertTrue(
        "Expected a system click effect for long-press. effectId=" + effectId0,
        effectId0 == VibrationEffect.EFFECT_HEAVY_CLICK
            || effectId0 == VibrationEffect.EFFECT_CLICK);

    Mockito.doReturn(KeyCodes.SPACE).when(key).getPrimaryCode();
    ShadowVibrator.reset();
    shadowVibrator.setHasVibrator(true);
    mImeServiceUnderTest.onLongPressDone(key);
    Assert.assertNotEquals(
        "Expected some system vibration for long-press", 0, shadowVibrator.getMilliseconds());
    final int effectId = shadowVibrator.getEffectId();
    Assert.assertTrue(
        "Expected a system click effect for long-press. effectId=" + effectId,
        effectId == VibrationEffect.EFFECT_HEAVY_CLICK || effectId == VibrationEffect.EFFECT_CLICK);
  }

  @Test
  @Config(sdk = {29})
  public void testSystemVibrationDoesNotAlsoVibrateWhenViewHapticFeedbackHandled() {
    final AtomicInteger hapticCalls = new AtomicInteger(0);
    ImePressEffects.sHapticFeedbackPerformer =
        (view, effect, flags) -> {
          hapticCalls.incrementAndGet();
          return true;
        };

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_use_system_vibration, true);
    TestRxSchedulers.drainAllTasksUntilEnd();

    final ShadowVibrator shadowVibrator = Shadows.shadowOf(mImeServiceUnderTest.getVibrator());
    resetVibratorState(shadowVibrator);
    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    Assert.assertTrue("Expected view haptics to be attempted.", hapticCalls.get() > 0);
    assertNoVibrationInvoked(shadowVibrator);

    try {
      SharedPrefsHelper.setPrefsValue(
          R.string.settings_key_system_vibration_fallback_duration_int, 20);
      TestRxSchedulers.drainAllTasksUntilEnd();
      resetVibratorState(shadowVibrator);
      mImeServiceUnderTest.onPress(KeyCodes.SPACE);
      assertVibrationInvoked(shadowVibrator);
    } finally {
      SharedPrefsHelper.setPrefsValue(
          R.string.settings_key_system_vibration_fallback_duration_int, 0);
    }
  }

  @Test
  @Config(sdk = {29})
  public void
      testSystemVibrationFallsBackToVibratorWhenSystemTouchHapticsDisabledEvenIfViewHandled() {
    final AtomicInteger hapticCalls = new AtomicInteger(0);
    ImePressEffects.sHapticFeedbackPerformer =
        (view, effect, flags) -> {
          hapticCalls.incrementAndGet();
          return true;
        };

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_use_system_vibration, true);
    setSystemWideHaptic(false);
    TestRxSchedulers.drainAllTasksUntilEnd();

    final ShadowVibrator shadowVibrator = Shadows.shadowOf(mImeServiceUnderTest.getVibrator());
    resetVibratorState(shadowVibrator);
    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    Assert.assertTrue("Expected view haptics to be attempted.", hapticCalls.get() > 0);
    assertVibrationInvoked(shadowVibrator);
  }

  @Test
  @Config(sdk = {29})
  public void testSystemVibrateWhenSystemHapticTouchDisabled() {
    TestRxSchedulers.foregroundFlushAllJobs();
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_use_system_vibration, true);
    ShadowVibrator shadowVibrator = Shadows.shadowOf(mImeServiceUnderTest.getVibrator());
    // demo
    // milliseconds is set to -1 for pre-baked effects and setPrefsValue calls
    // foregroundFlushAllJobs, so the vibrator will already be finished vibrating
    // Assert.assertTrue(shadowVibrator.isVibrating());
    Assert.assertEquals(VibrationEffect.EFFECT_CLICK, shadowVibrator.getEffectId());
    Shadows.shadowOf(Looper.myLooper()).runToEndOfTasks();
    Assert.assertFalse(shadowVibrator.isVibrating());

    ShadowVibrator.reset();
    shadowVibrator.setHasVibrator(true);
    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    Assert.assertEquals(VibrationEffect.EFFECT_CLICK, shadowVibrator.getEffectId());

    setSystemWideHaptic(false);
    Shadows.shadowOf(Looper.myLooper()).runToEndOfTasks();

    ShadowVibrator.reset();
    shadowVibrator.setHasVibrator(true);
    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    Assert.assertEquals(VibrationEffect.EFFECT_CLICK, shadowVibrator.getEffectId());

    setSystemWideHaptic(true);
    Shadows.shadowOf(Looper.myLooper()).runToEndOfTasks();

    ShadowVibrator.reset();
    shadowVibrator.setHasVibrator(true);
    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    Assert.assertEquals(VibrationEffect.EFFECT_CLICK, shadowVibrator.getEffectId());
  }

  @Test
  @Config(sdk = {25, 26, 29})
  public void testDoNotVibrateWhenLowPower() {
    TestRxSchedulers.foregroundFlushAllJobs();
    final var prefs = NskApplicationBase.prefs(getApplicationContext());
    prefs
        .getBoolean(
            R.string.settings_key_power_save_mode_vibration_control, R.bool.settings_default_false)
        .set(true);
    prefs
        .getInteger(
            R.string.settings_key_vibrate_on_key_press_duration_int,
            R.integer.settings_default_vibrate_on_key_press_duration_int)
        .set(10);
    ShadowVibrator shadowVibrator = Shadows.shadowOf(mImeServiceUnderTest.getVibrator());

    TestRxSchedulers.drainAllTasksUntilEnd();
    Shadows.shadowOf(Looper.myLooper()).runToEndOfTasks();
    Assert.assertFalse(shadowVibrator.isVibrating());

    resetVibratorState(shadowVibrator);
    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    assertVibrationInvoked(shadowVibrator);
    if (shadowVibrator.getMilliseconds() != 0) {
      Assert.assertEquals(10, shadowVibrator.getMilliseconds());
    }

    Shadows.shadowOf(Looper.myLooper()).runToEndOfTasks();

    PowerSavingTest.sendBatteryState(true);
    TestRxSchedulers.drainAllTasksUntilEnd();

    resetVibratorState(shadowVibrator);
    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    assertNoVibrationInvoked(shadowVibrator);

    Shadows.shadowOf(Looper.myLooper()).runToEndOfTasks();

    prefs
        .getBoolean(
            R.string.settings_key_power_save_mode_vibration_control, R.bool.settings_default_false)
        .set(false);
    TestRxSchedulers.drainAllTasksUntilEnd();

    resetVibratorState(shadowVibrator);
    mImeServiceUnderTest.onPress(KeyCodes.SPACE);
    assertVibrationInvoked(shadowVibrator);
    if (shadowVibrator.getMilliseconds() != 0) {
      Assert.assertEquals(10, shadowVibrator.getMilliseconds());
    }
  }

  @Test
  @Config(sdk = {25, 26, 29})
  public void testDoesNotLongPressVibrateDisabled() {
    final Keyboard.Key key = Mockito.mock(Keyboard.Key.class);

    TestRxSchedulers.foregroundFlushAllJobs();
    final var prefs = NskApplicationBase.prefs(getApplicationContext());
    prefs
        .getBoolean(
            R.string.settings_key_use_system_vibration,
            R.bool.settings_default_use_system_vibration)
        .set(false);
    prefs
        .getBoolean(
            R.string.settings_key_vibrate_on_long_press,
            R.bool.settings_default_vibrate_on_long_press)
        .set(false);
    TestRxSchedulers.drainAllTasksUntilEnd();
    Shadows.shadowOf(Looper.myLooper()).runToEndOfTasks();
    ShadowVibrator shadowVibrator = Shadows.shadowOf(mImeServiceUnderTest.getVibrator());

    Mockito.doReturn(0).when(key).getPrimaryCode();
    resetVibratorState(shadowVibrator);
    mImeServiceUnderTest.onLongPressDone(key);
    assertNoVibrationInvoked(shadowVibrator);
    Assert.assertEquals(0, shadowVibrator.getEffectId());

    Mockito.doReturn(KeyCodes.SPACE).when(key).getPrimaryCode();
    resetVibratorState(shadowVibrator);
    mImeServiceUnderTest.onLongPressDone(key);
    assertNoVibrationInvoked(shadowVibrator);
    Assert.assertEquals(0, shadowVibrator.getEffectId());
  }

  @Test
  @Config(sdk = {25, 26, 29})
  public void testVibrateLongPressWhenEnabled() {
    final Keyboard.Key key = Mockito.mock(Keyboard.Key.class);

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_vibrate_on_long_press, true);
    Shadows.shadowOf(Looper.myLooper()).runToEndOfTasks();
    final ShadowVibrator shadowVibrator = Shadows.shadowOf(mImeServiceUnderTest.getVibrator());
    Assert.assertFalse(shadowVibrator.isVibrating());

    Mockito.doReturn(0).when(key).getPrimaryCode();
    resetVibratorState(shadowVibrator);
    mImeServiceUnderTest.onLongPressDone(key);
    Assert.assertTrue(shadowVibrator.isVibrating());

    Mockito.doReturn(KeyCodes.SPACE).when(key).getPrimaryCode();
    resetVibratorState(shadowVibrator);
    mImeServiceUnderTest.onLongPressDone(key);
    Assert.assertTrue(shadowVibrator.isVibrating());
  }

  @Test
  public void testSetupActualKeyPreviewController() {
    simulateOnStartInputFlow();
    Mockito.verify(mImeServiceUnderTest.getSpiedKeyboardView())
        .setKeyPreviewController(Mockito.isA(KeyPreviewsManager.class));
  }

  @Test
  public void testNewKeyPreviewControllerOnInputViewReCreate() {
    simulateOnStartInputFlow();
    final ArgumentCaptor<KeyPreviewsController> captor =
        ArgumentCaptor.forClass(KeyPreviewsController.class);
    Mockito.verify(mImeServiceUnderTest.getSpiedKeyboardView())
        .setKeyPreviewController(captor.capture());
    final KeyPreviewsController firstInstance = captor.getValue();
    simulateFinishInputFlow();

    mImeServiceUnderTest.onCreateInputView();
    simulateOnStartInputFlow();
    Mockito.verify(mImeServiceUnderTest.getSpiedKeyboardView())
        .setKeyPreviewController(
            Mockito.argThat(argument -> argument != null && argument != firstInstance));
  }

  @Test
  public void testUsesNoOpKeyPreviewWhenDisabledInSettings() {
    simulateFinishInputFlow();
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_key_press_shows_preview_popup, false);

    simulateOnStartInputFlow();
    Mockito.verify(mImeServiceUnderTest.getSpiedKeyboardView())
        .setKeyPreviewController(Mockito.isA(NullKeyPreviewsManager.class));
  }

  @Test
  public void testUsesNoOpKeyPreviewWhenNoAnimations() {
    simulateFinishInputFlow();
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_tweak_animations_level, "none");
    Mockito.verify(mImeServiceUnderTest.getSpiedKeyboardView())
        .setKeyPreviewController(Mockito.isA(NullKeyPreviewsManager.class));
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_tweak_animations_level, "some");
    Mockito.verify(mImeServiceUnderTest.getSpiedKeyboardView(), Mockito.times(2))
        .setKeyPreviewController(Mockito.isA(KeyPreviewsManager.class));
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_tweak_animations_level, "full");
    Mockito.verify(mImeServiceUnderTest.getSpiedKeyboardView(), Mockito.times(3))
        .setKeyPreviewController(Mockito.isA(KeyPreviewsManager.class));
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_tweak_animations_level, "none");
    Mockito.verify(mImeServiceUnderTest.getSpiedKeyboardView(), Mockito.times(2))
        .setKeyPreviewController(Mockito.isA(NullKeyPreviewsManager.class));
  }

  @Test
  public void testUsesNoOpKeyPreviewWhenLowPower() {
    Mockito.verify(mImeServiceUnderTest.getSpiedKeyboardView())
        .setKeyPreviewController(Mockito.isA(KeyPreviewsManager.class));
    PowerSavingTest.sendBatteryState(true);
    Mockito.verify(mImeServiceUnderTest.getSpiedKeyboardView())
        .setKeyPreviewController(Mockito.isA(NullKeyPreviewsManager.class));
    PowerSavingTest.sendBatteryState(false);
    Mockito.verify(
            mImeServiceUnderTest.getSpiedKeyboardView(), Mockito.times(2 /*the second time*/))
        .setKeyPreviewController(Mockito.isA(KeyPreviewsManager.class));
  }

  private KeyPreviewsController getLastKeyPreviewController() {
    return ((TestableImeServicePressEffects) mImeServiceUnderTest).mLastController;
  }

  @Test
  public void testUsesCorrectPositionCalculatorForKeyPreview() {
    Assert.assertTrue(
        ((KeyPreviewsManager) getLastKeyPreviewController()).getPositionCalculator()
            instanceof AboveKeyPositionCalculator);

    SharedPrefsHelper.setPrefsValue(
        R.string.settings_key_key_press_preview_popup_position, "above_keyboard");
    Assert.assertTrue(
        ((KeyPreviewsManager) getLastKeyPreviewController()).getPositionCalculator()
            instanceof AboveKeyboardPositionCalculator);

    SharedPrefsHelper.setPrefsValue(
        R.string.settings_key_key_press_preview_popup_position, "above_key");
    Assert.assertTrue(
        ((KeyPreviewsManager) getLastKeyPreviewController()).getPositionCalculator()
            instanceof AboveKeyPositionCalculator);
  }

  @Test
  public void testKeyPreviewControllerIsDestroyWhenNewOneCreated() {
    final KeyPreviewsManager first = (KeyPreviewsManager) getLastKeyPreviewController();
    Mockito.verify(first, Mockito.never()).destroy();

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_key_press_shows_preview_popup, false);
    final KeyPreviewsController second = getLastKeyPreviewController();
    Assert.assertNotSame(first, second);
    Mockito.verify(first).destroy();
  }

  @Test
  public void testKeyPreviewIsNoOpWhenPasswordField() {
    simulateFinishInputFlow();
    simulateOnStartInputFlow(
        false,
        createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD));
    Mockito.verify(mImeServiceUnderTest.getSpiedKeyboardView())
        .setKeyPreviewController(Mockito.isA(NullKeyPreviewsManager.class));

    simulateFinishInputFlow();
    simulateOnStartInputFlow(
        false, createEditorInfo(EditorInfo.IME_ACTION_NONE, EditorInfo.TYPE_CLASS_TEXT));
    Mockito.verify(mImeServiceUnderTest.getSpiedKeyboardView(), Mockito.times(2))
        .setKeyPreviewController(Mockito.isA(KeyPreviewsManager.class));

    // does not re-create
    simulateFinishInputFlow();
    simulateOnStartInputFlow(
        false, createEditorInfo(EditorInfo.IME_ACTION_NONE, EditorInfo.TYPE_CLASS_TEXT));
    Mockito.verify(mImeServiceUnderTest.getSpiedKeyboardView(), Mockito.times(2))
        .setKeyPreviewController(Mockito.isA(KeyPreviewsManager.class));

    simulateFinishInputFlow();
    simulateOnStartInputFlow(
        false,
        createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD));
    Mockito.verify(mImeServiceUnderTest.getSpiedKeyboardView(), Mockito.times(2))
        .setKeyPreviewController(Mockito.isA(NullKeyPreviewsManager.class));

    // does not re-create
    simulateFinishInputFlow();
    simulateOnStartInputFlow(
        false,
        createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD));
    Mockito.verify(mImeServiceUnderTest.getSpiedKeyboardView(), Mockito.times(2))
        .setKeyPreviewController(Mockito.isA(NullKeyPreviewsManager.class));

    simulateFinishInputFlow();
    simulateOnStartInputFlow(
        false, createEditorInfo(EditorInfo.IME_ACTION_NONE, EditorInfo.TYPE_CLASS_TEXT));
    Mockito.verify(mImeServiceUnderTest.getSpiedKeyboardView(), Mockito.times(3))
        .setKeyPreviewController(Mockito.isA(KeyPreviewsManager.class));

    simulateFinishInputFlow();
    simulateOnStartInputFlow(
        false,
        createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD));
    Mockito.verify(mImeServiceUnderTest.getSpiedKeyboardView(), Mockito.times(3))
        .setKeyPreviewController(Mockito.isA(NullKeyPreviewsManager.class));

    simulateFinishInputFlow();
    simulateOnStartInputFlow(
        false, createEditorInfo(EditorInfo.IME_ACTION_NONE, EditorInfo.TYPE_CLASS_TEXT));
    Mockito.verify(mImeServiceUnderTest.getSpiedKeyboardView(), Mockito.times(4))
        .setKeyPreviewController(Mockito.isA(KeyPreviewsManager.class));

    simulateFinishInputFlow();
    simulateOnStartInputFlow(
        false,
        createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD));
    Mockito.verify(mImeServiceUnderTest.getSpiedKeyboardView(), Mockito.times(4))
        .setKeyPreviewController(Mockito.isA(NullKeyPreviewsManager.class));

    simulateFinishInputFlow();
    simulateOnStartInputFlow(
        false, createEditorInfo(EditorInfo.IME_ACTION_NONE, EditorInfo.TYPE_CLASS_NUMBER));
    Mockito.verify(mImeServiceUnderTest.getSpiedKeyboardView(), Mockito.times(5))
        .setKeyPreviewController(Mockito.isA(KeyPreviewsManager.class));
  }

  @Test
  public void testNewThemeReCreateController() {
    final KeyboardThemeFactory keyboardThemeFactory =
        NskApplicationBase.getKeyboardThemeFactory(getApplicationContext());
    final KeyPreviewsController first = getLastKeyPreviewController();
    keyboardThemeFactory.setAddOnEnabled("55d9797c-850c-40a8-9a5d-7467b55bd537", true);
    Assert.assertNotSame(first, getLastKeyPreviewController());
  }

  private static class TestableImeServicePressEffects extends TestableImeService {
    KeyPreviewsController mLastController;

    @Override
    protected void onNewControllerOrInputView(
        KeyPreviewsController controller, InputViewBinder inputViewBinder) {
      mLastController = Mockito.spy(controller);
      super.onNewControllerOrInputView(mLastController, inputViewBinder);
    }
  }

  /*
     public static class FakeSystemSettingsContentProvider extends AbstractProvider {

         @Override
         public String getAuthority() {
             return Settings.System.CONTENT_URI.getAuthority();
         }

         @Table
         public static class System {
             @Column(value = Column.FieldType.INTEGER, primaryKey = true)
             public static final String KEY_ID = Settings.System._ID;
             private static final int KEY_ID_VALUE = 1;

             @Column(Column.FieldType.INTEGER)
             public static final String HAPTIC_FEEDBACK_ENABLED = Settings.System.HAPTIC_FEEDBACK_ENABLED;
         }

         @Override
         public boolean onCreate() {
             final boolean create = super.onCreate();
             ContentValues contentValues = new ContentValues();
             contentValues.put(FakeSystemSettingsContentProvider.System.KEY_ID, 0);
             contentValues.put(FakeSystemSettingsContentProvider.System.HAPTIC_FEEDBACK_ENABLED, 1);
             insert(Settings.System.CONTENT_URI, contentValues);
             TestRxSchedulers.drainAllTasks();
             return create;
         }

         public void setHapticEnabled(boolean hapticEnabled) {
             final int enabledInt = hapticEnabled? 1 : 0;
             ContentValues contentValues = new ContentValues();
             contentValues.put(FakeSystemSettingsContentProvider.System.KEY_ID, 0);
             contentValues.put(FakeSystemSettingsContentProvider.System.HAPTIC_FEEDBACK_ENABLED, enabledInt);
             update(Settings.System.CONTENT_URI, contentValues, FakeSystemSettingsContentProvider.System.KEY_ID + "=" + FakeSystemSettingsContentProvider.System.KEY_ID_VALUE, null);

             Settings.System.putInt(
                     ApplicationProvider.getApplicationContext().getContentResolver(),
                     Settings.System.HAPTIC_FEEDBACK_ENABLED,
                     enabledInt);

             TestRxSchedulers.drainAllTasks();
         }
     }

  */
}
