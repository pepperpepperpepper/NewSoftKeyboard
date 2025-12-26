package wtf.uhoh.newsoftkeyboard.app.ime;

import android.view.inputmethod.EditorInfo;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.app.testing.TestableImeService;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceIncognitoTest extends ImeServiceBaseTest {

  @Test
  public void testSetsIncognitoWhenInputFieldRequestsIt() {
    simulateFinishInputFlow();

    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
    simulateOnStartInputFlow(
        false,
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE | EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING, 0));
    Assert.assertTrue(mImeServiceUnderTest.getSuggest().isIncognitoMode());
  }

  @Test
  public void testSetsIncognitoWhenInputFieldRequestsItWithSendAction() {
    simulateFinishInputFlow();

    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
    simulateOnStartInputFlow(
        false,
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_SEND | EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING, 0));
    Assert.assertTrue(mImeServiceUnderTest.getSuggest().isIncognitoMode());
  }

  @Test
  public void testSetsIncognitoWhenPasswordInputFieldNumber() {
    simulateFinishInputFlow();

    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
    simulateOnStartInputFlow(
        false,
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD));
    Assert.assertTrue(mImeServiceUnderTest.getSuggest().isIncognitoMode());
  }

  @Test
  public void testDoesNotSetIncognitoWhenInputFieldNumberButNotPassword() {
    simulateFinishInputFlow();

    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
    simulateOnStartInputFlow(
        false,
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_VARIATION_NORMAL));
    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
  }

  @Test
  public void testDoesNotSetIncognitoWhenInputFieldNumberButNotNumberPassword() {
    simulateFinishInputFlow();

    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
    simulateOnStartInputFlow(
        false,
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD));
    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
  }

  @Test
  public void testSetsIncognitoWhenPasswordInputField() {
    simulateFinishInputFlow();

    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
    simulateOnStartInputFlow(
        false,
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD));
    Assert.assertTrue(mImeServiceUnderTest.getSuggest().isIncognitoMode());
  }

  @Test
  public void testDoesNotSetIncognitoWhenPasswordInputFieldButNotText() {
    simulateFinishInputFlow();

    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
    simulateOnStartInputFlow(
        false,
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE, EditorInfo.TYPE_TEXT_VARIATION_PASSWORD));
    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
  }

  @Test
  public void testDoesNotSetIncognitoWhenInputFieldTextButNormal() {
    simulateFinishInputFlow();

    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
    simulateOnStartInputFlow(
        false,
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_NORMAL));
    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
  }

  @Test
  public void testDoesNotSetIncognitoWhenInputFieldTextButNotPassword() {
    simulateFinishInputFlow();

    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
    simulateOnStartInputFlow(
        false,
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_LONG_MESSAGE));
    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
  }

  @Test
  public void testDoesNotSetIncognitoWhenInputFieldTextButNotTextPassword() {
    simulateFinishInputFlow();

    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
    simulateOnStartInputFlow(
        false,
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD));
    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
  }

  @Test
  public void testSetsIncognitoWhenPasswordInputFieldVisible() {
    simulateFinishInputFlow();

    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
    simulateOnStartInputFlow(
        false,
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD));
    Assert.assertTrue(mImeServiceUnderTest.getSuggest().isIncognitoMode());
  }

  @Test
  public void testSetsIncognitoWhenPasswordInputFieldWeb() {
    simulateFinishInputFlow();

    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
    simulateOnStartInputFlow(
        false,
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE,
            EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD));
    Assert.assertTrue(mImeServiceUnderTest.getSuggest().isIncognitoMode());
  }

  @Test
  public void testClearsIncognitoOnNewFieldAfterMomentary() {
    simulateFinishInputFlow();

    simulateOnStartInputFlow(
        false,
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE | EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING, 0));
    Assert.assertTrue(mImeServiceUnderTest.getSuggest().isIncognitoMode());

    simulateFinishInputFlow();
    simulateOnStartInputFlow(
        false, TestableImeService.createEditorInfo(EditorInfo.IME_ACTION_NONE, 0));

    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
  }

  @Test
  public void testClearsIncognitoWhileInMomentaryInputFieldWhenUserRequestsToClear() {
    simulateFinishInputFlow();

    simulateOnStartInputFlow(
        false,
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE + EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING, 0));
    Assert.assertTrue(mImeServiceUnderTest.getSuggest().isIncognitoMode());

    mImeServiceUnderTest.setIncognito(false, true);

    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());
  }

  @Test
  public void testDoesNotClearIncognitoOnNewFieldUserRequestIncognito() {
    mImeServiceUnderTest.setIncognito(true, true);
    Assert.assertTrue(mImeServiceUnderTest.getSuggest().isIncognitoMode());

    simulateFinishInputFlow();

    simulateOnStartInputFlow(
        false, TestableImeService.createEditorInfo(EditorInfo.IME_ACTION_NONE, 0));
    // still incognito
    Assert.assertTrue(mImeServiceUnderTest.getSuggest().isIncognitoMode());
  }

  @Test
  public void testDoesNotClearIncognitoOnNewFieldUserRequestIncognitoAfterMomentary() {
    mImeServiceUnderTest.setIncognito(true, true);
    Assert.assertTrue(mImeServiceUnderTest.getSuggest().isIncognitoMode());

    simulateFinishInputFlow();

    simulateOnStartInputFlow(
        false,
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE + EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING, 0));

    Assert.assertTrue(mImeServiceUnderTest.getSuggest().isIncognitoMode());

    simulateFinishInputFlow();

    simulateOnStartInputFlow(
        false, TestableImeService.createEditorInfo(EditorInfo.IME_ACTION_NONE, 0));

    Assert.assertTrue(mImeServiceUnderTest.getSuggest().isIncognitoMode());
  }

  @Test
  public void testMomentaryIncognitoAfterUserClearsPreviousInputField() {
    simulateFinishInputFlow();

    simulateOnStartInputFlow(
        false,
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE + EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING, 0));
    Assert.assertTrue(mImeServiceUnderTest.getSuggest().isIncognitoMode());

    mImeServiceUnderTest.setIncognito(false, true);
    Assert.assertFalse(mImeServiceUnderTest.getSuggest().isIncognitoMode());

    simulateFinishInputFlow();

    simulateOnStartInputFlow(
        false,
        TestableImeService.createEditorInfo(
            EditorInfo.IME_ACTION_NONE + EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING, 0));

    Assert.assertTrue(mImeServiceUnderTest.getSuggest().isIncognitoMode());
  }
}
