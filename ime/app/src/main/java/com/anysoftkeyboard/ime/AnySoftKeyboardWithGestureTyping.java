package com.anysoftkeyboard.ime;

import android.graphics.drawable.Drawable;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.anysoftkeyboard.dictionaries.DictionaryBackgroundLoader;
import com.anysoftkeyboard.dictionaries.WordComposer;
import com.anysoftkeyboard.gesturetyping.GestureTypingDetector;
import com.anysoftkeyboard.ime.gesturetyping.ClearGestureStripActionProvider;
import com.anysoftkeyboard.ime.gesturetyping.GestureTypingController;
import com.anysoftkeyboard.keyboards.AnyKeyboard;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.views.InputViewBinder;
import io.reactivex.disposables.Disposable;
import java.util.List;
import java.util.Map;

public abstract class AnySoftKeyboardWithGestureTyping extends AnySoftKeyboardWithQuickText {

  public static final long MINIMUM_GESTURE_TIME_MS =
      GestureTypingController.MINIMUM_GESTURE_TIME_MS;

  private final GestureTypingController gestureTypingController = new GestureTypingController();
  private GestureTypingController.Host gestureTypingHost;

  @VisibleForTesting
  protected final Map<String, GestureTypingDetector> mGestureTypingDetectors =
      gestureTypingController.getGestureTypingDetectorsForTesting();

  @VisibleForTesting protected ClearGestureStripActionProvider mClearLastGestureAction;

  @VisibleForTesting
  protected static String getKeyForDetector(@NonNull AnyKeyboard keyboard) {
    return GestureTypingController.getKeyForDetector(keyboard);
  }

  @NonNull
  private GestureTypingController.Host getGestureTypingHost() {
    if (gestureTypingHost != null) return gestureTypingHost;
    gestureTypingHost =
        new GestureTypingController.Host() {
          @Override
          public @NonNull android.content.Context context() {
            return AnySoftKeyboardWithGestureTyping.this;
          }

          @Override
          public @NonNull com.anysoftkeyboard.prefs.RxSharedPrefs prefs() {
            return AnySoftKeyboardWithGestureTyping.this.prefs();
          }

          @Override
          public void addDisposable(@NonNull Disposable disposable) {
            AnySoftKeyboardWithGestureTyping.this.addDisposable(disposable);
          }

          @Override
          public void setupInputViewWatermark() {
            AnySoftKeyboardWithGestureTyping.this.setupInputViewWatermark();
          }

          @Override
          public @Nullable AnyKeyboard currentAlphabetKeyboard() {
            return AnySoftKeyboardWithGestureTyping.this.getCurrentAlphabetKeyboard();
          }

          @Override
          public @Nullable AnyKeyboard currentKeyboard() {
            return AnySoftKeyboardWithGestureTyping.this.getCurrentKeyboard();
          }

          @Override
          public @Nullable InputViewBinder inputView() {
            return AnySoftKeyboardWithGestureTyping.this.getInputView();
          }

          @Override
          public @Nullable com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView
              inputViewContainer() {
            return AnySoftKeyboardWithGestureTyping.this.getInputViewContainer();
          }

          @Override
          public boolean prefsAutoSpace() {
            return mPrefsAutoSpace;
          }

          @Override
          public @NonNull InputConnectionRouter inputConnectionRouter() {
            return AnySoftKeyboardWithGestureTyping.this.getInputConnectionRouter();
          }

          @Override
          public @NonNull WordComposer currentComposedWord() {
            return AnySoftKeyboardWithGestureTyping.this.getCurrentComposedWord();
          }

          @Override
          public void abortCorrectionAndResetPredictionState(boolean disabledUntilNextInputStart) {
            AnySoftKeyboardWithGestureTyping.this.abortCorrectionAndResetPredictionState(
                disabledUntilNextInputStart);
          }

          @Override
          public void setSuggestions(
              @NonNull List<? extends CharSequence> suggestions, int highlightedIndex) {
            AnySoftKeyboardWithGestureTyping.this.setSuggestions(suggestions, highlightedIndex);
          }

          @Override
          public void markExpectingSelectionUpdate() {
            AnySoftKeyboardWithGestureTyping.this.markExpectingSelectionUpdate();
          }

          @Override
          public void pickSuggestionManually(
              int index, CharSequence suggestion, boolean withAutoSpaceEnabled) {
            AnySoftKeyboardWithGestureTyping.this.pickSuggestionManually(
                index, suggestion, withAutoSpaceEnabled);
          }

          @Override
          public void handleBackWord() {
            AnySoftKeyboardWithGestureTyping.this.handleBackWord();
          }

          @Override
          public @NonNull com.anysoftkeyboard.utils.ModifierKeyState shiftKeyState() {
            return mShiftKeyState;
          }

          @Override
          public void onClearGestureActionProviderReady(
              @NonNull ClearGestureStripActionProvider provider) {
            mClearLastGestureAction = provider;
          }
        };
    return gestureTypingHost;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    gestureTypingController.onCreate(getGestureTypingHost());
  }

