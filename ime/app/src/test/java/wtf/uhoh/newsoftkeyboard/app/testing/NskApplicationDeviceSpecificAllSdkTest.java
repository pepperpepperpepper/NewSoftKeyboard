package wtf.uhoh.newsoftkeyboard.app.testing;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.robolectric.annotation.Config.OLDEST_SDK;

import android.app.Application;
import android.os.Build;
import android.os.Vibrator;
import android.view.GestureDetector;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.annotation.Config;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.Clipboard;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.ClipboardV11;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.ClipboardV16;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.ClipboardV28;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.DeviceSpecific;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.DeviceSpecificV15;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.DeviceSpecificV16;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.DeviceSpecificV19;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.DeviceSpecificV24;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.DeviceSpecificV26;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.DeviceSpecificV28;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.DeviceSpecificV29;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.NskOnGestureListener;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.NskV19GestureDetector;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.NskV8GestureDetector;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.PressVibrator;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.PressVibratorV1;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.PressVibratorV26;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.PressVibratorV29;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.TestUtils;

@RunWith(NskRobolectricTestRunner.class)
public abstract class NskApplicationDeviceSpecificAllSdkTest {

  private final List<Class<? extends DeviceSpecific>> mExpectedDeviceSpecificClass =
      Arrays.asList(
          DeviceSpecificV15.class, // 0
          DeviceSpecificV15.class,
          DeviceSpecificV15.class,
          DeviceSpecificV15.class,
          DeviceSpecificV15.class,
          DeviceSpecificV15.class,
          DeviceSpecificV15.class,
          DeviceSpecificV15.class,
          DeviceSpecificV15.class,
          DeviceSpecificV15.class,
          DeviceSpecificV15.class,
          DeviceSpecificV15.class,
          DeviceSpecificV15.class,
          DeviceSpecificV15.class,
          DeviceSpecificV15.class,
          DeviceSpecificV15.class,
          DeviceSpecificV16.class, // 16
          DeviceSpecificV16.class,
          DeviceSpecificV16.class,
          DeviceSpecificV19.class, // 19
          DeviceSpecificV19.class, // 20
          DeviceSpecificV19.class,
          DeviceSpecificV19.class,
          DeviceSpecificV19.class,
          DeviceSpecificV24.class,
          DeviceSpecificV24.class,
          DeviceSpecificV26.class,
          DeviceSpecificV26.class,
          DeviceSpecificV28.class,
          DeviceSpecificV29.class,
          DeviceSpecificV29.class, // 30
          DeviceSpecificV29.class,
          DeviceSpecificV29.class,
          DeviceSpecificV29.class,
          DeviceSpecificV29.class,
          DeviceSpecificV29.class);

  private final List<Class<? extends Clipboard>> mExpectedClipboardClass =
      Arrays.asList(
          ClipboardV11.class, // 0
          ClipboardV11.class,
          ClipboardV11.class,
          ClipboardV11.class,
          ClipboardV11.class,
          ClipboardV11.class,
          ClipboardV11.class,
          ClipboardV11.class,
          ClipboardV11.class,
          ClipboardV11.class,
          ClipboardV11.class,
          ClipboardV11.class, // 11
          ClipboardV11.class,
          ClipboardV11.class,
          ClipboardV11.class,
          ClipboardV11.class,
          ClipboardV16.class, // 16
          ClipboardV16.class,
          ClipboardV16.class,
          ClipboardV16.class,
          ClipboardV16.class,
          ClipboardV16.class,
          ClipboardV16.class,
          ClipboardV16.class,
          ClipboardV16.class,
          ClipboardV16.class,
          ClipboardV16.class,
          ClipboardV16.class,
          ClipboardV28.class, // 28
          ClipboardV28.class,
          ClipboardV28.class, // 30
          ClipboardV28.class,
          ClipboardV28.class,
          ClipboardV28.class,
          ClipboardV28.class,
          ClipboardV28.class);

