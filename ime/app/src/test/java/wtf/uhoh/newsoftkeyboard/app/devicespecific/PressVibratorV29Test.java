package wtf.uhoh.newsoftkeyboard.app.devicespecific;

import android.media.AudioAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.annotation.Config;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
@Config(sdk = 29)
public class PressVibratorV29Test {

  @Test
  public void vibratePrefersLegacyVibrationInCustomMode() {
    final Vibrator vibrator = Mockito.mock(Vibrator.class);
    final PressVibratorV29 pressVibrator = new PressVibratorV29(vibrator);

    pressVibrator.setDuration(10);
    pressVibrator.vibrate(false);

    Mockito.verify(vibrator).vibrate(Mockito.any(VibrationEffect.class));
    Mockito.verify(vibrator, Mockito.never())
        .vibrate(Mockito.any(VibrationEffect.class), Mockito.any(AudioAttributes.class));
  }

  @Test
  public void vibrateUsesAudioAttributesInSystemMode() {
    final Vibrator vibrator = Mockito.mock(Vibrator.class);
    final PressVibratorV29 pressVibrator = new PressVibratorV29(vibrator);

    pressVibrator.setUseSystemVibration(true, true);
    pressVibrator.setDuration(10);
    pressVibrator.vibrate(false);

    Mockito.verify(vibrator)
        .vibrate(Mockito.any(VibrationEffect.class), Mockito.any(AudioAttributes.class));
    Mockito.verify(vibrator, Mockito.never()).vibrate(Mockito.any(VibrationEffect.class));
  }

  @Test
  public void vibrateFallbackPrefersLegacyVibrationEvenInSystemMode() {
    final Vibrator vibrator = Mockito.mock(Vibrator.class);
    final PressVibratorV29 pressVibrator = new PressVibratorV29(vibrator);

    pressVibrator.setUseSystemVibration(true, true);
    pressVibrator.setDuration(10);
    pressVibrator.vibrateFallback(false);

    Mockito.verify(vibrator).vibrate(Mockito.any(VibrationEffect.class));
    Mockito.verify(vibrator, Mockito.never())
        .vibrate(Mockito.any(VibrationEffect.class), Mockito.any(AudioAttributes.class));
  }

  @Test
  public void systemVibrationFallbackPrefersLegacyVibration() {
    final Vibrator vibrator = Mockito.mock(Vibrator.class);
    final PressVibratorV29 pressVibrator = new PressVibratorV29(vibrator);

    pressVibrator.setSystemVibrationFallbackDuration(12);
    pressVibrator.vibrateSystemVibrationFallback();

    Mockito.verify(vibrator).vibrate(Mockito.any(VibrationEffect.class));
    Mockito.verify(vibrator, Mockito.never())
        .vibrate(Mockito.any(VibrationEffect.class), Mockito.any(AudioAttributes.class));
  }
}
