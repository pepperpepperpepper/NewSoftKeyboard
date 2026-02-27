package wtf.uhoh.newsoftkeyboard.app.dictionaries.presage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import com.anysoftkeyboard.api.KeyCodes;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.debug.TestInputActivity;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateViewTestRegistry;
import wtf.uhoh.newsoftkeyboard.engine.EngineType;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelDefinition;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelDownloader;
import wtf.uhoh.newsoftkeyboard.engine.models.ModelStore;
import wtf.uhoh.newsoftkeyboard.engine.neural.NeuralPredictionManager;
import wtf.uhoh.newsoftkeyboard.prefs.DirectBootAwareSharedPreferences;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NextWordSuggestionsUiAutomatorTest {

  private static final String TAG = "NextWordUiAuto";

  private static String getAppPackage() {
    return InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName();
  }

  private static String resId(String idName) {
    return getAppPackage() + ":id/" + idName;
  }

  private static final long READY_TIMEOUT_MS = 10000L;
  private static final long SHORT_WAIT_MS = 400L;
  private static final long SUGGESTIONS_TIMEOUT_MS = 15000L;

  private static volatile UiDevice sDevice;
  private UiDevice mDevice;
  private ActivityScenario<TestInputActivity> mScenario;
  private String mImeComponent;

  @Before
  public void setUp() throws Exception {
    if (sDevice == null) {
      IllegalStateException last = null;
      for (int attempt = 0; attempt < 3; attempt++) {
        try {
          sDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
          last = null;
          break;
        } catch (IllegalStateException e) {
          last = e;
          SystemClock.sleep(500);
        }
      }
      if (sDevice == null && last != null) {
        throw last;
      }
    }
    mDevice = sDevice;
    clearLogcat();
    wakeAndUnlockDevice();

    // Ensure our IME is enabled and selected as default
    ensureImeEnabledAndSelected();
    assertImeSelected();

    // Ensure mixed-case neural model is installed and selected
    Context context = ApplicationProvider.getApplicationContext();
    ensureMixedcaseModelActive(context);

    // Ensure next-word + neural pipeline enabled
    SharedPreferences prefs = DirectBootAwareSharedPreferences.create(context);
    prefs
        .edit()
        .putBoolean(context.getString(R.string.settings_key_show_suggestions), true)
        .putBoolean(context.getString(R.string.settings_key_auto_space), true)
        .putString(context.getString(R.string.settings_key_next_word_dictionary_type), "words")
        .putString(context.getString(R.string.settings_key_prediction_engine_mode), "neural")
        .apply();
  }

  @After
  public void tearDown() {
    if (mScenario != null) {
      mScenario.close();
    }
  }

  @Test
  public void smoke() throws Exception {
    // Basic chaining flow (normal field).
    launchTestHarnessAndSeed(/* noPersonalizedLearning= */ false, /* noSuggestionsFlag= */ false);
    waitForNonEmptySuggestions();
    for (int i = 0; i < 3; i++) {
      mScenario.onActivity(activity -> CandidateViewTestRegistry.pickIfAvailable(0));
      SystemClock.sleep(SHORT_WAIT_MS);
      waitForEditorTextToEndWithSpace();
      waitForNonEmptySuggestions();
    }
    closeScenarioIfOpen();

    // Keep-like behavior (no personalized learning + NO_SUGGESTIONS) should not break chaining.
    launchTestHarnessAndSeed(/* noPersonalizedLearning= */ true, /* noSuggestionsFlag= */ true);
    waitForNonEmptySuggestions();
    for (int i = 0; i < 3; i++) {
      mScenario.onActivity(activity -> CandidateViewTestRegistry.pickIfAvailable(0));
      SystemClock.sleep(SHORT_WAIT_MS);
      waitForEditorTextToEndWithSpace();
      waitForNonEmptySuggestions();
    }
    closeScenarioIfOpen();

    // Regression: manual SPACE after a manual pick (auto-space disabled) should not clear
    // next-words.
    final Context context = ApplicationProvider.getApplicationContext();
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(context);
    prefs.edit().putBoolean(context.getString(R.string.settings_key_auto_space), false).apply();
    SystemClock.sleep(SHORT_WAIT_MS);

    launchTestHarnessAndSeed(/* noPersonalizedLearning= */ false, /* noSuggestionsFlag= */ false);
    mScenario.onActivity(
        activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.setAutoSpaceEnabledForTest(false));
    waitForNonEmptySuggestions();

    mScenario.onActivity(activity -> CandidateViewTestRegistry.pickIfAvailable(0));
    SystemClock.sleep(SHORT_WAIT_MS);

    final String afterPick = getEditorText();
    Assert.assertFalse(
        "Expected editor text to not end with space after manual pick when auto-space is disabled;"
            + " got '"
            + afterPick
            + "'",
        afterPick.endsWith(" "));

    mScenario.onActivity(
        activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.handleSeparator(KeyCodes.SPACE));
    SystemClock.sleep(SHORT_WAIT_MS);
    waitForEditorTextToEndWithSpace();
    waitForNonEmptySuggestions();
  }

  @Test
  public void keepMeInformedDoesNotSurfaceDomainTokens() throws Exception {
    // Use "no personalized learning" so legacy/user sources are suppressed and we see engine
    // output.
    launchTestHarness(
        /* noPersonalizedLearning= */ true,
        /* noSuggestionsFlag= */ false,
        /* simulateInvisibleComposing= */ false,
        /* simulateTypeNull= */ false);

    mScenario.onActivity(
        activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.resetNextWordSentence());
    SystemClock.sleep(SHORT_WAIT_MS);

    // Seed the exact context reported in the field repro.
    mScenario.onActivity(
        activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.commitText("keep me "));
    SystemClock.sleep(SHORT_WAIT_MS);
    waitForNonEmptySuggestions();

    final boolean[] hasNonTrivialCandidate = {false};
    final String[] suggestions = {""};
    mScenario.onActivity(
        activity -> {
          final int count = CandidateViewTestRegistry.getCount();
          final int scanLimit = Math.min(count, 8);
          final java.util.ArrayList<String> seen = new java.util.ArrayList<>(scanLimit);
          for (int i = 0; i < scanLimit; i++) {
            final String candidate = CandidateViewTestRegistry.getSuggestionAt(i);
            if (candidate == null) continue;
            final String trimmed = candidate.trim();
            if (trimmed.isEmpty()) continue;
            seen.add(trimmed);
            final String lower = trimmed.toLowerCase(Locale.ROOT);
            Assert.assertNotEquals("Unexpected domain-like token in normal prose", "com", lower);
            Assert.assertNotEquals("Unexpected domain-like token in normal prose", "www", lower);
            Assert.assertNotEquals("Unexpected domain-like token in normal prose", "http", lower);
            Assert.assertNotEquals("Unexpected domain-like token in normal prose", "https", lower);
            if (trimmed.length() >= 4) {
              hasNonTrivialCandidate[0] = true;
            }
          }
          suggestions[0] = seen.toString();
        });

    Assert.assertTrue(
        "Expected at least one non-trivial candidate after 'keep me', got: " + suggestions[0],
        hasNonTrivialCandidate[0]);
  }

  @Test
  public void composeNonsenseSentenceUsingOnlySuggestions() throws Exception {
    launchTestHarnessAndSeed(/* noPersonalizedLearning= */ false, /* noSuggestionsFlag= */ false);
    // Wait until we have at least one suggestion visible
    waitForNonEmptySuggestions();
    // Log the first few suggestions for visibility
    mScenario.onActivity(
        activity -> {
          int count = CandidateViewTestRegistry.getCount();
          String first = CandidateViewTestRegistry.getSuggestionAt(0);
          Log.d(TAG, "SUG_COUNT=" + count + " FIRST='" + first + "'");
        });

    // Build a sentence by picking the first suggestion repeatedly, with brief waits in between
    final int picks = 12;
    final StringBuilder built = new StringBuilder();
    final String[] previous = {""};
    for (int i = 0; i < picks; i++) {
      // Choose a suggestion that is not identical to the previous token, if possible.
      final int[] chosenIndex = {0};
      mScenario.onActivity(
          activity -> {
            int count = CandidateViewTestRegistry.getCount();
            int pickIdx = 0;
            for (int idx = 0; idx < count; idx++) {
              String cand = CandidateViewTestRegistry.getSuggestionAt(idx);
              if (cand != null && !cand.trim().isEmpty()) {
                if (previous[0].isEmpty() || !cand.trim().equalsIgnoreCase(previous[0].trim())) {
                  pickIdx = idx;
                  break;
                }
              }
            }
            chosenIndex[0] = pickIdx;
            String pick = CandidateViewTestRegistry.getSuggestionAt(pickIdx);
            if (pick == null) pick = "";
            if (built.length() > 0) built.append(' ');
            built.append(pick.trim());
            previous[0] = pick;
          });
      // Pick the chosen suggestion if available
      final int idxToPick = chosenIndex[0];
      mScenario.onActivity(activity -> CandidateViewTestRegistry.pickIfAvailable(idxToPick));
      SystemClock.sleep(SHORT_WAIT_MS);
      waitForEditorTextToEndWithSpace();
      waitForNonEmptySuggestions();
    }

    // Build the sentence from the picked suggestions (first position each time).
    String sentence = built.toString().trim();
    Log.d(TAG, "NON_SENSE_SENTENCE=" + sentence);
    assertFalse("Expected sentence from suggestions only", sentence.isEmpty());

    // Ensure the actual editor content received multiple words separated by spaces.
    final String editorText = getEditorText();
    assertFalse(
        "Expected editor to receive committed suggestion text", editorText.trim().isEmpty());
    Assert.assertTrue(
        "Expected multiple words in editor text; got '" + editorText + "'",
        editorText.trim().split("\\s+").length > 2);
  }

  private void waitForNonEmptySuggestions() {
    final long start = SystemClock.uptimeMillis();
    long lastRefresh = 0L;
    int count;
    do {
      final int[] c = {0};
      mScenario.onActivity(activity -> c[0] = CandidateViewTestRegistry.getCount());
      count = c[0];
      if (count > 0) return;
      final long now = SystemClock.uptimeMillis();
      if (now - lastRefresh >= 1000L) {
        lastRefresh = now;
        mScenario.onActivity(
            activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.forceNextWordFromCursor());
      }
      SystemClock.sleep(200);
    } while (SystemClock.uptimeMillis() - start < SUGGESTIONS_TIMEOUT_MS);
    // One last check before giving up
    final int[] c = {0};
    mScenario.onActivity(activity -> c[0] = CandidateViewTestRegistry.getCount());
    if (c[0] == 0) {
      dumpWindowHierarchyForDebug();
      Log.w(TAG, "No suggestions visible after timeout");
      throw new AssertionError("Suggestions did not appear");
    }
  }

  @Test
  public void composeNonsenseSentenceUsingOnlySuggestions_underNoPersonalizedLearning()
      throws Exception {
    // Google Keep and other apps may set IME_FLAG_NO_PERSONALIZED_LEARNING and/or NO_SUGGESTIONS.
    // This should not break next-word prediction nor the "tap-to-chain" flow.
    launchTestHarnessAndSeed(/* noPersonalizedLearning= */ true, /* noSuggestionsFlag= */ true);
    waitForNonEmptySuggestions();

    for (int i = 0; i < 8; i++) {
      mScenario.onActivity(activity -> CandidateViewTestRegistry.pickIfAvailable(0));
      SystemClock.sleep(SHORT_WAIT_MS);
      waitForEditorTextToEndWithSpace();
      waitForNonEmptySuggestions();
    }

    final String editorText = getEditorText();
    Assert.assertTrue(
        "Expected editor text to grow under no-personalized-learning; got '" + editorText + "'",
        editorText.trim().split("\\s+").length > 3);
  }

  @Test
  public void manualPickNotClearedByDelayedSuggestionsUpdate() throws Exception {
    launchTestHarnessAndSeed(/* noPersonalizedLearning= */ false, /* noSuggestionsFlag= */ false);
    waitForNonEmptySuggestions();

    // Simulate the "typing scheduled an update, but user picked before it fired" race.
    mScenario.onActivity(
        activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.postUpdateSuggestions());
    mScenario.onActivity(activity -> CandidateViewTestRegistry.pickIfAvailable(0));
    SystemClock.sleep(SHORT_WAIT_MS);
    waitForEditorTextToEndWithSpace();
    waitForNonEmptySuggestions();

    // If the delayed update fires after this point and is not canceled, it may overwrite the strip
    // with an empty/blank set. Clear internal next-word state to make the regression deterministic.
    mScenario.onActivity(
        activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.resetNextWordSentence());
    SystemClock.sleep(
        wtf.uhoh.newsoftkeyboard.app.ime.ImeSuggestionsController.GET_SUGGESTIONS_DELAY
            + SHORT_WAIT_MS);

    final int[] count = {0};
    final String[] first = {""};
    mScenario.onActivity(
        activity -> {
          count[0] = CandidateViewTestRegistry.getCount();
          first[0] = CandidateViewTestRegistry.getSuggestionAt(0);
        });
    Assert.assertTrue("Expected suggestions to remain visible; count=" + count[0], count[0] > 0);
    Assert.assertTrue(
        "Expected first suggestion to be non-empty, but was '" + first[0] + "'",
        first[0] != null && !first[0].isEmpty());
  }

  @Test
  public void manualSpaceAfterManualPickKeepsNextWordSuggestions() throws Exception {
    // Repro for "no suggestions until typing next letter" when auto-space is disabled and the user
    // manually presses SPACE after picking a suggestion.
    final Context context = ApplicationProvider.getApplicationContext();
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(context);
    prefs.edit().putBoolean(context.getString(R.string.settings_key_auto_space), false).apply();
    SystemClock.sleep(SHORT_WAIT_MS);

    launchTestHarnessAndSeed(/* noPersonalizedLearning= */ false, /* noSuggestionsFlag= */ false);
    mScenario.onActivity(
        activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.setAutoSpaceEnabledForTest(false));
    waitForNonEmptySuggestions();

    // Manual pick commits the word but (with auto-space disabled) should not insert a trailing
    // space.
    mScenario.onActivity(activity -> CandidateViewTestRegistry.pickIfAvailable(0));
    SystemClock.sleep(SHORT_WAIT_MS);

    final String afterPick = getEditorText();
    Assert.assertFalse(
        "Expected editor text to not end with space after manual pick when auto-space is disabled;"
            + " got '"
            + afterPick
            + "'",
        afterPick.endsWith(" "));

    // User manually presses SPACE; next-word suggestions should remain populated (not cleared).
    mScenario.onActivity(
        activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.handleSeparator(KeyCodes.SPACE));
    SystemClock.sleep(SHORT_WAIT_MS);
    waitForEditorTextToEndWithSpace();
    waitForNonEmptySuggestions();
  }

  @Test
  public void nextWordPickInsertsLeadingSpaceWhenAutoSpaceIsDisabled() throws Exception {
    // Regression: when auto-space is disabled, picking a next-word suggestion after a previous pick
    // (which
    // did not insert a trailing space) must still insert a separator before committing the next
    // word.
    final Context context = ApplicationProvider.getApplicationContext();
    final SharedPreferences prefs = DirectBootAwareSharedPreferences.create(context);
    prefs.edit().putBoolean(context.getString(R.string.settings_key_auto_space), false).apply();
    SystemClock.sleep(SHORT_WAIT_MS);

    launchTestHarnessAndSeed(/* noPersonalizedLearning= */ false, /* noSuggestionsFlag= */ false);
    mScenario.onActivity(
        activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.setAutoSpaceEnabledForTest(false));
    waitForNonEmptySuggestions();

    // First pick commits a word but (with auto-space disabled) should not insert a trailing space.
    mScenario.onActivity(activity -> CandidateViewTestRegistry.pickIfAvailable(0));
    SystemClock.sleep(SHORT_WAIT_MS);

    final String afterFirstPick = getEditorText();
    Assert.assertFalse(
        "Expected editor text to not end with space after a pick when auto-space is disabled; got '"
            + afterFirstPick
            + "'",
        afterFirstPick.endsWith(" "));

    // Ensure we have next-word suggestions available for the second pick.
    mScenario.onActivity(
        activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.forceNextWordFromCursor());
    SystemClock.sleep(SHORT_WAIT_MS);
    waitForNonEmptySuggestions();

    final String beforeSecondPick = getEditorText();
    Assert.assertFalse(
        "Expected editor text to not end with space before a second next-word pick when auto-space"
            + " is disabled; got '"
            + beforeSecondPick
            + "'",
        beforeSecondPick.endsWith(" "));

    // Second pick should insert a leading separator (space) before the committed next word.
    mScenario.onActivity(activity -> CandidateViewTestRegistry.pickIfAvailable(0));
    SystemClock.sleep(SHORT_WAIT_MS);

    final String afterSecondPick = getEditorText();
    Assert.assertTrue(
        "Expected editor text to grow after a second pick (before='"
            + beforeSecondPick
            + "', after='"
            + afterSecondPick
            + "')",
        afterSecondPick.length() > beforeSecondPick.length());
    Assert.assertTrue(
        "Expected a leading space before the second picked next word (before='"
            + beforeSecondPick
            + "', after='"
            + afterSecondPick
            + "')",
        afterSecondPick.startsWith(beforeSecondPick + " "));
    Assert.assertFalse(
        "Expected auto-space disabled to not insert a trailing space after the second pick; got '"
            + afterSecondPick
            + "'",
        afterSecondPick.endsWith(" "));
  }

  @Test
  public void typedCharactersAppearWhenComposingIsInvisible() throws Exception {
    launchTestHarnessAndSeed(
        /* noPersonalizedLearning= */ false,
        /* noSuggestionsFlag= */ false,
        /* simulateInvisibleComposing= */ true);
    waitForNonEmptySuggestions();

    // Type a single letter using the on-screen keyboard. In composing-hostile editors, the IME
    // must fall back to committing real characters so the typed input is visible immediately.
    final String beforeText = getEditorText();
    tapKeyMiddleRow(0.78f); // approximate 'k'
    SystemClock.sleep(SHORT_WAIT_MS);

    final String editorText = getEditorText();
    assertSingleLetterAppended(beforeText, editorText);
  }

  @Test
  public void composeNonsenseSentenceUsingOnlySuggestions_whenComposingIsInvisible()
      throws Exception {
    launchTestHarnessAndSeed(
        /* noPersonalizedLearning= */ false,
        /* noSuggestionsFlag= */ false,
        /* simulateInvisibleComposing= */ true);
    waitForNonEmptySuggestions();

    for (int i = 0; i < 5; i++) {
      mScenario.onActivity(activity -> CandidateViewTestRegistry.pickIfAvailable(0));
      SystemClock.sleep(SHORT_WAIT_MS);
      waitForEditorTextToEndWithSpace();
      waitForNonEmptySuggestions();
    }

    final String editorText = getEditorText();
    Assert.assertTrue(
        "Expected editor text to grow when composing is invisible; got '" + editorText + "'",
        editorText.trim().split("\\s+").length > 3);
  }

  @Test
  public void typeNullFieldDoesNotSwallowCharactersOrShowSuggestions() throws Exception {
    // Some editors omit inputType bits entirely and report inputType==0 (TYPE_NULL). Treat those
    // as text fields for strip visibility, but disable auto-space/auto-pick for compatibility.
    launchTestHarness(
        /* noPersonalizedLearning= */ false,
        /* noSuggestionsFlag= */ false,
        /* simulateInvisibleComposing= */ false,
        /* simulateTypeNull= */ true);

    final boolean[] autoSpaceEnabled = {true};
    mScenario.onActivity(
        activity ->
            autoSpaceEnabled[0] =
                wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.isAutoSpaceEnabledForTest());
    Assert.assertFalse("Expected auto-space disabled for TYPE_NULL fields", autoSpaceEnabled[0]);

    final String beforeText = getEditorText();
    tapKeyMiddleRow(0.78f); // approximate 'k'
    SystemClock.sleep(SHORT_WAIT_MS);

    final String editorText = getEditorText();
    assertSingleLetterAppended(beforeText, editorText);

    final int[] afterCount = {0};
    mScenario.onActivity(activity -> afterCount[0] = CandidateViewTestRegistry.getCount());
    Assert.assertTrue(
        "Expected suggestions to remain visible after typing in TYPE_NULL fields, but count was "
            + afterCount[0],
        afterCount[0] > 0);
  }

  private void closeScenarioIfOpen() {
    if (mScenario != null) {
      mScenario.close();
      mScenario = null;
    }
  }

  private void assertSingleLetterAppended(String beforeText, String afterText) {
    final int expected = beforeText.length() + 1;
    Assert.assertTrue(
        "Expected editor text to grow by 1 character after a key press (before='"
            + beforeText
            + "', after='"
            + afterText
            + "')",
        afterText.length() == expected);
    final char appended = afterText.charAt(afterText.length() - 1);
    Assert.assertTrue(
        "Expected appended character to be a letter, but was '"
            + appended
            + "' (before='"
            + beforeText
            + "', after='"
            + afterText
            + "')",
        Character.isLetter(appended));
  }

  private void launchTestHarnessAndSeed(boolean noPersonalizedLearning, boolean noSuggestionsFlag) {
    launchTestHarnessAndSeed(noPersonalizedLearning, noSuggestionsFlag, false);
  }

  private void launchTestHarnessAndSeed(
      boolean noPersonalizedLearning,
      boolean noSuggestionsFlag,
      boolean simulateInvisibleComposing) {
    launchTestHarness(noPersonalizedLearning, noSuggestionsFlag, simulateInvisibleComposing, false);

    // Seed a neutral prefix so suggestions start flowing.
    mScenario.onActivity(
        activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.commitText("the "));
    SystemClock.sleep(SHORT_WAIT_MS);
    // Ask IME to compute and show next-word suggestions from previous token.
    mScenario.onActivity(
        activity -> wtf.uhoh.newsoftkeyboard.app.ime.ImeTestApi.forceNextWordFromCursor());
    SystemClock.sleep(SHORT_WAIT_MS);
  }

  private void launchTestHarness(
      boolean noPersonalizedLearning,
      boolean noSuggestionsFlag,
      boolean simulateInvisibleComposing,
      boolean simulateTypeNull) {
    if (mScenario != null) {
      mScenario.close();
      mScenario = null;
    }
    final Context context = ApplicationProvider.getApplicationContext();
    final Intent intent = new Intent(context, TestInputActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.putExtra(
        TestInputActivity.EXTRA_IME_FLAG_NO_PERSONALIZED_LEARNING, noPersonalizedLearning);
    intent.putExtra(TestInputActivity.EXTRA_TYPE_TEXT_FLAG_NO_SUGGESTIONS, noSuggestionsFlag);
    intent.putExtra(
        TestInputActivity.EXTRA_SIMULATE_INVISIBLE_COMPOSING, simulateInvisibleComposing);
    intent.putExtra(TestInputActivity.EXTRA_SIMULATE_TYPE_NULL, simulateTypeNull);
    mScenario = ActivityScenario.launch(intent);

    waitForEditorVisible();
    focusEditor();
    mScenario.onActivity(TestInputActivity::forceShowKeyboard);
    SystemClock.sleep(SHORT_WAIT_MS);
    waitForKeyboardVisible();
  }

  private String getEditorText() {
    final ActivityScenario<TestInputActivity> scenario = mScenario;
    if (scenario != null) {
      final String[] text = {""};
      scenario.onActivity(
          activity -> {
            android.widget.EditText editText = activity.findViewById(R.id.test_edit_text);
            if (editText == null) return;
            final CharSequence value = editText.getText();
            text[0] = value == null ? "" : value.toString();
          });
      return text[0] == null ? "" : text[0];
    }

    UiObject2 editor = mDevice.findObject(By.res(resId("test_edit_text")));
    if (editor == null) return "";
    final String text = editor.getText();
    return text == null ? "" : text;
  }

  private void waitForEditorTextToEndWithSpace() {
    final long start = SystemClock.uptimeMillis();
    do {
      final String text = getEditorText();
      if (text.endsWith(" ")) return;
      SystemClock.sleep(100);
    } while (SystemClock.uptimeMillis() - start < 3000);
    final String text = getEditorText();
    Assert.fail("Expected editor text to end with space, but was '" + text + "'");
  }

  private void ensureMixedcaseModelActive(Context context) throws Exception {
    final ModelStore store = new ModelStore(context);
    final ModelDefinition defForEntry =
        ModelDefinition.builder("distilgpt2_mixedcase_sanity")
            .setLabel("DistilGPT-2 mixedcase (sanity)")
            .setEngineType(EngineType.NEURAL)
            .setOnnxFile("model_int8.onnx", null, null)
            .setTokenizerVocabFile("vocab.json", null, null)
            .setTokenizerMergesFile("merges.txt", null, null)
            .build();
    final PresageModelCatalog.CatalogEntry target =
        new PresageModelCatalog.CatalogEntry(
            defForEntry,
            "https://fdroid.uh-oh.wtf/models/distilgpt2_mixedcase_sanity_v1.zip",
            "06dbfa67aed36b24c931dabdb10060b0e93b4af5cbf123c1ce7358b26fec13d4",
            53587027L,
            1,
            false);

    final ModelDownloader downloader = new ModelDownloader(context, store);
    try {
      DownloaderCompat.run(downloader, target);
    } catch (IOException e) {
      // If already installed, ignore network error
      Log.w(TAG, "Downloader error (continuing if already installed): ", e);
    }
    store.persistSelectedModelId(EngineType.NEURAL, "distilgpt2_mixedcase_sanity");
    final NeuralPredictionManager manager = new NeuralPredictionManager(context);
    if (!manager.activate()) {
      fail("Neural predictor failed to activate with mixedcase model");
    }
    manager.deactivate();
  }

  private void waitForEditorVisible() {
    boolean visible =
        mDevice.wait(Until.hasObject(By.res(resId("test_edit_text"))), READY_TIMEOUT_MS);
    if (!visible) {
      dumpWindowHierarchyForDebug();
      fail("Test editor not visible");
    }
  }

  private void waitForKeyboardVisible() {
    boolean visible =
        mDevice.wait(Until.hasObject(By.res(resId("AnyKeyboardMainView"))), READY_TIMEOUT_MS);
    if (!visible) {
      visible =
          mDevice.wait(
              Until.hasObject(
                  By.clazz("wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardView")),
              READY_TIMEOUT_MS);
    }
    if (!visible) {
      dumpWindowHierarchyForDebug();
      fail("Keyboard window not visible");
    }
  }

  private void focusEditor() {
    UiObject2 editor = mDevice.wait(Until.findObject(By.res(resId("test_edit_text"))), 3000);
    if (editor != null) {
      editor.click();
      SystemClock.sleep(300);
    }
  }

  private void clickKeyboardRelative(float relX, float relY) {
    Rect bounds = locateKeyboardBounds();
    int x = bounds.left + Math.round(bounds.width() * Math.min(0.98f, Math.max(0.02f, relX)));
    int y = bounds.top + Math.round(bounds.height() * Math.min(0.98f, Math.max(0.02f, relY)));
    mDevice.click(x, y);
  }

  private Rect locateKeyboardBounds() {
    UiObject2 keyboard =
        mDevice.wait(
            Until.findObject(
                By.res(resId("AnyKeyboardMainView"))
                    .clazz("wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardView")),
            1500);
    if (keyboard == null) {
      keyboard =
          mDevice.wait(
              Until.findObject(
                  By.clazz("wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardView")),
              1500);
    }
    if (keyboard != null) {
      return keyboard.getVisibleBounds();
    }
    Rect fromDump = fetchImeWindowBounds();
    if (fromDump != null) return fromDump;
    // fallback guess
    int width = mDevice.getDisplayWidth();
    int height = mDevice.getDisplayHeight();
    return new Rect(0, (int) (height * 0.60f), width, height);
  }

  private void typeSoft(String text) {
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case 't':
          tapKeyTopRow(0.46f); // approximate position of 't'
          break;
        case 'h':
          tapKeyMiddleRow(0.56f); // approximate 'h'
          break;
        case 'e':
          tapKeyTopRow(0.26f); // approximate 'e'
          break;
        case ' ':
          clickKeyboardRelative(0.50f, 0.90f); // spacebar
          break;
        default:
          // fallback: small center tap to keep IME active
          clickKeyboardRelative(0.5f, 0.7f);
      }
      SystemClock.sleep(120);
    }
  }

  private void tapKeyTopRow(float relX) {
    Rect kb = locateKeyboardBounds();
    // top row roughly at 20% of keyboard height
    int x = kb.left + Math.round(kb.width() * clamp(relX));
    int y = kb.top + Math.round(kb.height() * 0.20f);
    mDevice.click(x, y);
  }

  private void tapKeyMiddleRow(float relX) {
    Rect kb = locateKeyboardBounds();
    // middle row roughly at 50% of keyboard height
    int x = kb.left + Math.round(kb.width() * clamp(relX));
    int y = kb.top + Math.round(kb.height() * 0.50f);
    mDevice.click(x, y);
  }

  private float clamp(float v) {
    return Math.min(0.98f, Math.max(0.02f, v));
  }

  private Rect fetchImeWindowBounds() {
    try {
      String dump = executeShellCommand("dumpsys window windows");
      java.util.regex.Matcher frameMatcher =
          java.util.regex.Pattern.compile(
                  "Window\\{[^}]+"
                      + " InputMethod[\\s\\S]*?mFrame=\\[(\\d+),\\s*(\\d+)]\\[(\\d+),\\s*(\\d+)]",
                  java.util.regex.Pattern.DOTALL)
              .matcher(dump);
      if (frameMatcher.find()) {
        return new Rect(
            Integer.parseInt(frameMatcher.group(1)),
            Integer.parseInt(frameMatcher.group(2)),
            Integer.parseInt(frameMatcher.group(3)),
            Integer.parseInt(frameMatcher.group(4)));
      }
      java.util.regex.Matcher shownMatcher =
          java.util.regex.Pattern.compile(
                  "Window\\{[^}]+ InputMethod[\\s\\S]*?Shown frame:"
                      + " \\[(\\d+),\\s*(\\d+)]\\[(\\d+),\\s*(\\d+)]",
                  java.util.regex.Pattern.DOTALL)
              .matcher(dump);
      if (shownMatcher.find()) {
        return new Rect(
            Integer.parseInt(shownMatcher.group(1)),
            Integer.parseInt(shownMatcher.group(2)),
            Integer.parseInt(shownMatcher.group(3)),
            Integer.parseInt(shownMatcher.group(4)));
      }
    } catch (IOException ignored) {
    }
    return null;
  }

  private void clearLogcat() throws IOException {
    executeShellCommand("logcat -c");
  }

  private void dumpWindowHierarchyForDebug() {
    try {
      java.io.File cacheDir = ApplicationProvider.getApplicationContext().getCacheDir();
      java.io.File dumpFile = java.io.File.createTempFile("uia_dump_", ".xml", cacheDir);
      mDevice.dumpWindowHierarchy(dumpFile);
      Log.w(TAG, "UI dump at " + dumpFile.getAbsolutePath());
    } catch (IOException ignored) {
    }
  }

  private void ensureImeEnabledAndSelected() throws IOException {
    mImeComponent = resolveImeComponentId();
    String enableOutput = executeShellCommand("ime enable --user 0 " + mImeComponent).trim();
    Log.d(TAG, "ime enable output: " + enableOutput);
    if (enableOutput.contains("Unknown") || enableOutput.contains("Error")) {
      throw new IOException("Failed to enable IME. Output: " + enableOutput);
    }

    String setOutput = executeShellCommand("ime set --user 0 " + mImeComponent).trim();
    Log.d(TAG, "ime set output: " + setOutput);

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

  private void assertImeSelected() throws IOException {
    String current = executeShellCommand("settings get secure default_input_method").trim();
    String expanded = expandComponent(mImeComponent);
    if (!(current.equals(mImeComponent) || current.equals(expanded))) {
      Log.e(TAG, "default_input_method=" + current);
      String enabled = executeShellCommand("settings get secure enabled_input_methods").trim();
      Log.e(TAG, "enabled_input_methods=" + enabled);
      String imeListAll = executeShellCommand("ime list -a -s").trim();
      Log.e(TAG, "ime list -a -s:\n" + imeListAll);
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
      // Prefer the nsk flavor service if present.
      if (trimmed.endsWith(".NewSoftKeyboardService")
          || trimmed.endsWith("/.NewSoftKeyboardService")) {
        return trimmed;
      }
      // Otherwise prefer legacy naming.
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
    if (mDevice != null) {
      return mDevice.executeShellCommand(command);
    }
    ParcelFileDescriptor pfd =
        InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
    try (FileInputStream inputStream = new FileInputStream(pfd.getFileDescriptor());
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
      StringBuilder builder = new StringBuilder();
      char[] buffer = new char[1024];
      int read;
      while ((read = reader.read(buffer)) != -1) {
        builder.append(buffer, 0, read);
      }
      return builder.toString();
    } finally {
      try {
        pfd.close();
      } catch (IOException ignored) {
      }
    }
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
    mDevice.pressHome();
    mDevice.waitForIdle();
  }
}
