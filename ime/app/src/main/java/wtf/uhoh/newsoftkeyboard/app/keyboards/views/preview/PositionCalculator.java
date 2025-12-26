package wtf.uhoh.newsoftkeyboard.app.keyboards.views.preview;

import android.graphics.Point;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;

public interface PositionCalculator {
  Point calculatePositionForPreview(Keyboard.Key key, PreviewPopupTheme theme, int[] windowOffset);
}
