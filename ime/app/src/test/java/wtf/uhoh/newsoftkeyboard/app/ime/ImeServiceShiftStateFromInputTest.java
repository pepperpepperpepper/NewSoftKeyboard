package wtf.uhoh.newsoftkeyboard.app.ime;

import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import androidx.core.util.Pair;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import wtf.uhoh.newsoftkeyboard.app.testing.ShadowDictionaryAddOnAndBuilder;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceShiftStateFromInputTest extends ImeServiceBaseTest {

  @Override
  public void setUpForImeServiceBase() throws Exception {
    ShadowDictionaryAddOnAndBuilder.setDictionaryOverrides(
        "English",
        Arrays.asList(
            Pair.create("he", 187),
            Pair.create("he'll", 94),
            Pair.create("hell", 108),
            Pair.create("hello", 120),
            Pair.create("is", 90),
            Pair.create("face", 141)));
    super.setUpForImeServiceBase();
  }

  @Before
  public void setupForShiftTests() {
    simulateFinishInputFlow();
    mImeServiceUnderTest.getTestInputConnection().setRealCapsMode(true);
  }

  @Test
  public void testShiftSentences() {
    simulateOnStartInputFlow(false, createEditorInfoWithCaps(TextUtils.CAP_MODE_SENTENCES));
    mImeServiceUnderTest.simulateTextTyping("hello my name is bond. james bond");
    Assert.assertEquals(
        "Hello my name is bond. James bond", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testShiftNever() {
    simulateOnStartInputFlow(false, createEditorInfoWithCaps(0));
    mImeServiceUnderTest.simulateTextTyping("hello my name is bond. james bond");
    Assert.assertEquals(
        "hello my name is bond. james bond", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testShiftWords() {
    simulateOnStartInputFlow(false, createEditorInfoWithCaps(TextUtils.CAP_MODE_WORDS));
    mImeServiceUnderTest.simulateTextTyping("hello my name is bond. james bond");
    Assert.assertEquals(
        "Hello My Name Is Bond. James Bond", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testShiftCaps() {
    simulateOnStartInputFlow(false, createEditorInfoWithCaps(TextUtils.CAP_MODE_CHARACTERS));
    mImeServiceUnderTest.simulateTextTyping("hello my name is bond. james bond");
    Assert.assertEquals(
        "HELLO MY NAME IS BOND. JAMES BOND", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  private EditorInfo createEditorInfoWithCaps(int capsFlag) {
    EditorInfo editorInfo = createEditorInfoTextWithSuggestionsForSetUp();
    editorInfo.inputType |= capsFlag;
    return editorInfo;
  }
}
