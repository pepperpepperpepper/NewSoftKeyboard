package wtf.uhoh.newsoftkeyboard.app.keyboards.views.extradraw;

import android.graphics.Canvas;
import android.graphics.Paint;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewWithExtraDraw;

public interface ExtraDraw {
  boolean onDraw(Canvas canvas, Paint keyValuesPaint, KeyboardViewWithExtraDraw parentKeyboardView);
}
