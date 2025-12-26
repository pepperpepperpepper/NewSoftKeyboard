package wtf.uhoh.newsoftkeyboard.app.ime;

import static org.mockito.ArgumentMatchers.any;

import android.os.SystemClock;
import android.view.View;
import android.widget.Toast;
import com.anysoftkeyboard.api.KeyCodes;
import io.reactivex.disposables.CompositeDisposable;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowToast;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardKey;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewContainerView;
import wtf.uhoh.newsoftkeyboard.app.testing.AddOnTestUtils;
import wtf.uhoh.newsoftkeyboard.app.testing.ViewTestUtils;
import wtf.uhoh.newsoftkeyboard.dictionaries.Dictionary;
import wtf.uhoh.newsoftkeyboard.dictionaries.DictionaryBackgroundLoader;
import wtf.uhoh.newsoftkeyboard.dictionaries.GetWordsCallback;
import wtf.uhoh.newsoftkeyboard.gesturetyping.GestureTypingDetector;
import wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers;
import wtf.uhoh.newsoftkeyboard.testing.NskRobolectricTestRunner;
import wtf.uhoh.newsoftkeyboard.testing.SharedPrefsHelper;

@RunWith(NskRobolectricTestRunner.class)
public class ImeServiceGestureTypingTest extends ImeServiceBaseTest {

  private CompositeDisposable mDisposable;

