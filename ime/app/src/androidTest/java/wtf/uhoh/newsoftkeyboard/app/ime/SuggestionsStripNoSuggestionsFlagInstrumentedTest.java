package wtf.uhoh.newsoftkeyboard.app.ime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.view.inputmethod.EditorInfo;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.debug.TestInputTypesActivity;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SuggestionsStripNoSuggestionsFlagInstrumentedTest {

  private static final long UI_TIMEOUT_MS = 8000L;
  private static final long POLL_INTERVAL_MS = 150L;

  private UiDevice mDevice;
  private ActivityScenario<TestInputTypesActivity> mScenario;
  private String mImeComponent;

  @Before
  public void setUp() throws Exception {
    final Context appContext = ApplicationProvider.getApplicationContext();
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(appContext);
    prefs
        .edit()
        .putString(appContext.getString(R.string.settings_key_night_mode), "never")
        .putString(appContext.getString(R.string.settings_key_power_save_mode), "never")
        .putBoolean(appContext.getString(R.string.settings_key_show_suggestions), true)
        .apply();

    mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    wakeAndUnlockDevice();
    mImeComponent = resolveImeComponentId();
    ensureImeEnabledAndSelected();
  }

  @After
  public void tearDown() {
    if (mScenario != null) {
      mScenario.close();
      mScenario = null;
    }
  }

  @Test
  public void smoke() throws Exception {
    noSuggestionsIgnoredByDefaultShowsStrip();
    closeScenarioIfOpen();

    classlessNoSuggestionsIgnoredByDefaultShowsStrip();
    closeScenarioIfOpen();

    noSuggestionsRespectedWhenEnabledDisablesPredictionsButShowsStrip();
    closeScenarioIfOpen();

    classlessNoSuggestionsRespectedWhenEnabledDisablesPredictionsButShowsStrip();
    closeScenarioIfOpen();

    typeNullInputShowsStrip();
  }

  @Test
  public void noSuggestionsIgnoredByDefaultShowsStrip() throws Exception {
    setRespectNoSuggestionsFlag(false);

    launchHarnessAndShowKeyboard();
    focusEditor("test_edit_text_no_suggestions");

    waitForMainKeyboardVisible();
    assertCandidateStripVisible(true);
    assertNoSuggestionsActionVisible(false);
    assertCurrentEditorHasNoSuggestionsFlag(true);
    assertPredictionOn(true);
  }

  @Test
  public void noSuggestionsRespectedWhenEnabledDisablesPredictionsButShowsStrip() throws Exception {
    setRespectNoSuggestionsFlag(true);

    launchHarnessAndShowKeyboard();
    focusEditor("test_edit_text_no_suggestions");

    waitForMainKeyboardVisible();
    assertCandidateStripVisible(true);
    assertNoSuggestionsActionVisible(true);
    assertCurrentEditorHasNoSuggestionsFlag(true);
    assertPredictionOn(false);
    assertNoSuggestionsActionOpensTypingSettings();
  }

  @Test
  public void typeNullInputShowsStrip() throws Exception {
    setRespectNoSuggestionsFlag(false);

    launchHarnessAndShowKeyboard();
    focusEditor("test_edit_text_type_null");

    waitForMainKeyboardVisible();
    assertCandidateStripVisible(true);
    assertNoSuggestionsActionVisible(false);
    assertCurrentEditorInputType(0);
    assertPredictionOn(true);
  }

  @Test
  public void classlessNoSuggestionsIgnoredByDefaultShowsStrip() throws Exception {
    setRespectNoSuggestionsFlag(false);

    launchHarnessAndShowKeyboard();
    focusEditor("test_edit_text_classless_no_suggestions");

    waitForMainKeyboardVisible();
    assertCandidateStripVisible(true);
    assertNoSuggestionsActionVisible(false);
    assertCurrentEditorHasNoSuggestionsFlag(true);
    assertCurrentEditorHasTextClass(false);
    assertPredictionOn(true);
  }

  @Test
  public void classlessNoSuggestionsRespectedWhenEnabledDisablesPredictionsButShowsStrip()
      throws Exception {
    setRespectNoSuggestionsFlag(true);

    launchHarnessAndShowKeyboard();
    focusEditor("test_edit_text_classless_no_suggestions");

    waitForMainKeyboardVisible();
    assertCandidateStripVisible(true);
    assertNoSuggestionsActionVisible(true);
    assertCurrentEditorHasNoSuggestionsFlag(true);
    assertCurrentEditorHasTextClass(false);
    assertPredictionOn(false);
  }

  private void closeScenarioIfOpen() {
    if (mScenario != null) {
      mScenario.close();
      mScenario = null;
    }
  }

  private void setRespectNoSuggestionsFlag(boolean value) {
    final Context appContext = ApplicationProvider.getApplicationContext();
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(appContext);
    prefs
        .edit()
        .putBoolean(
            appContext.getString(R.string.settings_key_respect_app_no_suggestions_flag), value)
        .apply();
  }

  private void launchHarnessAndShowKeyboard() {
    mScenario = ActivityScenario.launch(TestInputTypesActivity.class);
    waitForEditorVisible("test_edit_text_plain");
    focusEditor("test_edit_text_plain");
    mScenario.onActivity(TestInputTypesActivity::forceShowKeyboard);
    SystemClock.sleep(400);
    waitForImeInstance();
  }

  private void waitForEditorVisible(String editorIdName) {
    boolean editorVisible =
        mDevice.wait(Until.hasObject(By.res(resId(editorIdName))), UI_TIMEOUT_MS);
    if (!editorVisible) {
      try {
        UiScrollable scrollable = new UiScrollable(new UiSelector().scrollable(true));
        scrollable.scrollIntoView(new UiSelector().resourceId(resId(editorIdName)));
        editorVisible = mDevice.wait(Until.hasObject(By.res(resId(editorIdName))), UI_TIMEOUT_MS);
      } catch (Throwable ignored) {
      }
    }
    if (!editorVisible) {
      fail("Test harness editor not visible for id: " + editorIdName);
    }
  }

  private void focusEditor(String editorIdName) {
    UiObject2 editor = mDevice.wait(Until.findObject(By.res(resId(editorIdName))), UI_TIMEOUT_MS);
    if (editor == null) {
      try {
        UiScrollable scrollable = new UiScrollable(new UiSelector().scrollable(true));
        scrollable.scrollIntoView(new UiSelector().resourceId(resId(editorIdName)));
        editor = mDevice.wait(Until.findObject(By.res(resId(editorIdName))), UI_TIMEOUT_MS);
      } catch (Throwable ignored) {
      }
    }
    if (editor == null) {
      fail("Unable to locate editor: " + editorIdName);
      return;
    }
    editor.click();
    SystemClock.sleep(250);
    if (mScenario != null) {
      mScenario.onActivity(TestInputTypesActivity::forceShowKeyboard);
      SystemClock.sleep(250);
    }
  }

  private void waitForMainKeyboardVisible() {
    boolean visible =
        mDevice.wait(Until.hasObject(By.res(resId("AnyKeyboardMainView"))), UI_TIMEOUT_MS);
    if (!visible) {
      fail("Keyboard main view not visible.");
    }
  }

  private void assertCandidateStripVisible(boolean expectedVisible) {
    final BySelector selector = By.res(resId("candidate_view"));
    final long deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MS;
    while (SystemClock.uptimeMillis() < deadline) {
      UiObject2 strip = mDevice.findObject(selector);
      boolean visible = strip != null;
      if (visible == expectedVisible) return;
      SystemClock.sleep(POLL_INTERVAL_MS);
    }
    fail("Expected candidate strip visible=" + expectedVisible);
  }

  private void assertNoSuggestionsActionVisible(boolean expectedVisible) {
    final BySelector selector = By.res(resId("no_suggestions_strip_action_root"));
    final long deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MS;
    while (SystemClock.uptimeMillis() < deadline) {
      UiObject2 action = mDevice.findObject(selector);
      boolean visible = action != null;
      if (visible == expectedVisible) return;
      SystemClock.sleep(POLL_INTERVAL_MS);
    }
    fail("Expected NO_SUGGESTIONS action visible=" + expectedVisible);
  }

  private void assertNoSuggestionsActionOpensTypingSettings() {
    final UiObject2 action =
        mDevice.wait(
            Until.findObject(By.res(resId("no_suggestions_strip_action_root"))), UI_TIMEOUT_MS);
    if (action == null) {
      fail("No suggestions strip action not visible to click.");
      return;
    }

    action.click();

    final Context context = ApplicationProvider.getApplicationContext();
    final String prefTitle = context.getString(R.string.respect_app_no_suggestions_flag_title);
    boolean typingPrefVisible = mDevice.wait(Until.hasObject(By.text(prefTitle)), UI_TIMEOUT_MS);
    if (!typingPrefVisible) {
      fail("Typing settings did not open or preference not visible: " + prefTitle);
      return;
    }

    mDevice.pressBack();
    // Best-effort UI cleanup. Depending on task affinity and device behavior, the settings screen
    // may be launched in a separate task, and a single BACK press may return to home instead of
    // the test harness activity.
    if (!mDevice.wait(Until.gone(By.text(prefTitle)), UI_TIMEOUT_MS)) {
      mDevice.pressBack();
      mDevice.wait(Until.gone(By.text(prefTitle)), UI_TIMEOUT_MS);
    }
  }

  private void assertCurrentEditorHasNoSuggestionsFlag(boolean expected) {
    final boolean[] hasFlag = {false};
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              ImeServiceBase ime = ImeServiceBase.getInstance();
              assertNotNull("IME instance did not become available.", ime);
              EditorInfo info = ime.getCurrentInputEditorInfo();
              assertNotNull("Current EditorInfo was null.", info);
              int flags = info.inputType & EditorInfo.TYPE_MASK_FLAGS;
              hasFlag[0] = (flags & EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0;
            });
    assertEquals("Expected NO_SUGGESTIONS flag mismatch.", expected, hasFlag[0]);
  }

  private void assertCurrentEditorInputType(int expectedInputType) {
    final int[] inputType = {0};
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              ImeServiceBase ime = ImeServiceBase.getInstance();
              assertNotNull("IME instance did not become available.", ime);
              EditorInfo info = ime.getCurrentInputEditorInfo();
              assertNotNull("Current EditorInfo was null.", info);
              inputType[0] = info.inputType;
            });
    assertEquals("Expected inputType mismatch.", expectedInputType, inputType[0]);
  }

  private void assertCurrentEditorHasTextClass(boolean expected) {
    final boolean[] hasTextClass = {false};
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              ImeServiceBase ime = ImeServiceBase.getInstance();
              assertNotNull("IME instance did not become available.", ime);
              EditorInfo info = ime.getCurrentInputEditorInfo();
              assertNotNull("Current EditorInfo was null.", info);
              int typeClass = info.inputType & EditorInfo.TYPE_MASK_CLASS;
              hasTextClass[0] = typeClass == EditorInfo.TYPE_CLASS_TEXT;
            });
    assertEquals("Expected inputType class mismatch.", expected, hasTextClass[0]);
  }

  private void assertPredictionOn(boolean expected) {
    final boolean[] predictionOn = {false};
    InstrumentationRegistry.getInstrumentation()
        .runOnMainSync(
            () -> {
              ImeServiceBase ime = ImeServiceBase.getInstance();
              assertNotNull("IME instance did not become available.", ime);
              predictionOn[0] = ime.isPredictionOn();
            });
    assertEquals("Prediction-on mismatch.", expected, predictionOn[0]);
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
    if (enableOutput.contains("Unknown") || enableOutput.contains("Error")) {
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

  private static String getAppPackage() {
    return InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName();
  }

  private static String resId(String idName) {
    return getAppPackage() + ":id/" + idName;
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
