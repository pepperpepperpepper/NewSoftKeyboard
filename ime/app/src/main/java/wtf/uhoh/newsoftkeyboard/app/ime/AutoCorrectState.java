package wtf.uhoh.newsoftkeyboard.app.ime;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Tracks auto-correct related flags and revert length. */
final class AutoCorrectState {
  private static final int MAX_REJECTED_CORRECTIONS = 64;

  int wordRevertLength = 0;
  boolean justAutoAddedWord = false;

  // Corrections the user explicitly reverted (lowercase typed -> lowercase correction).
  // Deliberately NOT cleared in reset(): reset fires on cursor moves, paste, etc., and the
  // rejection memory should survive those — it clears when a new input session starts.
  private final Map<String, String> mRejectedCorrections = new HashMap<>();

  void reset() {
    wordRevertLength = 0;
    justAutoAddedWord = false;
  }

  boolean shouldRevertOnDelete() {
    return wordRevertLength > 0;
  }

  void recordRejectedCorrection(
      @NonNull CharSequence typedWord, @NonNull CharSequence rejectedCorrection) {
    if (TextUtils.isEmpty(typedWord) || TextUtils.isEmpty(rejectedCorrection)) return;
    if (mRejectedCorrections.size() >= MAX_REJECTED_CORRECTIONS) mRejectedCorrections.clear();
    mRejectedCorrections.put(
        typedWord.toString().toLowerCase(Locale.ROOT),
        rejectedCorrection.toString().toLowerCase(Locale.ROOT));
  }

  boolean isRejectedCorrection(@NonNull CharSequence typedWord, @NonNull CharSequence candidate) {
    if (mRejectedCorrections.isEmpty()) return false;
    final String rejected = mRejectedCorrections.get(typedWord.toString().toLowerCase(Locale.ROOT));
    return rejected != null && rejected.equals(candidate.toString().toLowerCase(Locale.ROOT));
  }

  void clearRejectedCorrections() {
    mRejectedCorrections.clear();
  }
}
