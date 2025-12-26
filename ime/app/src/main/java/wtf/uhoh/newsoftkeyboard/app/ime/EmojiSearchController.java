package wtf.uhoh.newsoftkeyboard.app.ime;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import wtf.uhoh.newsoftkeyboard.R;
import wtf.uhoh.newsoftkeyboard.app.keyboards.Keyboard;
import wtf.uhoh.newsoftkeyboard.app.keyboards.views.KeyboardViewContainerView;
import wtf.uhoh.newsoftkeyboard.app.quicktextkeys.QuickKeyHistoryRecords;
import wtf.uhoh.newsoftkeyboard.app.quicktextkeys.TagsExtractor;
import wtf.uhoh.newsoftkeyboard.app.quicktextkeys.ui.EmojiSearchOverlay;

/**
 * Encapsulates all Emoji Search overlay interactions so {@link
 * wtf.uhoh.newsoftkeyboard.app.ime.ImeServiceBase} can stay lean.
 */
public final class EmojiSearchController {

  public interface Host {
    @Nullable
    TagsExtractor getQuickTextTagsSearcher();

    @NonNull
    QuickKeyHistoryRecords getQuickKeyHistoryRecords();

    boolean handleCloseRequest();

    void showToastMessage(@StringRes int resId, boolean important);

    @Nullable
    KeyboardViewContainerView getInputViewContainer();

    void commitEmojiFromSearch(CharSequence emoji);

    @NonNull
    Context getContext();
  }

  private final Host host;
  @Nullable private EmojiSearchOverlay overlay;

  public EmojiSearchController(@NonNull Host host) {
    this.host = host;
  }

  public boolean requestShow() {
    if (isShowing()) {
      return true;
    }
    final TagsExtractor tagsExtractor = host.getQuickTextTagsSearcher();
    if (tagsExtractor == null || !tagsExtractor.isEnabled()) {
      host.showToastMessage(R.string.emoji_search_disabled_toast, true);
      return false;
    }

    host.handleCloseRequest();
    ensureOverlay();
    final KeyboardViewContainerView anchor = host.getInputViewContainer();
    if (anchor == null) {
      return false;
    }

    overlay.show(
        anchor,
        tagsExtractor,
        host.getQuickKeyHistoryRecords(),
        new EmojiSearchOverlay.Listener() {
          @Override
          public void onEmojiPicked(CharSequence emoji) {
            host.commitEmojiFromSearch(emoji);
          }

          @Override
          public void onOverlayDismissed() {
            // no-op; state derived from isShowing()
          }
        });
    return true;
  }

  public boolean handleOverlayKey(int primaryCode, @Nullable Keyboard.Key key) {
    return isShowing() && overlay.handleKey(primaryCode, key);
  }

  public boolean handleOverlayText(CharSequence text) {
    return isShowing() && overlay.handleText(text);
  }

  public boolean dismissOverlay() {
    if (isShowing()) {
      overlay.dismiss();
      return true;
    }
    return false;
  }

  public boolean isShowing() {
    return overlay != null && overlay.isShowing();
  }

  public void onWindowHidden() {
    dismissOverlay();
  }

  private void ensureOverlay() {
    if (overlay == null) {
      overlay = new EmojiSearchOverlay(host.getContext());
    }
  }
}
