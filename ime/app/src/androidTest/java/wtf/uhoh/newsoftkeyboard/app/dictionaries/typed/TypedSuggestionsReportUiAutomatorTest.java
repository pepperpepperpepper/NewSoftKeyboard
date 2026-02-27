package wtf.uhoh.newsoftkeyboard.app.dictionaries.typed;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.widget.EditText;
import androidx.annotation.NonNull;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.debug.TestInputActivity;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateViewTestRegistry;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

/**
 * Produces a human-reviewable report for typed/current-word suggestions (completions/corrections).
 *
 * <p>Outputs a gzip+base64 encoded JSON blob on stdout between markers:
 *
 * <pre>
 * TYPED_SUGGESTIONS_REPORT_JSON_BEGIN
 * TSJSONGZ:000000:&lt;base64 chunk&gt;
 * TSJSONGZ:000001:&lt;base64 chunk&gt;
 * TYPED_SUGGESTIONS_REPORT_JSON_END
 * </pre>
 *
 * <p>This test is intentionally best-effort: it records timeouts/errors into the report instead of
 * failing fast.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class TypedSuggestionsReportUiAutomatorTest {

  private static final String TAG = "TypedSuggestRpt";

  public static final String REPORT_BEGIN = "TYPED_SUGGESTIONS_REPORT_JSON_BEGIN";
  public static final String REPORT_END = "TYPED_SUGGESTIONS_REPORT_JSON_END";
  private static final int REPORT_CHUNK_CHARS = 1800;

  private static final long READY_TIMEOUT_MS = 10_000L;
  private static final long SUGGESTIONS_TIMEOUT_MS = 6_000L;
  private static final long SHORT_WAIT_MS = 250L;

  private UiDevice mDevice;
  @Nullable private ActivityScenario<TestInputActivity> mScenario;
  private String mImeComponent;

  private static String getAppPackage() {
    return InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName();
  }

  private static String resId(String idName) {
    return getAppPackage() + ":id/" + idName;
  }

  @Before
  public void setUp() throws Exception {
    mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    clearLogcat();
    wakeAndUnlockDevice();

    ensureImeEnabledAndSelected();
    assertImeSelected();

    final Context context = ApplicationProvider.getApplicationContext();
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(context);
    prefs
        .edit()
        .putBoolean(context.getString(R.string.settings_key_show_suggestions), true)
        // For report purposes, always record suggestions even in fields marked NO_SUGGESTIONS.
        .putBoolean(context.getString(R.string.settings_key_respect_app_no_suggestions_flag), false)
        .putBoolean(context.getString(R.string.settings_key_quick_fix), true)
        .putBoolean(context.getString(R.string.settings_key_quick_fix_second_disabled), false)
        // Avoid non-determinism from device contacts.
        .putBoolean(context.getString(R.string.settings_key_use_contacts_dictionary), false)
        .apply();
  }

  @After
  public void tearDown() {
    closeScenarioIfOpen();
  }

  @Test
  public void generateTypedSuggestionsReport() throws Exception {
    final Context context = ApplicationProvider.getApplicationContext();

    final JSONObject root = new JSONObject();
    root.put("schemaVersion", 1);
    final java.text.SimpleDateFormat fmt =
        new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    fmt.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
    root.put("exportedAtUtc", fmt.format(new java.util.Date()));

    final JSONObject meta = new JSONObject();
    meta.put("device_model", Build.MODEL);
    meta.put("device_manufacturer", Build.MANUFACTURER);
    meta.put("android_sdk", Build.VERSION.SDK_INT);
    meta.put("android_release", Build.VERSION.RELEASE);
    meta.put("build_fingerprint", Build.FINGERPRINT);
    meta.put("device", (Build.MANUFACTURER + " " + Build.MODEL).trim());
    meta.put("android", Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")");
    meta.put("lang", Locale.getDefault().toString());

    try {
      final android.content.pm.PackageInfo info =
          context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
      meta.put("app_version_name", info.versionName);
      if (Build.VERSION.SDK_INT >= 28) {
        meta.put("app_version_code", info.getLongVersionCode());
      } else {
        //noinspection deprecation
        meta.put("app_version_code", info.versionCode);
      }
    } catch (Exception ignored) {
      // best-effort
    }
    root.put("meta", meta);

    final String onlyCaseIdArg =
        InstrumentationRegistry.getArguments().getString("onlyCaseId", "").trim();
    final java.util.Set<String> onlyCaseIds = new java.util.LinkedHashSet<>();
    if (!onlyCaseIdArg.isEmpty()) {
      for (String raw : onlyCaseIdArg.split("[,\\s]+")) {
        if (raw == null) continue;
        final String token = raw.trim();
        if (!token.isEmpty()) onlyCaseIds.add(token);
      }
      Log.i(TAG, "Filtering typed-suggestions report to cases: " + onlyCaseIds);
    }
    final boolean runAllCases = onlyCaseIds.isEmpty();

    final JSONArray cases = new JSONArray();
    for (TypedCase c : TypedCase.cases()) {
      if (!runAllCases && !onlyCaseIds.contains(c.id)) continue;
      cases.put(runCaseBestEffort(c));
    }
    root.put("cases", cases);

    final String pretty = root.toString(2);
    final byte[] gz = gzip(pretty);
    final String base64 = Base64.encodeToString(gz, Base64.NO_WRAP);
    Log.i(
        TAG,
        "Generated typed-suggestions report ("
            + pretty.length()
            + " chars, gzip="
            + gz.length
            + " bytes, base64="
            + base64.length()
            + " chars).");
    System.out.println(REPORT_BEGIN);
    int lineNo = 0;
    for (int start = 0; start < base64.length(); start += REPORT_CHUNK_CHARS) {
      final int end = Math.min(start + REPORT_CHUNK_CHARS, base64.length());
      System.out.println(
          String.format(Locale.US, "TSJSONGZ:%06d:%s", lineNo++, base64.substring(start, end)));
    }
    System.out.println(REPORT_END);
  }

  @NonNull
  private JSONObject runCaseBestEffort(@NonNull TypedCase c) {
    final JSONObject out = new JSONObject();
    try {
      out.put("id", c.id);
      out.put("title", c.title);
      out.put("noPersonalizedLearning", c.noPersonalizedLearning);
      out.put("noSuggestionsFlag", c.noSuggestionsFlag);
      if (c.editorInputTypeOverride >= 0) {
        out.put("editorInputTypeOverride", c.editorInputTypeOverride);
      }
      if (c.preseedCommittedText != null) {
        out.put("preseedCommittedText", c.preseedCommittedText);
      }
      out.put("typeText", c.typeText);

      closeScenarioIfOpen();
      launchTestHarness(
          c.noPersonalizedLearning,
          c.noSuggestionsFlag,
          /* editorInputTypeOverride= */ c.editorInputTypeOverride);

      clearEditorText();
      mScenario.onActivity(
          activity -> {
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                .abortCorrectionAndResetPredictionStateForTest();
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.resetNextWordSentence();
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi
                .clearLastCommittedWordForNextSuggestionsForTest();
            wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.clearSuggestionsForTest();
          });
      SystemClock.sleep(120);

      if (c.preseedCommittedText != null && !c.preseedCommittedText.isEmpty()) {
        final String before = getEditorText();
        mScenario.onActivity(
            activity ->
                wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.commitText(c.preseedCommittedText));
        waitForEditorTextToChangeBestEffort(before);
        SystemClock.sleep(SHORT_WAIT_MS);
      }

      final JSONArray steps = new JSONArray();
      final StringBuilder typed = new StringBuilder();

      int index = 0;
      while (index < c.typeText.length()) {
        final int codePoint = Character.codePointAt(c.typeText, index);
        typed.appendCodePoint(codePoint);
        final String expectedTyped = typed.toString();

        final int cp = codePoint;
        mScenario.onActivity(
            activity -> {
              wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.typeCodePointForTest(cp);
              wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.performUpdateSuggestionsNowForTest();
            });

        final boolean updated = waitForTypedWordSuggestionAtIndex0BestEffort(expectedTyped);
        final JSONObject step = new JSONObject();
        step.put("typed", expectedTyped);
        if (!updated) step.put("warning", "suggestion_0_timeout");
        step.put("suggestions", new JSONArray(readTopSuggestions(12)));
        step.put("editorText", safeGetEditorText());
        steps.put(step);

        index += Character.charCount(codePoint);
        SystemClock.sleep(60);
      }

      out.put("steps", steps);
      out.put("finalEditorText", safeGetEditorText());
    } catch (Exception e) {
      try {
        out.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        out.put("finalEditorText", safeGetEditorText());
      } catch (Exception ignored) {
        // best-effort
      }
    } finally {
      closeScenarioIfOpen();
    }
    return out;
  }

  private void launchTestHarness(
      boolean noPersonalizedLearning, boolean noSuggestionsFlag, int editorInputTypeOverride) {
    final Context context = ApplicationProvider.getApplicationContext();
    final Intent intent = new Intent(context, TestInputActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.putExtra(
        TestInputActivity.EXTRA_IME_FLAG_NO_PERSONALIZED_LEARNING, noPersonalizedLearning);
    intent.putExtra(TestInputActivity.EXTRA_TYPE_TEXT_FLAG_NO_SUGGESTIONS, noSuggestionsFlag);
    if (editorInputTypeOverride >= 0) {
      intent.putExtra(TestInputActivity.EXTRA_EDITOR_INPUT_TYPE_OVERRIDE, editorInputTypeOverride);
    }

    mScenario = ActivityScenario.launch(intent);
    if (!waitForEditorVisible()) {
      throw new RuntimeException("Test editor not visible");
    }
    focusEditor();
    mScenario.onActivity(TestInputActivity::forceShowKeyboard);
    SystemClock.sleep(SHORT_WAIT_MS);
    if (!waitForKeyboardVisible()) {
      throw new RuntimeException("Keyboard window not visible");
    }
  }

  private void closeScenarioIfOpen() {
    if (mScenario != null) {
      mScenario.close();
      mScenario = null;
    }
  }

  private boolean waitForEditorVisible() {
    return mDevice.wait(Until.hasObject(By.res(resId("test_edit_text"))), READY_TIMEOUT_MS);
  }

  private boolean waitForKeyboardVisible() {
    boolean visible =
        mDevice.wait(Until.hasObject(By.res(resId("AnyKeyboardMainView"))), READY_TIMEOUT_MS);
    if (!visible) {
      visible = mDevice.wait(Until.hasObject(By.res(resId("candidate_view"))), READY_TIMEOUT_MS);
    }
    return visible;
  }

  private void focusEditor() {
    UiObject2 editor = mDevice.wait(Until.findObject(By.res(resId("test_edit_text"))), 3000);
    if (editor != null) {
      editor.click();
      SystemClock.sleep(250);
    }
  }

  private void clearEditorText() {
    if (mScenario == null) return;
    mScenario.onActivity(
        activity -> {
          final EditText editText = activity.findViewById(R.id.test_edit_text);
          if (editText != null) editText.setText("");
        });
    SystemClock.sleep(120);
  }

  private boolean waitForTypedWordSuggestionAtIndex0BestEffort(@NonNull String expectedTyped) {
    if (mScenario == null) return false;
    final long start = SystemClock.uptimeMillis();
    do {
      final String[] got = {""};
      mScenario.onActivity(activity -> got[0] = CandidateViewTestRegistry.getSuggestionAt(0));
      final String trimmed = got[0] == null ? "" : got[0].trim();
      if (expectedTyped.equals(trimmed)) return true;
      SystemClock.sleep(50);
    } while (SystemClock.uptimeMillis() - start < SUGGESTIONS_TIMEOUT_MS);
    return false;
  }

  @NonNull
  private List<String> readTopSuggestions(int max) {
    final int limit = Math.max(0, max);
    final List<String> out = new ArrayList<>(limit);
    if (mScenario == null) return out;
    mScenario.onActivity(
        activity -> {
          final int count = CandidateViewTestRegistry.getCount();
          final int scan = Math.min(count, limit);
          for (int i = 0; i < scan; i++) {
            final String candidate = CandidateViewTestRegistry.getSuggestionAt(i);
            if (candidate == null) continue;
            final String trimmed = candidate.trim();
            if (!trimmed.isEmpty()) out.add(trimmed);
          }
        });
    return out;
  }

  @NonNull
  private String getEditorText() {
    final UiObject2 editor = mDevice.findObject(By.res(resId("test_edit_text")));
    if (editor == null) return "";
    final String text = editor.getText();
    return text == null ? "" : text;
  }

  @NonNull
  private String safeGetEditorText() {
    try {
      return getEditorText();
    } catch (Exception e) {
      return "";
    }
  }

  private void waitForEditorTextToChangeBestEffort(@NonNull String previousText) {
    final long start = SystemClock.uptimeMillis();
    do {
      final String text = getEditorText();
      if (!text.equals(previousText)) return;
      SystemClock.sleep(60);
    } while (SystemClock.uptimeMillis() - start < 3_000L);
  }

  @NonNull
  private static byte[] gzip(@NonNull String content) throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
      gzip.write(content.getBytes(StandardCharsets.UTF_8));
    }
    return baos.toByteArray();
  }

  private void clearLogcat() throws IOException {
    executeShellCommand("logcat -c");
  }

  private void ensureImeEnabledAndSelected() throws IOException {
    mImeComponent = resolveImeComponentId();
    executeShellCommand("ime enable --user 0 " + mImeComponent);
    executeShellCommand("ime set --user 0 " + mImeComponent);

    String enabled = executeShellCommand("settings get secure enabled_input_methods").trim();
    String expanded = expandComponent(mImeComponent);
    if (!enabled.contains(mImeComponent) && !enabled.contains(expanded)) {
      String prefix = enabled.isEmpty() ? "" : enabled + ":";
      executeShellCommand(
          "settings put secure enabled_input_methods \"" + prefix + mImeComponent + "\"");
    }
    executeShellCommand("settings put secure show_ime_with_hard_keyboard 1");
    SystemClock.sleep(350);
  }

  private void assertImeSelected() throws IOException {
    String current = executeShellCommand("settings get secure default_input_method").trim();
    String expanded = expandComponent(mImeComponent);
    if (!(current.equals(mImeComponent) || current.equals(expanded))) {
      throw new AssertionError(
          "NewSoftKeyboard IME not selected. Expected: " + mImeComponent + " Current: " + current);
    }
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
      if (trimmed.endsWith(".SoftKeyboard") || trimmed.endsWith("/.SoftKeyboard")) {
        fallback = trimmed;
      } else if (fallback == null) {
        fallback = trimmed;
      }
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
    return mDevice.executeShellCommand(command);
  }

  private void wakeAndUnlockDevice() throws IOException {
    try {
      if (!mDevice.isScreenOn()) {
        mDevice.wakeUp();
      }
    } catch (android.os.RemoteException e) {
      throw new RuntimeException(e);
    }
    executeShellCommand("wm dismiss-keyguard");
    executeShellCommand("input keyevent 82");
    SystemClock.sleep(250);
  }

  private static final class TypedCase {
    final String id;
    final String title;
    final boolean noPersonalizedLearning;
    final boolean noSuggestionsFlag;
    final int editorInputTypeOverride;
    @Nullable final String preseedCommittedText;
    @NonNull final String typeText;

    private TypedCase(
        @NonNull String id,
        @NonNull String title,
        boolean noPersonalizedLearning,
        boolean noSuggestionsFlag,
        int editorInputTypeOverride,
        @Nullable String preseedCommittedText,
        @NonNull String typeText) {
      this.id = id;
      this.title = title;
      this.noPersonalizedLearning = noPersonalizedLearning;
      this.noSuggestionsFlag = noSuggestionsFlag;
      this.editorInputTypeOverride = editorInputTypeOverride;
      this.preseedCommittedText = preseedCommittedText;
      this.typeText = typeText;
    }

    static List<TypedCase> cases() {
      final List<TypedCase> out = new ArrayList<>();

      // Baseline English prefixes (plain text).
      out.add(new TypedCase("plain-tha", "Plain text: type 'tha'", false, false, -1, null, "tha"));
      out.add(
          new TypedCase("plain-than", "Plain text: type 'than'", false, false, -1, null, "than"));
      out.add(
          new TypedCase(
              "plain-thank", "Plain text: type 'thank'", false, false, -1, null, "thank"));
      out.add(new TypedCase("plain-you", "Plain text: type 'you'", false, false, -1, null, "you"));
      out.add(
          new TypedCase(
              "plain-im",
              "Plain text: type 'im' (apostrophe/casing expectations)",
              false,
              false,
              -1,
              null,
              "im"));

      // Symbol edge-case (should not be suggested as a word candidate in most cases).
      out.add(
          new TypedCase(
              "plain-pipe",
              "Plain text: type '|' (symbol edge-case)",
              false,
              false,
              -1,
              null,
              "|"));

      // Context injection: commit a phrase so next-word candidates exist, then type a prefix.
      out.add(
          new TypedCase(
              "context-keep-me-i",
              "Context: commit 'keep me ' then type 'i' (next-word injection into typed strip)",
              false,
              false,
              -1,
              "keep me ",
              "i"));

      // Field-type behaviors.
      out.add(
          new TypedCase(
              "field-email-tha",
              "Email field: type 'tha' (inputType=email)",
              false,
              false,
              InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
              null,
              "tha"));
      out.add(
          new TypedCase(
              "field-url-http",
              "URL field: type 'http' (inputType=uri)",
              false,
              false,
              InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI,
              null,
              "http"));

      // Editor flags.
      out.add(
          new TypedCase(
              "flag-no-suggestions-tha",
              "Editor flag: NO_SUGGESTIONS + type 'tha' (we ignore the flag for the report)",
              false,
              true,
              -1,
              null,
              "tha"));
      out.add(
          new TypedCase(
              "flag-no-personalized-learning-tha",
              "Editor flag: NO_PERSONALIZED_LEARNING + type 'tha'",
              true,
              false,
              -1,
              null,
              "tha"));

      return out;
    }
  }
}
