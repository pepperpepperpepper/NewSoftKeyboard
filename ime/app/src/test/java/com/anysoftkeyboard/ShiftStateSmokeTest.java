package com.anysoftkeyboard;

import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(AnySoftKeyboardRobolectricTestRunner.class)
public class ShiftStateSmokeTest extends AnySoftKeyboardBaseTest {

  @Before
  public void enableRealCapsMode() {
    simulateFinishInputFlow();
    mAnySoftKeyboardUnderTest.getTestInputConnection().setRealCapsMode(true);
  }

  @Test
  public void capsWordsShiftsViewOnStart() {
    Mockito.clearInvocations(mAnySoftKeyboardUnderTest.getSpiedKeyboardView());
    EditorInfo editorInfo = createEditorInfoWithCaps(TextUtils.CAP_MODE_WORDS);
    simulateOnStartInputFlow(false, editorInfo);
    Assert.assertTrue(mAnySoftKeyboardUnderTest.getCurrentKeyboardForTests().keyboardSupportShift());
    Assert.assertTrue(mAnySoftKeyboardUnderTest.getCurrentKeyboardForTests().isShifted());
    Mockito.verify(mAnySoftKeyboardUnderTest.getSpiedKeyboardView(), Mockito.atLeastOnce())
        .setShifted(true);
  }

  private EditorInfo createEditorInfoWithCaps(int capsFlag) {
    EditorInfo editorInfo = createEditorInfoTextWithSuggestionsForSetUp();
    editorInfo.inputType |= capsFlag;
    return editorInfo;
  }
}
