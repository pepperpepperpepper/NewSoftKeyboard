package wtf.uhoh.newsoftkeyboard.app.notices;

import android.view.inputmethod.EditorInfo;
import wtf.uhoh.newsoftkeyboard.app.keyboards.KeyboardDefinition;

public interface OnVisible extends PublicNotice {
  void onVisible(PublicNotices ime, KeyboardDefinition keyboard, EditorInfo editorInfo);

  void onHidden(PublicNotices ime, KeyboardDefinition keyboard);
}
