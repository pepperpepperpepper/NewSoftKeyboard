package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import wtf.uhoh.newsoftkeyboard.app.theme.KeyboardTheme;
import wtf.uhoh.newsoftkeyboard.gesturetyping.GestureTrailTheme;
import wtf.uhoh.newsoftkeyboard.gesturetyping.GestureTypingPathDraw;
import wtf.uhoh.newsoftkeyboard.gesturetyping.GestureTypingPathDrawHelper;

final class GestureTrailRenderer {

  private final GestureTypingPathDraw.OnInvalidateCallback invalidateCallback;
  private GestureTypingPathDraw gestureDrawingHelper;
  private boolean gestureTypingActive = false;
  private boolean shouldDraw = false;

  GestureTrailRenderer(@NonNull GestureTypingPathDraw.OnInvalidateCallback invalidateCallback) {
    this.invalidateCallback = invalidateCallback;
  }

  void onThemeSet(@NonNull Context context, @NonNull KeyboardTheme theme) {
    gestureDrawingHelper =
        GestureTypingPathDrawHelper.create(
            invalidateCallback,
            GestureTrailTheme.fromThemeResource(
                context,
                theme.getPackageContext(),
                theme.getResourceMapping(),
                theme.getGestureTrailThemeResId()));
  }

  void onTouchesDisabled() {
    gestureTypingActive = false;
    shouldDraw = false;
  }

  void onTouchEvent(@NonNull MotionEvent motionEvent, @NonNull PointerTracker pointerTracker) {
    gestureTypingActive = pointerTracker.isInGestureTyping();
    if (gestureDrawingHelper != null) {
      gestureDrawingHelper.handleTouchEvent(motionEvent);
    }

    shouldDraw = gestureTypingActive && motionEvent.getActionMasked() == MotionEvent.ACTION_MOVE;
  }

  boolean shouldDisableGestureDetector() {
    return gestureTypingActive;
  }

  void draw(@NonNull Canvas canvas) {
    if (!shouldDraw || gestureDrawingHelper == null) return;
    gestureDrawingHelper.draw(canvas);
  }
}
