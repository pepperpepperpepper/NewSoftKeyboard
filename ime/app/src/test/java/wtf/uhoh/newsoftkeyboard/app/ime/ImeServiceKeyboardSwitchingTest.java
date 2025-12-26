package wtf.uhoh.newsoftkeyboard.app.ime;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.app.Application;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.view.inputmethod.EditorInfo;
import androidx.appcompat.app.AlertDialog;
import androidx.test.core.app.ApplicationProvider;
import com.anysoftkeyboard.api.KeyCodes;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Shadows;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.GenericKeyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardFactory;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardSwitcher;
import wtf.uhoh.newsoftkeyboard.app.testing.AddOnTestUtils;
import wtf.uhoh.newsoftkeyboard.app.testing.TestableImeService;
import wtf.uhoh.newsoftkeyboard.app.ui.settings.MainSettingsActivity;
import wtf.uhoh.newsoftkeyboard.testing.GeneralDialogTestUtil;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceKeyboardSwitchingTest extends ImeServiceBaseTest {

  @Test
  public void testSwitchToSymbols() {
    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardName(),
        getApplicationContext().getString(R.string.english_keyboard));
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardName(),
        getApplicationContext().getString(R.string.symbols_keyboard));
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardName(),
        getApplicationContext().getString(R.string.symbols_alt_keyboard));
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardName(),
        getApplicationContext().getString(R.string.symbols_numbers_keyboard));
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardName(),
        getApplicationContext().getString(R.string.symbols_keyboard));
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardName(),
        getApplicationContext().getString(R.string.english_keyboard));
  }

  @Test
  public void testCreateOrUseCacheKeyboard() {
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    verifyCreatedGenericKeyboard("symbols_keyboard", KeyboardSwitcher.INPUT_MODE_TEXT);
    final KeyboardDefinition symbolsKeyboard = mImeServiceUnderTest.getCurrentKeyboardForTests();
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    verifyCreatedGenericKeyboard("alt_symbols_keyboard", KeyboardSwitcher.INPUT_MODE_TEXT);
    final KeyboardDefinition altSymbolsKeyboard = mImeServiceUnderTest.getCurrentKeyboardForTests();
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    verifyCreatedGenericKeyboard("alt_numbers_symbols_keyboard", KeyboardSwitcher.INPUT_MODE_TEXT);
    final KeyboardDefinition altNumbersSymbolsKeyboard =
        mImeServiceUnderTest.getCurrentKeyboardForTests();
    Assert.assertNotSame(symbolsKeyboard, altSymbolsKeyboard);
    Assert.assertNotSame(altSymbolsKeyboard, altNumbersSymbolsKeyboard);
    Assert.assertNotSame(altNumbersSymbolsKeyboard, symbolsKeyboard);
    // already created
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    Assert.assertSame(symbolsKeyboard, mImeServiceUnderTest.getCurrentKeyboardForTests());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    Assert.assertSame(symbolsKeyboard, mImeServiceUnderTest.getCurrentKeyboardForTests());
  }

  /** Solves https://github.com/AnySoftKeyboard/AnySoftKeyboard/issues/719 */
  @Test
  public void testInvalidateCachedLayoutsWhenInputModeChanges() {
    mImeServiceUnderTest.onFinishInputView(true);
    mImeServiceUnderTest.onFinishInput();

    EditorInfo editorInfo =
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT + EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
    mImeServiceUnderTest.onStartInput(editorInfo, false);
    mImeServiceUnderTest.onStartInputView(editorInfo, false);

    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    Assert.assertEquals(
        Keyboard.KEYBOARD_ROW_MODE_EMAIL,
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardMode());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);

    Assert.assertEquals(
        "symbols_keyboard", mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    Assert.assertEquals(
        Keyboard.KEYBOARD_ROW_MODE_EMAIL,
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardMode());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);

    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    Assert.assertEquals(
        Keyboard.KEYBOARD_ROW_MODE_EMAIL,
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardMode());

    // switching input types
    mImeServiceUnderTest.onFinishInputView(true);
    mImeServiceUnderTest.onFinishInput();

    editorInfo =
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT + EditorInfo.TYPE_TEXT_VARIATION_URI);
    mImeServiceUnderTest.onStartInput(editorInfo, false);
    mImeServiceUnderTest.onStartInputView(editorInfo, false);

    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    Assert.assertEquals(
        Keyboard.KEYBOARD_ROW_MODE_URL,
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardMode());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);

    Assert.assertEquals(
        "symbols_keyboard", mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    Assert.assertEquals(
        Keyboard.KEYBOARD_ROW_MODE_URL,
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardMode());
  }

  @Test
  public void testCreateOrUseCacheKeyboardWhen16KeysEnabled() {
    SharedPrefsHelper.setPrefsValue("settings_key_use_16_keys_symbols_keyboards", true);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    verifyCreatedGenericKeyboard("symbols_keyboard", KeyboardSwitcher.INPUT_MODE_TEXT);
    final KeyboardDefinition symbolsKeyboard = mImeServiceUnderTest.getCurrentKeyboardForTests();
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    verifyCreatedGenericKeyboard("alt_symbols_keyboard", KeyboardSwitcher.INPUT_MODE_TEXT);
    final KeyboardDefinition altSymbolsKeyboard = mImeServiceUnderTest.getCurrentKeyboardForTests();
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    verifyCreatedGenericKeyboard("alt_numbers_symbols_keyboard", KeyboardSwitcher.INPUT_MODE_TEXT);
    final KeyboardDefinition altNumbersSymbolsKeyboard =
        mImeServiceUnderTest.getCurrentKeyboardForTests();
    // all newly created
    Assert.assertNotSame(symbolsKeyboard, altSymbolsKeyboard);
    Assert.assertNotSame(altSymbolsKeyboard, altNumbersSymbolsKeyboard);
    Assert.assertNotSame(altNumbersSymbolsKeyboard, symbolsKeyboard);

    // now, cycling should use cached instances
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    verifyCreatedGenericKeyboard("symbols_keyboard", KeyboardSwitcher.INPUT_MODE_TEXT);
    Assert.assertSame(symbolsKeyboard, mImeServiceUnderTest.getCurrentKeyboardForTests());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    verifyCreatedGenericKeyboard("alt_symbols_keyboard", KeyboardSwitcher.INPUT_MODE_TEXT);
    Assert.assertSame(altSymbolsKeyboard, mImeServiceUnderTest.getCurrentKeyboardForTests());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    verifyCreatedGenericKeyboard("alt_numbers_symbols_keyboard", KeyboardSwitcher.INPUT_MODE_TEXT);
    Assert.assertSame(altNumbersSymbolsKeyboard, mImeServiceUnderTest.getCurrentKeyboardForTests());
  }

  private void verifyCreatedGenericKeyboard(String keyboardId, int mode) {
    Assert.assertTrue(mImeServiceUnderTest.getCurrentKeyboardForTests() instanceof GenericKeyboard);
    Assert.assertEquals(mode, mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardMode());
    Assert.assertEquals(
        keyboardId, mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
  }

  @Test
  public void testModeSwitch() {
    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardName(),
        getApplicationContext().getString(R.string.english_keyboard));
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.KEYBOARD_MODE_CHANGE);
    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardName(),
        getApplicationContext().getString(R.string.symbols_keyboard));
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.KEYBOARD_MODE_CHANGE);
    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardName(),
        getApplicationContext().getString(R.string.english_keyboard));
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.KEYBOARD_MODE_CHANGE);
    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardName(),
        getApplicationContext().getString(R.string.symbols_keyboard));
  }

  @Test
  public void testModeSwitchLoadsDictionary() {
    Mockito.reset(mImeServiceUnderTest.getSuggest());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.KEYBOARD_MODE_CHANGE);
    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never())
        .setupSuggestionsForKeyboard(Mockito.anyList(), Mockito.any());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.KEYBOARD_MODE_CHANGE);
    Mockito.verify(mImeServiceUnderTest.getSuggest())
        .setupSuggestionsForKeyboard(Mockito.anyList(), Mockito.any());
  }

  @Test
  public void testOnKeyboardSetLoadsDictionary() {
    KeyboardDefinition alphabetKeyboard = mImeServiceUnderTest.getCurrentKeyboardForTests();
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.KEYBOARD_MODE_CHANGE);
    KeyboardDefinition symbolsKeyboard = mImeServiceUnderTest.getCurrentKeyboardForTests();

    Mockito.reset(mImeServiceUnderTest.getSuggest());
    mImeServiceUnderTest.onSymbolsKeyboardSet(symbolsKeyboard);
    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.never())
        .setupSuggestionsForKeyboard(Mockito.anyList(), Mockito.any());

    mImeServiceUnderTest.onAlphabetKeyboardSet(alphabetKeyboard);

    Mockito.verify(mImeServiceUnderTest.getSuggest())
        .setupSuggestionsForKeyboard(Mockito.anyList(), Mockito.any());
  }

  @Test
  public void testModeSwitchesOnConfigurationChange() {
    Configuration configuration = mImeServiceUnderTest.getResources().getConfiguration();
    configuration.orientation = Configuration.ORIENTATION_PORTRAIT;
    mImeServiceUnderTest.onConfigurationChanged(configuration);
    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardName(),
        getApplicationContext().getString(R.string.english_keyboard));
    configuration.orientation = Configuration.ORIENTATION_LANDSCAPE;
    mImeServiceUnderTest.onConfigurationChanged(configuration);
    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardName(),
        getApplicationContext().getString(R.string.english_keyboard));

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.KEYBOARD_MODE_CHANGE);
    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardName(),
        getApplicationContext().getString(R.string.symbols_keyboard));

    configuration.orientation = Configuration.ORIENTATION_PORTRAIT;
    mImeServiceUnderTest.onConfigurationChanged(configuration);
    // switches back to symbols since this is a non-restarting event.
    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardName(),
        getApplicationContext().getString(R.string.english_keyboard));
  }

  @Test
  public void testCanNotSwitchWhenInLockedMode() {
    mImeServiceUnderTest.onFinishInputView(true);
    mImeServiceUnderTest.onFinishInput();

    EditorInfo editorInfo =
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE, EditorInfo.TYPE_CLASS_PHONE);
    mImeServiceUnderTest.onStartInput(editorInfo, true);
    mImeServiceUnderTest.onStartInputView(editorInfo, true);

    final KeyboardDefinition phoneKeyboardInstance =
        mImeServiceUnderTest.getCurrentKeyboardForTests();
    Assert.assertEquals(
        getApplicationContext().getString(R.string.symbols_phone_keyboard),
        phoneKeyboardInstance.getKeyboardName());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.KEYBOARD_MODE_CHANGE);
    Assert.assertSame(phoneKeyboardInstance, mImeServiceUnderTest.getCurrentKeyboardForTests());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    Assert.assertSame(phoneKeyboardInstance, mImeServiceUnderTest.getCurrentKeyboardForTests());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);
    Assert.assertSame(phoneKeyboardInstance, mImeServiceUnderTest.getCurrentKeyboardForTests());

    // and making sure it is unlocked when restarting the input connection
    mImeServiceUnderTest.onFinishInputView(true);
    mImeServiceUnderTest.onFinishInput();
    editorInfo = TestableImeService.createEditorInfoTextWithSuggestions();
    mImeServiceUnderTest.onStartInput(editorInfo, true);
    mImeServiceUnderTest.onStartInputView(editorInfo, true);

    Assert.assertNotSame(phoneKeyboardInstance, mImeServiceUnderTest.getCurrentKeyboardForTests());
    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardName(),
        getApplicationContext().getString(R.string.english_keyboard));
  }

  @Test
  public void testShowSelectedKeyboardForURLField() {
    Resources resources = getApplicationContext().getResources();
    // default value should be first keyboard
    final KeyboardFactory keyboardFactory =
        NskApplicationBase.getKeyboardFactory(getApplicationContext());
    Assert.assertEquals(
        resources.getString(R.string.settings_default_keyboard_id),
        keyboardFactory.getEnabledIds().get(0));

    AddOnTestUtils.ensureKeyboardAtIndexEnabled(0, true);
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(1, true);
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(2, true);

    EditorInfo editorInfo =
        TestableImeService.createEditorInfo(EditorInfo.IME_ACTION_NONE, EditorInfo.TYPE_CLASS_TEXT);
    mImeServiceUnderTest.onStartInput(editorInfo, true);
    mImeServiceUnderTest.onCreateInputView();
    mImeServiceUnderTest.onStartInputView(editorInfo, true);

    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId(),
        keyboardFactory.getEnabledIds().get(0));

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);

    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId(),
        keyboardFactory.getEnabledIds().get(1));

    mImeServiceUnderTest.onFinishInputView(false);
    mImeServiceUnderTest.onFinishInput();
    editorInfo = TestableImeService.createEditorInfoTextWithSuggestions();
    mImeServiceUnderTest.onStartInput(editorInfo, false);
    mImeServiceUnderTest.onStartInputView(editorInfo, false);

    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId(),
        keyboardFactory.getEnabledIds().get(1));

    mImeServiceUnderTest.onFinishInputView(false);
    mImeServiceUnderTest.onFinishInput();
    editorInfo =
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT + EditorInfo.TYPE_TEXT_VARIATION_URI);
    mImeServiceUnderTest.onStartInput(editorInfo, false);
    mImeServiceUnderTest.onStartInputView(editorInfo, false);

    // automatically switched to the keyboard in the prefs
    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId(),
        keyboardFactory.getEnabledIds().get(0));

    mImeServiceUnderTest.onFinishInputView(false);
    mImeServiceUnderTest.onFinishInput();

    SharedPrefsHelper.setPrefsValue(
        R.string.settings_key_layout_for_internet_fields,
        keyboardFactory.getEnabledIds().get(2).toString());

    mImeServiceUnderTest.onStartInput(editorInfo, false);
    mImeServiceUnderTest.onStartInputView(editorInfo, false);

    // automatically switched to the keyboard in the prefs
    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId(),
        keyboardFactory.getEnabledIds().get(2));
  }

  @Test
  public void testShowPreviousKeyboardIfInternetKeyboardPrefIdIsInvalid() {
    final KeyboardFactory keyboardFactory =
        NskApplicationBase.getKeyboardFactory(getApplicationContext());

    AddOnTestUtils.ensureKeyboardAtIndexEnabled(0, true);
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(1, true);
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(2, true);

    EditorInfo editorInfo =
        TestableImeService.createEditorInfo(EditorInfo.IME_ACTION_NONE, EditorInfo.TYPE_CLASS_TEXT);
    mImeServiceUnderTest.onStartInput(editorInfo, true);
    mImeServiceUnderTest.onCreateInputView();
    mImeServiceUnderTest.onStartInputView(editorInfo, true);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);

    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId(),
        keyboardFactory.getEnabledIds().get(1));

    mImeServiceUnderTest.onFinishInputView(false);
    mImeServiceUnderTest.onFinishInput();

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_layout_for_internet_fields, "none");

    editorInfo =
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT + EditorInfo.TYPE_TEXT_VARIATION_URI);

    mImeServiceUnderTest.onStartInput(editorInfo, false);
    mImeServiceUnderTest.onStartInputView(editorInfo, false);

    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId(),
        keyboardFactory.getEnabledIds().get(1));

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);

    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId(),
        keyboardFactory.getEnabledIds().get(2));

    mImeServiceUnderTest.onFinishInputView(false);
    mImeServiceUnderTest.onFinishInput();
    mImeServiceUnderTest.onStartInput(editorInfo, false);
    mImeServiceUnderTest.onStartInputView(editorInfo, false);

    Assert.assertEquals(
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId(),
        keyboardFactory.getEnabledIds().get(2));
  }

  @Test
  public void testLanguageDialogShowLanguagesAndSettings() {
    Assert.assertSame(
        GeneralDialogTestUtil.NO_DIALOG, GeneralDialogTestUtil.getLatestShownDialog());

    AddOnTestUtils.ensureKeyboardAtIndexEnabled(0, true);
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(1, true);
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(2, true);

    mImeServiceUnderTest.onKey(KeyCodes.MODE_ALPHABET_POPUP, null, 0, null, true);

    final AlertDialog latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertNotNull(latestAlertDialog);

    Assert.assertEquals(
        "Select keyboard", GeneralDialogTestUtil.getTitleFromDialog(latestAlertDialog));
    Assert.assertEquals(4, latestAlertDialog.getListView().getCount());

    Assert.assertEquals(
        getResText(R.string.english_keyboard),
        latestAlertDialog.getListView().getAdapter().getItem(0));
    Assert.assertEquals(
        getResText(R.string.compact_keyboard_16keys),
        latestAlertDialog.getListView().getAdapter().getItem(1));
    Assert.assertEquals(
        getResText(R.string.english_keyboard),
        latestAlertDialog.getListView().getAdapter().getItem(2));
    Assert.assertEquals(
        "Setup languages…", latestAlertDialog.getListView().getAdapter().getItem(3));
  }

  @Test
  public void testLanguageDialogSwitchLanguage() {
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(0, true);
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(1, true);
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(2, true);

    mImeServiceUnderTest.onKey(KeyCodes.MODE_ALPHABET_POPUP, null, 0, null, true);

    final AlertDialog latestAlertDialog = GeneralDialogTestUtil.getLatestShownDialog();
    Assert.assertEquals(
        "c7535083-4fe6-49dc-81aa-c5438a1a343a",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());

    Shadows.shadowOf(latestAlertDialog.getListView()).performItemClick(1);

    Assert.assertEquals(
        "12335055-4aa6-49dc-8456-c7d38a1a5123",
        mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
  }

  @Test
  public void testLanguageDialogGoToSettings() {
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(0, true);
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(1, true);
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(2, true);

    mImeServiceUnderTest.onKey(KeyCodes.MODE_ALPHABET_POPUP, null, 0, null, true);

    Assert.assertNull(
        Shadows.shadowOf((Application) ApplicationProvider.getApplicationContext())
            .getNextStartedActivity());

    Shadows.shadowOf(GeneralDialogTestUtil.getLatestShownDialog().getListView())
        .performItemClick(3);
    Intent settingsIntent =
        Shadows.shadowOf((Application) ApplicationProvider.getApplicationContext())
            .getNextStartedActivity();
    Assert.assertNotNull(settingsIntent);
    Assert.assertEquals(
        getApplicationContext().getPackageName(), settingsIntent.getComponent().getPackageName());
    Assert.assertEquals(
        MainSettingsActivity.class.getName(), settingsIntent.getComponent().getClassName());
    Assert.assertEquals(
        Uri.parse(getApplicationContext().getString(R.string.deeplink_url_keyboards)),
        settingsIntent.getData());
    Assert.assertEquals(Intent.ACTION_VIEW, settingsIntent.getAction());
    Assert.assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, settingsIntent.getFlags());
  }
}
