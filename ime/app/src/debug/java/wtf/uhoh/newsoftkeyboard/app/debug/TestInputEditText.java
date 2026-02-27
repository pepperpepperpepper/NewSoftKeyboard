package wtf.uhoh.newsoftkeyboard.app.debug;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import androidx.annotation.Nullable;

/**
 * Debug-only editor for instrumentation that can simulate composing-hostile behavior.
 *
 * <p>Some editors return {@code true} from {@link InputConnection#setComposingText} while not
 * reflecting composing text in the UI and/or not supporting readback via {@link
 * InputConnection#getTextBeforeCursor}. This view can simulate that class of issues.
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
      // Some apps (for example terminal/remote-control UIs) request TYPE_NULL (inputType==0).
      // Use this to validate that the IME does not attempt prediction/composing in that mode.
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
        // Simulate an editor that reports success but doesn't show composing text.
        return true;
      }
    };
  }
}
