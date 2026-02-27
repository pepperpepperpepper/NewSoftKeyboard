package wtf.uhoh.newsoftkeyboard.app.ime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.debug.TestInputActivity;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.PressVibrator;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class KeyPressHapticsInstrumentedTest {

  private static final long UI_TIMEOUT_MS = 8000L;
  private static final long POLL_INTERVAL_MS = 150L;

  private UiDevice mDevice;
  private ActivityScenario<TestInputActivity> mScenario;
  private String mImeComponent;

  @Before
  public void setUp() throws Exception {
    final Context appContext = ApplicationProvider.getApplicationContext();
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(appContext);
    prefs
        .edit()
        .putString(appContext.getString(R.string.settings_key_night_mode), "never")
        .putString(appContext.getString(R.string.settings_key_power_save_mode), "never")
        .putBoolean(appContext.getString(R.string.settings_key_use_system_vibration), true)
        .putInt(
            appContext.getString(R.string.settings_key_system_vibration_fallback_duration_int), 0)
        .apply();

    mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    wakeAndUnlockDevice();
    mImeComponent = resolveImeComponentId();
    ensureImeEnabledAndSelected();

    mScenario = ActivityScenario.launch(TestInputActivity.class);
    waitForTestHarness();
    focusTestEditor();
    mScenario.onActivity(TestInputActivity::forceShowKeyboard);
    SystemClock.sleep(400);
    focusTestEditor();
    waitForKeyboardToBeReady();
    waitForImeInstance();
  }

  @After
  public void tearDown() {
    ImePressEffects.sHapticFeedbackPerformer = null;
    if (mScenario != null) {
      mScenario.close();
    }
  }

  @Test
  public void systemHapticsFallsBackToVirtualKeyWhenKeyboardTapFails() throws Exception {
    final ImeServiceBase ime = waitForImeInstance();
    assertNotNull("IME instance did not become available.", ime);
    waitForSystemHapticsMode(ime);

    final List<Integer> observedEffects = Collections.synchronizedList(new ArrayList<>());
    ImePressEffects.sHapticFeedbackPerformer =
        (view, effect, flags) -> {
          observedEffects.add(effect);
          // Simulate a device/runtime where KEYBOARD_TAP is unsupported but VIRTUAL_KEY works.
          return effect == HapticFeedbackConstants.VIRTUAL_KEY;
        };

    final PressVibrator throwingVibrator =
        new PressVibrator(null) {
          @Override
          public void setDuration(int duration) {}

          @Override
          public void setLongPressDuration(int duration) {}

          @Override
          public void setUseSystemVibration(boolean system, boolean systemWideHapticEnabled) {}

          @Override
          public boolean hasVibrator() {
            return true;
          }

          @Override
          public void vibrate(boolean longPress) {
            throw new RuntimeException("boom");
          }
        };

    final PressVibrator original = swapPressVibrator(ime, throwingVibrator);
    try {
      runOnImeThread(
          () -> {
            observedEffects.clear();
            ImeServiceBase instance = ImeServiceBase.getInstance();
            assertNotNull("IME instance did not become available.", instance);
            instance.onPress('a');
          });
    } finally {
      swapPressVibrator(ime, original);
    }

    assertEquals(
        Arrays.asList(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.VIRTUAL_KEY),
        observedEffects);
  }

  @Test
  public void keyPressWithZeroPrimaryCodeStillTriggersHaptics() throws Exception {
    final ImeServiceBase ime = waitForImeInstance();
    assertNotNull("IME instance did not become available.", ime);
    waitForSystemHapticsMode(ime);

    final AtomicBoolean vibrated = new AtomicBoolean(false);
    final List<Integer> observedEffects = Collections.synchronizedList(new ArrayList<>());
    final PressVibrator fakeVibrator =
        new PressVibrator(null) {
          @Override
          public void setDuration(int duration) {}

          @Override
          public void setLongPressDuration(int duration) {}

          @Override
          public void setUseSystemVibration(boolean system, boolean systemWideHapticEnabled) {}

          @Override
          public boolean hasVibrator() {
            return true;
          }

          @Override
          public void vibrate(boolean longPress) {
            vibrated.set(true);
          }
        };

    final PressVibrator original = swapPressVibrator(ime, fakeVibrator);
    try {
      ImePressEffects.sHapticFeedbackPerformer =
          (view, effect, flags) -> {
            observedEffects.add(effect);
            return false;
          };

      runOnImeThread(
          () -> {
            vibrated.set(false);
            observedEffects.clear();
            ImeServiceBase instance = ImeServiceBase.getInstance();
            assertNotNull("IME instance did not become available.", instance);
            instance.onPress(0);
          });

      assertTrue(
          "Expected a haptics attempt even when primaryCode is 0 (many output-text keys do not have"
              + " explicit key codes).",
          vibrated.get() || !observedEffects.isEmpty());
    } finally {
      swapPressVibrator(ime, original);
    }
  }

  @Test
  public void systemHapticsAlsoAttemptsVirtualKeyEvenIfKeyboardTapReportsHandled()
      throws Exception {
    final ImeServiceBase ime = waitForImeInstance();
    assertNotNull("IME instance did not become available.", ime);
    waitForSystemHapticsMode(ime);

    final List<Integer> observedEffects = Collections.synchronizedList(new ArrayList<>());
    ImePressEffects.sHapticFeedbackPerformer =
        (view, effect, flags) -> {
          observedEffects.add(effect);
          // Simulate a device/runtime where KEYBOARD_TAP reports success but produces no physical
          // haptics.
          // We still try VIRTUAL_KEY as an additional best-effort system effect.
          return effect == HapticFeedbackConstants.KEYBOARD_TAP;
        };

    final PressVibrator throwingVibrator =
        new PressVibrator(null) {
          @Override
          public void setDuration(int duration) {}

          @Override
          public void setLongPressDuration(int duration) {}

          @Override
          public void setUseSystemVibration(boolean system, boolean systemWideHapticEnabled) {}

          @Override
          public boolean hasVibrator() {
            return true;
          }

          @Override
          public void vibrate(boolean longPress) {
            throw new RuntimeException("boom");
          }
        };

    final PressVibrator original = swapPressVibrator(ime, throwingVibrator);
    try {
      runOnImeThread(
          () -> {
            observedEffects.clear();
            ImeServiceBase instance = ImeServiceBase.getInstance();
            assertNotNull("IME instance did not become available.", instance);
            instance.onPress('a');
          });
    } finally {
      swapPressVibrator(ime, original);
    }

    assertEquals(
        Arrays.asList(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.VIRTUAL_KEY),
        observedEffects);
  }

  @Test
  @SdkSuppress(minSdkVersion = 29)
  public void systemHapticsSkipsVibratorWhenViewHapticsHandledOnApi29Plus() throws Exception {
    final ImeServiceBase ime = waitForImeInstance();
    assertNotNull("IME instance did not become available.", ime);
    waitForSystemHapticsMode(ime);

    final AtomicBoolean vibrated = new AtomicBoolean(false);
    final List<Integer> observedEffects = Collections.synchronizedList(new ArrayList<>());
    final PressVibrator fakeVibrator =
        new PressVibrator(requireVibratorService(ime)) {
          @Override
          public void setDuration(int duration) {}

          @Override
          public void setLongPressDuration(int duration) {}

          @Override
          public void setUseSystemVibration(boolean system, boolean systemWideHapticEnabled) {}

          @Override
          public boolean hasVibrator() {
            return true;
          }

          @Override
          public void vibrate(boolean longPress) {
            vibrated.set(true);
          }
        };

    final PressVibrator original = swapPressVibrator(ime, fakeVibrator);
    try {
      ImePressEffects.sHapticFeedbackPerformer =
          (view, effect, flags) -> {
            observedEffects.add(effect);
            return true;
          };

      runOnImeThread(
          () -> {
            setSystemWideHapticsEnabled(ime, true);
            vibrated.set(false);
            observedEffects.clear();
            ImeServiceBase instance = ImeServiceBase.getInstance();
            assertNotNull("IME instance did not become available.", instance);
            instance.onPress('a');
          });

      if (vibrated.get()) {
        fail(
            "Expected vibrator effects to be skipped when view haptics report handled. This avoids"
                + " canceling a working view-haptics vibration with a suppressed vibrator path.");
      }
      assertEquals(
          Arrays.asList(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.VIRTUAL_KEY),
          observedEffects);
    } finally {
      swapPressVibrator(ime, original);
    }
  }

  @Test
  @SdkSuppress(minSdkVersion = 29)
  public void systemHapticsPrefersVibratorWhenSystemWideDisabledOnApi29Plus() throws Exception {
    final ImeServiceBase ime = waitForImeInstance();
    assertNotNull("IME instance did not become available.", ime);
    waitForSystemHapticsMode(ime);

    final AtomicBoolean vibrated = new AtomicBoolean(false);
    final List<Integer> observedEffects = Collections.synchronizedList(new ArrayList<>());
    final PressVibrator fakeVibrator =
        new PressVibrator(requireVibratorService(ime)) {
          @Override
          public void setDuration(int duration) {}

          @Override
          public void setLongPressDuration(int duration) {}

          @Override
          public void setUseSystemVibration(boolean system, boolean systemWideHapticEnabled) {}

          @Override
          public boolean hasVibrator() {
            return true;
          }

          @Override
          public void vibrate(boolean longPress) {
            vibrated.set(true);
          }
        };

    final PressVibrator original = swapPressVibrator(ime, fakeVibrator);
    try {
      ImePressEffects.sHapticFeedbackPerformer =
          (view, effect, flags) -> {
            observedEffects.add(effect);
            return true;
          };

      runOnImeThread(
          () -> {
            setSystemWideHapticsEnabled(ime, false);
            vibrated.set(false);
            observedEffects.clear();
            ImeServiceBase instance = ImeServiceBase.getInstance();
            assertNotNull("IME instance did not become available.", instance);
            instance.onPress('a');
          });

      if (!vibrated.get()) {
        fail("Expected vibrator to be preferred when system-wide touch haptics are disabled.");
      }
      assertEquals(Collections.emptyList(), observedEffects);
    } finally {
      swapPressVibrator(ime, original);
    }
  }

  @Test
  @SdkSuppress(minSdkVersion = 29)
  public void systemHapticsFallsBackToViewHapticsWhenNoVibratorOnApi29Plus() throws Exception {
    final ImeServiceBase ime = waitForImeInstance();
    assertNotNull("IME instance did not become available.", ime);
    waitForSystemHapticsMode(ime);

    final AtomicBoolean vibrated = new AtomicBoolean(false);
    final List<Integer> observedEffects = Collections.synchronizedList(new ArrayList<>());
    final PressVibrator fakeVibrator =
        new PressVibrator(null) {
          @Override
          public void setDuration(int duration) {}

          @Override
          public void setLongPressDuration(int duration) {}

          @Override
          public void setUseSystemVibration(boolean system, boolean systemWideHapticEnabled) {}

          @Override
          public boolean hasVibrator() {
            return false;
          }

          @Override
          public void vibrate(boolean longPress) {
            vibrated.set(true);
          }
        };

    final PressVibrator original = swapPressVibrator(ime, fakeVibrator);
    try {
      ImePressEffects.sHapticFeedbackPerformer =
          (view, effect, flags) -> {
            observedEffects.add(effect);
            return true;
          };

      runOnImeThread(
          () -> {
            setSystemWideHapticsEnabled(ime, false);
            vibrated.set(false);
            observedEffects.clear();
            ImeServiceBase instance = ImeServiceBase.getInstance();
            assertNotNull("IME instance did not become available.", instance);
            instance.onPress('a');
          });

      if (vibrated.get()) {
        fail("Expected vibrator to be skipped when the device has no vibrator.");
      }
      assertEquals(
          Arrays.asList(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.VIRTUAL_KEY),
          observedEffects);
    } finally {
      swapPressVibrator(ime, original);
    }
  }

  @Test
  @SdkSuppress(minSdkVersion = 29)
  public void systemHapticsFallsBackToVibratorWhenVibratorPreviouslyErroredOnApi29Plus()
      throws Exception {
    final ImeServiceBase ime = waitForImeInstance();
    assertNotNull("IME instance did not become available.", ime);
    waitForSystemHapticsMode(ime);

    final AtomicBoolean vibrated = new AtomicBoolean(false);
    final AtomicBoolean vibratedFallback = new AtomicBoolean(false);
    final PressVibrator fakeVibrator =
        new PressVibrator(requireVibratorService(ime)) {
          @Override
          public void setDuration(int duration) {}

          @Override
          public void setLongPressDuration(int duration) {}

          @Override
          public void setUseSystemVibration(boolean system, boolean systemWideHapticEnabled) {}

          @Override
          public boolean hasVibrator() {
            return true;
          }

          @Override
          public void vibrate(boolean longPress) {
            vibrated.set(true);
          }

          @Override
          public void vibrateFallback(boolean longPress) {
            vibratedFallback.set(true);
          }
        };

    final PressVibrator original = swapPressVibrator(ime, fakeVibrator);
    try {
      ImePressEffects.sHapticFeedbackPerformer = (view, effect, flags) -> false;

      runOnImeThread(
          () -> {
            setSystemWideHapticsEnabled(ime, true);
            setVibratorError(ime, true);
            vibrated.set(false);
            vibratedFallback.set(false);
            ImeServiceBase instance = ImeServiceBase.getInstance();
            assertNotNull("IME instance did not become available.", instance);
            instance.onPress('a');
          });

      if (!vibratedFallback.get()) {
        fail("Expected fallback vibration when view haptics fail and vibrator previously errored.");
      }
      if (vibrated.get()) {
        fail("Expected system vibration to be skipped when vibrator error is flagged.");
      }
    } finally {
      swapPressVibrator(ime, original);
      setVibratorError(ime, false);
    }
  }

  @Test
  public void systemHapticsAlsoVibratesWhenFallbackEnabledEvenIfViewHapticsSucceed()
      throws Exception {
    final ImeServiceBase ime = waitForImeInstance();
    assertNotNull("IME instance did not become available.", ime);
    waitForSystemHapticsMode(ime);

    setSystemVibrationFallbackDuration(ime, 30);

    final AtomicBoolean vibrated = new AtomicBoolean(false);
    final PressVibrator fakeVibrator =
        new PressVibrator(requireVibratorService(ime)) {
          @Override
          public void setDuration(int duration) {}

          @Override
          public void setLongPressDuration(int duration) {}

          @Override
          public void setUseSystemVibration(boolean system, boolean systemWideHapticEnabled) {}

          @Override
          public void vibrate(boolean longPress) {}

          @Override
          public void vibrateSystemVibrationFallback() {
            vibrated.set(true);
          }

          @Override
          public boolean hasVibrator() {
            // This test asserts that the IME attempts the explicit fallback vibration path even
            // when view haptics report success. Use a fake "vibrator present" result so the
            // ImePressEffects logic reaches the fallback call sites.
            return true;
          }
        };

    final PressVibrator original = swapPressVibrator(ime, fakeVibrator);
    try {
      ImePressEffects.sHapticFeedbackPerformer = (view, effect, flags) -> true;

      runOnImeThread(
          () -> {
            vibrated.set(false);
            ImeServiceBase instance = ImeServiceBase.getInstance();
            assertNotNull("IME instance did not become available.", instance);
            instance.onPress('a');
          });

      if (!vibrated.get()) {
        fail("Expected vibrator fallback even when system haptic-feedback reported success.");
      }
    } finally {
      swapPressVibrator(ime, original);
      setSystemVibrationFallbackDuration(ime, 0);
    }
  }

  private void runOnImeThread(Runnable runnable) {
    InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    SystemClock.sleep(150);
  }

  private PressVibrator swapPressVibrator(ImeServiceBase ime, PressVibrator replacement) {
    final AtomicBoolean done = new AtomicBoolean(false);
    final AtomicReference<PressVibrator> original = new AtomicReference<>(null);
    runOnImeThread(
        () -> {
          try {
            Field field = ImePressEffects.class.getDeclaredField("mVibrator");
            field.setAccessible(true);
            original.set((PressVibrator) field.get(ime));
            field.set(ime, replacement);
            done.set(true);
          } catch (Exception e) {
            throw new AssertionError("Failed swapping IME press vibrator.", e);
          }
        });
    if (!done.get()) {
      throw new AssertionError("Failed swapping IME press vibrator (did not complete).");
    }
    PressVibrator result = original.get();
    if (result == null) {
      throw new AssertionError("Original IME press vibrator was null.");
    }
    return result;
  }

  private static void setSystemWideHapticsEnabled(ImeServiceBase ime, boolean enabled) {
    try {
      Field field = ImePressEffects.class.getDeclaredField("mSystemWideHapticEnabled");
      field.setAccessible(true);
      field.setBoolean(ime, enabled);
    } catch (Exception e) {
      throw new AssertionError("Failed setting IME system wide haptics enabled.", e);
    }
  }

  private static void setVibratorError(ImeServiceBase ime, boolean errored) {
    try {
      Field field = ImePressEffects.class.getDeclaredField("mVibratorError");
      field.setAccessible(true);
      field.setBoolean(ime, errored);
    } catch (Exception e) {
      throw new AssertionError("Failed setting IME vibrator error state.", e);
    }
  }

  private void waitForSystemHapticsMode(ImeServiceBase ime) {
    final long deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MS;
    while (SystemClock.uptimeMillis() < deadline) {
      if (ime.isUsingSystemHapticFeedback()) return;
      SystemClock.sleep(POLL_INTERVAL_MS);
    }
    fail("Timed out waiting for system haptics mode to become enabled.");
  }

  private static Vibrator requireVibratorService(ImeServiceBase ime) {
    final Vibrator vibrator = ime.getVibrator();
    assertNotNull("Vibrator service was not available on this device/emulator.", vibrator);
    return vibrator;
  }

  private void setSystemVibrationFallbackDuration(ImeServiceBase ime, int duration) {
    final Context appContext = ApplicationProvider.getApplicationContext();
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(appContext);
    prefs
        .edit()
        .putInt(
            appContext.getString(R.string.settings_key_system_vibration_fallback_duration_int),
            duration)
        .apply();

    final long deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MS;
    while (SystemClock.uptimeMillis() < deadline) {
      if (readSystemVibrationFallbackDuration(ime) == duration) return;
      SystemClock.sleep(POLL_INTERVAL_MS);
    }
    fail("Timed out waiting for system vibration fallback duration to become " + duration + ".");
  }

  private static int readSystemVibrationFallbackDuration(ImeServiceBase ime) {
    try {
      Field field = ImePressEffects.class.getDeclaredField("mSystemVibrationFallbackDuration");
      field.setAccessible(true);
      return (int) field.get(ime);
    } catch (Exception e) {
      throw new AssertionError("Failed reading IME system vibration fallback duration.", e);
    }
  }

  private ImeServiceBase waitForImeInstance() {
    long timeout = SystemClock.uptimeMillis() + UI_TIMEOUT_MS;
    while (SystemClock.uptimeMillis() < timeout) {
      ImeServiceBase ime = ImeServiceBase.getInstance();
      if (ime != null) {
        return ime;
      }
      SystemClock.sleep(POLL_INTERVAL_MS);
    }
    throw new AssertionError("ImeServiceBase instance did not become available.");
  }

  private void waitForKeyboardToBeReady() {
    boolean keyboardVisible = mDevice.wait(Until.hasObject(By.pkg(getAppPackage())), UI_TIMEOUT_MS);
    if (!keyboardVisible) {
      fail("Keyboard window not detected for package " + getAppPackage());
    }
    SystemClock.sleep(250);
  }

  private void waitForTestHarness() {
    boolean editorVisible =
        mDevice.wait(Until.hasObject(By.res(getAppPackage(), "test_edit_text")), UI_TIMEOUT_MS);
    if (!editorVisible) {
      fail("Test harness editor not visible.");
    }
  }

  private void focusTestEditor() {
    UiObject2 editor =
        mDevice.wait(Until.findObject(By.res(getAppPackage(), "test_edit_text")), UI_TIMEOUT_MS);
    if (editor != null) {
      editor.click();
      SystemClock.sleep(250);
    } else {
      fail("Unable to locate test input editor.");
    }
  }

  private static String getAppPackage() {
    return InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName();
  }

  private void wakeAndUnlockDevice() throws IOException, RemoteException {
    mDevice.wakeUp();
    SystemClock.sleep(200);
    mDevice.pressMenu();
    SystemClock.sleep(200);
    mDevice.pressHome();
    SystemClock.sleep(200);
  }

  private void ensureImeEnabledAndSelected() throws IOException {
    String enableOutput = executeShellCommand("ime enable --user 0 " + mImeComponent).trim();
    if (enableOutput.contains("Unknown")) {
      throw new IOException("Failed to enable IME: " + enableOutput);
    }
    executeShellCommand("ime set --user 0 " + mImeComponent);
    String enabled = executeShellCommand("settings get secure enabled_input_methods").trim();
    String expanded = expandComponent(mImeComponent);
    if (!enabled.contains(mImeComponent) && !enabled.contains(expanded)) {
      String prefix = enabled.isEmpty() ? "" : enabled + ":";
      executeShellCommand(
          "settings put secure enabled_input_methods \"" + prefix + mImeComponent + "\"");
    }
    executeShellCommand("settings put secure show_ime_with_hard_keyboard 1");
    SystemClock.sleep(400);
  }

  private String resolveImeComponentId() throws IOException {
    String list = executeShellCommand("ime list -a -s").trim();
    String[] lines = list.split("\\n");
    String prefix = getAppPackage() + "/";
    String fallback = null;
    for (String line : lines) {
      String trimmed = line.trim();
      if (!trimmed.startsWith(prefix)) continue;
      if (trimmed.endsWith(".NewSoftKeyboardService")
          || trimmed.endsWith("/.NewSoftKeyboardService")) {
        return trimmed;
      }
      if (fallback == null) fallback = trimmed;
    }
    if (fallback != null) return fallback;
    throw new IOException("Unable to find NewSoftKeyboard IME in: " + list);
  }

  private static String expandComponent(String component) {
    String[] parts = component.split("/", 2);
    if (parts.length != 2) return component;
    String pkg = parts[0];
    String svc = parts[1];
    if (!svc.startsWith(".")) return component;
    return pkg + "/" + pkg + svc;
  }

  private String executeShellCommand(String command) throws IOException {
    ParcelFileDescriptor pfd =
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
    try (InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        BufferedReader bufferedReader = new BufferedReader(reader)) {
      StringBuilder builder = new StringBuilder();
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        builder.append(line).append('\n');
      }
      return builder.toString();
    }
  }
}