  @Before
  @Override
  public void setUpForImeServiceBase() throws Exception {
    mDisposable = new CompositeDisposable();
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_gesture_typing, true);
    super.setUpForImeServiceBase();
    wtf.uhoh.newsoftkeyboard.rx.TestRxSchedulers.backgroundFlushAllJobs();
    TestRxSchedulers.foregroundFlushAllJobs();
  }

  @After
  public void tearDownDisposables() {
    mDisposable.dispose();
  }

  private Supplier<GestureTypingDetector.LoadingState> createLatestStateProvider(
      GestureTypingDetector detector) {
    final AtomicReference<GestureTypingDetector.LoadingState> currentState =
        new AtomicReference<>();
    mDisposable.add(
        detector
            .state()
            .subscribe(
                currentState::set,
                e -> {
                  throw new RuntimeException(e);
                }));
    return currentState::get;
  }

  @Test
  public void testDoesNotOutputIfGestureTypingIsDisabled() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_gesture_typing, false);
    Assert.assertFalse(simulateGestureProcess("hello"));
    Assert.assertEquals("", mImeServiceUnderTest.getCurrentInputConnectionText());
    verifyNoSuggestionsInteractions();
  }

  @Test
  public void testDoesNotCallGetWordsWhenGestureIsOff() {
    simulateFinishInputFlow();
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_gesture_typing, false);
    simulateOnStartInputFlow();
    ArgumentCaptor<DictionaryBackgroundLoader.Listener> captor =
        ArgumentCaptor.forClass(DictionaryBackgroundLoader.Listener.class);
    Mockito.verify(mImeServiceUnderTest.getSuggest(), Mockito.times(2))
        .setupSuggestionsForKeyboard(Mockito.anyList(), captor.capture());
    final DictionaryBackgroundLoader.Listener listener = captor.getAllValues().get(1);
    Dictionary dictionary = Mockito.mock(Dictionary.class);
    Mockito.doAnswer(
            invocation -> {
              ((GetWordsCallback) invocation.getArgument(0))
                  .onGetWordsFinished(new char[][] {"hello".toCharArray()}, new int[] {1});
              return null;
            })
        .when(dictionary)
        .getLoadedWords(any());
    listener.onDictionaryLoadingStarted(dictionary);
    listener.onDictionaryLoadingDone(dictionary);
    Mockito.verify(dictionary, Mockito.never()).getLoadedWords(any());
  }

  @Test
  public void testCallsGetWordsWhenGestureIsOn() {
    ArgumentCaptor<DictionaryBackgroundLoader.Listener> captor =
        ArgumentCaptor.forClass(DictionaryBackgroundLoader.Listener.class);
    Mockito.verify(mImeServiceUnderTest.getSuggest())
        .setupSuggestionsForKeyboard(Mockito.anyList(), captor.capture());
    final DictionaryBackgroundLoader.Listener listener = captor.getAllValues().get(0);
    Dictionary dictionary = Mockito.mock(Dictionary.class);
    Mockito.doAnswer(
            invocation -> {
              ((GetWordsCallback) invocation.getArgument(0))
                  .onGetWordsFinished(new char[][] {"hello".toCharArray()}, new int[] {1});
              return null;
            })
        .when(dictionary)
        .getLoadedWords(any());
    listener.onDictionaryLoadingStarted(dictionary);
    listener.onDictionaryLoadingDone(dictionary);
    Mockito.verify(dictionary).getLoadedWords(any());
  }

  @Test
  public void testNotCrashingWhenExceptionIsThrownInGetWordsAndGestureIsOn() {
    ArgumentCaptor<DictionaryBackgroundLoader.Listener> captor =
        ArgumentCaptor.forClass(DictionaryBackgroundLoader.Listener.class);
    Mockito.verify(mImeServiceUnderTest.getSuggest())
        .setupSuggestionsForKeyboard(Mockito.anyList(), captor.capture());
    final DictionaryBackgroundLoader.Listener listener = captor.getAllValues().get(0);
    Dictionary dictionary = Mockito.mock(Dictionary.class);
    Mockito.doThrow(new UnsupportedOperationException()).when(dictionary).getLoadedWords(any());
    listener.onDictionaryLoadingStarted(dictionary);
    listener.onDictionaryLoadingDone(dictionary);
    Mockito.verify(dictionary).getLoadedWords(any());
  }

  @Test
  public void testOutputPrimarySuggestionOnGestureDone() {
    Assert.assertTrue(simulateGestureProcess("hello"));
    Assert.assertEquals("hello", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testDoesNotFailWhenInputConnectionReturnsNullAsTextInInput() {
    // This test is simulating dead IC

    // This can happen if the IC dies while running
    Mockito.doReturn(null)
        .when(mImeServiceUnderTest.getCurrentTestInputConnection())
        .getTextBeforeCursor(Mockito.anyInt(), Mockito.anyInt());

    // Returns false since the gesture was not handled
    Assert.assertFalse(simulateGestureProcess("hello"));
    Assert.assertEquals("", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testOutputCapitalisedOnShiftLocked() {
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT_LOCK);
    simulateGestureProcess("hello");
    Assert.assertEquals("HELLO", mImeServiceUnderTest.getCurrentInputConnectionText());
    simulateGestureProcess("hello");
    Assert.assertEquals("HELLO HELLO", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testOutputTitleCaseOnShifted() {
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    simulateGestureProcess("hello");
    Assert.assertEquals("Hello", mImeServiceUnderTest.getCurrentInputConnectionText());
    simulateGestureProcess("hello");
    Assert.assertEquals("Hello hello", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testCanOutputFromBothDictionaries() {
    mImeServiceUnderTest
        .mGestureTypingDetectors
        .get(ImeWithGestureTyping.getKeyForDetector(mImeServiceUnderTest.getCurrentKeyboard()))
        .setWords(
            Arrays.asList(
                new char[][] {
                  "keyboard".toCharArray(),
                  "welcome".toCharArray(),
                  "is".toCharArray(),
                  "you".toCharArray(),
                },
                new char[][] {
                  "luck".toCharArray(),
                  "bye".toCharArray(),
                  "one".toCharArray(),
                  "two".toCharArray(),
                  "three".toCharArray(),
                  "tree".toCharArray()
                }),
            Arrays.asList(new int[] {50, 100, 250, 200}, new int[] {80, 190, 220, 140, 130, 27}));

    TestRxSchedulers.drainAllTasks();

    simulateGestureProcess("keyboard");
    Assert.assertEquals("keyboard", mImeServiceUnderTest.getCurrentInputConnectionText());

    simulateGestureProcess("luck");
    Assert.assertEquals("keyboard luck", mImeServiceUnderTest.getCurrentInputConnectionText());

    simulateGestureProcess("bye");
    Assert.assertEquals("keyboard luck bye", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testConfirmsLastGesturesWhenPrintableKeyIsPressed() {
    simulateGestureProcess("hello");
    mImeServiceUnderTest.simulateKeyPress('a');
    Assert.assertEquals("hello a", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testOutputDoubleSpacesToDotAfterGestureIfEnabled() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_double_space_to_period, true);
    simulateGestureProcess("hello");
    Assert.assertEquals("hello", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(' ');
    Assert.assertEquals("hello ", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(' ');
    Assert.assertEquals("hello. ", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testOutputDoubleSpacesToDotAfterGestureIfDisabled() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_double_space_to_period, false);
    simulateGestureProcess("hello");
    Assert.assertEquals("hello", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(' ');
    Assert.assertEquals("hello ", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(' ');
    Assert.assertEquals("hello  ", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testDoesNotConfirmLastGesturesWhenNonePrintableKeyIsPressed() {
    simulateGestureProcess("hello");
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SHIFT);
    Assert.assertEquals("hello", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testConfirmsLastGesturesOnNextGestureStarts() {
    simulateGestureProcess("hello");
    simulateGestureProcess("welcome");
    Assert.assertEquals("hello welcome", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testDeleteGesturedWordOneCharacterAtTime() {
    simulateGestureProcess("hello");
    simulateGestureProcess("welcome");
    Assert.assertEquals("hello welcome", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hello welcom", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hello welco", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hello welc", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hello wel", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hello we", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hello w", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hello ", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hello", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hell", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hel", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("he", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testRewriteGesturedWord() {
    simulateGestureProcess("hello");
    Assert.assertEquals("hello", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hell", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("hel", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress('p');
    Assert.assertEquals("help", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("help ", mImeServiceUnderTest.getCurrentInputConnectionText());
    simulateGestureProcess("welcome");
    Assert.assertEquals("help welcome", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE);
    Assert.assertEquals("help welcom", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateTextTyping("ing");
    Assert.assertEquals("help welcoming", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testSpaceAfterGestureJustConfirms() {
    simulateGestureProcess("hello");
    Assert.assertEquals("hello", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals("hello ", mImeServiceUnderTest.getCurrentInputConnectionText());
    simulateGestureProcess("you");
    Assert.assertEquals("hello you", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateTextTyping("all");
    Assert.assertEquals("hello you all", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testOnlySingleSpaceAfterPickingGestureSuggestion() {
    simulateGestureProcess("hello");
    Assert.assertEquals("hello", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.pickSuggestionManually(0, "hello", true);
    Assert.assertEquals("hello ", mImeServiceUnderTest.getCurrentInputConnectionText());
    simulateGestureProcess("welcome");
    Assert.assertEquals("hello welcome", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testDoesNotOutputGestureWhenPathIsTooQuick() {
    final String pathKeys = "you"; // to gesture you
    long time = SystemClock.uptimeMillis();
    Keyboard.Key startKey = mImeServiceUnderTest.findKeyWithPrimaryKeyCode(pathKeys.charAt(0));
    mImeServiceUnderTest.onPress(startKey.getPrimaryCode());
    TestRxSchedulers.drainAllTasks();
    mImeServiceUnderTest.onGestureTypingInputStart(
        Keyboard.Key.getCenterX(startKey),
        Keyboard.Key.getCenterY(startKey),
        (KeyboardKey) startKey,
        time);
    TestRxSchedulers.drainAllTasks();
    // travelling from P to O, but very quickly!
    final long lastTime = time + ImeWithGestureTyping.MINIMUM_GESTURE_TIME_MS - 1;

    final Keyboard.Key followingKey =
        mImeServiceUnderTest.findKeyWithPrimaryKeyCode(pathKeys.charAt(1));
    // simulating gesture from startKey to followingKey
    final float xStep = startKey.width / 3.0f;
    final float yStep = startKey.height / 3.0f;

    final float xDistance =
        Keyboard.Key.getCenterX(followingKey) - Keyboard.Key.getCenterX(startKey);
    final float yDistance =
        Keyboard.Key.getCenterY(followingKey) - Keyboard.Key.getCenterY(startKey);
    int callsToMake = (int) Math.ceil(((xDistance + yDistance) / 2f) / ((xStep + yStep) / 2f));

    final long timeStep = ImeWithGestureTyping.MINIMUM_GESTURE_TIME_MS / callsToMake;

    float currentX = Keyboard.Key.getCenterX(startKey);
    float currentY = Keyboard.Key.getCenterY(startKey);

    TestRxSchedulers.foregroundAdvanceBy(timeStep);
    time = SystemClock.uptimeMillis();
    ;
    mImeServiceUnderTest.onGestureTypingInput(
        Keyboard.Key.getCenterX(startKey), Keyboard.Key.getCenterY(startKey), time);

    while (callsToMake > 0) {
      callsToMake--;
      currentX += xStep;
      currentY += yStep;
      TestRxSchedulers.foregroundAdvanceBy(timeStep);
      time = SystemClock.uptimeMillis();
      ;
      mImeServiceUnderTest.onGestureTypingInput((int) currentX, (int) currentY, time);
    }

    mImeServiceUnderTest.onGestureTypingInput(
        Keyboard.Key.getCenterX(followingKey), Keyboard.Key.getCenterY(followingKey), lastTime);

    Assert.assertFalse(mImeServiceUnderTest.onGestureTypingInputDone());
    TestRxSchedulers.drainAllTasks();

    // nothing should be outputted
    Assert.assertEquals("", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testDoesNotOutputGestureWhenPathIsTooShort() {
    final String pathKeys = "po"; // to gesture pop, but will not
    long time = SystemClock.uptimeMillis();
    Keyboard.Key startKey = mImeServiceUnderTest.findKeyWithPrimaryKeyCode(pathKeys.charAt(0));
    mImeServiceUnderTest.onPress(startKey.getPrimaryCode());
    TestRxSchedulers.drainAllTasks();
    mImeServiceUnderTest.onGestureTypingInputStart(
        Keyboard.Key.getCenterX(startKey),
        Keyboard.Key.getCenterY(startKey),
        (KeyboardKey) startKey,
        time);
    TestRxSchedulers.drainAllTasks();
    // travelling from P to O, but slow enough to trigger a gesture!
    final long lastTime = time + ImeWithGestureTyping.MINIMUM_GESTURE_TIME_MS + 1;

    final Keyboard.Key followingKey =
        mImeServiceUnderTest.findKeyWithPrimaryKeyCode(pathKeys.charAt(1));
    // just to make sure we are using the right keys
    Assert.assertTrue(Keyboard.Key.getCenterX(startKey) > Keyboard.Key.getCenterX(followingKey));
    Assert.assertEquals(Keyboard.Key.getCenterY(startKey), Keyboard.Key.getCenterY(followingKey));
    // simulating gesture from startKey to followingKey
    // they are on the same row (p -> o), from back on the X axis.
    // we'll do 3 steps, each a quarter of a key. Overall, less than a key width.
    final float xStep = -startKey.width / 4.0f;
    int callsToMake = 3;

    final long timeStep = ImeWithGestureTyping.MINIMUM_GESTURE_TIME_MS / callsToMake;

    float currentX = Keyboard.Key.getCenterX(startKey);
    final float currentY = Keyboard.Key.getCenterY(startKey);

    mImeServiceUnderTest.onGestureTypingInput(
        Keyboard.Key.getCenterX(startKey), Keyboard.Key.getCenterY(startKey), time);

    while (callsToMake > 0) {
      callsToMake--;
      currentX += xStep;
      TestRxSchedulers.foregroundAdvanceBy(timeStep);
      time = SystemClock.uptimeMillis();
      mImeServiceUnderTest.onGestureTypingInput((int) currentX, (int) currentY, time);
    }

    // ensuring lastTime is used, so we know that the last input
    // to the gesture-detector was pass the minimum time
    mImeServiceUnderTest.onGestureTypingInput((int) currentX, (int) currentY, lastTime);

    Assert.assertFalse(mImeServiceUnderTest.onGestureTypingInputDone());
    TestRxSchedulers.drainAllTasks();

    // nothing should be outputted
    Assert.assertEquals("", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testOutputsGestureIfPathIsJustLongEnough() {
    Assert.assertTrue(simulateGestureProcess("po"));
    Assert.assertEquals("poo", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testDeleteGesturedWordOnWholeWord() {
    simulateGestureProcess("hello");
    simulateGestureProcess("welcome");
    Assert.assertEquals("hello welcome", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE_WORD);
    Assert.assertEquals("hello ", mImeServiceUnderTest.getCurrentInputConnectionText());
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.DELETE_WORD);
    Assert.assertEquals("", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testShowClearGestureButton() {
    simulateGestureProcess("hello");
    Assert.assertEquals(View.VISIBLE, mImeServiceUnderTest.mClearLastGestureAction.getVisibility());
  }

  @Test
  public void testHideClearGestureButtonOnConfirmed() {
    simulateGestureProcess("hello");
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.SPACE);
    Assert.assertEquals(View.GONE, mImeServiceUnderTest.mClearLastGestureAction.getVisibility());
  }

  @Test
  public void testClearGestureButtonClearsGesture() {
    simulateGestureProcess("hello");
    final KeyboardViewContainerView.StripActionProvider provider =
        mImeServiceUnderTest.mClearLastGestureAction;
    View rootActionView =
        provider
            .inflateActionView(mImeServiceUnderTest.getInputViewContainer())
            .findViewById(R.id.clear_gesture_action_icon);
    final View.OnClickListener onClickListener =
        Shadows.shadowOf(rootActionView).getOnClickListener();

    onClickListener.onClick(rootActionView);

    Assert.assertEquals("", mImeServiceUnderTest.getCurrentInputConnectionText());
  }

  @Test
  public void testHideClearGestureButtonOnClear() {
    simulateGestureProcess("hello");
    final KeyboardViewContainerView.StripActionProvider provider =
        mImeServiceUnderTest.mClearLastGestureAction;
    View rootActionView =
        provider
            .inflateActionView(mImeServiceUnderTest.getInputViewContainer())
            .findViewById(R.id.clear_gesture_action_icon);
    final View.OnClickListener onClickListener =
        Shadows.shadowOf(rootActionView).getOnClickListener();

    onClickListener.onClick(rootActionView);

    Assert.assertEquals(View.GONE, mImeServiceUnderTest.mClearLastGestureAction.getVisibility());
  }

  @Test
  public void testShowsTipOnSwipe() {
    simulateGestureProcess("hello");
    var view =
        mImeServiceUnderTest.getInputViewContainer().findViewById(R.id.clear_gesture_action_icon);
    final View.OnClickListener onClickListener = Shadows.shadowOf(view).getOnClickListener();

    Assert.assertEquals(0, ShadowToast.shownToastCount());
    onClickListener.onClick(view);
    Assert.assertEquals(1, ShadowToast.shownToastCount());
    Assert.assertEquals(Toast.LENGTH_LONG, ShadowToast.getLatestToast().getDuration());
    Assert.assertTrue(ShadowToast.getTextOfLatestToast().startsWith("Tip:"));

    simulateGestureProcess("hello");
    onClickListener.onClick(view);
    Assert.assertEquals(2, ShadowToast.shownToastCount());
    Assert.assertEquals(Toast.LENGTH_SHORT, ShadowToast.getLatestToast().getDuration());
    Assert.assertTrue(ShadowToast.getTextOfLatestToast().startsWith("Tip:"));

    simulateGestureProcess("hello");
    onClickListener.onClick(view);
    Assert.assertEquals(3, ShadowToast.shownToastCount());
    Assert.assertEquals(Toast.LENGTH_SHORT, ShadowToast.getLatestToast().getDuration());
    Assert.assertTrue(ShadowToast.getTextOfLatestToast().startsWith("Tip:"));

    simulateGestureProcess("hello");
    onClickListener.onClick(view);
    // not showing the tip anymore
    Assert.assertEquals(3, ShadowToast.shownToastCount());
  }

  @Test
  public void testClearAllDetectorsWhenCriticalAddOnChange() {
    Assert.assertTrue(mImeServiceUnderTest.mGestureTypingDetectors.size() > 0);

    AddOnTestUtils.ensureKeyboardAtIndexEnabled(1, true);

    Assert.assertEquals(0, mImeServiceUnderTest.mGestureTypingDetectors.size());

    simulateOnStartInputFlow();

    Assert.assertEquals(1, mImeServiceUnderTest.mGestureTypingDetectors.size());
  }

  @Test
  public void testClearDetectorsOnLowMemory() {
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(1, true);
    simulateOnStartInputFlow();
    final GestureTypingDetector detector1 = getCurrentGestureTypingDetectorFromMap();
    Supplier<GestureTypingDetector.LoadingState> detector1State =
        createLatestStateProvider(detector1);
    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    Assert.assertEquals(2, mImeServiceUnderTest.mGestureTypingDetectors.size());
    final GestureTypingDetector detector2 = getCurrentGestureTypingDetectorFromMap();
    Supplier<GestureTypingDetector.LoadingState> detector2State =
        createLatestStateProvider(detector2);

    // this keeps the currently used detector2, but kills the second
    mImeServiceUnderTest.onLowMemory();
    TestRxSchedulers.drainAllTasks();
    Assert.assertEquals(1, mImeServiceUnderTest.mGestureTypingDetectors.size());
    Assert.assertSame(detector2, getCurrentGestureTypingDetectorFromMap());

    Assert.assertEquals(GestureTypingDetector.LoadingState.NOT_LOADED, detector1State.get());
    Assert.assertEquals(GestureTypingDetector.LoadingState.LOADED, detector2State.get());
  }

  @Test
  public void testDoesNotCrashIfOnLowMemoryCalledBeforeLoaded() {
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(1, true);
    simulateOnStartInputFlow();
    final GestureTypingDetector detector1 = getCurrentGestureTypingDetectorFromMap();
    Assert.assertNotNull(detector1);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    Assert.assertEquals(2, mImeServiceUnderTest.mGestureTypingDetectors.size());
    final GestureTypingDetector detector2 = getCurrentGestureTypingDetectorFromMap();
    Supplier<GestureTypingDetector.LoadingState> detector2State =
        createLatestStateProvider(detector2);
    Assert.assertEquals(GestureTypingDetector.LoadingState.LOADING, detector2State.get());

    // this keeps the currently used detector2, but kills the second
    mImeServiceUnderTest.onLowMemory();
    Assert.assertEquals(1, mImeServiceUnderTest.mGestureTypingDetectors.size());
    Assert.assertSame(detector2, getCurrentGestureTypingDetectorFromMap());

    TestRxSchedulers.drainAllTasks();
    Assert.assertEquals(GestureTypingDetector.LoadingState.LOADED, detector2State.get());
  }

  @Test
  public void testCreatesDetectorOnNewKeyboard() {
    AddOnTestUtils.ensureKeyboardAtIndexEnabled(1, true);

    Assert.assertEquals(0, mImeServiceUnderTest.mGestureTypingDetectors.size());

    simulateOnStartInputFlow();

    Assert.assertEquals(1, mImeServiceUnderTest.mGestureTypingDetectors.size());
    final GestureTypingDetector detector1 = getCurrentGestureTypingDetectorFromMap();
    Assert.assertNotNull(detector1);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);

    Assert.assertEquals(2, mImeServiceUnderTest.mGestureTypingDetectors.size());
    final GestureTypingDetector detector2 = getCurrentGestureTypingDetectorFromMap();
    Assert.assertNotNull(detector2);
    Assert.assertNotSame(detector1, detector2);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);

    Assert.assertEquals(2, mImeServiceUnderTest.mGestureTypingDetectors.size());
    // cached now
    final GestureTypingDetector detector1Again = getCurrentGestureTypingDetectorFromMap();
    Assert.assertNotNull(detector1Again);
    Assert.assertSame(detector1, detector1Again);
  }

  private GestureTypingDetector getCurrentGestureTypingDetectorFromMap() {
    return mImeServiceUnderTest.mGestureTypingDetectors.get(
        ImeWithGestureTyping.getKeyForDetector(mImeServiceUnderTest.getCurrentKeyboard()));
  }

  @Test
  public void testBadgeGestureLifeCycle() {
    SharedPrefsHelper.setPrefsValue(R.string.settings_key_gesture_typing, false);
    TestRxSchedulers.drainAllTasks();

    ViewTestUtils.assertCurrentWatermarkDoesNotHaveDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_gesture);
    ViewTestUtils.assertCurrentWatermarkDoesNotHaveDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_gesture_not_loaded);

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_gesture_typing, true);

    ViewTestUtils.assertCurrentWatermarkDoesNotHaveDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_gesture);
    ViewTestUtils.assertCurrentWatermarkHasDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_gesture_not_loaded);

    simulateOnStartInputFlow();

    ViewTestUtils.assertCurrentWatermarkDoesNotHaveDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_gesture);
    ViewTestUtils.assertCurrentWatermarkHasDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_gesture_not_loaded);

    TestRxSchedulers.drainAllTasks();

    ViewTestUtils.assertCurrentWatermarkHasDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_gesture);
    ViewTestUtils.assertCurrentWatermarkDoesNotHaveDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_gesture_not_loaded);
  }

  @Test
  public void testBadgeClearedWhenPrefDisabled() {
    ViewTestUtils.assertCurrentWatermarkHasDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_gesture);
    ViewTestUtils.assertCurrentWatermarkDoesNotHaveDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_gesture_not_loaded);

    SharedPrefsHelper.setPrefsValue(R.string.settings_key_gesture_typing, false);
    TestRxSchedulers.drainAllTasks();

    ViewTestUtils.assertCurrentWatermarkDoesNotHaveDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_gesture);
    ViewTestUtils.assertCurrentWatermarkDoesNotHaveDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_gesture_not_loaded);

    Assert.assertEquals(0, mImeServiceUnderTest.mGestureTypingDetectors.size());
  }

  @Test
  public void testBadgeClearedWhenSwitchingToSymbols() {
    ViewTestUtils.assertCurrentWatermarkHasDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_gesture);
    ViewTestUtils.assertCurrentWatermarkDoesNotHaveDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_gesture_not_loaded);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_SYMBOLS);

    ViewTestUtils.assertCurrentWatermarkDoesNotHaveDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_gesture);
    ViewTestUtils.assertCurrentWatermarkDoesNotHaveDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_gesture_not_loaded);

    mImeServiceUnderTest.simulateKeyPress(KeyCodes.MODE_ALPHABET);
    ViewTestUtils.assertCurrentWatermarkHasDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_gesture);
    ViewTestUtils.assertCurrentWatermarkDoesNotHaveDrawable(
        mImeServiceUnderTest.getInputView(), R.drawable.ic_watermark_gesture_not_loaded);
  }

  private boolean simulateGestureProcess(String pathKeys) {
    long time = SystemClock.uptimeMillis();
    Keyboard.Key startKey = mImeServiceUnderTest.findKeyWithPrimaryKeyCode(pathKeys.charAt(0));
    mImeServiceUnderTest.onPress(startKey.getPrimaryCode());
    TestRxSchedulers.drainAllTasks();
    mImeServiceUnderTest.onGestureTypingInputStart(
        Keyboard.Key.getCenterX(startKey),
        Keyboard.Key.getCenterY(startKey),
        (KeyboardKey) startKey,
        time);
    TestRxSchedulers.drainAllTasks();
    for (int keyIndex = 1; keyIndex < pathKeys.length(); keyIndex++) {
      final Keyboard.Key followingKey =
          mImeServiceUnderTest.findKeyWithPrimaryKeyCode(pathKeys.charAt(keyIndex));
      // simulating gesture from startKey to followingKey
      final float xStep = startKey.width / 3.0f;
      final float yStep = startKey.height / 3.0f;

      final float xDistance =
          Keyboard.Key.getCenterX(followingKey) - Keyboard.Key.getCenterX(startKey);
      final float yDistance =
          Keyboard.Key.getCenterY(followingKey) - Keyboard.Key.getCenterY(startKey);
      int callsToMake =
          (int) Math.ceil(Math.abs((xDistance + yDistance) / 2f) / ((xStep + yStep) / 2f));

      final long timeStep = 16;

      float currentX = Keyboard.Key.getCenterX(startKey);
      float currentY = Keyboard.Key.getCenterY(startKey);

      TestRxSchedulers.foregroundAdvanceBy(timeStep);
      time = SystemClock.uptimeMillis();
      mImeServiceUnderTest.onGestureTypingInput(
          Keyboard.Key.getCenterX(startKey), Keyboard.Key.getCenterY(startKey), time);

      while (callsToMake > 0) {
        callsToMake--;
        currentX += xStep;
        currentY += yStep;
        TestRxSchedulers.foregroundAdvanceBy(timeStep);
        time = SystemClock.uptimeMillis();
        mImeServiceUnderTest.onGestureTypingInput((int) currentX, (int) currentY, time);
      }

      TestRxSchedulers.foregroundAdvanceBy(timeStep);
      time = SystemClock.uptimeMillis();
      ;
      mImeServiceUnderTest.onGestureTypingInput(
          Keyboard.Key.getCenterX(followingKey), Keyboard.Key.getCenterY(followingKey), time);

      startKey = followingKey;
    }
    var handled = mImeServiceUnderTest.onGestureTypingInputDone();
    TestRxSchedulers.drainAllTasks();
    return handled;
  }
}
