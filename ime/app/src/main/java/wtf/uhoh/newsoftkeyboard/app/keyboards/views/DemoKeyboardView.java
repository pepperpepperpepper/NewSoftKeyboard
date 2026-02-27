package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import java.lang.ref.WeakReference;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;

/** Will render the keyboard view but will not provide ANY interactivity. */
@SuppressWarnings("this-escape")
public class DemoKeyboardView extends KeyboardView {
  private final TypingSimulator mTypingSimulator;
  @Nullable private OnViewBitmapReadyListener mOnViewBitmapReadyListener = null;
  private final int mInitialKeyboardWidth;
  private float mKeyboardScale = 1f;

  @Nullable private RectF mHighlightRect = null;
  private long mHighlightStartTimeMs = 0;
  private long mHighlightDurationMs = 0;
  @Nullable private Paint mHighlightPaint = null;

  public DemoKeyboardView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public DemoKeyboardView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    mTypingSimulator = new TypingSimulator(this);

    // CHECKSTYLE:OFF: RawGetKeyboardTheme
    setKeyboardTheme(NskApplicationBase.getKeyboardThemeFactory(getContext()).getEnabledAddOn());
    // CHECKSTYLE:ON: RawGetKeyboardTheme

    mInitialKeyboardWidth = getThemedKeyboardDimens().getKeyboardMaxWidth();
  }

  @Override
  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    KeyboardDefinition keyboard = getKeyboard();
    if (keyboard == null) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    } else {
      int width = keyboard.getMinWidth() + getPaddingLeft() + getPaddingRight();
      if (MeasureSpec.getSize(widthMeasureSpec) < width + 10) {
        width = MeasureSpec.getSize(widthMeasureSpec);
        mKeyboardScale = ((float) width) / mInitialKeyboardWidth;
      } else {
        mKeyboardScale = 1f;
      }
      int height = keyboard.getHeight() + getPaddingTop() + getPaddingBottom();
      setMeasuredDimension((int) (width / mKeyboardScale), (int) (height * mKeyboardScale));
    }
  }

  @Override
  public void onDraw(Canvas canvas) {
    canvas.scale(mKeyboardScale, mKeyboardScale);
    super.onDraw(canvas);
    drawHighlight(canvas);
  }

  @Override
  public boolean onTouchEvent(@NonNull MotionEvent me) {
    // not handling ANY touch event.
    return false;
  }

  private void simulateKeyTouchEvent(int primaryCode, boolean isDownEvent) {
    final KeyboardDefinition keyboard = getKeyboard();
    if (keyboard == null) return;

    for (Keyboard.Key key : keyboard.getKeys()) {
      if (key.getPrimaryCode() == primaryCode) {
        final long eventTime = SystemClock.uptimeMillis();
        final long downEventTime = eventTime - (isDownEvent ? 0 : TypingSimulator.KEY_DOWN_DELAY);
        MotionEvent motionEvent =
            MotionEvent.obtain(
                downEventTime,
                eventTime,
                isDownEvent ? MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP,
                Keyboard.Key.getCenterX(key),
                Keyboard.Key.getCenterY(key),
                0);
        super.onTouchEvent(motionEvent);
        motionEvent.recycle();
      }
    }
  }

  private void simulateCancelTouchEvent() {
    final long eventTime = SystemClock.uptimeMillis();
    MotionEvent motionEvent =
        MotionEvent.obtain(eventTime, eventTime, MotionEvent.ACTION_CANCEL, 0, 0, 0);
    super.onTouchEvent(motionEvent);
    motionEvent.recycle();
  }

  public void simulateKeyDown(int primaryCode) {
    simulateKeyTouchEvent(primaryCode, true);
  }

  public void simulateKeyUp(int primaryCode) {
    simulateKeyTouchEvent(primaryCode, false);
  }

  public void simulateCancel() {
    simulateCancelTouchEvent();
  }

  public void highlightKey(@NonNull Keyboard.Key key) {
    highlightRect(key.x, key.y, key.x + key.width, key.y + key.height, 1200);
  }

  public void highlightRect(int left, int top, int right, int bottom, long durationMs) {
    mHighlightRect = new RectF(left, top, right, bottom);
    mHighlightStartTimeMs = SystemClock.uptimeMillis();
    mHighlightDurationMs = durationMs;
    postInvalidateOnAnimation();
  }

  private void drawHighlight(@NonNull Canvas canvas) {
    final RectF rect = mHighlightRect;
    if (rect == null) return;

    final long now = SystemClock.uptimeMillis();
    final long elapsed = now - mHighlightStartTimeMs;
    if (elapsed >= mHighlightDurationMs) {
      mHighlightRect = null;
      return;
    }

    final float fractionLeft =
        1f - (mHighlightDurationMs == 0 ? 1f : (elapsed / (float) mHighlightDurationMs));
    final int alpha = (int) (200f * Math.max(0f, Math.min(1f, fractionLeft)));

    Paint paint = mHighlightPaint;
    if (paint == null) {
      paint = new Paint(Paint.ANTI_ALIAS_FLAG);
      paint.setStyle(Paint.Style.STROKE);
      final float density = getResources().getDisplayMetrics().density;
      paint.setStrokeWidth(2f * density);
      paint.setColor(
          ContextCompat.getColor(getContext(), wtf.uhoh.newsoftkeyboard.R.color.app_accent));
      mHighlightPaint = paint;
    }
    paint.setAlpha(alpha);

    final float density = getResources().getDisplayMetrics().density;
    final float radius = 6f * density;
    canvas.drawRoundRect(rect, radius, radius, paint);
    postInvalidateOnAnimation();
  }

  public void setOnViewBitmapReadyListener(@NonNull OnViewBitmapReadyListener listener) {
    mOnViewBitmapReadyListener = listener;
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    final OnViewBitmapReadyListener listener = mOnViewBitmapReadyListener;
    if (changed && listener != null && getWidth() > 0 && getHeight() > 0) {
      final Bitmap bitmap = generateBitmapFromView();
      if (bitmap != null) {
        listener.onViewBitmapReady(bitmap);
      }
    }
  }

  private Bitmap generateBitmapFromView() {
    Bitmap b = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(b);
    draw(c);
    return b;
  }

  public void setSimulatedTypingText(@Nullable String textToSimulate) {
    if (TextUtils.isEmpty(textToSimulate)) {
      mTypingSimulator.stopSimulating();
    } else {
      mTypingSimulator.startSimulating(textToSimulate);
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    mTypingSimulator.onViewDetach();
  }

  @Override
  public void onStartTemporaryDetach() {
    super.onStartTemporaryDetach();
    mTypingSimulator.onViewDetach();
  }

  @Override
  public void onFinishTemporaryDetach() {
    super.onFinishTemporaryDetach();
    mTypingSimulator.onViewAttach();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    mTypingSimulator.onViewAttach();
  }

  private static class TypingSimulator extends Handler {
    private static final long INITIAL_DELAY = 512;
    private static final long NEXT_KEY_DELAY = 256;
    private static final long NEXT_KEY_SPACE_DELAY = 512;
    private static final long NEXT_CYCLE_DELAY = 1024;
    private static final long KEY_DOWN_DELAY = 128;

    private static final int PRESS_MESSAGE = 109;
    private static final int RELEASE_MESSAGE = 110;
    private static final int CANCEL_MESSAGE = 111;

    private final WeakReference<DemoKeyboardView> mDemoKeyboardViewWeakReference;
    @NonNull private String mTextToSimulate = "";
    private int mSimulationIndex = 0;
    private boolean mIsEnabled;

    private TypingSimulator(@NonNull DemoKeyboardView keyboardView) {
      super(Looper.getMainLooper());
      mDemoKeyboardViewWeakReference = new WeakReference<>(keyboardView);
    }

    public void startSimulating(@NonNull String textToSimulate) {
      stopSimulating();
      mTextToSimulate = textToSimulate;
      if (!TextUtils.isEmpty(mTextToSimulate)) {
        sendMessageDelayed(obtainMessage(PRESS_MESSAGE), INITIAL_DELAY);
      }
    }

    public void stopSimulating() {
      clearPressMessages();
      mTextToSimulate = "";
      mSimulationIndex = 0;
    }

    private void clearPressMessages() {
      removeMessages(PRESS_MESSAGE);
      removeMessages(RELEASE_MESSAGE);
      removeMessages(CANCEL_MESSAGE);
    }

    @Override
    public void handleMessage(Message msg) {
      DemoKeyboardView keyboardView = mDemoKeyboardViewWeakReference.get();
      if (keyboardView == null || mTextToSimulate.length() == 0) return;
      final char keyToSimulate = mTextToSimulate.charAt(mSimulationIndex);
      switch (msg.what) {
        case PRESS_MESSAGE:
          if (mIsEnabled) keyboardView.simulateKeyTouchEvent(keyToSimulate, true);
          if (mIsEnabled) {
            sendMessageDelayed(obtainMessage(RELEASE_MESSAGE), KEY_DOWN_DELAY);
          }
          break;
        case RELEASE_MESSAGE:
          // sending RELEASE even if we are disabled
          keyboardView.simulateKeyTouchEvent(keyToSimulate, false);
          mSimulationIndex++;
          if (mSimulationIndex == mTextToSimulate.length()) {
            mSimulationIndex = 0;
            if (mIsEnabled) {
              sendMessageDelayed(obtainMessage(PRESS_MESSAGE), NEXT_CYCLE_DELAY);
            }
          } else {
            if (mIsEnabled) {
              sendMessageDelayed(
                  obtainMessage(PRESS_MESSAGE),
                  (keyToSimulate == ' ') ? NEXT_KEY_SPACE_DELAY : NEXT_KEY_DELAY);
            }
          }
          break;
        case CANCEL_MESSAGE:
          keyboardView.simulateCancelTouchEvent();
          keyboardView.resetInputView();
          break;
        default:
          super.handleMessage(msg);
          break;
      }
    }

    public void onViewDetach() {
      if (!mIsEnabled) return;

      mIsEnabled = false;
      clearPressMessages();
      sendMessage(obtainMessage(CANCEL_MESSAGE));
    }

    public void onViewAttach() {
      if (mIsEnabled) return;
      mIsEnabled = true;
      startSimulating(mTextToSimulate);
    }
  }

  public interface OnViewBitmapReadyListener {
    void onViewBitmapReady(Bitmap bitmap);
  }
}
