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
@Config(sdk = 26)
public class PressVibratorV26Test {

  @Test
  public void vibratePrefersLegacyVibration() {
    final Vibrator vibrator = Mockito.mock(Vibrator.class);
    final PressVibratorV26 pressVibrator = new PressVibratorV26(vibrator);

    pressVibrator.setDuration(10);
    pressVibrator.vibrate(false);

    Mockito.verify(vibrator).vibrate(Mockito.any(VibrationEffect.class));
    Mockito.verify(vibrator, Mockito.never())
        .vibrate(Mockito.any(VibrationEffect.class), Mockito.any(AudioAttributes.class));
  }

  @Test
  public void systemVibrationFallbackPrefersLegacyVibration() {
    final Vibrator vibrator = Mockito.mock(Vibrator.class);
    final PressVibratorV26 pressVibrator = new PressVibratorV26(vibrator);

    pressVibrator.setSystemVibrationFallbackDuration(12);
    pressVibrator.vibrateSystemVibrationFallback();

    Mockito.verify(vibrator).vibrate(Mockito.any(VibrationEffect.class));
    Mockito.verify(vibrator, Mockito.never())
        .vibrate(Mockito.any(VibrationEffect.class), Mockito.any(AudioAttributes.class));
  }
}
