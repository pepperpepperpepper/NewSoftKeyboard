package wtf.uhoh.newsoftkeyboard.app.ime;

import android.os.Vibrator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class ImePressEffectsTest {

  @Test
  public void selectBestVibrator_returnsNullWhenNoVibratorsProvided() {
    Assert.assertNull(ImePressEffects.selectBestVibrator(null, null));
  }

  @Test
  public void selectBestVibrator_prefersSystemWhenItReportsHardware() {
    final Vibrator systemVibrator = Mockito.mock(Vibrator.class);
    final Vibrator legacyVibrator = Mockito.mock(Vibrator.class);
    Mockito.when(systemVibrator.hasVibrator()).thenReturn(true);
    Mockito.when(legacyVibrator.hasVibrator()).thenReturn(true);

    Assert.assertSame(
        systemVibrator, ImePressEffects.selectBestVibrator(systemVibrator, legacyVibrator));
  }

  @Test
  public void selectBestVibrator_fallsBackToLegacyWhenSystemReportsNoHardware() {
    final Vibrator systemVibrator = Mockito.mock(Vibrator.class);
    final Vibrator legacyVibrator = Mockito.mock(Vibrator.class);
    Mockito.when(systemVibrator.hasVibrator()).thenReturn(false);
    Mockito.when(legacyVibrator.hasVibrator()).thenReturn(true);

    Assert.assertSame(
        legacyVibrator, ImePressEffects.selectBestVibrator(systemVibrator, legacyVibrator));
  }

  @Test
  public void selectBestVibrator_prefersSystemWhenLegacyReportsNoHardware() {
    final Vibrator systemVibrator = Mockito.mock(Vibrator.class);
    final Vibrator legacyVibrator = Mockito.mock(Vibrator.class);
    Mockito.when(systemVibrator.hasVibrator()).thenReturn(true);
    Mockito.when(legacyVibrator.hasVibrator()).thenReturn(false);

    Assert.assertSame(
        systemVibrator, ImePressEffects.selectBestVibrator(systemVibrator, legacyVibrator));
  }

  @Test
  public void selectBestVibrator_prefersSystemWhenNeitherReportsHardware() {
    final Vibrator systemVibrator = Mockito.mock(Vibrator.class);
    final Vibrator legacyVibrator = Mockito.mock(Vibrator.class);
    Mockito.when(systemVibrator.hasVibrator()).thenReturn(false);
    Mockito.when(legacyVibrator.hasVibrator()).thenReturn(false);

    Assert.assertSame(
        systemVibrator, ImePressEffects.selectBestVibrator(systemVibrator, legacyVibrator));
  }

  @Test
  public void selectBestVibrator_returnsLegacyWhenSystemIsNull() {
    final Vibrator legacyVibrator = Mockito.mock(Vibrator.class);
    Mockito.when(legacyVibrator.hasVibrator()).thenReturn(true);

    Assert.assertSame(legacyVibrator, ImePressEffects.selectBestVibrator(null, legacyVibrator));
  }

  @Test
  public void selectBestVibrator_returnsSystemWhenLegacyIsNull() {
    final Vibrator systemVibrator = Mockito.mock(Vibrator.class);
    Mockito.when(systemVibrator.hasVibrator()).thenReturn(false);

    Assert.assertSame(systemVibrator, ImePressEffects.selectBestVibrator(systemVibrator, null));
  }
}