  private final List<Class<? extends GestureDetector>> mExpectedGestureDetectorClass =
      Arrays.asList(
          GestureDetector.class, // 0
          GestureDetector.class, // 1
          GestureDetector.class,
          GestureDetector.class,
          GestureDetector.class,
          GestureDetector.class,
          GestureDetector.class,
          GestureDetector.class,
          NskV8GestureDetector.class, // 8
          NskV8GestureDetector.class,
          NskV8GestureDetector.class, // 10
          NskV8GestureDetector.class, // 11
          NskV8GestureDetector.class,
          NskV8GestureDetector.class,
          NskV8GestureDetector.class, // 14
          NskV8GestureDetector.class,
          NskV8GestureDetector.class, // 16
          NskV8GestureDetector.class,
          NskV8GestureDetector.class,
          NskV19GestureDetector.class, // 19
          NskV19GestureDetector.class, // 20
          NskV19GestureDetector.class,
          NskV19GestureDetector.class,
          NskV19GestureDetector.class,
          NskV19GestureDetector.class,
          NskV19GestureDetector.class,
          NskV19GestureDetector.class,
          NskV19GestureDetector.class,
          NskV19GestureDetector.class,
          NskV19GestureDetector.class,
          NskV19GestureDetector.class, // 30
          NskV19GestureDetector.class,
          NskV19GestureDetector.class,
          NskV19GestureDetector.class,
          NskV19GestureDetector.class,
          NskV19GestureDetector.class);

  private final List<Class<? extends PressVibrator>> mExpectedPressVibratorClass =
      Arrays.asList(
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV1.class,
          PressVibratorV26.class,
          PressVibratorV26.class,
          PressVibratorV26.class,
          PressVibratorV29.class,
          PressVibratorV29.class, // 30
          PressVibratorV29.class,
          PressVibratorV29.class,
          PressVibratorV29.class,
          PressVibratorV29.class,
          PressVibratorV29.class);

  void testCreateDeviceSpecificImplementationImpl() {
    if (Build.VERSION.SDK_INT > 100) return; // FUTURE?

    final Application application = getApplicationContext();

    final DeviceSpecific deviceSpecific = NskApplicationBase.getDeviceSpecific();
    Assert.assertNotNull(deviceSpecific);
    Assert.assertSame(
        mExpectedDeviceSpecificClass.get(Build.VERSION.SDK_INT), deviceSpecific.getClass());

    Assert.assertEquals(deviceSpecific.getClass().getSimpleName(), deviceSpecific.getApiLevel());

    final Clipboard clipboard = deviceSpecific.createClipboard(application);
    Assert.assertNotNull(clipboard);
    Assert.assertSame(mExpectedClipboardClass.get(Build.VERSION.SDK_INT), clipboard.getClass());

    final GestureDetector gestureDetector =
        deviceSpecific.createGestureDetector(application, Mockito.mock(NskOnGestureListener.class));
    Assert.assertNotNull(gestureDetector);
    Assert.assertSame(
        mExpectedGestureDetectorClass.get(Build.VERSION.SDK_INT), gestureDetector.getClass());

    final PressVibrator pressVibrator =
        deviceSpecific.createPressVibrator(Mockito.mock(Vibrator.class));
    Assert.assertNotNull(pressVibrator);
    Assert.assertSame(
        mExpectedPressVibratorClass.get(Build.VERSION.SDK_INT), pressVibrator.getClass());
  }

  public static class NskApplicationDeviceSpecificAllSdkShard1Test
      extends NskApplicationDeviceSpecificAllSdkTest {
    @Test
    @Config(minSdk = OLDEST_SDK, maxSdk = 23)
    public void testCreateDeviceSpecificImplementation() {
      testCreateDeviceSpecificImplementationImpl();
    }
  }

  public static class NskApplicationDeviceSpecificAllSdkShard2Test
      extends NskApplicationDeviceSpecificAllSdkTest {
    @Test
    @Config(minSdk = 24, maxSdk = 28)
    public void testCreateDeviceSpecificImplementation() {
      testCreateDeviceSpecificImplementationImpl();
    }
  }

  public static class NskApplicationDeviceSpecificAllSdkShard3Test
      extends NskApplicationDeviceSpecificAllSdkTest {
    @Test
    @Config(minSdk = 29, maxSdk = TestUtils.LATEST_STABLE_API_LEVEL)
    public void testCreateDeviceSpecificImplementation() {
      testCreateDeviceSpecificImplementationImpl();
    }
  }
}
