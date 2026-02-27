package wtf.uhoh.newsoftkeyboard.app.debug;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

/**
 * A text editor which reports {@code EditorInfo.inputType} with no class bits set, but with {@code
 * TYPE_TEXT_FLAG_NO_SUGGESTIONS} enabled.
 *
 * <p>This emulates editors which set flags/variations but omit {@code TYPE_CLASS_TEXT}.
 */
public class ClasslessNoSuggestionsEditText extends EditText {

  public ClasslessNoSuggestionsEditText(Context context) {
    super(context);
  }

  public ClasslessNoSuggestionsEditText(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ClasslessNoSuggestionsEditText(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
    final InputConnection inputConnection = super.onCreateInputConnection(outAttrs);
    if (outAttrs != null) {
      outAttrs.inputType = EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
    }
    return inputConnection;
  }
}
