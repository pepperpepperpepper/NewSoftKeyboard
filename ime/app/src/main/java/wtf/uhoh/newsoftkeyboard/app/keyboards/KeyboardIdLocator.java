package wtf.uhoh.newsoftkeyboard.app.keyboards;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.base.utils.Logger;

/** Finds an enabled keyboard index by its id, with helpful logging when not found. */
final class KeyboardIdLocator {

  private static final int NOT_FOUND = -1;

  private KeyboardIdLocator() {}

  static int findIndexOrLog(
      @NonNull String tag,
      @NonNull List<KeyboardAddOnAndBuilder> enabledKeyboardsBuilders,
      @NonNull String keyboardId) {
    final int keyboardsCount = enabledKeyboardsBuilders.size();
    for (int keyboardIndex = 0; keyboardIndex < keyboardsCount; keyboardIndex++) {
      if (TextUtils.equals(enabledKeyboardsBuilders.get(keyboardIndex).getId(), keyboardId)) {
        return keyboardIndex;
      }
    }

    Logger.w(tag, "For some reason, I can't find keyboard with ID " + keyboardId);
    Logger.d(tag, "Available keyboard IDs:");
    for (int i = 0; i < keyboardsCount; i++) {
      Logger.d(tag, "  " + i + ": " + enabledKeyboardsBuilders.get(i).getId());
    }
    return NOT_FOUND;
  }

  @Nullable
  static Integer findIndexOrNull(
      @NonNull String tag,
      @NonNull List<KeyboardAddOnAndBuilder> enabledKeyboardsBuilders,
      @NonNull String keyboardId) {
    final int index = findIndexOrLog(tag, enabledKeyboardsBuilders, keyboardId);
    return index == NOT_FOUND ? null : index;
  }
}
