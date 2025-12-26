package wtf.uhoh.newsoftkeyboard.app.keyboards.views.preview;

import android.graphics.drawable.Drawable;
import android.view.View;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;

public interface KeyPreviewsController {
  void hidePreviewForKey(Keyboard.Key key);

  void showPreviewForKey(
      Keyboard.Key key, Drawable icon, View parentView, PreviewPopupTheme previewPopupTheme);

  void showPreviewForKey(
      Keyboard.Key key, CharSequence label, View parentView, PreviewPopupTheme previewPopupTheme);

  void cancelAllPreviews();

  void destroy();
}
