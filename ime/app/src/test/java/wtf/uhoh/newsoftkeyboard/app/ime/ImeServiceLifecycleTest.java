package wtf.uhoh.newsoftkeyboard.app.ime;

import android.graphics.Color;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ServiceController;
import wtf.uhoh.newsoftkeyboard.NewSoftKeyboardService;
import wtf.uhoh.newsoftkeyboard.app.testing.TestableImeService;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceLifecycleTest {

  private ServiceController<NewSoftKeyboardService> mImeServiceUnderTest;

  @Before
  public void setUp() throws Exception {
    mImeServiceUnderTest = Robolectric.buildService(NewSoftKeyboardService.class);
  }

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testSimpleLifeCycle() throws Exception {
    mImeServiceUnderTest.create().destroy();
  }

  @Test
  public void testOnCreateCandidatesView() throws Exception {
    // we do not use AOSP's candidates view mechanism.
    Assert.assertNull(mImeServiceUnderTest.create().get().onCreateCandidatesView());
  }

  @Test
  public void testKeyboardHiddenBehavior() throws Exception {
    ServiceController<TestableImeService> testableImeServiceController =
        Robolectric.buildService(TestableImeService.class);
    TestableImeService testableImeService = testableImeServiceController.create().get();
    Assert.assertTrue(testableImeService.isKeyboardViewHidden());

    final EditorInfo editorInfo = TestableImeService.createEditorInfoTextWithSuggestions();

    testableImeService.onCreateInputView();
    testableImeService.onStartInput(editorInfo, false);

    Assert.assertTrue(testableImeService.isKeyboardViewHidden());
    testableImeService.onStartInputView(editorInfo, false);
    Assert.assertFalse(testableImeService.isKeyboardViewHidden());

    testableImeServiceController.destroy();
    Assert.assertTrue(testableImeService.isKeyboardViewHidden());
  }

  @Test
  public void testKeyboardDoesNotCloseWhenUserCancelKey() throws Exception {
    ServiceController<TestableImeService> testableImeServiceController =
        Robolectric.buildService(TestableImeService.class);
    TestableImeService testableImeService = testableImeServiceController.create().get();
    final EditorInfo editorInfo = TestableImeService.createEditorInfoTextWithSuggestions();

    testableImeService.onCreateInputView();
    testableImeService.onStartInput(editorInfo, false);
    testableImeService.onStartInputView(editorInfo, false);
    Assert.assertFalse(testableImeService.isKeyboardViewHidden());

    testableImeService.onCancel();
    Assert.assertFalse(testableImeService.isKeyboardViewHidden());
  }

  @Test
  public void testExtractViewThemeSet() throws Exception {
    ServiceController<TestableImeService> testableImeServiceController =
        Robolectric.buildService(TestableImeService.class);
    TestableImeService testableImeService = testableImeServiceController.create().get();
    final EditorInfo editorInfo = TestableImeService.createEditorInfoTextWithSuggestions();

    testableImeService.onCreateInputView();
    testableImeService.onStartInput(editorInfo, false);
    testableImeService.onStartInputView(editorInfo, false);

    final View extractView = testableImeService.onCreateExtractTextView();
    Assert.assertNotNull(extractView);

    final EditText extractEditText = extractView.findViewById(android.R.id.inputExtractEditText);
    Assert.assertNotNull(extractEditText);

    testableImeService.updateFullscreenMode();

    int backgroundResId = Shadows.shadowOf(extractView.getBackground()).getCreatedFromResId();
    Assert.assertNotEquals(0, backgroundResId);
    Assert.assertEquals(Color.WHITE, extractEditText.getTextColors().getDefaultColor());
  }

  @Test
  public void testExtractViewThemeNotSetWithoutInputViewCreated() throws Exception {
    ServiceController<TestableImeService> testableImeServiceController =
        Robolectric.buildService(TestableImeService.class);
    TestableImeService testableImeService = testableImeServiceController.create().get();

    final View extractView = testableImeService.onCreateExtractTextView();
    Assert.assertNotNull(extractView);

    final EditText extractEditText = extractView.findViewById(android.R.id.inputExtractEditText);
    Assert.assertNotNull(extractEditText);

    testableImeService.updateFullscreenMode();

    Assert.assertNull(extractView.getBackground());
    Assert.assertNotEquals(Color.WHITE, extractEditText.getTextColors().getDefaultColor());
  }
}
