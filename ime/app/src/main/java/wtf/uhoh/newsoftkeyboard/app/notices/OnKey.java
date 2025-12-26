package wtf.uhoh.newsoftkeyboard.app.notices;

import androidx.annotation.Nullable;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;

public interface OnKey extends PublicNotice {
  void onKey(PublicNotices ime, int primaryCode, @Nullable Keyboard.Key key);
}
