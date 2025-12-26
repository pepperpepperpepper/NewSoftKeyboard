package wtf.uhoh.newsoftkeyboard.app.ime;

import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class ShiftStateSmokeTest extends ImeServiceBaseTest {

  @Before
  public void enableRealCapsMode() {
    simulateFinishInputFlow();
    mImeServiceUnderTest.getTestInputConnection().setRealCapsMode(true);
  }

  @Test
  public void capsWordsShiftsViewOnStart() {
    Mockito.clearInvocations(mImeServiceUnderTest.getSpiedKeyboardView());
    EditorInfo editorInfo = createEditorInfoWithCaps(TextUtils.CAP_MODE_WORDS);
    simulateOnStartInputFlow(false, editorInfo);
    Assert.assertTrue(mImeServiceUnderTest.getCurrentKeyboardForTests().keyboardSupportShift());
    Assert.assertTrue(mImeServiceUnderTest.getCurrentKeyboardForTests().isShifted());
    Mockito.verify(mImeServiceUnderTest.getSpiedKeyboardView(), Mockito.atLeastOnce())
        .setShifted(true);
  }

  private EditorInfo createEditorInfoWithCaps(int capsFlag) {
    EditorInfo editorInfo = createEditorInfoTextWithSuggestionsForSetUp();
    editorInfo.inputType |= capsFlag;
    return editorInfo;
  }
}
