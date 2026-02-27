package wtf.uhoh.newsoftkeyboard.app.ime;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import androidx.annotation.Nullable;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.debug.TestInputActivity;
import wtf.uhoh.newsoftkeyboard.app.devicespecific.PressVibrator;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

/**
 * Produces actionable logs for debugging "keypress haptics not working" reports on real devices.
 *
 * <p>Intended to run on AWS Device Farm via {@code devicefarm-smoke} (CLI-only).
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class DeviceFarmHapticsDiagnosticsTest {

  private static final String TAG = "DeviceFarmHaptics";
  private static final long UI_TIMEOUT_MS = 8000L;
  private static final long POLL_INTERVAL_MS = 150L;
  private static final int MAX_LOG_CHARS = 2000;

  private static void logToStdout(String message) {
    System.out.println(TAG + ": " + message);
  }

  private static void logInfo(String message) {
    Log.i(TAG, message);
    logToStdout(message);
  }

  private UiDevice mDevice;
  private ActivityScenario<TestInputActivity> mScenario;
  private String mImeComponent;

  private SharedPreferences mPrefs;
  @Nullable private SharedPrefsSnapshot mPrefsSnapshot;
  @Nullable private DeviceSettingSnapshot mDeviceSettingSnapshot;

  private PressVibrator mOriginalPressVibrator;
  private ImePressEffects.HapticFeedbackPerformer mOriginalPerformer;
  private final List<String> mEvents = Collections.synchronizedList(new ArrayList<>());

  @Before
  public void setUp() throws Exception {
    final Context appContext = ApplicationProvider.getApplicationContext();
    mPrefs = DirectBootAwareSharedPreferences.create(appContext);
    mPrefsSnapshot =
        SharedPrefsSnapshot.capture(
            mPrefs,
            appContext.getString(R.string.settings_key_night_mode),
            appContext.getString(R.string.settings_key_power_save_mode),
            appContext.getString(R.string.settings_key_use_system_vibration),
            appContext.getString(R.string.settings_key_vibrate_on_key_press_duration_int),
            appContext.getString(R.string.settings_key_system_vibration_fallback_duration_int));
    mDeviceSettingSnapshot = DeviceSettingSnapshot.capture(this::executeShellCommand);

    mPrefs
        .edit()
        .putString(appContext.getString(R.string.settings_key_night_mode), "never")
        .putString(appContext.getString(R.string.settings_key_power_save_mode), "never")
        .putBoolean(appContext.getString(R.string.settings_key_use_system_vibration), true)
        .putInt(appContext.getString(R.string.settings_key_vibrate_on_key_press_duration_int), 50)
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
    waitForImeInstance();

    // Ensure settings observers are initialized before we install logging wrappers.
    waitForSystemHapticsMode();
  }

  @After
  public void tearDown() {
    final SharedPreferences prefs = mPrefs;
    final SharedPrefsSnapshot prefsSnapshot = mPrefsSnapshot;
    final DeviceSettingSnapshot deviceSettingSnapshot = mDeviceSettingSnapshot;
    mPrefsSnapshot = null;
    mDeviceSettingSnapshot = null;

    if (mOriginalPressVibrator != null) {
      swapPressVibrator(waitForImeInstanceOrNull(), mOriginalPressVibrator);
      mOriginalPressVibrator = null;
    }
    ImePressEffects.sHapticFeedbackPerformer = mOriginalPerformer;
    mOriginalPerformer = null;

    if (mScenario != null) {
      mScenario.close();
      mScenario = null;
    }

    if (prefsSnapshot != null && prefs != null) {
      try {
        prefsSnapshot.restore(prefs);
      } catch (Throwable t) {
        Log.w(TAG, "Failed restoring app prefs snapshot: " + t);
      }
    }

    if (deviceSettingSnapshot != null) {
      try {
        deviceSettingSnapshot.restore(this::executeShellCommand);
      } catch (Throwable t) {
        Log.w(TAG, "Failed restoring device settings snapshot: " + t);
      }
    }
  }

  @Test
  public void smoke() throws Exception {
    final ImeServiceBase ime = waitForImeInstance();
    assertNotNull("IME instance did not become available.", ime);

    logDeviceSummary();
    logSystemHapticsSettings("initial");

    // Best-effort: force-enable the global haptics settings (different OEMs store these in
    // different namespaces). We still log after attempting to set them.
    bestEffortShell("settings put system haptic_feedback_enabled 1");
    bestEffortShell("settings put secure haptic_feedback_enabled 1");
    bestEffortShell("settings put system haptic_feedback_intensity 1");
    bestEffortShell("settings put secure haptic_feedback_intensity 1");
    bestEffortShell("settings put system keyboard_vibration_enabled 1");
    bestEffortShell("settings put secure keyboard_vibration_enabled 1");
    logSystemHapticsSettings("after-forcing-enabled");

    installLoggingWrappers(ime);

    // Scenario A: system vibration enabled, no vibrator fallback. This is the problematic mode for
    // some devices where performHapticFeedback(...) reports "handled" but produces no physical
    // haptics.
    setUseSystemVibrationPref(true);
    setSystemVibrationFallbackDurationPref(0);
    waitForUseSystemHapticsMode(true);
    runScenario(ime, "system-haptics-fallback0", 'a');
    runScenario(ime, "system-haptics-fallback0-code0", 0);
    runScenario(
        ime,
        "system-haptics-fallback0-forceTouchUsage",
        'a',
        /* keyboardVibrationEnabledOverride= */ false);
    runScenario(
        ime,
        "system-haptics-fallback0-forceTouchUsage-code0",
        0,
        /* keyboardVibrationEnabledOverride= */ false);

    // Scenario B: app-controlled vibration (non-system) using the duration slider.
    setUseSystemVibrationPref(false);
    setSystemVibrationFallbackDurationPref(0);
    setKeyPressDurationPref(50);
    waitForUseSystemHapticsMode(false);
    runScenario(ime, "custom-vibration-50ms", 'a');
    runScenario(ime, "custom-vibration-50ms-code0", 0);

    // Scenario C: system vibration with an explicit fallback duration (forces vibrator path too).
    setUseSystemVibrationPref(true);
    setSystemVibrationFallbackDurationPref(20);
    waitForUseSystemHapticsMode(true);
    waitForSystemVibrationFallbackDuration(20);
    runScenario(ime, "system-haptics-fallback20ms", 'a');
    runScenario(ime, "system-haptics-fallback20ms-code0", 0);
  }

  private void runScenario(ImeServiceBase ime, String name, int keyCode) {
    runScenario(ime, name, keyCode, null);
  }

  private void runScenario(
      ImeServiceBase ime, String name, int keyCode, Boolean keyboardVibrationEnabledOverride) {
    mEvents.clear();
    logInfo("scenario.begin: " + name);
    logImeInternalState("scenario." + name + ".before");
    VibratorManagerSnapshot beforeSnapshot = readVibratorManagerSnapshot();
    VibratorStateProbe probe = VibratorStateProbe.maybeInstall(readPressVibrator(ime));

    if (keyboardVibrationEnabledOverride != null) {
      logInfo(
          "scenario.override: "
              + name
              + " keyboardVibrationEnabled="
              + keyboardVibrationEnabledOverride);
      pressOnImeThreadWithKeyboardVibrationEnabledOverride(
          ime, keyCode, keyboardVibrationEnabledOverride);
    } else {
      pressOnImeThread(ime, keyCode);
    }
    SystemClock.sleep(250);

    if (probe != null) {
      probe.logSummary("scenario." + name);
      probe.close();
    }

    logImeInternalState("scenario." + name + ".after");
    VibratorManagerSnapshot afterSnapshot = readVibratorManagerSnapshot();

    logVibratorManagerDelta(name, beforeSnapshot, afterSnapshot);

    logInfo("scenario.summary: " + name + " events=" + mEvents.size());
    for (String event : mEvents) logInfo("scenario.event: " + name + " " + event);

    if (!hasHapticsAttempt(mEvents)) {
      fail("Scenario '" + name + "' did not attempt any haptics path (view/vibrator).");
    }

    logInfo("scenario.end: " + name);
  }

  private void installLoggingWrappers(ImeServiceBase ime) {
    mOriginalPerformer = ImePressEffects.sHapticFeedbackPerformer;
    ImePressEffects.sHapticFeedbackPerformer =
        (view, effect, flags) -> {
          boolean result = view.performHapticFeedback(effect, flags);
          String entry =
              "view.performHapticFeedback effect="
                  + effectName(effect)
                  + "("
                  + effect
                  + ") flags="
                  + flags
                  + " result="
                  + result
                  + " view="
                  + view.getClass().getSimpleName();
          mEvents.add(entry);
          logInfo(entry);
          return result;
        };

    final PressVibrator original = readPressVibrator(ime);
    mOriginalPressVibrator = original;
    swapPressVibrator(ime, new LoggingPressVibrator(original, mEvents));
  }

  private void pressOnImeThread(ImeServiceBase ime, int code) {
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              ImeServiceBase instance = ImeServiceBase.getInstance();
              assertNotNull("IME instance did not become available.", instance);
              instance.onPress(code);
            });
  }

  private void pressOnImeThreadWithKeyboardVibrationEnabledOverride(
      ImeServiceBase ime, int code, boolean keyboardVibrationEnabled) {
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              ImeServiceBase instance = ImeServiceBase.getInstance();
              assertNotNull("IME instance did not become available.", instance);

              boolean baselineKeyboardVibrationEnabled = true;
              try {
                Field field =
                    ImePressEffects.class.getDeclaredField("mSystemKeyboardVibrationEnabled");
                field.setAccessible(true);
                baselineKeyboardVibrationEnabled = field.getBoolean(instance);
              } catch (Exception e) {
                Log.w(TAG, "Failed reading baseline keyboard vibration enabled: " + e);
              }

              PressVibrator pv = readPressVibrator(instance);
              if (pv != null) {
                pv.setSystemKeyboardVibrationEnabled(keyboardVibrationEnabled);
              }
              instance.onPress(code);
              if (pv != null) {
                pv.setSystemKeyboardVibrationEnabled(baselineKeyboardVibrationEnabled);
              }
            });
  }

  private void logImeInternalState(String label) {
    final ImeServiceBase ime = waitForImeInstanceOrNull();
    if (ime == null) {
      Log.w(TAG, label + ": IME instance missing");
      return;
    }
    try {
      Field useSystem = ImePressEffects.class.getDeclaredField("mUseSystemHapticFeedback");
      useSystem.setAccessible(true);
      Field wideEnabled = ImePressEffects.class.getDeclaredField("mSystemWideHapticEnabled");
      wideEnabled.setAccessible(true);
      Field keyboardEnabled =
          ImePressEffects.class.getDeclaredField("mSystemKeyboardVibrationEnabled");
      keyboardEnabled.setAccessible(true);
      Field fallback = ImePressEffects.class.getDeclaredField("mSystemVibrationFallbackDuration");
      fallback.setAccessible(true);
      Field error = ImePressEffects.class.getDeclaredField("mVibratorError");
      error.setAccessible(true);

      PressVibrator pv = readPressVibrator(ime);
      String pressVibrator =
          pv == null
              ? "null"
              : (pv instanceof LoggingPressVibrator
                  ? ((LoggingPressVibrator) pv).delegateSimpleName()
                  : pv.getClass().getSimpleName());
      String vibratorService =
          pv == null || pv.getVibrator() == null ? "null" : pv.getVibrator().getClass().getName();
      String vibratorState = bestEffortIsVibrating(pv);
      logInfo(
          label
              + ": useSystemHaptics="
              + useSystem.getBoolean(ime)
              + " systemWideEnabled="
              + wideEnabled.getBoolean(ime)
              + " keyboardVibrationEnabled="
              + keyboardEnabled.getBoolean(ime)
              + " systemFallbackMs="
              + fallback.getInt(ime)
              + " vibratorError="
              + error.getBoolean(ime)
              + " pressVibrator="
              + pressVibrator
              + " hasVibrator="
              + (pv != null && pv.hasVibrator())
              + " vibratorService="
              + vibratorService
              + " vibratorState="
              + vibratorState);
    } catch (Exception e) {
      Log.w(TAG, label + ": failed reading IME internal state: " + e);
    }
  }

  private static String bestEffortIsVibrating(PressVibrator pv) {
    if (pv == null) return "unknown";
    if (pv.getVibrator() == null) return "no-vibrator";

    try {
      Method method = pv.getVibrator().getClass().getMethod("isVibrating");
      Object result = method.invoke(pv.getVibrator());
      if (result instanceof Boolean) return String.valueOf(result);
      return "unknown";
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause == null) return "error(InvocationTargetException)";
      return "error(" + cause.getClass().getSimpleName() + ")";
    } catch (NoSuchMethodException e) {
      return "unsupported";
    } catch (Throwable t) {
      return "error(" + t.getClass().getSimpleName() + ")";
    }
  }

  private static boolean hasHapticsAttempt(List<String> events) {
    for (String event : events) {
      if (event.startsWith("view.performHapticFeedback")) return true;
      if (event.startsWith("vibrator.vibrate ")) return true;
      if (event.startsWith("vibrator.vibrateFallback ")) return true;
      if (event.startsWith("vibrator.vibrateSystemVibrationFallback")) return true;
    }
    return false;
  }

  private void logDeviceSummary() {
    logInfo(
        "device: sdk="
            + Build.VERSION.SDK_INT
            + " release="
            + Build.VERSION.RELEASE
            + " manufacturer="
            + Build.MANUFACTURER
            + " model="
            + Build.MODEL
            + " brand="
            + Build.BRAND);

    logAppVibrationState();
  }

  private void logAppVibrationState() {
    final Context appContext = ApplicationProvider.getApplicationContext();
    final String pkg = appContext.getPackageName();
    final int uid = Process.myUid();
    final int permState =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            ? appContext.checkSelfPermission(Manifest.permission.VIBRATE)
            : PackageManager.PERMISSION_GRANTED;
    logInfo(
        "app: package="
            + pkg
            + " uid="
            + uid
            + " permission.VIBRATE="
            + (permState == PackageManager.PERMISSION_GRANTED));

    try {
      AppOpsManager appOps = (AppOpsManager) appContext.getSystemService(Context.APP_OPS_SERVICE);
      if (appOps == null) {
        Log.w(TAG, "appops: missing APP_OPS_SERVICE");
        return;
      }
      final String vibrateOp = AppOpsManager.permissionToOp(Manifest.permission.VIBRATE);
      if (vibrateOp == null || vibrateOp.trim().isEmpty()) {
        Log.w(TAG, "appops: permissionToOp(VIBRATE) returned null/empty");
        return;
      }
      int mode = appOps.checkOpNoThrow(vibrateOp, uid, pkg);
      logInfo("appops: " + vibrateOp + " mode=" + appOpsModeName(mode) + "(" + mode + ")");
    } catch (Throwable t) {
      Log.w(TAG, "appops: failed checking OPSTR_VIBRATE (" + t.getClass().getSimpleName() + ")");
    }
  }

  private static String appOpsModeName(int mode) {
    if (mode == AppOpsManager.MODE_ALLOWED) return "MODE_ALLOWED";
    if (mode == AppOpsManager.MODE_IGNORED) return "MODE_IGNORED";
    if (mode == AppOpsManager.MODE_ERRORED) return "MODE_ERRORED";
    if (mode == AppOpsManager.MODE_DEFAULT) return "MODE_DEFAULT";
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mode == AppOpsManager.MODE_FOREGROUND) {
      return "MODE_FOREGROUND";
    }
    return "MODE_" + mode;
  }

  private void logSystemHapticsSettings(String label) {
    logInfo(
        label
            + ": settings.system.haptic_feedback_enabled="
            + getSetting("system", "haptic_feedback_enabled"));
    logInfo(
        label
            + ": settings.secure.haptic_feedback_enabled="
            + getSetting("secure", "haptic_feedback_enabled"));
    logInfo(
        label
            + ": settings.system.haptic_feedback_intensity="
            + getSetting("system", "haptic_feedback_intensity"));
    logInfo(
        label
            + ": settings.secure.haptic_feedback_intensity="
            + getSetting("secure", "haptic_feedback_intensity"));
    logInfo(
        label
            + ": settings.system.keyboard_vibration_enabled="
            + getSetting("system", "keyboard_vibration_enabled"));
    logInfo(
        label
            + ": settings.secure.keyboard_vibration_enabled="
            + getSetting("secure", "keyboard_vibration_enabled"));
  }

  private String getSetting(String namespace, String key) {
    try {
      return executeShellCommand("settings get " + namespace + " " + key).trim();
    } catch (Exception e) {
      return "error(" + e.getClass().getSimpleName() + ")";
    }
  }

  private void bestEffortShell(String command) {
    try {
      executeShellCommand(command);
    } catch (Exception e) {
      Log.w(
          TAG, "best-effort shell failed: " + command + " (" + e.getClass().getSimpleName() + ")");
    }
  }

  private VibratorManagerSnapshot readVibratorManagerSnapshot() {
    final String out = bestEffortDumpsys("dumpsys vibrator_manager");
    if (out == null || out.trim().isEmpty()) {
      return new VibratorManagerSnapshot(0, null);
    }

    int matches = 0;
    String lastMatch = null;
    String pkg = getAppPackage();
    String needle = "opPkg=" + pkg;
    String[] lines = out.split("\\n");
    for (String line : lines) {
      if (line.contains(needle)) {
        matches++;
        lastMatch = line.trim();
      }
    }

    return new VibratorManagerSnapshot(matches, lastMatch);
  }

  private void logVibratorManagerDelta(
      String scenario, VibratorManagerSnapshot before, VibratorManagerSnapshot after) {
    logInfo(
        "scenario.vibrator_manager: "
            + scenario
            + " opPkgMatchesBefore="
            + before.mMatches
            + " opPkgMatchesAfter="
            + after.mMatches);
    if (before.mLastMatch != null) {
      logInfo(
          "scenario.vibrator_manager.before: " + scenario + " " + trimForLog(before.mLastMatch));
    }
    if (after.mLastMatch != null) {
      logInfo("scenario.vibrator_manager.after: " + scenario + " " + trimForLog(after.mLastMatch));
    }
  }

  private String bestEffortDumpsys(String command) {
    try {
      return executeShellCommand(command);
    } catch (Exception e) {
      Log.w(
          TAG,
          "best-effort dumpsys failed: " + command + " (" + e.getClass().getSimpleName() + ")");
      return null;
    }
  }

  private static String trimForLog(String text) {
    if (text == null) return "";
    if (text.length() <= MAX_LOG_CHARS) return text;
    return text.substring(0, MAX_LOG_CHARS) + "\n…(truncated)…";
  }

  private void waitForSystemHapticsMode() {
    final long deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MS;
    while (SystemClock.uptimeMillis() < deadline) {
      ImeServiceBase ime = ImeServiceBase.getInstance();
      if (ime != null && ime.isUsingSystemHapticFeedback()) return;
      SystemClock.sleep(POLL_INTERVAL_MS);
    }
    fail("Timed out waiting for system haptics mode to become active.");
  }

  private void waitForUseSystemHapticsMode(boolean expected) {
    final long deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MS;
    while (SystemClock.uptimeMillis() < deadline) {
      ImeServiceBase ime = ImeServiceBase.getInstance();
      if (ime != null && ime.isUsingSystemHapticFeedback() == expected) return;
      SystemClock.sleep(POLL_INTERVAL_MS);
    }
    fail("Timed out waiting for use system haptics mode to become " + expected + ".");
  }

  private void waitForSystemVibrationFallbackDuration(int expected) {
    final long deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MS;
    while (SystemClock.uptimeMillis() < deadline) {
      ImeServiceBase ime = ImeServiceBase.getInstance();
      if (ime == null) {
        SystemClock.sleep(POLL_INTERVAL_MS);
        continue;
      }
      try {
        Field field = ImePressEffects.class.getDeclaredField("mSystemVibrationFallbackDuration");
        field.setAccessible(true);
        int value = field.getInt(ime);
        if (value == expected) return;
      } catch (Exception e) {
        Log.w(TAG, "Failed reading fallback duration: " + e);
        break;
      }
      SystemClock.sleep(POLL_INTERVAL_MS);
    }
    fail("Timed out waiting for system vibration fallback duration to become " + expected + ".");
  }

  private void setUseSystemVibrationPref(boolean value) {
    final Context appContext = ApplicationProvider.getApplicationContext();
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(appContext);
    prefs
        .edit()
        .putBoolean(appContext.getString(R.string.settings_key_use_system_vibration), value)
        .apply();
  }

  private void setKeyPressDurationPref(int value) {
    final Context appContext = ApplicationProvider.getApplicationContext();
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(appContext);
    prefs
        .edit()
        .putInt(
            appContext.getString(R.string.settings_key_vibrate_on_key_press_duration_int), value)
        .apply();
  }

  private void setSystemVibrationFallbackDurationPref(int value) {
    final Context appContext = ApplicationProvider.getApplicationContext();
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(appContext);
    prefs
        .edit()
        .putInt(
            appContext.getString(R.string.settings_key_system_vibration_fallback_duration_int),
            value)
        .apply();
  }

  private PressVibrator readPressVibrator(ImeServiceBase ime) {
    try {
      Field field = ImePressEffects.class.getDeclaredField("mVibrator");
      field.setAccessible(true);
      return (PressVibrator) field.get(ime);
    } catch (Exception e) {
      throw new AssertionError("Failed reading IME press vibrator.", e);
    }
  }

  private void swapPressVibrator(ImeServiceBase ime, PressVibrator replacement) {
    if (ime == null) return;
    final AtomicBoolean done = new AtomicBoolean(false);
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              try {
                Field field = ImePressEffects.class.getDeclaredField("mVibrator");
                field.setAccessible(true);
                field.set(ime, replacement);
                done.set(true);
              } catch (Exception e) {
                throw new AssertionError("Failed swapping IME press vibrator.", e);
              }
            });
    if (!done.get()) {
      throw new AssertionError("Failed swapping IME press vibrator (did not complete).");
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

  private ImeServiceBase waitForImeInstanceOrNull() {
    return ImeServiceBase.getInstance();
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

  private static String effectName(int effect) {
    if (effect == HapticFeedbackConstants.KEYBOARD_TAP) return "KEYBOARD_TAP";
    if (effect == HapticFeedbackConstants.VIRTUAL_KEY) return "VIRTUAL_KEY";
    if (effect == HapticFeedbackConstants.LONG_PRESS) return "LONG_PRESS";
    return "UNKNOWN";
  }

  private static final class VibratorManagerSnapshot {
    private final int mMatches;
    private final String mLastMatch;

    VibratorManagerSnapshot(int matches, String lastMatch) {
      mMatches = matches;
      mLastMatch = lastMatch;
    }
  }

  private static final class SharedPrefsSnapshot {
    private final Map<String, Object> mValues = new HashMap<>();
    private final List<String> mMissingKeys = new ArrayList<>();

    static SharedPrefsSnapshot capture(SharedPreferences prefs, String... keys) {
      SharedPrefsSnapshot snapshot = new SharedPrefsSnapshot();
      if (prefs == null || keys == null) return snapshot;

      Map<String, ?> all = prefs.getAll();
      for (String key : keys) {
        if (key == null) continue;
        if (prefs.contains(key)) {
          Object value = all.get(key);
          if (value != null) {
            snapshot.mValues.put(key, value);
          } else {
            snapshot.mMissingKeys.add(key);
          }
        } else {
          snapshot.mMissingKeys.add(key);
        }
      }
      return snapshot;
    }

    void restore(SharedPreferences prefs) {
      if (prefs == null) return;
      SharedPreferences.Editor editor = prefs.edit();
      for (String key : mMissingKeys) {
        editor.remove(key);
      }
      for (Map.Entry<String, Object> entry : mValues.entrySet()) {
        putPreference(editor, entry.getKey(), entry.getValue());
      }
      editor.apply();
    }

    @SuppressWarnings("unchecked")
    private static void putPreference(SharedPreferences.Editor editor, String key, Object value) {
      if (value instanceof Boolean) {
        editor.putBoolean(key, (Boolean) value);
      } else if (value instanceof Integer) {
        editor.putInt(key, (Integer) value);
      } else if (value instanceof Long) {
        editor.putLong(key, (Long) value);
      } else if (value instanceof Float) {
        editor.putFloat(key, (Float) value);
      } else if (value instanceof String) {
        editor.putString(key, (String) value);
      } else if (value instanceof Set) {
        editor.putStringSet(key, (Set<String>) value);
      } else if (value != null) {
        editor.putString(key, String.valueOf(value));
      }
    }
  }

  private static final class DeviceSettingSnapshot {
    private final Map<String, String> mValues = new HashMap<>();

    interface ShellCommand {
      String run(String command) throws IOException;
    }

    static DeviceSettingSnapshot capture(ShellCommand shell) {
      DeviceSettingSnapshot snapshot = new DeviceSettingSnapshot();
      snapshot.captureValue(shell, "secure", "default_input_method");
      snapshot.captureValue(shell, "secure", "enabled_input_methods");
      snapshot.captureValue(shell, "secure", "show_ime_with_hard_keyboard");

      snapshot.captureValue(shell, "system", "haptic_feedback_enabled");
      snapshot.captureValue(shell, "secure", "haptic_feedback_enabled");
      snapshot.captureValue(shell, "system", "haptic_feedback_intensity");
      snapshot.captureValue(shell, "secure", "haptic_feedback_intensity");
      snapshot.captureValue(shell, "system", "keyboard_vibration_enabled");
      snapshot.captureValue(shell, "secure", "keyboard_vibration_enabled");
      return snapshot;
    }

    private void captureValue(ShellCommand shell, String namespace, String key) {
      String fullKey = namespace + ":" + key;
      try {
        final String value = shell.run("settings get " + namespace + " " + key);
        mValues.put(fullKey, value == null ? null : value.trim());
      } catch (Throwable t) {
        mValues.put(fullKey, null);
      }
    }

    void restore(ShellCommand shell) {
      restoreSetting(shell, "system", "haptic_feedback_enabled");
      restoreSetting(shell, "secure", "haptic_feedback_enabled");
      restoreSetting(shell, "system", "haptic_feedback_intensity");
      restoreSetting(shell, "secure", "haptic_feedback_intensity");
      restoreSetting(shell, "system", "keyboard_vibration_enabled");
      restoreSetting(shell, "secure", "keyboard_vibration_enabled");

      restoreSetting(shell, "secure", "enabled_input_methods");
      restoreSetting(shell, "secure", "show_ime_with_hard_keyboard");

      restoreDefaultInputMethod(shell);
    }

    private void restoreDefaultInputMethod(ShellCommand shell) {
      final String defaultIme = mValues.get("secure:default_input_method");
      if (defaultIme == null
          || defaultIme.trim().isEmpty()
          || "null".equalsIgnoreCase(defaultIme)) {
        restoreSetting(shell, "secure", "default_input_method");
        return;
      }

      try {
        shell.run("ime set --user 0 " + defaultIme.trim());
      } catch (Throwable t) {
        Log.w(TAG, "Failed restoring default IME via ime set: " + t);
        restoreSetting(shell, "secure", "default_input_method");
      }
    }

    private void restoreSetting(ShellCommand shell, String namespace, String key) {
      final String fullKey = namespace + ":" + key;
      final String value = mValues.get(fullKey);
      if (value == null) return;

      try {
        final String trimmed = value.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) {
          shell.run("settings delete " + namespace + " " + key);
        } else {
          shell.run("settings put " + namespace + " " + key + " " + trimmed);
        }
      } catch (Throwable t) {
        Log.w(TAG, "Failed restoring setting " + fullKey + ": " + t);
      }
    }

    @SuppressWarnings("unused")
    void logSnapshot() {
      for (Map.Entry<String, String> entry : mValues.entrySet()) {
        final String fullKey = entry.getKey();
        final String value = entry.getValue();
        Log.d(TAG, "snapshot.setting: " + fullKey + "=" + value);
      }
    }
  }

  private static final class VibratorStateProbe implements AutoCloseable {
    private static final long VIBRATION_OBSERVE_TIMEOUT_MS = 1200L;

    private final PressVibrator mPressVibrator;
    private final Object mListener;
    private final Method mRemoveMethod;
    private final AtomicBoolean mBecameActive;
    private final AtomicBoolean mBecameInactive;

    private VibratorStateProbe(
        PressVibrator pressVibrator,
        Object listener,
        Method removeMethod,
        AtomicBoolean becameActive,
        AtomicBoolean becameInactive) {
      mPressVibrator = pressVibrator;
      mListener = listener;
      mRemoveMethod = removeMethod;
      mBecameActive = becameActive;
      mBecameInactive = becameInactive;
    }

    static VibratorStateProbe maybeInstall(PressVibrator pressVibrator) {
      if (pressVibrator == null || pressVibrator.getVibrator() == null) return null;
      try {
        final Class<?> listenerClass =
            Class.forName("android.os.Vibrator$OnVibratorStateChangedListener");
        final AtomicBoolean becameActive = new AtomicBoolean(false);
        final AtomicBoolean becameInactive = new AtomicBoolean(false);

        InvocationHandler handler =
            (proxy, method, args) -> {
              if (!"onVibratorStateChanged".equals(method.getName())) return null;
              if (args == null || args.length != 1 || !(args[0] instanceof Boolean)) return null;
              final boolean active = (Boolean) args[0];
              if (active) {
                becameActive.set(true);
              } else {
                becameInactive.set(true);
              }
              return null;
            };
        final ClassLoader proxyLoader =
            listenerClass.getClassLoader() != null
                ? listenerClass.getClassLoader()
                : DeviceFarmHapticsDiagnosticsTest.class.getClassLoader();
        Object listener =
            Proxy.newProxyInstance(proxyLoader, new Class<?>[] {listenerClass}, handler);

        final Method addMethod =
            firstMethod(
                pressVibrator.getVibrator().getClass(),
                new String[] {"addVibratorStateListener", "addOnVibratorStateChangedListener"},
                new Class<?>[] {Executor.class, listenerClass});
        final Method removeMethod =
            firstMethod(
                pressVibrator.getVibrator().getClass(),
                new String[] {
                  "removeVibratorStateListener", "removeOnVibratorStateChangedListener"
                },
                new Class<?>[] {listenerClass});
        if (addMethod == null || removeMethod == null) return null;

        Executor direct = Runnable::run;
        addMethod.invoke(pressVibrator.getVibrator(), direct, listener);
        return new VibratorStateProbe(
            pressVibrator, listener, removeMethod, becameActive, becameInactive);
      } catch (InvocationTargetException e) {
        Throwable cause = e.getCause();
        Log.w(
            TAG,
            "vibrator.probe.install failed: "
                + (cause == null ? "InvocationTargetException" : cause.getClass().getSimpleName()));
        return null;
      } catch (Throwable t) {
        Log.w(TAG, "vibrator.probe.install failed: " + t.getClass().getSimpleName());
        return null;
      }
    }

    void logSummary(String label) {
      final long deadline = SystemClock.uptimeMillis() + VIBRATION_OBSERVE_TIMEOUT_MS;
      while (SystemClock.uptimeMillis() < deadline) {
        if (mBecameActive.get() && mBecameInactive.get()) break;
        SystemClock.sleep(POLL_INTERVAL_MS);
      }

      logInfo(
          label
              + ": vibrator.probe active="
              + mBecameActive.get()
              + " inactive="
              + mBecameInactive.get()
              + " pressVibratorHasVibrator="
              + mPressVibrator.hasVibrator());
    }

    @Override
    public void close() {
      if (mPressVibrator == null || mPressVibrator.getVibrator() == null) return;
      if (mListener == null || mRemoveMethod == null) return;
      try {
        mRemoveMethod.invoke(mPressVibrator.getVibrator(), mListener);
      } catch (Throwable ignored) {
        // best-effort; diagnostics only
      }
    }

    private static Method firstMethod(Class<?> type, String[] names, Class<?>[] args) {
      for (String name : names) {
        try {
          return type.getMethod(name, args);
        } catch (NoSuchMethodException ignored) {
          // try next
        }
      }
      return null;
    }
  }

  private static final class LoggingPressVibrator extends PressVibrator {
    private final PressVibrator mDelegate;
    private final List<String> mEvents;

    LoggingPressVibrator(PressVibrator delegate, List<String> events) {
      super(delegate.getVibrator());
      mDelegate = delegate;
      mEvents = events;
    }

    @Override
    public void setDuration(int duration) {
      mEvents.add("vibrator.setDuration " + duration);
      try {
        mDelegate.setDuration(duration);
      } catch (Throwable t) {
        String entry = "vibrator.setDuration.exception " + t.getClass().getSimpleName();
        mEvents.add(entry);
        Log.w(TAG, entry + ": " + t);
        throw t;
      }
    }

    @Override
    public void setLongPressDuration(int duration) {
      mEvents.add("vibrator.setLongPressDuration " + duration);
      try {
        mDelegate.setLongPressDuration(duration);
      } catch (Throwable t) {
        String entry = "vibrator.setLongPressDuration.exception " + t.getClass().getSimpleName();
        mEvents.add(entry);
        Log.w(TAG, entry + ": " + t);
        throw t;
      }
    }

    @Override
    public void setUseSystemVibration(boolean system, boolean systemWideHapticEnabled) {
      mEvents.add(
          "vibrator.setUseSystemVibration system="
              + system
              + " systemWideHapticEnabled="
              + systemWideHapticEnabled);
      try {
        mDelegate.setUseSystemVibration(system, systemWideHapticEnabled);
      } catch (Throwable t) {
        String entry = "vibrator.setUseSystemVibration.exception " + t.getClass().getSimpleName();
        mEvents.add(entry);
        Log.w(TAG, entry + ": " + t);
        throw t;
      }
    }

    @Override
    public void setSystemKeyboardVibrationEnabled(boolean enabled) {
      mEvents.add("vibrator.setSystemKeyboardVibrationEnabled enabled=" + enabled);
      try {
        mDelegate.setSystemKeyboardVibrationEnabled(enabled);
      } catch (Throwable t) {
        String entry =
            "vibrator.setSystemKeyboardVibrationEnabled.exception " + t.getClass().getSimpleName();
        mEvents.add(entry);
        Log.w(TAG, entry + ": " + t);
        throw t;
      }
    }

    @Override
    public void setSystemVibrationFallbackDuration(int duration) {
      mEvents.add("vibrator.setSystemVibrationFallbackDuration " + duration);
      try {
        mDelegate.setSystemVibrationFallbackDuration(duration);
      } catch (Throwable t) {
        String entry =
            "vibrator.setSystemVibrationFallbackDuration.exception " + t.getClass().getSimpleName();
        mEvents.add(entry);
        Log.w(TAG, entry + ": " + t);
        throw t;
      }
    }

    @Override
    public boolean hasVibrator() {
      try {
        boolean result = mDelegate.hasVibrator();
        mEvents.add("vibrator.hasVibrator " + result);
        return result;
      } catch (Throwable t) {
        String entry = "vibrator.hasVibrator.exception " + t.getClass().getSimpleName();
        mEvents.add(entry);
        Log.w(TAG, entry + ": " + t);
        throw t;
      }
    }

    @Override
    public void vibrate(boolean longPress) {
      mEvents.add("vibrator.vibrate longPress=" + longPress);
      try {
        mDelegate.vibrate(longPress);
      } catch (Throwable t) {
        String entry = "vibrator.vibrate.exception " + t.getClass().getSimpleName();
        mEvents.add(entry);
        Log.w(TAG, entry + ": " + t);
        throw t;
      }
    }

    @Override
    public void vibrateFallback(boolean longPress) {
      mEvents.add("vibrator.vibrateFallback longPress=" + longPress);
      try {
        mDelegate.vibrateFallback(longPress);
      } catch (Throwable t) {
        String entry = "vibrator.vibrateFallback.exception " + t.getClass().getSimpleName();
        mEvents.add(entry);
        Log.w(TAG, entry + ": " + t);
        throw t;
      }
    }

    @Override
    public void vibrateSystemVibrationFallback() {
      mEvents.add("vibrator.vibrateSystemVibrationFallback");
      try {
        mDelegate.vibrateSystemVibrationFallback();
      } catch (Throwable t) {
        String entry =
            "vibrator.vibrateSystemVibrationFallback.exception " + t.getClass().getSimpleName();
        mEvents.add(entry);
        Log.w(TAG, entry + ": " + t);
        throw t;
      }
    }

    String delegateSimpleName() {
      return mDelegate.getClass().getSimpleName();
    }
  }
}
