package wtf.uhoh.newsoftkeyboard.app.ime;

import com.anysoftkeyboard.api.KeyCodes;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewWithExtraDraw;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.extradraw.ExtraDraw;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.extradraw.PopTextExtraDraw;
import wtf.uhoh.newsoftkeyboard.app.testing.TestInputConnection;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServicePopTextTest extends ImeServiceBaseTest {

  @Test
  public void testDefaultPopTextOutOfKeyOnCorrection() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");
    // pressing SPACE will auto-correct and pop the text out of the key
    Assert.assertEquals("hel", inputConnection.getCurrentTextInInputConnection());
    verifyNothingAddedInteractions();
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("he'll ", inputConnection.getCurrentTextInInputConnection());
    verifyPopText("he'll");

    // regular-word
    mImeServiceUnderTest.simulateTextTyping("gggg");
    verifySuggestions(true, "gggg");
    verifyNothingAddedInteractions();
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("he'll gggg ", inputConnection.getCurrentTextInInputConnection());
    verifyNothingAddedInteractions();
  }

  @Test
  public void testWordRevert() {
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");
    // pressing SPACE will auto-correct and pop the text out of the key
    Assert.assertEquals("hel", inputConnection.getCurrentTextInInputConnection());
    verifyNothingAddedInteractions();
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("he'll ", inputConnection.getCurrentTextInInputConnection());

    ArgumentCaptor<ExtraDraw> popTextCaptor = ArgumentCaptor.forClass(ExtraDraw.class);
    Mockito.verify(((KeyboardViewWithExtraDraw) mImeServiceUnderTest.getInputView()))
        .addExtraDraw(popTextCaptor.capture());
    Assert.assertTrue(popTextCaptor.getValue() instanceof PopTextExtraDraw.PopOut);
    PopTextExtraDraw popTextExtraDraw = (PopTextExtraDraw) popTextCaptor.getValue();
    Assert.assertEquals("he'll", popTextExtraDraw.getPopText().toString());
    Mockito.reset(mImeServiceUnderTest.getInputView());

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);

    popTextCaptor = ArgumentCaptor.forClass(ExtraDraw.class);
    Mockito.verify(((KeyboardViewWithExtraDraw) mImeServiceUnderTest.getInputView()))
        .addExtraDraw(popTextCaptor.capture());
    Assert.assertTrue(popTextCaptor.getValue() instanceof PopTextExtraDraw.PopIn);
    Assert.assertEquals(
        "he'll", ((PopTextExtraDraw) popTextCaptor.getValue()).getPopText().toString());
  }

  @Test
  public void testAllPopTextOutOfKeyOnKeyPressAndCorrection() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_pop_text_option, "any_key");

    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("he'll ", inputConnection.getCurrentTextInInputConnection());

    ArgumentCaptor<ExtraDraw> popTextCaptor = ArgumentCaptor.forClass(ExtraDraw.class);
    Mockito.verify(
            ((KeyboardViewWithExtraDraw) mImeServiceUnderTest.getInputView()), Mockito.times(4))
        .addExtraDraw(popTextCaptor.capture());

    Assert.assertEquals(4, popTextCaptor.getAllValues().size());
    for (ExtraDraw extraDraw : popTextCaptor.getAllValues()) {
      Assert.assertTrue(extraDraw instanceof PopTextExtraDraw.PopOut);
    }
    Assert.assertEquals(
        "h", ((PopTextExtraDraw) popTextCaptor.getAllValues().get(0)).getPopText().toString());
    Assert.assertEquals(
        "e", ((PopTextExtraDraw) popTextCaptor.getAllValues().get(1)).getPopText().toString());
    Assert.assertEquals(
        "l", ((PopTextExtraDraw) popTextCaptor.getAllValues().get(2)).getPopText().toString());
    Assert.assertEquals(
        "he'll", ((PopTextExtraDraw) popTextCaptor.getAllValues().get(3)).getPopText().toString());
  }

  @Test
  public void testAllWordsPopTextOutOfKeyOnKeyPressAndCorrection() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_pop_text_option, "on_word");
    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");
    // pressing SPACE will auto-correct and pop the text out of the key
    Assert.assertEquals("hel", inputConnection.getCurrentTextInInputConnection());
    verifyNothingAddedInteractions();
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("he'll ", inputConnection.getCurrentTextInInputConnection());
    verifyPopText("he'll");
    // regular-word
    mImeServiceUnderTest.simulateTextTyping("gggg");
    verifySuggestions(true, "gggg");
    verifyNothingAddedInteractions();
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("he'll gggg ", inputConnection.getCurrentTextInInputConnection());

    verifyPopText("gggg");
  }

  @Test
  public void testRestorePrefOnServiceRestart() throws Exception {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_pop_text_option, "on_word");
    mImeServiceController.destroy();
    // restarting
    setUpForImeServiceBase();

    simulateOnStartInputFlow();

    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");
    // pressing SPACE will auto-correct and pop the text out of the key
    Assert.assertEquals("hel", inputConnection.getCurrentTextInInputConnection());
    verifyNothingAddedInteractions();
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("he'll ", inputConnection.getCurrentTextInInputConnection());
    verifyPopText("he'll");
    // regular-word
    mImeServiceUnderTest.simulateTextTyping("gggg");
    verifySuggestions(true, "gggg");
    verifyNothingAddedInteractions();
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("he'll gggg ", inputConnection.getCurrentTextInInputConnection());

    verifyPopText("gggg");
  }

  @Test
  public void testDoesNotPopTextWhenManuallyPicked() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_pop_text_option, "on_word");
    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");
    verifyNothingAddedInteractions();
    mImeServiceUnderTest.pickSuggestionManually(1, "hell");
    verifyNothingAddedInteractions();
  }

  @Test
  public void testDoesNotCrashOnPopTextWhenFunctionalKeyPress() {
    simulateFinishInputFlow();
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_pop_text_option, "any_key");
    simulateOnStartInputFlow();

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);

    Assert.assertEquals(
        "symbols_keyboard", mImeServiceUnderTest.getCurrentKeyboardForTests().getKeyboardId());
    verifyNothingAddedInteractions();
  }

  @Test
  public void testNeverPopTextOut() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_pop_text_option, "never");

    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("he'll ", inputConnection.getCurrentTextInInputConnection());

    verifyNothingAddedInteractions();
  }

  @Test
  public void testDefaultSwitchCaseSameAsNever() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_pop_text_option, "blahblah");

    TestInputConnection inputConnection =
        (TestInputConnection) mImeServiceUnderTest.getCurrentInputConnection();

    mImeServiceUnderTest.simulateTextTyping("hel");
    verifySuggestions(true, "hel", "he'll", "hello", "hell");
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("he'll ", inputConnection.getCurrentTextInInputConnection());

    verifyNothingAddedInteractions();
  }

  private void verifyNothingAddedInteractions() {
    Mockito.verify(
            ((KeyboardViewWithExtraDraw) mImeServiceUnderTest.getInputView()), Mockito.never())
        .addExtraDraw(Mockito.any());
  }

  private void verifyPopText(String text) {
    ArgumentCaptor<ExtraDraw> popTextCaptor = ArgumentCaptor.forClass(ExtraDraw.class);
    Mockito.verify(((KeyboardViewWithExtraDraw) mImeServiceUnderTest.getInputView()))
        .addExtraDraw(popTextCaptor.capture());
    Assert.assertTrue(popTextCaptor.getValue() instanceof PopTextExtraDraw.PopOut);
    Assert.assertEquals(
        text, ((PopTextExtraDraw) popTextCaptor.getValue()).getPopText().toString());
    Mockito.reset(mImeServiceUnderTest.getInputView());
  }
}