  @Override
  public void onStartInputView(EditorInfo info, boolean restarting) {
    super.onStartInputView(info, restarting);
    gestureTypingController.onStartInputView(getGestureTypingHost());
  }

  @Override
  public void onFinishInputView(boolean finishInput) {
    gestureTypingController.onFinishInputView(getGestureTypingHost());
    super.onFinishInputView(finishInput);
  }

  @Override
  public void onFinishInput() {
    gestureTypingController.onFinishInput(getGestureTypingHost());
    super.onFinishInput();
  }

  @Override
  public void onAddOnsCriticalChange() {
    super.onAddOnsCriticalChange();
    gestureTypingController.onAddOnsCriticalChange(getGestureTypingHost());
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    gestureTypingController.onLowMemory(getGestureTypingHost());
  }

  @NonNull
  @Override
  protected DictionaryBackgroundLoader.Listener getDictionaryLoadedListener(
      @NonNull AnyKeyboard currentAlphabetKeyboard) {
    final DictionaryBackgroundLoader.Listener gestureListener =
        gestureTypingController.maybeGetDictionaryLoadedListener(currentAlphabetKeyboard);
    return gestureListener != null
        ? gestureListener
        : super.getDictionaryLoadedListener(currentAlphabetKeyboard);
  }

  @Override
  protected boolean shouldLoadDictionariesForGestureTyping() {
    return gestureTypingController.shouldLoadDictionariesForGestureTyping();
  }

  /**
   * When alphabet keyboard loaded, we start loading our gesture-typing word corners data. It is
   * earlier than the first time we click on the keyboard.
   */
  @Override
  public void onAlphabetKeyboardSet(@NonNull AnyKeyboard keyboard) {
    super.onAlphabetKeyboardSet(keyboard);
    gestureTypingController.onAlphabetKeyboardSet(getGestureTypingHost(), keyboard);
  }

  @Override
  public void onSymbolsKeyboardSet(@NonNull AnyKeyboard keyboard) {
    super.onSymbolsKeyboardSet(keyboard);
    gestureTypingController.onSymbolsKeyboardSet(getGestureTypingHost());
  }

  @Override
  public boolean onGestureTypingInputStart(int x, int y, AnyKeyboard.AnyKey key, long eventTime) {
    return gestureTypingController.onGestureTypingInputStart(
        getGestureTypingHost(), x, y, key, eventTime);
  }

  @Override
  public void onGestureTypingInput(int x, int y, long eventTime) {
    gestureTypingController.onGestureTypingInput(x, y, eventTime);
  }

  @Override
  public boolean onGestureTypingInputDone() {
    return gestureTypingController.onGestureTypingInputDone(getGestureTypingHost());
  }

  @NonNull
  @Override
  protected List<Drawable> generateWatermark() {
    final List<Drawable> watermark = super.generateWatermark();
    gestureTypingController.decorateWatermark(getGestureTypingHost(), watermark);
    return watermark;
  }

  @Override
  public void onKey(
      int primaryCode, Keyboard.Key key, int multiTapIndex, int[] nearByKeyCodes, boolean fromUI) {
    gestureTypingController.onKey(getGestureTypingHost(), primaryCode);
    super.onKey(primaryCode, key, multiTapIndex, nearByKeyCodes, fromUI);
  }

  @Override
  public void pickSuggestionManually(
      int index, CharSequence suggestion, boolean withAutoSpaceEnabled) {
    gestureTypingController.onPickSuggestionManually();
    super.pickSuggestionManually(index, suggestion, withAutoSpaceEnabled);
  }
}
