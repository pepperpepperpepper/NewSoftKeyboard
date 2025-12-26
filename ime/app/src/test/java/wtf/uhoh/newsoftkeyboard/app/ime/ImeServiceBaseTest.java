package wtf.uhoh.newsoftkeyboard.app.ime;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import android.app.Application;
import android.app.Service;
import android.inputmethodservice.AbstractInputMethodService;
import android.os.Build;
import android.os.IBinder;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.shadow.api.Shadow;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.CandidateView;
import wtf.uhoh.newsoftkeyboard.app.testing.InputMethodManagerShadow;
import wtf.uhoh.newsoftkeyboard.app.testing.TestInputConnection;
import wtf.uhoh.newsoftkeyboard.app.testing.TestableImeService;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;

@RunWith(NskRobolectricTestRunner.class)
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class ImeServiceBaseTest {

  protected TestableImeService mImeServiceUnderTest;

  protected IBinder mMockBinder;
  protected ServiceController<? extends TestableImeService> mImeServiceController;
  private InputMethodManagerShadow mInputMethodManagerShadow;
  private AbstractInputMethodService.AbstractInputMethodImpl mAbstractInputMethod;

  protected TestInputConnection getCurrentTestInputConnection() {
    return mImeServiceUnderTest.getTestInputConnection();
  }

  protected CandidateView getMockCandidateView() {
    return mImeServiceUnderTest.getMockCandidateView();
  }

  protected Class<? extends TestableImeService> getServiceClass() {
    return TestableImeService.class;
  }

  @Before
  public void setUpForImeServiceBase() throws Exception {
    final Application application = getApplicationContext();

    mInputMethodManagerShadow =
        Shadow.extract(application.getSystemService(Service.INPUT_METHOD_SERVICE));
    mMockBinder = Mockito.mock(IBinder.class);

    mImeServiceController = Robolectric.buildService(getServiceClass());
    mImeServiceUnderTest = mImeServiceController.create().get();

    mAbstractInputMethod = mImeServiceUnderTest.onCreateInputMethodInterface();
    mAbstractInputMethod.createSession(
        session -> {
          mImeServiceUnderTest.setInputSession(session);
          session.toggleSoftInput(InputMethod.SHOW_EXPLICIT, 0);
        });
    final EditorInfo editorInfo = createEditorInfoTextWithSuggestionsForSetUp();

    mAbstractInputMethod.attachToken(mMockBinder);

    mAbstractInputMethod.showSoftInput(InputMethod.SHOW_EXPLICIT, null);
    mAbstractInputMethod.startInput(mImeServiceUnderTest.getTestInputConnection(), editorInfo);
    TestRxSchedulers.drainAllTasks();
    mImeServiceUnderTest.showWindow(true);

    Assert.assertNotNull(getMockCandidateView());

    KeyboardDefinition currentAlphabetKeyboard = mImeServiceUnderTest.getCurrentKeyboardForTests();
    Assert.assertNotNull(currentAlphabetKeyboard);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      mImeServiceUnderTest.simulateCurrentSubtypeChanged(
          new InputMethodSubtype.InputMethodSubtypeBuilder()
              .setSubtypeExtraValue(currentAlphabetKeyboard.getKeyboardId())
              .setSubtypeLocale(currentAlphabetKeyboard.getLocale().toString())
              .build());
    }

    Mockito.verify(mImeServiceUnderTest.getMockCandidateView())
        .setHost(Mockito.same(mImeServiceUnderTest));

    verifySuggestions(true);
  }

  @After
  public void tearDownForImeServiceBase() throws Exception {}

  protected final InputMethodManagerShadow getShadowInputMethodManager() {
    return mInputMethodManagerShadow;
  }

  protected EditorInfo createEditorInfoTextWithSuggestionsForSetUp() {
    return TestableImeService.createEditorInfoTextWithSuggestions();
  }

  protected final void verifyNoSuggestionsInteractions() {
    Mockito.verify(getMockCandidateView(), Mockito.never())
        .setSuggestions(Mockito.anyList(), Mockito.anyInt());
  }

  protected final void verifySuggestions(
      boolean resetCandidateView, CharSequence... expectedSuggestions) {
    TestRxSchedulers.drainAllTasks();

    List actualSuggestions = verifyAndCaptureSuggestion(resetCandidateView);
    Assert.assertEquals(
        "Actual suggestions are " + Arrays.toString(actualSuggestions.toArray()),
        expectedSuggestions.length,
        actualSuggestions.size());
    for (int expectedSuggestionIndex = 0;
        expectedSuggestionIndex < expectedSuggestions.length;
        expectedSuggestionIndex++) {
      String expectedSuggestion = expectedSuggestions[expectedSuggestionIndex].toString();
      Assert.assertEquals(
          expectedSuggestion, actualSuggestions.get(expectedSuggestionIndex).toString());
    }
  }

  protected List verifyAndCaptureSuggestion(boolean resetCandidateView) {
    ArgumentCaptor<List> suggestionsCaptor = ArgumentCaptor.forClass(List.class);
    Mockito.verify(getMockCandidateView(), Mockito.atLeastOnce())
        .setSuggestions(suggestionsCaptor.capture(), Mockito.anyInt());
    List<List> allValues = suggestionsCaptor.getAllValues();

    if (resetCandidateView) mImeServiceUnderTest.resetMockCandidateView();

    return allValues.get(allValues.size() - 1);
  }

  protected void simulateOnStartInputFlow() {
    simulateOnStartInputFlow(false, createEditorInfoTextWithSuggestionsForSetUp());
  }

  protected void simulateOnStartInputFlow(boolean restarting, EditorInfo editorInfo) {
    if (restarting) {
      mImeServiceUnderTest
          .getCreatedInputMethodInterface()
          .restartInput(getCurrentTestInputConnection(), editorInfo);
    } else {
      mImeServiceUnderTest
          .getCreatedInputMethodInterface()
          .startInput(getCurrentTestInputConnection(), editorInfo);
    }
    mImeServiceUnderTest.showWindow(true);
    TestRxSchedulers.foregroundAdvanceBy(0);
  }

  protected void simulateFinishInputFlow() {
    mAbstractInputMethod.hideSoftInput(InputMethodManager.RESULT_HIDDEN, null);
    mImeServiceUnderTest.getCreatedInputMethodSessionInterface().finishInput();
    TestRxSchedulers.foregroundAdvanceBy(0);
  }

  protected CharSequence getResText(int stringId) {
    return getApplicationContext().getText(stringId);
  }
}
