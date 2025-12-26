package wtf.uhoh.newsoftkeyboard.app.keyboards.views.extradraw;

import android.graphics.Paint;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewWithExtraDraw;

public interface PaintModifier<T> {
  Paint modify(Paint original, KeyboardViewWithExtraDraw ime, T extraData);
}
