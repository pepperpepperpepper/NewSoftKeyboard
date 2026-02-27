package wtf.uhoh.newsoftkeyboard.app.devicespecific;

import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.annotation.Config;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
@Config(sdk = 33)
public class PressVibratorV33Test {

  @Test
  public void vibrateUsesLegacyVibrationInCustomMode() {
    final Vibrator vibrator = Mockito.mock(Vibrator.class);
    final PressVibratorV33 pressVibrator = new PressVibratorV33(vibrator);

    pressVibrator.setDuration(10);
    pressVibrator.vibrate(false);

    Mockito.verify(vibrator).vibrate(Mockito.any(VibrationEffect.class));
    Mockito.verify(vibrator, Mockito.never())
        .vibrate(Mockito.any(VibrationEffect.class), Mockito.any(VibrationAttributes.class));
  }

  @Test
  public void vibrateUsesTouchUsageWhenSystemKeyboardVibrationDisabled() {
    final Vibrator vibrator = Mockito.mock(Vibrator.class);
    final PressVibratorV33 pressVibrator = new PressVibratorV33(vibrator);

    pressVibrator.setUseSystemVibration(true, true);
    pressVibrator.setSystemKeyboardVibrationEnabled(false);
    pressVibrator.setDuration(10);
    pressVibrator.vibrate(false);

    final ArgumentCaptor<VibrationAttributes> captor =
        ArgumentCaptor.forClass(VibrationAttributes.class);
    Mockito.verify(vibrator).vibrate(Mockito.any(VibrationEffect.class), captor.capture());
    Assert.assertEquals(VibrationAttributes.USAGE_TOUCH, captor.getValue().getUsage());
  }

  @Test
  public void vibrateUsesTouchUsageWhenTouchHapticsEnabledEvenIfSystemKeyboardVibrationEnabled() {
    final Vibrator vibrator = Mockito.mock(Vibrator.class);
    final PressVibratorV33 pressVibrator = new PressVibratorV33(vibrator);

    pressVibrator.setUseSystemVibration(true, true);
    pressVibrator.setSystemKeyboardVibrationEnabled(true);
    pressVibrator.setDuration(10);
    pressVibrator.vibrate(false);

    final ArgumentCaptor<VibrationAttributes> captor =
        ArgumentCaptor.forClass(VibrationAttributes.class);
    Mockito.verify(vibrator).vibrate(Mockito.any(VibrationEffect.class), captor.capture());
    Assert.assertEquals(VibrationAttributes.USAGE_TOUCH, captor.getValue().getUsage());
  }

  @Test
  public void vibrateUsesImeUsageWhenTouchHapticsDisabledAndSystemKeyboardVibrationEnabled() {
    final Vibrator vibrator = Mockito.mock(Vibrator.class);
    final PressVibratorV33 pressVibrator = new PressVibratorV33(vibrator);

    pressVibrator.setUseSystemVibration(true, false);
    pressVibrator.setSystemKeyboardVibrationEnabled(true);
    pressVibrator.setDuration(10);
    pressVibrator.vibrate(false);

    final ArgumentCaptor<VibrationAttributes> captor =
        ArgumentCaptor.forClass(VibrationAttributes.class);
    Mockito.verify(vibrator).vibrate(Mockito.any(VibrationEffect.class), captor.capture());

    final int expectedUsage = expectedImeUsageOrTouch();
    Assert.assertEquals(expectedUsage, captor.getValue().getUsage());
  }

  @Test
  public void vibrateFallsBackToLegacyVibrateWhenVibrationAttributesAreRejected() {
    final Vibrator vibrator = Mockito.mock(Vibrator.class);
    Mockito.doThrow(new RuntimeException("no-vibration-attrs"))
        .when(vibrator)
        .vibrate(Mockito.any(VibrationEffect.class), Mockito.any(VibrationAttributes.class));

    final PressVibratorV33 pressVibrator = new PressVibratorV33(vibrator);

    pressVibrator.setUseSystemVibration(true, true);
    pressVibrator.setDuration(10);
    pressVibrator.vibrate(false);

    Mockito.verify(vibrator).vibrate(Mockito.any(VibrationEffect.class));
  }

  @Test
  public void vibrateFallbackUsesLegacyVibrateEvenInSystemMode() {
    final Vibrator vibrator = Mockito.mock(Vibrator.class);
    final PressVibratorV33 pressVibrator = new PressVibratorV33(vibrator);

    pressVibrator.setUseSystemVibration(true, true);
    pressVibrator.setDuration(10);
    pressVibrator.vibrateFallback(false);

    Mockito.verify(vibrator).vibrate(Mockito.any(VibrationEffect.class));
    Mockito.verify(vibrator, Mockito.never())
        .vibrate(Mockito.any(VibrationEffect.class), Mockito.any(VibrationAttributes.class));
  }

  @Test
  public void systemVibrationFallbackUsesLegacyVibrate() {
    final Vibrator vibrator = Mockito.mock(Vibrator.class);
    final PressVibratorV33 pressVibrator = new PressVibratorV33(vibrator);

    pressVibrator.setUseSystemVibration(true, true);
    pressVibrator.setSystemVibrationFallbackDuration(12);
    pressVibrator.vibrateSystemVibrationFallback();

    Mockito.verify(vibrator).vibrate(Mockito.any(VibrationEffect.class));
  }

  private static int expectedImeUsageOrTouch() {
    try {
      return VibrationAttributes.createForUsage(0x52).getUsage();
    } catch (Throwable ignored) {
      // Some builds may reject non-public usages. In that case, PressVibratorV33 falls back to
      // USAGE_TOUCH.
      return VibrationAttributes.USAGE_TOUCH;
    }
  }
}
