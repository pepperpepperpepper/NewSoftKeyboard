package wtf.uhoh.newsoftkeyboard.app.debug;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import androidx.annotation.Nullable;

/**
 * Release-compatible editor for instrumentation that can simulate composing-hostile behavior.
 *
 * <p>Keeping this class in the release source-set allows connected tests that run against the
 * release variant to reproduce tricky editor behaviors.
 */
public class TestInputEditText extends android.widget.EditText {

  private boolean mSimulateInvisibleComposing;
  private boolean mSimulateTypeNull;

  public TestInputEditText(Context context) {
    super(context);
  }

  public TestInputEditText(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public TestInputEditText(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public void setSimulateInvisibleComposing(boolean enabled) {
    mSimulateInvisibleComposing = enabled;
  }

  public void setSimulateTypeNull(boolean enabled) {
    mSimulateTypeNull = enabled;
  }

  @Override
  public @Nullable InputConnection onCreateInputConnection(EditorInfo outAttrs) {
    final InputConnection base = super.onCreateInputConnection(outAttrs);
    if (outAttrs != null && mSimulateTypeNull) {
      outAttrs.inputType = 0;
    }
    if (base == null || !mSimulateInvisibleComposing) {
      return base;
    }

    return new InputConnectionWrapper(base, true /*mutable*/) {
      @Override
      public @Nullable CharSequence getTextBeforeCursor(int n, int flags) {
        return null;
      }

      @Override
      public boolean setComposingText(CharSequence text, int newCursorPosition) {
        return true;
      }
    };
  }
}
