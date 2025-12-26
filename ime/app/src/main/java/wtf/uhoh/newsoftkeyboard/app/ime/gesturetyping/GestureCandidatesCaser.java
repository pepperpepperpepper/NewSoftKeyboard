package wtf.uhoh.newsoftkeyboard.app.ime.gesturetyping;

import androidx.annotation.NonNull;
import java.util.List;
import java.util.Locale;

public final class GestureCandidatesCaser {

  private GestureCandidatesCaser() {}

  public static void applyCasing(
      @NonNull List<String> candidates,
      boolean gestureWasShifted,
      boolean capsLocked,
      @NonNull Locale locale) {
    if (!(gestureWasShifted || capsLocked)) {
      return;
    }

    for (int i = 0; i < candidates.size(); ++i) {
      final String word = candidates.get(i);
      if (word.isEmpty()) continue;

      if (capsLocked) {
        candidates.set(i, word.toUpperCase(locale));
      } else {
        candidates.set(i, word.substring(0, 1).toUpperCase(locale) + word.substring(1));
      }
    }
  }
}
