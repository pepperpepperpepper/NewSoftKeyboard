package wtf.uhoh.newsoftkeyboard.app.keyboards.views;

import android.os.Looper;
import androidx.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.List;

/** Helper so instrumentation can request candidate picks inside the app process (release build). */
public final class CandidateViewTestRegistry {
  private static volatile WeakReference<CandidateView> sActive = new WeakReference<>(null);

  private CandidateViewTestRegistry() {}

  static void setActive(CandidateView view) {
    sActive = new WeakReference<>(view);
  }

  public static boolean pickByIndex(final int index) {
    final CandidateView v = sActive.get();
    if (v == null) return false;
    v.post(() -> v.pickCandidateAtForTest(index));
    return true;
  }

  /** Returns the current number of visible suggestions, or 0 if none/unknown. */
  public static int getCount() {
    final CandidateView v = sActive.get();
    if (v == null) return 0;
    final java.util.List<CharSequence> list = v.getSuggestions();
    return list == null ? 0 : list.size();
  }

  /** Returns a suggestion text at index, or null if unavailable. */
  public static String getSuggestionAt(int index) {
    final CandidateView v = sActive.get();
    if (v == null) return null;
    final List<CharSequence> list = v.getSuggestions();
    if (list == null || index < 0 || index >= list.size()) return null;
    final CharSequence cs = list.get(index);
    return cs == null ? null : cs.toString();
  }

  /** Picks a candidate immediately (main-thread) and returns its text, or null if unavailable. */
  @Nullable
  public static String pickNowAndReturnSuggestionAt(final int index) {
    final CandidateView v = sActive.get();
    if (v == null) return null;
    final List<CharSequence> list = v.getSuggestions();
    if (list == null || index < 0 || index >= list.size()) return null;
    final CharSequence cs = list.get(index);
    final String text = cs == null ? null : cs.toString();
    if (Looper.myLooper() == Looper.getMainLooper()) {
      v.pickCandidateAtForTest(index);
    } else {
      v.post(() -> v.pickCandidateAtForTest(index));
    }
    return text;
  }

  /** Picks a candidate only if it exists at the given index, returning true on scheduling. */
  public static boolean pickIfAvailable(final int index) {
    final CandidateView v = sActive.get();
    if (v == null) return false;
    final List<CharSequence> list = v.getSuggestions();
    if (list == null || index < 0 || index >= list.size()) return false;
    v.post(() -> v.pickCandidateAtForTest(index));
    return true;
  }
}
