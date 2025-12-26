package wtf.uhoh.newsoftkeyboard.app.notices;

import android.view.inputmethod.EditorInfo;
import java.util.ArrayList;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.app.NskApplicationBase;
import wtf.uhoh.newsoftkeyboard.app.ime.ImeServiceBase;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;

public abstract class PublicNotices extends ImeServiceBase {

  private final List<OnKey> mOnKeyListeners = new ArrayList<>();
  private final List<OnVisible> mOnVisibleListeners = new ArrayList<>();

  @Override
  public void onCreate() {
    super.onCreate();
    for (PublicNotice publicNotice : ((NskApplicationBase) getApplication()).getPublicNotices()) {
      if (publicNotice instanceof OnKey) mOnKeyListeners.add((OnKey) publicNotice);
      if (publicNotice instanceof OnVisible) mOnVisibleListeners.add((OnVisible) publicNotice);
    }
  }

  @Override
  public void onKey(
      int primaryCode, Keyboard.Key key, int multiTapIndex, int[] nearByKeyCodes, boolean fromUI) {
    super.onKey(primaryCode, key, multiTapIndex, nearByKeyCodes, fromUI);
    for (OnKey onKey : mOnKeyListeners) {
      onKey.onKey(this, primaryCode, key);
    }
  }

  @Override
  public void onStartInputView(EditorInfo attribute, boolean restarting) {
    super.onStartInputView(attribute, restarting);
    for (OnVisible onVisibleListener : mOnVisibleListeners) {
      onVisibleListener.onVisible(this, getCurrentKeyboard(), attribute);
    }
  }

  @Override
  public void onFinishInputView(boolean finishingInput) {
    super.onFinishInputView(finishingInput);
    for (OnVisible onVisibleListener : mOnVisibleListeners) {
      onVisibleListener.onHidden(this, getCurrentKeyboard());
    }
  }
}
