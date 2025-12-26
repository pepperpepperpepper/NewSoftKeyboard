package wtf.uhoh.newsoftkeyboard.app.ime;

import static android.os.SystemClock.sleep;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.view.inputmethod.EditorInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.ExternalDictionaryFactory;
import wtf.uhoh.newsoftkeyboard.app.dictionaries.UserDictionary;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardFactory;
import wtf.uhoh.newsoftkeyboard.app.testing.TestableImeService;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceDictionaryEnablingTest extends ImeServiceBaseTest {

  private static final String[] DICTIONARY_WORDS =
      new String[] {"high", "hello", "menny", "AnySoftKeyboard", "keyboard", "com/google", "low"};

  @Before
  public void setUp() throws Exception {
    UserDictionary userDictionary = new UserDictionary(getApplicationContext(), "en");
    userDictionary.loadDictionary();
    for (int wordIndex = 0; wordIndex < DICTIONARY_WORDS.length; wordIndex++) {
      userDictionary.addWord(DICTIONARY_WORDS[wordIndex], DICTIONARY_WORDS.length - wordIndex);
    }
    userDictionary.close();
  }

  @Test
  public void testDictionariesCreatedForText() {
    simulateFinishInputFlow();

    Mockito.reset(mImeServiceUnderTest.getSuggest());

    final EditorInfo editorInfo = TestableImeService.createEditorInfoTextWithSuggestions();
    mImeServiceUnderTest.onStartInput(editorInfo, false);

    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never())
        .setupSuggestionsForKeyboard(Mockito.anyList(), Mockito.any());

    mImeServiceUnderTest.onCreateInputView();
    mImeServiceUnderTest.onStartInputView(editorInfo, false);
    mImeServiceUnderTest.simulateKeyPress('h');

    Mockito.verify(mImeServiceUnderTest.getSuggest())
        .setupSuggestionsForKeyboard(Mockito.anyList(), Mockito.any());
    Assert.assertTrue(mImeServiceUnderTest.isPredictionOn());
    Assert.assertTrue(mImeServiceUnderTest.isAutoCorrect());
  }

  @Test
  public void testDictionariesNotCreatedForTextWithOutViewCreated() {
    simulateFinishInputFlow();
    Mockito.reset(mImeServiceUnderTest.getSuggest());
    final EditorInfo editorInfo = TestableImeService.createEditorInfoTextWithSuggestions();
    // NOTE: Not creating View!
    mImeServiceUnderTest.onStartInput(editorInfo, false);
    mImeServiceUnderTest.onStartInputView(editorInfo, false);
  }

  @Test
  public void testDictionariesNotCreatedForPassword() {
    Mockito.reset(mImeServiceUnderTest.getSuggest());

    final EditorInfo editorInfo =
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT + EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
    simulateFinishInputFlow();
    simulateOnStartInputFlow(false, editorInfo);

    mImeServiceUnderTest.simulateKeyPress('h');

    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never())
        .setupSuggestionsForKeyboard(Mockito.anyList(), Mockito.any());
    Assert.assertFalse(mImeServiceUnderTest.isPredictionOn());
    Assert.assertFalse(mImeServiceUnderTest.isAutoCorrect());
  }

  @Test
  public void testDictionariesNotCreatedForVisiblePassword() {
    final EditorInfo editorInfo =
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT + EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
    simulateFinishInputFlow();
    simulateOnStartInputFlow(false, editorInfo);
    mImeServiceUnderTest.simulateKeyPress('h');

    Assert.assertFalse(mImeServiceUnderTest.isPredictionOn());
    Assert.assertFalse(mImeServiceUnderTest.isAutoCorrect());
  }

  @Test
  public void testDictionariesNotCreatedForWebPassword() {
    final EditorInfo editorInfo =
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT + EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD);
    simulateFinishInputFlow();
    simulateOnStartInputFlow(false, editorInfo);

    mImeServiceUnderTest.simulateKeyPress('h');

    Assert.assertFalse(mImeServiceUnderTest.isPredictionOn());
    Assert.assertFalse(mImeServiceUnderTest.isAutoCorrect());
  }

  @Test
  public void testDictionariesCreatedForUriInputButWithoutAutoPick() {
    simulateFinishInputFlow();
    simulateOnStartInputFlow(
        false,
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT + EditorInfo.TYPE_TEXT_VARIATION_URI));
    mImeServiceUnderTest.simulateKeyPress('h');

    Assert.assertTrue(mImeServiceUnderTest.isPredictionOn());
    Assert.assertFalse(mImeServiceUnderTest.isAutoCorrect());
  }

  @Test
  public void testDictionariesCreatedForEmailInputButNotAutoPick() {
    final EditorInfo editorInfo =
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT + EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
    simulateFinishInputFlow();
    simulateOnStartInputFlow(false, editorInfo);
    mImeServiceUnderTest.simulateKeyPress('h');

    Assert.assertTrue(mImeServiceUnderTest.isPredictionOn());
    Assert.assertFalse(mImeServiceUnderTest.isAutoCorrect());
  }

  @Test
  public void testDictionariesCreatedForWebEmailInputButNotAutoPick() {
    final EditorInfo editorInfo =
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT + EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS);
    simulateFinishInputFlow();
    simulateOnStartInputFlow(false, editorInfo);
    mImeServiceUnderTest.simulateKeyPress('h');

    Assert.assertTrue(mImeServiceUnderTest.isPredictionOn());
    Assert.assertFalse(mImeServiceUnderTest.isAutoCorrect());
  }

  @Test
  public void testDictionariesCreatedForAutoComplete() {
    final EditorInfo editorInfo = TestableImeService.createEditorInfoTextWithSuggestions();
    editorInfo.inputType += EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE;
    simulateFinishInputFlow();
    simulateOnStartInputFlow(false, editorInfo);
    mImeServiceUnderTest.simulateKeyPress('h');

    Assert.assertTrue(mImeServiceUnderTest.isPredictionOn());
    Assert.assertTrue(mImeServiceUnderTest.isAutoCorrect());
  }

  @Test
  public void testDictionariesNotCreatedForNoSuggestions() {
    final EditorInfo editorInfo = TestableImeService.createEditorInfoTextWithSuggestions();
    editorInfo.inputType += EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
    simulateFinishInputFlow();
    simulateOnStartInputFlow(false, editorInfo);
    mImeServiceUnderTest.simulateKeyPress('h');

    Assert.assertFalse(mImeServiceUnderTest.isPredictionOn());
    Assert.assertFalse(mImeServiceUnderTest.isAutoCorrect());
  }

  @Test
  public void testDictionariesResetForPassword() {
    mImeServiceUnderTest.simulateKeyPress('h');

    Assert.assertTrue(mImeServiceUnderTest.isPredictionOn());

    final EditorInfo editorInfoPassword =
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT + EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD);
    simulateFinishInputFlow();
    simulateOnStartInputFlow(false, editorInfoPassword);

    mImeServiceUnderTest.simulateKeyPress('h');

    Assert.assertFalse(mImeServiceUnderTest.isPredictionOn());
    Assert.assertFalse(mImeServiceUnderTest.isAutoCorrect());
  }

  @Test
  public void testReleasingAllDictionariesIfPrefsSetToNoSuggestions() {
    mImeServiceUnderTest.simulateKeyPress('h');

    Assert.assertTrue(mImeServiceUnderTest.isPredictionOn());

    Mockito.reset(mImeServiceUnderTest.getSuggest());

    simulateFinishInputFlow();

    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never()).closeDictionaries();

    SharedPrefsHelper.setPrefsValue("candidates_on", false);

    Mockito.verify(mImeServiceUnderTest.getSuggest()).closeDictionaries();
    Mockito.verify(mImeServiceUnderTest.getSuggest())
        .setCorrectionMode(
            Mockito.eq(false), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyBoolean());

    simulateOnStartInputFlow();

    mImeServiceUnderTest.simulateKeyPress('h');

    Assert.assertFalse(mImeServiceUnderTest.isPredictionOn());

    simulateFinishInputFlow();

    Mockito.reset(mImeServiceUnderTest.getSuggest());

    SharedPrefsHelper.setPrefsValue("candidates_on", true);

    Mockito.verify(mImeServiceUnderTest.getSuggest())
        .setCorrectionMode(
            Mockito.eq(true), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyBoolean());

    simulateOnStartInputFlow();

    mImeServiceUnderTest.simulateKeyPress('h');

    Mockito.verify(mImeServiceUnderTest.getSuggest())
        .setupSuggestionsForKeyboard(Mockito.anyList(), Mockito.any());
    Assert.assertTrue(mImeServiceUnderTest.isPredictionOn());
  }

  @Test
  public void testDoesNotCloseDictionaryIfInputRestartsQuickly() {
    mImeServiceUnderTest.simulateKeyPress('h');

    // setting the dictionary
    Mockito.verify(mImeServiceUnderTest.getSuggest())
        .setupSuggestionsForKeyboard(Mockito.anyList(), Mockito.any());

    Mockito.reset(mImeServiceUnderTest.getSuggest());

    simulateFinishInputFlow();

    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never()).closeDictionaries();
    // waiting a bit
    sleep(10);
    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never()).closeDictionaries();
    // restarting the input
    simulateOnStartInputFlow();

    mImeServiceUnderTest.simulateKeyPress('h');

    Mockito.verify(mImeServiceUnderTest.getSuggest())
        .setupSuggestionsForKeyboard(Mockito.anyList(), Mockito.any());
    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never()).closeDictionaries();
  }

  @Test
  public void testDoesCloseDictionaryIfInputRestartsSlowly() {
    mImeServiceUnderTest.simulateKeyPress('h');

    // setting the dictionary
    Mockito.verify(mImeServiceUnderTest.getSuggest())
        .setupSuggestionsForKeyboard(Mockito.anyList(), Mockito.any());

    Mockito.reset(mImeServiceUnderTest.getSuggest());

    simulateFinishInputFlow();
    // waiting a long time
    TestRxSchedulers.foregroundAdvanceBy(10000);
    TestRxSchedulers.drainAllTasks();
    Mockito.verify(mImeServiceUnderTest.getSuggest()).closeDictionaries();
    Mockito.reset(mImeServiceUnderTest.getSuggest());
    // restarting the input
    simulateOnStartInputFlow();
    mImeServiceUnderTest.simulateKeyPress('h');

    Mockito.verify(mImeServiceUnderTest.getSuggest())
        .setupSuggestionsForKeyboard(Mockito.anyList(), Mockito.any());
  }

  @Test
  public void testSettingCorrectModeFromPrefs() {
    SharedPrefsHelper.setPrefsValue(
        "settings_key_auto_pick_suggestion_aggressiveness", "minimal_aggressiveness");
    Mockito.verify(mImeServiceUnderTest.getSuggest())
        .setCorrectionMode(true, 1, 1, false /*the default*/);
  }

  @Test
  public void testSetDictionaryOnOverridePrefs() {
    mImeServiceUnderTest.simulateKeyPress('h');

    Mockito.reset(mImeServiceUnderTest.getSuggest());

    KeyboardDefinition currentKeyboard = mImeServiceUnderTest.getCurrentKeyboardForTests();
    SharedPrefsHelper.setPrefsValue(
        ExternalDictionaryFactory.getDictionaryOverrideKey(currentKeyboard), "dictionary_sdfsdfsd");
    Mockito.verify(mImeServiceUnderTest.getSuggest())
        .setupSuggestionsForKeyboard(Mockito.anyList(), Mockito.any());
    Mockito.verify(mImeServiceUnderTest.getSuggest()).resetNextWordSentence();
  }

  @Test
  public void testNotSetDictionaryOnNonOverridePrefs() {
    mImeServiceUnderTest.simulateKeyPress('h');

    Mockito.reset(mImeServiceUnderTest.getSuggest());

    SharedPrefsHelper.setPrefsValue("bsbsbsbs", "dictionary_sdfsdfsd");
    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never())
        .setupSuggestionsForKeyboard(Mockito.anyList(), Mockito.any());
    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never()).resetNextWordSentence();

    KeyboardDefinition currentKeyboard = mImeServiceUnderTest.getCurrentKeyboardForTests();
    SharedPrefsHelper.setPrefsValue(
        /*no prefix*/ currentKeyboard.getKeyboardId() + "_override_dictionary",
        "dictionary_sdfsdfsd");
    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never())
        .setupSuggestionsForKeyboard(Mockito.anyList(), Mockito.any());
    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never()).resetNextWordSentence();

    SharedPrefsHelper.setPrefsValue(
        KeyboardFactory.PREF_ID_PREFIX + currentKeyboard.getKeyboardId() /*no postfix*/,
        "dictionary_sdfsdfsd");
    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never())
        .setupSuggestionsForKeyboard(Mockito.anyList(), Mockito.any());
    // this will be called, since abortSuggestions is called (the prefix matches).
    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.atLeastOnce())
        .resetNextWordSentence();
  }
}
