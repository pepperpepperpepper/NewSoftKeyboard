package wtf.uhoh.newsoftkeyboard.app.debug;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

/** A text editor which reports {@code EditorInfo.inputType=0} (TYPE_NULL) to the IME. */
public class TypeNullEditText extends EditText {

  public TypeNullEditText(Context context) {
    super(context);
  }

  public TypeNullEditText(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public TypeNullEditText(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
    final InputConnection inputConnection = super.onCreateInputConnection(outAttrs);
    if (outAttrs != null) {
      outAttrs.inputType = 0;
    }
    return inputConnection;
  }
}
