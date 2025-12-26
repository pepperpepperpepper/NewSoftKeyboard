package wtf.uhoh.newsoftkeyboard.app.ime;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.os.Build;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.testing.TestableImeService;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.O)
public class ImeServiceAutofillStripActionTest extends ImeServiceBaseTest {

  @Before
  public void setUpAutofill() {
    mImeServiceUnderTest.setAutofillAvailableOverride(true);
    mImeServiceUnderTest.resetAutofillRequestFlag();
  }

  @Test
  public void testShowsAutofillActionWhenHintsAvailable() {
    EditorInfo editorInfo = createEditorInfo();
    simulateOnStartInputFlow(false, editorInfo);

    View action =
        mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.autofill_strip_action_root);
    assertNotNull(action);
    assertTrue(mImeServiceUnderTest.wasAutofillRequested());
  }

  @Test
  public void testHidesAutofillActionWhenDisabled() {
    mImeServiceUnderTest.setAutofillAvailableOverride(false);
    EditorInfo editorInfo = createEditorInfo();
    simulateOnStartInputFlow(false, editorInfo);

    View action =
        mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.autofill_strip_action_root);
    assertNull(action);
  }

  @NonNull
  private static EditorInfo createEditorInfo() {
    return TestableImeService.createEditorInfo(
        EditorInfo.IME_ACTION_NONE, EditorInfo.TYPE_CLASS_TEXT);
  }
}
