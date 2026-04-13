package wtf.uhoh.newsoftkeyboard.app.ime;

import android.view.inputmethod.EditorInfo;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardSwitcher;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class InputFieldConfiguratorTest {

  @Test
  public void inputTypeNull_treatedAsTextButDisablesAutoSpaceAndAutoPick() {
    final EditorInfo editorInfo = new EditorInfo();
    editorInfo.inputType = 0;

    final KeyboardSwitcher keyboardSwitcher = Mockito.mock(KeyboardSwitcher.class);

    final InputFieldConfigurator.Result result =
        new InputFieldConfigurator()
            .configure(editorInfo, false, keyboardSwitcher, true, true, "test");

    Assert.assertFalse(result.predictionOn);
    Assert.assertFalse(result.autoSpace);
    Assert.assertFalse(result.inputFieldSupportsAutoPick);
    Mockito.verify(keyboardSwitcher)
        .setKeyboardMode(KeyboardSwitcher.INPUT_MODE_TEXT, editorInfo, false);
  }

  @Test
  public void inputTypeNull_connectBotDisablesPredictionsToAvoidComposingOnlyOutput() {
    final EditorInfo editorInfo = new EditorInfo();
    editorInfo.packageName = "org.connectbot";
    editorInfo.inputType = 0;

    final KeyboardSwitcher keyboardSwitcher = Mockito.mock(KeyboardSwitcher.class);

    final InputFieldConfigurator.Result result =
        new InputFieldConfigurator()
            .configure(editorInfo, false, keyboardSwitcher, true, true, "test");

    Assert.assertFalse(result.predictionOn);
    Assert.assertFalse(result.autoSpace);
    Assert.assertFalse(result.inputFieldSupportsAutoPick);
    Mockito.verify(keyboardSwitcher)
        .setKeyboardMode(KeyboardSwitcher.INPUT_MODE_TEXT, editorInfo, false);
  }

  @Test
  public void typeNullMask_connectBotDisablesPredictionsEvenIfNoSuggestionsFlagIsIgnored() {
    final EditorInfo editorInfo = new EditorInfo();
    editorInfo.packageName = "org.connectbot";
    editorInfo.inputType = EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS;

    final KeyboardSwitcher keyboardSwitcher = Mockito.mock(KeyboardSwitcher.class);

    final InputFieldConfigurator.Result result =
        new InputFieldConfigurator()
            .configure(editorInfo, false, keyboardSwitcher, true, false, "test");

    Assert.assertFalse(result.predictionOn);
    Assert.assertFalse(result.autoSpace);
    Assert.assertFalse(result.inputFieldSupportsAutoPick);
    Mockito.verify(keyboardSwitcher)
        .setKeyboardMode(KeyboardSwitcher.INPUT_MODE_TEXT, editorInfo, false);
  }

  @Test
  public void typeNullMaskWithNoSuggestionsFlag_respected_disablesPredictionsAndAutoSpace() {
    final EditorInfo editorInfo = new EditorInfo();
    editorInfo.inputType = EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS;

    final KeyboardSwitcher keyboardSwitcher = Mockito.mock(KeyboardSwitcher.class);

    final InputFieldConfigurator.Result result =
        new InputFieldConfigurator()
            .configure(editorInfo, false, keyboardSwitcher, true, true, "test");

    Assert.assertFalse(result.predictionOn);
    Assert.assertFalse(result.autoSpace);
    Assert.assertFalse(result.inputFieldSupportsAutoPick);
    Mockito.verify(keyboardSwitcher)
        .setKeyboardMode(KeyboardSwitcher.INPUT_MODE_TEXT, editorInfo, false);
  }

  @Test
  public void typeNullMaskWithNoSuggestionsFlag_ignored_disablesPredictionsAnyway() {
    final EditorInfo editorInfo = new EditorInfo();
    editorInfo.inputType = EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS;

    final KeyboardSwitcher keyboardSwitcher = Mockito.mock(KeyboardSwitcher.class);

    final InputFieldConfigurator.Result result =
        new InputFieldConfigurator()
            .configure(editorInfo, false, keyboardSwitcher, true, false, "test");

    Assert.assertFalse(result.predictionOn);
    Assert.assertFalse(result.autoSpace);
    Assert.assertFalse(result.inputFieldSupportsAutoPick);
    Mockito.verify(keyboardSwitcher)
        .setKeyboardMode(KeyboardSwitcher.INPUT_MODE_TEXT, editorInfo, false);
  }

  @Test
  public void typeNullMaskWithPasswordVariation_disablesPredictions() {
    final EditorInfo editorInfo = new EditorInfo();
    editorInfo.inputType = EditorInfo.TYPE_TEXT_VARIATION_PASSWORD;

    final KeyboardSwitcher keyboardSwitcher = Mockito.mock(KeyboardSwitcher.class);

    final InputFieldConfigurator.Result result =
        new InputFieldConfigurator()
            .configure(editorInfo, false, keyboardSwitcher, true, false, "test");

    Assert.assertFalse(result.predictionOn);
    Assert.assertFalse(result.autoSpace);
    Assert.assertFalse(result.inputFieldSupportsAutoPick);
    Mockito.verify(keyboardSwitcher)
        .setKeyboardMode(KeyboardSwitcher.INPUT_MODE_TEXT, editorInfo, false);
  }

  @Test
  public void noSuggestionsFlag_respected_disablesPredictions() {
    final EditorInfo editorInfo = new EditorInfo();
    editorInfo.inputType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS;

    final KeyboardSwitcher keyboardSwitcher = Mockito.mock(KeyboardSwitcher.class);

    final InputFieldConfigurator.Result result =
        new InputFieldConfigurator()
            .configure(
                editorInfo,
                /* restarting= */ false,
                keyboardSwitcher,
                /* prefsAutoSpace= */ true,
                /* respectNoSuggestionsFlag= */ true,
                /* logTag= */ "test");

    Assert.assertFalse(result.predictionOn);
    Mockito.verify(keyboardSwitcher)
        .setKeyboardMode(KeyboardSwitcher.INPUT_MODE_TEXT, editorInfo, false);
  }

  @Test
  public void noSuggestionsFlag_ignored_keepsPredictionsButDisablesAutoPick() {
    final EditorInfo editorInfo = new EditorInfo();
    editorInfo.inputType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS;

    final KeyboardSwitcher keyboardSwitcher = Mockito.mock(KeyboardSwitcher.class);

    final InputFieldConfigurator.Result result =
        new InputFieldConfigurator()
            .configure(
                editorInfo,
                /* restarting= */ false,
                keyboardSwitcher,
                /* prefsAutoSpace= */ true,
                /* respectNoSuggestionsFlag= */ false,
                /* logTag= */ "test");

    Assert.assertTrue(result.predictionOn);
    Assert.assertFalse(result.inputFieldSupportsAutoPick);
    Mockito.verify(keyboardSwitcher)
        .setKeyboardMode(KeyboardSwitcher.INPUT_MODE_TEXT, editorInfo, false);
  }

  @Test
  public void noSuggestionsFlag_onNonTextField_stillDisablesPredictions() {
    final EditorInfo editorInfo = new EditorInfo();
    editorInfo.inputType = EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS;

    final KeyboardSwitcher keyboardSwitcher = Mockito.mock(KeyboardSwitcher.class);

    final InputFieldConfigurator.Result result =
        new InputFieldConfigurator()
            .configure(
                editorInfo,
                /* restarting= */ false,
                keyboardSwitcher,
                /* prefsAutoSpace= */ true,
                /* respectNoSuggestionsFlag= */ false,
                /* logTag= */ "test");

    Assert.assertFalse(result.predictionOn);
    Mockito.verify(keyboardSwitcher)
        .setKeyboardMode(KeyboardSwitcher.INPUT_MODE_NUMBERS, editorInfo, false);
  }
}
