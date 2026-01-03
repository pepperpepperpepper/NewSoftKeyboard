package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;

/** A {@link KeyboardView} tuned for precise tapping (no vertical correction). */
public class DesignerKeyboardView extends KeyboardView {

  public DesignerKeyboardView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public DesignerKeyboardView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  protected void setKeyboard(@NonNull KeyboardDefinition newKeyboard, float verticalCorrection) {
    super.setKeyboard(newKeyboard, 0f);
  }
}
