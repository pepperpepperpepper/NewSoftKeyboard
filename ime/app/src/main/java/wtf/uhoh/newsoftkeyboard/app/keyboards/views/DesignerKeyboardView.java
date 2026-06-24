package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewParent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;

/** A {@link KeyboardView} tuned for precise tapping (no vertical correction). */
public class DesignerKeyboardView extends KeyboardView {

  // Selection chrome is drawn directly in onDraw (not via addExtraDraw, which is disabled
  // when animations are off and re-invalidates at 60fps while alive).
  private static final int SELECTION_FILL_COLOR = 0x332196F3;
  private static final int SELECTION_STROKE_COLOR = 0xFF2196F3;

  private static final int DRAG_GHOST_FILL_COLOR = 0xAA2196F3;
  private static final int DRAG_GHOST_LABEL_COLOR = 0xFFFFFFFF;
  private static final int DROP_INDICATOR_COLOR = 0xFFFF9800;

  private final Paint mSelectionFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint mSelectionStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint mDragGhostPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint mDragGhostLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint mDropIndicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF mSelectionRect = new RectF();
  @Nullable private Keyboard.Key mSelectedKey;
  @Nullable private RectF mDragGhostRect;
  @Nullable private String mDragGhostLabel;
  @Nullable private RectF mDropIndicatorRect;

  public DesignerKeyboardView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public DesignerKeyboardView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    mSelectionFillPaint.setStyle(Paint.Style.FILL);
    mSelectionFillPaint.setColor(SELECTION_FILL_COLOR);
    mSelectionStrokePaint.setStyle(Paint.Style.STROKE);
    mSelectionStrokePaint.setColor(SELECTION_STROKE_COLOR);
    mSelectionStrokePaint.setStrokeWidth(2f * context.getResources().getDisplayMetrics().density);
    mDragGhostPaint.setStyle(Paint.Style.FILL);
    mDragGhostPaint.setColor(DRAG_GHOST_FILL_COLOR);
    mDragGhostLabelPaint.setColor(DRAG_GHOST_LABEL_COLOR);
    mDragGhostLabelPaint.setTextAlign(Paint.Align.CENTER);
    mDragGhostLabelPaint.setTextSize(18f * context.getResources().getDisplayMetrics().density);
    mDropIndicatorPaint.setStyle(Paint.Style.FILL);
    mDropIndicatorPaint.setColor(DROP_INDICATOR_COLOR);
  }

  @Override
  public boolean onTouchEvent(MotionEvent me) {
    // The editor canvas lives inside a ScrollView. Claim the gesture stream on touch-down so a
    // vertical drag (key reorder) isn't stolen by the ScrollView; release on up/cancel.
    final ViewParent parent = getParent();
    if (parent != null) {
      final int action = me.getActionMasked();
      if (action == MotionEvent.ACTION_DOWN) {
        parent.requestDisallowInterceptTouchEvent(true);
      } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
        parent.requestDisallowInterceptTouchEvent(false);
      }
    }
    return super.onTouchEvent(me);
  }

  @Override
  protected void setKeyboard(@NonNull KeyboardDefinition newKeyboard, float verticalCorrection) {
    super.setKeyboard(newKeyboard, 0f);
    mSelectedKey = null;
  }

  public void setSelectedKey(@Nullable Keyboard.Key key) {
    if (mSelectedKey == key) return;
    mSelectedKey = key;
    invalidate();
  }

  /** All rects are in view coordinates; pass nulls to clear the drag chrome. */
  public void setDragVisual(
      @Nullable RectF ghostRect, @Nullable String ghostLabel, @Nullable RectF dropIndicatorRect) {
    mDragGhostRect = ghostRect;
    mDragGhostLabel = ghostLabel;
    mDropIndicatorRect = dropIndicatorRect;
    invalidate();
  }

  @Override
  public void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    final float radius = 4f * getResources().getDisplayMetrics().density;

    final Keyboard.Key key = mSelectedKey;
    if (key != null) {
      final float left = key.x + getPaddingLeft();
      final float top = key.y + getPaddingTop();
      mSelectionRect.set(left, top, left + key.width, top + key.height);
      canvas.drawRoundRect(mSelectionRect, radius, radius, mSelectionFillPaint);
      canvas.drawRoundRect(mSelectionRect, radius, radius, mSelectionStrokePaint);
    }

    final RectF indicator = mDropIndicatorRect;
    if (indicator != null) {
      canvas.drawRoundRect(indicator, radius / 2f, radius / 2f, mDropIndicatorPaint);
    }

    final RectF ghost = mDragGhostRect;
    if (ghost != null) {
      canvas.drawRoundRect(ghost, radius, radius, mDragGhostPaint);
      final String label = mDragGhostLabel;
      if (label != null && !label.isEmpty()) {
        final float textY =
            ghost.centerY() - (mDragGhostLabelPaint.ascent() + mDragGhostLabelPaint.descent()) / 2f;
        canvas.drawText(label, ghost.centerX(), textY, mDragGhostLabelPaint);
      }
    }
  }
}
