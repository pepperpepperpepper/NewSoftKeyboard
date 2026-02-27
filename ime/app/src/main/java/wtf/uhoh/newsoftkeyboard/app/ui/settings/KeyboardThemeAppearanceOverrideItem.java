package wtf.uhoh.newsoftkeyboard.app.ui.settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class KeyboardThemeAppearanceOverrideItem {
  @NonNull final CharSequence title;
  @Nullable final CharSequence summary;
  @NonNull final String scrollToKey;
  @NonNull final Runnable resetAction;

  KeyboardThemeAppearanceOverrideItem(
      @NonNull CharSequence title,
      @Nullable CharSequence summary,
      @NonNull String scrollToKey,
      @NonNull Runnable resetAction) {
    this.title = title;
    this.summary = summary;
    this.scrollToKey = scrollToKey;
    this.resetAction = resetAction;
  }
}
