package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.content.Context;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.addons.AddOn;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;

final class KeyboardViewTouchController {

  private static final long TWO_FINGERS_LINGER_TIME = 30;

  @NonNull private final KeyboardViewBase view;
  @NonNull private final TouchDispatcher touchDispatcher;
  @NonNull private final PointerTrackerRegistry pointerTrackerRegistry;
  @NonNull private final PointerActionDispatcher pointerActionDispatcher;
  @NonNull private final PointerTrackerAccessor pointerTrackerAccessor;
  @NonNull private final KeyPressTimingHandler keyPressTimingHandler;
  @NonNull private final KeyPreviewManagerFacade keyPreviewManager;
  @NonNull private final LongPressHelper longPressHelper;
  @NonNull private final KeyPreviewInteractor keyPreviewInteractor;
  @NonNull private final KeyPreviewControllerBinder keyPreviewControllerBinder;

  @NonNull
  private final KeyboardActionListenerHolder keyboardActionListenerHolder =
      new KeyboardActionListenerHolder();

  KeyboardViewTouchController(
      @NonNull KeyboardViewBase view,
      @NonNull TouchDispatcher touchDispatcher,
      @NonNull PointerTrackerRegistry pointerTrackerRegistry,
      @NonNull PointerActionDispatcher pointerActionDispatcher,
      @NonNull PointerTrackerAccessor pointerTrackerAccessor,
      @NonNull KeyPressTimingHandler keyPressTimingHandler,
      @NonNull KeyPreviewManagerFacade keyPreviewManager,
      @NonNull LongPressHelper longPressHelper,
      @NonNull KeyPreviewInteractor keyPreviewInteractor,
      @NonNull KeyPreviewControllerBinder keyPreviewControllerBinder) {
    this.view = view;
    this.touchDispatcher = touchDispatcher;
    this.pointerTrackerRegistry = pointerTrackerRegistry;
    this.pointerActionDispatcher = pointerActionDispatcher;
    this.pointerTrackerAccessor = pointerTrackerAccessor;
    this.keyPressTimingHandler = keyPressTimingHandler;
    this.keyPreviewManager = keyPreviewManager;
    this.longPressHelper = longPressHelper;
    this.keyPreviewInteractor = keyPreviewInteractor;
    this.keyPreviewControllerBinder = keyPreviewControllerBinder;
  }

  boolean areTouchesDisabled(@NonNull MotionEvent motionEvent) {
    return touchDispatcher.areTouchesDisabled(motionEvent);
  }

  boolean isAtTwoFingersState() {
    return touchDispatcher.isAtTwoFingersState(TWO_FINGERS_LINGER_TIME);
  }

  void disableTouchesTillFingersAreUp() {
    keyPressTimingHandler.cancelAllMessages();
    keyPreviewManager.dismissAll();
    touchDispatcher.disableTouchesTillFingersAreUp(pointerTrackerRegistry);
  }

  @Nullable
  OnKeyboardActionListener getOnKeyboardActionListener() {
    return keyboardActionListenerHolder.get();
  }

  void setOnKeyboardActionListener(@Nullable OnKeyboardActionListener listener) {
    keyboardActionListenerHolder.set(listener);
    pointerTrackerAccessor.setOnKeyboardActionListener(listener);
  }

  @NonNull
  PointerTracker getPointerTracker(@NonNull final MotionEvent motionEvent) {
    return pointerTrackerAccessor.getForMotionEvent(
        motionEvent, view.keyboardRenderState.keys, keyboardActionListenerHolder.get());
  }

  @NonNull
  PointerTracker getPointerTracker(final int id) {
    return pointerTrackerAccessor.get(
        id, view.keyboardRenderState.keys, keyboardActionListenerHolder.get());
  }

  boolean onLongPress(
      @NonNull AddOn keyboardAddOn,
      @NonNull Keyboard.Key key,
      boolean isSticky,
      @NonNull PointerTracker tracker) {
    final Context context = view.getContext();
    return longPressHelper.handleLongPress(
        context,
        keyboardActionListenerHolder.get(),
        keyboardAddOn,
        key,
        isSticky,
        tracker,
        () -> view.onCancelEvent(tracker));
  }

  void markTwoFingers(long timeMs) {
    touchDispatcher.markTwoFingers(timeMs);
  }

  boolean areTouchesTemporarilyDisabled() {
    return touchDispatcher.areTouchesTemporarilyDisabled();
  }

  void enableTouches() {
    touchDispatcher.enableTouches();
  }

  boolean isInKeyRepeat() {
    return keyPressTimingHandler.isInKeyRepeat();
  }

  void cancelKeyRepeat() {
    keyPressTimingHandler.cancelKeyRepeatTimer();
  }

  void dispatchPointerAction(
      final int action,
      final long eventTime,
      final int x,
      final int y,
      @NonNull PointerTracker tracker) {
    pointerActionDispatcher.dispatchPointerAction(action, eventTime, x, y, tracker);
  }

  boolean onTouchEvent(@NonNull MotionEvent nativeMotionEvent) {
    return touchDispatcher.onTouchEvent(nativeMotionEvent);
  }

  void onUpEvent(@NonNull PointerTracker tracker, int x, int y, long eventTime) {
    pointerActionDispatcher.onUpEvent(tracker, x, y, eventTime);
  }

  void onCancelEvent(@NonNull PointerTracker tracker) {
    pointerActionDispatcher.onCancelEvent(tracker);
  }

  void dismissAllKeyPreviews() {
    keyPreviewInteractor.dismissAll();
  }

  void hidePreview(int keyIndex, @NonNull PointerTracker tracker) {
    keyPreviewInteractor.hidePreview(keyIndex, tracker);
  }

  void showPreview(int keyIndex, @NonNull PointerTracker tracker) {
    keyPreviewInteractor.showPreview(keyIndex, tracker, view.getKeyboard(), view::guessLabelForKey);
  }

  void setKeyPreviewController(
      @NonNull
          wtf.uhoh.newsoftkeyboard.app.keyboards.views.preview.KeyPreviewsController controller) {
    keyPreviewControllerBinder.setKeyPreviewController(controller);
  }

  void onViewNotRequired() {
    keyboardActionListenerHolder.set(null);
  }

  void setWatermark(@NonNull List<android.graphics.drawable.Drawable> watermark) {
    // no-op
  }
}
